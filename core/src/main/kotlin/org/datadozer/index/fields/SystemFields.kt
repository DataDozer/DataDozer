package org.datadozer.index.fields

import org.datadozer.models.Similarity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
 * Used for representing the id of an index
 */
object IdField {
    private val info = KeywordField.info.copy(
            defaultFieldName = "__id"
    )

    val INSTANCE = FieldType(info)
    val SCHEMA = FieldSchema(
            fieldName = info.defaultFieldName!!,
            schemaName = info.defaultFieldName!!,
            docValues = false,
            fieldType = INSTANCE,
            similarity = Similarity.BM25,
            analyzers = null,
            fieldOrder = 0,
            multiValued = false
    )
}

/**
 * This fields is used to add causal ordering to the events in
 * the index. A document with lower modify index was created/updated before
 * a document with the higher index.
 * This is also used for concurrency updates.
 */
object ModifyIndex {
    private val info = LongField.info.copy(
            defaultFieldName = "__modifyindex"
    )

    val INSTANCE = FieldType(info)
    val SCHEMA = FieldSchema(
            fieldName = info.defaultFieldName!!,
            schemaName = info.defaultFieldName!!,
            docValues = true,
            fieldType = INSTANCE,
            similarity = Similarity.BM25,
            analyzers = null,
            fieldOrder = 1,
            multiValued = false
    )
}

/**
 * Field to be used for time stamp. This fields is used by index to capture the modification
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
    val SCHEMA = FieldSchema(
            fieldName = info.defaultFieldName!!,
            schemaName = info.defaultFieldName!!,
            docValues = true,
            fieldType = INSTANCE,
            similarity = Similarity.BM25,
            analyzers = null,
            fieldOrder = -1,
            multiValued = false
    )
}

/**
 * Log fields is an internal fields which is used to maintain the transaction
 * log. The advantage of saving the transaction log in the index is that it
 * is straight forward to sync the replicas and slave from the index using the
 * modify index. There is a minimal disk space overhead when saving the transaction
 * log in the index.
 *
 * Complete documents are not saved in the log as they are already part of the
 * Lucene document. we only save important metadata which helps us in identifying
 * the transaction information.
 */
object LogField {
    private val info = org.datadozer.index.fields.StoredField.info.copy(
            defaultFieldName = "__log",
            autoPopulated = true
    )

    val INSTANCE = FieldType(info)
    val SCHEMA = FieldSchema(
            fieldName = info.defaultFieldName!!,
            schemaName = info.defaultFieldName!!,
            docValues = false,
            fieldType = INSTANCE,
            similarity = Similarity.BM25,
            analyzers = null,
            fieldOrder = 2,
            multiValued = false
    )
}

/**
 * Internal fields used to represent the type of transaction that was
 * performed when the related document was added to the index. This
 * can be broadly classified into document and index related operations.
 * Index related operations deal with index schema changes while document
 * related changes are related to creating/updating and deleting of the
 * documents.
 *
 * This fields is also used to filter out the deleted and non document related
 * results from the search query.
 */
object TransactionTypeField {

}