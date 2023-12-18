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

package com.booleworks.logicng.encodings.cc;

import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.encodings.CcIncrementalData;
import com.booleworks.logicng.encodings.EncoderConfig;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

/**
 * Modular Totalizer.
 * @version 3.0.0
 * @since 1.0
 */
public final class CcModularTotalizer {

    private CcModularTotalizer() {
        // Only static classes
    }

    /**
     * Builds an at-most-k constraint.
     * @param result the result of the encoding
     * @param vars   the variables of the constraint
     * @param rhs    the right-hand side of the constraint
     */
    public static CcIncrementalData amk(final EncodingResult result, final Variable[] vars, final int rhs) {
        final var state = new State(result.factory());
        final int mod = initialize(result, rhs, vars.length, state);
        for (final Variable var : vars) {
            state.inlits.push(var);
        }
        toCNF(result, mod, state.cardinalityUpOutvars, state.cardinalityLwOutvars, vars.length, state);
        assert state.inlits.size() == 0;
        encodeOutput(result, rhs, mod, state);
        state.currentCardinalityRhs = rhs + 1;
        return new CcIncrementalData(result, EncoderConfig.AMK_ENCODER.MODULAR_TOTALIZER, rhs,
                state.cardinalityUpOutvars,
                state.cardinalityLwOutvars, mod);
    }

    /**
     * Builds an at-least-k constraint.
     * @param result the result of the encoding
     * @param vars   the variables of the constraint
     * @param rhs    the right-hand side of the constraint
     */
    public static CcIncrementalData alk(final EncodingResult result, final Variable[] vars, final int rhs) {
        final var state = new State(result.factory());
        final int newRHS = vars.length - rhs;
        final int mod = initialize(result, newRHS, vars.length, state);
        for (final Variable var : vars) {
            state.inlits.push(var.negate(result.factory()));
        }
        toCNF(result, mod, state.cardinalityUpOutvars, state.cardinalityLwOutvars, vars.length, state);
        assert state.inlits.size() == 0;
        encodeOutput(result, newRHS, mod, state);
        state.currentCardinalityRhs = newRHS + 1;
        return new CcIncrementalData(result, EncoderConfig.ALK_ENCODER.MODULAR_TOTALIZER, rhs, vars.length,
                state.cardinalityUpOutvars, state.cardinalityLwOutvars, mod);
    }

    private static int initialize(final EncodingResult result, final int rhs, final int n, final State state) {
        state.cardinalityLwOutvars = new LNGVector<>();
        final int mod = (int) Math.ceil(Math.sqrt(rhs + 1.0));
        state.cardinalityUpOutvars = new LNGVector<>(n / mod);
        for (int i = 0; i < n / mod; i++) {
            state.cardinalityUpOutvars.push(result.newVariable());
        }
        state.cardinalityLwOutvars = new LNGVector<>(mod - 1);
        for (int i = 0; i < mod - 1; i++) {
            state.cardinalityLwOutvars.push(result.newVariable());
        }
        state.inlits = new LNGVector<>(n);
        state.currentCardinalityRhs = rhs + 1;
        if (state.cardinalityUpOutvars.size() == 0) {
            state.cardinalityUpOutvars.push(state.h0);
        }
        return mod;
    }

    private static void encodeOutput(final EncodingResult result, final int rhs, final int mod, final State state) {
        assert state.cardinalityUpOutvars.size() != 0 || state.cardinalityLwOutvars.size() != 0;
        final FormulaFactory f = result.factory();
        final int ulimit = (rhs + 1) / mod;
        final int llimit = (rhs + 1) - ulimit * mod;
        assert ulimit <= state.cardinalityUpOutvars.size();
        assert llimit <= state.cardinalityLwOutvars.size();
        for (int i = ulimit; i < state.cardinalityUpOutvars.size(); i++) {
            result.addClause(state.cardinalityUpOutvars.get(i).negate(f));
        }
        if (ulimit != 0 && llimit != 0) {
            for (int i = llimit - 1; i < state.cardinalityLwOutvars.size(); i++) {
                result.addClause(state.cardinalityUpOutvars.get(ulimit - 1).negate(f),
                        state.cardinalityLwOutvars.get(i).negate(f));
            }
        } else {
            if (ulimit == 0) {
                assert llimit != 0;
                for (int i = llimit - 1; i < state.cardinalityLwOutvars.size(); i++) {
                    result.addClause(state.cardinalityLwOutvars.get(i).negate(f));
                }
            } else {
                result.addClause(state.cardinalityUpOutvars.get(ulimit - 1).negate(f));
            }
        }
    }

    private static void toCNF(final EncodingResult result, final int mod, final LNGVector<Literal> ubvars,
                              final LNGVector<Literal> lwvars, final int rhs,
                              final State state) {
        final LNGVector<Literal> lupper = new LNGVector<>();
        final LNGVector<Literal> llower = new LNGVector<>();
        final LNGVector<Literal> rupper = new LNGVector<>();
        final LNGVector<Literal> rlower = new LNGVector<>();
        assert rhs > 1;
        final int split = rhs / 2;
        int left = 1;
        int right = 1;
        if (split == 1) {
            assert state.inlits.size() > 0;
            lupper.push(state.h0);
            llower.push(state.inlits.back());
            state.inlits.pop();
        } else {
            left = split / mod;
            for (int i = 0; i < left; i++) {
                lupper.push(result.newVariable());
            }
            int limit = mod - 1;
            if (left % mod == 0 && split < mod - 1) {
                limit = split;
            }
            for (int i = 0; i < limit; i++) {
                llower.push(result.newVariable());
            }
        }
        if (rhs - split == 1) {
            assert state.inlits.size() > 0;
            rupper.push(state.h0);
            rlower.push(state.inlits.back());
            state.inlits.pop();
        } else {
            right = (rhs - split) / mod;
            for (int i = 0; i < right; i++) {
                rupper.push(result.newVariable());
            }
            int limit = mod - 1;
            if (right % mod == 0 && rhs - split < mod - 1) {
                limit = rhs - split;
            }
            for (int i = 0; i < limit; i++) {
                rlower.push(result.newVariable());
            }
        }
        if (lupper.size() == 0) {
            lupper.push(state.h0);
        }
        if (rupper.size() == 0) {
            rupper.push(state.h0);
        }
        adder(result, mod, ubvars, lwvars, rupper, rlower, lupper, llower, state);
        int val = left * mod + split - left * mod;
        if (val > 1) {
            toCNF(result, mod, lupper, llower, val, state);
        }
        val = right * mod + (rhs - split) - right * mod;
        if (val > 1) {
            toCNF(result, mod, rupper, rlower, val, state);
        }
    }

