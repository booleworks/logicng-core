// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Substitution;
import com.booleworks.logicng.io.parsers.ParserException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.HashMap;

public class SubstitutionTest extends TestWithFormulaContext {

    private final FormulaContext _s = new FormulaContext(FormulaFactory.nonCaching());

    private Substitution getSubstitution(final FormulaContext context) {
        final Substitution subst;
        subst = new Substitution();
        subst.addMapping(context.a, context.na);
        subst.addMapping(context.b, context.or1);
        subst.addMapping(context.x, context.and1);
        return subst;
    }

    @Test
    public void testConstructor() {
        assertThat(new Substitution()).isNotNull();
    }

    @Test
    public void testCopyConstructor() {
        final Substitution original = new Substitution();
        original.addMapping(_s.a, _s.na);
        original.addMapping(_s.b, _s.nb);
        final Substitution copy = new Substitution(original);
        copy.addMapping(_s.x, _s.nx);
        original.addMapping(_s.y, _s.ny);
        assertThat(original.size()).isEqualTo(3);
        assertThat(copy.size()).isEqualTo(3);
        assertThat(copy.getSubstitution(_s.a)).isEqualTo(_s.na);
        assertThat(copy.getSubstitution(_s.b)).isEqualTo(_s.nb);
        assertThat(original.getSubstitution(_s.x)).isNull();
        assertThat(copy.getSubstitution(_s.x)).isEqualTo(_s.nx);
        assertThat(original.getSubstitution(_s.y)).isEqualTo(_s.ny);
        assertThat(copy.getSubstitution(_s.y)).isNull();
    }

    @Test
    public void testConstructionWithMapping() {
        final HashMap<Variable, Formula> mapping = new HashMap<>();
        mapping.put(_s.x, _s.or1);
        mapping.put(_s.y, _s.and1);
        final Substitution substitution = new Substitution(mapping);
        assertThat(substitution.getSubstitution(_s.x)).isEqualTo(_s.or1);
        assertThat(substitution.getSubstitution(_s.y)).isEqualTo(_s.and1);
        assertThat(substitution.getSubstitution(_s.a)).isNull();
        substitution.addMapping(_s.a, _s.na);
        assertThat(substitution.getSubstitution(_s.a)).isEqualTo(_s.na);
        assertThat(new Substitution(Collections.emptyMap()).size()).isEqualTo(0);
    }

    @Test
    public void testSize() {
        final Substitution subst = new Substitution();
        subst.addMapping(_s.a, _s.na);
        subst.addMapping(_s.b, _s.or1);
        subst.addMapping(_s.c, _s.and1);
        assertThat(subst.size()).isEqualTo(3);
    }

    @Test
    public void testGetSubstitution() {
        final Substitution subst = new Substitution();
        subst.addMapping(_s.a, _s.na);
        subst.addMapping(_s.b, _s.or1);
        subst.addMapping(_s.c, _s.and1);
        assertThat(subst.getSubstitution(_s.a)).isEqualTo(_s.na);
        assertThat(subst.getSubstitution(_s.b)).isEqualTo(_s.or1);
        assertThat(subst.getSubstitution(_s.c)).isEqualTo(_s.and1);
        assertThat(subst.getSubstitution(_s.x)).isNull();
        subst.addMapping(_s.b, _s.and1);
        assertThat(subst.getSubstitution(_s.b)).isEqualTo(_s.and1);
    }

    @Test
    public void testGetMapping() {
        final Substitution subst = new Substitution();
        assertThat(subst.getMapping()).isEqualTo(Collections.emptyMap());
        subst.addMapping(_s.a, _s.na);
        subst.addMapping(_s.b, _s.or1);
        subst.addMapping(_s.c, _s.and1);
        final HashMap<Variable, Formula> expected = new HashMap<>();
        expected.put(_s.a, _s.na);
        expected.put(_s.b, _s.or1);
        expected.put(_s.c, _s.and1);
        assertThat(subst.getMapping()).isEqualTo(expected);
    }

    @Test
    public void testHashCode() {
        final Substitution subst = new Substitution();
        subst.addMapping(_s.a, _s.na);
        subst.addMapping(_s.b, _s.or1);
        subst.addMapping(_s.c, _s.and1);
        final Substitution subst2 = new Substitution();
        subst2.addMapping(_s.b, _s.or1);
        subst2.addMapping(_s.c, _s.and1);
        subst2.addMapping(_s.a, _s.na);
        assertThat(subst2.hashCode()).isEqualTo(subst.hashCode());
    }

    @Test
    public void testEquals() {
        final Substitution subst = new Substitution();
        subst.addMapping(_s.a, _s.na);
        subst.addMapping(_s.b, _s.or1);
        subst.addMapping(_s.c, _s.and1);
        final Substitution subst2 = new Substitution();
        subst2.addMapping(_s.b, _s.or1);
        subst2.addMapping(_s.c, _s.and1);
        subst2.addMapping(_s.a, _s.na);
        final Substitution subst3 = new Substitution();
        subst3.addMapping(_s.b, _s.or1);
        subst3.addMapping(_s.c, _s.and1);
        assertThat(subst2).isEqualTo(subst);
        assertThat(subst).isEqualTo(subst);
        Assertions.assertThat(new Assignment()).isNotEqualTo(subst);
        assertThat(subst3).isNotEqualTo(subst);
    }

