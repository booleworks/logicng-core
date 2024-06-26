// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.datastructures;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Tristate;
import org.junit.jupiter.api.Test;

public class SolversDatastructuresTest {

    @Test
    public void testLNGBoundedIntQueue() {
        final LNGBoundedIntQueue queue = new LNGBoundedIntQueue();
        queue.initSize(2);
        queue.push(64);
        queue.push(32);
        queue.push(8);
        queue.push(16);
        final String expected =
                "LNGBoundedIntQueue{first=0, last=0, sumOfQueue=24, maxSize=2, queueSize=2, elems=[8, 16]}";
        assertThat(queue.toString()).isEqualTo(expected);
    }

    @Test
    public void testLNGBoundedLongQueue() {
        final LNGBoundedLongQueue queue = new LNGBoundedLongQueue();
        queue.initSize(2);
        queue.push(64L);
        queue.push(32L);
        queue.push(8L);
        queue.push(17L);
        final String expected =
                "LNGBoundedLongQueue{first=0, last=0, sumOfQueue=25, maxSize=2, queueSize=2, elems=[8, 17]}";
        assertThat(queue.toString()).isEqualTo(expected);
    }

    @Test
    public void testMSClause() {
        final LNGIntVector vec = new LNGIntVector();
        vec.push(2);
        vec.push(4);
        vec.push(6);
        final LNGClause clause = new LNGClause(vec, 0);
        clause.setCanBeDel(true);
        clause.setLBD(42);
        clause.setSeen(true);
        final String expected =
                "MSClause{activity=0.0, learntOnState=0, seen=true, lbd=42, canBeDel=true, oneWatched=false, isAtMost=false, atMostWatchers=-1, lits=[1, 2, 3]}";
        assertThat(clause.toString()).isEqualTo(expected);
        assertThat(clause.equals(clause)).isTrue();
        assertThat(clause.hashCode()).isEqualTo(clause.hashCode());
        assertThat(clause.equals("Test")).isFalse();
    }

    @Test
    public void testMSHardClause() {
        final LNGIntVector vec = new LNGIntVector();
        vec.push(2);
        vec.push(4);
        vec.push(6);
        final LNGHardClause clause = new LNGHardClause(vec);
        final String expected = "MSHardClause{lits=[1, 2, 3]}";
        assertThat(clause.toString()).isEqualTo(expected);
    }

    @Test
    public void testMSSoftClause() {
        final LNGIntVector vec = new LNGIntVector();
        vec.push(2);
        vec.push(4);
        vec.push(6);
        final LNGSoftClause clause = new LNGSoftClause(vec, 2, 4, vec);
        final String expected = "MSSoftClause{weight=2, assumption=4 lits=[1, 2, 3] relax[1, 2, 3]}";
        assertThat(clause.toString()).isEqualTo(expected);
    }

    @Test
    public void testMSVariable() {
        final LNGVariable var = new LNGVariable(true);
        var.setDecision(true);
        var.setLevel(12);
        var.setReason(null);
        var.assign(Tristate.TRUE);
        final String expected =
                "MSVariable{assignment=TRUE, level=12, reason=null, activity=0.000000, polarity=true, decision=true}";
        assertThat(var.toString()).isEqualTo(expected);
    }

    @Test
    public void testMSWatcher() {
        final LNGIntVector vec = new LNGIntVector();
        vec.push(2);
        vec.push(4);
        vec.push(6);
        final LNGClause clause = new LNGClause(vec, 1);
        final LNGWatcher watcher = new LNGWatcher(clause, 2);
        final String expected =
                "MSWatcher{clause=MSClause{activity=0.0, learntOnState=1, seen=false, lbd=0, canBeDel=true, oneWatched=false, isAtMost=false, atMostWatchers=-1, lits=[1, 2, 3]}, blocker=2}";
        assertThat(watcher.toString()).isEqualTo(expected);
        assertThat(watcher.hashCode()).isEqualTo(watcher.hashCode());
    }
}