    private static void adder(final EncodingResult result, final int mod, final LNGVector<Literal> upper,
                              final LNGVector<Literal> lower,
                              final LNGVector<Literal> lupper, final LNGVector<Literal> llower,
                              final LNGVector<Literal> rupper,
                              final LNGVector<Literal> rlower, final State state) {
        assert upper.size() != 0;
        assert lower.size() >= llower.size() && lower.size() >= rlower.size();
        final FormulaFactory f = result.factory();
        Variable carry = state.varUndef;
        // != is ok here - we are within the same formula factory
        if (upper.get(0) != state.h0) {
            carry = result.newVariable();
        }
        for (int i = 0; i <= llower.size(); i++) {
            for (int j = 0; j <= rlower.size(); j++) {
                if (i + j > state.currentCardinalityRhs + 1 && state.currentCardinalityRhs + 1 < mod) {
                    continue;
                }
                if (i + j < mod) {
                    if (i == 0 && j != 0) {
                        if (upper.get(0) != state.h0) {
                            result.addClause(rlower.get(j - 1).negate(f), lower.get(i + j - 1), carry);
                        } else {
                            result.addClause(rlower.get(j - 1).negate(f), lower.get(i + j - 1));
                        }
                    } else if (j == 0 && i != 0) {
                        if (upper.get(0) != state.h0) {
                            result.addClause(llower.get(i - 1).negate(f), lower.get(i + j - 1), carry);
                        } else {
                            result.addClause(llower.get(i - 1).negate(f), lower.get(i + j - 1));
                        }
                    } else if (i != 0) {
                        if (upper.get(0) != state.h0) {
                            result.addClause(llower.get(i - 1).negate(f), rlower.get(j - 1).negate(f),
                                    lower.get(i + j - 1), carry);
                        } else {
                            assert i + j - 1 < lower.size();
                            result.addClause(llower.get(i - 1).negate(f), rlower.get(j - 1).negate(f),
                                    lower.get(i + j - 1));
                        }
                    }
                } else if (i + j > mod) {
                    assert i + j > 0;
                    result.addClause(llower.get(i - 1).negate(f), rlower.get(j - 1).negate(f),
                            lower.get((i + j) % mod - 1));
                } else {
                    assert i + j == mod;
                    assert carry != state.varUndef;
                    result.addClause(llower.get(i - 1).negate(f), rlower.get(j - 1).negate(f), carry);
                }
            }
        }
        if (upper.get(0) != state.h0) {
            finalAdder(result, mod, upper, lupper, rupper, carry, state);
        }
    }

    private static void finalAdder(final EncodingResult result, final int mod, final LNGVector<Literal> upper,
                                   final LNGVector<Literal> lupper,
                                   final LNGVector<Literal> rupper, final Variable carry, final State state) {
        final FormulaFactory f = result.factory();
        for (int i = 0; i <= lupper.size(); i++) {
            for (int j = 0; j <= rupper.size(); j++) {
                Literal a = state.varError;
                Literal b = state.varError;
                Literal c = state.varError;
                Literal d = state.varError;
                int closeMod = state.currentCardinalityRhs / mod;
                if (state.currentCardinalityRhs % mod != 0) {
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
                if (c != state.varUndef && c != state.varError) {
                    final LNGVector<Literal> clause = new LNGVector<>();
                    if (a != state.varUndef && a != state.varError) {
                        clause.push(a.negate(f));
                    }
                    if (b != state.varUndef && b != state.varError) {
                        clause.push(b.negate(f));
                    }
                    clause.push(c);
                    if (clause.size() > 1) {
                        result.addClause(clause);
                    }
                }
                final LNGVector<Literal> clause = new LNGVector<>();
                clause.push(carry.negate(f));
                if (a != state.varUndef && a != state.varError) {
                    clause.push(a.negate(f));
                }
                if (b != state.varUndef && b != state.varError) {
                    clause.push(b.negate(f));
                }
                if (d != state.varError && d != state.varUndef) {
                    clause.push(d);
                }
                if (clause.size() > 1) {
                    result.addClause(clause);
                }
            }
        }
    }

    private static final class State {
        private final Variable varUndef;
        private final Variable varError;
        private final Variable h0;
        private LNGVector<Literal> inlits;
        private LNGVector<Literal> cardinalityUpOutvars;
        private LNGVector<Literal> cardinalityLwOutvars;
        private int currentCardinalityRhs;

        private State(final FormulaFactory f) {
            varUndef = f.variable("RESERVED@VAR_UNDEF");
            varError = f.variable("RESERVED@VAR_ERROR");
            h0 = varUndef;
            currentCardinalityRhs = -1;
            inlits = new LNGVector<>();
        }
    }
}
