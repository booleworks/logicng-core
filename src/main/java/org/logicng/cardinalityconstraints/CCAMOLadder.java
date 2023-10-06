// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * Open-WBO -- Copyright (c) 2013-2015, Ruben Martins, Vasco Manquinho, Ines Lynce
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.logicng.cardinalityconstraints;

import org.logicng.datastructures.EncodingResult;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

/**
 * Encodes that at most one variable is assigned value true.  Uses the Ladder/Regular encoding.
 * @version 3.0.0
 * @since 1.0
 */
public final class CCAMOLadder implements CCAtMostOne {

    private static final CCAMOLadder INSTANCE = new CCAMOLadder();

    private CCAMOLadder() {
        // Singleton pattern
    }

    public static CCAMOLadder get() {
        return INSTANCE;
    }

    @Override
    public void build(final EncodingResult result, final CCConfig config, final Variable... vars) {
        final FormulaFactory f = result.factory();
        result.reset();
        final Variable[] seqAuxiliary = new Variable[vars.length - 1];
        for (int i = 0; i < vars.length - 1; i++) {
            seqAuxiliary[i] = result.newVariable();
        }
        for (int i = 0; i < vars.length; i++) {
            if (i == 0) {
                result.addClause(vars[0].negate(f), seqAuxiliary[0]);
            } else if (i == vars.length - 1) {
                result.addClause(vars[i].negate(f), seqAuxiliary[i - 1].negate(f));
            } else {
                result.addClause(vars[i].negate(f), seqAuxiliary[i]);
                result.addClause(seqAuxiliary[i - 1].negate(f), seqAuxiliary[i]);
                result.addClause(vars[i].negate(f), seqAuxiliary[i - 1].negate(f));
            }
        }
    }
}
