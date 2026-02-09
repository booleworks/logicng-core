// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.maxsat.algorithms;

import static com.booleworks.logicng.handlers.events.ComputationFinishedEvent.MAX_SAT_CALL_FINISHED;
import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.MAX_SAT_CALL_STARTED;
import static com.booleworks.logicng.solvers.MaxSatSolver.SEL_PREFIX;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Verbosity;
import static com.booleworks.logicng.solvers.sat.LngCoreSolver.LIT_UNDEF;

import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.handlers.events.MaxSatNewLowerBoundEvent;
import com.booleworks.logicng.handlers.events.MaxSatNewUpperBoundEvent;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.datastructures.LngHardClause;
import com.booleworks.logicng.solvers.datastructures.LngSoftClause;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Super class for the MaxSAT solvers.
 * @version 3.0.0
 * @since 1.0
 */
public abstract class MaxSat {
    protected static final MaxSatResult UNSAT = new MaxSatResult(false, -1, -1, null);

    /**
     * The MaxSAT problem type: {@code UNWEIGHTED} or {@code WEIGHTED}.
     */
    public enum ProblemType {
        UNWEIGHTED,
        WEIGHTED
    }

    protected final FormulaFactory f;
    protected Encoder encoder;
    protected final MaxSatConfig config;
    protected final MaxSatConfig.IncrementalStrategy incrementalStrategy;
    protected final PrintStream output;
    protected final Verbosity verbosity;
    protected final LngBooleanVector model;
    protected SortedMap<Variable, Integer> var2index;
    protected SortedMap<Integer, Variable> index2var;
    protected final LngVector<LngSoftClause> softClauses;
    protected final LngVector<LngHardClause> hardClauses;
    protected final LngIntVector orderWeights;
    protected final int hardWeight;
    protected ProblemType problemType;
    protected int nbVars;
    protected int nbInitialVariables;
    protected int nbCores;
    protected int nbSymmetryClauses;
    protected long sumSizeCores;
    protected int nbSatisfiable;
    protected int totalSoftWeight;
    protected int ubCost;
    protected int lbCost;
    protected int currentWeight;
    protected MaxSat.Stats lastStats;

    // bookkeeping of solver states
    protected LngIntVector validStates;
    protected int nextStateId;

    /**
     * Constructor.
     * @param f      the formula factory
     * @param config the solver configuration
     */
    protected MaxSat(final FormulaFactory f, final MaxSatConfig config) {
        this.f = f;
        this.config = config;
        output = config.output;
        verbosity = config.verbosity;
        incrementalStrategy = config.incrementalStrategy;
        var2index = new TreeMap<>();
        index2var = new TreeMap<>();
        hardClauses = new LngVector<>();
        softClauses = new LngVector<>();
        hardWeight = Integer.MAX_VALUE;
        problemType = ProblemType.UNWEIGHTED;
        nbVars = 0;
        nbInitialVariables = 0;
        currentWeight = 1;
        model = new LngBooleanVector();
        totalSoftWeight = 0;
        ubCost = 0;
        lbCost = 0;
        nbSymmetryClauses = 0;
        nbCores = 0;
        nbSatisfiable = 0;
        sumSizeCores = 0;
        orderWeights = new LngIntVector();
        validStates = new LngIntVector();
        nextStateId = 0;
    }

    /**
     * Creates a new variable in the SAT solver.
     * @param s the SAT solver
     */
    public static void newSatVariable(final LngCoreSolver s) {
        s.newVar(true, true);
    }

    /**
     * Solves the formula that is currently loaded in the SAT solver with a set
     * of assumptions.
     * @param s           the SAT solver
     * @param handler     the handler
     * @param assumptions the assumptions
     * @return the result of the solving process
     */
    public static LngResult<Boolean> searchSatSolver(final LngCoreSolver s, final ComputationHandler handler,
                                                     final LngIntVector assumptions) {
        return s.internalSolve(handler, assumptions);
    }

    /**
     * Solves the formula without assumptions.
     * @param s       the SAT solver
     * @param handler the handler
     * @return the result of the solving process
     */
    public static LngResult<Boolean> searchSatSolver(final LngCoreSolver s, final ComputationHandler handler) {
        return s.internalSolve(handler);
    }

    /**
     * The main MaxSAT solving method.
     * @param handler a MaxSAT handler
     * @return the result of the solving process
     * @throws IllegalArgumentException if the configuration was not valid
     */
    public final LngResult<MaxSatResult> search(final ComputationHandler handler) {
        if (!handler.shouldResume(MAX_SAT_CALL_STARTED)) {
            return LngResult.canceled(MAX_SAT_CALL_STARTED);
        }
        final MaxSatState stateBeforeSolving = saveState();
        final LngResult<MaxSatResult> result = internalSearch(handler);
        if (!handler.shouldResume(MAX_SAT_CALL_FINISHED)) {
            return LngResult.canceled(MAX_SAT_CALL_FINISHED);
        }
        lastStats = new Stats();
        loadState(stateBeforeSolving);
        return result;
    }

