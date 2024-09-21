// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.primecomputation;

import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.FIXED_END;
import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.RESTARTING_TIMEOUT;
import static com.booleworks.logicng.handlers.TimeoutHandler.TimerType.SINGLE_TIMEOUT;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_INC_WBO;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_LINEAR_SU;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_LINEAR_US;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_MSU3;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_OLL;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_WBO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import com.booleworks.logicng.FormulaCornerCases;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
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
import com.booleworks.logicng.util.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

public class PrimeCompilerTest extends TestWithFormulaContext {

    public static Collection<Object[]> configs() {
        final List<Object[]> configs = new ArrayList<>();
        configs.add(new Object[]{CONFIG_INC_WBO, "INCWBO"});
        configs.add(new Object[]{CONFIG_LINEAR_SU, "LINEAR_SU"});
        configs.add(new Object[]{CONFIG_LINEAR_US, "LINEAR_US"});
        configs.add(new Object[]{CONFIG_MSU3, "MSU3"});
        configs.add(new Object[]{CONFIG_OLL, "OLL"});
        configs.add(new Object[]{CONFIG_WBO, "WBO"});
        return configs;
    }

    public static Collection<Object[]> fastConfigs() {
        final List<Object[]> configs = new ArrayList<>();
        configs.add(new Object[]{CONFIG_LINEAR_SU, "LINEAR_SU"});
        configs.add(new Object[]{CONFIG_LINEAR_US, "LINEAR_US"});
        configs.add(new Object[]{CONFIG_MSU3, "MSU3"});
        configs.add(new Object[]{CONFIG_OLL, "OLL"});
        return configs;
    }


