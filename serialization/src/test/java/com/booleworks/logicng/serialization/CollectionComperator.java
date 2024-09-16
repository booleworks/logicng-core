// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngLongVector;

public class CollectionComperator {

    public static void assertBoolVecEquals(final LngBooleanVector v1, final LngBooleanVector v2) {
        if (v1 == null && v2 == null) {
            return;
        }
        assertThat(v1.size()).isEqualTo(v2.size());
        for (int i = 0; i < v1.size(); i++) {
            assertThat(v1.get(i)).isEqualTo(v2.get(i));
        }
    }

    public static void assertIntVecEquals(final LngIntVector v1, final LngIntVector v2) {
        if (v1 == null && v2 == null) {
            return;
        }
        assertThat(v1.size()).isEqualTo(v2.size());
        for (int i = 0; i < v1.size(); i++) {
            assertThat(v1.get(i)).isEqualTo(v2.get(i));
        }
    }

    public static void assertLongVecEquals(final LngLongVector v1, final LngLongVector v2) {
        if (v1 == null && v2 == null) {
            return;
        }
        assertThat(v1.size()).isEqualTo(v2.size());
        for (int i = 0; i < v1.size(); i++) {
            assertThat(v1.get(i)).isEqualTo(v2.get(i));
        }
    }
}
