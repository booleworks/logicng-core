// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.parsers.antlr;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.FormulaParser;
import com.booleworks.logicng.io.parsers.ParserException;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.io.IOException;
import java.io.InputStream;

/**
 * An ANTLR parser for propositional formulas.
 * <p>
 * The syntax for propositional formulas in LogicNG is:
 * <ul>
 * <li>{@code $true} for the constant TRUE</li>
 * <li>{@code $false} for the constant FALSE</li>
 * <li>{@code ~} for the negation</li>
 * <li>{@code |} for the disjunction (OR)</li>
 * <li>{@code &} for the conjunction (AND)</li>
 * <li>{@code =>} for the implication</li>
 * <li>{@code <=>} for the equivalence</li>
 * </ul>
 * Brackets are {@code (} and {@code )}.  For variable names, there are the following rules:
 * <ul>
 * <li>must begin with a alphabetic character, {@code _}, {@code @}, or {@code #}</li>
 * <li>can only contain alphanumerical character, {@code _}, or {@code #}</li>
 * <li>{@code @} is only allowed at the beginning of the variable name and is reserved for special internal variables</li>
 * </ul>
 * @version 3.0.0
 * @since 1.0
 */
public final class AntlrPropositionalParser implements FormulaParser {

    private final FormulaFactory f;
    private Lexer lexer;
    private ParserWithFormula parser;

    /**
     * Constructs a new parser.
     * @param f the formula factory
     */
    public AntlrPropositionalParser(final FormulaFactory f) {
        this.f = f;
        final Lexer lexer = new PropositionalLexer(null);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final ParserWithFormula parser = new LogicNGPropositionalParser(tokens);
        setLexerAndParser(lexer, parser);
    }

    private void setLexerAndParser(final Lexer lexer, final ParserWithFormula parser) {
        this.lexer = lexer;
        this.parser = parser;
        this.parser.setFormulaFactory(f);
        this.lexer.removeErrorListeners();
        this.parser.removeErrorListeners();
        this.parser.setErrorHandler(new BailErrorStrategy());
    }

    /**
     * Parses and returns a given input stream.
     * @param inputStream an input stream
     * @return the {@link Formula} representation of this stream
     * @throws ParserException if there was a problem with the input stream
     */
    @Override
    public Formula parse(final InputStream inputStream) throws ParserException {
        if (inputStream == null) {
            return f.verum();
        }
        try {
            final CharStream input = CharStreams.fromStream(inputStream);
            lexer.setInputStream(input);
            final CommonTokenStream tokens = new CommonTokenStream(lexer);
            parser.setInputStream(tokens);
            return parser.getParsedFormula();
        } catch (final IOException e) {
            throw new ParserException("IO exception when parsing the formula", e);
        } catch (final ParseCancellationException e) {
            throw new ParserException("Parse cancellation exception when parsing the formula", e);
        } catch (final LexerException e) {
            throw new ParserException("Lexer exception when parsing the formula.", e);
        }
    }

    /**
     * Parses and returns a given string.
     * @param in a string
     * @return the {@link Formula} representation of this string
     * @throws ParserException if the string was not a valid formula
     */
    @Override
    public Formula parse(final String in) throws ParserException {
        if (in == null || in.isEmpty()) {
            return f.verum();
        }
        try {
            final CharStream input = CharStreams.fromString(in);
            lexer.setInputStream(input);
            final CommonTokenStream tokens = new CommonTokenStream(lexer);
            parser.setInputStream(tokens);
            return parser.getParsedFormula();
        } catch (final ParseCancellationException e) {
            throw new ParserException("Parse cancellation exception when parsing the formula", e);
        } catch (final LexerException e) {
            throw new ParserException("Lexer exception when parsing the formula.", e);
        }
    }

    /**
     * Returns the factory of this parser.
     * @return the factory of this parser
     */
    @Override
    public FormulaFactory factory() {
        return f;
    }
}
