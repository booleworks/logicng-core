// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import org.logicng.collections.LNGIntVector;
import org.logicng.collections.LNGVector;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;
import org.logicng.datastructures.Tristate;
import org.logicng.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * A pseudo-Boolean constraint of the form {@code c_1 * l_1 + ... + c_n * l_n R k} where {@code R} is one of
 * {@code =, >, >=, <, <=}.
 * @version 3.0.0
 * @since 1.0
 */
public class PBConstraint extends LngCachedFormula {

    private static final Iterator<Formula> ITERATOR = new Iterator<>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Formula next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

    protected final Literal[] literals;
    protected final int[] coefficients;
    protected CType comparator;
    protected int rhs;
    protected int hashCode;
    protected int maxWeight;

    /**
     * Constructs a new pseudo-Boolean constraint.
     * @param literals     the literals
     * @param coefficients the coefficients
     * @param comparator   the comparator
     * @param rhs          the right-hand side
     * @param f            the formula factory
     * @throws IllegalArgumentException if the number of literals and coefficients do not correspond
     */
    PBConstraint(final Literal[] literals, final int[] coefficients, final CType comparator, final int rhs, final FormulaFactory f) {
        super(FType.PBC, f);
        if (literals.length != coefficients.length) {
            throw new IllegalArgumentException("Cannot generate a pseudo-Boolean constraint with literals.length != coefficients.length");
        }
        this.literals = literals;
        this.coefficients = coefficients;
        this.maxWeight = Integer.MIN_VALUE;
        for (final int c : coefficients) {
            if (c > this.maxWeight) {
                this.maxWeight = c;
            }
        }
        this.comparator = comparator;
        this.rhs = rhs;
        this.hashCode = 0;
    }

    /**
     * Returns the GCD of two given values.
     * @param small the smaller value
     * @param big   the larger value
     * @return the GCD of the two values
     */
    private static int gcd(final int small, final int big) {
        return small == 0 ? big : gcd(big % small, small);
    }

    /**
     * Internal helper for checking if a given coefficient-sum min- and max-value can comply with a given right-hand-side
     * according to this PBConstraint's comparator.
     * @param minValue   the minimum coefficient sum
     * @param maxValue   the maximum coefficient sum
     * @param rhs        the right-hand-side
     * @param comparator the comparator
     * @return {@link Tristate#TRUE} if the constraint is true, {@link Tristate#FALSE} if it is false and
     * {@link Tristate#UNDEF} if both are still possible
     */
    static Tristate evaluateCoeffs(final int minValue, final int maxValue, final int rhs, final CType comparator) {
        int status = 0;
        if (rhs >= minValue) {
            status++;
        }
        if (rhs > minValue) {
            status++;
        }
        if (rhs >= maxValue) {
            status++;
        }
        if (rhs > maxValue) {
            status++;
        }

        switch (comparator) {
            case EQ:
                return (status == 0 || status == 4) ? Tristate.FALSE : Tristate.UNDEF;
            case LE:
                return status >= 3 ? Tristate.TRUE : (status < 1 ? Tristate.FALSE : Tristate.UNDEF);
            case LT:
                return status > 3 ? Tristate.TRUE : (status <= 1 ? Tristate.FALSE : Tristate.UNDEF);
            case GE:
                return status <= 1 ? Tristate.TRUE : (status > 3 ? Tristate.FALSE : Tristate.UNDEF);
            case GT:
                return status < 1 ? Tristate.TRUE : (status >= 3 ? Tristate.FALSE : Tristate.UNDEF);
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean comparator: " + comparator);
        }
    }

    /**
     * Returns the literals of this constraint.
     * @return the literals of this constraint
     */
    public Literal[] operands() {
        return Arrays.copyOf(literals, literals.length);
    }

    /**
     * Returns the coefficients of this constraint.
     * @return the coefficients of this constraint
     */
    public int[] coefficients() {
        return Arrays.copyOf(coefficients, coefficients.length);
    }