    @Test
    public void testToString() {
        final Substitution subst = new Substitution();
        assertThat(subst.toString()).isEqualTo("Substitution{}");
        subst.addMapping(_s.a, _s.na);
        assertThat(subst.toString()).isEqualTo("Substitution{a=~a}");
        subst.addMapping(_s.b, _s.or1);
        assertThat(subst.toString()).isEqualTo("Substitution{a=~a, b=x | y}");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstantSubstitution(final FormulaContext _c) {
        final Substitution subst = getSubstitution(_c);
        assertThat(_c.falsum.substitute(_c.f, subst)).isEqualTo(_c.falsum);
        assertThat(_c.verum.substitute(_c.f, subst)).isEqualTo(_c.verum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiteralSubstitution(final FormulaContext _c) {
        final Substitution subst = getSubstitution(_c);
        assertThat(_c.c.substitute(_c.f, subst)).isEqualTo(_c.c);
        assertThat(_c.a.substitute(_c.f, subst)).isEqualTo(_c.na);
        assertThat(_c.b.substitute(_c.f, subst)).isEqualTo(_c.or1);
        assertThat(_c.x.substitute(_c.f, subst)).isEqualTo(_c.and1);
        assertThat(_c.na.substitute(_c.f, subst)).isEqualTo(_c.a);
        assertThat(_c.nb.substitute(_c.f, subst)).isEqualTo(_c.not2);
        assertThat(_c.nx.substitute(_c.f, subst)).isEqualTo(_c.not1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNotSubstitution(final FormulaContext _c) throws ParserException {
        final Substitution subst = getSubstitution(_c);
        assertThat(_c.not1.substitute(_c.f, subst)).isEqualTo(_c.p.parse("~(~a & (x | y))"));
        assertThat(_c.not2.substitute(_c.f, subst)).isEqualTo(_c.p.parse("~(a & b | y)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinarySubstitution(final FormulaContext _c) throws ParserException {
        final Substitution subst = getSubstitution(_c);
        assertThat(_c.imp1.substitute(_c.f, subst)).isEqualTo(_c.p.parse("~a => (x | y)"));
        assertThat(_c.imp4.substitute(_c.f, subst)).isEqualTo(_c.p.parse("(~a <=> (x | y)) => (~(a & b) <=> ~y)"));
        assertThat(_c.eq2.substitute(_c.f, subst)).isEqualTo(_c.p.parse("a <=> ~(x | y)"));
        assertThat(_c.eq3.substitute(_c.f, subst)).isEqualTo(_c.p.parse("(~a & (x | y)) <=> (a & b | y)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNArySubstitution(final FormulaContext _c) throws ParserException {
        final Substitution subst = getSubstitution(_c);
        assertThat(_c.and3.substitute(_c.f, subst)).isEqualTo(_c.p.parse("(a & b | y) & (~(a & b) | ~y)"));
        assertThat(_c.f.and(_c.nb, _c.c, _c.x, _c.ny).substitute(_c.f, subst))
                .isEqualTo(_c.p.parse("~(x | y) & c & a & b & ~y"));
        assertThat(_c.or3.substitute(_c.f, subst)).isEqualTo(_c.p.parse("(~a & (x | y)) | (a & ~(x | y))"));
        assertThat(_c.f.or(_c.a, _c.nb, _c.c, _c.x, _c.ny).substitute(_c.f, subst))
                .isEqualTo(_c.p.parse("~a | ~(x | y) | c | a & b | ~y"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSingleSubstitution(final FormulaContext _c) throws ParserException {
        assertThat(_c.a.substitute(_c.f, _c.a, _c.or1)).isEqualTo(_c.p.parse("x | y"));
        assertThat(_c.na.substitute(_c.f, _c.a, _c.or1)).isEqualTo(_c.p.parse("~(x | y)"));
        assertThat(_c.imp1.substitute(_c.f, _c.b, _c.or1)).isEqualTo(_c.p.parse("a => (x | y)"));
        assertThat(_c.eq2.substitute(_c.f, _c.b, _c.or1)).isEqualTo(_c.p.parse("~a <=> ~(x | y)"));
        assertThat(_c.f.and(_c.a, _c.nb, _c.c, _c.nx, _c.ny).substitute(_c.f, _c.y, _c.x))
                .isEqualTo(_c.p.parse("a & ~b & c & ~x"));
        assertThat(_c.f.or(_c.a, _c.nb, _c.c, _c.nx, _c.ny).substitute(_c.f, _c.y, _c.x))
                .isEqualTo(_c.p.parse("a | ~b | c | ~x"));
    }
}
