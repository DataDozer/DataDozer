package org.datadozer.search

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.apache.lucene.search.Query
import org.datadozer.parser.FlexQueryBaseListener
import org.datadozer.parser.FlexQueryLexer
import org.datadozer.parser.FlexQueryParser
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

fun parseQueryString(query: String): FlexQueryParser.StatementContext {
    val lexer = FlexQueryLexer(CharStreams.fromReader(StringReader(query)))
    lexer.removeErrorListeners()
    val tokens = CommonTokenStream(lexer)
    val parser = FlexQueryParser(tokens)
    parser.removeErrorListeners()
    parser.errorHandler = OperationMessageErrorStrategy()
    return parser.statement()
}

fun getQuery(context: FlexQueryParser.StatementContext?): Query? {
    val walker = ParseTreeWalker()
    val listener = QueryListener()
    walker.walk(listener, context)
    return listener.query
}

/**
 * Generates a lucene query by listening to the ANTLR parser events
 */
class QueryListener : FlexQueryBaseListener() {
    var query: Query? = null
        private set(value) {
            field = value
        }

    /**
     * Enter a parse tree produced by [FlexQueryParser.statement].
     * @param ctx the parse tree
     */
    override fun enterStatement(ctx: FlexQueryParser.StatementContext?) {
    }

    /**
     * Exit a parse tree produced by [FlexQueryParser.statement].
     * @param ctx the parse tree
     */
    override fun exitStatement(ctx: FlexQueryParser.StatementContext?) {

    }

    /**
     * Enter a parse tree produced by [FlexQueryParser.query].
     * @param ctx the parse tree
     */
    override fun enterQuery(ctx: FlexQueryParser.QueryContext?) {

    }

    /**
     * Exit a parse tree produced by [FlexQueryParser.query].
     * @param ctx the parse tree
     */
    override fun exitQuery(ctx: FlexQueryParser.QueryContext?) {

    }

    /**
     * Enter a parse tree produced by [FlexQueryParser.group].
     * @param ctx the parse tree
     */
    override fun enterGroup(ctx: FlexQueryParser.GroupContext?) {

    }

    /**
     * Exit a parse tree produced by [FlexQueryParser.group].
     * @param ctx the parse tree
     */
    override fun exitGroup(ctx: FlexQueryParser.GroupContext?) {

    }
}
