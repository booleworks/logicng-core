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
 */

package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.datastructures.Tristate.UNDEF;
import static com.booleworks.logicng.handlers.Handler.aborted;
import static com.booleworks.logicng.handlers.Handler.start;
import static com.booleworks.logicng.handlers.SATHandler.finishSolving;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.datastructures.LNGBoundedIntQueue;
import com.booleworks.logicng.solvers.datastructures.LNGBoundedLongQueue;
import com.booleworks.logicng.solvers.datastructures.LNGClause;
import com.booleworks.logicng.solvers.datastructures.LNGHeap;
import com.booleworks.logicng.solvers.datastructures.LNGVariable;
import com.booleworks.logicng.solvers.datastructures.LNGWatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The core SAT Solver of LogicNG. Heavily inspired by MiniSat, Glucose, and MiniCard.
 * <p>
 * <b>This core solver should usually not be used directly. It exposes a lot of methods
 * which should only be called if you really require to tweak with the internals of
 * the SAT Solver. Using {@link SATSolver} will be sufficient in almost all cases.</b>
 * @version 3.0.0
 * @since 3.0.0
 */
// TODO: cleanup and sort methods, add links (and licences?) to MiniSat, Glucose, and MiniCard
public class LNGCoreSolver {

    /**
     * The undefined literal.
     */
    public static final int LIT_UNDEF = -1;

    /**
     * The error literal.
     */
    protected static final int LIT_ERROR = -2;

    /**
     * The ratio of clauses which will be removed.
     */
    protected static final int RATIO_REMOVE_CLAUSES = 2;

    /**
     * The lower bound for blocking restarts.
     */
    protected static final int LB_BLOCKING_RESTART = 10000;

    protected FormulaFactory f;
    protected SATSolverConfig config;
    protected SATSolverLowLevelConfig llConfig;
    protected boolean inSatCall;

    // mapping of variable names to variable indices
    protected Map<String, Integer> name2idx = new TreeMap<>();
    protected Map<Integer, String> idx2name = new TreeMap<>();

    // bookkeeping of solver states
    protected LNGIntVector validStates = new LNGIntVector();
    protected int nextStateId = 0;

    // internal solver state
    protected boolean ok = true;
    protected int qhead = 0;
    protected LNGIntVector unitClauses = new LNGIntVector();
    protected LNGVector<LNGClause> clauses = new LNGVector<>();
    protected LNGVector<LNGClause> learnts = new LNGVector<>();
    protected LNGVector<LNGVector<LNGWatcher>> watches = new LNGVector<>();
    protected LNGVector<LNGVariable> vars = new LNGVector<>();
    protected LNGHeap orderHeap = new LNGHeap(this);
    protected LNGIntVector trail = new LNGIntVector();
    protected LNGIntVector trailLim = new LNGIntVector();
    protected LNGBooleanVector model = new LNGBooleanVector();
    protected LNGIntVector assumptionsConflict = new LNGIntVector();
    protected LNGIntVector assumptions = new LNGIntVector();
    protected LNGVector<Proposition> assumptionPropositions = new LNGVector<>();
    protected LNGBooleanVector seen = new LNGBooleanVector();
    protected int analyzeBtLevel = 0;
    protected double claInc = 1;
    protected double varInc;
    protected double varDecay;
    protected int clausesLiterals = 0;
    protected int learntsLiterals = 0;

    // SAT handler
    protected SATHandler handler;
    protected boolean canceledByHandler = false;

    // Proof generating information
    protected LNGVector<ProofInformation> pgOriginalClauses = new LNGVector<>();
    protected LNGVector<LNGIntVector> pgProof = new LNGVector<>();

    // backbone computation
    protected boolean computingBackbone = false;
    protected Stack<Integer> backboneCandidates;
    protected LNGIntVector backboneAssumptions;
    protected HashMap<Integer, Tristate> backboneMap;

    // Selection order
    protected LNGIntVector selectionOrder = new LNGIntVector();
    protected int selectionOrderIdx = 0;

    // internal glucose-related state
    protected LNGVector<LNGVector<LNGWatcher>> watchesBin = new LNGVector<>();
    protected LNGIntVector permDiff = new LNGIntVector();
    protected LNGIntVector lastDecisionLevel = new LNGIntVector();
    protected LNGBoundedLongQueue lbdQueue = new LNGBoundedLongQueue();
    protected LNGBoundedIntQueue trailQueue = new LNGBoundedIntQueue();
    protected int myflag = 0;
    protected long analyzeLBD = 0;
    protected int nbClausesBeforeReduce;
    protected int conflicts = 0;
    protected int conflictsRestarts = 0;
    protected double sumLBD = 0;
    protected int curRestart = 1;

    /**
     * Constructs a new core solver with a given configuration and formula factory.
     * @param f      the formula factory
     * @param config the configuration
     */
    public LNGCoreSolver(final FormulaFactory f, final SATSolverConfig config) {
        this.f = f;
        this.config = config;
        llConfig = config.lowLevelConfig;
        varInc = llConfig.varInc;
        varDecay = llConfig.varDecay;
        lbdQueue.initSize(llConfig.sizeLBDQueue);
        trailQueue.initSize(llConfig.sizeTrailQueue);
        nbClausesBeforeReduce = llConfig.firstReduceDB;
    }

