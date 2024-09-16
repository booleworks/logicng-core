// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGLongVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBBoundedIntQueue;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBBoundedLongQueue;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBClause;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBHeap;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBVariable;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBWatcher;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.datastructures.LNGBoundedIntQueue;
import com.booleworks.logicng.solvers.datastructures.LNGBoundedLongQueue;
import com.booleworks.logicng.solvers.datastructures.LNGClause;
import com.booleworks.logicng.solvers.datastructures.LNGHeap;
import com.booleworks.logicng.solvers.datastructures.LNGVariable;
import com.booleworks.logicng.solvers.datastructures.LNGWatcher;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class SolverDatastructuresTest {

    @Test
    public void testLngHeap() {
        final FormulaFactory f = FormulaFactory.caching();
        final SATSolver solver = SATSolver.newSolver(f);

        final LNGIntVector heapContent = new LNGIntVector(new int[]{1, 3, 5, -7, 9}, 5);
        final LNGIntVector indices = new LNGIntVector(new int[]{42, 43, 44}, 3);

        final LNGHeap heap = new LNGHeap(solver.getUnderlyingSolver(), heapContent, indices);

        final PBHeap serialized = SolverDatastructures.serializeHeap(heap);
        final LNGHeap deserialized = SolverDatastructures.deserializeHeap(serialized, solver.getUnderlyingSolver());
        CollectionComperator.assertIntVecEquals(heap.getHeap(), deserialized.getHeap());
        CollectionComperator.assertIntVecEquals(heap.getIndices(), deserialized.getIndices());
    }

    @Test
    public void testLngClause() {
        final LNGIntVector data = new LNGIntVector(new int[]{1, 3, 5, -7, 9}, 5);
        final LNGClause clause = new LNGClause(data, 17, true, 3.3, true, 8990L, true, false, 7);
        final PBClause serialized = SolverDatastructures.serializeClause(clause, 47);
        final LNGClause deserialized = SolverDatastructures.deserializeClause(serialized);

        assertThat(deserialized.get(0)).isEqualTo(1);
        assertThat(deserialized.get(1)).isEqualTo(3);
        assertThat(deserialized.get(2)).isEqualTo(5);
        assertThat(deserialized.get(3)).isEqualTo(-7);
        assertThat(deserialized.get(4)).isEqualTo(9);

        assertThat(deserialized.getLearntOnState()).isEqualTo(17);
        assertThat(deserialized.learnt()).isEqualTo(true);
        assertThat(deserialized.isAtMost()).isEqualTo(true);
        assertThat(deserialized.activity()).isEqualTo(3.3);
        assertThat(deserialized.seen()).isEqualTo(true);
        assertThat(deserialized.lbd()).isEqualTo(8990);
        assertThat(deserialized.canBeDel()).isEqualTo(true);
        assertThat(deserialized.oneWatched()).isEqualTo(false);
        assertThat(deserialized.atMostWatchers()).isEqualTo(7);
    }

    @Test
    public void testMsVariable() {
        final LNGIntVector data = new LNGIntVector(new int[]{1, 3, 5, -7, 9}, 5);
        final LNGClause clause = new LNGClause(data, 17, true, 3.3, true, 8990L, true, false, 7);
        final LNGVariable variable = new LNGVariable(true);
        variable.assign(Tristate.UNDEF);
        variable.setLevel(42);
        variable.setReason(clause);
        variable.incrementActivity(23.3);
        variable.setDecision(true);
        final IdentityHashMap<LNGClause, Integer> clauseMap = new IdentityHashMap<>();
        clauseMap.put(clause, 42);
        final Map<Integer, LNGClause> reverseMap = new HashMap<>();
        reverseMap.put(42, clause);

        final PBVariable serialized = SolverDatastructures.serializeVariable(variable, clauseMap);
        final LNGVariable deserialized = SolverDatastructures.deserializeVariable(serialized, reverseMap);

        assertThat(deserialized.assignment()).isEqualTo(Tristate.UNDEF);
        assertThat(deserialized.level()).isEqualTo(42);
        assertThat(deserialized.activity()).isEqualTo(23.3);
        assertThat(deserialized.polarity()).isEqualTo(true);
        assertThat(deserialized.decision()).isEqualTo(true);
    }

    @Test
    public void testLngWatcher() {
        final LNGIntVector data = new LNGIntVector(new int[]{1, 3, 5, -7, 9}, 5);
        final LNGClause clause = new LNGClause(data, 17, true, 3.3, true, 8990L, true, false, 7);
        final LNGWatcher watcher = new LNGWatcher(clause, 42);
        final IdentityHashMap<LNGClause, Integer> clauseMap = new IdentityHashMap<>();
        clauseMap.put(clause, 42);
        final Map<Integer, LNGClause> reverseMap = new HashMap<>();
        reverseMap.put(42, clause);

        final PBWatcher serialized = SolverDatastructures.serializeWatcher(watcher, clauseMap);
        final LNGWatcher deserialized = SolverDatastructures.deserializeWatcher(serialized, reverseMap);

        assertThat(deserialized.blocker()).isEqualTo(42);
        assertThat(deserialized.clause().get(1)).isEqualTo(3);
        assertThat(deserialized.clause().get(2)).isEqualTo(5);
        assertThat(deserialized.clause().get(3)).isEqualTo(-7);
        assertThat(deserialized.clause().get(4)).isEqualTo(9);

        assertThat(deserialized.clause().learnt()).isEqualTo(true);
        assertThat(deserialized.clause().isAtMost()).isEqualTo(true);
        assertThat(deserialized.clause().activity()).isEqualTo(3.3);
        assertThat(deserialized.clause().seen()).isEqualTo(true);
        assertThat(deserialized.clause().lbd()).isEqualTo(8990);
        assertThat(deserialized.clause().canBeDel()).isEqualTo(true);
        assertThat(deserialized.clause().oneWatched()).isEqualTo(false);
        assertThat(deserialized.clause().atMostWatchers()).isEqualTo(7);
    }

    @Test
    public void testLngBoundedIntQueue() {
        final LNGBoundedIntQueue queue = new LNGBoundedIntQueue(new LNGIntVector(new int[]{1, 3, 5, 8}, 4), 1, 3, 5, 17, 42);
        final PBBoundedIntQueue serialized = SolverDatastructures.serializeIntQueue(queue);
        final LNGBoundedIntQueue deserialized = SolverDatastructures.deserializeIntQueue(serialized);

        assertThat(deserialized.getElems().get(0)).isEqualTo(1);
        assertThat(deserialized.getElems().get(1)).isEqualTo(3);
        assertThat(deserialized.getElems().get(2)).isEqualTo(5);
        assertThat(deserialized.getElems().get(3)).isEqualTo(8);

        assertThat(deserialized.getFirst()).isEqualTo(1);
        assertThat(deserialized.getLast()).isEqualTo(3);
        assertThat(deserialized.getSumOfQueue()).isEqualTo(5);
        assertThat(deserialized.getMaxSize()).isEqualTo(17);
        assertThat(deserialized.getQueueSize()).isEqualTo(42);
    }

    @Test
    public void testLngBoundedLongQueue() {
        final LNGBoundedLongQueue queue = new LNGBoundedLongQueue(new LNGLongVector(1, 3, 5, 8), 1, 3, 5, 17, 42);
        final PBBoundedLongQueue serialized = SolverDatastructures.serializeLongQueue(queue);
        final LNGBoundedLongQueue deserialized = SolverDatastructures.deserializeLongQueue(serialized);

        assertThat(deserialized.getElems().get(0)).isEqualTo(1);
        assertThat(deserialized.getElems().get(1)).isEqualTo(3);
        assertThat(deserialized.getElems().get(2)).isEqualTo(5);
        assertThat(deserialized.getElems().get(3)).isEqualTo(8);

        assertThat(deserialized.getFirst()).isEqualTo(1);
        assertThat(deserialized.getLast()).isEqualTo(3);
        assertThat(deserialized.getSumOfQueue()).isEqualTo(5);
        assertThat(deserialized.getMaxSize()).isEqualTo(17);
        assertThat(deserialized.getQueueSize()).isEqualTo(42);
    }
}
