// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngLongVector;
import com.booleworks.logicng.serialization.ProtoBufCollections.PbBooleanVector;
import com.booleworks.logicng.serialization.ProtoBufCollections.PbIntVector;
import com.booleworks.logicng.serialization.ProtoBufCollections.PbLongVector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class CollectionsTest {

    @Test
    public void testLngBooleanVector() {
        final Random random = new Random(42);
        final List<LngBooleanVector> vecs = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            final LngBooleanVector vec = new LngBooleanVector();
            for (int j = 0; j < 500; j++) {
                vec.push(random.nextBoolean());
            }
            vecs.add(vec);
        }
        final List<PbBooleanVector> serialized = vecs.stream().map(Collections::serializeBoolVec).collect(Collectors.toList());
        final List<LngBooleanVector> deserialized = serialized.stream().map(Collections::deserializeBooVec).collect(Collectors.toList());
        for (int i = 0; i < vecs.size(); i++) {
            CollectionComperator.assertBoolVecEquals(vecs.get(i), deserialized.get(i));
        }
    }

    @Test
    public void testLngIntVector() {
        final Random random = new Random(42);
        final List<LngIntVector> vecs = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            final LngIntVector vec = new LngIntVector();
            for (int j = 0; j < 500; j++) {
                vec.push(random.nextInt());
            }
            vecs.add(vec);
        }
        final List<PbIntVector> serialized = vecs.stream().map(Collections::serializeIntVec).collect(Collectors.toList());
        final List<LngIntVector> deserialized = serialized.stream().map(Collections::deserializeIntVec).collect(Collectors.toList());
        for (int i = 0; i < vecs.size(); i++) {
            CollectionComperator.assertIntVecEquals(vecs.get(i), deserialized.get(i));
        }
    }

    @Test
    public void testLngLongVector() {
        final Random random = new Random(42);
        final List<LngLongVector> vecs = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            final LngLongVector vec = new LngLongVector();
            for (int j = 0; j < 500; j++) {
                vec.push(random.nextLong());
            }
            vecs.add(vec);
        }
        final List<PbLongVector> serialized = vecs.stream().map(Collections::serializeLongVec).collect(Collectors.toList());
        final List<LngLongVector> deserialized = serialized.stream().map(Collections::deserializeLongVec).collect(Collectors.toList());
        for (int i = 0; i < vecs.size(); i++) {
            CollectionComperator.assertLongVecEquals(vecs.get(i), deserialized.get(i));
        }
    }
}
