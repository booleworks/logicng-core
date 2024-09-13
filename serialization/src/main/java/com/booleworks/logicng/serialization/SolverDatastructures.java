// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBBoundedIntQueue;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBBoundedLongQueue;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBClause;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBHeap;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBTristate;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBVariable;
import com.booleworks.logicng.serialization.ProtoBufSolverDatastructures.PBWatcher;
import com.booleworks.logicng.solvers.datastructures.LNGBoundedIntQueue;
import com.booleworks.logicng.solvers.datastructures.LNGBoundedLongQueue;
import com.booleworks.logicng.solvers.datastructures.LNGClause;
import com.booleworks.logicng.solvers.datastructures.LNGHeap;
import com.booleworks.logicng.solvers.datastructures.LNGVariable;
import com.booleworks.logicng.solvers.datastructures.LNGWatcher;
import com.booleworks.logicng.solvers.sat.LNGCoreSolver;

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
    static PBTristate serializeTristate(final Tristate tristate) {
        switch (tristate) {
            case FALSE:
                return PBTristate.FALSE;
            case TRUE:
                return PBTristate.TRUE;
            case UNDEF:
                return PBTristate.UNDEF;
            default:
                throw new IllegalArgumentException("Unknown tristate: " + tristate);
        }
    }

    /**
     * Deserializes a tristate from a protocol buffer.
     * @param bin the protocol buffer
     * @return the tristate
     */
    static Tristate deserializeTristate(final PBTristate bin) {
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
    static PBHeap serializeHeap(final LNGHeap heap) {
        return PBHeap.newBuilder()
                .setHeap(Collections.serializeIntVec(heap.getHeap()))
                .setIndices(Collections.serializeIntVec(heap.getIndices()))
                .build();
    }

    /**
     * Deserializes a solver heap from a protocol buffer.
     * @param bin the protocol buffer
     * @return the heap
     */
    static LNGHeap deserializeHeap(final PBHeap bin, final LNGCoreSolver solver) {
        final LNGIntVector heap = Collections.deserializeIntVec(bin.getHeap());
        final LNGIntVector indices = Collections.deserializeIntVec(bin.getIndices());
        return new LNGHeap(solver, heap, indices);
    }

    /**
     * Serializes a MiniSat clause to a protocol buffer.
     * @param clause the clause
     * @param id     the clause ID
     * @return the protocol buffer
     */
    static PBClause serializeClause(final LNGClause clause, final int id) {
        return PBClause.newBuilder()
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
    static LNGClause deserializeClause(final PBClause bin) {
        return new LNGClause(
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
    static PBVariable serializeVariable(final LNGVariable variable, final IdentityHashMap<LNGClause, Integer> clauseMap) {
        return PBVariable.newBuilder()
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
    static LNGVariable deserializeVariable(final PBVariable bin, final Map<Integer, LNGClause> clauseMap) {
        final LNGClause reason = bin.getReason() == -1 ? null : clauseMap.get(bin.getReason());
        return new LNGVariable(deserializeTristate(bin.getAssignment()), bin.getLevel(), reason, bin.getActivity(), bin.getPolarity(), bin.getDecision());
    }

    /**
     * Serializes a MiniSat watcher to a protocol buffer.
     * @param watcher   the watcher
     * @param clauseMap a mapping from clause to clause ID
     * @return the protocol buffer
     */
    static PBWatcher serializeWatcher(final LNGWatcher watcher, final IdentityHashMap<LNGClause, Integer> clauseMap) {
        return PBWatcher.newBuilder()
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
    static LNGWatcher deserializeWatcher(final PBWatcher bin, final Map<Integer, LNGClause> clauseMap) {
        return new LNGWatcher(clauseMap.get(bin.getClause()), bin.getBlocker());
    }

    /**
     * Serializes a bounded integer queue to a protocol buffer.
     * @param queue the queue
     * @return the protocol buffer
     */
    static PBBoundedIntQueue serializeIntQueue(final LNGBoundedIntQueue queue) {
        return PBBoundedIntQueue.newBuilder()
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
    static LNGBoundedIntQueue deserializeIntQueue(final PBBoundedIntQueue bin) {
        return new LNGBoundedIntQueue(Collections.deserializeIntVec(bin.getElems()), bin.getFirst(), bin.getLast(),
                bin.getSumOfQueue(), bin.getMaxSize(), bin.getQueueSize());
    }

    /**
     * Serializes a bounded long queue to a protocol buffer.
     * @param queue the queue
     * @return the protocol buffer
     */
    static PBBoundedLongQueue serializeLongQueue(final LNGBoundedLongQueue queue) {
        return PBBoundedLongQueue.newBuilder()
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
    static LNGBoundedLongQueue deserializeLongQueue(final PBBoundedLongQueue bin) {
        return new LNGBoundedLongQueue(Collections.deserializeLongVec(bin.getElems()), bin.getFirst(), bin.getLast(),
                bin.getSumOfQueue(), bin.getMaxSize(), bin.getQueueSize());
    }
}
