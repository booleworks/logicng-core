// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

/**
 * An SDD element. A pair of two SDD nodes: A prime and a sub.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddElement implements Comparable<SddElement> {
    private final SddNode prime;
    private final SddNode sub;

    /**
     * Constructs a new SDD element.  The prime most not be false.
     * @param prime the prime node
     * @param sub   the sub node
     */
    public SddElement(final SddNode prime,
                      final SddNode sub) {
        assert !prime.isFalse();
        this.prime = prime;
        this.sub = sub;
    }

    /**
     * Returns the prime of the element.
     * @return the prime of the element
     */
    public SddNode getPrime() {
        return prime;
    }

    /**
     * Returns the sub of the element.
     * @return the sub of the element
     */
    public SddNode getSub() {
        return sub;
    }

    @Override
    public int compareTo(final SddElement o) {
        final int cSub = sub.compareTo(o.sub);
        if (cSub != 0) {
            return cSub;
        }
        return prime.compareTo(o.prime);
    }

    @Override
    public String toString() {
        return "<" + prime + "," + sub + ">";
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof SddElement)) {
            return false;
        }

        final SddElement element = (SddElement) o;
        return prime.equals(element.prime) && sub.equals(element.sub);
    }

    @Override
    public int hashCode() {
        int result = prime.hashCode();
        result = 31 * result + sub.hashCode();
        return result;
    }
}
