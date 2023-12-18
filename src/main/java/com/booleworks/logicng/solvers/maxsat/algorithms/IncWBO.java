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

import static com.booleworks.logicng.datastructures.Tristate.FALSE;
import static com.booleworks.logicng.datastructures.Tristate.UNDEF;
import static com.booleworks.logicng.handlers.Handler.aborted;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.solvers.maxsat.encodings.Encoder;
import com.booleworks.logicng.solvers.sat.MiniSatStyleSolver;
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
     */
    public IncWBO() {
        this(MaxSATConfig.builder().build());
    }

    /**
     * Constructs a new solver with a given configuration.
     * @param config the configuration
     */
    public IncWBO(final MaxSATConfig config) {
        super(config);
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
    public MaxSATResult search() {
        nbInitialVariables = nVars();
        if (currentWeight == 1) {
            problemType = ProblemType.UNWEIGHTED;
            weightStrategy = MaxSATConfig.WeightStrategy.NONE;
        }
        if (symmetryStrategy) {
            initSymmetry();
        }
        if (problemType == ProblemType.UNWEIGHTED || weightStrategy == MaxSATConfig.WeightStrategy.NONE) {
            return normalSearch();
        } else if (weightStrategy == MaxSATConfig.WeightStrategy.NORMAL ||
                weightStrategy == MaxSATConfig.WeightStrategy.DIVERSIFY) {
                    return weightSearch();
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
                    assert MiniSatStyleSolver.var(relaxationMapping.get(p).get(j)) > nbInitialVariables;
                    coreIntersection[core].push(relaxationMapping.get(p).get(j));
                }
                for (int j = 0; j < addCores.size(); j++) {
                    final int core = addCores.get(j);
                    final int b = softMapping.get(p).size() - 1;
                    assert b < relaxationMapping.get(p).size();
                    assert MiniSatStyleSolver.var(relaxationMapping.get(p).get(b)) > nbInitialVariables;
                    coreIntersectionCurrent[core].push(relaxationMapping.get(p).get(b));
                }
                for (int k = 0; k < coreList.size(); k++) {
                    for (int m = 0; m < coreIntersection[coreList.get(k)].size(); m++) {
                        for (int j = m + 1; j < coreIntersectionCurrent[coreList.get(k)].size(); j++) {
                            final LNGIntVector clause = new LNGIntVector();
                            clause.push(MiniSatStyleSolver.not(coreIntersection[coreList.get(k)].get(m)));
                            clause.push(MiniSatStyleSolver.not(coreIntersectionCurrent[coreList.get(k)].get(j)));
                            Pair<Integer, Integer> symClause =
                                    new Pair<>(MiniSatStyleSolver.var(coreIntersection[coreList.get(k)].get(m)),
                                            MiniSatStyleSolver.var(coreIntersectionCurrent[coreList.get(k)].get(j)));
                            if (MiniSatStyleSolver.var(coreIntersection[coreList.get(k)].get(m)) >
                                    MiniSatStyleSolver.var(coreIntersectionCurrent[coreList.get(k)].get(j))) {
                                symClause = new Pair<>(
                                        MiniSatStyleSolver.var(coreIntersectionCurrent[coreList.get(k)].get(j)),
                                        MiniSatStyleSolver.var(coreIntersection[coreList.get(k)].get(m)));
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
    protected MaxSATResult weightSearch() {
        assert weightStrategy == MaxSATConfig.WeightStrategy.NORMAL ||
                weightStrategy == MaxSATConfig.WeightStrategy.DIVERSIFY;
        final Tristate unsatResult = unsatSearch();
        if (unsatResult == UNDEF) {
            return MaxSATResult.UNDEF;
        } else if (unsatResult == FALSE) {
            return MaxSATResult.UNSATISFIABLE;
        }
        initAssumptions(assumptions);
        updateCurrentWeight(weightStrategy);
        incrementalBuildWeightSolver(weightStrategy);
        incSoft.growTo(nSoft(), false);
        while (true) {
            assumptions.clear();
            for (int i = 0; i < incSoft.size(); i++) {
                if (!incSoft.get(i)) {
                    assumptions.push(MiniSatStyleSolver.not(softClauses.get(i).assumptionVar()));
                }
            }
            final SATHandler satHandler = satHandler();
            final Tristate res = searchSATSolver(solver, satHandler, assumptions);
            if (aborted(satHandler)) {
                return MaxSATResult.UNDEF;
            } else if (res == FALSE) {
                nbCores++;
                assert solver.conflict().size() > 0;
                final int coreCost = computeCostCore(solver.conflict());
                lbCost += coreCost;
                if (verbosity != MaxSATConfig.Verbosity.NONE) {
                    output.printf("c LB : %d CS : %d W : %d%n", lbCost, solver.conflict().size(), coreCost);
                }
                if (!foundLowerBound(lbCost, null)) {
                    return MaxSATResult.UNDEF;
                }
                relaxCore(solver.conflict(), coreCost);
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
                    return MaxSATResult.OPTIMUM;
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
                        return MaxSATResult.OPTIMUM;
                    } else if (!foundUpperBound(ubCost, null)) {
                        return MaxSATResult.UNDEF;
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
                assert MiniSatStyleSolver.var(softClauses.get(i).clause().get(j)) < currentModel.size();
                if ((MiniSatStyleSolver.sign(softClauses.get(i).clause().get(j)) &&
                        !currentModel.get(MiniSatStyleSolver.var(softClauses.get(i).clause().get(j)))) ||
                        (!MiniSatStyleSolver.sign(softClauses.get(i).clause().get(j)) &&
                                currentModel.get(MiniSatStyleSolver.var(softClauses.get(i).clause().get(j))))) {
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
    protected MaxSATResult normalSearch() {
        final Tristate unsatResult = unsatSearch();
        if (unsatResult == UNDEF) {
            return MaxSATResult.UNDEF;
        } else if (unsatResult == FALSE) {
            return MaxSATResult.UNSATISFIABLE;
        }
        initAssumptions(assumptions);
        solver = rebuildSolver();
        incSoft.growTo(nSoft(), false);
        while (true) {
            assumptions.clear();
            for (int i = 0; i < incSoft.size(); i++) {
                if (!incSoft.get(i)) {
                    assumptions.push(MiniSatStyleSolver.not(softClauses.get(i).assumptionVar()));
                }
            }
            final SATHandler satHandler = satHandler();
            final Tristate res = searchSATSolver(solver, satHandler, assumptions);
            if (aborted(satHandler)) {
                return MaxSATResult.UNDEF;
            } else if (res == FALSE) {
                nbCores++;
                assert solver.conflict().size() > 0;
                final int coreCost = computeCostCore(solver.conflict());
                lbCost += coreCost;
                if (verbosity != MaxSATConfig.Verbosity.NONE) {
                    output.printf("c LB : %d CS : %d W : %d%n", lbCost, solver.conflict().size(), coreCost);
                }
                if (lbCost == ubCost) {
                    if (verbosity != MaxSATConfig.Verbosity.NONE) {
                        output.println("c LB = UB");
                    }
                    return MaxSATResult.OPTIMUM;
                }
                if (!foundLowerBound(lbCost, null)) {
                    return MaxSATResult.UNDEF;
                }
                relaxCore(solver.conflict(), coreCost);
            } else {
                nbSatisfiable++;
                ubCost = incComputeCostModel(solver.model());
                assert lbCost == ubCost;
                if (verbosity != MaxSATConfig.Verbosity.NONE) {
                    output.println("o " + lbCost);
                }
                saveModel(solver.model());
                return MaxSATResult.OPTIMUM;
            }
        }
    }
}
