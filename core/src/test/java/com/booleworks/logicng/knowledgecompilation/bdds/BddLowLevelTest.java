// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddConstruction;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BddLowLevelTest {

    private Bdd bdd;

    @BeforeEach
    public void init() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser parser = new PropositionalParser(f);
        final BddKernel kernel = new BddKernel(f, 3, 1000, 1000);
        BddFactory.build(f, f.verum(), kernel);
        BddFactory.build(f, f.falsum(), kernel);
        BddFactory.build(f, f.literal("A", true), kernel);
        BddFactory.build(f, f.literal("A", false), kernel);
        BddFactory.build(f, parser.parse("A => ~B"), kernel);
        BddFactory.build(f, parser.parse("A <=> ~B"), kernel);
        BddFactory.build(f, parser.parse("A | B | ~C"), kernel);
        bdd = BddFactory.build(f, parser.parse("A & B & ~C"), kernel);
    }

    @Test
    public void testStatistics() {
        final BddKernel.BddStatistics statistics = bdd.getUnderlyingKernel().statistics();
        assertThat(statistics.cachesize()).isEqualTo(1000);
        assertThat(statistics.freenum()).isEqualTo(993);
        assertThat(statistics.gbcollectnum()).isEqualTo(0);
        assertThat(statistics.nodesize()).isEqualTo(1009);
        assertThat(statistics.produced()).isEqualTo(14);
        assertThat(statistics.varnum()).isEqualTo(3);
        assertThat(statistics.toString()).isEqualTo(
                "BDDStatistics{produced nodes=14, allocated nodes=1009, free nodes=993, variables=3, cache size=1000, garbage collections=0}");
    }

    @Test
    public void kernelTests() {
        final BddConstruction kernel = new BddConstruction(bdd.getUnderlyingKernel());
        assertThat(kernel.ithVar(0)).isEqualTo(2);
        assertThat(kernel.nithVar(0)).isEqualTo(3);
        assertThat(kernel.bddVar(2)).isEqualTo(0);
        assertThat(kernel.bddLow(2)).isEqualTo(0);
        assertThat(kernel.bddHigh(2)).isEqualTo(1);

    }

    @Test
    public void illegalKernel1() {
        final BddConstruction kernel = new BddConstruction(bdd.getUnderlyingKernel());
        assertThatThrownBy(() -> kernel.ithVar(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void illegalKernel2() {
        final BddConstruction kernel = new BddConstruction(bdd.getUnderlyingKernel());
        assertThatThrownBy(() -> kernel.nithVar(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void illegalKernel3() {
        final BddConstruction kernel = new BddConstruction(bdd.getUnderlyingKernel());
        assertThatThrownBy(() -> kernel.bddVar(1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void illegalKernel4() {
        final BddConstruction kernel = new BddConstruction(bdd.getUnderlyingKernel());
        assertThatThrownBy(() -> kernel.bddLow(1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void illegalKernel5() {
        final BddConstruction kernel = new BddConstruction(bdd.getUnderlyingKernel());
        assertThatThrownBy(() -> kernel.bddHigh(1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSetNegativeVarNum() {
        assertThatThrownBy(() -> new BddKernel(FormulaFactory.caching(), -4, 100, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
