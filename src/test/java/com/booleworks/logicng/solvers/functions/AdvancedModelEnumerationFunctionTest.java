// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.solvers.functions.AdvancedModelEnumerationFunction.ModelEnumerationCollector.getCartesianProduct;
import static com.booleworks.logicng.testutils.TestUtil.getDontCareVariables;
import static com.booleworks.logicng.testutils.TestUtil.modelCount;
import static com.booleworks.logicng.util.CollectionHelper.union;
import static com.booleworks.logicng.util.FormulaHelper.strings2literals;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySortedSet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.AdvancedNumberOfModelsHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.modelenumeration.AdvancedModelEnumerationConfig;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultAdvancedModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.EnumerationCollectorTestHandler;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitvariablesprovider.FixedVariableProvider;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitvariablesprovider.LeastCommonVariablesProvider;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitvariablesprovider.MostCommonVariablesProvider;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitvariablesprovider.SplitVariableProvider;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class AdvancedModelEnumerationFunctionTest extends TestWithFormulaContext {

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

    @Test
    public void testNonIncrementalSolver() throws ParserException {
        final MiniSat solver = MiniSat.miniSat(f, MiniSatConfig.builder().incremental(false).build());
        solver.add(f.parse("A | B | C"));
        assertThatThrownBy(() -> solver.execute(AdvancedModelEnumerationFunction.builder().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Recursive model enumeration function can only be applied to solvers with load/save state capability.");
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testContradiction(final SplitVariableProvider splitProvider) {
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.literal("A", true));
        solver.add(f.literal("A", false));
        final List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder().variables().configuration(config).build());
        assertThat(models).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testTautology(final SplitVariableProvider splitProvider) {
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = MiniSat.miniSat(f);
        List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder().variables().configuration(config).build());
        assertThat(models).containsExactly(new Model());
        final SortedSet<Variable> additionalVars = f.variables("A", "B");
        models = solver.execute(AdvancedModelEnumerationFunction.builder().variables().additionalVariables(additionalVars).configuration(config).build());
        assertThat(models).hasSize(1);
        assertThat(variables(models.get(0))).containsAll(additionalVars);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testEmptyEnumerationVariables(final SplitVariableProvider splitProvider) throws ParserException {
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = MiniSat.miniSat(f);
        final Formula formula = f.parse("A & (B | C)");
        solver.add(formula);
        List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder().variables().configuration(config).build());
        assertThat(models).containsExactly(new Model());
        models = solver.execute(AdvancedModelEnumerationFunction.builder().variables().additionalVariables(formula.variables(f)).configuration(config).build());
        assertThat(models).hasSize(1);
        assertThat(variables(models.get(0))).containsAll(formula.variables(f));
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testSimple1(final SplitVariableProvider splitProvider) throws ParserException {
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("A & (B | C)"));
        final List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder().configuration(config).build());
        assertThat(modelsToSets(models)).containsExactlyInAnyOrder(
                set(f.variable("A"), f.variable("B"), f.variable("C")),
                set(f.variable("A"), f.variable("B"), f.literal("C", false)),
                set(f.variable("A"), f.literal("B", false), f.variable("C"))
        );
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testSimple2(final SplitVariableProvider splitProvider) throws ParserException {
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("(~A | C) & (~B | C)"));
        final List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder().configuration(config).build());
        assertThat(models).hasSize(5);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testResultLiteralOrderIndependentFromInputOrder(final SplitVariableProvider splitProvider) throws ParserException {
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("A & (B | C)"));
        final List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder().configuration(config).build());
        final List<Model> modelsABC = solver.execute(AdvancedModelEnumerationFunction.builder().variables(f.variables("A", "B", "C")).configuration(config).build());
        final List<Model> modelsBCA = solver.execute(AdvancedModelEnumerationFunction.builder().variables(f.variables("B", "C", "A")).configuration(config).build());

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
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("A & (B | C)"));
        final List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder()
                .variables(f.variables("A", "A", "B"))
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
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("(~A | C) & (~B | C)"));
        final AdvancedModelEnumerationFunction meFunction = AdvancedModelEnumerationFunction.builder().configuration(config).build();
        final List<Model> firstRun = solver.execute(meFunction);
        final List<Model> secondRun = solver.execute(meFunction);
        assertThat(firstRun).hasSize(5);
        assertThat(modelsToSets(firstRun)).containsExactlyInAnyOrderElementsOf(modelsToSets(secondRun));
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testAdditionalVariablesSimple(final SplitVariableProvider splitProvider) throws ParserException {
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("A & C | B & ~C"));
        final Variable a = f.variable("A");
        final Variable b = f.variable("B");
        final Variable c = f.variable("C");
        final List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder()
                .variables(List.of(a, b))
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
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("A & (B | C)"));
        final List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder()
                .variables(f.variables("A"))
                .additionalVariables(f.variables("B", "B"))
                .configuration(config).build());
        assertThat(models).hasSize(1);
        assertThat(models).extracting(Model::size).allMatch(size -> size == 2);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testDontCareVariables1(final SplitVariableProvider splitProvider) throws ParserException {
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("(~A | C) & (~B | C)"));
        final List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder()
                .variables(f.variables("A", "B", "C", "D"))
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
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(2).build())
                        .build();
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("(~A | C) & (~B | C)"));
        final List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder()
                .variables(f.variables("A", "C", "D", "E"))
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
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().strategy(DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(3).build()).build();
        final SATSolver solver = MiniSat.miniSat(f);
        final Formula formula = f.parse("A | B | (X & ~X)"); // X will be simplified out and become a don't care variable unknown by the solver
        solver.add(formula);
        final SortedSet<Variable> enumerationVars = new TreeSet<>(f.variables("A", "B", "X"));
        final List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder()
                .variables(enumerationVars)
                .configuration(config)
                .build());
        assertThat(models).hasSize(6);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testHandlerWithNumModelsLimit(final SplitVariableProvider splitProvider) throws ParserException {
        final AdvancedNumberOfModelsHandler handler = new AdvancedNumberOfModelsHandler(3);
        final AdvancedModelEnumerationConfig config =
                AdvancedModelEnumerationConfig.builder().handler(handler)
                        .strategy(splitProvider == null ? null : DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(splitProvider).maxNumberOfModels(3).build()).build();
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("(~A | C) & (~B | C)"));
        final List<Model> models = solver.execute(AdvancedModelEnumerationFunction.builder().configuration(config).build());
        assertThat(handler.aborted()).isTrue();
        assertThat(models).hasSize(3);
    }

    @RandomTag
    @Test
    public void testAdditionalVariables() {
        final SATSolver solver = MiniSat.miniSat(f);
        final AdvancedModelEnumerationConfig config = AdvancedModelEnumerationConfig.builder()
                .strategy(DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(new LeastCommonVariablesProvider()).maxNumberOfModels(10).build())
                .build();

        for (int i = 1; i <= 1000; i++) {
            // given
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
            final List<Model> modelsRecursive = solver.execute(AdvancedModelEnumerationFunction.builder()
                    .variables(pmeVars)
                    .additionalVariables(additionalVars)
                    .configuration(config).build());

            final List<Assignment> modelsOld =
                    solver.execute(ModelEnumerationFunction.builder().variables(pmeVars).additionalVariables(additionalVars).build());

            final List<Assignment> updatedModels1 = restrictModelsToPmeVars(pmeVars, modelsRecursive);
            final List<Assignment> updatedModels2 = extendByDontCares(restrictAssignmentsToPmeVars(pmeVars, modelsOld), pmeVars);

            assertThat(BigInteger.valueOf(modelsRecursive.size())).isEqualTo(modelCount(modelsOld, pmeVars));
            assertThat(assignmentsToSets(updatedModels1)).containsExactlyInAnyOrderElementsOf(assignmentsToSets(updatedModels2));

            // check that models are buildable and every model contains all additional variables
            for (final Model model : modelsRecursive) {
                assertThat(variables(model)).containsAll(additionalVars);
                solver.add(model.getLiterals());
                assertThat(solver.sat()).isEqualTo(Tristate.TRUE);
                solver.reset();
            }
            solver.reset();
        }
    }

    @Test
    @RandomTag
    public void testRandomFormulas() {
        for (int i = 1; i <= 100; i++) {
            final FormulaRandomizer randomizer = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(i).numVars(15).build());
            final Formula formula = randomizer.formula(3);

            final SATSolver solver = MiniSat.miniSat(f);
            solver.add(formula);

            // no split
            final List<Assignment> modelsNoSplit = solver.execute(ModelEnumerationFunction.builder().build());

            // recursive call: least common vars
            final AdvancedModelEnumerationConfig configLcv =
                    AdvancedModelEnumerationConfig.builder().strategy(DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(new LeastCommonVariablesProvider()).maxNumberOfModels(500).build()).build();
            final List<Model> models1 = solver.execute(AdvancedModelEnumerationFunction.builder().configuration(configLcv).build());

            // recursive call: most common vars
            final AdvancedModelEnumerationConfig configMcv =
                    AdvancedModelEnumerationConfig.builder().strategy(DefaultAdvancedModelEnumerationStrategy.builder().splitVariableProvider(new MostCommonVariablesProvider()).maxNumberOfModels(500).build()).build();
            final List<Model> models2 = solver.execute(AdvancedModelEnumerationFunction.builder().configuration(configMcv).build());

            assertThat(models1.size()).isEqualTo(modelsNoSplit.size());
            assertThat(models2.size()).isEqualTo(modelsNoSplit.size());

            final List<Set<Literal>> setNoSplit = assignmentsToSets(modelsNoSplit);
            assertThat(setNoSplit).containsExactlyInAnyOrderElementsOf(modelsToSets(models1));
            assertThat(setNoSplit).containsExactlyInAnyOrderElementsOf(modelsToSets(models2));
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCollector(final FormulaContext _c) {
        final MiniSat solver = MiniSat.miniSat(_c.f);
        solver.add(_c.eq1);
        solver.sat();

        final EnumerationCollectorTestHandler handler = new EnumerationCollectorTestHandler();
        final AdvancedModelEnumerationFunction.ModelEnumerationCollector collector = new AdvancedModelEnumerationFunction.ModelEnumerationCollector(emptySortedSet(), emptySortedSet());
        assertThat(collector.getResult()).isEmpty();
        assertThat(handler.getFoundModels()).isZero();
        assertThat(handler.getCommitCalls()).isZero();
        assertThat(handler.getRollbackCalls()).isZero();

        final LNGBooleanVector modelFromSolver1 = new LNGBooleanVector(true, true);
        final LNGBooleanVector modelFromSolver2 = new LNGBooleanVector(false, false);

        final Model expectedModel1 = new Model(_c.a, _c.b);
        final Model expectedModel2 = new Model(_c.na, _c.nb);

        collector.addModel(modelFromSolver1, solver, null, handler);
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

        collector.addModel(modelFromSolver2, solver, null, handler);
        assertThat(collector.getResult()).isEqualTo(result1);
        assertThat(handler.getFoundModels()).isEqualTo(2);
        assertThat(handler.getCommitCalls()).isEqualTo(1);
        assertThat(handler.getRollbackCalls()).isZero();

        collector.rollback(handler);
        assertThat(collector.getResult()).isEqualTo(result1);
        assertThat(handler.getFoundModels()).isEqualTo(2);
        assertThat(handler.getCommitCalls()).isEqualTo(1);
        assertThat(handler.getRollbackCalls()).isEqualTo(1);

        collector.addModel(modelFromSolver2, solver, null, handler);
        final List<Model> rollbackModels = collector.rollbackAndReturnModels(solver, handler);
        assertThat(rollbackModels).containsExactly(expectedModel2);
        assertThat(collector.getResult()).isEqualTo(result1);
        assertThat(handler.getFoundModels()).isEqualTo(3);
        assertThat(handler.getCommitCalls()).isEqualTo(1);
        assertThat(handler.getRollbackCalls()).isEqualTo(2);

        collector.addModel(modelFromSolver2, solver, null, handler);
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

    private static List<Assignment> extendByDontCares(final List<Assignment> assignments, final SortedSet<Variable> variables) {
        final SortedSet<Variable> dontCareVars = getDontCareVariables(assignments, variables);
        final List<List<Literal>> cartesianProduct = getCartesianProduct(dontCareVars);
        final List<Assignment> extendedAssignments = new ArrayList<>();
        for (final Assignment assignment : assignments) {
            final SortedSet<Literal> assignmentLiterals = assignment.literals();
            for (final List<Literal> literals : cartesianProduct) {
                extendedAssignments.add(new Assignment(union(assignmentLiterals, literals, TreeSet::new)));
            }
        }
        return extendedAssignments;
    }

    private List<Assignment> restrictAssignmentsToPmeVars(final SortedSet<Variable> pmeVars, final List<Assignment> models) {
        final List<Assignment> updatedModels = new ArrayList<>();
        for (final Assignment assignment : models) {
            final Assignment updatedAssignment = new Assignment();
            for (final Literal literal : assignment.literals()) {
                if (pmeVars.contains(literal.variable())) {
                    updatedAssignment.addLiteral(literal);
                }
            }
            updatedModels.add(updatedAssignment);
        }
        return updatedModels;
    }

    private List<Assignment> restrictModelsToPmeVars(final SortedSet<Variable> pmeVars, final List<Model> models) {
        final List<Assignment> updatedModels = new ArrayList<>();
        for (final Model assignment : models) {
            final Assignment updatedAssignment = new Assignment();
            for (final Literal literal : assignment.getLiterals()) {
                if (pmeVars.contains(literal.variable())) {
                    updatedAssignment.addLiteral(literal);
                }
            }
            updatedModels.add(updatedAssignment);
        }
        return updatedModels;
    }

    private static List<Set<Literal>> modelsToSets(final List<Model> models) {
        return models.stream().map(x -> new HashSet<>(x.getLiterals())).collect(Collectors.toList());
    }

    private static List<Set<Literal>> assignmentsToSets(final List<Assignment> models) {
        return models.stream().map(x -> new HashSet<>(x.literals())).collect(Collectors.toList());
    }

    private static Set<Literal> set(final Literal... literals) {
        return new HashSet<>(List.of(literals));
    }

    private static List<Variable> variables(final Model model) {
        return model.getLiterals().stream().map(Literal::variable).collect(Collectors.toList());
    }
}
