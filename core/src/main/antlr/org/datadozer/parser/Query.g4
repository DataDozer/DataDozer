// Default query parser for DataDozer
/*
Complex example for parsing
a(regex "")
s(notempty)
bool(true)
bool(false)
+b(> 1000, < 5000, 23)^23|noanalysis
+a(like "dfdf"=0,+regex "world \"bnmb"~12,(100)^32=12|any 2)^32=12|any 2
c(("germany", "uk")|any 1, +"france")
d(("john kerry"~2, "george bush"~2)~u10)|e 100|s 0
e(@~2, @telephone)|noanalysis
(
    +e(> 2000, < 5000, 23)^23
    f(("germany", "uk")|any 1, +"france")
)

(
    (
        (
            +e(1000, < 5000, 23)^23
            f(("germany", "uk")|any 1, +"france")
        )|e 100^23|any 1

        +e(> 1000, < 5000, 23)^23
        f(("germany", "uk")|any 1, +"france")
    )
    +g(> 1000, < 5000, 23)^23
    h(("germany", "uk")|any 1, +"france")
)|e 100^23|any 1
*/
grammar Query;

@header {
package org.datadozer.parser;
}

query       : clauses+;
clauses     : clause
            | '(' clauses+ ')' property*
            ;

clause      : clause_type* ID '(' condition (',' condition)* ')' property*
            ;

clause_type : PLUS
            | MINUS
            ;

condition   : clause_type* value property*
            | '(' condition (',' condition)* ')' property*
            ;

value       : basic_value
            | function
            | variable
            | TRUE
            | FALSE
            ;

function    : ID STRING?         // regex "" | prefix "" | like "" // empty // nonempty
            | (GT | GTE | LT | LTE) basic_value
            ;

basic_value : STRING
            | FLOAT
            | NUMBER
            ;

property    : (BOOST | EQ | SLOP | UNORDERED) NUMBER
            | '|' ID (STRING | NUMBER)?
            ;

variable    : VARIABLE ID?;

ID          : [a-z_]+ ;             // match lower-case identifiers
WS          : [ \t\r\n]+ -> skip ;  // skip spaces, tabs, newlines

STRING      : '"' (ESC|.)*? '"';
NUMBER      : DIGIT+;

FLOAT       : DIGIT+ '.' DIGIT*   // match 1.39
            |        '.' DIGIT+   // match .1
            ;

TRUE        : 'true';
FALSE       : 'false';

/*
All these are handled with ID as they are
causing issues with ordering
REGEX       : 'regex';
LIKE        : 'like';
PREFIX      : 'prefix';
EMPTY       : 'empty';
NOT_EMPTY   : 'notempty';
*/

UNORDERED   : '~u';
COMMA       : ',';
LT          : '<';
LTE         : '<=';
GT          : '>';
GTE         : '>=';
EQ          : '=';
NEQ         : '!=';
BRACE_OPEN  : '(';
BRACE_CLOSE : ')';
PLUS        : '+';
MINUS       : '-' | 'not';
BOOST       : '^';
SLOP        : '~';
PIPE        : '|';
VARIABLE    : '@';

fragment
DIGIT       : '0' .. '9';

fragment
ESC         : '\\"' | '\\\\';
