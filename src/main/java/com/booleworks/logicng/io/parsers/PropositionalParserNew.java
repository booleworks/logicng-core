// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.parsers;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.javacc.BooleanFormulaParser;
import com.booleworks.logicng.io.parsers.javacc.ParseException;
import com.booleworks.logicng.io.parsers.javacc.TokenMgrError;

import java.io.StringReader;

/**
 * A parser for propositional (including pseudo-Boolean) formulas.
 * <p>
 * The syntax for pseudo Boolean formulas in LogicNG is:
 * <ul>
 * <li>{@code $true} for the constant TRUE</li>
 * <li>{@code $false} for the constant FALSE</li>
 * <li>{@code ~} for the negation</li>
 * <li>{@code |} for the disjunction (OR)</li>
 * <li>{@code &} for the conjunction (AND)</li>
 * <li>{@code =>} for the implication</li>
 * <li>{@code <=>} for the equivalence</li>
 * <li>{@code *} for the multiplication of a literal and its coefficient</li>
 * <li>{@code +} for the summation</li>
 * <li>{@code =} for equals</li>
 * <li>{@code <=} for less-equals</li>
 * <li>{@code <} for less than</li>
 * <li>{@code >=} for greater-equals</li>
 * <li>{@code >} for greater than</li>
 * </ul>
 * Brackets are {@code (} and {@code )}. For variable names, there are the
 * following rules:
 * <ul>
 * <li>must begin with a alphabetic character, {@code _}, {@code @}, or
 * {@code #}</li>
 * <li>can only contain alphanumerical character, {@code _}, or {@code #}</li>
 * <li>{@code @} is only allowed at the beginning of the variable name and is
 * reserved for special internal variables</li>
 * </ul>
 * <p>
 * A valid pseudo Boolean expression is of the form
 * {@code c_1 * l_1 + ... + c_n * l_n R k} where the {@code c_i} are
 * coefficients, {@code l_i} are literals, and {@code R} is one of
 * {@code =, >, >=, <, <=}.
 * @version 2.4.1
 * @since 1.0
 */
public final class PropositionalParserNew {

    private final BooleanFormulaParser parser;

    /**
     * Constructs a new parser for pseudo boolean formulas.
     * @param f the formula factory
     */
    public PropositionalParserNew(final FormulaFactory f) {
        parser = new BooleanFormulaParser(new StringReader(""));
        parser.setFactory(f);
    }

    public Formula parse(final String input) throws ParserException {
        if (input == null || input.isBlank()) {
            return parser.getFactory().verum();
        }
        try {
            parser.ReInit(new StringReader(input));
            return parser.formula();
        } catch (final TokenMgrError e) {
            throw new ParserException("lexer error", null); //TODO
        } catch (final ParseException e) {
            throw new ParserException("parser error", e); //TODO
        }
    }

    public FormulaFactory factory() {
        return parser.getFactory();
    }
}
