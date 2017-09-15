package org.datadozer

import com.google.protobuf.GeneratedMessageV3
import java.io.*
import java.nio.ByteBuffer
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

/**
 * Converts UUID to byte array
 */
fun uuidToByteArray(id: UUID): ByteArray {
    val bb = ByteBuffer.wrap(kotlin.ByteArray(16))
    bb.putLong(id.mostSignificantBits)
    bb.putLong(id.leastSignificantBits)
    return bb.array()
}

/**
 * Writes a given protobuf message to file
 */
fun writeFile(file: File, message: GeneratedMessageV3) {
    val writer = BufferedOutputStream(FileOutputStream(file))
    writer.use {
        message.writeTo(writer)
        writer.flush()
    }
}

/**
 * Reads a given file and deserializes it using the passed parser method
 */
fun <T> readFile(file: File, parseFrom: (ByteArray) -> T): T {
    val writer = BufferedInputStream(FileInputStream(file))
    writer.use {
        return parseFrom(writer.readBytes())
    }
}
