package org.datadozer.search

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.datadozer.addKeyValue
import org.datadozer.models.OperationMessage
import org.datadozer.models.OperationStatus
import org.datadozer.toException

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

class OperationMessageErrorStrategy : DefaultErrorStrategy() {
    /** Instead of recovering from exception `e`, re-throw it wrapped
     * in a [ParseCancellationException] so it is not caught by the
     * rule function catches.  Use [Exception.getCause] to get the
     * original [RecognitionException].
     */
    override fun recover(recognizer: Parser, e: RecognitionException?) {
        var context: ParserRuleContext? = recognizer.context
        while (context != null) {
            context.exception = e
            context = context.getParent()
        }

        throw ParseCancellationException(e)
    }

    /** Make sure we don't attempt to recover from problems in subrules.  */
    override fun sync(recognizer: Parser) {}

    /**
     * Returns a new operation message for the parsing errors
     */
    private fun getOperationMessage(errorType: String,
                                    line: Int,
                                    charPositionInLine: Int,
                                    offendingToken: String,
                                    expectedTokens: String,
                                    underlineError: String,
                                    helpText: String): OperationMessage {
        val error = """Query parsing error: $errorType at $line:$charPositionInLine
Expecting token:$expectedTokens Found token:$offendingToken

$underlineError
$helpText
        """
        return OperationMessage.newBuilder()
                .setMessage(error)
                .addKeyValue("line_no", line)
                .addKeyValue("character_position", charPositionInLine)
                .addKeyValue("offending_token", offendingToken)
                .addKeyValue("expected_token", expectedTokens)
                .addKeyValue("underline_error", underlineError)
                .addKeyValue("help_text", helpText)
                .addKeyValue("error_type", errorType)
                .setStatus(OperationStatus.FAILURE)
                .build()
    }

    private fun reportErrorUsingOperationMessage(recognizer: Parser, errorType: String) {
        val offendingToken = getTokenErrorDisplay(recognizer.currentToken)
        val expectedTokens = getExpectedTokens(recognizer).toString(recognizer.vocabulary)
        val line = recognizer.currentToken.line
        val charPositionInLine = recognizer.currentToken.charPositionInLine
        val underlineError = underlineError(recognizer, recognizer.currentToken, line, charPositionInLine)
        val helpText = getHelpTextForToken(expectedTokens)
        throw getOperationMessage(errorType, line, charPositionInLine, offendingToken, expectedTokens, underlineError,
                                  helpText)
                .toException()
    }

    /**
     * This is called by [.reportError] when the exception is a
     * [NoViableAltException].
     *
     * @see .reportError
     *
     *
     * @param recognizer the parser instance
     * @param e the recognition exception
     */
    override fun reportNoViableAlternative(recognizer: Parser,
                                           e: NoViableAltException) {
        reportErrorUsingOperationMessage(recognizer, "No viable alternative")
    }

    /**
     * This is called by [.reportError] when the exception is an
     * [InputMismatchException].
     *
     * @see .reportError
     *
     *
     * @param recognizer the parser instance
     * @param e the recognition exception
     */
    override fun reportInputMismatch(recognizer: Parser,
                                     e: InputMismatchException) {
        reportErrorUsingOperationMessage(recognizer, "Input mismatch")
    }

    /**
     * This method is called to report a syntax error which requires the
     * insertion of a missing token into the input stream. At the time this
     * method is called, the missing token has not yet been inserted. When this
     * method returns, {@code recognizer} is in error recovery mode.
     *
     * <p>This method is called when {@link #singleTokenInsertion} identifies
     * single-token insertion as a viable recovery strategy for a mismatched
     * input error.</p>
     *
     * <p>The default implementation simply returns if the handler is already in
     * error recovery mode. Otherwise, it calls {@link #beginErrorCondition} to
     * enter error recovery mode, followed by calling
     * {@link Parser#notifyErrorListeners}.</p>
     *
     * @param recognizer the parser instance
     */
    override fun reportMissingToken(recognizer: Parser) {
        reportErrorUsingOperationMessage(recognizer, "Missing token")
    }

    /**
     * This method is called to report a syntax error which requires the removal
     * of a token from the input stream. At the time this method is called, the
     * erroneous symbol is current `LT(1)` symbol and has not yet been
     * removed from the input stream. When this method returns,
     * `recognizer` is in error recovery mode.
     *
     *
     * This method is called when [.singleTokenDeletion] identifies
     * single-token deletion as a viable recovery strategy for a mismatched
     * input error.
     *
     *
     * The default implementation simply returns if the handler is already in
     * error recovery mode. Otherwise, it calls [.beginErrorCondition] to
     * enter error recovery mode, followed by calling
     * [Parser.notifyErrorListeners].
     *
     * @param recognizer the parser instance
     */
    override fun reportUnwantedToken(recognizer: Parser) {
        reportErrorUsingOperationMessage(recognizer, "Unwanted token")
    }

    /**
     * Underline the point at which the error occurs in the input string.
     * This is useful for the end user as it makes it easy to find the errors.
     */
    private fun underlineError(recognizer: Recognizer<*, *>?,
                               offendingToken: Token, line: Int,
                               charPositionInLine: Int): String {
        val tokens = recognizer?.inputStream as CommonTokenStream
        val input = tokens.tokenSource.inputStream.toString()
        val lines = input.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val errorLine = lines[line - 1]
        val sb = StringBuilder()
        val widthOfLineNumber = when {
            line < 10 -> 1
            line < 100 -> 2
            else -> 3
        }

        sb.appendln("$line|    $errorLine")
        for (i in 0 until charPositionInLine + widthOfLineNumber) {
            sb.append(" ")
        }
        val start = offendingToken.startIndex
        val stop = offendingToken.stopIndex
        for (i in 0 until widthOfLineNumber + 4) {
            sb.append(" ")
        }

        if (start >= 0 && stop >= 0) {
            if (start > stop) {
                for (i in stop until start) {
                    sb.append("^")
                }
            } else {
                for (i in start..stop) {
                    sb.append("^")
                }
            }
        }

        return sb.toString()
    }
}
