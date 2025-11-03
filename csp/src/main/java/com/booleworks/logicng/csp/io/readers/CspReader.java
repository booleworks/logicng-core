package com.booleworks.logicng.csp.io.readers;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.io.parsers.CspParser;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A reader for CSP files.
 * @version 3.0.0
 * @since 3.0.0
 */
public class CspReader {
    private CspReader() {
    }

    /**
     * Reads a given file and returns the contained CSP problem and constructs
     * formula.
     * <p>
     * Each line is handled separately and can be either an integer variable
     * declaration or a formula.
     * <p>
     * Declarations are {@code int <var_name> [<lb>,<ub>]} and
     * {@code int <var_name> {<v_1>, <v_2>, <v_3>, ...}}. A declaration will
     * create the variable on the formula factory, so that it can be used in
     * formulas in the following lines. Make sure that before the declaration
     * the variable is not defined on the factory or has the same domain as
     * the declaration.
     * <p>
     * The resulting formula is the conjunction of all lines that are
     * interpreted as formulas.
     * @param cf       the factory
     * @param fileName the file name
     * @return the parsed formula
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static Formula readCsp(final CspFactory cf, final String fileName) throws IOException, ParserException {
        return readCsp(new CspParser(cf), new File(fileName));
    }

    /**
     * Reads a given file and returns the contained CSP problem and constructs
     * formula.
     * <p>
     * Each line is handled separately and can be either an integer variable
     * declaration or a formula.
     * <p>
     * Declarations are {@code int <var_name> [<lb>,<ub>]} and
     * {@code int <var_name> {<v_1>, <v_2>, <v_3>, ...}}. A declaration will
     * create the variable on the formula factory, so that it can be used in
     * formulas in the following lines. Make sure that before the declaration
     * the variable is not defined on the factory or has the same domain as
     * the declaration.
     * <p>
     * The resulting formula is the conjunction of all lines that are
     * interpreted as formulas.
     * @param cf   the factory
     * @param file the file
     * @return the parsed formula
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static Formula readCsp(final CspFactory cf, final File file) throws IOException, ParserException {
        return readCsp(new CspParser(cf), file);
    }

    /**
     * Reads a given file and returns the contained CSP problem and constructs
     * formula.
     * <p>
     * Each line is handled separately and can be either an integer variable
     * declaration or a formula.
     * <p>
     * Declarations are {@code int <var_name> [<lb>,<ub>]} and
     * {@code int <var_name> {<v_1>, <v_2>, <v_3>, ...}}. A declaration will
     * create the variable on the formula factory, so that it can be used in
     * formulas in the following lines. Make sure that before the declaration
     * the variable is not defined on the factory or has the same domain as the
     * declaration.
     * <p>
     * The resulting formula is the conjunction of all lines that are
     * interpreted as formulas.
     * @param parser   the parser
     * @param fileName the file name
     * @return the parsed formula
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static Formula readCsp(final CspParser parser, final String fileName) throws IOException, ParserException {
        return parser.getFactory().getFormulaFactory().and(readCspAsList(parser, fileName));
    }

    /**
     * Reads a given file and returns the contained CSP problem and constructs
     * formula.
     * <p>
     * Each line is handled separately and can be either an integer variable
     * declaration or a formula.
     * <p>
     * Declarations are {@code int <var_name> [<lb>,<ub>]} and
     * {@code int <var_name> {<v_1>, <v_2>, <v_3>, ...}}. A declaration will
     * create the variable on the formula factory, so that it can be used in
     * formulas in the following lines. Make sure that before the declaration
     * the variable is not defined on the factory or has the same domain as
     * the declaration.
     * <p>
     * The resulting formula is the conjunction of all lines that are
     * interpreted as formulas.
     * @param parser the parser
     * @param file   the file
     * @return the parsed formula
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static Formula readCsp(final CspParser parser, final File file) throws IOException, ParserException {
        return parser.getFactory().getFormulaFactory().and(readCspAsList(parser, file));
    }

    /**
     * Reads a given file and returns the contained CSP problem and constructs
     * formula.
     * <p>
     * Each line is handled separately and can be either an integer variable
     * declaration or a formula.
     * <p>
     * Declarations are {@code int <var_name> [<lb>,<ub>]} and
     * {@code int <var_name> {<v_1>, <v_2>, <v_3>, ...}}. A declaration will
     * create the variable on the formula factory, so that it can be used in
     * formulas in the following lines. Make sure that before the declaration
     * the variable is not defined on the factory or has the same domain as the
     * declaration.
     * <p>
     * The resulting formula is a list of all lines that are interpreted as
     * formulas.
     * @param cf       the factory
     * @param fileName the file name
     * @return list of all formulas (in order, without declarations)
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static List<Formula> readCspAsList(final CspFactory cf, final String fileName)
            throws IOException, ParserException {
        return readCspAsList(new CspParser(cf), fileName);
    }

    /**
     * Reads a given file and returns the contained CSP problem and constructs
     * formula.
     * <p>
     * Each line is handled separately and can be either an integer variable
     * declaration or a formula.
     * <p>
     * Declarations are {@code int <var_name> [<lb>,<ub>]} and
     * {@code int <var_name> {<v_1>, <v_2>, <v_3>, ...}}. A declaration will
     * create the variable on the formula factory, so that it can be used in
     * formulas in the following lines. Make sure that before the declaration
     * the variable is not defined on the factory or has the same domain as
     * the declaration.
     * <p>
     * The resulting formula is a list of all lines that are interpreted as
     * formulas.
     * @param cf   the factory
     * @param file the file
     * @return list of all formulas (in order, without declarations)
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static List<Formula> readCspAsList(final CspFactory cf, final File file)
            throws IOException, ParserException {
        return readCspAsList(new CspParser(cf), file);
    }

    /**
     * Reads a given file and returns the contained CSP problem and constructs
     * formula.
     * <p>
     * Each line is handled separately and can be either an integer variable
     * declaration or a formula.
     * <p>
     * Declarations are {@code int <var_name> [<lb>,<ub>]} and
     * {@code int <var_name> {<v_1>, <v_2>, <v_3>, ...}}. A declaration will
     * create the variable on the formula factory, so that it can be used in
     * formulas in the following lines. Make sure that before the declaration
     * the variable is not defined on the factory or has the same domain as
     * the declaration.
     * <p>
     * The resulting formula is a list of all lines that are interpreted as
     * formulas.
     * @param parser   the parser
     * @param fileName the file name
     * @return list of all formulas (in order, without declarations)
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static List<Formula> readCspAsList(final CspParser parser, final String fileName)
            throws IOException, ParserException {
        return readCspAsList(parser, new File(fileName));
    }

    /**
     * Reads a given file and returns the contained CSP problem and constructs
     * formula.
     * <p>
     * Each line is handled separately and can be either an integer variable
     * declaration or a formula.
     * <p>
     * Declarations are {@code int <var_name> [<lb>,<ub>]} and
     * {@code int <var_name> {<v_1>, <v_2>, <v_3>, ...}}. A declaration will
     * create the variable on the formula factory, so that it can be used in
     * formulas in the following lines. Make sure that before the declaration
     * the variable is not defined on the factory or has the same domain as the
     * declaration.
     * <p>
     * The resulting formula is a list of all lines that are interpreted as
     * formulas.
     * @param parser the parser
     * @param file   the file
     * @return list of all formulas (in order, without declarations)
     * @throws IOException     if there was a problem reading the file
     * @throws ParserException if there was a problem parsing the formula
     */
    public static List<Formula> readCspAsList(final CspParser parser, final File file)
            throws IOException, ParserException {
        final List<Formula> formulas = new ArrayList<>();
        try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
            while (br.ready()) {
                final String line = br.readLine();
                final Pair<IntegerVariable, Formula> p = parser.parseDeclarationOrFormula(line);
                if (p.getFirst() == null) {
                    formulas.add(p.getSecond());
                }
            }
        }
        return formulas;
    }
}