    /**
     * Returns the comparator of this constraint.
     * @return the comparator of this constraint
     */
    public CType comparator() {
        return comparator;
    }

    /**
     * Returns the right-hand side of this constraint.
     * @return the right-hand side of this constraint
     */
    public int rhs() {
        return rhs;
    }

    /**
     * Returns {@code true} if this constraint is a cardinality constraint, {@code false} otherwise.
     * @return {@code true} if this constraint is a cardinality constraint
     */
    public boolean isCC() {
        return false;
    }

    /**
     * Returns {@code true} if this constraint is an at-most-one cardinality constraint, {@code false} otherwise.
     * @return {@code true} if this constraint is an at-most-one cardinality constraint
     */
    public boolean isAmo() {
        return false;
    }

    /**
     * Returns {@code true} if this constraint is an exactly-one cardinality constraint, {@code false} otherwise.
     * @return {@code true} if this constraint is an excatly-one cardinality constraint
     */
    public boolean isExo() {
        return false;
    }

    /**
     * Returns the maximal coefficient of this constraint.
     * @return the maximal coefficient of this constraint
     */
    public int maxWeight() {
        return maxWeight;
    }

    /**
     * Normalizes this constraint s.t. it can be converted to CNF.
     * @return the normalized constraint
     */
    public Formula normalize() {
        final LNGVector<Literal> normPs = new LNGVector<>(literals.length);
        final LNGIntVector normCs = new LNGIntVector(literals.length);
        int normRhs;
        switch (comparator) {
            case EQ:
                for (int i = 0; i < literals.length; i++) {
                    normPs.push(literals[i]);
                    normCs.push(coefficients[i]);
                }
                normRhs = rhs;
                final Formula f1 = normalize(normPs, normCs, normRhs);
                normPs.clear();
                normCs.clear();
                for (int i = 0; i < literals.length; i++) {
                    normPs.push(literals[i]);
                    normCs.push(-coefficients[i]);
                }
                normRhs = -rhs;
                final Formula f2 = normalize(normPs, normCs, normRhs);
                return f.and(f1, f2);
            case LT:
            case LE:
                for (int i = 0; i < literals.length; i++) {
                    normPs.push(literals[i]);
                    normCs.push(coefficients[i]);
                }
                normRhs = comparator == CType.LE ? rhs : rhs - 1;
                return normalize(normPs, normCs, normRhs);
            case GT:
            case GE:
                for (int i = 0; i < literals.length; i++) {
                    normPs.push(literals[i]);
                    normCs.push(-coefficients[i]);
                }
                normRhs = comparator == CType.GE ? -rhs : -rhs - 1;
                return normalize(normPs, normCs, normRhs);
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean comparator: " + comparator);
        }
    }

