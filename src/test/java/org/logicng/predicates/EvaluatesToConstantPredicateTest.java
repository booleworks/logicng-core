// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.predicates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;
import org.logicng.RandomTag;
import org.logicng.TestWithExampleFormulas;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.CType;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.PBConstraint;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.util.FormulaCornerCases;
import org.logicng.util.FormulaRandomizer;
import org.logicng.util.FormulaRandomizerConfig;

import java.util.HashMap;
import java.util.stream.Collectors;

public class EvaluatesToConstantPredicateTest extends TestWithExampleFormulas {

    private final EvaluatesToConstantPredicate emptyToFalse;
    private final EvaluatesToConstantPredicate aToFalse;
    private final EvaluatesToConstantPredicate aNotBToFalse;

    private final EvaluatesToConstantPredicate emptyToTrue;
    private final EvaluatesToConstantPredicate aToTrue;
    private final EvaluatesToConstantPredicate aNotBToTrue;

    public EvaluatesToConstantPredicateTest() {
        emptyToFalse = new EvaluatesToConstantPredicate(f, false, new HashMap<>());
        emptyToTrue = new EvaluatesToConstantPredicate(f, true, new HashMap<>());

        final HashMap<Variable, Boolean> aMap = new HashMap<>();
        aMap.put(A, true);
        aToFalse = new EvaluatesToConstantPredicate(f, false, aMap);
        aToTrue = new EvaluatesToConstantPredicate(f, true, aMap);

        final HashMap<Variable, Boolean> aNotBMap = new HashMap<>();
        aNotBMap.put(A, true);
        aNotBMap.put(B, false);
        aNotBToFalse = new EvaluatesToConstantPredicate(f, false, aNotBMap);
        aNotBToTrue = new EvaluatesToConstantPredicate(f, true, aNotBMap);
    }

    @Test
    public void getMapping() {
        assertThat(emptyToFalse.getMapping()).containsExactly();
        assertThat(aToFalse.getMapping()).containsExactly(entry(A, true));
        assertThat(aNotBToFalse.getMapping()).containsExactly(entry(A, true), entry(B, false));

        assertThat(emptyToTrue.getMapping()).containsExactly();
        assertThat(aToTrue.getMapping()).containsExactly(entry(A, true));
        assertThat(aNotBToTrue.getMapping()).containsExactly(entry(A, true), entry(B, false));
    }

    @Test
    public void testConstantsToFalse() {
        assertThat(f.falsum().holds(emptyToFalse)).isTrue();
        assertThat(f.falsum().holds(aToFalse)).isTrue();
        assertThat(f.falsum().holds(aNotBToFalse)).isTrue();

        assertThat(f.verum().holds(emptyToFalse)).isFalse();
        assertThat(f.verum().holds(aToFalse)).isFalse();
        assertThat(f.verum().holds(aNotBToFalse)).isFalse();
    }

    @Test
    public void testLiteralsToFalse() {
        assertThat(A.holds(emptyToFalse)).isFalse();
        assertThat(A.holds(aToFalse)).isFalse();
        assertThat(A.holds(aNotBToFalse)).isFalse();

        assertThat(NA.holds(emptyToFalse)).isFalse();
        assertThat(NA.holds(aToFalse)).isTrue();
        assertThat(NA.holds(aNotBToFalse)).isTrue();

        assertThat(B.holds(emptyToFalse)).isFalse();
        assertThat(B.holds(aToFalse)).isFalse();
        assertThat(B.holds(aNotBToFalse)).isTrue();

        assertThat(NB.holds(emptyToFalse)).isFalse();
        assertThat(NB.holds(aToFalse)).isFalse();
        assertThat(NB.holds(aNotBToFalse)).isFalse();
    }

