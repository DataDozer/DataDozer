// Default query parser for DataDozer
grammar FlexQuery;

@header {
package org.datadozer.parser;
}

statement
    : (query | group)+ EOF;

/*
Simple clauses which can contain multiple values
*/
query
    : occur=(PLUS|MINUS)? fieldName=ID? COLON queryName=ID
        BRACE_OPEN
            value (COMMA value)* (COMMA properties)?
        BRACE_CLOSE
    ;

/*
In group clause the field name is optional as you can get it from the
name of the group.
*/
group
    : (SQUARE_OPEN groupName=ID (COLON groupQuery=ID)? SQUARE_CLOSE)?
        CURLY_OPEN
            (query | group)+ properties?
        CURLY_CLOSE;

value
    : occur=(PLUS|MINUS)?
        ( STRING
        | FLOAT
        | NUMBER
        | AT variableName=ID
        | TRUE
        | FALSE
        )
      properties?
    ;

property
    : ID propertyName=EQ propertyValue=(NUMBER | STRING);

properties
    : SQUARE_OPEN property (COMMA property)* SQUARE_CLOSE;

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
SQUARE_OPEN     : '[';
SQUARE_CLOSE    : ']';
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

