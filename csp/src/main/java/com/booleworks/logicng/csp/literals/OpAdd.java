package com.booleworks.logicng.csp.literals;

import com.booleworks.logicng.csp.datastructures.IntegerVariableSubstitution;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerHolder;
import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Auxiliary literal representing a relation with an addition of two variables:
 * <p>
 * {@code z (op) x + y} with {@code op in {=, <=, >=, !=}}
 * <p>
 * This class is an intermediate representation used by encoding algorithms. It should not be used directly.
 */
public class OpAdd implements RCSPLiteral {
    private final IntegerHolder z, x, y;
    private final Operator op;

    /**
     * Constructs new addition relation: {@code z (op) x + y; op in {=, <=, >=, !=}}
     * @param op operator of the relation
     * @param z  argument for z
     * @param x  argument for x
     * @param y  argument for y
     */
    public OpAdd(final Operator op, final IntegerHolder z, final IntegerHolder x, final IntegerHolder y) {
        this.z = z;
        this.x = x;
        this.y = y;
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
         * Greater-than-equals ({@code >=}).
         */
        GE,
        /**
         * Equals ({@code =}).
         */
        EQ,
        /**
         * Not equals ({@code !=}).
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

        /**
         * Convert linear literal operator and inverts the operator.
         * @param op     linear literal operator
         * @param invert whether to invert the operator or not
         * @return converted literal
         */
        public static Operator from(final LinearLiteral.Operator op, final boolean invert) {
            switch (op) {
                case LE:
                    return invert ? GE : LE;
                case EQ:
                    return EQ;
                case NE:
                    return NE;
            }
            throw new RuntimeException("Unreachable Code");
        }
    }

    /**
     * Returns z.
     * @return z
     */
    public IntegerHolder getZ() {
        return z;
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

    /**
     * Returns the operator of the relation.
     * @return operator of the relation
     */
    public Operator getOp() {
        return op;
    }

    @Override
    public int getUpperBound() {
        return Math.max(Math.max(z.getDomain().ub(), x.getDomain().ub()), y.getDomain().ub());
    }

    @Override
    public boolean isValid() {
        final IntegerDomain zd = z.getDomain();
        final IntegerDomain xd = x.getDomain();
        final IntegerDomain yd = y.getDomain();
        switch (op) {
            case LE:
                return zd.ub() <= xd.lb() + yd.lb();
            case GE:
                return zd.lb() >= xd.ub() + yd.ub();
            case EQ:
                return zd.size() == 1 && xd.size() == 1 && yd.size() == 1 && zd.ub() == xd.ub() + yd.ub();
            case NE:
                return zd.cap(xd.add(yd)).isEmpty();
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
        if (z instanceof IntegerVariable) {
            set.add((IntegerVariable) z);
        }
        return set;
    }

    @Override
    public ArithmeticLiteral substitute(final IntegerVariableSubstitution assignment) {
        IntegerHolder newX = this.x;
        IntegerHolder newY = this.y;
        IntegerHolder newZ = this.z;
        if (this.x instanceof IntegerVariable) {
            final IntegerVariable x = (IntegerVariable) this.x;
            newX = assignment.getOrSelf(x);
        }
        if (this.y instanceof IntegerVariable) {
            final IntegerVariable y = (IntegerVariable) this.y;
            newY = assignment.getOrSelf(y);
        }
        if (this.z instanceof IntegerVariable) {
            final IntegerVariable z = (IntegerVariable) this.z;
            newZ = assignment.getOrSelf(z);
        }
        if (this.x == newX && this.y == newY && this.z == newZ) {
            return this;
        } else {
            return new OpAdd(op, newZ, newX, newY);
        }
    }

    @Override
    public String toString() {
        return "(" + op + "add " + z + " " + x + " " + y + ")";
    }
}
