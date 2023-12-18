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
import java.util.LinkedHashSet;

/**
 * A reader for formulas.
 * <p>
 * Reads a formula from an input file. If the file has more than one line, the
 * lines will be co-joined.
 * @version 3.0.0
 * @since 1.2
 */
public final class FormulaReader {

    /**
     * Private constructor.
     */
    private FormulaReader() {
        // Intentionally left empty.
    }

    /**
     * Reads a given file and returns the contained propositional formula.
     * @param f        the formula factory
     * @param fileName the file name
     * @return the parsed formula
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static Formula readPropositionalFormula(final FormulaFactory f, final String fileName)
            throws IOException, ParserException {
        return read(new File(fileName), new PropositionalParser(f));
    }

    /**
     * Reads a given file and returns the contained propositional formula.
     * @param f    the formula factory
     * @param file the file
     * @return the parsed formula
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static Formula readPropositionalFormula(final FormulaFactory f, final File file)
            throws IOException, ParserException {
        return read(file, new PropositionalParser(f));
    }

    /**
     * Internal read function.
     * @param file   the file
     * @param parser the parser
     * @return the parsed formula
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    private static Formula read(final File file, final FormulaParser parser) throws IOException, ParserException {
        try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
            final LinkedHashSet<Formula> ops = new LinkedHashSet<>();
            while (br.ready()) {
                ops.add(parser.parse(br.readLine()));
            }
            return parser.factory().and(ops);
        }
    }
}
