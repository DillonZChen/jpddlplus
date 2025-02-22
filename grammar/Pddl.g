
/* Grammar pddl 3.0. Starting to update towards pddl 3.1*/


grammar Pddl;
options {
    output=AST;
    backtrack=true;
    memoize=true;
    //k=4;
}

tokens {
    DOMAIN;
    DOMAIN_NAME;
    REQUIREMENTS;
    TYPES;
    EITHER_TYPE;
    CONSTANTS;
    FUNCTIONS;
    FREE_FUNCTIONS;
    PREDICATES;
    ACTION;
    EVENT;
    PROCESS;
    CONSTRAINT;
    GLOBAL_CONSTRAINT;
    DURATIVE_ACTION;
    PROBLEM;
    PROBLEM_NAME;
    PROBLEM_DOMAIN;
    OBJECTS;
    INIT;
    FUNC_HEAD;
    PRECONDITION;
    EFFECT;
    FORMULAINIT;/*This is to represent a belief instead of a fully specified state*/
    BELIEF;
    AND_GD;
    OR_GD;
	NOT_GD;
	IMPLY_GD;
	EXISTS_GD;
	FORALL_GD;
	COMPARISON_GD;
	AND_EFFECT;
	FORALL_EFFECT;
	WHEN_EFFECT;
	ASSIGN_EFFECT;
	NOT_EFFECT;
	PRED_HEAD;
	GOAL;
	BINARY_OP;
	EQUALITY_CON;
	MULTI_OP;
	MINUS_OP;
	UNARY_MINUS;
	SIN;
	ASIN;
	COS;
	ACOS;
	ATAN;
	TAN;
	ABS;
	INIT_EQ;
	INIT_AT;
	NOT_PRED_INIT;
	PRED_INST;
	PROBLEM_CONSTRAINT;
	PROBLEM_METRIC;
	//UNCERTAINTY
	UNKNOWN;
	ONEOF;
}


@header {
package com.hstairs.ppmajal.parser;
}
@lexer::header { package com.hstairs.ppmajal.parser; }


@parser::members {
private boolean wasError = false;
public void reportError(RecognitionException e) {
	wasError = true;
	super.reportError(e);
}
public boolean invalidGrammar() {
	return wasError;
}
}
// Standard way of disabling the default error handler, and throwing Exceptions instead:
//@rulecatch { }
//@members {
// // raise exception, rather than recovering, on mismatched token within alt
// protected void mismatch(IntStream input, int ttype, BitSet follow)
//   throws RecognitionException
// {
//   throw new MismatchedTokenException(ttype, input);
// }
//}




/************* Start of grammar *******************/

pddlDoc : domain | problem;

/************* DOMAINS ****************************/

domain
    : '(' 'define' domainName
      requireDef?
      typesDef?
      constantsDef?
      predicatesDef?
      functionsDef?
      free_functionsDef?
      constraints?
      structureDef*
      ')'
      -> ^(DOMAIN domainName requireDef? typesDef?
                constantsDef? predicatesDef? functionsDef? free_functionsDef?
                constraints? structureDef*)
    ;

free_functionsDef
	: '(' ':free_functions' functionList ')'
	-> ^(FREE_FUNCTIONS functionList)
	;

domainName
    : '(' 'domain' NAME ')'
    	-> ^(DOMAIN_NAME NAME)
    ;

requireDef
	: '(' ':requirements' REQUIRE_KEY+ ')'
	-> ^(REQUIREMENTS REQUIRE_KEY+)
	;

typesDef
	: '(' ':types' typedNameList ')'
	  -> ^(TYPES typedNameList)
	;

// If have any typed names, they must come FIRST!
typedNameList
    : (NAME* | singleTypeNameList+ NAME*)
    ;

singleTypeNameList
    : (NAME+ '-' t=type)
	  -> ^(NAME $t)+
	;

type
	: ( '(' 'either' primType+ ')' )
	  -> ^(EITHER_TYPE primType+) {new String("debug")}
	| primType
	;

primType : NAME ;

functionsDef
	: '(' ':functions' functionList ')'
	-> ^(FUNCTIONS functionList)
	;

functionList
	: (atomicFunctionSkeleton+ ('-' functionType)? )*
	;

atomicFunctionSkeleton
	: '('! functionSymbol^ typedVariableList ')'!
	;

functionSymbol : NAME | '#t';

functionType : 'number' ; // Currently in PDDL only numeric functions are allowed

constantsDef
	: '(' ':constants' typedNameList ')'
	-> ^(CONSTANTS typedNameList)
	;

