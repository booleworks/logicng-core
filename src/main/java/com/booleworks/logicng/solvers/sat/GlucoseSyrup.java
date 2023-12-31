// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

/*
 * Glucose -- Copyright (c) 2009-2014, Gilles Audemard, Laurent Simon CRIL -
 * Univ. Artois, France LRI - Univ. Paris Sud, France (2009-2013) Labri - Univ.
 * Bordeaux, France <p> Syrup (Glucose Parallel) -- Copyright (c) 2013-2014,
 * Gilles Audemard, Laurent Simon CRIL - Univ. Artois, France Labri - Univ.
 * Bordeaux, France <p> Glucose sources are based on MiniSat (see below MiniSat
 * copyrights). Permissions and copyrights of Glucose (sources until 2013,
 * Glucose 3.0, single core) are exactly the same as Minisat on which it is
 * based on. (see below). <p> Glucose-Syrup sources are based on another
 * copyright. Permissions and copyrights for the parallel version of
 * Glucose-Syrup (the "Software") are granted, free of charge, to deal with the
 * Software without restriction, including the rights to use, copy, modify,
 * merge, publish, distribute, sublicence, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to
 * the following conditions: <p> - The above and below copyrights notices and
 * this permission notice shall be included in all copies or substantial
 * portions of the Software; - The parallel version of Glucose (all files
 * modified since Glucose 3.0 releases, 2013) cannot be used in any competitive
 * event (sat competitions/evaluations) without the express permission of the
 * authors (Gilles Audemard / Laurent Simon). This is also the case for any
 * competitive event using Glucose Parallel as an embedded SAT engine (single
 * core or not). <p> <p> --------------- Original Minisat Copyrights <p>
 * Copyright (c) 2003-2006, Niklas Een, Niklas Sorensson Copyright (c)
 * 2007-2010, Niklas Sorensson <p> Permission is hereby granted, free of charge,
 * to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following
 * conditions: <p> The above copyright notice and this permission notice shall
 * be included in all copies or substantial portions of the Software. <p> THE
 * SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
import com.booleworks.logicng.solvers.datastructures.LNGBoundedIntQueue;
import com.booleworks.logicng.solvers.datastructures.LNGBoundedLongQueue;
import com.booleworks.logicng.solvers.datastructures.MSClause;
import com.booleworks.logicng.solvers.datastructures.MSVariable;
import com.booleworks.logicng.solvers.datastructures.MSWatcher;

/**
 * Glucose 4.0 solver.
 * @version 2.1.0
 * @since 1.0
 */
public class GlucoseSyrup extends MiniSatStyleSolver {

    /**
     * the ratio of clauses which will be removed
     */
    protected static final int RATIO_REMOVE_CLAUSES = 2;

    /**
     * the lower bound for blocking restarts
     */
    protected static final int LB_BLOCKING_RESTART = 10000;

    // external solver configuration
    protected final GlucoseConfig glucoseConfig;

    // internal solver state
    protected LNGVector<LNGVector<MSWatcher>> watchesBin;
    protected LNGIntVector permDiff;
    protected LNGIntVector lastDecisionLevel;
    protected LNGBoundedLongQueue lbdQueue;
    protected LNGBoundedIntQueue trailQueue;
    protected LNGBooleanVector assump;
    protected int myflag;
    protected long analyzeLBD;
    protected int analyzeSzWithoutSelectors;
    protected int nbclausesbeforereduce;
    protected int conflicts;
    protected int conflictsRestarts;
    protected double sumLBD;
    protected int curRestart;

    // solver configuration
    protected int lbLBDMinimizingClause;
    protected int lbLBDFrozenClause;
    protected int lbSizeMinimizingClause;
    protected int firstReduceDB;
    protected int specialIncReduceDB;
    protected int incReduceDB;
    protected double factorK;
    protected double factorR;
    protected int sizeLBDQueue;
    protected int sizeTrailQueue;
    protected boolean reduceOnSize;
    protected int reduceOnSizeSize;
    protected double maxVarDecay;

    /**
     * Constructs a new Glucose 2 solver with the default values for solver
     * configuration. By default, incremental mode is activated.
     */
    GlucoseSyrup() {
        this(MiniSatConfig.builder().build(), GlucoseConfig.builder().build());
    }

    /**
     * Constructs a new Glucose solver with a given solver configuration.
     * @param config        the MiniSat configuration
     * @param glucoseConfig the Glucose configuration
     */
    public GlucoseSyrup(final MiniSatConfig config, final GlucoseConfig glucoseConfig) {
        super(config);
        this.glucoseConfig = glucoseConfig;
        initializeGlucose();
    }

