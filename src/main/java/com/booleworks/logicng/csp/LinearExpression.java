package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.csp.terms.Term;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class LinearExpression implements Comparable<LinearExpression> {

    private final SortedMap<IntegerVariable, Integer> coef;
    private int b;
    private IntegerDomain domain = null;

    public LinearExpression(final int b) {
        this.coef = new TreeMap<>();
        this.b = b;
    }

    public LinearExpression(final int a0, final IntegerVariable v0, final int b) {
        this(b);
        this.coef.put(v0, a0);
    }

    public LinearExpression(final IntegerVariable v0) {
        this(1, v0, 0);
    }

    public LinearExpression(final LinearExpression e) {
        this.coef = new TreeMap<>(e.coef);
        this.b = e.b;
        this.domain = e.domain;
    }

    public LinearExpression(final SortedMap<IntegerVariable, Integer> coef, final int b) {
        this.coef = coef;
        this.b = b;
    }

    public int size() {
        return coef.size();
    }

    public int getB() {
        return b;
    }

    public SortedMap<IntegerVariable, Integer> getCoef() {
        return coef;
    }

    public Set<IntegerVariable> getVariables() {
        return coef.keySet();
    }

    public IntegerVariable[] getVariablesSorted() {
        final int n = coef.size();
        IntegerVariable[] vs = new IntegerVariable[n];
        vs = coef.keySet().toArray(vs);
        Arrays.sort(vs, (v1, v2) -> {
            final long s1 = v1.getDomain().size();
            final long s2 = v2.getDomain().size();
            if (s1 != s2) {
                return s1 < s2 ? -1 : 1;
            }
            final long a1 = Math.abs(getA(v1));
            final long a2 = Math.abs(getA(v2));
            if (a1 != s2) {
                return a1 > s2 ? -1 : 1;
            }
            return v1.compareTo(v2);
        });
        return vs;
    }

    public boolean isIntegerVariable() {
        return b == 0 && size() == 1 && getA(coef.firstKey()) == 1;
    }

    public Integer getA(final IntegerVariable v) {
        Integer a = coef.get(v);
        if (a == null) {
            a = 0;
        }
        return a;
    }

    private int gcd(int p, int q) {
        while (true) {
            final int r = p % q;
            if (r == 0) {break;}
            p = q;
            q = r;
        }
        return q;
    }

    public int factor() {
        if (size() == 0) {
            return b == 0 ? 1 : Math.abs(b);
        }
        int gcd = Math.abs(getA(coef.firstKey()));
        for (final IntegerVariable v : coef.keySet()) {
            gcd = gcd(gcd, Math.abs(getA(v)));
            if (gcd == 1) {break;}
        }
        if (b != 0) {
            gcd = gcd(gcd, Math.abs(b));
        }
        return gcd;
    }

    public IntegerDomain getDomain() {
        if (domain == null) {
            domain = new IntegerRangeDomain(b, b);
            for (final IntegerVariable v : coef.keySet()) {
                final int a = getA(v);
                domain = domain.add(v.getDomain().mul(a));
            }
        }
        return domain;
    }

    public IntegerDomain getDomainExcept(final IntegerVariable v, final Map<IntegerVariable, IntegerVariable> restrictions) {
        IntegerDomain d = new IntegerRangeDomain(b, b);
        for (final IntegerVariable v2 : coef.keySet()) {
            if (!v2.equals(v)) {
                final int a = getA(v2);
                d = d.add(restrictions.getOrDefault(v2, v2).getDomain().mul(a));
            }
        }
        return d;
    }

    public boolean isDomainLargerThan(final long limit) {
        long size = 1;
        for (final IntegerVariable v : coef.keySet()) {
            size *= v.getDomain().size();
            if (size > limit) {return true;}
        }
        return false;
    }

    public Term toTerm(final CspFactory cspFactory) {
        if (isIntegerVariable()) {
            return coef.firstKey();
        } else if (coef.isEmpty()) {
            return cspFactory.constant(b);
        }
        final List<Term> terms = new ArrayList<>();
        coef.forEach((v, c) -> terms.add(cspFactory.mul(c, v)));
        if (b != 0) {
            terms.add(cspFactory.constant(b));
        }
        return cspFactory.add(terms);
    }

    public boolean equals(final LinearExpression linearExpression) {
        if (linearExpression == null) {
            return false;
        }
        if (this == linearExpression) {
            return true;
        }
        return b == linearExpression.b && coef.equals(linearExpression.coef);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return equals((LinearExpression) obj);
    }

    @Override
    public int compareTo(final LinearExpression other) {
        if (other == null) {
            return 1;
        }
        if (this.equals(other)) {
            return 0;
        }
        if (coef.size() < other.coef.size()) {
            return -1;
        }
        if (coef.size() > other.coef.size()) {
            return 1;
        }
        final Iterator<IntegerVariable> it1 = coef.keySet().iterator();
        final Iterator<IntegerVariable> it2 = coef.keySet().iterator();
        while (it1.hasNext()) {
            assert it2.hasNext();
            final IntegerVariable v1 = it1.next();
            final IntegerVariable v2 = it2.next();
            final int cv = v1.compareTo(v2);
            if (cv != 0) {
                return cv;
            }
            final int ca = getA(v1).compareTo(other.getA(v2));
            if (ca != 0) {
                return ca;
            }
        }
        return Integer.compare(b, other.b);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((coef == null) ? 0 : coef.hashCode());
        result = PRIME * result + (int) b;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Add<");
        for (final IntegerVariable v : coef.keySet()) {
            final long c = getA(v);
            if (c == 0) {
            } else if (c == 1) {
                sb.append(v.toString());
            } else {
                sb.append("Mul<");
                sb.append(c);
                sb.append(",");
                sb.append(v.toString());
                sb.append(">");
            }
        }
        sb.append(b);
        sb.append(">");
        return sb.toString();
    }

    public static class Builder {
        LinearExpression expression;

        public Builder(final int b) {
            expression = new LinearExpression(b);
        }

        public Builder(final int a0, final IntegerVariable v0, final int b) {
            expression = new LinearExpression(a0, v0, b);
        }

        public Builder(final IntegerVariable v0) {
            expression = new LinearExpression(v0);
        }

        public Builder(final LinearExpression e) {
            expression = new LinearExpression(e);
        }

        public Builder(final SortedMap<IntegerVariable, Integer> coef, final int b) {
            expression = new LinearExpression(coef, b);
        }

        public LinearExpression build() {
            final LinearExpression e = expression;
            expression = null;
            return e;
        }

        public Builder setB(final int b) {
            expression.b = b;
            return this;
        }

        public Builder setA(final int a, final IntegerVariable v) {
            if (a == 0) {
                expression.coef.remove(v);
            } else {
                expression.coef.put(v, a);
            }
            expression.domain = null;
            return this;
        }

        public Builder add(final LinearExpression other) {
            expression.b += other.b;
            for (final IntegerVariable v : other.coef.keySet()) {
                final int a = expression.getA(v) + other.getA(v);
                setA(a, v);
            }
            expression.domain = null;
            return this;
        }

        public Builder subtract(final LinearExpression other) {
            expression.b -= other.b;
            for (final IntegerVariable v : other.coef.keySet()) {
                final int a = expression.getA(v) - other.getA(v);
                setA(a, v);
            }
            expression.domain = null;
            return this;
        }

        public Builder multiply(final int c) {
            expression.b *= c;
            for (final IntegerVariable v : expression.coef.keySet()) {
                final int a = c * expression.getA(v);
                setA(a, v);
            }
            expression.domain = null;
            return this;
        }

        public Builder divide(final int c) {
            expression.b /= c;
            for (final IntegerVariable v : expression.coef.keySet()) {
                final int a = expression.getA(v) / c;
                setA(a, v);
            }
            expression.domain = null;
            return this;
        }

        public Builder normalize() {
            final int factor = expression.factor();
            if (factor > 1) {
                divide(factor);
            }
            return this;
        }

        public IntegerDomain getDomain() {
            return expression.getDomain();
        }

        public boolean isDomainLargerThan(final long limit) {
            return expression.isDomainLargerThan(limit);
        }

        public int factor() {
            return expression.factor();
        }

        public int size() {
            return expression.size();
        }

        public int getB() {
            return expression.getB();
        }

        public SortedMap<IntegerVariable, Integer> getCoef() {
            return expression.getCoef();
        }

        public Set<IntegerVariable> getVariables() {
            return expression.getVariables();
        }

        public IntegerVariable[] getVariablesSorted() {
            return expression.getVariablesSorted();
        }

        public boolean isIntegerVariable() {
            return expression.isIntegerVariable();
        }

        public Integer getA(final IntegerVariable v) {
            return expression.getA(v);
        }
    }

    public static LinearExpression add(final LinearExpression a, final LinearExpression b) {
        return new Builder(a).add(b).build();
    }

    public static LinearExpression subtract(final LinearExpression a, final LinearExpression b) {
        return new Builder(a).subtract(b).build();
    }

    public static LinearExpression multiply(final LinearExpression a, final int c) {
        return new Builder(a).multiply(c).build();
    }

    public static LinearExpression divide(final LinearExpression a, final int c) {
        return new Builder(a).divide(c).build();
    }

    public static LinearExpression normalized(final LinearExpression a) {
        return new Builder(a).normalize().build();
    }
}

