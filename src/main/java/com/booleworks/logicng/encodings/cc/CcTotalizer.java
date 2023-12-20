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
    public static CcIncrementalData amk(final EncodingResult result, final Variable[] vars, final int rhs) {
        final TotalizerVars tv = initializeConstraint(result, vars);
        toCNF(result, tv, rhs, Bound.UPPER);
        assert tv.invars.size() == 0;
        for (int i = rhs; i < tv.outvars.size(); i++) {
            result.addClause(tv.outvars.get(i).negate(result.factory()));
        }
        return new CcIncrementalData(result, EncoderConfig.AMK_ENCODER.TOTALIZER, rhs, tv.outvars);
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
    public static CcIncrementalData alk(final EncodingResult result, final Variable[] vars, final int rhs) {
        final TotalizerVars tv = initializeConstraint(result, vars);
        toCNF(result, tv, rhs, Bound.LOWER);
        assert tv.invars.size() == 0;
        for (int i = 0; i < rhs; i++) {
            result.addClause(tv.outvars.get(i));
        }
        return new CcIncrementalData(result, EncoderConfig.ALK_ENCODER.TOTALIZER, rhs, vars.length, tv.outvars);
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
        toCNF(result, tv, rhs, Bound.BOTH);
        assert tv.invars.size() == 0;
        for (int i = 0; i < rhs; i++) {
            result.addClause(tv.outvars.get(i));
        }
        for (int i = rhs; i < tv.outvars.size(); i++) {
            result.addClause(tv.outvars.get(i).negate(result.factory()));
        }
    }

    private static TotalizerVars initializeConstraint(final EncodingResult result, final Variable[] vars) {
        final var tv = new TotalizerVars(vars.length);
        for (final Variable var : vars) {
            tv.invars.push(var);
            tv.outvars.push(result.newVariable());
        }
        return tv;
    }

    private static void toCNF(final EncodingResult result, final TotalizerVars tv, final int rhs, final Bound bound) {
        final LNGVector<Variable> left = new LNGVector<>();
        final LNGVector<Variable> right = new LNGVector<>();
        assert tv.outvars.size() > 1;
        final int split = tv.outvars.size() / 2;
        for (int i = 0; i < tv.outvars.size(); i++) {
            if (i < split) {
                if (split == 1) {
                    assert tv.invars.size() > 0;
                    left.push(tv.invars.back());
                    tv.invars.pop();
                } else {
                    left.push(result.newVariable());
                }
            } else {
                if (tv.outvars.size() - split == 1) {
                    assert tv.invars.size() > 0;
                    right.push(tv.invars.back());
                    tv.invars.pop();
                } else {
                    right.push(result.newVariable());
                }
            }
        }
        if (bound == Bound.UPPER || bound == Bound.BOTH) {
            adderAMK(result, left, right, tv.outvars, rhs);
        }
        if (bound == Bound.LOWER || bound == Bound.BOTH) {
            adderALK(result, left, right, tv.outvars);
        }
        if (left.size() > 1) {
            toCNF(result, tv.withNewOutvars(left), rhs, bound);
        }
        if (right.size() > 1) {
            toCNF(result, tv.withNewOutvars(right), rhs, bound);
        }
    }

    private static void adderAMK(final EncodingResult result, final LNGVector<Variable> left,
                                 final LNGVector<Variable> right,
                                 final LNGVector<Variable> output, final int rhs) {
        assert output.size() == left.size() + right.size();
        final FormulaFactory f = result.factory();
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

    private static void adderALK(final EncodingResult result, final LNGVector<Variable> left,
                                 final LNGVector<Variable> right,
                                 final LNGVector<Variable> output) {
        assert output.size() == left.size() + right.size();
        final FormulaFactory f = result.factory();
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
        private final LNGVector<Variable> invars;
        private final LNGVector<Variable> outvars;

        private TotalizerVars(final LNGVector<Variable> invars, final LNGVector<Variable> outvars) {
            this.invars = invars;
            this.outvars = outvars;
        }

        private TotalizerVars(final int length) {
            invars = new LNGVector<>(length);
            outvars = new LNGVector<>(length);
        }

        private TotalizerVars withNewOutvars(final LNGVector<Variable> newOutvars) {
            return new TotalizerVars(invars, newOutvars);
        }
    }
}
