// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.util;

/**
 * Data structure for a comparable pair.
 * @param <A> the type parameter of the first entry
 * @param <B> the type parameter of the second entry
 * @version 1.2
 * @since 1.2
 */
public final class ComparablePair<A extends Comparable<A>, B extends Comparable<B>> extends Pair<A, B> implements Comparable<ComparablePair<A, B>> {

    /**
     * Constructs a new comparable pair.
     * @param a the first entry
     * @param b the second entry
     */
    public ComparablePair(final A a, final B b) {
        super(a, b);
    }

    @Override
    public int compareTo(final ComparablePair<A, B> o) {
        final int compare = first.compareTo(o.first);
        return compare != 0 ? compare : second.compareTo(o.second);
    }
}
