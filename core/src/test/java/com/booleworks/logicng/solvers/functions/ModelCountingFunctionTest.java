// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.CallLimitComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NumberOfModelsHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.modelcounting.ModelCounter;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.EnumerationCollectorTestHandler;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.FixedVariableProvider;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.LeastCommonVariablesProvider;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.MostCommonVariablesProvider;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.SplitVariableProvider;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class ModelCountingFunctionTest extends TestWithFormulaContext {

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
    public void testTautology(final SplitVariableProvider splitProvider) {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider)
                                .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final BigInteger numberOfModels =
                solver.execute(ModelCountingFunction.builder(f.variables()).configuration(config).build());
        assertThat(numberOfModels).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testEmptyEnumerationVariables(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider)
                                .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final Formula formula = f.parse("A & (B | C)");
        solver.add(formula);
        final BigInteger numberOfModels =
                solver.execute(ModelCountingFunction.builder(f.variables()).configuration(config).build());
        assertThat(numberOfModels).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testSimple1(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider)
                                .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(f.parse("A & (B | C)"));
        final BigInteger numberOfModels =
                solver.execute(ModelCountingFunction.builder(f.variables("A", "B", "C")).configuration(config).build());
        assertThat(numberOfModels).isEqualTo(3);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testSimple2(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider)
                                .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(f.parse("(~A | C) & (~B | C)"));
        final BigInteger numberOfModels =
                solver.execute(ModelCountingFunction.builder(f.variables("A", "B", "C")).configuration(config).build());
        assertThat(numberOfModels).isEqualTo(5);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testMultipleModelEnumeration(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider)
                                .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final Formula formula = f.parse("(~A | C) & (~B | C)");
        solver.add(formula);
        final ModelCountingFunction meFunction =
                ModelCountingFunction.builder(f.variables("A", "B", "C")).configuration(config).build();
        final BigInteger firstRun = solver.execute(meFunction);
        final BigInteger secondRun = solver.execute(meFunction);
        assertThat(firstRun).isEqualTo(5);
        assertThat(secondRun).isEqualTo(5);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testDontCareVariables1(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider)
                                .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final Formula formula = f.parse("(~A | C) & (~B | C)");
        final SortedSet<Variable> variables = f.variables("A", "B", "C", "D");
        solver.add(formula);
        final BigInteger numberOfModels = solver.execute(ModelCountingFunction.builder(variables)
                .configuration(config)
                .build());
        assertThat(numberOfModels).isEqualTo(10);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testDontCareVariables2(final SplitVariableProvider splitProvider) throws ParserException {
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null
                                : DefaultModelEnumerationStrategy.builder().splitVariableProvider(splitProvider)
                                .maxNumberOfModels(2).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final Formula formula = f.parse("(~A | C) & (~B | C)");
        final SortedSet<Variable> variables = f.variables("A", "C", "D", "E");
        solver.add(formula);
        final BigInteger numberOfModels = solver.execute(ModelCountingFunction.builder(variables)
                .configuration(config)
                .build());
        assertThat(numberOfModels).isEqualTo(12);
    }

    @Test
    public void testDontCareVariables3() throws ParserException {
        final FixedVariableProvider splitProvider = new FixedVariableProvider(new TreeSet<>(f.variables("X")));
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder().strategy(DefaultModelEnumerationStrategy.builder()
                        .splitVariableProvider(splitProvider).maxNumberOfModels(3).build()).build();
        final SatSolver solver = SatSolver.newSolver(f);
        // X will be simplified out and become a don't care variable unknown by the solver
        final Formula formula = f.parse("A | B | (X & ~X)");
        solver.add(formula);
        final SortedSet<Variable> variables = new TreeSet<>(f.variables("A", "B", "X"));
        final BigInteger numberOfModels = solver.execute(ModelCountingFunction.builder(variables)
                .configuration(config)
                .build());
        assertThat(numberOfModels).isEqualTo(6);
    }

    @ParameterizedTest
    @MethodSource("splitProviders")
    public void testHandlerWithNumModelsLimit(final SplitVariableProvider splitProvider) throws ParserException {
        final NumberOfModelsHandler handler = new NumberOfModelsHandler(3);
        final ModelEnumerationConfig config =
                ModelEnumerationConfig.builder()
                        .strategy(splitProvider == null ? null : DefaultModelEnumerationStrategy.builder()
                                .splitVariableProvider(splitProvider).maxNumberOfModels(3).build())
                        .build();
        final SatSolver solver = SatSolver.newSolver(f);
        final Formula formula = f.parse("(~A | C) & (~B | C)");
        solver.add(formula);
        final LngResult<BigInteger> numberOfModels = solver.execute(
                ModelCountingFunction.builder(formula.variables(f)).configuration(config).build(), handler);
        assertThat(numberOfModels.isSuccess()).isFalse();
        assertThat(numberOfModels.isPartial()).isTrue();
        assertThat(numberOfModels.getPartialResult()).isEqualTo(3);
    }

    @Test
    @RandomTag
    public void testRandomFormulas() {
        for (int i = 1; i <= 100; i++) {
            final FormulaRandomizer randomizer =
                    new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(i).numVars(15).build());
            final Formula formula = randomizer.formula(3);

            final SatSolver solver = SatSolver.newSolver(f);
            solver.add(formula);

            // no split
            final var count = ModelCounter.count(f, List.of(formula), formula.variables(f));

            // recursive call: least common vars
            final ModelEnumerationConfig configLcv =
                    ModelEnumerationConfig.builder().strategy(DefaultModelEnumerationStrategy.builder()
                                    .splitVariableProvider(new LeastCommonVariablesProvider()).maxNumberOfModels(500).build())
                            .build();
            final BigInteger count1 = solver
                    .execute(ModelCountingFunction.builder(formula.variables(f)).configuration(configLcv).build());

            // recursive call: most common vars
            final ModelEnumerationConfig configMcv = ModelEnumerationConfig.builder()
                    .strategy(DefaultModelEnumerationStrategy.builder()
                            .splitVariableProvider(new MostCommonVariablesProvider()).maxNumberOfModels(500).build())
                    .build();
            final BigInteger count2 = solver
                    .execute(ModelCountingFunction.builder(formula.variables(f)).configuration(configMcv).build());

            assertThat(count1).isEqualTo(count);
            assertThat(count2).isEqualTo(count);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCollector(final FormulaContext _c) {
        final SatSolver solver = SatSolver.newSolver(_c.f);
        solver.add(_c.eq1);

        final EnumerationCollectorTestHandler handler = new EnumerationCollectorTestHandler();
        final ModelCountingFunction.ModelCountCollector collector = new ModelCountingFunction.ModelCountCollector(0);
        assertThat(collector.getResult().intValue()).isZero();
        assertThat(handler.getFoundModels()).isZero();
        assertThat(handler.getCommitCalls()).isZero();
        assertThat(handler.getRollbackCalls()).isZero();

        final LngBooleanVector modelFromSolver1 = new LngBooleanVector(true, true);
        final LngBooleanVector modelFromSolver2 = new LngBooleanVector(false, false);
        final LngIntVector relevantIndices = LngIntVector.of(0, 1);

        final Model expectedModel2 = new Model(_c.na, _c.nb);

        collector.addModel(modelFromSolver1, solver, relevantIndices, handler);
        assertThat(collector.getResult().intValue()).isZero();
        assertThat(handler.getFoundModels()).isEqualTo(1);
        assertThat(handler.getCommitCalls()).isZero();
        assertThat(handler.getRollbackCalls()).isZero();

        collector.commit(handler);
        assertThat(collector.getResult().intValue()).isEqualTo(1);
        assertThat(handler.getFoundModels()).isEqualTo(1);
        assertThat(handler.getCommitCalls()).isEqualTo(1);
        assertThat(handler.getRollbackCalls()).isZero();
        final BigInteger result1 = collector.getResult();

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
        assertThat(collector.getResult().intValue()).isEqualTo(2);
        assertThat(handler.getFoundModels()).isEqualTo(4);
        assertThat(handler.getCommitCalls()).isEqualTo(2);
        assertThat(handler.getRollbackCalls()).isEqualTo(2);
        final BigInteger result2 = collector.getResult();

        collector.rollback(handler);
        assertThat(collector.getResult()).isEqualTo(result2);
        assertThat(collector.rollbackAndReturnModels(solver, handler).getResult()).isEmpty();
        assertThat(handler.getFoundModels()).isEqualTo(4);
        assertThat(handler.getCommitCalls()).isEqualTo(2);
        assertThat(handler.getRollbackCalls()).isEqualTo(4);
    }

    @Test
    @LongRunningTag
    public void testComputationHandlerExitPoints() {
        final Formula formula = new PigeonHoleGenerator(f).generate(10).negate(f);
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(formula);
        for (int callLimit = 0; callLimit < 5000; callLimit++) {
            final ModelCountingFunction me = ModelCountingFunction.builder(formula.variables(f)).build();
            final LngResult<BigInteger> result = me.apply(solver, new CallLimitComputationHandler(callLimit));
            assertThat(result.isSuccess()).isFalse();
            assertThatThrownBy(result::getResult).isInstanceOf(IllegalStateException.class);
        }
    }
}
