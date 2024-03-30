// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * MiniSat -- Copyright (c) 2003-2006, Niklas Een, Niklas Sorensson
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * ---
 * Glucose -- Copyright (c) 2009-2014, Gilles Audemard, Laurent Simon
 * CRIL - Univ. Artois, France
 * LRI  - Univ. Paris Sud, France (2009-2013)
 * Labri - Univ. Bordeaux, France
 * Syrup (Glucose Parallel) -- Copyright (c) 2013-2014, Gilles Audemard, Laurent Simon
 * CRIL - Univ. Artois, France
 * Labri - Univ. Bordeaux, France
 * Glucose sources are based on MiniSat (see below MiniSat copyrights). Permissions and copyrights of
 * Glucose (sources until 2013, Glucose 3.0, single core) are exactly the same as Minisat on which it
 * is based on. (see below).
 * Glucose-Syrup sources are based on another copyright. Permissions and copyrights for the parallel
 * version of Glucose-Syrup (the "Software") are granted, free of charge, to deal with the Software
 * without restriction, including the rights to use, copy, modify, merge, publish, distribute,
 * sublicence, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * - The above and below copyrights notices and this permission notice shall be included in all
 * copies or substantial portions of the Software;
 * - The parallel version of Glucose (all files modified since Glucose 3.0 releases, 2013) cannot
 * be used in any competitive event (sat competitions/evaluations) without the express permission of
 * the authors (Gilles Audemard / Laurent Simon). This is also the case for any competitive event
 * using Glucose Parallel as an embedded SAT engine (single core or not).
 * ---
 * MiniCARD Copyright (c) 2012, Mark Liffiton, Jordyn Maglalang
 * MiniCARD is based on MiniSAT, whose original copyright notice is maintained below,
 * and it is released under the same license.
 */

package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.handlers.Handler.start;
import static com.booleworks.logicng.handlers.SATHandler.finishSolving;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.datastructures.MSClause;
import com.booleworks.logicng.solvers.datastructures.MSVariable;
import com.booleworks.logicng.solvers.datastructures.MSWatcher;

/**
 * A solver based on MiniSAT 2.2.0, including Glucose 4.0 and MiniCard features.
 * <p>
 * If the incremental mode is activated, this solver allows to save and load the solver state in an efficient manner.
 * Therefore, clause deletion and simplifications are deactivated in this mode.  This mode is most efficient on small
 * to mid-size industrial formulas (up to 50,000 variables, 100,000 clauses).  Whenever you have lots of small formulas
 * to solve or need the ability to add and delete formulas from the solver, we recommend to consider this mode.
 * @version 2.1.0
 * @since 1.0
 */
public class MiniSat2Solver extends MiniSatStyleSolver {

    protected static final int LIT_ERROR = -2;

    /**
     * the ratio of clauses which will be removed
     */
    protected static final int RATIO_REMOVE_CLAUSES = 2;

    /**
     * the lower bound for blocking restarts
     */
    protected static final int LB_BLOCKING_RESTART = 10000;

    /**
     * The current state ID.
     */
    protected int stateId = 0;

    /**
     * The stack of currently valid states IDs.
     */
    protected LNGIntVector validStates = new LNGIntVector();

    /**
     * Constructs a new MiniSAT 2 solver with the default values for solver configuration.  By default, incremental mode
     * is activated.
     */
    public MiniSat2Solver() {
        this(MiniSatConfig.builder().build());
    }

    /**
     * Constructs a new MiniSAT 2 solver with a given solver configuration.
     * @param config the solver configuration
     */
    public MiniSat2Solver(final MiniSatConfig config) {
        super(config);
    }

    @Override
    public int newVar(final boolean sign, final boolean dvar) {
        final int v = nVars();
        final MSVariable newVar = new MSVariable(sign);
        vars.push(newVar);
        watches.push(new LNGVector<>());
        watches.push(new LNGVector<>());
        seen.push(false);
        watchesBin.push(new LNGVector<>());
        watchesBin.push(new LNGVector<>());
        permDiff.push(0);
        newVar.setDecision(dvar);
        insertVarOrder(v);
        return v;
    }

