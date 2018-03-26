grammar FunctionCall;

compile
    : expression EOF
    ;

expression
    : LP expression RP
    | ( DECIMAL | VARIABLE | STRING)
    | VARIABLE ( LP (expression (COMMA expression)*)? RP )?
    ;

LP:      '(';
RP:      [)];
COMMA:   [,];

WS: [ \t\n\r]+ -> skip;
VARIABLE: [_$a-zA-Z] [_$a-z.A-Z0-9]*;
STRING    : ['] ( '\\\'' | '\\\\' | ~[\\'] )*? [']
          ;

DECIMAL: ( INTEGER ( [.] [0-9]* )? | [.] [0-9]+ ) ( [eE] [+\-]? [0-9]+ )?;
fragment INTEGER
    : [0]
    | [1-9] [0-9]*
    ;
