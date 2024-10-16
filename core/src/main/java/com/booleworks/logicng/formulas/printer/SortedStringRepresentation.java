// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.printer;

import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.Equivalence;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.NAryOperator;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A sorted string representation for formulas.
 * <p>
 * A variable ordering is given as a list of variables. The variables in the
 * formula will be sorted in the same order as they appear in the list.
 * <p>
 * Let f1 and f2 be two sub-formulas of a formula to be sorted. It is
 * iteratively checked whether the variables of the given ordering appear in
 * either of the two sub-formulas. We distinguish the following cases for a
 * currently considered variable v:
 * <ul>
 * <li>If v is in f1 and f2, then continue with the next variable</li>
 * <li>If v is in f1 but not f2, then f1 is ordered before f2</li>
 * <li>If v is in f2 but not f1, then f2 is ordered before f1</li>
 * <li>If all variables of the ordering have been in both f1 and f2, the two
 * formulas can be ordered arbitrarily</li>
 * </ul>
 * <p>
 * Example 1: Given the variable ordering {@code [a, b, c, d]}, the sorted
 * string representation for a simple conjunction {@code b & d & ~a & ~c} would
 * be {@code ~a & b & ~c & d}.
 * <p>
 * It is important to note that the first variable that appear in only one of
 * the compared sub-formulas decides their ordering. Hence, apart from the
 * deciding variable, the other variables of the sub-formulas might suggest a
 * different order. The user is urged to keep this in mind and an exemplary
 * situation is therefore illustrated in the following example.
 * <p>
 * Example 2: Given the variable ordering {@code [a, b, c, d, e, f]}, the sorted
 * string representation for the formula {@code b | c | d <=> a | e | f} would
 * be {@code a | e | f <=> b | c | d}.
 * <p>
 * Furthermore, the fact that implications cannot be ordered should also be kept
 * in mind.
 * <p>
 * Example 3: Given the variable ordering {@code [a, b]}, the sorted string
 * representation for the formula {@code b => a} stays {@code b => a}.
 * <p>
 * Finally, the user should be aware that any variables of a formula that do not
 * appear in the given ordering will be sorted after the variables that do
 * appear in the ordering.
 * <p>
 * Example 4: Given the variable ordering {@code [b, c, d]}, the sorted string
 * representation for the formula {@code a & (c | (d => b))} would be
 * {@code ((d => b) | c) & a}.
 * @version 3.0.0
 * @since 1.5.0
 */
public final class SortedStringRepresentation extends DefaultStringRepresentation {

    /**
     * A list of variables in the order they are supposed to appear in a given
     * formula.
     */
    private final List<Variable> varOrder;
    private final FormulaComparator comparator;

    /**
     * Constructs a new sorted string representation with a given ordering of
     * variables.
     * @param f        the formula factory to use for caching
     * @param varOrder the given variable ordering
     */
    public SortedStringRepresentation(final FormulaFactory f, final List<Variable> varOrder) {
        this.varOrder = varOrder;
        comparator = new FormulaComparator(f, varOrder);
    }

    /**
     * Returns the sorted string representation of the given formula.
     * @param formula the formula
     * @return the sorted string representation of the formula with regard to
     * the variable ordering
     */
    @Override
    public String toInnerString(final Formula formula) {
        switch (formula.getType()) {
            case FALSE:
                return falsum();
            case TRUE:
                return verum();
            case LITERAL:
                final Literal lit = (Literal) formula;
                return lit.getPhase() ? lit.getName() : negation() + lit.getName();
            case PREDICATE:
                return formula.toString();
            case NOT:
                final Not not = (Not) formula;
                return negation() + bracket(not.getOperand());
            case IMPL:
                return binaryOperator((BinaryOperator) formula, implication());
            case EQUIV:
                return sortedEquivalence((Equivalence) formula);
            case AND:
            case OR:
                final NAryOperator nary = (NAryOperator) formula;
                final String op = formula.getType() == FType.AND ? and() : or();
                return naryOperator(nary, String.format("%s", op));
            case PBC:
                final PbConstraint pbc = (PbConstraint) formula;
                return String.format("%s%s%d", pbLhs(pbc.getOperands(), pbc.getCoefficients()),
                        pbComparator(pbc.comparator()), pbc.getRhs());
            default:
                throw new IllegalArgumentException("Cannot print the unknown formula type " + formula.getType());
        }
    }

