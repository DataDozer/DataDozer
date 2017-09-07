package org.datadozer

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.singleton
import java.util.concurrent.ThreadPoolExecutor

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

fun dependencyFrameworkProvider(): Kodein {
    return Kodein {
        bind<ThreadPoolExecutor>() with singleton { threadPoolExecutorProvider() }
        bind<Settings>() with singleton { Settings() }
    }
}

/**
 * Initialize the listeners to be used across the application
 */
fun initializeListeners() {

}

/**
 * Capture all un-handled exceptions
 */
fun subscribeToUnhandledExceptions() {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        Logger.error("Critical system error", e)
    }
}

/**
 * Load all the server components and return writerSettings
 */
fun load() {
    subscribeToUnhandledExceptions()

}

fun main(args: Array<String>) {

}