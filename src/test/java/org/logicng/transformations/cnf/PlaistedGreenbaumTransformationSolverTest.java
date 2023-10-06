// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.cnf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.RandomTag;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.io.parsers.ParserException;
import org.logicng.predicates.satisfiability.SATPredicate;
import org.logicng.predicates.satisfiability.TautologyPredicate;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;
import org.logicng.solvers.SolverState;
import org.logicng.solvers.sat.MiniSatConfig;
import org.logicng.util.FormulaCornerCases;
import org.logicng.util.FormulaRandomizer;
import org.logicng.util.FormulaRandomizerConfig;

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

            final Formula randomFormula01 = randomSATFormula(randomizer, 4, f);
            final Formula randomFormula02 = randomSATFormula(randomizer, 4, f);
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

    private static Formula randomSATFormula(final FormulaRandomizer randomizer, final int maxDepth, final FormulaFactory f) {
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