    @Override
    public boolean addClause(final LNGIntVector ps, final Proposition proposition) {
        assert decisionLevel() == 0;
        int p;
        int i;
        int j;
        if (config.proofGeneration) {
            final LNGIntVector vec = new LNGIntVector(ps.size());
            for (i = 0; i < ps.size(); i++) {
                vec.push((var(ps.get(i)) + 1) * (-2 * (sign(ps.get(i)) ? 1 : 0) + 1));
            }
            pgOriginalClauses.push(new ProofInformation(vec, proposition));
        }
        if (!ok) {
            return false;
        }
        ps.sort();

        boolean flag = false;
        LNGIntVector oc = null;
        if (config.proofGeneration) {
            oc = new LNGIntVector();
            for (i = 0, p = LIT_UNDEF; i < ps.size(); i++) {
                oc.push(ps.get(i));
                if (value(ps.get(i)) == Tristate.TRUE || ps.get(i) == not(p) || value(ps.get(i)) == Tristate.FALSE) {
                    flag = true;
                }
            }
        }

        for (i = 0, j = 0, p = LIT_UNDEF; i < ps.size(); i++) {
            if (value(ps.get(i)) == Tristate.TRUE || ps.get(i) == not(p)) {
                return true;
            } else if (value(ps.get(i)) != Tristate.FALSE && ps.get(i) != p) {
                p = ps.get(i);
                ps.set(j++, p);
            }
        }
        ps.removeElements(i - j);

        if (flag) {
            LNGIntVector vec = new LNGIntVector(ps.size() + 1);
            vec.push(1);
            for (i = 0; i < ps.size(); i++) {
                vec.push((var(ps.get(i)) + 1) * (-2 * (sign(ps.get(i)) ? 1 : 0) + 1));
            }
            pgProof.push(vec);

            vec = new LNGIntVector(oc.size() + 1);
            vec.push(-1);
            for (i = 0; i < oc.size(); i++) {
                vec.push((var(oc.get(i)) + 1) * (-2 * (sign(oc.get(i)) ? 1 : 0) + 1));
            }
            pgProof.push(vec);
        }

        if (ps.empty()) {
            ok = false;
            if (config.proofGeneration) {
                pgProof.push(new LNGIntVector(1, 0));
            }
            return false;
        } else if (ps.size() == 1) {
            uncheckedEnqueue(ps.get(0), null);
            ok = propagate() == null;
            unitClauses.push(ps.get(0));
            if (!ok && config.proofGeneration) {
                pgProof.push(new LNGIntVector(1, 0));
            }
            return ok;
        } else {
            final MSClause c = new MSClause(ps, -1);
            clauses.push(c);
            attachClause(c);
        }
        return true;
    }

    @Override
    public Tristate solve(final SATHandler handler) {
        this.handler = handler;
        start(handler);
        model.clear();
        conflict.clear();
        if (!ok) {
            return Tristate.FALSE;
        }
        Tristate status = Tristate.UNDEF;
        while (status == Tristate.UNDEF && !canceledByHandler) {
            status = search();
        }

        if (config.proofGeneration && assumptions.empty()) {
            if (status == Tristate.FALSE) {
                pgProof.push(new LNGIntVector(1, 0));
            }
        }

        if (status == Tristate.TRUE) {
            model = new LNGBooleanVector(vars.size());
            for (final MSVariable v : vars) {
                model.push(v.assignment() == Tristate.TRUE);
            }
        } else if (status == Tristate.FALSE && conflict.empty()) {
            ok = false;
        }
        finishSolving(handler);
        cancelUntil(0);
        this.handler = null;
        canceledByHandler = false;
        return status;
    }

    /**
     * Saves and returns the solver state expressed as an integer array which stores the length of the internal data
     * structures.  The array has the following layout:
     * <p>
     * {@code | current solver state | #vars | #clauses | #unit clauses | #pg original | #pg proof}
     * @return the current solver state
     */
    @Override
    public SolverState saveState() {
        final int[] state;
        state = new int[6];
        state[0] = ok ? 1 : 0;
        state[1] = vars.size();
        state[2] = clauses.size();
        state[3] = unitClauses.size();
        if (config.proofGeneration) {
            state[4] = pgOriginalClauses.size();
            state[5] = pgProof.size();
        }
        final int id = stateId++;
        validStates.push(id);
        return new SolverState(id, state);
    }

