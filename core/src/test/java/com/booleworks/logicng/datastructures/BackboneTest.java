// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public class BackboneTest {
    private final FormulaFactory f = FormulaFactory.caching();
    private final PropositionalParser p = new PropositionalParser(f);
    private final Variable a1 = f.variable("a1");
    private final Variable a2 = f.variable("a2");
    private final Variable a3 = f.variable("a3");
    private final Variable b1 = f.variable("b1");
    private final Variable b2 = f.variable("b2");
    private final Variable b3 = f.variable("b3");
    private final Variable x1 = f.variable("x1");
    private final Variable x2 = f.variable("x2");
    private final Variable x3 = f.variable("x3");

    @Test
    public void testToFormula() throws ParserException {
        Backbone backbone = Backbone.satBackbone(set(a1, a2, a3), null, null);
        assertThat(backbone.toFormula(f)).isEqualTo(p.parse("a1 & a2 & a3"));
        backbone = Backbone.satBackbone(null, set(b1, b2, b3), null);
        assertThat(backbone.toFormula(f)).isEqualTo(p.parse("~b1 & ~b2 & ~b3"));
        backbone = Backbone.satBackbone(set(a1, a2, a3), set(b1), null);
        assertThat(backbone.toFormula(f)).isEqualTo(p.parse("a1 & a2 & a3 & ~b1"));
        backbone = Backbone.satBackbone(set(a1, a2, a3), null, set(x1, x2, x3));
        assertThat(backbone.toFormula(f)).isEqualTo(p.parse("a1 & a2 & a3"));
        backbone = Backbone.satBackbone(null, set(b1, b2, b3), set(x1, x2, x3));
        assertThat(backbone.toFormula(f)).isEqualTo(p.parse("~b1 & ~b2 & ~b3"));
        backbone = Backbone.satBackbone(set(a1), set(b1, b2, b3), set(x1));
        assertThat(backbone.toFormula(f)).isEqualTo(p.parse("a1 & ~b1 & ~b2 & ~b3"));
    }

    @Test
    public void testToMap() {
        Backbone backbone = Backbone.satBackbone(set(a1, a2), null, null);
        assertThat(backbone.toMap()).containsExactly(
                MapEntry.entry(a1, Tristate.TRUE),
                MapEntry.entry(a2, Tristate.TRUE)
        );
        backbone = Backbone.satBackbone(null, set(b1, b2, b3), null);
        assertThat(backbone.toMap()).containsExactly(
                MapEntry.entry(b1, Tristate.FALSE),
                MapEntry.entry(b2, Tristate.FALSE),
                MapEntry.entry(b3, Tristate.FALSE)
        );
        backbone = Backbone.satBackbone(set(a1, a2, a3), set(b1), null);
        assertThat(backbone.toMap()).containsExactly(
                MapEntry.entry(a1, Tristate.TRUE),
                MapEntry.entry(a2, Tristate.TRUE),
                MapEntry.entry(a3, Tristate.TRUE),
                MapEntry.entry(b1, Tristate.FALSE)
        );
        backbone = Backbone.satBackbone(set(a1, a2, a3), null, set(x1, x2, x3));
        assertThat(backbone.toMap()).containsExactly(
                MapEntry.entry(a1, Tristate.TRUE),
                MapEntry.entry(a2, Tristate.TRUE),
                MapEntry.entry(a3, Tristate.TRUE),
                MapEntry.entry(x1, Tristate.UNDEF),
                MapEntry.entry(x2, Tristate.UNDEF),
                MapEntry.entry(x3, Tristate.UNDEF)
        );
        backbone = Backbone.satBackbone(null, set(b1, b2, b3), set(x1, x2, x3));
        assertThat(backbone.toMap()).containsExactly(
                MapEntry.entry(b1, Tristate.FALSE),
                MapEntry.entry(b2, Tristate.FALSE),
                MapEntry.entry(b3, Tristate.FALSE),
                MapEntry.entry(x1, Tristate.UNDEF),
                MapEntry.entry(x2, Tristate.UNDEF),
                MapEntry.entry(x3, Tristate.UNDEF)
        );
        backbone = Backbone.satBackbone(set(a1), set(b1, b2), set(x1));
        assertThat(backbone.toMap()).containsExactly(
                MapEntry.entry(a1, Tristate.TRUE),
                MapEntry.entry(b1, Tristate.FALSE),
                MapEntry.entry(b2, Tristate.FALSE),
                MapEntry.entry(x1, Tristate.UNDEF)
        );
    }

    @Test
    public void testUnsatBackbone() {
        final Backbone backbone = Backbone.unsatBackbone();
        assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        assertThat(backbone.getNegativeBackbone()).isEmpty();
        assertThat(backbone.getPositiveBackbone()).isEmpty();
        assertThat(backbone.getOptionalVariables()).isEmpty();
        assertThat(backbone.toMap()).isEmpty();
        assertThat(backbone.toFormula(f)).isEqualTo(f.falsum());
    }

    @Test
    public void testToString() {
        final Backbone backbone = Backbone.satBackbone(set(a1, a2, a3), set(b1, b2, b3), set(x1, x2, x3));
        assertThat(backbone.toString()).isEqualTo(
                "Backbone{sat=true, positiveBackbone=[a1, a2, a3], negativeBackbone=[b1, b2, b3], optionalVariables=[x1, x2, x3]}");
    }

    @Test
    public void testEqualsAndHashCode() {
        final Backbone backbone1a = Backbone.satBackbone(set(a1, a2, a3), null, null);
        final Backbone backbone1b = Backbone.satBackbone(set(a1, a2, a3), null, null);
        final Backbone backbone3 = Backbone.satBackbone(set(a1, a2, a3), set(b1), null);
        final Backbone backbone5 = Backbone.satBackbone(null, set(b1, b2, b3), set(x1, x2, x3));
        final Backbone satBB = Backbone.satBackbone(Collections.emptySortedSet(), Collections.emptySortedSet(),
                Collections.emptySortedSet());
        final Backbone unsatBB = Backbone.unsatBackbone();

        assertThat(backbone1a.hashCode()).isEqualTo(backbone1b.hashCode());
        assertThat(backbone1a.equals(backbone1a)).isTrue();
        assertThat(backbone1a.equals(backbone1b)).isTrue();
        assertThat(backbone1a.equals(backbone3)).isFalse();
        assertThat(backbone1a.equals(backbone5)).isFalse();
        assertThat(backbone1a.equals("String")).isFalse();
        assertThat(backbone1a.equals(null)).isFalse();
        assertThat(satBB.equals(unsatBB)).isFalse();
    }

    private SortedSet<Variable> set(final Variable... variables) {
        return new TreeSet<>(Arrays.asList(variables));
    }
}
