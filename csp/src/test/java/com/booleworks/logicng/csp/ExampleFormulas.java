package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Predicate;

import java.util.List;

public class ExampleFormulas {
    public static Formula arithmJavaCreamSolver(final CspFactory cf) {
        final String prefix = "F1";
        final IntegerVariable a = cf.variable(prefix + "A", 0, 9);
        final IntegerVariable c = cf.variable(prefix + "C", 1, 9);
        final IntegerVariable e = cf.variable(prefix + "E", 0, 9);
        final IntegerVariable j = cf.variable(prefix + "J", 1, 9);
        final IntegerVariable l = cf.variable(prefix + "L", 0, 9);
        final IntegerVariable m = cf.variable(prefix + "M", 0, 9);
        final IntegerVariable o = cf.variable(prefix + "O", 0, 9);
        final IntegerVariable r = cf.variable(prefix + "R", 0, 9);
        final IntegerVariable s = cf.variable(prefix + "S", 1, 9);
        final IntegerVariable v = cf.variable(prefix + "V", 0, 9);
        final IntegerVariable c1 = cf.variable(prefix + "C1", 0, 1);
        final IntegerVariable c2 = cf.variable(prefix + "C2", 0, 1);
        final IntegerVariable c3 = cf.variable(prefix + "C3", 0, 1);
        final IntegerVariable c4 = cf.variable(prefix + "C4", 0, 1);
        final IntegerVariable c5 = cf.variable(prefix + "C5", 0, 1);
        final CspPredicate p1 = cf.allDifferent(List.of(a, c, e, j, l, m, o, r, s, v));
        final CspPredicate p2 = cf.eq(cf.add(a, m), cf.add(cf.mul(10, c1), r));
        final CspPredicate p3 = cf.eq(cf.add(c1, v, a), cf.add(cf.mul(10, c2), e));
        final CspPredicate p4 = cf.eq(cf.add(c2, a, e), cf.add(cf.mul(10, c3), v));
        final CspPredicate p5 = cf.eq(cf.add(c3, j, r), cf.add(cf.mul(10, c4), l));
        final CspPredicate p6 = cf.eq(cf.add(c4, c), cf.add(cf.mul(10, c5), o));
        final CspPredicate p7 = cf.eq(c5, s);
        return cf.getFormulaFactory().and(p1, p2, p3, p4, p5, p6, p7);
    }

    public static Formula arithmSatCoeAzucar(final CspFactory cf) {
        final String prefix = "F2";
        final IntegerVariable a = cf.variable(prefix + "A", 1, 9);
        final IntegerVariable c = cf.variable(prefix + "C", 1, 9);
        final IntegerVariable e = cf.variable(prefix + "E", 0, 9);
        final IntegerVariable o = cf.variable(prefix + "O", 0, 9);
        final IntegerVariable r = cf.variable(prefix + "R", 0, 9);
        final IntegerVariable s = cf.variable(prefix + "S", 1, 9);
        final IntegerVariable t = cf.variable(prefix + "T", 0, 9);
        final IntegerVariable u = cf.variable(prefix + "U", 0, 9);
        final IntegerVariable z = cf.variable(prefix + "z", 0, 9);
        final IntegerVariable c1 = cf.variable(prefix + "C1", 0, 1);
        final IntegerVariable c2 = cf.variable(prefix + "C2", 0, 1);
        final IntegerVariable c3 = cf.variable(prefix + "C3", 0, 1);
        final IntegerVariable c4 = cf.variable(prefix + "C4", 0, 1);
        final IntegerVariable c5 = cf.variable(prefix + "C5", 0, 1);
        final IntegerVariable sat = cf.variable(prefix + "SAT", 0, 999);
        final IntegerVariable coe = cf.variable(prefix + "COE", 0, 999);
        final IntegerVariable azucar = cf.variable(prefix + "AZUCAR", 0, 999999);
        final CspPredicate p1 = cf.allDifferent(List.of(a, c, e, t, o, r, s, u, z));
        final Predicate p2 = cf.eq(coe, cf.add(cf.mul(100, c), cf.mul(10, o), e));
        final Predicate p3 = cf.eq(sat, cf.add(cf.mul(100, s), cf.mul(10, a), t));
        final Predicate p4 = cf.eq(azucar,
                cf.add(cf.mul(100000, a), cf.mul(10000, z), cf.mul(1000, u), cf.mul(100, c), cf.mul(10, a), r));
        final Predicate p5 = cf.eq(cf.mul(sat, coe), azucar);
        return cf.getFormulaFactory().and(p1, p2, p3, p4, p5);
    }
}
