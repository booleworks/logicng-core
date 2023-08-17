// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.primecomputation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.RandomTag;
import org.logicng.TestWithExampleFormulas;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.handlers.BoundedSatHandler;
import org.logicng.handlers.SATHandler;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.readers.FormulaReader;
import org.logicng.solvers.MiniSat;
import org.logicng.util.FormulaCornerCases;
import org.logicng.util.FormulaRandomizer;
import org.logicng.util.FormulaRandomizerConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class PrimeImplicantReductionTest extends TestWithExampleFormulas {

    @Test
    public void testSimple() throws ParserException {
        final NaivePrimeReduction naiveTautology = new NaivePrimeReduction(f, f.verum());
        assertThat(naiveTautology.reduceImplicant(new TreeSet<>(Arrays.asList(A, B)))).isEmpty();

        final NaivePrimeReduction naive01 = new NaivePrimeReduction(f, f.parse("a&b|c&d"));
        assertThat(naive01.reduceImplicant(new TreeSet<>(Arrays.asList(A, B, C, D.negate()))))
                .containsExactlyInAnyOrder(A, B);
        assertThat(naive01.reduceImplicant(new TreeSet<>(Arrays.asList(A.negate(), B, C, D))))
                .containsExactlyInAnyOrder(C, D);

        final NaivePrimeReduction naive02 = new NaivePrimeReduction(f, f.parse("a|b|~a&~b"));
        assertThat(naive02.reduceImplicant(new TreeSet<>(Arrays.asList(A.negate(), B))))
                .containsExactlyInAnyOrder();
        assertThat(naive02.reduceImplicant(new TreeSet<>(Arrays.asList(A.negate(), B))))
                .containsExactlyInAnyOrder();

        final NaivePrimeReduction naive03 = new NaivePrimeReduction(f, f.parse("(a => b) | b | c"));
        assertThat(naive03.reduceImplicant(new TreeSet<>(Arrays.asList(A, B, C.negate()))))
                .containsExactlyInAnyOrder(B);
        assertThat(naive03.reduceImplicant(new TreeSet<>(Arrays.asList(A, B.negate(), C))))
                .containsExactlyInAnyOrder(C);
    }

    @Test
    public void testFormula1() throws IOException, ParserException {
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/formula1.txt", f);
        testFormula(formula);
    }

    @Test
    public void testSimplifyFormulas() throws IOException, ParserException {
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/simplify_formulas.txt", f);
        testFormula(formula);
    }

    @Test
    public void testLargeFormula() throws IOException, ParserException {
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/large_formula.txt", f);
        testFormula(formula);
    }

    @Test
    public void testSmallFormulas() throws IOException, ParserException {
        final List<String> lines = Files.readAllLines(Paths.get("src/test/resources/formulas/small_formulas.txt"));
        for (final String line : lines) {
            testFormula(f.parse(line));
        }
    }

    @Test
    public void testCornerCases() {
        final FormulaFactory f = FormulaFactory.caching();
        final FormulaCornerCases cornerCases = new FormulaCornerCases(f);
        cornerCases.cornerCases().forEach(this::testFormula);
    }

    @Test
    @RandomTag
    public void testRandom() {
        for (int i = 0; i < 500; i++) {
            final FormulaFactory f = FormulaFactory.caching();
            final FormulaRandomizer randomizer = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().numVars(20).weightPbc(2).seed(i * 42).build());
            final Formula formula = randomizer.formula(4);
            testFormula(formula);
        }
    }

    @Test
    public void testCancellationPoints() throws ParserException, IOException {
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/large_formula.txt", f);
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
        final MiniSat solver = MiniSat.miniSat(f);
        solver.add(formula);
        final boolean isSAT = solver.sat() == Tristate.TRUE;
        if (!isSAT) {
            return;
        }
        final SortedSet<Literal> model = solver.model().literals();
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

    public static void testPrimeImplicantProperty(final Formula formula, final SortedSet<Literal> primeImplicant) {
        final FormulaFactory f = formula.factory();
        final MiniSat solver = MiniSat.miniSat(f);
        solver.add(formula.negate());
        assertThat(solver.sat(primeImplicant)).isEqualTo(Tristate.FALSE);
        for (final Literal lit : primeImplicant) {
            final SortedSet<Literal> reducedPrimeImplicant = new TreeSet<>(primeImplicant);
            reducedPrimeImplicant.remove(lit);
            assertThat(solver.sat(reducedPrimeImplicant)).isEqualTo(Tristate.TRUE);
        }
    }
}
