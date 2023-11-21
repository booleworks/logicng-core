// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.propositions.StandardProposition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class UNSATCoreTest {

    private final List<StandardProposition> props1;
    private final List<StandardProposition> props2;
    private final UNSATCore<?> core1;
    private final UNSATCore<?> core2;

    public UNSATCoreTest() throws ParserException {
        props1 = new ArrayList<>();
        props2 = new ArrayList<>();
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser parser = new PropositionalParser(f);
        props1.add(new StandardProposition(parser.parse("a | b")));
        props1.add(new StandardProposition(parser.parse("~a | b")));
        props1.add(new StandardProposition(parser.parse("a | ~b")));
        props1.add(new StandardProposition(parser.parse("~a | ~b")));
        props2.add(new StandardProposition(parser.parse("a | b")));
        props2.add(new StandardProposition(parser.parse("~a | b")));
        props2.add(new StandardProposition(parser.parse("a | ~b")));
        props2.add(new StandardProposition(parser.parse("~a | ~b")));
        props2.add(new StandardProposition(parser.parse("~a | ~b | c")));
        core1 = new UNSATCore<>(props1, true);
        core2 = new UNSATCore<>(props2, false);
    }

    @Test
    public void testGetters() {
        Assertions.assertThat(core1.propositions()).isEqualTo(props1);
        Assertions.assertThat(core2.propositions()).isEqualTo(props2);
        assertThat(core1.isMUS()).isTrue();
        assertThat(core2.isMUS()).isFalse();
    }

    @Test
    public void testHashCode() {
        assertThat(core1.hashCode()).isEqualTo(core1.hashCode());
        assertThat(new UNSATCore<>(props2, false).hashCode()).isEqualTo(core2.hashCode());
    }

    @Test
    public void testEquals() {
        assertThat(core1).isEqualTo(core1);
        assertThat(core1.equals(core1)).isTrue();
        Assertions.assertThat(new UNSATCore<>(props1, true)).isEqualTo(core1);
        assertThat(core2).isNotEqualTo(core1);
        Assertions.assertThat(new UNSATCore<>(props1, false)).isNotEqualTo(core1);
        Assertions.assertThat(new UNSATCore<>(props2, true)).isNotEqualTo(core1);
        assertThat("String").isNotEqualTo(core1);
        assertThat(core1).isNotEqualTo("String");
    }

    @Test
    public void testToString() {
        final String exp1 = "UNSATCore{isMUS=true, propositions=[StandardProposition{formula=a | b, description=}, " +
                "StandardProposition{formula=~a | b, description=}, StandardProposition{formula=a | ~b, " +
                "description=}, StandardProposition{formula=~a | ~b, description=}]}";
        final String exp2 = "UNSATCore{isMUS=false, propositions=[StandardProposition{formula=a | b, description=}, " +
                "StandardProposition{formula=~a | b, description=}, StandardProposition{formula=a | ~b, " +
                "description=}, StandardProposition{formula=~a | ~b, description=}, " +
                "StandardProposition{formula=~a | ~b | c, description=}]}";
        assertThat(core1.toString()).isEqualTo(exp1);
        assertThat(core2.toString()).isEqualTo(exp2);
    }
}
