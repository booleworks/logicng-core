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

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.events.LNGEvent;
import com.booleworks.logicng.solvers.maxsat.InternalMaxSATResult;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;
import com.booleworks.logicng.util.Pair;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Incremental WBO solver.
 * @version 2.1.0
 * @since 1.0
 */
public class IncWBO extends WBO {

    protected final Encoder encoder;
    protected final LNGBooleanVector incSoft;
    protected final PrintStream output;
    protected boolean firstBuild;

    /**
     * Constructs a new solver with default values.
     * @param f the formula factory
     */
    public IncWBO(final FormulaFactory f) {
        this(f, MaxSATConfig.builder().build());
    }

    /**
     * Constructs a new solver with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public IncWBO(final FormulaFactory f, final MaxSATConfig config) {
        super(f, config);
        solver = null;
        verbosity = config.verbosity;
        nbCurrentSoft = 0;
        weightStrategy = config.weightStrategy;
        symmetryStrategy = config.symmetry;
        symmetryBreakingLimit = config.limit;
        firstBuild = true;
        coreMapping = new TreeMap<>();
        assumptions = new LNGIntVector();
        indexSoftCore = new LNGIntVector();
        softMapping = new LNGVector<>();
        relaxationMapping = new LNGVector<>();
        duplicatedSymmetryClauses = new HashSet<>();
        encoder = new Encoder(MaxSATConfig.CardinalityEncoding.TOTALIZER);
        encoder.setAMOEncoding(config.amoEncoding);
        incSoft = new LNGBooleanVector();
        output = config.output;
    }

    @Override
    protected LNGResult<InternalMaxSATResult> internalSearch(final ComputationHandler handler) {
        nbInitialVariables = nVars();
        if (currentWeight == 1) {
            problemType = ProblemType.UNWEIGHTED;
            weightStrategy = MaxSATConfig.WeightStrategy.NONE;
        }
        if (symmetryStrategy) {
            initSymmetry();
        }
        if (problemType == ProblemType.UNWEIGHTED || weightStrategy == MaxSATConfig.WeightStrategy.NONE) {
            return normalSearch(handler);
        } else if (weightStrategy == MaxSATConfig.WeightStrategy.NORMAL ||
                weightStrategy == MaxSATConfig.WeightStrategy.DIVERSIFY) {
            return weightSearch(handler);
        }
        throw new IllegalArgumentException("Unknown problem type.");
    }

    protected void incrementalBuildWeightSolver(final MaxSATConfig.WeightStrategy strategy) {
        assert strategy == MaxSATConfig.WeightStrategy.NORMAL || strategy == MaxSATConfig.WeightStrategy.DIVERSIFY;
        if (firstBuild) {
            solver = newSATSolver();
            for (int i = 0; i < nVars(); i++) {
                newSATVariable(solver);
            }
            for (int i = 0; i < nHard(); i++) {
                solver.addClause(hardClauses.get(i).clause(), null);
            }
            if (symmetryStrategy) {
                symmetryBreaking();
            }
            firstBuild = false;
        }
        LNGIntVector clause;
        nbCurrentSoft = 0;
        for (int i = 0; i < nSoft(); i++) {
            if (softClauses.get(i).weight() >= currentWeight && softClauses.get(i).weight() != 0) {
                nbCurrentSoft++;
                clause = new LNGIntVector(softClauses.get(i).clause());
                for (int j = 0; j < softClauses.get(i).relaxationVars().size(); j++) {
                    clause.push(softClauses.get(i).relaxationVars().get(j));
                }
                clause.push(softClauses.get(i).assumptionVar());
                solver.addClause(clause, null);
            }
        }
    }

    protected void relaxCore(final LNGIntVector conflict, final int weightCore) {
        assert conflict.size() > 0;
        assert weightCore > 0;
        final LNGIntVector lits = new LNGIntVector();
        for (int i = 0; i < conflict.size(); i++) {
            final int indexSoft = coreMapping.get(conflict.get(i));
            if (softClauses.get(indexSoft).weight() == weightCore) {
                final LNGIntVector clause = new LNGIntVector(softClauses.get(indexSoft).clause());
                final LNGIntVector vars = new LNGIntVector(softClauses.get(indexSoft).relaxationVars());
                final int p = newLiteral(false);
                newSATVariable(solver);
                vars.push(p);
                lits.push(p);
                addSoftClause(weightCore, clause, vars);
                final int l = newLiteral(false);
                newSATVariable(solver);
                softClauses.get(nSoft() - 1).setAssumptionVar(l);
                coreMapping.put(l, nSoft() - 1);
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
                    softMapping.push(new LNGIntVector(softMapping.get(indexSoft)));
                    softMapping.get(indexSoft).clear();
                    relaxationMapping.push(new LNGIntVector(relaxationMapping.get(indexSoft)));
                    relaxationMapping.get(indexSoft).clear();
                    symmetryLog(nSoft() - 1);
                }
            } else {
                assert softClauses.get(indexSoft).weight() - weightCore > 0;
                softClauses.get(indexSoft).setWeight(softClauses.get(indexSoft).weight() - weightCore);
                LNGIntVector clause = new LNGIntVector(softClauses.get(indexSoft).clause());
                LNGIntVector vars = new LNGIntVector(softClauses.get(indexSoft).relaxationVars());
                addSoftClause(softClauses.get(indexSoft).weight(), clause, vars);
                if (symmetryStrategy) {
                    softMapping.push(new LNGIntVector(softMapping.get(indexSoft)));
                    softMapping.get(indexSoft).clear();
                    relaxationMapping.push(new LNGIntVector(relaxationMapping.get(indexSoft)));
                    relaxationMapping.get(indexSoft).clear();
                }
                incSoft.set(indexSoft, true);
                int l = newLiteral(false);
                newSATVariable(solver);
                softClauses.get(nSoft() - 1).setAssumptionVar(l);
                coreMapping.put(l, nSoft() - 1);
                incSoft.push(false);
                for (int j = 0; j < vars.size(); j++) {
                    clause.push(vars.get(j));
                }
                clause.push(l);
                solver.addClause(clause, null);
                clause.clear();
                vars.clear();
                clause = new LNGIntVector(softClauses.get(indexSoft).clause());
                vars = new LNGIntVector(softClauses.get(indexSoft).relaxationVars());
                l = newLiteral(false);
                newSATVariable(solver);
                vars.push(l);
                lits.push(l);
                addSoftClause(weightCore, clause, vars);
                l = newLiteral(false);
                newSATVariable(solver);
                softClauses.get(nSoft() - 1).setAssumptionVar(l);
                coreMapping.put(l, nSoft() - 1);
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
                    softMapping.push(new LNGIntVector());
                    relaxationMapping.push(new LNGIntVector());
                    symmetryLog(nSoft() - 1);
                }
            }
        }
        encoder.encodeAMO(solver, lits);
        nbVars = solver.nVars();
        if (symmetryStrategy) {
            symmetryBreaking();
        }
        sumSizeCores += conflict.size();
    }

    @Override
    protected void symmetryBreaking() {
        if (indexSoftCore.size() != 0 && nbSymmetryClauses < symmetryBreakingLimit) {
            final LNGIntVector[] coreIntersection = new LNGIntVector[nbCores];
            final LNGIntVector[] coreIntersectionCurrent = new LNGIntVector[nbCores];
            for (int i = 0; i < nbCores; i++) {
                coreIntersection[i] = new LNGIntVector();
                coreIntersectionCurrent[i] = new LNGIntVector();
            }
            final LNGIntVector coreList = new LNGIntVector();
            for (int i = 0; i < indexSoftCore.size(); i++) {
                final int p = indexSoftCore.get(i);
                final LNGIntVector addCores = new LNGIntVector();
                for (int j = 0; j < softMapping.get(p).size() - 1; j++) {
                    final int core = softMapping.get(p).get(j);
                    addCores.push(core);
                    if (coreIntersection[core].size() == 0) {
                        coreList.push(core);
                    }
                    assert j < relaxationMapping.get(p).size();
                    assert LNGCoreSolver.var(relaxationMapping.get(p).get(j)) > nbInitialVariables;
                    coreIntersection[core].push(relaxationMapping.get(p).get(j));
                }
                for (int j = 0; j < addCores.size(); j++) {
                    final int core = addCores.get(j);
                    final int b = softMapping.get(p).size() - 1;
                    assert b < relaxationMapping.get(p).size();
                    assert LNGCoreSolver.var(relaxationMapping.get(p).get(b)) > nbInitialVariables;
                    coreIntersectionCurrent[core].push(relaxationMapping.get(p).get(b));
                }
                for (int k = 0; k < coreList.size(); k++) {
                    for (int m = 0; m < coreIntersection[coreList.get(k)].size(); m++) {
                        for (int j = m + 1; j < coreIntersectionCurrent[coreList.get(k)].size(); j++) {
                            final LNGIntVector clause = new LNGIntVector();
                            clause.push(LNGCoreSolver.not(coreIntersection[coreList.get(k)].get(m)));
                            clause.push(LNGCoreSolver.not(coreIntersectionCurrent[coreList.get(k)].get(j)));
                            Pair<Integer, Integer> symClause =
                                    new Pair<>(LNGCoreSolver.var(coreIntersection[coreList.get(k)].get(m)),
                                            LNGCoreSolver.var(coreIntersectionCurrent[coreList.get(k)].get(j)));
                            if (LNGCoreSolver.var(coreIntersection[coreList.get(k)].get(m)) >
                                    LNGCoreSolver.var(coreIntersectionCurrent[coreList.get(k)].get(j))) {
                                symClause =
                                        new Pair<>(LNGCoreSolver.var(coreIntersectionCurrent[coreList.get(k)].get(j)),
                                                LNGCoreSolver.var(coreIntersection[coreList.get(k)].get(m)));
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
    protected LNGResult<InternalMaxSATResult> weightSearch(final ComputationHandler handler) {
        assert weightStrategy == MaxSATConfig.WeightStrategy.NORMAL ||
                weightStrategy == MaxSATConfig.WeightStrategy.DIVERSIFY;
        final LNGResult<Boolean> unsatResult = unsatSearch(handler);
        if (!unsatResult.isSuccess()) {
            return LNGResult.aborted(unsatResult.getAbortionEvent());
        } else if (!unsatResult.getResult()) {
            return LNGResult.of(InternalMaxSATResult.unsatisfiable());
        }
        initAssumptions(assumptions);
        updateCurrentWeight(weightStrategy);
        incrementalBuildWeightSolver(weightStrategy);
        incSoft.growTo(nSoft(), false);
        while (true) {
            assumptions.clear();
            for (int i = 0; i < incSoft.size(); i++) {
                if (!incSoft.get(i)) {
                    assumptions.push(LNGCoreSolver.not(softClauses.get(i).assumptionVar()));
                }
            }
            final LNGResult<Boolean> res = searchSATSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LNGResult.aborted(res.getAbortionEvent());
            } else if (!res.getResult()) {
                nbCores++;
                assert solver.assumptionsConflict().size() > 0;
                final int coreCost = computeCostCore(solver.assumptionsConflict());
                lbCost += coreCost;
                if (verbosity != MaxSATConfig.Verbosity.NONE) {
                    output.printf("c LB : %d CS : %d W : %d%n", lbCost, solver.assumptionsConflict().size(), coreCost);
                }
                final LNGEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                if (lowerBoundEvent != null) {
                    return LNGResult.aborted(lowerBoundEvent);
                }
                relaxCore(solver.assumptionsConflict(), coreCost);
                incrementalBuildWeightSolver(weightStrategy);
            } else {
                nbSatisfiable++;
                if (nbCurrentSoft == nSoft()) {
                    assert incComputeCostModel(solver.model()) == lbCost;
                    if (lbCost == ubCost && verbosity != MaxSATConfig.Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    if (lbCost < ubCost) {
                        ubCost = lbCost;
                        saveModel(solver.model());
                        if (verbosity != MaxSATConfig.Verbosity.NONE) {
                            output.println("o " + lbCost);
                        }
                    }
                    return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                } else {
                    updateCurrentWeight(weightStrategy);
                    final int cost = incComputeCostModel(solver.model());
                    if (cost < ubCost) {
                        ubCost = cost;
                        saveModel(solver.model());
                        if (verbosity != MaxSATConfig.Verbosity.NONE) {
                            output.println("o " + ubCost);
                        }
                    }
                    if (lbCost == ubCost) {
                        if (verbosity != MaxSATConfig.Verbosity.NONE) {
                            output.println("c LB = UB");
                        }
                        return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                    } else {
                        final LNGEvent upperBoundEvent = foundUpperBound(ubCost, handler);
                        if (upperBoundEvent != null) {
                            return LNGResult.aborted(upperBoundEvent);
                        }
                    }
                    incrementalBuildWeightSolver(weightStrategy);
                }
            }
        }
    }

    protected int incComputeCostModel(final LNGBooleanVector currentModel) {
        assert currentModel.size() != 0;
        int currentCost = 0;
        for (int i = 0; i < nSoft(); i++) {
            boolean unsatisfied = true;
            for (int j = 0; j < softClauses.get(i).clause().size(); j++) {
                if (incSoft.get(i)) {
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

    @Override
    protected LNGResult<InternalMaxSATResult> normalSearch(final ComputationHandler handler) {
        final LNGResult<Boolean> unsatResult = unsatSearch(handler);
        if (!unsatResult.isSuccess()) {
            return LNGResult.aborted(unsatResult.getAbortionEvent());
        } else if (!unsatResult.getResult()) {
            return LNGResult.of(InternalMaxSATResult.unsatisfiable());
        }
        initAssumptions(assumptions);
        solver = rebuildSolver();
        incSoft.growTo(nSoft(), false);
        while (true) {
            assumptions.clear();
            for (int i = 0; i < incSoft.size(); i++) {
                if (!incSoft.get(i)) {
                    assumptions.push(LNGCoreSolver.not(softClauses.get(i).assumptionVar()));
                }
            }
            final LNGResult<Boolean> res = searchSATSolver(solver, handler, assumptions);
            if (!res.isSuccess()) {
                return LNGResult.aborted(res.getAbortionEvent());
            } else if (!res.getResult()) {
                nbCores++;
                assert solver.assumptionsConflict().size() > 0;
                final int coreCost = computeCostCore(solver.assumptionsConflict());
                lbCost += coreCost;
                if (verbosity != MaxSATConfig.Verbosity.NONE) {
                    output.printf("c LB : %d CS : %d W : %d%n", lbCost, solver.assumptionsConflict().size(), coreCost);
                }
                if (lbCost == ubCost) {
                    if (verbosity != MaxSATConfig.Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
                }
                final LNGEvent lowerBoundEvent = foundLowerBound(lbCost, handler);
                if (lowerBoundEvent != null) {
                    return LNGResult.aborted(lowerBoundEvent);
                }
                relaxCore(solver.assumptionsConflict(), coreCost);
            } else {
                nbSatisfiable++;
                ubCost = incComputeCostModel(solver.model());
                assert lbCost == ubCost;
                if (verbosity != MaxSATConfig.Verbosity.NONE) {
                    output.println("o " + lbCost);
                }
                saveModel(solver.model());
                return LNGResult.of(InternalMaxSATResult.optimum(ubCost, model));
            }
        }
    }
}
