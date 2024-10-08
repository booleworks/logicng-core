// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.solvers.sat.SatSolverConfig.ClauseMinimization.BASIC;
import static com.booleworks.logicng.solvers.sat.SatSolverConfig.ClauseMinimization.DEEP;
import static com.booleworks.logicng.solvers.sat.SatSolverConfig.ClauseMinimization.NONE;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.CLAUSE_MINIMIZATION;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.CNF_METHOD;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.INITIAL_PHASE;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.PROOF_GENERATION;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES;
import static com.booleworks.logicng.util.FormulaHelper.variables;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NumberOfModelsHandler;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.functions.FormulaOnSolverFunction;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.UpZeroLiteralsFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationStrategy;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

public class SATTest extends TestWithExampleFormulas implements LogicNGTest {

    private final List<SatSolver> solvers;
    private final List<Function<FormulaFactory, SatSolver>> solverSuppliers;
    private final PigeonHoleGenerator pg;
    private final PropositionalParser parser;

    public SATTest() {
        pg = new PigeonHoleGenerator(f);
        parser = new PropositionalParser(f);
        solvers = SolverTestSet.solverTestSet(
                Set.of(USE_AT_MOST_CLAUSES, CNF_METHOD, CLAUSE_MINIMIZATION, PROOF_GENERATION, INITIAL_PHASE), f);
        solverSuppliers = SolverTestSet.solverSupplierTestSet(
                Set.of(USE_AT_MOST_CLAUSES, CNF_METHOD, CLAUSE_MINIMIZATION, PROOF_GENERATION, INITIAL_PHASE));
    }

    @Test
    public void testTrue() {
        for (final SatSolver s : solvers) {
            s.add(TRUE);
            assertThat(s.satCall().model(f.variables()).size()).isEqualTo(0);
        }
    }

    @Test
    public void testFalse() {
        for (final SatSolver s : solvers) {
            s.add(FALSE);
            assertThat(s.satCall().model(f.variables())).isNull();
        }
    }

    @Test
    public void testLiterals1() {
        for (final SatSolver s : solvers) {
            s.add(A);
            assertThat(s.satCall().model(List.of(A)).size()).isEqualTo(1);
            assertThat(s.satCall().model(List.of(A)).toAssignment().evaluateLit(A)).isTrue();
            s.add(NA);
            assertSolverUnsat(s);
        }
    }

    @Test
    public void testLiterals2() {
        for (final SatSolver s : solvers) {
            s.add(NA);
            assertThat(s.satCall().model(List.of(A)).size()).isEqualTo(1);
            assertThat(s.satCall().model(List.of(A)).toAssignment().evaluateLit(NA)).isTrue();
        }
    }

    @Test
    public void testAnd1() {
        for (final SatSolver s : solvers) {
            s.add(AND1);
            assertThat(s.satCall().model(AND1.variables(f)).size()).isEqualTo(2);
            assertThat(s.satCall().model(AND1.variables(f)).toAssignment().evaluateLit(A)).isTrue();
            assertThat(s.satCall().model(AND1.variables(f)).toAssignment().evaluateLit(B)).isTrue();
            s.add(NOT1);
            assertSolverUnsat(s);
            assertThat(s.satCall().model(NOT1.variables(f))).isNull();
        }
    }

    @Test
    public void testAnd2() {
        for (final SatSolver s : solvers) {
            final StandardProposition prop = new StandardProposition(
                    f.and(f.literal("a", true), f.literal("b", false), f.literal("c", true), f.literal("d", false)));
            s.add(prop);
            assertThat(s.satCall().model(prop.getFormula().variables(f)).size()).isEqualTo(4);
            assertThat(s.satCall().model(prop.getFormula().variables(f)).toAssignment().evaluateLit(f.variable("a"))).isTrue();
            assertThat(s.satCall().model(prop.getFormula().variables(f)).toAssignment().evaluateLit(f.variable("b"))).isFalse();
            assertThat(s.satCall().model(prop.getFormula().variables(f)).toAssignment().evaluateLit(f.variable("c"))).isTrue();
            assertThat(s.satCall().model(prop.getFormula().variables(f)).toAssignment().evaluateLit(f.variable("d"))).isFalse();
        }
    }

    @Test
    public void testAnd3() {
        for (final SatSolver s : solvers) {
            final List<Formula> formulas = new ArrayList<>(3);
            formulas.add(f.literal("a", true));
            formulas.add(f.literal("b", false));
            formulas.add(f.literal("a", false));
            formulas.add(f.literal("d", false));
            s.add(formulas);
            assertSolverUnsat(s);
        }
    }

