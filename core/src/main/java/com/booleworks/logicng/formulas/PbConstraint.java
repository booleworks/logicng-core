// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Substitution;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * A pseudo-Boolean constraint of the form
 * {@code c_1 * l_1 + ... + c_n * l_n R k} where {@code R} is one of
 * {@code =, >, >=, <, <=}.
 * @version 3.0.0
 * @since 1.0
 */
public interface PbConstraint extends Formula {
    Iterator<Formula> ITERATOR = new Iterator<>() {
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
     * Internal helper for checking if a given coefficient-sum min- and
     * max-value can comply with a given right-hand-side according to this
     * PBConstraint's comparator.
     * @param minValue   the minimum coefficient sum
     * @param maxValue   the maximum coefficient sum
     * @param rhs        the right-hand-side
     * @param comparator the comparator
     * @return {@link Tristate#TRUE} if the constraint is true,
     * {@link Tristate#FALSE} if it is false and {@link Tristate#UNDEF}
     * if both are still possible
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
    List<Literal> getOperands();

    /**
     * Returns the coefficients of this constraint.
     * @return the coefficients of this constraint
     */
    List<Integer> getCoefficients();

    /**
     * Returns the comparator of this constraint.
     * @return the comparator of this constraint
     */
    CType comparator();

    /**
     * Returns the right-hand side of this constraint.
     * @return the right-hand side of this constraint
     */
    int getRhs();

    /**
     * Returns {@code true} if this constraint is a cardinality constraint,
     * {@code false} otherwise.
     * @return {@code true} if this constraint is a cardinality constraint
     */
    default boolean isCc() {
        return false;
    }

    /**
     * Returns {@code true} if this constraint is an at-most-one cardinality
     * constraint, {@code false} otherwise.
     * @return {@code true} if this constraint is an at-most-one cardinality
     * constraint
     */
    default boolean isAmo() {
        return false;
    }

    /**
     * Returns {@code true} if this constraint is an exactly-one cardinality
     * constraint, {@code false} otherwise.
     * @return {@code true} if this constraint is an exactly-one cardinality
     * constraint
     */
    default boolean isExo() {
        return false;
    }

    /**
     * Returns the maximal coefficient of this constraint.
     * @return the maximal coefficient of this constraint
     */
    int maxWeight();

    /**
     * Normalizes this constraint s.t. it can be converted to CNF.
     * @param f the formula factory to generate new formulas
     * @return the normalized constraint
     */
    default Formula normalize(final FormulaFactory f) {
        final LngVector<Literal> normPs = new LngVector<>(getOperands().size());
        final LngIntVector normCs = new LngIntVector(getOperands().size());
        int normRhs;
        switch (comparator()) {
            case EQ:
                for (int i = 0; i < getOperands().size(); i++) {
                    normPs.push(getOperands().get(i));
                    normCs.push(getCoefficients().get(i));
                }
                normRhs = getRhs();
                final Formula f1 = normalize(f, normPs, normCs, normRhs);
                normPs.clear();
                normCs.clear();
                for (int i = 0; i < getOperands().size(); i++) {
                    normPs.push(getOperands().get(i));
                    normCs.push(-getCoefficients().get(i));
                }
                normRhs = -getRhs();
                final Formula f2 = normalize(f, normPs, normCs, normRhs);
                return f.and(f1, f2);
            case LT:
            case LE:
                for (int i = 0; i < getOperands().size(); i++) {
                    normPs.push(getOperands().get(i));
                    normCs.push(getCoefficients().get(i));
                }
                normRhs = comparator() == CType.LE ? getRhs() : getRhs() - 1;
                return normalize(f, normPs, normCs, normRhs);
            case GT:
            case GE:
                for (int i = 0; i < getOperands().size(); i++) {
                    normPs.push(getOperands().get(i));
                    normCs.push(-getCoefficients().get(i));
                }
                normRhs = comparator() == CType.GE ? -getRhs() : -getRhs() - 1;
                return normalize(f, normPs, normCs, normRhs);
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean comparator: " + comparator());
        }
    }

    /**
     * Internal helper for normalization of a <= constraint. Can also be used
     * for >= constraints by multiplying the right side and the coefficients
     * with -1.
     * @param f   the formula factory to generate new formulas
     * @param ps  the literals
     * @param cs  the coefficients
     * @param rhs the right-hand side
     * @return the normalized constraint
     */
    private Formula normalize(final FormulaFactory f, final LngVector<Literal> ps, final LngIntVector cs,
                              final int rhs) {
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
            if (!ps.get(i).getPhase()) {
                var2consts.put(x, new Pair<>(consts.getFirst() + cs.get(i), consts.getSecond()));
            } else {
                var2consts.put(x, new Pair<>(consts.getFirst(), consts.getSecond() + cs.get(i)));
            }
        }
        final LngVector<Pair<Integer, Literal>> csps = new LngVector<>(var2consts.size());
        for (final Map.Entry<Literal, Pair<Integer, Integer>> all : var2consts.entrySet()) {
            if (all.getValue().getFirst() < all.getValue().getSecond()) {
                c -= all.getValue().getFirst();
                csps.push(new Pair<>(all.getValue().getSecond() - all.getValue().getFirst(), all.getKey()));
            } else {
                c -= all.getValue().getSecond();
                csps.push(new Pair<>(all.getValue().getFirst() - all.getValue().getSecond(), all.getKey().negate(f)));
            }
        }
        int sum = 0;
        int zeros = 0;
        cs.clear();
        ps.clear();
        for (final Pair<Integer, Literal> pair : csps) {
            if (pair.getFirst() != 0) {
                cs.push(pair.getFirst());
                ps.push(pair.getSecond());
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
            assert !cs.isEmpty();
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
    default int numberOfOperands() {
        return 0;
    }

    @Override
    default boolean isConstantFormula() {
        return false;
    }

    @Override
    default boolean isAtomicFormula() {
        return true;
    }

    @Override
    default boolean containsVariable(final Variable variable) {
        for (final Literal lit : getOperands()) {
            if (lit.containsVariable(variable)) {
                return true;
            }
        }
        return false;
    }

    @Override
    default boolean evaluate(final Assignment assignment) {
        final int lhs = evaluateLhs(assignment);
        return evaluateComparator(lhs);
    }

    @Override
    default Formula restrict(final FormulaFactory f, final Assignment assignment) {
        final List<Literal> newLits = new ArrayList<>();
        final List<Integer> newCoeffs = new ArrayList<>();
        int lhsFixed = 0;
        int minValue = 0;
        int maxValue = 0;
        for (int i = 0; i < getOperands().size(); i++) {
            final Formula restriction = assignment.restrictLit(f, getOperands().get(i));
            if (restriction.getType() == FType.LITERAL) {
                newLits.add(getOperands().get(i));
                final int coeff = getCoefficients().get(i);
                newCoeffs.add(coeff);
                if (coeff > 0) {
                    maxValue += coeff;
                } else {
                    minValue += coeff;
                }
            } else if (restriction.getType() == FType.TRUE) {
                lhsFixed += getCoefficients().get(i);
            }
        }

        if (newLits.isEmpty()) {
            return f.constant(evaluateComparator(lhsFixed));
        }

        final int newRhs = getRhs() - lhsFixed;
        if (comparator() != CType.EQ) {
            final Tristate fixed = evaluateCoeffs(minValue, maxValue, newRhs, comparator());
            if (fixed == Tristate.TRUE) {
                return f.verum();
            } else if (fixed == Tristate.FALSE) {
                return f.falsum();
            }
        }
        return f.pbc(comparator(), newRhs, newLits, newCoeffs);
    }

    @Override
    default boolean containsNode(final Formula formula) {
        if (this == formula || equals(formula)) {
            return true;
        }
        if (formula.getType() == FType.LITERAL) {
            for (final Literal lit : getOperands()) {
                if (lit.equals(formula) || lit.variable().equals(formula)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    default Formula substitute(final FormulaFactory f, final Substitution substitution) {
        final List<Literal> newLits = new ArrayList<>();
        final List<Integer> newCoeffs = new ArrayList<>();
        int lhsFixed = 0;
        for (int i = 0; i < getOperands().size(); i++) {
            final Formula subst = substitution.getSubstitution(getOperands().get(i).variable());
            if (subst == null) {
                newLits.add(getOperands().get(i));
                newCoeffs.add(getCoefficients().get(i));
            } else {
                switch (subst.getType()) {
                    case TRUE:
                        if (getOperands().get(i).getPhase()) {
                            lhsFixed += getCoefficients().get(i);
                        }
                        break;
                    case FALSE:
                        if (!getOperands().get(i).getPhase()) {
                            lhsFixed += getCoefficients().get(i);
                        }
                        break;
                    case LITERAL:
                        newLits.add(getOperands().get(i).getPhase() ? (Literal) subst : ((Literal) subst).negate(f));
                        newCoeffs.add(getCoefficients().get(i));
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Cannot substitute a formula for a literal in a pseudo-Boolean constraint");
                }
            }
        }
        return newLits.isEmpty() ? evaluateComparator(lhsFixed) ? f.verum() : f.falsum()
                : f.pbc(comparator(), getRhs() - lhsFixed, newLits, newCoeffs);
    }

    @Override
    default Formula negate(final FormulaFactory f) {
        switch (comparator()) {
            case EQ:
                return f.or(f.pbc(CType.LT, getRhs(), getOperands(), getCoefficients()),
                        f.pbc(CType.GT, getRhs(), getOperands(), getCoefficients()));
            case LE:
                return f.pbc(CType.GT, getRhs(), getOperands(), getCoefficients());
            case LT:
                return f.pbc(CType.GE, getRhs(), getOperands(), getCoefficients());
            case GE:
                return f.pbc(CType.LT, getRhs(), getOperands(), getCoefficients());
            case GT:
                return f.pbc(CType.LE, getRhs(), getOperands(), getCoefficients());
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean comparator");
        }
    }

    /**
     * Returns the evaluation of the left-hand side of this constraint.
     * @param assignment the assignment
     * @return the evaluation of the left-hand side of this constraint
     */
    private int evaluateLhs(final Assignment assignment) {
        int lhs = 0;
        for (int i = 0; i < getOperands().size(); i++) {
            if (getOperands().get(i).evaluate(assignment)) {
                lhs += getCoefficients().get(i);
            }
        }
        return lhs;
    }

    /**
     * Computes the result of evaluating the comparator with a given left-hand
     * side.
     * @param lhs the left-hand side
     * @return {@code true} if the comparator evaluates to true, {@code false}
     * otherwise
     */
    private boolean evaluateComparator(final int lhs) {
        switch (comparator()) {
            case EQ:
                return lhs == getRhs();
            case LE:
                return lhs <= getRhs();
            case LT:
                return lhs < getRhs();
            case GE:
                return lhs >= getRhs();
            case GT:
                return lhs > getRhs();
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean comparator");
        }
    }

    default int computeHash() {
        int hashCode = comparator().hashCode() + getRhs();
        for (int i = 0; i < getOperands().size(); i++) {
            hashCode += 11 * getOperands().get(i).hashCode();
            hashCode += 13 * getCoefficients().get(i);
        }
        return hashCode;
    }

    /**
     * Encodes this constraint as CNF and stores the result, if the encoding
     * does not already exist.
     * @param f the formula factory to generate new formulas
     * @return the encoding
     */
    List<Formula> getEncoding(final FormulaFactory f);

    @Override
    default Iterator<Formula> iterator() {
        return ITERATOR;
    }

    @Override
    default Stream<Formula> stream() {
        return Stream.empty();
    }
}
