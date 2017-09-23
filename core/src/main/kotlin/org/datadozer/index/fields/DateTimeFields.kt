package org.datadozer.index.fields

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
