package com.booleworks.logicng.io.parsers;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;

import java.io.InputStream;

/**
 * Interface for formula parsers.
 * @version 3.0.0
 * @since 3.0.0
 */
public interface FormulaParser {

    /**
     * Parses and returns a given input stream.
     * @param inStream an input stream
     * @return the {@link Formula} representation of this stream
     * @throws ParserException if there was a problem with the input stream
     */
    Formula parse(final InputStream inStream) throws ParserException;

    /**
     * Parses and returns a given string.
     * @param in a string
     * @return the {@link Formula} representation of this string
     * @throws ParserException if the string was not a valid formula
     */
    Formula parse(final String in) throws ParserException;

    /**
     * An unsafe parse method which throws a runtime exception when a parser
     * error occurs instead of a checked exception like the other parse methods.
     * @param in a string
     * @return the {@link Formula} representation of this string
     * @throws IllegalArgumentException if the string was not a valid formula
     */
    default Formula parseUnsafe(final String in) {
        try {
            return parse(in);
        } catch (final ParserException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the factory of this parser.
     * @return the factory of this parser
     */
    FormulaFactory getFactory();
}
