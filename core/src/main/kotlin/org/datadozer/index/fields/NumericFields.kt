package org.datadozer.index.fields

import org.apache.lucene.document.*
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

/**
 * Integer 32 bit definition
 */
object IntField {
    val info = FieldProperties(
            dataTypeName = "Int",
            defaultFieldName = null,
            sortFieldType = SortField.Type.INT,
            isNumeric = true,
            defaultStringValue = "0",
            supportsAnalyzer = false,
            storedOnly = false,
            needsExtraStoreField = true,
            autoPopulated = false,
            minimumStringValue = Int.MAX_VALUE.toString(),
            maximumStringValue = Int.MAX_VALUE.toString(),
            createField = fun(fieldName) = IntPoint(fieldName, 0),
            createDvField = fun(fieldName) = NumericDocValuesField(fieldName, 0L),
            toInternalString = null,
            toExternal = null,
            valueCase = FieldValue.ValueCase.INTEGER_VALUE,
            minimum = Int.MIN_VALUE,
            maximum = Int.MAX_VALUE,
            defaultValue = 0,
            addOne = fun(x) = x + 1,
            subtractOne = fun(x) = x - 1,
            tryParse = fun(x) = tryParse(String::toInt, x, 0),
            rangeQueryGenerator = IntPoint::newRangeQuery,
            exactQueryGenerator = IntPoint::newExactQuery,
            setQueryGenerator = IntPoint::newSetQuery,
            toInternal = null,
            updateFieldTemplate = IntField::updateFieldTemplate,
            updateFieldTemplateWithFieldValue = fun(ft, value) {
                assert(value.valueCase == FieldValue.ValueCase.INTEGER_VALUE)
                IntField::updateFieldTemplate.invoke(ft, value.integerValue)
            }
    )

    private fun updateFieldTemplate(ft: FieldTemplate, value: Int) {
        ft.fields[0].setIntValue(value)
        if (ft.docValues != null) {
            ft.docValues[0].setLongValue(value.toLong())
        }
    }

    val INSTANCE = FieldType(info)
}

/**
 * Integer 64 bit definition
 */
object LongField {
    val info = FieldProperties(
            dataTypeName = "Long",
            defaultFieldName = null,
            sortFieldType = SortField.Type.LONG,
            isNumeric = true,
            defaultStringValue = "0",
            supportsAnalyzer = false,
            storedOnly = false,
            needsExtraStoreField = true,
            autoPopulated = false,
            minimumStringValue = Long.MAX_VALUE.toString(),
            maximumStringValue = Long.MAX_VALUE.toString(),
            createField = fun(fieldName) = LongPoint(fieldName, 0),
            createDvField = fun(fieldName) = NumericDocValuesField(fieldName, 0L),
            toInternalString = null,
            toExternal = null,
            valueCase = FieldValue.ValueCase.LONG_VALUE,
            minimum = Long.MIN_VALUE,
            maximum = Long.MAX_VALUE,
            defaultValue = 0,
            addOne = fun(x) = x + 1,
            subtractOne = fun(x) = x - 1,
            tryParse = fun(x) = tryParse(String::toLong, x, 0),
            rangeQueryGenerator = LongPoint::newRangeQuery,
            exactQueryGenerator = LongPoint::newExactQuery,
            setQueryGenerator = LongPoint::newSetQuery,
            toInternal = null,
            updateFieldTemplate = LongField::updateFieldTemplate,
            updateFieldTemplateWithFieldValue = fun(ft, value) {
                assert(value.valueCase == FieldValue.ValueCase.LONG_VALUE)
                LongField::updateFieldTemplate.invoke(ft, value.longValue)
            }
    )

    private fun updateFieldTemplate(ft: FieldTemplate, value: Long) {
        ft.fields[0].setLongValue(value)
        if (ft.docValues != null) {
            ft.docValues[0].setLongValue(value)
        }
    }

    val INSTANCE = FieldType(info)
}

/**
 * Float 32 bit definition
 */
object FloatField {
    private val info = FieldProperties(
            dataTypeName = "Float",
            defaultFieldName = null,
            sortFieldType = SortField.Type.FLOAT,
            isNumeric = true,
            defaultStringValue = "0.0",
            supportsAnalyzer = false,
            storedOnly = false,
            needsExtraStoreField = true,
            autoPopulated = false,
            minimumStringValue = Float.MAX_VALUE.toString(),
            maximumStringValue = Float.MAX_VALUE.toString(),
            createField = fun(fieldName) = FloatPoint(fieldName, 0.0f),
            createDvField = fun(fieldName) = FloatDocValuesField(fieldName, 0.0f),
            toInternalString = null,
            toExternal = null,
            valueCase = FieldValue.ValueCase.FLOAT_VALUE,
            minimum = Float.MIN_VALUE,
            maximum = Float.MAX_VALUE,
            defaultValue = 0.0f,
            addOne = fun(x) = x + 1,
            subtractOne = fun(x) = x - 1,
            tryParse = fun(x) = tryParse(String::toFloat, x, 0.0f),
            rangeQueryGenerator = FloatPoint::newRangeQuery,
            exactQueryGenerator = FloatPoint::newExactQuery,
            setQueryGenerator = FloatPoint::newSetQuery,
            toInternal = null,
            updateFieldTemplate = FloatField::updateFieldTemplate,
            updateFieldTemplateWithFieldValue = fun(ft, value) {
                FloatField::updateFieldTemplate.invoke(ft, value.floatValue)
            }
    )

    private fun updateFieldTemplate(ft: FieldTemplate, value: Float) {
        ft.fields[0].setFloatValue(value)
        if (ft.docValues != null) {
            ft.docValues[0].setFloatValue(value)
        }
    }

    val INSTANCE = FieldType(info)
}

/**
 * Double 64 bit definition
 */
object DoubleField {
    private val info = FieldProperties(
            dataTypeName = "Long",
            defaultFieldName = null,
            sortFieldType = SortField.Type.DOUBLE,
            isNumeric = true,
            defaultStringValue = "0.0",
            supportsAnalyzer = false,
            storedOnly = false,
            needsExtraStoreField = true,
            autoPopulated = false,
            minimumStringValue = Double.MAX_VALUE.toString(),
            maximumStringValue = Double.MAX_VALUE.toString(),
            createField = fun(fieldName) = DoublePoint(fieldName, 0.0),
            createDvField = fun(fieldName) = DoubleDocValuesField(fieldName, 0.0),
            toInternalString = null,
            toExternal = null,
            valueCase = FieldValue.ValueCase.LONG_VALUE,
            minimum = Double.MIN_VALUE,
            maximum = Double.MAX_VALUE,
            defaultValue = 0.0,
            addOne = fun(x) = x + 1,
            subtractOne = fun(x) = x - 1,
            tryParse = fun(x) = tryParse(String::toDouble, x, 0.0),
            rangeQueryGenerator = DoublePoint::newRangeQuery,
            exactQueryGenerator = DoublePoint::newExactQuery,
            setQueryGenerator = DoublePoint::newSetQuery,
            toInternal = null,
            updateFieldTemplate = DoubleField::updateFieldTemplate,
            updateFieldTemplateWithFieldValue = fun(ft, value) {
                DoubleField::updateFieldTemplate.invoke(ft, value.doubleValue)
            }
    )

    private fun updateFieldTemplate(ft: FieldTemplate, value: Double) {
        ft.fields[0].setDoubleValue(value)
        if (ft.docValues != null) {
            ft.docValues[0].setDoubleValue(value)
        }
    }

    val INSTANCE = FieldType(info)
}
