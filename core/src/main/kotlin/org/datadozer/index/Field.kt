package org.datadozer.index

import org.apache.lucene.document.*
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.SortField
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TermRangeQuery
import org.apache.lucene.util.BytesRef
import org.datadozer.Message
import org.datadozer.OperationException
import org.datadozer.models.Document
import org.datadozer.models.FieldValue
import org.datadozer.models.Similarity
import org.datadozer.parseTextUsingAnalyzer
import org.datadozer.tryParse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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
typealias LuceneAnalyzer = org.apache.lucene.analysis.Analyzer
typealias LuceneDocument = org.apache.lucene.document.Document
typealias LuceneField = org.apache.lucene.document.Field
typealias LuceneFieldType = org.apache.lucene.document.FieldType
typealias SchemaName = String

val stringDefaultValue = "null"

/**
 * Represents the analyzers associated with a field. By creating this abstraction
 * we can easily create a cache able copy of it which can be shared across field types
 */
data class FieldAnalyzers(
        val searchAnalyzer: LuceneAnalyzer,
        val indexAnalyzer: LuceneAnalyzer)

data class FieldProperties<T>(
        val dataTypeName: String,
        val defaultFieldName: String?,
        val sortFieldType: SortField.Type,
        val defaultStringValue: String,
        val needsExtraStoreField: Boolean,
        val isNumeric: Boolean,
        val supportsAnalyzer: Boolean,
        val storedOnly: Boolean,
        val minimumStringValue: String,
        val maximumStringValue: String,
        val autoPopulated: Boolean,
        val createField: (String) -> LuceneField,
        val createDvField: ((String) -> LuceneField)?,
        val toInternalString: ((String) -> String)?,

        /**
         * Generate any type specific formatting that is needed before sending
         * the data out as part of search result. This is useful in case of enums
         * and boolean fields which have a different internal representation.
         */
        val toExternal: ((String) -> String)?,
        val valueCase: FieldValue.ValueCase,

        // Type related properties start here
        val minimum: T,
        val maximum: T,
        val defaultValue: T,
        val addOne: (T) -> T,
        val subtractOne: (T) -> T,
        val rangeQueryGenerator: ((SchemaName, T, T) -> Query)?,
        val exactQueryGenerator: ((SchemaName, T) -> Query)?,
        val setQueryGenerator: ((SchemaName, List<T>) -> Query)?,
        val tryParse: (String) -> Pair<Boolean, T>,

        /**
         * Generates the internal representation of the field. This is mostly
         * useful when searching if the field does not have an associated analyzer.
         */
        val toInternal: ((T) -> T)?,

        /**
         * Update a field template with the given value. Call to this
         * method should be chained from Validate
         */
        val updateFieldTemplate: (FieldTemplate, T) -> Unit,
        val updateFieldTemplateWithFieldValue: (FieldTemplate, FieldValue) -> Unit
)

/**
 * Represents the minimum unit to represent a field in DataDozer Document. The reason
 * to use array is to support fields which can maps to multiple internal fields.
 * Note: We will create a new instance of FieldTemplate per field in an index. So, it
 * should not occupy a lot of memory
 */
data class FieldTemplate(
        val fields: Array<LuceneField>,
        val docValues: Array<LuceneField>?) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FieldTemplate

        if (!Arrays.equals(fields, other.fields)) return false
        if (!Arrays.equals(docValues, other.docValues)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(fields)
        result = 31 * result + (docValues?.let { Arrays.hashCode(it) } ?: 0)
        return result
    }
}

/**
 * Represents a field in an Index
 */
data class FieldSchema(
        val schemaName: String,
        val fieldName: String,
        val fieldOrder: Int,
        val docValues: Boolean,
        val analyzers: FieldAnalyzers?,
        val similarity: Similarity,
        val fieldType: IndexField
)

/**
 * FieldBase containing all the Field related properties which are not
 * dependent upon the type information
 */
abstract class IndexField {
    abstract fun toInternalString(fieldValue: String): String

    /**
     * Create a new Field template for the given field.
     */
    abstract fun createFieldTemplate(fs: FieldSchema): FieldTemplate