predicatesDef
	: '(' ':predicates' atomicFormulaSkeleton+ ')'
	-> ^(PREDICATES atomicFormulaSkeleton+)
	;

atomicFormulaSkeleton
	: '('! predicate^ typedVariableList ')'!
	;

predicate : NAME ;

// If have any typed variables, they must come FIRST!
typedVariableList
    : (VARIABLE* | singleTypeVarList+ VARIABLE*)
    ;

singleTypeVarList
    : (VARIABLE+ '-' t=type)
      -> ^(VARIABLE $t)+
    ;

constraints
	: '('! ':constraints'^ conGD ')'!
	;

structureDef
	: actionDef
	| durativeActionDef
	| derivedDef
	| constraintDef
	| processDef
	| eventDef
	;


/************* ACTIONS ****************************/

actionDef
	: '(' ':action' actionSymbol
	      (':parameters'  '(' typedVariableList ')')?
           actionDefBody ')'
       -> ^(ACTION actionSymbol typedVariableList? actionDefBody)
    ;
eventDef
	: '(' ':event' actionSymbol
	      ':parameters'  '(' typedVariableList ')'
           actionDefBody ')'
       -> ^(EVENT actionSymbol typedVariableList? actionDefBody)
    ;
processDef
	: '(' ':process' actionSymbol
	      ':parameters'  '(' typedVariableList ')'
           actionDefBody ')'
       -> ^(PROCESS actionSymbol typedVariableList? actionDefBody)
    ;

constraintDef
	: '(' ':constraint' constraintSymbol
	      ':parameters'  '(' typedVariableList ')'
           constraintDefBody ')'
       -> ^(GLOBAL_CONSTRAINT constraintSymbol typedVariableList? constraintDefBody)
    ;


actionSymbol : NAME ;

constraintSymbol : NAME ;

// Should allow preGD instead of goalDesc for preconditions -
// but I can't get the LL(*) parsing to work
// This means 'preference' preconditions cannot be used
actionDefBody
	: ( ':precondition' (('(' ')') | goalDesc))?
	  ( ':effect' (('(' ')') | effect))?
	  -> ^(PRECONDITION goalDesc?) ^(EFFECT effect?)
	;

constraintDefBody
	: ( ':condition' (('(' ')') | goalDesc))?
	  -> ^(PRECONDITION goalDesc?)
	;


//preGD
//	: prefGD
//	| '(' 'and' preGD* ')'
//	| '(' 'forall' '(' typedVariableList ')' preGD ')'
//	;
//
//prefGD
//	: '(' 'preference' NAME? goalDesc ')'
//	| goalDesc
//	;

belief
	: goalDesc
	  initEl* -> ^(BELIEF goalDesc initEl*)
;

goalDesc
	: atomicTermFormula
	| '(' 'and' goalDesc* ')'
	          -> ^(AND_GD goalDesc*)
	| '(' 'or' goalDesc* ')'
	          -> ^(OR_GD goalDesc*)
	| '(' 'not' goalDesc ')'
	          -> ^(NOT_GD goalDesc)
	| '(' 'oneof'  goalDesc* ')'  -> ^(ONEOF goalDesc*)
	| '(' 'imply' goalDesc goalDesc ')'
	          -> ^(IMPLY_GD goalDesc goalDesc)
	| '(' 'exists' '(' typedVariableList ')' goalDesc ')'
	          -> ^(EXISTS_GD typedVariableList goalDesc)
	| '(' 'forall' '(' typedVariableList ')' goalDesc ')'
	          -> ^(FORALL_GD typedVariableList goalDesc)
    | fComp
              -> ^(COMPARISON_GD fComp)
	| equality 
			  -> ^(EQUALITY_CON equality)
    ;

equality
	: '('! '=' term term ')'!
	;
fComp
	: '('! binaryComp fExp fExp ')'!
	;

atomicTermFormula
	: '(' predicate term* ')' -> ^(PRED_HEAD predicate term*)
	;

term : NAME | VARIABLE;

/************* DURATIVE ACTIONS ****************************/

durativeActionDef
	: '(' ':durative-action' actionSymbol
	      ':parameters'  '(' (typedVariableList)? ')'
           daDefBody ')'
       -> ^(DURATIVE_ACTION actionSymbol typedVariableList daDefBody)
    ;

daDefBody
	: ':duration' durationConstraint
	| ':condition' (('(' ')') | daGD)
    | ':effect' (('(' ')') | daEffect)
    ;

daGD
	: prefTimedGD
	| '(' 'and' daGD* ')'
	| '(' 'forall' '(' typedVariableList ')' daGD ')'
	;

