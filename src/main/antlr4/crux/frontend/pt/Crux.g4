grammar Crux;
program
 : declarationList EOF
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

VOID	:	'void' ;
BOOL 	:	TRUE | FALSE;
INTEGER 		: '0' | [1-9] [0-9]*;
IDENTIFIER 		: [a-zA-Z] [a-zA-Z0-9_]* ;


/*
READINT		:	'readInt';
READCHAR	:	'readChar';
PRINTBOOL	:	'printBool';
PRINTINT	:	'printInt';
PRINTCHAR	:	'printChar';
PRINTLN		:	'println';
*/

//ERROR

 literal 		:	INTEGER | TRUE | FALSE;
 designator 	:	IDENTIFIER ( OPEN_BRACKET expression0 CLOSE_BRACKET )*;
 type 			: 	 'void' | 'bool' | 'int';

 op0 : GREATER_EQUAL | LESSER_EQUAL | NOT_EQUAL | EQUAL | GREATER_THAN | LESS_THAN ;
 op1 : ADD | SUB | OR ;
 op2 : MUL | DIV | AND ;

 expression0 	: 	expression1 (op0 expression1)* ;
 expression1	:	expression2 	(op1 expression2)*;
 expression2 	:	expression3 	(op2 expression3)*;
 expression3 	:	NOT expression3 | OPEN_PAREN expression0 CLOSE_PAREN | designator | callExpression | literal;

 callExpression : CALL IDENTIFIER OPEN_PAREN expressionList CLOSE_PAREN ; // | CALL ( READINT | READCHAR | PRINTBOOL | PRINTINT | PRINTCHAR | PRINTLN)
 expressionList : expression0?  (COMMA expression0)* ;

 parameter 		: IDENTIFIER COLON type;
 parameterList : ( parameter? ( COMMA parameter )*  );

 variableDeclaration 	: VAR IDENTIFIER COLON type SEMICOLON;
 arrayDeclaration		: ARRAY IDENTIFIER COLON type OPEN_BRACKET INTEGER CLOSE_BRACKET SEMICOLON;
 functionDefinition	: FUNC IDENTIFIER OPEN_PAREN parameterList CLOSE_PAREN COLON type statementBlock;

 declaration 			: variableDeclaration | arrayDeclaration | functionDefinition;

 declarationList 		: declaration* ;
 assignmentStatement 	: LET designator ASSIGN expression0 SEMICOLON;
 callStatement 		: callExpression SEMICOLON;
 ifStatement			: IF expression0  statementBlock ( ELSE  statementBlock)* ;
 whileStatement 		: WHILE expression0  statementBlock;
 returnStatement 		: RETURN expression0 SEMICOLON;

 statement 			 	
 	:  variableDeclaration 
 	| callStatement 
 	| assignmentStatement 
 	| ifStatement 
 	| whileStatement 
 	| returnStatement
 ;

 statementList 		: statement* ;
 statementBlock 		: OPEN_BRACE statementList CLOSE_BRACE ; 



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
