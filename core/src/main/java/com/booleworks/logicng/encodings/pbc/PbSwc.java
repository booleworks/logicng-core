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

package com.booleworks.logicng.encodings.pbc;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;

/**
 * A sequential weight counter for the encoding of pseudo-Boolean constraints in
 * CNF.
 * @version 3.0.0
 * @since 1.0
 */
public final class PbSwc {

    public static void encode(final EncodingResult result, final LNGVector<Literal> lits, final LNGIntVector coeffs,
                              final int rhs) {
        generateConstraint(result, rhs, lits, coeffs);
    }

    private static void generateConstraint(final EncodingResult result, final int rhs, final LNGVector<Literal> lits,
                                           final LNGIntVector coeffs) {
        final FormulaFactory f = result.factory();
        final int n = lits.size();
        final LNGVector<LNGVector<Literal>> seqAuxiliary = new LNGVector<>(n + 1);
        for (int i = 0; i < n + 1; i++) {
            final LNGVector<Literal> temp = new LNGVector<>();
            temp.growTo(rhs + 1);
            seqAuxiliary.push(temp);
        }
        for (int i = 1; i <= n; ++i) {
            for (int j = 1; j <= rhs; ++j) {
                seqAuxiliary.get(i).set(j, f.newPBVariable());
            }
        }
        for (int i = 1; i <= n; i++) {
            final int wi = coeffs.get(i - 1);
            assert wi <= rhs;
            for (int j = 1; j <= rhs; j++) {
                if (i >= 2 && i <= n && j <= rhs) {
                    result.addClause(seqAuxiliary.get(i - 1).get(j).negate(f), seqAuxiliary.get(i).get(j));
                }
                if (i <= n && j <= wi) {
                    result.addClause(lits.get(i - 1).negate(f), seqAuxiliary.get(i).get(j));
                }
                if (i >= 2 && i <= n && j <= rhs - wi) {
                    result.addClause(seqAuxiliary.get(i - 1).get(j).negate(f), lits.get(i - 1).negate(f),
                            seqAuxiliary.get(i).get(j + wi));
                }
            }
            if (i >= 2) {
                result.addClause(seqAuxiliary.get(i - 1).get(rhs + 1 - wi).negate(f), lits.get(i - 1).negate(f));
            }
        }
    }
}
