// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.solvers.functions.OptimizationFunction.builder;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.CNF_METHOD;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.INITIAL_PHASE;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.PROOF_GENERATION;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.FormulaCornerCases;
import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.BoundedOptimizationHandler;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.handlers.events.OptimizationFoundBetterBoundEvent;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.predicates.satisfiability.SatPredicate;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import com.booleworks.logicng.solvers.sat.SolverTestSet;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class OptimizationFunctionTest implements LogicNGTest {

    public static List<Arguments> solverSuppliers() {
        final List<Arguments> solverSuppliers = SolverTestSet.solverSupplierTestSetForParameterizedTests(
                Set.of(USE_AT_MOST_CLAUSES, CNF_METHOD, INITIAL_PHASE, PROOF_GENERATION));
        return solverSuppliers.stream()
                .map(args -> Arguments.of(
                        args.get()[0],
                        FormulaFactory.caching(FormulaFactoryConfig.builder()
                                .formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build()),
                        args.get()[1]))
                .collect(Collectors.toList());
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    public void testUnsatFormula(final Function<FormulaFactory, SatSolver> solver, final FormulaFactory f,
                                 final String solverDescription)
            throws ParserException {
        final Formula formula = f.parse("a & b & (a => ~b)");
        final LngResult<Model> minimumModel = optimize(Collections.singleton(formula),
                formula.variables(f), Collections.emptyList(), false, solver.apply(f), NopHandler.get());
        assertThat(minimumModel).isNull();
        final LngResult<Model> maximumModel = optimize(Collections.singleton(formula),
                formula.variables(f), Collections.emptyList(), true, solver.apply(f), NopHandler.get());
        assertThat(maximumModel).isNull();
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    public void testSingleModel(final Function<FormulaFactory, SatSolver> solver, final FormulaFactory f,
                                final String solverDescription)
            throws ParserException {
        final Formula formula = f.parse("~a & ~b & ~c");
        final LngResult<Model> minimumModel = optimize(Collections.singleton(formula),
                formula.variables(f), Collections.emptyList(), false, solver.apply(f), NopHandler.get());
        testMinimumModel(formula, minimumModel, formula.variables(f));
        final LngResult<Model> maximumModel = optimize(Collections.singleton(formula),
                formula.variables(f), Collections.emptyList(), true, solver.apply(f), NopHandler.get());
        testMaximumModel(formula, maximumModel, formula.variables(f));
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    public void testExoModel(final Function<FormulaFactory, SatSolver> solver, final FormulaFactory f,
                             final String solverDescription) {
        final CardinalityConstraint exo =
                (CardinalityConstraint) f.exo(f.variable("a"), f.variable("b"), f.variable("c"));
        final LngResult<Model> minimumModel = optimize(Collections.singleton(exo), exo.variables(f),
                Collections.emptyList(), false, solver.apply(f), NopHandler.get());
        testMinimumModel(exo, minimumModel, exo.variables(f));
        final LngResult<Model> maximumModel = optimize(Collections.singleton(exo), exo.variables(f),
                Collections.emptyList(), true, solver.apply(f), NopHandler.get());
        testMaximumModel(exo, maximumModel, exo.variables(f));
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    public void testCornerCases(final Function<FormulaFactory, SatSolver> solver, final FormulaFactory f,
                                final String solverDescription) {
        final FormulaCornerCases cornerCases = new FormulaCornerCases(f);
        for (final Formula formula : cornerCases.cornerCases()) {
            if (formula.holds(new SatPredicate(f))) {
                final Set<Variable> targetLiterals = cornerCases.getVariables();
                final LngResult<Model> minimumModel = optimize(Collections.singleton(formula),
                        targetLiterals, Collections.emptySet(), false, solver.apply(f), NopHandler.get());
                testMinimumModel(formula, minimumModel, targetLiterals);
                final LngResult<Model> maximumModel = optimize(Collections.singleton(formula),
                        targetLiterals, Collections.emptySet(), true, solver.apply(f), NopHandler.get());
                testMaximumModel(formula, maximumModel, targetLiterals);
            }
        }
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @RandomTag
    @Execution(ExecutionMode.SAME_THREAD)

    public void testRandomSmall(final Function<FormulaFactory, SatSolver> solver, final FormulaFactory f0,
                                final String solverDescription) {
        final FormulaFactory f = FormulaFactory.nonCaching(FormulaFactoryConfig.builder()
                // caching factory goes out of heap
                .formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        final SatPredicate satPredicate = new SatPredicate(f);
        final Random random = new Random(42);
        final FormulaRandomizer randomizer =
                new FormulaRandomizer(f, FormulaRandomizerConfig.builder().numVars(6).weightPbc(2).seed(42).build());
        for (int i = 0; i < 1000; i++) {
            final Formula formula = Stream.generate(() -> randomizer.formula(2))
                    .filter(fm -> fm.holds(satPredicate)).findFirst().get();
            final List<Variable> variables = new ArrayList<>(formula.variables(f));

            final Set<Literal> targetLiterals =
                    randomTargetLiterals(f, random, randomSubset(random, variables, Math.min(variables.size(), 5)));
            final Set<Variable> additionalVariables = randomSubset(random, variables, Math.min(variables.size(), 3));

            final LngResult<Model> minimumModel = optimize(Collections.singleton(formula),
                    targetLiterals, additionalVariables, false, solver.apply(f), NopHandler.get());
            testMinimumModel(formula, minimumModel, targetLiterals);

            final LngResult<Model> maximumModel = optimize(Collections.singleton(formula),
                    targetLiterals, additionalVariables, true, solver.apply(f), NopHandler.get());
            testMaximumModel(formula, maximumModel, targetLiterals);
        }
    }

    private static <T> Set<T> randomSubset(final Random random, final List<T> elements, final int subsetSize) {
        if (subsetSize > elements.size()) {
            throw new IllegalArgumentException();
        }
        final Set<T> subset = new HashSet<>();
        while (subset.size() < subsetSize) {
            subset.add(elements.get(random.nextInt(elements.size())));
        }
        return subset;
    }

    private static SortedSet<Literal> randomTargetLiterals(final FormulaFactory f, final Random random,
                                                           final Collection<Variable> variables) {
        return variables.stream().map(var -> f.literal(var.getName(), random.nextBoolean()))
                .collect(toCollection(TreeSet::new));
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    public void testIncrementalityMinimizeAndMaximize(final Function<FormulaFactory, SatSolver> solverSupplier, final FormulaFactory f)
            throws ParserException {
        Formula formula = f.parse("(a|b|c|d|e) & (p|q) & (x|y|z)");
        final SatSolver solver = solverSupplier.apply(f);
        final SortedSet<Variable> vars = new TreeSet<>(formula.variables(f));
        solver.add(formula);

        Model minimumModel = solver.execute(builder().minimize().literals(vars).build());
        Model maximumModel = solver.execute(builder().maximize().literals(vars).build());
        assertThat(minimumModel.positiveVariables()).hasSize(3);
        assertThat(maximumModel.positiveVariables()).hasSize(10);

        formula = f.parse("~p");
        vars.addAll(formula.variables(f));
        solver.add(formula);
        minimumModel = solver.execute(builder().minimize().literals(vars).build());
        maximumModel = solver.execute(builder().maximize().literals(vars).build());
        assertThat(minimumModel.positiveVariables()).hasSize(3).contains(f.variable("q"));
        assertThat(maximumModel.positiveVariables()).hasSize(9).contains(f.variable("q"));

        formula = f.parse("(x => n) & (y => m) & (a => ~b & ~c)");
        vars.addAll(formula.variables(f));
        solver.add(formula);
        minimumModel = solver.execute(builder().minimize().literals(vars).build());
        maximumModel = solver.execute(builder().maximize().literals(vars).build());
        assertThat(minimumModel.positiveVariables()).hasSize(3).contains(f.variable("q"), f.variable("z"));
        assertThat(maximumModel.positiveVariables()).hasSize(10)
                .contains(f.variable("q"), f.variable("z"))
                .doesNotContain(f.variable("a"));

        formula = f.parse("(z => v & w) & (m => v) & (b => ~c & ~d & ~e)");
        vars.addAll(formula.variables(f));
        solver.add(formula);
        minimumModel = solver.execute(builder().minimize().literals(vars).build());
        maximumModel = solver.execute(builder().maximize().literals(vars).build());
        assertThat(minimumModel.positiveVariables()).hasSize(4).contains(f.variable("q"), f.variable("x"),
                f.variable("n"));
        assertThat(maximumModel.positiveVariables()).hasSize(11)
                .contains(f.variable("q"), f.variable("x"), f.variable("n"), f.variable("v"), f.variable("w"))
                .doesNotContain(f.variable("b"));

        formula = f.parse("~q");
        vars.addAll(formula.variables(f));
        solver.add(formula);
        assertThatThrownBy(() -> solver.execute(builder().minimize().literals(vars).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The given formula must be satisfiable");
        assertThatThrownBy(() -> solver.execute(builder().maximize().literals(vars).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The given formula must be satisfiable");
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    public void testAdditionalVariables(final Function<FormulaFactory, SatSolver> solver, final FormulaFactory f,
                                        final String solverDescription)
            throws ParserException {
        final Variable a = f.variable("a");
        final Literal na = f.literal("a", false);
        final Variable b = f.variable("b");
        final Literal nb = f.literal("b", false);
        final Variable c = f.variable("c");
        final Variable x = f.variable("x");
        final Literal nx = f.literal("x", false);
        final Variable y = f.variable("y");

        final Formula formula = f.parse("(a|b) & (~a => c) & (x|y)");

        final List<Literal> literalsANBX = Arrays.asList(a, nb, x);
        final LngResult<Model> minimumModel = optimize(Collections.singleton(formula), literalsANBX,
                Collections.emptyList(), false, solver.apply(f), NopHandler.get());
        assertThat(minimumModel.getResult().getLiterals()).containsExactlyInAnyOrder(na, b, nx);
        final LngResult<Model> minimumModelWithY = optimize(Collections.singleton(formula),
                literalsANBX, Collections.singleton(y), false, solver.apply(f), NopHandler.get());
        assertThat(minimumModelWithY.getResult().getLiterals())
                .containsExactlyInAnyOrder(na, b, nx, y);
        final Model minimumModelWithCY = optimize(Collections.singleton(formula),
                literalsANBX, Arrays.asList(c, y), false, solver.apply(f), NopHandler.get()).getResult();
        assertThat(minimumModelWithCY.getLiterals()).containsExactlyInAnyOrder(na, b, c, nx, y);

        final List<Literal> literalsNBNX = Arrays.asList(na, nx);
        final Model maximumModel = optimize(Collections.singleton(formula), literalsNBNX, Collections.emptyList(),
                true, solver.apply(f), NopHandler.get()).getResult();
        assertThat(maximumModel.getLiterals()).containsExactlyInAnyOrder(na, nx);
        final Model maximumModelWithC = optimize(Collections.singleton(formula), literalsNBNX,
                Collections.singleton(c), true, solver.apply(f), NopHandler.get()).getResult();
        assertThat(maximumModelWithC.getLiterals()).containsExactlyInAnyOrder(na, c, nx);
        final Model maximumModelWithACY = optimize(Collections.singleton(formula), literalsNBNX,
                Arrays.asList(a, c, y), true, solver.apply(f), NopHandler.get()).getResult();
        assertThat(maximumModelWithACY.getLiterals()).containsExactlyInAnyOrder(na, c, nx, y);
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @LongRunningTag
    public void testLargeFormulaMinimize(final Function<FormulaFactory, SatSolver> solver, final FormulaFactory f,
                                         final String solverDescription)
            throws IOException, ParserException {
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/large_formula.txt");
        final List<Variable> variables = randomSubset(formula.variables(f), 300);
        final LngResult<Model> minimumModel = optimize(Collections.singleton(formula), variables,
                Collections.emptyList(), false, solver.apply(f), NopHandler.get());
        testMinimumModel(formula, minimumModel, variables);
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @LongRunningTag
    public void testLargeFormulaMaximize(final Function<FormulaFactory, SatSolver> solver, final FormulaFactory f,
                                         final String solverDescription)
            throws IOException, ParserException {
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/large_formula.txt");
        final LngResult<Model> maximumModel = optimize(Collections.singleton(formula),
                formula.variables(f), Collections.emptyList(), true, solver.apply(f), NopHandler.get());
        testMaximumModel(formula, maximumModel, formula.variables(f));
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @LongRunningTag
    public void testLargerFormulaMinimize(final Function<FormulaFactory, SatSolver> solver, final FormulaFactory f,
                                          final String solverDescription)
            throws IOException, ParserException {
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/small_formulas.txt");
        final LngResult<Model> minimumModel = optimize(Collections.singleton(formula),
                formula.variables(f), Collections.emptyList(), false, solver.apply(f), NopHandler.get());
        testMinimumModel(formula, minimumModel, formula.variables(f));
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @LongRunningTag
    public void testLargerFormulaMaximize(final Function<FormulaFactory, SatSolver> solver, final FormulaFactory f,
                                          final String solverDescription)
            throws IOException, ParserException {
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/small_formulas.txt");
        final List<Variable> variables = randomSubset(formula.variables(f), 300);
        final LngResult<Model> maximumModel = optimize(Collections.singleton(formula), variables,
                Collections.emptyList(), true, solver.apply(f), NopHandler.get());
        testMaximumModel(formula, maximumModel, variables);
    }

    @Test
    public void compareWithMaxSat() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final BufferedReader reader =
                new BufferedReader(new FileReader("../test_files/formulas/large_formula.txt"));
        final List<Formula> formulas = new ArrayList<>();
        final SortedSet<Variable> variables = new TreeSet<>();
        while (reader.ready()) {
            final Formula parsed = p.parse(reader.readLine());
            formulas.add(parsed);
            variables.addAll(parsed.variables(f));
        }
        final int expected = 25;
        assertThat(solveMaxSat(formulas, variables, MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_INC_WBO))).isEqualTo(expected);
        assertThat(solveMaxSat(formulas, variables, MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_LINEAR_SU))).isEqualTo(expected);
        assertThat(solveMaxSat(formulas, variables, MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_LINEAR_US))).isEqualTo(expected);
        assertThat(solveMaxSat(formulas, variables, MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_MSU3))).isEqualTo(expected);
        assertThat(solveMaxSat(formulas, variables, MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_WBO))).isEqualTo(expected);
        assertThat(satisfiedLiterals(optimize(formulas, variables, Collections.emptyList(), false,
                SatSolver.newSolver(f, SatSolverConfig.builder().useAtMostClauses(false).build()),
                NopHandler.get()), variables).size())
                .isEqualTo(expected);
        assertThat(satisfiedLiterals(optimize(formulas, variables, Collections.emptyList(), false,
                SatSolver.newSolver(f, SatSolverConfig.builder().useAtMostClauses(true).build()),
                NopHandler.get()), variables).size())
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @LongRunningTag
    public void testTimeoutOptimizationHandler(final Function<FormulaFactory, SatSolver> solver, final FormulaFactory f,
                                               final String solverDescription)
            throws IOException, ParserException {
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/large_formula.txt");
        final TimeoutHandler handlerMax = new TimeoutHandler(1L);
        final LngResult<Model> maximumModel = optimize(Collections.singleton(formula),
                formula.variables(f), Collections.emptyList(), true, solver.apply(f), handlerMax);
        assertThat(maximumModel.isSuccess()).isFalse();
        assertThat(maximumModel.isPartial()).isFalse();
        assertThat(maximumModel.getPartialResult()).isNull();

        final TimeoutHandler handlerTooShort = new TimeoutHandler(0L);
        final LngResult<Model> model = optimize(Collections.singleton(formula), formula.variables(f),
                Collections.emptyList(), false, solver.apply(f), handlerTooShort);
        assertThat(model.isSuccess()).isFalse();
        assertThat(model.isPartial()).isFalse();
        assertThat(model.getPartialResult()).isNull();
        final CustomOptimizationHandler customHandler = new CustomOptimizationHandler();
        final LngResult<Model> modelCustom = optimize(Collections.singleton(formula),
                formula.variables(f), Collections.emptyList(), true, solver.apply(f), customHandler);
        assertThat(modelCustom.isSuccess()).isFalse();
        assertThat(modelCustom.isPartial()).isTrue();
        assertThat(modelCustom.getPartialResult()).isNotNull();
        assertThatThrownBy(modelCustom::getResult).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @LongRunningTag
    public void testCancellationPoints(final Function<FormulaFactory, SatSolver> solverSupplier, final FormulaFactory f0,
                                       final String solverDescription) throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final SortedSet<Variable> selVars = new TreeSet<>();
        final List<Formula> clauses = DimacsReader.readCNF(f, "../test_files/sat/c499_gr_rcs_w6.shuffled.cnf");
        final List<Formula> formulas = new ArrayList<>();
        for (final Formula clause : clauses) {
            final Variable selVar = f.variable("@SEL_" + selVars.size());
            selVars.add(selVar);
            formulas.add(f.equivalence(selVar, clause));
        }
        final SatSolver solver = solverSupplier.apply(f);
        for (int numSatHandlerStarts = 1; numSatHandlerStarts < 4; numSatHandlerStarts++) {
            solver.add(formulas);
            final ComputationHandler handler = new BoundedOptimizationHandler(numSatHandlerStarts, -1);
            final OptimizationFunction optimizationFunction = builder().literals(selVars).maximize().build();
            final LngResult<Model> result = solver.execute(optimizationFunction, handler);
            assertThat(result.isSuccess()).isFalse();
        }
    }

    private int solveMaxSat(final List<Formula> formulas, final SortedSet<Variable> variables,
                            final MaxSatSolver solver) {
        formulas.forEach(solver::addHardFormula);
        variables.forEach(v -> solver.addSoftFormula(v.negate(formulas.iterator().next().getFactory()), 1));
        return solver.solve().getOptimum();
    }

    private List<Literal> satisfiedLiterals(final LngResult<Model> model,
                                            final Collection<? extends Literal> literals) {
        final Set<Literal> modelLiterals = new HashSet<>(model.getResult().getLiterals());
        return literals.stream().filter(modelLiterals::contains).collect(Collectors.toList());
    }

    private static LngResult<Model> optimize(
            final Collection<Formula> formulas, final Collection<? extends Literal> literals,
            final Collection<Variable> additionalVariables, final boolean maximize,
            final SatSolver solver, final ComputationHandler handler) {
        formulas.forEach(solver::add);
        if (!solver.sat()) {
            return null;
        }
        if (maximize) {
            return solver.execute(builder().maximize().literals(literals)
                    .additionalVariables(additionalVariables).build(), handler);
        } else {
            return solver.execute(builder().minimize().literals(literals)
                    .additionalVariables(additionalVariables).build(), handler);
        }
    }

    private void testMinimumModel(final Formula formula, final LngResult<Model> resultModel,
                                  final Collection<? extends Literal> literals) {
        testOptimumModel(formula, resultModel, literals, false);
    }

    private void testMaximumModel(final Formula formula, final LngResult<Model> resultModel,
                                  final Collection<? extends Literal> literals) {
        testOptimumModel(formula, resultModel, literals, true);
    }

    private void testOptimumModel(final Formula formula, final LngResult<Model> optimumModel,
                                  final Collection<? extends Literal> literals, final boolean maximize) {
        assertThat(optimumModel.isSuccess()).isTrue();
        final FormulaFactory f = formula.getFactory();
        final List<Literal> optimumLiterals = optimumModel.getResult().getLiterals();
        if (literals.isEmpty()) {
            assertThat(optimumLiterals).isEmpty();
        } else {
            final int actualNumSatisfied = satisfiedLiterals(optimumModel, literals).size();
            final MaxSatSolver solver = MaxSatSolver.newSolver(f, MaxSatConfig.CONFIG_OLL);
            solver.addHardFormula(formula);
            literals.forEach(l -> solver.addSoftFormula(maximize ? l : l.negate(f), 1));
            final int numSatisfiedOll = satisfiedLiterals(LngResult.of(solver.solve().getModel()), literals).size();
            assertThat(actualNumSatisfied).isEqualTo(numSatisfiedOll);
        }
    }

    private static List<Variable> randomSubset(final Collection<Variable> original, final int size) {
        final List<Variable> variables = new ArrayList<>(original);
        Collections.shuffle(variables, new Random(42));
        return variables.subList(0, size);
    }

    private static class CustomOptimizationHandler implements ComputationHandler {
        public Model currentResult;
        private boolean canceled;

        @Override
        public boolean shouldResume(final LngEvent event) {
            if (event instanceof OptimizationFoundBetterBoundEvent) {
                currentResult = ((OptimizationFoundBetterBoundEvent) event).getModel().get();
                canceled = currentResult.positiveVariables().size() >= 161;
            }
            return !canceled;
        }
    }
}
