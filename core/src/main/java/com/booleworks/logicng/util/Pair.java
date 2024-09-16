// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.util;

import java.util.Objects;

/**
 * Data structure for a pair.
 * @param <A> the type parameter of the first entry
 * @param <B> the type parameter of the second entry
 * @version 3.0.0
 * @since 1.0
 */
public class Pair<A, B> {

    protected final A first;
    protected final B second;

    /**
     * Constructs a new pair.
     * @param first  the first entry
     * @param second the second entry
     */
    public Pair(final A first, final B second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Returns the first entry of this pair.
     * @return the first entry
     */
    public A getFirst() {
        return first;
    }

    /**
     * Returns the second entry of this pair.
     * @return the second entry
     */
    public B getSecond() {
        return second;
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Pair) {
            final Pair<?, ?> o = (Pair<?, ?>) other;
            return Objects.equals(second, o.second) && Objects.equals(first, o.first);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", first, second);
    }
}
