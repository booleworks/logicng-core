// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.serialization;

import com.booleworks.logicng.collections.LngBooleanVector;
import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngLongVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.serialization.ProtoBufCollections.PbBooleanVector;
import com.booleworks.logicng.serialization.ProtoBufCollections.PbIntVector;
import com.booleworks.logicng.serialization.ProtoBufCollections.PbIntVectorVector;
import com.booleworks.logicng.serialization.ProtoBufCollections.PbLongVector;

/**
 * Serialization methods for LogicNG collections.
 * @version 3.0.0
 * @since 2.5.0
 */
public interface Collections {

    /**
     * Serializes a Boolean vector to a protocol buffer.
     * @param vec the Boolean vector
     * @return the protocol buffer
     */
    static PbBooleanVector serializeBoolVec(final LngBooleanVector vec) {
        final PbBooleanVector.Builder builder = PbBooleanVector.newBuilder().setSize(vec.size());
        for (int i = 0; i < vec.size(); i++) {
            builder.addElement(vec.get(i));
        }
        return builder.build();
    }

    /**
     * Deserializes a Boolean vector from a protocol buffer.
     * @param bin the protocol buffer
     * @return the Boolean vector
     */
    static LngBooleanVector deserializeBooVec(final PbBooleanVector bin) {
        final boolean[] elements = new boolean[bin.getElementCount()];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = bin.getElement(i);
        }
        return new LngBooleanVector(elements, bin.getSize());
    }

    /**
     * Serializes an integer vector to a protocol buffer.
     * @param vec the integer vector
     * @return the protocol buffer
     */
    static PbIntVector serializeIntVec(final LngIntVector vec) {
        final PbIntVector.Builder builder = PbIntVector.newBuilder().setSize(vec.size());
        for (int i = 0; i < vec.size(); i++) {
            builder.addElement(vec.get(i));
        }
        return builder.build();
    }

    /**
     * Deserializes an integer vector from a protocol buffer.
     * @param bin the protocol buffer
     * @return the integer vector
     */
    static LngIntVector deserializeIntVec(final PbIntVector bin) {
        final int[] elements = new int[bin.getElementCount()];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = bin.getElement(i);
        }
        return new LngIntVector(elements, bin.getSize());
    }

    /**
     * Serializes a vector of integer vector to a protocol buffer.
     * @param vec the vector of integer vectors
     * @return the protocol buffer
     */
    static PbIntVectorVector serializeVec(final LngVector<LngIntVector> vec) {
        final PbIntVectorVector.Builder builder = PbIntVectorVector.newBuilder().setSize(vec.size());
        for (int i = 0; i < vec.size(); i++) {
            builder.addElement(serializeIntVec(vec.get(i)));
        }
        return builder.build();
    }

    /**
     * Deserializes a vector of integer vectors from a protocol buffer.
     * @param bin the protocol buffer
     * @return the vector of integer vectors
     */
    static LngVector<LngIntVector> deserializeVec(final PbIntVectorVector bin) {
        final LngVector<LngIntVector> vec = new LngVector<>(bin.getSize());
        for (final PbIntVector i : bin.getElementList()) {
            vec.push(deserializeIntVec(i));
        }
        return vec;
    }

    /**
     * Serializes a long vector to a protocol buffer.
     * @param vec the long vector
     * @return the protocol buffer
     */
    static PbLongVector serializeLongVec(final LngLongVector vec) {
        final PbLongVector.Builder builder = PbLongVector.newBuilder().setSize(vec.size());
        for (int i = 0; i < vec.size(); i++) {
            builder.addElement(vec.get(i));
        }
        return builder.build();
    }

    /**
     * Deserializes a long vector from a protocol buffer.
     * @param bin the protocol buffer
     * @return the long vector
     */
    static LngLongVector deserializeLongVec(final PbLongVector bin) {
        final long[] elements = new long[bin.getElementCount()];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = bin.getElement(i);
        }
        return new LngLongVector(elements, bin.getSize());
    }
}
