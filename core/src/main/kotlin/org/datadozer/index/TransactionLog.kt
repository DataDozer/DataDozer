package org.datadozer.index

import com.google.protobuf.GeneratedMessageV3
import org.datadozer.Settings
import org.datadozer.logProvider
import org.datadozer.models.Document
import org.datadozer.models.TransactionLogEntry
import org.datadozer.models.TransactionLogEntryHeader
import org.datadozer.models.TransactionLogEntryType
import java.io.*
import java.nio.ByteBuffer
import java.nio.file.Paths
import java.util.*
import kotlin.collections.ArrayList

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
 * Transaction Writer settings
 */
data class WriterSettings(
        val bufferSize: Int = 8192,
        val syncFlush: Boolean = false,
        val outstandingFlushCount: Int = 5) {
    companion object {
        fun factory(settings: Settings): WriterSettings {
            return WriterSettings(
                    bufferSize = settings.getIntOrDefault("buffersize", 8192),
                    syncFlush = settings.getBooleanOrDefault("syncflush", false),
                    outstandingFlushCount = settings.getIntOrDefault("outstandingflushcount", 5)
            )
        }
    }
}

/**
 * TransactionWriter is used for writing transaction entries.
 * NOTE: This is not thread-safe and should be pooled using the custom
 * object pool for better performance.
 */
class TransactionWriter(
        private val dirPath: File,
        private val settings: WriterSettings)
    : AutoCloseable {

    data class Writers(val header: DataOutputStream, val data: DataOutputStream)

    private val logger = logProvider<TransactionWriter>()

    private var writers = getFileWriters()
    private var dataPos = 0
    private var transactionCount = 0
    private var toBeFlushedCount = 0

    /**
     * Starting transaction number of the current file. This is helpful
     * in determining if a file could be deleted or not
     */
    private var startTransactionNumber = 0L

    init {
        if (!dirPath.exists()) {
            dirPath.mkdir()
        }
    }

    /**
     * Returns a StreamWriter based on the physical file whose name is determined
     * by the transaction ID
     */
    private fun getFileWriters(): Writers {
        val id = UUID.randomUUID()
        fun getFileWriter(extension: String): DataOutputStream {
            val headerFile = File(Paths.get(dirPath.absolutePath, "$id.$extension").toUri())
            val headerStream = FileOutputStream(headerFile)
            return DataOutputStream(BufferedOutputStream(headerStream, settings.bufferSize))
        }

        return Writers(getFileWriter("log"), getFileWriter("bin"))
    }

    /**
     * Refreshes the writers by closing the older writers and creating
     * new ones based on the transaction numbers
     */
    private fun refreshWriters(transactionId: Long) {
        close()

        // Since we are starting in a new file all the counters should be reset
        dataPos = 0
        writers = getFileWriters()
        transactionCount = 0
        startTransactionNumber = transactionId
    }

    /**
     * Append an transaction entry into the transaction log
     */
    fun appendEntry(entry: TransactionLogEntry, indexId: Int) {
        // Start writing to a new file if the transaction count has exceeded
        if (transactionCount > 1000 || dataPos > 10 * 1024 * 1024) {
            refreshWriters(entry.header.modifyIndex)
        }

        writers.data.writeInt(entry.header.messageSize)
        entry.data.writeTo(writers.data)

        // Each header is fixed size and we don't want to use proto buffer for encoding these
        // as the gain are not substantial compared to the advantage of fixed size records
        writers.header.writeLong(entry.header.modifyIndex)
        writers.header.writeInt(indexId)
        writers.header.writeInt(entry.header.entryType.number)
        writers.header.writeInt(dataPos)

        // Increment the data location for the next message
        // The 4 bytes are for the prepended message size value in the data file.
        dataPos += entry.header.messageSize + 4
        toBeFlushedCount++

        if (toBeFlushedCount == settings.outstandingFlushCount || settings.syncFlush) {
            writers.header.flush()
            writers.data.flush()
            toBeFlushedCount = 0
        }

        transactionCount++
    }

    override fun close() {
        try {
            writers.header.close()
        } catch (e: Exception) {
            logger.error(
                    "Error closing the Transaction writer header file. " +
                            "Path='${dirPath.absolutePath}/$startTransactionNumber.log'")
        }

        try {
            writers.data.close()
        } catch (e: Exception) {
            logger.error(
                    "Error closing the Transaction writer data file. " +
                            "Path='${dirPath.absolutePath}/$startTransactionNumber.bin'")
        }
    }
}

/**
 * Responsible for reading transaction logs from the log files. The logs are all read first
 * then sorted by the transactionId.
 */
class TransactionReader(private val transactionId: Long, private val dirPath: String) {
    private val transactions = ArrayList<TransactionLogEntryHeader>()

    /**
     * Saves the mappings between the fileIds and the file names
     */
    private val fileIds = ArrayList<String>()

    init {
        readTransactions()
    }

    private fun readTransactions() {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            throw IllegalArgumentException("The specified path for Transaction Logs is not valid: '$dirPath'")
        }

        val files = dir.listFiles({ _, name -> name.endsWith(".log") })
        if (files.isEmpty()) {
            return
        }

        for ((fileId, file) in files.withIndex()) {
            val fileInputStream = FileInputStream(file)
            val inputStream = DataInputStream(BufferedInputStream(fileInputStream))
            val byteArray = ByteArray(20)
            while (inputStream.read(byteArray) != -1) {
                val builder = TransactionLogEntryHeader.newBuilder()
                val buffer = ByteBuffer.wrap(byteArray)
                builder.modifyIndex = buffer.getLong(0)
                builder.indexId = buffer.getInt(8)
                builder.entryType = TransactionLogEntryType.forNumber(buffer.getInt(12))
                builder.dataLocation = buffer.getInt(16)

                builder.fileId = fileId
                transactions.add(builder.build())
            }

            fileIds.add(file.nameWithoutExtension)
        }

        transactions.sortBy { it.modifyIndex }
        transactions.removeIf({ it.modifyIndex < transactionId })
    }

    val totalTransactionsCount get() = transactions.size

    /**
     * Used by an index to retrieve transaction logs applicable for it
     */
    fun replayTransactionsByIndex(indexId: Int): Sequence<TransactionLogEntryHeader> {
        return transactions.filter { it.indexId == indexId }.asSequence()
    }

    fun getDataForEntry(entry: TransactionLogEntryHeader): Document {
        val fileInputStream = FileInputStream(Paths.get(dirPath, fileIds[entry.fileId] + ".bin").toFile())
        val inputStream = DataInputStream(BufferedInputStream(fileInputStream))
        inputStream.skip(entry.dataLocation.toLong())
        val size = inputStream.readInt()
        val data = ByteArray(size)
        inputStream.read(data)
        return Document.parseFrom(data)
    }
}

/**
 * Generates a transaction log entry for a given message
 */
fun transactionLogEntryFactory(indexId: Int, transactionId: Long,
                               transactionLogEntryType: TransactionLogEntryType,
                               messageV3: GeneratedMessageV3): TransactionLogEntry {
    val builder = TransactionLogEntry.newBuilder()
    builder.headerBuilder.indexId = indexId
    builder.headerBuilder.modifyIndex = transactionId
    builder.headerBuilder.entryType = transactionLogEntryType
    builder.headerBuilder.messageSize = messageV3.serializedSize
    builder.data = messageV3.toByteString()
    return builder.build()
}

