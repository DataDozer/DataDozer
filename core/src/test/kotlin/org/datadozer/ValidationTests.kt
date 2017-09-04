package org.datadozer

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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

val fieldName = "test"

class ValidationTests {
    @Test
    fun `hasDuplicates with duplicate entries should fail`() {
        val sut = arrayOf("a", "a", "b")
        testThrows {
            hasDuplicates(fieldName, fieldName, sut)
        }
    }

    @Test
    fun `hasDuplicates without duplicate entries should pass`() {
        val sut = arrayOf("a", "b", "c")
        hasDuplicates(fieldName, fieldName, sut)
    }

    @Test
    fun `greaterThan tests`() {
        assertAll(
                exec { greaterThan(fieldName, 4, 5) },
                execT { greaterThan(fieldName, 4, 4) },
                execT { greaterThan(fieldName, 6, 4) }
        )
    }

    @Test
    fun `greaterThanEqual tests`() {
        assertAll(
                exec { greaterThanEqual(fieldName, 4, 5) },
                exec { greaterThanEqual(fieldName, 4, 4) },
                execT { greaterThanEqual(fieldName, 4, 3) }
        )
    }

    @Test
    fun `lessThan tests`() {
        assertAll(
                exec { lessThan(fieldName, 4, 3) },
                execT { lessThan(fieldName, 4, 4) },
                execT { lessThan(fieldName, 6, 8) }
        )
    }

    @Test
    fun `lessThanEqual tests`() {
        assertAll(
                exec { lessThanEqual(fieldName, 4, 4) },
                exec { lessThanEqual(fieldName, 4, 3) },
                execT { lessThanEqual(fieldName, 4, 5) }
        )
    }

    @Test
    fun `notBlank tests`() {
        assertAll(
                exec { notBlank(fieldName, "test") },
                execT { notBlank(fieldName, "") }
        )
    }

    @ParameterizedTest
    @ValueSource(strings = arrayOf("test", "1234", "_test"))
    fun `propertyValidator success tests`(sut: String) {
        isPropertyName(fieldName, sut)
    }

    @ParameterizedTest
    @ValueSource(strings = arrayOf("&test", "Test", "@test"))
    fun `propertyValidator failure tests`(sut: String) {
        testThrows { isPropertyName(fieldName, sut) }
    }
}