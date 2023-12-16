// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * Open-WBO -- Copyright (c) 2013-2015, Ruben Martins, Vasco Manquinho, Ines Lynce
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.booleworks.logicng.solvers.maxsat.encodings;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSAT;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig;
import com.booleworks.logicng.solvers.sat.MiniSatStyleSolver;

/**
 * Encodes that at most 'rhs' literals can be assigned value true.  Uses the totalizer encoding for
 * translating the cardinality constraint into CNF.
 * @version 2.4.0
 * @since 1.0
 */
public class Totalizer extends Encoding {

    protected final LNGVector<LNGIntVector> totalizerIterativeLeft;
    protected final LNGVector<LNGIntVector> totalizerIterativeRight;
    protected final LNGVector<LNGIntVector> totalizerIterativeOutput;
    protected final LNGIntVector totalizerIterativeRhs;
    protected final int blocking;
    protected final LNGIntVector cardinalityOutlits;
    protected LNGIntVector cardinalityInlits;
    protected MaxSATConfig.IncrementalStrategy incrementalStrategy;
    protected int currentCardinalityRhs;
    protected boolean joinMode;
    private LNGIntVector ilits;

    /**
     * Constructs a new totalizer with a given incremental strategy.
     * @param strategy the incremental strategy
     */
    Totalizer(final MaxSATConfig.IncrementalStrategy strategy) {
        blocking = MiniSatStyleSolver.LIT_UNDEF;
        joinMode = false;
        currentCardinalityRhs = -1;
        incrementalStrategy = strategy;
        totalizerIterativeLeft = new LNGVector<>();
        totalizerIterativeRight = new LNGVector<>();
        totalizerIterativeOutput = new LNGVector<>();
        totalizerIterativeRhs = new LNGIntVector();
        cardinalityInlits = new LNGIntVector();
        cardinalityOutlits = new LNGIntVector();
        ilits = new LNGIntVector();
    }

    /**
     * Updates the right-hand side.
     * @param s   the solver
     * @param rhs the new right-hand side
     */
    public void update(final MiniSatStyleSolver s, final int rhs) {
        final LNGIntVector assumptions = new LNGIntVector();
        update(s, rhs, assumptions);
    }

    /**
     * Returns {@code true} if an encoding was created, {@code false} otherwise.
     * @return {@code true} if an encoding was created
     */
    boolean hasCreatedEncoding() {
        return hasEncoding;
    }

    /**
     * Sets the incremental strategy.
     * @param incremental the incremental strategy
     */
    public void setIncremental(final MaxSATConfig.IncrementalStrategy incremental) {
        incrementalStrategy = incremental;
    }

    /**
     * Returns the incremental strategy.
     * @return the incremental strategy
     */
    public MaxSATConfig.IncrementalStrategy incremental() {
        return incrementalStrategy;
    }

    /**
     * Joins two constraints.  The given constraint is added to the current one.
     * @param s    the solver
     * @param lits the literals of the constraint
     * @param rhs  the right-hand side of the constraint
     */
    void join(final MiniSatStyleSolver s, final LNGIntVector lits, final int rhs) {
        assert incrementalStrategy == MaxSATConfig.IncrementalStrategy.ITERATIVE;
        final LNGIntVector leftCardinalityOutlits = new LNGIntVector(cardinalityOutlits);
        final int oldCardinality = currentCardinalityRhs;
        if (lits.size() > 1) {
            build(s, lits, Math.min(rhs, lits.size()));
        } else {
            assert lits.size() == 1;
            cardinalityOutlits.clear();
            cardinalityOutlits.push(lits.get(0));
        }
        final LNGIntVector rightCardinalityOutlits = new LNGIntVector(cardinalityOutlits);
        cardinalityOutlits.clear();
        for (int i = 0; i < leftCardinalityOutlits.size() + rightCardinalityOutlits.size(); i++) {
            final int p = MiniSatStyleSolver.mkLit(s.nVars(), false);
            MaxSAT.newSATVariable(s);
            cardinalityOutlits.push(p);
        }
        currentCardinalityRhs = rhs;
        adder(s, leftCardinalityOutlits, rightCardinalityOutlits, cardinalityOutlits);
        currentCardinalityRhs = oldCardinality;
        for (int i = 0; i < lits.size(); i++) {
            ilits.push(lits.get(i));
        }
    }

    /**
     * Updates the right-hand side of a constraint.
     * @param s           the solver
     * @param rhs         the new right-hand side
     * @param assumptions the assumptions
     * @throws IllegalStateException if the incremental strategy is unknown
     */
    public void update(final MiniSatStyleSolver s, final int rhs, final LNGIntVector assumptions) {
        assert hasEncoding;
        switch (incrementalStrategy) {
            case NONE:
                for (int i = rhs; i < cardinalityOutlits.size(); i++) {
                    addUnitClause(s, MiniSatStyleSolver.not(cardinalityOutlits.get(i)));
                }
                break;
            case ITERATIVE:
                incremental(s, rhs);
                assumptions.clear();
                for (int i = rhs; i < cardinalityOutlits.size(); i++) {
                    assumptions.push(MiniSatStyleSolver.not(cardinalityOutlits.get(i)));
                }
                break;
            default:
                throw new IllegalStateException("Unknown incremental strategy: " + incrementalStrategy);
        }
    }

