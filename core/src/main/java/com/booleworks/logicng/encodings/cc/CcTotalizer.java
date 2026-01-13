// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings.cc;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResult;
import com.booleworks.logicng.encodings.CcIncrementalData;
import com.booleworks.logicng.encodings.EncoderConfig;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;

/**
 * Totalizer due to Bailleux and Boufkhad.
 * @version 3.0.0
 * @since 1.0
 */
public final class CcTotalizer {

    private enum Bound {
        LOWER,
        UPPER,
        BOTH
    }

    private CcTotalizer() {
        // Only static methods
    }

    /**
     * Builds an at-most-k constraint.
     * @param result the result
     * @param vars   the variables
     * @param rhs    the right-hand side
     * @return the incremental data for the encoded constraint
     * @throws IllegalArgumentException if the right-hand side of the constraint
     *                                  was negative
     */
    public static <T extends EncodingResult> CcIncrementalData<T> amk(final T result, final Variable[] vars,
                                                                      final int rhs) {
        final TotalizerVars tv = initializeConstraint(result, vars);
        toCnf(result, tv, rhs, Bound.UPPER);
        assert tv.invars.isEmpty();
        for (int i = rhs; i < tv.outvars.size(); i++) {
            result.addClause(tv.outvars.get(i).negate(result.getFactory()));
        }
        return new CcIncrementalData<>(result, EncoderConfig.AmkEncoder.TOTALIZER, rhs, tv.outvars);
    }

    /**
     * Builds an at-least-k constraint.
     * @param result the result
     * @param vars   the variables
     * @param rhs    the right-hand side
     * @return the incremental data for the encoded constraint
     * @throws IllegalArgumentException if the right-hand side of the constraint
     *                                  was negative
     */
    public static <T extends EncodingResult> CcIncrementalData<T> alk(final T result,
                                                                      final Variable[] vars, final int rhs) {
        final TotalizerVars tv = initializeConstraint(result, vars);
        toCnf(result, tv, rhs, Bound.LOWER);
        assert tv.invars.isEmpty();
        for (int i = 0; i < rhs; i++) {
            result.addClause(tv.outvars.get(i));
        }
        return new CcIncrementalData<>(result, EncoderConfig.AlkEncoder.TOTALIZER, rhs, vars.length, tv.outvars);
    }

    /**
     * Builds an exactly-k constraint.
     * @param result the encoding result
     * @param vars   the variables
     * @param rhs    the right-hand side
     * @throws IllegalArgumentException if the right-hand side of the constraint
     *                                  was negative
     */
    public static void exk(final EncodingResult result, final Variable[] vars, final int rhs) {
        final TotalizerVars tv = initializeConstraint(result, vars);
        toCnf(result, tv, rhs, Bound.BOTH);
        assert tv.invars.isEmpty();
        for (int i = 0; i < rhs; i++) {
            result.addClause(tv.outvars.get(i));
        }
        for (int i = rhs; i < tv.outvars.size(); i++) {
            result.addClause(tv.outvars.get(i).negate(result.getFactory()));
        }
    }

    private static TotalizerVars initializeConstraint(final EncodingResult result, final Variable[] vars) {
        final var tv = new TotalizerVars(vars.length);
        for (final Variable var : vars) {
            tv.invars.push(var);
            tv.outvars.push(result.newCcVariable());
        }
        return tv;
    }

    private static void toCnf(final EncodingResult result, final TotalizerVars tv, final int rhs, final Bound bound) {
        final LngVector<Variable> left = new LngVector<>();
        final LngVector<Variable> right = new LngVector<>();
        assert tv.outvars.size() > 1;
        final int split = tv.outvars.size() / 2;
        for (int i = 0; i < tv.outvars.size(); i++) {
            if (i < split) {
                if (split == 1) {
                    assert !tv.invars.isEmpty();
                    left.push(tv.invars.back());
                    tv.invars.pop();
                } else {
                    left.push(result.newCcVariable());
                }
            } else {
                if (tv.outvars.size() - split == 1) {
                    assert !tv.invars.isEmpty();
                    right.push(tv.invars.back());
                    tv.invars.pop();
                } else {
                    right.push(result.newCcVariable());
                }
            }
        }
        if (bound == Bound.UPPER || bound == Bound.BOTH) {
            adderAmk(result, left, right, tv.outvars, rhs);
        }
        if (bound == Bound.LOWER || bound == Bound.BOTH) {
            adderAlk(result, left, right, tv.outvars);
        }
        if (left.size() > 1) {
            toCnf(result, tv.withNewOutvars(left), rhs, bound);
        }
        if (right.size() > 1) {
            toCnf(result, tv.withNewOutvars(right), rhs, bound);
        }
    }

    private static void adderAmk(final EncodingResult result, final LngVector<Variable> left,
                                 final LngVector<Variable> right,
                                 final LngVector<Variable> output, final int rhs) {
        assert output.size() == left.size() + right.size();
        final FormulaFactory f = result.getFactory();
        for (int i = 0; i <= left.size(); i++) {
            for (int j = 0; j <= right.size(); j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                if (i + j > rhs + 1) {
                    continue;
                }
                if (i == 0) {
                    result.addClause(right.get(j - 1).negate(f), output.get(j - 1));
                } else if (j == 0) {
                    result.addClause(left.get(i - 1).negate(f), output.get(i - 1));
                } else {
                    result.addClause(left.get(i - 1).negate(f), right.get(j - 1).negate(f), output.get(i + j - 1));
                }
            }
        }
    }

    private static void adderAlk(final EncodingResult result, final LngVector<Variable> left,
                                 final LngVector<Variable> right,
                                 final LngVector<Variable> output) {
        assert output.size() == left.size() + right.size();
        final FormulaFactory f = result.getFactory();
        for (int i = 0; i <= left.size(); i++) {
            for (int j = 0; j <= right.size(); j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                if (i == 0) {
                    result.addClause(right.get(j - 1), output.get(left.size() + j - 1).negate(f));
                } else if (j == 0) {
                    result.addClause(left.get(i - 1), output.get(right.size() + i - 1).negate(f));
                } else {
                    result.addClause(left.get(i - 1), right.get(j - 1), output.get(i + j - 2).negate(f));
                }
            }
        }
    }

    private static final class TotalizerVars {
        private final LngVector<Variable> invars;
        private final LngVector<Variable> outvars;

        private TotalizerVars(final LngVector<Variable> invars, final LngVector<Variable> outvars) {
            this.invars = invars;
            this.outvars = outvars;
        }

        private TotalizerVars(final int length) {
            invars = new LngVector<>(length);
            outvars = new LngVector<>(length);
        }

        private TotalizerVars withNewOutvars(final LngVector<Variable> newOutvars) {
            return new TotalizerVars(invars, newOutvars);
        }
    }
}
