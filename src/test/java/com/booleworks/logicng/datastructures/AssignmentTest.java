// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AssignmentTest extends TestWithFormulaContext {

    final FormulaContext c = new FormulaContext(FormulaFactory.caching());

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEvaluateLit(final FormulaContext _c) {
        final Assignment ass = new Assignment(Arrays.asList(_c.a, _c.nx));
        assertThat(ass.evaluateLit(_c.a)).isTrue();
        assertThat(ass.evaluateLit(_c.nx)).isTrue();
        assertThat(ass.evaluateLit(_c.nb)).isTrue();
        assertThat(ass.evaluateLit(_c.na)).isFalse();
        assertThat(ass.evaluateLit(_c.x)).isFalse();
        assertThat(ass.evaluateLit(_c.b)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testRestrictLit(final FormulaContext _c) {
        final Assignment ass = new Assignment(Arrays.asList(_c.a, _c.nx));
        assertThat(ass.restrictLit(_c.f, _c.a)).isEqualTo(_c.verum);
        assertThat(ass.restrictLit(_c.f, _c.nx)).isEqualTo(_c.verum);
        assertThat(ass.restrictLit(_c.f, _c.na)).isEqualTo(_c.falsum);
        assertThat(ass.restrictLit(_c.f, _c.x)).isEqualTo(_c.falsum);
        assertThat(ass.restrictLit(_c.f, _c.b)).isEqualTo(_c.b);
        assertThat(ass.restrictLit(_c.f, _c.nb)).isEqualTo(_c.nb);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFormula(final FormulaContext _c) throws ParserException {
        assertThat(new Assignment(List.of(_c.a)).formula(_c.f)).isEqualTo(_c.p.parse("a"));
        assertThat(new Assignment(List.of(_c.na)).formula(_c.f)).isEqualTo(_c.p.parse("~a"));
        assertThat(new Assignment(Arrays.asList(_c.a, _c.b)).formula(_c.f)).isEqualTo(_c.p.parse("a & b"));
        assertThat(new Assignment(Arrays.asList(_c.a, _c.b, _c.nx, _c.ny)).formula(_c.f)).isEqualTo(_c.p.parse("a & b & ~x & ~y"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBlockingClause(final FormulaContext _c) throws ParserException {
        final Assignment ass = new Assignment();
        ass.addLiteral(_c.a);
        ass.addLiteral(_c.b);
        ass.addLiteral(_c.nx);
        ass.addLiteral(_c.ny);
        final Formula bc01 = ass.blockingClause(_c.f);
        assertThat(bc01.containsVariable(_c.c)).isFalse();
        assertThat(bc01).isEqualTo(_c.f.parse("~a | ~b | x | y"));
        final Formula bc02 = ass.blockingClause(_c.f, null);
        assertThat(bc02.containsVariable(_c.c)).isFalse();
        assertThat(bc02).isEqualTo(_c.f.parse("~a | ~b | x | y"));
        final List<Literal> lits = Arrays.asList(_c.a, _c.x, _c.c);
        final Formula bcProjected = ass.blockingClause(_c.f, lits);
        assertThat(bcProjected.containsVariable(_c.c)).isFalse();
        assertThat(bcProjected).isEqualTo(_c.f.parse("~a | x"));
    }

    @Test
    public void testCreators() {
        assertThat(new Assignment(Arrays.asList(c.a, c.b, c.x, c.y))).isNotNull();
    }

    @Test
    public void testSize() {
        assertThat(new Assignment(c.a, c.b, c.x, c.y).size()).isEqualTo(4);
        assertThat(new Assignment(c.a, c.nb).size()).isEqualTo(2);
    }

    @Test
    public void testPositiveVariables() {
        final Variable[] a = {c.a, c.b, c.x, c.y};
        Assignment ass1 = new Assignment(a);
        assertThat(ass1.positiveVariables()).containsExactly(a);
        ass1 = new Assignment(Arrays.asList(c.a, c.b, c.nx, c.ny));
        assertThat(ass1.positiveVariables()).containsExactly(c.a, c.b);
        ass1 = new Assignment(Arrays.asList(c.na, c.nb, c.nx, c.ny));
        assertThat(ass1.positiveVariables().size()).isEqualTo(0);
    }

    @Test
    public void testNegativeLiterals() {
        final Literal[] a = {c.na, c.nb, c.nx, c.ny};
        Assignment ass = new Assignment(Arrays.asList(a));
        assertThat(ass.negativeLiterals()).containsExactly(a);
        ass = new Assignment(Arrays.asList(c.a, c.b, c.nx, c.ny));
        assertThat(ass.negativeLiterals()).containsExactly(c.nx, c.ny);
        ass = new Assignment(Arrays.asList(c.a, c.b, c.x, c.y));
        assertThat(ass.negativeLiterals().size()).isEqualTo(0);
    }

    @Test
    public void testNegativeVariables() {
        final Variable[] a = {c.a, c.b, c.x, c.y};
        final Literal[] na = {c.na, c.nb, c.nx, c.ny};
        Assignment ass = new Assignment(Arrays.asList(na));
        assertThat(ass.negativeVariables()).containsExactly(a);
        ass = new Assignment(Arrays.asList(c.a, c.b, c.nx, c.ny));
        assertThat(ass.negativeVariables()).containsExactly(c.x, c.y);
        ass = new Assignment(Arrays.asList(c.a, c.b, c.x, c.y));
        assertThat(ass.negativeVariables().size()).isEqualTo(0);
    }

    @Test
    public void testAddLiteral() {
        final Assignment ass = new Assignment();
        ass.addLiteral(c.a);
        ass.addLiteral(c.b);
        ass.addLiteral(c.nx);
        ass.addLiteral(c.ny);
        assertThat(ass.positiveVariables()).containsExactly(c.a, c.b);
        assertThat(ass.negativeLiterals()).containsExactly(c.nx, c.ny);
    }

    @Test
    public void testHashCode() {
        final Assignment ass = new Assignment();
        ass.addLiteral(c.a);
        ass.addLiteral(c.b);
        ass.addLiteral(c.nx);
        ass.addLiteral(c.ny);
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny).hashCode()).isEqualTo(new Assignment(c.a, c.b, c.nx, c.ny).hashCode());
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny).hashCode()).isEqualTo(new Assignment(c.a, c.b, c.nx, c.ny).hashCode());
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny).hashCode()).isEqualTo(ass.hashCode());
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny).hashCode()).isEqualTo(new Assignment(c.a, c.b, c.nx, c.ny).hashCode());
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny).hashCode()).isEqualTo(new Assignment(c.a, c.b, c.nx, c.ny).hashCode());
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny).hashCode()).isEqualTo(ass.hashCode());
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny).hashCode()).isEqualTo(ass.hashCode());
    }

    @Test
    public void testEquals() {
        final Assignment ass = new Assignment();
        ass.addLiteral(c.a);
        ass.addLiteral(c.b);
        ass.addLiteral(c.nx);
        ass.addLiteral(c.ny);
        assertThat(ass).isNotEqualTo(null);
        assertThat(ass.equals(null)).isFalse();
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny)).isEqualTo(new Assignment(c.a, c.b, c.nx, c.ny));
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny)).isEqualTo(new Assignment(c.a, c.b, c.nx, c.ny));
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny)).isEqualTo(new Assignment(c.a, c.b, c.nx, c.ny));
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny)).isEqualTo(new Assignment(c.a, c.b, c.nx, c.ny));
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny)).isEqualTo(ass);
        assertThat(new Assignment(c.a, c.b, c.nx, c.ny)).isEqualTo(ass);
        assertThat(ass).isEqualTo(ass);
        assertThat(ass.equals(ass)).isTrue();
        assertThat(new Assignment(Arrays.asList(c.a, c.b, c.nx))).isNotEqualTo(ass);
        assertThat(new Assignment(Arrays.asList(c.a, c.b, c.nx, c.ny, c.c))).isNotEqualTo(ass);
        assertThat(c.verum).isNotEqualTo(ass);
    }

    @Test
    public void testToString() {
        assertThat(new Assignment().toString()).isEqualTo("Assignment{pos=[], neg=[]}");
        assertThat(new Assignment(Collections.singletonList(c.a)).toString()).isEqualTo("Assignment{pos=[a], neg=[]}");
        assertThat(new Assignment(Collections.singletonList(c.na)).toString()).isEqualTo("Assignment{pos=[], neg=[~a]}");
        assertThat(new Assignment(Arrays.asList(c.a, c.b, c.nx, c.ny, c.c)).toString()).isEqualTo("Assignment{pos=[a, b, c], neg=[~x, ~y]}");
    }
}
