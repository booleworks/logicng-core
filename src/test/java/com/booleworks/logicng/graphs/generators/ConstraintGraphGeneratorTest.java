// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.generators;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.encodings.EncoderConfig;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.graphs.algorithms.ConnectedComponentsComputation;
import com.booleworks.logicng.graphs.datastructures.Graph;
import com.booleworks.logicng.graphs.datastructures.Node;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.transformations.cnf.CNFFactorization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConstraintGraphGeneratorTest {

    @Test
    public void testSimple() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(ConstraintGraphGenerator.generateFromFormulas(f, List.of(f.falsum())).nodes()).isEmpty();
        assertThat(ConstraintGraphGenerator.generateFromFormulas(f, f.verum()).nodes()).isEmpty();
        Graph<Variable> graph = ConstraintGraphGenerator.generateFromFormulas(f, p.parse("a"));
        assertThat(graph.nodes()).containsExactly(graph.node(f.variable("a")));
        graph = ConstraintGraphGenerator.generateFromFormulas(f, p.parse("~a"));
        assertThat(graph.nodes()).containsExactly(graph.node(f.variable("a")));
    }

    @Test
    public void testOr() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final Graph<Variable> expected = new Graph<>();
        final Node<Variable> a = expected.node(f.variable("a"));
        final Node<Variable> b = expected.node(f.variable("b"));
        final Node<Variable> c = expected.node(f.variable("c"));
        expected.connect(a, b);
        expected.connect(a, c);
        expected.connect(b, c);
        assertThat(ConstraintGraphGenerator.generateFromFormulas(f, p.parse("a | ~b | c")).toString())
                .isEqualTo(expected.toString());
    }

    @Test
    public void testCC() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final Graph<Variable> expected = new Graph<>();
        final Node<Variable> a = expected.node(f.variable("a"));
        final Node<Variable> b = expected.node(f.variable("b"));
        final Node<Variable> c = expected.node(f.variable("c"));
        expected.connect(a, b);
        expected.connect(a, c);
        expected.connect(b, c);
        assertThat(ConstraintGraphGenerator.generateFromFormulas(f, p.parse("a + b + c <= 1")).toString())
                .isEqualTo(expected.toString());
    }

    @Test
    public void testCnf() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final Graph<Variable> expected = new Graph<>();
        final Node<Variable> a = expected.node(f.variable("a"));
        final Node<Variable> b = expected.node(f.variable("b"));
        final Node<Variable> c = expected.node(f.variable("c"));
        final Node<Variable> d = expected.node(f.variable("d"));
        final Node<Variable> e = expected.node(f.variable("e"));
        expected.node(f.variable("g"));
        expected.connect(a, b);
        expected.connect(a, c);
        expected.connect(b, c);
        expected.connect(d, a);
        expected.connect(d, e);
        assertThat(ConstraintGraphGenerator.generateFromFormulas(f,
                p.parse("a | ~b | c"),
                p.parse("d | ~a"),
                p.parse("d + e = 1"),
                p.parse("g")).toString())
                .isEqualTo(expected.toString());
    }

    @Test
    public void testRealExample() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        f.putConfiguration(EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.PURE).build());
        final Formula parsed = FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/formula1.txt");
        final List<Formula> formulas = new ArrayList<>();
        for (final Formula formula : parsed) {
            if (formula instanceof PBConstraint) {
                formulas.add(formula);
            } else {
                formulas.add(formula.transform(new CNFFactorization(f)));
            }
        }
        final Graph<Variable> constraintGraph = ConstraintGraphGenerator.generateFromFormulas(f, formulas);
        final Set<Set<Node<Variable>>> ccs = ConnectedComponentsComputation.compute(constraintGraph);
        assertThat(ccs).hasSize(4);
    }
}
