// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * PBLib -- Copyright (c) 2012-2013 Peter Steinke <p> Permission is hereby
 * granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions: <p> The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of
 * the Software. <p> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.booleworks.logicng.encodings.pbc;

import static com.booleworks.logicng.encodings.cc.CcSorting.ImplicationDirection.INPUT_TO_OUTPUT;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.encodings.cc.CcSorting;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;

/**
 * The binary merge encoding for pseudo-Boolean constraints to CNF due to
 * Manthey, Philipp, and Steinke.
 * @version 3.0.0
 * @since 1.1
 */
public final class PbBinaryMerge {

    public static void encode(final EncodingResult result, final LngVector<Literal> lts, final LngIntVector cffs,
                              final int rhs, final boolean useGac, final boolean noSuppoertSingleBit,
                              final boolean useWatchDog) {
        final FormulaFactory f = result.getFactory();
        final LngVector<Literal> lits = new LngVector<>(lts.size());
        for (final Literal lit : lts) {
            lits.push(lit);
        }
        final LngIntVector coeffs = new LngIntVector(cffs);
        final int maxWeight = maxWeight(coeffs);
        if (!useGac) {
            binaryMerge(result, lts, new LngIntVector(cffs), rhs, maxWeight, lits.size(), null, useWatchDog);
        } else {
            Literal x;
            boolean encode_complete_constraint = false;
            for (int i = 0; i < lits.size(); i++) {
                final boolean condition = Double.compare(
                        Math.floor(Math.log(coeffs.get(i)) / Math.log(2)),
                        Math.log(coeffs.get(i)) / Math.log(2)) == 0;
                if (noSuppoertSingleBit && condition) {
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
                            result.addClause(x.negate(f), lits.get(j).negate(f));
                        }
                    } else {
                        binaryMerge(result, lits, coeffs, rhs - tmpCoeff, mw, lits.size(), x.negate(f), useWatchDog);
                    }
                } else {
                    if (rhs - tmpCoeff <= 0) {
                        for (int j = 0; j < lits.size(); j++) {
                            result.addClause(x.negate(f), lits.get(j).negate(f));
                        }
                    }
                    binaryMerge(result, lits, coeffs, rhs - tmpCoeff, maxWeight, lits.size(), x.negate(f), useWatchDog);
                }
                if (i < lits.size()) {
                    lits.push(lits.get(i));
                    lits.set(i, tmpLit);
                    coeffs.push(coeffs.get(i));
                    coeffs.set(i, tmpCoeff);
                }
            }
            if (noSuppoertSingleBit && encode_complete_constraint) {
                binaryMerge(result, lts, new LngIntVector(cffs), rhs, maxWeight, lits.size(), null, useWatchDog);
            }
        }
    }

    private static int maxWeight(final LngIntVector weights) {
        int maxweight = Integer.MIN_VALUE;
        for (int i = 0; i < weights.size(); i++) {
            if (weights.get(i) > maxweight) {
                maxweight = weights.get(i);
            }
        }
        return maxweight;
    }

    private static void binaryMerge(final EncodingResult result, final LngVector<Literal> literals,
                                    final LngIntVector coefficients, final int leq,
                                    final int maxWeight, final int n, final Literal gac_lit,
                                    final boolean useWatchDog) {
        final FormulaFactory f = result.getFactory();
        final int lessThen = leq + 1;
        final int p = (int) Math.floor(Math.log(maxWeight) / Math.log(2));
        final int m = (int) Math.ceil(lessThen / Math.pow(2, p));
        final int newLessThen = (int) (m * Math.pow(2, p));
        final int t = (int) ((m * Math.pow(2, p)) - lessThen);

        final Literal true_lit = f.newPbVariable();
        result.addClause(true_lit);
        final LngVector<LngVector<Literal>> buckets = new LngVector<>();
        int bit = 1;
        for (int i = 0; i <= p; i++) {
            buckets.push(new LngVector<>());
            if ((t & bit) != 0) {
                buckets.get(i).push(true_lit);
            }
            for (int j = 0; j < n; j++) {
                if ((coefficients.get(j) & bit) != 0) {
                    if (gac_lit != null && coefficients.get(j) >= lessThen) {
                        result.addClause(gac_lit, literals.get(j).negate(f));
                    } else {
                        buckets.get(i).push(literals.get(j));
                    }
                }
            }
            bit = bit << 1;
        }
        final LngVector<LngVector<Literal>> bucket_card = new LngVector<>(p + 1);
        final LngVector<LngVector<Literal>> bucket_merge = new LngVector<>(p + 1);
        for (int i = 0; i < p + 1; i++) {
            bucket_card.push(new LngVector<>());
            bucket_merge.push(new LngVector<>());
        }
        assert bucket_card.size() == buckets.size();
        final LngVector<Literal> carries = new LngVector<>();
        for (int i = 0; i < buckets.size(); i++) {
            final int k = (int) Math.ceil(newLessThen / Math.pow(2, i));
            if (useWatchDog) {
                totalizer(result, buckets.get(i), bucket_card.get(i));
            } else {
                CcSorting.sort(f, k, buckets.get(i), result, bucket_card.get(i), INPUT_TO_OUTPUT);
            }
            if (k <= buckets.get(i).size()) {
                assert k == bucket_card.get(i).size() || useWatchDog;
                if (gac_lit != null) {
                    result.addClause(gac_lit, bucket_card.get(i).get(k - 1).negate(f));
                } else {
                    result.addClause(bucket_card.get(i).get(k - 1).negate(f));
                }
            }
            if (i > 0) {
                if (!carries.isEmpty()) {
                    if (bucket_card.get(i).isEmpty()) {
                        bucket_merge.set(i, carries);
                    } else {
                        if (useWatchDog) {
                            unary_adder(result, bucket_card.get(i), carries, bucket_merge.get(i));
                        } else {
                            CcSorting.merge(f, k, bucket_card.get(i), carries, result, bucket_merge.get(i),
                                    INPUT_TO_OUTPUT);
                        }
                        if (k == bucket_merge.get(i).size() || (useWatchDog && k <= bucket_merge.get(i).size())) {
                            if (gac_lit != null) {
                                result.addClause(gac_lit, bucket_merge.get(i).get(k - 1).negate(f));
                            } else {
                                result.addClause(bucket_merge.get(i).get(k - 1).negate(f));
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

    private static void totalizer(final EncodingResult result, final LngVector<Literal> x,
                                  final LngVector<Literal> u_x) {
        u_x.clear();
        if (x.isEmpty()) {
            return;
        }
        if (x.size() == 1) {
            u_x.push(x.get(0));
        } else {
            for (int i = 0; i < x.size(); i++) {
                u_x.push(result.getFactory().newPbVariable());
            }
            final LngVector<Literal> x_1 = new LngVector<>();
            final LngVector<Literal> x_2 = new LngVector<>();
            int i = 0;
            for (; i < x.size() / 2; i++) {
                x_1.push(x.get(i));
            }
            for (; i < x.size(); i++) {
                x_2.push(x.get(i));
            }
            final LngVector<Literal> u_x_1 = new LngVector<>();
            final LngVector<Literal> u_x_2 = new LngVector<>();
            totalizer(result, x_1, u_x_1);
            totalizer(result, x_2, u_x_2);
            unary_adder(result, u_x_1, u_x_2, u_x);
        }
    }

    private static void unary_adder(final EncodingResult result, final LngVector<Literal> u, final LngVector<Literal> v,
                                    final LngVector<Literal> w) {
        final FormulaFactory f = result.getFactory();
        w.clear();
        if (u.isEmpty()) {
            for (int i = 0; i < v.size(); i++) {
                w.push(v.get(i));
            }
        } else if (v.isEmpty()) {
            for (int i = 0; i < u.size(); i++) {
                w.push(u.get(i));
            }
        } else {
            for (int i = 0; i < u.size() + v.size(); i++) {
                w.push(f.newPbVariable());
            }
            for (int a = 0; a < u.size(); a++) {
                for (int b = 0; b < v.size(); b++) {
                    result.addClause(u.get(a).negate(f), v.get(b).negate(f), w.get(a + b + 1));
                }
            }
            for (int i = 0; i < v.size(); i++) {
                result.addClause(v.get(i).negate(f), w.get(i));
            }
            for (int i = 0; i < u.size(); i++) {
                result.addClause(u.get(i).negate(f), w.get(i));
            }
        }
    }
}
