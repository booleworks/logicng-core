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

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.solvers.sat.MiniSatStyleSolver;

/**
 * Super-class for the different encodings.
 * @version 1.3
 * @since 1.0
 */
public abstract class Encoding {

    protected final LNGIntVector clause;
    boolean hasEncoding;

    /**
     * Constructor.
     */
    Encoding() {
        clause = new LNGIntVector();
    }

    /**
     * Adds a unit clause to the given SAT solver.
     * @param s the sat solver
     * @param a the unit literal
     */
    void addUnitClause(final MiniSatStyleSolver s, final int a) {
        addUnitClause(s, a, MiniSatStyleSolver.LIT_UNDEF);
    }

    /**
     * Adds a unit clause to the given SAT solver.
     * @param s        the sat solver
     * @param a        the unit literal
     * @param blocking the blocking literal
     */
    private void addUnitClause(final MiniSatStyleSolver s, final int a, final int blocking) {
        assert clause.size() == 0;
        assert a != MiniSatStyleSolver.LIT_UNDEF;
        assert MiniSatStyleSolver.var(a) < s.nVars();
        clause.push(a);
        if (blocking != MiniSatStyleSolver.LIT_UNDEF) {
            clause.push(blocking);
        }
        s.addClause(clause, null);
        clause.clear();
    }

    /**
     * Adds a binary clause to the given SAT solver.
     * @param s the sat solver
     * @param a the first literal
     * @param b the second literal
     */
    void addBinaryClause(final MiniSatStyleSolver s, final int a, final int b) {
        addBinaryClause(s, a, b, MiniSatStyleSolver.LIT_UNDEF);
    }

    /**
     * Adds a binary clause to the given SAT solver.
     * @param s        the sat solver
     * @param a        the first literal
     * @param b        the second literal
     * @param blocking the blocking literal
     */
    void addBinaryClause(final MiniSatStyleSolver s, final int a, final int b, final int blocking) {
        assert clause.size() == 0;
        assert a != MiniSatStyleSolver.LIT_UNDEF && b != MiniSatStyleSolver.LIT_UNDEF;
        assert MiniSatStyleSolver.var(a) < s.nVars() && MiniSatStyleSolver.var(b) < s.nVars();
        clause.push(a);
        clause.push(b);
        if (blocking != MiniSatStyleSolver.LIT_UNDEF) {
            clause.push(blocking);
        }
        s.addClause(clause, null);
        clause.clear();
    }

    /**
     * Adds a ternary clause to the given SAT solver.
     * @param s the sat solver
     * @param a the first literal
     * @param b the second literal
     * @param c the third literal
     */
    void addTernaryClause(final MiniSatStyleSolver s, final int a, final int b, final int c) {
        addTernaryClause(s, a, b, c, MiniSatStyleSolver.LIT_UNDEF);
    }

    /**
     * Adds a ternary clause to the given SAT solver.
     * @param s        the sat solver
     * @param a        the first literal
     * @param b        the second literal
     * @param c        the third literal
     * @param blocking the blocking literal
     */
    void addTernaryClause(final MiniSatStyleSolver s, final int a, final int b, final int c, final int blocking) {
        assert clause.size() == 0;
        assert a != MiniSatStyleSolver.LIT_UNDEF && b != MiniSatStyleSolver.LIT_UNDEF &&
                c != MiniSatStyleSolver.LIT_UNDEF;
        assert MiniSatStyleSolver.var(a) < s.nVars() && MiniSatStyleSolver.var(b) < s.nVars() &&
                MiniSatStyleSolver.var(c) < s.nVars();
        clause.push(a);
        clause.push(b);
        clause.push(c);
        if (blocking != MiniSatStyleSolver.LIT_UNDEF) {
            clause.push(blocking);
        }
        s.addClause(clause, null);
        clause.clear();
    }

    /**
     * Adds a quaterary clause to the given SAT solver.
     * @param s the sat solver
     * @param a the first literal
     * @param b the second literal
     * @param c the third literal
     * @param d the fourth literal
     */
    void addQuaternaryClause(final MiniSatStyleSolver s, final int a, final int b, final int c, final int d) {
        addQuaternaryClause(s, a, b, c, d, MiniSatStyleSolver.LIT_UNDEF);
    }

    /**
     * Adds a quaterary clause to the given SAT solver.
     * @param s        the sat solver
     * @param a        the first literal
     * @param b        the second literal
     * @param c        the third literal
     * @param d        the fourth literal
     * @param blocking the blocking literal
     */
    private void addQuaternaryClause(final MiniSatStyleSolver s, final int a, final int b, final int c, final int d,
                                     final int blocking) {
        assert clause.size() == 0;
        assert a != MiniSatStyleSolver.LIT_UNDEF && b != MiniSatStyleSolver.LIT_UNDEF &&
                c != MiniSatStyleSolver.LIT_UNDEF && d != MiniSatStyleSolver.LIT_UNDEF;
        assert MiniSatStyleSolver.var(a) < s.nVars() && MiniSatStyleSolver.var(b) < s.nVars() &&
                MiniSatStyleSolver.var(c) < s.nVars() && MiniSatStyleSolver.var(d) < s.nVars();
        clause.push(a);
        clause.push(b);
        clause.push(c);
        clause.push(d);
        if (blocking != MiniSatStyleSolver.LIT_UNDEF) {
            clause.push(blocking);
        }
        s.addClause(clause, null);
        clause.clear();
    }
}
