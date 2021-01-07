grammar Crux;
program
 : declaration_list EOF
 ;

WhiteSpaces
 : [ \t\r\n]+ -> skip
 ;

Comment
 : '//' ~[\r\n]* -> skip
 ;

AND 	:	'and';
OR 		:	'or' ;
NOT 	:	'not';
LET 	:	'let';
VAR 	:	'var';
ARRAY 	:	'array';
FUNC 	:	'func';
IF 		:	'if';
ELSE	: 	'else';
WHILE	:	'while';
TRUE 	:	'true';
FALSE 	:	'false';
RETURN	:	'return';


OPEN_PAREN		:	'(';
CLOSE_PAREN		:	')';
OPEN_BRACE		:	'{';
CLOSE_BRACE		:	'}';
OPEN_BRACKET	:	'[';
CLOSE_BRACKET	:	']';
ADD				:	'+';
SUB				:	'-';
MUL				:	'*';
DIV				:	'/';
GREATER_EQUAL	:	'>=';
LESSER_EQUAL	:	'<=';
NOT_EQUAL		:	'!=';
EQUAL 			:	'==';
GREATER_THAN	:	'>';
LESS_THAN		:	'<';
ASSIGN			:	'=';
COMMA			:	',';
SEMICOLON 		:	';';
COLON 			:	':';
CALL			:	'::';

INTEGER 		: '0' | [1-9] [0-9]*;
IDENTIFIER 	: [a-zA-Z] [a-zA-Z0-9_]* ;




//ERROR

 literal 		:	INTEGER | TRUE | FALSE;
 designator 	:	IDENTIFIER ( OPEN_BRACKET expression0 CLOSE_BRACKET );
 type 			: 	IDENTIFIER;

 op0 : GREATER_EQUAL | LESSER_EQUAL | NOT_EQUAL | EQUAL | GREATER_THAN | LESS_THAN ;
 op1 : ADD | SUB | OR ;
 op2 : MUL | DIV | AND ;

 expression0 	: 	expression1 (op0 expression1) ;
 expression1	:	expression2 	(op1 expression2)*;
 expression2 	:	expression3 	(op2 expression3)*;
 expression3 	:	NOT expression3 | OPEN_PAREN expression0 CLOSE_PAREN | designator | call_expression | literal;

 call_expression : CALL IDENTIFIER OPEN_PAREN expression_list CLOSE_PAREN;
 expression_list :  ( expression0 OPEN_BRACE COMMA expression0 CLOSE_BRACE );

 parameter 		: IDENTIFIER COLON type;
 parameter_list : ( parameter ( COMMA parameter )*  );

 variable_declaration 	: VAR IDENTIFIER COLON type SEMICOLON;
 array_declaration		: ARRAY IDENTIFIER COLON type OPEN_BRACKET INTEGER CLOSE_BRACKET SEMICOLON;
 function_declaration	: FUNC IDENTIFIER OPEN_PAREN parameter_list CLOSE_PAREN ':' type statement_block;

 declaration 			: variable_declaration | array_declaration | function_declaration;

 declaration_list 		: declaration* ;
 assignment_statement 	: LET designator ASSIGN expression0 SEMICOLON;
 call_statement 		: call_expression SEMICOLON;
 if_statement			: IF expression0 statement_block ( ELSE statement_block) ;
 while_statement 		: WHILE expression0 statement_block;
 return_statement 		: RETURN expression0 SEMICOLON;

 statement 			 	
 	:  variable_declaration 
 	| call_statement 
 	| assignment_statement 
 	| if_statement 
 	| while_statement 
 	| return_statement
 ;

 statement_list 		: statement* ;
 statement_block 		: OPEN_BRACE statement_list CLOSE_BRACE ; 



/*
declarationList
 : declaration*
 ;

declaration
 : variableDeclaration
// | arrayDeclaration
// | functionDefinition
 ;

variableDeclaration
 : Var Identifier COLON type SEMICOLON
 ;

type
 : Identifier
 ;

literal
 : Integer
 | True
 | False
 ;

SemiColon: SEMICOLON;
Colon: ':';



True: 'true';
False: 'false';

Var: 'var';

Identifier
 : [a_zA-Z] [a-zA-Z0-9_]*
 ;
 */