    /**
     * Generates a clause vector of a collection of literals for a given SAT Solver and configuration.
     * If variables are unknown, they will be added with {@link LNGCoreSolver#newVar} and the given
     * initial phase and the decision variable flag.
     * @param literals     the literals
     * @param solver       the internal solver
     * @param initialPhase the initial phase of new literals
     * @param decisionVar  whether the variable should be handled as decision variable
     * @return the clause vector
     */
    public static LNGIntVector generateClauseVector(final Collection<? extends Literal> literals,
                                                    final LNGCoreSolver solver,
                                                    final boolean initialPhase, final boolean decisionVar) {
        final LNGIntVector clauseVec = new LNGIntVector(literals.size());
        for (final Literal lit : literals) {
            clauseVec.unsafePush(solverLiteral(lit, solver, initialPhase, decisionVar));
        }
        return clauseVec;
    }

    /**
     * Generates a clause vector of a collection of literals for a given SAT Solver and configuration.
     * @param literals the literals
     * @param solver   the internal solver
     * @return the clause vector
     */
    public static LNGIntVector generateClauseVector(final Collection<? extends Literal> literals,
                                                    final LNGCoreSolver solver) {
        return generateClauseVector(literals, solver, solver.config().initialPhase(), true);
    }

    /**
     * Returns or creates the internal index for the given literal on the given solver.
     * If it is unknown, it will be created with the given initial phase.
     * @param lit          the literal
     * @param solver       the solver
     * @param initialPhase the initial phase if the variable is unknown
     * @param decisionVar  whether the variable should be handled as decision variable
     * @return the internal index of the literal
     */
    public static int solverLiteral(final Literal lit, final LNGCoreSolver solver, final boolean initialPhase, final boolean decisionVar) {
        int index = solver.idxForName(lit.name());
        if (index == -1) {
            index = solver.newVar(!initialPhase, decisionVar);
            solver.addName(lit.name(), index);
        }
        return lit.phase() ? index * 2 : (index * 2) ^ 1;
    }

    /**
     * Returns or creates the internal index for the given literal on the given solver.
     * If it is unknown, it will be created with the given initial phase specified by the
     * {@link SATSolverConfig#initialPhase() configuration of the solver} and with
     * {@code decisionVar == true}.
     * @param lit    the literal
     * @param solver the solver
     * @return the internal index of the literal
     */
    public static int solverLiteral(final Literal lit, final LNGCoreSolver solver) {
        return solverLiteral(lit, solver, solver.config.initialPhase, true);
    }

    /**
     * Marks this solver to be used in a {@link SATCall}.
     * Until {@link #finishSatCall()} is called, additional calls to this method or
     * other operations on the SAT solver like adding new formulas, executing solver
     * functions, or saving/loading state, will fail with an {@link IllegalStateException}.
     */
    protected void startSatCall() {
        assertNotInSatCall();
        inSatCall = true;
    }

    /**
     * Declares that the solver is not used anymore in a {@link SATCall}.
     */
    protected void finishSatCall() {
        inSatCall = false;
    }

    /**
     * Creates a literal for a given variable number and literal.
     * @param var  the variable number
     * @param sign {@code true} if the literal is negative, {@code false} otherwise
     * @return the literal (as integer value)
     */
    public static int mkLit(final int var, final boolean sign) {
        return var + var + (sign ? 1 : 0);
    }

    /**
     * Negates a given literal.
     * @param lit the literal
     * @return the negated literal
     */
    public static int not(final int lit) {
        return lit ^ 1;
    }

    /**
     * Returns {@code true} if a given literal is negated, {@code false} otherwise.
     * @param lit the literal
     * @return {@code true} if the literal is negated
     */
    public static boolean sign(final int lit) {
        return (lit & 1) == 1;
    }

    /**
     * Returns the variable index for a given literal.
     * @param lit the literal
     * @return the variable index of the literal
     */
    public static int var(final int lit) {
        return lit >> 1;
    }

    /**
     * Returns the variable for a given literal.
     * @param lit the literal
     * @return the variable of the literal
     */
    protected LNGVariable v(final int lit) {
        return vars.get(lit >> 1);
    }

    /**
     * Returns the assigned value of a given literal.
     * @param lit the literal
     * @return the assigned value of the literal
     */
    protected Tristate value(final int lit) {
        return sign(lit) ? Tristate.negate(v(lit).assignment()) : v(lit).assignment();
    }

    /**
     * Compares two variables by their activity.
     * @param x the first variable
     * @param y the second variable
     * @return {@code true} if the first variable's activity is larger than the second one's
     */
    public boolean lt(final int x, final int y) {
        return vars.get(x).activity() > vars.get(y).activity();
    }

    /**
     * Returns the variable index for a given variable name.
     * @param name the variable name
     * @return the variable index for the name
     */
    public int idxForName(final String name) {
        final Integer id = name2idx.get(name);
        return id == null ? -1 : id;
    }

    /**
     * Returns the name for a given variable index.
     * @param var the variable index
     * @return the name for the index
     */
    public String nameForIdx(final int var) {
        return idx2name.get(var);
    }

    /**
     * Adds a new variable name with a given variable index to this solver.
     * @param name the variable name
     * @param id   the variable index
     */
    public void addName(final String name, final int id) {
        name2idx.put(name, id);
        idx2name.put(id, name);
    }

