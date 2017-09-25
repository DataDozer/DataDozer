// Default query parser for DataDozer
grammar Query;

@header {
package org.datadozer.parser;
}

statement
    : query+ EOF;

query
    : clause
    | group
    ;

/*
Simple clauses which can contain multiple values
*/
clause
    : occur? ID? COLON ID BRACE_OPEN clause_value (COMMA clause_value)* (COMMA keyvalue_pair)* BRACE_CLOSE
    ;

/*
In group clause the field name is optional as you can get it from the
name of the group.
*/
group
    : ID? (COLON ID)? CURLY_OPEN query+ keyvalue_pair* CURLY_CLOSE;

occur
    : PLUS
    | MINUS
    ;

clause_value
    : occur? value keyvalue_pair*
    ;

value
    : STRING
    | FLOAT
    | NUMBER
    | variable
    | TRUE
    | FALSE
    ;

variable
    : AT ID;

keyvalue_pair
    : ID EQ (NUMBER | STRING);

// LEXER
/**
NOTE: The order of lexer rules matter. Make sure more specific rules are at
the top followed by more generic rules/ So things like string literal matches
should be above rules like ID.
*/

// Separators
COMMA           : ',';
BRACE_OPEN      : '(';
BRACE_CLOSE     : ')';
CURLY_OPEN      : '{';
CURLY_CLOSE     : '}';
PLUS            : '+';
MINUS           : '-';
AT              : '@';
COLON           : ':';
EQ              : '=';

// Keywords
TRUE            : 'true';
FALSE           : 'false';

ID              : [a-z_]+ ;             // match lower-case identifiers
WS              : [ \t\r\n]+ -> skip ;  // skip spaces, tabs, newlines

STRING          : '\'' (ESC|.)*? '\'';
NUMBER          : DIGIT+;

FLOAT           : DIGIT+ '.' DIGIT*   // match 1.39
                |        '.' DIGIT+   // match .1
                ;

fragment
DIGIT           : '0' .. '9';

fragment
ESC             : '\\\'' | '\\\\';
