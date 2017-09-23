package org.datadozer.index.fields

import org.apache.lucene.document.StoredField
import org.apache.lucene.search.SortField
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

object StoredField {
    val info = FieldProperties(
            dataTypeName = "Stored",
            defaultFieldName = null,
            sortFieldType = SortField.Type.SCORE,
            isNumeric = false,
            defaultStringValue = "",
            supportsAnalyzer = false,
            storedOnly = true,
            needsExtraStoreField = false,
            autoPopulated = false,
            minimumStringValue = "",
            maximumStringValue = "",
            createField = fun(fieldName) = StoredField(fieldName, ""),
            createDvField = null,
            toInternalString = null,
            toExternal = null,
            valueCase = FieldValue.ValueCase.STRING_VALUE,
            minimum = "",
            maximum = "",
            defaultValue = "",
            addOne = fun(x) = x,
            subtractOne = fun(x) = x,
            tryParse = KeywordField::stringTryParse,
            rangeQueryGenerator = null,
            exactQueryGenerator = null,
            setQueryGenerator = null,
            toInternal = null,
            updateFieldTemplate = this::updateFieldTemplate,
            updateFieldTemplateWithFieldValue = fun(ft, value) {
                this::updateFieldTemplate.invoke(ft, value.stringValue)
            }
    )

    private fun updateFieldTemplate(ft: FieldTemplate, value: String) {
        ft.fields[0].setStringValue(value)
    }

    val INSTANCE = FieldType(info)
}
