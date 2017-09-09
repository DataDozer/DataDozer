package org.datadozer.index

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.codecs.Codec
import org.apache.lucene.codecs.lucene62.Lucene62Codec
import org.apache.lucene.search.SearcherManager
import org.datadozer.models.IndexConfiguration
import org.datadozer.models.IndexVersion
import org.datadozer.models.ShardConfiguration
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
 * Wraps Lucene Analyzers in an dictionary to create a per field analyzer
 */
class AnalyzerWrapper(private val defaultAnalyzer: Analyzer = StandardAnalyzer()) :
        DelegatingAnalyzerWrapper(Analyzer.PER_FIELD_REUSE_STRATEGY) {

    private val map = ConcurrentHashMap<String, Analyzer>()

    /**
     * Creates per field analyzer for an index from the index field data. These analyzers
     * are used for searching and indexing rather than the global field analyzer.
     */
    fun buildAnalyzer(fields: List<FieldSchema>, isIndexAnalyzer: Boolean) {
        fields.filter { it.analyzers != null }.forEach {
            val analyzer = if (isIndexAnalyzer) {
                it.analyzers!!.indexAnalyzer
            } else {
                it.analyzers!!.searchAnalyzer
            }
            map[it.schemaName] = analyzer
        }
    }

    /**
     * Retrieves the wrapped Analyzer appropriate for analyzing the field with
     * the given name
     *
     * @param fieldName Name of the field which is to be analyzed
     * @return Analyzer for the field with the given name.  Assumed to be non-null
     */
    override fun getWrappedAnalyzer(fieldName: String?): Analyzer {
        return map.getOrDefault(fieldName, defaultAnalyzer)
    }
}

/**
 * Get the default codec associated with the index version
 */
fun getCodec(version: IndexVersion): Codec {
    return when (version) {
        IndexVersion.DATA_DOZER_1A -> Lucene62Codec()
        else ->
            // TODO(Convert to a proper message)
            throw Exception("sdsd")
    }
}

/**
 * General index settings
 */
data class IndexSettings(
        val indexName: String,
        val indexId: Int,
        val indexAnalyzer: AnalyzerWrapper,
        val searchAnalyzer: AnalyzerWrapper,
        val fields: Array<FieldSchema>,
        val indexConfiguration: IndexConfiguration,
        val baseDir: String,
        val settingsFolder: String,
        val shardConfiguration: ShardConfiguration
)

/**
 * Wrapper around SearcherManager to expose auto closable functionality
 */
class RealTimeSearcher(private val searcherManager: SearcherManager) : AutoCloseable {
    val indexSearcher = searcherManager.acquire()

    /**
     * IndexReader provides an interface for accessing a point-in-time view of
     * an index. Any changes made to the index via IndexWriter
     * will not be visible until a new IndexReader is opened.
     */
    val indexReader = indexSearcher.indexReader

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * `try`-with-resources statement.
     */
    override fun close() {
        searcherManager.release(indexSearcher)
    }
}