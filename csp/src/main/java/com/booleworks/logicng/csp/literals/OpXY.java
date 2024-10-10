package com.booleworks.logicng.csp.literals;

import com.booleworks.logicng.csp.datastructures.IntegerVariableSubstitution;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerHolder;
import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Auxiliary literal representing a simple relation between two variables:
 * <p>
 * {@code x (op) y} with {@code op in {=, <=, !=}}
 * <p>
 * This class is an intermediate representation used by encoding algorithms. It should not be used directly.
 */
public class OpXY implements RCSPLiteral {
    private final IntegerHolder x, y;
    private final Operator op;

    /**
     * Constructs new simple relation: {@code x (op) y; op in {=, <=, !=}}
     * @param op operator of the relation
     * @param x  argument x
     * @param y  argument y
     */
    public OpXY(final Operator op, final IntegerHolder x, final IntegerHolder y) {
        this(op, x, y, false);
    }

    /**
     * Constructors new simple relation and invert it:
     * <pre>
     *     {@code
     *     invert -> y <= x | x = y | x != y
     *     !invert -> x <= y | x = y | x != y
     *     }
     * </pre>
     * @param op     operator of the relation
     * @param x      argument x
     * @param y      argument y
     * @param invert whether to invert the relation or not
     */
    public OpXY(final Operator op, final IntegerHolder x, final IntegerHolder y, final boolean invert) {
        if (invert && op == Operator.LE) {
            this.x = y;
            this.y = x;
        } else {
            this.x = x;
            this.y = y;
        }
        this.op = op;
    }

    /**
     * Operators that can be used this literal.
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
        NE;

        /**
         * Convert a linear literal operator
         * @param op linear literal operator
         * @return converted literal
         */
        public static Operator from(final LinearLiteral.Operator op) {
            switch (op) {
                case LE:
                    return LE;
                case EQ:
                    return EQ;
                case NE:
                    return NE;
            }
            throw new RuntimeException("Unreachable Code");
        }
    }

    /**
     * Returns the operator of the relation.
     * @return operator of the relation
     */
    public Operator getOp() {
        return op;
    }

    /**
     * Returns x.
     * @return x
     */
    public IntegerHolder getX() {
        return x;
    }

    /**
     * Returns y.
     * @return y
     */
    public IntegerHolder getY() {
        return y;
    }

    @Override
    public int getUpperBound() {
        return Math.max(x.getDomain().ub(), y.getDomain().ub());
    }

    @Override
    public boolean isValid() {
        final IntegerDomain xd = x.getDomain();
        final IntegerDomain yd = y.getDomain();
        switch (op) {
            case LE:
                return xd.ub() <= yd.lb();
            case EQ:
                return xd.size() == 1 && yd.size() == 1 && xd.ub() == yd.ub();
            case NE:
                return xd.cap(yd).isEmpty();
        }
        throw new RuntimeException("Unreachable code");
    }

    @Override
    public boolean isUnsat() {
        return false;
    }

    @Override
    public Set<IntegerVariable> getVariables() {
        final Set<IntegerVariable> set = new LinkedHashSet<>();
        if (x instanceof IntegerVariable) {
            set.add((IntegerVariable) x);
        }
        if (y instanceof IntegerVariable) {
            set.add((IntegerVariable) y);
        }
        return set;
    }

    @Override
    public ArithmeticLiteral substitute(final IntegerVariableSubstitution assignment) {
        IntegerHolder newX = this.x;
        IntegerHolder newY = this.y;
        if (this.x instanceof IntegerVariable) {
            final IntegerVariable x = (IntegerVariable) this.x;
            newX = assignment.getOrSelf(x);
        }
        if (this.y instanceof IntegerVariable) {
            final IntegerVariable y = (IntegerVariable) this.y;
            newY = assignment.getOrSelf(y);
        }
        if (newX == x && newY == y) {
            return this;
        } else {
            return new OpXY(op, newX, newY);
        }
    }

    @Override
    public String toString() {
        return "(" + op + " " + x + " " + y + ")";
    }
}
