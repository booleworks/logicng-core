// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.mus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.BoundedSatHandler;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MUSGenerationTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final PigeonHoleGenerator pg = new PigeonHoleGenerator(f);

    private final List<StandardProposition> pg3;
    private final List<StandardProposition> pg4;
    private final List<StandardProposition> pg5;
    private final List<StandardProposition> pg6;
    private final List<StandardProposition> pg7;
    private final List<StandardProposition> file1;
    private final List<StandardProposition> file2;
    private final List<StandardProposition> file3;
    private final List<StandardProposition> file4;

    public MUSGenerationTest() throws IOException {
        pg3 = generatePGPropositions(3);
        pg4 = generatePGPropositions(4);
        pg5 = generatePGPropositions(5);
        pg6 = generatePGPropositions(6);
        pg7 = generatePGPropositions(7);
        file1 = readDimacs("src/test/resources/sat/3col40_5_10.shuffled.cnf");
        file2 = readDimacs("src/test/resources/sat/x1_16.shuffled.cnf");
        file3 = readDimacs("src/test/resources/sat/grid_10_20.shuffled.cnf");
        file4 = readDimacs("src/test/resources/sat/ca032.shuffled.cnf");
    }

    @Test
    public void testNoFormulas() {
        final MUSGeneration mus = new MUSGeneration();
        assertThatThrownBy(() -> mus.computeMUS(f, Collections.emptyList(), MUSConfig.builder().build())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSATFormulaSetDeletionBasedMUS() {
        final MUSGeneration mus = new MUSGeneration();
        final StandardProposition proposition = new StandardProposition(f.variable("a"));
        assertThatThrownBy(() -> mus.computeMUS(f, Collections.singletonList(proposition),
                MUSConfig.builder().algorithm(MUSConfig.Algorithm.DELETION).build())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @LongRunningTag
    public void testDeletionBasedMUS() {
        final MUSGeneration mus = new MUSGeneration();
        final UNSATCore<StandardProposition> mus1 = mus.computeMUS(f, pg3);
        final UNSATCore<StandardProposition> mus2 = mus.computeMUS(f, pg4);
        final UNSATCore<StandardProposition> mus3 = mus.computeMUS(f, pg5);
        final UNSATCore<StandardProposition> mus4 = mus.computeMUS(f, pg6);
        final UNSATCore<StandardProposition> mus5 = mus.computeMUS(f, pg7);
        final UNSATCore<StandardProposition> mus6 = mus.computeMUS(f, file1);
        final UNSATCore<StandardProposition> mus7 = mus.computeMUS(f, file2);
        final UNSATCore<StandardProposition> mus8 = mus.computeMUS(f, file3);
        final UNSATCore<StandardProposition> mus9 = mus.computeMUS(f, file4);
        testMUS(pg3, mus1);
        testMUS(pg4, mus2);
        testMUS(pg5, mus3);
        testMUS(pg6, mus4);
        testMUS(pg7, mus5);
        testMUS(file1, mus6);
        testMUS(file2, mus7);
        testMUS(file3, mus8);
        testMUS(file4, mus9);
    }

    @Test
    public void testSATFormulaSetPlainInsertionBasedMUS() {
        final MUSGeneration mus = new MUSGeneration();
        final StandardProposition proposition = new StandardProposition(f.variable("a"));
        assertThatThrownBy(() -> mus.computeMUS(f, Collections.singletonList(proposition),
                MUSConfig.builder().algorithm(MUSConfig.Algorithm.PLAIN_INSERTION).build())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testPlainInsertionBasedMUS() {
        final MUSGeneration mus = new MUSGeneration();
        final MUSConfig config = MUSConfig.builder().algorithm(MUSConfig.Algorithm.PLAIN_INSERTION).build();
        final UNSATCore<StandardProposition> mus1 = mus.computeMUS(f, pg3, config);
        final UNSATCore<StandardProposition> mus2 = mus.computeMUS(f, pg4, config);
        final UNSATCore<StandardProposition> mus3 = mus.computeMUS(f, pg5, config);
        final UNSATCore<StandardProposition> mus6 = mus.computeMUS(f, file1, config);
        final UNSATCore<StandardProposition> mus7 = mus.computeMUS(f, file2, config);
        testMUS(pg3, mus1);
        testMUS(pg4, mus2);
        testMUS(pg5, mus3);
        testMUS(file1, mus6);
        testMUS(file2, mus7);
    }

    @Test
    public void testDeletionBasedCancellationPoints() throws IOException {
        final MUSGeneration mus = new MUSGeneration();
        final List<StandardProposition> propositions = DimacsReader.readCNF(f, "src/test/resources/sat/too_large_gr_rcs_w5.shuffled.cnf").stream()
                .map(StandardProposition::new)
                .collect(Collectors.toList());
        for (int numStarts = 0; numStarts < 20; numStarts++) {
            final SATHandler handler = new BoundedSatHandler(numStarts);
            final MUSConfig config = MUSConfig.builder().handler(handler).algorithm(MUSConfig.Algorithm.PLAIN_INSERTION).build();

            final UNSATCore<StandardProposition> result = mus.computeMUS(f, propositions, config);

            assertThat(handler.aborted()).isTrue();
            assertThat(result).isNull();
        }
    }

    @Test
    public void testCancellationPoints() throws IOException {
        final MUSGeneration mus = new MUSGeneration();
        final List<StandardProposition> propositions = DimacsReader.readCNF(f, "src/test/resources/sat/unsat/bf0432-007.cnf").stream()
                .map(StandardProposition::new)
                .collect(Collectors.toList());
        final List<MUSConfig.Algorithm> algorithms = Arrays.asList(MUSConfig.Algorithm.DELETION, MUSConfig.Algorithm.PLAIN_INSERTION);
        for (final MUSConfig.Algorithm algorithm : algorithms) {
            for (int numStarts = 0; numStarts < 10; numStarts++) {
                final SATHandler handler = new BoundedSatHandler(numStarts);
                final MUSConfig config = MUSConfig.builder().handler(handler).algorithm(algorithm).build();

                final UNSATCore<StandardProposition> result = mus.computeMUS(f, propositions, config);

                assertThat(handler.aborted()).isTrue();
                assertThat(result).isNull();
            }
        }
    }

    private List<StandardProposition> generatePGPropositions(final int n) {
        final List<StandardProposition> result = new ArrayList<>();
        final Formula pgf = pg.generate(n);
        for (final Formula f : pgf) {
            result.add(new StandardProposition(f));
        }
        return result;
    }

    private List<StandardProposition> readDimacs(final String fileName) throws IOException {
        final List<StandardProposition> result = new ArrayList<>();
        final BufferedReader reader = new BufferedReader(new FileReader(fileName));
        while (reader.ready()) {
            final String line = reader.readLine();
            if (!line.startsWith("p") && !line.startsWith("c")) {
                final String[] tokens = line.split("\\s");
                final List<Literal> clause = new ArrayList<>();
                for (int i = 0; i < tokens.length - 1; i++) {
                    final int lit = Integer.parseInt(tokens[i]);
                    clause.add(lit < 0 ? f.literal("v" + (-lit), false) : f.literal("v" + lit, true));
                }
                result.add(new StandardProposition(f.clause(clause)));
            }
        }
        return result;
    }

    private void testMUS(final List<StandardProposition> original, final UNSATCore<StandardProposition> mus) {
        assertThat(mus.isMUS()).isTrue();
        assertThat(mus.propositions().size() <= original.size()).isTrue();
        final SATSolver miniSat = SATSolver.newSolver(f);
        for (final StandardProposition p : mus.propositions()) {
            assertThat(original.contains(p)).isTrue();
            Assertions.assertThat(miniSat.sat()).isTrue();
            miniSat.add(p);
        }
        Assertions.assertThat(miniSat.sat()).isFalse();
    }
}
