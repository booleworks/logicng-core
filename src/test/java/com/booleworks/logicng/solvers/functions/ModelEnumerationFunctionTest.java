// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.solvers.functions.ModelEnumerationFunction.ModelEnumerationCollector.getCartesianProduct;
import static com.booleworks.logicng.util.FormulaHelper.strings2literals;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySortedSet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.NumberOfModelsHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.modelcounting.ModelCounter;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.EnumerationCollectorTestHandler;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.solvers.functions.modelenumeration.NoSplitModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.FixedVariableProvider;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.LeastCommonVariablesProvider;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.MostCommonVariablesProvider;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.SplitVariableProvider;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import com.booleworks.logicng.util.FormulaHelper;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ModelEnumerationFunctionTest extends TestWithFormulaContext {

    private FormulaFactory f;

    public static Collection<Object[]> splitProviders() {
        final List<Object[]> providers = new ArrayList<>();
        providers.add(new Object[]{null});
        providers.add(new Object[]{new LeastCommonVariablesProvider()});
        providers.add(new Object[]{new MostCommonVariablesProvider()});
        return providers;
    }

    @BeforeEach
    public void init() {
        f = FormulaFactory.caching();
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testContradiction(final SplitVariableProvider splitProvider) {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.literal("A", true));
        solver.add(f.literal("A", false));
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(List.of()).configuration(config).build());
        assertThat(models).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testTautology(final SplitVariableProvider splitProvider) {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = SATSolver.newSolver(f);
        List<Model> models = solver.execute(ModelEnumerationFunction.builder(List.of()).configuration(config).build());
        assertThat(models).containsExactly(new Model());
        final SortedSet<Variable> additionalVars = f.variables("A", "B");
        models = solver.execute(ModelEnumerationFunction.builder(List.of()).additionalVariables(additionalVars).configuration(config).build());
        assertThat(models).hasSize(1);
        assertThat(variables(models.get(0))).containsAll(additionalVars);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testEmptyEnumerationVariables(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = SATSolver.newSolver(f);
        final Formula formula = f.parse("A & (B | C)");
        solver.add(formula);
        List<Model> models = solver.execute(ModelEnumerationFunction.builder(List.of()).configuration(config).build());
        assertThat(models).containsExactly(new Model());
        models = solver.execute(ModelEnumerationFunction.builder(List.of()).additionalVariables(formula.variables(f)).configuration(config).build());
        assertThat(models).hasSize(1);
        assertThat(variables(models.get(0))).containsAll(formula.variables(f));
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testSimple1(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.parse("A & (B | C)"));
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(f.variables("A", "B", "C")).configuration(config).build());
        assertThat(modelsToSets(models)).containsExactlyInAnyOrder(
                set(f.variable("A"), f.variable("B"), f.variable("C")),
                set(f.variable("A"), f.variable("B"), f.literal("C", false)),
                set(f.variable("A"), f.literal("B", false), f.variable("C"))
        );
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testSimple2(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.parse("(~A | C) & (~B | C)"));
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(f.variables("A", "B", "C")).configuration(config).build());
        assertThat(models).hasSize(5);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testResultLiteralOrderIndependentFromInputOrder(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.parse("A & (B | C)"));
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(f.variables("A", "B", "C")).configuration(config).build());
        final List<Model> modelsABC = solver.execute(ModelEnumerationFunction.builder(f.variables("A", "B", "C")).configuration(config).build());
        final List<Model> modelsBCA = solver.execute(ModelEnumerationFunction.builder(f.variables("B", "C", "A")).configuration(config).build());

        assertThat(modelsToSets(models)).containsExactlyInAnyOrder(
                set(f.variable("A"), f.variable("B"), f.variable("C")),
                set(f.variable("A"), f.variable("B"), f.literal("C", false)),
                set(f.variable("A"), f.literal("B", false), f.variable("C"))
        );
        assertThat(models).containsExactlyInAnyOrderElementsOf(modelsABC);
        assertThat(modelsABC).containsExactlyInAnyOrderElementsOf(modelsBCA);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testDuplicateEnumerationVariables(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.parse("A & (B | C)"));
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(f.variables("A", "A", "B"))
                .configuration(config).build());
        assertThat(modelsToSets(models)).containsExactlyInAnyOrder(
                set(f.variable("A"), f.variable("B")),
                set(f.variable("A"), f.literal("B", false))
        );
        assertThat(models).extracting(Model::size).allMatch(size -> size == 2);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testMultipleModelEnumeration(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = SATSolver.newSolver(f);
        final Formula formula = f.parse("(~A | C) & (~B | C)");
        solver.add(formula);
        final ModelEnumerationFunction meFunction = ModelEnumerationFunction.builder(formula.variables(f)).configuration(config).build();
        final List<Model> firstRun = solver.execute(meFunction);
        final List<Model> secondRun = solver.execute(meFunction);
        assertThat(firstRun).hasSize(5);
        assertThat(modelsToSets(firstRun)).containsExactlyInAnyOrderElementsOf(modelsToSets(secondRun));
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testAdditionalVariablesSimple(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.parse("A & C | B & ~C"));
        final Variable a = f.variable("A");
        final Variable b = f.variable("B");
        final Variable c = f.variable("C");
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(List.of(a, b))
                .additionalVariables(Collections.singletonList(c))
                .configuration(config)
                .build());
        assertThat(models).hasSize(3); // (A, B), (A, ~B), (~A, B)
        for (final Model model : models) {
            assertThat(variables(model)).containsExactly(a, b, c);
        }
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testDuplicateAdditionalVariables(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.parse("A & (B | C)"));
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(f.variables("A"))
                .additionalVariables(f.variables("B", "B"))
                .configuration(config).build());
        assertThat(models).hasSize(1);
        assertThat(models).extracting(Model::size).allMatch(size -> size == 2);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    @LongRunningTag
    public void testDontCareVariables1(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.parse("(~A | C) & (~B | C)"));
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(f.variables("A", "B", "C", "D"))
                .configuration(config)
                .build());
        assertThat(modelsToSets(models)).containsExactlyInAnyOrder(
                // models with ~D
                strings2literals(f, List.of("A", "~B", "C", "~D"), "~"),
                strings2literals(f, List.of("A", "B", "C", "~D"), "~"),
                strings2literals(f, List.of("~A", "~B", "~C", "~D"), "~"),
                strings2literals(f, List.of("~A", "~B", "C", "~D"), "~"),
                strings2literals(f, List.of("~A", "B", "C", "~D"), "~"),
                // models with D
                strings2literals(f, List.of("A", "~B", "C", "D"), "~"),
                strings2literals(f, List.of("A", "B", "C", "D"), "~"),
                strings2literals(f, List.of("~A", "~B", "~C", "D"), "~"),
                strings2literals(f, List.of("~A", "~B", "C", "D"), "~"),
                strings2literals(f, List.of("~A", "B", "C", "D"), "~")
        );
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testDontCareVariables2(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.parse("(~A | C) & (~B | C)"));
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(f.variables("A", "C", "D", "E"))
                .configuration(config)
                .build());
        assertThat(modelsToSets(models)).containsExactlyInAnyOrder(
                // models with ~D, ~E
                strings2literals(f, List.of("A", "C", "~D", "~E"), "~"),
                strings2literals(f, List.of("~A", "C", "~D", "~E"), "~"),
                strings2literals(f, List.of("~A", "~C", "~D", "~E"), "~"),
                // models with ~D, E
                strings2literals(f, List.of("A", "C", "~D", "E"), "~"),
                strings2literals(f, List.of("~A", "C", "~D", "E"), "~"),
                strings2literals(f, List.of("~A", "~C", "~D", "E"), "~"),
                // models with D, ~E
                strings2literals(f, List.of("A", "C", "D", "~E"), "~"),
                strings2literals(f, List.of("~A", "C", "D", "~E"), "~"),
                strings2literals(f, List.of("~A", "~C", "D", "~E"), "~"),
                // models with D, E
                strings2literals(f, List.of("A", "C", "D", "E"), "~"),
                strings2literals(f, List.of("~A", "C", "D", "E"), "~"),
                strings2literals(f, List.of("~A", "~C", "D", "E"), "~")
        );
    }

    @Test
    public void testDontCareVariables3() throws ParserException {
        final FixedVariableProvider splitProvider = new FixedVariableProvider(new TreeSet<>(f.variables("X")));
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(3).build()).build();
        final SATSolver solver = SATSolver.newSolver(f);
        final Formula formula = f.parse("A | B | (X & ~X)"); // X will be simplified out and become a don't care variable unknown by the solver
        solver.add(formula);
        final SortedSet<Variable> enumerationVars = new TreeSet<>(f.variables("A", "B", "X"));
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(enumerationVars)
                .configuration(config)
                .build());
        assertThat(models).hasSize(6);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testHandlerWithNumModelsLimit(final SplitVariableProvider splitProvider) throws ParserException {
        final NumberOfModelsHandler handler = new NumberOfModelsHandler(3);
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().handler(handler)
                        .strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(3).build()).build();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.parse("(~A | C) & (~B | C)"));
        final List<Model> models = solver.execute(ModelEnumerationFunction.builder(f.variables("A", "B", "C")).configuration(config).build());
        assertThat(handler.aborted()).isTrue();
        assertThat(models).hasSize(3);
    }

    @RandomTag
    @Test
    public void testAdditionalVariables() {
        final ModelEnumerationConfig config = ModelEnumerationConfig.builder()
                .strategy(DefaultModelEnumerationStrategy.builder().splitVariableProvider(new LeastCommonVariablesProvider()).maxNumberOfModels(10).build())
                .build();

        for (int i = 1; i <= 1000; i++) {
            // given
            final SATSolver solver = SATSolver.newSolver(f);
            final FormulaRandomizer randomizer = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(i).numVars(20).build());
            final Formula formula = randomizer.formula(4);
            solver.add(formula);

            final List<Variable> varsFormula = new ArrayList<>(formula.variables(f));
            final int numberOfVars = formula.variables(f).size();
            final int minNumberOfVars = (int) Math.ceil(numberOfVars / (double) 5);
            final SortedSet<Variable> pmeVars = new TreeSet<>(varsFormula.subList(0, minNumberOfVars));

            final int additionalVarsStart = Math.min(4 * minNumberOfVars, numberOfVars);
            final SortedSet<Variable> additionalVars = new TreeSet<>(varsFormula.subList(additionalVarsStart, varsFormula.size()));

            // when
            final List<Model> modelsRecursive = solver.execute(ModelEnumerationFunction.builder(pmeVars)
                    .additionalVariables(additionalVars)
                    .configuration(config).build());

            // check that models are buildable and every model contains all additional variables
            for (final Model model : modelsRecursive) {
                assertThat(variables(model)).containsAll(additionalVars);
                assertThat(solver.satCall().addFormulas(model.getLiterals()).sat()).isEqualTo(Tristate.TRUE);
            }
        }
    }

    @Test
    @RandomTag
    public void testRandomFormulas() {
        for (int i = 1; i <= 100; i++) {
            final FormulaRandomizer randomizer = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(i).numVars(15).build());
            final Formula formula = randomizer.formula(3);

            final SATSolver solver = SATSolver.newSolver(f);
            solver.add(formula);

            // no split
            final var count = ModelCounter.count(f, List.of(formula), formula.variables(f));
            final ModelEnumerationConfig configNoSplit = ModelEnumerationConfig.builder().strategy(NoSplitModelEnumerationStrategy.get()).build();
            final List<Model> models = solver.execute(ModelEnumerationFunction.builder(formula.variables(f)).configuration(configNoSplit).build());

            // recursive call: least common vars
            final ModelEnumerationConfig configLcv = ModelEnumerationConfig.builder().strategy(DefaultModelEnumerationStrategy.builder().splitVariableProvider(new LeastCommonVariablesProvider()).maxNumberOfModels(500).build()).build();
            final List<Model> models1 = solver.execute(ModelEnumerationFunction.builder(formula.variables(f)).configuration(configLcv).build());

            // recursive call: most common vars
            final ModelEnumerationConfig configMcv = ModelEnumerationConfig.builder().strategy(DefaultModelEnumerationStrategy.builder().splitVariableProvider(new MostCommonVariablesProvider()).maxNumberOfModels(500).build()).build();
            final List<Model> models2 = solver.execute(ModelEnumerationFunction.builder(formula.variables(f)).configuration(configMcv).build());

            assertThat(models1.size()).isEqualTo(count.intValue());
            assertThat(models2.size()).isEqualTo(count.intValue());

            final List<Set<Literal>> setNoSplit = modelsToSets(models);
            assertThat(setNoSplit).containsExactlyInAnyOrderElementsOf(modelsToSets(models1));
            assertThat(setNoSplit).containsExactlyInAnyOrderElementsOf(modelsToSets(models2));
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCollector(final FormulaContext _c) {
        final SATSolver solver = SATSolver.newSolver(_c.f);
        solver.add(_c.eq1);

        final EnumerationCollectorTestHandler handler = new EnumerationCollectorTestHandler();
        final ModelEnumerationFunction.ModelEnumerationCollector collector =
                new ModelEnumerationFunction.ModelEnumerationCollector(f, emptySortedSet(), emptySortedSet());
        assertThat(collector.getResult()).isEmpty();
        assertThat(handler.getFoundModels()).isZero();
        assertThat(handler.getCommitCalls()).isZero();
        assertThat(handler.getRollbackCalls()).isZero();

        final LNGBooleanVector modelFromSolver1 = new LNGBooleanVector(true, true);
        final LNGBooleanVector modelFromSolver2 = new LNGBooleanVector(false, false);

        final Model expectedModel1 = new Model(_c.a, _c.b);
        final Model expectedModel2 = new Model(_c.na, _c.nb);
        final LNGIntVector relevantIndices = new LNGIntVector(new int[]{0, 1});

        collector.addModel(modelFromSolver1, solver, relevantIndices, handler);
        assertThat(collector.getResult()).isEmpty();
        assertThat(handler.getFoundModels()).isEqualTo(1);
        assertThat(handler.getCommitCalls()).isZero();
        assertThat(handler.getRollbackCalls()).isZero();

        collector.commit(handler);
        assertThat(collector.getResult()).containsExactly(expectedModel1);
        assertThat(handler.getFoundModels()).isEqualTo(1);
        assertThat(handler.getCommitCalls()).isEqualTo(1);
        assertThat(handler.getRollbackCalls()).isZero();
        final List<Model> result1 = collector.getResult();

        collector.addModel(modelFromSolver2, solver, relevantIndices, handler);
        assertThat(collector.getResult()).isEqualTo(result1);
        assertThat(handler.getFoundModels()).isEqualTo(2);
        assertThat(handler.getCommitCalls()).isEqualTo(1);
        assertThat(handler.getRollbackCalls()).isZero();

        collector.rollback(handler);
        assertThat(collector.getResult()).isEqualTo(result1);
        assertThat(handler.getFoundModels()).isEqualTo(2);
        assertThat(handler.getCommitCalls()).isEqualTo(1);
        assertThat(handler.getRollbackCalls()).isEqualTo(1);

        collector.addModel(modelFromSolver2, solver, relevantIndices, handler);
        final List<Model> rollbackModels = collector.rollbackAndReturnModels(solver, handler);
        assertThat(rollbackModels).containsExactly(expectedModel2);
        assertThat(collector.getResult()).isEqualTo(result1);
        assertThat(handler.getFoundModels()).isEqualTo(3);
        assertThat(handler.getCommitCalls()).isEqualTo(1);
        assertThat(handler.getRollbackCalls()).isEqualTo(2);

        collector.addModel(modelFromSolver2, solver, relevantIndices, handler);
        collector.commit(handler);
        assertThat(collector.getResult()).containsExactlyInAnyOrder(expectedModel1, expectedModel2);
        assertThat(handler.getFoundModels()).isEqualTo(4);
        assertThat(handler.getCommitCalls()).isEqualTo(2);
        assertThat(handler.getRollbackCalls()).isEqualTo(2);
        final List<Model> result2 = collector.getResult();

        collector.rollback(handler);
        assertThat(collector.getResult()).isEqualTo(result2);
        assertThat(collector.rollbackAndReturnModels(solver, handler)).isEmpty();
        assertThat(handler.getFoundModels()).isEqualTo(4);
        assertThat(handler.getCommitCalls()).isEqualTo(2);
        assertThat(handler.getRollbackCalls()).isEqualTo(4);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testGetCartesianProduct(final FormulaContext _c) {
        assertThat(getCartesianProduct(emptySortedSet())).containsExactly(emptyList());
        assertThat(getCartesianProduct(new TreeSet<>(singletonList(_c.a)))).containsExactly(
                singletonList(_c.a),
                singletonList(_c.na));
        assertThat(getCartesianProduct(new TreeSet<>(
                List.of(_c.a, _c.b, _c.c)))).containsExactly(
                List.of(_c.a, _c.b, _c.c),
                List.of(_c.a, _c.b, _c.nc),
                List.of(_c.a, _c.nb, _c.c),
                List.of(_c.a, _c.nb, _c.nc),
                List.of(_c.na, _c.b, _c.c),
                List.of(_c.na, _c.b, _c.nc),
                List.of(_c.na, _c.nb, _c.c),
                List.of(_c.na, _c.nb, _c.nc)
        );
    }

    @Test
    public void testModelEnumerationSimple() throws ParserException {
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(f.parse("A & (B | C)"));
        final List<Model> models = solver.enumerateAllModels(f.variables("A", "B", "C"));
        assertThat(models).containsExactlyInAnyOrder(
                new Model(f.variable("A"), f.variable("B"), f.variable("C")),
                new Model(f.variable("A"), f.variable("B"), f.literal("C", false)),
                new Model(f.variable("A"), f.literal("B", false), f.variable("C"))
        );
    }

    @Test
    public void testVariableRemovedBySimplificationOccursInModels() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().simplifyComplementaryOperands(true).build());
        final SATSolver solver = SATSolver.newSolver(this.f, SATSolverConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.PG_ON_SOLVER).build());
        final Variable a = f.variable("A");
        final Variable b = f.variable("B");
        final Formula formula = this.f.parse("A & B => A");
        solver.add(formula); // during NNF conversion, used by the PG transformation, the formula simplifies to verum when added to the solver
        final List<Model> models = solver.enumerateAllModels(formula.variables(f));
        assertThat(models).hasSize(4);
        for (final Model model : models) {
            assertThat(FormulaHelper.variables(f, model.getLiterals())).containsExactlyInAnyOrder(a, b);
        }
    }

    @Test
    public void testUnknownVariableNotOccurringInModel() {
        final SATSolver solver = SATSolver.newSolver(f);
        final Variable a = f.variable("A");
        solver.add(a);
        final List<Model> models = solver.enumerateAllModels(f.variables("A", "X"));
        assertThat(models).hasSize(2);
        assertThat(models.get(0).getLiterals()).contains(a);
        assertThat(models.get(0).getLiterals()).contains(a);
    }

    private static List<Set<Literal>> modelsToSets(final List<Model> models) {
        return models.stream().map(x -> new HashSet<>(x.getLiterals())).collect(Collectors.toList());
    }

    private static Set<Literal> set(final Literal... literals) {
        return new HashSet<>(List.of(literals));
    }

    private static List<Variable> variables(final Model model) {
        return model.getLiterals().stream().map(Literal::variable).collect(Collectors.toList());
    }
}
