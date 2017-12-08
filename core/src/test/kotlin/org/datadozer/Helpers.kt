package org.datadozer

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.function.Executable

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

val k = dependencyFrameworkProvider()

typealias exec = Executable

fun testThrows(assertion: () -> Unit) {
    Assertions.assertThrows(OperationException::class.java, {
        assertion()
    })
}

fun execT(assertion: () -> Unit): Executable {
    return exec {
        Assertions.assertThrows(OperationException::class.java, {
            assertion()
        })
    }
}

fun assertEqualsIgnoreNewlineStyle(s1: String?, s2: String?, message: String) {
    assertEquals(true, s1 != null && s2 != null && normalizeLineEnds(s1) == normalizeLineEnds(s2), message)
}

private fun normalizeLineEnds(s: String): String {
    return s.replace("\r\n", "\n").replace('\r', '\n')
}