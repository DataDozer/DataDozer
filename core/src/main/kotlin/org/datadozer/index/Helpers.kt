package org.datadozer.index

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.search.SearcherManager
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
 * Wraps Lucene Analyzers in an dictionary to create a per fields analyzer
 */
class AnalyzerWrapper(private val map: ConcurrentHashMap<String, Analyzer>,
                      private val defaultAnalyzer: Analyzer = StandardAnalyzer()) :
        DelegatingAnalyzerWrapper(Analyzer.PER_FIELD_REUSE_STRATEGY) {

    /**
     * Retrieves the wrapped Analyzer appropriate for analyzing the fields with
     * the given name
     *
     * @param fieldName Name of the fields which is to be analyzed
     * @return Analyzer for the fields with the given name.  Assumed to be non-null
     */
    override fun getWrappedAnalyzer(fieldName: String?): Analyzer {
        return map.getOrDefault(fieldName, defaultAnalyzer)
    }
}

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