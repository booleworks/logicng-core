// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Substitution;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import java.util.Collections;
import java.util.HashMap;

public class SubstitutionTest extends TestWithExampleFormulas {

    private final Substitution subst;

    public SubstitutionTest() {
        subst = new Substitution();
        subst.addMapping(A, NA);
        subst.addMapping(B, OR1);
        subst.addMapping(X, AND1);
    }

    @Test
    public void testConstructor() {
        assertThat(new Substitution()).isNotNull();
    }

    @Test
    public void testCopyConstructor() {
        final Substitution original = new Substitution();
        original.addMapping(A, NA);
        original.addMapping(B, NB);
        final Substitution copy = new Substitution(original);
        copy.addMapping(X, NX);
        original.addMapping(Y, NY);
        assertThat(original.size()).isEqualTo(3);
        assertThat(copy.size()).isEqualTo(3);
        assertThat(copy.getSubstitution(A)).isEqualTo(NA);
        assertThat(copy.getSubstitution(B)).isEqualTo(NB);
        assertThat(original.getSubstitution(X)).isNull();
        assertThat(copy.getSubstitution(X)).isEqualTo(NX);
        assertThat(original.getSubstitution(Y)).isEqualTo(NY);
        assertThat(copy.getSubstitution(Y)).isNull();
    }

    @Test
    public void testConstructionWithMapping() {
        final HashMap<Variable, Formula> mapping = new HashMap<>();
        mapping.put(X, OR1);
        mapping.put(Y, AND1);
        final Substitution substitution = new Substitution(mapping);
        assertThat(substitution.getSubstitution(X)).isEqualTo(OR1);
        assertThat(substitution.getSubstitution(Y)).isEqualTo(AND1);
        assertThat(substitution.getSubstitution(A)).isNull();
        substitution.addMapping(A, NA);
        assertThat(substitution.getSubstitution(A)).isEqualTo(NA);
        assertThat(new Substitution(Collections.emptyMap()).size()).isEqualTo(0);
    }

    @Test
    public void testSize() {
        final Substitution subst = new Substitution();
        subst.addMapping(A, NA);
        subst.addMapping(B, OR1);
        subst.addMapping(C, AND1);
        assertThat(subst.size()).isEqualTo(3);
    }

    @Test
    public void testGetSubstitution() {
        final Substitution subst = new Substitution();
        subst.addMapping(A, NA);
        subst.addMapping(B, OR1);
        subst.addMapping(C, AND1);
        assertThat(subst.getSubstitution(A)).isEqualTo(NA);
        assertThat(subst.getSubstitution(B)).isEqualTo(OR1);
        assertThat(subst.getSubstitution(C)).isEqualTo(AND1);
        assertThat(subst.getSubstitution(X)).isNull();
        subst.addMapping(B, AND1);
        assertThat(subst.getSubstitution(B)).isEqualTo(AND1);
    }

    @Test
    public void testGetMapping() {
        final Substitution subst = new Substitution();
        assertThat(subst.getMapping()).isEqualTo(Collections.emptyMap());
        subst.addMapping(A, NA);
        subst.addMapping(B, OR1);
        subst.addMapping(C, AND1);
        final HashMap<Variable, Formula> expected = new HashMap<>();
        expected.put(A, NA);
        expected.put(B, OR1);
        expected.put(C, AND1);
        assertThat(subst.getMapping()).isEqualTo(expected);
    }

    @Test
    public void testConstantSubstitution() {
        assertThat(FALSE.substitute(subst)).isEqualTo(FALSE);
        assertThat(TRUE.substitute(subst)).isEqualTo(TRUE);
    }

    @Test
    public void testLiteralSubstitution() {
        assertThat(C.substitute(subst)).isEqualTo(C);
        assertThat(A.substitute(subst)).isEqualTo(NA);
        assertThat(B.substitute(subst)).isEqualTo(OR1);
        assertThat(X.substitute(subst)).isEqualTo(AND1);
        assertThat(NA.substitute(subst)).isEqualTo(A);
        assertThat(NB.substitute(subst)).isEqualTo(NOT2);
        assertThat(NX.substitute(subst)).isEqualTo(NOT1);
    }