    /**
     * Adds a new variable to the solver.
     * @param sign the initial polarity of the new variable, {@code true} if negative, {@code false} if positive
     * @param dvar {@code true} if this variable can be used as a decision variable, {@code false} if it should not be
     *             used as a decision variable
     * @return the index of the new variable
     */
    public int newVar(final boolean sign, final boolean dvar) {
        final int v = nVars();
        final LNGVariable newVar = new LNGVariable(sign);
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

    /**
     * Adds a unit clause to the solver.
     * @param lit         the unit clause's literal
     * @param proposition a proposition (if required for proof tracing)
     * @return {@code true} if the clause was added successfully, {@code false} otherwise
     */
    public boolean addClause(final int lit, final Proposition proposition) {
        final LNGIntVector unit = new LNGIntVector(1);
        unit.push(lit);
        return addClause(unit, proposition);
    }

    /**
     * Adds a clause to the solver.
     * @param ps          the literals of the clause
     * @param proposition a proposition (if required for proof tracing)
     * @return {@code true} if the clause was added successfully, {@code false} otherwise
     * @throws IllegalStateException if a {@link SATCall} is currently running on this solver
     */
    public boolean addClause(final LNGIntVector ps, final Proposition proposition) {
        assertNotInSatCall();
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
            final LNGClause c = new LNGClause(ps, -1);
            clauses.push(c);
            attachClause(c);
        }
        return true;
    }

    /**
     * Solves the formula currently stored in the solver.  Returns {@link Tristate#TRUE} if the formula is satisfiable (SAT),
     * {@link Tristate#FALSE} if the formula is unsatisfiable (UNSAT), or {@link Tristate#UNDEF} if the computation was canceled
     * by a {@link SATHandler}.  If {@code null} is passed as handler, the solver will run until the satisfiability is decided.
     * @param handler a sat handler
     * @return {@link Tristate#TRUE} if the formula is satisfiable, {@link Tristate#FALSE} if the formula is not satisfiable, or
     *         {@link Tristate#UNDEF} if the computation was canceled.
     */
    public Tristate internalSolve(final SATHandler handler) {
        this.handler = handler;
        final Tristate result = internalSolve();
        this.handler = null;
        return result;
    }

