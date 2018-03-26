package org.datadozer.index

import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.search.ReferenceManager
import org.apache.lucene.search.SearcherFactory
import org.apache.lucene.search.SearcherManager
import org.apache.lucene.store.Directory
import org.datadozer.LuceneDocument
import org.datadozer.index.fields.ModifyIndex
import org.datadozer.logProvider
import org.datadozer.models.IndexConfiguration

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
 * A ShardWriter creates and maintains a shard of an index.
 * Note: This encapsulates the functionality of IndexWriter and
 * SearcherManager through an easy to manage abstraction.
 */
class ShardWriter(
        private val shardNumber: Int,
        private val indexName: String,
        private val settings: IndexConfiguration,
        config: IndexWriterConfig,
        directory: Directory
) : AutoCloseable, IndexWriter(directory, config) {
    private val indexWriter = IndexWriter(directory, config)
    private val searcherManager = SearcherManager(indexWriter, SearcherFactory())
    private val logger = logProvider<ShardWriter>()

    /**
     * Get the highest modified index value from the shard
     */
    fun getMaxModifyIndex(): Long {
        var max = 0L
        val searcher = RealTimeSearcher(searcherManager)
        searcher.use {
            val r = searcher.indexReader
            for (l in r.leaves()) {
                val reader = l.reader()
                val nDocs = reader.getNumericDocValues(ModifyIndex.INSTANCE.properties.defaultFieldName)
                for (j in 0 until reader.maxDoc()) {
                    max = Math.max(max, nDocs.advance(j).toLong())
                }
            }
        }

        return max
    }

    /**
     * Commits all pending changes (added & deleted documents, segment merges, added indexes, etc.) to the index,
     * and syncs all referenced index files, such that a reader will see the changes and the index updates will
     * survive an OS or machine crash or power loss. Note that this does not wait for any running background
     * merges to finish. This may be a costly operation, so you should test the cost in your application and
     * do it only when really necessary.
     */
    fun commit(forceCommit: Boolean) {
        if (forceCommit || indexWriter.hasUncommittedChanges()) {
            indexWriter.commit()
        }
    }

    /**
     * Commits all changes to an index, waits for pending merges to complete, closes all
     * associated files and releases the write lock.
     */
    override fun close() {
        try {
            searcherManager.close()
            if (settings.commitOnClose.value) {
                commit(false)
            }
            indexWriter.close()
        } catch (e: Exception) {
            logger.error("Error in closing the shard of index. shardnumber='$shardNumber',index_name='$indexName'", e)
        }
    }

    /**
     * Returns real time searcher
     */
    fun getRealtimeSearcher() = RealTimeSearcher(searcherManager)

    /**
     * You must call this periodically, if you want that GetRealTimeSearcher() will return refreshed instances.
     */
    fun refresh() = searcherManager.maybeRefresh()

    /**
     * Adds a listener, to be notified when a reference is refreshed/swapped
     */
    fun addRefreshListener(item: ReferenceManager.RefreshListener) {
        searcherManager.addListener(item)
    }

    /**
     * Removes a listener added with AddRefreshListener
     */
    fun removeRefreshListener(item: ReferenceManager.RefreshListener) {
        searcherManager.removeListener(item)
    }

    /**
     * Adds a document to this index.
     */
    fun addDocument(document: LuceneDocument) {
        indexWriter.addDocument(document)
    }
}