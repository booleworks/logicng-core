// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import static com.booleworks.logicng.TestWithExampleFormulas.parse;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_INC_WBO;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_LINEAR_SU;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_LINEAR_US;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_MSU3;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_OLL;
import static com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig.CONFIG_WBO;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.encodings.EncoderConfig;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class MinimumPrimeImplicantTest {

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

    final FormulaFactory f = FormulaFactory.caching();

    public MinimumPrimeImplicantTest() {
        f.putConfiguration(EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.PURE).build());
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testSimpleCases(final MaxSatConfig config) {
        Formula formula = parse(f, "a");
        SortedSet<Literal> pi = formula.apply(new MinimumPrimeImplicantFunction(f, config));
        assertThat(pi).hasSize(1);
        isPrimeImplicant(formula, pi);

        formula = parse(f, "a | b | c");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f, config));
        assertThat(pi).hasSize(1);
        isPrimeImplicant(formula, pi);

        formula = parse(f, "a & b & c");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f, config));
        assertThat(pi).hasSize(3);
        isPrimeImplicant(formula, pi);

        formula = parse(f, "a | b | ~c => e & d & f");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f, config));
        assertThat(pi).hasSize(3);
        isPrimeImplicant(formula, pi);

        formula = parse(f, "a | b | ~c <=> e & d & f");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f, config));
        assertThat(pi).hasSize(4);
        isPrimeImplicant(formula, pi);

        formula = parse(f, "(a | b | ~c <=> e & d & f) | (a | b | ~c => e & d & f)");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f, config));
        assertThat(pi).hasSize(3);
        isPrimeImplicant(formula, pi);

        formula = parse(f, "(a | b | ~c <=> e & d & f) | (a | b | ~c => e & d & f) | (a & b)");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f, config));
        assertThat(pi).hasSize(2);
        isPrimeImplicant(formula, pi);

        formula = parse(f, "(a | b | ~c <=> e & d & f) | (a | b | ~c => e & d & f) | (a & b) | (f => g)");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f, config));
        assertThat(pi).hasSize(1);
        isPrimeImplicant(formula, pi);
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testSmallExamples(final MaxSatConfig config) {
        Formula formula = parse(f, "(~(v17 | v18) | ~v1494 & (v17 | v18)) & ~v687 => v686");
        SortedSet<Literal> pi = formula.apply(new MinimumPrimeImplicantFunction(f, config));
        assertThat(pi).hasSize(1);
        isPrimeImplicant(formula, pi);

        formula = parse(f, "(~(v17 | v18) | ~v1494 & (v17 | v18)) & v687 => ~v686");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f, config));
        assertThat(pi).hasSize(1);
        isPrimeImplicant(formula, pi);

        formula = parse(f, "v173 + v174 + v451 + v258 + v317 + v259 + v452 + v453 + v175 + v176 + v177 + v178 + " +
                "v179 + v180 + v181 + v182 + v183 + v102 + v103 + v104 + v105 = 1");
        pi = formula.apply(new MinimumPrimeImplicantFunction(f, config));
        assertThat(pi).hasSize(21);
        isPrimeImplicant(formula, pi);
    }

    @ParameterizedTest
    @MethodSource("configs")
    public void testMiddleExamples(final MaxSatConfig config) throws IOException, ParserException {
        final Formula parsed = FormulaReader.readFormula(f, "../test_files/formulas/formula1.txt");
        for (final Formula formula : parsed) {
            isPrimeImplicant(formula, formula.apply(new MinimumPrimeImplicantFunction(f, config)));
        }
    }

    @LongRunningTag
    @ParameterizedTest
    @MethodSource("configs")
    public void testLargeExamples(final MaxSatConfig config) throws IOException, ParserException {
        final Formula parsed =
                FormulaReader.readFormula(f, "../test_files/formulas/small_formulas.txt");
        for (final Formula formula : parsed) {
            isPrimeImplicant(formula, formula.apply(new MinimumPrimeImplicantFunction(f, config)));
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
