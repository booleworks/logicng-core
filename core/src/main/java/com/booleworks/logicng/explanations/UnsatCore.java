// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations;

import com.booleworks.logicng.propositions.Proposition;

import java.util.List;
import java.util.Objects;

/**
 * An unsatisfiable core (can be a minimal unsatisfiable sub-formula).
 * @param <T> the type of the core's propositions
 * @version 3.0.0
 * @since 1.1
 */
public final class UnsatCore<T extends Proposition> {

    private final List<T> propositions;
    private final boolean isMus;

    /**
     * Constructs a new unsatisfiable core.
     * @param propositions the propositions of the core
     * @param isMus        {@code true} if it is a MUS and {@code false} if it
     *                     is unknown whether it is a MUS.
     */
    public UnsatCore(final List<T> propositions, final boolean isMus) {
        this.propositions = propositions;
        this.isMus = isMus;
    }

    /**
     * Returns the propositions of this MUS.
     * @return the propositions of this MUS
     */
    public List<T> getPropositions() {
        return propositions;
    }

    /**
     * Returns {@code true} if this core is a MUS and {@code false} if it is
     * unknown whether it is a MUS. Note, if set to {@code false} this core
     * might be a MUS, but it is not yet verified.
     * @return {@code true} if this core is a MUS and {@code false} if it is
     * unknown whether it is a MUS.
     */
    public boolean isMus() {
        return isMus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(propositions, isMus);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnsatCore)) {
            return false;
        }
        final UnsatCore<?> unsatCore = (UnsatCore<?>) o;
        return isMus == unsatCore.isMus && Objects.equals(propositions, unsatCore.propositions);
    }

    @Override
    public String toString() {
        return String.format("UnsatCore{isMus=%s, propositions=%s}", isMus, propositions);
    }
}
