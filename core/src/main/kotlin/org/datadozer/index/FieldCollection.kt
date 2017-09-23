package org.datadozer.index

import org.datadozer.index.fields.FieldSchema

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
 * A list and map combination designed for holding the fields schemas. This
 * will provide both fast iteration and lookup over the fields schema values.
 * NOTE: This is not a general purpose collection.
 */
class FieldCollection : Map<String, FieldSchema>, Collection<FieldSchema> {
    private val map = HashMap<String, FieldSchema>()
    private val arrayList = ArrayList<FieldSchema>()
    private var locked = false

    /**
     * Adds the specified element to the collection.
     *
     * @return `true` if the element has been added, `false` if the collection does not support duplicates
     * and the element is already contained in the collection.
     */
    fun add(element: FieldSchema) {
        if (locked) {
            throw IllegalStateException("Elements cannot be added to collection after it is locked")
        }

        if (!map.containsKey(element.schemaName)) {
            map.put(element.schemaName, element)
            arrayList.add(element)
        }
    }

    /**
     * Locks the object and doesn't allow any further modification
     */
    fun immutable() {
        locked = true
    }

    /**
     * Returns the size of the collection.
     */
    override val size: Int
        get() = arrayList.size

    /**
     * Returns `true` if the collection is empty (contains no elements), `false` otherwise.
     */
    override fun isEmpty(): Boolean {
        return arrayList.isEmpty()
    }

    /**
     * Returns a read-only [Set] of all key/value pairs in this map.
     */
    override val entries: Set<Map.Entry<String, FieldSchema>>
        get() = map.entries
    /**
     * Returns a read-only [Set] of all keys in this map.
     */
    override val keys: Set<String>
        get() = map.keys

    /**
     * Returns a read-only [Collection] of all values in this map. Note that this collection may contain duplicate values.
     */
    override val values: Collection<FieldSchema>
        get() = arrayList

    /**
     * Returns `true` if the map contains the specified [key].
     */
    override fun containsKey(key: String): Boolean {
        return map.containsKey(key)
    }

    /**
     * Returns `true` if the map maps one or more keys to the specified [value].
     */
    override fun containsValue(value: FieldSchema): Boolean {
        return arrayList.contains(value)
    }

    /**
     * Returns the value corresponding to the given [key], or `null` if such a key is not present in the map.
     */
    override fun get(key: String): FieldSchema? {
        return map[key]
    }

    fun get(index: Int): FieldSchema {
        if (index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }

        return arrayList[index]
    }

    override fun contains(element: FieldSchema): Boolean {
        return arrayList.contains(element)
    }

    override fun containsAll(elements: Collection<FieldSchema>): Boolean {
        return arrayList.containsAll(elements)
    }

    override fun iterator(): Iterator<FieldSchema> {
        return arrayList.iterator()
    }
}