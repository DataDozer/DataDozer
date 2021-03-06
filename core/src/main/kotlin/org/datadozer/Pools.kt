package org.datadozer

import java.lang.IllegalStateException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
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
 * Provides a application specific thread pool where all the threads
 * are numbered. This pool is used along with the object pool to
 * provide no lock based object pool.
 */
fun threadPoolExecutorProvider(): ThreadPoolExecutor {
    // Custom thread factory to ensure that
    val threadFactory = object : ThreadFactory {
        val counter = AtomicInteger(-1)
        override fun newThread(r: Runnable?): Thread {
            val t = Thread(r)
            t.name = counter.incrementAndGet().toString()
            return t
        }
    }

    return ThreadPoolExecutor(AvailableProcessors, AvailableProcessors,
                              0L, TimeUnit.MILLISECONDS,
                              LinkedBlockingQueue<Runnable>(1000), threadFactory)
}

/**
 * A fast int parser which only works in limited cases. It expects the numbers
 * to be small and positive. Do not use it for parsing any user data. It is used
 * by various pools to parse the thread name.
 * NOTE:
 * - This class does not do any overflow, null or empty string checks
 * - We always assume the number is correctly formatted
 */
@Suppress("NOTHING_TO_INLINE")
inline fun fastIntParser(value: String): Int {
    // Unroll the first run of the loop
    var ch = value[0]
    var v = '0' - ch

    for (i in 1 until value.length) {
        ch = value[i]
        if (i > 0) {
            v *= 10
        }
        v += '0' - ch
    }
    return -v
}

/**
 * Returns the custom thread id associated with the current thread. This is only useful
 * for the threads from our custom thread pool.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun getThreadId(): Int {
    try {
        return fastIntParser(Thread.currentThread().name)
    } catch (e: Exception) {
        throw IllegalStateException("Object pool can only be accessed with specialized threads.")
    }
}

/**
 * This is a lock free object pool which only maintains one object per thread.
 * In this regard this is not a generalized object pool. In concept it is similar
 * to thread local variables but without the risk of leaking memory.
 *
 * @param provider
 * Provider to create new pool items
 */
class SingleInstancePerThreadObjectPool<T : Any?>(private val provider: () -> T) {
    private val logger = logProvider<SingleInstancePerThreadObjectPool<T>>()
    private val pool = ArrayList<T>(AvailableProcessors)

    // Tracks if an item is borrowed by a thread or not. Strictly speaking this is
    // not necessary. We don't even need the return functionality as the pool will always
    // hold an reference to the object. This is to avoid nasty problems in future if we have
    // the same thread modifying the same object at more than one place without recycling it.
    // We need strict discipline that the objects are always returned in the finally block.
    private val borrowStatus = Array(AvailableProcessors, { _ -> false })

    init {
        for (i in 0 until AvailableProcessors) {
            pool.add(provider())
        }
    }

    /**
     * Borrow an object from the pool. Make sure you only borrow the item once per thread.
     * Borrowing more than once per thread will result in an error. Items should only
     * be borrowed for a very short span of time.
     */
    fun borrowObject(slotNumber: Int? = null): T {
        val threadId = slotNumber ?: getThreadId()
        val item = pool[threadId]
        return if (!borrowStatus[threadId]) {
            borrowStatus[threadId] = true
            item
        } else {
            // This essentially means that we are borrowing an object from the same thread more than once without
            // returning it back to the pool. Ideally this should never happen and the objects should be returned.
            // The best way to recover is to give a new object back to the caller and log a warning.
            logger.warn("Expecting object pool thread slot to have a value. Make sure you are not borrowing twice.")
            provider()
        }
    }

    /**
     * Borrow an object from the pool and pass it to an action and automatically
     * return the object back to the pool
     */
    inline fun borrow(block: (T) -> Unit) {
        val borrowed = borrowObject()
        try {
            block(borrowed)
        } finally {
            returnObject(borrowed)
        }
    }

    /**
     * Return an borrowed object to the pool. Never return an object twice as it will result
     * in an error.
     */
    fun returnObject(item: T, slotNumber: Int? = null) {
        if (item == null) {
            return
        }

        val threadId = slotNumber ?: getThreadId()
        if (!borrowStatus[threadId]) {
            logger.warn(
                    "Expecting object pool thread slot to be empty. Make sure you are not returning twice or returning a non pooled object.")
        }

        // Irrespective of whether the item was returned twice the best way to recover from the situation is to
        // take the object and set the borrow status to false.
        pool[threadId] = item
        borrowStatus[threadId] = false
    }
}