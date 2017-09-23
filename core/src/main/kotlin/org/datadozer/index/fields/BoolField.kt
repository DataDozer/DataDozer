package org.datadozer.index.fields

import org.apache.lucene.document.IntPoint
import org.apache.lucene.search.Query
import org.apache.lucene.search.SortField
import org.datadozer.models.FieldValue
import org.datadozer.tryParse

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

object BoolField {
    private val info = FieldProperties(
            dataTypeName = "Bool",
            defaultFieldName = null,
            sortFieldType = SortField.Type.INT,
            isNumeric = true,
            defaultStringValue = "0",
            supportsAnalyzer = false,
            storedOnly = false,
            needsExtraStoreField = false,
            autoPopulated = false,
            minimumStringValue = "0",
            maximumStringValue = "1",
            createField = IntField.info.createField,
            createDvField = null,
            toInternalString = fun(value: String): String {
                return if (value.startsWith("t", true)) {
                    "1"
                } else {
                    "0"
                }
            },
            toExternal = fun(value): String {
                return if (value == "0") {
                    "false"
                } else {
                    "true"
                }
            },
            valueCase = FieldValue.ValueCase.INTEGER_VALUE,
            minimum = false,
            maximum = true,
            defaultValue = false,
            addOne = fun(x: Boolean) = x,
            subtractOne = fun(x: Boolean) = x,
            tryParse = fun(x) = tryParse(String::toBoolean, x, false),
            rangeQueryGenerator = null,
            exactQueryGenerator = this::newExactQuery,
            setQueryGenerator = null,
            toInternal = null,
            updateFieldTemplate = this::updateFieldTemplate,
            updateFieldTemplateWithFieldValue = fun(ft, value) {
                this::updateFieldTemplate.invoke(ft, value.boolValue)
            }
    )

    private fun updateFieldTemplate(ft: FieldTemplate, value: Boolean) {
        ft.fields[0].setIntValue(if (value) 1 else 0)
    }

    private fun newExactQuery(fieldName: String, value: Boolean): Query {
        return IntPoint::newExactQuery.invoke(fieldName, if (value) 1 else 0)
    }

    val INSTANCE = FieldType(info)
}