    /**
     * Initializes the additional parameters.
     */
    protected void initializeGlucose() {
        initializeGlucoseConfig();
        watchesBin = new LNGVector<>();
        permDiff = new LNGIntVector();
        lastDecisionLevel = new LNGIntVector();
        lbdQueue = new LNGBoundedLongQueue();
        trailQueue = new LNGBoundedIntQueue();
        assump = new LNGBooleanVector();
        lbdQueue.initSize(sizeLBDQueue);
        trailQueue.initSize(sizeTrailQueue);
        myflag = 0;
        analyzeBtLevel = 0;
        analyzeLBD = 0;
        analyzeSzWithoutSelectors = 0;
        nbclausesbeforereduce = firstReduceDB;
        conflicts = 0;
        conflictsRestarts = 0;
        sumLBD = 0;
        curRestart = 1;
    }

    /**
     * Initializes the glucose configuration.
     */
    protected void initializeGlucoseConfig() {
        lbLBDMinimizingClause = glucoseConfig.lbLBDMinimizingClause;
        lbLBDFrozenClause = glucoseConfig.lbLBDFrozenClause;
        lbSizeMinimizingClause = glucoseConfig.lbSizeMinimizingClause;
        firstReduceDB = glucoseConfig.firstReduceDB;
        specialIncReduceDB = glucoseConfig.specialIncReduceDB;
        incReduceDB = glucoseConfig.incReduceDB;
        factorK = glucoseConfig.factorK;
        factorR = glucoseConfig.factorR;
        sizeLBDQueue = glucoseConfig.sizeLBDQueue;
        sizeTrailQueue = glucoseConfig.sizeTrailQueue;
        reduceOnSize = glucoseConfig.reduceOnSize;
        reduceOnSizeSize = glucoseConfig.reduceOnSizeSize;
        maxVarDecay = glucoseConfig.maxVarDecay;
    }

    @Override
    public int newVar(final boolean sign, final boolean dvar) {
        final int v = nVars();
        final MSVariable newVar = new MSVariable(sign);
        watches.push(new LNGVector<>());
        watches.push(new LNGVector<>());
        watchesBin.push(new LNGVector<>());
        watchesBin.push(new LNGVector<>());
        vars.push(newVar);
        seen.push(false);
        permDiff.push(0);
        assump.push(false);
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

            vec = new LNGIntVector(oc.size());
            vec.push(-1);
            for (i = 0; i < oc.size(); i++) {
                vec.push((var(oc.get(i)) + 1) * (-2 * (sign(oc.get(i)) ? 1 : 0) + 1));
            }
            pgProof.push(vec);
        }

        if (ps.size() == 0) {
            ok = false;
            if (config.proofGeneration) {
                pgProof.push(new LNGIntVector(1, 0));
            }
            return false;
        } else if (ps.size() == 1) {
            uncheckedEnqueue(ps.get(0), null);
            ok = propagate() == null;
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
        if (config.incremental && config.proofGeneration) {
            throw new IllegalStateException("Cannot use incremental and proof generation at the same time");
        }
        this.handler = handler;
        start(handler);
        model.clear();
        conflict.clear();
        if (!ok) {
            return Tristate.FALSE;
        }
        for (int i = 0; i < assumptions.size(); i++) {
            assump.set(var(assumptions.get(i)), !sign(assumptions.get(i)));
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
        } else if (status == Tristate.FALSE && conflict.size() == 0) {
            ok = false;
        }
        finishSolving(handler);
        cancelUntil(0);
        this.handler = null;
        canceledByHandler = false;
        for (int i = 0; i < assumptions.size(); i++) {
            assump.set(var(assumptions.get(i)), false);
        }
        return status;
    }

    @Override
    public void reset() {
        super.initialize();
        initializeGlucose();
    }

    @Override
    public int[] saveState() {
        throw new UnsupportedOperationException("The Glucose solver does not support state loading/saving");
    }

