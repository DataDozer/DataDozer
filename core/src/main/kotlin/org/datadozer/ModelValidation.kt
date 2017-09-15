package org.datadozer

import org.datadozer.models.*

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

fun Tokenizer.validate() {
    isPropertyName("tokenizerName", this.tokenizerName)
    check(checkTokenizerExists(this.tokenizerName), {
        OperationMessage.newBuilder()
                .setMessage("Tokenizer: '${this.tokenizerName}' not found.")
                .setDetails("tokenizer_name='${this.tokenizerName}'")
                .build()
    })
}

fun Filter.validate() {
    isPropertyName("filterName", this.filterName)
    check(checkFilterExists(this.filterName), {
        OperationMessage.newBuilder()
                .setMessage("Filter: '${this.filterName}' not found.")
                .setDetails("filter_name='${this.filterName}'")
                .build()
    })
}

fun Analyzer.validate() {
    isPropertyName("analyzerName", this.analyzerName)
    check(this.hasTokenizer(), { Message.fieldIsMandatory("Tokenizer") })
    this.tokenizer.validate()
    this.filtersList.forEach {
        it.validate()
    }
}

fun Field.validate() {
    isPropertyName("fieldName", this.fieldName)
}