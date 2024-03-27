package com.booleworks.logicng.csp.literals;

import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.Map;
import java.util.Set;

public abstract class ArithmeticLiteral implements CspLiteral, Comparable<ArithmeticLiteral> {
    public abstract Set<IntegerVariable> getVariables();

    public abstract int[] getBound(IntegerVariable v, Map<IntegerVariable, IntegerVariable> restrictions);

    public abstract ArithmeticLiteral substitute(Map<IntegerVariable, IntegerVariable> assignment);
}
