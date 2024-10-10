package com.booleworks.logicng.csp.datastructures;

import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A class representing an assignment for integer variables and boolean variables. This means a mapping from integer
 * variables to an integer value and from a boolean variable to either {@code true} or {@code false}.
 */
public class CspAssignment {
    private final Map<IntegerVariable, Integer> integerAssignments;
    private final SortedSet<Variable> posBooleans;
    private final SortedSet<Literal> negBooleans;

    /**
     * Constructs a new assignment.
     */
    public CspAssignment() {
        this.integerAssignments = new TreeMap<>();
        this.posBooleans = new TreeSet<>();
        this.negBooleans = new TreeSet<>();
    }

    /**
     * Copies an existing assignment.
     * @param other the existing assigment
     */
    public CspAssignment(final CspAssignment other) {
        this.integerAssignments = new TreeMap<>(other.integerAssignments);
        this.posBooleans = new TreeSet<>(other.posBooleans);
        this.negBooleans = new TreeSet<>(other.negBooleans);
    }

    /**
     * Returns all integer assignments in this assignment.
     * @return all integer assignments in this assignment
     */
    public Map<IntegerVariable, Integer> getIntegerAssignments() {
        return Collections.unmodifiableMap(integerAssignments);
    }

    /**
     * Returns all positive boolean literals in this assignment.
     * @return all positive boolean literals in this assignment
     */
    public SortedSet<Variable> positiveBooleans() {
        return Collections.unmodifiableSortedSet(posBooleans);
    }

    /**
     * Returns all negative boolean literals in this assignment.
     * @return all negative boolean literals in this assignment
     */
    public SortedSet<Literal> negativeBooleans() {
        return Collections.unmodifiableSortedSet(negBooleans);
    }

    /**
     * Adds a boolean literals to this assignment
     * @param lit the boolean literal
     */
    public void addLiteral(final Literal lit) {
        if (lit.getPhase()) {
            posBooleans.add(lit.variable());
        } else {
            negBooleans.add(lit);
        }
    }

    /**
     * Adds a positive literal to the assignment
     * @param var the positive literal
     */
    public void addPos(final Variable var) {
        posBooleans.add(var);
    }

    /**
     * Adds a negative literal to the assignment
     * @param lit the negative literal
     */
    public void addNeg(final Literal lit) {
        negBooleans.add(lit);
    }

    /**
     * Adds an integer assignment to the assignment
     * @param var   the integer variable
     * @param value its value
     */
    public void addIntAssignment(final IntegerVariable var, final int value) {
        integerAssignments.put(var, value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CspAssignment that = (CspAssignment) o;

        if (!integerAssignments.equals(that.integerAssignments)) {
            return false;
        }
        if (!posBooleans.equals(that.posBooleans)) {
            return false;
        }
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
