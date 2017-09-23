package org.datadozer

import org.datadozer.models.OperationMessage
import org.datadozer.models.OperationStatus

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
 * An exception that is thrown to signify validation errors. This is more
 * performant than standard exception as we have overridden the fillInStackTrace.
 * In most of the validation exceptions we don't really care about the StackTrace.
 *
 * NOTE: After carefully considering various options like ResultType and error codes
 * for control flow, I feel this is the best and easiest option. I also get the feeling
 * that Java is designed to optimize the use of exceptions as I can't find any tryParse
 * method in standard library.
 *
 * Some of the rationale behind this strategy comes from the following articles:
 *
 * https://shipilev.net/blog/2014/exceptional-performance/
 * http://www.drmaciver.com/2009/03/exceptions-for-control-flow-considered-perfectly-acceptable-thanks-very-much/
 *
 */
class OperationException(val operationMessage: OperationMessage) : Exception(operationMessage.message) {
    @Synchronized
    override fun fillInStackTrace(): Throwable {
        return this
    }

    override fun toString(): String {
        return operationMessage.toString()
    }

    override val message: String?
        get() = operationMessage.toString()
}

/**
 * Throws an [OperationException] with the result of calling [lazyMessage] if the [value] is false.
 */
inline fun check(value: Boolean, lazyMessage: () -> OperationMessage) {
    if (!value) {
        throw OperationException(lazyMessage())
    }
}

/**
 * Throws an [OperationException] with the result of calling [lazyMessage]  if the [value] is null. Otherwise
 * returns the not null value.
 */
inline fun <T : Any> checkNotNull(value: T?, lazyMessage: () -> OperationMessage): T {
    if (value == null) {
        val message = lazyMessage()
        throw OperationException(message)
    } else {
        return value
    }
}

/**
 * Wrapper around general validation method which returns boolean instead of Exception
 */
fun tryValidate(validation: () -> Unit): Boolean {
    return try {
        validation()
        true
    } catch (e: OperationException) {
        false
    }
}

inline fun <T> tryParse(parser: (String) -> T, value: String, defaultValue: T): Pair<Boolean, T> {
    return try {
        val v = parser(value)
        Pair(true, v)
    } catch (e: OperationException) {
        Pair(false, defaultValue)
    }
}

inline fun <T> tryParseWith(parser: (String) -> T, value: String, defaultValue: T): T {
    return try {
        parser(value)
    } catch (e: OperationException) {
        defaultValue
    }
}

fun <T : Comparable<T>> greaterThan(fieldName: String, lowerLimit: T, value: T) {
    if (value <= lowerLimit) {
        throw OperationException(
                OperationMessage.newBuilder()
                        .setMessage("Field '$fieldName' must be greater than '$lowerLimit', but found '$value'.")
                        .setDetails("field_name='$fieldName',lower_limit='$lowerLimit',value='$value'")
                        .setStatus(OperationStatus.FAILURE)
                        .build())
    }
}

fun <T : Comparable<T>> greaterThanEqual(fieldName: String, lowerLimit: T, value: T) {
    if (value < lowerLimit) {
        throw OperationException(
                OperationMessage.newBuilder()
                        .setMessage(
                                "Field '$fieldName' must be greater than or equal to '$lowerLimit', but found '$value'")
                        .setDetails("field_name='$fieldName',lower_limit='$lowerLimit',value='$value'")
                        .setStatus(OperationStatus.FAILURE)
                        .build())
    }
}

fun <T : Comparable<T>> lessThan(fieldName: String, upperLimit: T, value: T) {
    if (value >= upperLimit) {
        throw OperationException(
                OperationMessage.newBuilder()
                        .setMessage("Field '$fieldName' must be less than '$upperLimit', but found '$value'")
                        .setDetails("field_name='$fieldName',upper_limit='$upperLimit',value='$value'")
                        .setStatus(OperationStatus.FAILURE)
                        .build())
    }
}

fun <T : Comparable<T>> lessThanEqual(fieldName: String, upperLimit: T, value: T) {
    if (value > upperLimit) {
        throw OperationException(
                OperationMessage.newBuilder()
                        .setMessage(
                                "Field '$fieldName' must be less than or equal to '$upperLimit', but found '$value'")
                        .setDetails("field_name='$fieldName',upper_limit='$upperLimit',value='$value'")
                        .setStatus(OperationStatus.FAILURE)
                        .build())
    }
}

/**
 * Checks if the given array has any duplicate values
 */
fun hasDuplicates(groupName: String, fieldName: String, input: Array<String>) {
    if (input.count() != input.distinct().count()) {
        throw OperationException(
                OperationMessage.newBuilder()
                        .setMessage("A duplicate entry '$fieldName' has been found in the group '$groupName'")
                        .setDetails("field_name='$fieldName',group_name='$groupName'")
                        .setStatus(OperationStatus.FAILURE)
                        .build())
    }
}

/**
 * Checks if the given string is null or empty
 */
fun notBlank(fieldName: String, value: String) {
    if (value.isBlank()) {
        throw OperationException(OperationMessage.newBuilder()
                                         .setMessage("Field '$fieldName' must not be blank.")
                                         .setDetails("field_name='$fieldName'")
                                         .setStatus(OperationStatus.FAILURE)
                                         .build())
    }
}

private val regex = Regex("^[a-z0-9_]*\$")

/**
 * Validates if the fields name satisfies the naming rules
 */
fun isPropertyName(fieldName: String, value: String) {
    if (!regex.containsMatchIn(value)) {
        throw OperationException(
                OperationMessage.newBuilder()
                        .setMessage(
                                "Name is invalid for fields '$fieldName'. A property name can only contain 'a-z', '0-9' and '_' characters.")
                        .setDetails("field_name='$fieldName'")
                        .setStatus(OperationStatus.FAILURE)
                        .build())
    }
}