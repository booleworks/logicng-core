// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.propositions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class StandardPropositionTest {

    private final PropositionalParser p;
    private final StandardProposition prop1;
    private final StandardProposition prop2;

    public StandardPropositionTest() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        p = new PropositionalParser(f);
        prop1 = new StandardProposition(p.parse("a & b"));
        prop2 = new StandardProposition("prop2", p.parse("a & b & ~c"));
    }

    @Test
    public void testGetters() throws ParserException {
        Assertions.assertThat(prop1.formula()).isEqualTo(p.parse("a & b"));
        Assertions.assertThat(prop2.formula()).isEqualTo(p.parse("a & b & ~c"));

        assertThat(prop1.description()).isEqualTo("");
        assertThat(prop2.description()).isEqualTo("prop2");
    }

    @Test
    public void testHashCode() throws ParserException {
        final StandardProposition prop11 = new StandardProposition(p.parse("a & b"));
        final StandardProposition prop21 = new StandardProposition("prop2", p.parse("a & b & ~c"));
        assertThat(prop1.hashCode()).isEqualTo(prop1.hashCode());
        assertThat(prop11.hashCode()).isEqualTo(prop1.hashCode());
        assertThat(prop21.hashCode()).isEqualTo(prop2.hashCode());
    }

    @Test
    public void testEquals() throws ParserException {
        final StandardProposition prop11 = new StandardProposition(p.parse("a & b"));
        final StandardProposition prop21 = new StandardProposition("prop2", p.parse("a & b & ~c"));
        assertThat(prop1.equals(prop1)).isTrue();
        assertThat(prop1.equals(prop11)).isTrue();
        assertThat(prop2.equals(prop21)).isTrue();
        assertThat(prop1.equals(prop2)).isFalse();
        assertThat(prop1.equals(null)).isFalse();
        assertThat(prop1.equals("String")).isFalse();
    }

    @Test
    public void testToString() {
        assertThat(prop1.toString()).isEqualTo("StandardProposition{formula=a & b, description=}");
        assertThat(prop2.toString()).isEqualTo("StandardProposition{formula=a & b & ~c, description=prop2}");
    }
}