    /**
     * Builds the cardinality constraint.
     * @param s    the solver
     * @param lits the literals of the constraint
     * @param rhs  the right-hand side of the constraint
     */
    public void build(final MiniSatStyleSolver s, final LNGIntVector lits, final int rhs) {
        cardinalityOutlits.clear();
        hasEncoding = false;
        if (rhs == 0) {
            for (int i = 0; i < lits.size(); i++) {
                addUnitClause(s, MiniSatStyleSolver.not(lits.get(i)));
            }
            return;
        }
        assert rhs >= 1 && rhs <= lits.size();
        if (incrementalStrategy == MaxSATConfig.IncrementalStrategy.NONE && rhs == lits.size()) {
            return;
        }
        if (rhs == lits.size() && !joinMode) {
            return;
        }
        for (int i = 0; i < lits.size(); i++) {
            final int p = MiniSatStyleSolver.mkLit(s.nVars(), false);
            MaxSAT.newSATVariable(s);
            cardinalityOutlits.push(p);
        }
        cardinalityInlits = new LNGIntVector(lits);
        currentCardinalityRhs = rhs;
        toCNF(s, cardinalityOutlits);
        assert cardinalityInlits.size() == 0;
        if (!joinMode) {
            joinMode = true;
        }
        hasEncoding = true;
        ilits = new LNGIntVector(lits);
    }

    protected void toCNF(final MiniSatStyleSolver s, final LNGIntVector lits) {
        final LNGIntVector left = new LNGIntVector();
        final LNGIntVector right = new LNGIntVector();
        assert lits.size() > 1;
        final int split = lits.size() / 2;
        for (int i = 0; i < lits.size(); i++) {
            if (i < split) {
                if (split == 1) {
                    assert cardinalityInlits.size() > 0;
                    left.push(cardinalityInlits.back());
                    cardinalityInlits.pop();
                } else {
                    final int p = MiniSatStyleSolver.mkLit(s.nVars(), false);
                    MaxSAT.newSATVariable(s);
                    left.push(p);
                }
            } else {
                if (lits.size() - split == 1) {
                    assert cardinalityInlits.size() > 0;
                    right.push(cardinalityInlits.back());
                    cardinalityInlits.pop();
                } else {
                    final int p = MiniSatStyleSolver.mkLit(s.nVars(), false);
                    MaxSAT.newSATVariable(s);
                    right.push(p);
                }
            }
        }
        adder(s, left, right, lits);
        if (left.size() > 1) {
            toCNF(s, left);
        }
        if (right.size() > 1) {
            toCNF(s, right);
        }
    }

    protected void adder(final MiniSatStyleSolver s, final LNGIntVector left, final LNGIntVector right, final LNGIntVector output) {
        assert output.size() == left.size() + right.size();
        if (incrementalStrategy == MaxSATConfig.IncrementalStrategy.ITERATIVE) {
            totalizerIterativeLeft.push(new LNGIntVector(left));
            totalizerIterativeRight.push(new LNGIntVector(right));
            totalizerIterativeOutput.push(new LNGIntVector(output));
            totalizerIterativeRhs.push(currentCardinalityRhs);
        }
        for (int i = 0; i <= left.size(); i++) {
            for (int j = 0; j <= right.size(); j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                if (i + j > currentCardinalityRhs + 1) {
                    continue;
                }
                if (i == 0) {
                    addBinaryClause(s, MiniSatStyleSolver.not(right.get(j - 1)), output.get(j - 1), blocking);
                } else if (j == 0) {
                    addBinaryClause(s, MiniSatStyleSolver.not(left.get(i - 1)), output.get(i - 1), blocking);
                } else {
                    addTernaryClause(s, MiniSatStyleSolver.not(left.get(i - 1)), MiniSatStyleSolver.not(right.get(j - 1)), output.get(i + j - 1), blocking);
                }
            }
        }
    }

    protected void incremental(final MiniSatStyleSolver s, final int rhs) {
        for (int z = 0; z < totalizerIterativeRhs.size(); z++) {
            for (int i = 0; i <= totalizerIterativeLeft.get(z).size(); i++) {
                for (int j = 0; j <= totalizerIterativeRight.get(z).size(); j++) {
                    if (i == 0 && j == 0) {
                        continue;
                    }
                    if (i + j > rhs + 1 || i + j <= totalizerIterativeRhs.get(z) + 1) {
                        continue;
                    }
                    if (i == 0) {
                        addBinaryClause(s, MiniSatStyleSolver.not(totalizerIterativeRight.get(z).get(j - 1)), totalizerIterativeOutput.get(z).get(j - 1));
                    } else if (j == 0) {
                        addBinaryClause(s, MiniSatStyleSolver.not(totalizerIterativeLeft.get(z).get(i - 1)), totalizerIterativeOutput.get(z).get(i - 1));
                    } else {
                        addTernaryClause(s, MiniSatStyleSolver.not(totalizerIterativeLeft.get(z).get(i - 1)), MiniSatStyleSolver.not(totalizerIterativeRight.get(z).get(j - 1)), totalizerIterativeOutput.get(z).get(i + j - 1));
                    }
                }
            }
            totalizerIterativeRhs.set(z, rhs);
        }
    }

    /**
     * Returns the totalizer's literals.
     * @return the literals
     */
    public LNGIntVector lits() {
        return ilits;
    }

    /**
     * Returns the totalizer's output literals.
     * @return the literals
     */
    public LNGIntVector outputs() {
        return cardinalityOutlits;
    }
}