    @Test
    public void testNotToFalse() throws ParserException {
        assertThat(f.parse("~~a").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~~a").holds(aToFalse)).isFalse();
        assertThat(f.parse("~~a").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("~~~a").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~~~a").holds(aToFalse)).isTrue();
        assertThat(f.parse("~~~a").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("~(a & b)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~(a & b)").holds(aToFalse)).isFalse();
        assertThat(f.parse("~(a & b)").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("~(~a & b)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~(~a & b)").holds(aToFalse)).isFalse();
        assertThat(f.parse("~(~a & b)").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("~(a & ~b)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~(a & ~b)").holds(aToFalse)).isFalse();
        assertThat(f.parse("~(a & ~b)").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("~(~a & ~b)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~(~a & ~b)").holds(aToFalse)).isFalse();
        assertThat(f.parse("~(~a & ~b)").holds(aNotBToFalse)).isFalse();
    }

    @Test
    public void testAndToFalse() throws ParserException {
        assertThat(f.parse("a & ~a").holds(emptyToFalse)).isTrue();
        assertThat(f.parse("a & ~a").holds(aToFalse)).isTrue();
        assertThat(f.parse("a & ~a").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("a & b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a & b").holds(aToFalse)).isFalse();
        assertThat(f.parse("a & b").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("~a & b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a & b").holds(aToFalse)).isTrue();
        assertThat(f.parse("~a & b").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("a & ~b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a & ~b").holds(aToFalse)).isFalse();
        assertThat(f.parse("a & ~b").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("~a & ~b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a & ~b").holds(aToFalse)).isTrue();
        assertThat(f.parse("~a & ~b").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("~a & ~b & c & ~d").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a & ~b & c & ~d").holds(aToFalse)).isTrue();
        assertThat(f.parse("~a & ~b & c & ~d").holds(aNotBToFalse)).isTrue();
    }

    @Test
    public void testOrToFalse() throws ParserException {
        assertThat(f.parse("a | b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a | b").holds(aToFalse)).isFalse();
        assertThat(f.parse("a | b").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("~a | b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a | b").holds(aToFalse)).isFalse();
        assertThat(f.parse("~a | b").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("a | ~b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a | ~b").holds(aToFalse)).isFalse();
        assertThat(f.parse("a | ~b").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("~a | ~b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a | ~b").holds(aToFalse)).isFalse();
        assertThat(f.parse("~a | ~b").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("~a | ~b | c | ~d").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a | ~b | c | ~d").holds(aToFalse)).isFalse();
        assertThat(f.parse("~a | ~b | c | ~d").holds(aNotBToFalse)).isFalse();
    }

    @Test
    public void testImplicationToFalse() throws ParserException {
        assertThat(f.parse("a => a").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a => a").holds(aToFalse)).isFalse();
        assertThat(f.parse("a => a").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("b => b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("b => b").holds(aToFalse)).isFalse();
        assertThat(f.parse("b => b").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("a => b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a => b").holds(aToFalse)).isFalse();
        assertThat(f.parse("a => b").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("~a => b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a => b").holds(aToFalse)).isFalse();
        assertThat(f.parse("~a => b").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("a => ~b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a => ~b").holds(aToFalse)).isFalse();
        assertThat(f.parse("a => ~b").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("~a => ~b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a => ~b").holds(aToFalse)).isFalse();
        assertThat(f.parse("~a => ~b").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("b => a").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("b => a").holds(aToFalse)).isFalse();
        assertThat(f.parse("b => a").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("~b => a").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~b => a").holds(aToFalse)).isFalse();
        assertThat(f.parse("~b => a").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("b => ~a").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("b => ~a").holds(aToFalse)).isFalse();
        assertThat(f.parse("b => ~a").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("~b => ~a").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~b => ~a").holds(aToFalse)).isFalse();
        assertThat(f.parse("~b => ~a").holds(aNotBToFalse)).isTrue();
    }

    @Test
    public void testEquivalenceToFalse() throws ParserException {
        assertThat(f.parse("a <=> a").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a <=> a").holds(aToFalse)).isFalse();
        assertThat(f.parse("a <=> a").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("b <=> b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("b <=> b").holds(aToFalse)).isFalse();
        assertThat(f.parse("b <=> b").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("a <=> b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a <=> b").holds(aToFalse)).isFalse();
        assertThat(f.parse("a <=> b").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("~a <=> b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a <=> b").holds(aToFalse)).isFalse();
        assertThat(f.parse("~a <=> b").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("a <=> ~b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a <=> ~b").holds(aToFalse)).isFalse();
        assertThat(f.parse("a <=> ~b").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("~a <=> ~b").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a <=> ~b").holds(aToFalse)).isFalse();
        assertThat(f.parse("~a <=> ~b").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("b <=> a").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("b <=> a").holds(aToFalse)).isFalse();
        assertThat(f.parse("b <=> a").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("~b <=> a").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~b <=> a").holds(aToFalse)).isFalse();
        assertThat(f.parse("~b <=> a").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("b <=> ~a").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("b <=> ~a").holds(aToFalse)).isFalse();
        assertThat(f.parse("b <=> ~a").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("~b <=> ~a").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~b <=> ~a").holds(aToFalse)).isFalse();
        assertThat(f.parse("~b <=> ~a").holds(aNotBToFalse)).isTrue();
    }

    @Test
    public void testPBCToFalse() {
        final PBConstraint pbc01 = (PBConstraint) f.pbc(CType.EQ, 2, new Literal[]{A, B}, new int[]{2, -4});
        assertThat(pbc01.holds(emptyToFalse)).isFalse();
        assertThat(pbc01.holds(aToFalse)).isFalse();
        assertThat(pbc01.holds(aNotBToFalse)).isFalse();

        final PBConstraint pbc02 = (PBConstraint) f.pbc(CType.GT, 2, new Literal[]{B, C}, new int[]{2, 1});
        assertThat(pbc02.holds(emptyToFalse)).isFalse();
        assertThat(pbc02.holds(aToFalse)).isFalse();
        assertThat(pbc02.holds(aNotBToFalse)).isTrue();

        assertThat(PBC1.holds(emptyToFalse)).isFalse();
        assertThat(PBC1.holds(aToFalse)).isFalse();
        assertThat(PBC1.holds(aNotBToFalse)).isFalse();

        assertThat(PBC2.holds(emptyToFalse)).isFalse();
        assertThat(PBC2.holds(aToFalse)).isFalse();
        assertThat(PBC2.holds(aNotBToFalse)).isFalse();
    }

    @Test
    public void testMixedToFalse() throws ParserException {
        assertThat(f.parse("~a & (a | ~b)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a & (a | ~b)").holds(aToFalse)).isTrue();
        assertThat(f.parse("~a & (a | ~b)").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("~b & (b | ~a)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~b & (b | ~a)").holds(aToFalse)).isTrue();
        assertThat(f.parse("~b & (b | ~a)").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("~a & (a | ~b) & c & (a => b | e)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a & (a | ~b) & c & (a => b | e)").holds(aToFalse)).isTrue();
        assertThat(f.parse("~a & (a | ~b) & c & (a => b | e)").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("~a & ~(a | ~b) & c & (a => b | e)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("~a & ~(a | ~b) & c & (a => b | e)").holds(aToFalse)).isTrue();
        assertThat(f.parse("~a & ~(a | ~b) & c & (a => b | e)").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("a & (a | ~b) & c & (a => b | e)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & c & (a => b | e)").holds(aToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & c & (a => b | e)").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("a & (a | ~b) & c & (a => ~b | e)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & c & (a => ~b | e)").holds(aToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & c & (a => ~b | e)").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("a & (a | ~b) & (a => b | e)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (a => b | e)").holds(aToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (a => b | e)").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("a & (a | ~b) & c & (a <=> ~b | e)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & c & (a <=> ~b | e)").holds(aToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & c & (a <=> ~b | e)").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("a & (a | ~b) & (a <=> b | e)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (a <=> b | e)").holds(aToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (a <=> b | e)").holds(aNotBToFalse)).isFalse();

        assertThat(f.parse("a & (a | ~b) & (a <=> b)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (a <=> b)").holds(aToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (a <=> b)").holds(aNotBToFalse)).isTrue();

        assertThat(f.parse("a & (a | ~b) & (3 * a + 2 * b > 4)").holds(emptyToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (3 * a + 2 * b > 4)").holds(aToFalse)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (3 * a + 2 * b > 4)").holds(aNotBToFalse)).isTrue();
    }

    @Test
    public void testConstantsToTrue() {
        assertThat(f.falsum().holds(emptyToTrue)).isFalse();
        assertThat(f.falsum().holds(aToTrue)).isFalse();
        assertThat(f.falsum().holds(aNotBToTrue)).isFalse();

        assertThat(f.verum().holds(emptyToTrue)).isTrue();
        assertThat(f.verum().holds(aToTrue)).isTrue();
        assertThat(f.verum().holds(aNotBToTrue)).isTrue();
    }

    @Test
    public void testLiteralsToTrue() {
        assertThat(A.holds(emptyToTrue)).isFalse();
        assertThat(A.holds(aToTrue)).isTrue();
        assertThat(A.holds(aNotBToTrue)).isTrue();

        assertThat(NA.holds(emptyToTrue)).isFalse();
        assertThat(NA.holds(aToTrue)).isFalse();
        assertThat(NA.holds(aNotBToTrue)).isFalse();

        assertThat(B.holds(emptyToTrue)).isFalse();
        assertThat(B.holds(aToTrue)).isFalse();
        assertThat(B.holds(aNotBToTrue)).isFalse();

        assertThat(NB.holds(emptyToTrue)).isFalse();
        assertThat(NB.holds(aToTrue)).isFalse();
        assertThat(NB.holds(aNotBToTrue)).isTrue();
    }

    @Test
    public void testNotToTrue() throws ParserException {
        assertThat(f.parse("~~a").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~~a").holds(aToTrue)).isTrue();
        assertThat(f.parse("~~a").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("~~~a").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~~~a").holds(aToTrue)).isFalse();
        assertThat(f.parse("~~~a").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("~(a & b)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~(a & b)").holds(aToTrue)).isFalse();
        assertThat(f.parse("~(a & b)").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("~(~a & b)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~(~a & b)").holds(aToTrue)).isTrue();
        assertThat(f.parse("~(~a & b)").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("~(a & ~b)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~(a & ~b)").holds(aToTrue)).isFalse();
        assertThat(f.parse("~(a & ~b)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("~(~a & ~b)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~(~a & ~b)").holds(aToTrue)).isTrue();
        assertThat(f.parse("~(~a & ~b)").holds(aNotBToTrue)).isTrue();
    }

    @Test
    public void testAndToTrue() throws ParserException {
        assertThat(f.parse("a & ~a").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a & ~a").holds(aToTrue)).isFalse();
        assertThat(f.parse("a & ~a").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("a & b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a & b").holds(aToTrue)).isFalse();
        assertThat(f.parse("a & b").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("~a & b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a & b").holds(aToTrue)).isFalse();
        assertThat(f.parse("~a & b").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("a & ~b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a & ~b").holds(aToTrue)).isFalse();
        assertThat(f.parse("a & ~b").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("~a & ~b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a & ~b").holds(aToTrue)).isFalse();
        assertThat(f.parse("~a & ~b").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("~a & ~b & c & ~d").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a & ~b & c & ~d").holds(aToTrue)).isFalse();
        assertThat(f.parse("~a & ~b & c & ~d").holds(aNotBToTrue)).isFalse();
    }

    @Test
    public void testOrToTrue() throws ParserException {
        assertThat(f.parse("a | b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a | b").holds(aToTrue)).isTrue();
        assertThat(f.parse("a | b").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("~a | b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a | b").holds(aToTrue)).isFalse();
        assertThat(f.parse("~a | b").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("a | ~b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a | ~b").holds(aToTrue)).isTrue();
        assertThat(f.parse("a | ~b").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("~a | ~b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a | ~b").holds(aToTrue)).isFalse();
        assertThat(f.parse("~a | ~b").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("~a | ~b | c | ~d").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a | ~b | c | ~d").holds(aToTrue)).isFalse();
        assertThat(f.parse("~a | ~b | c | ~d").holds(aNotBToTrue)).isTrue();
    }

    @Test
    public void testImplicationToTrue() throws ParserException {
        assertThat(f.parse("a => a").holds(emptyToTrue)).isTrue();
        assertThat(f.parse("a => a").holds(aToTrue)).isTrue();
        assertThat(f.parse("a => a").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("b => b").holds(emptyToTrue)).isTrue();
        assertThat(f.parse("b => b").holds(aToTrue)).isTrue();
        assertThat(f.parse("b => b").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("a => b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a => b").holds(aToTrue)).isFalse();
        assertThat(f.parse("a => b").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("~a => b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a => b").holds(aToTrue)).isTrue();
        assertThat(f.parse("~a => b").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("a => ~b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a => ~b").holds(aToTrue)).isFalse();
        assertThat(f.parse("a => ~b").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("~a => ~b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a => ~b").holds(aToTrue)).isTrue();
        assertThat(f.parse("~a => ~b").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("b => a").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("b => a").holds(aToTrue)).isTrue();
        assertThat(f.parse("b => a").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("~b => a").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~b => a").holds(aToTrue)).isTrue();
        assertThat(f.parse("~b => a").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("b => ~a").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("b => ~a").holds(aToTrue)).isFalse();
        assertThat(f.parse("b => ~a").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("~b => ~a").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~b => ~a").holds(aToTrue)).isFalse();
        assertThat(f.parse("~b => ~a").holds(aNotBToTrue)).isFalse();
    }

    @Test
    public void testEquivalenceToTrue() throws ParserException {
        assertThat(f.parse("a <=> a").holds(emptyToTrue)).isTrue();
        assertThat(f.parse("a <=> a").holds(aToTrue)).isTrue();
        assertThat(f.parse("a <=> a").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("b <=> b").holds(emptyToTrue)).isTrue();
        assertThat(f.parse("b <=> b").holds(aToTrue)).isTrue();
        assertThat(f.parse("b <=> b").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("a <=> b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a <=> b").holds(aToTrue)).isFalse();
        assertThat(f.parse("a <=> b").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("~a <=> b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a <=> b").holds(aToTrue)).isFalse();
        assertThat(f.parse("~a <=> b").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("a <=> ~b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a <=> ~b").holds(aToTrue)).isFalse();
        assertThat(f.parse("a <=> ~b").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("~a <=> ~b").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a <=> ~b").holds(aToTrue)).isFalse();
        assertThat(f.parse("~a <=> ~b").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("b <=> a").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("b <=> a").holds(aToTrue)).isFalse();
        assertThat(f.parse("b <=> a").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("~b <=> a").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~b <=> a").holds(aToTrue)).isFalse();
        assertThat(f.parse("~b <=> a").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("b <=> ~a").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("b <=> ~a").holds(aToTrue)).isFalse();
        assertThat(f.parse("b <=> ~a").holds(aNotBToTrue)).isTrue();

        assertThat(f.parse("~b <=> ~a").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~b <=> ~a").holds(aToTrue)).isFalse();
        assertThat(f.parse("~b <=> ~a").holds(aNotBToTrue)).isFalse();
    }

    @Test
    public void testPBCToTrue() {
        final PBConstraint pbc01 = (PBConstraint) f.pbc(CType.EQ, 2, new Literal[]{A, B}, new int[]{2, -4});
        assertThat(pbc01.holds(emptyToTrue)).isFalse();
        assertThat(pbc01.holds(aToTrue)).isFalse();
        assertThat(pbc01.holds(aNotBToTrue)).isTrue();

        final PBConstraint pbc02 = (PBConstraint) f.pbc(CType.GT, 2, new Literal[]{B, C}, new int[]{2, 1});
        assertThat(pbc02.holds(emptyToTrue)).isFalse();
        assertThat(pbc02.holds(aToTrue)).isFalse();
        assertThat(pbc02.holds(aNotBToTrue)).isFalse();

        assertThat(PBC1.holds(emptyToTrue)).isFalse();
        assertThat(PBC1.holds(aToTrue)).isFalse();
        assertThat(PBC1.holds(aNotBToTrue)).isFalse();

        assertThat(PBC2.holds(emptyToTrue)).isFalse();
        assertThat(PBC2.holds(aToTrue)).isFalse();
        assertThat(PBC2.holds(aNotBToTrue)).isFalse();
    }

    @Test
    public void testMixedToTrue() throws ParserException {
        assertThat(f.parse("~a & (a | ~b)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a & (a | ~b)").holds(aToTrue)).isFalse();
        assertThat(f.parse("~a & (a | ~b)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("~b & (b | ~a)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~b & (b | ~a)").holds(aToTrue)).isFalse();
        assertThat(f.parse("~b & (b | ~a)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("~a & (a | ~b)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a & (a | ~b)").holds(aToTrue)).isFalse();
        assertThat(f.parse("~a & (a | ~b)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("~b & (b | ~a)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~b & (b | ~a)").holds(aToTrue)).isFalse();
        assertThat(f.parse("~b & (b | ~a)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("~a & (a | ~b) & c & (a => b | e)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a & (a | ~b) & c & (a => b | e)").holds(aToTrue)).isFalse();
        assertThat(f.parse("~a & (a | ~b) & c & (a => b | e)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("~a & ~(a | ~b) & c & (a => b | e)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("~a & ~(a | ~b) & c & (a => b | e)").holds(aToTrue)).isFalse();
        assertThat(f.parse("~a & ~(a | ~b) & c & (a => b | e)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("a & (a | ~b) & c & (a => b | e)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & c & (a => b | e)").holds(aToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & c & (a => b | e)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("a & (a | ~b) & c & (a => ~b | e)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & c & (a => ~b | e)").holds(aToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & c & (a => ~b | e)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("a & (a | ~b) & (a => b | e)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (a => b | e)").holds(aToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (a => b | e)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("a & (a | ~b) & c & (a <=> ~b | e)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & c & (a <=> ~b | e)").holds(aToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & c & (a <=> ~b | e)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("a & (a | ~b) & (a <=> b | e)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (a <=> b | e)").holds(aToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (a <=> b | e)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("a & (a | ~b) & (a <=> b)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (a <=> b)").holds(aToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (a <=> b)").holds(aNotBToTrue)).isFalse();

        assertThat(f.parse("a & (a | ~b) & (3 * a + 2 * b > 4)").holds(emptyToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (3 * a + 2 * b > 4)").holds(aToTrue)).isFalse();
        assertThat(f.parse("a & (a | ~b) & (3 * a + 2 * b > 4)").holds(aNotBToTrue)).isFalse();
    }

    @Test
    public void testCornerCases() {
        final FormulaFactory f = FormulaFactory.caching();
        final FormulaCornerCases cornerCases = new FormulaCornerCases(f);
        for (final Formula formula : cornerCases.cornerCases()) {
            final Assignment assignment = new Assignment();
            assignment.addLiteral(f.literal("v0", false));
            assignment.addLiteral(f.literal("v1", false));
            assignment.addLiteral(f.literal("v2", true));
            assignment.addLiteral(f.literal("v3", true));
            final EvaluatesToConstantPredicate falseEvaluation = new EvaluatesToConstantPredicate(f, false,
                    assignment.literals().stream().collect(Collectors.toMap(Literal::variable, Literal::phase)));
            final EvaluatesToConstantPredicate trueEvaluation = new EvaluatesToConstantPredicate(f, true,
                    assignment.literals().stream().collect(Collectors.toMap(Literal::variable, Literal::phase)));
            final Formula restricted = formula.restrict(assignment);
            assertThat(restricted.type() == FType.FALSE).isEqualTo(formula.holds(falseEvaluation));
            assertThat(restricted.type() == FType.TRUE).isEqualTo(formula.holds(trueEvaluation));
        }
    }

    @Test
    @RandomTag
    public void testRandom() {
        for (int i = 0; i < 1000; i++) {
            final FormulaFactory f = FormulaFactory.caching();
            final Assignment assignment = new Assignment();
            assignment.addLiteral(f.literal("v0", false));
            assignment.addLiteral(f.literal("v1", false));
            assignment.addLiteral(f.literal("v2", true));
            assignment.addLiteral(f.literal("v3", true));
            final EvaluatesToConstantPredicate falseEvaluation = new EvaluatesToConstantPredicate(f, false,
                    assignment.literals().stream().collect(Collectors.toMap(Literal::variable, Literal::phase)));
            final EvaluatesToConstantPredicate trueEvaluation = new EvaluatesToConstantPredicate(f, true,
                    assignment.literals().stream().collect(Collectors.toMap(Literal::variable, Literal::phase)));
            final FormulaRandomizer randomizer = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().numVars(10).weightPbc(1).seed(i * 42).build());
            final Formula formula = randomizer.formula(6);
            final Formula restricted = formula.restrict(assignment);
            assertThat(restricted.type() == FType.FALSE).isEqualTo(formula.holds(falseEvaluation));
            assertThat(restricted.type() == FType.TRUE).isEqualTo(formula.holds(trueEvaluation));
        }
    }
}
