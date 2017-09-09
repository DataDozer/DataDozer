package org.datadozer

import org.datadozer.models.OperationMessage

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

object Message {
    fun dataCannotBeParsed(fieldName: String, dataType: String, value: String): OperationMessage {
        return (OperationMessage.newBuilder()
                .setMessage("Data cannot be parsed for the field '$fieldName'.")
                .setDetails("field_name='$fieldName',expected_data_type='$dataType',actual_value='$value'")
                .setOperationCode(OperationCode.ValidationError)
                .build())
    }

    fun rangeQueryNotSupported(fieldName: String, dataType: String): OperationMessage {
        return (OperationMessage.newBuilder()
                .setMessage("Range query is not supported by the field '$fieldName' of type '$dataType'.")
                .setDetails("field_name='$fieldName',expected_data_type='$dataType'")
                .setOperationCode(OperationCode.ValidationError)
                .build())
    }

    fun setQueryNotSupported(fieldName: String, dataType: String): OperationMessage {
        return (OperationMessage.newBuilder()
                .setMessage("Set query is not supported by the field '$fieldName' of type '$dataType'.")
                .setDetails("field_name='$fieldName',expected_data_type='$dataType'")
                .setOperationCode(OperationCode.ValidationError)
                .build())
    }

    fun exactQueryNotSupported(fieldName: String, dataType: String): OperationMessage {
        return (OperationMessage.newBuilder()
                .setMessage("Exact query is not supported by the field '$fieldName' of type '$dataType'.")
                .setDetails("field_name='$fieldName',expected_data_type='$dataType'")
                .setOperationCode(OperationCode.ValidationError)
                .build())
    }
}