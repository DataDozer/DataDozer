package org.datadozer

import org.datadozer.models.OperationMessage
import org.datadozer.models.OperationStatus

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

fun OperationMessage.toException(): OperationException {
    return OperationException(this)
}

object Message {
    fun dataCannotBeParsed(fieldName: String, dataType: String, value: String): OperationMessage {
        return OperationMessage.newBuilder()
                .setMessage("Data cannot be parsed for the fields '$fieldName'.")
                .setDetails("field_name='$fieldName',expected_data_type='$dataType',actual_value='$value'")
                .setStatus(OperationStatus.FAILURE)
                .build()
    }

    fun rangeQueryNotSupported(fieldName: String, dataType: String): OperationMessage {
        return OperationMessage.newBuilder()
                .setMessage("Range query is not supported by the fields '$fieldName' of type '$dataType'.")
                .setDetails("field_name='$fieldName',expected_data_type='$dataType'")
                .setStatus(OperationStatus.FAILURE)
                .build()
    }

    fun setQueryNotSupported(fieldName: String, dataType: String): OperationMessage {
        return OperationMessage.newBuilder()
                .setMessage("Set query is not supported by the fields '$fieldName' of type '$dataType'.")
                .setDetails("field_name='$fieldName',expected_data_type='$dataType'")
                .setStatus(OperationStatus.FAILURE)
                .build()
    }

    fun exactQueryNotSupported(fieldName: String, dataType: String): OperationMessage {
        return OperationMessage.newBuilder()
                .setMessage("Exact query is not supported by the fields '$fieldName' of type '$dataType'.")
                .setDetails("field_name='$fieldName',expected_data_type='$dataType'")
                .setStatus(OperationStatus.FAILURE)
                .build()
    }

    fun analyzerNotFound(analyzerName: String): OperationMessage {
        return OperationMessage.newBuilder()
                .setMessage("Analyzer not found: '$analyzerName'.")
                .setDetails("analyzer_name='$analyzerName'")
                .setStatus(OperationStatus.FAILURE)
                .build()
    }

    fun fieldIsMandatory(fieldName: String): OperationMessage {
        return OperationMessage.newBuilder()
                .setMessage("Field:'$fieldName' is mandatory.")
                .setDetails("field_name='$fieldName'")
                .setStatus(OperationStatus.FAILURE)
                .build()
    }

    fun documentIdAlreadyExists(id: String): OperationMessage {
        return OperationMessage.newBuilder()
                .setMessage(
                        "Document with id: $id already exists. Optimistic update failed.")
                .setDetails("id='$id',expected_id='0'")
                .setStatus(OperationStatus.FAILURE)
                .build()
    }

    fun documentNotFound(id: String): OperationMessage {
        return OperationMessage.newBuilder()
                .setMessage(
                        "Document with id: $id does not exists.")
                .setDetails("id='$id'")
                .setStatus(OperationStatus.FAILURE)
                .build()
    }

    fun versionConflict(id: String, modifyIndex: Long, existingVersion: Long): OperationMessage {
        return OperationMessage.newBuilder()
                .setMessage(
                        "Indexing version conflict, id: $id with modifyIndex: $modifyIndex does not match the current modifyIndex: $existingVersion. Optimistic update failed.")
                .setDetails(
                        "id='$id',modify_index='$modifyIndex',existing_index='$existingVersion'")
                .setStatus(OperationStatus.FAILURE)
                .build()
    }
}