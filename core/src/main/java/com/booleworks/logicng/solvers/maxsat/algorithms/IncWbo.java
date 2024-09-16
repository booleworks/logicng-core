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

import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;
import com.booleworks.logicng.util.Pair;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Incremental WBO solver.
 * @version 3.0.0
 * @since 1.0
 */
public class IncWbo extends Wbo {

    protected Encoder encoder;
    protected final LngBooleanVector incSoft;
    protected final PrintStream output;
    protected boolean firstBuild;

    /**
     * Constructs a new solver with default values.
     * @param f the formula factory
     */
    public IncWbo(final FormulaFactory f) {
        this(f, MaxSatConfig.builder().build());
    }

    /**
     * Constructs a new solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public IncWbo(final FormulaFactory f, final MaxSatConfig config) {
        super(f, config);
        solver = null;
        verbosity = config.verbosity;
        nbCurrentSoft = 0;
        weightStrategy = config.weightStrategy;
        symmetryStrategy = config.symmetry;
        symmetryBreakingLimit = config.limit;
        firstBuild = true;
        coreMapping = new TreeMap<>();
        assumptions = new LngIntVector();
        indexSoftCore = new LngIntVector();
        softMapping = new LngVector<>();
        relaxationMapping = new LngVector<>();
        duplicatedSymmetryClauses = new HashSet<>();
        incSoft = new LngBooleanVector();
        output = config.output;
    }

    @Override
    protected LngResult<MaxSatResult> internalSearch(final ComputationHandler handler) {
        encoder = new Encoder(MaxSatConfig.CardinalityEncoding.TOTALIZER);
        encoder.setAmoEncoding(config.amoEncoding);
        nbInitialVariables = nVars();
        coreMapping.clear();
        assumptions.clear();
        indexSoftCore.clear();
        softMapping.clear();
        relaxationMapping.clear();
        duplicatedSymmetryClauses.clear();
        incSoft.clear();
        firstBuild = true;
        if (currentWeight == 1) {
            weightStrategy = MaxSatConfig.WeightStrategy.NONE;
        }
        if (symmetryStrategy) {
            initSymmetry();
        }
        if (problemType == ProblemType.UNWEIGHTED || weightStrategy == MaxSatConfig.WeightStrategy.NONE) {
            return normalSearch(handler);
        } else if (weightStrategy == MaxSatConfig.WeightStrategy.NORMAL ||
                weightStrategy == MaxSatConfig.WeightStrategy.DIVERSIFY) {
            return weightSearch(handler);
        }
        throw new IllegalArgumentException("Unknown problem type.");
    }

    protected void incrementalBuildWeightSolver(final MaxSatConfig.WeightStrategy strategy) {
        assert strategy == MaxSatConfig.WeightStrategy.NORMAL || strategy == MaxSatConfig.WeightStrategy.DIVERSIFY;
        if (firstBuild) {
            solver = newSatSolver();
            for (int i = 0; i < nVars(); i++) {
                newSatVariable(solver);
            }
            for (int i = 0; i < hardClauses.size(); i++) {
                solver.addClause(hardClauses.get(i).clause(), null);
            }
            if (symmetryStrategy) {
                symmetryBreaking();
            }
            firstBuild = false;
        }
        LngIntVector clause;
        nbCurrentSoft = 0;
        for (int i = 0; i < softClauses.size(); i++) {
            if (softClauses.get(i).weight() >= currentWeight && softClauses.get(i).weight() != 0) {
                nbCurrentSoft++;
                clause = new LngIntVector(softClauses.get(i).clause());
                for (int j = 0; j < softClauses.get(i).relaxationVars().size(); j++) {
                    clause.push(softClauses.get(i).relaxationVars().get(j));
                }
                clause.push(softClauses.get(i).assumptionVar());
                solver.addClause(clause, null);
            }
        }
    }

    protected void relaxCore(final LngIntVector conflict, final int weightCore) {
        assert conflict.size() > 0;
        assert weightCore > 0;
        final LngIntVector lits = new LngIntVector();
        for (int i = 0; i < conflict.size(); i++) {
            final int indexSoft = coreMapping.get(conflict.get(i));
            if (softClauses.get(indexSoft).weight() == weightCore) {
                final LngIntVector clause = new LngIntVector(softClauses.get(indexSoft).clause());
                final LngIntVector vars = new LngIntVector(softClauses.get(indexSoft).relaxationVars());
                final int p = newLiteral(false);
                newSatVariable(solver);
                vars.push(p);
                lits.push(p);
                addSoftClause(weightCore, clause, vars);
                final int l = newLiteral(false);
                newSatVariable(solver);
                softClauses.get(softClauses.size() - 1).setAssumptionVar(l);
                coreMapping.put(l, softClauses.size() - 1);
                incSoft.set(indexSoft, true);
                incSoft.push(false);
                for (int j = 0; j < vars.size(); j++) {
                    clause.push(vars.get(j));
                }
                clause.push(l);
                solver.addClause(clause, null);
                clause.clear();
                clause.push(softClauses.get(indexSoft).assumptionVar());
                solver.addClause(clause, null);
                if (symmetryStrategy) {
                    softMapping.push(new LngIntVector(softMapping.get(indexSoft)));
                    softMapping.get(indexSoft).clear();
                    relaxationMapping.push(new LngIntVector(relaxationMapping.get(indexSoft)));
                    relaxationMapping.get(indexSoft).clear();
                    symmetryLog(softClauses.size() - 1);
                }
            } else {
                assert softClauses.get(indexSoft).weight() - weightCore > 0;
                softClauses.get(indexSoft).setWeight(softClauses.get(indexSoft).weight() - weightCore);
                LngIntVector clause = new LngIntVector(softClauses.get(indexSoft).clause());
                LngIntVector vars = new LngIntVector(softClauses.get(indexSoft).relaxationVars());
                addSoftClause(softClauses.get(indexSoft).weight(), clause, vars);
                if (symmetryStrategy) {
                    softMapping.push(new LngIntVector(softMapping.get(indexSoft)));
                    softMapping.get(indexSoft).clear();
                    relaxationMapping.push(new LngIntVector(relaxationMapping.get(indexSoft)));
                    relaxationMapping.get(indexSoft).clear();
                }
                incSoft.set(indexSoft, true);
                int l = newLiteral(false);
                newSatVariable(solver);
                softClauses.get(softClauses.size() - 1).setAssumptionVar(l);
                coreMapping.put(l, softClauses.size() - 1);
                incSoft.push(false);
                for (int j = 0; j < vars.size(); j++) {
                    clause.push(vars.get(j));
                }
                clause.push(l);
                solver.addClause(clause, null);
                clause.clear();
                vars.clear();
                clause = new LngIntVector(softClauses.get(indexSoft).clause());
                vars = new LngIntVector(softClauses.get(indexSoft).relaxationVars());
                l = newLiteral(false);
                newSatVariable(solver);
                vars.push(l);
                lits.push(l);
                addSoftClause(weightCore, clause, vars);
                l = newLiteral(false);
                newSatVariable(solver);
                softClauses.get(softClauses.size() - 1).setAssumptionVar(l);
                coreMapping.put(l, softClauses.size() - 1);
                incSoft.push(false);
                for (int j = 0; j < vars.size(); j++) {
                    clause.push(vars.get(j));
                }
                clause.push(l);
                solver.addClause(clause, null);
                clause.clear();
                clause.push(softClauses.get(indexSoft).assumptionVar());
                solver.addClause(clause, null);
                if (symmetryStrategy) {
                    softMapping.push(new LngIntVector());
                    relaxationMapping.push(new LngIntVector());
                    symmetryLog(softClauses.size() - 1);
                }
            }
        }
        encoder.encodeAmo(solver, lits);
        nbVars = solver.nVars();
        if (symmetryStrategy) {
            symmetryBreaking();
        }
        sumSizeCores += conflict.size();
    }

    @Override
    protected void symmetryBreaking() {
        if (indexSoftCore.size() != 0 && nbSymmetryClauses < symmetryBreakingLimit) {
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
                    if (coreIntersection[core].size() == 0) {
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
                                solver.addClause(clause, null);
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

    @Override
    protected LngResult<MaxSatResult> weightSearch(final ComputationHandler handler) {
        assert weightStrategy == MaxSatConfig.WeightStrategy.NORMAL ||
                weightStrategy == MaxSatConfig.WeightStrategy.DIVERSIFY;
        final LngResult<Boolean> unsatResult = unsatSearch(handler);
        if (!unsatResult.isSuccess()) {
            return LngResult.canceled(unsatResult.getCancelCause());
        } else if (!unsatResult.getResult()) {
            return unsat();
        }
        initAssumptions();
        updateCurrentWeight(weightStrategy);
        incrementalBuildWeightSolver(weightStrategy);
        incSoft.growTo(softClauses.size(), false);
        while (true) {
            assumptions.clear();
            for (int i = 0; i < incSoft.size(); i++) {
                if (!incSoft.get(i)) {
                    assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                }
            }
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (!res.getResult()) {
                nbCores++;
                assert solver.assumptionsConflict().size() > 0;
                final int coreCost = computeCostCore(solver.assumptionsConflict());
                lbCost += coreCost;
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
                    output.printf("c LB : %d CS : %d W : %d%n", lbCost, solver.assumptionsConflict().size(), coreCost);
                }
                final LngEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                if (lowerBoundEvent != null) {
                    return LngResult.canceled(lowerBoundEvent);
                }
                relaxCore(solver.assumptionsConflict(), coreCost);
                incrementalBuildWeightSolver(weightStrategy);
            } else {
                nbSatisfiable++;
                if (nbCurrentSoft == softClauses.size()) {
                    assert incComputeCostModel(solver.model()) == lbCost;
                    if (lbCost == ubCost && verbosity != MaxSatConfig.Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    if (lbCost < ubCost) {
                        ubCost = lbCost;
                        saveModel(solver.model());
                        if (verbosity != MaxSatConfig.Verbosity.NONE) {
                            output.println("o " + lbCost);
                        }
                    }
                    return optimum();
                } else {
                    updateCurrentWeight(weightStrategy);
                    final int cost = incComputeCostModel(solver.model());
                    if (cost < ubCost) {
                        ubCost = cost;
                        saveModel(solver.model());
                        if (verbosity != MaxSatConfig.Verbosity.NONE) {
                            output.println("o " + ubCost);
                        }
                    }
                    if (lbCost == ubCost) {
                        if (verbosity != MaxSatConfig.Verbosity.NONE) {
                            output.println("c LB = UB");
                        }
                        return optimum();
                    } else {
                        final LngEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                        if (upperBoundEvent != null) {
                            return LngResult.canceled(upperBoundEvent);
                        }
                    }
                    incrementalBuildWeightSolver(weightStrategy);
                }
            }
        }
    }

    protected int incComputeCostModel(final LngBooleanVector currentModel) {
        int currentCost = 0;
        for (int i = 0; i < softClauses.size(); i++) {
            boolean unsatisfied = true;
            for (int j = 0; j < softClauses.get(i).clause().size(); j++) {
                if (incSoft.get(i)) {
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

    @Override
    protected LngResult<MaxSatResult> normalSearch(final ComputationHandler handler) {
        final LngResult<Boolean> unsatResult = unsatSearch(handler);
        if (!unsatResult.isSuccess()) {
            return LngResult.canceled(unsatResult.getCancelCause());
        } else if (!unsatResult.getResult()) {
            return unsat();
        }
        initAssumptions();
        solver = rebuildSolver();
        incSoft.growTo(softClauses.size(), false);
        while (true) {
            assumptions.clear();
            for (int i = 0; i < incSoft.size(); i++) {
                if (!incSoft.get(i)) {
                    assumptions.push(LngCoreSolver.not(softClauses.get(i).assumptionVar()));
                }
            }
            final LngResult<Boolean> res = searchSatSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LngResult.canceled(res.getCancelCause());
            } else if (!res.getResult()) {
                nbCores++;
                assert solver.assumptionsConflict().size() > 0;
                final int coreCost = computeCostCore(solver.assumptionsConflict());
                lbCost += coreCost;
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
                    output.printf("c LB : %d CS : %d W : %d%n", lbCost, solver.assumptionsConflict().size(), coreCost);
                }
                if (lbCost == ubCost) {
                    if (verbosity != MaxSatConfig.Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    return optimum();
                }
                final LngEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                if (lowerBoundEvent != null) {
                    return LngResult.canceled(lowerBoundEvent);
                }
                relaxCore(solver.assumptionsConflict(), coreCost);
            } else {
                nbSatisfiable++;
                ubCost = incComputeCostModel(solver.model());
                assert lbCost == ubCost;
                if (verbosity != MaxSatConfig.Verbosity.NONE) {
                    output.println("o " + lbCost);
                }
                saveModel(solver.model());
                return optimum();
            }
        }
    }
}
