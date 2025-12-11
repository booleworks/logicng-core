// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;

import java.util.List;

/**
 * Incremental data for an at-most-k cardinality constraint. When an at-most-k
 * cardinality constraint is constructed, it is possible to save incremental
 * data with it. Then one can modify the constraint after it was created by
 * tightening the original bound.
 * @version 3.0.0
 * @since 1.1
 */
public final class CcIncrementalData {
    private final EncodingResult result;
    private final EncoderConfig.AmkEncoder amkEncoder;
    private final EncoderConfig.AlkEncoder alkEncoder;
    private final LngVector<? extends Literal> vector1;
    private final LngVector<? extends Literal> vector2;
    private final int mod;
    private int nVars;
    private int currentRhs;

    /**
     * Constructs new incremental data for an at-most-k encoder and the given
     * internal data.
     * @param result     the result
     * @param amkEncoder the at-most-one amkEncoder
     * @param rhs        the current right-hand-side
     * @param vector1    the first internal vector
     * @param vector2    the second internal vector
     * @param mod        the modulo value
     */
    public CcIncrementalData(final EncodingResult result, final EncoderConfig.AmkEncoder amkEncoder, final int rhs,
                             final LngVector<? extends Literal> vector1, final LngVector<? extends Literal> vector2,
                             final int mod) {
        this.result = result;
        this.amkEncoder = amkEncoder;
        alkEncoder = null;
        currentRhs = rhs;
        this.vector1 = vector1;
        this.vector2 = vector2;
        this.mod = mod;
    }

    /**
     * Constructs new incremental data for an at-most-k encoder and the given
     * internal data.
     * @param result  the result
     * @param encoder the at-most-one amkEncoder
     * @param rhs     the current right-hand-side
     * @param vector1 the first internal vector
     */
    public CcIncrementalData(final EncodingResult result, final EncoderConfig.AmkEncoder encoder, final int rhs,
                             final LngVector<? extends Literal> vector1) {
        this(result, encoder, rhs, vector1, null, -1);
    }

    /**
     * Constructs new incremental data for an at-least-k encoder and the given
     * internal data.
     * @param result     the result
     * @param alkEncoder the at-least-one amkEncoder
     * @param rhs        the current right-hand-side
     * @param nVars      the number of variables
     * @param vector1    the first internal vector
     * @param vector2    the second internal vector
     * @param mod        the modulo value
     */
    public CcIncrementalData(final EncodingResult result, final EncoderConfig.AlkEncoder alkEncoder, final int rhs,
                             final int nVars,
                             final LngVector<? extends Literal> vector1, final LngVector<? extends Literal> vector2,
                             final int mod) {
        this.result = result;
        this.alkEncoder = alkEncoder;
        amkEncoder = null;
        currentRhs = rhs;
        this.nVars = nVars;
        this.vector1 = vector1;
        this.vector2 = vector2;
        this.mod = mod;

    }

    /**
     * Constructs new incremental data for an at-least-k encoder and the given
     * internal data.
     * @param result     the result
     * @param alkEncoder the at-most-one amkEncoder
     * @param rhs        the current right-hand-side
     * @param nVars      the number of variables
     * @param vector1    the first internal vector
     */
    public CcIncrementalData(final EncodingResult result, final EncoderConfig.AlkEncoder alkEncoder, final int rhs,
                             final int nVars, final LngVector<? extends Literal> vector1) {
        this(result, alkEncoder, rhs, nVars, vector1, null, -1);
    }

    /**
     * Tightens the upper bound of an at-most-k constraint and returns the
     * resulting formula.
     * @param rhs the new upperBound
     * @return the incremental encoding of the new upper bound
     */
    public List<Formula> newUpperBound(final int rhs) {
        computeUbConstraint(result, rhs);
        return result.getResult();
    }

    /**
     * Tightens the upper bound of an at-most-k constraint and encodes it on the
     * solver of the result.
     * <p>
     * Usage constraints: -New right-hand side must be smaller than current
     * right-hand side. -Cannot be used for at-least-k constraints.
     * @param rhs the new upperBound
     */
    public void newUpperBoundForSolver(final int rhs) {
        computeUbConstraint(result, rhs);
    }

