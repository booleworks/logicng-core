package com.booleworks.logicng.csp.datastructures;

import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A class representing a linear expression, i.e., a sum of integer variables with a coefficient and a constant offset.
 *
 * <pre>{@code a_0*v_0 + ... + a_n*v_n + b}</pre>
 */
public class LinearExpression implements Comparable<LinearExpression> {

    private final SortedMap<IntegerVariable, Integer> coef;
    private int b;
    private IntegerDomain domain = null;

    /**
     * Construct a linear expression with only a constant offset.
     * @param b the constant offset
     */
    public LinearExpression(final int b) {
        this.coef = new TreeMap<>();
        this.b = b;
    }

    /**
     * Construct a linear expression with only one variable and a constant offset.
     * @param a0 the coefficient of the variable
     * @param v0 the variable
     * @param b  the constant offset
     */
    public LinearExpression(final int a0, final IntegerVariable v0, final int b) {
        this(b);
        if (a0 != 0) {
            this.coef.put(v0, a0);
        }
    }

    /**
     * Construct a linear expression with only one variable without coefficient ({@code = 1}).
     * @param v0 the variable
     */
    public LinearExpression(final IntegerVariable v0) {
        this(1, v0, 0);
    }

    /**
     * Construct a linear expression from a map of variables and coefficients and a constant offset.
     * @param coef the map of variables and coefficients
     * @param b    the constant offset
     */
    public LinearExpression(final SortedMap<IntegerVariable, Integer> coef, final int b) {
        this.coef = new TreeMap<>(coef);
        this.coef.entrySet().removeIf(e -> e.getValue() == 0);
        this.b = b;
    }

    /**
     * Copy a linear expression from an existing linear expression.
     * @param e the existing linear expression
     */
    private LinearExpression(final LinearExpression e) {
        this.coef = new TreeMap<>(e.coef);
        this.b = e.b;
        this.domain = e.domain;
    }

    /**
     * Returns the number of variables in the linear expression.
     * @return the number of variables int the linear expression.
     */
    public int size() {
        return coef.size();
    }

    /**
     * Returns the constant offset.
     * @return the constant offset
     */
    public int getB() {
        return b;
    }

    /**
     * Returns a map with variables and their coefficients.
     * @return a map with variables and their coefficients
     */
    public SortedMap<IntegerVariable, Integer> getCoef() {
        return Collections.unmodifiableSortedMap(coef);
    }

    /**
     * Returns all variables of this linear expression.
     * @return all variables of this linear expression
     */
    public Set<IntegerVariable> getVariables() {
        return coef.keySet();
    }

