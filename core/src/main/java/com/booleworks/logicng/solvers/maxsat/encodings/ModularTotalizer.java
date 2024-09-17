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

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSat;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

/**
 * Encodes that at most 'rhs' literals can be assigned value true. Uses the
 * modular totalizer encoding for translating the cardinality constraint into
 * CNF.
 * @version 2.0.0
 * @since 1.0
 */
public class ModularTotalizer extends Encoding {

    protected static final int LIT_ERROR = -2;

    protected final int h0;
    protected final LngIntVector cardinalityUpoutlits;
    protected final LngIntVector cardinalityLwoutlits;
    protected int modulo;
    protected LngIntVector cardinalityInlits;
    protected int currentCardinalityRhs;

    /**
     * Constructs a new modular totalizer.
     */
    ModularTotalizer() {
        h0 = LngCoreSolver.LIT_UNDEF;
        modulo = -1;
        currentCardinalityRhs = -1;
        cardinalityInlits = new LngIntVector();
        cardinalityUpoutlits = new LngIntVector();
        cardinalityLwoutlits = new LngIntVector();
    }

    /**
     * Sets the modulo value.
     * @param m the modulo value
     */
    void setModulo(final int m) {
        modulo = m;
    }

    /**
     * Returns {@code true} if an encoding was created, {@code false} otherwise.
     * @return {@code true} if an encoding was created
     */
    boolean hasCreatedEncoding() {
        return hasEncoding;
    }

    /**
     * Encodes a cardinality constraint.
     * @param s    the solver
     * @param lits the literals of the constraint
     * @param rhs  the right-hand side of the constraint
     */
    public void encode(final LngCoreSolver s, final LngIntVector lits, final int rhs) {
        hasEncoding = false;
        cardinalityUpoutlits.clear();
        cardinalityLwoutlits.clear();
        if (rhs == 0) {
            for (int i = 0; i < lits.size(); i++) {
                addUnitClause(s, LngCoreSolver.not(lits.get(i)));
            }
            return;
        }
        assert rhs >= 1 && rhs <= lits.size();
        if (rhs == lits.size()) {
            return;
        }
        hasEncoding = true;
        int mod = (int) Math.ceil(Math.sqrt(rhs + 1.0));
        if (modulo == -1) {
            modulo = mod;
        } else {
            mod = modulo;
        }
        for (int i = 0; i < lits.size() / mod; i++) {
            final int p = LngCoreSolver.mkLit(s.nVars(), false);
            MaxSat.newSatVariable(s);
            cardinalityUpoutlits.push(p);
        }
        for (int i = 0; i < mod - 1; i++) {
            final int p = LngCoreSolver.mkLit(s.nVars(), false);
            MaxSat.newSatVariable(s);
            cardinalityLwoutlits.push(p);
        }
        cardinalityInlits = new LngIntVector(lits);
        currentCardinalityRhs = rhs + 1;
        if (cardinalityUpoutlits.isEmpty()) {
            cardinalityUpoutlits.push(h0);
        }
        toCnf(s, mod, cardinalityUpoutlits, cardinalityLwoutlits, lits.size());
        assert cardinalityInlits.isEmpty();
        update(s, rhs);
    }

    /**
     * Updates the right-hand side of the current constraint.
     * @param s   the solver
     * @param rhs the new right-hand side
     */
    public void update(final LngCoreSolver s, final int rhs) {
        assert currentCardinalityRhs != -1;
        assert hasEncoding;
        encodeOutput(s, rhs);
        currentCardinalityRhs = rhs + 1;
    }

