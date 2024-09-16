// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.solvers.datastructures.LngClause;
import com.booleworks.logicng.solvers.datastructures.LngHeap;
import com.booleworks.logicng.solvers.datastructures.LngVariable;
import com.booleworks.logicng.solvers.datastructures.LngWatcher;

public class SolverDatastructureComparator {

    public static void assertClausesEquals(final LngVector<LngClause> c1, final LngVector<LngClause> c2) {
        assertThat(c1.size()).isEqualTo(c2.size());
        for (int i = 0; i < c1.size(); i++) {
            assertClauseEquals(c1.get(i), c2.get(i));
        }
    }

    public static void assertClauseEquals(final LngClause c1, final LngClause c2) {
        if (c1 == null && c2 == null) {
            return;
        }
        CollectionComperator.assertIntVecEquals(c1.getData(), c2.getData());
        assertThat(c1.learnt()).isEqualTo(c2.learnt());
        assertThat(c1.getLearntOnState()).isEqualTo(c2.getLearntOnState());
        assertThat(c1.isAtMost()).isEqualTo(c2.isAtMost());
        assertThat(c1.activity()).isEqualTo(c2.activity());
        assertThat(c1.seen()).isEqualTo(c2.seen());
        assertThat(c1.lbd()).isEqualTo(c2.lbd());
        assertThat(c1.canBeDel()).isEqualTo(c2.canBeDel());
        assertThat(c1.oneWatched()).isEqualTo(c2.oneWatched());
        if (c1.isAtMost()) {
            assertThat(c1.atMostWatchers()).isEqualTo(c2.atMostWatchers());
        }
    }

    public static void assertVariablesEquals(final LngVector<LngVariable> v1, final LngVector<LngVariable> v2) {
        assertThat(v1.size()).isEqualTo(v2.size());
        for (int i = 0; i < v1.size(); i++) {
            assertVariableEquals(v1.get(i), v2.get(i));
        }
    }

    public static void assertVariableEquals(final LngVariable v1, final LngVariable v2) {
        assertThat(v1.assignment()).isEqualTo(v2.assignment());
        assertThat(v1.level()).isEqualTo(v2.level());
        assertClauseEquals(v1.reason(), v2.reason());
        assertThat(v1.assignment()).isEqualTo(v2.assignment());
        assertThat(v1.activity()).isEqualTo(v2.activity());
        assertThat(v1.polarity()).isEqualTo(v2.polarity());
        assertThat(v1.decision()).isEqualTo(v2.decision());
    }

    public static void assertHeapEquals(final LngHeap heap1, final LngHeap heap2) {
        CollectionComperator.assertIntVecEquals(heap1.getHeap(), heap2.getHeap());
        CollectionComperator.assertIntVecEquals(heap1.getIndices(), heap2.getIndices());
    }

    public static void assertWatchListsEquals(final LngVector<LngVector<LngWatcher>> w1, final LngVector<LngVector<LngWatcher>> w2) {
        assertThat(w1.size()).isEqualTo(w2.size());
        for (int i = 0; i < w1.size(); i++) {
            assertWatchesEquals(w1.get(i), w2.get(i));
        }
    }

    public static void assertWatchesEquals(final LngVector<LngWatcher> w1, final LngVector<LngWatcher> w2) {
        assertThat(w1.size()).isEqualTo(w2.size());
        for (int i = 0; i < w1.size(); i++) {
            assertWatchEquals(w1.get(i), w2.get(i));
        }
    }

    public static void assertWatchEquals(final LngWatcher w1, final LngWatcher w2) {
        assertClauseEquals(w1.clause(), w2.clause());
        assertThat(w1.blocker()).isEqualTo(w2.blocker());
    }
}
