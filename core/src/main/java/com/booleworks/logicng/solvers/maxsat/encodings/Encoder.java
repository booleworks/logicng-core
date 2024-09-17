// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * Open-WBO -- Copyright (c) 2013-2015, Ruben Martins, Vasco Manquinho, Ines
 * Lynce <p> Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom
 * the Software is furnished to do so, subject to the following conditions: <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software. <p> THE SOFTWARE IS
 * PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.booleworks.logicng.solvers.maxsat.encodings;

import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.AmoEncoding;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CardinalityEncoding;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.IncrementalStrategy;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.PbEncoding;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

/**
 * Encoders for cardinality constraints, pseudo Booleans and AMO constraints.
 * @version 2.4.0
 * @since 1.0
 */
public class Encoder {

    protected final CardinalityEncoding cardinalityEncoding;
    protected final Ladder ladder;
    protected final ModularTotalizer mtotalizer;
    protected final Totalizer totalizer;
    protected final SequentialWeightCounter swc;
    protected IncrementalStrategy incrementalStrategy;
    protected PbEncoding pbEncoding;
    protected AmoEncoding amoEncoding;

    /**
     * Constructs a new Encoder.
     * @param cardinality the cardinality constraint encoder
     */
    public Encoder(final CardinalityEncoding cardinality) {
        this(IncrementalStrategy.NONE, cardinality, AmoEncoding.LADDER, PbEncoding.SWC);
    }

    /**
     * Constructs a new Encoder.
     * @param incremental the incremental strategy
     * @param cardinality the cardinality constraint encoder
     * @param amo         the AMO constraint encoder
     * @param pb          the pseudo Boolean encoder
     */
    protected Encoder(final IncrementalStrategy incremental, final CardinalityEncoding cardinality,
                      final AmoEncoding amo, final PbEncoding pb) {
        incrementalStrategy = incremental;
        cardinalityEncoding = cardinality;
        amoEncoding = amo;
        pbEncoding = pb;
        ladder = new Ladder();
        totalizer = new Totalizer(incremental);
        mtotalizer = new ModularTotalizer();
        swc = new SequentialWeightCounter();
    }

    /**
     * Returns the cardinality encoding.
     * @return the cardinality encoding
     */
    public CardinalityEncoding cardEncoding() {
        return cardinalityEncoding;
    }

    /**
     * Sets the pseudo Boolean encoding.
     * @param enc the pseudo Boolean encoding
     */
    public void setPbEncoding(final PbEncoding enc) {
        pbEncoding = enc;
    }

    /**
     * Sets the AMO encoding.
     * @param enc the AMO encoding
     */
    public void setAmoEncoding(final AmoEncoding enc) {
        amoEncoding = enc;
    }

    /**
     * Controls the modulo value that is used in the modulo totalizer encoding.
     * @param m the module value
     */
    public void setModulo(final int m) {
        mtotalizer.setModulo(m);
    }

    /**
     * Sets the incremental strategy for the totalizer encoding.
     * @param incremental the incremental strategy
     */
    public void setIncremental(final IncrementalStrategy incremental) {
        incrementalStrategy = incremental;
        totalizer.setIncremental(incremental);
    }

    /**
     * Encodes an AMO constraint in the given solver.
     * @param s    the solver
     * @param lits the literals for the constraint
     * @throws IllegalStateException if the AMO encoding is unknown
     */
    public void encodeAmo(final LngCoreSolver s, final LngIntVector lits) {
        switch (amoEncoding) {
            case LADDER:
                ladder.encode(s, lits);
                break;
            default:
                throw new IllegalStateException("Unknown AMO encoding: " + amoEncoding);
        }
    }

