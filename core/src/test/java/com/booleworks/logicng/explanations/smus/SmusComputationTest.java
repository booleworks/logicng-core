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
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
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
    public void testFromPaper(final MaxSatConfig config) throws ParserException {
        final List<Proposition> input = Arrays.asList(
                new StandardProposition(f.parse("~s")),
                new StandardProposition(f.parse("s|~p")),
                new StandardProposition(f.parse("p")),
                new StandardProposition(f.parse("~p|m")),
                new StandardProposition(f.parse("~m|n")),
                new StandardProposition(f.parse("~n")),
                new StandardProposition(f.parse("~m|l")),
                new StandardProposition(f.parse("~l"))
        );
        final List<Proposition> result = Smus.builder(f).maxSatConfig(config).build().compute(input);
        assertThat(result).containsExactlyInAnyOrder(
                new StandardProposition(f.parse("~s")),
                new StandardProposition(f.parse("s|~p")),
                new StandardProposition(f.parse("p")));
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testWithAdditionalConstraint(final MaxSatConfig config) throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("~s"),
                f.parse("s|~p"),
                f.parse("p"),
                f.parse("~p|m"),
                f.parse("~m|n"),
                f.parse("~n"),
                f.parse("~m|l"),
                f.parse("~l")
        );
        final List<Formula> result = Smus.builder(f)
                .additionalConstraints(List.of(f.parse("n|l")))
                .maxSatConfig(config)
                .build()
                .computeForFormulas(input);
        assertThat(result).containsExactlyInAnyOrder(f.parse("~n"), f.parse("~l"));
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testSatisfiable(final MaxSatConfig config) throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("~s"),
                f.parse("s|~p"),
                f.parse("~p|m"),
                f.parse("~m|n"),
                f.parse("~n"),
                f.parse("~m|l")
        );
        assertThatThrownBy(() -> Smus.builder(f)
                .additionalConstraints(List.of(f.parse("n|l")))
                .maxSatConfig(config)
                .build()
                .computeForFormulas(input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testUnsatisfiableAdditionalConstraints(final MaxSatConfig config) throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("~s"),
                f.parse("s|~p"),
                f.parse("~p|m"),
                f.parse("~m|n"),
                f.parse("~n|s")
        );
        final List<Formula> result = Smus.builder(f)
                .maxSatConfig(config)
                .additionalConstraints(List.of(f.parse("~a&b"), f.parse("a|~b")))
                .build()
                .computeForFormulas(input);
        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testTrivialUnsatFormula(final MaxSatConfig config) throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("~s"),
                f.parse("s|~p"),
                f.parse("p"),
                f.parse("~p|m"),
                f.parse("~m|n"),
                f.parse("~n"),
                f.parse("~m|l"),
                f.parse("~l"),
                f.parse("a&~a")
        );
        final List<Formula> result = Smus.builder(f)
                .additionalConstraints(List.of(f.parse("n|l")))
                .maxSatConfig(config)
                .build()
                .computeForFormulas(input);
        assertThat(result).containsExactly(f.falsum());
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testUnsatFormula(final MaxSatConfig config) throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("~s"),
                f.parse("s|~p"),
                f.parse("p"),
                f.parse("~p|m"),
                f.parse("~m|n"),
                f.parse("~n"),
                f.parse("~m|l"),
                f.parse("~l"),
                f.parse("(a<=>b)&(~a<=>b)")
        );
        final List<Formula> result = Smus.builder(f)
                .maxSatConfig(config)
                .additionalConstraints(List.of(f.parse("n|l")))
                .build()
                .computeForFormulas(input);
        assertThat(result).containsExactly(f.parse("(a<=>b)&(~a<=>b)"));
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testShorterConflict(final MaxSatConfig config) throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("~s"),
                f.parse("s|~p"),
                f.parse("p"),
                f.parse("p&~s"),
                f.parse("~p|m"),
                f.parse("~m|n"),
                f.parse("~n"),
                f.parse("~m|l"),
                f.parse("~l")
        );
        final List<Formula> result = Smus.builder(f).maxSatConfig(config).build().computeForFormulas(input);
        assertThat(result).containsExactlyInAnyOrder(f.parse("s|~p"), f.parse("p&~s"));
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testCompleteConflict(final MaxSatConfig config) throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("~s"),
                f.parse("s|~p"),
                f.parse("p|~m"),
                f.parse("m|~n"),
                f.parse("n|~l"),
                f.parse("l|s")
        );
        final List<Formula> result = Smus.builder(f).maxSatConfig(config).build().computeForFormulas(input);
        assertThat(result).containsExactlyInAnyOrderElementsOf(input);
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testLongConflictWithShortcut(final MaxSatConfig config) throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("~s"),
                f.parse("s|~p"),
                f.parse("p|~m"),
                f.parse("m|~n"),
                f.parse("n|~l"),
                f.parse("l|s"),
                f.parse("n|s")
        );
        final List<Formula> result = Smus.builder(f).maxSatConfig(config).build().computeForFormulas(input);
        assertThat(result).containsExactlyInAnyOrder(f.parse("~s"),
                f.parse("s|~p"),
                f.parse("p|~m"),
                f.parse("m|~n"),
                f.parse("n|s"));
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testManyConflicts(final MaxSatConfig config) throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("a"),
                f.parse("~a|b"),
                f.parse("~b|c"),
                f.parse("~c|~a"),
                f.parse("a1"),
                f.parse("~a1|b1"),
                f.parse("~b1|c1"),
                f.parse("~c1|~a1"),
                f.parse("a2"),
                f.parse("~a2|b2"),
                f.parse("~b2|c2"),
                f.parse("~c2|~a2"),
                f.parse("a3"),
                f.parse("~a3|b3"),
                f.parse("~b3|c3"),
                f.parse("~c3|~a3"),
                f.parse("a1|a2|a3|a4|b1|x|y"),
                f.parse("x&~y"),
                f.parse("x=>y")
        );
        final List<Formula> result = Smus.builder(f).maxSatConfig(config).build().computeForFormulas(input);
        assertThat(result).containsExactlyInAnyOrder(f.parse("x&~y"), f.parse("x=>y"));
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testTimeoutHandlerSmall(final MaxSatConfig config) throws ParserException {
        final List<TimeoutHandler> handlers = Arrays.asList(
                new TimeoutHandler(5_000L, SINGLE_TIMEOUT),
                new TimeoutHandler(5_000L, RESTARTING_TIMEOUT),
                new TimeoutHandler(System.currentTimeMillis() + 5_000L, FIXED_END)
        );
        final List<Formula> formulas = Arrays.asList(
                f.parse("a"),
                f.parse("~a")
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
        final Formula formula = FormulaReader.readFormula(f, "../test_files/formulas/large_formula.txt");
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
    public void testMinimumHittingSetCancelled() throws ParserException {
        final ComputationHandler handler = new BoundedOptimizationHandler(-1, 0);
        final List<Formula> formulas = Arrays.asList(
                f.parse("a"),
                f.parse("~a")
        );
        testHandler(handler, formulas, MaxSatConfig.CONFIG_OLL, true);
    }

    @Test
    public void testHSolverCancelled() throws ParserException {
        final ComputationHandler handler = new BoundedOptimizationHandler(-1, 3);
        final List<Formula> formulas = Arrays.asList(
                f.parse("a"),
                f.parse("~a"),
                f.parse("c")
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
