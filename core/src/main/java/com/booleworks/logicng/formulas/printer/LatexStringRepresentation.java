// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.printer;

import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The LaTeX string representation for formulas.
 * @version 3.0.0
 * @since 1.0
 */
public class LatexStringRepresentation extends FormulaStringRepresentation {

    protected static final Pattern pattern = Pattern.compile("(.*?)(\\d*)");
    private static final LatexStringRepresentation INSTANCE = new LatexStringRepresentation();

    /**
     * Returns the singleton instance.
     * @return the singleton instance
     */
    public static LatexStringRepresentation get() {
        return INSTANCE;
    }

    /**
     * Returns the latex string for a variable name
     * @param name the name
     * @return the matching UTF8 symbol
     */
    protected static String latexName(final String name) {
        final Matcher matcher = pattern.matcher(name);
        if (!matcher.matches()) {
            return name;
        }
        if (matcher.group(2).isEmpty()) {
            return matcher.group(1);
        }
        return matcher.group(1) + "_{" + matcher.group(2) + "}";
    }

    @Override
    public String toInnerString(final Formula formula) {
        if (formula.getType() == FType.LITERAL) {
            final Literal lit = (Literal) formula;
            return lit.getPhase() ? latexName(lit.getName()) : negation() + " " + latexName(lit.getName());
        }
        return super.toInnerString(formula);
    }

    @Override
    protected String falsum() {
        return "\\bottom";
    }

    @Override
    protected String verum() {
        return "\\top";
    }

    @Override
    protected String negation() {
        return "\\lnot";
    }

    @Override
    protected String implication() {
        return " \\rightarrow ";
    }

    @Override
    protected String equivalence() {
        return " \\leftrightarrow ";
    }

    @Override
    protected String and() {
        return " \\land ";
    }

    @Override
    protected String or() {
        return " \\lor ";
    }

    @Override
    protected String pbComparator(final CType comparator) {
        switch (comparator) {
            case EQ:
                return " = ";
            case LE:
                return " \\leq ";
            case LT:
                return " < ";
            case GE:
                return " \\geq ";
            case GT:
                return " > ";
            default:
                throw new IllegalArgumentException("Unknown pseudo-Boolean comparison: " + comparator);
        }
    }

    @Override
    protected String pbMul() {
        return "\\cdot ";
    }

    @Override
    protected String pbAdd() {
        return " + ";
    }

    @Override
    protected String lbr() {
        return "\\left(";
    }

    @Override
    protected String rbr() {
        return "\\right)";
    }
}
