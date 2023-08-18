// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.propositions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import java.util.Objects;

public class ExtendedPropositionTest {

    private final PropositionalParser p;
    private final ExtendedProposition<Backpack> prop1;
    private final ExtendedProposition<Backpack> prop2;

    public ExtendedPropositionTest() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        p = new PropositionalParser(f);
        prop1 = new ExtendedProposition<>(new Backpack("prop1"), p.parse("a & b"));
        prop2 = new ExtendedProposition<>(new Backpack("prop2"), p.parse("a & b & ~c"));
    }

    @Test
    public void testGetters() throws ParserException {
        assertThat(prop1.formula()).isEqualTo(p.parse("a & b"));
        assertThat(prop2.formula()).isEqualTo(p.parse("a & b & ~c"));

        assertThat(prop1.backpack().description).isEqualTo("prop1");
        assertThat(prop2.backpack().description).isEqualTo("prop2");
    }

    @Test
    public void testHashCode() throws ParserException {
        final ExtendedProposition<Backpack> prop11 = new ExtendedProposition<>(new Backpack("prop1"), p.parse("a & b"));
        assertThat(prop1.hashCode()).isEqualTo(prop1.hashCode());
        assertThat(prop11.hashCode()).isEqualTo(prop1.hashCode());
    }

    @Test
    public void testEquals() throws ParserException {
        final ExtendedProposition<Backpack> prop11 = new ExtendedProposition<>(new Backpack("prop1"), p.parse("a & b"));
        final ExtendedProposition<Backpack> prop21 = new ExtendedProposition<>(new Backpack("prop2"), p.parse("a & b & ~c"));
        assertThat(prop1.equals(prop1)).isTrue();
        assertThat(prop1.equals(prop11)).isTrue();
        assertThat(prop2.equals(prop21)).isTrue();
        assertThat(prop1.equals(prop2)).isFalse();
        assertThat(prop1.equals(null)).isFalse();
        assertThat(prop1.equals("String")).isFalse();
    }

    @Test
    public void testToString() {
        assertThat(prop1.toString()).isEqualTo("ExtendedProposition{formula=a & b, backpack=prop1}");
        assertThat(prop2.toString()).isEqualTo("ExtendedProposition{formula=a & b & ~c, backpack=prop2}");
    }

    private static final class Backpack implements PropositionBackpack {
        private final String description;

        private Backpack(final String description) {
            this.description = description;
        }

        @Override
        public int hashCode() {
            return description.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Backpack && Objects.equals(description, ((Backpack) obj).description);
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
