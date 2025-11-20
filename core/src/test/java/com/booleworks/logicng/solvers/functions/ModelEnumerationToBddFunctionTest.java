// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.TestWithExampleFormulas.parse;
import static java.util.Collections.emptySortedSet;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NumberOfModelsHandler;
import com.booleworks.logicng.knowledgecompilation.bdds.Bdd;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.ModelEnumerationToBddFunction.BddModelEnumerationCollector;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.EnumerationCollectorTestHandler;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.FixedVariableProvider;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.LeastCommonVariablesProvider;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.MostCommonVariablesProvider;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.SplitVariableProvider;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class ModelEnumerationToBddFunctionTest extends TestWithFormulaContext {

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
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider)
                                .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(f.literal("A", true));
        solver.add(f.literal("A", false));
        final Bdd bdd = solver.execute(ModelEnumerationToBddFunction.builder(List.of()).configuration(config).build());
        assertThat(bdd.isContradiction()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testTautology(final SplitVariableProvider splitProvider) {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider)
                                .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final Bdd bdd = solver.execute(ModelEnumerationToBddFunction.builder(List.of()).configuration(config).build());
        assertThat(bdd.isTautology()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testEmptyEnumerationVariables(final SplitVariableProvider splitProvider) {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                                        : DefaultModelEnumerationStrategy.builder()
                                          .splitVariableProvider(splitProvider)
                                          .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final Formula formula = parse(f, "A & (B | C)");
        solver.add(formula);
        final Bdd bdd = solver.execute(ModelEnumerationToBddFunction.builder(List.of()).configuration(config).build());
        assertThat(bdd.isTautology()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testSimple1(final SplitVariableProvider splitProvider) {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                                        : DefaultModelEnumerationStrategy.builder()
                                          .splitVariableProvider(splitProvider)
                                          .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final Formula formula = parse(f, "A & (B | C)");
        solver.add(formula);
        final Bdd bdd = solver
                .execute(ModelEnumerationToBddFunction.builder(formula.variables(f)).configuration(config).build());
        compareModels(formula, formula.variables(f), bdd);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testSimple2(final SplitVariableProvider splitProvider) {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                                        : DefaultModelEnumerationStrategy.builder()
                                          .splitVariableProvider(splitProvider)
                                          .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final Formula formula = parse(f, "(~A | C) & (~B | C)");
        solver.add(formula);
        final Bdd bdd = solver
                .execute(ModelEnumerationToBddFunction.builder(formula.variables(f)).configuration(config).build());
        assertThat(bdd.modelCount()).isEqualTo(5);
        compareModels(formula, formula.variables(f), bdd);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testMultipleModelEnumeration(final SplitVariableProvider splitProvider) {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                                        : DefaultModelEnumerationStrategy.builder()
                                          .splitVariableProvider(splitProvider)
                                          .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final Formula formula = parse(f, "(~A | C) & (~B | C)");
        solver.add(formula);
        final ModelEnumerationToBddFunction meFunction =
                ModelEnumerationToBddFunction.builder(formula.variables(f)).configuration(config).build();
        final Bdd firstRun = solver.execute(meFunction);
        final Bdd secondRun = solver.execute(meFunction);
        assertThat(firstRun.modelCount()).isEqualTo(5);
        assertThat(secondRun.modelCount()).isEqualTo(5);
        compareModels(formula, formula.variables(f), firstRun);
        compareModels(formula, formula.variables(f), secondRun);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testDontCareVariables1(final SplitVariableProvider splitProvider) {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                                        : DefaultModelEnumerationStrategy.builder()
                                          .splitVariableProvider(splitProvider)
                                          .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final Formula formula = parse(f, "(~A | C) & (~B | C)");
        final SortedSet<Variable> variables = f.variables("A", "B", "C", "D");
        solver.add(formula);
        final Bdd bdd = solver.execute(ModelEnumerationToBddFunction.builder(variables)
                .configuration(config)
                .build());
        assertThat(bdd.modelCount()).isEqualTo(10);
        compareModels(formula, variables, bdd);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testDontCareVariables2(final SplitVariableProvider splitProvider) {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                                        : DefaultModelEnumerationStrategy.builder()
                                          .splitVariableProvider(splitProvider)
                                          .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final Formula formula = parse(f, "(~A | C) & (~B | C)");
        final SortedSet<Variable> variables = f.variables("A", "C", "D", "E");
        solver.add(formula);
        final Bdd bdd = solver.execute(ModelEnumerationToBddFunction.builder(variables)
                .configuration(config)
                .build());
        assertThat(bdd.modelCount()).isEqualTo(12);
        compareModels(formula, variables, bdd);
    }

    @Test
    public void testDontCareVariables3() {
        final FixedVariableProvider splitProvider = new FixedVariableProvider(new TreeSet<>(f.variables("X")));
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(DefaultModelEnumerationStrategy.builder()
                        .splitVariableProvider(splitProvider).maxNumberOfModels(3).build()).build();
        final SatSolver solver = SatSolver.newSolver(f);
        // X will be simplified out and become a don't care variable unknown
        // by the solver
        final Formula formula = parse(f, "A | B | (X & ~X)");
        solver.add(formula);
        final SortedSet<Variable> variables = new TreeSet<>(f.variables("A", "B", "X"));
        final Bdd bdd = solver.execute(ModelEnumerationToBddFunction.builder(variables)
                .configuration(config)
                .build());
        assertThat(bdd.modelCount()).isEqualTo(6);
        compareModels(formula, variables, bdd);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testHandlerWithNumModelsLimit(final SplitVariableProvider splitProvider) {
        final NumberOfModelsHandler handler = new NumberOfModelsHandler(3);
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder()
                                .splitVariableProvider(splitProvider).maxNumberOfModels(3).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(parse(f, "(~A | C) & (~B | C)"));
        final LngResult<Bdd> bdd = solver.execute(
                ModelEnumerationToBddFunction.builder(f.variables("A", "B", "C")).configuration(config).build(),
                handler);
        assertThat(bdd.isSuccess()).isFalse();
        assertThat(bdd.isPartial()).isTrue();
        assertThat(bdd.getPartialResult().modelCount()).isEqualTo(3);
    }

    @RandomTag
    @Test
    public void testRandomFormulas() {
        for (int i = 1; i <= 50; i++) {
            final FormulaRandomizer randomizer =
                    new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(i).numVars(15).build());
            final Formula formula = randomizer.formula(3);

            final SatSolver solver = SatSolver.newSolver(f);
            solver.add(formula);

            // recursive call: least common vars
            final ModelEnumerationConfig configLcv =
                    ModelEnumerationConfig.builder().strategy(DefaultModelEnumerationStrategy.builder()
                                    .splitVariableProvider(new LeastCommonVariablesProvider()).maxNumberOfModels(500).build())
                            .build();
            final Bdd bdd1 = solver.execute(
                    ModelEnumerationToBddFunction.builder(formula.variables(f)).configuration(configLcv).build());

            // recursive call: most common vars
            final ModelEnumerationConfig configMcv =
                    ModelEnumerationConfig.builder().strategy(DefaultModelEnumerationStrategy.builder()
                                    .splitVariableProvider(new MostCommonVariablesProvider()).maxNumberOfModels(500).build())
                            .build();
            final Bdd bdd2 = solver.execute(
                    ModelEnumerationToBddFunction.builder(formula.variables(f)).configuration(configMcv).build());

            compareModels(formula, formula.variables(f), bdd1);
            compareModels(formula, formula.variables(f), bdd2);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCollector(final FormulaContext _c) {
        final SatSolver solver = SatSolver.newSolver(_c.f);
        solver.add(_c.eq1);

        final EnumerationCollectorTestHandler handler = new EnumerationCollectorTestHandler();
        final BddModelEnumerationCollector collector =
                new BddModelEnumerationCollector(_c.f, _c.eq1.variables(_c.f), emptySortedSet(), 0);
        assertThat(collector.getResult().modelCount()).isZero();
        assertThat(handler.getFoundModels()).isZero();
        assertThat(handler.getCommitCalls()).isZero();
        assertThat(handler.getRollbackCalls()).isZero();

        final LngBooleanVector modelFromSolver1 = new LngBooleanVector(true, true);
        final LngBooleanVector modelFromSolver2 = new LngBooleanVector(false, false);
        final LngIntVector relevantIndices = LngIntVector.of(0, 1);

        final Model expectedModel1 = new Model(_c.a, _c.b);
        final Model expectedModel2 = new Model(_c.na, _c.nb);

        collector.addModel(modelFromSolver1, solver, relevantIndices, handler);
        assertThat(collector.getResult().enumerateAllModels()).isEmpty();
        assertThat(handler.getFoundModels()).isEqualTo(1);
        assertThat(handler.getCommitCalls()).isZero();
        assertThat(handler.getRollbackCalls()).isZero();

        collector.commit(handler);
        assertThat(collector.getResult().enumerateAllModels()).containsExactly(expectedModel1);
        assertThat(handler.getFoundModels()).isEqualTo(1);
        assertThat(handler.getCommitCalls()).isEqualTo(1);
        assertThat(handler.getRollbackCalls()).isZero();
        final Bdd result1 = collector.getResult();

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
        final List<Model> rollbackModels = collector.rollbackAndReturnModels(solver, handler).getResult();
        assertThat(rollbackModels).containsExactly(expectedModel2);
        assertThat(collector.getResult()).isEqualTo(result1);
        assertThat(handler.getFoundModels()).isEqualTo(3);
        assertThat(handler.getCommitCalls()).isEqualTo(1);
        assertThat(handler.getRollbackCalls()).isEqualTo(2);

        collector.addModel(modelFromSolver2, solver, relevantIndices, handler);
        collector.commit(handler);
        assertThat(collector.getResult().enumerateAllModels()).containsExactlyInAnyOrder(expectedModel1,
                expectedModel2);
        assertThat(handler.getFoundModels()).isEqualTo(4);
        assertThat(handler.getCommitCalls()).isEqualTo(2);
        assertThat(handler.getRollbackCalls()).isEqualTo(2);
        final Bdd result2 = collector.getResult();

        collector.rollback(handler);
        assertThat(collector.getResult()).isEqualTo(result2);
        assertThat(collector.rollbackAndReturnModels(solver, handler).getResult()).isEmpty();
        assertThat(handler.getFoundModels()).isEqualTo(4);
        assertThat(handler.getCommitCalls()).isEqualTo(2);
        assertThat(handler.getRollbackCalls()).isEqualTo(4);
    }

    private void compareModels(final Formula formula, final Collection<Variable> variables, final Bdd bdd) {
        final FormulaFactory factory = formula.getFactory();
        final SatSolver solver = SatSolver.newSolver(factory);
        solver.add(formula);
        final Variable taut = factory.variable("@TAUT");
        for (final Variable variable : variables) {
            solver.add(factory.or(taut.negate(formula.getFactory()), variable));
        }
        solver.add(taut.negate(formula.getFactory()));
        final List<Model> formulaModels = solver.enumerateAllModels(variables);
        final List<Model> bddModels = bdd.enumerateAllModels(variables);
        assertThat(formulaModels).containsExactlyInAnyOrderElementsOf(bddModels);
    }
}