    /**
     * Get tokens for a given input. This is not supported by all field types for example
     * it does not make any sense to tokenize numeric types and exact text fields. In these
     * cases the internal representation of the field type is used.
     * Note: An instance of List is passed so that we can avoid memory allocation and
     * reuse the list from the object pool.
     * Note: Avoid using the below for numeric types.
     */
    abstract fun getTokens(fieldValue: String, tokens: ArrayList<String>, fs: FieldSchema)

    /**
     * Update a field template from the given FlexDocument. This is a higher level method which
     * bring together a number of lower level method from FieldBase
     */
    abstract fun updateDocument(document: Document, fieldSchema: FieldSchema, fieldTemplate: FieldTemplate)

    /**
     * Returns a query which provides the exact text match for the given field type.
     */
    abstract fun exactQuery(schemaName: SchemaName, fieldValue: String): Query

    /**
     * Returns a query which provides the range matching over the given lower and
     * upper range.
     */
    abstract fun rangeQuery(schemaName: SchemaName, lowerRange: String, upperRange: String, inclusiveMinimum: Boolean,
                            inclusiveMaximum: Boolean): Query

    /**
     * Returns a query which matches any of the terms in the given values array.
     */
    abstract fun setQuery(schemaName: SchemaName, values: Array<String>): Query
}

/**
 * Information needed to represent a field in DataDozer document
 * This should only contain information which is fixed for a given type so that the
 * instance could be cached. Any Index specific information should go to FieldSchema.
 */
class FieldType<T>(val properties: FieldProperties<T>) : IndexField() {

    private val p = properties

    /**
     * Validates the provided string input to see if it matches the correct format for
     * field type. In case it can't validate the input then it returns a tuple with
     * false and the default value of the field. Then it is up to the caller to decide
     * whether to thrown an error or use the default value for the field.
     */
    private fun validate(value: String): Pair<Boolean, T> {
        if (value.isBlank()) {
            return Pair(false, p.defaultValue)
        }

        if (value.equals(p.defaultStringValue, true)) {
            return Pair(true, p.defaultValue)
        }

        if (p.isNumeric) {
            if (value == p.maximumStringValue) {
                return Pair(true, p.maximum)
            }
            if (value == p.minimumStringValue) {
                return Pair(true, p.minimum)
            }
        }

        val (result, v) = p.tryParse(value)
        if (result) {
            return Pair(true, v)
        }
        return Pair(false, p.defaultValue)
    }

    /**
     * Convert the value to the internal representation
     */
    @Suppress("unused")
    fun toInternalRepresentation(value: T): T {
        if (p.toInternal != null) {
            return p.toInternal.invoke(value)
        }
        return value
    }

    private fun validateAndThrow(schemaName: String, value: String): T {
        val (result, v) = validate(value)
        if (result) {
            return v
        }

        throw OperationException(Message.dataCannotBeParsed(schemaName, p.dataTypeName, value))
    }

    override fun toInternalString(fieldValue: String): String {
        if (properties.toInternalString != null) {
            return properties.toInternalString.invoke(fieldValue)
        }
        return fieldValue
    }

    /**
     * Create a new Field template for the given field.
     */
    override fun createFieldTemplate(fs: FieldSchema): FieldTemplate {
        val fields = when (p.needsExtraStoreField) {
            true -> arrayOf(properties.createField(fs.schemaName),
                            StoredField(fs.schemaName, stringDefaultValue))
            false -> arrayOf(properties.createField(fs.schemaName))
        }

        val docValues =
                if (fs.docValues && properties.createDvField != null) {
                    arrayOf(properties.createDvField.invoke(fs.schemaName))
                } else null

        return FieldTemplate(fields, docValues)
    }

    /**
     * Update a field template from the given FlexDocument. This is a higher level method which
     * bring together a number of lower level method from FieldBase
     */
    override fun updateDocument(document: Document, fieldSchema: FieldSchema, fieldTemplate: FieldTemplate) {
        if (document.fieldsCount < fieldSchema.fieldOrder) {
            val value = document.getFields(fieldSchema.fieldOrder)
            if (value.valueCase == properties.valueCase) {
                p.updateFieldTemplateWithFieldValue(fieldTemplate, value)
            }
        } else {
            p.updateFieldTemplate(fieldTemplate, p.defaultValue)
        }
    }