    /**
     * Saves and returns the solver state.
     * @return the current solver state
     */
    public MaxSatState saveState() {
        final int[] softWeights = new int[softClauses.size()];
        for (int i = 0; i < softClauses.size(); i++) {
            softWeights[i] = softClauses.get(i).weight();
        }
        final int stateId = nextStateId++;
        validStates.push(stateId);
        return new MaxSatState(stateId, nbVars, hardClauses.size(), softClauses.size(), ubCost, currentWeight,
                totalSoftWeight, softWeights);
    }

    /**
     * Loads a given state in the solver.
     * <p>
     * ATTENTION: You can only load a state which was created by this instance
     * of the solver before the current state. Only the sizes of the internal
     * data structures are stored, meaning you can go back in time and
     * restore a solver state with fewer variables and/or fewer clauses. It is
     * not possible to import a solver state from another solver or another
     * solving execution.
     * @param state the solver state to load
     * @throws IllegalArgumentException if the solver state is not valid anymore
     */
    public void loadState(final MaxSatState state) {
        int index = -1;
        for (int i = validStates.size() - 1; i >= 0 && index == -1; i--) {
            if (validStates.get(i) == state.getStateId()) {
                index = i;
            }
        }
        if (index == -1) {
            throw new IllegalArgumentException("The given solver state is not valid anymore.");
        }
        validStates.shrinkTo(index + 1);

        hardClauses.shrinkTo(state.getNbHard());
        softClauses.shrinkTo(state.getNbSoft());
        orderWeights.clear();
        for (int i = state.getNbVars(); i < nbVars; i++) {
            final Variable var = index2var.remove(i);
            if (var != null) {
                var2index.remove(var);
            }
        }
        nbVars = state.getNbVars();
        nbCores = 0;
        nbSymmetryClauses = 0;
        sumSizeCores = 0;
        nbSatisfiable = 0;
        ubCost = state.getUbCost();
        lbCost = 0;
        currentWeight = state.getCurrentWeight();
        totalSoftWeight = state.getTotalSoftWeight();
        for (int i = 0; i < softClauses.size(); i++) {
            final LngSoftClause clause = softClauses.get(i);
            clause.relaxationVars().clear();
            clause.setWeight(state.getSoftWeights()[i]);
            clause.setAssumptionVar(LIT_UNDEF);
        }
        model.clear();
    }

