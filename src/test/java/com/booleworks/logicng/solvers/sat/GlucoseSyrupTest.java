// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.collections.LNGIntVector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GlucoseSyrupTest {

    private final GlucoseSyrup gs = new GlucoseSyrup();

    @BeforeEach
    public void prepare() {
        gs.newVar(true, true);
        gs.newVar(true, true);
        gs.newVar(true, true);
        gs.newVar(true, true);
        gs.addClause(clause(1, 2, 3), null);
    }

    @Test
    public void testSaveState() {
        assertThatThrownBy(gs::saveState).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testLoadState() {
        assertThatThrownBy(() -> gs.loadState(new int[5])).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testToString() {
        final String expected = String.format("ok            true%n" +
                "qhead         0%n" +
                "#clauses      1%n" +
                "#learnts      0%n" +
                "#watches      8%n" +
                "#vars         4%n" +
                "#orderheap    4%n" +
                "#trail        0%n" +
                "#trailLim     0%n" +
                "model         []%n" +
                "conflict      []%n" +
                "assumptions   []%n" +
                "#seen         4%n" +
                "claInc        1.0%n" +
                "simpDBAssigns -1%n" +
                "simpDBProps   0%n" +
                "#clause lits  3%n" +
                "#learnts lits 0%n");
        assertThat(gs.toString()).isEqualTo(expected);
    }

    private LNGIntVector clause(final int... lits) {
        final LNGIntVector c = new LNGIntVector(lits.length);
        for (final int l : lits) {
            c.push(literal(l));
        }
        return c;
    }

    private int literal(final int l) {
        return l < 0 ? (-l * 2) ^ 1 : l * 2;
    }
}
