// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.datastructures;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AssignmentTest extends TestWithExampleFormulas {

    @Test
    public void testCreators() {
        assertThat(new Assignment(Arrays.asList(A, B, X, Y))).isNotNull();
    }

    @Test
    public void testSize() {
        assertThat(new Assignment(Arrays.asList(A, B, X, Y), true).size()).isEqualTo(4);
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY), false).size()).isEqualTo(4);
        assertThat(new Assignment(Arrays.asList(A, NB)).size()).isEqualTo(2);
    }

    @Test
    public void testPositiveVariables() {
        final Variable[] a = {A, B, X, Y};
        Assignment ass1 = new Assignment(Arrays.asList(a), false);
        assertThat(ass1.positiveVariables()).containsExactly(a);
        ass1 = new Assignment(Arrays.asList(A, B, NX, NY));
        assertThat(ass1.positiveVariables()).containsExactly(A, B);
        ass1 = new Assignment(Arrays.asList(NA, NB, NX, NY));
        assertThat(ass1.positiveVariables().size()).isEqualTo(0);
    }

    @Test
    public void testNegativeLiterals() {
        final Literal[] a = {NA, NB, NX, NY};
        Assignment ass = new Assignment(Arrays.asList(a));
        assertThat(ass.negativeLiterals()).containsExactly(a);
        ass = new Assignment(Arrays.asList(A, B, NX, NY));
        assertThat(ass.negativeLiterals()).containsExactly(NX, NY);
        ass = new Assignment(Arrays.asList(A, B, X, Y));
        assertThat(ass.negativeLiterals().size()).isEqualTo(0);
    }

    @Test
    public void testNegativeVariables() {
        final Variable[] a = {A, B, X, Y};
        final Literal[] na = {NA, NB, NX, NY};
        Assignment ass = new Assignment(Arrays.asList(na));
        assertThat(ass.negativeVariables()).containsExactly(a);
        ass = new Assignment(Arrays.asList(A, B, NX, NY));
        assertThat(ass.negativeVariables()).containsExactly(X, Y);
        ass = new Assignment(Arrays.asList(A, B, X, Y));
        assertThat(ass.negativeVariables().size()).isEqualTo(0);
    }

    @Test
    public void testAddLiteral() {
        final Assignment ass = new Assignment();
        ass.addLiteral(A);
        ass.addLiteral(B);
        ass.addLiteral(NX);
        ass.addLiteral(NY);
        assertThat(ass.positiveVariables()).containsExactly(A, B);
        assertThat(ass.negativeLiterals()).containsExactly(NX, NY);
    }

    @Test
    public void testEvaluateLit() {
        final Assignment ass = new Assignment(Arrays.asList(A, NX));
        assertThat(ass.evaluateLit(A)).isTrue();
        assertThat(ass.evaluateLit(NX)).isTrue();
        assertThat(ass.evaluateLit(NB)).isTrue();
        assertThat(ass.evaluateLit(NA)).isFalse();
        assertThat(ass.evaluateLit(X)).isFalse();
        assertThat(ass.evaluateLit(B)).isFalse();
    }

    @Test
    public void testRestrictLit() {
        final Assignment ass = new Assignment(Arrays.asList(A, NX));
        assertThat(ass.restrictLit(A, f)).isEqualTo(TRUE);
        assertThat(ass.restrictLit(NX, f)).isEqualTo(TRUE);
        assertThat(ass.restrictLit(NA, f)).isEqualTo(FALSE);
        assertThat(ass.restrictLit(X, f)).isEqualTo(FALSE);
        assertThat(ass.restrictLit(B, f)).isEqualTo(B);
        assertThat(ass.restrictLit(NB, f)).isEqualTo(NB);
    }

    @Test
    public void testFormula() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(new Assignment(Collections.singletonList(A)).formula(f)).isEqualTo(p.parse("a"));
        assertThat(new Assignment(Collections.singletonList(NA)).formula(f)).isEqualTo(p.parse("~a"));
        assertThat(new Assignment(Arrays.asList(A, B)).formula(f)).isEqualTo(p.parse("a & b"));
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY)).formula(f)).isEqualTo(p.parse("a & b & ~x & ~y"));
    }

    @Test
    public void testFastEvaluable() {
        Assignment ass = new Assignment(Arrays.asList(A, NX), false);
        assertThat(ass.fastEvaluable()).isFalse();
        ass.convertToFastEvaluable();
        assertThat(ass.fastEvaluable()).isTrue();
        assertThat(ass.positiveVariables()).containsExactly(A);
        assertThat(ass.negativeLiterals()).containsExactly(NX);
        assertThat(ass.negativeVariables()).containsExactly(X);
        ass.addLiteral(NB);
        ass.addLiteral(Y);
        assertThat(ass.positiveVariables()).containsExactly(A, Y);
        assertThat(ass.negativeLiterals()).containsExactly(NB, NX);
        assertThat(ass.negativeVariables()).containsExactlyInAnyOrder(X, B);
        assertThat(ass.evaluateLit(Y)).isTrue();
        assertThat(ass.evaluateLit(B)).isFalse();
        assertThat(ass.restrictLit(NB, f)).isEqualTo(TRUE);
        assertThat(ass.restrictLit(X, f)).isEqualTo(FALSE);
        assertThat(ass.restrictLit(C, f)).isEqualTo(C);
        assertThat(ass.formula(f)).isEqualTo(f.and(A, NX, NB, Y));
        ass = new Assignment(Arrays.asList(A, NX), true);
        assertThat(ass.fastEvaluable()).isTrue();
        ass.convertToFastEvaluable();
        assertThat(ass.fastEvaluable()).isTrue();
    }

    @Test
    public void testHashCode() {
        final Assignment ass = new Assignment();
        ass.addLiteral(A);
        ass.addLiteral(B);
        ass.addLiteral(NX);
        ass.addLiteral(NY);
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY)).hashCode()).isEqualTo(new Assignment(Arrays.asList(A, B, NX, NY), true).hashCode());
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY)).hashCode()).isEqualTo(new Assignment(Arrays.asList(A, B, NX, NY), true).hashCode());
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY), true).hashCode()).isEqualTo(ass.hashCode());
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY)).hashCode()).isEqualTo(new Assignment(Arrays.asList(A, B, NX, NY)).hashCode());
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY)).hashCode()).isEqualTo(new Assignment(Arrays.asList(A, B, NX, NY), true).hashCode());
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY)).hashCode()).isEqualTo(ass.hashCode());
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY)).hashCode()).isEqualTo(ass.hashCode());
    }

    @Test
    public void testEquals() {
        final Assignment ass = new Assignment();
        ass.addLiteral(A);
        ass.addLiteral(B);
        ass.addLiteral(NX);
        ass.addLiteral(NY);
        final Assignment fastAss = new Assignment(true);
        fastAss.addLiteral(A);
        fastAss.addLiteral(B);
        fastAss.addLiteral(NX);
        fastAss.addLiteral(NY);
        assertThat(ass).isNotEqualTo(null);
        assertThat(ass.equals(null)).isFalse();
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY), false)).isEqualTo(new Assignment(Arrays.asList(A, B, NX, NY), false));
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY), true)).isEqualTo(new Assignment(Arrays.asList(A, B, NX, NY), false));
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY), false)).isEqualTo(new Assignment(Arrays.asList(A, B, NX, NY), true));
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY), true)).isEqualTo(new Assignment(Arrays.asList(A, B, NX, NY), true));
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY))).isEqualTo(ass);
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY))).isEqualTo(ass);
        assertThat(ass).isEqualTo(ass);
        assertThat(ass.equals(ass)).isTrue();
        assertThat(new Assignment(Arrays.asList(A, B, NX))).isNotEqualTo(ass);
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY, C))).isNotEqualTo(ass);
        assertThat(TRUE).isNotEqualTo(ass);
    }

    @Test
    public void testBlockingClause() throws ParserException {
        final Assignment ass = new Assignment();
        ass.addLiteral(A);
        ass.addLiteral(B);
        ass.addLiteral(NX);
        ass.addLiteral(NY);
        final Formula bc01 = ass.blockingClause(f);
        assertThat(bc01.containsVariable(C)).isFalse();
        assertThat(bc01).isEqualTo(f.parse("~a | ~b | x | y"));
        final Formula bc02 = ass.blockingClause(f, null);
        assertThat(bc02.containsVariable(C)).isFalse();
        assertThat(bc02).isEqualTo(f.parse("~a | ~b | x | y"));
        final List<Literal> lits = Arrays.asList(A, X, C);
        final Formula bcProjected = ass.blockingClause(f, lits);
        assertThat(bcProjected.containsVariable(C)).isFalse();
        assertThat(bcProjected).isEqualTo(f.parse("~a | x"));
    }

    @Test
    public void testToString() {
        assertThat(new Assignment().toString()).isEqualTo("Assignment{pos=[], neg=[]}");
        assertThat(new Assignment(Collections.singletonList(A)).toString()).isEqualTo("Assignment{pos=[a], neg=[]}");
        assertThat(new Assignment(Collections.singletonList(NA)).toString()).isEqualTo("Assignment{pos=[], neg=[~a]}");
        assertThat(new Assignment(Arrays.asList(A, B, NX, NY, C)).toString()).isEqualTo("Assignment{pos=[a, b, c], neg=[~x, ~y]}");
    }
}
