// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.writers;

import static com.booleworks.logicng.TestWithExampleFormulas.parse;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.transformations.cnf.CnfConfig;
import com.booleworks.logicng.transformations.cnf.CnfEncoder;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FormulaDimacsFileWriterTest extends TestWithFormulaContext {

    private final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().name("F").build());
    private final CnfConfig config = CnfConfig.builder().algorithm(CnfConfig.Algorithm.FACTORIZATION).build();
    private final PropositionalParser p = new PropositionalParser(f);
    private final PropositionalParser pp = new PropositionalParser(f);

    @Test
    public void testNonCNF() {
        assertThatThrownBy(
                () -> FormulaDimacsFileWriter.write("non-cnf", parse(FormulaFactory.nonCaching(), "a => b"), false))
                .isInstanceOf(IllegalArgumentException.class);
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
        final Formula f1 = CnfEncoder.encode(f, p.parse("(a & b) <=> (~c => (x | z))"), config);
        final Formula f2 = CnfEncoder.encode(f, p.parse("a & b | b & ~c"), config);
        final Formula f3 = CnfEncoder.encode(f, p.parse("(a & b) <=> (~c => (a | b))"), config);
        final Formula f4 = CnfEncoder.encode(f, p.parse("~(a & b) | b & ~c"), config);
        final Formula f5 = CnfEncoder.encode(f, pp.parse("a | ~b | (2*a + 3*~b + 4*c <= 4)"), config);
        testFiles("f1", f1);
        testFiles("f2", f2);
        testFiles("f3", f3);
        testFiles("f4", f4);
        testFiles("f5", f5);
    }

    @Test
    public void testDuplicateFormulaParts() throws ParserException, IOException {
        final Formula f6 = CnfEncoder.encode(f, p.parse("(a & b) | (c & ~(a & b))"), config);
        testFiles("f6", f6);
        final Formula f7 = CnfEncoder.encode(f, p.parse("(c & d) | (a & b) | ((c & d) <=> (a & b))"), config);
        testFiles("f7", f7);
    }

    private void testFiles(final String fileName, final Formula formula) throws IOException {
        FormulaDimacsFileWriter.write("../test_files/writers/temp/" + fileName + "_t.cnf", formula, true);
        FormulaDimacsFileWriter.write("../test_files/writers/temp/" + fileName + "_f", formula, false);
        final File expectedT = new File("../test_files/writers/formulas-dimacs/" + fileName + "_t.cnf");
        final File expectedF = new File("../test_files/writers/formulas-dimacs/" + fileName + "_f.cnf");
        final File tempT = new File("../test_files/writers/temp/" + fileName + "_t.cnf");
        final File tempF = new File("../test_files/writers/temp/" + fileName + "_f.cnf");
        final File expectedMap = new File("../test_files/writers/formulas-dimacs/" + fileName + "_t.map");
        final File tempMap = new File("../test_files/writers/temp/" + fileName + "_t.map");
        assertFilesEqual(expectedT, tempT);
        assertFilesEqual(expectedF, tempF);
        assertFilesEqual(expectedMap, tempMap);
    }

    private void assertFilesEqual(final File expected, final File actual) throws IOException {
        final SoftAssertions softly = new SoftAssertions();
        final BufferedReader expReader = new BufferedReader(new FileReader(expected));
        final BufferedReader actReader = new BufferedReader(new FileReader(actual));
        for (int lineNumber = 1; expReader.ready() && actReader.ready(); lineNumber++) {
            softly.assertThat(actReader.readLine()).as("Line " + lineNumber + " not equal")
                    .isEqualTo(expReader.readLine());
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
