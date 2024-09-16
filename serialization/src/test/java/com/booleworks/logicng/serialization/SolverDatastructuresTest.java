// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngLongVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbBoundedIntQueue;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbBoundedLongQueue;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbClause;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbHeap;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbVariable;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbWatcher;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.datastructures.LngBoundedIntQueue;
import com.booleworks.logicng.solvers.datastructures.LngBoundedLongQueue;
import com.booleworks.logicng.solvers.datastructures.LngClause;
import com.booleworks.logicng.solvers.datastructures.LngHeap;
import com.booleworks.logicng.solvers.datastructures.LngVariable;
import com.booleworks.logicng.solvers.datastructures.LngWatcher;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class SolverDatastructuresTest {

    @Test
    public void testLngHeap() {
        final FormulaFactory f = FormulaFactory.caching();
        final SatSolver solver = SatSolver.newSolver(f);

        final LngIntVector heapContent = new LngIntVector(new int[]{1, 3, 5, -7, 9}, 5);
        final LngIntVector indices = new LngIntVector(new int[]{42, 43, 44}, 3);

        final LngHeap heap = new LngHeap(solver.getUnderlyingSolver(), heapContent, indices);

        final PbHeap serialized = SolverDatastructures.serializeHeap(heap);
        final LngHeap deserialized = SolverDatastructures.deserializeHeap(serialized, solver.getUnderlyingSolver());
        CollectionComperator.assertIntVecEquals(heap.getHeap(), deserialized.getHeap());
        CollectionComperator.assertIntVecEquals(heap.getIndices(), deserialized.getIndices());
    }

    @Test
    public void testLngClause() {
        final LngIntVector data = new LngIntVector(new int[]{1, 3, 5, -7, 9}, 5);
        final LngClause clause = new LngClause(data, 17, true, 3.3, true, 8990L, true, false, 7);
        final PbClause serialized = SolverDatastructures.serializeClause(clause, 47);
        final LngClause deserialized = SolverDatastructures.deserializeClause(serialized);

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
        final LngIntVector data = new LngIntVector(new int[]{1, 3, 5, -7, 9}, 5);
        final LngClause clause = new LngClause(data, 17, true, 3.3, true, 8990L, true, false, 7);
        final LngVariable variable = new LngVariable(true);
        variable.assign(Tristate.UNDEF);
        variable.setLevel(42);
        variable.setReason(clause);
        variable.incrementActivity(23.3);
        variable.setDecision(true);
        final IdentityHashMap<LngClause, Integer> clauseMap = new IdentityHashMap<>();
        clauseMap.put(clause, 42);
        final Map<Integer, LngClause> reverseMap = new HashMap<>();
        reverseMap.put(42, clause);

        final PbVariable serialized = SolverDatastructures.serializeVariable(variable, clauseMap);
        final LngVariable deserialized = SolverDatastructures.deserializeVariable(serialized, reverseMap);

        assertThat(deserialized.assignment()).isEqualTo(Tristate.UNDEF);
        assertThat(deserialized.level()).isEqualTo(42);
        assertThat(deserialized.activity()).isEqualTo(23.3);
        assertThat(deserialized.polarity()).isEqualTo(true);
        assertThat(deserialized.decision()).isEqualTo(true);
    }

    @Test
    public void testLngWatcher() {
        final LngIntVector data = new LngIntVector(new int[]{1, 3, 5, -7, 9}, 5);
        final LngClause clause = new LngClause(data, 17, true, 3.3, true, 8990L, true, false, 7);
        final LngWatcher watcher = new LngWatcher(clause, 42);
        final IdentityHashMap<LngClause, Integer> clauseMap = new IdentityHashMap<>();
        clauseMap.put(clause, 42);
        final Map<Integer, LngClause> reverseMap = new HashMap<>();
        reverseMap.put(42, clause);

        final PbWatcher serialized = SolverDatastructures.serializeWatcher(watcher, clauseMap);
        final LngWatcher deserialized = SolverDatastructures.deserializeWatcher(serialized, reverseMap);

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
        final LngBoundedIntQueue queue = new LngBoundedIntQueue(new LngIntVector(new int[]{1, 3, 5, 8}, 4), 1, 3, 5, 17, 42);
        final PbBoundedIntQueue serialized = SolverDatastructures.serializeIntQueue(queue);
        final LngBoundedIntQueue deserialized = SolverDatastructures.deserializeIntQueue(serialized);

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
        final LngBoundedLongQueue queue = new LngBoundedLongQueue(new LngLongVector(1, 3, 5, 8), 1, 3, 5, 17, 42);
        final PbBoundedLongQueue serialized = SolverDatastructures.serializeLongQueue(queue);
        final LngBoundedLongQueue deserialized = SolverDatastructures.deserializeLongQueue(serialized);

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
