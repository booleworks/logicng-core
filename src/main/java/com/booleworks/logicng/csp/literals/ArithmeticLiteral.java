package com.booleworks.logicng.csp.literals;

import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.Set;

public abstract class ArithmeticLiteral implements CspLiteral {
    public abstract Set<IntegerVariable> getVariables();
}
