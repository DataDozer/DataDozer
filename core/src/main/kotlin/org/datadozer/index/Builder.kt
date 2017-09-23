package org.datadozer.index

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.codecs.Codec
import org.apache.lucene.codecs.lucene50.Lucene50StoredFieldsFormat
import org.apache.lucene.codecs.lucene62.Lucene62Codec
import org.apache.lucene.index.ConcurrentMergeScheduler
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy
import org.apache.lucene.index.PersistentSnapshotDeletionPolicy
import org.apache.lucene.search.similarities.BM25Similarity
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.store.*
import org.datadozer.LuceneDirectory
import org.datadozer.Message
import org.datadozer.OperationException
import org.datadozer.index.fields.*
import org.datadozer.models.*
import org.datadozer.services.AnalyzerService
import org.datadozer.validate
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/*
 * Licensed to DataDozer under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. DataDozer licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * General index settings
 */
class IndexSettings private constructor(
        val indexName: String,
        val indexId: Int,
        val indexAnalyzer: AnalyzerWrapper,
        val searchAnalyzer: AnalyzerWrapper,
        val fields: FieldCollection,
        val indexConfiguration: IndexConfiguration,
        val baseDir: File,
        val settingsDir: File,
        val shardConfiguration: ShardConfiguration
) {

    val shardCount = shardConfiguration.shardCount
    val optimisticConcurrency = if (indexConfiguration.hasSupportOptimisticConcurrency()) {
        indexConfiguration.supportOptimisticConcurrency.value
    } else {
        false
    }

    class Builder(
            private val index: Index,
            private val baseDir: File,
            private val setttingsDir: File,
            private val analyzerService: AnalyzerService) {

        private var indexAnalyzer: AnalyzerWrapper? = null
        private var searchAnalyzer: AnalyzerWrapper? = null
        private var fields = FieldCollection()
        private var fieldCount = 2

        fun build(): IndexSettings {
            if (!baseDir.exists()) {
                baseDir.mkdir()
            }

            buildMetadataFields()
            buildFields()
            buildAnalyzers()
            return IndexSettings(
                    indexName = index.indexName,
                    indexId = 0,
                    indexAnalyzer = indexAnalyzer!!,
                    searchAnalyzer = searchAnalyzer!!,
                    fields = fields,
                    indexConfiguration = index.indexConfiguration,
                    baseDir = baseDir,
                    settingsDir = setttingsDir,
                    shardConfiguration = index.shardConfiguration
            )
        }

        /**
         * Creates per fields analyzer for an index from the index fields data. These analyzers
         * are used for searching and indexing rather than the global fields analyzer.
         */
        private fun buildAnalyzer(isIndexAnalyzer: Boolean): AnalyzerWrapper {
            val map = ConcurrentHashMap<String, Analyzer>()
            (fields as Collection<FieldSchema>)
                    .filter { it.analyzers != null }
                    .forEach {
                        if (isIndexAnalyzer) {
                            map.put(it.schemaName, it.analyzers!!.indexAnalyzer)
                        } else {
                            map.put(it.schemaName, it.analyzers!!.searchAnalyzer)
                        }
                    }
            return AnalyzerWrapper(map)
        }

        private fun buildAnalyzers() {
            searchAnalyzer = buildAnalyzer(false)
            indexAnalyzer = buildAnalyzer(true)
        }

        /**
         * Returns a fields instance associated with a fields data type
         */
        private fun getFieldType(fieldType: FieldDataType): IndexField {
            return when (fieldType) {
                FieldDataType.TEXT -> TextField.INSTANCE
                FieldDataType.KEYWORD -> KeywordField.INSTANCE
                FieldDataType.INTEGER_POINT -> IntField.INSTANCE
                FieldDataType.LONG_PONT -> LongField.INSTANCE
                FieldDataType.FLOAT_POINT -> FloatField.INSTANCE
                FieldDataType.DOUBLE_POINT -> DoubleField.INSTANCE
                FieldDataType.BINARY_POINT -> TODO()
                FieldDataType.DATE -> DateField.INSTANCE
                FieldDataType.DATE_TIME -> DateTimeField.INSTANCE
                FieldDataType.STORED_ONLY -> StoredField.INSTANCE
                FieldDataType.IP4 -> TODO()
                FieldDataType.IP6 -> TODO()
                FieldDataType.LAT_LONG -> TODO()
                FieldDataType.BOOLEAN -> BoolField.INSTANCE
                FieldDataType.TIMESTAMP -> TimeStampField.INSTANCE
                FieldDataType.UNRECOGNIZED -> TODO()
            }
        }

        private fun buildFieldSchema(field: Field): FieldSchema {
            val dataType = getFieldType(field.dataType)
            val indexAnalyzer =
                    if (dataType.supportsAnalyzer && field.hasIndexAnalyzer()) {
                        analyzerService.getAnalyzer(field.indexAnalyzer.value) ?: throw OperationException(
                                Message.analyzerNotFound(field.indexAnalyzer.value))
                    } else {
                        null
                    }

            val searchAnalyzer =
                    if (dataType.supportsAnalyzer && field.hasSearchAnalyzer()) {
                        analyzerService.getAnalyzer(field.searchAnalyzer.value) ?: throw OperationException(
                                Message.analyzerNotFound(field.searchAnalyzer.value))
                    } else {
                        null
                    }

            return FieldSchema(
                    schemaName = field.fieldName,
                    fieldName = field.fieldName,
                    fieldOrder = fieldCount,
                    docValues = if (field.hasAllowSort()) {
                        field.allowSort.value
                    } else {
                        false
                    },
                    analyzers = if (indexAnalyzer != null && searchAnalyzer != null) {
                        FieldAnalyzers(searchAnalyzer, indexAnalyzer)
                    } else {
                        null
                    },
                    similarity = field.similarity,
                    fieldType = dataType,
                    multiValued = if (field.hasMultivalued()) {
                        field.multivalued.value
                    } else {
                        false
                    }
            )
        }

        private fun buildFields() {
            index.fieldsList.forEach {
                it.validate()
                fields.add(buildFieldSchema(it))
            }
        }

        private fun buildMetadataFields() {
            fields.add(IdField.SCHEMA)
            fields.add(ModifyIndex.SCHEMA)
        }
    }
}