prefTimedGD
	: timedGD
	| '(' 'preference' NAME? timedGD ')'
	;

timedGD
	: '(' '__at__' timeSpecifier goalDesc ')'
	| '(' 'over' interval goalDesc ')'
	;

timeSpecifier : 'start' | 'end' ;
interval : 'all' ;

/************* DERIVED DEFINITIONS ****************************/

derivedDef
	: '('! ':derived'^ typedVariableList goalDesc ')'!
	;

/************* EXPRESSIONS ****************************/

fExp
	: NUMBER
	| '(' binaryOp fExp fExp2 ')' -> ^(BINARY_OP binaryOp fExp fExp2)
	| '(' '-' fExp ')' -> ^(UNARY_MINUS fExp)
	| '(' 'sin' fExp ')' -> ^(SIN fExp)
	| '(' 'cos' fExp ')' -> ^(COS fExp)
	| '(' 'asin' fExp ')' -> ^(ASIN fExp)
	| '(' 'acos' fExp ')' -> ^(ACOS fExp)
	| '(' 'atan' fExp ')' -> ^(ATAN fExp)
	| '(' 'tan' fExp ')' -> ^(TAN fExp)
	| '(' 'abs' fExp ')' -> ^(ABS fExp)
	| fHead
	;

// This is purely a workaround for an ANTLR bug in tree construction
// http://www.antlr.org/wiki/display/ANTLR3/multiple+occurences+of+a+token+mix+up+the+list+management+in+tree+rewrites
fExp2 : fExp ;

fHead
	: '(' functionSymbol term* ')' -> ^(FUNC_HEAD functionSymbol term*)
	| functionSymbol -> ^(FUNC_HEAD functionSymbol)
	;

effect
	: '(' 'and' cEffect* ')' -> ^(AND_EFFECT cEffect*)
	| cEffect
	;

cEffect
	: '(' 'forall' '(' typedVariableList ')' effect ')'
	  -> ^(FORALL_EFFECT typedVariableList effect)
	| '(' 'when' goalDesc condEffect ')'
	  -> ^(WHEN_EFFECT goalDesc condEffect)
	| pEffect
	;

pEffect
	: '(' assignOp fHead fExp ')'
	  -> ^(ASSIGN_EFFECT assignOp fHead fExp)
	| '(' 'not' atomicTermFormula ')'
	  -> ^(NOT_EFFECT atomicTermFormula)
	| atomicTermFormula
	;


// TODO: why is this different from the "and cEffect" above? Does it matter?
condEffect
	: '(' 'and' pEffect* ')' -> ^(AND_EFFECT pEffect*)
	| pEffect
	;

// TODO: should these be uppercase & lexer section?
//binaryOp : '*' | '+' | '-' | '/' | '^'| ;
binaryOp : '*' | '+' | '-' | '/' | '^' | 'atan2';

multiOp	: '*' | '+' ;	

binaryComp : '>' | '<' | '=' | '>=' | '<=' ;

assignOp : 'assign' | 'scale-up' | 'scale-down' | 'increase' | 'decrease' ;


/************* DURATIONS  ****************************/

durationConstraint
	: '(' 'and' simpleDurationConstraint+ ')'
	| '(' ')'
	| simpleDurationConstraint
	;

simpleDurationConstraint
	: '(' durOp '?duration' durValue ')'
	| '(' '__at__' timeSpecifier simpleDurationConstraint ')'
	;

durOp : '<=' | '>=' | '=' ;

durValue : NUMBER | fExp ;

daEffect
	: '(' 'and' daEffect* ')'
	| timedEffect
	| '(' 'forall' '(' typedVariableList ')' daEffect ')'
	| '(' 'when' daGD timedEffect ')'
	| '(' assignOp fHead fExpDA ')'
	;

timedEffect
	: '(' '__at__' timeSpecifier daEffect ')'     // BNF has a-effect here, but not defined anywhere
	| '(' '__at__' timeSpecifier fAssignDA ')'
	| '(' assignOp fHead fExp ')'         // BNF has assign-op-t and f-exp-t here, but not defined anywhere
	;

fAssignDA
	: '(' assignOp fHead fExpDA ')'
	;

fExpDA
	: '(' ((binaryOp fExpDA fExpDA) | ('-' fExpDA)) ')'
	| '?duration'
	| fExp
	;

/************* PROBLEMS ****************************/

problem
	: '(' 'define' problemDecl
	  problemDomain
      requireDef?
      objectDecl?
      init
      goal
      probConstraints?
      metricSpec?
      // lengthSpec? This is not defined anywhere in the BNF spec
      ')'
      -> ^(PROBLEM problemDecl problemDomain requireDef? objectDecl?
      		init goal probConstraints? metricSpec?)
    ;

