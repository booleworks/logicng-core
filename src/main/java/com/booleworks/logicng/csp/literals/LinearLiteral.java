package com.booleworks.logicng.csp.literals;

import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.LinearExpression;
import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.Set;

public class LinearLiteral extends ArithmeticLiteral {
    private final LinearExpression sum;
    private final Operator op;

    public LinearLiteral(final LinearExpression sum, final Operator op) {
        this.sum = LinearExpression.normalized(sum);
        this.op = op;
    }

    public enum Operator {
        LE, EQ, NE, GE
    }

    public LinearExpression getSum() {
        return this.sum;
    }

    public Operator getOperator() {
        return this.op;
    }

    @Override
    public Set<IntegerVariable> getVariables() {
        return this.sum.getVariables();
    }

    public LinearExpression getLinearExpression() {
        return this.sum;
    }

    @Override
    public boolean isValid() {
        final IntegerDomain d = this.sum.getDomain();
        switch (this.op) {
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
        final IntegerDomain d = this.sum.getDomain();
        switch (this.op) {
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
    public String toString() {
        return this.op.toString() + "(" + this.sum.toString() + " 0)";
    }
}
