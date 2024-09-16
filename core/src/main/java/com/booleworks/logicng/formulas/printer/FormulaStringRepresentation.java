// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.printer;

import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.NAryOperator;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.PbConstraint;

import java.util.List;

/**
 * Super class for a formula string representation.
 * @version 3.0.0
 * @since 1.0
 */
public abstract class FormulaStringRepresentation {

    /**
     * Returns the string representation of the given formula.
     * <p>
     * In order to add a prefix/suffix or do one-time calculations on the
     * formula it is recommended to overwrite this method in subclasses.
     * @param formula the formula
     * @return the string representation of the formula
     */
    public String toString(final Formula formula) {
        return toInnerString(formula);
    }

    /**
     * Returns the string representation of the given formula.
     * <p>
     * This method is used for recursive calls in order to format the
     * sub-formulas.
     * @param formula the formula
     * @return the string representation of the formula
     */
    protected String toInnerString(final Formula formula) {
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
            case EQUIV:
                final BinaryOperator binary = (BinaryOperator) formula;
                final String binOp = formula.getType() == FType.IMPL ? implication() : equivalence();
                return binaryOperator(binary, binOp);
            case AND:
            case OR:
                final NAryOperator nary = (NAryOperator) formula;
                final String naryOp = formula.getType() == FType.AND ? and() : or();
                return naryOperator(nary, naryOp);
            case PBC:
                final PbConstraint pbc = (PbConstraint) formula;
                return pbLhs(pbc.getOperands(), pbc.getCoefficients()) + pbComparator(pbc.comparator()) + pbc.getRhs();
            default:
                throw new IllegalArgumentException("Cannot print the unknown formula type " + formula.getType());
        }
    }

    /**
     * Returns a bracketed string version of a given formula.
     * @param formula the formula
     * @return {@code "(" + formula.toString() + ")"}
     */
    protected String bracket(final Formula formula) {
        return String.format("%s%s%s", lbr(), toInnerString(formula), rbr());
    }

    /**
     * Returns the string representation of a binary operator.
     * @param operator the binary operator
     * @param opString the operator string
     * @return the string representation
     */
    protected String binaryOperator(final BinaryOperator operator, final String opString) {
        final String leftString = operator.getType().getPrecedence() < operator.getLeft().getType().getPrecedence()
                ? toInnerString(operator.getLeft()) : bracket(operator.getLeft());
        final String rightString = operator.getType().getPrecedence() < operator.getRight().getType().getPrecedence()
                ? toInnerString(operator.getRight()) : bracket(operator.getRight());
        return leftString + opString + rightString;
    }

    /**
     * Returns the string representation of an n-ary operator.
     * @param operator the n-ary operator
     * @param opString the operator string
     * @return the string representation
     */
    protected String naryOperator(final NAryOperator operator, final String opString) {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        final int size = operator.numberOfOperands();
        Formula last = null;
        for (final Formula op : operator) {
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
     * Returns the string representation of the left-hand side of a
     * pseudo-Boolean constraint.
     * @param operands     the literals of the constraint
     * @param coefficients the coefficients of the constraint
     * @return the string representation
     */
    protected String pbLhs(final List<Literal> operands, final List<Integer> coefficients) {
        assert operands.size() == coefficients.size();
        final StringBuilder sb = new StringBuilder();
        final String mul = pbMul();
        final String add = pbAdd();
        for (int i = 0; i < operands.size() - 1; i++) {
            if (coefficients.get(i) != 1) {
                sb.append(coefficients.get(i)).append(mul).append(operands.get(i)).append(add);
            } else {
                sb.append(operands.get(i)).append(add);
            }
        }
        if (!operands.isEmpty()) {
            if (coefficients.get(operands.size() - 1) != 1) {
                sb.append(coefficients.get(operands.size() - 1)).append(mul).append(operands.get(operands.size() - 1));
            } else {
                sb.append(operands.get(operands.size() - 1));
            }
        }
        return sb.toString();
    }

    /**
     * Returns the string representation of false.
     * @return the string representation of false
     */
    protected abstract String falsum();

    /**
     * Returns the string representation of true.
     * @return the string representation of true
     */
    protected abstract String verum();

    /**
     * Returns the string representation of not.
     * @return the string representation of not
     */
    protected abstract String negation();

    /**
     * Returns the string representation of an implication.
     * @return the string representation of an implication
     */
    protected abstract String implication();

    /**
     * Returns the string representation of an equivalence.
     * @return the string representation of an equivalence
     */
    protected abstract String equivalence();

    /**
     * Returns the string representation of and.
     * @return the string representation of and
     */
    protected abstract String and();

    /**
     * Returns the string representation of or.
     * @return the string representation of or
     */
    protected abstract String or();

    /**
     * Returns the string representation of a pseudo-Boolean comparator.
     * @param comparator the pseudo-Boolean comparator
     * @return the string representation of a pseudo-Boolean comparator
     */
    protected abstract String pbComparator(final CType comparator);

    /**
     * Returns the string representation of a pseudo-Boolean multiplication.
     * @return the string representation of a pseudo-Boolean multiplication
     */
    protected abstract String pbMul();

    /**
     * Returns the string representation of a pseudo-Boolean addition.
     * @return the string representation of a pseudo-Boolean addition
     */
    protected abstract String pbAdd();

    /**
     * Returns the string representation of a left bracket.
     * @return the string representation of a left bracket
     */
    protected abstract String lbr();

    /**
     * Returns the string representation of right bracket.
     * @return the string representation of right bracket
     */
    protected abstract String rbr();
}