    protected void encodeOutput(final LngCoreSolver s, final int rhs) {
        assert hasEncoding;
        assert !cardinalityUpoutlits.isEmpty() || !cardinalityLwoutlits.isEmpty();
        final int mod = modulo;
        final int ulimit = (rhs + 1) / mod;
        final int llimit = (rhs + 1) - ulimit * mod;
        assert ulimit <= cardinalityUpoutlits.size();
        assert llimit <= cardinalityLwoutlits.size();
        for (int i = ulimit; i < cardinalityUpoutlits.size(); i++) {
            addUnitClause(s, LngCoreSolver.not(cardinalityUpoutlits.get(i)));
        }
        if (ulimit != 0 && llimit != 0) {
            for (int i = llimit - 1; i < cardinalityLwoutlits.size(); i++) {
                addBinaryClause(s, LngCoreSolver.not(cardinalityUpoutlits.get(ulimit - 1)),
                        LngCoreSolver.not(cardinalityLwoutlits.get(i)));
            }
        } else {
            if (ulimit == 0) {
                assert llimit != 0;
                for (int i = llimit - 1; i < cardinalityLwoutlits.size(); i++) {
                    addUnitClause(s, LngCoreSolver.not(cardinalityLwoutlits.get(i)));
                }
            } else {
                addUnitClause(s, LngCoreSolver.not(cardinalityUpoutlits.get(ulimit - 1)));
            }
        }
    }

    protected void toCnf(final LngCoreSolver s, final int mod, final LngIntVector ublits, final LngIntVector lwlits,
                         final int rhs) {
        final LngIntVector lupper = new LngIntVector();
        final LngIntVector llower = new LngIntVector();
        final LngIntVector rupper = new LngIntVector();
        final LngIntVector rlower = new LngIntVector();
        assert rhs > 1;
        final int split = rhs / 2;
        int left = 1;
        int right = 1;
        if (split == 1) {
            assert !cardinalityInlits.isEmpty();
            lupper.push(h0);
            llower.push(cardinalityInlits.back());
            cardinalityInlits.pop();
        } else {
            left = split / mod;
            for (int i = 0; i < left; i++) {
                final int p = LngCoreSolver.mkLit(s.nVars(), false);
                MaxSat.newSatVariable(s);
                lupper.push(p);
            }
            int limit = mod - 1;
            if (left % mod == 0 && split < mod - 1) {
                limit = split;
            }
            for (int i = 0; i < limit; i++) {
                final int p = LngCoreSolver.mkLit(s.nVars(), false);
                MaxSat.newSatVariable(s);
                llower.push(p);
            }
        }
        if (rhs - split == 1) {
            assert !cardinalityInlits.isEmpty();
            rupper.push(h0);
            rlower.push(cardinalityInlits.back());
            cardinalityInlits.pop();
        } else {
            right = (rhs - split) / mod;
            for (int i = 0; i < right; i++) {
                final int p = LngCoreSolver.mkLit(s.nVars(), false);
                MaxSat.newSatVariable(s);
                rupper.push(p);
            }
            int limit = mod - 1;
            if (right % mod == 0 && rhs - split < mod - 1) {
                limit = rhs - split;
            }
            for (int i = 0; i < limit; i++) {
                final int p = LngCoreSolver.mkLit(s.nVars(), false);
                MaxSat.newSatVariable(s);
                rlower.push(p);
            }
        }
        if (lupper.isEmpty()) {
            lupper.push(h0);
        }
        if (rupper.isEmpty()) {
            rupper.push(h0);
        }
        adder(s, mod, ublits, lwlits, rupper, rlower, lupper, llower);
        if (left * mod + split - left * mod > 1) {
            toCnf(s, mod, lupper, llower, left * mod + split - left * mod);
        }
        if (right * mod + (rhs - split) - right * mod > 1) {
            toCnf(s, mod, rupper, rlower, right * mod + (rhs - split) - right * mod);
        }
    }

