// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat.encodings;

import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSat.newSatVariable;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

/**
 * A sequential weight counter for the encoding of pseudo-Boolean constraints in
 * CNF.
 * @version 2.0.0
 * @since 1.0
 */
public class SequentialWeightCounter extends Encoding {

    protected final LngIntVector pbOutlits;
    protected final LngIntVector unitLits;
    protected final LngIntVector unitCoeffs;
    protected int currentPbRhs;
    protected int currentLitBlocking;
    protected LngVector<LngIntVector> seqAuxiliaryInc;
    protected LngIntVector litsInc;
    protected LngIntVector coeffsInc;

    /**
     * Constructs a new sequential weight counter encoder.
     */
    SequentialWeightCounter() {
        currentPbRhs = -1;
        currentLitBlocking = LngCoreSolver.LIT_UNDEF;
        pbOutlits = new LngIntVector();
        unitLits = new LngIntVector();
        unitCoeffs = new LngIntVector();
        seqAuxiliaryInc = new LngVector<>();
        litsInc = new LngIntVector();
        coeffsInc = new LngIntVector();
    }

    /**
     * Updates the assumptions with the unit literals.
     * @param assumptions the current assumptions
     */
    void updateAssumptions(final LngIntVector assumptions) {
        assumptions.push(LngCoreSolver.not(currentLitBlocking));
        for (int i = 0; i < unitLits.size(); i++) {
            assumptions.push(LngCoreSolver.not(unitLits.get(i)));
        }
    }

    /**
     * Returns {@code true} if an encoding was created, {@code false} otherwise.
     * @return {@code true} if an encoding was created
     */
    boolean hasCreatedEncoding() {
        return hasEncoding;
    }

    /**
     * Encodes the pseudo-Boolean constraint
     * @param s      the solver
     * @param lits   the literals of the constraint
     * @param coeffs the coefficients of the constraints
     * @param rhs    the right-hand side of the constraint
     */
    public void encode(final LngCoreSolver s, final LngIntVector lits, final LngIntVector coeffs, final int rhs) {
        if (rhs == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Overflow in the encoding.");
        }
        hasEncoding = false;
        final LngIntVector simpLits = new LngIntVector(lits);
        final LngIntVector simpCoeffs = new LngIntVector(coeffs);
        lits.clear();
        coeffs.clear();
        for (int i = 0; i < simpLits.size(); i++) {
            if (simpCoeffs.get(i) <= rhs) {
                lits.push(simpLits.get(i));
                coeffs.push(simpCoeffs.get(i));
            } else {
                addUnitClause(s, LngCoreSolver.not(simpLits.get(i)));
            }
        }
        if (lits.size() == 1) {
            addUnitClause(s, LngCoreSolver.not(lits.get(0)));
            return;
        }
        if (lits.isEmpty()) {
            return;
        }
        final int n = lits.size();
        final LngIntVector[] seqAuxiliary = new LngIntVector[n + 1];
        for (int i = 0; i < n + 1; i++) {
            seqAuxiliary[i] = new LngIntVector();
            seqAuxiliary[i].growTo(rhs + 1, -1);
        }
        for (int i = 1; i <= n; ++i) {
            for (int j = 1; j <= rhs; ++j) {
                seqAuxiliary[i].set(j, LngCoreSolver.mkLit(s.nVars(), false));
                newSatVariable(s);
            }
        }
        for (int i = 1; i <= rhs; ++i) {
            pbOutlits.push(seqAuxiliary[n].get(i));
        }
        for (int i = 1; i <= n; i++) {
            final int wi = coeffs.get(i - 1);
            assert wi <= rhs;
            for (int j = 1; j <= rhs; j++) {
                if (i >= 2 && i <= n && j <= rhs) {
                    addBinaryClause(s, LngCoreSolver.not(seqAuxiliary[i - 1].get(j)), seqAuxiliary[i].get(j));
                }
                if (i <= n && j <= wi) {
                    addBinaryClause(s, LngCoreSolver.not(lits.get(i - 1)), seqAuxiliary[i].get(j));
                }
                if (i >= 2 && i <= n && j <= rhs - wi) {
                    addTernaryClause(s, LngCoreSolver.not(seqAuxiliary[i - 1].get(j)),
                            LngCoreSolver.not(lits.get(i - 1)), seqAuxiliary[i].get(j + wi));
                }
            }
            if (i >= 2) {
                addBinaryClause(s, LngCoreSolver.not(seqAuxiliary[i - 1].get(rhs + 1 - wi)),
                        LngCoreSolver.not(lits.get(i - 1)));
            }
        }
        currentPbRhs = rhs;
        hasEncoding = true;
    }