    @Test
    public void testFormula1() throws ParserException {
        for (final SatSolver s : solvers) {
            final Formula formula = parser.parse("(x => y) & (~x => y) & (y => z) & (z => ~x)");
            s.add(formula);
            assertThat(s.satCall().model(formula.variables(f)).size()).isEqualTo(3);
            assertThat(s.satCall().model(formula.variables(f)).toAssignment().evaluateLit(f.variable("x"))).isFalse();
            assertThat(s.satCall().model(formula.variables(f)).toAssignment().evaluateLit(f.variable("y"))).isTrue();
            assertThat(s.satCall().model(formula.variables(f)).toAssignment().evaluateLit(f.variable("z"))).isTrue();
            s.add(f.variable("x"));
            assertSolverUnsat(s);
        }
    }

    @Test
    public void testFormula2() throws ParserException {
        for (final SatSolver s : solvers) {
            s.add(parser.parse("(x => y) & (~x => y) & (y => z) & (z => ~x)"));
            final List<Model> models = s.enumerateAllModels(f.variables("x", "y", "z"));
            assertThat(models.size()).isEqualTo(1);
            assertThat(models.get(0).size()).isEqualTo(3);
            assertThat(models.get(0).toAssignment().evaluateLit(f.variable("x"))).isFalse();
            assertThat(models.get(0).toAssignment().evaluateLit(f.variable("y"))).isTrue();
            assertThat(models.get(0).toAssignment().evaluateLit(f.variable("z"))).isTrue();
            s.add(f.variable("x"));
            assertSolverUnsat(s);
        }
    }

    @Test
    public void testFormula3() throws ParserException {
        for (final SatSolver s : solvers) {
            s.add(parser.parse("a | b"));
            final List<Model> models = s.execute(ModelEnumerationFunction.builder(f.variables("a", "b"))
                    .additionalVariables(f.variable("c"))
                    .configuration(ModelEnumerationConfig.builder().strategy(defaultMeStrategy).build())
                    .build());
            assertThat(models.size()).isEqualTo(3);
            for (final Model model : models) {
                assertThat(model.size()).isEqualTo(3);
                assertThat(model.getLiterals()).contains(f.literal("c", false));
            }
        }
    }

    @Test
    public void testCC1() {
        for (final SatSolver s : solvers) {
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
        }
    }