    /**
     * Returns the sorted string representation of an n-ary operator.
     * @param operator the n-ary operator
     * @param opString the operator string
     * @return the string representation
     */
    @Override
    protected String naryOperator(final NAryOperator operator, final String opString) {
        final List<Formula> operands = new ArrayList<>();
        for (final Formula op : operator) {
            operands.add(op);
        }
        final int size = operator.numberOfOperands();
        operands.sort(comparator);
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        Formula last = null;
        for (final Formula op : operands) {
            if (++count == size) {
                last = op;
            } else {
                sb.append(operator.getType().getPrecedence() < op.getType().getPrecedence() ? toInnerString(op) : bracket(op));
                sb.append(opString);
            }
        }
        if (last != null) {
            sb.append(operator.getType().getPrecedence() < last.getType().getPrecedence() ? toInnerString(last) : bracket(last));
        }
        return sb.toString();
    }

    /**
     * Returns the sorted string representation of the left-hand side of a
     * pseudo-Boolean constraint.
     * @param operands     the literals of the constraint
     * @param coefficients the coefficients of the constraint
     * @return the sorted string representation
     */
    @Override
    protected String pbLhs(final List<Literal> operands, final List<Integer> coefficients) {
        assert operands.size() == coefficients.size();
        final List<Literal> sortedOperands = new ArrayList<>();
        final List<Integer> sortedCoefficients = new ArrayList<>();
        for (final Variable v : varOrder) {
            final int index = operands.indexOf(v);
            if (index != -1) {
                sortedOperands.add(v);
                sortedCoefficients.add(coefficients.get(index));
            }
        }
        for (final Literal givenOperand : operands) {
            if (!sortedOperands.contains(givenOperand)) {
                sortedOperands.add(givenOperand);
                sortedCoefficients.add(coefficients.get(operands.indexOf(givenOperand)));
            }
        }
        final StringBuilder sb = new StringBuilder();
        final String mul = pbMul();
        final String add = pbAdd();
        for (int i = 0; i < sortedOperands.size() - 1; i++) {
            if (sortedCoefficients.get(i) != 1) {
                sb.append(sortedCoefficients.get(i)).append(mul).append(sortedOperands.get(i)).append(add);
            } else {
                sb.append(sortedOperands.get(i)).append(add);
            }
        }
        if (!sortedOperands.isEmpty()) {
            if (sortedCoefficients.get(sortedOperands.size() - 1) != 1) {
                sb.append(sortedCoefficients.get(sortedOperands.size() - 1)).append(mul)
                        .append(sortedOperands.get(sortedOperands.size() - 1));
            } else {
                sb.append(sortedOperands.get(sortedOperands.size() - 1));
            }
        }
        return sb.toString();
    }

    /**
     * Returns the string representation of an equivalence.
     * @param equivalence the equivalence
     * @return the string representation
     */
    private String sortedEquivalence(final Equivalence equivalence) {
        final Formula right;
        final Formula left;
        if (comparator.compare(equivalence.getLeft(), equivalence.getRight()) <= 0) {
            right = equivalence.getRight();
            left = equivalence.getLeft();
        } else {
            right = equivalence.getLeft();
            left = equivalence.getRight();
        }
        final String leftString =
                FType.EQUIV.getPrecedence() < left.getType().getPrecedence() ? toInnerString(left) : bracket(left);
        final String rightString =
                FType.EQUIV.getPrecedence() < right.getType().getPrecedence() ? toInnerString(right) : bracket(right);
        return String.format("%s%s%s", leftString, equivalence(), rightString);
    }

    static class FormulaComparator implements Comparator<Formula> {

        private final List<Variable> varOrder;
        private final FormulaFactory f;

        FormulaComparator(final FormulaFactory f, final List<Variable> varOrder) {
            this.f = f;
            this.varOrder = varOrder;
        }

        /**
         * Compares two given formulas considering the variable ordering of this
         * class.
         * @param formula1 the first formula
         * @param formula2 the second formula
         * @return -1 iff formula1 &lt; formula2 (when for the first time a
         * variable of the ordering appears in formula1 but not
         * formula2) 0 iff formula1 = formula2 (when all variables of
         * the ordering appear (or not appear) in both formula1 and
         * formula2) 1 iff formula1 &gt; formula2 (when for the first
         * time a variable of the ordering appears in formula2 but not
         * formula1)
         */
        @Override
        public int compare(final Formula formula1, final Formula formula2) {
            final SortedSet<Variable> vars1 = new TreeSet<>(formula1.variables(f));
            final SortedSet<Variable> vars2 = new TreeSet<>(formula2.variables(f));
            for (final Variable v : varOrder) {
                if (vars1.isEmpty() && vars2.isEmpty()) {
                    break;
                } else if (vars1.isEmpty() || (vars1.contains(v) && !vars2.contains(v))) {
                    return -1;
                } else if (vars2.isEmpty() || (!vars1.contains(v) && vars2.contains(v))) {
                    return 1;
                } else if (vars1.contains(v) && vars2.contains(v)) {
                    vars1.remove(v);
                    vars2.remove(v);
                }
            }
            return (int) formula1.numberOfAtoms(f) - (int) formula2.numberOfAtoms(f);
        }
    }
}
