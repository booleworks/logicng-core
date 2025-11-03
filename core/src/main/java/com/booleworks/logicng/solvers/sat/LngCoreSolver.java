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

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.datastructures.LngBoundedIntQueue;
import com.booleworks.logicng.solvers.datastructures.LngBoundedLongQueue;
import com.booleworks.logicng.solvers.datastructures.LngClause;
import com.booleworks.logicng.solvers.datastructures.LngHeap;
import com.booleworks.logicng.solvers.datastructures.LngVariable;
import com.booleworks.logicng.solvers.datastructures.LngWatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import static com.booleworks.logicng.datastructures.Tristate.FALSE;
import static com.booleworks.logicng.datastructures.Tristate.TRUE;
import static com.booleworks.logicng.datastructures.Tristate.UNDEF;
import static com.booleworks.logicng.handlers.events.ComputationFinishedEvent.SAT_CALL_FINISHED;
import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.BACKBONE_COMPUTATION_STARTED;
import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.SAT_CALL_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.SAT_CONFLICT_DETECTED;

/**
 * The core SAT Solver of LogicNG. Heavily inspired by MiniSat, Glucose, and
 * MiniCard.
 * <p>
 * <b>This core solver should usually not be used directly. It exposes a lot of
 * methods which should only be called if you really require to tweak with the
 * internals of the SAT Solver. Using {@link SatSolver} will be sufficient in
 * almost all cases.</b>
 * @version 3.0.0
 * @since 3.0.0
 */
// TODO: cleanup and sort methods, add links (and licences?) to MiniSat,
// Glucose, and MiniCard
public class LngCoreSolver {

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
    protected SatSolverConfig config;
    protected SatSolverLowLevelConfig llConfig;
    protected boolean inSatCall;

    // mapping of variable names to variable indices
    protected Map<String, Integer> name2idx = new HashMap<>();
    protected Map<Integer, String> idx2name = new HashMap<>();

    // bookkeeping of solver states
    protected LngIntVector validStates = new LngIntVector();
    protected int nextStateId = 0;

    // internal solver state
    protected boolean ok = true;
    protected int qhead = 0;
    protected LngIntVector unitClauses = new LngIntVector();
    protected LngVector<LngClause> clauses = new LngVector<>();
    protected LngVector<LngClause> learnts = new LngVector<>();
    protected LngVector<LngVector<LngWatcher>> watches = new LngVector<>();
    protected LngVector<LngVariable> vars = new LngVector<>();
    protected LngHeap orderHeap = new LngHeap(this);
    protected LngIntVector trail = new LngIntVector();
    protected LngIntVector trailLim = new LngIntVector();
    protected LngBooleanVector model = new LngBooleanVector();
    protected LngIntVector assumptionsConflict = new LngIntVector();
    protected LngIntVector assumptions = new LngIntVector();
    protected LngVector<Proposition> assumptionPropositions = new LngVector<>();
    protected LngBooleanVector seen = new LngBooleanVector();
    protected int analyzeBtLevel = 0;
    protected double claInc = 1;
    protected double varInc;
    protected double varDecay;
    protected int clausesLiterals = 0;
    protected int learntsLiterals = 0;

    // Proof generating information
    protected LngVector<ProofInformation> pgOriginalClauses = new LngVector<>();
    protected LngVector<LngIntVector> pgProof = new LngVector<>();

    // backbone computation
    protected boolean computingBackbone = false;
    protected Stack<Integer> backboneCandidates;
    protected LngIntVector backboneAssumptions;
    protected HashMap<Integer, Tristate> backboneMap;

    // Selection order
    protected LngIntVector selectionOrder = new LngIntVector();
    protected int selectionOrderIdx = 0;

    // internal glucose-related state
    protected LngVector<LngVector<LngWatcher>> watchesBin = new LngVector<>();
    protected LngIntVector permDiff = new LngIntVector();
    protected LngIntVector lastDecisionLevel = new LngIntVector();
    protected LngBoundedLongQueue lbdQueue = new LngBoundedLongQueue();
    protected LngBoundedIntQueue trailQueue = new LngBoundedIntQueue();
    protected int myflag = 0;
    protected long analyzeLbd = 0;
    protected int nbClausesBeforeReduce;
    protected int conflicts = 0;
    protected int conflictsRestarts = 0;
    protected double sumLbd = 0;
    protected int curRestart = 1;

    /**
     * Constructs a new core solver with a given configuration and formula
     * factory.
     * @param f      the formula factory
     * @param config the configuration
     */
    public LngCoreSolver(final FormulaFactory f, final SatSolverConfig config) {
        this.f = f;
        this.config = config;
        llConfig = config.lowLevelConfig;
        varInc = llConfig.varInc;
        varDecay = llConfig.varDecay;
        lbdQueue.initSize(llConfig.sizeLbdQueue);
        trailQueue.initSize(llConfig.sizeTrailQueue);
        nbClausesBeforeReduce = llConfig.firstReduceDb;
    }

