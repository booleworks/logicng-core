// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BackboneSimplifierTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testTrivialBackbones(final FormulaContext _c) throws ParserException {
        final BackboneSimplifier backboneSimplifier = new BackboneSimplifier(_c.f);

        assertThat(_c.p.parse("$true").transform(backboneSimplifier)).isEqualTo(_c.p.parse("$true"));
        assertThat(_c.p.parse("$false").transform(backboneSimplifier)).isEqualTo(_c.p.parse("$false"));
        assertThat(_c.p.parse("A & (A => B) & ~B").transform(backboneSimplifier))
                .isEqualTo(_c.p.parse("$false"));
        assertThat(_c.p.parse("A").transform(backboneSimplifier)).isEqualTo(_c.p.parse("A"));
        assertThat(_c.p.parse("A & B").transform(backboneSimplifier)).isEqualTo(_c.p.parse("A & B"));
        assertThat(_c.p.parse("A | B | C").transform(backboneSimplifier)).isEqualTo(_c.p.parse("A | B | C"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testRealBackbones(final FormulaContext _c) throws ParserException {
        final BackboneSimplifier backboneSimplifier = new BackboneSimplifier(_c.f);

        assertThat(_c.p.parse("A & B & (B | C)").transform(backboneSimplifier))
                .isEqualTo(_c.p.parse("A & B"));
        assertThat(_c.p.parse("A & B & (~B | C)").transform(backboneSimplifier))
                .isEqualTo(_c.p.parse("A & B & C"));
        assertThat(_c.p.parse("A & B & (~B | C) & (B | D) & (A => F)").transform(backboneSimplifier))
                .isEqualTo(_c.p.parse("A & B & C & F"));
        assertThat(_c.p.parse("X & Y & (~B | C) & (B | D) & (A => F)").transform(backboneSimplifier))
                .isEqualTo(_c.p.parse("X & Y & (~B | C) & (B | D) & (A => F)"));
        assertThat(_c.p.parse("~A & ~B & (~B | C) & (B | D) & (A => F)").transform(backboneSimplifier))
                .isEqualTo(_c.p.parse("~A & ~B & D"));
    }
}