fun getDirectoryType(directoryType: DirectoryType, file: File): LuceneDirectory {
    val lockFactory = NativeFSLockFactory.getDefault()
    return when (directoryType) {

        DirectoryType.MEMORY_MAPPED -> MMapDirectory.open(file.toPath())
        DirectoryType.RAM -> RAMDirectory()
        DirectoryType.FILE_SYSTEM -> FSDirectory.open(file.toPath())
        DirectoryType.UNRECOGNIZED -> throw IllegalStateException("Unknown Directory type")
    }
}

class IndexWriterConfigBuilder(
        private val indexSettings: IndexSettings,
        private val directory: Directory) {
    private val ic = indexSettings.indexConfiguration

    /**
     * Get the default codec associated with the index version
     */
    private fun getCodec(version: IndexVersion, compressionMode: CompressionMode): Codec {
        val compression = when (compressionMode) {

            CompressionMode.OPTIMIZE_FOR_SPEED -> Lucene50StoredFieldsFormat.Mode.BEST_SPEED
            CompressionMode.OPTIMIZE_FOR_SIZE -> Lucene50StoredFieldsFormat.Mode.BEST_COMPRESSION
            CompressionMode.UNRECOGNIZED -> throw IllegalStateException("Unknown Compression Mode")
        }

        return when (version) {
            IndexVersion.DATA_DOZER_1A -> Lucene62Codec(compression)
            else -> throw IllegalStateException("Unknown codec")
        }
    }

    private fun getSimilarity(similarity: Similarity): org.apache.lucene.search.similarities.Similarity {
        return when (similarity) {
            Similarity.BM25 -> ClassicSimilarity()
            Similarity.TFIDF -> BM25Similarity()
            Similarity.UNRECOGNIZED -> throw IllegalStateException("Unknown similarity value")
        }
    }

    fun build(): IndexWriterConfig {
        val iwc = IndexWriterConfig(indexSettings.indexAnalyzer)

        // TODO: Expose info stream and link it to the logger
        //iwc.setInfoStream(PrintStreamInfoStream())

        iwc.similarity = getSimilarity(indexSettings.indexConfiguration.defaultSimilarity)
        iwc.codec = getCodec(ic.indexVersion, ic.compressionMode)
        iwc.commitOnClose =
                if (ic.hasCommitOnClose()) {
                    ic.commitOnClose.value
                } else {
                    true
                }
        iwc.openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND
        iwc.ramBufferSizeMB =
                if (ic.hasRamBufferSizeMb()) {
                    ic.ramBufferSizeMb.value.toDouble()
                } else {
                    32.0
                }
        iwc.maxBufferedDocs =
                if (ic.hasMaxBufferedDocs()) {
                    ic.maxBufferedDocs.value
                } else {
                    -1
                }

        // TODO: Expose CMS settings through the settings object
        // TODO: Expose max merges and threads
        val cms = ConcurrentMergeScheduler()
        cms.enableAutoIOThrottle()
        iwc.mergeScheduler = cms

        val idp = PersistentSnapshotDeletionPolicy(KeepOnlyLastCommitDeletionPolicy(),
                                                   directory,
                                                   IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
        iwc.indexDeletionPolicy = idp
        return iwc
    }
}

