// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.datastructures;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.datastructures.Tristate;
import org.junit.jupiter.api.Test;

public class SolversDatastructuresTest {

    @Test
    public void testLngBoundedIntQueue() {
        final LngBoundedIntQueue queue = new LngBoundedIntQueue();
        queue.initSize(2);
        queue.push(64);
        queue.push(32);
        queue.push(8);
        queue.push(16);
        final String expected =
                "LngBoundedIntQueue{first=0, last=0, sumOfQueue=24, maxSize=2, queueSize=2, elems=[8, 16]}";
        assertThat(queue.toString()).isEqualTo(expected);
    }

    @Test
    public void testLngBoundedLongQueue() {
        final LngBoundedLongQueue queue = new LngBoundedLongQueue();
        queue.initSize(2);
        queue.push(64L);
        queue.push(32L);
        queue.push(8L);
        queue.push(17L);
        final String expected =
                "LngBoundedLongQueue{first=0, last=0, sumOfQueue=25, maxSize=2, queueSize=2, elems=[8, 17]}";
        assertThat(queue.toString()).isEqualTo(expected);
    }

    @Test
    public void testLngClause() {
        final LngIntVector vec = new LngIntVector();
        vec.push(2);
        vec.push(4);
        vec.push(6);
        final LngClause clause = new LngClause(vec, 0);
        clause.setCanBeDel(true);
        clause.setLbd(42);
        clause.setSeen(true);
        final String expected =
                "LngClause{activity=0.0, learntOnState=0, seen=true, lbd=42, canBeDel=true, oneWatched=false, isAtMost=false, atMostWatchers=-1, lits=[1, 2, " +
                        "3]}";
        assertThat(clause.toString()).isEqualTo(expected);
        assertThat(clause.equals(clause)).isTrue();
        assertThat(clause.hashCode()).isEqualTo(clause.hashCode());
        assertThat(clause.equals("Test")).isFalse();
    }

    @Test
    public void testLngHardClause() {
        final LngIntVector vec = new LngIntVector();
        vec.push(2);
        vec.push(4);
        vec.push(6);
        final LngHardClause clause = new LngHardClause(vec);
        final String expected = "LngHardClause{lits=[1, 2, 3]}";
        assertThat(clause.toString()).isEqualTo(expected);
    }

    @Test
    public void testLngSoftClause() {
        final LngIntVector vec = new LngIntVector();
        vec.push(2);
        vec.push(4);
        vec.push(6);
        final LngSoftClause clause = new LngSoftClause(vec, 2, 4, vec);
        final String expected = "LngSoftClause{weight=2, assumption=4 lits=[1, 2, 3] relax[1, 2, 3]}";
        assertThat(clause.toString()).isEqualTo(expected);
    }

    @Test
    public void testLngVariable() {
        final LngVariable var = new LngVariable(true);
        var.setDecision(true);
        var.setLevel(12);
        var.setReason(null);
        var.assign(Tristate.TRUE);
        final String expected =
                "LngVariable{assignment=TRUE, level=12, reason=null, activity=0.000000, polarity=true, decision=true}";
        assertThat(var.toString()).isEqualTo(expected);
    }

    @Test
    public void testLngWatcher() {
        final LngIntVector vec = new LngIntVector();
        vec.push(2);
        vec.push(4);
        vec.push(6);
        final LngClause clause = new LngClause(vec, 1);
        final LngWatcher watcher = new LngWatcher(clause, 2);
        final String expected =
                "LngWatcher{clause=LngClause{activity=0.0, learntOnState=1, seen=false, lbd=0, canBeDel=true, oneWatched=false, isAtMost=false, " +
                        "atMostWatchers=-1, lits=[1, 2, 3]}, blocker=2}";
        assertThat(watcher.toString()).isEqualTo(expected);
        assertThat(watcher.hashCode()).isEqualTo(watcher.hashCode());
    }
}
