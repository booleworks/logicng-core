// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbBoundedIntQueue;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbBoundedLongQueue;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbClause;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbHeap;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbTristate;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbVariable;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PbWatcher;
import com.booleworks.logicng.solvers.datastructures.LngBoundedIntQueue;
import com.booleworks.logicng.solvers.datastructures.LngBoundedLongQueue;
import com.booleworks.logicng.solvers.datastructures.LngClause;
import com.booleworks.logicng.solvers.datastructures.LngHeap;
import com.booleworks.logicng.solvers.datastructures.LngVariable;
import com.booleworks.logicng.solvers.datastructures.LngWatcher;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Serialization methods for SAT solver datastructures.
 * @version 3.0.0
 * @since 2.5.0
 */
public interface SolverDatastructures {

    /**
     * Serializes a tristate to a protocol buffer.
     * @param tristate the tristate value
     * @return the protocol buffer
     */
    static PbTristate serializeTristate(final Tristate tristate) {
        switch (tristate) {
            case FALSE:
                return PbTristate.FALSE;
            case TRUE:
                return PbTristate.TRUE;
            case UNDEF:
                return PbTristate.UNDEF;
            default:
                throw new IllegalArgumentException("Unknown tristate: " + tristate);
        }
    }

    /**
     * Deserializes a tristate from a protocol buffer.
     * @param bin the protocol buffer
     * @return the tristate
     */
    static Tristate deserializeTristate(final PbTristate bin) {
        switch (bin) {
            case FALSE:
                return Tristate.FALSE;
            case TRUE:
                return Tristate.TRUE;
            case UNDEF:
                return Tristate.UNDEF;
            default:
                throw new IllegalArgumentException("Unknown tristate: " + bin);
        }
    }

    /**
     * Serializes a solver heap to a protocol buffer.
     * @param heap the heap
     * @return the protocol buffer
     */
    static PbHeap serializeHeap(final LngHeap heap) {
        return PbHeap.newBuilder()
                .setHeap(Collections.serializeIntVec(heap.getHeap()))
                .setIndices(Collections.serializeIntVec(heap.getIndices()))
                .build();
    }

    /**
     * Deserializes a solver heap from a protocol buffer.
     * @param bin the protocol buffer
     * @return the heap
     */
    static LngHeap deserializeHeap(final PbHeap bin, final LngCoreSolver solver) {
        final LngIntVector heap = Collections.deserializeIntVec(bin.getHeap());
        final LngIntVector indices = Collections.deserializeIntVec(bin.getIndices());
        return new LngHeap(solver, heap, indices);
    }

    /**
     * Serializes a MiniSat clause to a protocol buffer.
     * @param clause the clause
     * @param id     the clause ID
     * @return the protocol buffer
     */
    static PbClause serializeClause(final LngClause clause, final int id) {
        return PbClause.newBuilder()
                .setData(Collections.serializeIntVec(clause.getData()))
                .setLearntOnState(clause.getLearntOnState())
                .setIsAtMost(clause.isAtMost())
                .setActivity(clause.activity())
                .setSeen(clause.seen())
                .setLbd(clause.lbd())
                .setCanBeDel(clause.canBeDel())
                .setOneWatched(clause.oneWatched())
                .setAtMostWatchers(clause.isAtMost() ? clause.atMostWatchers() : -1)
                .setId(id)
                .build();
    }

    /**
     * Deserializes a MiniSat clause from a protocol buffer.
     * @param bin the protocol buffer
     * @return the clause
     */
    static LngClause deserializeClause(final PbClause bin) {
        return new LngClause(
                Collections.deserializeIntVec(bin.getData()),
                bin.getLearntOnState(),
                bin.getIsAtMost(),
                bin.getActivity(),
                bin.getSeen(),
                bin.getLbd(),
                bin.getCanBeDel(),
                bin.getOneWatched(),
                bin.getAtMostWatchers()
        );
    }

