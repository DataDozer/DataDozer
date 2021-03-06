package org.datadozer

import org.apache.logging.log4j.LogManager

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
 * Returns the number of processors available to the Java virtual machine.
 */
val AvailableProcessors = Runtime.getRuntime().availableProcessors()

/**
 * Returns the default logger
 */
inline fun <reified T : Any> logProvider() = LogManager.getLogger(T::class.java.simpleName)!!

/**
 * Main default logger
 */
val Logger = LogManager.getLogger("main")!!
