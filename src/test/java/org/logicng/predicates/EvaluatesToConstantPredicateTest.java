// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.RandomTag;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.CType;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.Literal;
import org.logicng.formulas.PBConstraint;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.util.FormulaCornerCases;
import org.logicng.util.FormulaRandomizer;
import org.logicng.util.FormulaRandomizerConfig;

import java.util.HashMap;
import java.util.stream.Collectors;

public class EvaluatesToConstantPredicateTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void getMapping(final FormulaContext _c) {
        final var e = new Preds(_c);

        assertThat(e.emptyToFalse.getMapping()).containsExactly();
        assertThat(e.aToFalse.getMapping()).containsExactly(entry(_c.a, true));
        assertThat(e.aNotBToFalse.getMapping()).containsExactly(entry(_c.a, true), entry(_c.b, false));

        assertThat(e.emptyToTrue.getMapping()).containsExactly();
        assertThat(e.aToTrue.getMapping()).containsExactly(entry(_c.a, true));
        assertThat(e.aNotBToTrue.getMapping()).containsExactly(entry(_c.a, true), entry(_c.b, false));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstantsToFalse(final FormulaContext _c) {
        final var e = new Preds(_c);

        assertThat(_c.f.falsum().holds(e.emptyToFalse)).isTrue();
        assertThat(_c.f.falsum().holds(e.aToFalse)).isTrue();
        assertThat(_c.f.falsum().holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.verum().holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.verum().holds(e.aToFalse)).isFalse();
        assertThat(_c.f.verum().holds(e.aNotBToFalse)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiteralsToFalse(final FormulaContext _c) {
        final var e = new Preds(_c);

        assertThat(_c.a.holds(e.emptyToFalse)).isFalse();
        assertThat(_c.a.holds(e.aToFalse)).isFalse();
        assertThat(_c.a.holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.na.holds(e.emptyToFalse)).isFalse();
        assertThat(_c.na.holds(e.aToFalse)).isTrue();
        assertThat(_c.na.holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.b.holds(e.emptyToFalse)).isFalse();
        assertThat(_c.b.holds(e.aToFalse)).isFalse();
        assertThat(_c.b.holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.nb.holds(e.emptyToFalse)).isFalse();
        assertThat(_c.nb.holds(e.aToFalse)).isFalse();
        assertThat(_c.nb.holds(e.aNotBToFalse)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNotToFalse(final FormulaContext _c) throws ParserException {
        final var e = new Preds(_c);

        assertThat(_c.f.parse("~~a").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~~a").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~~a").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("~~~a").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~~~a").holds(e.aToFalse)).isTrue();
        assertThat(_c.f.parse("~~~a").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("~(a & b)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~(a & b)").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~(a & b)").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("~(~a & b)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~(~a & b)").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~(~a & b)").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("~(a & ~b)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~(a & ~b)").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~(a & ~b)").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("~(~a & ~b)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~(~a & ~b)").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~(~a & ~b)").holds(e.aNotBToFalse)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAndToFalse(final FormulaContext _c) throws ParserException {
        final var e = new Preds(_c);

        assertThat(_c.f.parse("a & ~a").holds(e.emptyToFalse)).isTrue();
        assertThat(_c.f.parse("a & ~a").holds(e.aToFalse)).isTrue();
        assertThat(_c.f.parse("a & ~a").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("a & b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a & b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a & b").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("~a & b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a & b").holds(e.aToFalse)).isTrue();
        assertThat(_c.f.parse("~a & b").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("a & ~b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a & ~b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a & ~b").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("~a & ~b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a & ~b").holds(e.aToFalse)).isTrue();
        assertThat(_c.f.parse("~a & ~b").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("~a & ~b & c & ~d").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a & ~b & c & ~d").holds(e.aToFalse)).isTrue();
        assertThat(_c.f.parse("~a & ~b & c & ~d").holds(e.aNotBToFalse)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testOrToFalse(final FormulaContext _c) throws ParserException {
        final var e = new Preds(_c);

        assertThat(_c.f.parse("a | b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a | b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a | b").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("~a | b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a | b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~a | b").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("a | ~b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a | ~b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a | ~b").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("~a | ~b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a | ~b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~a | ~b").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("~a | ~b | c | ~d").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a | ~b | c | ~d").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~a | ~b | c | ~d").holds(e.aNotBToFalse)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testImplicationToFalse(final FormulaContext _c) throws ParserException {
        final var e = new Preds(_c);

        assertThat(_c.f.parse("a => a").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a => a").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a => a").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("b => b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("b => b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("b => b").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("a => b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a => b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a => b").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("~a => b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a => b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~a => b").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("a => ~b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a => ~b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a => ~b").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("~a => ~b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a => ~b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~a => ~b").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("b => a").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("b => a").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("b => a").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("~b => a").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~b => a").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~b => a").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("b => ~a").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("b => ~a").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("b => ~a").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("~b => ~a").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~b => ~a").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~b => ~a").holds(e.aNotBToFalse)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquivalenceToFalse(final FormulaContext _c) throws ParserException {
        final var e = new Preds(_c);

        assertThat(_c.f.parse("a <=> a").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a <=> a").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a <=> a").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("b <=> b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("b <=> b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("b <=> b").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("a <=> b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a <=> b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a <=> b").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("~a <=> b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a <=> b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~a <=> b").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("a <=> ~b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a <=> ~b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a <=> ~b").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("~a <=> ~b").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a <=> ~b").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~a <=> ~b").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("b <=> a").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("b <=> a").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("b <=> a").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("~b <=> a").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~b <=> a").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~b <=> a").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("b <=> ~a").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("b <=> ~a").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("b <=> ~a").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("~b <=> ~a").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~b <=> ~a").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("~b <=> ~a").holds(e.aNotBToFalse)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPBCToFalse(final FormulaContext _c) {
        final var e = new Preds(_c);

        final PBConstraint pbc01 = (PBConstraint) _c.f.pbc(CType.EQ, 2, new Literal[]{_c.a, _c.b}, new int[]{2, -4});
        assertThat(pbc01.holds(e.emptyToFalse)).isFalse();
        assertThat(pbc01.holds(e.aToFalse)).isFalse();
        assertThat(pbc01.holds(e.aNotBToFalse)).isFalse();

        final PBConstraint pbc02 = (PBConstraint) _c.f.pbc(CType.GT, 2, new Literal[]{_c.b, _c.c}, new int[]{2, 1});
        assertThat(pbc02.holds(e.emptyToFalse)).isFalse();
        assertThat(pbc02.holds(e.aToFalse)).isFalse();
        assertThat(pbc02.holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.pbc1.holds(e.emptyToFalse)).isFalse();
        assertThat(_c.pbc1.holds(e.aToFalse)).isFalse();
        assertThat(_c.pbc1.holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.pbc2.holds(e.emptyToFalse)).isFalse();
        assertThat(_c.pbc2.holds(e.aToFalse)).isFalse();
        assertThat(_c.pbc2.holds(e.aNotBToFalse)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testMixedToFalse(final FormulaContext _c) throws ParserException {
        final var e = new Preds(_c);

        assertThat(_c.f.parse("~a & (a | ~b)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a & (a | ~b)").holds(e.aToFalse)).isTrue();
        assertThat(_c.f.parse("~a & (a | ~b)").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("~b & (b | ~a)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~b & (b | ~a)").holds(e.aToFalse)).isTrue();
        assertThat(_c.f.parse("~b & (b | ~a)").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("~a & (a | ~b) & c & (a => b | e)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a & (a | ~b) & c & (a => b | e)").holds(e.aToFalse)).isTrue();
        assertThat(_c.f.parse("~a & (a | ~b) & c & (a => b | e)").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("~a & ~(a | ~b) & c & (a => b | e)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("~a & ~(a | ~b) & c & (a => b | e)").holds(e.aToFalse)).isTrue();
        assertThat(_c.f.parse("~a & ~(a | ~b) & c & (a => b | e)").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("a & (a | ~b) & c & (a => b | e)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & c & (a => b | e)").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & c & (a => b | e)").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("a & (a | ~b) & c & (a => ~b | e)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & c & (a => ~b | e)").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & c & (a => ~b | e)").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("a & (a | ~b) & (a => b | e)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (a => b | e)").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (a => b | e)").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("a & (a | ~b) & c & (a <=> ~b | e)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & c & (a <=> ~b | e)").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & c & (a <=> ~b | e)").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("a & (a | ~b) & (a <=> b | e)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (a <=> b | e)").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (a <=> b | e)").holds(e.aNotBToFalse)).isFalse();

        assertThat(_c.f.parse("a & (a | ~b) & (a <=> b)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (a <=> b)").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (a <=> b)").holds(e.aNotBToFalse)).isTrue();

        assertThat(_c.f.parse("a & (a | ~b) & (3 * a + 2 * b > 4)").holds(e.emptyToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (3 * a + 2 * b > 4)").holds(e.aToFalse)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (3 * a + 2 * b > 4)").holds(e.aNotBToFalse)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstantsToTrue(final FormulaContext _c) {
        final var e = new Preds(_c);

        assertThat(_c.f.falsum().holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.falsum().holds(e.aToTrue)).isFalse();
        assertThat(_c.f.falsum().holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.verum().holds(e.emptyToTrue)).isTrue();
        assertThat(_c.f.verum().holds(e.aToTrue)).isTrue();
        assertThat(_c.f.verum().holds(e.aNotBToTrue)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiteralsToTrue(final FormulaContext _c) {
        final var e = new Preds(_c);

        assertThat(_c.a.holds(e.emptyToTrue)).isFalse();
        assertThat(_c.a.holds(e.aToTrue)).isTrue();
        assertThat(_c.a.holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.na.holds(e.emptyToTrue)).isFalse();
        assertThat(_c.na.holds(e.aToTrue)).isFalse();
        assertThat(_c.na.holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.b.holds(e.emptyToTrue)).isFalse();
        assertThat(_c.b.holds(e.aToTrue)).isFalse();
        assertThat(_c.b.holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.nb.holds(e.emptyToTrue)).isFalse();
        assertThat(_c.nb.holds(e.aToTrue)).isFalse();
        assertThat(_c.nb.holds(e.aNotBToTrue)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNotToTrue(final FormulaContext _c) throws ParserException {
        final var e = new Preds(_c);

        assertThat(_c.f.parse("~~a").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~~a").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("~~a").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("~~~a").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~~~a").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~~~a").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("~(a & b)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~(a & b)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~(a & b)").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("~(~a & b)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~(~a & b)").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("~(~a & b)").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("~(a & ~b)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~(a & ~b)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~(a & ~b)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("~(~a & ~b)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~(~a & ~b)").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("~(~a & ~b)").holds(e.aNotBToTrue)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAndToTrue(final FormulaContext _c) throws ParserException {
        final var e = new Preds(_c);

        assertThat(_c.f.parse("a & ~a").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a & ~a").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a & ~a").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("a & b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a & b").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a & b").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("~a & b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a & b").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~a & b").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("a & ~b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a & ~b").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a & ~b").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("~a & ~b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a & ~b").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~a & ~b").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("~a & ~b & c & ~d").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a & ~b & c & ~d").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~a & ~b & c & ~d").holds(e.aNotBToTrue)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testOrToTrue(final FormulaContext _c) throws ParserException {
        final var e = new Preds(_c);

        assertThat(_c.f.parse("a | b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a | b").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("a | b").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("~a | b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a | b").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~a | b").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("a | ~b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a | ~b").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("a | ~b").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("~a | ~b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a | ~b").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~a | ~b").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("~a | ~b | c | ~d").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a | ~b | c | ~d").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~a | ~b | c | ~d").holds(e.aNotBToTrue)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testImplicationToTrue(final FormulaContext _c) throws ParserException {
        final var e = new Preds(_c);

        assertThat(_c.f.parse("a => a").holds(e.emptyToTrue)).isTrue();
        assertThat(_c.f.parse("a => a").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("a => a").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("b => b").holds(e.emptyToTrue)).isTrue();
        assertThat(_c.f.parse("b => b").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("b => b").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("a => b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a => b").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a => b").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("~a => b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a => b").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("~a => b").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("a => ~b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a => ~b").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a => ~b").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("~a => ~b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a => ~b").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("~a => ~b").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("b => a").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("b => a").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("b => a").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("~b => a").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~b => a").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("~b => a").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("b => ~a").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("b => ~a").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("b => ~a").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("~b => ~a").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~b => ~a").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~b => ~a").holds(e.aNotBToTrue)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquivalenceToTrue(final FormulaContext _c) throws ParserException {
        final var e = new Preds(_c);

        assertThat(_c.f.parse("a <=> a").holds(e.emptyToTrue)).isTrue();
        assertThat(_c.f.parse("a <=> a").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("a <=> a").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("b <=> b").holds(e.emptyToTrue)).isTrue();
        assertThat(_c.f.parse("b <=> b").holds(e.aToTrue)).isTrue();
        assertThat(_c.f.parse("b <=> b").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("a <=> b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a <=> b").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a <=> b").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("~a <=> b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a <=> b").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~a <=> b").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("a <=> ~b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a <=> ~b").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a <=> ~b").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("~a <=> ~b").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a <=> ~b").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~a <=> ~b").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("b <=> a").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("b <=> a").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("b <=> a").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("~b <=> a").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~b <=> a").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~b <=> a").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("b <=> ~a").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("b <=> ~a").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("b <=> ~a").holds(e.aNotBToTrue)).isTrue();

        assertThat(_c.f.parse("~b <=> ~a").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~b <=> ~a").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~b <=> ~a").holds(e.aNotBToTrue)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPBCToTrue(final FormulaContext _c) {
        final var e = new Preds(_c);

        final PBConstraint pbc01 = (PBConstraint) _c.f.pbc(CType.EQ, 2, new Literal[]{_c.a, _c.b}, new int[]{2, -4});
        assertThat(pbc01.holds(e.emptyToTrue)).isFalse();
        assertThat(pbc01.holds(e.aToTrue)).isFalse();
        assertThat(pbc01.holds(e.aNotBToTrue)).isTrue();

        final PBConstraint pbc02 = (PBConstraint) _c.f.pbc(CType.GT, 2, new Literal[]{_c.b, _c.c}, new int[]{2, 1});
        assertThat(pbc02.holds(e.emptyToTrue)).isFalse();
        assertThat(pbc02.holds(e.aToTrue)).isFalse();
        assertThat(pbc02.holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.pbc1.holds(e.emptyToTrue)).isFalse();
        assertThat(_c.pbc1.holds(e.aToTrue)).isFalse();
        assertThat(_c.pbc1.holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.pbc2.holds(e.emptyToTrue)).isFalse();
        assertThat(_c.pbc2.holds(e.aToTrue)).isFalse();
        assertThat(_c.pbc2.holds(e.aNotBToTrue)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testMixedToTrue(final FormulaContext _c) throws ParserException {
        final var e = new Preds(_c);

        assertThat(_c.f.parse("~a & (a | ~b)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a & (a | ~b)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~a & (a | ~b)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("~b & (b | ~a)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~b & (b | ~a)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~b & (b | ~a)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("~a & (a | ~b)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a & (a | ~b)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~a & (a | ~b)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("~b & (b | ~a)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~b & (b | ~a)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~b & (b | ~a)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("~a & (a | ~b) & c & (a => b | e)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a & (a | ~b) & c & (a => b | e)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~a & (a | ~b) & c & (a => b | e)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("~a & ~(a | ~b) & c & (a => b | e)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("~a & ~(a | ~b) & c & (a => b | e)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("~a & ~(a | ~b) & c & (a => b | e)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("a & (a | ~b) & c & (a => b | e)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & c & (a => b | e)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & c & (a => b | e)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("a & (a | ~b) & c & (a => ~b | e)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & c & (a => ~b | e)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & c & (a => ~b | e)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("a & (a | ~b) & (a => b | e)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (a => b | e)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (a => b | e)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("a & (a | ~b) & c & (a <=> ~b | e)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & c & (a <=> ~b | e)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & c & (a <=> ~b | e)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("a & (a | ~b) & (a <=> b | e)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (a <=> b | e)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (a <=> b | e)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("a & (a | ~b) & (a <=> b)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (a <=> b)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (a <=> b)").holds(e.aNotBToTrue)).isFalse();

        assertThat(_c.f.parse("a & (a | ~b) & (3 * a + 2 * b > 4)").holds(e.emptyToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (3 * a + 2 * b > 4)").holds(e.aToTrue)).isFalse();
        assertThat(_c.f.parse("a & (a | ~b) & (3 * a + 2 * b > 4)").holds(e.aNotBToTrue)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCornerCases(final FormulaContext _c) {
        final FormulaCornerCases cornerCases = new FormulaCornerCases(_c.f);
        for (final Formula formula : cornerCases.cornerCases()) {
            final Assignment assignment = new Assignment();
            assignment.addLiteral(_c.f.literal("v0", false));
            assignment.addLiteral(_c.f.literal("v1", false));
            assignment.addLiteral(_c.f.literal("v2", true));
            assignment.addLiteral(_c.f.literal("v3", true));
            final EvaluatesToConstantPredicate falseEvaluation = new EvaluatesToConstantPredicate(_c.f, false,
                    assignment.literals().stream().collect(Collectors.toMap(Literal::variable, Literal::phase)));
            final EvaluatesToConstantPredicate trueEvaluation = new EvaluatesToConstantPredicate(_c.f, true,
                    assignment.literals().stream().collect(Collectors.toMap(Literal::variable, Literal::phase)));
            final Formula restricted = formula.restrict(assignment);
            assertThat(restricted.type() == FType.FALSE).isEqualTo(formula.holds(falseEvaluation));
            assertThat(restricted.type() == FType.TRUE).isEqualTo(formula.holds(trueEvaluation));
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void testRandom(final FormulaContext _c) {
        for (int i = 0; i < 1000; i++) {
            final Assignment assignment = new Assignment();
            assignment.addLiteral(_c.f.literal("v0", false));
            assignment.addLiteral(_c.f.literal("v1", false));
            assignment.addLiteral(_c.f.literal("v2", true));
            assignment.addLiteral(_c.f.literal("v3", true));
            final EvaluatesToConstantPredicate falseEvaluation = new EvaluatesToConstantPredicate(_c.f, false,
                    assignment.literals().stream().collect(Collectors.toMap(Literal::variable, Literal::phase)));
            final EvaluatesToConstantPredicate trueEvaluation = new EvaluatesToConstantPredicate(_c.f, true,
                    assignment.literals().stream().collect(Collectors.toMap(Literal::variable, Literal::phase)));
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f, FormulaRandomizerConfig.builder().numVars(10).weightPbc(1).seed(i * 42).build());
            final Formula formula = randomizer.formula(6);
            final Formula restricted = formula.restrict(assignment);
            assertThat(restricted.type() == FType.FALSE).isEqualTo(formula.holds(falseEvaluation));
            assertThat(restricted.type() == FType.TRUE).isEqualTo(formula.holds(trueEvaluation));
        }
    }

    private static final class Preds {
        private final EvaluatesToConstantPredicate emptyToFalse;
        private final EvaluatesToConstantPredicate aToFalse;
        private final EvaluatesToConstantPredicate aNotBToFalse;

        private final EvaluatesToConstantPredicate emptyToTrue;
        private final EvaluatesToConstantPredicate aToTrue;
        private final EvaluatesToConstantPredicate aNotBToTrue;

        public Preds(final FormulaContext c) {
            emptyToFalse = new EvaluatesToConstantPredicate(c.f, false, new HashMap<>());
            emptyToTrue = new EvaluatesToConstantPredicate(c.f, true, new HashMap<>());

            final HashMap<Variable, Boolean> aMap = new HashMap<>();
            aMap.put(c.a, true);
            aToFalse = new EvaluatesToConstantPredicate(c.f, false, aMap);
            aToTrue = new EvaluatesToConstantPredicate(c.f, true, aMap);

            final HashMap<Variable, Boolean> aNotBMap = new HashMap<>();
            aNotBMap.put(c.a, true);
            aNotBMap.put(c.b, false);
            aNotBToFalse = new EvaluatesToConstantPredicate(c.f, false, aNotBMap);
            aNotBToTrue = new EvaluatesToConstantPredicate(c.f, true, aNotBMap);
        }
    }
}
