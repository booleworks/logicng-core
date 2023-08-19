// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas.printer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.io.parsers.ParserException;
import org.logicng.transformations.cnf.BDDCNFTransformation;

public class FormulaSatPredicatesTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsSatisfiable(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.f.parse("(a | b) & (c | ~d)");
        final Formula f2 = _c.f.parse("~a & ~b & (a | b)");
        assertThat(_c.f.falsum().isSatisfiable()).isFalse();
        assertThat(_c.f.verum().isSatisfiable()).isTrue();
        assertThat(f1.isSatisfiable()).isTrue();
        assertThat(f2.isSatisfiable()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsTautology(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.f.parse("(a | b) & (c | ~d)");
        final Formula f2 = _c.f.parse("(a & b) | (~a & b) | (a & ~b) | (~a & ~b)");
        assertThat(_c.f.falsum().isTautology()).isFalse();
        assertThat(_c.f.verum().isTautology()).isTrue();
        assertThat(f1.isTautology()).isFalse();
        assertThat(f2.isTautology()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsContradiction(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.f.parse("(a | b) & (c | ~d)");
        final Formula f2 = _c.f.parse("~a & ~b & (a | b)");
        assertThat(_c.f.falsum().isContradiction()).isTrue();
        assertThat(_c.f.verum().isContradiction()).isFalse();
        assertThat(f1.isContradiction()).isFalse();
        assertThat(f2.isContradiction()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testImplies(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.f.parse("(a | b) & (c | ~d)");
        final Formula f2 = _c.f.parse("(a | b) & (c | ~d) & (e | ~f)");
        final Formula f3 = _c.f.parse("(a | b) & (c | d)");
        assertThat(f1.implies(f2)).isFalse();
        assertThat(f2.implies(f1)).isTrue();
        assertThat(f1.implies(f3)).isFalse();
        assertThat(f2.implies(f3)).isFalse();
        assertThat(f2.implies(f2)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsImpliedBy(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.f.parse("(a | b) & (c | ~d)");
        final Formula f2 = _c.f.parse("(a | b) & (c | ~d) & (e | ~f)");
        final Formula f3 = _c.f.parse("(a | b) & (c | d)");
        assertThat(f1.isImpliedBy(f2)).isTrue();
        assertThat(f2.isImpliedBy(f1)).isFalse();
        assertThat(f1.isImpliedBy(f3)).isFalse();
        assertThat(f2.isImpliedBy(f3)).isFalse();
        assertThat(f2.isImpliedBy(f2)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsEquivalentTo(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.f.parse("(a | b) & (c | ~d)");
        final Formula f2 = _c.f.parse("(a | b) & (c | ~d) & (e | ~f)");
        final Formula f3 = _c.f.parse("(a & c) | (a & ~d) | (b & c) | (b & ~d)");
        assertThat(f1.isEquivalentTo(f2)).isFalse();
        assertThat(f2.isEquivalentTo(f1)).isFalse();
        assertThat(f1.isEquivalentTo(f3)).isTrue();
        assertThat(f3.isEquivalentTo(f1)).isTrue();
        assertThat(f2.isEquivalentTo(f3)).isFalse();
        assertThat(f2.isEquivalentTo(f2.transform(new BDDCNFTransformation(_c.f)))).isTrue();
    }
}