    /**
     * Generates a clause vector of a collection of literals for a given SAT
     * Solver and configuration. If variables are unknown, they will be added
     * with {@link LngCoreSolver#newVar} and the given initial phase and the
     * decision variable flag.
     * @param literals     the literals
     * @param solver       the internal solver
     * @param initialPhase the initial phase of new literals
     * @param decisionVar  whether the variable should be handled as decision
     *                     variable
     * @return the clause vector
     */
    public static LngIntVector generateClauseVector(final Collection<? extends Literal> literals,
                                                    final LngCoreSolver solver,
                                                    final boolean initialPhase, final boolean decisionVar) {
        final LngIntVector clauseVec = new LngIntVector(literals.size());
        for (final Literal lit : literals) {
            clauseVec.unsafePush(solverLiteral(lit, solver, initialPhase, decisionVar));
        }
        return clauseVec;
    }

    /**
     * Generates a clause vector of a collection of literals for a given SAT
     * Solver and configuration.
     * @param literals the literals
     * @param solver   the internal solver
     * @return the clause vector
     */
    public static LngIntVector generateClauseVector(final Collection<? extends Literal> literals,
                                                    final LngCoreSolver solver) {
        return generateClauseVector(literals, solver, solver.getConfig().getInitialPhase(), true);
    }

    /**
     * Returns or creates the internal index for the given literal on the given
     * solver. If it is unknown, it will be created with the given initial
     * phase.
     * @param lit          the literal
     * @param solver       the solver
     * @param initialPhase the initial phase if the variable is unknown
     * @param decisionVar  whether the variable should be handled as decision
     *                     variable
     * @return the internal index of the literal
     */
    public static int solverLiteral(final Literal lit, final LngCoreSolver solver, final boolean initialPhase,
                                    final boolean decisionVar) {
        int index = solver.idxForName(lit.getName());
        if (index == -1) {
            index = solver.newVar(!initialPhase, decisionVar);
            solver.addName(lit.getName(), index);
        }
        return lit.getPhase() ? index * 2 : (index * 2) ^ 1;
    }

    /**
     * Returns or creates the internal index for the given literal on the given
     * solver. If it is unknown, it will be created with the given initial phase
     * specified by the {@link SatSolverConfig#getInitialPhase() configuration of
     * the solver} and with {@code decisionVar == true}.
     * @param lit    the literal
     * @param solver the solver
     * @return the internal index of the literal
     */
    public static int solverLiteral(final Literal lit, final LngCoreSolver solver) {
        return solverLiteral(lit, solver, solver.config.initialPhase, true);
    }

    /**
     * Marks this solver to be used in a {@link SatCall}. Until
     * {@link #finishSatCall()} is called, additional calls to this method or
     * other operations on the SAT solver like adding new formulas, executing
     * solver functions, or saving/loading state, will fail with an
     * {@link IllegalStateException}.
     */
    protected void startSatCall() {
        assertNotInSatCall();
        inSatCall = true;
    }

    /**
     * Declares that the solver is not used anymore in a {@link SatCall}.
     */
    protected void finishSatCall() {
        inSatCall = false;
    }

    /**
     * Creates a literal for a given variable number and literal.
     * @param var  the variable number
     * @param sign {@code true} if the literal is negative, {@code false}
     *             otherwise
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
     * Returns {@code true} if a given literal is negated, {@code false}
     * otherwise.
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
    protected LngVariable v(final int lit) {
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
     * @return {@code true} if the first variable's activity is larger than the
     * second one's
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
     * @param sign the initial polarity of the new variable, {@code true} if
     *             negative, {@code false} if positive
     * @param dvar {@code true} if this variable can be used as a decision
     *             variable, {@code false} if it should not be used as a
     *             decision variable
     * @return the index of the new variable
     */
    public int newVar(final boolean sign, final boolean dvar) {
        final int v = nVars();
        final LngVariable newVar = new LngVariable(sign);
        vars.push(newVar);
        watches.push(new LngVector<>());
        watches.push(new LngVector<>());
        seen.push(false);
        watchesBin.push(new LngVector<>());
        watchesBin.push(new LngVector<>());
        permDiff.push(0);
        newVar.setDecision(dvar);
        insertVarOrder(v);
        return v;
    }

    /**
     * Adds a unit clause to the solver.
     * @param lit         the unit clause's literal
     * @param proposition a proposition (if required for proof tracing)
     */
    public void addClause(final int lit, final Proposition proposition) {
        final LngIntVector unit = new LngIntVector(1);
        unit.push(lit);
        addClause(unit, proposition);
    }

