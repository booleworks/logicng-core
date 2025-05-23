package com.booleworks.logicng.csp.literals;

import com.booleworks.logicng.csp.datastructures.IntegerVariableSubstitution;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.Set;

/**
 * Represents a relation with a linear sum:
 * <p>
 * {@code c_1 * a_1 + ... c_n * a_n (op) 0} with {@code op in {<=, =, !=}}
 */
public class LinearLiteral implements ArithmeticLiteral {
    private final LinearExpression sum;
    private final Operator op;

    /**
     * Construct new linear literal: {@code c_1 * a_1 + ... c_n * a_n <= 0}
     * @param sum left side of the relation
     * @param op  operator of the relation ({@code <=, =, !=})
     */
    public LinearLiteral(final LinearExpression sum, final Operator op) {
        this.sum = LinearExpression.normalized(sum);
        this.op = op;
    }

    /**
     * Operators that can be used in a linear literal.
     * <ul>
     *     <li>LE: less-than-equals ({@code <=})</li>
     *     <li>EQ: equals ({@code =})</li>
     *     <li>NE: not equals ({@code !=})</li>
     * </ul>
     */
    public enum Operator {
        /**
         * Less-than-equals ({@code <=}).
         */
        LE,
        /**
         * equals ({@code =}).
         */
        EQ,
        /**
         * not equals ({@code !=}).
         */
        NE
    }

    /**
     * Returns the sum (left side) of the relation.
     * @return the sum (left side) of the relation
     */
    public LinearExpression getSum() {
        return sum;
    }

    /**
     * Returns the operator of the relation.
     * @return operator of the relation
     */
    public Operator getOperator() {
        return op;
    }

    /**
     * Returns a new linear literal that is the negation of this literal.
     * @return a new linear literal that is the negation of this literal
     */
    public LinearLiteral negate() {
        switch (op) {
            case LE:
                return new LinearLiteral(new LinearExpression.Builder(sum).setB(sum.getB() - 1).multiply(-1).build(),
                        Operator.LE);
            case EQ:
                return new LinearLiteral(sum, Operator.NE);
            case NE:
                return new LinearLiteral(sum, Operator.EQ);
            default:
                throw new RuntimeException("Unreachable code");
        }
    }

    @Override
    public Set<IntegerVariable> getVariables() {
        return sum.getVariables();
    }

    @Override
    public boolean isValid() {
        final IntegerDomain d = sum.getDomain();
        switch (op) {
            case LE:
                return d.ub() <= 0;
            case EQ:
                return d.contains(0) && d.size() == 1;
            case NE:
                return !d.contains(0);
            default:
                throw new RuntimeException("Unreachable code");
        }
    }

    @Override
    public boolean isUnsat() {
        final IntegerDomain d = sum.getDomain();
        switch (op) {
            case LE:
                return d.lb() > 0;
            case EQ:
                return !d.contains(0);
            case NE:
                return d.contains(0) && d.size() == 1;
            default:
                throw new RuntimeException("Unreachable code");
        }
    }

    @Override
    public LinearLiteral substitute(final IntegerVariableSubstitution assignment) {
        final LinearExpression.Builder le = new LinearExpression.Builder(sum.getB());
        int replaced = 0;
        for (final IntegerVariable key : sum.getCoef().keySet()) {
            if (assignment.containsKey(key)) {
                final IntegerVariable newVar = assignment.get(key);
                if (newVar.isUnsatisfiable()) {
                    return null;
                }
                ++replaced;
                le.setA(sum.getCoef().get(key), assignment.get(key));
            } else {
                le.setA(sum.getCoef().get(key), key);
            }
        }
        if (replaced > 0) {
            return new LinearLiteral(le.build(), op);
        } else {
            return this;
        }
    }

    @Override
    public String toString() {
        return op.toString() + "(" + sum.toString() + ",0)";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LinearLiteral that = (LinearLiteral) o;

        if (!op.equals(that.op)) {
            return false;
        }
        return sum.equals(that.sum);
    }

    @Override
    public int hashCode() {
        int result = sum.hashCode();
        result = 31 * result + op.hashCode();
        return result;
    }
}
