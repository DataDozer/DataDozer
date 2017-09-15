package org.datadozer.services

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.datadozer.*
import org.datadozer.models.Analyzer
import java.io.File
import java.nio.file.Paths
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
 * Analyzer/Analysis related services
 */
class AnalyzerService(
        private val confDir: File,
        testMode: Boolean = false) {

    private data class AnalyzerPair(
            val definition: Analyzer,
            val analyzer: LuceneAnalyzer
    )

    private val store = ConcurrentHashMap<String, AnalyzerPair>()
    private fun addInternalAnalyzer(analyzerName: String, analyzer: LuceneAnalyzer) {
        val analyzerDef = Analyzer.newBuilder().setAnalyzerName(analyzerName).build()
        if (analyzerDef != null) {
            store.put("standard", AnalyzerPair(analyzerDef, analyzer))
        }
    }

    init {
        if (!testMode) {
            if (!confDir.exists()) {
                confDir.mkdir()
            }

            confDir.listFiles({ _, name -> name.endsWith(".bin") }).forEach {
                val analyzer = readFile(it, Analyzer::parseFrom)
                store.put(analyzer.analyzerName, AnalyzerPair(analyzer,
                                                              getCustomAnalyzer(
                                                                      analyzer)))
            }
        }

        // Add all the predefined analyzers
        // Standard analyzer
        addInternalAnalyzer("standard", StandardAnalyzer())
        // Keyword
        addInternalAnalyzer("keyword", getKeywordAnalyzer())
    }

    /**
     * Get an analyzer by name.
     * This returns a null if analyzer is not found.
     */
    fun getAnalyzer(analyzerName: String): LuceneAnalyzer? {
        return store[analyzerName]?.analyzer
    }

    /**
     * Get analyzer information
     */
    fun getAnalyzerInfo(analyzerName: String): Analyzer? {
        return store[analyzerName]?.definition
    }

    /**
     * Get all analyzers
     */
    fun getAllAnalyzers(): Array<Analyzer> {
        return store.values.map {
            it.definition
        }.toTypedArray()
    }

    /**
     * Update a given analyzer
     */
    @Synchronized
    fun updateAnalyzer(analyzer: Analyzer) {
        analyzer.validate()
        val instance = getCustomAnalyzer(analyzer)
        val file = Paths.get(confDir.absolutePath, "${analyzer.analyzerName}.bin").toFile()
        writeFile(file, analyzer)
        store.put(analyzer.analyzerName, AnalyzerPair(analyzer, instance))
    }

    /**
     * Deletes the analyzer from the system
     */
    @Synchronized
    fun deleteAnalyzer(analyzerName: String) {
        if (store.contains(analyzerName)) {
            store.remove(analyzerName)
            val file = Paths.get(confDir.absolutePath, "$analyzerName.bin").toFile()
            file.delete()
        }
    }

    /**
     * Analyzer the given input using the analyzer name
     */
    fun analyze(analyzerName: String, input: String): Array<String> {
        val analyzer = getAnalyzer(analyzerName)
        val tokens = ArrayList<String>()
        if (analyzer != null) {
            parseTextUsingAnalyzer(analyzer, "", input, tokens)
        } else {
            throw OperationException(Message.analyzerNotFound(analyzerName))
        }

        return tokens.toTypedArray()
    }
}