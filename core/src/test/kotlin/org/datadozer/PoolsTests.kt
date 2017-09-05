package org.datadozer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

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

data class Test(var prop1: String, var prop2: Int)

class PoolTests {

    @Test
    fun `Integers can be parsed`() {
        for (i in 0 until 110) {
            assertEquals(i, fastIntParser(i.toString()))
        }

        assertEquals(999, fastIntParser(999.toString()))
        assertEquals(1000, fastIntParser(1000.toString()))
        assertEquals(10000, fastIntParser(10000.toString()))
    }
}

class SingleInstancePerThreadObjectPoolTests {
    private val threadPool = threadPoolExecutorProvider()
    private val pool = SingleInstancePerThreadObjectPool({ Test("test", 0) })

    @Test
    fun `Should work with a custom thread pool`() {
        println("Available processors: $AvailableProcessors")
        val runnable = Runnable {
            println("Running in thread:${Thread.currentThread().name}")
            for (i in 0..50) {
                var value = pool.borrowObject(null)
                assertEquals(i, value.prop2)
                value.prop2 += 1
                pool.returnObject(value, null)
            }
        }

        val tasks = List(AvailableProcessors, { _ -> Executors.callable(runnable) })
        var result = threadPool.invokeAll(tasks)

        for (res in result) {
            res.get()
        }
    }

    @Test
    fun `Borrowing multiple objects will return a new instance`() {
        val runnable = Runnable {
            println("Running in thread:${Thread.currentThread().name}")
            var value = pool.borrowObject(getThreadId())
            assertEquals(0, value.prop2)
            value.prop2 += 1
            pool.returnObject(value, getThreadId())
            value = pool.borrowObject(null)
            // Value should be 1 as we have set it above
            assertEquals(1, value.prop2)

            // Borrowing again without returning will return a new object
            var value1 = pool.borrowObject(getThreadId())
            assertEquals(0, value1.prop2)
            assertNotEquals(value, value1)
        }

        threadPool.submit(runnable).get()
    }
}