    /**
     * Adds a clause to the solver.
     * @param ps          the literals of the clause
     * @param proposition a proposition (if required for proof tracing)
     * @return {@code true} if the clause was added successfully, {@code false}
     * otherwise
     * @throws IllegalStateException if a {@link SatCall} is currently running
     *                               on this solver
     */
    public boolean addClause(final LngIntVector ps, final Proposition proposition) {
        assertNotInSatCall();
        assert decisionLevel() == 0;
        int p;
        int i;
        int j;
        if (config.proofGeneration) {
            final LngIntVector vec = new LngIntVector(ps.size());
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
        LngIntVector oc = null;
        if (config.proofGeneration) {
            oc = new LngIntVector();
            for (i = 0, p = LIT_UNDEF; i < ps.size(); i++) {
                oc.push(ps.get(i));
                if (value(ps.get(i)) == TRUE || ps.get(i) == not(p) || value(ps.get(i)) == FALSE) {
                    flag = true;
                }
            }
        }

        for (i = 0, j = 0, p = LIT_UNDEF; i < ps.size(); i++) {
            if (value(ps.get(i)) == TRUE || ps.get(i) == not(p)) {
                return true;
            } else if (value(ps.get(i)) != FALSE && ps.get(i) != p) {
                p = ps.get(i);
                ps.set(j++, p);
            }
        }
        ps.removeElements(i - j);

        if (flag) {
            LngIntVector vec = new LngIntVector(ps.size() + 1);
            vec.push(1);
            for (i = 0; i < ps.size(); i++) {
                vec.push((var(ps.get(i)) + 1) * (-2 * (sign(ps.get(i)) ? 1 : 0) + 1));
            }
            pgProof.push(vec);

            vec = new LngIntVector(oc.size() + 1);
            vec.push(-1);
            for (i = 0; i < oc.size(); i++) {
                vec.push((var(oc.get(i)) + 1) * (-2 * (sign(oc.get(i)) ? 1 : 0) + 1));
            }
            pgProof.push(vec);
        }

        if (ps.isEmpty()) {
            ok = false;
            if (config.proofGeneration) {
                pgProof.push(LngIntVector.of(0));
            }
            return false;
        } else if (ps.size() == 1) {
            uncheckedEnqueue(ps.get(0), null);
            ok = propagate() == null;
            unitClauses.push(ps.get(0));
            if (!ok && config.proofGeneration) {
                pgProof.push(LngIntVector.of(0));
            }
            return ok;
        } else {
            final LngClause c = new LngClause(ps, -1);
            clauses.push(c);
            attachClause(c);
        }
        return true;
    }

    /**
     * Solves the formula currently stored in the solver and returns whether
     * it is satisfiable or not. If the handler cancels the computation earlier,
     * a result with the respective cancel event is returned.
     * @param handler a handler
     * @return the result of the solve call
     */
    public LngResult<Boolean> internalSolve(final ComputationHandler handler) {
        if (!handler.shouldResume(SAT_CALL_STARTED)) {
            return LngResult.canceled(SAT_CALL_STARTED);
        }
        model.clear();
        assumptionsConflict.clear();
        if (!ok) {
            return LngResult.of(false);
        }
        LngResult<Tristate> status = LngResult.of(UNDEF);
        while (status.isSuccess() && status.getResult() == UNDEF) {
            status = search(handler);
        }

        if (!status.isSuccess()) {
            cancelUntil(0);
            return LngResult.canceled(status.getCancelCause());
        }

        final boolean result = status.getResult() == TRUE;

        if (config.proofGeneration && assumptions.isEmpty() && !result) {
            pgProof.push(LngIntVector.of(0));
        }

        if (result) {
            model = new LngBooleanVector(vars.size());
            for (final LngVariable v : vars) {
                model.push(v.assignment() == TRUE);
            }
        } else if (assumptionsConflict.isEmpty()) {
            ok = false;
        }
        cancelUntil(0);
        if (!handler.shouldResume(SAT_CALL_FINISHED)) {
            return LngResult.canceled(SAT_CALL_FINISHED);
        }
        return LngResult.of(result);
    }

    /**
     * Solves the formula currently stored in the solver together with the
     * given assumption literals and returns whether it is satisfiable or not.
     * If the handler cancels the computation earlier, a result with the
     * respective cancel event is returned.
     * @param handler     a handler
     * @param assumptions the assumptions as a given vector of literals
     * @return the result of the solve call
     */
    public LngResult<Boolean> internalSolve(final ComputationHandler handler, final LngIntVector assumptions) {
        this.assumptions = new LngIntVector(assumptions);
        final LngResult<Boolean> result = internalSolve(handler);
        this.assumptions.clear();
        return result;
    }

    /**
     * Returns the current model of the solver or an empty vector if there is
     * none.
     * @return the current model of the solver
     */
    public LngBooleanVector model() {
        return model;
    }

    /**
     * Returns {@code false} if this solver is known to be in a conflicting
     * state, otherwise {@code true}.
     * @return {@code false} if this solver is known to be in a conflicting
     * state, otherwise {@code true}
     */
    public boolean ok() {
        return ok;
    }

    /**
     * Returns the conflict of the solver or an empty vector if there is none.
     * @return the conflict of the solver
     */
    public LngIntVector assumptionsConflict() {
        return assumptionsConflict;
    }

    /**
     * Saves and returns the solver state.
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
     * ATTENTION: You can only load a state which was created by this instance
     * of the solver before the current state. Only the sizes of the internal
     * data structures are stored, meaning you can track back in time and
     * restore a solver state with fewer variables and/or fewer clauses. It is
     * not possible to import a solver state from another solver or another
     * solving execution.
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
            detachClause(clauses.get(i));
        }
        clauses.shrinkTo(newClausesSize);
        int newLearntsLength = 0;
        for (int i = 0; i < learnts.size(); i++) {
            final LngClause learnt = learnts.get(i);
            if (learnt.getLearntOnState() <= solverState.getId()) {
                learnts.set(newLearntsLength++, learnt);
            } else {
                detachClause(learnt);
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
     * Helper function used to maintain an abstraction of levels involved during
     * conflict analysis.
     * @param x a variable index
     * @return the abstraction of levels
     */
    protected int abstractLevel(final int x) {
        return 1 << (vars.get(x).level() & 31);
    }

    /**
     * Inserts a variable (given by its index) into the heap of decision
     * variables.
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
        if (!selectionOrder.isEmpty() && selectionOrderIdx < selectionOrder.size()) {
            while (selectionOrderIdx < selectionOrder.size()) {
                final int lit = selectionOrder.get(selectionOrderIdx++);
                final int var = var(lit);
                final LngVariable lngVariable = vars.get(var);
                if (lngVariable.assignment() == UNDEF) {
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
        final LngVariable var = vars.get(v);
        var.incrementActivity(inc);
        if (var.activity() > 1e100) {
            for (final LngVariable variable : vars) {
                variable.rescaleActivity();
            }
            varInc *= 1e-100;
        }
        if (orderHeap.inHeap(v)) {
            orderHeap.decrease(v);
        }
    }

    /**
     * Returns {@code true} if the given clause is locked and therefore cannot
     * be removed, {@code false} otherwise.
     * @param c the clause
     * @return {@code true} if the given clause is locked
     */
    protected boolean locked(final LngClause c) {
        return value(c.get(0)) == TRUE && v(c.get(0)).reason() != null && v(c.get(0)).reason() == c;
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
    protected void claBumpActivity(final LngClause c) {
        c.incrementActivity(claInc);
        if (c.activity() > 1e20) {
            for (final LngClause clause : learnts) {
                clause.rescaleActivity();
            }
            claInc *= 1e-20;
        }
    }

    /**
     * Assigns a literal (= a variable to the respective value).
     * @param lit    the literal
     * @param reason the reason clause of the assignment (conflict resolution)
     *               or {@code null} if it was a decision
     */
    protected void uncheckedEnqueue(final int lit, final LngClause reason) {
        assert value(lit) == UNDEF;
        final LngVariable var = v(lit);
        var.assign(Tristate.fromBool(!sign(lit)));
        var.setReason(reason);
        var.setLevel(decisionLevel());
        trail.push(lit);
    }

    /**
     * Attaches a given clause to the solver (i.e. the watchers for this clause
     * are initialized).
     * @param c the clause
     */
    protected void attachClause(final LngClause c) {
        if (c.isAtMost()) {
            for (int i = 0; i < c.atMostWatchers(); i++) {
                final int l = c.get(i);
                watches.get(l).push(new LngWatcher(c, LIT_UNDEF));
            }
            clausesLiterals += c.size();
        } else {
            assert c.size() > 1;
            if (c.size() == 2) {
                watchesBin.get(not(c.get(0))).push(new LngWatcher(c, c.get(1)));
                watchesBin.get(not(c.get(1))).push(new LngWatcher(c, c.get(0)));
            } else {
                watches.get(not(c.get(0))).push(new LngWatcher(c, c.get(1)));
                watches.get(not(c.get(1))).push(new LngWatcher(c, c.get(0)));
            }
            if (c.learnt()) {
                learntsLiterals += c.size();
            } else {
                clausesLiterals += c.size();
            }
        }
    }

    /**
     * Detaches a given clause (e.g. removes all watchers pointing to this
     * clause).
     * @param c the clause
     */
    protected void detachClause(final LngClause c) {
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
    protected void removeClause(final LngClause c) {
        assert !c.isAtMost();
        if (config.proofGeneration) {
            final LngIntVector vec = new LngIntVector(c.size());
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

    /**
     * Performs unit propagation.
     * @return the conflicting clause if a conflict arose during unit
     * propagation or {@code null} if there was none
     */
    protected LngClause propagate() {
        LngClause confl = null;
        while (qhead < trail.size()) {
            final int p = trail.get(qhead++);
            final LngVector<LngWatcher> ws = watches.get(p);
            int iInd = 0;
            int jInd = 0;
            final LngVector<LngWatcher> wbin = watchesBin.get(p);
            for (int k = 0; k < wbin.size(); k++) {
                final int imp = wbin.get(k).blocker();
                if (value(imp) == FALSE) {
                    return wbin.get(k).clause();
                }
                if (value(imp) == UNDEF) {
                    uncheckedEnqueue(imp, wbin.get(k).clause());
                }
            }
            while (iInd < ws.size()) {
                final LngWatcher i = ws.get(iInd);
                final int blocker = i.blocker();
                if (blocker != LIT_UNDEF && value(blocker) == TRUE) {
                    ws.set(jInd++, i);
                    iInd++;
                    continue;
                }
                final LngClause c = i.clause();

                if (c.isAtMost()) {
                    final int newWatch = findNewWatchForAtMostClause(c, p);
                    if (newWatch == LIT_UNDEF) {
                        for (int k = 0; k < c.atMostWatchers(); k++) {
                            if (c.get(k) != p && value(c.get(k)) != FALSE) {
                                assert value(c.get(k)) == UNDEF || value(c.get(k)) == FALSE;
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
                        final LngWatcher w = new LngWatcher(c, LIT_UNDEF);
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
                    final LngWatcher w = new LngWatcher(c, first);
                    if (first != blocker && value(first) == TRUE) {
                        ws.set(jInd++, w);
                        continue;
                    }
                    boolean foundWatch = false;
                    for (int k = 2; k < c.size() && !foundWatch; k++) {
                        if (value(c.get(k)) != FALSE) {
                            c.set(1, c.get(k));
                            c.set(k, falseLit);
                            watches.get(not(c.get(1))).push(w);
                            foundWatch = true;
                        }
                    }
                    if (!foundWatch) {
                        ws.set(jInd++, w);
                        if (value(first) == FALSE) {
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
     * Returns {@code true} if a given literal is redundant in the current
     * conflict analysis, {@code false} otherwise.
     * @param p              the literal
     * @param abstractLevels an abstraction of levels
     * @param analyzeToClear helper vector
     * @return {@code true} if a given literal is redundant in the current
     * conflict analysis
     */
    protected boolean litRedundant(final int p, final int abstractLevels, final LngIntVector analyzeToClear) {
        final LngIntVector analyzeStack = new LngIntVector();
        analyzeStack.push(p);
        final int top = analyzeToClear.size();
        while (!analyzeStack.isEmpty()) {
            assert v(analyzeStack.back()).reason() != null;
            final LngClause c = v(analyzeStack.back()).reason();
            analyzeStack.pop();
            if (c.isAtMost()) {
                for (int i = 0; i < c.size(); i++) {
                    if (value(c.get(i)) != TRUE) {
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
                if (c.size() == 2 && value(c.get(0)) == FALSE) {
                    assert value(c.get(1)) == TRUE;
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
        LngVariable v;
        for (int i = trail.size() - 1; i >= trailLim.get(0); i--) {
            x = var(trail.get(i));
            if (seen.get(x)) {
                v = vars.get(x);
                if (v.reason() == null) {
                    assert v.level() > 0;
                    assumptionsConflict.push(not(trail.get(i)));
                } else {
                    final LngClause c = v.reason();
                    if (!c.isAtMost()) {
                        for (int j = c.size() == 2 ? 0 : 1; j < c.size(); j++) {
                            if (v(c.get(j)).level() > 0) {
                                seen.set(var(c.get(j)), true);
                            }
                        }
                    } else {
                        for (int j = 0; j < c.size(); j++) {
                            if (value(c.get(j)) == TRUE && v(c.get(j)).level() > 0) {
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
            for (int c = trail.size() - 1; c >= trailLim.get(level); c--) {
                final int x = var(trail.get(c));
                final LngVariable v = vars.get(x);
                v.assign(UNDEF);
                v.setReason(null);
                v.setPolarity(!computingBackbone && sign(trail.get(c)));
                insertVarOrder(x);
            }
            qhead = trailLim.get(level);
            trail.removeElements(trail.size() - trailLim.get(level));
            trailLim.removeElements(trailLim.size() - level);
        }
    }

    /**
     * Reduces the database of learnt clauses. Only clauses of the first half of
     * the clauses with the most activity are possibly removed. A clause is only
     * removed if it is not locked, i.e. is the reason of an assignment for a
     * variable.
     */
    protected void reduceDb() {
        int i;
        int j;
        learnts.manualSort(LngClause.glucoseComparator);
        if (learnts.get(learnts.size() / RATIO_REMOVE_CLAUSES).lbd() <= 3) {
            nbClausesBeforeReduce += llConfig.specialIncReduceDb;
        }
        if (learnts.back().lbd() <= 5) {
            nbClausesBeforeReduce += llConfig.specialIncReduceDb;
        }
        int limit = learnts.size() / 2;
        for (i = j = 0; i < learnts.size(); i++) {
            final LngClause c = learnts.get(i);
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
    public LngVector<ProofInformation> pgOriginalClauses() {
        return pgOriginalClauses;
    }

    /**
     * Returns the proof clauses for proof generation.
     * @return the proof clauses for proof generation
     */
    public LngVector<LngIntVector> pgProof() {
        return pgProof;
    }

    /**
     * The main search procedure of the CDCL algorithm.
     * @return a {@link Tristate} representing the result. {@code FALSE} if the
     * formula is UNSAT, {@code TRUE} if the formula is SAT, and
     * {@code UNDEF} if the state is not known yet (restart) or the
     * handler canceled the computation
     */
    protected LngResult<Tristate> search(final ComputationHandler handler) {
        if (!ok) {
            return LngResult.of(FALSE);
        }
        final LngIntVector learntClause = new LngIntVector();
        selectionOrderIdx = 0;
        while (true) {
            final LngClause confl = propagate();
            if (confl != null) {
                if (!handler.shouldResume(SAT_CONFLICT_DETECTED)) {
                    return LngResult.canceled(SAT_CONFLICT_DETECTED);
                }
                conflicts++;
                conflictsRestarts++;
                if (conflicts % 5000 == 0 && varDecay < llConfig.maxVarDecay) {
                    varDecay += 0.01;
                }
                if (decisionLevel() == 0) {
                    return LngResult.of(FALSE);
                }
                trailQueue.push(trail.size());
                if (conflictsRestarts > LB_BLOCKING_RESTART && lbdQueue.valid() &&
                        trail.size() > llConfig.factorR * trailQueue.avg()) {
                    lbdQueue.fastClear();
                }
                learntClause.clear();
                analyze(confl, learntClause);
                lbdQueue.push(analyzeLbd);
                sumLbd += analyzeLbd;
                cancelUntil(analyzeBtLevel);
                if (analyzeBtLevel < selectionOrder.size()) {
                    selectionOrderIdx = analyzeBtLevel;
                }

                if (config.proofGeneration) {
                    final LngIntVector vec = new LngIntVector(learntClause.size() + 1);
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
                    final LngClause cr = new LngClause(learntClause, nextStateId);
                    cr.setLbd(analyzeLbd);
                    cr.setOneWatched(false);
                    learnts.push(cr);
                    attachClause(cr);
                    claBumpActivity(cr);
                    uncheckedEnqueue(learntClause.get(0), cr);
                }
                varDecayActivity();
                claDecayActivity();
            } else {
                if (lbdQueue.valid() && (lbdQueue.avg() * llConfig.factorK) > (sumLbd / conflictsRestarts)) {
                    lbdQueue.fastClear();
                    cancelUntil(0);
                    return LngResult.of(UNDEF);
                }
                if (conflicts >= (curRestart * nbClausesBeforeReduce) && !learnts.isEmpty()) {
                    curRestart = (conflicts / nbClausesBeforeReduce) + 1;
                    reduceDb();
                    nbClausesBeforeReduce += llConfig.incReduceDb;
                }
                int next = LIT_UNDEF;
                while (decisionLevel() < assumptions.size()) {
                    final int p = assumptions.get(decisionLevel());
                    if (value(p) == TRUE) {
                        trailLim.push(trail.size());
                    } else if (value(p) == FALSE) {
                        if (config.proofGeneration) {
                            final int drupLit = (var(p) + 1) * (-2 * (sign(p) ? 1 : 0) + 1);
                            pgOriginalClauses.push(new ProofInformation(LngIntVector.of(drupLit),
                                    assumptionPropositions.get(decisionLevel())));
                        }
                        analyzeAssumptionConflict(not(p));
                        return LngResult.of(FALSE);
                    } else {
                        if (config.proofGeneration) {
                            final int drupLit = (var(p) + 1) * (-2 * (sign(p) ? 1 : 0) + 1);
                            pgOriginalClauses.push(new ProofInformation(LngIntVector.of(drupLit),
                                    assumptionPropositions.get(decisionLevel())));
                        }
                        next = p;
                        break;
                    }
                }
                if (next == LIT_UNDEF) {
                    next = pickBranchLit();
                    if (next == LIT_UNDEF) {
                        return LngResult.of(TRUE);
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
    protected void analyze(final LngClause conflictClause, final LngIntVector outLearnt) {
        LngClause c = conflictClause;
        int pathC = 0;
        int p = LIT_UNDEF;
        outLearnt.push(-1);
        int index = trail.size() - 1;
        do {
            assert c != null;
            if (c.isAtMost()) {
                for (int j = 0; j < c.size(); j++) {
                    if (value(c.get(j)) != TRUE) {
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
                if (p != LIT_UNDEF && c.size() == 2 && value(c.get(0)) == FALSE) {
                    assert value(c.get(1)) == TRUE;
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
                    final long nblevels = computeLbd(c);
                    if (nblevels + 1 < c.lbd()) {
                        if (c.lbd() <= llConfig.lbLbdFrozenClause) {
                            c.setCanBeDel(false);
                        }
                        c.setLbd(nblevels);
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
        simplifyClause(outLearnt);
    }

    /**
     * Minimizes a given learnt clause depending on the minimization method of
     * the solver configuration.
     * @param outLearnt the learnt clause which should be minimized
     */
    protected void simplifyClause(final LngIntVector outLearnt) {
        int i;
        int j;
        final LngIntVector analyzeToClear = new LngIntVector(outLearnt);
        if (config.clauseMinimization == SatSolverConfig.ClauseMinimization.DEEP) {
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
        } else if (config.clauseMinimization == SatSolverConfig.ClauseMinimization.BASIC) {
            for (i = j = 1; i < outLearnt.size(); i++) {
                final LngClause c = v(outLearnt.get(i)).reason();
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
        analyzeLbd = computeLbd(outLearnt);
        for (int k = 0; k < lastDecisionLevel.size(); k++) {
            if ((v(lastDecisionLevel.get(k)).reason()).lbd() < analyzeLbd) {
                varBumpActivity(var(lastDecisionLevel.get(k)));
            }
        }
        lastDecisionLevel.clear();
        for (int l = 0; l < analyzeToClear.size(); l++) {
            seen.set(var(analyzeToClear.get(l)), false);
        }
    }

    /**
     * Computes the LBD for a given clause
     * @param c the clause
     * @return the LBD
     */
    protected long computeLbd(final LngClause c) {
        return computeLbd(c.getData());
    }

    /**
     * Computes the LBD for a given vector of literals.
     * @param lits the vector of literals
     * @return the LBD
     */
    protected long computeLbd(final LngIntVector lits) {
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
    protected void minimisationWithBinaryResolution(final LngIntVector outLearnt) {
        final long lbd = computeLbd(outLearnt);
        int p = not(outLearnt.get(0));
        if (lbd <= llConfig.lbLbdMinimizingClause) {
            myflag++;
            for (int i = 1; i < outLearnt.size(); i++) {
                permDiff.set(var(outLearnt.get(i)), myflag);
            }
            int nb = 0;
            for (final LngWatcher wbin : watchesBin.get(p)) {
                final int imp = wbin.blocker();
                if (permDiff.get(var(imp)) == myflag && value(imp) == TRUE) {
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
            final LngVariable var = vars.get(v);
            var.assign(UNDEF);
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
    protected void simpleRemoveClause(final LngClause c) {
        if (c.isAtMost()) {
            for (int i = 0; i < c.atMostWatchers(); i++) {
                watches.get(c.get(i)).remove(new LngWatcher(c, c.get(i)));
            }
        } else if (c.size() == 2) {
            watchesBin.get(not(c.get(0))).remove(new LngWatcher(c, c.get(1)));
            watchesBin.get(not(c.get(1))).remove(new LngWatcher(c, c.get(0)));
        } else {
            watches.get(not(c.get(0))).remove(new LngWatcher(c, c.get(1)));
            watches.get(not(c.get(1))).remove(new LngWatcher(c, c.get(0)));
        }
    }

    /**
     * Adds an at-most k constraint.
     * @param ps  the literals of the constraint
     * @param rhs the right-hand side of the constraint
     */
    public void addAtMost(final LngIntVector ps, final int rhs) {
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
            if (value(ps.get(i)) == TRUE) {
                k--;
            } else if (ps.get(i) == not(p)) {
                p = ps.get(i);
                j--;
                k--;
            } else if (value(ps.get(i)) != FALSE) {
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
        final LngClause cr = new LngClause(ps, -1, true);
        cr.setAtMostWatchers(ps.size() - k + 1);
        clauses.push(cr);
        attachClause(cr);
    }

    protected int findNewWatchForAtMostClause(final LngClause c, final int p) {
        assert c.isAtMost();
        int numFalse = 0;
        int numTrue = 0;
        final int maxTrue = c.size() - c.atMostWatchers() + 1;
        for (int q = 0; q < c.atMostWatchers(); q++) {
            switch (value(c.get(q))) {
                case UNDEF:
                    continue;
                case FALSE:
                    numFalse++;
                    if (numFalse >= c.atMostWatchers() - 1) {
                        return p;
                    }
                    continue;
                case TRUE:
                    numTrue++;
                    if (numTrue > maxTrue) {
                        return LIT_ERROR;
                    }
                    if (c.get(q) == p) {
                        for (int next = c.atMostWatchers(); next < c.size(); next++) {
                            if (value(c.get(next)) != TRUE) {
                                final int newWatch = c.get(next);
                                c.set(next, c.get(q));
                                c.set(q, newWatch);
                                return newWatch;
                            }
                        }
                    }
            }
        }
        return numTrue > 1 ? LIT_ERROR : LIT_UNDEF;
    }

    /**
     * Converts the internal model into a list of literals, considering only the
     * variables with the relevant indices.
     * @param internalModel   the internal model (e.g. from {@link #model()}
     * @param relevantIndices the indices of the relevant variables
     * @return the external model
     */
    public List<Literal> convertInternalModel(final LngBooleanVector internalModel,
                                              final LngIntVector relevantIndices) {
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
     * Returns {@code true} if a {@link SatCall} is currently using this solver,
     * otherwise {@code false}.
     * @return {@code true} if a {@link SatCall} is currently using this solver,
     * otherwise {@code false}
     */
    public boolean inSatCall() {
        return inSatCall;
    }

    /**
     * Checks if this solver is currently used in a {@link SatCall} and throws
     * an {@link IllegalStateException} in this case. Otherwise, nothing
     * happens.
     * @throws IllegalStateException if this solver is currently used in a SAT
     *                               call
     */
    public void assertNotInSatCall() {
        if (inSatCall) {
            throw new IllegalStateException(
                    "This operation is not allowed because a SAT call is running on this solver!");
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
        protected final LngIntVector clause;
        protected final Proposition proposition;

        /**
         * Constructor.
         * @param clause      the clause
         * @param proposition the proposition
         */
        public ProofInformation(final LngIntVector clause, final Proposition proposition) {
            this.clause = clause;
            this.proposition = proposition;
        }

        /**
         * Returns the clause.
         * @return the clause
         */
        public LngIntVector getClause() {
            return clause;
        }

        /**
         * Returns the proposition.
         * @return the proposition
         */
        public Proposition getProposition() {
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
    public LngIntVector upZeroLiterals() {
        final LngIntVector upZeroLiterals = new LngIntVector();
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
     * Computes the backbone of the given variables with respect to the formulas
     * added to the solver.
     * @param variables variables to test
     * @param type      backbone type
     * @return the backbone projected to the relevant variables
     */
    public LngResult<Backbone> computeBackbone(final Collection<Variable> variables, final BackboneType type) {
        return computeBackbone(variables, type, NopHandler.get());
    }

    /**
     * Computes the backbone of the given variables with respect to the formulas
     * added to the solver.
     * @param variables variables to test
     * @param type      backbone type
     * @param handler   the handler
     * @return the backbone projected to the relevant variables or {@code null}
     * if the computation was canceled by the handler
     */
    public LngResult<Backbone> computeBackbone(final Collection<Variable> variables, final BackboneType type,
                                               final ComputationHandler handler) {
        if (!handler.shouldResume(BACKBONE_COMPUTATION_STARTED)) {
            return LngResult.canceled(BACKBONE_COMPUTATION_STARTED);
        }
        final SolverState stateBeforeBackbone = saveState();
        final LngResult<Boolean> solveResult = internalSolve(handler);
        final LngResult<Backbone> result;
        if (!solveResult.isSuccess()) {
            result = LngResult.canceled(solveResult.getCancelCause());
        } else if (solveResult.getResult()) {
            computingBackbone = true;
            final List<Integer> relevantVarIndices = getRelevantVarIndices(variables);
            initBackboneDS(relevantVarIndices);
            final LngEvent backboneEvent = computeBackbone(relevantVarIndices, type, handler);
            if (backboneEvent != null) {
                result = LngResult.canceled(backboneEvent);
            } else {
                final Backbone backbone = buildBackbone(variables, type);
                result = LngResult.of(backbone);
            }
            computingBackbone = false;
        } else {
            result = LngResult.of(Backbone.unsatBackbone());
        }
        loadState(stateBeforeBackbone);
        return result;
    }

    /**
     * Returns a list of relevant variable indices. A relevant variable is known
     * by the solver.
     * @param variables variables to convert and filter
     * @return list of relevant variable indices
     */
    protected List<Integer> getRelevantVarIndices(final Collection<Variable> variables) {
        final List<Integer> relevantVarIndices = new ArrayList<>(variables.size());
        for (final Variable var : variables) {
            final Integer idx = name2idx.get(var.getName());
            // Note: Unknown variables are variables added to the solver yet.
            // Thus, these are optional variables and can
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
        backboneAssumptions = new LngIntVector(variables.size());
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
    protected LngEvent computeBackbone(final List<Integer> variables, final BackboneType type,
                                       final ComputationHandler handler) {
        createInitialCandidates(variables, type);
        while (!backboneCandidates.isEmpty()) {
            final int lit = backboneCandidates.pop();
            final LngResult<Boolean> satResult = solveWithLit(lit, handler);
            if (!satResult.isSuccess()) {
                return satResult.getCancelCause();
            } else if (satResult.getResult()) {
                refineUpperBound();
            } else {
                addBackboneLiteral(lit);
            }
        }
        return null;
    }

    /**
     * Creates the initial candidate literals for the backbone computation.
     * @param variables variables to test
     * @param type      the type of the backbone
     */
    protected void createInitialCandidates(final List<Integer> variables, final BackboneType type) {
        for (final Integer var : variables) {
            if (isUpZeroLit(var)) {
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
     * Refines the upper bound by optional checks (UP zero literal, complement
     * model literal, rotatable literal).
     */
    protected void refineUpperBound() {
        for (final Integer lit : new ArrayList<>(backboneCandidates)) {
            final int var = var(lit);
            if (isUpZeroLit(var)) {
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
     * Tests the given literal with the formula on the solver for
     * satisfiability.
     * @param lit     literal to test
     * @param handler the handler
     * @return {@code true} if satisfiable, otherwise {@code false}
     */
    protected LngResult<Boolean> solveWithLit(final int lit, final ComputationHandler handler) {
        backboneAssumptions.push(not(lit));
        final LngResult<Boolean> result = internalSolve(handler, backboneAssumptions);
        backboneAssumptions.pop();
        return result;
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
            final Integer idx = name2idx.get(var.getName());
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
     * Tests the given variable whether it is a unit propagated literal on level
     * 0.
     * <p>
     * Assumption: The formula on the solver has successfully been tested to be
     * satisfiable before.
     * @param var variable index to test
     * @return {@code true} if the variable is a unit propagated literal on
     * level 0, otherwise {@code false}
     */
    protected boolean isUpZeroLit(final int var) {
        return vars.get(var).level() == 0;
    }

    /**
     * Tests the given literal whether it is unit in the given clause.
     * @param lit    literal to test
     * @param clause clause containing the literal
     * @return {@code true} if the literal is unit, {@code false} otherwise
     */
    protected boolean isUnit(final int lit, final LngClause clause) {
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
        for (final LngWatcher watcher : watchesBin.get(not(lit))) {
            if (isUnit(lit, watcher.clause())) {
                return false;
            }
        }
        // A rotatable literal MUST NOT be unit
        for (final LngWatcher watcher : watches.get(not(lit))) {
            if (isUnit(lit, watcher.clause())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds the given literal to the backbone result and optionally adds the
     * literal to the solver.
     * @param lit literal to add
     */
    protected void addBackboneLiteral(final int lit) {
        backboneMap.put(var(lit), sign(lit) ? FALSE : TRUE);
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
    public LngVector<LngClause> getClauses() {
        return clauses;
    }

    /**
     * Returns the variables known by the solver.
     * @return the variables
     */
    public LngVector<LngVariable> getVariables() {
        return vars;
    }

    /**
     * Sets the variable's selection order that is used to solve the formula on
     * the solver.
     * <p>
     * If a custom selection order is set, the solver will pick a variable from
     * the custom order in order to branch on it during the search. The given
     * polarity in the selection order is used as assignment for the variable.
     * If all variables in the custom order are already assigned, the solver
     * falls back to the activity based variable selection.
     * @param selectionOrder the custom selection order
     */
    public void setSelectionOrder(final List<? extends Literal> selectionOrder) {
        this.selectionOrder.clear();
        for (final Literal literal : selectionOrder) {
            final Integer var = name2idx.get(literal.getName());
            if (var != null) {
                this.selectionOrder.push(mkLit(var, !literal.getPhase()));
            }
        }
    }

    public SatSolverConfig getConfig() {
        return config;
    }
}