    /**
     * Serializes a MiniSat variable to a protocol buffer.
     * @param variable  the variable
     * @param clauseMap a mapping from clause to clause ID
     * @return the protocol buffer
     */
    static PbVariable serializeVariable(final LngVariable variable, final IdentityHashMap<LngClause, Integer> clauseMap) {
        return PbVariable.newBuilder()
                .setAssignment(serializeTristate(variable.assignment()))
                .setLevel(variable.level())
                .setActivity(variable.activity())
                .setPolarity(variable.polarity())
                .setDecision(variable.decision())
                .setReason(variable.reason() == null ? -1 : clauseMap.get(variable.reason())).build();
    }

    /**
     * Deserializes a MiniSat variable from a protocol buffer.
     * @param bin       the protocol buffer
     * @param clauseMap a mapping from clause ID to clause
     * @return the variable
     */
    static LngVariable deserializeVariable(final PbVariable bin, final Map<Integer, LngClause> clauseMap) {
        final LngClause reason = bin.getReason() == -1 ? null : clauseMap.get(bin.getReason());
        return new LngVariable(deserializeTristate(bin.getAssignment()), bin.getLevel(), reason, bin.getActivity(), bin.getPolarity(), bin.getDecision());
    }

    /**
     * Serializes a MiniSat watcher to a protocol buffer.
     * @param watcher   the watcher
     * @param clauseMap a mapping from clause to clause ID
     * @return the protocol buffer
     */
    static PbWatcher serializeWatcher(final LngWatcher watcher, final IdentityHashMap<LngClause, Integer> clauseMap) {
        return PbWatcher.newBuilder()
                .setClause(clauseMap.get(watcher.clause()))
                .setBlocker(watcher.blocker())
                .build();
    }

    /**
     * Deserializes a MiniSat watcher from a protocol buffer.
     * @param bin       the protocol buffer
     * @param clauseMap a mapping from clause ID to clause
     * @return the watcher
     */
    static LngWatcher deserializeWatcher(final PbWatcher bin, final Map<Integer, LngClause> clauseMap) {
        return new LngWatcher(clauseMap.get(bin.getClause()), bin.getBlocker());
    }

    /**
     * Serializes a bounded integer queue to a protocol buffer.
     * @param queue the queue
     * @return the protocol buffer
     */
    static PbBoundedIntQueue serializeIntQueue(final LngBoundedIntQueue queue) {
        return PbBoundedIntQueue.newBuilder()
                .setElems(Collections.serializeIntVec(queue.getElems()))
                .setFirst(queue.getFirst())
                .setLast(queue.getLast())
                .setSumOfQueue(queue.getSumOfQueue())
                .setMaxSize(queue.getMaxSize())
                .setQueueSize(queue.getQueueSize())
                .build();
    }

    /**
     * Deserializes a bounded integer queue from a protocol buffer.
     * @param bin the protocol buffer
     * @return the queue
     */
    static LngBoundedIntQueue deserializeIntQueue(final PbBoundedIntQueue bin) {
        return new LngBoundedIntQueue(Collections.deserializeIntVec(bin.getElems()), bin.getFirst(), bin.getLast(),
                bin.getSumOfQueue(), bin.getMaxSize(), bin.getQueueSize());
    }

    /**
     * Serializes a bounded long queue to a protocol buffer.
     * @param queue the queue
     * @return the protocol buffer
     */
    static PbBoundedLongQueue serializeLongQueue(final LngBoundedLongQueue queue) {
        return PbBoundedLongQueue.newBuilder()
                .setElems(Collections.serializeLongVec(queue.getElems()))
                .setFirst(queue.getFirst())
                .setLast(queue.getLast())
                .setSumOfQueue(queue.getSumOfQueue())
                .setMaxSize(queue.getMaxSize())
                .setQueueSize(queue.getQueueSize())
                .build();
    }

    /**
     * Deserializes a bounded long queue from a protocol buffer.
     * @param bin the protocol buffer
     * @return the queue
     */
    static LngBoundedLongQueue deserializeLongQueue(final PbBoundedLongQueue bin) {
        return new LngBoundedLongQueue(Collections.deserializeLongVec(bin.getElems()), bin.getFirst(), bin.getLast(),
                bin.getSumOfQueue(), bin.getMaxSize(), bin.getQueueSize());
    }
}
