// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.primecomputation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.logicng.RandomTag;
import org.logicng.TestWithExampleFormulas;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.handlers.BoundedOptimizationHandler;
import org.logicng.handlers.OptimizationHandler;
import org.logicng.handlers.TimeoutHandler;
import org.logicng.handlers.TimeoutOptimizationHandler;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.readers.FormulaReader;
import org.logicng.predicates.satisfiability.TautologyPredicate;
import org.logicng.util.FormulaCornerCases;
import org.logicng.util.FormulaRandomizer;
import org.logicng.util.FormulaRandomizerConfig;
import org.logicng.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

public class PrimeCompilerTest extends TestWithExampleFormulas {

    @Test
    public void testSimple() {
        computeAndVerify(f, TRUE);
        computeAndVerify(f, FALSE);
        computeAndVerify(f, A);
        computeAndVerify(f, NA);
        computeAndVerify(f, AND1);
        computeAndVerify(f, AND2);
        computeAndVerify(f, AND3);
        computeAndVerify(f, OR1);
        computeAndVerify(f, OR2);
        computeAndVerify(f, OR3);
        computeAndVerify(f, NOT1);
        computeAndVerify(f, NOT2);
        computeAndVerify(f, IMP1);
        computeAndVerify(f, IMP2);
        computeAndVerify(f, IMP3);
        computeAndVerify(f, IMP4);
        computeAndVerify(f, EQ1);
        computeAndVerify(f, EQ2);
        computeAndVerify(f, EQ3);
        computeAndVerify(f, EQ4);
        computeAndVerify(f, PBC1);
        computeAndVerify(f, PBC2);
        computeAndVerify(f, PBC3);
        computeAndVerify(f, PBC4);
        computeAndVerify(f, PBC5);
    }

    @Test
    public void testCornerCases() {
        final FormulaFactory f = FormulaFactory.caching();
        final FormulaCornerCases cornerCases = new FormulaCornerCases(f);
        cornerCases.cornerCases().forEach(it -> computeAndVerify(f, it));
    }

