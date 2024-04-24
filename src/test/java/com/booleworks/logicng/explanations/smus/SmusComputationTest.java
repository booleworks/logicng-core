// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.smus;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.handlers.BoundedOptimizationHandler;
import com.booleworks.logicng.handlers.OptimizationHandler;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.handlers.TimeoutOptimizationHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.io.readers.FormulaReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SmusComputationTest extends TestWithExampleFormulas {

    @Test
    public void testFromPaper() throws ParserException {
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
        final List<Formula> result = SmusComputation.computeSmusForFormulas(f, input, Collections.emptyList());
        assertThat(result).containsExactlyInAnyOrder(f.parse("~s"), f.parse("s|~p"), f.parse("p"));
    }

    @Test
    public void testWithAdditionalConstraint() throws ParserException {
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
        final List<Formula> result =
                SmusComputation.computeSmusForFormulas(f, input, Collections.singletonList(f.parse("n|l")));
        assertThat(result).containsExactlyInAnyOrder(f.parse("~n"), f.parse("~l"));
    }

    @Test
    public void testSatisfiable() throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("~s"),
                f.parse("s|~p"),
                f.parse("~p|m"),
                f.parse("~m|n"),
                f.parse("~n"),
                f.parse("~m|l")
        );
        final List<Formula> result =
                SmusComputation.computeSmusForFormulas(f, input, Collections.singletonList(f.parse("n|l")));
        assertThat(result).isNull();
    }

    @Test
    public void testUnsatisfiableAdditionalConstraints() throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("~s"),
                f.parse("s|~p"),
                f.parse("~p|m"),
                f.parse("~m|n"),
                f.parse("~n|s")
        );
        final List<Formula> result =
                SmusComputation.computeSmusForFormulas(f, input, Arrays.asList(f.parse("~a&b"), f.parse("a|~b")));
        assertThat(result).isEmpty();
    }

    @Test
    public void testTrivialUnsatFormula() throws ParserException {
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
        final List<Formula> result =
                SmusComputation.computeSmusForFormulas(f, input, Collections.singletonList(f.parse("n|l")));
        assertThat(result).containsExactly(f.falsum());
    }

    @Test
    public void testUnsatFormula() throws ParserException {
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
        final List<Formula> result =
                SmusComputation.computeSmusForFormulas(f, input, Collections.singletonList(f.parse("n|l")));
        assertThat(result).containsExactly(f.parse("(a<=>b)&(~a<=>b)"));
    }

    @Test
    public void testShorterConflict() throws ParserException {
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
        final List<Formula> result = SmusComputation.computeSmusForFormulas(f, input, Collections.emptyList());
        assertThat(result).containsExactlyInAnyOrder(f.parse("s|~p"), f.parse("p&~s"));
    }

    @Test
    public void testCompleteConflict() throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("~s"),
                f.parse("s|~p"),
                f.parse("p|~m"),
                f.parse("m|~n"),
                f.parse("n|~l"),
                f.parse("l|s")
        );
        final List<Formula> result = SmusComputation.computeSmusForFormulas(f, input, Collections.emptyList());
        assertThat(result).containsExactlyInAnyOrderElementsOf(input);
    }

    @Test
    public void testLongConflictWithShortcut() throws ParserException {
        final List<Formula> input = Arrays.asList(
                f.parse("~s"),
                f.parse("s|~p"),
                f.parse("p|~m"),
                f.parse("m|~n"),
                f.parse("n|~l"),
                f.parse("l|s"),
                f.parse("n|s")
        );
        final List<Formula> result = SmusComputation.computeSmusForFormulas(f, input, Collections.emptyList());
        assertThat(result).containsExactlyInAnyOrder(f.parse("~s"),
                f.parse("s|~p"),
                f.parse("p|~m"),
                f.parse("m|~n"),
                f.parse("n|s"));
    }

    @Test
    public void testManyConflicts() throws ParserException {
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
        final List<Formula> result = SmusComputation.computeSmusForFormulas(f, input, Collections.emptyList());
        assertThat(result).containsExactlyInAnyOrder(f.parse("x&~y"), f.parse("x=>y"));
    }

    @Test
    public void testTimeoutHandlerSmall() throws ParserException {
        final List<TimeoutOptimizationHandler> handlers = Arrays.asList(
                new TimeoutOptimizationHandler(5_000L, TimeoutHandler.TimerType.SINGLE_TIMEOUT),
                new TimeoutOptimizationHandler(5_000L, TimeoutHandler.TimerType.RESTARTING_TIMEOUT),
                new TimeoutOptimizationHandler(System.currentTimeMillis() + 5_000L, TimeoutHandler.TimerType.FIXED_END)
        );
        final List<Formula> formulas = Arrays.asList(
                f.parse("a"),
                f.parse("~a")
        );
        for (final TimeoutOptimizationHandler handler : handlers) {
            testHandler(handler, formulas, false);
        }
    }

    @Test
    public void testTimeoutHandlerLarge() throws ParserException, IOException {
        final List<TimeoutOptimizationHandler> handlers = Arrays.asList(
                new TimeoutOptimizationHandler(1L, TimeoutHandler.TimerType.SINGLE_TIMEOUT),
                new TimeoutOptimizationHandler(1L, TimeoutHandler.TimerType.RESTARTING_TIMEOUT),
                new TimeoutOptimizationHandler(System.currentTimeMillis() + 1L, TimeoutHandler.TimerType.FIXED_END)
        );
        final Formula formula =
                FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/large_formula.txt");
        final List<Formula> formulas = formula.stream().collect(Collectors.toList());
        for (final TimeoutOptimizationHandler handler : handlers) {
            testHandler(handler, formulas, true);
        }
    }

    @Test
    @LongRunningTag
    public void testCancellationPoints() throws IOException {
        final List<Formula> formulas = DimacsReader.readCNF(f, "src/test/resources/sat/unsat/bf0432-007.cnf");
        for (int numOptimizationStarts = 1; numOptimizationStarts < 5; numOptimizationStarts++) {
            for (int numSatHandlerStarts = 1; numSatHandlerStarts < 10; numSatHandlerStarts++) {
                final OptimizationHandler handler =
                        new BoundedOptimizationHandler(numSatHandlerStarts, numOptimizationStarts);
                testHandler(handler, formulas, true);
            }
        }
    }

    @Test
    public void testMinimumHittingSetCancelled() throws ParserException {
        final OptimizationHandler handler = new BoundedOptimizationHandler(-1, 0);
        final List<Formula> formulas = Arrays.asList(
                f.parse("a"),
                f.parse("~a")
        );
        testHandler(handler, formulas, true);
    }

    @Test
    public void testHSolverCancelled() throws ParserException {
        final OptimizationHandler handler = new BoundedOptimizationHandler(-1, 3);
        final List<Formula> formulas = Arrays.asList(
                f.parse("a"),
                f.parse("~a"),
                f.parse("c")
        );
        testHandler(handler, formulas, true);
    }

    private void testHandler(final OptimizationHandler handler, final List<Formula> formulas,
                             final boolean expAborted) {
        final List<Formula> result =
                SmusComputation.computeSmusForFormulas(f, formulas, Collections.emptyList(), handler);
        assertThat(handler.aborted()).isEqualTo(expAborted);
        if (expAborted) {
            assertThat(result).isNull();
        } else {
            assertThat(result).isNotNull();
        }
    }
}
