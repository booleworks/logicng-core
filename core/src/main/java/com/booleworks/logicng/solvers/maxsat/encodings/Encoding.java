// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat.encodings;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

/**
 * Super-class for the different encodings.
 * @version 1.3
 * @since 1.0
 */
public abstract class Encoding {

    protected final LngIntVector clause;
    boolean hasEncoding;

    /**
     * Constructor.
     */
    Encoding() {
        clause = new LngIntVector();
    }

    /**
     * Adds a unit clause to the given SAT solver.
     * @param s the sat solver
     * @param a the unit literal
     */
    void addUnitClause(final LngCoreSolver s, final int a) {
        addUnitClause(s, a, LngCoreSolver.LIT_UNDEF);
    }

    /**
     * Adds a unit clause to the given SAT solver.
     * @param s        the sat solver
     * @param a        the unit literal
     * @param blocking the blocking literal
     */
    private void addUnitClause(final LngCoreSolver s, final int a, final int blocking) {
        assert clause.isEmpty();
        assert a != LngCoreSolver.LIT_UNDEF;
        assert LngCoreSolver.var(a) < s.nVars();
        clause.push(a);
        if (blocking != LngCoreSolver.LIT_UNDEF) {
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
    void addBinaryClause(final LngCoreSolver s, final int a, final int b) {
        addBinaryClause(s, a, b, LngCoreSolver.LIT_UNDEF);
    }

    /**
     * Adds a binary clause to the given SAT solver.
     * @param s        the sat solver
     * @param a        the first literal
     * @param b        the second literal
     * @param blocking the blocking literal
     */
    void addBinaryClause(final LngCoreSolver s, final int a, final int b, final int blocking) {
        assert clause.isEmpty();
        assert a != LngCoreSolver.LIT_UNDEF && b != LngCoreSolver.LIT_UNDEF;
        assert LngCoreSolver.var(a) < s.nVars() && LngCoreSolver.var(b) < s.nVars();
        clause.push(a);
        clause.push(b);
        if (blocking != LngCoreSolver.LIT_UNDEF) {
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
    void addTernaryClause(final LngCoreSolver s, final int a, final int b, final int c) {
        addTernaryClause(s, a, b, c, LngCoreSolver.LIT_UNDEF);
    }

    /**
     * Adds a ternary clause to the given SAT solver.
     * @param s        the sat solver
     * @param a        the first literal
     * @param b        the second literal
     * @param c        the third literal
     * @param blocking the blocking literal
     */
    void addTernaryClause(final LngCoreSolver s, final int a, final int b, final int c, final int blocking) {
        assert clause.isEmpty();
        assert a != LngCoreSolver.LIT_UNDEF && b != LngCoreSolver.LIT_UNDEF && c != LngCoreSolver.LIT_UNDEF;
        assert LngCoreSolver.var(a) < s.nVars() && LngCoreSolver.var(b) < s.nVars() && LngCoreSolver.var(c) < s.nVars();
        clause.push(a);
        clause.push(b);
        clause.push(c);
        if (blocking != LngCoreSolver.LIT_UNDEF) {
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
    void addQuaternaryClause(final LngCoreSolver s, final int a, final int b, final int c, final int d) {
        addQuaternaryClause(s, a, b, c, d, LngCoreSolver.LIT_UNDEF);
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
    private void addQuaternaryClause(final LngCoreSolver s, final int a, final int b, final int c, final int d,
                                     final int blocking) {
        assert clause.isEmpty();
        assert a != LngCoreSolver.LIT_UNDEF && b != LngCoreSolver.LIT_UNDEF && c != LngCoreSolver.LIT_UNDEF &&
                d != LngCoreSolver.LIT_UNDEF;
        assert LngCoreSolver.var(a) < s.nVars() && LngCoreSolver.var(b) < s.nVars() &&
                LngCoreSolver.var(c) < s.nVars() && LngCoreSolver.var(d) < s.nVars();
        clause.push(a);
        clause.push(b);
        clause.push(c);
        clause.push(d);
        if (blocking != LngCoreSolver.LIT_UNDEF) {
            clause.push(blocking);
        }
        s.addClause(clause, null);
        clause.clear();
    }
}
