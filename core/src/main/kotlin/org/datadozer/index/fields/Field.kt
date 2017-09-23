package org.datadozer.index.fields

import org.apache.lucene.document.StoredField
import org.apache.lucene.search.Query
import org.apache.lucene.search.SortField
import org.datadozer.*
import org.datadozer.models.FieldValue
import org.datadozer.models.Similarity
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

val stringDefaultValue = "null"

/**
 * Represents the analyzers associated with a fields. By creating this abstraction
 * we can easily create a cache able copy of it which can be shared across fields types
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
        inline val addOne: (T) -> T,
        inline val subtractOne: (T) -> T,
        inline val rangeQueryGenerator: ((SchemaName, T, T) -> Query)?,
        inline val exactQueryGenerator: ((SchemaName, T) -> Query)?,
        inline val setQueryGenerator: ((SchemaName, List<T>) -> Query)?,
        inline val tryParse: (String) -> Pair<Boolean, T>,

        /**
         * Generates the internal representation of the fields. This is mostly
         * useful when searching if the fields does not have an associated analyzer.
         */
        inline val toInternal: ((T) -> T)?,

        /**
         * Update a fields builder with the given value. Call to this
         * method should be chained from Validate
         */
        inline val updateFieldTemplate: (FieldTemplate, T) -> Unit,
        inline val updateFieldTemplateWithFieldValue: (FieldTemplate, FieldValue) -> Unit
)

/**
 * Represents the minimum unit to represent a fields in DataDozer Document. The reason
 * to use array is to support fields which can maps to multiple internal fields.
 * Note: We will create a new instance of FieldTemplate per fields in an index. So, it
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
 * Represents a fields in an Index
 */
data class FieldSchema(
        val schemaName: String,
        val fieldName: String,
        val fieldOrder: Int,
        val docValues: Boolean,
        val analyzers: FieldAnalyzers?,
        val similarity: Similarity,
        val fieldType: IndexField,
        val multiValued: Boolean
)

/**
 * FieldBase containing all the Field related properties which are not
 * dependent upon the type information
 */
abstract class IndexField {
    abstract val dataTypeName: String
    abstract val defaultFieldName: String?
    abstract val sortFieldType: SortField.Type
    abstract val defaultStringValue: String
    abstract val needsExtraStoreField: Boolean
    abstract val isNumeric: Boolean
    abstract val supportsAnalyzer: Boolean
    abstract val storedOnly: Boolean
    abstract val minimumStringValue: String
    abstract val maximumStringValue: String
    abstract val autoPopulated: Boolean

    abstract fun toInternalString(fieldValue: String): String

    /**
     * Create a new Field builder for the given fields.
     */
    abstract fun createFieldTemplate(fs: FieldSchema): FieldTemplate

    /**
     * Get tokens for a given input. This is not supported by all fields types for example
     * it does not make any sense to tokenize numeric types and exact text fields. In these
     * cases the internal representation of the fields type is used.
     * Note: An instance of List is passed so that we can avoid memory allocation and
     * reuse the list from the object pool.
     * Note: Avoid using the below for numeric types.
     */
    abstract fun getTokens(fieldValue: String, tokens: ArrayList<String>, fs: FieldSchema)

    /**
     * Update a fields builder from the given FlexDocument. This is a higher level method which
     * bring together a number of lower level method from FieldBase
     */
    abstract fun updateDocument(value: FieldValue, fieldSchema: FieldSchema, fieldTemplate: FieldTemplate)

    /**
     * Returns a query which provides the exact text match for the given fields type.
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
 * Information needed to represent a fields in DataDozer document
 * This should only contain information which is fixed for a given type so that the
 * instance could be cached. Any Index specific information should go to FieldSchema.
 */
class FieldType<T>(val properties: FieldProperties<T>) : IndexField() {
    override val dataTypeName: String
        get() = properties.dataTypeName

    override val defaultFieldName: String?
        get() = properties.defaultFieldName

    override val sortFieldType: SortField.Type
        get() = properties.sortFieldType

    override val defaultStringValue: String
        get() = properties.defaultStringValue

    override val needsExtraStoreField: Boolean
        get() = properties.needsExtraStoreField

    override val isNumeric: Boolean
        get() = properties.isNumeric

    override val supportsAnalyzer: Boolean
        get() = properties.supportsAnalyzer

    override val storedOnly: Boolean
        get() = properties.storedOnly

    override val minimumStringValue: String
        get() = properties.minimumStringValue

    override val maximumStringValue: String
        get() = properties.maximumStringValue

    override val autoPopulated: Boolean
        get() = properties.autoPopulated

    private val p = properties

    /**
     * Validates the provided string input to see if it matches the correct format for
     * fields type. In case it can't validate the input then it returns a tuple with
     * false and the default value of the fields. Then it is up to the caller to decide
     * whether to thrown an error or use the default value for the fields.
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
     * Create a new Field builder for the given fields.
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
     * Update a fields builder from the given FlexDocument. This is a higher level method which
     * bring together a number of lower level method from FieldBase
     */
    override fun updateDocument(value: FieldValue, fieldSchema: FieldSchema, fieldTemplate: FieldTemplate) {
        if (value.valueCase == properties.valueCase) {
            p.updateFieldTemplateWithFieldValue(fieldTemplate, value)
        } else {
            p.updateFieldTemplate(fieldTemplate, p.defaultValue)
        }
    }

    /**
     * Get tokens for a given input. This is not supported by all fields types for example
     * it does not make any sense to tokenize numeric types and exact text fields. In these
     * cases the internal representation of the fields type is used.
     * Note: An instance of List is passed so that we can avoid memory allocation and
     * reuse the list from the object pool.
     * Note: Avoid using the below for numeric types.
     */
    override fun getTokens(fieldValue: String, tokens: ArrayList<String>, fs: FieldSchema) {
        when (fs.analyzers) {
            null ->
                // The fields does not have an associated analyzer so just add the input to
                // the result by using the fields specific formatting
                tokens.add(toInternalString(fieldValue))
            else -> parseTextUsingAnalyzer(fs.analyzers.searchAnalyzer, fs.schemaName, fieldValue, tokens)
        }
    }

    /**
     * Returns a query which provides the exact text match for the given fields type.
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