    /**
     * Incremental construction of the SWC encoding.
     * @param s           the solver
     * @param lits        the literals of the constraint
     * @param coeffs      the coefficients of the constraint
     * @param rhs         the right-hand size of the constraint
     * @param assumptions the current assumptions
     * @param size        the size
     */
    public void encode(final LngCoreSolver s, final LngIntVector lits, final LngIntVector coeffs,
                       final int rhs, final LngIntVector assumptions, final int size) {
        if (rhs == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Overflow in the encoding.");
        }
        hasEncoding = false;
        final LngIntVector simpLits = new LngIntVector(lits);
        final LngIntVector simpCoeffs = new LngIntVector(coeffs);
        lits.clear();
        coeffs.clear();
        final LngIntVector simpUnitLits = new LngIntVector(unitLits);
        final LngIntVector simpUnitCoeffs = new LngIntVector(unitCoeffs);
        unitLits.clear();
        unitCoeffs.clear();
        for (int i = 0; i < simpUnitLits.size(); i++) {
            if (simpUnitCoeffs.get(i) <= rhs) {
                lits.push(simpUnitLits.get(i));
                coeffs.push(simpUnitCoeffs.get(i));
            } else {
                unitLits.push(simpUnitLits.get(i));
                unitCoeffs.push(simpUnitCoeffs.get(i));
            }
        }
        for (int i = 0; i < simpLits.size(); i++) {
            if (simpCoeffs.get(i) <= rhs) {
                lits.push(simpLits.get(i));
                coeffs.push(simpCoeffs.get(i));
            } else {
                unitLits.push(simpLits.get(i));
                unitCoeffs.push(simpCoeffs.get(i));
            }
        }
        if (lits.size() == 1) {
            for (int i = 0; i < unitLits.size(); i++) {
                assumptions.push(LngCoreSolver.not(unitLits.get(i)));
            }
            unitLits.push(lits.get(0));
            unitCoeffs.push(coeffs.get(0));
            return;
        }
        if (lits.isEmpty()) {
            for (int i = 0; i < unitLits.size(); i++) {
                assumptions.push(LngCoreSolver.not(unitLits.get(i)));
            }
            return;
        }
        final int n = lits.size();
        seqAuxiliaryInc = new LngVector<>(size + 1);
        for (int i = 0; i <= n; i++) {
            seqAuxiliaryInc.set(i, new LngIntVector());
            seqAuxiliaryInc.get(i).growTo(rhs + 1, -1);
        }
        for (int i = 1; i <= n; ++i) {
            for (int j = 1; j <= rhs; ++j) {
                seqAuxiliaryInc.get(i).set(j, LngCoreSolver.mkLit(s.nVars(), false));
                newSatVariable(s);
            }
        }
        final int blocking = LngCoreSolver.mkLit(s.nVars(), false);
        newSatVariable(s);
        currentLitBlocking = blocking;
        assumptions.push(LngCoreSolver.not(blocking));
        for (int i = 1; i <= n; i++) {
            final int wi = coeffs.get(i - 1);
            assert rhs >= wi;
            for (int j = 1; j <= rhs; j++) {
                if (i >= 2 && i <= n) {
                    addBinaryClause(s, LngCoreSolver.not(seqAuxiliaryInc.get(i - 1).get(j)),
                            seqAuxiliaryInc.get(i).get(j));
                }
                if (i <= n && j <= wi) {
                    addBinaryClause(s, LngCoreSolver.not(lits.get(i - 1)), seqAuxiliaryInc.get(i).get(j));
                }
                if (i >= 2 && i <= n && j <= rhs - wi) {
                    addTernaryClause(s, LngCoreSolver.not(seqAuxiliaryInc.get(i - 1).get(j)),
                            LngCoreSolver.not(lits.get(i - 1)), seqAuxiliaryInc.get(i).get(j + wi));
                }
            }
            if (i >= 2) {
                addBinaryClause(s, LngCoreSolver.not(seqAuxiliaryInc.get(i - 1).get(rhs + 1 - wi)),
                        LngCoreSolver.not(lits.get(i - 1)), blocking);
            }
        }
        for (int i = 0; i < unitLits.size(); i++) {
            assumptions.push(LngCoreSolver.not(unitLits.get(i)));
        }
        currentPbRhs = rhs;
        hasEncoding = true;
        litsInc = new LngIntVector(lits);
        coeffsInc = new LngIntVector(coeffs);
    }

