// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.primecomputation;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.handlers.BoundedSatHandler;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.SATCall;
import com.booleworks.logicng.util.FormulaCornerCases;
import com.booleworks.logicng.util.FormulaHelper;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class PrimeImplicateReductionTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPrimeImplicateNaive(final FormulaContext _c) throws ParserException {
        final NaivePrimeReduction naive01 = new NaivePrimeReduction(_c.f, _c.f.parse("a&b"));
        assertThat(naive01.reduceImplicate(_c.f, new TreeSet<>(Arrays.asList(_c.a, _c.b))))
                .containsAnyOf(_c.a, _c.b).hasSize(1);

        final NaivePrimeReduction naive02 = new NaivePrimeReduction(_c.f, _c.f.parse("(a => b) | b | c"));
        assertThat(naive02.reduceImplicate(_c.f, new TreeSet<>(Arrays.asList(_c.a.negate(_c.f), _c.b, _c.c))))
                .containsExactly(_c.a.negate(_c.f), _c.b, _c.c);

        final NaivePrimeReduction naive03 = new NaivePrimeReduction(_c.f, _c.f.parse("(a => b) & b & c"));
        assertThat(naive03.reduceImplicate(_c.f, new TreeSet<>(Arrays.asList(_c.b, _c.c))))
                .containsAnyOf(_c.b, _c.c).hasSize(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFormula1(final FormulaContext _c) throws IOException, ParserException {
        final Formula formula =
                FormulaReader.readPropositionalFormula(_c.f, "src/test/resources/formulas/formula1.txt");
        testFormula(formula);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimplifyFormulas(final FormulaContext _c) throws IOException, ParserException {
        final Formula formula =
                FormulaReader.readPropositionalFormula(_c.f, "src/test/resources/formulas/simplify_formulas.txt");
        testFormula(formula);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLargeFormula(final FormulaContext _c) throws IOException, ParserException {
        final Formula formula =
                FormulaReader.readPropositionalFormula(_c.f, "src/test/resources/formulas/large_formula.txt");
        testFormula(formula);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSmallFormulas(final FormulaContext _c) throws IOException, ParserException {
        final List<String> lines = Files.readAllLines(Paths.get("src/test/resources/formulas/small_formulas.txt"));
        for (final String line : lines) {
            testFormula(_c.f.parse(line));
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCornerCases(final FormulaContext _c) {
        final FormulaCornerCases cornerCases = new FormulaCornerCases(_c.f);
        cornerCases.cornerCases().forEach(this::testFormula);
    }

    @Test
    @RandomTag
    public void testRandom() {
        for (int i = 0; i < 500; i++) {
            final FormulaFactory f = FormulaFactory.caching();
            final FormulaRandomizer randomizer = new FormulaRandomizer(f,
                    FormulaRandomizerConfig.builder().numVars(20).weightPbc(2).seed(i * 42).build());
            final Formula formula = randomizer.formula(4);
            testFormula(formula);
        }
    }

    @Test
    public void testCancellationPoints() throws ParserException, IOException {
        final Formula formula = FormulaReader.readPropositionalFormula(FormulaFactory.nonCaching(),
                "src/test/resources/formulas/large_formula.txt");
        for (int numStarts = 0; numStarts < 20; numStarts++) {
            final ComputationHandler handler = new BoundedSatHandler(numStarts);
            testFormula(formula, handler, true);
        }
    }

    private void testFormula(final Formula formula) {
        testFormula(formula, NopHandler.get(), false);
    }

    private void testFormula(final Formula formula, final ComputationHandler handler, final boolean expCanceled) {
        final FormulaFactory f = formula.factory();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(formula.negate(f));
        try (final SATCall call = solver.satCall().solve()) {
            if (!call.getSatResult().getResult()) {
                return;
            }
            final SortedSet<Literal> falsifyingAssignment =
                    FormulaHelper.negateLiterals(f, call.model(formula.variables(f)).getLiterals(), TreeSet::new);
            final NaivePrimeReduction naive = new NaivePrimeReduction(f, formula);
            final LNGResult<SortedSet<Literal>> primeImplicate = naive.reduceImplicate(f, falsifyingAssignment, handler);
            if (expCanceled) {
                assertThat(primeImplicate.isSuccess()).isFalse();
            } else {
                assertThat(falsifyingAssignment).containsAll(primeImplicate.getResult());
                testPrimeImplicateProperty(formula, primeImplicate.getResult());
            }
        }
    }

    public static void testPrimeImplicateProperty(final Formula formula, final SortedSet<Literal> primeImplicate) {
        final FormulaFactory f = formula.factory();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(formula);
        final SortedSet<Literal> negatedLiterals = FormulaHelper.negateLiterals(f, primeImplicate, TreeSet::new);
        assertThat(solver.satCall().addFormulas(negatedLiterals).sat().getResult()).isFalse();
        for (final Literal lit : negatedLiterals) {
            final SortedSet<Literal> reducedNegatedLiterals = new TreeSet<>(negatedLiterals);
            reducedNegatedLiterals.remove(lit);
            assertThat(solver.satCall().addFormulas(reducedNegatedLiterals).sat().getResult()).isTrue();
        }
    }
}