    protected void adder(final LngCoreSolver s, final int mod, final LngIntVector upper, final LngIntVector lower,
                         final LngIntVector lupper, final LngIntVector llower, final LngIntVector rupper,
                         final LngIntVector rlower) {
        assert !upper.isEmpty();
        assert lower.size() >= llower.size() && lower.size() >= rlower.size();
        int carry = LngCoreSolver.LIT_UNDEF;
        if (upper.get(0) != h0) {
            carry = LngCoreSolver.mkLit(s.nVars(), false);
            MaxSat.newSatVariable(s);
        }
        for (int i = 0; i <= llower.size(); i++) {
            for (int j = 0; j <= rlower.size(); j++) {
                if (i + j > currentCardinalityRhs + 1 && currentCardinalityRhs + 1 < modulo) {
                    continue;
                }
                if (i + j < mod) {
                    if (i == 0 && j != 0) {
                        if (upper.get(0) != h0) {
                            addTernaryClause(s, LngCoreSolver.not(rlower.get(j - 1)), lower.get(i + j - 1), carry);
                        } else {
                            addBinaryClause(s, LngCoreSolver.not(rlower.get(j - 1)), lower.get(i + j - 1));
                        }
                    } else if (j == 0 && i != 0) {
                        if (upper.get(0) != h0) {
                            addTernaryClause(s, LngCoreSolver.not(llower.get(i - 1)), lower.get(i + j - 1), carry);
                        } else {
                            addBinaryClause(s, LngCoreSolver.not(llower.get(i - 1)), lower.get(i + j - 1));
                        }
                    } else if (i != 0) {
                        if (upper.get(0) != h0) {
                            addQuaternaryClause(s, LngCoreSolver.not(llower.get(i - 1)),
                                    LngCoreSolver.not(rlower.get(j - 1)), lower.get(i + j - 1), carry);
                        } else {
                            assert i + j - 1 < lower.size();
                            addTernaryClause(s, LngCoreSolver.not(llower.get(i - 1)),
                                    LngCoreSolver.not(rlower.get(j - 1)), lower.get(i + j - 1));
                        }
                    }
                } else if (i + j > mod) {
                    assert i + j > 0;
                    addTernaryClause(s, LngCoreSolver.not(llower.get(i - 1)), LngCoreSolver.not(rlower.get(j - 1)),
                            lower.get((i + j) % mod - 1));
                } else {
                    assert i + j == mod;
                    assert carry != LngCoreSolver.LIT_UNDEF;
                    addTernaryClause(s, LngCoreSolver.not(llower.get(i - 1)), LngCoreSolver.not(rlower.get(j - 1)),
                            carry);
                }
            }
        }
        if (upper.get(0) != h0) {
            for (int i = 0; i <= lupper.size(); i++) {
                for (int j = 0; j <= rupper.size(); j++) {
                    int a = LIT_ERROR;
                    int b = LIT_ERROR;
                    int c = LIT_ERROR;
                    int d = LIT_ERROR;
                    int closeMod = currentCardinalityRhs / mod;
                    if (currentCardinalityRhs % mod != 0) {
                        closeMod++;
                    }
                    if (mod * (i + j) > closeMod * mod) {
                        continue;
                    }
                    if (i != 0) {
                        a = lupper.get(i - 1);
                    }
                    if (j != 0) {
                        b = rupper.get(j - 1);
                    }
                    if (i + j != 0 && i + j - 1 < upper.size()) {
                        c = upper.get(i + j - 1);
                    }
                    if (i + j < upper.size()) {
                        d = upper.get(i + j);
                    }
                    if (c != LngCoreSolver.LIT_UNDEF && c != LIT_ERROR) {
                        final LngIntVector clause = new LngIntVector();
                        if (a != LngCoreSolver.LIT_UNDEF && a != LIT_ERROR) {
                            clause.push(LngCoreSolver.not(a));
                        }
                        if (b != LngCoreSolver.LIT_UNDEF && b != LIT_ERROR) {
                            clause.push(LngCoreSolver.not(b));
                        }
                        clause.push(c);
                        if (clause.size() > 1) {
                            s.addClause(clause, null);
                        }
                    }
                    final LngIntVector clause = new LngIntVector();
                    clause.push(LngCoreSolver.not(carry));
                    if (a != LngCoreSolver.LIT_UNDEF && a != LIT_ERROR) {
                        clause.push(LngCoreSolver.not(a));
                    }
                    if (b != LngCoreSolver.LIT_UNDEF && b != LIT_ERROR) {
                        clause.push(LngCoreSolver.not(b));
                    }
                    if (d != LIT_ERROR && d != LngCoreSolver.LIT_UNDEF) {
                        clause.push(d);
                    }
                    if (clause.size() > 1) {
                        s.addClause(clause, null);
                    }
                }
            }
        }
    }
}
