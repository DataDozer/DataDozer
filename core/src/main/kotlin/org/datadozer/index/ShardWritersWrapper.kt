package org.datadozer.index

import org.apache.lucene.codecs.bloom.MurmurHash2
import org.apache.lucene.index.Term
import org.apache.lucene.util.BytesRef
import org.datadozer.Message
import org.datadozer.OperationException
import org.datadozer.SingleInstancePerThreadObjectPool
import org.datadozer.index.fields.IdField
import org.datadozer.models.*
import org.datadozer.services.DeleteDocumentRequest
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicLong

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
 * Maps the document id to a shard number using murmur hash 2 in case
 * of string and bytes fields. For integer fields it uses a simple mod.
 */
fun mapToShard(id: FieldValue, shardCount: Int, idValueCase: FieldValue.ValueCase): Int {
    if (shardCount == 1) {
        return 0
    }
    if (id.valueCase == idValueCase) {
        when (id.valueCase) {
            FieldValue.ValueCase.STRING_VALUE ->
                return Math.floorMod(MurmurHash2.hash32(id.stringValueBytes.toByteArray(), 0,
                                                        id.stringValue.length),
                                     shardCount)
            FieldValue.ValueCase.INTEGER_VALUE ->
                return Math.floorMod(id.integerValue, shardCount)
            FieldValue.ValueCase.LONG_VALUE -> return Math.floorMod(id.longValue,
                                                                    shardCount.toLong()).toInt()
            FieldValue.ValueCase.BYTES_VALUE ->
                return Math.floorMod(MurmurHash2.hash32(id.bytesValue.toByteArray(), 0,
                                                        id.bytesValue.size()),
                                     shardCount)
            FieldValue.ValueCase.VALUE_NOT_SET -> throw OperationException(Message.fieldIsMandatory("id"))
            else -> throw OperationException(Message.fieldIsMandatory("id"))
        }
    }

    throw OperationException(Message.fieldIsMandatory("id"))
}

class ShardWritersWrapper private constructor(
        val builder: SingleInstancePerThreadObjectPool<DocumentBuilder>,
        val indexId: Int,
        val caches: Array<VersionCache>?,
        val shardWriters: Array<ShardWriter>,
        val settings: IndexSettings,
        val txWriterPool: SingleInstancePerThreadObjectPool<TransactionWriter>,
        val modifyIndex: AtomicLong,
        private var status: IndexStatus) {

    object Builder {
        fun build(settings: IndexSettings,
                  txWriterPool: SingleInstancePerThreadObjectPool<TransactionWriter>,
                  indexId: Int): ShardWritersWrapper {

            /**
             * Create a shard writer with the provided shard number
             */
            fun createShard(shardNo: Int): ShardWriter {
                val path = Paths.get(settings.baseDir.absolutePath, "shards", shardNo.toString(), "index")
                val directory = getDirectoryType(settings.indexConfiguration.directoryType, path.toFile())
                val indexWriterConfig = IndexWriterConfigBuilder(settings, directory).build()
                return ShardWriter(shardNo, settings.indexName, settings.indexConfiguration,
                                   indexWriterConfig,
                                   directory)
            }

            val shardWriters = Array(settings.shardConfiguration.shardCount, {
                createShard(it)
            })

            val modifyIndex = 1L

            val versionCaches =
                    if (settings.indexConfiguration.hasSupportOptimisticConcurrency()
                            && settings.indexConfiguration.supportOptimisticConcurrency.value) {
                        shardWriters.map {
                            VersionCache(it)
                        }.toTypedArray()
                    } else {
                        null
                    }

            val indexWriter = ShardWritersWrapper(
                    builder = SingleInstancePerThreadObjectPool { DocumentBuilder.build(settings) },
                    shardWriters = shardWriters,
                    settings = settings,
                    txWriterPool = txWriterPool,
                    modifyIndex = AtomicLong(modifyIndex),
                    status = IndexStatus.OPENING,
                    caches = versionCaches,
                    indexId = indexId
            )

            // TODO : Replay transaction log here
            return indexWriter
        }
    }

    private val idValueCase =
            when (settings.indexConfiguration.idKeyDatatype) {
                IdKeyDataType.STRING -> FieldValue.ValueCase.STRING_VALUE
                IdKeyDataType.INTEGER -> FieldValue.ValueCase.INTEGER_VALUE
                IdKeyDataType.LONG -> FieldValue.ValueCase.LONG_VALUE
                IdKeyDataType.BYTES -> FieldValue.ValueCase.BYTES_VALUE
                IdKeyDataType.UNRECOGNIZED -> TODO()
                else -> TODO()
            }

    fun refresh() {
        shardWriters.forEach { it.refresh() }
    }

    fun commit(forceCommit: Boolean) {
        shardWriters.forEach { it.commit(forceCommit) }
    }

    /**
     * Add or update a document
     */
    fun addOrUpdateDocuments(document: Document, create: Boolean, addToTxLog: Boolean) {
        assert(document.hasId())
        val shardNo = mapToShard(document.id, settings.shardCount, idValueCase)
        val modifyIndex = modifyIndex.incrementAndGet()
        val documentId = document.id.toByteArray()
        if (settings.optimisticConcurrency) {
            val existingVersion = caches!![shardNo].versionCheck(documentId, document.modifyIndex,
                                                                 modifyIndex)
            caches[shardNo].addOrUpdate(documentId, modifyIndex, existingVersion)
        }

        if (addToTxLog) {
            val entryType = if (create) {
                TransactionLogEntryType.DOC_CREATE
            } else {
                TransactionLogEntryType.DOC_UPDATE
            }

            txWriterPool.borrow {
                it.appendEntry(
                        transactionLogEntryFactory(indexId, modifyIndex, entryType, document), indexId)

            }
        }

        builder.borrow {
            val doc = it.updateDocument(document)
            if (create) {
                shardWriters[shardNo].addDocument(doc)
            } else {
                shardWriters[shardNo].updateDocument(Term(IdField.INSTANCE.defaultFieldName, BytesRef(documentId)), doc)
            }
        }
    }

    fun addDocument(document: Document) {
        addOrUpdateDocuments(document, true, true)
    }

    fun updateDocument(document: Document) {
        addOrUpdateDocuments(document, false, true)
    }

    fun deleteDocument(request: DeleteDocumentRequest) {
        val shardNo = mapToShard(request.id, settings.shardCount, idValueCase)
        if (caches != null) {
            caches[shardNo].delete(request.id.toByteArray(), -1L)
        }
        val modifyIndex = modifyIndex.incrementAndGet()
        txWriterPool.borrow {
            it.appendEntry(
                    transactionLogEntryFactory(indexId, modifyIndex, TransactionLogEntryType.DOC_DELETE,
                                               request), indexId)
        }
        shardWriters[shardNo].deleteDocuments(Term(IdField.SCHEMA.schemaName, BytesRef(request.id.toByteArray())))
    }
}