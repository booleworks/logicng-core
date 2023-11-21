// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * PBLib       -- Copyright (c) 2012-2013  Peter Steinke
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.booleworks.logicng.pseudobooleans;

import static com.booleworks.logicng.cardinalityconstraints.CCSorting.ImplicationDirection.INPUT_TO_OUTPUT;

import com.booleworks.logicng.cardinalityconstraints.CCSorting;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;

import java.util.List;

/**
 * The binary merge encoding for pseudo-Boolean constraints to CNF due to Manthey, Philipp, and Steinke.
 * @version 2.0.0
 * @since 1.1
 */
public final class PBBinaryMerge implements PBEncoding {

    private static final PBBinaryMerge INSTANCE = new PBBinaryMerge();

    private PBBinaryMerge() {
        // Singleton pattern
    }

    public static PBBinaryMerge get() {
        return INSTANCE;
    }

    @Override
    public List<Formula> encode(final FormulaFactory f, final LNGVector<Literal> lts, final LNGIntVector cffs, final int rhs, final List<Formula> formula,
                                final PBConfig initConfig) {
        final PBConfig config = initConfig != null ? initConfig : (PBConfig) f.configurationFor(ConfigurationType.PB_ENCODER);
        final LNGVector<Literal> lits = new LNGVector<>(lts.size());
        for (final Literal lit : lts) {
            lits.push(lit);
        }
        final LNGIntVector coeffs = new LNGIntVector(cffs);
        final int maxWeight = maxWeight(coeffs);
        if (!config.binaryMergeUseGAC) {
            binary_merge(f, lts, new LNGIntVector(cffs), rhs, maxWeight, lits.size(), formula, null, config);
        } else {
            Literal x;
            boolean encode_complete_constraint = false;
            for (int i = 0; i < lits.size(); i++) {
                if (config.binaryMergeNoSupportForSingleBit && Double.compare(Math.floor(Math.log(coeffs.get(i)) / Math.log(2)), Math.log(coeffs.get(i)) / Math.log(2)) == 0) {
                    encode_complete_constraint = true;
                    continue;
                }
                final Literal tmpLit = lits.get(i);
                final int tmpCoeff = coeffs.get(i);
                lits.set(i, lits.back());
                coeffs.set(i, coeffs.back());
                lits.pop();
                coeffs.pop();
                x = tmpLit;
                if (maxWeight == tmpCoeff) {
                    int mw = 0;
                    for (int j = 0; j < coeffs.size(); j++) {
                        mw = Math.max(mw, coeffs.get(j));
                    }
                    if (rhs - tmpCoeff <= 0) {
                        for (int j = 0; j < lits.size(); j++) {
                            formula.add(f.clause(x.negate(f), lits.get(j).negate(f)));
                        }
                    } else {
                        binary_merge(f, lits, coeffs, rhs - tmpCoeff, mw, lits.size(), formula, x.negate(f), config);
                    }
                } else {
                    if (rhs - tmpCoeff <= 0) {
                        for (int j = 0; j < lits.size(); j++) {
                            formula.add(f.clause(x.negate(f), lits.get(j).negate(f)));
                        }
                    }
                    binary_merge(f, lits, coeffs, rhs - tmpCoeff, maxWeight, lits.size(), formula, x.negate(f), config);
                }
                if (i < lits.size()) {
                    lits.push(lits.get(i));
                    lits.set(i, tmpLit);
                    coeffs.push(coeffs.get(i));
                    coeffs.set(i, tmpCoeff);
                }
            }
            if (config.binaryMergeNoSupportForSingleBit && encode_complete_constraint) {
                binary_merge(f, lts, new LNGIntVector(cffs), rhs, maxWeight, lits.size(), formula, null, config);
            }
        }
        return formula;
    }

    private static int maxWeight(final LNGIntVector weights) {
        int maxweight = Integer.MIN_VALUE;
        for (int i = 0; i < weights.size(); i++) {
            if (weights.get(i) > maxweight) {
                maxweight = weights.get(i);
            }
        }
        return maxweight;
    }