    /**
     * Get tokens for a given input. This is not supported by all field types for example
     * it does not make any sense to tokenize numeric types and exact text fields. In these
     * cases the internal representation of the field type is used.
     * Note: An instance of List is passed so that we can avoid memory allocation and
     * reuse the list from the object pool.
     * Note: Avoid using the below for numeric types.
     */
    override fun getTokens(fieldValue: String, tokens: ArrayList<String>, fs: FieldSchema) {
        when (fs.analyzers) {
            null ->
                // The field does not have an associated analyzer so just add the input to
                // the result by using the field specific formatting
                tokens.add(toInternalString(fieldValue))
            else -> parseTextUsingAnalyzer(fs.analyzers.searchAnalyzer, fs.schemaName, fieldValue, tokens)
        }
    }

    /**
     * Returns a query which provides the exact text match for the given field type.
     */
    override fun exactQuery(schemaName: SchemaName, fieldValue: String): Query {
        if (p.exactQueryGenerator == null) {
            throw OperationException(Message.exactQueryNotSupported(schemaName, p.dataTypeName))
        }
        val value = validateAndThrow(schemaName, fieldValue)
        return p.exactQueryGenerator.invoke(schemaName, value)
    }

    /**
     * Returns a query which provides the range matching over the given lower and
     * upper range.
     */
    override fun rangeQuery(schemaName: SchemaName, lowerRange: String, upperRange: String, inclusiveMinimum: Boolean,
                            inclusiveMaximum: Boolean): Query {
        if (p.rangeQueryGenerator == null) {
            throw OperationException(Message.rangeQueryNotSupported(schemaName, p.dataTypeName))
        }

        val lower = validateAndThrow(schemaName, lowerRange)
        val lr = if (inclusiveMaximum) {
            lower
        } else {
            p.addOne(lower)
        }

        val upper = validateAndThrow(schemaName, upperRange)
        val ur = if (inclusiveMaximum) {
            upper
        } else {
            p.subtractOne(upper)
        }
        return p.rangeQueryGenerator.invoke(schemaName, lr, ur)
    }

    /**
     * Returns a query which matches any of the terms in the given values array.
     */
    override fun setQuery(schemaName: SchemaName, values: Array<String>): Query {
        if (p.setQueryGenerator == null) {
            throw OperationException(Message.setQueryNotSupported(schemaName, p.dataTypeName))
        }
        return p.setQueryGenerator.invoke(schemaName, values.map { validateAndThrow(schemaName, it) })
    }
}

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
            valueCase = FieldValue.ValueCase.FLOATS_VALUE,
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

object DateTimeField {
    val info = LongField.info.copy(
            dataTypeName = "DateTime",
            defaultStringValue = "10101000000", // Equivalent to 00:00:00.0000000, January 1, 0001, in the Gregorian calendar
            minimumStringValue = "10101000000",
            maximumStringValue = "99991231235959",
            minimum = 10101000000L,
            maximum = 99991231235959L,
            defaultValue = 10101000000L
    )

    val INSTANCE = FieldType(info)
}

object DateField {
    private val info = LongField.info.copy(
            dataTypeName = "Date",
            defaultStringValue = "10101",
            minimumStringValue = "10101",
            maximumStringValue = "99991231",
            minimum = 10101L,
            maximum = 99991231235959L,
            defaultValue = 99991231L
    )

    val INSTANCE = FieldType(info)
}

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

object StoredField {
    private val info = FieldProperties(
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
            createField = fun(fieldName) = org.apache.lucene.document.StoredField(fieldName, ""),
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

/**
 * Used for representing the id of an index
 */
object IdField {
    private val info = KeywordField.info.copy(
            defaultFieldName = "__id"
    )

    val INSTANCE = FieldType(info)
}

/**
 * This field is used to add causal ordering to the events in
 * the index. A document with lower modify index was created/updated before
 * a document with the higher index.
 * This is also used for concurrency updates.
 */
object ModifyIndex {
    private val info = LongField.info.copy(
            defaultFieldName = "__modifyindex"
    )

    val INSTANCE = FieldType(info)
}

/**
 * Field to be used for time stamp. This field is used by index to capture the modification
 * time.
 * It only supports a fixed yyyyMMddHHmmss format.
 */
object TimeStampField {
    private val info = DateTimeField.info.copy(
            defaultFieldName = "__timestamp",
            autoPopulated = true,
            toInternal = fun(_) = LocalDateTime.now().format(dateFormat).toLong()
    )

    private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    val INSTANCE = FieldType(info)
}