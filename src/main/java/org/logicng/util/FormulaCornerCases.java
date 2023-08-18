// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.util;

import org.logicng.formulas.CType;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Creation of formula corner cases.
 * <p>
 * Formula corner cases help to cover all cases when writing unit tests with formulas involved.
 * @version 2.0.0
 * @since 2.0.0
 */
public final class FormulaCornerCases {

    private final FormulaFactory f;
    private final Variable a;
    private final Variable b;
    private final Variable c;
    private final Literal na;
    private final Literal nb;
    private final Literal nc;

    /**
     * Constructs a new corner cases creator.
     * @param f the formula factory
     */
    public FormulaCornerCases(final FormulaFactory f) {
        this.f = f;
        a = f.variable("a");
        b = f.variable("b");
        c = f.variable("c");
        na = a.negate(f);
        nb = b.negate(f);
        nc = c.negate(f);
    }

    /**
     * Returns the set of variables used for creating the corner cases.
     * @return the set of variables
     */
    public SortedSet<Variable> getVariables() {
        return new TreeSet<>(Arrays.asList(a, b, c));
    }

    /**
     * Generates corner cases for all operators.
     * @return formulas representing corner cases for all operators
     */
    public List<Formula> cornerCases() {
        final List<Formula> formulas = new ArrayList<>();
        formulas.add(f.falsum());
        formulas.add(f.not(f.falsum()));
        formulas.add(f.verum());
        formulas.add(f.not(f.verum()));
        formulas.add(a);
        formulas.add(a.negate(f));
        formulas.add(f.not(a));
        formulas.add(f.not(f.not(a)));
        formulas.add(f.not(f.not(f.not(a))));
        formulas.addAll(binaryCornerCases(FType.IMPL, a, b));
        formulas.addAll(binaryCornerCases(FType.EQUIV, a, b));
        formulas.addAll(naryCornerCases(FType.OR, a, b, c));
        formulas.addAll(naryCornerCases(FType.AND, a, b, c));
        formulas.addAll(pbcCornerCases(a, b, c));
        return formulas;
    }

    private List<Formula> binaryCornerCases(final FType type, final Variable a, final Variable b) {
        final List<Formula> formulas = new ArrayList<>();
        formulas.add(f.binaryOperator(type, f.verum(), f.verum()));
        formulas.add(f.binaryOperator(type, f.falsum(), f.verum()));
        formulas.add(f.binaryOperator(type, f.verum(), f.falsum()));
        formulas.add(f.binaryOperator(type, f.falsum(), f.falsum()));

        formulas.add(f.binaryOperator(type, f.verum(), a));
        formulas.add(f.binaryOperator(type, a, f.verum()));
        formulas.add(f.binaryOperator(type, f.verum(), na));
        formulas.add(f.binaryOperator(type, na, f.verum()));

        formulas.add(f.binaryOperator(type, f.falsum(), a));
        formulas.add(f.binaryOperator(type, a, f.falsum()));
        formulas.add(f.binaryOperator(type, f.falsum(), na));
        formulas.add(f.binaryOperator(type, na, f.falsum()));

        formulas.add(f.binaryOperator(type, a, a));
        formulas.add(f.binaryOperator(type, a, na));
        formulas.add(f.binaryOperator(type, na, a));
        formulas.add(f.binaryOperator(type, na, na));

        formulas.add(f.binaryOperator(type, a, b));
        formulas.add(f.binaryOperator(type, a, nb));
        formulas.add(f.binaryOperator(type, na, b));
        formulas.add(f.binaryOperator(type, na, nb));
        return formulas;
    }

    private List<Formula> naryCornerCases(final FType type, final Variable a, final Variable b, final Variable c) {
        final List<Formula> formulas = new ArrayList<>();
        formulas.add(f.naryOperator(type, new Variable[0]));

        formulas.add(f.naryOperator(type, f.falsum()));
        formulas.add(f.naryOperator(type, f.verum()));
        formulas.add(f.naryOperator(type, f.falsum(), f.verum()));

        formulas.add(f.naryOperator(type, a));
        formulas.add(f.naryOperator(type, na));

        formulas.add(f.naryOperator(type, f.verum(), a));
        formulas.add(f.naryOperator(type, f.verum(), na));
        formulas.add(f.naryOperator(type, f.falsum(), na));
        formulas.add(f.naryOperator(type, f.falsum(), na));

        formulas.add(f.naryOperator(type, a, na));
        formulas.add(f.naryOperator(type, a, b));
        formulas.add(f.naryOperator(type, a, b, c));
        formulas.add(f.naryOperator(type, na, nb, nc));
        formulas.add(f.naryOperator(type, a, b, c, na));
        return formulas;
    }

    private List<Formula> pbcCornerCases(final Variable a, final Variable b, final Variable c) {
        final List<Formula> formulas = new ArrayList<>();
        for (final CType type : CType.values()) {
            formulas.addAll(pbcCornerCases(type, a, b, c));
        }
        return formulas;
    }

    private List<Formula> pbcCornerCases(final CType comparator, final Variable a, final Variable b, final Variable c) {
        final List<Formula> formulas = new ArrayList<>();
        formulas.addAll(pbcCornerCases(comparator, new Literal[0], new int[0], f));

        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a}, new int[]{-1}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a}, new int[]{0}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a}, new int[]{1}, f));

        formulas.addAll(pbcCornerCases(comparator, new Literal[]{na}, new int[]{-1}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{na}, new int[]{0}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{na}, new int[]{1}, f));

        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, b}, new int[]{-1, -1}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, b}, new int[]{0, 0}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, b}, new int[]{1, 1}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, b}, new int[]{1, -1}, f));

        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, nb}, new int[]{-1, -1}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, nb}, new int[]{0, 0}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, nb}, new int[]{1, 1}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, nb}, new int[]{1, -1}, f));

        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, na}, new int[]{-1, -1}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, na}, new int[]{0, 0}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, na}, new int[]{1, 1}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, na}, new int[]{1, -1}, f));

        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, b, c}, new int[]{-1, -1, -1}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, b, c}, new int[]{0, 0, 0}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, b, c}, new int[]{1, 1, 1}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{a, b, c}, new int[]{-1, 1, -1}, f));
        formulas.addAll(pbcCornerCases(comparator, new Literal[]{na, nb, c}, new int[]{-1, 1, -1}, f));
        return formulas;
    }

    private List<Formula> pbcCornerCases(final CType comparator, final Literal[] literals, final int[] coefficients, final FormulaFactory f) {
        final List<Formula> formulas = new ArrayList<>();
        for (final Integer rhs : Arrays.asList(-1, 0, 1, -3, -4, 3, 4)) {
            formulas.add(f.pbc(comparator, rhs, literals, coefficients));
        }
        return formulas;
    }
}
