package org.datadozer.index

import org.datadozer.SingleInstancePerThreadObjectPool
import org.datadozer.models.Document
import org.datadozer.models.FieldValue
import org.datadozer.models.TransactionLogEntryType
import org.datadozer.threadPoolExecutorProvider
import org.junit.AfterClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

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

class TransactionLogTests {

    private val settings = WriterSettings(syncFlush = true)
    private val threadPool = threadPoolExecutorProvider()

    private val path = Files.createTempDirectory(null)
    private val pool = SingleInstancePerThreadObjectPool({ TransactionWriter(path.toFile(), settings) })
    private var transactionId = AtomicLong(1)

    @Test
    fun `Can read write transactions using multiple threads`() {
        val runnable = Runnable {
            var value = pool.borrowObject()
            val doc = Document.newBuilder().setId("1").setIndexName("index1").build()
            val txEntry = transactionLogEntryFactory(1, transactionId.getAndIncrement(),
                                                     TransactionLogEntryType.DOC_DELETE, doc)
            value.appendEntry(txEntry, 1)
            pool.returnObject(value)
        }

        val tasks = List(999, { _ -> Executors.callable(runnable) })
        var result = threadPool.invokeAll(tasks)

        for (res in result) {
            res.get()
        }

        val sut = TransactionReader(0L, path.toAbsolutePath().toString())
        var txId = 1L
        for (t in sut.replayTransactionsByIndex(1)) {
            assertEquals(txId, t.modifyIndex)
            txId++
        }

        // We started with x transactions so we should get back x transactions
        assertEquals(999, sut.totalTransactionsCount)
    }

    @Test
    fun `Can read back the data from a transaction`() {
        val path = Files.createTempDirectory(null)
        val sut = TransactionWriter(path.toFile(), settings)
        for (i in 1 until 100) {
            val doc = Document.newBuilder().setId(i.toString()).setIndexName("index1").build()
            val entry = transactionLogEntryFactory(1, i.toLong(),
                                                   TransactionLogEntryType.DOC_DELETE, doc)
            sut.appendEntry(entry, 1)
        }

        // Let read all the data from the transactions
        var txId = 1L
        val tr = TransactionReader(1, path.toAbsolutePath().toString())
        for (t in tr.replayTransactionsByIndex(1)) {
            val data = tr.getDataForEntry(t)
            assertEquals(txId.toString(), data.id)
            assertEquals("index1", data.indexName)
            txId++
        }
    }

    @Test
    fun Test() {
        val fv = FieldValue.newBuilder()
        fv.stringValue = "53w5"
        val f = fv.build()

        if(f.integerValue == 0) {
            val c = true;
        }
    }

    @AfterClass
    fun cleanup() {
        Files.delete(path)
    }
}