// SPDX-License-Identifier: Apache-2.0
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

package org.logicng.cardinalityconstraints;

import org.logicng.collections.LNGVector;
import org.logicng.datastructures.EncodingResult;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

/**
 * Encodes that at most one variable is assigned value true.  Uses the commander encoding due to Klieber &amp; Kwon.
 * @version 3.0.0
 * @since 1.1
 */
public final class CCAMOCommander implements CCAtMostOne {

    private static final CCAMOCommander INSTANCE = new CCAMOCommander();

    private CCAMOCommander() {
        // Singleton pattern
    }

    public static CCAMOCommander get() {
        return INSTANCE;
    }

    @Override
    public void build(final EncodingResult result, final CCConfig config, final Variable... vars) {
        result.reset();
        final LNGVector<Literal> currentLiterals = new LNGVector<>();
        for (final Variable var : vars) {
            currentLiterals.push(var);
        }
        final int groupSize = config.commanderGroupSize;
        encodeRecursive(result, groupSize, currentLiterals);
    }

    private static void encodeRecursive(final EncodingResult result, final int groupSize, final LNGVector<Literal> currentLiterals) {
        final FormulaFactory f = result.factory();
        boolean isExactlyOne = false;
        while (currentLiterals.size() > groupSize) {
            final LNGVector<Literal> literals = new LNGVector<>();
            final LNGVector<Literal> nextLiterals = new LNGVector<>();
            for (int i = 0; i < currentLiterals.size(); i++) {
                literals.push(currentLiterals.get(i));
                if (i % groupSize == groupSize - 1 || i == currentLiterals.size() - 1) {
                    CCAtMostOne.encodeNaive(result, literals);
                    literals.push(result.newVariable());
                    nextLiterals.push(literals.back().negate(f));
                    if (isExactlyOne && literals.size() > 0) {
                        result.addClause(literals);
                    }
                    for (int j = 0; j < literals.size() - 1; j++) {
                        result.addClause(literals.back().negate(f), literals.get(j).negate(f));
                    }
                    literals.clear();
                }
            }
            currentLiterals.replaceInplace(nextLiterals);
            isExactlyOne = true;
        }
        CCAtMostOne.encodeNaive(result, currentLiterals);
        if (isExactlyOne && currentLiterals.size() > 0) {
            result.addClause(currentLiterals);
        }
    }
}