    /**
     * Internal helper for normalization of a <= constraint. Can also be used for >= constraints by multiplying the right
     * side and the coefficients with -1.
     * @param ps  the literals
     * @param cs  the coefficients
     * @param rhs the right-hand side
     * @return the normalized constraint
     */
    private Formula normalize(final LNGVector<Literal> ps, final LNGIntVector cs, final int rhs) {
        int c = rhs;
        int newSize = 0;
        for (int i = 0; i < ps.size(); i++) {
            if (cs.get(i) != 0) {
                ps.set(newSize, ps.get(i));
                cs.set(newSize, cs.get(i));
                newSize++;
            }
        }
        ps.removeElements(ps.size() - newSize);
        cs.removeElements(cs.size() - newSize);
        final SortedMap<Literal, Pair<Integer, Integer>> var2consts = new TreeMap<>();
        for (int i = 0; i < ps.size(); i++) {
            final Variable x = ps.get(i).variable();
            Pair<Integer, Integer> consts = var2consts.get(x);
            if (consts == null) {
                consts = new Pair<>(0, 0);
            }
            if (!ps.get(i).phase()) {
                var2consts.put(x, new Pair<>(consts.first() + cs.get(i), consts.second()));
            } else {
                var2consts.put(x, new Pair<>(consts.first(), consts.second() + cs.get(i)));
            }
        }
        final LNGVector<Pair<Integer, Literal>> csps = new LNGVector<>(var2consts.size());
        for (final Map.Entry<Literal, Pair<Integer, Integer>> all : var2consts.entrySet()) {
            if (all.getValue().first() < all.getValue().second()) {
                c -= all.getValue().first();
                csps.push(new Pair<>(all.getValue().second() - all.getValue().first(), all.getKey()));
            } else {
                c -= all.getValue().second();
                csps.push(new Pair<>(all.getValue().first() - all.getValue().second(), all.getKey().negate()));
            }
        }
        int sum = 0;
        int zeros = 0;
        cs.clear();
        ps.clear();
        for (final Pair<Integer, Literal> pair : csps) {
            if (pair.first() != 0) {
                cs.push(pair.first());
                ps.push(pair.second());
                sum += cs.back();
            } else {
                zeros++;
            }
        }
        ps.removeElements(ps.size() - csps.size() - zeros);
        cs.removeElements(cs.size() - csps.size() - zeros);
        boolean changed;
        do {
            changed = false;
            if (c < 0) {
                return f.falsum();
            }
            if (sum <= c) {
                return f.verum();
            }
            assert cs.size() > 0;
            int div = c;
            for (int i = 0; i < cs.size(); i++) {
                div = gcd(div, cs.get(i));
            }
            if (div != 0 && div != 1) {
                for (int i = 0; i < cs.size(); i++) {
                    cs.set(i, cs.get(i) / div);
                }
                c = c / div;
            }
            if (div != 1 && div != 0) {
                changed = true;
            }
        } while (changed);
        final Literal[] lits = new Literal[ps.size()];
        for (int i = 0; i < lits.length; i++) {
            lits[i] = ps.get(i);
        }
        final int[] coeffs = new int[cs.size()];
        for (int i = 0; i < coeffs.length; i++) {
            coeffs[i] = cs.get(i);
        }
        return f.pbc(CType.LE, c, lits, coeffs);
    }

    @Override
    public int numberOfOperands() {
        return 0;
    }

    @Override
    public boolean isConstantFormula() {
        return false;
    }

    @Override
    public boolean isAtomicFormula() {
        return true;
    }

