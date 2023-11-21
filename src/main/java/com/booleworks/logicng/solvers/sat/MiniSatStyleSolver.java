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

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.datastructures.LNGHeap;
import com.booleworks.logicng.solvers.datastructures.MSClause;
import com.booleworks.logicng.solvers.datastructures.MSVariable;
import com.booleworks.logicng.solvers.datastructures.MSWatcher;

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
 * The super class for all MiniSAT-style solvers.
 * @version 2.1.0
 * @since 1.0
 */
public abstract class MiniSatStyleSolver {

    /**
     * The undefined literal
     */
    public static final int LIT_UNDEF = -1;

    // external solver configuration
    protected MiniSatConfig config;

    // internal solver state
    protected boolean ok;
    protected int qhead;
    protected LNGVector<MSClause> clauses;
    protected LNGVector<MSClause> learnts;
    protected LNGVector<LNGVector<MSWatcher>> watches;
    protected LNGVector<MSVariable> vars;
    protected LNGHeap orderHeap;
    protected LNGIntVector trail;
    protected LNGIntVector trailLim;
    protected LNGBooleanVector model;
    protected LNGIntVector conflict;
    protected LNGIntVector assumptions;
    protected LNGBooleanVector seen;
    protected int analyzeBtLevel;
    protected double claInc;
    protected int simpDBAssigns;
    protected int simpDBProps;
    protected int clausesLiterals;
    protected int learntsLiterals;

    // solver configuration
    protected double varDecay;
    protected double varInc;
    protected MiniSatConfig.ClauseMinimization ccminMode;
    protected int restartFirst;
    protected double restartInc;
    protected double clauseDecay;
    protected boolean shouldRemoveSatsisfied;
    protected double learntsizeFactor;
    protected double learntsizeInc;
    protected boolean incremental;

    // mapping of variable names to variable indices
    protected Map<String, Integer> name2idx;
    protected Map<Integer, String> idx2name;

    // SAT handler
    protected SATHandler handler;
    protected boolean canceledByHandler;

    // Proof generating information
    protected LNGVector<ProofInformation> pgOriginalClauses;
    protected LNGVector<LNGIntVector> pgProof;

    // backbone computation
    protected Stack<Integer> backboneCandidates;
    protected LNGIntVector backboneAssumptions;
    protected HashMap<Integer, Tristate> backboneMap;
    protected boolean computingBackbone;

    // Selection order
    protected LNGIntVector selectionOrder;
    protected int selectionOrderIdx;

    protected double learntsizeAdjustConfl;
    protected int learntsizeAdjustCnt;
    protected int learntsizeAdjustStartConfl;
    protected double learntsizeAdjustInc;
    protected double maxLearnts;

    /**
     * Constructs a new MiniSAT-style solver with a given configuration.
     * @param config the configuration
     */
    protected MiniSatStyleSolver(final MiniSatConfig config) {
        this.config = config;
        initialize();
    }

