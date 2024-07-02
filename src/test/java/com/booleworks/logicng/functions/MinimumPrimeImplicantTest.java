// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.encodings.EncoderConfig;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

public class MinimumPrimeImplicantTest {

    final FormulaFactory f = FormulaFactory.caching();

    public MinimumPrimeImplicantTest() {
        f.putConfiguration(EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.PURE).build());
    }

    @Test
    public void testSimpleCases() throws ParserException {
        Formula formula = f.parse("a");
        SortedSet<Literal> pi = formula.apply(new MinimumPrimeImplicantFunction(f)).getResult();
        assertThat(pi).hasSize(1);
        isPrimeImplicant(formula, pi);

        formula = f.parse("a | b | c");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f)).getResult();
        assertThat(pi).hasSize(1);
        isPrimeImplicant(formula, pi);

        formula = f.parse("a & b & (~a|~b)");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f)).getResult();
        assertThat(pi).isNull();

        formula = f.parse("a & b & c");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f)).getResult();
        assertThat(pi).hasSize(3);
        isPrimeImplicant(formula, pi);

        formula = f.parse("a | b | ~c => e & d & f");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f)).getResult();
        assertThat(pi).hasSize(3);
        isPrimeImplicant(formula, pi);

        formula = f.parse("a | b | ~c <=> e & d & f");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f)).getResult();
        assertThat(pi).hasSize(4);
        isPrimeImplicant(formula, pi);

        formula = f.parse("(a | b | ~c <=> e & d & f) | (a | b | ~c => e & d & f)");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f)).getResult();
        assertThat(pi).hasSize(3);
        isPrimeImplicant(formula, pi);

        formula = f.parse("(a | b | ~c <=> e & d & f) | (a | b | ~c => e & d & f) | (a & b)");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f)).getResult();
        assertThat(pi).hasSize(2);
        isPrimeImplicant(formula, pi);

        formula = f.parse("(a | b | ~c <=> e & d & f) | (a | b | ~c => e & d & f) | (a & b) | (f => g)");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f)).getResult();
        assertThat(pi).hasSize(1);
        isPrimeImplicant(formula, pi);
    }

    @Test
    public void testSmallExamples() throws ParserException {
        Formula formula = f.parse("(~(v17 | v18) | ~v1494 & (v17 | v18)) & ~v687 => v686");
        SortedSet<Literal> pi = formula.apply(new MinimumPrimeImplicantFunction(f)).getResult();
        assertThat(pi).hasSize(1);
        isPrimeImplicant(formula, pi);

        formula = f.parse("(~(v17 | v18) | ~v1494 & (v17 | v18)) & v687 => ~v686");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f)).getResult();
        assertThat(pi).hasSize(1);
        isPrimeImplicant(formula, pi);

        formula = f.parse(
                "v173 + v174 + v451 + v258 + v317 + v259 + v452 + v453 + v175 + v176 + v177 + v178 + v179 + v180 + v181 + v182 + v183 + v102 + v103 + v104 + v105 = 1");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f)).getResult();
        assertThat(pi).hasSize(21);
        isPrimeImplicant(formula, pi);
    }

    @Test
    public void testMiddleExamples() throws IOException, ParserException {
        final Formula parsed = FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/formula1.txt");
        for (final Formula formula : parsed) {
            isPrimeImplicant(formula, formula.apply(new MinimumPrimeImplicantFunction(f)).getResult());
        }
    }

    @Test
    public void testLargeExamples() throws IOException, ParserException {
        final Formula parsed =
                FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/small_formulas.txt");
        for (final Formula formula : parsed) {
            isPrimeImplicant(formula, formula.apply(new MinimumPrimeImplicantFunction(f)).getResult());
        }
    }

    private void isPrimeImplicant(final Formula formula, final SortedSet<Literal> pi) {
        assertThat(f.implication(f.and(pi), formula).holds(new TautologyPredicate(f))).isTrue();
        for (final Literal literal : pi) {
            final TreeSet<Literal> newSet = new TreeSet<>(pi);
            newSet.remove(literal);
            if (!newSet.isEmpty()) {
                assertThat(f.implication(f.and(newSet), formula).holds(new TautologyPredicate(f))).isFalse();
            }
        }
    }

}
