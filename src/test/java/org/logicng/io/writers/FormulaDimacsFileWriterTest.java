// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.io.writers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.io.parsers.PseudoBooleanParser;
import org.logicng.transformations.cnf.CNFConfig;
import org.logicng.transformations.cnf.CNFEncoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FormulaDimacsFileWriterTest extends TestWithFormulaContext {

    private final FormulaFactory f = FormulaFactory.caching();
    private final CNFConfig config = CNFConfig.builder().algorithm(CNFConfig.Algorithm.FACTORIZATION).build();
    private final PropositionalParser p = new PropositionalParser(f);
    private final PseudoBooleanParser pp = new PseudoBooleanParser(f);

    @Test
    public void testNonCNF() {
        assertThatThrownBy(() -> FormulaDimacsFileWriter.write("non-cnf", FormulaFactory.nonCaching().parse("a => b"), false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConstants() throws IOException {
        testFiles("false", f.falsum());
        testFiles("true", f.verum());
    }

    @Test
    public void testLiterals() throws IOException {
        testFiles("x", f.variable("x"));
        testFiles("not_x", f.literal("x", false));
    }

    @Test
    public void testFormulas() throws IOException, ParserException {
        final Formula f1 = CNFEncoder.encode(p.parse("(a & b) <=> (~c => (x | z))"), config);
        final Formula f2 = CNFEncoder.encode(p.parse("a & b | b & ~c"), config);
        final Formula f3 = CNFEncoder.encode(p.parse("(a & b) <=> (~c => (a | b))"), config);
        final Formula f4 = CNFEncoder.encode(p.parse("~(a & b) | b & ~c"), config);
        final Formula f5 = CNFEncoder.encode(pp.parse("a | ~b | (2*a + 3*~b + 4*c <= 4)"), config);
        testFiles("f1", f1);
        testFiles("f2", f2);
        testFiles("f3", f3);
        testFiles("f4", f4);
        testFiles("f5", f5);
    }

    @Test
    public void testDuplicateFormulaParts() throws ParserException, IOException {
        final Formula f6 = CNFEncoder.encode(p.parse("(a & b) | (c & ~(a & b))"), config);
        testFiles("f6", f6);
        final Formula f7 = CNFEncoder.encode(p.parse("(c & d) | (a & b) | ((c & d) <=> (a & b))"), config);
        testFiles("f7", f7);
    }

    private void testFiles(final String fileName, final Formula formula) throws IOException {
        FormulaDimacsFileWriter.write("src/test/resources/writers/temp/" + fileName + "_t.cnf", formula, true);
        FormulaDimacsFileWriter.write("src/test/resources/writers/temp/" + fileName + "_f", formula, false);
        final File expectedT = new File("src/test/resources/writers/formulas-dimacs/" + fileName + "_t.cnf");
        final File expectedF = new File("src/test/resources/writers/formulas-dimacs/" + fileName + "_f.cnf");
        final File tempT = new File("src/test/resources/writers/temp/" + fileName + "_t.cnf");
        final File tempF = new File("src/test/resources/writers/temp/" + fileName + "_f.cnf");
        final File expectedMap = new File("src/test/resources/writers/formulas-dimacs/" + fileName + "_t.map");
        final File tempMap = new File("src/test/resources/writers/temp/" + fileName + "_t.map");
        assertFilesEqual(expectedT, tempT);
        assertFilesEqual(expectedF, tempF);
        assertFilesEqual(expectedMap, tempMap);
    }

    private void assertFilesEqual(final File expected, final File actual) throws IOException {
        final SoftAssertions softly = new SoftAssertions();
        final BufferedReader expReader = new BufferedReader(new FileReader(expected));
        final BufferedReader actReader = new BufferedReader(new FileReader(actual));
        for (int lineNumber = 1; expReader.ready() && actReader.ready(); lineNumber++) {
            softly.assertThat(actReader.readLine()).as("Line " + lineNumber + " not equal").isEqualTo(expReader.readLine());
        }
        if (expReader.ready()) {
            softly.fail("Missing line(s) found, starting with \"" + expReader.readLine() + "\"");
        }
        if (actReader.ready()) {
            softly.fail("Additional line(s) found, starting with \"" + actReader.readLine() + "\"");
        }
        softly.assertAll();
    }
}
