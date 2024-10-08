// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.parsers.antlr;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

/**
 * Abstract super class for parsers which have a formula factory and return a formula.
 * @version 3.0.0
 * @since 1.4.0
 */
public abstract class ParserWithFormula extends Parser {

    protected FormulaFactory f;

    /**
     * Constructor.
     * @param input the token stream (e.g. a lexer) for the parser
     */
    public ParserWithFormula(final TokenStream input) {
        super(input);
    }

    /**
     * Sets the LogicNG formula factory for this parser
     * @param f the LogicNG formula factory
     */
    public void setFormulaFactory(final FormulaFactory f) {
        this.f = f;
    }

    /**
     * Returns the parsed formula.
     * @return the parsed formula
     */
    public abstract Formula getParsedFormula();
}
