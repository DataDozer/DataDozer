package org.datadozer.index.fields

import org.apache.lucene.document.Field
import org.apache.lucene.document.SortedDocValuesField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.SortField
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TermRangeQuery
import org.apache.lucene.util.BytesRef
import org.datadozer.models.FieldValue

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

object KeywordField {
    /**
     * Parse a string. Matches the signature of other parse methods
     */
    fun stringTryParse(value: String): Pair<Boolean, String> {
        return if (value.isBlank()) {
            Pair(false, "null")
        } else {
            Pair(true, value)
        }
    }

    private fun termRangeQuery(schemaName: String, lowerRange: String, upperRange: String): TermRangeQuery {
        return TermRangeQuery(schemaName, BytesRef(lowerRange), BytesRef(upperRange), true, true)
    }

    val info = FieldProperties(
            dataTypeName = "Keyword",
            defaultFieldName = null,
            sortFieldType = SortField.Type.STRING,
            isNumeric = false,
            defaultStringValue = "null",
            supportsAnalyzer = false,
            storedOnly = false,
            needsExtraStoreField = false,
            autoPopulated = false,
            minimumStringValue = "",
            maximumStringValue = "",
            createField = fun(fieldName) = StringField(fieldName, "null", Field.Store.YES),
            createDvField = fun(fieldName) = SortedDocValuesField(fieldName, BytesRef()),
            toInternalString = String::toLowerCase,
            toExternal = null,
            valueCase = FieldValue.ValueCase.STRING_VALUE,
            minimum = "",
            maximum = "",
            defaultValue = "null",
            addOne = fun(x) = x,
            subtractOne = fun(x) = x,
            tryParse = KeywordField::stringTryParse,
            rangeQueryGenerator = KeywordField::termRangeQuery,
            exactQueryGenerator = fun(schemaName, value: String) = TermQuery(Term(schemaName, value)),
            setQueryGenerator = KeywordField::setQuery,
            toInternal = String::toLowerCase,
            updateFieldTemplate = KeywordField::updateFieldTemplate,
            updateFieldTemplateWithFieldValue = fun(ft, value) {
                KeywordField::updateFieldTemplate.invoke(ft, value.stringValue)
            }
    )

    private fun updateFieldTemplate(ft: FieldTemplate, value: String) {
        ft.fields[0].setStringValue(value)
        if (ft.docValues != null) {
            ft.docValues[0].setBytesValue(value.toByteArray())
        }
    }

    private fun setQuery(field: String, values: List<String>): Query {
        throw Exception("TODO")
    }

    val INSTANCE = FieldType(info)
}

object TextField {
    private val info = KeywordField.info.copy(
            dataTypeName = "text",
            supportsAnalyzer = true,
            createField = fun(fieldName) = TextField(fieldName, "null", Field.Store.YES),
            createDvField = null,
            toInternal = null
    )

    val INSTANCE = FieldType(info)
}