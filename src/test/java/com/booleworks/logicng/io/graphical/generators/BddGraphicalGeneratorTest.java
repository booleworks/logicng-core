// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.graphical.generators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.graphical.GraphicalColor;
import com.booleworks.logicng.io.graphical.GraphicalDotWriter;
import com.booleworks.logicng.io.graphical.GraphicalEdgeStyle;
import com.booleworks.logicng.io.graphical.GraphicalMermaidWriter;
import com.booleworks.logicng.io.graphical.GraphicalNodeStyle;
import com.booleworks.logicng.io.graphical.GraphicalRepresentation;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.knowledgecompilation.bdds.BDD;
import com.booleworks.logicng.knowledgecompilation.bdds.BDDFactory;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class BddGraphicalGeneratorTest {

    @Test
    public void testFormulas() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final List<Variable> ordering =
                Arrays.asList(f.variable("A"), f.variable("B"), f.variable("C"), f.variable("D"));
        final BDDKernel kernel = new BDDKernel(f, ordering, 1000, 1000);
        testFiles("false", BDDFactory.build(f, p.parse("$false"), kernel), BddGraphicalGenerator.builder().build());
        testFiles("true", BDDFactory.build(f, p.parse("$true"), kernel), BddGraphicalGenerator.builder().build());
        testFiles("a", BDDFactory.build(f, p.parse("A"), kernel), BddGraphicalGenerator.builder().build());
        testFiles("not_a", BDDFactory.build(f, p.parse("~A"), kernel), BddGraphicalGenerator.builder().build());
        testFiles("impl", BDDFactory.build(f, p.parse("A => ~C"), kernel), BddGraphicalGenerator.builder().build());
        testFiles("equiv", BDDFactory.build(f, p.parse("A <=> ~C"), kernel), BddGraphicalGenerator.builder().build());
        testFiles("or", BDDFactory.build(f, p.parse("A | B | ~C"), kernel), BddGraphicalGenerator.builder().build());
        testFiles("and", BDDFactory.build(f, p.parse("A & B & ~C"), kernel), BddGraphicalGenerator.builder().build());
        testFiles("not", BDDFactory.build(f, p.parse("~(A & B & ~C)"), kernel),
                BddGraphicalGenerator.builder().build());
        testFiles("formula", BDDFactory.build(f, p.parse("(A => (B|~C)) & (B => C & D) & (D <=> A)"), kernel),
                BddGraphicalGenerator.builder().build());
    }

    @Test
    public void testFixedStyle() throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final List<Variable> ordering =
                Arrays.asList(f.variable("A"), f.variable("B"), f.variable("C"), f.variable("D"));
        final BDDKernel kernel = new BDDKernel(f, ordering, 1000, 1000);
        final BDD bdd = BDDFactory.build(f, p.parse("(A => (B|~C)) & (B => C & D) & (D <=> A)"), kernel);

        final BddGraphicalGenerator generator = BddGraphicalGenerator.builder()
                .falseNodeStyle(GraphicalNodeStyle.rectangle(GraphicalColor.PURPLE, GraphicalColor.WHITE,
                        GraphicalColor.PURPLE))
                .trueNodeStyle(
                        GraphicalNodeStyle.rectangle(GraphicalColor.CYAN, GraphicalColor.WHITE, GraphicalColor.CYAN))
                .negativeEdgeStyle(GraphicalEdgeStyle.dotted(GraphicalColor.PURPLE))
                .defaultEdgeStyle(GraphicalEdgeStyle.bold(GraphicalColor.CYAN))
                .defaultNodeStyle(
                        GraphicalNodeStyle.circle(GraphicalColor.ORANGE, GraphicalColor.BLACK, GraphicalColor.ORANGE))
                .backgroundColor(GraphicalColor.GRAY_LIGHT)
                .alignTerminals(true)
                .build();
        testFiles("formula-fixedStyle", bdd, generator);
    }

    @Test
    public void testDynamic() throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final List<Variable> ordering =
                Arrays.asList(f.variable("A"), f.variable("B"), f.variable("C"), f.variable("D"));
        final BDDKernel kernel = new BDDKernel(f, ordering, 1000, 1000);
        final BDD bdd = BDDFactory.build(f, p.parse("(A => (B|~C)) & (B => C & D) & (D <=> A)"), kernel);

        final BddGraphicalGenerator generator = BddGraphicalGenerator.builder()
                .negativeEdgeMapper(new MyNegEdgeMapper(kernel))
                .nodeStyleMapper(new MyStyleMapper(kernel))
                .labelMapper(new MyLabelMapper(kernel))
                .edgeMapper(new MyPosEdgeMapper(kernel))
                .build();
        testFiles("formula-dynamic", bdd, generator);
    }

    private void testFiles(final String fileName, final BDD bdd, final BddGraphicalGenerator generator)
            throws IOException {
        final GraphicalRepresentation representation = generator.translate(bdd);
        representation.write("src/test/resources/writers/temp/" + fileName + "_bdd.dot", GraphicalDotWriter.get());
        representation.write("src/test/resources/writers/temp/" + fileName + "_bdd.txt", GraphicalMermaidWriter.get());
        final File expectedT = new File("src/test/resources/writers/bdd/" + fileName + "_bdd.dot");
        final File tempT = new File("src/test/resources/writers/temp/" + fileName + "_bdd.dot");
        assertThat(contentOf(tempT)).isEqualTo(contentOf(expectedT));
    }

    private static class MyStyleMapper extends BddNodeStyleMapper {

        final GraphicalNodeStyle falseStyle =
                GraphicalNodeStyle.rectangle(GraphicalColor.RED, GraphicalColor.RED, GraphicalColor.WHITE);
        final GraphicalNodeStyle trueStyle =
                GraphicalNodeStyle.rectangle(GraphicalColor.GREEN, GraphicalColor.GREEN, GraphicalColor.WHITE);
        final GraphicalNodeStyle bStyle =
                GraphicalNodeStyle.circle(GraphicalColor.ORANGE, GraphicalColor.BLACK, GraphicalColor.ORANGE);
        final GraphicalNodeStyle otherStyle =
                GraphicalNodeStyle.circle(GraphicalColor.CYAN, GraphicalColor.WHITE, GraphicalColor.CYAN);

        public MyStyleMapper(final BDDKernel kernel) {
            super(kernel);
        }

        @Override
        public GraphicalNodeStyle computeStyle(final Integer index) {
            if (isFalse(index)) {
                return falseStyle;
            } else if (isTrue(index)) {
                return trueStyle;
            } else {
                final Variable variable = variable(index);
                if (variable.name().equals("B")) {
                    return bStyle;
                } else {
                    return otherStyle;
                }
            }
        }
    }

    private static class MyLabelMapper extends BddLabelMapper {

        public MyLabelMapper(final BDDKernel kernel) {
            super(kernel);
        }

        @Override
        public String computeLabel(final Integer index) {
            if (isFalse(index)) {
                return "falsch";
            } else if (isTrue(index)) {
                return "wahr";
            } else {
                final Variable variable = variable(index);
                if (variable.name().equals("B")) {
                    return "B!!";
                } else {
                    return variable.name();
                }
            }
        }
    }

    private static class MyPosEdgeMapper extends BddEdgeStyleMapper {

        final GraphicalEdgeStyle style1 = GraphicalEdgeStyle.solid(GraphicalColor.GREEN);
        final GraphicalEdgeStyle style2 = GraphicalEdgeStyle.bold(GraphicalColor.GREEN);

        public MyPosEdgeMapper(final BDDKernel kernel) {
            super(kernel);
        }

        @Override
        public GraphicalEdgeStyle computeStyle(final Integer source, final Integer destination) {
            return variable(source).name().equals("B") ? style2 : style1;
        }
    }

    private static class MyNegEdgeMapper extends BddEdgeStyleMapper {

        final GraphicalEdgeStyle style1 = GraphicalEdgeStyle.dotted(GraphicalColor.RED);
        final GraphicalEdgeStyle style2 = GraphicalEdgeStyle.bold(GraphicalColor.RED);

        public MyNegEdgeMapper(final BDDKernel kernel) {
            super(kernel);
        }

        @Override
        public GraphicalEdgeStyle computeStyle(final Integer source, final Integer destination) {
            return variable(source).name().equals("B") ? style2 : style1;
        }
    }
}
