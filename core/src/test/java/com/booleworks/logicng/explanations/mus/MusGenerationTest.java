// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.mus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.explanations.UnsatCore;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.BoundedSatHandler;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MusGenerationTest {

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

    public MusGenerationTest() throws IOException {
        pg3 = generatePGPropositions(3);
        pg4 = generatePGPropositions(4);
        pg5 = generatePGPropositions(5);
        pg6 = generatePGPropositions(6);
        pg7 = generatePGPropositions(7);
        file1 = readDimacs("../test_files/sat/3col40_5_10.shuffled.cnf");
        file2 = readDimacs("../test_files/sat/x1_16.shuffled.cnf");
        file3 = readDimacs("../test_files/sat/grid_10_20.shuffled.cnf");
        file4 = readDimacs("../test_files/sat/ca032.shuffled.cnf");
    }

    @Test
    public void testNoFormulas() {
        final MusGeneration mus = new MusGeneration();
        assertThatThrownBy(() -> mus.computeMus(f, Collections.emptyList(), MusConfig.builder().build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSATFormulaSetDeletionBasedMUS() {
        final MusGeneration mus = new MusGeneration();
        final StandardProposition proposition = new StandardProposition(f.variable("a"));
        assertThatThrownBy(() -> mus.computeMus(f, Collections.singletonList(proposition),
                MusConfig.builder().algorithm(MusConfig.Algorithm.DELETION).build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @LongRunningTag
    public void testDeletionBasedMUS() {
        final MusGeneration mus = new MusGeneration();
        final UnsatCore<StandardProposition> mus1 = mus.computeMus(f, pg3);
        final UnsatCore<StandardProposition> mus2 = mus.computeMus(f, pg4);
        final UnsatCore<StandardProposition> mus3 = mus.computeMus(f, pg5);
        final UnsatCore<StandardProposition> mus4 = mus.computeMus(f, pg6);
        final UnsatCore<StandardProposition> mus5 = mus.computeMus(f, pg7);
        final UnsatCore<StandardProposition> mus6 = mus.computeMus(f, file1);
        final UnsatCore<StandardProposition> mus7 = mus.computeMus(f, file2);
        final UnsatCore<StandardProposition> mus8 = mus.computeMus(f, file3);
        final UnsatCore<StandardProposition> mus9 = mus.computeMus(f, file4);
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
        final MusGeneration mus = new MusGeneration();
        final StandardProposition proposition = new StandardProposition(f.variable("a"));
        assertThatThrownBy(() -> mus.computeMus(f, Collections.singletonList(proposition),
                MusConfig.builder().algorithm(MusConfig.Algorithm.PLAIN_INSERTION).build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testPlainInsertionBasedMUS() {
        final MusGeneration mus = new MusGeneration();
        final MusConfig config = MusConfig.builder().algorithm(MusConfig.Algorithm.PLAIN_INSERTION).build();
        final UnsatCore<StandardProposition> mus1 = mus.computeMus(f, pg3, config);
        final UnsatCore<StandardProposition> mus2 = mus.computeMus(f, pg4, config);
        final UnsatCore<StandardProposition> mus3 = mus.computeMus(f, pg5, config);
        final UnsatCore<StandardProposition> mus6 = mus.computeMus(f, file1, config);
        final UnsatCore<StandardProposition> mus7 = mus.computeMus(f, file2, config);
        testMUS(pg3, mus1);
        testMUS(pg4, mus2);
        testMUS(pg5, mus3);
        testMUS(file1, mus6);
        testMUS(file2, mus7);
    }

    @Test
    public void testDeletionBasedCancellationPoints() throws IOException {
        final MusGeneration mus = new MusGeneration();
        final List<StandardProposition> propositions =
                DimacsReader.readCNF(f, "../test_files/sat/too_large_gr_rcs_w5.shuffled.cnf").stream()
                        .map(StandardProposition::new)
                        .collect(Collectors.toList());
        for (int numStarts = 0; numStarts < 20; numStarts++) {
            final BoundedSatHandler handler = new BoundedSatHandler(numStarts);
            final MusConfig config = MusConfig.builder().algorithm(MusConfig.Algorithm.PLAIN_INSERTION).build();
            final LngResult<UnsatCore<StandardProposition>> result = mus.computeMus(f, propositions, config, handler);
            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Test
    public void testCancellationPoints() throws IOException {
        final MusGeneration mus = new MusGeneration();
        final List<StandardProposition> propositions =
                DimacsReader.readCNF(f, "../test_files/sat/unsat/bf0432-007.cnf").stream()
                        .map(StandardProposition::new)
                        .collect(Collectors.toList());
        final List<MusConfig.Algorithm> algorithms =
                Arrays.asList(MusConfig.Algorithm.DELETION, MusConfig.Algorithm.PLAIN_INSERTION);
        for (final MusConfig.Algorithm algorithm : algorithms) {
            for (int numStarts = 0; numStarts < 10; numStarts++) {
                final ComputationHandler handler = new BoundedSatHandler(numStarts);
                final MusConfig config = MusConfig.builder().algorithm(algorithm).build();
                final LngResult<UnsatCore<StandardProposition>> result = mus.computeMus(f, propositions, config, handler);
                assertThat(result.isSuccess()).isFalse();
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

    private void testMUS(final List<StandardProposition> original, final UnsatCore<StandardProposition> mus) {
        assertThat(mus.isMus()).isTrue();
        assertThat(mus.getPropositions().size() <= original.size()).isTrue();
        final SatSolver solver = SatSolver.newSolver(f);
        for (final StandardProposition p : mus.getPropositions()) {
            assertThat(original.contains(p)).isTrue();
            assertThat(solver.sat()).isTrue();
            solver.add(p);
        }
        assertThat(solver.sat()).isFalse();
    }
}
