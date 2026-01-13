// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import static com.booleworks.logicng.TestWithExampleFormulas.parse;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_INC_WBO;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_LINEAR_SU;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_OLL;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_WBO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.FormulaCornerCases;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.handlers.BoundedOptimizationHandler;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class AdvancedSimplifierTest extends TestWithFormulaContext {

    public static Collection<Object[]> configs() {
        final List<Object[]> configs = new ArrayList<>();
        configs.add(new Object[]{CONFIG_INC_WBO, "INCWBO"});
        configs.add(new Object[]{CONFIG_LINEAR_SU, "LINEAR_SU"});
        configs.add(new Object[]{CONFIG_OLL, "OLL"});
        configs.add(new Object[]{CONFIG_WBO, "WBO"});
        return configs;
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testConstants(final MaxSatConfig config) {
        final var f = FormulaFactory.caching();
        final var cfg = AdvancedSimplifierConfig.builder().maxSatConfig(config).build();
        final AdvancedSimplifier simplifier = new AdvancedSimplifier(f, cfg);

        assertThat(f.falsum().transform(simplifier)).isEqualTo(f.falsum());
        assertThat(f.verum().transform(simplifier)).isEqualTo(f.verum());
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testCornerCases(final MaxSatConfig config) {
        final var fc = FormulaFactory.caching();
        final var fn = FormulaFactory.nonCaching();
        final var cfg = AdvancedSimplifierConfig.builder().maxSatConfig(config).build();
        FormulaCornerCases cornerCases = new FormulaCornerCases(fc);
        cornerCases.cornerCases().forEach(it -> computeAndVerify(it, cfg));
        cornerCases = new FormulaCornerCases(fn);
        cornerCases.cornerCases().forEach(it -> computeAndVerify(it, cfg));
    }

    @ParameterizedTest
    @MethodSource("configs")
    @RandomTag
    public void testRandomized(final MaxSatConfig config) {
        final var f = FormulaFactory.caching();
        final var cfg = AdvancedSimplifierConfig.builder().maxSatConfig(config).build();
        for (int i = 0; i < 100; i++) {
            final FormulaRandomizer randomizer = new FormulaRandomizer(f,
                    FormulaRandomizerConfig.builder().numVars(8).weightPbc(2).seed(i * 42).build());
            final Formula formula = randomizer.formula(5);
            computeAndVerify(formula, cfg);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testTimeoutHandlerSmall(final FormulaContext _c) throws ParserException {
        final List<TimeoutHandler> handlers = Arrays.asList(
                new TimeoutHandler(5_000L, TimeoutHandler.TimerType.SINGLE_TIMEOUT),
                new TimeoutHandler(5_000L, TimeoutHandler.TimerType.RESTARTING_TIMEOUT),
                new TimeoutHandler(System.currentTimeMillis() + 5_000L, TimeoutHandler.TimerType.FIXED_END)
        );
        final Formula formula = _c.p.parse("a & b | ~c & a");
        for (final TimeoutHandler handler : handlers) {
            testHandler(handler, formula, false);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testTimeoutHandlerLarge(final FormulaContext _c) throws ParserException, IOException {
        final List<TimeoutHandler> handlers = Arrays.asList(
                new TimeoutHandler(1L, TimeoutHandler.TimerType.SINGLE_TIMEOUT),
                new TimeoutHandler(1L, TimeoutHandler.TimerType.RESTARTING_TIMEOUT),
                new TimeoutHandler(System.currentTimeMillis() + 1L, TimeoutHandler.TimerType.FIXED_END)
        );
        final Formula formula =
                FormulaReader.readFormula(_c.f, "../test_files/formulas/large_formula.txt");
        for (final TimeoutHandler handler : handlers) {
            testHandler(handler, formula, true);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPrimeCompilerIsCancelled(final FormulaContext _c) throws ParserException {
        final ComputationHandler handler = new BoundedOptimizationHandler(-1, 0);
        final Formula formula = _c.p.parse("a&(b|c)");
        testHandler(handler, formula, true);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSmusComputationIsCancelled(final FormulaContext _c) throws ParserException {
        final ComputationHandler handler = new BoundedOptimizationHandler(-1, 5);
        final Formula formula = _c.p.parse("a&(b|c)");
        testHandler(handler, formula, true);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @LongRunningTag
    public void testAdvancedSimplifierConfig(final FormulaContext _c) {
        final List<AdvancedSimplifierConfig> configs = Arrays.asList(
                AdvancedSimplifierConfig.builder().build(),
                AdvancedSimplifierConfig.builder().restrictBackbone(false).factorOut(false).simplifyNegations(false)
                        .build(),
                AdvancedSimplifierConfig.builder().factorOut(false).simplifyNegations(false).build(),
                AdvancedSimplifierConfig.builder().restrictBackbone(false).simplifyNegations(false).build(),
                AdvancedSimplifierConfig.builder().restrictBackbone(false).factorOut(false).build(),
                AdvancedSimplifierConfig.builder().restrictBackbone(false).build(),
                AdvancedSimplifierConfig.builder().factorOut(false).build(),
                AdvancedSimplifierConfig.builder().simplifyNegations(false).build());

        for (final AdvancedSimplifierConfig config : configs) {
            final AdvancedSimplifier advancedSimplifier = new AdvancedSimplifier(_c.f, config);
            for (int i = 1; i < 10; i++) {
                final FormulaRandomizer randomizer =
                        new FormulaRandomizer(_c.f, FormulaRandomizerConfig.builder().seed(i).build());
                final Formula formula = randomizer.formula(3);
                final Formula simplified = formula.transform(advancedSimplifier);
                if (simplified != null) {
                    assertThat(_c.f.equivalence(formula, simplified).holds(new TautologyPredicate(_c.f))).isTrue();
                    assertThat(formula.toString().length()).isGreaterThanOrEqualTo(simplified.toString().length());
                }
            }
        }
    }

    @LongRunningTag
    @Test
    public void testCancellationPoints() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula =
                parse(f, "~v16 & ~v22 & ~v12 & (~v4 | ~v14) & (~v4 | ~v15) & (v3 | v4) & (v3 | ~v14) & (v3 | ~v15) " +
                        "& (~v20 | ~v8) & (v9 | ~v20) & (~v21 | ~v8) & (v9 | ~v21) & (~v21 | ~v10) & (~v21 | ~v11) & "
                        + "v19");
        for (int numOptimizationStarts = 1; numOptimizationStarts < 30; numOptimizationStarts++) {
            for (int numSatHandlerStarts = 1; numSatHandlerStarts < 500; numSatHandlerStarts++) {
                final ComputationHandler handler =
                        new BoundedOptimizationHandler(numSatHandlerStarts, numOptimizationStarts);
                testHandler(handler, formula, true);
            }
        }
    }

    private void computeAndVerify(final Formula formula, final AdvancedSimplifierConfig config) {
        final Formula simplified = formula.transform(new AdvancedSimplifier(formula.getFactory(), config));
        assertThat(formula.getFactory().equivalence(formula, simplified).holds(new TautologyPredicate(formula.getFactory())))
                .as("Minimized formula is equivalent to original Formula")
                .isTrue();
    }

    private void testHandler(final ComputationHandler handler, final Formula formula, final boolean expCanceled) {
        final AdvancedSimplifier simplifierWithHandler =
                new AdvancedSimplifier(formula.getFactory(), AdvancedSimplifierConfig.builder().build());
        final LngResult<Formula> simplified = formula.transform(simplifierWithHandler, handler);
        assertThat(simplified.isSuccess()).isEqualTo(!expCanceled);
        if (expCanceled) {
            assertThatThrownBy(simplified::getResult).isInstanceOf(IllegalStateException.class);
        } else {
            assertThat(simplified.getResult()).isNotNull();
        }
    }
}
