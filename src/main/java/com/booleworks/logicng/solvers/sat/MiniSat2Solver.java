// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * MiniSat -- Copyright (c) 2003-2006, Niklas Een, Niklas Sorensson Permission
 * is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of
 * the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
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
import com.booleworks.logicng.solvers.datastructures.MSClause;
import com.booleworks.logicng.solvers.datastructures.MSVariable;
import com.booleworks.logicng.solvers.datastructures.MSWatcher;

/**
 * A solver based on MiniSAT 2.2.0. If the incremental mode is deactivated, this
 * version should behave exactly like the C++ version.
 * <p>
 * If the incremental mode is activated, this solver allows to save and load the
 * solver state in an efficient manner. Therefore, clause deletion and
 * simplifications are deactivated in this mode. This mode is most efficient on
 * small to mid-size industrial formulas (up to 50,000 variables, 100,000
 * clauses). Whenever you have lots of small formulas to solve or need the
 * ability to add and delete formulas from the solver, we recommend to consider
 * this mode.
 * @version 2.1.0
 * @since 1.0
 */
public class MiniSat2Solver extends MiniSatStyleSolver {

    protected LNGIntVector unitClauses;

    /**
     * Constructs a new MiniSAT 2 solver with the default values for solver
     * configuration. By default, incremental mode is activated.
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
        initializeMiniSAT();
    }

    /**
     * Initializes the additional parameters.
     */
    protected void initializeMiniSAT() {
        unitClauses = new LNGIntVector();
        learntsizeAdjustConfl = 0;
        learntsizeAdjustCnt = 0;
        learntsizeAdjustStartConfl = 100;
        learntsizeAdjustInc = 1.5;
        maxLearnts = 0;
    }