    @ParameterizedTest
    @MethodSource("configs")
    public void testSimple(final MaxSatConfig config) {
        final var f = FormulaFactory.caching();
        final var _c = new FormulaContext(f);
        computeAndVerify(_c.f, config, _c.verum);
        computeAndVerify(_c.f, config, _c.falsum);
        computeAndVerify(_c.f, config, _c.a);
        computeAndVerify(_c.f, config, _c.na);
        computeAndVerify(_c.f, config, _c.and1);
        computeAndVerify(_c.f, config, _c.and2);
        computeAndVerify(_c.f, config, _c.and3);
        computeAndVerify(_c.f, config, _c.or1);
        computeAndVerify(_c.f, config, _c.or2);
        computeAndVerify(_c.f, config, _c.or3);
        computeAndVerify(_c.f, config, _c.not1);
        computeAndVerify(_c.f, config, _c.not2);
        computeAndVerify(_c.f, config, _c.imp1);
        computeAndVerify(_c.f, config, _c.imp2);
        computeAndVerify(_c.f, config, _c.imp3);
        computeAndVerify(_c.f, config, _c.imp4);
        computeAndVerify(_c.f, config, _c.eq1);
        computeAndVerify(_c.f, config, _c.eq2);
        computeAndVerify(_c.f, config, _c.eq3);
        computeAndVerify(_c.f, config, _c.eq4);
        computeAndVerify(_c.f, config, _c.pbc1);
        computeAndVerify(_c.f, config, _c.pbc2);
        computeAndVerify(_c.f, config, _c.pbc3);
        computeAndVerify(_c.f, config, _c.pbc4);
        computeAndVerify(_c.f, config, _c.pbc5);
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testCornerCases(final MaxSatConfig config) {
        final var f = FormulaFactory.caching();
        final var cornerCases = new FormulaCornerCases(f);
        cornerCases.cornerCases().forEach(it -> computeAndVerify(f, config, it));
    }

    @ParameterizedTest
    @MethodSource("configs")
    @RandomTag
    public void testRandomized(final MaxSatConfig config) {
        final var f = FormulaFactory.caching();
        for (int i = 0; i < 100; i++) {
            final FormulaRandomizer randomizer = new FormulaRandomizer(f,
                    FormulaRandomizerConfig.builder().numVars(10).weightPbc(0).seed(i * 42).build());
            final Formula formula = randomizer.formula(4);
            computeAndVerify(f, config, formula);
        }
    }

    @ParameterizedTest
    @MethodSource("fastConfigs")
    @LongRunningTag
    public void testOriginalFormulas(final MaxSatConfig config) throws IOException {
        final var f = FormulaFactory.caching();
        Files.lines(Paths.get("../test_files/formulas/simplify_formulas.txt"))
                .filter(s -> !s.isEmpty())
                .forEach(s -> {
                    try {
                        final Formula formula = f.parse(s);
                        final PrimeResult resultImplicantsMin = new PrimeCompiler(f, false, config)
                                .compute(formula, PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
                        verify(resultImplicantsMin, formula);
                        final PrimeResult resultImplicatesMin = new PrimeCompiler(f, false, config)
                                .compute(formula, PrimeResult.CoverageType.IMPLICATES_COMPLETE);
                        verify(resultImplicatesMin, formula);
                    } catch (final ParserException e) {
                        fail(e.toString());
                    }
                });
    }

    @Test
    public void testTimeoutHandlerSmall() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final List<Pair<PrimeCompiler, PrimeResult.CoverageType>> compilers = Arrays.asList(
                new Pair<>(new PrimeCompiler(f, true), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(new PrimeCompiler(f, true), PrimeResult.CoverageType.IMPLICATES_COMPLETE),
                new Pair<>(new PrimeCompiler(f, false), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(new PrimeCompiler(f, false), PrimeResult.CoverageType.IMPLICATES_COMPLETE));
        for (final Pair<PrimeCompiler, PrimeResult.CoverageType> compiler : compilers) {
            final List<TimeoutHandler> handlers = Arrays.asList(
                    new TimeoutHandler(5_000L, SINGLE_TIMEOUT),
                    new TimeoutHandler(5_000L, RESTARTING_TIMEOUT),
                    new TimeoutHandler(System.currentTimeMillis() + 5_000L, FIXED_END)
            );
            final Formula formula = f.parse("a & b | ~c & a");
            for (final TimeoutHandler handler : handlers) {
                testHandler(handler, formula, compiler.getFirst(), compiler.getSecond(), false);
            }
        }
    }

    @Test
    public void testTimeoutHandlerLarge() throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.nonCaching();
        final List<Pair<PrimeCompiler, PrimeResult.CoverageType>> compilers = Arrays.asList(
                new Pair<>(new PrimeCompiler(f, true), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(new PrimeCompiler(f, true), PrimeResult.CoverageType.IMPLICATES_COMPLETE),
                new Pair<>(new PrimeCompiler(f, false), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(new PrimeCompiler(f, false), PrimeResult.CoverageType.IMPLICATES_COMPLETE));
        for (final Pair<PrimeCompiler, PrimeResult.CoverageType> compiler : compilers) {
            final List<TimeoutHandler> handlers = Arrays.asList(
                    new TimeoutHandler(1L, SINGLE_TIMEOUT),
                    new TimeoutHandler(1L, RESTARTING_TIMEOUT),
                    new TimeoutHandler(System.currentTimeMillis() + 1L, FIXED_END)
            );
            final Formula formula =
                    FormulaReader.readFormula(f, "../test_files/formulas/large_formula.txt");
            for (final TimeoutHandler handler : handlers) {
                testHandler(handler, formula, compiler.getFirst(), compiler.getSecond(), true);
            }
        }
    }

    @Test
    public void testCancellationPoints() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.nonCaching();
        final Formula formula =
                f.parse(Files.readAllLines(Paths.get("../test_files/formulas/simplify_formulas.txt")).get(0));
        final List<Pair<PrimeCompiler, PrimeResult.CoverageType>> compilers = Arrays.asList(
                new Pair<>(new PrimeCompiler(f, true), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(new PrimeCompiler(f, true), PrimeResult.CoverageType.IMPLICATES_COMPLETE),
                new Pair<>(new PrimeCompiler(f, false), PrimeResult.CoverageType.IMPLICANTS_COMPLETE),
                new Pair<>(new PrimeCompiler(f, false), PrimeResult.CoverageType.IMPLICATES_COMPLETE));
        for (final Pair<PrimeCompiler, PrimeResult.CoverageType> compiler : compilers) {
            for (int numOptimizationStarts = 1; numOptimizationStarts < 5; numOptimizationStarts++) {
                for (int numSatHandlerStarts = 1; numSatHandlerStarts < 10; numSatHandlerStarts++) {
                    final BoundedOptimizationHandler handler =
                            new BoundedOptimizationHandler(numSatHandlerStarts, numOptimizationStarts);
                    testHandler(handler, formula, compiler.getFirst(), compiler.getSecond(), true);
                }
            }
        }
    }

    private void computeAndVerify(final FormulaFactory f, final MaxSatConfig config, final Formula formula) {
        final PrimeResult resultImplicantsMax = new PrimeCompiler(f, true, config)
                .compute(formula, PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        verify(resultImplicantsMax, formula);
        final PrimeResult resultImplicantsMin = new PrimeCompiler(f, false, config)
                .compute(formula, PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        verify(resultImplicantsMin, formula);
        assertThat(resultImplicantsMax.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        assertThat(resultImplicantsMin.getCoverageType()).isEqualTo(PrimeResult.CoverageType.IMPLICANTS_COMPLETE);
        assertThat(resultImplicantsMax.getPrimeImplicants())
                .containsExactlyInAnyOrderElementsOf(resultImplicantsMin.getPrimeImplicants());

        final PrimeResult resultImplicatesMax = new PrimeCompiler(f, true, config)
                .compute(formula, PrimeResult.CoverageType.IMPLICATES_COMPLETE);
        verify(resultImplicatesMax, formula);
        final PrimeResult resultImplicatesMin = new PrimeCompiler(f, false, config)
                .compute(formula, PrimeResult.CoverageType.IMPLICATES_COMPLETE);
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
        final FormulaFactory f = formula.getFactory();
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
        final FormulaFactory f = formula.getFactory();
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
        final LngResult<PrimeResult> result = compiler.compute(formula, coverageType, handler);
        assertThat(!result.isSuccess()).isEqualTo(expCanceled);
        if (expCanceled) {
            assertThatThrownBy(result::getResult).isInstanceOf(IllegalStateException.class);
        } else {
            assertThat(result.getResult()).isNotNull();
        }
    }
}