    private static void binary_merge(final FormulaFactory f, final LNGVector<Literal> literals, final LNGIntVector coefficients, final int leq, final int maxWeight, final int n,
                                     final List<Formula> formula, final Literal gac_lit, final PBConfig config) {
        final int less_then = leq + 1;
        final int p = (int) Math.floor(Math.log(maxWeight) / Math.log(2));
        final int m = (int) Math.ceil(less_then / Math.pow(2, p));
        final int new_less_then = (int) (m * Math.pow(2, p));
        final int T = (int) ((m * Math.pow(2, p)) - less_then);

        final Literal true_lit = f.newPBVariable();
        formula.add(true_lit);
        final LNGVector<LNGVector<Literal>> buckets = new LNGVector<>();
        int bit = 1;
        for (int i = 0; i <= p; i++) {
            buckets.push(new LNGVector<>());
            if ((T & bit) != 0) {
                buckets.get(i).push(true_lit);
            }
            for (int j = 0; j < n; j++) {
                if ((coefficients.get(j) & bit) != 0) {
                    if (gac_lit != null && coefficients.get(j) >= less_then) {
                        formula.add(f.clause(gac_lit, literals.get(j).negate(f)));
                    } else {
                        buckets.get(i).push(literals.get(j));
                    }
                }
            }
            bit = bit << 1;
        }
        final LNGVector<LNGVector<Literal>> bucket_card = new LNGVector<>(p + 1);
        final LNGVector<LNGVector<Literal>> bucket_merge = new LNGVector<>(p + 1);
        for (int i = 0; i < p + 1; i++) {
            bucket_card.push(new LNGVector<>());
            bucket_merge.push(new LNGVector<>());
        }
        assert bucket_card.size() == buckets.size();
        final LNGVector<Literal> carries = new LNGVector<>();
        final EncodingResult tempResul = EncodingResult.resultForFormula(f);
        for (int i = 0; i < buckets.size(); i++) {
            final int k = (int) Math.ceil(new_less_then / Math.pow(2, i));
            if (config.binaryMergeUseWatchDog) {
                totalizer(f, buckets.get(i), bucket_card.get(i), formula);
            } else {
                CCSorting.sort(f, k, buckets.get(i), tempResul, bucket_card.get(i), INPUT_TO_OUTPUT);
                formula.addAll(tempResul.result());
            }
            if (k <= buckets.get(i).size()) {
                assert k == bucket_card.get(i).size() || config.binaryMergeUseWatchDog;
                if (gac_lit != null) {
                    formula.add(f.clause(gac_lit, bucket_card.get(i).get(k - 1).negate(f)));
                } else {
                    formula.add(f.clause(bucket_card.get(i).get(k - 1).negate(f)));
                }
            }
            if (i > 0) {
                if (carries.size() > 0) {
                    if (bucket_card.get(i).size() == 0) {
                        bucket_merge.set(i, carries);
                    } else {
                        if (config.binaryMergeUseWatchDog) {
                            unary_adder(f, bucket_card.get(i), carries, bucket_merge.get(i), formula);
                        } else {
                            CCSorting.merge(f, k, bucket_card.get(i), carries, tempResul, bucket_merge.get(i), INPUT_TO_OUTPUT);
                            formula.addAll(tempResul.result());
                        }
                        if (k == bucket_merge.get(i).size() || (config.binaryMergeUseWatchDog && k <= bucket_merge.get(i).size())) {
                            if (gac_lit != null) {
                                formula.add(f.clause(gac_lit, bucket_merge.get(i).get(k - 1).negate(f)));
                            } else {
                                formula.add(f.clause(bucket_merge.get(i).get(k - 1).negate(f)));
                            }
                        }
                    }
                } else {
                    bucket_merge.get(i).replaceInplace(bucket_card.get(i));
                }
            }
            carries.clear();
            if (i == 0) {
                for (int j = 1; j < bucket_card.get(0).size(); j = j + 2) {
                    carries.push(bucket_card.get(0).get(j));
                }
            } else {
                for (int j = 1; j < bucket_merge.get(i).size(); j = j + 2) {
                    carries.push(bucket_merge.get(i).get(j));
                }
            }
        }
    }

    private static void totalizer(final FormulaFactory f, final LNGVector<Literal> x, final LNGVector<Literal> u_x, final List<Formula> formula) {
        u_x.clear();
        if (x.size() == 0) {
            return;
        }
        if (x.size() == 1) {
            u_x.push(x.get(0));
        } else {
            for (int i = 0; i < x.size(); i++) {
                u_x.push(f.newPBVariable());
            }
            final LNGVector<Literal> x_1 = new LNGVector<>();
            final LNGVector<Literal> x_2 = new LNGVector<>();
            int i = 0;
            for (; i < x.size() / 2; i++) {
                x_1.push(x.get(i));
            }
            for (; i < x.size(); i++) {
                x_2.push(x.get(i));
            }
            final LNGVector<Literal> u_x_1 = new LNGVector<>();
            final LNGVector<Literal> u_x_2 = new LNGVector<>();
            totalizer(f, x_1, u_x_1, formula);
            totalizer(f, x_2, u_x_2, formula);
            unary_adder(f, u_x_1, u_x_2, u_x, formula);
        }
    }

    private static void unary_adder(final FormulaFactory f, final LNGVector<Literal> u, final LNGVector<Literal> v, final LNGVector<Literal> w,
                                    final List<Formula> formula) {
        w.clear();
        if (u.size() == 0) {
            for (int i = 0; i < v.size(); i++) {
                w.push(v.get(i));
            }
        } else if (v.size() == 0) {
            for (int i = 0; i < u.size(); i++) {
                w.push(u.get(i));
            }
        } else {
            for (int i = 0; i < u.size() + v.size(); i++) {
                w.push(f.newPBVariable());
            }
            for (int a = 0; a < u.size(); a++) {
                for (int b = 0; b < v.size(); b++) {
                    formula.add(f.clause(u.get(a).negate(f), v.get(b).negate(f), w.get(a + b + 1)));
                }
            }
            for (int i = 0; i < v.size(); i++) {
                formula.add(f.clause(v.get(i).negate(f), w.get(i)));
            }
            for (int i = 0; i < u.size(); i++) {
                formula.add(f.clause(u.get(i).negate(f), w.get(i)));
            }
        }
    }
}