    /**
     * Updates the 'rhs' of an already existent pseudo-Boolean encoding. This
     * method allows for all learned clauses from previous iterations to be kept
     * in the next iteration.
     * @param s   the solver
     * @param rhs the new right-hand side
     */
    public void update(final LngCoreSolver s, final int rhs) {
        assert currentPbRhs != -1;
        for (int i = rhs; i < currentPbRhs; i++) {
            addUnitClause(s, LngCoreSolver.not(pbOutlits.get(i)));
        }
        currentPbRhs = rhs;
    }

    /**
     * Incremental update of the SWC encoding.
     * @param s   the solver
     * @param rhs the new right-hand side
     */
    public void updateInc(final LngCoreSolver s, final int rhs) {
        if (currentLitBlocking != LngCoreSolver.LIT_UNDEF) {
            addUnitClause(s, currentLitBlocking);
        }
        final int n = litsInc.size();
        final int offset = currentPbRhs + 1;
        assert currentPbRhs < rhs;
        for (int i = 1; i <= n; i++) {
            for (int j = offset; j <= rhs; j++) {
                seqAuxiliaryInc.get(i).push(LngCoreSolver.LIT_UNDEF);
            }
        }
        for (int i = 1; i <= n; ++i) {
            for (int j = offset; j <= rhs; ++j) {
                assert seqAuxiliaryInc.get(i).size() > j;
                seqAuxiliaryInc.get(i).set(j, LngCoreSolver.mkLit(s.nVars(), false));
                newSatVariable(s);
            }
        }
        for (int i = 1; i < litsInc.size(); i++) {
            assert seqAuxiliaryInc.get(i).size() == rhs + 1;
        }
        currentLitBlocking = LngCoreSolver.mkLit(s.nVars(), false);
        newSatVariable(s);
        for (int i = 1; i <= n; i++) {
            final int wi = coeffsInc.get(i - 1);
            assert wi > 0;
            assert rhs >= wi;
            for (int j = 1; j <= rhs; j++) {
                if (i >= 2 && i <= n && j <= rhs && j >= offset) {
                    assert seqAuxiliaryInc.get(i).size() > j;
                    addBinaryClause(s, LngCoreSolver.not(seqAuxiliaryInc.get(i - 1).get(j)),
                            seqAuxiliaryInc.get(i).get(j));
                }
                if (i >= 2 && i <= n && j <= rhs - wi && j >= offset - wi) {
                    addTernaryClause(s, LngCoreSolver.not(seqAuxiliaryInc.get(i - 1).get(j)),
                            LngCoreSolver.not(litsInc.get(i - 1)), seqAuxiliaryInc.get(i).get(j + wi));
                }
            }
            if (i >= 2) {
                assert seqAuxiliaryInc.get(i - 1).size() > rhs + 1 - wi;
                assert rhs + 1 - wi > 0;
                assert i - 1 < litsInc.size();
                addBinaryClause(s, LngCoreSolver.not(seqAuxiliaryInc.get(i - 1).get(rhs + 1 - wi)),
                        LngCoreSolver.not(litsInc.get(i - 1)), currentLitBlocking);
            }
        }
        currentPbRhs = rhs;
    }