    private void computeUbConstraint(final EncodingResult result, final int rhs) {
        final FormulaFactory f = result.getFactory();
        if (rhs >= currentRhs) {
            throw new IllegalArgumentException(
                    "New upper bound " + rhs + " + does not tighten the current bound of " + currentRhs);
        }
        currentRhs = rhs;
        if (amkEncoder == null) {
            throw new IllegalStateException("Cannot encode a new upper bound for an at-most-k constraint");
        }
        switch (amkEncoder) {
            case MODULAR_TOTALIZER:
                assert !vector1.isEmpty() || !vector2.isEmpty();
                final int ulimit = (rhs + 1) / mod;
                final int llimit = (rhs + 1) - ulimit * mod;
                assert ulimit <= vector1.size();
                assert llimit <= vector2.size();
                for (int i = ulimit; i < vector1.size(); i++) {
                    result.addClause(vector1.get(i).negate(f));
                }
                if (ulimit != 0 && llimit != 0) {
                    for (int i = llimit - 1; i < vector2.size(); i++) {
                        result.addClause(vector1.get(ulimit - 1).negate(f), vector2.get(i).negate(f));
                    }
                } else {
                    if (ulimit == 0) {
                        assert llimit != 0;
                        for (int i = llimit - 1; i < vector2.size(); i++) {
                            result.addClause(vector2.get(i).negate(f));
                        }
                    } else {
                        result.addClause(vector1.get(ulimit - 1).negate(f));
                    }
                }
                break;
            case TOTALIZER:
                for (int i = rhs; i < vector1.size(); i++) {
                    result.addClause(vector1.get(i).negate(f));
                }
                break;
            case CARDINALITY_NETWORK:
                if (vector1.size() > rhs) {
                    result.addClause(vector1.get(rhs).negate(f));
                }
                break;
            default:
                throw new IllegalStateException("Unknown at-most-k encoder: " + amkEncoder);
        }
    }

    /**
     * Tightens the lower bound of an at-least-k constraint and returns the
     * resulting formula.
     * @param rhs the new upperBound
     * @return the incremental encoding of the new lower bound
     */
    public List<Formula> newLowerBound(final int rhs) {
        computeLbConstraint(result, rhs);
        return result.getResult();
    }

    /**
     * Tightens the lower bound of an at-least-k constraint and encodes it on
     * the solver of the result.
     * <p>
     * Usage constraints: -New right-hand side must be greater than current
     * right-hand side. -Cannot be used for at-most-k constraints.
     * @param rhs the new upperBound
     */
    public void newLowerBoundForSolver(final int rhs) {
        computeLbConstraint(result, rhs);
    }

    private void computeLbConstraint(final EncodingResult result, final int rhs) {
        final FormulaFactory f = result.getFactory();
        if (rhs <= currentRhs) {
            throw new IllegalArgumentException(
                    "New lower bound " + rhs + " + does not tighten the current bound of " + currentRhs);
        }
        currentRhs = rhs;
        if (alkEncoder == null) {
            throw new IllegalStateException("Cannot encode a new lower bound for an at-least-k constraint");
        }
        switch (alkEncoder) {
            case TOTALIZER:
                for (int i = 0; i < rhs; i++) {
                    result.addClause(vector1.get(i));
                }
                break;
            case MODULAR_TOTALIZER:
                int newRhs = nVars - rhs;
                assert !vector1.isEmpty() || !vector2.isEmpty();
                final int ulimit = (newRhs + 1) / mod;
                final int llimit = (newRhs + 1) - ulimit * mod;
                assert ulimit <= vector1.size();
                assert llimit <= vector2.size();
                for (int i = ulimit; i < vector1.size(); i++) {
                    result.addClause(vector1.get(i).negate(f));
                }
                if (ulimit != 0 && llimit != 0) {
                    for (int i = llimit - 1; i < vector2.size(); i++) {
                        result.addClause(vector1.get(ulimit - 1).negate(f), vector2.get(i).negate(f));
                    }
                } else {
                    if (ulimit == 0) {
                        assert llimit != 0;
                        for (int i = llimit - 1; i < vector2.size(); i++) {
                            result.addClause(vector2.get(i).negate(f));
                        }
                    } else {
                        result.addClause(vector1.get(ulimit - 1).negate(f));
                    }
                }
                break;
            case CARDINALITY_NETWORK:
                newRhs = nVars - rhs;
                if (vector1.size() > newRhs) {
                    result.addClause(vector1.get(newRhs).negate(f));
                }
                break;
            default:
                throw new IllegalStateException("Unknown at-least-k encoder: " + alkEncoder);
        }
    }

    /**
     * Returns the current right-hand side of this CCIncrementalData.
     * @return the current right-hand side of this CCIncrementalData.
     */
    public int getCurrentRhs() {
        return currentRhs;
    }

    @Override
    public String toString() {
        return "CcIncrementalData{" +
                ", amkEncoder=" + amkEncoder +
                ", alkEncoder=" + alkEncoder +
                ", vector1=" + vector1 +
                ", vector2=" + vector2 +
                ", mod=" + mod +
                ", currentRhs=" + currentRhs +
                '}';
    }
}