    @Override
    public int newVar(final boolean sign, final boolean dvar) {
        final int v = vars.size();
        final MSVariable newVar = new MSVariable(sign);
        vars.push(newVar);
        watches.push(new LNGVector<>());
        watches.push(new LNGVector<>());
        seen.push(false);
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
            if (incremental) {
                unitClauses.push(ps.get(0));
            }
            if (!ok && config.proofGeneration) {
                pgProof.push(new LNGIntVector(1, 0));
            }
            return ok;
        } else {
            final MSClause c = new MSClause(ps, false);
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
        learntsizeAdjustConfl = learntsizeAdjustStartConfl;
        learntsizeAdjustCnt = (int) learntsizeAdjustConfl;
        maxLearnts = clauses.size() * learntsizeFactor;
        Tristate status = Tristate.UNDEF;
        int currRestarts = 0;
        while (status == Tristate.UNDEF && !canceledByHandler) {
            final double restBase = luby(restartInc, currRestarts);
            status = search((int) (restBase * restartFirst));
            currRestarts++;
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

    @Override
    public void reset() {
        super.initialize();
        initializeMiniSAT();
    }

    /**
     * Saves and returns the solver state expressed as an integer array which
     * stores the length of the internal data structures. The array has length 5
     * and has the following layout:
     * <p>
     * {@code | current solver state | #vars | #clauses | #learnt clauses | #unit clauses | #pg original | #pg proof}
     * @return the current solver state
     */
    @Override
    public int[] saveState() {
        if (!incremental) {
            throw new IllegalStateException("Cannot save a state when the incremental mode is deactivated");
        }
        final int[] state;
        state = new int[7];
        state[0] = ok ? 1 : 0;
        state[1] = vars.size();
        state[2] = clauses.size();
        state[3] = learnts.size();
        state[4] = unitClauses.size();
        if (config.proofGeneration) {
            state[5] = pgOriginalClauses.size();
            state[6] = pgProof.size();
        }
        return state;
    }

    @Override
    public void loadState(final int[] state) {
        if (!incremental) {
            throw new IllegalStateException("Cannot load a state when the incremental mode is deactivated");
        }
        int i;
        completeBacktrack();
        ok = state[0] == 1;
        final int newVarsSize = Math.min(state[1], vars.size());
        for (i = vars.size() - 1; i >= newVarsSize; i--) {
            orderHeap.remove(name2idx.remove(idx2name.remove(i)));
        }
        vars.shrinkTo(newVarsSize);
        final int newClausesSize = Math.min(state[2], clauses.size());
        for (i = clauses.size() - 1; i >= newClausesSize; i--) {
            simpleRemoveClause(clauses.get(i));
        }
        clauses.shrinkTo(newClausesSize);
        final int newLearntsSize = Math.min(state[3], learnts.size());
        for (i = learnts.size() - 1; i >= newLearntsSize; i--) {
            simpleRemoveClause(learnts.get(i));
        }
        learnts.shrinkTo(newLearntsSize);
        watches.shrinkTo(newVarsSize * 2);
        unitClauses.shrinkTo(state[4]);
        for (i = 0; ok && i < unitClauses.size(); i++) {
            uncheckedEnqueue(unitClauses.get(i), null);
            ok = propagate() == null;
        }
        if (config.proofGeneration) {
            final int newPgOriginalSize = Math.min(state[5], pgOriginalClauses.size());
            pgOriginalClauses.shrinkTo(newPgOriginalSize);
            final int newPgProofSize = Math.min(state[6], pgProof.size());
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
        assert c.size() > 1;
        watches.get(not(c.get(0))).push(new MSWatcher(c, c.get(1)));
        watches.get(not(c.get(1))).push(new MSWatcher(c, c.get(0)));
        if (c.learnt()) {
            learntsLiterals += c.size();
        } else {
            clausesLiterals += c.size();
        }
    }

    @Override
    protected void detachClause(final MSClause c) {
        assert c.size() > 1;
        watches.get(not(c.get(0))).remove(new MSWatcher(c, c.get(1)));
        watches.get(not(c.get(1))).remove(new MSWatcher(c, c.get(0)));
        if (c.learnt()) {
            learntsLiterals -= c.size();
        } else {
            clausesLiterals -= c.size();
        }
    }

    @Override
    protected void removeClause(final MSClause c) {
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

    @Override
    protected MSClause propagate() {
        MSClause confl = null;
        int numProps = 0;
        while (qhead < trail.size()) {
            final int p = trail.get(qhead++);
            final LNGVector<MSWatcher> ws = watches.get(p);
            int iInd = 0;
            int jInd = 0;
            numProps++;
            while (iInd < ws.size()) {
                final MSWatcher i = ws.get(iInd);
                final int blocker = i.blocker();
                if (value(blocker) == Tristate.TRUE) {
                    ws.set(jInd++, i);
                    iInd++;
                    continue;
                }
                final MSClause c = i.clause();
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
            ws.removeElements(iInd - jInd);
        }
        simpDBProps -= numProps;
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
                    for (int j = 1; j < c.size(); j++) {
                        if (v(c.get(j)).level() > 0) {
                            seen.set(var(c.get(j)), true);
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
        final double extraLim = claInc / learnts.size();
        learnts.manualSort(MSClause.minisatComparator);
        for (i = j = 0; i < learnts.size(); i++) {
            final MSClause c = learnts.get(i);
            if (c.size() > 2 && !locked(c) && (i < learnts.size() / 2 || c.activity() < extraLim)) {
                removeClause(learnts.get(i));
            } else {
                learnts.set(j++, learnts.get(i));
            }
        }
        learnts.removeElements(i - j);
    }

    @Override
    protected void removeSatisfied(final LNGVector<MSClause> cs) {
        int i;
        int j;
        for (i = j = 0; i < cs.size(); i++) {
            final MSClause c = cs.get(i);
            if (satisfied(c)) {
                removeClause(cs.get(i));
            } else {
                assert value(c.get(0)) == Tristate.UNDEF && value(c.get(1)) == Tristate.UNDEF;
                if (!config.proofGeneration) {
                    // This simplification does not work with proof generation
                    for (int k = 2; k < c.size(); k++) {
                        if (value(c.get(k)) == Tristate.FALSE) {
                            c.set(k--, c.get(c.size() - 1));
                            c.pop();
                        }
                    }
                }
                cs.set(j++, cs.get(i));
            }
        }
        cs.removeElements(i - j);
    }

    @Override
    protected boolean satisfied(final MSClause c) {
        for (int i = 0; i < c.size(); i++) {
            if (value(c.get(i)) == Tristate.TRUE) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean simplify() {
        assert decisionLevel() == 0;
        if (!ok || propagate() != null) {
            ok = false;
            return false;
        }
        if (nAssigns() == simpDBAssigns || (simpDBProps > 0)) {
            return true;
        }
        removeSatisfied(learnts);
        if (shouldRemoveSatsisfied) {
            removeSatisfied(clauses);
        }
        rebuildOrderHeap();
        simpDBAssigns = nAssigns();
        simpDBProps = clausesLiterals + learntsLiterals;
        return true;
    }

    /**
     * The main search procedure of the CDCL algorithm.
     * @param nofConflicts the number of conflicts till the next restart
     * @return a {@link Tristate} representing the result. {@code FALSE} if the
     *         formula is UNSAT, {@code TRUE} if the formula is SAT, and
     *         {@code UNDEF} if the state is not known yet (restart) or the
     *         handler canceled the computation
     */
    protected Tristate search(final int nofConflicts) {
        if (!ok) {
            return Tristate.FALSE;
        }
        int conflictC = 0;
        selectionOrderIdx = 0;
        while (true) {
            final MSClause confl = propagate();
            if (confl != null) {
                if (handler != null && !handler.detectedConflict()) {
                    canceledByHandler = true;
                    return Tristate.UNDEF;
                }
                conflictC++;
                if (decisionLevel() == 0) {
                    return Tristate.FALSE;
                }
                final LNGIntVector learntClause = new LNGIntVector();
                analyze(confl, learntClause);
                cancelUntil(analyzeBtLevel);
                if (analyzeBtLevel < selectionOrder.size()) {
                    selectionOrderIdx = analyzeBtLevel;
                }

                if (config.proofGeneration) {
                    final LNGIntVector vec = new LNGIntVector(learntClause.size());
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
                    final MSClause cr = new MSClause(learntClause, true);
                    learnts.push(cr);
                    attachClause(cr);
                    if (!incremental) {
                        claBumpActivity(cr);
                    }
                    uncheckedEnqueue(learntClause.get(0), cr);
                }
                decayActivities();
            } else {
                if (nofConflicts >= 0 && conflictC >= nofConflicts) {
                    cancelUntil(0);
                    return Tristate.UNDEF;
                }
                if (!incremental) {
                    if (decisionLevel() == 0 && !simplify()) {
                        return Tristate.FALSE;
                    }
                    if (learnts.size() - nAssigns() >= maxLearnts) {
                        reduceDB();
                    }
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

    /**
     * Analyzes a given conflict clause wrt. the current solver state. A 1-UIP
     * clause is created during this procedure and the new backtracking level is
     * stored in the solver state.
     * @param conflictClause the conflict clause to start the resolution
     *                       analysis with
     * @param outLearnt      the vector where the new learnt 1-UIP clause is
     *                       stored
     */
    protected void analyze(final MSClause conflictClause, final LNGIntVector outLearnt) {
        MSClause c = conflictClause;
        int pathC = 0;
        int p = LIT_UNDEF;
        outLearnt.push(-1);
        int index = trail.size() - 1;
        do {
            assert c != null;
            if (!incremental && c.learnt()) {
                claBumpActivity(c);
            }
            for (int j = (p == LIT_UNDEF) ? 0 : 1; j < c.size(); j++) {
                final int q = c.get(j);
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
            while (!seen.get(var(trail.get(index--)))) {
            }
            p = trail.get(index + 1);
            c = v(p).reason();
            seen.set(var(p), false);
            pathC--;
        } while (pathC > 0);
        outLearnt.set(0, not(p));
        simplifyClause(outLearnt);
    }

    /**
     * Minimizes a given learnt clause depending on the minimization method of
     * the solver configuration.
     * @param outLearnt the learnt clause which should be minimized
     */
    protected void simplifyClause(final LNGIntVector outLearnt) {
        int i;
        int j;
        final LNGIntVector analyzeToClear = new LNGIntVector(outLearnt);
        if (ccminMode == MiniSatConfig.ClauseMinimization.DEEP) {
            int abstractLevel = 0;
            for (i = 1; i < outLearnt.size(); i++) {
                abstractLevel |= abstractLevel(var(outLearnt.get(i)));
            }
            for (i = j = 1; i < outLearnt.size(); i++) {
                if (v(outLearnt.get(i)).reason() == null ||
                        !litRedundant(outLearnt.get(i), abstractLevel, analyzeToClear)) {
                    outLearnt.set(j++, outLearnt.get(i));
                }
            }
        } else if (ccminMode == MiniSatConfig.ClauseMinimization.BASIC) {
            for (i = j = 1; i < outLearnt.size(); i++) {
                if (v(outLearnt.get(i)).reason() == null) {
                    outLearnt.set(j++, outLearnt.get(i));
                } else {
                    final MSClause c = v(outLearnt.get(i)).reason();
                    for (int k = 1; k < c.size(); k++) {
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
     * Performs a simple removal of clauses used during the loading of an older
     * state.
     * @param c the clause to remove
     */
    protected void simpleRemoveClause(final MSClause c) {
        watches.get(not(c.get(0))).remove(new MSWatcher(c, c.get(1)));
        watches.get(not(c.get(1))).remove(new MSWatcher(c, c.get(0)));
    }
}