    public Tristate internalSolve() {
        start(handler);
        model.clear();
        assumptionsConflict.clear();
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
            for (final LNGVariable v : vars) {
                model.push(v.assignment() == Tristate.TRUE);
            }
        } else if (status == Tristate.FALSE && assumptionsConflict.empty()) {
            ok = false;
        }
        finishSolving(handler);
        cancelUntil(0);
        canceledByHandler = false;
        return status;
    }

    /**
     * Sets (or clears) the SAT handler which should be used for subsequent SAT calls.
     * @param handler the SAT handler to be used
     */
    public void setHandler(final SATHandler handler) {
        this.handler = handler;
    }

    /**
     * Solves the formula currently stored in the solver together with the given assumption literals.  Returns
     * {@link Tristate#TRUE} if the formula and the assumptions are satisfiable (SAT), {@link Tristate#FALSE} if the formula and the
     * assumptions are not satisfiable together (UNSAT), or {@link Tristate#UNDEF} if the computation was canceled by a
     * {@link SATHandler}. If {@code null} is passed as handler, the solver will run until the satisfiability is decided.
     * @param handler     a sat handler
     * @param assumptions the assumptions as a given vector of literals
     * @return {@link Tristate#TRUE} if the formula and the assumptions are satisfiable, {@link Tristate#FALSE} if they are
     *         not satisfiable, or {@link Tristate#UNDEF} if the computation was canceled.
     */
    public Tristate internalSolve(final SATHandler handler, final LNGIntVector assumptions) {
        this.assumptions = new LNGIntVector(assumptions);
        final Tristate result = internalSolve(handler);
        this.assumptions.clear();
        return result;
    }

    /**
     * Returns the current model of the solver or an empty vector if there is none.
     * @return the current model of the solver
     */
    public LNGBooleanVector model() {
        return model;
    }

    /**
     * Returns {@code false} if this solver is known to be in a conflicting state, otherwise {@code true}.
     * @return {@code false} if this solver is known to be in a conflicting state, otherwise {@code true}
     */
    public boolean ok() {
        return ok;
    }

    /**
     * Returns the conflict of the solver or an empty vector if there is none.
     * @return the conflict of the solver
     */
    public LNGIntVector assumptionsConflict() {
        return assumptionsConflict;
    }

    /**
     * Saves and returns the solver state expressed as an integer array which stores the length of the internal data
     * structures.
     * @return the current solver state
     */
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
        final int id = nextStateId++;
        validStates.push(id);
        return new SolverState(id, state);
    }

    /**
     * Loads a given state in the solver.
     * <p>
     * ATTENTION: You can only load a state which was created by this instance of the solver before the current state.
     * Only the sizes of the internal data structures are stored, meaning you can track back in time and restore a solver
     * state with fewer variables and/or fewer clauses.  It is not possible to import a solver state from another solver
     * or another solving execution.
     * @param solverState the solver state to load
     * @throws IllegalArgumentException if the solver state is not valid anymore
     */
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
            final LNGClause learnt = learnts.get(i);
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

    /**
     * Returns the number of variables of the solver.
     * @return the number of variables of the solver
     */
    public int nVars() {
        return vars.size();
    }

    /**
     * Returns the mapping from variable names to internal solver indices.
     * @return the mapping from variable names to internal solver indices
     */
    public Map<String, Integer> name2idx() {
        return name2idx;
    }

    /**
     * Returns the set of variables currently known by the solver.
     * @return the set of variables currently known by the solver
     */
    public SortedSet<Variable> knownVariables() {
        final SortedSet<Variable> result = new TreeSet<>();
        for (final String name : name2idx.keySet()) {
            result.add(f.variable(name));
        }
        return result;
    }

    /**
     * Returns the current decision level of the solver.
     * @return the current decision level of the solver
     */
    protected int decisionLevel() {
        return trailLim.size();
    }

    /**
     * Helper function used to maintain an abstraction of levels involved during conflict analysis.
     * @param x a variable index
     * @return the abstraction of levels
     */
    protected int abstractLevel(final int x) {
        return 1 << (vars.get(x).level() & 31);
    }

    /**
     * Inserts a variable (given by its index) into the heap of decision variables.
     * @param x the variable index
     */
    protected void insertVarOrder(final int x) {
        if (!orderHeap.inHeap(x) && vars.get(x).decision()) {
            orderHeap.insert(x);
        }
    }

    /**
     * Picks the next branching literal.
     * @return the literal or -1 if there are no unassigned literals left
     */
    protected int pickBranchLit() {
        if (selectionOrder.size() > 0 && selectionOrderIdx < selectionOrder.size()) {
            while (selectionOrderIdx < selectionOrder.size()) {
                final int lit = selectionOrder.get(selectionOrderIdx++);
                final int var = var(lit);
                final LNGVariable LNGVariable = vars.get(var);
                if (LNGVariable.assignment() == UNDEF) {
                    return lit;
                }
            }
        }
        int next = -1;
        while (next == -1 || vars.get(next).assignment() != UNDEF || !vars.get(next).decision()) {
            if (orderHeap.empty()) {
                return -1;
            } else {
                next = orderHeap.removeMin();
            }
        }
        return mkLit(next, vars.get(next).polarity());
    }

    /**
     * Decays the variable activity increment by the variable decay factor.
     */
    protected void varDecayActivity() {
        varInc *= (1 / varDecay);
    }

    /**
     * Bumps the activity of the variable at a given index.
     * @param v the variable index
     */
    protected void varBumpActivity(final int v) {
        varBumpActivity(v, varInc);
    }

    /**
     * Bumps the activity of the variable at a given index by a given value.
     * @param v   the variable index
     * @param inc the increment value
     */
    protected void varBumpActivity(final int v, final double inc) {
        final LNGVariable var = vars.get(v);
        var.incrementActivity(inc);
        if (var.activity() > 1e100) {
            for (final LNGVariable variable : vars) {
                variable.rescaleActivity();
            }
            varInc *= 1e-100;
        }
        if (orderHeap.inHeap(v)) {
            orderHeap.decrease(v);
        }
    }

    /**
     * Returns {@code true} if the given clause is locked and therefore cannot be removed, {@code false} otherwise.
     * @param c the clause
     * @return {@code true} if the given clause is locked
     */
    protected boolean locked(final LNGClause c) {
        return value(c.get(0)) == Tristate.TRUE && v(c.get(0)).reason() != null && v(c.get(0)).reason() == c;
    }

    /**
     * Decays the clause activity increment by the clause decay factor.
     */
    protected void claDecayActivity() {
        claInc *= (1 / llConfig.clauseDecay);
    }

    /**
     * Bumps the activity of the given clause.
     * @param c the clause
     */
    protected void claBumpActivity(final LNGClause c) {
        c.incrementActivity(claInc);
        if (c.activity() > 1e20) {
            for (final LNGClause clause : learnts) {
                clause.rescaleActivity();
            }
            claInc *= 1e-20;
        }
    }

    /**
     * Assigns a literal (= a variable to the respective value).
     * @param lit    the literal
     * @param reason the reason clause of the assignment (conflict resolution) or {@code null} if it was a decision
     */
    protected void uncheckedEnqueue(final int lit, final LNGClause reason) {
        assert value(lit) == Tristate.UNDEF;
        final LNGVariable var = v(lit);
        var.assign(Tristate.fromBool(!sign(lit)));
        var.setReason(reason);
        var.setLevel(decisionLevel());
        trail.push(lit);
    }

    /**
     * Attaches a given clause to the solver (i.e. the watchers for this clause are initialized).
     * @param c the clause
     */
    protected void attachClause(final LNGClause c) {
        if (c.isAtMost()) {
            for (int i = 0; i < c.atMostWatchers(); i++) {
                final int l = c.get(i);
                watches.get(l).push(new LNGWatcher(c, LIT_UNDEF));
            }
            clausesLiterals += c.size();
        } else {
            assert c.size() > 1;
            if (c.size() == 2) {
                watchesBin.get(not(c.get(0))).push(new LNGWatcher(c, c.get(1)));
                watchesBin.get(not(c.get(1))).push(new LNGWatcher(c, c.get(0)));
            } else {
                watches.get(not(c.get(0))).push(new LNGWatcher(c, c.get(1)));
                watches.get(not(c.get(1))).push(new LNGWatcher(c, c.get(0)));
            }
            if (c.learnt()) {
                learntsLiterals += c.size();
            } else {
                clausesLiterals += c.size();
            }
        }
    }

    /**
     * Detaches a given clause (e.g. removes all watchers pointing to this clause).
     * @param c the clause
     */
    protected void detachClause(final LNGClause c) {
        assert c.size() > 1 && !c.isAtMost();
        simpleRemoveClause(c);
        if (c.learnt()) {
            learntsLiterals -= c.size();
        } else {
            clausesLiterals -= c.size();
        }
    }

    /**
     * Removes a given clause.
     * @param c the clause to remove
     */
    protected void removeClause(final LNGClause c) {
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

    /**
     * Performs unit propagation.
     * @return the conflicting clause if a conflict arose during unit propagation or {@code null} if there was none
     */
    protected LNGClause propagate() {
        LNGClause confl = null;
        while (qhead < trail.size()) {
            final int p = trail.get(qhead++);
            final LNGVector<LNGWatcher> ws = watches.get(p);
            int iInd = 0;
            int jInd = 0;
            final LNGVector<LNGWatcher> wbin = watchesBin.get(p);
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
                final LNGWatcher i = ws.get(iInd);
                final int blocker = i.blocker();
                if (blocker != LIT_UNDEF && value(blocker) == Tristate.TRUE) {
                    ws.set(jInd++, i);
                    iInd++;
                    continue;
                }
                final LNGClause c = i.clause();

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
                        final LNGWatcher w = new LNGWatcher(c, LIT_UNDEF);
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
                    final LNGWatcher w = new LNGWatcher(c, first);
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

    /**
     * Returns {@code true} if a given literal is redundant in the current conflict analysis, {@code false} otherwise.
     * @param p              the literal
     * @param abstractLevels an abstraction of levels
     * @param analyzeToClear helper vector
     * @return {@code true} if a given literal is redundant in the current conflict analysis
     */
    protected boolean litRedundant(final int p, final int abstractLevels, final LNGIntVector analyzeToClear) {
        final LNGIntVector analyzeStack = new LNGIntVector();
        analyzeStack.push(p);
        final int top = analyzeToClear.size();
        while (analyzeStack.size() > 0) {
            assert v(analyzeStack.back()).reason() != null;
            final LNGClause c = v(analyzeStack.back()).reason();
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

    /**
     * Analyses the final conflict if there were assumptions.
     * @param p the conflicting literal
     */
    protected void analyzeAssumptionConflict(final int p) {
        assumptionsConflict.clear();
        assumptionsConflict.push(p);
        if (decisionLevel() == 0) {
            return;
        }
        seen.set(var(p), true);
        int x;
        LNGVariable v;
        for (int i = trail.size() - 1; i >= trailLim.get(0); i--) {
            x = var(trail.get(i));
            if (seen.get(x)) {
                v = vars.get(x);
                if (v.reason() == null) {
                    assert v.level() > 0;
                    assumptionsConflict.push(not(trail.get(i)));
                } else {
                    final LNGClause c = v.reason();
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

    protected void cancelUntil(final int level) {
        if (decisionLevel() > level) {
            if (!computingBackbone) {
                for (int c = trail.size() - 1; c >= trailLim.get(level); c--) {
                    final int x = var(trail.get(c));
                    final LNGVariable v = vars.get(x);
                    v.assign(Tristate.UNDEF);
                    v.setPolarity(sign(trail.get(c)));
                    insertVarOrder(x);
                }
            } else {
                for (int c = trail.size() - 1; c >= trailLim.get(level); c--) {
                    final int x = var(trail.get(c));
                    final LNGVariable v = vars.get(x);
                    v.assign(Tristate.UNDEF);
                    v.setPolarity(!computingBackbone && sign(trail.get(c)));
                    insertVarOrder(x);
                }
            }
            qhead = trailLim.get(level);
            trail.removeElements(trail.size() - trailLim.get(level));
            trailLim.removeElements(trailLim.size() - level);
        }
    }

    /**
     * Reduces the database of learnt clauses.  Only clauses of the first half of the clauses with the most activity
     * are possibly removed.  A clause is only removed if it is not locked, i.e. is the reason of an assignment for a
     * variable.
     */
    protected void reduceDB() {
        int i;
        int j;
        learnts.manualSort(LNGClause.glucoseComparator);
        if (learnts.get(learnts.size() / RATIO_REMOVE_CLAUSES).lbd() <= 3) {
            nbClausesBeforeReduce += llConfig.specialIncReduceDB;
        }
        if (learnts.back().lbd() <= 5) {
            nbClausesBeforeReduce += llConfig.specialIncReduceDB;
        }
        int limit = learnts.size() / 2;
        for (i = j = 0; i < learnts.size(); i++) {
            final LNGClause c = learnts.get(i);
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
     * Returns the original clauses for proof generation.
     * @return the original clauses for proof generation
     */
    public LNGVector<ProofInformation> pgOriginalClauses() {
        return pgOriginalClauses;
    }

    /**
     * Returns the proof clauses for proof generation.
     * @return the proof clauses for proof generation
     */
    public LNGVector<LNGIntVector> pgProof() {
        return pgProof;
    }

    /**
     * The main search procedure of the CDCL algorithm.
     * @return a {@link Tristate} representing the result.  {@code FALSE} if the formula is UNSAT, {@code TRUE} if the
     *         formula is SAT, and {@code UNDEF} if the state is not known yet (restart) or the handler canceled the computation
     */
    protected Tristate search() {
        if (!ok) {
            return Tristate.FALSE;
        }
        final LNGIntVector learntClause = new LNGIntVector();
        final LNGIntVector selectors = new LNGIntVector();
        selectionOrderIdx = 0;
        while (true) {
            final LNGClause confl = propagate();
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
                    final LNGClause cr = new LNGClause(learntClause, nextStateId);
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
                        if (config.proofGeneration) {
                            final int drupLit = (var(p) + 1) * (-2 * (sign(p) ? 1 : 0) + 1);
                            pgOriginalClauses.push(new ProofInformation(new LNGIntVector(1, drupLit), assumptionPropositions.get(decisionLevel())));
                        }
                        analyzeAssumptionConflict(not(p));
                        return Tristate.FALSE;
                    } else {
                        if (config.proofGeneration) {
                            final int drupLit = (var(p) + 1) * (-2 * (sign(p) ? 1 : 0) + 1);
                            pgOriginalClauses.push(new ProofInformation(new LNGIntVector(1, drupLit), assumptionPropositions.get(decisionLevel())));
                        }
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
     * Analyzes a given conflict clause wrt. the current solver state.  A 1-UIP clause is created during this procedure
     * and the new backtracking level is stored in the solver state.
     * @param conflictClause the conflict clause to start the resolution analysis with
     * @param outLearnt      the vector where the new learnt 1-UIP clause is stored
     * @param selectors      a vector of selector variables
     */
    protected void analyze(final LNGClause conflictClause, final LNGIntVector outLearnt,
                           final LNGIntVector selectors) {
        LNGClause c = conflictClause;
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
        if (config.clauseMinimization == SATSolverConfig.ClauseMinimization.DEEP) {
            int abstractLevel = 0;
            for (i = 1; i < outLearnt.size(); i++) {
                abstractLevel |= abstractLevel(var(outLearnt.get(i)));
            }
            for (i = j = 1; i < outLearnt.size(); i++) {
                if (v(outLearnt.get(i)).reason() == null || !litRedundant(outLearnt.get(i), abstractLevel, analyzeToClear)) {
                    outLearnt.set(j++, outLearnt.get(i));
                }
            }
        } else if (config.clauseMinimization == SATSolverConfig.ClauseMinimization.BASIC) {
            for (i = j = 1; i < outLearnt.size(); i++) {
                final LNGClause c = v(outLearnt.get(i)).reason();
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
     * Computes the LBD for a given clause
     * @param c the clause
     * @return the LBD
     */
    protected long computeLBD(final LNGClause c) {
        return computeLBD(c.getData());
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
            for (final LNGWatcher wbin : watchesBin.get(p)) {
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
     * Performs an unconditional backtrack to level zero.
     */
    protected void completeBacktrack() {
        for (int v = 0; v < vars.size(); v++) {
            final LNGVariable var = vars.get(v);
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
    protected void simpleRemoveClause(final LNGClause c) {
        if (c.isAtMost()) {
            for (int i = 0; i < c.atMostWatchers(); i++) {
                watches.get(c.get(i)).remove(new LNGWatcher(c, c.get(i)));
            }
        } else if (c.size() == 2) {
            watchesBin.get(not(c.get(0))).remove(new LNGWatcher(c, c.get(1)));
            watchesBin.get(not(c.get(1))).remove(new LNGWatcher(c, c.get(0)));
        } else {
            watches.get(not(c.get(0))).remove(new LNGWatcher(c, c.get(1)));
            watches.get(not(c.get(1))).remove(new LNGWatcher(c, c.get(0)));
        }
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
        final LNGClause cr = new LNGClause(ps, -1, true);
        cr.setAtMostWatchers(ps.size() - k + 1);
        clauses.push(cr);
        attachClause(cr);
    }

    /**
     * Detaches a given at-most clause.
     * @param c the at-most clause.
     */
    protected void detachAtMost(final LNGClause c) {
        for (int i = 0; i < c.atMostWatchers(); i++) {
            watches.get(c.get(i)).remove(new LNGWatcher(c, c.get(i)));
        }
        clausesLiterals -= c.size();
    }

    protected int findNewWatchForAtMostClause(final LNGClause c, final int p) {
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
     * Converts the internal model into a list of literals, considering only the variables with the relevant indices.
     * @param internalModel   the internal model (e.g. from {@link #model()}
     * @param relevantIndices the indices of the relevant variables
     * @return the external model
     */
    public List<Literal> convertInternalModel(final LNGBooleanVector internalModel, final LNGIntVector relevantIndices) {
        final List<Literal> literals = new ArrayList<>(internalModel.size());
        for (int i = 0; i < relevantIndices.size(); i++) {
            final int index = relevantIndices.get(i);
            if (index != -1) {
                literals.add(f.literal(nameForIdx(index), internalModel.get(index)));
            }
        }
        return literals;
    }

    /**
     * Returns {@code true} if a {@link SATCall} is currently using this solver, otherwise {@code false}.
     * @return {@code true} if a {@link SATCall} is currently using this solver, otherwise {@code false}
     */
    public boolean inSatCall() {
        return inSatCall;
    }

    /**
     * Checks if this solver is currently used in a {@link SATCall} and throws an {@link IllegalStateException}
     * in this case. Otherwise, nothing happens.
     * @throws IllegalStateException if this solver is currently used in a SAT call
     */
    public void assertNotInSatCall() {
        if (inSatCall) {
            throw new IllegalStateException("This operation is not allowed because a SAT call is running on this solver!");
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ok            ").append(ok).append(System.lineSeparator());
        sb.append("qhead         ").append(qhead).append(System.lineSeparator());
        sb.append("#clauses      ").append(clauses.size()).append(System.lineSeparator());
        sb.append("#learnts      ").append(learnts.size()).append(System.lineSeparator());
        sb.append("#watches      ").append(watches.size()).append(System.lineSeparator());
        sb.append("#vars         ").append(vars.size()).append(System.lineSeparator());
        sb.append("#orderheap    ").append(orderHeap.size()).append(System.lineSeparator());
        sb.append("#trail        ").append(trail.size()).append(System.lineSeparator());
        sb.append("#trailLim     ").append(trailLim.size()).append(System.lineSeparator());

        sb.append("model         ").append(model).append(System.lineSeparator());
        sb.append("conflict      ").append(assumptionsConflict).append(System.lineSeparator());
        sb.append("assumptions   ").append(assumptions).append(System.lineSeparator());
        sb.append("#seen         ").append(seen.size()).append(System.lineSeparator());

        sb.append("claInc        ").append(claInc).append(System.lineSeparator());
        sb.append("#clause lits  ").append(clausesLiterals).append(System.lineSeparator());
        sb.append("#learnts lits ").append(learntsLiterals).append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * Class containing the information required for generating a proof.
     */
    public static class ProofInformation {
        protected final LNGIntVector clause;
        protected final Proposition proposition;

        /**
         * Constructor.
         * @param clause      the clause
         * @param proposition the proposition
         */
        public ProofInformation(final LNGIntVector clause, final Proposition proposition) {
            this.clause = clause;
            this.proposition = proposition;
        }

        /**
         * Returns the clause.
         * @return the clause
         */
        public LNGIntVector clause() {
            return clause;
        }

        /**
         * Returns the proposition.
         * @return the proposition
         */
        public Proposition proposition() {
            return proposition;
        }

        @Override
        public String toString() {
            return "ProofInformation{" +
                    "clause=" + clause +
                    ", proposition=" + proposition +
                    '}';
        }
    }

    /**
     * Returns the unit propagated literals on level zero.
     * @return unit propagated literal on level zero
     */
    public LNGIntVector upZeroLiterals() {
        final LNGIntVector upZeroLiterals = new LNGIntVector();
        for (int i = 0; i < trail.size(); ++i) {
            final int lit = trail.get(i);
            if (v(lit).level() > 0) {
                break;
            } else {
                upZeroLiterals.push(lit);
            }
        }
        return upZeroLiterals;
    }

    ///// Backbone Stuff /////

    /**
     * Computes the backbone of the given variables with respect to the formulas added to the solver.
     * @param variables variables to test
     * @param type      backbone type
     * @return the backbone projected to the relevant variables
     */
    public Backbone computeBackbone(final Collection<Variable> variables, final BackboneType type) {
        return computeBackbone(variables, type, null);
    }

    /**
     * Computes the backbone of the given variables with respect to the formulas added to the solver.
     * @param variables variables to test
     * @param type      backbone type
     * @param handler   the handler
     * @return the backbone projected to the relevant variables or {@code null} if the computation was aborted by the handler
     */
    public Backbone computeBackbone(final Collection<Variable> variables, final BackboneType type, final SATHandler handler) {
        final boolean sat = internalSolve(handler) == Tristate.TRUE;
        if (aborted(handler)) {
            return null;
        }
        if (sat) {
            computingBackbone = true;
            final List<Integer> relevantVarIndices = getRelevantVarIndices(variables);
            initBackboneDS(relevantVarIndices);
            computeBackbone(relevantVarIndices, type, handler);
            if (aborted(handler)) {
                return null;
            }
            final Backbone backbone = buildBackbone(variables, type);
            computingBackbone = false;
            return backbone;
        } else {
            return Backbone.unsatBackbone();
        }
    }

    /**
     * Returns a list of relevant variable indices. A relevant variable is known by the solver.
     * @param variables variables to convert and filter
     * @return list of relevant variable indices
     */
    protected List<Integer> getRelevantVarIndices(final Collection<Variable> variables) {
        final List<Integer> relevantVarIndices = new ArrayList<>(variables.size());
        for (final Variable var : variables) {
            final Integer idx = name2idx.get(var.name());
            // Note: Unknown variables are variables added to the solver yet. Thus, these are optional variables and can
            // be left out for the backbone computation.
            if (idx != null) {
                relevantVarIndices.add(idx);
            }
        }
        return relevantVarIndices;
    }

    /**
     * Initializes the internal solver state for backbones.
     * @param variables to test
     */
    protected void initBackboneDS(final List<Integer> variables) {
        backboneCandidates = new Stack<>();
        backboneAssumptions = new LNGIntVector(variables.size());
        backboneMap = new HashMap<>();
        for (final Integer var : variables) {
            backboneMap.put(var, UNDEF);
        }
    }

    /**
     * Computes the backbone for the given variables.
     * @param variables variables to test
     * @param type      the type of the backbone
     * @param handler   the handler
     */
    protected void computeBackbone(final List<Integer> variables, final BackboneType type, final SATHandler handler) {
        createInitialCandidates(variables, type);
        while (!backboneCandidates.isEmpty()) {
            final int lit = backboneCandidates.pop();
            final boolean sat = solveWithLit(lit, handler);
            if (aborted(handler)) {
                return;
            }
            if (sat) {
                refineUpperBound();
            } else {
                addBackboneLiteral(lit);
            }
        }
    }

    /**
     * Creates the initial candidate literals for the backbone computation.
     * @param variables variables to test
     * @param type      the type of the backbone
     */
    protected void createInitialCandidates(final List<Integer> variables, final BackboneType type) {
        for (final Integer var : variables) {
            if (isUPZeroLit(var)) {
                final int backboneLit = mkLit(var, !model.get(var));
                addBackboneLiteral(backboneLit);
            } else {
                final boolean modelPhase = model.get(var);
                if (isBothOrNegativeType(type) && !modelPhase || isBothOrPositiveType(type) && modelPhase) {
                    final int lit = mkLit(var, !modelPhase);
                    if (!isRotatable(lit)) {
                        backboneCandidates.add(lit);
                    }
                }
            }
        }
    }

    /**
     * Refines the upper bound by optional checks (UP zero literal, complement model literal, rotatable literal).
     */
    protected void refineUpperBound() {
        for (final Integer lit : new ArrayList<>(backboneCandidates)) {
            final int var = var(lit);
            if (isUPZeroLit(var)) {
                backboneCandidates.remove(lit);
                addBackboneLiteral(lit);
            } else if (model.get(var) == sign(lit)) {
                backboneCandidates.remove(lit);
            } else if (isRotatable(lit)) {
                backboneCandidates.remove(lit);
            }
        }
    }

    /**
     * Tests the given literal with the formula on the solver for satisfiability.
     * @param lit     literal to test
     * @param handler the handler
     * @return {@code true} if satisfiable, otherwise {@code false}
     */
    protected boolean solveWithLit(final int lit, final SATHandler handler) {
        backboneAssumptions.push(not(lit));
        final boolean sat = internalSolve(handler, backboneAssumptions) == Tristate.TRUE;
        backboneAssumptions.pop();
        return sat;
    }

    /**
     * Builds the backbone object from the computed backbone literals.
     * @param variables relevant variables
     * @param type      the type of the backbone
     * @return backbone
     */
    protected Backbone buildBackbone(final Collection<Variable> variables, final BackboneType type) {
        final SortedSet<Variable> posBackboneVars = isBothOrPositiveType(type) ? new TreeSet<>() : null;
        final SortedSet<Variable> negBackboneVars = isBothOrNegativeType(type) ? new TreeSet<>() : null;
        final SortedSet<Variable> optionalVars = isBothType(type) ? new TreeSet<>() : null;
        for (final Variable var : variables) {
            final Integer idx = name2idx.get(var.name());
            if (idx == null) {
                if (isBothType(type)) {
                    optionalVars.add(var);
                }
            } else {
                switch (backboneMap.get(idx)) {
                    case TRUE:
                        if (isBothOrPositiveType(type)) {
                            posBackboneVars.add(var);
                        }
                        break;
                    case FALSE:
                        if (isBothOrNegativeType(type)) {
                            negBackboneVars.add(var);
                        }
                        break;
                    case UNDEF:
                        if (isBothType(type)) {
                            optionalVars.add(var);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unknown tristate: " + backboneMap.get(idx));
                }
            }
        }
        return Backbone.satBackbone(posBackboneVars, negBackboneVars, optionalVars);
    }

    /**
     * Tests the given variable whether it is a unit propagated literal on level 0.
     * <p>
     * Assumption: The formula on the solver has successfully been tested to be satisfiable before.
     * @param var variable index to test
     * @return {@code true} if the variable is a unit propagated literal on level 0, otherwise {@code false}
     */
    protected boolean isUPZeroLit(final int var) {
        return vars.get(var).level() == 0;
    }

    /**
     * Tests the given literal whether it is unit in the given clause.
     * @param lit    literal to test
     * @param clause clause containing the literal
     * @return {@code true} if the literal is unit, {@code false} otherwise
     */
    protected boolean isUnit(final int lit, final LNGClause clause) {
        if (!clause.isAtMost()) {
            for (int i = 0; i < clause.size(); ++i) {
                final int clauseLit = clause.get(i);
                if (lit != clauseLit && model.get(var(clauseLit)) != sign(clauseLit)) {
                    return false;
                }
            }
            return true;
        } else {
            int countPos = 0;
            final int cardinality = clause.cardinality();
            for (int i = 0; i < clause.size(); ++i) {
                final int var = var(clause.get(i));
                if (var(lit) != var && model.get(var)) {
                    if (++countPos == cardinality) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Tests the given literal whether it is rotatable in the current model.
     * @param lit literal to test
     * @return {@code true} if the literal is rotatable, otherwise {@code false}
     */
    protected boolean isRotatable(final int lit) {
        // A rotatable literal MUST NOT be a unit propagated literal
        if (v(lit).reason() != null) {
            return false;
        }
        for (final LNGWatcher watcher : watchesBin.get(not(lit))) {
            if (isUnit(lit, watcher.clause())) {
                return false;
            }
        }
        // A rotatable literal MUST NOT be unit
        for (final LNGWatcher watcher : watches.get(not(lit))) {
            if (isUnit(lit, watcher.clause())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds the given literal to the backbone result and optionally adds the literal to the solver.
     * @param lit literal to add
     */
    protected void addBackboneLiteral(final int lit) {
        backboneMap.put(var(lit), sign(lit) ? Tristate.FALSE : Tristate.TRUE);
        backboneAssumptions.push(lit);
    }

    protected boolean isBothOrPositiveType(final BackboneType type) {
        return type == BackboneType.POSITIVE_AND_NEGATIVE || type == BackboneType.ONLY_POSITIVE;
    }

    protected boolean isBothOrNegativeType(final BackboneType type) {
        return type == BackboneType.POSITIVE_AND_NEGATIVE || type == BackboneType.ONLY_NEGATIVE;
    }

    protected boolean isBothType(final BackboneType type) {
        return type == BackboneType.POSITIVE_AND_NEGATIVE;
    }

    /**
     * Returns the clauses loaded on the solver.
     * @return the clauses loaded on the solver
     */
    public LNGVector<LNGClause> clauses() {
        return clauses;
    }

    /**
     * Returns the variables known by the solver.
     * @return the variables
     */
    public LNGVector<LNGVariable> variables() {
        return vars;
    }

    /**
     * Sets the variable's selection order that is used to solve the formula on the solver.
     * <p>
     * If a custom selection order is set, the solver will pick a variable from the custom order in order to branch on it during the search.
     * The given polarity in the selection order is used as assignment for the variable.
     * If all variables in the custom order are already assigned, the solver falls back to the activity based variable selection.
     * @param selectionOrder the custom selection order
     */
    public void setSelectionOrder(final List<? extends Literal> selectionOrder) {
        this.selectionOrder.clear();
        for (final Literal literal : selectionOrder) {
            final Integer var = name2idx.get(literal.name());
            if (var != null) {
                this.selectionOrder.push(mkLit(var, !literal.phase()));
            }
        }
    }

    public SATSolverConfig config() {
        return config;
    }
}
