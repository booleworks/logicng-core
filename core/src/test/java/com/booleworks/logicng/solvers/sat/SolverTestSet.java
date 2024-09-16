package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.solvers.sat.SATSolverConfig.CNFMethod.FACTORY_CNF;
import static com.booleworks.logicng.solvers.sat.SATSolverConfig.CNFMethod.FULL_PG_ON_SOLVER;
import static com.booleworks.logicng.solvers.sat.SATSolverConfig.CNFMethod.PG_ON_SOLVER;
import static com.booleworks.logicng.solvers.sat.SATSolverConfig.ClauseMinimization.BASIC;
import static com.booleworks.logicng.solvers.sat.SATSolverConfig.ClauseMinimization.DEEP;
import static com.booleworks.logicng.solvers.sat.SATSolverConfig.ClauseMinimization.NONE;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.solvers.SATSolver;
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

    static List<SATSolver> solverTestSet(final Collection<SATSolverConfigParam> variance, final FormulaFactory f) {
        return solverSupplierTestSet(variance).stream().map(s -> s.apply(f)).collect(Collectors.toList());
    }

    static List<Function<FormulaFactory, SATSolver>> solverSupplierTestSet(
            final Collection<SATSolverConfigParam> variance) {
        List<SATSolverConfig> currentList = List.of(SATSolverConfig.builder().build());
        if (variance.contains(SATSolverConfigParam.PROOF_GENERATION)) {
            currentList = currentList.stream().flatMap(config -> Stream.of(
                    SATSolverConfig.copy(config).proofGeneration(false).build(),
                    SATSolverConfig.copy(config).proofGeneration(true).build()
            )).collect(Collectors.toList());
        }
        if (variance.contains(SATSolverConfigParam.USE_AT_MOST_CLAUSES)) {
            currentList = currentList.stream().flatMap(config -> Stream.of(
                    SATSolverConfig.copy(config).useAtMostClauses(false).build(),
                    SATSolverConfig.copy(config).useAtMostClauses(true).build()
            )).collect(Collectors.toList());
        }
        if (variance.contains(SATSolverConfigParam.CNF_METHOD)) {
            currentList = currentList.stream().flatMap(config -> Stream.of(
                    SATSolverConfig.copy(config).cnfMethod(FACTORY_CNF).build(),
                    SATSolverConfig.copy(config).cnfMethod(PG_ON_SOLVER).build(),
                    SATSolverConfig.copy(config).cnfMethod(FULL_PG_ON_SOLVER).build()
            )).collect(Collectors.toList());
        }
        if (variance.contains(SATSolverConfigParam.INITIAL_PHASE)) {
            currentList = currentList.stream().flatMap(config -> Stream.of(
                    SATSolverConfig.copy(config).initialPhase(false).build(),
                    SATSolverConfig.copy(config).initialPhase(true).build()
            )).collect(Collectors.toList());
        }
        if (variance.contains(SATSolverConfigParam.CLAUSE_MINIMIZATION)) {
            currentList = currentList.stream().flatMap(config -> Stream.of(
                    SATSolverConfig.copy(config).clauseMinimization(NONE).build(),
                    SATSolverConfig.copy(config).clauseMinimization(BASIC).build(),
                    SATSolverConfig.copy(config).clauseMinimization(DEEP).build()
            )).collect(Collectors.toList());
        }
        return currentList.stream()
                .map(config -> (Function<FormulaFactory, SATSolver>) f -> SATSolver.newSolver(f, config))
                .collect(Collectors.toList());
    }

    static String solverDescription(final SATSolver s, final Collection<SATSolverConfigParam> variance) {
        final SATSolverConfig config = s.getConfig();
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
