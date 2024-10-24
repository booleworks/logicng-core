// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

grammar LogicNGPropositional;

options {
  superClass = ParserWithFormula;
}

@header {
  package com.booleworks.logicng.io.parsers.antlr;

  import java.util.LinkedHashSet;
  import com.booleworks.logicng.formulas.*;
}

@parser::members {
  public Formula getParsedFormula() {
    return formula().f;
  }
}

formula returns [Formula f]
  : EOF {$f = f.verum();}
  | equiv EOF {$f = $equiv.f;};

constant returns [Formula f]
  : TRUE  {$f = f.verum();}
  | FALSE {$f = f.falsum();};

comparison returns [Formula f]
  : e = add EQ n = NUMBER  {$f = f.pbc(CType.EQ, Integer.parseInt($n.text), $e.literals, $e.coeffs);}
  | e = add LE n = NUMBER  {$f = f.pbc(CType.LE, Integer.parseInt($n.text), $e.literals, $e.coeffs);}
  | e = add LT n = NUMBER  {$f = f.pbc(CType.LT, Integer.parseInt($n.text), $e.literals, $e.coeffs);}
  | e = add GE n = NUMBER  {$f = f.pbc(CType.GE, Integer.parseInt($n.text), $e.literals, $e.coeffs);}
  | e = add GT n = NUMBER  {$f = f.pbc(CType.GT, Integer.parseInt($n.text), $e.literals, $e.coeffs);};

simp returns [Formula f]
  :	NUMBER        {$f = ($NUMBER.text.startsWith("~") ? f.literal($NUMBER.text.substring(1, $NUMBER.text.length()), false) : f.literal($NUMBER.text, true));}
  |	LITERAL       {$f = ($LITERAL.text.startsWith("~") ? f.literal($LITERAL.text.substring(1, $LITERAL.text.length()), false) : f.literal($LITERAL.text, true));}
  |	constant      {$f = $constant.f;}
  | comparison    {$f = $comparison.f;}
  | LBR equiv RBR {$f = $equiv.f;};

lit returns [Formula f]
  :	NOT a = lit {$f = f.not($a.f);}
  |	simp        {$f = $simp.f;};

conj returns [Formula f]
@init{LinkedHashSet<Formula> literals = new LinkedHashSet<>(); }
	:	a = lit {literals.add($a.f);} (AND b = lit {literals.add($b.f);})* {$f = f.and(literals);};

disj returns [Formula f]
@init{LinkedHashSet<Formula> conjunctions = new LinkedHashSet<>();}
  :	a = conj {conjunctions.add($a.f);} (OR b = conj {conjunctions.add($b.f);})* {$f = f.or(conjunctions);};

impl returns [Formula f]
@init{Formula[] operands = new Formula[2];}
  :	a = disj {operands[0] =$a.f;} (IMPL b = impl {operands[1] = $b.f;})? {$f = operands[1] == null ? operands[0] : f.implication(operands[0], operands[1]);};

equiv returns [Formula f]
@init{Formula[] operands = new Formula[2];}
  :	a = impl {operands[0] =$a.f;} (EQUIV b = equiv {operands[1] = $b.f;})? {$f = operands[1] == null ? operands[0] : f.equivalence(operands[0], operands[1]);};

mul returns [Literal l, int c]
  : LITERAL {$l = ($LITERAL.text.startsWith("~") ? f.literal($LITERAL.text.substring(1, $LITERAL.text.length()), false) : f.literal($LITERAL.text, true)); $c = 1;}
  | NUMBER  {$l = f.literal($NUMBER.text, true); $c = 1;}
  | NUMBER MUL LITERAL {$l = ($LITERAL.text.startsWith("~") ? f.literal($LITERAL.text.substring(1, $LITERAL.text.length()), false) : f.literal($LITERAL.text, true)); $c = Integer.parseInt($NUMBER.text);}
  | num = NUMBER MUL lt = NUMBER {$l = f.literal($lt.text, true); $c = Integer.parseInt($num.text);};

add returns [List<Literal> literals, List<Integer> coeffs]
@init{$literals = new ArrayList<>(); $coeffs = new ArrayList<>();}
  :	m1 = mul {$literals.add($m1.l); $coeffs.add($m1.c);} (ADD m2 = mul {$literals.add($m2.l); $coeffs.add($ADD.text.equals("+") ? $m2.c : -$m2.c);})*;


NUMBER   : [\-]?[0-9]+;
LITERAL  : [~]?[A-Za-z0-9_@#][A-Za-z0-9_#]*;
TRUE     : '$true';
FALSE    : '$false';
LBR      : '(';
RBR      : ')';
NOT      : '~';
AND      : '&';
OR       : '|';
IMPL     : '=>';
EQUIV    : '<=>';
MUL      : '*';
ADD      : [+-];
EQ       : '=';
LE       : '<=';
LT       : '<';
GE       : '>=';
GT       : '>';
WS       : [ \t\r\n]+ -> skip;
