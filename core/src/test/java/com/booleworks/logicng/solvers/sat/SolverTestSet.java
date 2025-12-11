// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.solvers.sat.SatSolverConfig.ClauseMinimization.BASIC;
import static com.booleworks.logicng.solvers.sat.SatSolverConfig.ClauseMinimization.DEEP;
import static com.booleworks.logicng.solvers.sat.SatSolverConfig.ClauseMinimization.NONE;
import static com.booleworks.logicng.solvers.sat.SatSolverConfig.CnfMethod.FACTORY_CNF;
import static com.booleworks.logicng.solvers.sat.SatSolverConfig.CnfMethod.FULL_PG_ON_SOLVER;
import static com.booleworks.logicng.solvers.sat.SatSolverConfig.CnfMethod.PG_ON_SOLVER;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.solvers.SatSolver;
import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface SolverTestSet {
    static List<Arguments> solverTestSetForParameterizedTests(final Collection<SATSolverConfigParam> variance,
                                                              final FormulaFactory f) {
        return solverTestSet(variance, f).stream().map(s -> Arguments.of(s, solverDescription(s, variance)))
                .collect(Collectors.toList());
    }

    static List<Arguments> solverSupplierTestSetForParameterizedTests(final Collection<SATSolverConfigParam> variance) {
        return solverSupplierTestSet(variance).stream()
                .map(s -> Arguments.of(s, solverDescription(s.apply(FormulaFactory.nonCaching()), variance)))
                .collect(Collectors.toList());
    }

    static List<SatSolver> solverTestSet(final Collection<SATSolverConfigParam> variance, final FormulaFactory f) {
        return solverSupplierTestSet(variance).stream().map(s -> s.apply(f)).collect(Collectors.toList());
    }

    static List<Function<FormulaFactory, SatSolver>> solverSupplierTestSet(
            final Collection<SATSolverConfigParam> variance) {
        List<SatSolverConfig> currentList = List.of(SatSolverConfig.builder().build());
        if (variance.contains(SATSolverConfigParam.PROOF_GENERATION)) {
            currentList = currentList.stream().flatMap(config -> Stream.of(
                    SatSolverConfig.copy(config).proofGeneration(false).build(),
                    SatSolverConfig.copy(config).proofGeneration(true).build()
            )).collect(Collectors.toList());
        }
        if (variance.contains(SATSolverConfigParam.USE_AT_MOST_CLAUSES)) {
            currentList = currentList.stream().flatMap(config -> Stream.of(
                    SatSolverConfig.copy(config).useAtMostClauses(false).build(),
                    SatSolverConfig.copy(config).useAtMostClauses(true).build()
            )).collect(Collectors.toList());
        }
        if (variance.contains(SATSolverConfigParam.CNF_METHOD)) {
            currentList = currentList.stream().flatMap(config -> Stream.of(
                    SatSolverConfig.copy(config).cnfMethod(FACTORY_CNF).build(),
                    SatSolverConfig.copy(config).cnfMethod(PG_ON_SOLVER).build(),
                    SatSolverConfig.copy(config).cnfMethod(FULL_PG_ON_SOLVER).build()
            )).collect(Collectors.toList());
        }
        if (variance.contains(SATSolverConfigParam.INITIAL_PHASE)) {
            currentList = currentList.stream().flatMap(config -> Stream.of(
                    SatSolverConfig.copy(config).initialPhase(false).build(),
                    SatSolverConfig.copy(config).initialPhase(true).build()
            )).collect(Collectors.toList());
        }
        if (variance.contains(SATSolverConfigParam.CLAUSE_MINIMIZATION)) {
            currentList = currentList.stream().flatMap(config -> Stream.of(
                    SatSolverConfig.copy(config).clauseMinimization(NONE).build(),
                    SatSolverConfig.copy(config).clauseMinimization(BASIC).build(),
                    SatSolverConfig.copy(config).clauseMinimization(DEEP).build()
            )).collect(Collectors.toList());
        }
        return currentList.stream()
                .map(config -> (Function<FormulaFactory, SatSolver>) f -> SatSolver.newSolver(f, config))
                .collect(Collectors.toList());
    }

    static String solverDescription(final SatSolver s, final Collection<SATSolverConfigParam> variance) {
        final SatSolverConfig config = s.getConfig();
        final List<String> elements = new ArrayList<>();
        if (variance.contains(SATSolverConfigParam.PROOF_GENERATION)) {
            elements.add((config.isProofGeneration() ? "+" : "-") + "PROOF");
        }
        if (variance.contains(SATSolverConfigParam.USE_AT_MOST_CLAUSES)) {
            elements.add((config.isUseAtMostClauses() ? "+" : "-") + "AT_MOST");
        }
        if (variance.contains(SATSolverConfigParam.CNF_METHOD)) {
            elements.add(config.getCnfMethod().name());
        }
        if (variance.contains(SATSolverConfigParam.INITIAL_PHASE)) {
            elements.add((config.getInitialPhase() ? "+" : "-") + "INITIAL_PHASE");
        }
        if (variance.contains(SATSolverConfigParam.CLAUSE_MINIMIZATION)) {
            elements.add(config.getClauseMinimization().name());
        }
        return String.join(" ", elements);
    }

    enum SATSolverConfigParam {
        PROOF_GENERATION,
        USE_AT_MOST_CLAUSES,
        CNF_METHOD,
        INITIAL_PHASE,
        CLAUSE_MINIMIZATION
    }
}
