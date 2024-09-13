// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.printer.UTF8StringRepresentation;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.io.writers.FormulaWriter;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FormulaWriterReaderTest {

    @Test
    public void testSimpleFormulaOneLine() throws ParserException, IOException {
        final String fileName = "../test_files/writers/temp/simple_formula1.txt";
        final File file = new File(fileName);
        final FormulaFactory f = FormulaFactory.caching();
        final Formula p1 = new PropositionalParser(f).parse("A & B & ~(C | (D => ~E))");
        FormulaWriter.write(file, p1, false);
        final Formula p2 = FormulaReader.readFormula(f, fileName);
        final Formula p3 = FormulaReader.readFormula(f, file);
        assertThat(p2).isEqualTo(p1);
        assertThat(p3).isEqualTo(p1);
        try (final BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            int count = 0;
            while (reader.ready()) {
                reader.readLine();
                count++;
            }
            assertThat(count).isEqualTo(1);
        }
        Files.deleteIfExists(file.toPath());
    }

    @Test
    public void testSimpleFormulaMultiLine() throws ParserException, IOException {
        final String fileName = "../test_files/writers/temp/simple_formula2.txt";
        final File file = new File(fileName);
        final FormulaFactory f = FormulaFactory.caching();
        final Formula p1 = new PropositionalParser(f).parse("A & B & ~(C | (D => ~E))");
        FormulaWriter.write(fileName, p1, true);
        final Formula p2 = FormulaReader.readFormula(f, fileName);
        assertThat(p2).isEqualTo(p1);
        try (final BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            int count = 0;
            while (reader.ready()) {
                reader.readLine();
                count++;
            }
            assertThat(count).isEqualTo(3);
        }
        Files.deleteIfExists(file.toPath());
    }

    @Test
    public void testPBFormulaOneLine() throws ParserException, IOException {
        final String fileName = "../test_files/writers/temp/simple_formula3.txt";
        final File file = new File(fileName);
        final FormulaFactory f = FormulaFactory.caching();
        final Formula p1 = new PropositionalParser(f).parse("A & B & ~(C | (D => ~E)) & (2*y + 3*y >= 4) & (x <= 1)");
        FormulaWriter.write(fileName, p1, false);
        final Formula p2 = FormulaReader.readFormula(f, fileName);
        final Formula p3 = FormulaReader.readFormula(f, file);
        assertThat(p2).isEqualTo(p1);
        assertThat(p3).isEqualTo(p1);
        try (final BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            int count = 0;
            while (reader.ready()) {
                reader.readLine();
                count++;
            }
            assertThat(count).isEqualTo(1);
        }
        Files.deleteIfExists(file.toPath());
    }

    @Test
    public void testPBFormulaMultiLine() throws ParserException, IOException {
        final String fileName = "../test_files/writers/temp/simple_formula4.txt";
        final File file = new File(fileName);
        final FormulaFactory f = FormulaFactory.caching();
        final Formula p1 = new PropositionalParser(f).parse("A & B & ~(C | (D => ~E)) & (2*y + 3*y >= 4) & (x <= 1)");
        FormulaWriter.write(fileName, p1, true);
        final Formula p2 = FormulaReader.readFormula(f, fileName);
        assertThat(p2).isEqualTo(p1);
        try (final BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            int count = 0;
            while (reader.ready()) {
                reader.readLine();
                count++;
            }
            assertThat(count).isEqualTo(5);
        }
        Files.deleteIfExists(file.toPath());
    }

    @Test
    public void testSimpleFormulaOneLineFormatter() throws ParserException, IOException {
        final String fileName = "../test_files/writers/temp/simple_formula5.txt";
        final File file = new File(fileName);
        final FormulaFactory f = FormulaFactory.caching();
        final Formula p1 = new PropositionalParser(f).parse("A & B & ~(C | (D => ~E))");
        FormulaWriter.write(fileName, p1, false, new UTF8StringRepresentation());
        try (final BufferedReader reader =
                     new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8))) {
            assertThat(reader.readLine()).isEqualTo("A ∧ B ∧ ¬(C ∨ (D ⇒ ¬E))");
        }
        Files.deleteIfExists(file.toPath());
    }

    @Test
    public void testSimpleFormulaMultiLineFormatter() throws ParserException, IOException {
        final String fileName = "../test_files/writers/temp/simple_formula6.txt";
        final File file = new File(fileName);
        final FormulaFactory f = FormulaFactory.caching();
        final Formula p1 = new PropositionalParser(f).parse("A & B & ~(C | (D => ~E))");
        FormulaWriter.write(fileName, p1, true, new UTF8StringRepresentation());
        try (final BufferedReader reader =
                     new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8))) {
            assertThat(reader.readLine()).isEqualTo("A");
            assertThat(reader.readLine()).isEqualTo("B");
            assertThat(reader.readLine()).isEqualTo("¬(C ∨ (D ⇒ ¬E))");
        }
        Files.deleteIfExists(file.toPath());
    }

}
