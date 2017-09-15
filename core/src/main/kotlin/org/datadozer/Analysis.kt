package org.datadozer

import org.apache.lucene.analysis.custom.CustomAnalyzer
import org.apache.lucene.analysis.util.TokenFilterFactory
import org.apache.lucene.analysis.util.TokenizerFactory
import org.datadozer.models.Analyzer
import java.io.StringReader

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
 * Utility function to get tokens from the search string based upon the passed analyzer
 * This will enable us to avoid using the Lucene query parser
 * We cannot use simple white space based token generation as it really depends
 * upon the analyzer used
 */
fun parseTextUsingAnalyzer(analyzer: LuceneAnalyzer, fieldName: String, queryText: String, tokens: ArrayList<String>) {
    val source = analyzer.tokenStream(fieldName, StringReader(queryText))
    // Get the CharTermAttribute from the TokenStream
    val termAtt = source.addAttribute(org.apache.lucene.analysis.tokenattributes.CharTermAttribute::class.java)
    source.use { source ->
        source.reset()
        while (source.incrementToken()) {
            tokens.add(termAtt.toString())
        }
        source.end()
    }
}

/**
 * Creates a Lucene analyzer from the DataDozer analyzer definition
 */
fun getCustomAnalyzer(analyzer: Analyzer): LuceneAnalyzer {
    val builder = CustomAnalyzer.builder()
    builder.withTokenizer(analyzer.tokenizer.tokenizerName, analyzer.tokenizer.parametersMap)
    for (filter in analyzer.filtersList) {
        builder.addTokenFilter(filter.filterName, filter.parametersMap)
    }
    return builder.build()
}

/**
 * Returns keyword analyzer
 */
fun getKeywordAnalyzer(): LuceneAnalyzer {
    return CustomAnalyzer.builder()
            .withTokenizer("keyword")
            .addTokenFilter("lowercase")
            .build()
}

/**
 * Checks if a given filter is present in the system and returns true if
 * it is found.
 */
fun checkFilterExists(filterName: String): Boolean {
    val filters = TokenFilterFactory::availableTokenFilters.invoke()
    return filters.contains(filterName)
}

/**
 * Checks if a given tokenizer is present in the system and returns true if
 * it is found.
 */
fun checkTokenizerExists(filterName: String): Boolean {
    val tokenizers = TokenizerFactory::availableTokenizers.invoke()
    return tokenizers.contains(filterName)
}