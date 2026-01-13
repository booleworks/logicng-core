// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings.cc;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResult;
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
     * @return the incremental data for this constraint
     */
    public static <T extends EncodingResult> CcIncrementalData<T> amk(final T result, final Variable[] vars,
                                                                      final int rhs) {
        final var state = new State(result.getFactory());
        final int mod = initialize(result, rhs, vars.length, state);
        for (final Variable var : vars) {
            state.inlits.push(var);
        }
        toCnf(result, mod, state.cardinalityUpOutvars, state.cardinalityLwOutvars, vars.length, state);
        assert state.inlits.isEmpty();
        encodeOutput(result, rhs, mod, state);
        state.currentCardinalityRhs = rhs + 1;
        return new CcIncrementalData<>(result, EncoderConfig.AmkEncoder.MODULAR_TOTALIZER, rhs,
                state.cardinalityUpOutvars,
                state.cardinalityLwOutvars, mod);
    }

    /**
     * Builds an at-least-k constraint.
     * @param result the result of the encoding
     * @param vars   the variables of the constraint
     * @param rhs    the right-hand side of the constraint
     * @return the incremental data for this constraint
     */
    public static <T extends EncodingResult> CcIncrementalData<T> alk(final T result, final Variable[] vars,
                                                                      final int rhs) {
        final var state = new State(result.getFactory());
        final int newRhs = vars.length - rhs;
        final int mod = initialize(result, newRhs, vars.length, state);
        for (final Variable var : vars) {
            state.inlits.push(var.negate(result.getFactory()));
        }
        toCnf(result, mod, state.cardinalityUpOutvars, state.cardinalityLwOutvars, vars.length, state);
        assert state.inlits.isEmpty();
        encodeOutput(result, newRhs, mod, state);
        state.currentCardinalityRhs = newRhs + 1;
        return new CcIncrementalData<>(result, EncoderConfig.AlkEncoder.MODULAR_TOTALIZER, rhs, vars.length,
                state.cardinalityUpOutvars, state.cardinalityLwOutvars, mod);
    }

    private static int initialize(final EncodingResult result, final int rhs, final int n, final State state) {
        state.cardinalityLwOutvars = new LngVector<>();
        final int mod = (int) Math.ceil(Math.sqrt(rhs + 1.0));
        state.cardinalityUpOutvars = new LngVector<>(n / mod);
        for (int i = 0; i < n / mod; i++) {
            state.cardinalityUpOutvars.push(result.newCcVariable());
        }
        state.cardinalityLwOutvars = new LngVector<>(mod - 1);
        for (int i = 0; i < mod - 1; i++) {
            state.cardinalityLwOutvars.push(result.newCcVariable());
        }
        state.inlits = new LngVector<>(n);
        state.currentCardinalityRhs = rhs + 1;
        if (state.cardinalityUpOutvars.isEmpty()) {
            state.cardinalityUpOutvars.push(state.h0);
        }
        return mod;
    }

    private static void encodeOutput(final EncodingResult result, final int rhs, final int mod, final State state) {
        assert !state.cardinalityUpOutvars.isEmpty() || !state.cardinalityLwOutvars.isEmpty();
        final FormulaFactory f = result.getFactory();
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

    private static void toCnf(final EncodingResult result, final int mod, final LngVector<Literal> ubvars,
                              final LngVector<Literal> lwvars, final int rhs,
                              final State state) {
        final LngVector<Literal> lupper = new LngVector<>();
        final LngVector<Literal> llower = new LngVector<>();
        final LngVector<Literal> rupper = new LngVector<>();
        final LngVector<Literal> rlower = new LngVector<>();
        assert rhs > 1;
        final int split = rhs / 2;
        int left = 1;
        int right = 1;
        if (split == 1) {
            assert !state.inlits.isEmpty();
            lupper.push(state.h0);
            llower.push(state.inlits.back());
            state.inlits.pop();
        } else {
            left = split / mod;
            for (int i = 0; i < left; i++) {
                lupper.push(result.newCcVariable());
            }
            int limit = mod - 1;
            if (left % mod == 0 && split < mod - 1) {
                limit = split;
            }
            for (int i = 0; i < limit; i++) {
                llower.push(result.newCcVariable());
            }
        }
        if (rhs - split == 1) {
            assert !state.inlits.isEmpty();
            rupper.push(state.h0);
            rlower.push(state.inlits.back());
            state.inlits.pop();
        } else {
            right = (rhs - split) / mod;
            for (int i = 0; i < right; i++) {
                rupper.push(result.newCcVariable());
            }
            int limit = mod - 1;
            if (right % mod == 0 && rhs - split < mod - 1) {
                limit = rhs - split;
            }
            for (int i = 0; i < limit; i++) {
                rlower.push(result.newCcVariable());
            }
        }
        if (lupper.isEmpty()) {
            lupper.push(state.h0);
        }
        if (rupper.isEmpty()) {
            rupper.push(state.h0);
        }
        adder(result, mod, ubvars, lwvars, rupper, rlower, lupper, llower, state);
        int val = left * mod + split - left * mod;
        if (val > 1) {
            toCnf(result, mod, lupper, llower, val, state);
        }
        val = right * mod + (rhs - split) - right * mod;
        if (val > 1) {
            toCnf(result, mod, rupper, rlower, val, state);
        }
    }

    private static void adder(final EncodingResult result, final int mod, final LngVector<Literal> upper,
                              final LngVector<Literal> lower,
                              final LngVector<Literal> lupper, final LngVector<Literal> llower,
                              final LngVector<Literal> rupper,
                              final LngVector<Literal> rlower, final State state) {
        assert !upper.isEmpty();
        assert lower.size() >= llower.size() && lower.size() >= rlower.size();
        final FormulaFactory f = result.getFactory();
        Variable carry = state.varUndef;
        // != is ok here - we are within the same formula factory
        if (upper.get(0) != state.h0) {
            carry = result.newCcVariable();
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

    private static void finalAdder(final EncodingResult result, final int mod, final LngVector<Literal> upper,
                                   final LngVector<Literal> lupper,
                                   final LngVector<Literal> rupper, final Variable carry, final State state) {
        final FormulaFactory f = result.getFactory();
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
                    final LngVector<Literal> clause = new LngVector<>();
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
                final LngVector<Literal> clause = new LngVector<>();
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
        private LngVector<Literal> inlits;
        private LngVector<Literal> cardinalityUpOutvars;
        private LngVector<Literal> cardinalityLwOutvars;
        private int currentCardinalityRhs;

        private State(final FormulaFactory f) {
            varUndef = f.variable("RESERVED@VAR_UNDEF");
            varError = f.variable("RESERVED@VAR_ERROR");
            h0 = varUndef;
            currentCardinalityRhs = -1;
            inlits = new LngVector<>();
        }
    }
}
