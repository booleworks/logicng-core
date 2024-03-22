package com.booleworks.logicng.csp.literals;

import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.Set;

public abstract class ArithmeticLiteral implements CspLiteral, Comparable<ArithmeticLiteral> {
    public abstract Set<IntegerVariable> getVariables();
}