    @Test
    public void testPBC() {
        for (final SatSolver s : solvers) {
            final List<Literal> lits = new ArrayList<>();
            final List<Integer> coeffs = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                lits.add(f.literal("x" + i, i % 2 == 0));
                coeffs.add(i + 1);
            }
            s.add(f.pbc(CType.GE, 10, lits, coeffs));
            assertSolverSat(s);
        }
    }

    @Test
    public void testPartialModel() {
        for (final SatSolver s : solvers) {
            s.add(A);
            s.add(B);
            s.add(C);
            final Variable[] relevantVars = new Variable[2];
            relevantVars[0] = A;
            relevantVars[1] = B;
            assertSolverSat(s);
            final Model relModel = s.satCall().model(Arrays.asList(relevantVars));
            assertThat(relModel.negativeLiterals().isEmpty()).isTrue();
            assertThat(relModel.getLiterals().contains(C)).isFalse();
        }
    }

    @Test
    public void testVariableRemovedBySimplificationOccursInModel() throws ParserException {
        final FormulaFactory f =
                FormulaFactory.caching(FormulaFactoryConfig.builder().simplifyComplementaryOperands(true).build());
        final SatSolver solver = SatSolver.newSolver(f,
                SatSolverConfig.builder().cnfMethod(SatSolverConfig.CnfMethod.PG_ON_SOLVER).build());
        final Variable a = f.variable("A");
        final Variable b = f.variable("B");
        final Formula formula = f.parse("A & B => A");
        solver.add(formula); // during NNF conversion, used by the PG
        // transformation, the formula simplifies to verum
        // when added to the solver
        assertThat(solver.sat()).isTrue();
        assertThat(solver.getUnderlyingSolver().knownVariables()).isEmpty();
        assertThat(variables(f, solver.satCall().model(List.of(a, b)).getLiterals())).containsExactlyInAnyOrder(a, b);
    }

    @Test
    public void testUnknownVariableNotOccurringInModel() {
        final SatSolver solver = SatSolver.newSolver(f);
        final Variable a = f.variable("A");
        solver.add(a);
        assertThat(solver.satCall().model(f.variables("A", "X")).getLiterals()).containsExactlyInAnyOrder(a,
                f.literal("X", false));
    }

    @Test
    public void testRelaxationFormulas() throws ParserException {
        for (final SatSolver s : solvers) {
            final Formula formula = f.parse("a & (b | c)");
            s.add(formula);
            assertSolverSat(s);
            s.addWithRelaxation(f.variable("x"), f.parse("~a & ~b"));
            assertSolverSat(s);
            assertThat(s.satCall().model(f.variables("a", "b", "c", "x")).positiveVariables())
                    .contains(f.variable("x"));
            s.add(f.variable("x").negate(f));
            assertSolverUnsat(s);
        }
    }

    @Test
    public void testPigeonHole1() {
        for (final SatSolver s : solvers) {
            final Formula formula = pg.generate(1);
            s.add(formula);
            assertSolverUnsat(s);
            assertThat(s.satCall().model(formula.variables(f))).isNull();
        }
    }

    @Test
    public void testPigeonHole2() {
        for (final SatSolver s : solvers) {
            final Formula formula = pg.generate(2);
            s.add(formula);
            assertSolverUnsat(s);
            assertSolverUnsat(s);
            assertThat(s.satCall().model(formula.variables(f))).isNull();
        }
    }

    @Test
    public void testPigeonHole3() {
        for (final SatSolver s : solvers) {
            final Formula formula = pg.generate(3);
            s.add(formula);
            assertSolverUnsat(s);
            assertThat(s.satCall().model(formula.variables(f))).isNull();
        }
    }

    @Test
    public void testPigeonHole4() {
        for (final SatSolver s : solvers) {
            final Formula formula = pg.generate(4);
            s.add(formula);
            assertSolverUnsat(s);
            assertThat(s.satCall().model(formula.variables(f))).isNull();
        }
    }

    @Test
    public void testPigeonHole5() {
        for (final SatSolver s : solvers) {
            final Formula formula = pg.generate(5);
            s.add(formula);
            assertSolverUnsat(s);
            assertThat(s.satCall().model(formula.variables(f))).isNull();
        }
    }

    @Test
    public void testPigeonHole6() {
        for (final SatSolver s : solvers) {
            final Formula formula = pg.generate(6);
            s.add(formula);
            assertSolverUnsat(s);
            assertThat(s.satCall().model(formula.variables(f))).isNull();
        }
    }

    @Test
    @LongRunningTag
    public void testPigeonHole7() {
        for (final SatSolver s : solvers) {
            final Formula formula = pg.generate(7);
            s.add(formula);
            assertSolverUnsat(s);
            assertThat(s.satCall().model(formula.variables(f))).isNull();
        }
    }

    @Test
    public void testDifferentClauseMinimizations() {
        final SatSolver[] moreSolvers = new SatSolver[6];
        moreSolvers[0] = SatSolver.newSolver(f,
                SatSolverConfig.builder().clauseMinimization(NONE).useAtMostClauses(false).build());
        moreSolvers[1] = SatSolver.newSolver(f,
                SatSolverConfig.builder().clauseMinimization(BASIC).useAtMostClauses(false).build());
        moreSolvers[2] = SatSolver.newSolver(f,
                SatSolverConfig.builder().clauseMinimization(DEEP).useAtMostClauses(false).build());
        moreSolvers[3] = SatSolver.newSolver(f,
                SatSolverConfig.builder().clauseMinimization(NONE).useAtMostClauses(true).build());
        moreSolvers[4] = SatSolver.newSolver(f,
                SatSolverConfig.builder().clauseMinimization(BASIC).useAtMostClauses(true).build());
        moreSolvers[5] = SatSolver.newSolver(f,
                SatSolverConfig.builder().clauseMinimization(DEEP).useAtMostClauses(true).build());
        for (final SatSolver s : moreSolvers) {
            final Formula formula = pg.generate(7);
            s.add(formula);
            assertSolverUnsat(s);
            assertThat(s.satCall().model(formula.variables(f))).isNull();
        }
    }

    @Test
    public void testTimeoutSATHandlerSmall() {
        for (final SatSolver s : solvers) {
            s.add(IMP1);
            final TimeoutHandler handler = new TimeoutHandler(1000L);
            final LngResult<Boolean> result = s.satCall().handler(handler).sat();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getResult()).isTrue();
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutSATHandlerLarge() {
        for (final SatSolver s : solvers) {
            s.add(pg.generate(10));
            final TimeoutHandler handler = new TimeoutHandler(1000L);
            final LngResult<Boolean> result = s.satCall().handler(handler).sat();
            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Test
    @LongRunningTag
    public void testDimacsFiles() throws IOException {
        final Map<String, Boolean> expectedResults = new HashMap<>();
        final BufferedReader reader = new BufferedReader(new FileReader("../test_files/sat/results.txt"));
        while (reader.ready()) {
            final String[] tokens = reader.readLine().split(";");
            expectedResults.put(tokens[0], Boolean.valueOf(tokens[1]));
        }
        final File testFolder = new File("../test_files/sat");
        final File[] files = testFolder.listFiles();
        assert files != null;
        for (final Function<FormulaFactory, SatSolver> solverSupplier : solverSuppliers) {
            for (final File file : files) {
                final SatSolver solver = solverSupplier.apply(f);
                final String fileName = file.getName();
                if (fileName.endsWith(".cnf")) {
                    readCNF(solver, file);
                    final boolean res = solver.sat();
                    assertThat(res).isEqualTo(expectedResults.get(fileName));
                }
            }
        }
    }

    private void readCNF(final SatSolver solver, final File file) throws IOException {
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
    @LongRunningTag
    public void testTimeoutModelEnumerationHandlerWithUNSATInstance() {
        for (final SatSolver solver : solvers) {
            final Formula formula = pg.generate(10);
            solver.add(formula);
            final var handler = new TimeoutHandler(1000L);
            final var me = meWithHandler(formula.variables(f));
            final LngResult<List<Model>> models = solver.execute(me, handler);
            assertThat(models.isSuccess()).isFalse();
        }
    }

    @Test
    @LongRunningTag
    public void testTimeoutModelEnumerationHandlerWithSATInstance1() {
        for (final SatSolver solver : solvers) {
            final List<Variable> variables = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                variables.add(f.variable("x" + i));
            }

            solver.add(f.exo(variables));
            final var handler = new TimeoutHandler(50L);
            final var me = meWithHandler(variables);
            final LngResult<List<Model>> result = solver.execute(me, handler);
            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Test
    public void testTimeoutModelEnumerationHandlerWithSATInstance2() {
        for (final SatSolver solver : solvers) {
            final List<Variable> variables = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                variables.add(f.variable("x" + i));
            }

            solver.add(f.exo(variables.subList(0, 5)));
            final var handler = new TimeoutHandler(50L);
            final var me = meWithHandler(variables.subList(0, 5));
            final LngResult<List<Model>> models = solver.execute(me, handler);
            assertThat(models.isSuccess()).isTrue();
            assertThat(models.getResult()).hasSize(5);
        }
    }

    @Test
    public void testModelEnumeration() {
        for (final SatSolver s : solvers) {
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
                    .configuration(ModelEnumerationConfig.builder().strategy(defaultMeStrategy).build())
                    .build();
            final List<Model> models = s.execute(me);
            assertThat(models.size()).isEqualTo(32);
            for (final Model model : models) {
                for (final Variable lit : lits) {
                    assertThat(model.positiveVariables().contains(lit) || model.negativeVariables().contains(lit))
                            .isTrue();
                }
            }
        }
    }

    @Test
    public void testModelEnumerationWithHandler01() {
        for (final SatSolver s : solvers) {
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
                    .configuration(ModelEnumerationConfig.builder().strategy(defaultMeStrategy).build())
                    .build();
            final LngResult<List<Model>> modelsWithHandler = s.execute(me, handler);
            assertThat(modelsWithHandler.isSuccess()).isFalse();
            assertThat(modelsWithHandler.isPartial()).isTrue();
            final List<Model> partialResult = modelsWithHandler.getPartialResult();
            assertThat(partialResult).hasSize(29);
            for (final Model model : partialResult) {
                for (final Variable lit : lits) {
                    assertThat(model.positiveVariables().contains(lit) || model.negativeVariables().contains(lit))
                            .isTrue();
                }
            }
        }
    }

    @Test
    public void testModelEnumerationWithHandler02() {
        for (final SatSolver s : solvers) {
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
                    .configuration(ModelEnumerationConfig.builder().strategy(defaultMeStrategy).build())
                    .build();
            final LngResult<List<Model>> modelsWithHandler = s.execute(me, handler);
            assertThat(modelsWithHandler.isSuccess()).isFalse();
            assertThat(modelsWithHandler.isPartial()).isTrue();
            assertThat(modelsWithHandler.getPartialResult()).hasSize(29);
            for (final Model model : modelsWithHandler.getPartialResult()) {
                for (final Variable lit : lits) {
                    assertThat(model.positiveVariables().contains(lit) || model.negativeVariables().contains(lit))
                            .isTrue();
                }
            }
        }
    }

    @Test
    public void testEmptyEnumeration() {
        for (final SatSolver s : solvers) {
            s.add(f.falsum());
            final List<Model> models = s.enumerateAllModels(List.of());
            assertThat(models.isEmpty()).isTrue();
        }
    }

    @Test
    public void testNumberOfModelHandler() {
        for (final Function<FormulaFactory, SatSolver> solverSupplier : solverSuppliers) {
            final Variable[] lits = new Variable[100];
            for (int j = 0; j < lits.length; j++) {
                lits[j] = f.variable("x" + j);
            }

            SatSolver s = solverSupplier.apply(f);
            s.add(f.exo(lits));
            var handler = new NumberOfModelsHandler(101);
            var me = ModelEnumerationFunction.builder(lits)
                    .configuration(ModelEnumerationConfig.builder().strategy(defaultMeStrategy).build())
                    .build();
            LngResult<List<Model>> models = s.execute(me, handler);
            assertThat(models.isSuccess()).isTrue();
            assertThat(models.getResult().size()).isEqualTo(100);
            for (final Model m : models.getResult()) {
                assertThat(m.positiveVariables().size()).isEqualTo(1);
            }

            s = solverSupplier.apply(f);
            s.add(f.exo(lits));
            handler = new NumberOfModelsHandler(200);
            me = ModelEnumerationFunction.builder(lits)
                    .configuration(ModelEnumerationConfig.builder().strategy(defaultMeStrategy).build())
                    .build();
            models = s.execute(me, handler);
            assertThat(models.isSuccess()).isTrue();
            assertThat(models.getResult().size()).isEqualTo(100);
            for (final Model m : models.getResult()) {
                assertThat(m.positiveVariables().size()).isEqualTo(1);
            }

            s = solverSupplier.apply(f);
            s.add(f.exo(lits));
            handler = new NumberOfModelsHandler(50);
            me = ModelEnumerationFunction.builder(lits)
                    .configuration(ModelEnumerationConfig.builder().strategy(defaultMeStrategy).build())
                    .build();
            models = s.execute(me, handler);
            assertThat(models.isSuccess()).isFalse();
            assertThat(models.isPartial()).isTrue();
            assertThat(models.getPartialResult()).hasSize(50);
            for (final Model m : models.getPartialResult()) {
                assertThat(m.positiveVariables().size()).isEqualTo(1);
            }

            s = solverSupplier.apply(f);
            s.add(f.exo(lits));
            handler = new NumberOfModelsHandler(1);
            me = ModelEnumerationFunction.builder(lits)
                    .configuration(ModelEnumerationConfig.builder().strategy(defaultMeStrategy).build())
                    .build();
            models = s.execute(me, handler);
            assertThat(models.isSuccess()).isFalse();
            assertThat(models.isPartial()).isTrue();
            assertThat(models.getPartialResult()).hasSize(1);
            for (final Model m : models.getPartialResult()) {
                assertThat(m.positiveVariables().size()).isEqualTo(1);
            }
        }
    }

    @Test
    public void testKnownVariables() throws ParserException {
        final PropositionalParser parser = new PropositionalParser(f);
        final Formula phi = parser.parse("x1 & x2 & x3 & (x4 | ~x5)");
        final SatSolver solverWithoutAtMost =
                SatSolver.newSolver(f, SatSolverConfig.builder().useAtMostClauses(false).build());
        final SatSolver solverWithAtMost =
                SatSolver.newSolver(f, SatSolverConfig.builder().useAtMostClauses(true).build());
        solverWithoutAtMost.add(phi);
        solverWithAtMost.add(phi);
        final SortedSet<Variable> expected = new TreeSet<>(Arrays.asList(
                f.variable("x1"),
                f.variable("x2"),
                f.variable("x3"),
                f.variable("x4"),
                f.variable("x5")));
        assertThat(solverWithoutAtMost.getUnderlyingSolver().knownVariables()).isEqualTo(expected);
        assertThat(solverWithAtMost.getUnderlyingSolver().knownVariables()).isEqualTo(expected);

        final SolverState state = solverWithoutAtMost.saveState();
        final SolverState stateCard = solverWithAtMost.saveState();
        solverWithoutAtMost.add(f.variable("x6"));
        solverWithAtMost.add(f.variable("x6"));
        final SortedSet<Variable> expected2 = new TreeSet<>(Arrays.asList(
                f.variable("x1"),
                f.variable("x2"),
                f.variable("x3"),
                f.variable("x4"),
                f.variable("x5"),
                f.variable("x6")));
        assertThat(solverWithoutAtMost.getUnderlyingSolver().knownVariables()).isEqualTo(expected2);
        assertThat(solverWithAtMost.getUnderlyingSolver().knownVariables()).isEqualTo(expected2);

        // load state
        solverWithoutAtMost.loadState(state);
        solverWithAtMost.loadState(stateCard);
        assertThat(solverWithoutAtMost.getUnderlyingSolver().knownVariables()).isEqualTo(expected);
        assertThat(solverWithAtMost.getUnderlyingSolver().knownVariables()).isEqualTo(expected);
    }

    @Test
    public void testUPZeroLiteralsUNSAT() throws ParserException {
        final Formula formula = parser.parse("a & (a => b) & (b => c) & (c => ~a)");
        for (final SatSolver solver : solvers) {
            solver.add(formula);
            final SortedSet<Literal> upLiterals = solver.execute(UpZeroLiteralsFunction.get());
            assertThat(upLiterals).isEmpty();
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
        for (final Function<FormulaFactory, SatSolver> solverSupplier : solverSuppliers) {
            for (final Formula formula : expectedSubsets.keySet()) {
                final SatSolver solver = solverSupplier.apply(f);
                solver.add(formula);
                final boolean res = solver.sat();
                assertThat(res).isTrue();
                final SortedSet<Literal> upLiterals = solver.execute(UpZeroLiteralsFunction.get());
                assertThat(upLiterals).containsAll(expectedSubsets.get(formula));
            }
        }
    }

    @Test
    @LongRunningTag
    public void testUPZeroLiteralsDimacsFiles() throws IOException {
        final File testFolder = new File("../test_files/sat");
        final File[] files = testFolder.listFiles();
        assert files != null;
        for (final Function<FormulaFactory, SatSolver> solverSupplier : solverSuppliers) {
            for (final File file : files) {
                final String fileName = file.getName();
                if (fileName.endsWith(".cnf")) {
                    final SatSolver solver = solverSupplier.apply(f);
                    readCNF(solver, file);
                    if (solver.sat()) {
                        final SortedSet<Literal> upZeroLiterals = solver.execute(UpZeroLiteralsFunction.get());
                        final List<Literal> negations = new ArrayList<>(upZeroLiterals.size());
                        for (final Literal lit : upZeroLiterals) {
                            negations.add(lit.negate(f));
                        }
                        solver.add(f.or(negations));
                        // Test if CNF implies identified unit propagated
                        // literals on level zero, i.e., each literal is a
                        // backbone literal
                        assertThat(solver.sat()).isFalse();
                    }
                }
            }
        }
    }

    @Test
    public void testFormulaOnSolver1() throws ParserException {
        for (final SatSolver solver : solvers) {
            final PropositionalParser p = new PropositionalParser(f);
            final Set<Formula> formulas = new LinkedHashSet<>();
            formulas.add(p.parse("A | B | C"));
            formulas.add(p.parse("~A | ~B | ~C"));
            formulas.add(p.parse("A | ~B"));
            formulas.add(p.parse("A"));
            solver.add(formulas);
            compareFormulas(formulas, solver.execute(FormulaOnSolverFunction.get()));
            formulas.add(p.parse("~A | C"));
        }
    }

    @Test
    public void testFormulaOnSolver2() throws ParserException {
        for (final SatSolver solver : solvers) {
            final PropositionalParser p = new PropositionalParser(f);
            final Set<Formula> formulas = new LinkedHashSet<>();
            formulas.add(p.parse("A | B | C"));
            formulas.add(p.parse("~A | ~B | ~C"));
            formulas.add(p.parse("A | ~B"));
            formulas.add(p.parse("A"));
            solver.add(formulas);
            compareFormulas(formulas, solver.execute(FormulaOnSolverFunction.get()));
            final Formula formula = p.parse("C + D + E <= 2");
            formulas.add(formula);
            solver.add(formula);
            compareFormulas(formulas, solver.execute(FormulaOnSolverFunction.get()));
        }
    }

    @Test
    public void testFormulaOnSolverWithContradiction1() throws ParserException {
        for (final SatSolver solver : solvers) {
            solver.add(f.variable("A"));
            solver.add(f.variable("B"));
            solver.add(f.parse("C & (~A | ~B)"));
            assertThat(solver.execute(FormulaOnSolverFunction.get()))
                    .containsExactlyInAnyOrder(f.variable("A"), f.variable("B"), f.variable("C"), f.falsum());
        }
    }

    @Test
    public void testFormulaOnSolverWithContradiction2() throws ParserException {
        for (final SatSolver solver : solvers) {
            solver.add(f.parse("A <=> B"));
            solver.add(f.parse("B <=> ~A"));
            assertThat(solver.execute(FormulaOnSolverFunction.get()))
                    .containsExactlyInAnyOrder(f.parse("A | ~B"), f.parse("~A | B"), f.parse("~B | ~A"),
                            f.parse("B | A"));
            solver.sat(); // adds learnt clauses s.t. the formula on the solver
            // changes
            assertThat(solver.execute(FormulaOnSolverFunction.get()))
                    .containsExactlyInAnyOrder(f.parse("A | ~B"), f.parse("~A | B"), f.parse("~B | ~A"),
                            f.parse("B | A"),
                            f.literal("A", !solver.getConfig().getInitialPhase()),
                            f.literal("B", !solver.getConfig().getInitialPhase()), f.falsum());
        }
    }

    @Test
    public void testSelectionOrderSimple01() throws ParserException {
        for (final SatSolver solver : solvers) {
            final Formula formula = parser.parse("~(x <=> y)");
            solver.add(formula);

            List<Literal> selectionOrder = Arrays.asList(X, Y);
            Model model = solver.satCall().selectionOrder(selectionOrder).model(formula.variables(f));
            assertThat(model.getLiterals()).containsExactlyInAnyOrder(X, NY);
            testLocalMinimum(solver, model, selectionOrder);
            testHighestLexicographicalModel(solver, model, selectionOrder);

            selectionOrder = Arrays.asList(Y, X);
            model = solver.satCall().selectionOrder(selectionOrder).model(formula.variables(f));
            assertThat(model.getLiterals()).containsExactlyInAnyOrder(Y, NX);
            testLocalMinimum(solver, model, selectionOrder);
            testHighestLexicographicalModel(solver, model, selectionOrder);

            selectionOrder = Collections.singletonList(NX);
            model = solver.satCall().selectionOrder(selectionOrder).model(formula.variables(f));
            assertThat(model.getLiterals()).containsExactlyInAnyOrder(Y, NX);
            testLocalMinimum(solver, model, selectionOrder);
            testHighestLexicographicalModel(solver, model, selectionOrder);

            selectionOrder = Arrays.asList(NY, NX);
            model = solver.satCall().selectionOrder(selectionOrder).model(formula.variables(f));
            assertThat(model.getLiterals()).containsExactlyInAnyOrder(X, NY);
            testLocalMinimum(solver, model, selectionOrder);
            testHighestLexicographicalModel(solver, model, selectionOrder);
        }
    }

    @Test
    public void testSelectionOrderSimple02() {
        for (final SatSolver solver : solvers) {
            final List<Variable> literals = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                final Variable lit = f.variable("x" + i);
                literals.add(lit);
            }
            solver.add(f.cc(CType.EQ, 2, literals));

            for (int i = 0; i < 10; ++i) {
                assertThat(solver.satCall().selectionOrder(literals).sat().getResult()).isTrue();
                final Model model = solver.satCall().selectionOrder(literals).model(literals);
                testLocalMinimum(solver, model, literals);
                testHighestLexicographicalModel(solver, model, literals);
                solver.add(model.blockingClause(f, literals));
            }
        }
    }

    @Test
    public void testSelectionOrderSimple03() {
        for (final SatSolver solver : solvers) {
            final List<Variable> literals = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                final Variable lit = f.variable("x" + i);
                literals.add(lit);
            }
            solver.add(f.cc(CType.EQ, 2, literals));
            final List<Literal> selectionOrder02 = Arrays.asList(
                    f.literal("x4", true), f.literal("x0", false),
                    f.literal("x1", true), f.literal("x2", true),
                    f.literal("x3", true));

            for (int i = 0; i < 10; ++i) {
                assertThat(solver.satCall().selectionOrder(selectionOrder02).sat().getResult()).isTrue();
                final Model model = solver.satCall().selectionOrder(selectionOrder02).model(literals);
                testLocalMinimum(solver, model, selectionOrder02);
                testHighestLexicographicalModel(solver, model, selectionOrder02);
                solver.add(model.blockingClause(f, selectionOrder02));
            }
        }
    }

    @Test
    @LongRunningTag
    public void testDimacsFilesWithSelectionOrder() throws IOException {
        final Map<String, Boolean> expectedResults = new HashMap<>();
        final BufferedReader reader = new BufferedReader(new FileReader("../test_files/sat/results.txt"));
        while (reader.ready()) {
            final String[] tokens = reader.readLine().split(";");
            expectedResults.put(tokens[0], Boolean.valueOf(tokens[1]));
        }
        final File testFolder = new File("../test_files/sat");
        final File[] files = testFolder.listFiles();
        assert files != null;
        for (final Function<FormulaFactory, SatSolver> solverSupplier : solverSuppliers) {
            for (final File file : files) {
                final String fileName = file.getName();
                if (fileName.endsWith(".cnf")) {
                    final SatSolver solver = solverSupplier.apply(f);
                    readCNF(solver, file);
                    final List<Literal> selectionOrder = new ArrayList<>();
                    for (final Variable var : variables(f, solver.execute(FormulaOnSolverFunction.get()))) {
                        if (selectionOrder.size() < 10) {
                            selectionOrder.add(var.negate(f));
                        }
                    }
                    final boolean res = solver.satCall().selectionOrder(selectionOrder).sat().getResult();
                    assertThat(res).isEqualTo(expectedResults.get(fileName));
                    if (expectedResults.get(fileName)) {
                        final Model model =
                                solver.satCall().model(solver.getUnderlyingSolver().knownVariables());
                        testLocalMinimum(solver, model, selectionOrder);
                        testHighestLexicographicalModel(solver, model, selectionOrder);
                    }
                }
            }
        }
    }

    @Test
    public void testModelEnumerationWithAdditionalVariables() throws ParserException {
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(f.parse("A | B | C | D | E"));
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(f.variables("A", "B"))
                .additionalVariables(f.variables("C", "D")).build());
        for (final Model model : models) {
            int countB = 0;
            for (final Variable variable : model.positiveVariables()) {
                if (variable.getName().equals("B")) {
                    countB++;
                }
            }
            assertThat(countB).isLessThan(2);
            countB = 0;
            for (final Variable variable : model.negativeVariables()) {
                if (variable.getName().equals("B")) {
                    countB++;
                }
            }
            assertThat(countB).isLessThan(2);
        }

    }

    private void compareFormulas(final Collection<Formula> original, final Collection<Formula> fromSolver) {
        final SortedSet<Variable> vars = new TreeSet<>();
        for (final Formula formula : original) {
            vars.addAll(formula.variables(f));
        }
        final SatSolver solver1 = SatSolver.newSolver(f);
        solver1.add(original);
        final List<Model> models1 = solver1.enumerateAllModels(vars);
        final SatSolver solver2 = SatSolver.newSolver(f);
        solver2.add(fromSolver);
        final List<Model> models2 = solver2.enumerateAllModels(vars);
        assertThat(models1).hasSameElementsAs(models2);
    }

    private void testLocalMinimum(final SatSolver solver, final Model model,
                                  final Collection<? extends Literal> relevantLiterals) {
        final Set<Literal> literals = new HashSet<>(model.getLiterals());
        for (final Literal lit : relevantLiterals) {
            if (!literals.contains(lit)) {
                final SortedSet<Literal> literalsWithFlip = new TreeSet<>(literals);
                literalsWithFlip.remove(lit.negate(f));
                literalsWithFlip.add(lit);
                assertThat(solver.satCall().addFormulas(literalsWithFlip).sat().getResult()).isFalse();
            }
        }
    }

    /**
     * Tests if the given satisfying model is the highest model in the
     * lexicographical order based on the given literals order.
     * @param solver the solver with the loaded formulas
     * @param model  the satisfying model
     * @param order  the literals order
     */
    private void testHighestLexicographicalModel(final SatSolver solver, final Model model,
                                                 final List<? extends Literal> order) {
        final Set<Literal> literals = new HashSet<>(model.getLiterals());
        final List<Literal> orderSublist = new ArrayList<>();
        for (final Literal lit : order) {
            final boolean containsLit = literals.contains(lit);
            if (!containsLit) {
                final SortedSet<Literal> orderSubsetWithFlip = new TreeSet<>(orderSublist);
                orderSubsetWithFlip.remove(lit.negate(f));
                orderSubsetWithFlip.add(lit);
                assertThat(solver.satCall().addFormulas(orderSubsetWithFlip).sat().getResult()).isFalse();
            }
            orderSublist.add(containsLit ? lit : lit.negate(f));
        }
    }

    static ModelEnumerationFunction meWithHandler(final Collection<Variable> variables) {
        return ModelEnumerationFunction.builder(variables)
                .configuration(ModelEnumerationConfig.builder().strategy(defaultMeStrategy).build())
                .build();
    }

    static ModelEnumerationStrategy defaultMeStrategy = DefaultModelEnumerationStrategy.builder().build();
}
