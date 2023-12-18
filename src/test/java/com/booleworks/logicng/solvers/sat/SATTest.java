// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.util.FormulaHelper.variables;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ModelEnumerationHandler;
import com.booleworks.logicng.handlers.NumberOfModelsHandler;
import com.booleworks.logicng.handlers.TimeoutModelEnumerationHandler;
import com.booleworks.logicng.handlers.TimeoutSATHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.functions.FormulaOnSolverFunction;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.UpZeroLiteralsFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.NoSplitModelEnumerationStrategy;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class SATTest extends TestWithExampleFormulas implements LogicNGTest {

    private final SATSolver[] solvers;
    private final PigeonHoleGenerator pg;
    private final PropositionalParser parser;
    private final String[] testStrings;

    public SATTest() {
        pg = new PigeonHoleGenerator(f);
        parser = new PropositionalParser(f);
        solvers = new SATSolver[6];
        solvers[0] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(true).build());
        solvers[1] = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).build());
        solvers[2] = MiniSat.glucose(f, MiniSatConfig.builder().incremental(false).build(),
                GlucoseConfig.builder().build());
        solvers[3] = MiniSat.miniCard(f, MiniSatConfig.builder().incremental(true).build());
        solvers[4] = MiniSat.miniCard(f, MiniSatConfig.builder().incremental(false).build());
        solvers[5] =
                MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER).build());

        testStrings = new String[8];
        testStrings[0] = "MiniSat2Solver{result=UNDEF, incremental=true}";
        testStrings[1] = "MiniSat2Solver{result=UNDEF, incremental=false}";
        testStrings[2] = "GlucoseSyrup{result=UNDEF, incremental=false}";
        testStrings[3] = "MiniCard{result=UNDEF, incremental=true}";
        testStrings[4] = "MiniCard{result=UNDEF, incremental=false}";
        testStrings[5] = "MiniSat2Solver{result=UNDEF, incremental=true}";
        testStrings[6] = "MiniSat2Solver{result=UNDEF, incremental=true}";
        testStrings[7] = "MiniSat2Solver{result=UNDEF, incremental=true}";
    }

    @Test
    public void testTrue() {
        for (final SATSolver s : solvers) {
            s.add(TRUE);
            assertSolverSat(s);
            Assertions.assertThat(s.model(f.variables()).size()).isEqualTo(0);
            s.reset();
        }
    }

    @Test
    public void testFalse() {
        for (final SATSolver s : solvers) {
            s.add(FALSE);
            assertSolverUnsat(s);
            Assertions.assertThat(s.model(f.variables())).isNull();
            s.reset();
        }
    }

    @Test
    public void testLiterals() {
        for (final SATSolver s : solvers) {
            s.add(A);
            assertSolverSat(s);
            Assertions.assertThat(s.model(List.of(A)).size()).isEqualTo(1);
            Assertions.assertThat(s.model(List.of(A)).evaluateLit(A)).isTrue();
            s.add(NA);
            assertSolverUnsat(s);
            s.reset();
            s.add(NA);
            assertSolverSat(s);
            Assertions.assertThat(s.model(List.of(A)).size()).isEqualTo(1);
            Assertions.assertThat(s.model(List.of(A)).evaluateLit(NA)).isTrue();
            s.reset();
        }
    }

    @Test
    public void testAnd1() {
        for (final SATSolver s : solvers) {
            s.add(AND1);
            assertSolverSat(s);
            Assertions.assertThat(s.model(AND1.variables(f)).size()).isEqualTo(2);
            Assertions.assertThat(s.model(AND1.variables(f)).evaluateLit(A)).isTrue();
            Assertions.assertThat(s.model(AND1.variables(f)).evaluateLit(B)).isTrue();
            s.add(NOT1);
            assertSolverUnsat(s);
            Assertions.assertThat(s.model(NOT1.variables(f))).isNull();
            s.reset();
        }
    }

    @Test
    public void testAnd2() {
        for (final SATSolver s : solvers) {
            final StandardProposition prop = new StandardProposition(
                    f.and(f.literal("a", true), f.literal("b", false), f.literal("c", true), f.literal("d", false)));
            s.add(prop);
            assertSolverSat(s);
            Assertions.assertThat(s.model(prop.formula().variables(f)).size()).isEqualTo(4);
            Assertions.assertThat(s.model(prop.formula().variables(f)).evaluateLit(f.variable("a"))).isTrue();
            Assertions.assertThat(s.model(prop.formula().variables(f)).evaluateLit(f.variable("b"))).isFalse();
            Assertions.assertThat(s.model(prop.formula().variables(f)).evaluateLit(f.variable("c"))).isTrue();
            Assertions.assertThat(s.model(prop.formula().variables(f)).evaluateLit(f.variable("d"))).isFalse();
            s.reset();
        }
    }

    @Test
    public void testAnd3() {
        for (final SATSolver s : solvers) {
            final List<Formula> formulas = new ArrayList<>(3);
            formulas.add(f.literal("a", true));
            formulas.add(f.literal("b", false));
            formulas.add(f.literal("a", false));
            formulas.add(f.literal("d", false));
            s.add(formulas);
            assertSolverUnsat(s);
            s.reset();
        }
    }

    @Test
    public void testFormula1() throws ParserException {
        for (final SATSolver s : solvers) {
            final Formula formula = parser.parse("(x => y) & (~x => y) & (y => z) & (z => ~x)");
            s.add(formula);
            assertSolverSat(s);
            Assertions.assertThat(s.model(formula.variables(f)).size()).isEqualTo(3);
            Assertions.assertThat(s.model(formula.variables(f)).evaluateLit(f.variable("x"))).isFalse();
            Assertions.assertThat(s.model(formula.variables(f)).evaluateLit(f.variable("y"))).isTrue();
            Assertions.assertThat(s.model(formula.variables(f)).evaluateLit(f.variable("z"))).isTrue();
            s.add(f.variable("x"));
            assertSolverUnsat(s);
            s.reset();
        }
    }

    @Test
    public void testFormula2() throws ParserException {
        for (int i = 0; i < solvers.length - 1; i++) {
            final SATSolver s = solvers[i];
            s.add(parser.parse("(x => y) & (~x => y) & (y => z) & (z => ~x)"));
            final List<Model> models = s.enumerateAllModels(f.variables("x", "y", "z"));
            assertThat(models.size()).isEqualTo(1);
            assertThat(models.get(0).size()).isEqualTo(3);
            assertThat(models.get(0).assignment().evaluateLit(f.variable("x"))).isFalse();
            assertThat(models.get(0).assignment().evaluateLit(f.variable("y"))).isTrue();
            assertThat(models.get(0).assignment().evaluateLit(f.variable("z"))).isTrue();
            s.add(f.variable("x"));
            assertSolverUnsat(s);
            s.reset();
        }
    }

    @Test
    public void testFormula3() throws ParserException {
        for (int i = 0; i < solvers.length - 1; i++) {
            final SATSolver s = solvers[i];
            s.add(parser.parse("a | b"));
            final List<Model> models = s.execute(ModelEnumerationFunction.builder(f.variables("a", "b"))
                    .additionalVariables(f.variable("c"))
                    .configuration(ModelEnumerationConfig.builder().strategy(strategy(s)).build())
                    .build());
            assertThat(models.size()).isEqualTo(3);
            for (final Model model : models) {
                assertThat(model.size()).isEqualTo(3);
                assertThat(model.getLiterals()).contains(f.literal("c", false));
            }
            s.reset();
        }
    }

    @Test
    public void testCC1() {
        for (int i = 0; i < solvers.length - 1; i++) {
            final SATSolver s = solvers[i];
            final Variable[] lits = new Variable[100];
            for (int j = 0; j < lits.length; j++) {
                lits[j] = f.variable("x" + j);
            }
            s.add(f.exo(lits));
            final List<Model> models = s.enumerateAllModels(lits);
            assertThat(models.size()).isEqualTo(100);
            for (final Model m : models) {
                assertThat(m.positiveVariables().size()).isEqualTo(1);
            }
            s.reset();
        }
    }

    @Test
    public void testPBC() {
        for (final SATSolver s : solvers) {
            final List<Literal> lits = new ArrayList<>();
            final List<Integer> coeffs = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                lits.add(f.literal("x" + i, i % 2 == 0));
                coeffs.add(i + 1);
            }
            s.add(f.pbc(CType.GE, 10, lits, coeffs));
            assertSolverSat(s);
            s.reset();
        }
    }

    @Test
    public void testPartialModel() {
        for (final SATSolver s : solvers) {
            s.add(A);
            s.add(B);
            s.add(C);
            final Variable[] relevantVars = new Variable[2];
            relevantVars[0] = A;
            relevantVars[1] = B;
            assertSolverSat(s);
            final Assignment relModel = s.model(relevantVars);
            assertThat(relModel.negativeLiterals().isEmpty()).isTrue();
            assertThat(relModel.literals().contains(C)).isFalse();
            s.reset();
        }
    }

    @Test
    public void testVariableRemovedBySimplificationOccursInModel() throws ParserException {
        final FormulaFactory ff =
                FormulaFactory.caching(FormulaFactoryConfig.builder().simplifyComplementaryOperands(true).build());
        final SATSolver solver =
                MiniSat.miniSat(ff, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER).build());
        final Variable a = ff.variable("A");
        final Variable b = ff.variable("B");
        final Formula formula = ff.parse("A & B => A");
        solver.add(formula); // during NNF conversion, used by the PG
                             // transformation, the formula simplifies to verum
                             // when added to the solver
        assertThat(solver.sat()).isEqualTo(Tristate.TRUE);
        assertThat(solver.knownVariables()).containsExactlyInAnyOrder(a, b);
        assertThat(variables(ff, solver.model(formula.variables(f)).literals())).containsExactlyInAnyOrder(a, b);
    }

    @Test
    public void testUnknownVariableNotOccurringInModel() {
        final SATSolver solver = MiniSat.miniSat(f);
        final Variable a = f.variable("A");
        solver.add(a);
        assertThat(solver.sat()).isEqualTo(Tristate.TRUE);
        assertThat(solver.model(f.variables("A", "X")).literals()).containsExactly(a);
    }

    @Test
    public void testRelaxationFormulas() throws ParserException {
        for (final SATSolver s : solvers) {
            final Formula formula = f.parse("a & (b | c)");
            s.add(formula);
            assertSolverSat(s);
            s.addWithRelaxation(f.variable("x"), f.parse("~a & ~b"));
            assertSolverSat(s);
            Assertions.assertThat(s.model(f.variables("a", "b", "c", "x")).positiveVariables())
                    .contains(f.variable("x"));
            s.add(f.variable("x").negate(f));
            assertSolverUnsat(s);
        }
    }

    @Test
    public void testPigeonHole1() {
        for (final SATSolver s : solvers) {
            final Formula formula = pg.generate(1);
            s.add(formula);
            assertSolverUnsat(s);
            Assertions.assertThat(s.model(formula.variables(f))).isNull();
            s.reset();
        }
    }

    @Test
    public void testPigeonHole2() {
        for (final SATSolver s : solvers) {
            final Formula formula = pg.generate(2);
            s.add(formula);
            assertSolverUnsat(s);
            assertSolverUnsat(s);
            Assertions.assertThat(s.model(formula.variables(f))).isNull();
            s.reset();
        }
    }

    @Test
    public void testPigeonHole3() {
        for (final SATSolver s : solvers) {
            final Formula formula = pg.generate(3);
            s.add(formula);
            assertSolverUnsat(s);
            Assertions.assertThat(s.model(formula.variables(f))).isNull();
            s.reset();
        }
    }

    @Test
    public void testPigeonHole4() {
        for (final SATSolver s : solvers) {
            final Formula formula = pg.generate(4);
            s.add(formula);
            assertSolverUnsat(s);
            Assertions.assertThat(s.model(formula.variables(f))).isNull();
            s.reset();
        }
    }

    @Test
    public void testPigeonHole5() {
        for (final SATSolver s : solvers) {
            final Formula formula = pg.generate(5);
            s.add(formula);
            assertSolverUnsat(s);
            Assertions.assertThat(s.model(formula.variables(f))).isNull();
            s.reset();
        }
    }

    @Test
    public void testPigeonHole6() {
        for (final SATSolver s : solvers) {
            final Formula formula = pg.generate(6);
            s.add(formula);
            assertSolverUnsat(s);
            Assertions.assertThat(s.model(formula.variables(f))).isNull();
            s.reset();
        }
    }

    @Test
    public void testPigeonHole7() {
        for (final SATSolver s : solvers) {
            final Formula formula = pg.generate(7);
            s.add(formula);
            assertSolverUnsat(s);
            Assertions.assertThat(s.model(formula.variables(f))).isNull();
            s.reset();
        }
    }

    @Test
    public void testDifferentClauseMinimizations() {
        final SATSolver[] moreSolvers = new SATSolver[6];
        moreSolvers[0] = MiniSat.miniSat(f,
                MiniSatConfig.builder().clMinimization(MiniSatConfig.ClauseMinimization.NONE).build());
        moreSolvers[1] = MiniSat.miniSat(f,
                MiniSatConfig.builder().clMinimization(MiniSatConfig.ClauseMinimization.BASIC).build());
        moreSolvers[2] = MiniSat.glucose(f,
                MiniSatConfig.builder().clMinimization(MiniSatConfig.ClauseMinimization.NONE).build(),
                GlucoseConfig.builder().build());
        moreSolvers[3] = MiniSat.glucose(f,
                MiniSatConfig.builder().clMinimization(MiniSatConfig.ClauseMinimization.BASIC).build(),
                GlucoseConfig.builder().build());
        moreSolvers[4] = MiniSat.miniCard(f,
                MiniSatConfig.builder().clMinimization(MiniSatConfig.ClauseMinimization.NONE).build());
        moreSolvers[5] = MiniSat.miniCard(f,
                MiniSatConfig.builder().clMinimization(MiniSatConfig.ClauseMinimization.BASIC).build());
        for (final SATSolver s : moreSolvers) {
            final Formula formula = pg.generate(7);
            s.add(formula);
            assertSolverUnsat(s);
            Assertions.assertThat(s.model(formula.variables(f))).isNull();
        }
    }

    @Test
    public void testTimeoutSATHandlerSmall() {
        for (final SATSolver s : solvers) {
            s.add(IMP1);
            final TimeoutSATHandler handler = new TimeoutSATHandler(1000L);
            final Tristate result = s.sat(handler);
            assertThat(handler.aborted()).isFalse();
            assertThat(result).isEqualTo(Tristate.TRUE);
            s.reset();
        }
    }

    @Test
    public void testTimeoutSATHandlerLarge() {
        for (final SATSolver s : solvers) {
            s.add(pg.generate(10));
            final TimeoutSATHandler handler = new TimeoutSATHandler(1000L);
            final Tristate result = s.sat(handler);
            assertThat(handler.aborted()).isTrue();
            assertThat(result).isEqualTo(Tristate.UNDEF);
            s.reset();
        }
    }

    @Test
    public void testDimacsFiles() throws IOException {
        final Map<String, Boolean> expectedResults = new HashMap<>();
        final BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/sat/results.txt"));
        while (reader.ready()) {
            final String[] tokens = reader.readLine().split(";");
            expectedResults.put(tokens[0], Boolean.valueOf(tokens[1]));
        }
        final File testFolder = new File("src/test/resources/sat");
        final File[] files = testFolder.listFiles();
        assert files != null;
        for (final SATSolver solver : solvers) {
            for (final File file : files) {
                final String fileName = file.getName();
                if (fileName.endsWith(".cnf")) {
                    readCNF(solver, file);
                    final boolean res = solver.sat() == Tristate.TRUE;
                    assertThat(res).isEqualTo(expectedResults.get(fileName));
                }
            }
            solver.reset();
        }
    }

    private void readCNF(final SATSolver solver, final File file) throws IOException {
        solver.reset();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            final String line = reader.readLine();
            if (line.startsWith("p cnf")) {
                break;
            }
        }
        String[] tokens;
        final List<Literal> literals = new ArrayList<>();
        while (reader.ready()) {
            tokens = reader.readLine().split("\\s+");
            if (tokens.length >= 2) {
                assert "0".equals(tokens[tokens.length - 1]);
                literals.clear();
                for (int i = 0; i < tokens.length - 1; i++) {
                    if (!tokens[i].isEmpty()) {
                        final int parsedLit = Integer.parseInt(tokens[i]);
                        final String var = "v" + Math.abs(parsedLit);
                        literals.add(parsedLit > 0 ? f.literal(var, true) : f.literal(var, false));
                    }
                }
                if (!literals.isEmpty()) {
                    solver.add(f.or(literals));
                }
            }
        }
    }

    @Test
    public void testPigeonHoleWithReset() {
        for (final SATSolver s : solvers) {
            s.add(pg.generate(4));
            assertSolverUnsat(s);
            s.reset();
            s.add(pg.generate(5));
            assertSolverUnsat(s);
            s.reset();
            s.add(pg.generate(6));
            assertSolverUnsat(s);
            s.reset();
            s.add(pg.generate(7));
            assertSolverUnsat(s);
            s.reset();
        }
    }

    @Test
    public void testTimeoutModelEnumerationHandlerWithUNSATInstance() {
        for (final SATSolver solver : solvers) {
            final Formula formula = pg.generate(10);
            solver.add(formula);
            final var handler = new TimeoutModelEnumerationHandler(1000L);
            final var me = meWithHandler(handler, solver, formula.variables(f));
            final List<Model> assignments = solver.execute(me);
            assertThat(assignments).isEmpty();
            assertThat(handler.aborted()).isTrue();
            solver.reset();
        }
    }

    @Test
    public void testTimeoutModelEnumerationHandlerWithSATInstance() {
        for (final SATSolver solver : solvers) {
            final List<Variable> variables = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                variables.add(f.variable("x" + i));
            }

            solver.add(f.exo(variables));
            var handler = new TimeoutModelEnumerationHandler(50L);
            var me = meWithHandler(handler, solver, variables);
            solver.execute(me);
            assertThat(handler.aborted()).isTrue();
            solver.reset();

            solver.add(f.exo(variables.subList(0, 5)));
            handler = new TimeoutModelEnumerationHandler(50L);
            me = meWithHandler(handler, solver, variables.subList(0, 5));
            final List<Model> assignments = solver.execute(me);
            assertThat(assignments).hasSize(5);
            assertThat(handler.aborted()).isFalse();
            solver.reset();
        }
    }

    @Test
    public void testModelEnumeration() {
        for (int i = 0; i < solvers.length - 1; i++) {
            final SATSolver s = solvers[i];
            final SortedSet<Variable> lits = new TreeSet<>();
            final SortedSet<Variable> firstFive = new TreeSet<>();
            for (int j = 0; j < 20; j++) {
                final Variable lit = f.variable("x" + j);
                lits.add(lit);
                if (j < 5) {
                    firstFive.add(lit);
                }
            }
            s.add(f.cc(CType.GE, 1, lits));

            final var me = ModelEnumerationFunction.builder(firstFive)
                    .additionalVariables(lits)
                    .configuration(ModelEnumerationConfig.builder().strategy(strategy(s)).build())
                    .build();
            final List<Model> models = s.execute(me);
            assertThat(models.size()).isEqualTo(32);
            for (final Model model : models) {
                for (final Variable lit : lits) {
                    assertThat(model.positiveVariables().contains(lit) || model.negativeVariables().contains(lit))
                            .isTrue();
                }
            }
            s.reset();
        }
    }

    @Test
    public void testModelEnumerationWithHandler01() {
        for (int i = 0; i < solvers.length - 1; i++) {
            final SATSolver s = solvers[i];
            final SortedSet<Variable> lits = new TreeSet<>();
            final SortedSet<Variable> firstFive = new TreeSet<>();
            for (int j = 0; j < 20; j++) {
                final Variable lit = f.variable("x" + j);
                lits.add(lit);
                if (j < 5) {
                    firstFive.add(lit);
                }
            }
            s.add(f.cc(CType.GE, 1, lits));

            final var handler = new NumberOfModelsHandler(29);
            final var me = ModelEnumerationFunction.builder(firstFive)
                    .additionalVariables(lits)
                    .configuration(ModelEnumerationConfig.builder().strategy(strategy(s)).handler(handler).build())
                    .build();
            final List<Model> modelsWithHandler = s.execute(me);
            assertThat(handler.aborted()).isTrue();
            assertThat(modelsWithHandler.size()).isEqualTo(29);
            for (final Model model : modelsWithHandler) {
                for (final Variable lit : lits) {
                    assertThat(model.positiveVariables().contains(lit) || model.negativeVariables().contains(lit))
                            .isTrue();
                }
            }
            s.reset();
        }
    }

    @Test
    public void testModelEnumerationWithHandler02() {
        for (int i = 0; i < solvers.length - 1; i++) {
            final SATSolver s = solvers[i];
            final SortedSet<Variable> lits = new TreeSet<>();
            final SortedSet<Variable> firstFive = new TreeSet<>();
            for (int j = 0; j < 20; j++) {
                final Variable lit = f.variable("x" + j);
                lits.add(lit);
                if (j < 5) {
                    firstFive.add(lit);
                }
            }
            s.add(f.cc(CType.GE, 1, lits));

            final var handler = new NumberOfModelsHandler(29);
            final var me = ModelEnumerationFunction.builder(lits)
                    .additionalVariables(firstFive)
                    .configuration(ModelEnumerationConfig.builder().strategy(strategy(s)).handler(handler).build())
                    .build();
            final List<Model> modelsWithHandler = s.execute(me);
            assertThat(handler.aborted()).isTrue();
            assertThat(modelsWithHandler.size()).isEqualTo(29);
            for (final Model model : modelsWithHandler) {
                for (final Variable lit : lits) {
                    assertThat(model.positiveVariables().contains(lit) || model.negativeVariables().contains(lit))
                            .isTrue();
                }
            }
            s.reset();
        }
    }

    @Test
    public void testEmptyEnumeration() {
        for (int i = 0; i < solvers.length - 1; i++) {
            final SATSolver s = solvers[i];
            s.add(f.falsum());
            final List<Model> models = s.enumerateAllModels(List.of());
            assertThat(models.isEmpty()).isTrue();

            s.reset();
        }
    }

    @Test
    public void testNumberOfModelHandler() {
        for (int i = 0; i < solvers.length - 1; i++) {
            final SATSolver s = solvers[i];
            final Variable[] lits = new Variable[100];
            for (int j = 0; j < lits.length; j++) {
                lits[j] = f.variable("x" + j);
            }
            s.add(f.exo(lits));
            var handler = new NumberOfModelsHandler(101);
            var me = ModelEnumerationFunction.builder(lits)
                    .configuration(ModelEnumerationConfig.builder().strategy(strategy(s)).handler(handler).build())
                    .build();

            List<Model> models = s.execute(me);
            assertThat(handler.aborted()).isFalse();
            assertThat(models.size()).isEqualTo(100);
            for (final Model m : models) {
                assertThat(m.positiveVariables().size()).isEqualTo(1);
            }
            s.reset();

            s.add(f.exo(lits));
            handler = new NumberOfModelsHandler(200);
            me = ModelEnumerationFunction.builder(lits)
                    .configuration(ModelEnumerationConfig.builder().strategy(strategy(s)).handler(handler).build())
                    .build();
            models = s.execute(me);
            assertThat(handler.aborted()).isFalse();
            assertThat(models.size()).isEqualTo(100);
            for (final Model m : models) {
                assertThat(m.positiveVariables().size()).isEqualTo(1);
            }
            s.reset();

            s.add(f.exo(lits));
            handler = new NumberOfModelsHandler(50);
            me = ModelEnumerationFunction.builder(lits)
                    .configuration(ModelEnumerationConfig.builder().strategy(strategy(s)).handler(handler).build())
                    .build();
            models = s.execute(me);
            assertThat(handler.aborted()).isTrue();
            assertThat(models.size()).isEqualTo(50);
            for (final Model m : models) {
                assertThat(m.positiveVariables().size()).isEqualTo(1);
            }
            s.reset();

            s.add(f.exo(lits));
            handler = new NumberOfModelsHandler(1);
            me = ModelEnumerationFunction.builder(lits)
                    .configuration(ModelEnumerationConfig.builder().strategy(strategy(s)).handler(handler).build())
                    .build();
            models = s.execute(me);
            assertThat(handler.aborted()).isTrue();
            assertThat(models.size()).isEqualTo(1);
            for (final Model m : models) {
                assertThat(m.positiveVariables().size()).isEqualTo(1);
            }
            s.reset();
        }
    }

    @Test
    public void testModelBeforeSolving() {
        final MiniSat solver = MiniSat.miniSat(f);
        assertThatThrownBy(() -> solver.model(List.of())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testToString() {
        for (int i = 0; i < solvers.length; i++) {
            assertThat(solvers[i].toString()).isEqualTo(testStrings[i]);
        }
    }

    @Test
    public void testKnownVariables() throws ParserException {
        final PropositionalParser parser = new PropositionalParser(f);
        final Formula phi = parser.parse("x1 & x2 & x3 & (x4 | ~x5)");
        final SATSolver minisat = MiniSat.miniSat(f);
        final SATSolver minicard = MiniSat.miniCard(f);
        minisat.add(phi);
        minicard.add(phi);
        final SortedSet<Variable> expected = new TreeSet<>(Arrays.asList(
                f.variable("x1"),
                f.variable("x2"),
                f.variable("x3"),
                f.variable("x4"),
                f.variable("x5")));
        Assertions.assertThat(minisat.knownVariables()).isEqualTo(expected);
        Assertions.assertThat(minicard.knownVariables()).isEqualTo(expected);

        final SolverState state = minisat.saveState();
        final SolverState stateCard = minicard.saveState();
        minisat.add(f.variable("x6"));
        minicard.add(f.variable("x6"));
        final SortedSet<Variable> expected2 = new TreeSet<>(Arrays.asList(
                f.variable("x1"),
                f.variable("x2"),
                f.variable("x3"),
                f.variable("x4"),
                f.variable("x5"),
                f.variable("x6")));
        Assertions.assertThat(minisat.knownVariables()).isEqualTo(expected2);
        Assertions.assertThat(minicard.knownVariables()).isEqualTo(expected2);

        // load state for minisat
        minisat.loadState(state);
        minicard.loadState(stateCard);
        Assertions.assertThat(minisat.knownVariables()).isEqualTo(expected);
        Assertions.assertThat(minicard.knownVariables()).isEqualTo(expected);
    }

    @Test
    public void testUPZeroLiteralsForUndefState() {
        assertThatThrownBy(() -> {
            final SATSolver solver = MiniSat.miniSat(f);
            solver.add(f.parse("a & b"));
            solver.execute(UpZeroLiteralsFunction.get());
        }).isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Cannot get unit propagated literals on level 0 as long as the formula is not solved.  Call 'sat' first.");
    }

    @Test
    public void testUPZeroLiteralsUNSAT() throws ParserException {
        final Formula formula = parser.parse("a & (a => b) & (b => c) & (c => ~a)");
        for (final SATSolver solver : solvers) {
            solver.reset();
            solver.add(formula);
            solver.sat();
            final SortedSet<Literal> upLiterals = solver.execute(UpZeroLiteralsFunction.get());
            assertThat(upLiterals).isNull();
        }
    }

    @Test
    public void testUPZeroLiterals() throws ParserException {
        // Note: The complete unit propagated set of literals on level 0 depends
        // on each solver's added learned clauses during the solving process
        final Map<Formula, SortedSet<Literal>> expectedSubsets = new HashMap<>();
        expectedSubsets.put(f.verum(), new TreeSet<>());
        expectedSubsets.put(parser.parse("a"), new TreeSet<>(Collections.singletonList(f.literal("a", true))));
        expectedSubsets.put(parser.parse("a | b"), new TreeSet<>());
        expectedSubsets.put(parser.parse("a & b"),
                new TreeSet<>(Arrays.asList(f.literal("a", true), f.literal("b", true))));
        expectedSubsets.put(parser.parse("a & ~b"),
                new TreeSet<>(Arrays.asList(f.literal("a", true), f.literal("b", false))));
        expectedSubsets.put(parser.parse("(a | c) & ~b"),
                new TreeSet<>(Collections.singletonList(f.literal("b", false))));
        expectedSubsets.put(parser.parse("(b | c) & ~b & (~c | d)"), new TreeSet<>(Arrays.asList(
                f.literal("b", false), f.literal("c", true), f.literal("d", true))));
        for (final SATSolver solver : solvers) {
            for (final Formula formula : expectedSubsets.keySet()) {
                solver.reset();
                solver.add(formula);
                final boolean res = solver.sat() == Tristate.TRUE;
                assertThat(res).isTrue();
                final SortedSet<Literal> upLiterals = solver.execute(UpZeroLiteralsFunction.get());
                assertThat(upLiterals).containsAll(expectedSubsets.get(formula));
            }
        }
    }

    @Test
    public void testUPZeroLiteralsDimacsFiles() throws IOException {
        final File testFolder = new File("src/test/resources/sat");
        final File[] files = testFolder.listFiles();
        assert files != null;
        for (final SATSolver solver : solvers) {
            for (final File file : files) {
                final String fileName = file.getName();
                if (fileName.endsWith(".cnf")) {
                    readCNF(solver, file);
                    final boolean res = solver.sat() == Tristate.TRUE;
                    if (res) {
                        final SortedSet<Literal> upZeroLiterals = solver.execute(UpZeroLiteralsFunction.get());
                        final List<Literal> negations = new ArrayList<>(upZeroLiterals.size());
                        for (final Literal lit : upZeroLiterals) {
                            negations.add(lit.negate(f));
                        }
                        solver.add(f.or(negations));
                        // Test if CNF implies identified unit propagated
                        // literals on level zero, i.e., each literal is a
                        // backbone literal
                        Assertions.assertThat(solver.sat()).isEqualTo(Tristate.FALSE);
                    }
                }
            }
            solver.reset();
        }
    }

    @Test
    public void testFormulaOnSolver() throws ParserException {
        for (final SATSolver solver : solvers) {
            if (solver instanceof MiniSat) {
                final PropositionalParser p = new PropositionalParser(f);
                final Set<Formula> formulas = new LinkedHashSet<>();
                formulas.add(p.parse("A | B | C"));
                formulas.add(p.parse("~A | ~B | ~C"));
                formulas.add(p.parse("A | ~B"));
                formulas.add(p.parse("A"));
                solver.add(formulas);
                compareFormulas(formulas, solver.execute(FormulaOnSolverFunction.get()));
                formulas.add(p.parse("~A | C"));
                solver.reset();
                solver.add(formulas);
                compareFormulas(formulas, solver.execute(FormulaOnSolverFunction.get()));
                final Formula formula = p.parse("C + D + E <= 2");
                formulas.add(formula);
                solver.add(formula);
                compareFormulas(formulas, solver.execute(FormulaOnSolverFunction.get()));
            }
        }
    }

    @Test
    public void testFormulaOnSolverWithContradiction() throws ParserException {
        for (final SATSolver solver : solvers) {
            if (solver instanceof MiniSat) {
                solver.add(f.variable("A"));
                solver.add(f.variable("B"));
                solver.add(f.parse("C & (~A | ~B)"));
                Assertions.assertThat(solver.execute(FormulaOnSolverFunction.get()))
                        .containsExactlyInAnyOrder(f.variable("A"), f.variable("B"), f.variable("C"), f.falsum());
                solver.reset();
                solver.add(f.parse("A <=> B"));
                solver.add(f.parse("B <=> ~A"));
                Assertions.assertThat(solver.execute(FormulaOnSolverFunction.get()))
                        .containsExactlyInAnyOrder(f.parse("A | ~B"), f.parse("~A | B"), f.parse("~B | ~A"),
                                f.parse("B | A"));
                solver.sat();
                Assertions.assertThat(solver.execute(FormulaOnSolverFunction.get()))
                        .containsExactlyInAnyOrder(f.parse("A | ~B"), f.parse("~A | B"), f.parse("~B | ~A"),
                                f.parse("B | A"),
                                f.variable("A"), f.variable("B"), f.falsum());
            }
        }
    }

    @Test
    public void testSelectionOrderSimple01() throws ParserException {
        for (final SATSolver solver : solvers) {
            final Formula formula = parser.parse("~(x <=> y)");
            solver.add(formula);

            List<Literal> selectionOrder = Arrays.asList(X, Y);
            Assertions.assertThat(solver.satWithSelectionOrder(selectionOrder)).isEqualTo(Tristate.TRUE);
            Assignment assignment = solver.model(formula.variables(f));
            Assertions.assertThat(assignment.literals()).containsExactlyInAnyOrder(X, NY);
            testLocalMinimum(solver, assignment, selectionOrder);
            testHighestLexicographicalAssignment(solver, assignment, selectionOrder);

            solver.setSolverToUndef();
            selectionOrder = Arrays.asList(Y, X);
            Assertions.assertThat(solver.satWithSelectionOrder(selectionOrder)).isEqualTo(Tristate.TRUE);
            assignment = solver.model(formula.variables(f));
            Assertions.assertThat(assignment.literals()).containsExactlyInAnyOrder(Y, NX);
            testLocalMinimum(solver, assignment, selectionOrder);
            testHighestLexicographicalAssignment(solver, assignment, selectionOrder);

            solver.setSolverToUndef();
            selectionOrder = Collections.singletonList(NX);
            Assertions.assertThat(solver.sat(selectionOrder)).isEqualTo(Tristate.TRUE);
            assignment = solver.model(formula.variables(f));
            Assertions.assertThat(assignment.literals()).containsExactlyInAnyOrder(Y, NX);
            testLocalMinimum(solver, assignment, selectionOrder);
            testHighestLexicographicalAssignment(solver, assignment, selectionOrder);

            solver.setSolverToUndef();
            selectionOrder = Arrays.asList(NY, NX);
            Assertions.assertThat(solver.satWithSelectionOrder(selectionOrder)).isEqualTo(Tristate.TRUE);
            assignment = solver.model(formula.variables(f));
            Assertions.assertThat(assignment.literals()).containsExactlyInAnyOrder(X, NY);
            testLocalMinimum(solver, assignment, selectionOrder);
            testHighestLexicographicalAssignment(solver, assignment, selectionOrder);

            solver.reset();
        }
    }

    @Test
    public void testSelectionOrderSimple02() {
        for (final SATSolver solver : solvers) {
            final List<Variable> literals = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                final Variable lit = f.variable("x" + i);
                literals.add(lit);
            }
            solver.add(f.cc(CType.EQ, 2, literals));

            for (int i = 0; i < 10; ++i) {
                Assertions.assertThat(solver.satWithSelectionOrder(literals)).isEqualTo(Tristate.TRUE);
                final Assignment assignment = solver.model(literals);
                testLocalMinimum(solver, assignment, literals);
                testHighestLexicographicalAssignment(solver, assignment, literals);
                solver.add(assignment.blockingClause(f, literals));
            }

            solver.reset();
            solver.add(f.cc(CType.EQ, 2, literals));
            final List<Literal> selectionOrder02 = Arrays.asList(
                    f.literal("x4", true), f.literal("x0", false),
                    f.literal("x1", true), f.literal("x2", true),
                    f.literal("x3", true));

            for (int i = 0; i < 10; ++i) {
                Assertions.assertThat(solver.satWithSelectionOrder(selectionOrder02)).isEqualTo(Tristate.TRUE);
                final Assignment assignment = solver.model(literals);
                testLocalMinimum(solver, assignment, selectionOrder02);
                testHighestLexicographicalAssignment(solver, assignment, selectionOrder02);
                solver.add(assignment.blockingClause(f, selectionOrder02));
            }

            solver.reset();
        }
    }

    @Test
    @LongRunningTag
    public void testDimacsFilesWithSelectionOrder() throws IOException {
        final Map<String, Boolean> expectedResults = new HashMap<>();
        final BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/sat/results.txt"));
        while (reader.ready()) {
            final String[] tokens = reader.readLine().split(";");
            expectedResults.put(tokens[0], Boolean.valueOf(tokens[1]));
        }
        final File testFolder = new File("src/test/resources/sat");
        final File[] files = testFolder.listFiles();
        assert files != null;
        for (final SATSolver solver : solvers) {
            for (final File file : files) {
                final String fileName = file.getName();
                if (fileName.endsWith(".cnf")) {
                    readCNF(solver, file);
                    final List<Literal> selectionOrder = new ArrayList<>();
                    for (final Variable var : variables(f, solver.execute(FormulaOnSolverFunction.get()))) {
                        if (selectionOrder.size() < 10) {
                            selectionOrder.add(var.negate(f));
                        }
                    }
                    final boolean res = solver.satWithSelectionOrder(selectionOrder) == Tristate.TRUE;
                    assertThat(res).isEqualTo(expectedResults.get(fileName));
                    if (expectedResults.get(fileName)) {
                        final Assignment assignment = solver.model(solver.knownVariables());
                        testLocalMinimum(solver, assignment, selectionOrder);
                        testHighestLexicographicalAssignment(solver, assignment, selectionOrder);
                    }
                }
            }
            solver.reset();
        }
    }

    @Test
    public void testModelEnumerationWithAdditionalVariables() throws ParserException {
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("A | B | C | D | E"));
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(f.variables("A", "B"))
                .additionalVariables(f.variables("C", "D")).build());
        for (final Model model : models) {
            int countB = 0;
            for (final Variable variable : model.positiveVariables()) {
                if (variable.name().equals("B")) {
                    countB++;
                }
            }
            assertThat(countB).isLessThan(2);
            countB = 0;
            for (final Variable variable : model.negativeVariables()) {
                if (variable.name().equals("B")) {
                    countB++;
                }
            }
            assertThat(countB).isLessThan(2);
        }

    }

    private void compareFormulas(final Collection<Formula> original, final Collection<Formula> solver) {
        final SortedSet<Variable> vars = new TreeSet<>();
        for (final Formula formula : original) {
            vars.addAll(formula.variables(f));
        }
        final MiniSat miniSat = MiniSat.miniSat(f);
        miniSat.add(original);
        final List<Model> models1 = miniSat.enumerateAllModels(vars);
        miniSat.reset();
        miniSat.add(solver);
        final List<Model> models2 = miniSat.enumerateAllModels(vars);
        assertThat(models1).hasSameElementsAs(models2);
    }

    /**
     * Tests if the given satisfying assignment is a local minimal model
     * regarding the given relevant literals, i.e. there is no variable in the
     * assignment, contained in the relevant literals, that can be flipped
     * without resulting in an unsatisfying assignment.
     * @param solver           the solver with the loaded formulas
     * @param assignment       the satisfying assignment
     * @param relevantLiterals the relevant literals.
     */
    private void testLocalMinimum(final SATSolver solver, final Assignment assignment,
                                  final Collection<? extends Literal> relevantLiterals) {
        final SortedSet<Literal> literals = assignment.literals();
        for (final Literal lit : relevantLiterals) {
            if (!literals.contains(lit)) {
                final SortedSet<Literal> literalsWithFlip = new TreeSet<>(literals);
                literalsWithFlip.remove(lit.negate(f));
                literalsWithFlip.add(lit);
                Assertions.assertThat(solver.sat(literalsWithFlip)).isEqualTo(Tristate.FALSE);
            }
        }
    }

    /**
     * Tests if the given satisfying assignment is the highest assignment in the
     * lexicographical order based on the given literals order.
     * @param solver     the solver with the loaded formulas
     * @param assignment the satisfying assignment
     * @param order      the literals order
     */
    private void testHighestLexicographicalAssignment(final SATSolver solver, final Assignment assignment,
                                                      final List<? extends Literal> order) {
        final SortedSet<Literal> literals = assignment.literals();
        final List<Literal> orderSublist = new ArrayList<>();
        for (final Literal lit : order) {
            final boolean containsLit = literals.contains(lit);
            if (!containsLit) {
                final SortedSet<Literal> orderSubsetWithFlip = new TreeSet<>(orderSublist);
                orderSubsetWithFlip.remove(lit.negate(f));
                orderSubsetWithFlip.add(lit);
                Assertions.assertThat(solver.sat(orderSubsetWithFlip)).isEqualTo(Tristate.FALSE);
            }
            orderSublist.add(containsLit ? lit : lit.negate(f));
        }
    }

    static ModelEnumerationFunction meWithHandler(final ModelEnumerationHandler handler, final SATSolver solver,
                                                  final Collection<Variable> variables) {
        return ModelEnumerationFunction.builder(variables)
                .configuration(ModelEnumerationConfig.builder()
                        .strategy(strategy(solver))
                        .handler(handler)
                        .build())
                .build();
    }

    static ModelEnumerationStrategy strategy(final SATSolver solver) {
        return solver.canSaveLoadState() ? DefaultModelEnumerationStrategy.builder().build() :
                NoSplitModelEnumerationStrategy.get();
    }
}

