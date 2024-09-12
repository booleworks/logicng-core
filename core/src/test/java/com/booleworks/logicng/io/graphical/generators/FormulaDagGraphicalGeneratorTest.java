// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.graphical.generators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.io.graphical.GraphicalColor;
import com.booleworks.logicng.io.graphical.GraphicalDotWriter;
import com.booleworks.logicng.io.graphical.GraphicalEdgeStyle;
import com.booleworks.logicng.io.graphical.GraphicalMermaidWriter;
import com.booleworks.logicng.io.graphical.GraphicalNodeStyle;
import com.booleworks.logicng.io.graphical.GraphicalRepresentation;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class FormulaDagGraphicalGeneratorTest {
    private FormulaFactory f;
    private PropositionalParser p;

    @BeforeEach
    public void init() {
        f = FormulaFactory.caching();
        p = new PropositionalParser(f);
    }

    @Test
    public void testConstants() throws IOException {
        testFiles("false", f.falsum(), FormulaDagGraphicalGenerator.builder().build());
        testFiles("true", f.verum(), FormulaDagGraphicalGenerator.builder().build());
    }

    @Test
    public void testLiterals() throws IOException {
        testFiles("x", f.variable("x"), FormulaDagGraphicalGenerator.builder().build());
        testFiles("not_x", f.literal("x", false), FormulaDagGraphicalGenerator.builder().build());
    }

    @Test
    public void testFormulas() throws IOException, ParserException {
        final Formula f1 = p.parse("(a & b) <=> (~c => (x | z))");
        final Formula f2 = p.parse("a & b | b & ~c");
        final Formula f3 = p.parse("(a & b) <=> (~c => (a | b))");
        final Formula f4 = p.parse("~(a & b) | b & ~c");
        final Formula f5 = p.parse("a | ~b | (2*a + 3*~b + 4*c <= 23)");
        testFiles("f1", f1, FormulaDagGraphicalGenerator.builder().build());
        testFiles("f2", f2, FormulaDagGraphicalGenerator.builder().build());
        testFiles("f3", f3, FormulaDagGraphicalGenerator.builder().build());
        testFiles("f4", f4, FormulaDagGraphicalGenerator.builder().build());
        testFiles("f5", f5, FormulaDagGraphicalGenerator.builder().build());
    }

    @Test
    public void testDuplicateFormulaParts() throws ParserException, IOException {
        final Formula f6 = p.parse("(a & b) | (c & ~(a & b))");
        testFiles("f6", f6, FormulaDagGraphicalGenerator.builder().build());
        final Formula f7 = p.parse("(c & d) | (a & b) | ((c & d) <=> (a & b))");
        testFiles("f7", f7, FormulaDagGraphicalGenerator.builder().build());
    }

    @Test
    public void testFixedStyle() throws ParserException, IOException {
        final Formula f8 = p.parse("(A <=> B & (~A | C | X)) => a + b + c <= 2");
        final FormulaDagGraphicalGenerator generator = FormulaDagGraphicalGenerator.builder()
                .backgroundColor("#020202")
                .defaultEdgeStyle(GraphicalEdgeStyle.bold(GraphicalColor.CYAN))
                .defaultNodeStyle(
                        GraphicalNodeStyle.circle(GraphicalColor.BLUE, GraphicalColor.WHITE, GraphicalColor.BLUE))
                .alignTerminals(true)
                .build();
        testFiles("f8", f8, generator);
    }

    @Test
    public void testDynamicStyle() throws ParserException, IOException {
        final Formula f9 = p.parse("(A <=> B & (~A | C | X)) => a + b + c <= 2 & (~a | d => X & ~B)");

        final GraphicalNodeStyle style1 = GraphicalNodeStyle.rectangle(GraphicalColor.GRAY_DARK,
                GraphicalColor.GRAY_DARK, GraphicalColor.GRAY_LIGHT);
        final GraphicalNodeStyle style2 =
                GraphicalNodeStyle.circle(GraphicalColor.YELLOW, GraphicalColor.BLACK, GraphicalColor.YELLOW);
        final GraphicalNodeStyle style3 =
                GraphicalNodeStyle.circle(GraphicalColor.TURQUOISE, GraphicalColor.WHITE, GraphicalColor.TURQUOISE);
        final GraphicalNodeStyle style4 =
                GraphicalNodeStyle.ellipse(GraphicalColor.BLACK, GraphicalColor.BLACK, GraphicalColor.WHITE);

        final NodeStyleMapper<Formula> mapper = (formula) -> {
            if (formula.type() == FType.PBC) {
                return style1;
            } else if (formula.type() == FType.LITERAL) {
                final Literal lit = (Literal) formula;
                return Character.isLowerCase(lit.name().charAt(0)) ? style2 : style3;
            } else {
                return style4;
            }
        };

        final FormulaDagGraphicalGenerator generator = FormulaDagGraphicalGenerator.builder()
                .defaultEdgeStyle(GraphicalEdgeStyle.solid(GraphicalColor.PURPLE))
                .nodeStyleMapper(mapper)
                .labelMapper((f) -> "Formula Type: " + f.type())
                .build();

        testFiles("f9", f9, generator);
    }

    @Test
    public void testEdgeMapper() throws ParserException, IOException {
        final Formula f10 = p.parse("(A <=> B & (~A | C | X)) => a + b + c <= 2 & (~a | d => X & ~B)");

        final GraphicalEdgeStyle style1 = GraphicalEdgeStyle.dotted(GraphicalColor.GRAY_DARK);

        final EdgeStyleMapper<Formula> edgeMapper = (source, dest) -> {
            if (source.type() == FType.PBC) {
                return style1;
            } else {
                return GraphicalEdgeStyle.noStyle();
            }
        };

        final FormulaDagGraphicalGenerator generator = FormulaDagGraphicalGenerator.builder()
                .defaultEdgeStyle(GraphicalEdgeStyle.solid(GraphicalColor.PURPLE))
                .edgeMapper(edgeMapper)
                .build();

        testFiles("f10", f10, generator);
    }

    private void testFiles(final String fileName, final Formula formula, final FormulaDagGraphicalGenerator generator)
            throws IOException {
        final GraphicalRepresentation representation = generator.translate(formula);
        representation.write("src/test/resources/writers/temp/" + fileName + ".dot", GraphicalDotWriter.get());
        representation.write("src/test/resources/writers/temp/" + fileName + ".txt", GraphicalMermaidWriter.get());

        final File expectedDot = new File("src/test/resources/writers/formulas-dag/" + fileName + ".dot");
        final File tempDot = new File("src/test/resources/writers/temp/" + fileName + ".dot");
        assertThat(contentOf(tempDot)).isEqualTo(contentOf(expectedDot));

        final File expectedMermaid = new File("src/test/resources/writers/formulas-dag/" + fileName + ".txt");
        final File tempMermaid = new File("src/test/resources/writers/temp/" + fileName + ".txt");
        assertThat(contentOf(tempMermaid)).isEqualTo(contentOf(expectedMermaid));
    }
}