    @Override
    public void loadState(final SolverState solverState) {
        int index = -1;
        for (int i = validStates.size() - 1; i >= 0 && index == -1; i--) {
            if (validStates.get(i) == solverState.getId()) {
                index = i;
            }
        }
        if (index == -1) {
            throw new IllegalArgumentException("The given solver state is not valid anymore.");
        }
        final int[] state = solverState.getState();
        validStates.shrinkTo(index + 1);
        completeBacktrack();
        ok = state[0] == 1;
        final int newVarsSize = Math.min(state[1], vars.size());
        for (int i = vars.size() - 1; i >= newVarsSize; i--) {
            orderHeap.remove(name2idx.remove(idx2name.remove(i)));
        }
        vars.shrinkTo(newVarsSize);
        permDiff.shrinkTo(newVarsSize);
        final int newClausesSize = Math.min(state[2], clauses.size());
        for (int i = clauses.size() - 1; i >= newClausesSize; i--) {
            simpleRemoveClause(clauses.get(i));
        }
        clauses.shrinkTo(newClausesSize);
        int newLearntsLength = 0;
        for (int i = 0; i < learnts.size(); i++) {
            final MSClause learnt = learnts.get(i);
            if (learnt.getLearntOnState() <= solverState.getId()) {
                learnts.set(newLearntsLength++, learnt);
            } else {
                simpleRemoveClause(learnt);
            }
        }
        learnts.shrinkTo(newLearntsLength);
        watches.shrinkTo(newVarsSize * 2);
        watchesBin.shrinkTo(newVarsSize * 2);
        unitClauses.shrinkTo(state[3]);
        for (int i = 0; ok && i < unitClauses.size(); i++) {
            uncheckedEnqueue(unitClauses.get(i), null);
            ok = propagate() == null;
        }
        if (config.proofGeneration) {
            final int newPgOriginalSize = Math.min(state[4], pgOriginalClauses.size());
            pgOriginalClauses.shrinkTo(newPgOriginalSize);
            final int newPgProofSize = Math.min(state[5], pgProof.size());
            pgProof.shrinkTo(newPgProofSize);
        }
    }

    @Override
    protected void uncheckedEnqueue(final int lit, final MSClause reason) {
        assert value(lit) == Tristate.UNDEF;
        final MSVariable var = v(lit);
        var.assign(Tristate.fromBool(!sign(lit)));
        var.setReason(reason);
        var.setLevel(decisionLevel());
        trail.push(lit);
    }

    @Override
    protected void attachClause(final MSClause c) {
        if (c.isAtMost()) {
            for (int i = 0; i < c.atMostWatchers(); i++) {
                final int l = c.get(i);
                watches.get(l).push(new MSWatcher(c, LIT_UNDEF));
            }
            clausesLiterals += c.size();
        } else {
            assert c.size() > 1;
            if (c.size() == 2) {
                watchesBin.get(not(c.get(0))).push(new MSWatcher(c, c.get(1)));
                watchesBin.get(not(c.get(1))).push(new MSWatcher(c, c.get(0)));
            } else {
                watches.get(not(c.get(0))).push(new MSWatcher(c, c.get(1)));
                watches.get(not(c.get(1))).push(new MSWatcher(c, c.get(0)));
            }
            if (c.learnt()) {
                learntsLiterals += c.size();
            } else {
                clausesLiterals += c.size();
            }
        }
    }

    @Override
    protected void detachClause(final MSClause c) {
        assert c.size() > 1 && !c.isAtMost();
        simpleRemoveClause(c);
        if (c.learnt()) {
            learntsLiterals -= c.size();
        } else {
            clausesLiterals -= c.size();
        }
    }

    @Override
    protected void removeClause(final MSClause c) {
        if (c.isAtMost()) {
            detachAtMost(c);
            for (int i = 0; i < c.atMostWatchers(); i++) {
                if (value(c.get(i)) == Tristate.FALSE && v(c.get(i)).reason() != null && v(c.get(i)).reason() == c) {
                    v(c.get(i)).setReason(null);
                }
            }
        } else {
            if (config.proofGeneration) {
                final LNGIntVector vec = new LNGIntVector(c.size());
                vec.push(-1);
                for (int i = 0; i < c.size(); i++) {
                    vec.push((var(c.get(i)) + 1) * (-2 * (sign(c.get(i)) ? 1 : 0) + 1));
                }
                pgProof.push(vec);
            }

            detachClause(c);
            if (locked(c)) {
                v(c.get(0)).setReason(null);
            }
        }
    }