    @Test
    public void testNotSubstitution() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(NOT1.substitute(subst)).isEqualTo(p.parse("~(~a & (x | y))"));
        assertThat(NOT2.substitute(subst)).isEqualTo(p.parse("~(a & b | y)"));
    }

    @Test
    public void testBinarySubstitution() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(IMP1.substitute(subst)).isEqualTo(p.parse("~a => (x | y)"));
        assertThat(IMP4.substitute(subst)).isEqualTo(p.parse("(~a <=> (x | y)) => (~(a & b) <=> ~y)"));
        assertThat(EQ2.substitute(subst)).isEqualTo(p.parse("a <=> ~(x | y)"));
        assertThat(EQ3.substitute(subst)).isEqualTo(p.parse("(~a & (x | y)) <=> (a & b | y)"));
    }

    @Test
    public void testNArySubstitution() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(AND3.substitute(subst)).isEqualTo(p.parse("(a & b | y) & (~(a & b) | ~y)"));
        assertThat(f.and(NB, C, X, NY).substitute(subst)).isEqualTo(p.parse("~(x | y) & c & a & b & ~y"));
        assertThat(OR3.substitute(subst)).isEqualTo(p.parse("(~a & (x | y)) | (a & ~(x | y))"));
        assertThat(f.or(A, NB, C, X, NY).substitute(subst)).isEqualTo(p.parse("~a | ~(x | y) | c | a & b | ~y"));
    }

    @Test
    public void testSingleSubstitution() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(A.substitute(A, OR1)).isEqualTo(p.parse("x | y"));
        assertThat(NA.substitute(A, OR1)).isEqualTo(p.parse("~(x | y)"));
        assertThat(IMP1.substitute(B, OR1)).isEqualTo(p.parse("a => (x | y)"));
        assertThat(EQ2.substitute(B, OR1)).isEqualTo(p.parse("~a <=> ~(x | y)"));
        assertThat(f.and(A, NB, C, NX, NY).substitute(Y, X)).isEqualTo(p.parse("a & ~b & c & ~x"));
        assertThat(f.or(A, NB, C, NX, NY).substitute(Y, X)).isEqualTo(p.parse("a | ~b | c | ~x"));
    }

    @Test
    public void testHashCode() {
        final Substitution subst = new Substitution();
        subst.addMapping(A, NA);
        subst.addMapping(B, OR1);
        subst.addMapping(C, AND1);
        final Substitution subst2 = new Substitution();
        subst2.addMapping(B, OR1);
        subst2.addMapping(C, AND1);
        subst2.addMapping(A, NA);
        assertThat(subst2.hashCode()).isEqualTo(subst.hashCode());
    }

    @Test
    public void testEquals() {
        final Substitution subst = new Substitution();
        subst.addMapping(A, NA);
        subst.addMapping(B, OR1);
        subst.addMapping(C, AND1);
        final Substitution subst2 = new Substitution();
        subst2.addMapping(B, OR1);
        subst2.addMapping(C, AND1);
        subst2.addMapping(A, NA);
        final Substitution subst3 = new Substitution();
        subst3.addMapping(B, OR1);
        subst3.addMapping(C, AND1);
        assertThat(subst2).isEqualTo(subst);
        assertThat(subst).isEqualTo(subst);
        assertThat(new Assignment()).isNotEqualTo(subst);
        assertThat(subst3).isNotEqualTo(subst);
    }

    @Test
    public void testToString() {
        final Substitution subst = new Substitution();
        assertThat(subst.toString()).isEqualTo("Substitution{}");
        subst.addMapping(A, NA);
        assertThat(subst.toString()).isEqualTo("Substitution{a=~a}");
        subst.addMapping(B, OR1);
        assertThat(subst.toString()).isEqualTo("Substitution{a=~a, b=x | y}");
    }

}
