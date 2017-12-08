package org.datadozer.search

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.DiagnosticErrorListener
import org.antlr.v4.runtime.atn.PredictionMode
import org.datadozer.OperationException
import org.datadozer.assertEqualsIgnoreNewlineStyle
import org.datadozer.parser.FlexQueryLexer
import org.datadozer.parser.FlexQueryParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.StringReader

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

class GrammarTests {
    //@Test
    fun `Grammar should have ambiguity`() {
        val query = ""
        val lexer = FlexQueryLexer(CharStreams.fromReader(StringReader(query)))
        lexer.removeErrorListeners()
        lexer.addErrorListener(DiagnosticErrorListener())
        val tokens = CommonTokenStream(lexer)
        val parser = FlexQueryParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(DiagnosticErrorListener())
        parser.interpreter.predictionMode = PredictionMode.LL_EXACT_AMBIG_DETECTION
        //parser.stat()
    }
}

class ParseQueryStringTests {
    @ParameterizedTest
    @ValueSource(strings = arrayOf(
            "fn:ordered('')",
            "fn:ordered('seemant'|boost=32)",
            "fn:ordered('seemant'^32)"))
    fun `query string will parse`(sut: String) {
        parseQueryString(sut)
    }
}

class FailureTests {
    private fun parserError(queryString: String,
                            lineNumber: Int,
                            position: Int,
                            offendingToken: String,
                            expectedToken: String,
                            underlineError: String?) {
        try {
            parseQueryString(queryString)
            assertEquals(false, "Query string should not be parsed.")
        } catch (oe: OperationException) {
            println(oe.operationMessage.message)
            assertEquals(lineNumber.toString(), oe.operationMessage.getDetails(0).value, "Line Number")
            assertEquals(position.toString(), oe.operationMessage.getDetails(1).value, "Position")
            assertEquals(offendingToken, oe.operationMessage.getDetails(2).value, "Offending Token")
            assertEquals(expectedToken, oe.operationMessage.getDetails(3).value, "Expected Token")
            if (underlineError != null) {
                assertEqualsIgnoreNewlineStyle(underlineError, oe.operationMessage.getDetails(4).value.trim(),
                                               "Underline Error")
            }
        }
    }

    @Test
    fun `missing colon will result in error`() {
        val errorMessage = """
1|    a('b')
       ^
""".trim()

        parserError("a('b')", 1, 1, "'('", "':'", errorMessage)
    }

    @Test
    fun `missing query name will result in error`() {
        val underlineError = """
1|    a:('b')
        ^""".trim()
        parserError("a:('b')", 1, 2, "'('", "QUERY_NAME", underlineError)
        parserError("a:      ('b')", 1, 8, "'('", "QUERY_NAME", null)
    }

    @Test
    fun `missing closing bracket will result in error`() {
        val underlineError = """
1|    a:term('b'
                ^""".trim()
        parserError("a:term('b'", 1, 10, "'<EOF>'", "')'", underlineError)

    }

    @Test
    fun `missing closing bracket after comma will result in error`() {
        val underlineError = """
1|    a:term('b',
                 ^""".trim()
        parserError("a:term('b',", 1, 11, "'<EOF>'", "{'@', 'true', 'false', STRING, NUMBER, FLOAT}", underlineError)

    }
}