// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings.pbc;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResult;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;

/**
 * A sequential weight counter for the encoding of pseudo-Boolean constraints in
 * CNF.
 * @version 3.0.0
 * @since 1.0
 */
public final class PbSwc {

    public static void encode(final EncodingResult result, final LngVector<Literal> lits, final LngIntVector coeffs,
                              final int rhs) {
        generateConstraint(result, rhs, lits, coeffs);
    }

    private static void generateConstraint(final EncodingResult result, final int rhs, final LngVector<Literal> lits,
                                           final LngIntVector coeffs) {
        final FormulaFactory f = result.getFactory();
        final int n = lits.size();
        final LngVector<LngVector<Literal>> seqAuxiliary = new LngVector<>(n + 1);
        for (int i = 0; i < n + 1; i++) {
            final LngVector<Literal> temp = new LngVector<>();
            temp.growTo(rhs + 1);
            seqAuxiliary.push(temp);
        }
        for (int i = 1; i <= n; ++i) {
            for (int j = 1; j <= rhs; ++j) {
                seqAuxiliary.get(i).set(j, f.newPbVariable());
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