    @Override
    public void loadState(final int[] state) {
        throw new UnsupportedOperationException("The Glucose solver does not support state loading/saving");
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

    @Override
    protected void detachClause(final MSClause c) {
        assert c.size() > 1;
        if (c.size() == 2) {
            watchesBin.get(not(c.get(0))).remove(new MSWatcher(c, c.get(1)));
            watchesBin.get(not(c.get(1))).remove(new MSWatcher(c, c.get(0)));
        } else {
            watches.get(not(c.get(0))).remove(new MSWatcher(c, c.get(1)));
            watches.get(not(c.get(1))).remove(new MSWatcher(c, c.get(0)));
        }
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
                if (value(blocker) == Tristate.TRUE) {
                    ws.set(jInd++, i);
                    iInd++;
                    continue;
                }
                final MSClause c = i.clause();
                assert !c.oneWatched();
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
                if (incremental) {
                    int choosenPos = -1;
                    for (int k = 2; k < c.size(); k++) {
                        if (value(c.get(k)) != Tristate.FALSE) {
                            if (decisionLevel() > assumptions.size()) {
                                choosenPos = k;
                                break;
                            } else {
                                choosenPos = k;
                                if (value(c.get(k)) == Tristate.TRUE || !isSelector(var(c.get(k)))) {
                                    break;
                                }
                            }
                        }
                    }
                    if (choosenPos != -1) {
                        c.set(1, c.get(choosenPos));
                        c.set(choosenPos, falseLit);
                        watches.get(not(c.get(1))).push(w);
                        foundWatch = true;
                    }
                } else {
                    for (int k = 2; k < c.size() && !foundWatch; k++) {
                        if (value(c.get(k)) != Tristate.FALSE) {
                            c.set(1, c.get(k));
                            c.set(k, falseLit);
                            watches.get(not(c.get(1))).push(w);
                            foundWatch = true;
                        }
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
                    for (int j = c.size() == 2 ? 0 : 1; j < c.size(); j++) {
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
        learnts.manualSort(MSClause.glucoseComparator);
        if (learnts.get(learnts.size() / RATIO_REMOVE_CLAUSES).lbd() <= 3) {
            nbclausesbeforereduce += specialIncReduceDB;
        }
        if (learnts.back().lbd() <= 5) {
            nbclausesbeforereduce += specialIncReduceDB;
        }
        int limit = learnts.size() / 2;
        for (i = j = 0; i < learnts.size(); i++) {
            final MSClause c = learnts.get(i);
            if (c.lbd() > 2 && c.size() > 2 && c.canBeDel() && !locked(c) && (i < limit)) {
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

    @Override
    protected void removeSatisfied(final LNGVector<MSClause> cs) {
        int i;
        int j;
        for (i = j = 0; i < cs.size(); i++) {
            final MSClause c = cs.get(i);
            if (satisfied(c)) {
                removeClause(cs.get(i));
            } else {
                cs.set(j++, cs.get(i));
            }
        }
        cs.removeElements(i - j);
    }

    @Override
    protected boolean satisfied(final MSClause c) {
        if (incremental) {
            return (value(c.get(0)) == Tristate.TRUE) || (value(c.get(1)) == Tristate.TRUE);
        }
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
        if (!ok) {
            return false;
        } else {
            final MSClause cr = propagate();
            if (cr != null) {
                return ok = false;
            }
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
     * Computes the LBD for a given vector of literals.
     * @param lits the vector of literals
     * @param e    parameter for incremental mode
     * @return the LBD
     */
    protected long computeLBD(final LNGIntVector lits, final int e) {
        int end = e;
        long nblevels = 0;
        myflag++;
        if (incremental) {
            if (end == -1) {
                end = lits.size();
            }
            long nbDone = 0;
            for (int i = 0; i < lits.size(); i++) {
                if (nbDone >= end) {
                    break;
                }
                if (isSelector(var(lits.get(i)))) {
                    continue;
                }
                nbDone++;
                final int l = v(lits.get(i)).level();
                if (permDiff.get(l) != myflag) {
                    permDiff.set(l, myflag);
                    nblevels++;
                }
            }
        } else {
            for (int i = 0; i < lits.size(); i++) {
                final int l = v(lits.get(i)).level();
                if (permDiff.get(l) != myflag) {
                    permDiff.set(l, myflag);
                    nblevels++;
                }
            }
        }
        if (!reduceOnSize) {
            return nblevels;
        }
        if (lits.size() < reduceOnSizeSize) {
            return lits.size();
        }
        return lits.size() + nblevels;
    }

    /**
     * Computes the LBD for a given clause
     * @param c the clause
     * @return the LBD
     */
    protected long computeLBD(final MSClause c) {
        long nblevels = 0;
        myflag++;
        if (incremental) {
            long nbDone = 0;
            for (int i = 0; i < c.size(); i++) {
                if (nbDone >= c.sizeWithoutSelectors()) {
                    break;
                }
                if (isSelector(var(c.get(i)))) {
                    continue;
                }
                nbDone++;
                final int l = v(c.get(i)).level();
                if (permDiff.get(l) != myflag) {
                    permDiff.set(l, myflag);
                    nblevels++;
                }
            }
        } else {
            for (int i = 0; i < c.size(); i++) {
                final int l = v(c.get(i)).level();
                if (permDiff.get(l) != myflag) {
                    permDiff.set(l, myflag);
                    nblevels++;
                }
            }
        }
        if (!reduceOnSize) {
            return nblevels;
        }
        if (c.size() < reduceOnSizeSize) {
            return c.size();
        }
        return c.size() + nblevels;
    }

    /**
     * Returns {@code true} if a given variable is a selector variable,
     * {@code false} otherwise.
     * @param v the variable
     * @return {@code true} if the given variable is a selector variable
     */
    protected boolean isSelector(final int v) {
        return incremental && assump.get(v);
    }

    /**
     * A special clause minimization by binary resolution for small clauses.
     * @param outLearnt the vector where the new learnt 1-UIP clause is stored
     */
    protected void minimisationWithBinaryResolution(final LNGIntVector outLearnt) {
        final long lbd = computeLBD(outLearnt, -1);
        int p = not(outLearnt.get(0));
        if (lbd <= lbLBDMinimizingClause) {
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
     * @return a {@link Tristate} representing the result. {@code FALSE} if the
     *         formula is UNSAT, {@code TRUE} if the formula is SAT, and
     *         {@code UNKNOWN} if the state is not known yet (restart)
     */
    protected Tristate search() {
        assert ok;
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
                if (conflicts % 5000 == 0 && varDecay < maxVarDecay) {
                    varDecay += 0.01;
                }
                if (decisionLevel() == 0) {
                    return Tristate.FALSE;
                }
                trailQueue.push(trail.size());
                if (conflictsRestarts > LB_BLOCKING_RESTART && lbdQueue.valid() &&
                        trail.size() > factorR * trailQueue.avg()) {
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
                } else {
                    final MSClause cr = new MSClause(learntClause, true);
                    cr.setLBD(analyzeLBD);
                    cr.setOneWatched(false);
                    cr.setSizeWithoutSelectors(analyzeSzWithoutSelectors);
                    learnts.push(cr);
                    attachClause(cr);
                    claBumpActivity(cr);
                    uncheckedEnqueue(learntClause.get(0), cr);
                }
                varDecayActivity();
                claDecayActivity();
            } else {
                if (lbdQueue.valid() && (lbdQueue.avg() * factorK) > (sumLBD / conflictsRestarts)) {
                    lbdQueue.fastClear();
                    int bt = 0;
                    if (incremental) {
                        bt = Math.min(decisionLevel(), assumptions.size());
                    }
                    cancelUntil(bt);
                    return Tristate.UNDEF;
                }
                if (decisionLevel() == 0 && !simplify()) {
                    return Tristate.FALSE;
                }
                if (conflicts >= (curRestart * nbclausesbeforereduce) && learnts.size() > 0) {
                    curRestart = (conflicts / nbclausesbeforereduce) + 1;
                    reduceDB();
                    nbclausesbeforereduce += incReduceDB;
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
                    if (c.lbd() <= lbLBDFrozenClause) {
                        c.setCanBeDel(false);
                    }
                    c.setLBD(nblevels);
                }
            }
            for (int j = (p == LIT_UNDEF) ? 0 : 1; j < c.size(); j++) {
                final int q = c.get(j);
                if (!seen.get(var(q)) && v(q).level() != 0) {
                    if (!isSelector(var(q))) {
                        varBumpActivity(var(q));
                    }
                    seen.set(var(q), true);
                    if (v(q).level() >= decisionLevel()) {
                        pathC++;
                        if (!isSelector(var(q)) && (v(q).reason() != null) && v(q).reason().learnt()) {
                            lastDecisionLevel.push(q);
                        }
                    } else {
                        if (isSelector(var(q))) {
                            assert value(q) == Tristate.FALSE;
                            selectors.push(q);
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
     * Minimizes a given learnt clause depending on the minimization method of
     * the solver configuration.
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
                final MSVariable v = v(outLearnt.get(i));
                if (v.reason() == null) {
                    outLearnt.set(j++, outLearnt.get(i));
                } else {
                    final MSClause c = v(outLearnt.get(i)).reason();
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
        if (!incremental && outLearnt.size() <= lbSizeMinimizingClause) {
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
        analyzeSzWithoutSelectors = 0;
        if (incremental) {
            for (int k = 0; k < outLearnt.size(); k++) {
                if (!isSelector(var(outLearnt.get(k)))) {
                    analyzeSzWithoutSelectors++;
                } else if (k > 0) {
                    break;
                }
            }
        } else {
            analyzeSzWithoutSelectors = outLearnt.size();
        }
        analyzeLBD = computeLBD(outLearnt, outLearnt.size() - selectors.size());
        if (lastDecisionLevel.size() > 0) {
            for (int k = 0; k < lastDecisionLevel.size(); k++) {
                if ((v(lastDecisionLevel.get(k)).reason()).lbd() < analyzeLBD) {
                    varBumpActivity(var(lastDecisionLevel.get(k)));
                }
            }
            lastDecisionLevel.clear();
        }
        for (int m = 0; m < analyzeToClear.size(); m++) {
            seen.set(var(analyzeToClear.get(m)), false);
        }
        for (int m = 0; m < selectors.size(); m++) {
            seen.set(var(selectors.get(m)), false);
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

    public GlucoseConfig getGlucoseConfig() {
        return glucoseConfig;
    }
}

