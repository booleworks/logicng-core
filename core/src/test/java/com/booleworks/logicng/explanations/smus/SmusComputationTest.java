// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.smus;

import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.FIXED_END;
import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.RESTARTING_TIMEOUT;
import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.SINGLE_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.handlers.BoundedOptimizationHandler;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SmusComputationTest extends TestWithExampleFormulas {

    public static Collection<Object[]> configs() {
        final List<Object[]> configs = new ArrayList<>();
        configs.add(new Object[]{MaxSatConfig.CONFIG_INC_WBO, "INCWBO"});
        configs.add(new Object[]{MaxSatConfig.CONFIG_LINEAR_SU, "LINEAR_SU"});
        configs.add(new Object[]{MaxSatConfig.CONFIG_LINEAR_US, "LINEAR_US"});
        configs.add(new Object[]{MaxSatConfig.CONFIG_MSU3, "MSU3"});
        configs.add(new Object[]{MaxSatConfig.CONFIG_OLL, "OLL"});
        configs.add(new Object[]{MaxSatConfig.CONFIG_WBO, "WBO"});
        return configs;
    }


    @ParameterizedTest
    @MethodSource("configs")
    public void testFromPaper(final MaxSatConfig config) {
        final List<Proposition> input = Arrays.asList(
                new StandardProposition(parse(f, "~s")),
                new StandardProposition(parse(f, "s|~p")),
                new StandardProposition(parse(f, "p")),
                new StandardProposition(parse(f, "~p|m")),
                new StandardProposition(parse(f, "~m|n")),
                new StandardProposition(parse(f, "~n")),
                new StandardProposition(parse(f, "~m|l")),
                new StandardProposition(parse(f, "~l"))
        );
        final List<Proposition> result = Smus.builder(f).maxSatConfig(config).build().compute(input);
        assertThat(result).containsExactlyInAnyOrder(
                new StandardProposition(parse(f, "~s")),
                new StandardProposition(parse(f, "s|~p")),
                new StandardProposition(parse(f, "p")));
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testWithAdditionalConstraint(final MaxSatConfig config) {
        final List<Formula> input = Arrays.asList(
                parse(f, "~s"),
                parse(f, "s|~p"),
                parse(f, "p"),
                parse(f, "~p|m"),
                parse(f, "~m|n"),
                parse(f, "~n"),
                parse(f, "~m|l"),
                parse(f, "~l")
        );
        final List<Formula> result = Smus.builder(f)
                .additionalConstraints(List.of(parse(f, "n|l")))
                .maxSatConfig(config)
                .build()
                .computeForFormulas(input);
        assertThat(result).containsExactlyInAnyOrder(parse(f, "~n"), parse(f, "~l"));
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testSatisfiable(final MaxSatConfig config) {
        final List<Formula> input = Arrays.asList(
                parse(f, "~s"),
                parse(f, "s|~p"),
                parse(f, "~p|m"),
                parse(f, "~m|n"),
                parse(f, "~n"),
                parse(f, "~m|l")
        );
        assertThatThrownBy(() -> Smus.builder(f)
                .additionalConstraints(List.of(parse(f, "n|l")))
                .maxSatConfig(config)
                .build()
                .computeForFormulas(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testUnsatisfiableAdditionalConstraints(final MaxSatConfig config) {
        final List<Formula> input = Arrays.asList(
                parse(f, "~s"),
                parse(f, "s|~p"),
                parse(f, "~p|m"),
                parse(f, "~m|n"),
                parse(f, "~n|s")
        );
        final List<Formula> result = Smus.builder(f)
                .maxSatConfig(config)
                .additionalConstraints(List.of(parse(f, "~a&b"), parse(f, "a|~b")))
                .build()
                .computeForFormulas(input);
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testTrivialUnsatFormula(final MaxSatConfig config) {
        final List<Formula> input = Arrays.asList(
                parse(f, "~s"),
                parse(f, "s|~p"),
                parse(f, "p"),
                parse(f, "~p|m"),
                parse(f, "~m|n"),
                parse(f, "~n"),
                parse(f, "~m|l"),
                parse(f, "~l"),
                parse(f, "a&~a")
        );
        final List<Formula> result = Smus.builder(f)
                .additionalConstraints(List.of(parse(f, "n|l")))
                .maxSatConfig(config)
                .build()
                .computeForFormulas(input);
        assertThat(result).containsExactly(f.falsum());
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testUnsatFormula(final MaxSatConfig config) {
        final List<Formula> input = Arrays.asList(
                parse(f, "~s"),
                parse(f, "s|~p"),
                parse(f, "p"),
                parse(f, "~p|m"),
                parse(f, "~m|n"),
                parse(f, "~n"),
                parse(f, "~m|l"),
                parse(f, "~l"),
                parse(f, "(a<=>b)&(~a<=>b)")
        );
        final List<Formula> result = Smus.builder(f)
                .maxSatConfig(config)
                .additionalConstraints(List.of(parse(f, "n|l")))
                .build()
                .computeForFormulas(input);
        assertThat(result).containsExactly(parse(f, "(a<=>b)&(~a<=>b)"));
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testShorterConflict(final MaxSatConfig config) {
        final List<Formula> input = Arrays.asList(
                parse(f, "~s"),
                parse(f, "s|~p"),
                parse(f, "p"),
                parse(f, "p&~s"),
                parse(f, "~p|m"),
                parse(f, "~m|n"),
                parse(f, "~n"),
                parse(f, "~m|l"),
                parse(f, "~l")
        );
        final List<Formula> result = Smus.builder(f).maxSatConfig(config).build().computeForFormulas(input);
        assertThat(result).containsExactlyInAnyOrder(parse(f, "s|~p"), parse(f, "p&~s"));
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testCompleteConflict(final MaxSatConfig config) {
        final List<Formula> input = Arrays.asList(
                parse(f, "~s"),
                parse(f, "s|~p"),
                parse(f, "p|~m"),
                parse(f, "m|~n"),
                parse(f, "n|~l"),
                parse(f, "l|s")
        );
        final List<Formula> result = Smus.builder(f).maxSatConfig(config).build().computeForFormulas(input);
        assertThat(result).containsExactlyInAnyOrderElementsOf(input);
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testLongConflictWithShortcut(final MaxSatConfig config) {
        final List<Formula> input = Arrays.asList(
                parse(f, "~s"),
                parse(f, "s|~p"),
                parse(f, "p|~m"),
                parse(f, "m|~n"),
                parse(f, "n|~l"),
                parse(f, "l|s"),
                parse(f, "n|s")
        );
        final List<Formula> result = Smus.builder(f).maxSatConfig(config).build().computeForFormulas(input);
        assertThat(result).containsExactlyInAnyOrder(parse(f, "~s"),
                parse(f, "s|~p"),
                parse(f, "p|~m"),
                parse(f, "m|~n"),
                parse(f, "n|s"));
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testManyConflicts(final MaxSatConfig config) {
        final List<Formula> input = Arrays.asList(
                parse(f, "a"),
                parse(f, "~a|b"),
                parse(f, "~b|c"),
                parse(f, "~c|~a"),
                parse(f, "a1"),
                parse(f, "~a1|b1"),
                parse(f, "~b1|c1"),
                parse(f, "~c1|~a1"),
                parse(f, "a2"),
                parse(f, "~a2|b2"),
                parse(f, "~b2|c2"),
                parse(f, "~c2|~a2"),
                parse(f, "a3"),
                parse(f, "~a3|b3"),
                parse(f, "~b3|c3"),
                parse(f, "~c3|~a3"),
                parse(f, "a1|a2|a3|a4|b1|x|y"),
                parse(f, "x&~y"),
                parse(f, "x=>y")
        );
        final List<Formula> result = Smus.builder(f).maxSatConfig(config).build().computeForFormulas(input);
        assertThat(result).containsExactlyInAnyOrder(parse(f, "x&~y"), parse(f, "x=>y"));
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testTimeoutHandlerSmall(final MaxSatConfig config) {
        final List<TimeoutHandler> handlers = Arrays.asList(
                new TimeoutHandler(5_000L, SINGLE_TIMEOUT),
                new TimeoutHandler(5_000L, RESTARTING_TIMEOUT),
                new TimeoutHandler(System.currentTimeMillis() + 5_000L, FIXED_END)
        );
        final List<Formula> formulas = Arrays.asList(
                parse(f, "a"),
                parse(f, "~a")
        );
        for (final TimeoutHandler handler : handlers) {
            testHandler(handler, formulas, config, false);
        }
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testTimeoutHandlerLarge(final MaxSatConfig config) throws ParserException, IOException {
        final List<TimeoutHandler> handlers = Arrays.asList(
                new TimeoutHandler(1L, SINGLE_TIMEOUT),
                new TimeoutHandler(1L, RESTARTING_TIMEOUT),
                new TimeoutHandler(System.currentTimeMillis() + 1L, FIXED_END)
        );
        final Formula formula = new PigeonHoleGenerator(f).generate(15);
        final List<Formula> formulas = formula.stream().collect(Collectors.toList());
        for (final TimeoutHandler handler : handlers) {
            testHandler(handler, formulas, config, true);
        }
    }

    @Test
    @LongRunningTag
    public void testCancellationPoints() throws IOException {
        final List<Formula> formulas = DimacsReader.readCNF(f, "../test_files/sat/unsat/bf0432-007.cnf");
        for (int numOptimizationStarts = 1; numOptimizationStarts < 5; numOptimizationStarts++) {
            for (int numSatHandlerStarts = 1; numSatHandlerStarts < 10; numSatHandlerStarts++) {
                final ComputationHandler handler =
                        new BoundedOptimizationHandler(numSatHandlerStarts, numOptimizationStarts);
                testHandler(handler, formulas, MaxSatConfig.CONFIG_OLL, true);
            }
        }
    }

    @Test
    public void testMinimumHittingSetCancelled() {
        final ComputationHandler handler = new BoundedOptimizationHandler(-1, 0);
        final List<Formula> formulas = Arrays.asList(
                parse(f, "a"),
                parse(f, "~a")
        );
        testHandler(handler, formulas, MaxSatConfig.CONFIG_OLL, true);
    }

    @Test
    public void testHSolverCancelled() {
        final ComputationHandler handler = new BoundedOptimizationHandler(-1, 3);
        final List<Formula> formulas = Arrays.asList(
                parse(f, "a"),
                parse(f, "~a"),
                parse(f, "c")
        );
        testHandler(handler, formulas, MaxSatConfig.CONFIG_OLL, true);
    }

    private void testHandler(final ComputationHandler handler, final List<Formula> formulas,
                             final MaxSatConfig config, final boolean expCanceled) {
        final LngResult<List<Formula>> result = Smus.builder(f)
                .maxSatConfig(config)
                .build()
                .computeForFormulas(formulas, handler);
        assertThat(!result.isSuccess()).isEqualTo(expCanceled);
    }
}
