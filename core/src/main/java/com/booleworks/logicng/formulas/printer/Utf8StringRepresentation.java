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
 * The UTF8 string representation for formulas.
 * @version 1.1
 * @since 1.0
 */
public final class Utf8StringRepresentation extends FormulaStringRepresentation {

    private static final Pattern pattern = Pattern.compile("(.*?)(\\d*)");

    private static final Utf8StringRepresentation INSTANCE = new Utf8StringRepresentation();

    /**
     * Returns the singleton instance.
     * @return the singleton instance
     */
    public static Utf8StringRepresentation get() {
        return INSTANCE;
    }

    /**
     * Returns the UTF8 string for a variable name
     * @param name the name
     * @return the matching UTF8 symbol
     */
    private static String utf8Name(final String name) {
        final Matcher matcher = pattern.matcher(name);
        if (!matcher.matches()) {
            return name;
        }
        if (matcher.group(2).isEmpty()) {
            return matcher.group(1);
        }
        return matcher.group(1) + getSubscript(matcher.group(2));
    }

    private static String getSubscript(final String number) {
        final StringBuilder sb = new StringBuilder();
        for (final char c : number.toCharArray()) {
            switch (c) {
                case '0':
                    sb.append("₀");
                    break;
                case '1':
                    sb.append("₁");
                    break;
                case '2':
                    sb.append("₂");
                    break;
                case '3':
                    sb.append("₃");
                    break;
                case '4':
                    sb.append("₄");
                    break;
                case '5':
                    sb.append("₅");
                    break;
                case '6':
                    sb.append("₆");
                    break;
                case '7':
                    sb.append("₇");
                    break;
                case '8':
                    sb.append("₈");
                    break;
                case '9':
                    sb.append("₉");
                    break;
                default:
                    break;
            }
        }
        return sb.toString();
    }

    @Override
    protected String toInnerString(final Formula formula) {
        if (formula.getType() == FType.LITERAL) {
            final Literal lit = (Literal) formula;
            return lit.getPhase() ? utf8Name(lit.getName()) : negation() + utf8Name(lit.getName());
        }
        return super.toInnerString(formula);
    }

    @Override
    protected String falsum() {
        return "⊥";
    }

    @Override
    protected String verum() {
        return "⊤";
    }

    @Override
    protected String negation() {
        return "¬";
    }

    @Override
    protected String implication() {
        return " ⇒ ";
    }

    @Override
    protected String equivalence() {
        return " ⇔ ";
    }

    @Override
    protected String and() {
        return " ∧ ";
    }

    @Override
    protected String or() {
        return " ∨ ";
    }

    @Override
    protected String pbComparator(final CType comparator) {
        switch (comparator) {
            case EQ:
                return " = ";
            case LE:
                return " ≤ ";
            case LT:
                return " < ";
            case GE:
                return " ≥ ";
            case GT:
                return " > ";
            default:
                throw new IllegalArgumentException("Unknown pseudo-Boolean comparison: " + comparator);
        }
    }

    @Override
    protected String pbMul() {
        return "";
    }

    @Override
    protected String pbAdd() {
        return " + ";
    }

    @Override
    protected String lbr() {
        return "(";
    }

    @Override
    protected String rbr() {
        return ")";
    }
}
