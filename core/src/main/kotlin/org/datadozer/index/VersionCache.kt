package org.datadozer.index

import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.Term
import org.apache.lucene.search.ReferenceManager
import org.datadozer.Message
import org.datadozer.index.fields.IdField
import org.datadozer.index.fields.ModifyIndex
import org.datadozer.toException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

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
 * Version cache store used across the system. This helps in resolving
 * conflicts arising out of concurrent threads trying to update a Lucene document.
 * Every document update should go through version cache to ensure the update
 * integrity and optimistic locking.
 *
 * In order to reduce contention there will be one CacheStore per shard.
 * Initially Lucene's LiveFieldValues seemed like a good alternative but it
 * complicates the design and requires thread management.
 */
class VersionCache(private val shardWriter: ShardWriter)
    : ReferenceManager.RefreshListener {

    private val maps = Array(2, { ConcurrentHashMap<ByteArray, Long>() })

    private val current = AtomicInteger(0)
    private val currentMap
        get() = maps[current.get()]

    private val oldMap
        get() =
            if (current.get() == 0) {
                maps[1]
            } else {
                maps[0]
            }

    private val idFieldName = IdField.SCHEMA.schemaName
    private val modifyIndexFieldName = ModifyIndex.SCHEMA.schemaName

    /** Called right before a refresh attempt starts.  */
    override fun beforeRefresh() {
        // Start sending all updates after this point to the new
        // dictionary.  While reopen is running, any lookup will first
        // try this new dictionary, then fall back to old, then to the
        // current searcher
        if (current.compareAndSet(0, 1)) {
            // Now the active map is 1
            // So let's clear the map 0
            maps[0].clear()
        } else {
            // Active map is 1 so let's set it to 0
            current.set(0)
            maps[1].clear()
        }
    }

    /** Called after the attempted refresh; if the refresh
     * did open a new reference then didRefresh will be true
     * and [.acquire] is guaranteed to return the new
     * reference.  */
    override fun afterRefresh(didRefresh: Boolean) {
        // Now drop all the old values because they are now
        // visible via the searcher that was just opened; if
        // didRefresh is false, it's possible old has some
        // entries in it, which is fine: it means they were
        // actually already included in the previously opened
        // reader.  So we can safely clear old here.
        if (current.get() == 0) {
            maps[1].clear()
        } else {
            maps[0].clear()
        }
    }

    /**
     * An optimized key based lookup to get the version value using
     * Lucene's DocValues
     */
    fun primaryKeyLookup(id: ByteArray, r: IndexReader): Long {
        val term = Term(idFieldName, id.toString())
        for (leave in r.leaves()) {
            val reader = leave.reader()
            val terms = reader.terms(idFieldName)
            val termEnum = terms.iterator()
            if (termEnum.seekExact(term.bytes())) {
                val docEnums = termEnum.postings(null, 0)
                val nDocs = reader.getNumericDocValues(modifyIndexFieldName)
                return nDocs.advance(docEnums.docID()).toLong()
            }
        }

        return 0L
    }

    /**
     * Add or update a key in the version cache store
     */
    fun addOrUpdate(id: ByteArray, version: Long, comparison: Long): Boolean {
        val oldValue = currentMap[id]
        return if (oldValue == null) {
            currentMap.putIfAbsent(id, version) == null
        } else {
            if (comparison == 0L) {
                // It is an unconditional update
                currentMap.put(id, version)
                true
            } else {
                currentMap.replace(id, comparison, version)
            }
        }
    }

    /**
     * Delete an key from the version cache. This does not delete the key
     * but resets the version value to 0L
     */
    fun delete(id: ByteArray, version: Long): Boolean {
        return addOrUpdate(id, 0L, version)
    }

    /**
     * Get the version associated with a key from the cache store
     */
    fun get(id: ByteArray): Long {
        val value = currentMap[id]
        return if (value == null) {
            val oldValue = oldMap[id]
            if (oldValue == null) {
                val indexValue = primaryKeyLookup(id, shardWriter.getRealtimeSearcher().indexReader)
                currentMap.putIfAbsent(id, indexValue)
                indexValue
            } else {
                oldValue
            }
        } else {
            value
        }
    }

    /**
     * Check and returns the current version number of the document
     */
    fun versionCheck(id: ByteArray, modifyIndex: Long, newVersion: Long): Long {
        return when (modifyIndex) {
            0L ->
                // We don't care what the version is let's proceed with normal operation
                // and bypass id check.
                0L
            -1L -> {
                // Ensure that the document does not exists. Perform Id check
                val existingVersion = get(id)
                if (existingVersion != 0L) {
                    throw Message.documentIdAlreadyExists(id.toString()).toException()
                }
                return existingVersion
            }
            1L -> {
                // Ensure that the document exists
                val existingVersion = get(id)
                if (existingVersion != 0L) {
                    return existingVersion
                }

                throw Message.documentNotFound(id.toString()).toException()
            }
            else -> {
                val existingVersion = get(id)
                if (existingVersion != 0L) {
                    if (existingVersion != modifyIndex || existingVersion > newVersion) {
                        throw Message.versionConflict(id.toString(), modifyIndex, existingVersion).toException()
                    }

                    return existingVersion
                }

                throw Message.documentNotFound(id.toString()).toException()
            }
        }
    }
}