    @Override
    protected MSClause propagate() {
        MSClause confl = null;
        while (qhead < trail.size()) {
            final int p = trail.get(qhead++);
            final LNGVector<MSWatcher> ws = watches.get(p);
            int iInd = 0;
            int jInd = 0;
            final LNGVector<MSWatcher> wbin = watchesBin.get(p);
            for (int k = 0; k < wbin.size(); k++) {
                final int imp = wbin.get(k).blocker();
                if (value(imp) == Tristate.FALSE) {
                    return wbin.get(k).clause();
                }
                if (value(imp) == Tristate.UNDEF) {
                    uncheckedEnqueue(imp, wbin.get(k).clause());
                }
            }
            while (iInd < ws.size()) {
                final MSWatcher i = ws.get(iInd);
                final int blocker = i.blocker();
                if (blocker != LIT_UNDEF && value(blocker) == Tristate.TRUE) {
                    ws.set(jInd++, i);
                    iInd++;
                    continue;
                }
                final MSClause c = i.clause();

                if (c.isAtMost()) {
                    final int newWatch = findNewWatchForAtMostClause(c, p);
                    if (newWatch == LIT_UNDEF) {
                        for (int k = 0; k < c.atMostWatchers(); k++) {
                            if (c.get(k) != p && value(c.get(k)) != Tristate.FALSE) {
                                assert value(c.get(k)) == Tristate.UNDEF || value(c.get(k)) == Tristate.FALSE;
                                uncheckedEnqueue(not(c.get(k)), c);
                            }
                        }
                        ws.set(jInd++, ws.get(iInd++));
                    } else if (newWatch == LIT_ERROR) {
                        confl = c;
                        qhead = trail.size();
                        while (iInd < ws.size()) {
                            ws.set(jInd++, ws.get(iInd++));
                        }
                    } else if (newWatch == p) {
                        ws.set(jInd++, ws.get(iInd++));
                    } else {
                        iInd++;
                        final MSWatcher w = new MSWatcher(c, LIT_UNDEF);
                        watches.get(newWatch).push(w);
                    }
                } else {
                    final int falseLit = not(p);
                    if (c.get(0) == falseLit) {
                        c.set(0, c.get(1));
                        c.set(1, falseLit);
                    }
                    assert c.get(1) == falseLit;
                    iInd++;
                    final int first = c.get(0);
                    final MSWatcher w = new MSWatcher(c, first);
                    if (first != blocker && value(first) == Tristate.TRUE) {
                        ws.set(jInd++, w);
                        continue;
                    }
                    boolean foundWatch = false;
                    for (int k = 2; k < c.size() && !foundWatch; k++) {
                        if (value(c.get(k)) != Tristate.FALSE) {
                            c.set(1, c.get(k));
                            c.set(k, falseLit);
                            watches.get(not(c.get(1))).push(w);
                            foundWatch = true;
                        }
                    }
                    if (!foundWatch) {
                        ws.set(jInd++, w);
                        if (value(first) == Tristate.FALSE) {
                            confl = c;
                            qhead = trail.size();
                            while (iInd < ws.size()) {
                                ws.set(jInd++, ws.get(iInd++));
                            }
                        } else {
                            uncheckedEnqueue(first, c);
                        }
                    }
                }
            }
            ws.removeElements(iInd - jInd);
        }
        return confl;
    }

