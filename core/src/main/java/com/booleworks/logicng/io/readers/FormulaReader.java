// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.readers;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.FormulaParser;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A reader for formula files.
 * @version 3.0.0
 * @since 1.2
 */
public class FormulaReader {

    protected FormulaReader() {
        // Intentionally left empty.
    }

    /**
     * Reads a given file and returns the contained formulas as a formula conjunction.
     * @param f        the formula factory
     * @param fileName the file name
     * @return the parsed formula
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static Formula readFormula(final FormulaFactory f, final String fileName) throws IOException, ParserException {
        return readFormula(new PropositionalParser(f), new File(fileName));
    }

    /**
     * Reads a given file and returns the contained formulas as a formula conjunction.
     * @param f    the formula factory
     * @param file the file
     * @return the parsed formula
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static Formula readFormula(final FormulaFactory f, final File file) throws IOException, ParserException {
        return readFormula(new PropositionalParser(f), file);
    }

    /**
     * Reads a given file and returns the contained formulas.
     * @param f        the formula factory
     * @param fileName the file name
     * @return the parsed formulas
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static List<Formula> readFormulas(final FormulaFactory f, final String fileName) throws IOException, ParserException {
        return readFormulas(new PropositionalParser(f), new File(fileName));
    }

    /**
     * Reads a given file and returns the contained formulas.
     * @param f    the formula factory
     * @param file the file
     * @return the parsed formulas
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static List<Formula> readFormulas(final FormulaFactory f, final File file) throws IOException, ParserException {
        return readFormulas(new PropositionalParser(f), file);
    }

    /**
     * Reads a given file and returns the contained formulas as a formula conjunction.
     * @param parser   the parser
     * @param fileName the file name
     * @return the parsed formula
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formulas
     */
    public static Formula readFormula(final FormulaParser parser, final String fileName) throws IOException, ParserException {
        return parser.getFactory().and(readFormulas(parser, fileName));
    }

    /**
     * Reads a given file and returns the contained formulas as a formula conjunction.
     * @param parser the parser
     * @param file   the file
     * @return the parsed formula
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formulas
     */
    public static Formula readFormula(final FormulaParser parser, final File file) throws IOException, ParserException {
        return parser.getFactory().and(readFormulas(parser, file));
    }

    /**
     * Reads a given file and returns the contained formulas.
     * @param parser   the parser
     * @param fileName the file name
     * @return the parsed formulas
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formulas
     */
    public static List<Formula> readFormulas(final FormulaParser parser, final String fileName)
            throws IOException, ParserException {
        return readFormulas(parser, new File(fileName));
    }

    /**
     * Reads a given file and returns the contained formulas.
     * @param parser the parser
     * @param file   the file
     * @return the parsed formulas
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formulas
     */
    public static List<Formula> readFormulas(final FormulaParser parser, final File file)
            throws IOException, ParserException {
        final List<Formula> formulas = new ArrayList<>();
        try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
            while (br.ready()) {
                formulas.add(parser.parse(br.readLine()));
            }
        }
        return formulas;
    }
}
