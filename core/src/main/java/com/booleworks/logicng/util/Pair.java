// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.util;

import java.util.Objects;

/**
 * Data structure for a pair.
 * @param <A> the type parameter of the first entry
 * @param <B> the type parameter of the second entry
 * @version 1.2
 * @since 1.0
 */
public class Pair<A, B> {

    protected final A a;
    protected final B b;

    /**
     * Constructs a new pair.
     * @param a the first entry
     * @param b the second entry
     */
    public Pair(final A a, final B b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Returns the first entry of this pair.
     * @return the first entry
     */
    public A first() {
        return a;
    }

    /**
     * Returns the second entry of this pair.
     * @return the second entry
     */
    public B second() {
        return b;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Pair) {
            final Pair<?, ?> o = (Pair<?, ?>) other;
            return Objects.equals(b, o.b) && Objects.equals(a, o.a);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>", a, b);
    }
}
