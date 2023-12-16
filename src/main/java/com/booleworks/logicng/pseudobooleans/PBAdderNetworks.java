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

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * The adder networks encoding for pseudo-Boolean constraints to CNF.
 * @version 3.0.0
 * @since 1.1
 */
public final class PBAdderNetworks implements PBEncoding {

    private static final PBAdderNetworks INSTANCE = new PBAdderNetworks();

    private static int ldInt(final int x) {
        int ldretutn = 0;
        for (int i = 0; i < 31; i++) {
            if ((x & (1 << i)) > 0) {
                ldretutn = i + 1;
            }
        }
        return ldretutn;
    }

    private PBAdderNetworks() {
        // Singleton pattern
    }

    public static PBAdderNetworks get() {
        return INSTANCE;
    }

    @Override
    public void encode(final EncodingResult result, final LNGVector<Literal> lits, final LNGIntVector coeffs, final int rhs, final PBConfig config) {
        final LNGVector<Literal> literals = new LNGVector<>();
        final LNGVector<LinkedList<Literal>> buckets = new LNGVector<>();
        final int nb = ldInt(rhs);
        for (int iBit = 0; iBit < nb; ++iBit) {
            buckets.push(new LinkedList<>());
            literals.push(null);
            for (int iVar = 0; iVar < lits.size(); iVar++) {
                if (((1 << iBit) & coeffs.get(iVar)) != 0) {
                    buckets.back().push(lits.get(iVar));
                }
            }
        }
        adderTree(result, buckets, literals);
        final LNGBooleanVector kBits = numToBits(buckets.size(), rhs);
        lessThanOrEqual(result, literals, kBits);
    }

    private static void adderTree(final EncodingResult result, final LNGVector<LinkedList<Literal>> buckets, final LNGVector<Literal> literals) {
        Literal x;
        Literal y;
        Literal z;

        for (int i = 0; i < buckets.size(); i++) {
            if (buckets.get(i).isEmpty()) {
                continue;
            }
            if (i == buckets.size() - 1 && buckets.get(i).size() >= 2) {
                buckets.push(new LinkedList<>());
                literals.push(null);
            }
            while (buckets.get(i).size() >= 3) {
                x = buckets.get(i).removeFirst();
                y = buckets.get(i).removeFirst();
                z = buckets.get(i).removeFirst();
                final Literal xs = faSum(result, x, y, z);
                final Literal xc = faCarry(result, x, y, z);
                buckets.get(i).add(xs);
                buckets.get(i + 1).add(xc);
                faExtra(result, xc, xs, x, y, z);
            }
            if (buckets.get(i).size() == 2) {
                x = buckets.get(i).removeFirst();
                y = buckets.get(i).removeFirst();
                buckets.get(i).add(haSum(result, x, y));
                buckets.get(i + 1).add(haCarry(result, x, y));
            }
            literals.set(i, buckets.get(i).removeFirst());
        }
    }

    private static LNGBooleanVector numToBits(final int n, final int num) {
        int number = num;
        final LNGBooleanVector bits = new LNGBooleanVector();
        for (int i = n - 1; i >= 0; i--) {
            final int tmp = 1 << i;
            if (number < tmp) {
                bits.push(false);
            } else {
                bits.push(true);
                number -= tmp;
            }
        }
        bits.reverseInplace();
        return bits;
    }

    private static void lessThanOrEqual(final EncodingResult result, final LNGVector<Literal> xs, final LNGBooleanVector ys) {
        assert xs.size() == ys.size();
        final FormulaFactory f = result.factory();
        final List<Literal> clause = new ArrayList<>();
        boolean skip;
        for (int i = 0; i < xs.size(); ++i) {
            if (ys.get(i) || xs.get(i) == null) {
                continue;
            }
            clause.clear();
            skip = false;
            for (int j = i + 1; j < xs.size(); ++j) {
                if (ys.get(j)) {
                    if (xs.get(j) == null) {
                        skip = true;
                        break;
                    }
                    clause.add(xs.get(j).negate(f));
                } else {
                    if (xs.get(j) == null) {
                        continue;
                    }
                    clause.add(xs.get(j));
                }
            }
            if (skip) {
                continue;
            }
            clause.add(xs.get(i).negate(f));
            result.addClause(clause.toArray(new Literal[]{}));
        }
    }

    private static void faExtra(final EncodingResult result, final Literal xc, final Literal xs, final Literal a, final Literal b, final Literal c) {
        final FormulaFactory f = result.factory();
        result.addClause(xc.negate(f), xs.negate(f), a);
        result.addClause(xc.negate(f), xs.negate(f), b);
        result.addClause(xc.negate(f), xs.negate(f), c);
        result.addClause(xc, xs, a.negate(f));
        result.addClause(xc, xs, b.negate(f));
        result.addClause(xc, xs, c.negate(f));
    }

    private static Literal faCarry(final EncodingResult result, final Literal a, final Literal b, final Literal c) {
        final FormulaFactory f = result.factory();
        final Literal x = f.newPBVariable();
        result.addClause(b, c, x.negate(f));
        result.addClause(a, c, x.negate(f));
        result.addClause(a, b, x.negate(f));
        result.addClause(b.negate(f), c.negate(f), x);
        result.addClause(a.negate(f), c.negate(f), x);
        result.addClause(a.negate(f), b.negate(f), x);
        return x;
    }

    private static Literal faSum(final EncodingResult result, final Literal a, final Literal b, final Literal c) {
        final FormulaFactory f = result.factory();
        final Literal x = f.newPBVariable();
        result.addClause(a, b, c, x.negate(f));
        result.addClause(a, b.negate(f), c.negate(f), x.negate(f));
        result.addClause(a.negate(f), b, c.negate(f), x.negate(f));
        result.addClause(a.negate(f), b.negate(f), c, x.negate(f));
        result.addClause(a.negate(f), b.negate(f), c.negate(f), x);
        result.addClause(a.negate(f), b, c, x);
        result.addClause(a, b.negate(f), c, x);
        result.addClause(a, b, c.negate(f), x);
        return x;
    }

    private static Literal haCarry(final EncodingResult result, final Literal a, final Literal b) {
        final FormulaFactory f = result.factory();
        final Literal x = f.newPBVariable();
        result.addClause(a, x.negate(f));
        result.addClause(b, x.negate(f));
        result.addClause(a.negate(f), b.negate(f), x);
        return x;
    }

    private static Literal haSum(final EncodingResult result, final Literal a, final Literal b) {
        final FormulaFactory f = result.factory();
        final Literal x = f.newPBVariable();
        result.addClause(a.negate(f), b.negate(f), x.negate(f));
        result.addClause(a, b, x.negate(f));
        result.addClause(a.negate(f), b, x);
        result.addClause(a, b.negate(f), x);
        return x;
    }
}