    /**
     * Joins two pseudo boolean constraints. The given constraint is added to
     * the current one.
     * @param s      the solver
     * @param lits   the literals of the constraint
     * @param coeffs the coefficients of the constraint
     */
    void join(final LngCoreSolver s, final LngIntVector lits, final LngIntVector coeffs) {
        assert currentLitBlocking != LngCoreSolver.LIT_UNDEF;
        final int rhs = currentPbRhs;
        if (rhs == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Overflow in the encoding.");
        }
        final LngIntVector simpUnitLits = new LngIntVector(unitLits);
        final LngIntVector simpUnitCoeffs = new LngIntVector(unitCoeffs);
        unitLits.clear();
        unitCoeffs.clear();
        final int lhsJoin = litsInc.size();
        for (int i = 0; i < simpUnitLits.size(); i++) {
            if (simpUnitCoeffs.get(i) <= rhs) {
                litsInc.push(simpUnitLits.get(i));
                coeffsInc.push(simpUnitCoeffs.get(i));
            } else {
                unitLits.push(simpUnitLits.get(i));
                unitCoeffs.push(simpUnitCoeffs.get(i));
            }
        }
        for (int i = 0; i < lits.size(); i++) {
            if (coeffs.get(i) <= rhs) {
                litsInc.push(lits.get(i));
                coeffsInc.push(coeffs.get(i));
            } else {
                unitLits.push(lits.get(i));
                unitCoeffs.push(coeffs.get(i));
            }
        }
        if (litsInc.size() == lhsJoin) {
            return;
        }
        final int n = litsInc.size();
        assert !seqAuxiliaryInc.get(lhsJoin).isEmpty();
        for (int i = lhsJoin + 1; i <= n; i++) {
            seqAuxiliaryInc.set(i, new LngIntVector());
            seqAuxiliaryInc.get(i).growTo(rhs + 1, -1);
        }
        for (int i = lhsJoin + 1; i <= n; ++i) {
            for (int j = 1; j <= rhs; ++j) {
                seqAuxiliaryInc.get(i).set(j, LngCoreSolver.mkLit(s.nVars(), false));
                newSatVariable(s);
            }
        }
        for (int i = 1; i <= n; i++) {
            assert seqAuxiliaryInc.get(i).size() == rhs + 1;
        }
        for (int i = lhsJoin; i <= n; i++) {
            final int wi = coeffsInc.get(i - 1);
            assert wi > 0;
            assert wi <= rhs;
            for (int j = 1; j <= rhs; j++) {
                assert seqAuxiliaryInc.get(i).size() > j;
                assert seqAuxiliaryInc.get(i - 1).size() > j;
                addBinaryClause(s, LngCoreSolver.not(seqAuxiliaryInc.get(i - 1).get(j)), seqAuxiliaryInc.get(i).get(j));
                if (j <= wi) {
                    assert seqAuxiliaryInc.get(i).size() > j;
                    assert i - 1 < litsInc.size() && i - 1 >= 0;
                    addBinaryClause(s, LngCoreSolver.not(litsInc.get(i - 1)), seqAuxiliaryInc.get(i).get(j));
                }
                if (j <= rhs - wi) {
                    addTernaryClause(s, LngCoreSolver.not(seqAuxiliaryInc.get(i - 1).get(j)),
                            LngCoreSolver.not(litsInc.get(i - 1)), seqAuxiliaryInc.get(i).get(j + wi));
                }
            }
            if (i > lhsJoin) {
                assert rhs + 1 - wi >= 0;
                assert seqAuxiliaryInc.get(i - 1).size() > rhs + 1 - wi;
                assert i - 1 < litsInc.size();
                addBinaryClause(s, LngCoreSolver.not(seqAuxiliaryInc.get(i - 1).get(rhs + 1 - wi)),
                        LngCoreSolver.not(litsInc.get(i - 1)), currentLitBlocking);
            }
        }
    }
}
