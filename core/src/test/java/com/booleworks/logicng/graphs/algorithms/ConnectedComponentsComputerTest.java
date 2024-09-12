// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.algorithms;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.encodings.EncoderConfig;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.graphs.datastructures.Graph;
import com.booleworks.logicng.graphs.datastructures.GraphTest;
import com.booleworks.logicng.graphs.datastructures.Node;
import com.booleworks.logicng.graphs.generators.ConstraintGraphGenerator;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.transformations.cnf.CNFFactorization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConnectedComponentsComputerTest {

    @Test
    public void graph30Test() throws IOException {
        final Graph<Long> g = GraphTest.getLongGraph("30");
        for (long i = 0; i < 30; i++) {
            g.node(i);
        }

        assertThat(g.nodes().size()).isEqualTo(30);

        final Set<Set<Node<Long>>> ccs = ConnectedComponentsComputation.compute(g);
        assertThat(ccs.size()).isEqualTo(7);
        int bigOnes = 0;
        for (final Set<Node<Long>> cc : ccs) {
            if (cc.size() > 1) {
                assertThat(bigOnes < 4).isTrue();
                bigOnes++;
                assertThat(cc.size() > 4).isTrue();
            } else {
                assertThat(cc.size()).isEqualTo(1);
            }
            int equals = 0;
            for (final Set<Node<Long>> cc2 : ccs) {
                final Set<Node<Long>> cut = new HashSet<>(cc2);
                cut.retainAll(cc);
                if (cut.size() == cc.size()) {
                    equals++;
                } else {
                    assertThat(cut.isEmpty()).isTrue();
                }
            }
            assertThat(equals).isEqualTo(1);
        }
    }

    @Test
    public void graph50Test() throws IOException {
        final Graph<Long> g = GraphTest.getLongGraph("50");
        for (long i = 0; i < 60; i++) {
            g.node(i);
        }

        assertThat(g.nodes().size()).isEqualTo(60);

        final Set<Set<Node<Long>>> ccs = ConnectedComponentsComputation.compute(g);
        assertThat(ccs.size()).isEqualTo(11);
        boolean bigOneFound = false;
        for (final Set<Node<Long>> cc : ccs) {
            if (cc.size() > 1) {
                assertThat(bigOneFound).isFalse();
                bigOneFound = true;
                assertThat(cc.size()).isEqualTo(50);
            } else {
                assertThat(cc.size()).isEqualTo(1);
            }
            int equals = 0;
            for (final Set<Node<Long>> cc2 : ccs) {
                final Set<Node<Long>> cut = new HashSet<>(cc2);
                cut.retainAll(cc);
                if (cut.size() == cc.size()) {
                    equals++;
                } else {
                    assertThat(cut.isEmpty()).isTrue();
                }
            }
            assertThat(equals).isEqualTo(1);
        }
    }

    @Test
    public void testFormulaSplit() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        f.putConfiguration(EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.PURE).build());
        final Formula parsed = FormulaReader.readFormula(f, "src/test/resources/formulas/formula1.txt");
        final List<Formula> formulas = new ArrayList<>();
        final List<Formula> originalFormulas = new ArrayList<>();
        for (final Formula formula : parsed) {
            originalFormulas.add(formula);
            if (formula instanceof PBConstraint) {
                formulas.add(formula);
            } else {
                formulas.add(formula.transform(new CNFFactorization(f)));
            }
        }
        final Graph<Variable> constraintGraph = ConstraintGraphGenerator.generateFromFormulas(f, formulas);
        final Set<Set<Node<Variable>>> ccs = ConnectedComponentsComputation.compute(constraintGraph);
        final List<List<Formula>> split =
                ConnectedComponentsComputation.splitFormulasByComponent(f, originalFormulas, ccs);
        assertThat(split).hasSize(4);
        assertThat(split.get(0)).hasSize(1899);
        assertThat(split.get(1)).hasSize(3);
        assertThat(split.get(2)).hasSize(3);
        assertThat(split.get(3)).hasSize(3);
    }
}
