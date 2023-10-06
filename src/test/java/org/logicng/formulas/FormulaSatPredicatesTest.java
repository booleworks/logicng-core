// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.io.parsers.ParserException;
import org.logicng.transformations.cnf.BDDCNFTransformation;

public class FormulaSatPredicatesTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsSatisfiable(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.f.parse("(a | b) & (c | ~d)");
        final Formula f2 = _c.f.parse("~a & ~b & (a | b)");
        assertThat(_c.f.falsum().isSatisfiable(_c.f)).isFalse();
        assertThat(_c.f.verum().isSatisfiable(_c.f)).isTrue();
        assertThat(f1.isSatisfiable(_c.f)).isTrue();
        assertThat(f2.isSatisfiable(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsTautology(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.f.parse("(a | b) & (c | ~d)");
        final Formula f2 = _c.f.parse("(a & b) | (~a & b) | (a & ~b) | (~a & ~b)");
        assertThat(_c.f.falsum().isTautology(_c.f)).isFalse();
        assertThat(_c.f.verum().isTautology(_c.f)).isTrue();
        assertThat(f1.isTautology(_c.f)).isFalse();
        assertThat(f2.isTautology(_c.f)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsContradiction(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.f.parse("(a | b) & (c | ~d)");
        final Formula f2 = _c.f.parse("~a & ~b & (a | b)");
        assertThat(_c.f.falsum().isContradiction(_c.f)).isTrue();
        assertThat(_c.f.verum().isContradiction(_c.f)).isFalse();
        assertThat(f1.isContradiction(_c.f)).isFalse();
        assertThat(f2.isContradiction(_c.f)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testImplies(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.f.parse("(a | b) & (c | ~d)");
        final Formula f2 = _c.f.parse("(a | b) & (c | ~d) & (e | ~f)");
        final Formula f3 = _c.f.parse("(a | b) & (c | d)");
        assertThat(f1.implies(f2, _c.f)).isFalse();
        assertThat(f2.implies(f1, _c.f)).isTrue();
        assertThat(f1.implies(f3, _c.f)).isFalse();
        assertThat(f2.implies(f3, _c.f)).isFalse();
        assertThat(f2.implies(f2, _c.f)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsImpliedBy(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.f.parse("(a | b) & (c | ~d)");
        final Formula f2 = _c.f.parse("(a | b) & (c | ~d) & (e | ~f)");
        final Formula f3 = _c.f.parse("(a | b) & (c | d)");
        assertThat(f1.isImpliedBy(f2, _c.f)).isTrue();
        assertThat(f2.isImpliedBy(f1, _c.f)).isFalse();
        assertThat(f1.isImpliedBy(f3, _c.f)).isFalse();
        assertThat(f2.isImpliedBy(f3, _c.f)).isFalse();
        assertThat(f2.isImpliedBy(f2, _c.f)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsEquivalentTo(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.f.parse("(a | b) & (c | ~d)");
        final Formula f2 = _c.f.parse("(a | b) & (c | ~d) & (e | ~f)");
        final Formula f3 = _c.f.parse("(a & c) | (a & ~d) | (b & c) | (b & ~d)");
        assertThat(f1.isEquivalentTo(f2, _c.f)).isFalse();
        assertThat(f2.isEquivalentTo(f1, _c.f)).isFalse();
        assertThat(f1.isEquivalentTo(f3, _c.f)).isTrue();
        assertThat(f3.isEquivalentTo(f1, _c.f)).isTrue();
        assertThat(f2.isEquivalentTo(f3, _c.f)).isFalse();
        assertThat(f2.isEquivalentTo(f2.transform(new BDDCNFTransformation(_c.f)), _c.f)).isTrue();
    }
}
