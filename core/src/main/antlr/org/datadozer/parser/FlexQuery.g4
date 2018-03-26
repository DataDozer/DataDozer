// Default query parser for DataDozer
grammar FlexQuery;

@header {
package org.datadozer.parser;
}

// LEXER
/**
NOTE: The order of lexer rules matter. Make sure more specific rules are at
the top followed by more generic rules/ So things like string literal matches
should be above rules like ID.
*/

// Separators
COMMA           : ',';
DOT             : '.';
BRACE_OPEN      : '(';
BRACE_CLOSE     : ')';
CURLY_OPEN      : '{';
CURLY_CLOSE     : '}';
SQUARE_OPEN     : '[';
SQUARE_CLOSE    : ']';
PLUS            : '+';
MINUS           : '-';
TILDE           : '~';
AT              : '@';
COLON           : ':';
EQ              : '=';
POWER           : '^';
PIPE            : '|';

// Keywords
TRUE            : 'true';
FALSE           : 'false';

QUERY_NAME
    : 'ordered'
    | 'term'
    ;

GROUP_QUERY_NAME
    : 'span_near'
    | 'span_or'
    ;

PROPERTY_NAME
    : 'startat'
    | 'endat'
    | 'ordered'
    | 'unordered'
    | 'slop'
    | 'boost'
    | 'noscore'
    | 'score'
    | 'spanquery'
    | 'defaultfield'
    | 'matchall'
    | 'matchnone'
    | 'matchdefault'
    | 'fallback'
    ;

FIELD_NAME      : ID ;

WS              : [ \t\r\n]+ -> skip ;  // skip spaces, tabs, newlines

EXPRESSION      : '(' WS? ')' WS? '=>' ('\\;'|.)*? ';';
STRING          : '\'' ('\\\''|.)*? '\'';
NUMBER          : DIGIT+;

FLOAT           : DIGIT+ '.' DIGIT*   // match 1.39
                |        '.' DIGIT+   // match .1
                ;

fragment
DIGIT           : '0' .. '9';

fragment
ID              : [a-z_]+ ; // match lower-case identifiers


// PARSER

statement
    : (query | group)+ EOF;

/*
Simple clauses which can contain multiple values
*/
query
    : occur=(PLUS|MINUS)? fieldName=FIELD_NAME? COLON queryName=QUERY_NAME
        BRACE_OPEN
            value (COMMA value)*
        BRACE_CLOSE property*
    ;

/*
In group clause the field name is optional as you can get it from the
name of the group.
[fieldname:queryname] {
    query or groups
}
*/
group
    :  occur=(PLUS|MINUS)? (SQUARE_OPEN FIELD_NAME (COLON GROUP_QUERY_NAME)? SQUARE_CLOSE)?
        CURLY_OPEN
            (query | group)+
        CURLY_CLOSE property*;

value
    : occur=(PLUS|MINUS)?
        ( STRING
        | FLOAT
        | NUMBER
        | AT variableName=ID
        | TRUE
        | FALSE
        )
      property*
    ;

property
    : POWER NUMBER
    | TILDE NUMBER
    | EQ NUMBER
    | PIPE propertyName=PROPERTY_NAME EQ propertyValue=(NUMBER | STRING | EXPRESSION);

properties
    : property*;