    /**
     * Returns the name-to-index mapping for variables.
     * @return the name-to-index mapping
     */
    public Map<String, Integer> getName2idx() {
        return name2idx;
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
     * Computes the next number in the Luby sequence.
     * @param y the restart increment
     * @param x the current number of restarts
     * @return the next number in the Luby sequence
     */
    protected static double luby(final double y, final int x) {
        int intX = x;
        int size = 1;
        int seq = 0;
        while (size < intX + 1) {
            seq++;
            size = 2 * size + 1;
        }
        while (size - 1 != intX) {
            size = (size - 1) >> 1;
            seq--;
            intX = intX % size;
        }
        return Math.pow(y, seq);
    }

    /**
     * Initializes the internal solver state.
     */
    protected void initialize() {
        initializeConfig();
        ok = true;
        qhead = 0;
        clauses = new LNGVector<>();
        learnts = new LNGVector<>();
        watches = new LNGVector<>();
        vars = new LNGVector<>();
        orderHeap = new LNGHeap(this);
        trail = new LNGIntVector();
        trailLim = new LNGIntVector();
        model = new LNGBooleanVector();
        conflict = new LNGIntVector();
        assumptions = new LNGIntVector();
        seen = new LNGBooleanVector();
        analyzeBtLevel = 0;
        claInc = 1;
        simpDBAssigns = -1;
        simpDBProps = 0;
        clausesLiterals = 0;
        learntsLiterals = 0;
        name2idx = new TreeMap<>();
        idx2name = new TreeMap<>();
        canceledByHandler = false;
        if (config.proofGeneration) {
            pgOriginalClauses = new LNGVector<>();
            pgProof = new LNGVector<>();
        }
        computingBackbone = false;
        selectionOrder = new LNGIntVector();
        selectionOrderIdx = 0;
    }

    /**
     * Initializes the solver configuration.
     */
    protected void initializeConfig() {
        varDecay = config.varDecay;
        varInc = config.varInc;
        ccminMode = config.clauseMin;
        restartFirst = config.restartFirst;
        restartInc = config.restartInc;
        clauseDecay = config.clauseDecay;
        shouldRemoveSatsisfied = config.removeSatisfied;
        learntsizeFactor = config.learntsizeFactor;
        learntsizeInc = config.learntsizeInc;
        incremental = config.incremental;
    }

    /**
     * Returns the variable for a given literal.
     * @param lit the literal
     * @return the variable of the literal
     */
    protected MSVariable v(final int lit) {
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
    public abstract int newVar(boolean sign, boolean dvar);

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
     */
    public abstract boolean addClause(final LNGIntVector ps, final Proposition proposition);

    /**
     * Solves the formula currently stored in the solver.  Returns {@link Tristate#TRUE} if the formula is satisfiable (SAT),
     * {@link Tristate#FALSE} if the formula is unsatisfiable (UNSAT), or {@link Tristate#UNDEF} if the computation was canceled
     * by a {@link SATHandler}.  If {@code null} is passed as handler, the solver will run until the satisfiability is decided.
     * @param handler a sat handler
     * @return {@link Tristate#TRUE} if the formula is satisfiable, {@link Tristate#FALSE} if the formula is not satisfiable, or
     * {@link Tristate#UNDEF} if the computation was canceled.
     */
    public abstract Tristate solve(final SATHandler handler);

    /**
     * Solves the formula currently stored in the solver together with the given assumption literals.  Returns
     * {@link Tristate#TRUE} if the formula and the assumptions are satisfiable (SAT), {@link Tristate#FALSE} if the formula and the
     * assumptions are not satisfiable together (UNSAT), or {@link Tristate#UNDEF} if the computation was canceled by a
     * {@link SATHandler}. If {@code null} is passed as handler, the solver will run until the satisfiability is decided.
     * @param handler     a sat handler
     * @param assumptions the assumptions as a given vector of literals
     * @return {@link Tristate#TRUE} if the formula and the assumptions are satisfiable, {@link Tristate#FALSE} if they are
     * not satisfiable, or {@link Tristate#UNDEF} if the computation was canceled.
     */
    public Tristate solve(final SATHandler handler, final LNGIntVector assumptions) {
        this.assumptions = new LNGIntVector(assumptions);
        final Tristate result = solve(handler);
        this.assumptions.clear();
        return result;
    }

    /**
     * Resets the solver state.
     */
    public abstract void reset();

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
     * Returns the current conflict of the solver or an empty vector if there is none.
     * @return the current conflict of the solver
     */
    public LNGIntVector conflict() {
        return conflict;
    }

    /**
     * Saves and returns the solver state expressed as an integer array which stores the length of the internal data
     * structures.
     * @return the current solver state
     * @throws UnsupportedOperationException if the solver does not support state saving/loading
     * @throws IllegalStateException         if the solver is not in incremental mode
     */
    public abstract int[] saveState();

    /**
     * Loads a given state in the solver.
     * <p>
     * ATTENTION: You can only load a state which was created by this instance of the solver before the current state.
     * Only the sizes of the internal data structures are stored, meaning you can track back in time and restore a solver
     * state with fewer variables and/or fewer clauses.  It is not possible to import a solver state from another solver
     * or another solving execution.
     * @param state the solver state to load
     * @throws UnsupportedOperationException if the solver does not support state saving/loading
     * @throws IllegalStateException         if the solver is not in incremental mode
     */
    public abstract void loadState(int[] state);

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
     * Returns the number of assigned variables.
     * @return the number of assigned variables
     */
    protected int nAssigns() {
        return trail.size();
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
                final MSVariable msVariable = vars.get(var);
                if (msVariable.assignment() == UNDEF) {
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
        final MSVariable var = vars.get(v);
        var.incrementActivity(inc);
        if (var.activity() > 1e100) {
            for (final MSVariable variable : vars) {
                variable.rescaleActivity();
            }
            varInc *= 1e-100;
        }
        if (orderHeap.inHeap(v)) {
            orderHeap.decrease(v);
        }
    }

    /**
     * Rebuilds the heap of decision variables.
     */
    protected void rebuildOrderHeap() {
        final LNGIntVector vs = new LNGIntVector();
        for (int v = 0; v < nVars(); v++) {
            if (vars.get(v).decision() && vars.get(v).assignment() == UNDEF) {
                vs.push(v);
            }
        }
        orderHeap.build(vs);
    }

    /**
     * Returns {@code true} if the given clause is locked and therefore cannot be removed, {@code false} otherwise.
     * @param c the clause
     * @return {@code true} if the given clause is locked
     */
    protected boolean locked(final MSClause c) {
        return value(c.get(0)) == Tristate.TRUE && v(c.get(0)).reason() != null && v(c.get(0)).reason() == c;
    }

    /**
     * Decays the clause activity increment by the clause decay factor.
     */
    protected void claDecayActivity() {
        claInc *= (1 / clauseDecay);
    }

    /**
     * Bumps the activity of the given clause.
     * @param c the clause
     */
    protected void claBumpActivity(final MSClause c) {
        c.incrementActivity(claInc);
        if (c.activity() > 1e20) {
            for (final MSClause clause : learnts) {
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
    protected abstract void uncheckedEnqueue(int lit, MSClause reason);

    /**
     * Attaches a given clause to the solver (i.e. the watchers for this clause are initialized).
     * @param c the clause
     */
    protected abstract void attachClause(final MSClause c);

    /**
     * Detaches a given clause (e.g. removes all watchers pointing to this clause).
     * @param c the clause
     */
    protected abstract void detachClause(final MSClause c);

    /**
     * Removes a given clause.
     * @param c the clause to remove
     */
    protected abstract void removeClause(final MSClause c);

    /**
     * Performs unit propagation.
     * @return the conflicting clause if a conflict arose during unit propagation or {@code null} if there was none
     */
    protected abstract MSClause propagate();

    /**
     * Returns {@code true} if a given literal is redundant in the current conflict analysis, {@code false} otherwise.
     * @param p              the literal
     * @param abstractLevels an abstraction of levels
     * @param analyzeToClear helper vector
     * @return {@code true} if a given literal is redundant in the current conflict analysis
     */
    protected abstract boolean litRedundant(int p, int abstractLevels, LNGIntVector analyzeToClear);

    /**
     * Analysis the final conflict if there were assumptions.
     * @param p           the conflicting literal
     * @param outConflict the vector to store the final conflict
     */
    protected abstract void analyzeFinal(int p, final LNGIntVector outConflict);

    protected void cancelUntil(final int level) {
        if (decisionLevel() > level) {
            if (!computingBackbone) {
                for (int c = trail.size() - 1; c >= trailLim.get(level); c--) {
                    final int x = var(trail.get(c));
                    final MSVariable v = vars.get(x);
                    v.assign(Tristate.UNDEF);
                    v.setPolarity(sign(trail.get(c)));
                    insertVarOrder(x);
                }
            } else {
                for (int c = trail.size() - 1; c >= trailLim.get(level); c--) {
                    final int x = var(trail.get(c));
                    final MSVariable v = vars.get(x);
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
    protected abstract void reduceDB();

    /**
     * Removes all clauses which are satisfied under the current assignment of a set of clauses.
     * @param cs the set of clauses
     */
    protected abstract void removeSatisfied(final LNGVector<MSClause> cs);

    /**
     * Returns {@code true} if a given clause is satisfied under the current assignment, {@code false} otherwise.
     * @param c the clause
     * @return {@code true} if a given clause is satisfied under the current assignment
     */
    protected abstract boolean satisfied(final MSClause c);

    /**
     * Simplifies the database of clauses.  This method is only executed on level 0.  All learnt clauses which are
     * satisfied on level 0 are removed.  Depending on the configuration of the solver, also original clauses which are
     * satisfied at level 0 are removed.
     * @return {@code true} if simplification was successful and no conflict was found, {@code false} if a conflict was
     * found during the simplification
     */
    protected abstract boolean simplify();

    protected void decayActivities() {
        varDecayActivity();
        if (!incremental) {
            claDecayActivity();
        }
        if (--learntsizeAdjustCnt == 0) {
            learntsizeAdjustConfl *= learntsizeAdjustInc;
            learntsizeAdjustCnt = (int) learntsizeAdjustConfl;
            maxLearnts *= learntsizeInc;
        }
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
        sb.append("conflict      ").append(conflict).append(System.lineSeparator());
        sb.append("assumptions   ").append(assumptions).append(System.lineSeparator());
        sb.append("#seen         ").append(seen.size()).append(System.lineSeparator());

        sb.append("claInc        ").append(claInc).append(System.lineSeparator());
        sb.append("simpDBAssigns ").append(simpDBAssigns).append(System.lineSeparator());
        sb.append("simpDBProps   ").append(simpDBProps).append(System.lineSeparator());
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
        final boolean sat = solve(handler) == Tristate.TRUE;
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
        final Stack<Integer> candidates = createInitialCandidates(variables, type);
        while (candidates.size() > 0) {
            final int lit = candidates.pop();
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
     * @return initial candidates
     */
    protected Stack<Integer> createInitialCandidates(final List<Integer> variables, final BackboneType type) {
        for (final Integer var : variables) {
            if (isUPZeroLit(var)) {
                final int backboneLit = mkLit(var, !model.get(var));
                addBackboneLiteral(backboneLit);
            } else {
                final boolean modelPhase = model.get(var);
                if (isBothOrNegativeType(type) && !modelPhase || isBothOrPositiveType(type) && modelPhase) {
                    final int lit = mkLit(var, !modelPhase);
                    if (!config.bbInitialUBCheckForRotatableLiterals || !isRotatable(lit)) {
                        backboneCandidates.add(lit);
                    }
                }
            }
        }
        return backboneCandidates;
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
            } else if (config.bbCheckForComplementModelLiterals && model.get(var) == sign(lit)) {
                backboneCandidates.remove(lit);
            } else if (config.bbCheckForRotatableLiterals && isRotatable(lit)) {
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
        final boolean sat = solve(handler, backboneAssumptions) == Tristate.TRUE;
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
    protected boolean isUnit(final int lit, final MSClause clause) {
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
        // A rotatable literal MUST NOT be unit
        for (final MSWatcher watcher : watches.get(not(lit))) {
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
    public LNGVector<MSClause> clauses() {
        return clauses;
    }

    /**
     * Returns the variables known by the solver.
     * @return the variables
     */
    public LNGVector<MSVariable> variables() {
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

    /**
     * Resets a previously set selection order.
     */
    public void resetSelectionOrder() {
        selectionOrder.clear();
    }

    public MiniSatConfig getConfig() {
        return config;
    }
}
