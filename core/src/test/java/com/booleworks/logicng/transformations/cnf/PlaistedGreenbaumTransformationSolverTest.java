// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import static com.booleworks.logicng.util.FormulaHelper.variables;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.predicates.satisfiability.SATPredicate;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import com.booleworks.logicng.util.FormulaCornerCases;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlaistedGreenbaumTransformationSolverTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCornerCases(final FormulaContext _c) {
        final FormulaCornerCases cornerCases = new FormulaCornerCases(_c.f);
        for (final Formula formula : cornerCases.cornerCases()) {
            final SATSolver solverFactorization = SATSolver.newSolver(_c.f,
                    SATSolverConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.FACTORY_CNF).build());
            final SATSolver solverFullPG = SATSolver.newSolver(_c.f,
                    SATSolverConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.FULL_PG_ON_SOLVER).build());
            solverFactorization.add(formula);
            solverFullPG.add(formula);
            assertThat(solverFactorization.sat() == solverFullPG.sat()).isTrue();
        }
    }

    @Test
    @RandomTag
    public void randomCaching() {
        for (int i = 0; i < 500; i++) {
            final FormulaFactory f = FormulaFactory.caching();
            final SATSolver solver = SATSolver.newSolver(f,
                    SATSolverConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.FULL_PG_ON_SOLVER).build());
            final FormulaRandomizer randomizer = new FormulaRandomizer(f,
                    FormulaRandomizerConfig.builder().numVars(10).weightPbc(1).seed(i * 42).build());

            final Formula randomFormula01 = randomSATFormula(f, randomizer, 4);
            final Formula randomFormula02 = randomSATFormula(f, randomizer, 4);
            solver.add(randomFormula01);
            if (solver.sat()) {
                final List<Model> models = solver.enumerateAllModels(randomFormula01.variables(f));
                final Formula dnf =
                        f.or(models.stream().map(model -> f.and(model.getLiterals())).collect(Collectors.toList()));
                assertThat(f.equivalence(randomFormula01, dnf).holds(new TautologyPredicate(f))).isTrue();
            }
            final SolverState state = solver.saveState();
            solver.add(randomFormula02);
            if (solver.sat()) {
                final List<Model> models = solver.enumerateAllModels(variables(f, randomFormula01, randomFormula02));
                final Formula dnf =
                        f.or(models.stream().map(model -> f.and(model.getLiterals())).collect(Collectors.toList()));
                assertThat(f.equivalence(f.and(randomFormula01, randomFormula02), dnf).holds(new TautologyPredicate(f)))
                        .isTrue();
            }
            solver.loadState(state);
            if (solver.sat()) {
                final List<Model> models = solver.enumerateAllModels(randomFormula01.variables(f));
                final Formula dnf =
                        f.or(models.stream().map(model -> f.and(model.getLiterals())).collect(Collectors.toList()));
                assertThat(f.equivalence(randomFormula01, dnf).holds(new TautologyPredicate(f))).isTrue();
            }
            solver.add(randomFormula02);
            if (solver.sat()) {
                final List<Model> models = solver.enumerateAllModels(variables(f, randomFormula01, randomFormula02));
                final Formula dnf =
                        f.or(models.stream().map(model -> f.and(model.getLiterals())).collect(Collectors.toList()));
                assertThat(f.equivalence(f.and(randomFormula01, randomFormula02), dnf).holds(new TautologyPredicate(f)))
                        .isTrue();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void simple(final FormulaContext _c) throws ParserException {
        computeAndVerify(_c.p.parse("(X => (A & B)) | ~(A & B)"));
        computeAndVerify(_c.p.parse("~((A | (C & D)) & ((X & ~Z) | (H & E)))"));

        computeAndVerify(_c.p.parse("A | (B | ~C) & (D | ~E)"));
        computeAndVerify(_c.p.parse("A | B & (C | D)"));

        computeAndVerify(_c.p.parse("~(A&B)|X"));
        computeAndVerify(_c.p.parse("~(~(A&B)|X)"));

        computeAndVerify(_c.p.parse("~(~(A&B)|X)"));
        computeAndVerify(_c.p.parse("~(A&B=>X)"));

        computeAndVerify(_c.p.parse("A&B => X"));
        computeAndVerify(_c.p.parse("~(A&B=>X)"));

        computeAndVerify(_c.p.parse("A&B <=> X"));
        computeAndVerify(_c.p.parse("~(A&B<=>X)"));

        computeAndVerify(_c.p.parse("~(A&B)"));

        computeAndVerify(_c.p.parse("A & (B | A => (A <=> ~B))"));
        computeAndVerify(_c.p.parse("(A => ~A) <=> (B <=> (~A => B))"));
    }

    private static Formula randomSATFormula(final FormulaFactory f, final FormulaRandomizer randomizer,
                                            final int maxDepth) {
        return Stream.generate(() -> randomizer.formula(maxDepth))
                .filter(formula -> formula.holds(new SATPredicate(f)))
                .findAny().get();
    }

    private static void computeAndVerify(final Formula formula) {
        final FormulaFactory f = formula.getFactory();
        final SATSolver solver = SATSolver.newSolver(f,
                SATSolverConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.FULL_PG_ON_SOLVER).build());
        solver.add(formula);
        final List<Model> models = solver.enumerateAllModels(formula.variables(f));
        final Formula dnf = f.or(models.stream().map(model -> f.and(model.getLiterals())).collect(Collectors.toList()));
        assertThat(f.equivalence(formula, dnf).holds(new TautologyPredicate(f))).isTrue();
    }
}
