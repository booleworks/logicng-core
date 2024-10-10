package com.booleworks.logicng.csp.literals;

import com.booleworks.logicng.csp.datastructures.IntegerVariableSubstitution;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.LinkedHashSet;
import java.util.Set;

public class ProductLiteral implements ArithmeticLiteral {
    private final IntegerVariable v;
    private final IntegerVariable v1;
    private final IntegerVariable v2;

    public ProductLiteral(final IntegerVariable v, final IntegerVariable v1, final IntegerVariable v2) {
        this.v = v;
        this.v1 = v1;
        this.v2 = v2;
    }

    public IntegerVariable getV() {
        return v;
    }

    public IntegerVariable getV1() {
        return v1;
    }

    public IntegerVariable getV2() {
        return v2;
    }

    @Override
    public boolean isValid() {
        final IntegerDomain d = v.getDomain();
        final IntegerDomain muld = v1.getDomain().mul(v2.getDomain());
        return d.size() == 1 && muld.size() == 1 && d.lb() == muld.lb();
    }

    @Override
    public boolean isUnsat() {
        final IntegerDomain d = v.getDomain();
        final IntegerDomain muld = v1.getDomain().mul(v2.getDomain());
        return d.cap(muld).isEmpty();
    }

    @Override
    public Set<IntegerVariable> getVariables() {
        final Set<IntegerVariable> vs = new LinkedHashSet<>();
        vs.add(v);
        vs.add(v1);
        vs.add(v2);
        return vs;
    }

    @Override
    public ProductLiteral substitute(final IntegerVariableSubstitution assignment) {
        if (assignment.containsKey(v) || assignment.containsKey(v1) || assignment.containsKey(v2)) {
            return new ProductLiteral(assignment.getOrSelf(v), assignment.getOrSelf(v1), assignment.getOrSelf(v2));
        } else {
            return this;
        }
    }

    @Override
    public String toString() {
        return "EQ<" + v.toString() + ",Mul<" + v1 + "," + v2 + ">>";
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductLiteral)) {
            return false;
        }

        final ProductLiteral that = (ProductLiteral) o;
        return v.equals(that.v) && v1.equals(that.v1) && v2.equals(that.v2);
    }

    @Override
    public int hashCode() {
        int result = v.hashCode();
        result = 31 * result + v1.hashCode();
        result = 31 * result + v2.hashCode();
        return result;
    }
}
