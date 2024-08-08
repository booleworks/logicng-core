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

package com.booleworks.logicng.solvers.maxsat.algorithms;

import static com.booleworks.logicng.handlers.events.ComputationFinishedEvent.MAX_SAT_CALL_FINISHED;
import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.MAX_SAT_CALL_STARTED;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSATConfig.Verbosity;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.events.LNGEvent;
import com.booleworks.logicng.handlers.events.MaxSatNewLowerBoundEvent;
import com.booleworks.logicng.handlers.events.MaxSatNewUpperBoundEvent;
import com.booleworks.logicng.solvers.datastructures.LNGHardClause;
import com.booleworks.logicng.solvers.datastructures.LNGSoftClause;
import com.booleworks.logicng.solvers.maxsat.InternalMaxSATResult;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;

import java.util.Locale;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Super class for the MaxSAT solvers.
 * @version 2.0.0
 * @since 1.0
 */
public abstract class MaxSAT {

    /**
     * The MaxSAT problem type: {@code UNWEIGHTED} or {@code WEIGHTED}.
     */
    public enum ProblemType {
        UNWEIGHTED,
        WEIGHTED
    }

    protected final FormulaFactory f;
    protected final LNGBooleanVector model;
    final LNGVector<LNGSoftClause> softClauses;
    final LNGVector<LNGHardClause> hardClauses;
    final LNGIntVector orderWeights;
    protected Verbosity verbosity;
    int hardWeight;
    ProblemType problemType;
    int nbVars;
    int nbSoft;
    int nbHard;
    int nbInitialVariables;
    int nbCores;
    int nbSymmetryClauses;
    long sumSizeCores;
    int nbSatisfiable;
    int ubCost;
    int lbCost;
    int currentWeight;

    /**
     * Constructor.
     * @param f      the formula factory
     * @param config the solver configuration
     */
    protected MaxSAT(final FormulaFactory f, final MaxSATConfig config) {
        this.f = f;
        hardClauses = new LNGVector<>();
        softClauses = new LNGVector<>();
        hardWeight = Integer.MAX_VALUE;
        problemType = ProblemType.UNWEIGHTED;
        nbVars = 0;
        nbSoft = 0;
        nbHard = 0;
        nbInitialVariables = 0;
        currentWeight = 1;
        model = new LNGBooleanVector();
        ubCost = 0;
        lbCost = 0;
        nbSymmetryClauses = 0;
        nbCores = 0;
        nbSatisfiable = 0;
        sumSizeCores = 0;
        orderWeights = new LNGIntVector();
    }

