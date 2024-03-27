package com.booleworks.logicng.csp.literals;

import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.LinearExpression;
import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class LinearLiteral extends ArithmeticLiteral {
    private final LinearExpression sum;
    private final Operator op;

    public LinearLiteral(final LinearExpression sum, final Operator op) {
        this.sum = LinearExpression.normalized(sum);
        this.op = op;
    }

    public enum Operator {
        LE, EQ, NE
    }

    public LinearExpression getSum() {
        return sum;
    }

    public Operator getOperator() {
        return op;
    }

    @Override
    public Set<IntegerVariable> getVariables() {
        return sum.getVariables();
    }

    @Override
    public int[] getBound(final IntegerVariable v, final Map<IntegerVariable, IntegerVariable> restrictions) {
        final int a = sum.getA(v);
        final IntegerDomain d = sum.getDomainExcept(v, restrictions);
        int lb = v.getDomain().lb();
        int ub = v.getDomain().ub();
        switch (op) {
            case LE:
                if (a > 0) {
                    ub = divfloor(-d.lb(), a);
                } else if (a < 0) {
                    lb = divceil(-d.lb(), a);
                }
                break;
            case EQ:
                if (a > 0) {
                    lb = divceil(-d.ub(), a);
                    ub = divfloor(-d.lb(), a);
                } else if (a < 0) {
                    lb = divceil(-d.lb(), a);
                    ub = divfloor(-d.ub(), a);
                }
                break;
            case NE:
                return null;
        }
        if (lb > ub) {
            return null;
        }
        return new int[]{lb, ub};
    }

    public LinearExpression getLinearExpression() {
        return sum;
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
    public ArithmeticLiteral substitute(final Map<IntegerVariable, IntegerVariable> assignment) {
        final SortedMap<IntegerVariable, Integer> newCoefs = new TreeMap<>();
        int replaced = 0;
        for (final IntegerVariable key : sum.getCoef().keySet()) {
            if (assignment.containsKey(key)) {
                final IntegerVariable newVar = assignment.get(key);
                if (newVar.isUnsatisfiable()) {
                    return null;
                }
                ++replaced;
                newCoefs.put(assignment.get(key), sum.getCoef().get(key));
            } else {
                newCoefs.put(key, sum.getCoef().get(key));
            }
        }
        if (replaced > 0) {
            return new LinearLiteral(new LinearExpression(newCoefs, sum.getB()), op);
        } else {
            return this;
        }
    }

    @Override
    public String toString() {
        return op.toString() + "(" + sum.toString() + " 0)";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        final LinearLiteral that = (LinearLiteral) o;

        if (!sum.equals(that.sum)) {return false;}
        return op == that.op;
    }

    @Override
    public int hashCode() {
        int result = sum.hashCode();
        result = 31 * result + op.hashCode();
        return result;
    }

    @Override
    public int compareTo(final ArithmeticLiteral other) {
        if (this == other) {
            return 0;
        }
        if (other == null) {
            return 1;
        }
        if (other instanceof LinearLiteral) {
            final LinearLiteral o = (LinearLiteral) other;
            final int c1 = sum.compareTo(o.sum);
            return c1 == 0 ? op.compareTo(o.op) : c1;

        } else {
            return -1;
        }
    }

    /**
     * ceil(b/a)
     */
    static private int divceil(final int b, final int a) {
        if ((a >= 0 && b >= 0) ||
                (a < 0 && b < 0)) {
            return b / a;
        } else if (a < 0) {
            return (-b + a + 1) / -a;
        } else {
            return (b - a + 1) / a;
        }
    }

    /**
     * floor(b/a)
     */
    static private int divfloor(final int b, final int a) {
        if (a >= 0 && b >= 0) {
            return b / a;
        } else if (a < 0 && b < 0) {
            return -b / -a;
        } else if (a >= 0) {
            return (b - a + 1) / a;
        } else {
            return (-b + a + 1) / -a;
        }
    }
}
