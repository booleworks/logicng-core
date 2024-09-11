// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.primecomputation;

import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.FIXED_END;
import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.RESTARTING_TIMEOUT;
import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.SINGLE_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
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
import com.booleworks.logicng.util.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

public class PrimeCompilerTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimple(final FormulaContext _c) {
        computeAndVerify(_c.f, _c.verum);
        computeAndVerify(_c.f, _c.falsum);
        computeAndVerify(_c.f, _c.a);
        computeAndVerify(_c.f, _c.na);
        computeAndVerify(_c.f, _c.and1);
        computeAndVerify(_c.f, _c.and2);
        computeAndVerify(_c.f, _c.and3);
        computeAndVerify(_c.f, _c.or1);
        computeAndVerify(_c.f, _c.or2);
        computeAndVerify(_c.f, _c.or3);
        computeAndVerify(_c.f, _c.not1);
        computeAndVerify(_c.f, _c.not2);
        computeAndVerify(_c.f, _c.imp1);
        computeAndVerify(_c.f, _c.imp2);
        computeAndVerify(_c.f, _c.imp3);
        computeAndVerify(_c.f, _c.imp4);
        computeAndVerify(_c.f, _c.eq1);
        computeAndVerify(_c.f, _c.eq2);
        computeAndVerify(_c.f, _c.eq3);
        computeAndVerify(_c.f, _c.eq4);
        computeAndVerify(_c.f, _c.pbc1);
        computeAndVerify(_c.f, _c.pbc2);
        computeAndVerify(_c.f, _c.pbc3);
        computeAndVerify(_c.f, _c.pbc4);
        computeAndVerify(_c.f, _c.pbc5);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCornerCases(final FormulaContext _c) {
        final FormulaCornerCases cornerCases = new FormulaCornerCases(_c.f);
        cornerCases.cornerCases().forEach(it -> computeAndVerify(_c.f, it));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void testRandomized(final FormulaContext _c) {
        for (int i = 0; i < 100; i++) {
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f,
                    FormulaRandomizerConfig.builder().numVars(10).weightPbc(0).seed(i * 42).build());
            final Formula formula = randomizer.formula(4);
            computeAndVerify(_c.f, formula);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @LongRunningTag
    public void testOriginalFormulas(final FormulaContext _c) throws IOException {
        Files.lines(Paths.get("src/test/resources/formulas/simplify_formulas.txt"))
                .filter(s -> !s.isEmpty())
                .forEach(s -> {
                    try {
                        final Formula formula = _c.f.parse(s);
                        final PrimeResult resultImplicantsMin =
                                PrimeCompiler.getWithMinimization().compute(_c.f, formula,
                                        PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
                        verify(resultImplicantsMin, formula);
                        final PrimeResult resultImplicatesMin =
                                PrimeCompiler.getWithMinimization().compute(_c.f, formula,
                                        PrimeResult.CoverageType.IMPLICATES_COMPLETE);
                        verify(resultImplicatesMin, formula);
                    } catch (final ParserException e) {
                        fail(e.toString());
                    }
                });
    }

    @Test
    public void testTimeoutHandlerSmall() throws ParserException {
        final List<Pair<PrimeCompiler, PrimeResult.CoverageType>> compilers = Arrays.asList(
                new Pair<>(PrimeCompiler.getWithMaximization(), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMaximization(), PrimeResult.CoverageType.IMPLICATES_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMinimization(), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMinimization(), PrimeResult.CoverageType.IMPLICATES_COMPLETE));
        for (final Pair<PrimeCompiler, PrimeResult.CoverageType> compiler : compilers) {
            final List<TimeoutHandler> handlers = Arrays.asList(
                    new TimeoutHandler(5_000L, SINGLE_TIMEOUT),
                    new TimeoutHandler(5_000L, RESTARTING_TIMEOUT),
                    new TimeoutHandler(System.currentTimeMillis() + 5_000L, FIXED_END)
            );
            final Formula formula = FormulaFactory.caching().parse("a & b | ~c & a");
            for (final TimeoutHandler handler : handlers) {
                testHandler(handler, formula, compiler.first(), compiler.second(), false);
            }
        }
    }

    @Test
    public void testTimeoutHandlerLarge() throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.nonCaching();
        final List<Pair<PrimeCompiler, PrimeResult.CoverageType>> compilers = Arrays.asList(
                new Pair<>(PrimeCompiler.getWithMaximization(), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMaximization(), PrimeResult.CoverageType.IMPLICATES_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMinimization(), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMinimization(), PrimeResult.CoverageType.IMPLICATES_COMPLETE));
        for (final Pair<PrimeCompiler, PrimeResult.CoverageType> compiler : compilers) {
            final List<TimeoutHandler> handlers = Arrays.asList(
                    new TimeoutHandler(1L, SINGLE_TIMEOUT),
                    new TimeoutHandler(1L, RESTARTING_TIMEOUT),
                    new TimeoutHandler(System.currentTimeMillis() + 1L, FIXED_END)
            );
            final Formula formula =
                    FormulaReader.readFormula(f, "src/test/resources/formulas/large_formula.txt");
            for (final TimeoutHandler handler : handlers) {
                testHandler(handler, formula, compiler.first(), compiler.second(), true);
            }
        }
    }

    @Test
    public void testCancellationPoints() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.nonCaching();
        final Formula formula =
                f.parse(Files.readAllLines(Paths.get("src/test/resources/formulas/simplify_formulas.txt")).get(0));
        final List<Pair<PrimeCompiler, PrimeResult.CoverageType>> compilers = Arrays.asList(
                new Pair<>(PrimeCompiler.getWithMaximization(), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMaximization(), PrimeResult.CoverageType.IMPLICATES_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMinimization(), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMinimization(), PrimeResult.CoverageType.IMPLICATES_COMPLETE));
        for (final Pair<PrimeCompiler, PrimeResult.CoverageType> compiler : compilers) {
            for (int numOptimizationStarts = 1; numOptimizationStarts < 5; numOptimizationStarts++) {
                for (int numSatHandlerStarts = 1; numSatHandlerStarts < 10; numSatHandlerStarts++) {
                    final BoundedOptimizationHandler handler =
                            new BoundedOptimizationHandler(numSatHandlerStarts, numOptimizationStarts);
                    testHandler(handler, formula, compiler.first(), compiler.second(), true);
                }
            }
        }
    }

    private void computeAndVerify(final FormulaFactory f, final Formula formula) {
        final PrimeResult resultImplicantsMax =
                PrimeCompiler.getWithMaximization().compute(f, formula, PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        verify(resultImplicantsMax, formula);
        final PrimeResult resultImplicantsMin =
                PrimeCompiler.getWithMinimization().compute(f, formula, PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        verify(resultImplicantsMin, formula);
        assertThat(resultImplicantsMax.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        assertThat(resultImplicantsMin.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        assertThat(resultImplicantsMax.getPrimeImplicants())
                .containsExactlyInAnyOrderElementsOf(resultImplicantsMin.getPrimeImplicants());

        final PrimeResult resultImplicatesMax =
                PrimeCompiler.getWithMaximization().compute(f, formula, PrimeResult.CoverageType.IMPLICATES_COMPLETE);
        verify(resultImplicatesMax, formula);
        final PrimeResult resultImplicatesMin =
                PrimeCompiler.getWithMinimization().compute(f, formula, PrimeResult.CoverageType.IMPLICATES_COMPLETE);
        verify(resultImplicatesMin, formula);
        assertThat(resultImplicatesMax.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICATES_COMPLETE);
        assertThat(resultImplicatesMin.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICATES_COMPLETE);
        assertThat(resultImplicatesMax.getPrimeImplicates())
                .containsExactlyInAnyOrderElementsOf(resultImplicatesMin.getPrimeImplicates());
    }

    private void verify(final PrimeResult result, final Formula formula) {
        verifyImplicants(result.getPrimeImplicants(), formula);
        verifyImplicates(result.getPrimeImplicates(), formula);
    }

    private void verifyImplicants(final List<SortedSet<Literal>> implicantSets, final Formula formula) {
        final FormulaFactory f = formula.factory();
        final List<Formula> implicants = new ArrayList<>();
        for (final SortedSet<Literal> implicant : implicantSets) {
            implicants.add(f.and(implicant));
            PrimeImplicantReductionTest.testPrimeImplicantProperty(formula, implicant);
        }
        assertThat(f.equivalence(f.or(implicants), formula).holds(new TautologyPredicate(f)))
                .as("Disjunction of implicants should be equivalent to the original formula.")
                .isTrue();
    }

    private void verifyImplicates(final List<SortedSet<Literal>> implicateSets, final Formula formula) {
        final FormulaFactory f = formula.factory();
        final List<Formula> implicates = new ArrayList<>();
        for (final SortedSet<Literal> implicate : implicateSets) {
            implicates.add(f.or(implicate));
            PrimeImplicateReductionTest.testPrimeImplicateProperty(formula, implicate);
        }
        assertThat(f.equivalence(f.and(implicates), formula).holds(new TautologyPredicate(f)))
                .as("Conjunction of implicates should be equivalent to the original formula.")
                .isTrue();
    }

    private void testHandler(final ComputationHandler handler, final Formula formula, final PrimeCompiler compiler,
                             final PrimeResult.CoverageType coverageType, final boolean expCanceled) {
        final LNGResult<PrimeResult> result = compiler.compute(formula.factory(), formula, coverageType, handler);
        assertThat(!result.isSuccess()).isEqualTo(expCanceled);
        if (expCanceled) {
            assertThatThrownBy(result::getResult).isInstanceOf(IllegalStateException.class);
        } else {
            assertThat(result.getResult()).isNotNull();
        }
    }
}