    @Test
    @RandomTag
    public void testRandomized() {
        for (int i = 0; i < 200; i++) {
            final FormulaFactory f = FormulaFactory.caching();
            final FormulaRandomizer randomizer = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().numVars(10).weightPbc(2).seed(i * 42).build());
            final Formula formula = randomizer.formula(4);
            computeAndVerify(f, formula);
        }
    }

    @Test
    public void testOriginalFormulas() throws IOException {
        Files.lines(Paths.get("src/test/resources/formulas/simplify_formulas.txt"))
                .filter(s -> !s.isEmpty())
                .forEach(s -> {
                    try {
                        final Formula formula = f.parse(s);
                        final PrimeResult resultImplicantsMin = PrimeCompiler.getWithMinimization().compute(f, formula,
                                PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
                        verify(resultImplicantsMin, formula);
                        final PrimeResult resultImplicatesMin = PrimeCompiler.getWithMinimization().compute(f, formula,
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
            final List<TimeoutOptimizationHandler> handlers = Arrays.asList(
                    new TimeoutOptimizationHandler(5_000L, TimeoutHandler.TimerType.SINGLE_TIMEOUT),
                    new TimeoutOptimizationHandler(5_000L, TimeoutHandler.TimerType.RESTARTING_TIMEOUT),
                    new TimeoutOptimizationHandler(System.currentTimeMillis() + 5_000L, TimeoutHandler.TimerType.FIXED_END)
            );
            final Formula formula = f.parse("a & b | ~c & a");
            for (final TimeoutOptimizationHandler handler : handlers) {
                testHandler(handler, formula, compiler.first(), compiler.second(), false);
            }
        }
    }

    @Test
    public void testTimeoutHandlerLarge() throws ParserException, IOException {
        final List<Pair<PrimeCompiler, PrimeResult.CoverageType>> compilers = Arrays.asList(
                new Pair<>(PrimeCompiler.getWithMaximization(), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMaximization(), PrimeResult.CoverageType.IMPLICATES_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMinimization(), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMinimization(), PrimeResult.CoverageType.IMPLICATES_COMPLETE));
        for (final Pair<PrimeCompiler, PrimeResult.CoverageType> compiler : compilers) {
            final List<TimeoutOptimizationHandler> handlers = Arrays.asList(
                    new TimeoutOptimizationHandler(1L, TimeoutHandler.TimerType.SINGLE_TIMEOUT),
                    new TimeoutOptimizationHandler(1L, TimeoutHandler.TimerType.RESTARTING_TIMEOUT),
                    new TimeoutOptimizationHandler(System.currentTimeMillis() + 1L, TimeoutHandler.TimerType.FIXED_END)
            );
            final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/large_formula.txt", f);
            for (final TimeoutOptimizationHandler handler : handlers) {
                testHandler(handler, formula, compiler.first(), compiler.second(), true);
            }
        }
    }

    @Test
    public void testCancellationPoints() throws IOException, ParserException {
        final Formula formula = f.parse(Files.readAllLines(Paths.get("src/test/resources/formulas/simplify_formulas.txt")).get(0));
        final List<Pair<PrimeCompiler, PrimeResult.CoverageType>> compilers = Arrays.asList(
                new Pair<>(PrimeCompiler.getWithMaximization(), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMaximization(), PrimeResult.CoverageType.IMPLICATES_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMinimization(), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(PrimeCompiler.getWithMinimization(), PrimeResult.CoverageType.IMPLICATES_COMPLETE));
        for (final Pair<PrimeCompiler, PrimeResult.CoverageType> compiler : compilers) {
            for (int numOptimizationStarts = 1; numOptimizationStarts < 5; numOptimizationStarts++) {
                for (int numSatHandlerStarts = 1; numSatHandlerStarts < 10; numSatHandlerStarts++) {
                    final OptimizationHandler handler = new BoundedOptimizationHandler(numSatHandlerStarts, numOptimizationStarts);
                    testHandler(handler, formula, compiler.first(), compiler.second(), true);
                }
            }
        }
    }

    private void computeAndVerify(final FormulaFactory f, final Formula formula) {
        final PrimeResult resultImplicantsMax = PrimeCompiler.getWithMaximization().compute(f, formula, PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        verify(resultImplicantsMax, formula);
        final PrimeResult resultImplicantsMin = PrimeCompiler.getWithMinimization().compute(f, formula, PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        verify(resultImplicantsMin, formula);
        assertThat(resultImplicantsMax.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        assertThat(resultImplicantsMin.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        assertThat(resultImplicantsMax.getPrimeImplicants()).containsExactlyInAnyOrderElementsOf(resultImplicantsMin.getPrimeImplicants());

        final PrimeResult resultImplicatesMax = PrimeCompiler.getWithMaximization().compute(f, formula, PrimeResult.CoverageType.IMPLICATES_COMPLETE);
        verify(resultImplicatesMax, formula);
        final PrimeResult resultImplicatesMin = PrimeCompiler.getWithMinimization().compute(f, formula, PrimeResult.CoverageType.IMPLICATES_COMPLETE);
        verify(resultImplicatesMin, formula);
        assertThat(resultImplicatesMax.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICATES_COMPLETE);
        assertThat(resultImplicatesMin.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICATES_COMPLETE);
        assertThat(resultImplicatesMax.getPrimeImplicates()).containsExactlyInAnyOrderElementsOf(resultImplicatesMin.getPrimeImplicates());
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

    private void testHandler(final OptimizationHandler handler, final Formula formula, final PrimeCompiler compiler, final PrimeResult.CoverageType coverageType,
                             final boolean expAborted) {
        final PrimeResult result = compiler.compute(f, formula, coverageType, handler);
        assertThat(handler.aborted()).isEqualTo(expAborted);
        if (expAborted) {
            assertThat(result).isNull();
        } else {
            assertThat(result).isNotNull();
        }
    }
}