    @Override
    protected boolean litRedundant(final int p, final int abstractLevels, final LNGIntVector analyzeToClear) {
        final LNGIntVector analyzeStack = new LNGIntVector();
        analyzeStack.push(p);
        final int top = analyzeToClear.size();
        while (analyzeStack.size() > 0) {
            assert v(analyzeStack.back()).reason() != null;
            final MSClause c = v(analyzeStack.back()).reason();
            analyzeStack.pop();
            if (c.isAtMost()) {
                for (int i = 0; i < c.size(); i++) {
                    if (value(c.get(i)) != Tristate.TRUE) {
                        continue;
                    }
                    final int q = not(c.get(i));
                    if (!seen.get(var(q)) && v(q).level() > 0) {
                        if (v(q).reason() != null && (abstractLevel(var(q)) & abstractLevels) != 0) {
                            seen.set(var(q), true);
                            analyzeStack.push(q);
                            analyzeToClear.push(q);
                        } else {
                            for (int j = top; j < analyzeToClear.size(); j++) {
                                seen.set(var(analyzeToClear.get(j)), false);
                            }
                            analyzeToClear.removeElements(analyzeToClear.size() - top);
                            return false;
                        }
                    }
                }
            } else {
                if (c.size() == 2 && value(c.get(0)) == Tristate.FALSE) {
                    assert value(c.get(1)) == Tristate.TRUE;
                    final int tmp = c.get(0);
                    c.set(0, c.get(1));
                    c.set(1, tmp);
                }
                for (int i = 1; i < c.size(); i++) {
                    final int q = c.get(i);
                    if (!seen.get(var(q)) && v(q).level() > 0) {
                        if (v(q).reason() != null && (abstractLevel(var(q)) & abstractLevels) != 0) {
                            seen.set(var(q), true);
                            analyzeStack.push(q);
                            analyzeToClear.push(q);
                        } else {
                            for (int j = top; j < analyzeToClear.size(); j++) {
                                seen.set(var(analyzeToClear.get(j)), false);
                            }
                            analyzeToClear.removeElements(analyzeToClear.size() - top);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected void analyzeFinal(final int p, final LNGIntVector outConflict) {
        outConflict.clear();
        outConflict.push(p);
        if (decisionLevel() == 0) {
            return;
        }
        seen.set(var(p), true);
        int x;
        MSVariable v;
        for (int i = trail.size() - 1; i >= trailLim.get(0); i--) {
            x = var(trail.get(i));
            if (seen.get(x)) {
                v = vars.get(x);
                if (v.reason() == null) {
                    assert v.level() > 0;
                    outConflict.push(not(trail.get(i)));
                } else {
                    final MSClause c = v.reason();
                    if (!c.isAtMost()) {
                        for (int j = c.size() == 2 ? 0 : 1; j < c.size(); j++) {
                            if (v(c.get(j)).level() > 0) {
                                seen.set(var(c.get(j)), true);
                            }
                        }
                    } else {
                        for (int j = 0; j < c.size(); j++) {
                            if (value(c.get(j)) == Tristate.TRUE && v(c.get(j)).level() > 0) {
                                seen.set(var(c.get(j)), true);
                            }
                        }
                    }
                }
                seen.set(x, false);
            }
        }
        seen.set(var(p), false);
    }

    @Override
    protected void reduceDB() {
        int i;
        int j;
        learnts.manualSort(MSClause.glucoseComparator);
        if (learnts.get(learnts.size() / RATIO_REMOVE_CLAUSES).lbd() <= 3) {
            nbClausesBeforeReduce += llConfig.specialIncReduceDB;
        }
        if (learnts.back().lbd() <= 5) {
            nbClausesBeforeReduce += llConfig.specialIncReduceDB;
        }
        int limit = learnts.size() / 2;
        for (i = j = 0; i < learnts.size(); i++) {
            final MSClause c = learnts.get(i);
            if (c.lbd() > 2 && c.size() > 2 && c.canBeDel() && !locked(c) && i < limit) {
                removeClause(learnts.get(i));
            } else {
                if (!c.canBeDel()) {
                    limit++;
                }
                c.setCanBeDel(true);
                learnts.set(j++, learnts.get(i));
            }
        }
        learnts.removeElements(i - j);
    }

    /**
     * Adds an at-most k constraint.
     * @param ps  the literals of the constraint
     * @param rhs the right-hand side of the constraint
     */
    public void addAtMost(final LNGIntVector ps, final int rhs) {
        int k = rhs;
        assert decisionLevel() == 0;
        if (!ok) {
            return;
        }
        ps.sort();
        int p;
        int i;
        int j;
        for (i = j = 0, p = LIT_UNDEF; i < ps.size(); i++) {
            if (value(ps.get(i)) == Tristate.TRUE) {
                k--;
            } else if (ps.get(i) == not(p)) {
                p = ps.get(i);
                j--;
                k--;
            } else if (value(ps.get(i)) != Tristate.FALSE) {
                p = ps.get(i);
                ps.set(j++, p);
            }
        }
        ps.removeElements(i - j);
        if (k >= ps.size()) {
            return;
        }
        if (k < 0) {
            ok = false;
            return;
        }
        if (k == 0) {
            for (i = 0; i < ps.size(); i++) {
                uncheckedEnqueue(not(ps.get(i)), null);
                unitClauses.push(not(ps.get(i)));
            }
            ok = propagate() == null;
            return;
        }
        final MSClause cr = new MSClause(ps, -1, true);
        cr.setAtMostWatchers(ps.size() - k + 1);
        clauses.push(cr);
        attachClause(cr);
    }

    /**
     * Detaches a given at-most clause.
     * @param c the at-most clause.
     */
    protected void detachAtMost(final MSClause c) {
        for (int i = 0; i < c.atMostWatchers(); i++) {
            watches.get(c.get(i)).remove(new MSWatcher(c, c.get(i)));
        }
        clausesLiterals -= c.size();
    }

    /**
     * Computes the LBD for a given vector of literals.
     * @param lits the vector of literals
     * @return the LBD
     */
    protected long computeLBD(final LNGIntVector lits) {
        long nbLevels = 0;
        myflag++;
        for (int i = 0; i < lits.size(); i++) {
            final int l = v(lits.get(i)).level();
            if (permDiff.get(l) != myflag) {
                permDiff.set(l, myflag);
                nbLevels++;
            }
        }
        if (!llConfig.reduceOnSize) {
            return nbLevels;
        }
        if (lits.size() < llConfig.reduceOnSizeSize) {
            return lits.size();
        }
        return lits.size() + nbLevels;
    }

    /**
     * Computes the LBD for a given clause
     * @param c the clause
     * @return the LBD
     */
    protected long computeLBD(final MSClause c) {
        return computeLBD(c.getData());
    }

    /**
     * A special clause minimization by binary resolution for small clauses.
     * @param outLearnt the vector where the new learnt 1-UIP clause is stored
     */
    protected void minimisationWithBinaryResolution(final LNGIntVector outLearnt) {
        final long lbd = computeLBD(outLearnt);
        int p = not(outLearnt.get(0));
        if (lbd <= llConfig.lbLBDMinimizingClause) {
            myflag++;
            for (int i = 1; i < outLearnt.size(); i++) {
                permDiff.set(var(outLearnt.get(i)), myflag);
            }
            int nb = 0;
            for (final MSWatcher wbin : watchesBin.get(p)) {
                final int imp = wbin.blocker();
                if (permDiff.get(var(imp)) == myflag && value(imp) == Tristate.TRUE) {
                    nb++;
                    permDiff.set(var(imp), myflag - 1);
                }
            }
            int l = outLearnt.size() - 1;
            if (nb > 0) {
                for (int i = 1; i < outLearnt.size() - nb; i++) {
                    if (permDiff.get(var(outLearnt.get(i))) != myflag) {
                        p = outLearnt.get(l);
                        outLearnt.set(l, outLearnt.get(i));
                        outLearnt.set(i, p);
                        l--;
                        i--;
                    }
                }
                outLearnt.removeElements(nb);
            }
        }
    }

    /**
     * The main search procedure of the CDCL algorithm.
     * @return a {@link Tristate} representing the result.  {@code FALSE} if the formula is UNSAT, {@code TRUE} if the
     * formula is SAT, and {@code UNDEF} if the state is not known yet (restart) or the handler canceled the computation
     */
    protected Tristate search() {
        if (!ok) {
            return Tristate.FALSE;
        }
        final LNGIntVector learntClause = new LNGIntVector();
        final LNGIntVector selectors = new LNGIntVector();
        selectionOrderIdx = 0;
        while (true) {
            final MSClause confl = propagate();
            if (confl != null) {
                if (handler != null && !handler.detectedConflict()) {
                    canceledByHandler = true;
                    return Tristate.UNDEF;
                }
                conflicts++;
                conflictsRestarts++;
                if (conflicts % 5000 == 0 && varDecay < llConfig.maxVarDecay) {
                    varDecay += 0.01;
                }
                if (decisionLevel() == 0) {
                    return Tristate.FALSE;
                }
                trailQueue.push(trail.size());
                if (conflictsRestarts > LB_BLOCKING_RESTART && lbdQueue.valid() && trail.size() > llConfig.factorR * trailQueue.avg()) {
                    lbdQueue.fastClear();
                }
                learntClause.clear();
                selectors.clear();
                analyze(confl, learntClause, selectors);
                lbdQueue.push(analyzeLBD);
                sumLBD += analyzeLBD;
                cancelUntil(analyzeBtLevel);
                if (analyzeBtLevel < selectionOrder.size()) {
                    selectionOrderIdx = analyzeBtLevel;
                }

                if (config.proofGeneration) {
                    final LNGIntVector vec = new LNGIntVector(learntClause.size() + 1);
                    vec.push(1);
                    for (int i = 0; i < learntClause.size(); i++) {
                        vec.push((var(learntClause.get(i)) + 1) * (-2 * (sign(learntClause.get(i)) ? 1 : 0) + 1));
                    }
                    pgProof.push(vec);
                }

                if (learntClause.size() == 1) {
                    uncheckedEnqueue(learntClause.get(0), null);
                    unitClauses.push(learntClause.get(0));
                } else {
                    final MSClause cr = new MSClause(learntClause, stateId);
                    cr.setLBD(analyzeLBD);
                    cr.setOneWatched(false);
                    learnts.push(cr);
                    attachClause(cr);
                    claBumpActivity(cr);
                    uncheckedEnqueue(learntClause.get(0), cr);
                }
                varDecayActivity();
                claDecayActivity();
            } else {
                if (lbdQueue.valid() && (lbdQueue.avg() * llConfig.factorK) > (sumLBD / conflictsRestarts)) {
                    lbdQueue.fastClear();
                    cancelUntil(0);
                    return Tristate.UNDEF;
                }
                if (conflicts >= (curRestart * nbClausesBeforeReduce) && learnts.size() > 0) {
                    curRestart = (conflicts / nbClausesBeforeReduce) + 1;
                    reduceDB();
                    nbClausesBeforeReduce += llConfig.incReduceDB;
                }
                int next = LIT_UNDEF;
                while (decisionLevel() < assumptions.size()) {
                    final int p = assumptions.get(decisionLevel());
                    if (value(p) == Tristate.TRUE) {
                        trailLim.push(trail.size());
                    } else if (value(p) == Tristate.FALSE) {
                        analyzeFinal(not(p), conflict);
                        return Tristate.FALSE;
                    } else {
                        next = p;
                        break;
                    }
                }
                if (next == LIT_UNDEF) {
                    next = pickBranchLit();
                    if (next == LIT_UNDEF) {
                        return Tristate.TRUE;
                    }
                }
                trailLim.push(trail.size());
                uncheckedEnqueue(next, null);
            }
        }
    }

    protected int findNewWatchForAtMostClause(final MSClause c, final int p) {
        assert c.isAtMost();
        int newWatch = LIT_ERROR;
        int numFalse = 0;
        int numTrue = 0;
        final int maxTrue = c.size() - c.atMostWatchers() + 1;
        for (int q = 0; q < c.atMostWatchers(); q++) {
            final Tristate val = value(c.get(q));
            if (val == Tristate.UNDEF) {
                continue;
            } else if (val == Tristate.FALSE) {
                numFalse++;
                if (numFalse >= c.atMostWatchers() - 1) {
                    return p;
                }
                continue;
            }
            assert val == Tristate.TRUE;
            numTrue++;
            if (numTrue > maxTrue) {
                return LIT_ERROR;
            }
            if (c.get(q) == p) {
                assert newWatch == LIT_ERROR;
                for (int next = c.atMostWatchers(); next < c.size(); next++) {
                    if (value(c.get(next)) != Tristate.TRUE) {
                        newWatch = c.get(next);
                        c.set(next, c.get(q));
                        c.set(q, newWatch);
                        return newWatch;
                    }
                }
                newWatch = LIT_UNDEF;
            }
        }
        assert newWatch == LIT_UNDEF;
        if (numTrue > 1) {
            return LIT_ERROR;
        } else {
            return LIT_UNDEF;
        }
    }

    /**
     * Analyzes a given conflict clause wrt. the current solver state.  A 1-UIP clause is created during this procedure
     * and the new backtracking level is stored in the solver state.
     * @param conflictClause the conflict clause to start the resolution analysis with
     * @param outLearnt      the vector where the new learnt 1-UIP clause is stored
     * @param selectors      a vector of selector variables
     */
    protected void analyze(final MSClause conflictClause, final LNGIntVector outLearnt,
                           final LNGIntVector selectors) {
        MSClause c = conflictClause;
        int pathC = 0;
        int p = LIT_UNDEF;
        outLearnt.push(-1);
        int index = trail.size() - 1;
        do {
            assert c != null;
            if (c.isAtMost()) {
                for (int j = 0; j < c.size(); j++) {
                    if (value(c.get(j)) != Tristate.TRUE) {
                        continue;
                    }
                    final int q = not(c.get(j));
                    if (!seen.get(var(q)) && v(q).level() > 0) {
                        varBumpActivity(var(q));
                        seen.set(var(q), true);
                        if (v(q).level() >= decisionLevel()) {
                            pathC++;
                        } else {
                            outLearnt.push(q);
                        }
                    }
                }
            } else {
                if (p != LIT_UNDEF && c.size() == 2 && value(c.get(0)) == Tristate.FALSE) {
                    assert value(c.get(1)) == Tristate.TRUE;
                    final int tmp = c.get(0);
                    c.set(0, c.get(1));
                    c.set(1, tmp);
                }
                if (c.learnt()) {
                    claBumpActivity(c);
                } else {
                    if (!c.seen()) {
                        c.setSeen(true);
                    }
                }
                if (c.learnt() && c.lbd() > 2) {
                    final long nblevels = computeLBD(c);
                    if (nblevels + 1 < c.lbd()) {
                        if (c.lbd() <= llConfig.lbLBDFrozenClause) {
                            c.setCanBeDel(false);
                        }
                        c.setLBD(nblevels);
                    }
                }
                for (int j = (p == LIT_UNDEF) ? 0 : 1; j < c.size(); j++) {
                    final int q = c.get(j);
                    if (!seen.get(var(q)) && v(q).level() != 0) {
                        varBumpActivity(var(q));
                        seen.set(var(q), true);
                        if (v(q).level() >= decisionLevel()) {
                            pathC++;
                            if ((v(q).reason() != null) && v(q).reason().learnt()) {
                                lastDecisionLevel.push(q);
                            }
                        } else {
                            outLearnt.push(q);
                        }
                    }
                }
            }
            while (!seen.get(var(trail.get(index--)))) {
            }
            p = trail.get(index + 1);
            c = v(p).reason();
            seen.set(var(p), false);
            pathC--;
        } while (pathC > 0);
        outLearnt.set(0, not(p));
        simplifyClause(outLearnt, selectors);
    }

    /**
     * Minimizes a given learnt clause depending on the minimization method of the solver configuration.
     * @param outLearnt the learnt clause which should be minimized
     * @param selectors a vector of selector variables
     */
    protected void simplifyClause(final LNGIntVector outLearnt, final LNGIntVector selectors) {
        int i;
        int j;
        for (i = 0; i < selectors.size(); i++) {
            outLearnt.push(selectors.get(i));
        }
        final LNGIntVector analyzeToClear = new LNGIntVector(outLearnt);
        if (config.clauseMinimization == MiniSatConfig.ClauseMinimization.DEEP) {
            int abstractLevel = 0;
            for (i = 1; i < outLearnt.size(); i++) {
                abstractLevel |= abstractLevel(var(outLearnt.get(i)));
            }
            for (i = j = 1; i < outLearnt.size(); i++) {
                if (v(outLearnt.get(i)).reason() == null || !litRedundant(outLearnt.get(i), abstractLevel, analyzeToClear)) {
                    outLearnt.set(j++, outLearnt.get(i));
                }
            }
        } else if (config.clauseMinimization == MiniSatConfig.ClauseMinimization.BASIC) {
            for (i = j = 1; i < outLearnt.size(); i++) {
                final MSClause c = v(outLearnt.get(i)).reason();
                if (c == null) {
                    outLearnt.set(j++, outLearnt.get(i));
                } else {
                    for (int k = c.size() == 2 ? 0 : 1; k < c.size(); k++) {
                        if (!seen.get(var(c.get(k))) && v(c.get(k)).level() > 0) {
                            outLearnt.set(j++, outLearnt.get(i));
                            break;
                        }
                    }
                }
            }
        } else {
            i = j = outLearnt.size();
        }
        outLearnt.removeElements(i - j);
        if (outLearnt.size() <= llConfig.lbSizeMinimizingClause) {
            minimisationWithBinaryResolution(outLearnt);
        }
        analyzeBtLevel = 0;
        if (outLearnt.size() > 1) {
            int max = 1;
            for (int k = 2; k < outLearnt.size(); k++) {
                if (v(outLearnt.get(k)).level() > v(outLearnt.get(max)).level()) {
                    max = k;
                }
            }
            final int p = outLearnt.get(max);
            outLearnt.set(max, outLearnt.get(1));
            outLearnt.set(1, p);
            analyzeBtLevel = v(p).level();
        }
        analyzeLBD = computeLBD(outLearnt);
        for (int k = 0; k < lastDecisionLevel.size(); k++) {
            if ((v(lastDecisionLevel.get(k)).reason()).lbd() < analyzeLBD) {
                varBumpActivity(var(lastDecisionLevel.get(k)));
            }
        }
        lastDecisionLevel.clear();
        for (int m = 0; m < selectors.size(); m++) {
            seen.set(var(selectors.get(m)), false);
        }
        for (int l = 0; l < analyzeToClear.size(); l++) {
            seen.set(var(analyzeToClear.get(l)), false);
        }
    }

    /**
     * Performs an unconditional backtrack to level zero.
     */
    protected void completeBacktrack() {
        for (int v = 0; v < vars.size(); v++) {
            final MSVariable var = vars.get(v);
            var.assign(Tristate.UNDEF);
            var.setReason(null);
            if (!orderHeap.inHeap(v) && var.decision()) {
                orderHeap.insert(v);
            }
        }
        trail.clear();
        trailLim.clear();
        qhead = 0;
    }

    /**
     * Performs a simple removal of clauses used during the loading of an older state.
     * @param c the clause to remove
     */
    protected void simpleRemoveClause(final MSClause c) {
        if (c.isAtMost()) {
            for (int i = 0; i < c.atMostWatchers(); i++) {
                watches.get(c.get(i)).remove(new MSWatcher(c, c.get(i)));
            }
        } else if (c.size() == 2) {
            watchesBin.get(not(c.get(0))).remove(new MSWatcher(c, c.get(1)));
            watchesBin.get(not(c.get(1))).remove(new MSWatcher(c, c.get(0)));
        } else {
            watches.get(not(c.get(0))).remove(new MSWatcher(c, c.get(1)));
            watches.get(not(c.get(1))).remove(new MSWatcher(c, c.get(0)));
        }
    }

    @Override
    protected boolean isRotatable(final int lit) {
        if (!super.isRotatable(lit)) {
            return false;
        }
        for (final MSWatcher watcher : watchesBin.get(not(lit))) {
            if (isUnit(lit, watcher.clause())) {
                return false;
            }
        }
        return true;
    }
}
