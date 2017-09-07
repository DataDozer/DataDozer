package org.datadozer

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
 * This class is responsible for loading the system wide writerSettings. It is
 * a simple wrapper around the properties class but exposes some methods
 * to perform typed property retrieval.
 */
class Settings {
    private val properties = Properties()

    init {
        val input = this.javaClass.classLoader.getResourceAsStream("settings.properties")
        properties.load(input)
    }

    private fun <T> getValue(propertyName: String, defaultValue: T, parser: (String) -> T): T {
        val value = properties.getProperty(propertyName)
        return if (value == null) {
            defaultValue
        } else {
            tryParseWith(parser, value, defaultValue)
        }
    }

    /**
     * Returns integer or default value
     */
    fun getIntOrDefault(propertyName: String, defaultValue: Int): Int {
        return getValue(propertyName, defaultValue, String::toInt)
    }

    /**
     * Returns long or default value
     */
    fun getLongOrDefault(propertyName: String, defaultValue: Long): Long {
        return getValue(propertyName, defaultValue, String::toLong)
    }

    /**
     * Returns boolean or default value
     */
    fun getBooleanOrDefault(propertyName: String, defaultValue: Boolean): Boolean {
        return getValue(propertyName, defaultValue, String::toBoolean)
    }

    /**
     * Returns string or default value
     */
    fun getStringOrDefault(propertyName: String, defaultValue: String): String {
        return properties.getProperty(propertyName, defaultValue)
    }
}