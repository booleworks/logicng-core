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

package com.booleworks.logicng.solvers.maxsat.encodings;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSat;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

/**
 * Encodes that exactly one literal from 'lits' is assigned value true. Uses the
 * Ladder/Regular encoding for translating the AMO constraint into CNF.
 * @version 2.0.0
 * @since 1.0
 */
public class Ladder extends Encoding {

    /**
     * Encodes and adds the AMO constraint to the given solver.
     * @param s    the solver
     * @param lits the literals for the constraint
     */
    public void encode(final LngCoreSolver s, final LngIntVector lits) {
        assert !lits.isEmpty();
        if (lits.size() == 1) {
            addUnitClause(s, lits.get(0));
        } else {
            final LngIntVector seqAuxiliary = new LngIntVector();
            for (int i = 0; i < lits.size() - 1; i++) {
                seqAuxiliary.push(LngCoreSolver.mkLit(s.nVars(), false));
                MaxSat.newSatVariable(s);
            }
            for (int i = 0; i < lits.size(); i++) {
                if (i == 0) {
                    addBinaryClause(s, lits.get(i), LngCoreSolver.not(seqAuxiliary.get(i)));
                    addBinaryClause(s, LngCoreSolver.not(lits.get(i)), seqAuxiliary.get(i));
                } else if (i == lits.size() - 1) {
                    addBinaryClause(s, lits.get(i), seqAuxiliary.get(i - 1));
                    addBinaryClause(s, LngCoreSolver.not(lits.get(i)), LngCoreSolver.not(seqAuxiliary.get(i - 1)));
                } else {
                    addBinaryClause(s, LngCoreSolver.not(seqAuxiliary.get(i - 1)), seqAuxiliary.get(i));
                    addTernaryClause(s, lits.get(i), LngCoreSolver.not(seqAuxiliary.get(i)), seqAuxiliary.get(i - 1));
                    addBinaryClause(s, LngCoreSolver.not(lits.get(i)), seqAuxiliary.get(i));
                    addBinaryClause(s, LngCoreSolver.not(lits.get(i)), LngCoreSolver.not(seqAuxiliary.get(i - 1)));
                }
            }
        }
    }
}
