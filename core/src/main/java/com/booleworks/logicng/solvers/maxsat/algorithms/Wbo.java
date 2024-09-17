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

import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.Verbosity;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.WeightStrategy;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;
import com.booleworks.logicng.util.Pair;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Weighted Boolean Optimization solver.
 * @version 3.0.0
 * @since 1.0
 */
public class Wbo extends MaxSat {

    protected final PrintStream output;
    protected LngCoreSolver solver;
    protected int nbCurrentSoft;
    protected WeightStrategy weightStrategy;
    protected SortedMap<Integer, Integer> coreMapping;
    protected LngIntVector assumptions;
    protected boolean symmetryStrategy;
    protected LngIntVector indexSoftCore;
    protected LngVector<LngIntVector> softMapping;
    protected LngVector<LngIntVector> relaxationMapping;
    protected Set<Pair<Integer, Integer>> duplicatedSymmetryClauses;
    protected int symmetryBreakingLimit;

    /**
     * Constructs a new solver with default values.
     * @param f the formula factory
     */
    public Wbo(final FormulaFactory f) {
        this(f, MaxSatConfig.builder().build());
    }

    /**
     * Constructs a new solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public Wbo(final FormulaFactory f, final MaxSatConfig config) {
        super(f, config);
        solver = null;
        verbosity = config.verbosity;
        nbCurrentSoft = 0;
        weightStrategy = config.weightStrategy;
        symmetryStrategy = config.symmetry;
        symmetryBreakingLimit = config.limit;
        coreMapping = new TreeMap<>();
        assumptions = new LngIntVector();
        indexSoftCore = new LngIntVector();
        softMapping = new LngVector<>();
        relaxationMapping = new LngVector<>();
        duplicatedSymmetryClauses = new HashSet<>();
        output = config.output;
    }

    @Override
    protected LngResult<MaxSatResult> internalSearch(final ComputationHandler handler) {
        nbInitialVariables = nVars();
        coreMapping.clear();
        assumptions.clear();
        indexSoftCore.clear();
        softMapping.clear();
        relaxationMapping.clear();
        duplicatedSymmetryClauses.clear();
        if (currentWeight == 1) {
            weightStrategy = WeightStrategy.NONE;
        }
        if (symmetryStrategy) {
            initSymmetry();
        }
        if (problemType == ProblemType.UNWEIGHTED || weightStrategy == WeightStrategy.NONE) {
            return normalSearch(handler);
        } else if (weightStrategy == WeightStrategy.NORMAL || weightStrategy == WeightStrategy.DIVERSIFY) {
            return weightSearch(handler);
        }
        throw new IllegalArgumentException("Unknown problem type.");
    }

    protected LngCoreSolver rebuildWeightSolver(final WeightStrategy strategy) {
        assert strategy == WeightStrategy.NORMAL || strategy == WeightStrategy.DIVERSIFY;
        final LngCoreSolver s = newSatSolver();
        for (int i = 0; i < nVars(); i++) {
            newSatVariable(s);
        }
        for (int i = 0; i < hardClauses.size(); i++) {
            s.addClause(hardClauses.get(i).clause(), null);
        }
        if (symmetryStrategy) {
            symmetryBreaking();
        }
        LngIntVector clause = new LngIntVector();
        nbCurrentSoft = 0;
        for (int i = 0; i < softClauses.size(); i++) {
            if (softClauses.get(i).weight() >= currentWeight) {
                nbCurrentSoft++;
                clause.clear();
                clause = new LngIntVector(softClauses.get(i).clause());
                for (int j = 0; j < softClauses.get(i).relaxationVars().size(); j++) {
                    clause.push(softClauses.get(i).relaxationVars().get(j));
                }
                clause.push(softClauses.get(i).assumptionVar());

                s.addClause(clause, null);
            }
        }
        return s;
    }

    LngCoreSolver rebuildSolver() {
        assert weightStrategy == WeightStrategy.NONE;
        final LngCoreSolver s = newSatSolver();
        for (int i = 0; i < nVars(); i++) {
            newSatVariable(s);
        }
        for (int i = 0; i < hardClauses.size(); i++) {
            s.addClause(hardClauses.get(i).clause(), null);
        }
        if (symmetryStrategy) {
            symmetryBreaking();
        }
        LngIntVector clause;
        for (int i = 0; i < softClauses.size(); i++) {
            clause = new LngIntVector(softClauses.get(i).clause());
            for (int j = 0; j < softClauses.get(i).relaxationVars().size(); j++) {
                clause.push(softClauses.get(i).relaxationVars().get(j));
            }
            clause.push(softClauses.get(i).assumptionVar());
            s.addClause(clause, null);
        }
        return s;
    }

    protected LngCoreSolver rebuildHardSolver() {
        final LngCoreSolver s = newSatSolver();
        for (int i = 0; i < nVars(); i++) {
            newSatVariable(s);
        }
        for (int i = 0; i < hardClauses.size(); i++) {
            s.addClause(hardClauses.get(i).clause(), null);
        }
        return s;
    }

    void updateCurrentWeight(final WeightStrategy strategy) {
        switch (strategy) {
            case NORMAL:
                currentWeight = findNextWeight(currentWeight);
                break;
            case DIVERSIFY:
                currentWeight = findNextWeightDiversity(currentWeight);
                break;
            default:
                throw new IllegalArgumentException("Unknown strategy: " + strategy);
        }
    }

    protected int findNextWeight(final int weight) {
        int nextWeight = 1;
        for (int i = 0; i < softClauses.size(); i++) {
            if (softClauses.get(i).weight() > nextWeight && softClauses.get(i).weight() < weight) {
                nextWeight = softClauses.get(i).weight();
            }
        }
        return nextWeight;
    }

    protected int findNextWeightDiversity(final int weight) {
        assert weightStrategy == WeightStrategy.DIVERSIFY;
        assert nbSatisfiable > 0;
        int nextWeight = weight;
        int nbClauses;
        final SortedSet<Integer> nbWeights = new TreeSet<>();
        final double alpha = 1.25;
        boolean findNext = false;
        while (true) {
            if (nbSatisfiable > 1 || findNext) {
                nextWeight = findNextWeight(nextWeight);
            }
            nbClauses = 0;
            nbWeights.clear();
            for (int i = 0; i < softClauses.size(); i++) {
                if (softClauses.get(i).weight() >= nextWeight) {
                    nbClauses++;
                    nbWeights.add(softClauses.get(i).weight());
                }
            }
            if ((double) nbClauses / nbWeights.size() > alpha || nbClauses == softClauses.size()) {
                break;
            }
            if (nbSatisfiable == 1 && !findNext) {
                findNext = true;
            }
        }
        return nextWeight;
    }

    protected void encodeEo(final LngIntVector lits) {
        assert !lits.isEmpty();
        final LngIntVector clause = new LngIntVector();
        if (lits.size() == 1) {
            clause.push(lits.get(0));
            addHardClause(clause);
        } else {
            final LngIntVector auxVariables = new LngIntVector();
            for (int i = 0; i < lits.size() - 1; i++) {
                auxVariables.push(newLiteral(false));
            }
            for (int i = 0; i < lits.size(); i++) {
                if (i == 0) {
                    clause.clear();
                    clause.push(lits.get(i));
                    clause.push(LngCoreSolver.not(auxVariables.get(i)));
                    addHardClause(clause);
                    clause.clear();
                    clause.push(LngCoreSolver.not(lits.get(i)));
                    clause.push(auxVariables.get(i));
                    addHardClause(clause);
                } else if (i == lits.size() - 1) {
                    clause.clear();
                    clause.push(lits.get(i));
                    clause.push(auxVariables.get(i - 1));
                    addHardClause(clause);
                    clause.clear();
                    clause.push(LngCoreSolver.not(lits.get(i)));
                    clause.push(LngCoreSolver.not(auxVariables.get(i - 1)));
                    addHardClause(clause);
                } else {
                    clause.clear();
                    clause.push(LngCoreSolver.not(auxVariables.get(i - 1)));
                    clause.push(auxVariables.get(i));
                    addHardClause(clause);
                    clause.clear();
                    clause.push(lits.get(i));
                    clause.push(LngCoreSolver.not(auxVariables.get(i)));
                    clause.push(auxVariables.get(i - 1));
                    addHardClause(clause);
                    clause.clear();
                    clause.push(LngCoreSolver.not(lits.get(i)));
                    clause.push(auxVariables.get(i));
                    addHardClause(clause);
                    clause.clear();
                    clause.push(LngCoreSolver.not(lits.get(i)));
                    clause.push(LngCoreSolver.not(auxVariables.get(i - 1)));
                    addHardClause(clause);
                }
            }
        }
    }

    protected void relaxCore(final LngIntVector conflict, final int weightCore, final LngIntVector assumps) {
        assert !conflict.isEmpty();
        assert weightCore > 0;
        final LngIntVector lits = new LngIntVector();
        for (int i = 0; i < conflict.size(); i++) {
            final int indexSoft = coreMapping.get(conflict.get(i));

            if (softClauses.get(indexSoft).weight() == weightCore) {
                final int p = newLiteral(false);
                softClauses.get(indexSoft).relaxationVars().push(p);
                lits.push(p);
                if (symmetryStrategy) {
                    symmetryLog(indexSoft);
                }
            } else {
                assert softClauses.get(indexSoft).weight() - weightCore > 0;
                softClauses.get(indexSoft).setWeight(softClauses.get(indexSoft).weight() - weightCore);
                final LngIntVector clause = new LngIntVector(softClauses.get(indexSoft).clause());
                final LngIntVector vars = new LngIntVector(softClauses.get(indexSoft).relaxationVars());
                final int p = newLiteral(false);
                vars.push(p);
                lits.push(p);
                addSoftClause(weightCore, clause, vars);
                final int l = newLiteral(false);
                softClauses.get(softClauses.size() - 1).setAssumptionVar(l);
                // Map the new soft clause to its assumption literal
                coreMapping.put(l, softClauses.size() - 1);
                // Update the assumption vector
                assumps.push(LngCoreSolver.not(l));
                if (symmetryStrategy) {
                    symmetryLog(softClauses.size() - 1);
                }
            }
        }
        encodeEo(lits);
        sumSizeCores += conflict.size();
    }

    int computeCostCore(final LngIntVector conflict) {
        assert !conflict.isEmpty();
        if (problemType == ProblemType.UNWEIGHTED) {
            return 1;
        }
        int coreCost = Integer.MAX_VALUE;
        for (int i = 0; i < conflict.size(); i++) {
            final int indexSoft = coreMapping.get(conflict.get(i));
            if (softClauses.get(indexSoft).weight() < coreCost) {
                coreCost = softClauses.get(indexSoft).weight();
            }
        }
        return coreCost;
    }

    void initSymmetry() {
        for (int i = 0; i < softClauses.size(); i++) {
            softMapping.push(new LngIntVector());
            relaxationMapping.push(new LngIntVector());
        }
    }

    void symmetryLog(final int p) {
        if (nbSymmetryClauses < symmetryBreakingLimit) {
            while (softMapping.size() <= p) {
                softMapping.push(new LngIntVector());
                relaxationMapping.push(new LngIntVector());
            }
            softMapping.get(p).push(nbCores);
            relaxationMapping.get(p).push(softClauses.get(p).relaxationVars().back());
            if (softMapping.get(p).size() > 1) {
                indexSoftCore.push(p);
            }
        }
    }

    protected void symmetryBreaking() {
        if (!indexSoftCore.isEmpty() && nbSymmetryClauses < symmetryBreakingLimit) {
            final LngIntVector[] coreIntersection = new LngIntVector[nbCores];
            final LngIntVector[] coreIntersectionCurrent = new LngIntVector[nbCores];
            for (int i = 0; i < nbCores; i++) {
                coreIntersection[i] = new LngIntVector();
                coreIntersectionCurrent[i] = new LngIntVector();
            }
            final LngIntVector coreList = new LngIntVector();
            for (int i = 0; i < indexSoftCore.size(); i++) {
                final int p = indexSoftCore.get(i);
                final LngIntVector addCores = new LngIntVector();
                for (int j = 0; j < softMapping.get(p).size() - 1; j++) {
                    final int core = softMapping.get(p).get(j);
                    addCores.push(core);
                    if (coreIntersection[core].isEmpty()) {
                        coreList.push(core);
                    }
                    assert j < relaxationMapping.get(p).size();
                    assert LngCoreSolver.var(relaxationMapping.get(p).get(j)) > nbInitialVariables;
                    coreIntersection[core].push(relaxationMapping.get(p).get(j));
                }
                for (int j = 0; j < addCores.size(); j++) {
                    final int core = addCores.get(j);
                    final int b = softMapping.get(p).size() - 1;
                    assert b < relaxationMapping.get(p).size();
                    assert LngCoreSolver.var(relaxationMapping.get(p).get(b)) > nbInitialVariables;
                    coreIntersectionCurrent[core].push(relaxationMapping.get(p).get(b));
                }
                for (int k = 0; k < coreList.size(); k++) {
                    for (int m = 0; m < coreIntersection[coreList.get(k)].size(); m++) {
                        for (int j = m + 1; j < coreIntersectionCurrent[coreList.get(k)].size(); j++) {
                            final LngIntVector clause = new LngIntVector();
                            clause.push(LngCoreSolver.not(coreIntersection[coreList.get(k)].get(m)));
                            clause.push(LngCoreSolver.not(coreIntersectionCurrent[coreList.get(k)].get(j)));
                            Pair<Integer, Integer> symClause =
                                    new Pair<>(LngCoreSolver.var(coreIntersection[coreList.get(k)].get(m)),
                                            LngCoreSolver.var(coreIntersectionCurrent[coreList.get(k)].get(j)));
                            if (LngCoreSolver.var(coreIntersection[coreList.get(k)].get(m)) >
                                    LngCoreSolver.var(coreIntersectionCurrent[coreList.get(k)].get(j))) {
                                symClause =
                                        new Pair<>(LngCoreSolver.var(coreIntersectionCurrent[coreList.get(k)].get(j)),
                                                LngCoreSolver.var(coreIntersection[coreList.get(k)].get(m)));
                            }
                            if (!duplicatedSymmetryClauses.contains(symClause)) {
                                duplicatedSymmetryClauses.add(symClause);
                                addHardClause(clause);
                                nbSymmetryClauses++;
                                if (symmetryBreakingLimit == nbSymmetryClauses) {
                                    break;
                                }
                            }
                        }
                        if (symmetryBreakingLimit == nbSymmetryClauses) {
                            break;
                        }
                    }
                    if (symmetryBreakingLimit == nbSymmetryClauses) {
                        break;
                    }
                }
                if (symmetryBreakingLimit == nbSymmetryClauses) {
                    break;
                }
            }
        }
        indexSoftCore.clear();
    }

    LngResult<Boolean> unsatSearch(final ComputationHandler handler) {
        assert assumptions.isEmpty();
        solver = rebuildHardSolver();
        final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
        if (!res.isSuccess()) {
            return LngResult.canceled(res.getCancelCause());
        } else if (!res.getResult()) {
            nbCores++;
        } else {
            nbSatisfiable++;
            final int cost = computeCostModel(solver.model(), Integer.MAX_VALUE);
            assert cost <= ubCost;
            ubCost = cost;
            saveModel(solver.model());
            if (verbosity != Verbosity.NONE) {
                output.println("o " + ubCost);
            }
        }
        solver = null;
        return res;
    }

    protected LngResult<MaxSatResult> weightSearch(final ComputationHandler handler) {
        assert weightStrategy == WeightStrategy.NORMAL || weightStrategy == WeightStrategy.DIVERSIFY;
        final LngResult<Boolean> unsatResult = unsatSearch(handler);
        if (!unsatResult.isSuccess()) {
            return LngResult.canceled(unsatResult.getCancelCause());
        } else if (!unsatResult.getResult()) {
            return unsat();
        }
        initAssumptions();
        updateCurrentWeight(weightStrategy);
        solver = rebuildWeightSolver(weightStrategy);
        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (!res.getResult()) {
                nbCores++;
                assert !solver.assumptionsConflict().isEmpty();
                final int coreCost = computeCostCore(solver.assumptionsConflict());
                lbCost += coreCost;
                if (verbosity != Verbosity.NONE) {
                    output.printf("c LB : %d CS : %d W : %d%n", lbCost, solver.assumptionsConflict().size(), coreCost);
                }
                final LngEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                if (lowerBoundEvent != null) {
                    return LngResult.canceled(lowerBoundEvent);
                }
                relaxCore(solver.assumptionsConflict(), coreCost, assumptions);
                solver = rebuildWeightSolver(weightStrategy);
            } else {
                nbSatisfiable++;
                if (nbCurrentSoft == softClauses.size()) {
                    assert computeCostModel(solver.model(), Integer.MAX_VALUE) == lbCost;
                    if (lbCost == ubCost && verbosity != Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    if (lbCost < ubCost) {
                        ubCost = lbCost;
                        saveModel(solver.model());
                        if (verbosity != Verbosity.NONE) {
                            output.println("o " + lbCost);
                        }
                    }
                    return optimum();
                } else {
                    updateCurrentWeight(weightStrategy);
                    final int cost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                    if (cost < ubCost) {
                        ubCost = cost;
                        saveModel(solver.model());
                        if (verbosity != Verbosity.NONE) {
                            output.println("o " + ubCost);
                        }
                    }
                    if (lbCost == ubCost) {
                        if (verbosity != Verbosity.NONE) {
                            output.println("c LB = UB");
                        }
                        return optimum();
                    } else {
                        final LngEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                        if (upperBoundEvent != null) {
                            return LngResult.canceled(upperBoundEvent);
                        }
                    }
                    solver = rebuildWeightSolver(weightStrategy);
                }
            }
        }
    }

    protected LngResult<MaxSatResult> normalSearch(final ComputationHandler handler) {
        final LngResult<Boolean> unsatResult = unsatSearch(handler);
        if (!unsatResult.isSuccess()) {
            return LngResult.canceled(unsatResult.getCancelCause());
        } else if (!unsatResult.getResult()) {
            return unsat();
        }
        initAssumptions();
        solver = rebuildSolver();
        while (true) {
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (!res.getResult()) {
                nbCores++;
                assert !solver.assumptionsConflict().isEmpty();
                final int coreCost = computeCostCore(solver.assumptionsConflict());
                lbCost += coreCost;
                if (verbosity != Verbosity.NONE) {
                    output.printf("c LB : %d CS : %d W : %d%n", lbCost, solver.assumptionsConflict().size(), coreCost);
                }
                if (lbCost == ubCost) {
                    if (verbosity != Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    return optimum();
                } else {
                    final LngEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                    if (lowerBoundEvent != null) {
                        return LngResult.canceled(lowerBoundEvent);
                    }
                }
                relaxCore(solver.assumptionsConflict(), coreCost, assumptions);
                solver = rebuildSolver();
            } else {
                nbSatisfiable++;
                ubCost = computeCostModel(solver.model(), Integer.MAX_VALUE);
                assert lbCost == ubCost;
                if (verbosity != Verbosity.NONE) {
                    output.println("o " + lbCost);
                }
                saveModel(solver.model());
                return optimum();
            }
        }
    }

    void initAssumptions() {
        for (int i = 0; i < softClauses.size(); i++) {
            final int l = newLiteral(false);
            softClauses.get(i).setAssumptionVar(l);
            coreMapping.put(l, i);
            assumptions.push(LngCoreSolver.not(l));
        }
    }
}
