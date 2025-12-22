// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat.encodings;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSat;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

/**
 * Encodes that at most 'rhs' literals can be assigned value true. Uses the
 * totalizer encoding for translating the cardinality constraint into CNF.
 * @version 2.4.0
 * @since 1.0
 */
public class Totalizer extends Encoding {

    protected final LngVector<LngIntVector> totalizerIterativeLeft;
    protected final LngVector<LngIntVector> totalizerIterativeRight;
    protected final LngVector<LngIntVector> totalizerIterativeOutput;
    protected final LngIntVector totalizerIterativeRhs;
    protected final int blocking;
    protected final LngIntVector cardinalityOutlits;
    protected LngIntVector cardinalityInlits;
    protected MaxSatConfig.IncrementalStrategy incrementalStrategy;
    protected int currentCardinalityRhs;
    protected boolean joinMode;
    private LngIntVector ilits;

    /**
     * Constructs a new totalizer with a given incremental strategy.
     * @param strategy the incremental strategy
     */
    Totalizer(final MaxSatConfig.IncrementalStrategy strategy) {
        blocking = LngCoreSolver.LIT_UNDEF;
        joinMode = false;
        currentCardinalityRhs = -1;
        incrementalStrategy = strategy;
        totalizerIterativeLeft = new LngVector<>();
        totalizerIterativeRight = new LngVector<>();
        totalizerIterativeOutput = new LngVector<>();
        totalizerIterativeRhs = new LngIntVector();
        cardinalityInlits = new LngIntVector();
        cardinalityOutlits = new LngIntVector();
        ilits = new LngIntVector();
    }

