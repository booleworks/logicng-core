package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.csp.terms.Term;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
        return this.coef.size();
    }

    public int getB() {
        return this.b;
    }

    public SortedMap<IntegerVariable, Integer> getCoef() {
        return this.coef;
    }

    public Set<IntegerVariable> getVariables() {
        return this.coef.keySet();
    }

    public IntegerVariable[] getVariablesSorted() {
        final int n = this.coef.size();
        IntegerVariable[] vs = new IntegerVariable[n];
        vs = this.coef.keySet().toArray(vs);
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
        return this.b == 0 && size() == 1 && getA(this.coef.firstKey()) == 1;
    }

    public Integer getA(final IntegerVariable v) {
        Integer a = this.coef.get(v);
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
            return this.b == 0 ? 1 : Math.abs(this.b);
        }
        int gcd = Math.abs(getA(this.coef.firstKey()));
        for (final IntegerVariable v : this.coef.keySet()) {
            gcd = gcd(gcd, Math.abs(getA(v)));
            if (gcd == 1) {break;}
        }
        if (this.b != 0) {
            gcd = gcd(gcd, Math.abs(this.b));
        }
        return gcd;
    }

    public IntegerDomain getDomain() {
        if (this.domain == null) {
            this.domain = new IntegerRangeDomain(this.b, this.b);
            for (final IntegerVariable v : this.coef.keySet()) {
                final int a = getA(v);
                this.domain = this.domain.add(v.getDomain().mul(a));
            }
        }
        return this.domain;
    }

    public boolean isDomainLargerThan(final long limit) {
        long size = 1;
        for (final IntegerVariable v : this.coef.keySet()) {
            size *= v.getDomain().size();
            if (size > limit) {return true;}
        }
        return false;
    }

    public Term toTerm(final CspFactory cspFactory) {
        if (this.isIntegerVariable()) {
            return this.coef.firstKey();
        } else if (this.coef.isEmpty()) {
            return cspFactory.constant(this.b);
        }
        final List<Term> terms = new ArrayList<>();
        this.coef.forEach((v, c) -> terms.add(cspFactory.mul(c, v)));
        if (this.b != 0) {
            terms.add(cspFactory.constant(this.b));
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
        return this.b == linearExpression.b && this.coef.equals(linearExpression.coef);
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
        if (this.coef.size() < other.coef.size()) {
            return -1;
        }
        if (this.coef.size() > other.coef.size()) {
            return 1;
        }
        final Iterator<IntegerVariable> it1 = this.coef.keySet().iterator();
        final Iterator<IntegerVariable> it2 = this.coef.keySet().iterator();
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
        return Integer.compare(this.b, other.b);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((this.coef == null) ? 0 : this.coef.hashCode());
        result = PRIME * result + (int) this.b;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("(add ");
        for (final IntegerVariable v : this.coef.keySet()) {
            final long c = getA(v);
            if (c == 0) {
            } else if (c == 1) {
                sb.append(v.toString());
            } else {
                sb.append("(mul ");
                sb.append(c);
                sb.append(" ");
                sb.append(v.toString());
                sb.append(")");
            }
            sb.append(" ");
        }
        sb.append(this.b);
        sb.append(")");
        return sb.toString();
    }

    public static class Mutable extends LinearExpression {

        public Mutable(final int b) {
            super(b);
        }

        public Mutable(final int a0, final IntegerVariable v0, final int b) {
            super(a0, v0, b);
        }

        public Mutable(final IntegerVariable v0) {
            super(v0);
        }

        public Mutable(final LinearExpression e) {
            super(e);
        }

        public Mutable(final SortedMap<IntegerVariable, Integer> coef, final int b) {
            super(coef, b);
        }

        public LinearExpression build() {
            return this;
        }

        public Mutable setB(final int b) {
            super.b = b;
            return this;
        }

        public Mutable setA(final int a, final IntegerVariable v) {
            if (a == 0) {
                super.coef.remove(v);
            } else {
                super.coef.put(v, a);
            }
            super.domain = null;
            return this;
        }

        public Mutable add(final LinearExpression other) {
            super.b += other.b;
            for (final IntegerVariable v : other.coef.keySet()) {
                final int a = super.getA(v) + other.getA(v);
                setA(a, v);
            }
            super.domain = null;
            return this;
        }

        public Mutable subtract(final LinearExpression other) {
            super.b -= other.b;
            for (final IntegerVariable v : other.coef.keySet()) {
                final int a = super.getA(v) - other.getA(v);
                setA(a, v);
            }
            super.domain = null;
            return this;
        }

        public Mutable multiply(final int c) {
            super.b *= c;
            for (final IntegerVariable v : super.coef.keySet()) {
                final int a = c * super.getA(v);
                setA(a, v);
            }
            super.domain = null;
            return this;
        }

        public Mutable divide(final int c) {
            super.b /= c;
            for (final IntegerVariable v : super.coef.keySet()) {
                final int a = super.getA(v) / c;
                setA(a, v);
            }
            super.domain = null;
            return this;
        }

        public Mutable normalize() {
            final int factor = super.factor();
            if (factor > 1) {
                divide(factor);
            }
            return this;
        }
    }

    public static LinearExpression add(final LinearExpression a, final LinearExpression b) {
        return new Mutable(a).add(b).build();
    }

    public static LinearExpression subtract(final LinearExpression a, final LinearExpression b) {
        return new Mutable(a).subtract(b).build();
    }

    public static LinearExpression multiply(final LinearExpression a, final int c) {
        return new Mutable(a).multiply(c).build();
    }

    public static LinearExpression divide(final LinearExpression a, final int c) {
        return new Mutable(a).divide(c).build();
    }

    public static LinearExpression normalized(final LinearExpression a) {
        return new Mutable(a).normalize().build();
    }
}

