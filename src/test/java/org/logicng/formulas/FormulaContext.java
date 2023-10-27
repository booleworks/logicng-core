// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import org.logicng.formulas.implementation.cached.CachingFormulaFactory;
import org.logicng.io.parsers.FormulaParser;
import org.logicng.io.parsers.PropositionalParser;

import java.util.Arrays;
import java.util.List;

public class FormulaContext {
    public final FormulaFactory f;
    public final FormulaParser p;

    public final Constant verum;
    public final Constant falsum;

    public final Variable a;
    public final Variable b;
    public final Variable c;
    public final Variable d;
    public final Variable x;
    public final Variable y;
    public final Literal na;
    public final Literal nb;
    public final Literal nx;
    public final Literal ny;

    public final Formula or1;
    public final Formula or2;
    public final Formula or3;

    public final Formula and1;
    public final Formula and2;
    public final Formula and3;

    public final Formula not1;
    public final Formula not2;

    public final Formula imp1;
    public final Formula imp2;
    public final Formula imp3;
    public final Formula imp4;

    public final Formula eq1;
    public final Formula eq2;
    public final Formula eq3;
    public final Formula eq4;

    public final Formula pbc1;
    public final Formula pbc2;
    public final Formula pbc3;
    public final Formula pbc4;
    public final Formula pbc5;

    public final PBConstraint pb1;
    public final PBConstraint pb2;
    public final CardinalityConstraint cc1;
    public final CardinalityConstraint cc2;
    public final CardinalityConstraint amo1;
    public final CardinalityConstraint amo2;
    public final CardinalityConstraint exo1;
    public final CardinalityConstraint exo2;

    public final Formula tautology;
    public final Formula contradiction;

    public FormulaContext(final FormulaFactory f) {
        this.f = f;
        p = new PropositionalParser(f);

        verum = f.verum();
        falsum = f.falsum();

        a = f.variable("a");
        b = f.variable("b");
        c = f.variable("c");
        d = f.variable("d");
        x = f.variable("x");
        y = f.variable("y");
        na = f.literal("a", false);
        nb = f.literal("b", false);
        nx = f.literal("x", false);
        ny = f.literal("y", false);

        or1 = f.or(x, y);
        or2 = f.or(nx, ny);
        or3 = f.or(f.and(a, b), f.and(na, nb));

        and1 = f.and(a, b);
        and2 = f.and(na, nb);
        and3 = f.and(or1, or2);

        not1 = f.not(and1);
        not2 = f.not(or1);

        imp1 = f.implication(a, b);
        imp2 = f.implication(na, nb);
        imp3 = f.implication(and1, or1);
        imp4 = f.implication(f.equivalence(a, b), f.equivalence(nx, ny));

        eq1 = f.equivalence(a, b);
        eq2 = f.equivalence(na, nb);
        eq3 = f.equivalence(and1, or1);
        eq4 = f.equivalence(imp1, imp2);

        final Literal[] literals = new Literal[]{a, b, x};
        final int[] coefficients = new int[]{2, -4, 3};
        pbc1 = f.pbc(CType.EQ, 2, literals, coefficients);
        pbc2 = f.pbc(CType.GT, 2, literals, coefficients);
        pbc3 = f.pbc(CType.GE, 2, literals, coefficients);
        pbc4 = f.pbc(CType.LT, 2, literals, coefficients);
        pbc5 = f.pbc(CType.LE, 2, literals, coefficients);

        final Variable[] lits1 = new Variable[]{f.variable("a")};
        final List<Literal> lits2 = Arrays.asList(f.variable("a"), f.literal("b", false), f.variable("c"));
        final List<Variable> litsCC2 = Arrays.asList(f.variable("a"), f.variable("b"), f.variable("c"));
        final int[] coeffs1 = new int[]{3};
        final List<Integer> coeffs2 = Arrays.asList(3, -2, 7);
        pb1 = (PBConstraint) f.pbc(CType.LE, 2, lits1, coeffs1);
        pb2 = (PBConstraint) f.pbc(CType.LE, 8, lits2, coeffs2);
        cc1 = (CardinalityConstraint) f.cc(CType.LT, 1, lits1);
        cc2 = (CardinalityConstraint) f.cc(CType.GE, 2, litsCC2);
        amo1 = (CardinalityConstraint) f.amo(lits1);
        amo2 = (CardinalityConstraint) f.amo(litsCC2);
        exo1 = (CardinalityConstraint) f.exo(lits1);
        exo2 = (CardinalityConstraint) f.exo(litsCC2);

        tautology = f.or(a, na);
        contradiction = f.and(a, na);

    }

    public List<Formula> allFormulas() {
        return Arrays.asList(verum, falsum, a, b, c, d, x, y, na, nb, nx, ny, or1, or2, or3, and1, and2, and3, not1, not2, imp1, imp2, imp3, imp4,
                eq1, eq2, eq3, eq4, pbc1, pbc2, pbc3, pbc4, pbc5, pb1, pb2, cc1, cc2, amo1, amo2, exo1, exo2, tautology, contradiction);
    }

    @Override
    public String toString() {
        return f instanceof CachingFormulaFactory ? "Caching FF" : "Non-Caching FF";
    }
}
