// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.predicates.satisfiability.SATPredicate;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
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
            final SATSolver solverFactorization = MiniSat.miniSat(_c.f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FACTORY_CNF).build());
            final SATSolver solverFullPG = MiniSat.miniSat(_c.f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FULL_PG_ON_SOLVER).build());
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
            final SATSolver solver = MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FULL_PG_ON_SOLVER).auxiliaryVariablesInModels(false).build());
            final FormulaRandomizer randomizer = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().numVars(10).weightPbc(1).seed(i * 42).build());

            final Formula randomFormula01 = randomSATFormula(f, randomizer, 4);
            final Formula randomFormula02 = randomSATFormula(f, randomizer, 4);
            solver.reset();
            solver.add(randomFormula01);
            if (solver.sat() == Tristate.TRUE) {
                final List<Assignment> models = solver.enumerateAllModels();
                final Formula dnf = f.or(models.stream().map(model -> f.and(model.literals())).collect(Collectors.toList()));
                assertThat(f.equivalence(randomFormula01, dnf).holds(new TautologyPredicate(f))).isTrue();
            }
            final SolverState state = solver.saveState();
            solver.add(randomFormula02);
            if (solver.sat() == Tristate.TRUE) {
                final List<Assignment> models = solver.enumerateAllModels();
                final Formula dnf = f.or(models.stream().map(model -> f.and(model.literals())).collect(Collectors.toList()));
                assertThat(f.equivalence(f.and(randomFormula01, randomFormula02), dnf).holds(new TautologyPredicate(f))).isTrue();
            }
            solver.loadState(state);
            if (solver.sat() == Tristate.TRUE) {
                final List<Assignment> models = solver.enumerateAllModels();
                final Formula dnf = f.or(models.stream().map(model -> f.and(model.literals())).collect(Collectors.toList()));
                assertThat(f.equivalence(randomFormula01, dnf).holds(new TautologyPredicate(f))).isTrue();
            }
            solver.add(randomFormula02);
            if (solver.sat() == Tristate.TRUE) {
                final List<Assignment> models = solver.enumerateAllModels();
                final Formula dnf = f.or(models.stream().map(model -> f.and(model.literals())).collect(Collectors.toList()));
                assertThat(f.equivalence(f.and(randomFormula01, randomFormula02), dnf).holds(new TautologyPredicate(f))).isTrue();
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

    private static Formula randomSATFormula(final FormulaFactory f, final FormulaRandomizer randomizer, final int maxDepth) {
        return Stream.generate(() -> randomizer.formula(maxDepth))
                .filter(formula -> formula.holds(new SATPredicate(f)))
                .findAny().get();
    }

    private static void computeAndVerify(final Formula formula) {
        final FormulaFactory f = formula.factory();
        final SATSolver solver = MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FULL_PG_ON_SOLVER).auxiliaryVariablesInModels(false).build());
        solver.add(formula);
        final List<Assignment> models = solver.enumerateAllModels();
        final Formula dnf = f.or(models.stream().map(model -> f.and(model.literals())).collect(Collectors.toList()));
        assertThat(f.equivalence(formula, dnf).holds(new TautologyPredicate(f))).isTrue();
    }
}