    /**
     * Updates the right-hand side.
     * @param s   the solver
     * @param rhs the new right-hand side
     */
    public void update(final LngCoreSolver s, final int rhs) {
        final LngIntVector assumptions = new LngIntVector();
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
    public void setIncremental(final MaxSatConfig.IncrementalStrategy incremental) {
        incrementalStrategy = incremental;
    }

    /**
     * Returns the incremental strategy.
     * @return the incremental strategy
     */
    public MaxSatConfig.IncrementalStrategy incremental() {
        return incrementalStrategy;
    }

    /**
     * Joins two constraints. The given constraint is added to the current one.
     * @param s    the solver
     * @param lits the literals of the constraint
     * @param rhs  the right-hand side of the constraint
     */
    void join(final LngCoreSolver s, final LngIntVector lits, final int rhs) {
        assert incrementalStrategy == MaxSatConfig.IncrementalStrategy.ITERATIVE;
        final LngIntVector leftCardinalityOutlits = new LngIntVector(cardinalityOutlits);
        final int oldCardinality = currentCardinalityRhs;
        if (lits.size() > 1) {
            build(s, lits, Math.min(rhs, lits.size()));
        } else {
            assert lits.size() == 1;
            cardinalityOutlits.clear();
            cardinalityOutlits.push(lits.get(0));
        }
        final LngIntVector rightCardinalityOutlits = new LngIntVector(cardinalityOutlits);
        cardinalityOutlits.clear();
        for (int i = 0; i < leftCardinalityOutlits.size() + rightCardinalityOutlits.size(); i++) {
            final int p = LngCoreSolver.mkLit(s.nVars(), false);
            MaxSat.newSatVariable(s);
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
    public void update(final LngCoreSolver s, final int rhs, final LngIntVector assumptions) {
        assert hasEncoding;
        switch (incrementalStrategy) {
            case NONE:
                for (int i = rhs; i < cardinalityOutlits.size(); i++) {
                    addUnitClause(s, LngCoreSolver.not(cardinalityOutlits.get(i)));
                }
                break;
            case ITERATIVE:
                incremental(s, rhs);
                assumptions.clear();
                for (int i = rhs; i < cardinalityOutlits.size(); i++) {
                    assumptions.push(LngCoreSolver.not(cardinalityOutlits.get(i)));
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
    public void build(final LngCoreSolver s, final LngIntVector lits, final int rhs) {
        cardinalityOutlits.clear();
        hasEncoding = false;
        if (rhs == 0) {
            for (int i = 0; i < lits.size(); i++) {
                addUnitClause(s, LngCoreSolver.not(lits.get(i)));
            }
            return;
        }
        assert rhs >= 1 && rhs <= lits.size();
        if (incrementalStrategy == MaxSatConfig.IncrementalStrategy.NONE && rhs == lits.size()) {
            return;
        }
        if (rhs == lits.size() && !joinMode) {
            return;
        }
        for (int i = 0; i < lits.size(); i++) {
            final int p = LngCoreSolver.mkLit(s.nVars(), false);
            MaxSat.newSatVariable(s);
            cardinalityOutlits.push(p);
        }
        cardinalityInlits = new LngIntVector(lits);
        currentCardinalityRhs = rhs;
        toCnf(s, cardinalityOutlits);
        assert cardinalityInlits.isEmpty();
        if (!joinMode) {
            joinMode = true;
        }
        hasEncoding = true;
        ilits = new LngIntVector(lits);
    }

    protected void toCnf(final LngCoreSolver s, final LngIntVector lits) {
        final LngIntVector left = new LngIntVector();
        final LngIntVector right = new LngIntVector();
        assert lits.size() > 1;
        final int split = lits.size() / 2;
        for (int i = 0; i < lits.size(); i++) {
            if (i < split) {
                if (split == 1) {
                    assert !cardinalityInlits.isEmpty();
                    left.push(cardinalityInlits.back());
                    cardinalityInlits.pop();
                } else {
                    final int p = LngCoreSolver.mkLit(s.nVars(), false);
                    MaxSat.newSatVariable(s);
                    left.push(p);
                }
            } else {
                if (lits.size() - split == 1) {
                    assert !cardinalityInlits.isEmpty();
                    right.push(cardinalityInlits.back());
                    cardinalityInlits.pop();
                } else {
                    final int p = LngCoreSolver.mkLit(s.nVars(), false);
                    MaxSat.newSatVariable(s);
                    right.push(p);
                }
            }
        }
        adder(s, left, right, lits);
        if (left.size() > 1) {
            toCnf(s, left);
        }
        if (right.size() > 1) {
            toCnf(s, right);
        }
    }

    protected void adder(final LngCoreSolver s, final LngIntVector left, final LngIntVector right,
                         final LngIntVector output) {
        assert output.size() == left.size() + right.size();
        if (incrementalStrategy == MaxSatConfig.IncrementalStrategy.ITERATIVE) {
            totalizerIterativeLeft.push(new LngIntVector(left));
            totalizerIterativeRight.push(new LngIntVector(right));
            totalizerIterativeOutput.push(new LngIntVector(output));
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
                    addBinaryClause(s, LngCoreSolver.not(right.get(j - 1)), output.get(j - 1), blocking);
                } else if (j == 0) {
                    addBinaryClause(s, LngCoreSolver.not(left.get(i - 1)), output.get(i - 1), blocking);
                } else {
                    addTernaryClause(s, LngCoreSolver.not(left.get(i - 1)), LngCoreSolver.not(right.get(j - 1)),
                            output.get(i + j - 1), blocking);
                }
            }
        }
    }

    protected void incremental(final LngCoreSolver s, final int rhs) {
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
                        addBinaryClause(s, LngCoreSolver.not(totalizerIterativeRight.get(z).get(j - 1)),
                                totalizerIterativeOutput.get(z).get(j - 1));
                    } else if (j == 0) {
                        addBinaryClause(s, LngCoreSolver.not(totalizerIterativeLeft.get(z).get(i - 1)),
                                totalizerIterativeOutput.get(z).get(i - 1));
                    } else {
                        addTernaryClause(s, LngCoreSolver.not(totalizerIterativeLeft.get(z).get(i - 1)),
                                LngCoreSolver.not(totalizerIterativeRight.get(z).get(j - 1)),
                                totalizerIterativeOutput.get(z).get(i + j - 1));
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
    public LngIntVector lits() {
        return ilits;
    }

    /**
     * Returns the totalizer's output literals.
     * @return the literals
     */
    public LngIntVector outputs() {
        return cardinalityOutlits;
    }
}
