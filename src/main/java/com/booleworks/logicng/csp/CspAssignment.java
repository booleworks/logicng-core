package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class CspAssignment {
    private final Map<IntegerVariable, Integer> integerAssignments = new TreeMap<>();
    private final SortedSet<Variable> posBooleans = new TreeSet<>();
    private final SortedSet<Literal> negBooleans = new TreeSet<>();

    public CspAssignment() {}

    public Map<IntegerVariable, Integer> getIntegerAssignments() {
        return Collections.unmodifiableMap(integerAssignments);
    }

    public SortedSet<Variable> positiveBooleans() {
        return Collections.unmodifiableSortedSet(posBooleans);
    }

    public SortedSet<Literal> negativeBooleans() {
        return Collections.unmodifiableSortedSet(negBooleans);
    }

    public void addLiteral(final Literal lit) {
        if (lit.phase()) {
            posBooleans.add(lit.variable());
        } else {
            negBooleans.add(lit);
        }
    }

    public void addPos(final Variable var) {
        posBooleans.add(var);
    }

    public void addNeg(final Literal lit) {
        negBooleans.add(lit);
    }

    public void addIntAssignment(final IntegerVariable var, final int value) {
        integerAssignments.put(var, value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        final CspAssignment that = (CspAssignment) o;

        if (!integerAssignments.equals(that.integerAssignments)) {return false;}
        if (!posBooleans.equals(that.posBooleans)) {return false;}
        return negBooleans.equals(that.negBooleans);
    }

    @Override
    public int hashCode() {
        int result = integerAssignments.hashCode();
        result = 31 * result + posBooleans.hashCode();
        result = 31 * result + negBooleans.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CspAssignment{" +
                "integerAssignments=" + integerAssignments +
                ", posBooleans=" + posBooleans +
                ", negBooleans=" + negBooleans +
                '}';
    }
}