    /**
     * Creates a new variable in the SAT solver.
     * @param s the SAT solver
     */
    public static void newSATVariable(final LNGCoreSolver s) {
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
    public static LNGResult<Boolean> searchSATSolver(final LNGCoreSolver s, final ComputationHandler handler,
                                                     final LNGIntVector assumptions) {
        return s.internalSolve(handler, assumptions);
    }

    /**
     * Solves the formula without assumptions.
     * @param s       the SAT solver
     * @param handler the handler
     * @return the result of the solving process
     */
    public static LNGResult<Boolean> searchSATSolver(final LNGCoreSolver s, final ComputationHandler handler) {
        return s.internalSolve(handler);
    }

    /**
     * The main MaxSAT solving method.
     * @param handler a MaxSAT handler
     * @return the result of the solving process
     * @throws IllegalArgumentException if the configuration was not valid
     */
    public final LNGResult<InternalMaxSATResult> search(final ComputationHandler handler) {
        if (!handler.shouldResume(MAX_SAT_CALL_STARTED)) {
            return LNGResult.aborted(MAX_SAT_CALL_STARTED);
        }
        final LNGResult<InternalMaxSATResult> result = internalSearch(handler);
        if (!handler.shouldResume(MAX_SAT_CALL_FINISHED)) {
            return LNGResult.aborted(MAX_SAT_CALL_FINISHED);
        }
        return result;
    }

    /**
     * The main MaxSAT solving method.
     * @return the result of the solving process
     * @throws IllegalArgumentException if the configuration was not valid
     */
    protected abstract LNGResult<InternalMaxSATResult> internalSearch(ComputationHandler handler);

    /**
     * Returns the number of variables in the working MaxSAT formula.
     * @return the number of variables in the working MaxSAT formula
     */
    public int nVars() {
        return nbVars;
    }

    /**
     * Returns the number of soft clauses in the working MaxSAT formula.
     * @return the number of soft clauses in the working MaxSAT formula
     */
    public int nSoft() {
        return nbSoft;
    }

    /**
     * Returns the number of hard clauses in the working MaxSAT formula.
     * @return the number of hard clauses in the working MaxSAT formula
     */
    public int nHard() {
        return nbHard;
    }

    /**
     * Increases the number of variables in the working MaxSAT formula.
     */
    public void newVar() {
        nbVars++;
    }

    /**
     * Adds a new hard clause to the hard clause database.
     * @param lits the literals of the hard clause
     */
    public void addHardClause(final LNGIntVector lits) {
        hardClauses.push(new LNGHardClause(lits));
        nbHard++;
    }

    /**
     * Adds a new soft clause to the soft clause database.
     * @param weight the weight of the soft clause
     * @param lits   the literals of the soft clause
     */
    public void addSoftClause(final int weight, final LNGIntVector lits) {
        final LNGIntVector rVars = new LNGIntVector();
        softClauses.push(new LNGSoftClause(lits, weight, LNGCoreSolver.LIT_UNDEF, rVars));
        nbSoft++;
    }

    /**
     * Adds a new soft clause to the soft clause database with predefined
     * relaxation variables.
     * @param weight the weight of the soft clause
     * @param lits   the literals of the soft clause
     * @param vars   the relaxation variables of the soft clause
     */
    public void addSoftClause(final int weight, final LNGIntVector lits, final LNGIntVector vars) {
        softClauses.push(new LNGSoftClause(lits, weight, LNGCoreSolver.LIT_UNDEF, vars));
        nbSoft++;
    }

    /**
     * Creates a new literal to be used in the working MaxSAT formula.
     * @param sign the sign of the literal
     * @return the new literal
     */
    public int newLiteral(final boolean sign) {
        final int p = LNGCoreSolver.mkLit(nVars(), sign);
        newVar();
        return p;
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
    public void updateSumWeights(final int weight) {
        if (weight != hardWeight) {
            ubCost += weight;
        }
    }

    /**
     * Initializes the current weight to the maximum weight of the soft clauses.
     * @param weight the weight
     */
    public void setCurrentWeight(final int weight) {
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
     * Creates an empty SAT Solver.
     * @return the empty SAT solver
     */
    public LNGCoreSolver newSATSolver() {
        return new LNGCoreSolver(f, SATSolverConfig.builder().build());
    }

    /**
     * Saves the current model found by the SAT solver.
     * @param currentModel the model found by the solver
     */
    public void saveModel(final LNGBooleanVector currentModel) {
        assert nbInitialVariables != 0;
        assert currentModel.size() != 0;
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
    public int computeCostModel(final LNGBooleanVector currentModel, final int weight) {
        assert currentModel.size() != 0;
        int currentCost = 0;
        for (int i = 0; i < nSoft(); i++) {
            boolean unsatisfied = true;
            for (int j = 0; j < softClauses.get(i).clause().size(); j++) {
                if (weight != Integer.MAX_VALUE && softClauses.get(i).weight() != weight) {
                    unsatisfied = false;
                    continue;
                }
                assert LNGCoreSolver.var(softClauses.get(i).clause().get(j)) < currentModel.size();
                if ((LNGCoreSolver.sign(softClauses.get(i).clause().get(j)) &&
                        !currentModel.get(LNGCoreSolver.var(softClauses.get(i).clause().get(j)))) ||
                        (!LNGCoreSolver.sign(softClauses.get(i).clause().get(j)) &&
                                currentModel.get(LNGCoreSolver.var(softClauses.get(i).clause().get(j))))) {
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
     * @param cache is indicates whether the result should be cached.
     * @return {@code true} if the formula has lexicographical optimization
     *         criterion
     */
    public boolean isBMO(final boolean cache) {
        assert orderWeights.size() == 0;
        boolean bmo = true;
        final SortedSet<Integer> partitionWeights = new TreeSet<>();
        final SortedMap<Integer, Integer> nbPartitionWeights = new TreeMap<>();
        for (int i = 0; i < nSoft(); i++) {
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
        return new Stats();
    }

    /**
     * Informs the handler about a newly found lower bound and returns the
     * event if the handler aborted the computation. Otherwise, {@code null} is
     * returned.
     * @param lowerBound the new lower bound
     * @param handler    the computation handler
     * @return the event if the handler aborted the computation, otherwise
     *         {@code null}
     */
    LNGEvent foundLowerBound(final int lowerBound, final ComputationHandler handler) {
        final MaxSatNewLowerBoundEvent event = new MaxSatNewLowerBoundEvent(lowerBound);
        return handler.shouldResume(event) ? null : event;
    }

    /**
     * Informs the handler about a newly found upper bound and returns the
     * event if the handler aborted the computation. Otherwise, {@code null} is
     * returned.
     * @param upperBound the new upper bound
     * @param handler    the computation handler
     * @return the event if the handler aborted the computation, otherwise
     *         {@code null}
     */
    LNGEvent foundUpperBound(final int upperBound, final ComputationHandler handler) {
        final MaxSatNewUpperBoundEvent event = new MaxSatNewUpperBoundEvent(upperBound);
        return handler.shouldResume(event) ? null : event;
    }

    /**
     * The MaxSAT solver statistics.
     */
    public class Stats {
        protected final int ubC;
        protected final int nbS;
        protected final int nbC;
        protected final double avgCS;
        protected final int nbSC;

        protected Stats() {
            ubC = model.size() == 0 ? -1 : ubCost;
            nbS = nbSatisfiable;
            nbC = nbCores;
            avgCS = nbCores != 0 ? (double) sumSizeCores / nbCores : 0.0;
            nbSC = nbSymmetryClauses;
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
            return avgCS;
        }

        /**
         * Returns the number of symmetry clauses.
         * @return the number of symmetry clauses
         */
        public int symmetryClauses() {
            return nbSC;
        }

        @Override
        public String toString() {
            return String.format(Locale.ENGLISH,
                    "MaxSAT.Stats{best solution=%d, #sat calls=%d, #unsat calls=%d, average core size=%.2f, #symmetry clauses=%d}",
                    ubC, nbS, nbC, avgCS, nbSC);
        }
    }
}
