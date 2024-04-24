// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.primecomputation;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.handlers.BoundedSatHandler;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.SATCall;
import com.booleworks.logicng.util.FormulaCornerCases;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.assertj.core.api.Assertions;
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

public class PrimeImplicantReductionTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimple(final FormulaContext _c) throws ParserException {
        final NaivePrimeReduction naiveTautology = new NaivePrimeReduction(_c.f, _c.f.verum());
        assertThat(naiveTautology.reduceImplicant(new TreeSet<>(Arrays.asList(_c.a, _c.b)))).isEmpty();

        final NaivePrimeReduction naive01 = new NaivePrimeReduction(_c.f, _c.f.parse("a&b|c&d"));
        assertThat(naive01.reduceImplicant(new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.c, _c.d.negate(_c.f)))))
                .containsExactlyInAnyOrder(_c.a, _c.b);
        assertThat(naive01.reduceImplicant(new TreeSet<>(Arrays.asList(_c.a.negate(_c.f), _c.b, _c.c, _c.d))))
                .containsExactlyInAnyOrder(_c.c, _c.d);

        final NaivePrimeReduction naive02 = new NaivePrimeReduction(_c.f, _c.f.parse("a|b|~a&~b"));
        assertThat(naive02.reduceImplicant(new TreeSet<>(Arrays.asList(_c.a.negate(_c.f), _c.b))))
                .containsExactlyInAnyOrder();
        assertThat(naive02.reduceImplicant(new TreeSet<>(Arrays.asList(_c.a.negate(_c.f), _c.b))))
                .containsExactlyInAnyOrder();

        final NaivePrimeReduction naive03 = new NaivePrimeReduction(_c.f, _c.f.parse("(a => b) | b | c"));
        assertThat(naive03.reduceImplicant(new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.c.negate(_c.f)))))
                .containsExactlyInAnyOrder(_c.b);
        assertThat(naive03.reduceImplicant(new TreeSet<>(Arrays.asList(_c.a, _c.b.negate(_c.f), _c.c))))
                .containsExactlyInAnyOrder(_c.c);
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
    @LongRunningTag
    public void testLargeFormula(final FormulaContext _c) throws IOException, ParserException {
        final Formula formula =
                FormulaReader.readPropositionalFormula(_c.f, "src/test/resources/formulas/large_formula.txt");
        testFormula(formula);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @LongRunningTag
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
            final SATHandler handler = new BoundedSatHandler(numStarts);
            testFormula(formula, handler, true);
        }
    }

    private void testFormula(final Formula formula) {
        testFormula(formula, null, false);
    }

    private void testFormula(final Formula formula, final SATHandler handler, final boolean expAborted) {
        final FormulaFactory f = formula.factory();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(formula);
        try (final SATCall call = solver.satCall().solve()) {
            final boolean isSAT = call.getSatResult() == Tristate.TRUE;
            if (!isSAT) {
                return;
            }
            final SortedSet<Literal> model = call.model(formula.variables(f)).literals();
            final NaivePrimeReduction naive = new NaivePrimeReduction(f, formula);
            final SortedSet<Literal> primeImplicant = naive.reduceImplicant(model, handler);
            if (expAborted) {
                assertThat(handler.aborted()).isTrue();
                assertThat(primeImplicant).isNull();
            } else {
                assertThat(model).containsAll(primeImplicant);
                testPrimeImplicantProperty(formula, primeImplicant);
            }
        }
    }

    public static void testPrimeImplicantProperty(final Formula formula, final SortedSet<Literal> primeImplicant) {
        final FormulaFactory f = formula.factory();
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(formula.negate(f));
        Assertions.assertThat(solver.satCall().addFormulas(primeImplicant).sat()).isEqualTo(Tristate.FALSE);
        for (final Literal lit : primeImplicant) {
            final SortedSet<Literal> reducedPrimeImplicant = new TreeSet<>(primeImplicant);
            reducedPrimeImplicant.remove(lit);
            Assertions.assertThat(solver.satCall().addFormulas(reducedPrimeImplicant).sat()).isEqualTo(Tristate.TRUE);
        }
    }
}
