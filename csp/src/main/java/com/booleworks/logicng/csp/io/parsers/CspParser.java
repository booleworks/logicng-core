// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.io.parsers;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.io.javacc.CspFormulaParser;
import com.booleworks.logicng.csp.io.javacc.ParseException;
import com.booleworks.logicng.csp.io.javacc.TokenMgrError;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.util.Pair;

import java.io.InputStream;
import java.io.StringReader;

/**
 * Parser for {@link CspPredicate} and formulas with predicates.
 * @version 3.0.0
 * @since 3.0.0
 */
public class CspParser {
    private final CspFormulaParser parser;

    /**
     * Constructs new parser with an underlying factory which is used as a basis
     * for constructing new formulas, terms, and variables.
     * @param cf the underlying factory
     */
    public CspParser(final CspFactory cf) {
        parser = new CspFormulaParser(new StringReader(""));
        parser.setFactory(cf);
    }

    /**
     * Returns the underlying factory.
     * @return the underlying factory
     */
    public CspFactory getFactory() {
        return parser.getFactory();
    }

    /**
     * Parse a {@link CspPredicate} from an input stream.
     * @param inStream the input stream
     * @return the parsed predicate
     * @throws ParserException if parsing is not successful
     */
    public CspPredicate parsePredicate(final InputStream inStream) throws ParserException {
        if (inStream == null) {
            throw new ParserException("empty input", null);
        }
        try {
            parser.ReInit(inStream);
            return parser.predicate();
        } catch (final TokenMgrError e) {
            throw new ParserException("lexer error", e);
        } catch (final ParseException e) {
            throw new ParserException("parser error", e);
        }
    }

    /**
     * Parse a {@link CspPredicate} from a string.
     * @param input the input string
     * @return the parsed predicate
     * @throws ParserException if parsing is not successful
     */
    public CspPredicate parsePredicate(final String input) throws ParserException {
        if (input == null || input.isBlank()) {
            throw new ParserException("empty input", null);
        }
        try {
            parser.ReInit(new StringReader(input));
            return parser.predicate();
        } catch (final TokenMgrError e) {
            throw new ParserException("lexer error", e);
        } catch (final ParseException e) {
            throw new ParserException("parser error", e);
        }
    }

    /**
     * Parse a formula with {@link CspPredicate} from an input stream.
     * @param input the input stream
     * @return the parsed formula
     * @throws ParserException if parsing is not successful
     */
    public Formula parseFormula(final InputStream input) throws ParserException {
        if (input == null) {
            return parser.getFactory().getFormulaFactory().verum();
        }
        try {
            parser.ReInit(input);
            return parser.formula();
        } catch (final TokenMgrError e) {
            throw new ParserException("lexer error", e);
        } catch (final ParseException e) {
            throw new ParserException("parser error", e);
        }
    }

    /**
     * Parse a formula with {@link CspPredicate} from a string.
     * @param input the input string
     * @return the parsed formula
     * @throws ParserException if parsing is not successful
     */
    public Formula parseFormula(final String input) throws ParserException {
        if (input == null || input.isBlank()) {
            return parser.getFactory().getFormulaFactory().verum();
        }
        try {
            parser.ReInit(new StringReader(input));
            return parser.formula();
        } catch (final TokenMgrError e) {
            throw new ParserException("lexer error", e);
        } catch (final ParseException e) {
            throw new ParserException("parser error", e);
        }
    }

    /**
     * Parses an input which either is a declaration for an integer variable or
     * a formula with predicates.
     * <p>
     * Declarations are {@code int <var_name> [<lb>,<ub>]} and
     * {@code int <var_name> {<v_1>, <v_2>, <v_3>, ...}}.
     * <p>
     * In case of the declaration, the variable is created (on the factory) and
     * returned. Make sure the variable is not defined on the factory or has the
     * same domain. If the input is parsed as formula, it will not create new
     * integer variables.
     * @param input the input
     * @return a pair where exactly one of the values is not null. First value
     * is the result of a declaration; second value of a formula
     * @throws ParserException if parsing is not successful
     */
    public Pair<IntegerVariable, Formula> parseDeclarationOrFormula(final InputStream input) throws ParserException {
        if (input == null) {
            return new Pair<>(null, parser.getFactory().getFormulaFactory().verum());
        }
        try {
            parser.ReInit(input);
            return parser.declaration_or_formula();
        } catch (final TokenMgrError e) {
            throw new ParserException("lexer error", e);
        } catch (final ParseException e) {
            throw new ParserException("parser error", e);
        }
    }

    /**
     * Parses an input which either is a declaration for an integer variable or
     * a formula with predicates.
     * <p>
     * Declarations are {@code int <var_name> [<lb>,<ub>]} and
     * {@code int <var_name> {<v_1>, <v_2>, <v_3>, ...}}.
     * <p>
     * In case of the declaration, the variable is created (on the factory) and
     * returned. Make sure the variable is not defined on the factory or has the
     * same domain. If the input is parsed as formula, it will not create new
     * integer variables.
     * @param input the input
     * @return a pair where exactly one of the values is not null. First value
     * is the result of a declaration; second value of a formula
     * @throws ParserException if parsing is not successful
     */
    public Pair<IntegerVariable, Formula> parseDeclarationOrFormula(final String input) throws ParserException {
        if (input == null || input.isBlank()) {
            return new Pair<>(null, parser.getFactory().getFormulaFactory().verum());
        }
        try {
            parser.ReInit(new StringReader(input));
            return parser.declaration_or_formula();
        } catch (final TokenMgrError e) {
            throw new ParserException("lexer error", e);
        } catch (final ParseException e) {
            throw new ParserException("parser error", e);
        }
    }
}