    /**
     * Encodes a cardinality constraint in the given solver.
     * @param s    the solver
     * @param lits the literals for the constraint
     * @param rhs  the right-hand side of the constraint
     * @throws IllegalStateException if the cardinality encoding is unknown
     */
    public void encodeCardinality(final LngCoreSolver s, final LngIntVector lits, final int rhs) {
        switch (cardinalityEncoding) {
            case TOTALIZER:
                totalizer.build(s, lits, rhs);
                if (totalizer.hasCreatedEncoding()) {
                    totalizer.update(s, rhs);
                }
                break;
            case MTOTALIZER:
                mtotalizer.encode(s, lits, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown cardinality encoding: " + cardinalityEncoding);
        }
    }

    /**
     * Updates the cardinality constraint.
     * @param s   the solver
     * @param rhs the new right-hand side
     * @throws IllegalStateException if the cardinality encoding is unknown
     */
    public void updateCardinality(final LngCoreSolver s, final int rhs) {
        switch (cardinalityEncoding) {
            case TOTALIZER:
                totalizer.update(s, rhs);
                break;
            case MTOTALIZER:
                mtotalizer.update(s, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown cardinality encoding: " + cardinalityEncoding);
        }
    }

    /**
     * Manages the building of cardinality encodings. Currently, is only used
     * for incremental solving.
     * @param s    the solver
     * @param lits the literals for the constraint
     * @param rhs  the right-hand side of the constraint
     * @throws IllegalStateException if the cardinality encoding does not
     *                               support incrementality
     */
    public void buildCardinality(final LngCoreSolver s, final LngIntVector lits, final int rhs) {
        assert incrementalStrategy != IncrementalStrategy.NONE;
        switch (cardinalityEncoding) {
            case TOTALIZER:
                totalizer.build(s, lits, rhs);
                break;
            default:
                throw new IllegalStateException(
                        "Cardinality encoding does not support incrementality: " + incrementalStrategy);
        }
    }

    /**
     * Manages the incremental update of cardinality constraints.
     * @param s           the solver
     * @param join        the join literals
     * @param lits        the literals of the constraint
     * @param rhs         the right-hand side of the constraint
     * @param assumptions the assumptions
     * @throws IllegalStateException if the cardinality encoding does not
     *                               support incrementality
     */
    public void incUpdateCardinality(final LngCoreSolver s, final LngIntVector join, final LngIntVector lits,
                                     final int rhs, final LngIntVector assumptions) {
        assert incrementalStrategy == IncrementalStrategy.ITERATIVE;
        switch (cardinalityEncoding) {
            case TOTALIZER:
                if (!join.isEmpty()) {
                    totalizer.join(s, join, rhs);
                }
                assert !lits.isEmpty();
                totalizer.update(s, rhs, assumptions);
                break;
            default:
                throw new IllegalArgumentException(
                        "Cardinality encoding does not support incrementality: " + incrementalStrategy);
        }
    }

    /**
     * Encodes a pseudo-Boolean constraint.
     * @param s      the solver
     * @param lits   the literals of the constraint
     * @param coeffs the coefficients of the constraints
     * @param rhs    the right-hand side of the constraint
     * @throws IllegalStateException if the pseudo-Boolean encoding is unknown
     */
    public void encodePb(final LngCoreSolver s, final LngIntVector lits, final LngIntVector coeffs, final int rhs) {
        switch (pbEncoding) {
            case SWC:
                swc.encode(s, lits, coeffs, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean encoding: " + pbEncoding);
        }
    }

    /**
     * Updates a pseudo-Boolean encoding.
     * @param s   the solver
     * @param rhs the new right-hand side
     * @throws IllegalStateException if the pseudo-Boolean encoding is unknown
     */
    public void updatePb(final LngCoreSolver s, final int rhs) {
        switch (pbEncoding) {
            case SWC:
                swc.update(s, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean encoding: " + pbEncoding);
        }
    }

    /**
     * Incrementally encodes a pseudo-Boolean constraint.
     * @param s           the solver
     * @param lits        the literals of the constraint
     * @param coeffs      the coefficients of the constraint
     * @param rhs         the right-hand size of the constraint
     * @param assumptions the current assumptions
     * @param size        the size
     * @throws IllegalStateException if the pseudo-Boolean encoding is unknown
     */
    public void incEncodePb(final LngCoreSolver s, final LngIntVector lits, final LngIntVector coeffs,
                            final int rhs, final LngIntVector assumptions, final int size) {
        assert incrementalStrategy == IncrementalStrategy.ITERATIVE;
        switch (pbEncoding) {
            case SWC:
                swc.encode(s, lits, coeffs, rhs, assumptions, size);
                break;
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean encoding: " + pbEncoding);
        }
    }

    /**
     * Manages the incremental update of pseudo-Boolean encodings.
     * @param s      the solver
     * @param lits   the literals of the constraint
     * @param coeffs the coefficients of the constraint
     * @param rhs    the new right-hand side of the constraint
     * @throws IllegalStateException if the pseudo-Boolean encoding is unknown
     */
    public void incUpdatePb(final LngCoreSolver s, final LngIntVector lits, final LngIntVector coeffs, final int rhs) {
        assert incrementalStrategy == IncrementalStrategy.ITERATIVE;
        switch (pbEncoding) {
            case SWC:
                swc.updateInc(s, rhs);
                swc.join(s, lits, coeffs);
                break;
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean encoding: " + pbEncoding);
        }
    }

    /**
     * Manages the incremental update of assumptions.
     * @param assumptions the assumptions
     * @throws IllegalStateException if the pseudo-Boolean encoding is unknown
     */
    public void incUpdatePbAssumptions(final LngIntVector assumptions) {
        assert incrementalStrategy == IncrementalStrategy.ITERATIVE;
        switch (pbEncoding) {
            case SWC:
                swc.updateAssumptions(assumptions);
                break;
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean encoding: " + pbEncoding);
        }
    }

    /**
     * Returns {@code true} if the cardinality encoding was built, {@code false}
     * otherwise.
     * @return {@code true} if the cardinality encoding was built
     */
    public boolean hasCardEncoding() {
        switch (cardinalityEncoding) {
            case TOTALIZER:
                return totalizer.hasCreatedEncoding();
            case MTOTALIZER:
                return mtotalizer.hasCreatedEncoding();
            default:
                throw new IllegalStateException("Unknown cardinality encoding: " + cardinalityEncoding);
        }
    }

    /**
     * Returns {@code true} if the pseudo-Boolean encoding was built,
     * {@code false} otherwise.
     * @return {@code true} if the pseudo-Boolean encoding was built
     */
    public boolean hasPbEncoding() {
        return pbEncoding == PbEncoding.SWC && swc.hasCreatedEncoding();
    }

    /**
     * Returns the totalizer's literals.
     * @return the literals
     */
    public LngIntVector lits() {
        assert cardinalityEncoding == CardinalityEncoding.TOTALIZER &&
                incrementalStrategy == IncrementalStrategy.ITERATIVE;
        return totalizer.lits();
    }

    /**
     * Returns the totalizer's output literals.
     * @return the literals
     */
    public LngIntVector outputs() {
        assert cardinalityEncoding == CardinalityEncoding.TOTALIZER &&
                incrementalStrategy == IncrementalStrategy.ITERATIVE;
        return totalizer.outputs();
    }
}
