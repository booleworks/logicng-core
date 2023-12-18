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

import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSAT.newSATVariable;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.solvers.sat.MiniSatStyleSolver;

/**
 * A sequential weight counter for the encoding of pseudo-Boolean constraints in
 * CNF.
 * @version 2.0.0
 * @since 1.0
 */
public class SequentialWeightCounter extends Encoding {

    protected final LNGIntVector pbOutlits;
    protected final LNGIntVector unitLits;
    protected final LNGIntVector unitCoeffs;
    protected int currentPbRhs;
    protected int currentLitBlocking;
    protected LNGVector<LNGIntVector> seqAuxiliaryInc;
    protected LNGIntVector litsInc;
    protected LNGIntVector coeffsInc;

    /**
     * Constructs a new sequential weight counter encoder.
     */
    SequentialWeightCounter() {
        currentPbRhs = -1;
        currentLitBlocking = MiniSatStyleSolver.LIT_UNDEF;
        pbOutlits = new LNGIntVector();
        unitLits = new LNGIntVector();
        unitCoeffs = new LNGIntVector();
        seqAuxiliaryInc = new LNGVector<>();
        litsInc = new LNGIntVector();
        coeffsInc = new LNGIntVector();
    }

    /**
     * Updates the assumptions with the unit literals.
     * @param assumptions the current assumptions
     */
    void updateAssumptions(final LNGIntVector assumptions) {
        assumptions.push(MiniSatStyleSolver.not(currentLitBlocking));
        for (int i = 0; i < unitLits.size(); i++) {
            assumptions.push(MiniSatStyleSolver.not(unitLits.get(i)));
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
    public void encode(final MiniSatStyleSolver s, final LNGIntVector lits, final LNGIntVector coeffs, final int rhs) {
        if (rhs == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Overflow in the encoding.");
        }
        hasEncoding = false;
        final LNGIntVector simpLits = new LNGIntVector(lits);
        final LNGIntVector simpCoeffs = new LNGIntVector(coeffs);
        lits.clear();
        coeffs.clear();
        for (int i = 0; i < simpLits.size(); i++) {
            if (simpCoeffs.get(i) <= rhs) {
                lits.push(simpLits.get(i));
                coeffs.push(simpCoeffs.get(i));
            } else {
                addUnitClause(s, MiniSatStyleSolver.not(simpLits.get(i)));
            }
        }
        if (lits.size() == 1) {
            addUnitClause(s, MiniSatStyleSolver.not(lits.get(0)));
            return;
        }
        if (lits.size() == 0) {
            return;
        }
        final int n = lits.size();
        final LNGIntVector[] seqAuxiliary = new LNGIntVector[n + 1];
        for (int i = 0; i < n + 1; i++) {
            seqAuxiliary[i] = new LNGIntVector();
            seqAuxiliary[i].growTo(rhs + 1, -1);
        }
        for (int i = 1; i <= n; ++i) {
            for (int j = 1; j <= rhs; ++j) {
                seqAuxiliary[i].set(j, MiniSatStyleSolver.mkLit(s.nVars(), false));
                newSATVariable(s);
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
                    addBinaryClause(s, MiniSatStyleSolver.not(seqAuxiliary[i - 1].get(j)), seqAuxiliary[i].get(j));
                }
                if (i <= n && j <= wi) {
                    addBinaryClause(s, MiniSatStyleSolver.not(lits.get(i - 1)), seqAuxiliary[i].get(j));
                }
                if (i >= 2 && i <= n && j <= rhs - wi) {
                    addTernaryClause(s, MiniSatStyleSolver.not(seqAuxiliary[i - 1].get(j)),
                            MiniSatStyleSolver.not(lits.get(i - 1)), seqAuxiliary[i].get(j + wi));
                }
            }
            if (i >= 2) {
                addBinaryClause(s, MiniSatStyleSolver.not(seqAuxiliary[i - 1].get(rhs + 1 - wi)),
                        MiniSatStyleSolver.not(lits.get(i - 1)));
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
    public void encode(final MiniSatStyleSolver s, final LNGIntVector lits, final LNGIntVector coeffs,
                       final int rhs, final LNGIntVector assumptions, final int size) {
        if (rhs == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Overflow in the encoding.");
        }
        hasEncoding = false;
        final LNGIntVector simpLits = new LNGIntVector(lits);
        final LNGIntVector simpCoeffs = new LNGIntVector(coeffs);
        lits.clear();
        coeffs.clear();
        final LNGIntVector simpUnitLits = new LNGIntVector(unitLits);
        final LNGIntVector simpUnitCoeffs = new LNGIntVector(unitCoeffs);
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
                assumptions.push(MiniSatStyleSolver.not(unitLits.get(i)));
            }
            unitLits.push(lits.get(0));
            unitCoeffs.push(coeffs.get(0));
            return;
        }
        if (lits.size() == 0) {
            for (int i = 0; i < unitLits.size(); i++) {
                assumptions.push(MiniSatStyleSolver.not(unitLits.get(i)));
            }
            return;
        }
        final int n = lits.size();
        seqAuxiliaryInc = new LNGVector<>(size + 1);
        for (int i = 0; i <= n; i++) {
            seqAuxiliaryInc.set(i, new LNGIntVector());
            seqAuxiliaryInc.get(i).growTo(rhs + 1, -1);
        }
        for (int i = 1; i <= n; ++i) {
            for (int j = 1; j <= rhs; ++j) {
                seqAuxiliaryInc.get(i).set(j, MiniSatStyleSolver.mkLit(s.nVars(), false));
                newSATVariable(s);
            }
        }
        final int blocking = MiniSatStyleSolver.mkLit(s.nVars(), false);
        newSATVariable(s);
        currentLitBlocking = blocking;
        assumptions.push(MiniSatStyleSolver.not(blocking));
        for (int i = 1; i <= n; i++) {
            final int wi = coeffs.get(i - 1);
            assert rhs >= wi;
            for (int j = 1; j <= rhs; j++) {
                if (i >= 2 && i <= n) {
                    addBinaryClause(s, MiniSatStyleSolver.not(seqAuxiliaryInc.get(i - 1).get(j)),
                            seqAuxiliaryInc.get(i).get(j));
                }
                if (i <= n && j <= wi) {
                    addBinaryClause(s, MiniSatStyleSolver.not(lits.get(i - 1)), seqAuxiliaryInc.get(i).get(j));
                }
                if (i >= 2 && i <= n && j <= rhs - wi) {
                    addTernaryClause(s, MiniSatStyleSolver.not(seqAuxiliaryInc.get(i - 1).get(j)),
                            MiniSatStyleSolver.not(lits.get(i - 1)), seqAuxiliaryInc.get(i).get(j + wi));
                }
            }
            if (i >= 2) {
                addBinaryClause(s, MiniSatStyleSolver.not(seqAuxiliaryInc.get(i - 1).get(rhs + 1 - wi)),
                        MiniSatStyleSolver.not(lits.get(i - 1)), blocking);
            }
        }
        for (int i = 0; i < unitLits.size(); i++) {
            assumptions.push(MiniSatStyleSolver.not(unitLits.get(i)));
        }
        currentPbRhs = rhs;
        hasEncoding = true;
        litsInc = new LNGIntVector(lits);
        coeffsInc = new LNGIntVector(coeffs);
    }

    /**
     * Updates the 'rhs' of an already existent pseudo-Boolean encoding. This
     * method allows for all learned clauses from previous iterations to be kept
     * in the next iteration.
     * @param s   the solver
     * @param rhs the new right-hand side
     */
    public void update(final MiniSatStyleSolver s, final int rhs) {
        assert currentPbRhs != -1;
        for (int i = rhs; i < currentPbRhs; i++) {
            addUnitClause(s, MiniSatStyleSolver.not(pbOutlits.get(i)));
        }
        currentPbRhs = rhs;
    }

    /**
     * Incremental update of the SWC encoding.
     * @param s   the solver
     * @param rhs the new right-hand side
     */
    public void updateInc(final MiniSatStyleSolver s, final int rhs) {
        if (currentLitBlocking != MiniSatStyleSolver.LIT_UNDEF) {
            addUnitClause(s, currentLitBlocking);
        }
        final int n = litsInc.size();
        final int offset = currentPbRhs + 1;
        assert currentPbRhs < rhs;
        for (int i = 1; i <= n; i++) {
            for (int j = offset; j <= rhs; j++) {
                seqAuxiliaryInc.get(i).push(MiniSatStyleSolver.LIT_UNDEF);
            }
        }
        for (int i = 1; i <= n; ++i) {
            for (int j = offset; j <= rhs; ++j) {
                assert seqAuxiliaryInc.get(i).size() > j;
                seqAuxiliaryInc.get(i).set(j, MiniSatStyleSolver.mkLit(s.nVars(), false));
                newSATVariable(s);
            }
        }
        for (int i = 1; i < litsInc.size(); i++) {
            assert seqAuxiliaryInc.get(i).size() == rhs + 1;
        }
        currentLitBlocking = MiniSatStyleSolver.mkLit(s.nVars(), false);
        newSATVariable(s);
        for (int i = 1; i <= n; i++) {
            final int wi = coeffsInc.get(i - 1);
            assert wi > 0;
            assert rhs >= wi;
            for (int j = 1; j <= rhs; j++) {
                if (i >= 2 && i <= n && j <= rhs && j >= offset) {
                    assert seqAuxiliaryInc.get(i).size() > j;
                    addBinaryClause(s, MiniSatStyleSolver.not(seqAuxiliaryInc.get(i - 1).get(j)),
                            seqAuxiliaryInc.get(i).get(j));
                }
                if (i >= 2 && i <= n && j <= rhs - wi && j >= offset - wi) {
                    addTernaryClause(s, MiniSatStyleSolver.not(seqAuxiliaryInc.get(i - 1).get(j)),
                            MiniSatStyleSolver.not(litsInc.get(i - 1)), seqAuxiliaryInc.get(i).get(j + wi));
                }
            }
            if (i >= 2) {
                assert seqAuxiliaryInc.get(i - 1).size() > rhs + 1 - wi;
                assert rhs + 1 - wi > 0;
                assert i - 1 < litsInc.size();
                addBinaryClause(s, MiniSatStyleSolver.not(seqAuxiliaryInc.get(i - 1).get(rhs + 1 - wi)),
                        MiniSatStyleSolver.not(litsInc.get(i - 1)), currentLitBlocking);
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
    void join(final MiniSatStyleSolver s, final LNGIntVector lits, final LNGIntVector coeffs) {
        assert currentLitBlocking != MiniSatStyleSolver.LIT_UNDEF;
        final int rhs = currentPbRhs;
        if (rhs == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Overflow in the encoding.");
        }
        final LNGIntVector simpUnitLits = new LNGIntVector(unitLits);
        final LNGIntVector simpUnitCoeffs = new LNGIntVector(unitCoeffs);
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
        assert seqAuxiliaryInc.get(lhsJoin).size() > 0;
        for (int i = lhsJoin + 1; i <= n; i++) {
            seqAuxiliaryInc.set(i, new LNGIntVector());
            seqAuxiliaryInc.get(i).growTo(rhs + 1, -1);
        }
        for (int i = lhsJoin + 1; i <= n; ++i) {
            for (int j = 1; j <= rhs; ++j) {
                seqAuxiliaryInc.get(i).set(j, MiniSatStyleSolver.mkLit(s.nVars(), false));
                newSATVariable(s);
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
                addBinaryClause(s, MiniSatStyleSolver.not(seqAuxiliaryInc.get(i - 1).get(j)),
                        seqAuxiliaryInc.get(i).get(j));
                if (j <= wi) {
                    assert seqAuxiliaryInc.get(i).size() > j;
                    assert i - 1 < litsInc.size() && i - 1 >= 0;
                    addBinaryClause(s, MiniSatStyleSolver.not(litsInc.get(i - 1)), seqAuxiliaryInc.get(i).get(j));
                }
                if (j <= rhs - wi) {
                    addTernaryClause(s, MiniSatStyleSolver.not(seqAuxiliaryInc.get(i - 1).get(j)),
                            MiniSatStyleSolver.not(litsInc.get(i - 1)), seqAuxiliaryInc.get(i).get(j + wi));
                }
            }
            if (i > lhsJoin) {
                assert rhs + 1 - wi >= 0;
                assert seqAuxiliaryInc.get(i - 1).size() > rhs + 1 - wi;
                assert i - 1 < litsInc.size();
                addBinaryClause(s, MiniSatStyleSolver.not(seqAuxiliaryInc.get(i - 1).get(rhs + 1 - wi)),
                        MiniSatStyleSolver.not(litsInc.get(i - 1)), currentLitBlocking);
            }
        }
    }
}