    /**
     * Creates a model from a Boolean vector of the solver.
     * @return the model
     */
    protected Model createModel() {
        final List<Literal> mdl = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            final Variable var = varForIndex(i);
            if (var != null && !var.getName().startsWith(SEL_PREFIX)) {
                mdl.add(model.get(i) ? var : var.negate(f));
            }
        }
        return new Model(mdl);
    }

    /**
     * The main MaxSAT solving method.
     * @return the result of the solving process
     * @throws IllegalArgumentException if the configuration was not valid
     */
    protected abstract LngResult<MaxSatResult> internalSearch(ComputationHandler handler);

    /**
     * Returns the number of variables in the working MaxSAT formula.
     * @return the number of variables in the working MaxSAT formula
     */
    public int nVars() {
        return nbVars;
    }

    /**
     * Returns a new variable index and increases the internal number of
     * variables.
     * @return a new variable index
     */
    public int newVar() {
        return nbVars++;
    }

    /**
     * Adds a new hard clause to the hard clause database.
     * @param lits the literals of the hard clause
     */
    public void addHardClause(final LngIntVector lits) {
        hardClauses.push(new LngHardClause(lits));
    }

    /**
     * Adds a new soft clause to the soft clause database.
     * @param weight the weight of the soft clause
     * @param lits   the literals of the soft clause
     */
    public void addSoftClause(final int weight, final LngIntVector lits) {
        addSoftClause(weight, lits, new LngIntVector());
    }

    /**
     * Adds a new soft clause to the soft clause database with predefined
     * relaxation variables.
     * @param weight the weight of the soft clause
     * @param lits   the literals of the soft clause
     * @param vars   the relaxation variables of the soft clause
     */
    protected void addSoftClause(final int weight, final LngIntVector lits, final LngIntVector vars) {
        softClauses.push(new LngSoftClause(lits, weight, LIT_UNDEF, vars));
    }

    /**
     * Adds a clause to the solver.
     * @param formula the clause
     * @param weight  the weight of the clause (or -1 for a hard clause)
     */
    public void addClause(final Formula formula, final int weight) {
        final LngIntVector clauseVec = new LngIntVector((int) formula.numberOfAtoms(f));
        for (final Literal lit : formula.literals(f)) {
            Integer index = var2index.get(lit.variable());
            if (index == null) {
                index = newLiteral(false) >> 1;
                var2index.put(lit.variable(), index);
                index2var.put(index, lit.variable());
                assert var2index.size() == index2var.size();
            }
            final int litNum = lit.getPhase() ? index * 2 : (index * 2) ^ 1;
            clauseVec.push(litNum);
        }
        addClause(clauseVec, weight);
    }

    /**
     * Adds a clause to the solver.
     * @param clauseVec the clause
     * @param weight    the weight of the clause (or -1 for a hard clause)
     */
    public void addClause(final LngIntVector clauseVec, final int weight) {
        if (weight == -1) {
            addHardClause(clauseVec);
        } else {
            setCurrentWeight(weight);
            updateSumWeights(weight);
            addSoftClause(weight, clauseVec);
        }
    }

    public int literal(final Literal lit) {
        final Variable variable = lit.variable();
        Integer index = var2index.get(variable);
        if (index == null) {
            index = newLiteral(false) >> 1;
            var2index.put(variable, index);
            index2var.put(index, variable);
        }
        return lit.getPhase() ? index * 2 : (index * 2) ^ 1;
    }

    /**
     * Creates a new literal to be used in the working MaxSAT formula.
     * @param sign the sign of the literal
     * @return the new literal
     */
    public int newLiteral(final boolean sign) {
        return LngCoreSolver.mkLit(newVar(), sign);
    }

    /**
     * Returns the variable for the given index.
     * @param index the index
     * @return the variable for the given index
     */
    public Variable varForIndex(final int index) {
        return index2var.get(index);
    }

    /**
     * Sets the problem type.
     * @param type the problem type
     */
    public void setProblemType(final ProblemType type) {
        problemType = type;
    }

    /**
     * Initializes 'ubCost' to the sum of weights of the soft clauses
     * @param weight the weight
     */
    protected void updateSumWeights(final int weight) {
        if (weight != hardWeight) {
            totalSoftWeight += weight;
            ubCost += weight;
        }
    }

    /**
     * Initializes the current weight to the maximum weight of the soft clauses.
     * @param weight the weight
     */
    protected void setCurrentWeight(final int weight) {
        if (weight > currentWeight && weight != hardWeight) {
            currentWeight = weight;
        }
    }

    /**
     * Returns the current weight.
     * @return the current weight
     */
    public int currentWeight() {
        return currentWeight;
    }

    /**
     * Returns the sum of the weights of all soft clauses
     * @return the sum of the weights of all soft clauses
     */
    public int getTotalSoftWeight() {
        return totalSoftWeight;
    }

    /**
     * Creates an empty SAT Solver.
     * @return the empty SAT solver
     */
    public LngCoreSolver newSatSolver() {
        return new LngCoreSolver(f, SatSolverConfig.builder().build());
    }

    /**
     * Saves the current model found by the SAT solver.
     * @param currentModel the model found by the solver
     */
    public void saveModel(final LngBooleanVector currentModel) {
        model.clear();
        for (int i = 0; i < nbInitialVariables; i++) {
            model.push(currentModel.get(i));
        }
    }

    /**
     * Computes the cost of a given model. The cost of a model is the sum of the
     * weights of the unsatisfied soft clauses. If a weight is specified, then
     * it only considers the sum of the weights of the unsatisfied soft clauses
     * with the specified weight.
     * @param currentModel the model
     * @param weight       the weight
     * @return the cost of the given model
     */
    public int computeCostModel(final LngBooleanVector currentModel, final int weight) {
        int currentCost = 0;
        for (int i = 0; i < softClauses.size(); i++) {
            boolean unsatisfied = true;
            for (int j = 0; j < softClauses.get(i).clause().size(); j++) {
                if (weight != Integer.MAX_VALUE && softClauses.get(i).weight() != weight) {
                    unsatisfied = false;
                    continue;
                }
                assert LngCoreSolver.var(softClauses.get(i).clause().get(j)) < currentModel.size();
                if ((LngCoreSolver.sign(softClauses.get(i).clause().get(j)) &&
                        !currentModel.get(LngCoreSolver.var(softClauses.get(i).clause().get(j)))) ||
                        (!LngCoreSolver.sign(softClauses.get(i).clause().get(j)) &&
                                currentModel.get(LngCoreSolver.var(softClauses.get(i).clause().get(j))))) {
                    unsatisfied = false;
                    break;
                }
            }
            if (unsatisfied) {
                currentCost += softClauses.get(i).weight();
            }
        }
        return currentCost;
    }

    /**
     * Tests if the MaxSAT formula has lexicographical optimization criterion.
     * @param cache indicates whether the result should be cached.
     * @return {@code true} if the formula has lexicographical optimization
     * criterion
     */
    public boolean isBmo(final boolean cache) {
        assert orderWeights.isEmpty();
        boolean bmo = true;
        final SortedSet<Integer> partitionWeights = new TreeSet<>();
        final SortedMap<Integer, Integer> nbPartitionWeights = new TreeMap<>();
        for (int i = 0; i < softClauses.size(); i++) {
            final int weight = softClauses.get(i).weight();
            partitionWeights.add(weight);
            nbPartitionWeights.merge(weight, 1, Integer::sum);
        }
        for (final int i : partitionWeights) {
            orderWeights.push(i);
        }
        orderWeights.sortReverse();
        long totalWeights = 0;
        for (int i = 0; i < orderWeights.size(); i++) {
            totalWeights += orderWeights.get(i) * nbPartitionWeights.get(orderWeights.get(i));
        }
        for (int i = 0; i < orderWeights.size(); i++) {
            totalWeights -= orderWeights.get(i) * nbPartitionWeights.get(orderWeights.get(i));
            if (orderWeights.get(i) < totalWeights) {
                bmo = false;
                break;
            }
        }
        if (!cache) {
            orderWeights.clear();
        }
        return bmo;
    }

    /**
     * Returns the stats of this solver instance.
     * @return the stats of this solver instance
     */
    public Stats stats() {
        return lastStats;
    }

    /**
     * Informs the handler about a newly found lower bound and returns the
     * event if the handler canceled the computation. Otherwise, {@code null} is
     * returned.
     * @param lowerBound the new lower bound
     * @param handler    the computation handler
     * @return the event if the handler canceled the computation, otherwise
     * {@code null}
     */
    protected LngEvent foundLowerBound(final int lowerBound, final ComputationHandler handler) {
        final MaxSatNewLowerBoundEvent event = new MaxSatNewLowerBoundEvent(lowerBound);
        return handler.shouldResume(event) ? null : event;
    }

    /**
     * Informs the handler about a newly found upper bound and returns the
     * event if the handler canceled the computation. Otherwise, {@code null} is
     * returned.
     * @param upperBound the new upper bound
     * @param handler    the computation handler
     * @return the event if the handler canceled the computation, otherwise
     * {@code null}
     */
    protected LngEvent foundUpperBound(final int upperBound, final ComputationHandler handler) {
        final MaxSatNewUpperBoundEvent event = new MaxSatNewUpperBoundEvent(upperBound);
        return handler.shouldResume(event) ? null : event;
    }

    /**
     * Creates a new result for a satisfiable MaxSAT problem with the given
     * optimum and model.
     * @return the result
     */
    public LngResult<MaxSatResult> optimum() {
        return LngResult.of(new MaxSatResult(true, totalSoftWeight - ubCost, ubCost, createModel()));
    }

    /**
     * Creates a new result for an unsatisfiable MaxSAT problem.
     * @return the result
     */
    public LngResult<MaxSatResult> unsat() {
        return LngResult.of(UNSAT);
    }

    /**
     * The MaxSAT solver statistics.
     */
    public class Stats {
        protected final int ubC;
        protected final int nbS;
        protected final int nbC;
        protected final double avgCs;
        protected final int nbSc;

        protected Stats() {
            ubC = model.isEmpty() ? -1 : ubCost;
            nbS = nbSatisfiable;
            nbC = nbCores;
            avgCs = nbCores != 0 ? (double) sumSizeCores / nbCores : 0.0;
            nbSc = nbSymmetryClauses;
        }

        /**
         * Returns the best solution or -1 if there is none.
         * @return the best solution or -1 if there is none
         */
        public int bestSolution() {
            return ubC;
        }

        /**
         * Returns the number of SAT calls.
         * @return the number of SAT calls
         */
        public int satCalls() {
            return nbS;
        }

        /**
         * Returns the number of UNSAT calls (cores).
         * @return the number of UNSAT calls (cores)
         */
        public int unsatCalls() {
            return nbC;
        }

        /**
         * Returns the average core size.
         * @return the average core size
         */
        public double averageCoreSize() {
            return avgCs;
        }

        /**
         * Returns the number of symmetry clauses.
         * @return the number of symmetry clauses
         */
        public int symmetryClauses() {
            return nbSc;
        }

        @Override
        public String toString() {
            return String.format(Locale.ENGLISH,
                    "MaxSat.Stats{best solution=%d, #sat calls=%d, #unsat calls=%d, average core size=%.2f, #symmetry"
                            + " clauses=%d}",
                    ubC, nbS, nbC, avgCs, nbSc);
        }
    }
}