    @Override
    public boolean containsVariable(final Variable variable) {
        for (final Literal lit : literals) {
            if (lit.containsVariable(variable)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean evaluate(final Assignment assignment) {
        final int lhs = evaluateLHS(assignment);
        return evaluateComparator(lhs);
    }

    @Override
    public Formula restrict(final Assignment assignment) {
        final List<Literal> newLits = new ArrayList<>();
        final List<Integer> newCoeffs = new ArrayList<>();
        int lhsFixed = 0;
        int minValue = 0;
        int maxValue = 0;
        for (int i = 0; i < literals.length; i++) {
            final Formula restriction = assignment.restrictLit(literals[i]);
            if (restriction.type() == FType.LITERAL) {
                newLits.add(literals[i]);
                final int coeff = coefficients[i];
                newCoeffs.add(coeff);
                if (coeff > 0) {
                    maxValue += coeff;
                } else {
                    minValue += coeff;
                }
            } else if (restriction.type() == FType.TRUE) {
                lhsFixed += coefficients[i];
            }
        }

        if (newLits.isEmpty()) {
            return f.constant(evaluateComparator(lhsFixed));
        }

        final int newRHS = rhs - lhsFixed;
        if (comparator != CType.EQ) {
            final Tristate fixed = evaluateCoeffs(minValue, maxValue, newRHS, comparator);
            if (fixed == Tristate.TRUE) {
                return f.verum();
            } else if (fixed == Tristate.FALSE) {
                return f.falsum();
            }
        }
        return f.pbc(comparator, newRHS, newLits, newCoeffs);
    }

    @Override
    public boolean containsNode(final Formula formula) {
        if (this == formula || equals(formula)) {
            return true;
        }
        if (formula.type() == FType.LITERAL) {
            for (final Literal lit : literals) {
                if (lit.equals(formula) || lit.variable().equals(formula)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public Formula substitute(final Substitution substitution) {
        final List<Literal> newLits = new ArrayList<>();
        final List<Integer> newCoeffs = new ArrayList<>();
        int lhsFixed = 0;
        for (int i = 0; i < literals.length; i++) {
            final Formula subst = substitution.getSubstitution(literals[i].variable());
            if (subst == null) {
                newLits.add(literals[i]);
                newCoeffs.add(coefficients[i]);
            } else {
                switch (subst.type()) {
                    case TRUE:
                        if (literals[i].phase()) {
                            lhsFixed += coefficients[i];
                        }
                        break;
                    case FALSE:
                        if (!literals[i].phase()) {
                            lhsFixed += coefficients[i];
                        }
                        break;
                    case LITERAL:
                        newLits.add(literals[i].phase() ? (Literal) subst : ((Literal) subst).negate());
                        newCoeffs.add(coefficients[i]);
                        break;
                    default:
                        throw new IllegalArgumentException("Cannot substitute a formula for a literal in a pseudo-Boolean constraint");
                }
            }
        }
        return newLits.isEmpty()
                ? evaluateComparator(lhsFixed) ? f.verum() : f.falsum()
                : f.pbc(comparator, rhs - lhsFixed, newLits, newCoeffs);
    }

    @Override
    public Formula negate() {
        switch (comparator) {
            case EQ:
                return f.or(f.pbc(CType.LT, rhs, literals, coefficients), f.pbc(CType.GT, rhs, literals, coefficients));
            case LE:
                return f.pbc(CType.GT, rhs, literals, coefficients);
            case LT:
                return f.pbc(CType.GE, rhs, literals, coefficients);
            case GE:
                return f.pbc(CType.LT, rhs, literals, coefficients);
            case GT:
                return f.pbc(CType.LE, rhs, literals, coefficients);
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean comparator");
        }
    }

    /**
     * Returns the evaluation of the left-hand side of this constraint.
     * @param assignment the assignment
     * @return the evaluation of the left-hand side of this constraint
     */
    private int evaluateLHS(final Assignment assignment) {
        int lhs = 0;
        for (int i = 0; i < literals.length; i++) {
            if (literals[i].evaluate(assignment)) {
                lhs += coefficients[i];
            }
        }
        return lhs;
    }

    /**
     * Computes the result of evaluating the comparator with a given left-hand side.
     * @param lhs the left-hand side
     * @return {@code true} if the comparator evaluates to true, {@code false} otherwise
     */
    private boolean evaluateComparator(final int lhs) {
        switch (comparator) {
            case EQ:
                return lhs == rhs;
            case LE:
                return lhs <= rhs;
            case LT:
                return lhs < rhs;
            case GE:
                return lhs >= rhs;
            case GT:
                return lhs > rhs;
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean comparator");
        }
    }

    /**
     * Encodes this constraint as CNF and stores the result, if the encoding does not already exist.
     * @return the encoding
     */
    public List<Formula> getEncoding() {
        List<Formula> encoding = f.pbEncodingCache.get(this);
        if (encoding == null) {
            encoding = f.pbEncoder().encode(this);
            f.pbEncodingCache.put(this, encoding);
        }
        return Collections.unmodifiableList(encoding);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int temp = comparator.hashCode() + rhs;
            for (int i = 0; i < literals.length; i++) {
                temp += 11 * literals[i].hashCode();
                temp += 13 * coefficients[i];
            }
            hashCode = temp;
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Formula && f == ((Formula) other).factory()) {
            return false;
        }
        if (other instanceof PBConstraint) {
            final PBConstraint o = (PBConstraint) other;
            return rhs == o.rhs && comparator == o.comparator
                    && Arrays.equals(coefficients, o.coefficients)
                    && Arrays.equals(literals, o.literals);
        }
        return false;
    }

    @Override
    public Iterator<Formula> iterator() {
        return ITERATOR;
    }

    @Override
    public Stream<Formula> stream() {
        return Stream.empty();
    }
}
