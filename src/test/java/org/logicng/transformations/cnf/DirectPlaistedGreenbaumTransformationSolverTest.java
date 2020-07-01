package org.logicng.transformations.cnf;

import org.junit.jupiter.api.Test;
import org.logicng.RandomTag;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.F;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.logicng.datastructures.Tristate.TRUE;

/**
 * Unit Tests for the class {@link DirectPlaistedGreenbaumTransformationSolver}.
 * @version 2.0.0
 * @since 2.0.0
 */
public class DirectPlaistedGreenbaumTransformationSolverTest {

    private final FormulaFactory f = F.f;

    @Test
    public void testCornerCases() {
        final FormulaFactory f = new FormulaFactory();
        final FormulaCornerCases cornerCases = new FormulaCornerCases(f);
        for (final Formula formula : cornerCases.cornerCases()) {
            final SATSolver solverFactorization = MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FACTORY_CNF).build());
            final SATSolver solverDirectPG = MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.DIRECT_PG_ON_SOLVER).build());
            solverFactorization.add(formula);
            solverDirectPG.add(formula);
            assertThat(solverFactorization.sat() == solverDirectPG.sat()).isTrue();
        }
    }

    @Test
    @RandomTag
    public void random() {
        for (int i = 0; i < 1000; i++) {
            final FormulaFactory f = new FormulaFactory();
            final SATSolver solver = MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.DIRECT_PG_ON_SOLVER).auxiliaryVariablesInModels(false).build());
            final FormulaRandomizer randomizer = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().numVars(10).weightPbc(1).seed(42).build());

            final Formula randomFormula01 = randomSATFormula(randomizer, 4, f);
            final Formula randomFormula02 = randomSATFormula(randomizer, 4, f);
            solver.reset();
            solver.add(randomFormula01);
            if (solver.sat() == TRUE) {
                final List<Assignment> models = solver.enumerateAllModels();
                final Formula dnf = f.or(models.stream().map(model -> f.and(model.literals())).collect(Collectors.toList()));
                assertThat(f.equivalence(randomFormula01, dnf).holds(new TautologyPredicate(f))).isTrue();
            }
            final SolverState state = solver.saveState();
            solver.add(randomFormula02);
            if (solver.sat() == TRUE) {
                final List<Assignment> models = solver.enumerateAllModels();
                final Formula dnf = f.or(models.stream().map(model -> f.and(model.literals())).collect(Collectors.toList()));
                assertThat(f.equivalence(f.and(randomFormula01, randomFormula02), dnf).holds(new TautologyPredicate(f))).isTrue();
            }
            solver.loadState(state);
            if (solver.sat() == TRUE) {
                final List<Assignment> models = solver.enumerateAllModels();
                final Formula dnf = f.or(models.stream().map(model -> f.and(model.literals())).collect(Collectors.toList()));
                assertThat(f.equivalence(randomFormula01, dnf).holds(new TautologyPredicate(f))).isTrue();
            }
            solver.add(randomFormula02);
            if (solver.sat() == TRUE) {
                final List<Assignment> models = solver.enumerateAllModels();
                final Formula dnf = f.or(models.stream().map(model -> f.and(model.literals())).collect(Collectors.toList()));
                assertThat(f.equivalence(f.and(randomFormula01, randomFormula02), dnf).holds(new TautologyPredicate(f))).isTrue();
            }
        }
    }

    private static Formula randomSATFormula(final FormulaRandomizer randomizer, final int maxDepth, final FormulaFactory f) {
        return Stream.generate(() -> randomizer.formula(maxDepth))
                .filter(formula -> formula.holds(new SATPredicate(f)))
                .findAny().get();
    }

    @Test
    public void simple() throws ParserException {
        computeAndVerify(this.f.parse("(X => (A & B)) | ~(A & B)"));
        computeAndVerify(this.f.parse("~((A | (C & D)) & ((X & ~Z) | (H & E)))"));

        computeAndVerify(this.f.parse("A | (B | ~C) & (D | ~E)"));
        computeAndVerify(this.f.parse("A | B & (C | D)"));

        computeAndVerify(this.f.parse("~(A&B)|X"));
        computeAndVerify(this.f.parse("~(~(A&B)|X)"));

        computeAndVerify(this.f.parse("~(~(A&B)|X)"));
        computeAndVerify(this.f.parse("~(A&B=>X)"));

        computeAndVerify(this.f.parse("A&B => X"));
        computeAndVerify(this.f.parse("~(A&B=>X)"));

        computeAndVerify(this.f.parse("A&B <=> X"));
        computeAndVerify(this.f.parse("~(A&B<=>X)"));

        computeAndVerify(this.f.parse("~(A&B)"));

        computeAndVerify(this.f.parse("A & (B | A => (A <=> ~B))"));
        computeAndVerify(this.f.parse("(A => ~A) <=> (B <=> (~A => B))"));
    }

    private static void computeAndVerify(final Formula formula) {
        final FormulaFactory f = formula.factory();
        final SATSolver solver = MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.DIRECT_PG_ON_SOLVER).auxiliaryVariablesInModels(false).build());
        solver.add(formula);
        final List<Assignment> models = solver.enumerateAllModels();
        final Formula dnf = f.or(models.stream().map(model -> f.and(model.literals())).collect(Collectors.toList()));
        assertThat(f.equivalence(formula, dnf).holds(new TautologyPredicate(f))).isTrue();
    }
}