    /**
     * Returns all variables sorted by the size of their domain.
     * @return all variables sorted by the size of their domain
     */
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
            if (a1 != a2) {
                return a1 > a2 ? -1 : 1;
            }
            return v1.compareTo(v2);
        });
        return vs;
    }

    /**
     * Returns whether this linear expression is only an integer variable (No offset and no coefficient).
     * @return {@code true} if this linear expression is only an integer variable, {@code false} otherwise.
     */
    public boolean isIntegerVariable() {
        return b == 0 && size() == 1 && getA(coef.firstKey()) == 1;
    }

    /**
     * Returns the coefficient of a variable.
     * @param v the variable
     * @return the coefficient of the variable.
     */
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
            if (r == 0) {
                break;
            }
            p = q;
            q = r;
        }
        return q;
    }

    /**
     * Returns the factor this linear expression deviates from the normalized linear expression. (GCD of all
     * coefficients)
     * @return the factor the linear expression deviates from the normalized linear expression.
     */
    public int factor() {
        if (size() == 0) {
            return b == 0 ? 1 : Math.abs(b);
        }
        int gcd = Math.abs(getA(coef.firstKey()));
        for (final IntegerVariable v : coef.keySet()) {
            final int a = getA(v);
            if (a != 0) {
                gcd = gcd(gcd, Math.abs(getA(v)));
                if (gcd == 1) {
                    break;
                }
            }
        }
        if (b != 0) {
            gcd = gcd(gcd, Math.abs(b));
        }
        return gcd;
    }

    /**
     * Calculates and returns the domain of this linear expression.
     * @return the domain of this linear expression.
     */
    public IntegerDomain getDomain() {
        if (domain == null) {
            domain = IntegerDomain.of(b, b);
            for (final IntegerVariable v : coef.keySet()) {
                final int a = getA(v);
                domain = domain.add(v.getDomain().mul(a));
            }
        }
        return domain;
    }

    /**
     * Returns the domain of this linear expression without one integer variable.
     * @param v            the variable
     * @param restrictions temporal substitutions that need to be considered
     * @return the domain without the given integer variable
     */
    public IntegerDomain getDomainExcept(final IntegerVariable v, final IntegerVariableSubstitution restrictions) {
        IntegerDomain d = IntegerDomain.of(b, b);
        for (final IntegerVariable v2 : coef.keySet()) {
            if (!v2.equals(v)) {
                final int a = getA(v2);
                d = d.add(restrictions.getOrSelf(v2).getDomain().mul(a));
            }
        }
        return d;
    }

    /**
     * Returns whether the domains of all variables is larger than {@code limit}.
     * @param limit the limit for the domain's size
     * @return {@code true} is the domain is larger than {@code limit}, {@code false} otherwise
     */
    public boolean isDomainLargerThan(final long limit) {
        long size = 1;
        for (final IntegerVariable v : coef.keySet()) {
            size *= v.getDomain().size();
            if (size > limit) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether two linear expressions are equal.
     * @param linearExpression the other linear expression
     * @return {@code true} if they are equal
     */
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
        final Iterator<IntegerVariable> it2 = other.coef.keySet().iterator();
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
            if (c == 1) {
                sb.append(v.toString());
            } else if (c != 0) {
                sb.append("Mul<");
                sb.append(c);
                sb.append(",");
                sb.append(v.toString());
                sb.append(">");
            }
            sb.append(",");
        }
        sb.append(b);
        sb.append(">");
        return sb.toString();
    }

    /**
     * A builder for incrementally building a linear expression.
     */
    public static class Builder {
        private LinearExpression expression;

        /**
         * Construct a builder with only a constant offset.
         * @param b the constant offset
         */
        public Builder(final int b) {
            expression = new LinearExpression(b);
        }

        /**
         * Construct a builder with only one variable and a constant offset.
         * @param a0 the coefficient of the variable
         * @param v0 the variable
         * @param b  the constant offset
         */
        public Builder(final int a0, final IntegerVariable v0, final int b) {
            expression = new LinearExpression(a0, v0, b);
        }

        /**
         * Construct a builder with only one variable without coefficient ({@code = 1}).
         * @param v0 the variable
         */
        public Builder(final IntegerVariable v0) {
            expression = new LinearExpression(v0);
        }

        /**
         * Construct a builder by copying an existing linear expression.
         * @param e the existing linear expression
         */
        public Builder(final LinearExpression e) {
            expression = new LinearExpression(e);
        }

        /**
         * Construct a builder from a map of variables and coefficients and a constant offset.
         * @param coef the map of variables and coefficients
         * @param b    the constant offset
         */
        public Builder(final SortedMap<IntegerVariable, Integer> coef, final int b) {
            expression = new LinearExpression(coef, b);
        }

        /**
         * Builds the linear expression. This invalidates this builder.
         * @return the built linear expression
         */
        public LinearExpression build() {
            final LinearExpression e = expression;
            expression = null;
            return e;
        }

        /**
         * Set the constant offset.
         * @param b the constant offset
         * @return this builder.
         */
        public Builder setB(final int b) {
            expression.b = b;
            return this;
        }

        /**
         * Adds the variable with the given coefficient.
         * @param a the coefficient
         * @param v the variable
         * @return this builder
         */
        public Builder setA(final int a, final IntegerVariable v) {
            if (a == 0) {
                expression.coef.remove(v);
            } else {
                expression.coef.put(v, a);
            }
            expression.domain = null;
            return this;
        }

        /**
         * Arithmetically add another linear expression to this linear expression (inplace).
         * @param other the other linear expression
         * @return this builder
         */
        public Builder add(final LinearExpression other) {
            expression.b += other.b;
            for (final IntegerVariable v : other.coef.keySet()) {
                final int a = expression.getA(v) + other.getA(v);
                setA(a, v);
            }
            expression.domain = null;
            return this;
        }

        /**
         * Arithmetically subtract another linear expression from this linear expression (inplace).
         * @param other the other linear expression
         * @return this builder
         */
        public Builder subtract(final LinearExpression other) {
            expression.b -= other.b;
            for (final IntegerVariable v : other.coef.keySet()) {
                final int a = expression.getA(v) - other.getA(v);
                setA(a, v);
            }
            expression.domain = null;
            return this;
        }

        /**
         * Arithmetically multiply this linear expression with an integer constant (inplace).
         * @param c the integer constant
         * @return this builder
         */
        public Builder multiply(final int c) {
            expression.b *= c;
            for (final IntegerVariable v : expression.coef.keySet()) {
                final int a = c * expression.getA(v);
                setA(a, v);
            }
            expression.domain = null;
            return this;
        }

        /**
         * Arithmetically divide this linear expression with an integer constant (inplace).
         * @param c the integer constant
         * @return this builder
         */
        public Builder divide(final int c) {
            expression.b /= c;
            for (final IntegerVariable v : expression.coef.keySet()) {
                final int a = expression.getA(v) / c;
                setA(a, v);
            }
            expression.domain = null;
            return this;
        }

        /**
         * Normalize this linear expression (inplace).
         * @return this builder
         */
        public Builder normalize() {
            final int factor = expression.factor();
            if (factor > 1) {
                divide(factor);
            }
            return this;
        }

        /**
         * Returns the domain of the linear expression.
         * @return the domain of the linear expression
         */
        public IntegerDomain getDomain() {
            return expression.getDomain();
        }

        /**
         * Returns whether the domains of all variables is larger than {@code limit}.
         * @param limit the limit for the domain's size
         * @return {@code true} is the domain is larger than {@code limit}, {@code false} otherwise
         */
        public boolean isDomainLargerThan(final long limit) {
            return expression.isDomainLargerThan(limit);
        }

        /**
         * Returns the factor this linear expression deviates from the normalized linear expression. (GCD of all
         * coefficients)
         * @return the factor the linear expression deviates from the normalized linear expression.
         */
        public int factor() {
            return expression.factor();
        }

        /**
         * Returns the number of variables in the linear expression.
         * @return the number of variables int the linear expression.
         */
        public int size() {
            return expression.size();
        }

        /**
         * Returns the constant offset.
         * @return the constant offset
         */
        public int getB() {
            return expression.getB();
        }

        /**
         * Returns a map with variables and their coefficients.
         * @return a map with variables and their coefficients
         */
        public SortedMap<IntegerVariable, Integer> getCoef() {
            return expression.getCoef();
        }

        /**
         * Returns all variables of this linear expression.
         * @return all variables of this linear expression
         */
        public Set<IntegerVariable> getVariables() {
            return expression.getVariables();
        }

        /**
         * Returns all variables sorted by the size of their domain.
         * @return all variables sorted by the size of their domain
         */
        public IntegerVariable[] getVariablesSorted() {
            return expression.getVariablesSorted();
        }

        /**
         * Returns whether this linear expression is only an integer variable (No offset and no coefficient).
         * @return {@code true} if this linear expression is only an integer variable, {@code false} otherwise.
         */
        public boolean isIntegerVariable() {
            return expression.isIntegerVariable();
        }

        /**
         * Returns the coefficient of a variable.
         * @param v the variable
         * @return the coefficient of the variable.
         */
        public Integer getA(final IntegerVariable v) {
            return expression.getA(v);
        }
    }

    /**
     * Creates a new linear expression that is the sum of two linear expressions.
     * @param a left linear expression
     * @param b right linear expression
     * @return the sum
     */
    public static LinearExpression add(final LinearExpression a, final LinearExpression b) {
        return new Builder(a).add(b).build();
    }

    /**
     * Creates a new linear expression that is the subtraction of two linear expression.
     * @param a left linear expression
     * @param b right linear expression
     * @return the subtraction
     */
    public static LinearExpression subtract(final LinearExpression a, final LinearExpression b) {
        return new Builder(a).subtract(b).build();
    }

    /**
     * Creates a new linear expression that is the multiplication of a linear expression and a constant integer factor.
     * @param a the linear expression
     * @param c the constant integer factor
     * @return the new linear expression
     */
    public static LinearExpression multiply(final LinearExpression a, final int c) {
        return new Builder(a).multiply(c).build();
    }

    /**
     * Creates a new linear expression that is the division of a linear expression with a constant integer factor.
     * @param a the linear expression
     * @param c the constant integer factor
     * @return the new linear expression
     */
    public static LinearExpression divide(final LinearExpression a, final int c) {
        return new Builder(a).divide(c).build();
    }

    /**
     * Creates a new linear expression that is the normalization of a linear expression.
     * @param a the linear expression
     * @return the new linear expression
     */
    public static LinearExpression normalized(final LinearExpression a) {
        return new Builder(a).normalize().build();
    }
}

