// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.handlers.BoundedOptimizationHandler;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import com.booleworks.logicng.util.FormulaCornerCases;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AdvancedSimplifierTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final AdvancedSimplifier simplifier = new AdvancedSimplifier(_c.f);

        assertThat(_c.f.falsum().transform(simplifier)).isEqualTo(_c.f.falsum());
        assertThat(_c.f.verum().transform(simplifier)).isEqualTo(_c.f.verum());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCornerCases(final FormulaContext _c) {
        final FormulaCornerCases cornerCases = new FormulaCornerCases(_c.f);
        cornerCases.cornerCases().forEach(this::computeAndVerify);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void testRandomized(final FormulaContext _c) {
        for (int i = 0; i < 100; i++) {
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f,
                    FormulaRandomizerConfig.builder().numVars(8).weightPbc(2).seed(i * 42).build());
            final Formula formula = randomizer.formula(5);
            computeAndVerify(formula);
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
        final Formula formula = _c.f.parse("a & b | ~c & a");
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
                FormulaReader.readFormula(_c.f, "src/test/resources/formulas/large_formula.txt");
        for (final TimeoutHandler handler : handlers) {
            testHandler(handler, formula, true);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPrimeCompilerIsCancelled(final FormulaContext _c) throws ParserException {
        final ComputationHandler handler = new BoundedOptimizationHandler(-1, 0);
        final Formula formula = _c.f.parse("a&(b|c)");
        testHandler(handler, formula, true);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSmusComputationIsCancelled(final FormulaContext _c) throws ParserException {
        final ComputationHandler handler = new BoundedOptimizationHandler(-1, 5);
        final Formula formula = _c.f.parse("a&(b|c)");
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
                f.parse("~v16 & ~v22 & ~v12 & (~v4 | ~v14) & (~v4 | ~v15) & (v3 | v4) & (v3 | ~v14) & (v3 | ~v15) " +
                        "& (~v20 | ~v8) & (v9 | ~v20) & (~v21 | ~v8) & (v9 | ~v21) & (~v21 | ~v10) & (~v21 | ~v11) & v19");
        for (int numOptimizationStarts = 1; numOptimizationStarts < 30; numOptimizationStarts++) {
            for (int numSatHandlerStarts = 1; numSatHandlerStarts < 500; numSatHandlerStarts++) {
                final ComputationHandler handler =
                        new BoundedOptimizationHandler(numSatHandlerStarts, numOptimizationStarts);
                testHandler(handler, formula, true);
            }
        }
    }

    private void computeAndVerify(final Formula formula) {
        final Formula simplified = formula.transform(new AdvancedSimplifier(formula.factory()));
        assertThat(formula.factory().equivalence(formula, simplified).holds(new TautologyPredicate(formula.factory())))
                .as("Minimized formula is equivalent to original Formula")
                .isTrue();
    }

    private void testHandler(final ComputationHandler handler, final Formula formula, final boolean expCanceled) {
        final AdvancedSimplifier simplifierWithHandler =
                new AdvancedSimplifier(formula.factory(), AdvancedSimplifierConfig.builder().build());
        final LNGResult<Formula> simplified = formula.transform(simplifierWithHandler, handler);
        assertThat(simplified.isSuccess()).isEqualTo(!expCanceled);
        if (expCanceled) {
            assertThatThrownBy(simplified::getResult).isInstanceOf(IllegalStateException.class);
        } else {
            assertThat(simplified.getResult()).isNotNull();
        }
    }
}