problemDecl
    : '(' 'problem' NAME ')'
    -> ^(PROBLEM_NAME NAME)
    ;

problemDomain
	: '(' ':domain' NAME ')'
	-> ^(PROBLEM_DOMAIN NAME)
	;

objectDecl
	: '(' ':objects' typedNameList ')'
	-> ^(OBJECTS typedNameList)
	;

init
	: '(' ':init' initEl* ')'
	-> ^(INIT initEl*)
	| '(' ':init' belief ')' -> ^(FORMULAINIT belief)
	;

initEl
	: nameLiteral
	| '(' '=' fHead NUMBER ')'         -> ^(INIT_EQ fHead NUMBER)
// Disabled this	| '(' 'at' NUMBER nameLiteral ')'  -> ^(INIT_AT NUMBER nameLiteral)
	| '(' 'unknown'  atomicNameFormula ')'  -> ^(UNKNOWN atomicNameFormula)
	| '(' 'oneof'  atomicNameFormula* ')'  -> ^(ONEOF atomicNameFormula*)
	| '(' 'or'  nameLiteral* ')'  -> ^(OR_GD nameLiteral*)
	;

nameLiteral
	: atomicNameFormula
	| '(' 'not' atomicNameFormula ')' -> ^(NOT_PRED_INIT atomicNameFormula)
	;

atomicNameFormula
	: '(' predicate NAME* ')' -> ^(PRED_INST predicate NAME*)
	;

// Should allow preGD instead of goalDesc -
// but I can't get the LL(*) parsing to work
// This means 'preference' preconditions cannot be used
//goal : '(' ':goal' preGD ')'  -> ^(GOAL preGD);
goal : '(' ':goal' goalDesc  ')' -> ^(GOAL goalDesc) ;

probConstraints
	: '(' ':constraints'  prefConGD ')'
	  -> ^(PROBLEM_CONSTRAINT prefConGD)
	;

prefConGD
	: '(' 'and' prefConGD* ')'
	| '(' 'forall' '(' typedVariableList ')' prefConGD ')'
	| '(' 'preference' NAME? conGD ')'
	| conGD
	;

metricSpec
	: '(' ':metric' optimization metricFExp ')'
	  -> ^(PROBLEM_METRIC optimization metricFExp)
	;

optimization : 'minimize' | 'maximize' ;

metricFExp
	: '(' binaryOp metricFExp metricFExp ')'
  	  -> ^(BINARY_OP binaryOp metricFExp metricFExp)
	| '(' multiOp metricFExp metricFExp+ ')'
   	  -> ^(MULTI_OP multiOp metricFExp metricFExp+)
	| '(' '-' metricFExp ')'
	  -> ^(MINUS_OP metricFExp )
	| NUMBER
	| fHead
        /*| 'total-time'*/
	| '(' 'is-violated' NAME ')'
	;

/************* CONSTRAINTS ****************************/

conGD
	: '(' 'and' conGD* ')'
	| '(' 'forall' '(' typedVariableList ')' conGD ')'
	| '(' '__at__' 'end' goalDesc ')'
                      | '(' 'always' goalDesc ')'
	| '(' 'sometime' goalDesc ')'
 	| '(' 'within' NUMBER goalDesc ')'
	| '(' 'at-most-once' goalDesc ')'
	| '(' 'sometime-after' goalDesc goalDesc ')'
	| '(' 'sometime-before' goalDesc goalDesc ')'
	| '(' 'always-within' NUMBER goalDesc goalDesc ')'
	| '(' 'hold-during' NUMBER NUMBER goalDesc ')'
	| '(' 'hold-after' NUMBER goalDesc ')'
	;



/************* LEXER ****************************/


REQUIRE_KEY
    : ':'ANY_CHAR*
    ;


NAME:    LETTER ANY_CHAR*  ;

fragment LETTER:	'a'..'z' | 'A'..'Z';

fragment ANY_CHAR: LETTER | '0'..'9' | '-' | '_';

VARIABLE : '?' LETTER ANY_CHAR*  ;

NUMBER : ('-')? DIGIT+ ('.' DIGIT+)? | '#t' ;

fragment DIGIT: '0'..'9';

LINE_COMMENT
    : ';' ~('\n'|'\r')* '\r'? '\n' { $channel = HIDDEN; }
    ;

WHITESPACE
    :   (   ' '
        |   '\t'
        |   '\r'
        |   '\n'
        )+
        { $channel = HIDDEN; }
    ;



