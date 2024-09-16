// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.cache;

/**
 * The pre-defined predicate cache entries.
 * @version 3.0.0
 * @since 1.0
 */
public enum PredicateCacheEntry implements CacheEntry {
    IS_NNF("negation normal form"),
    IS_CNF("conjunctive normal form"),
    IS_DNF("disjunctive normal form"),
    IS_AIG("and-inverter graph"),
    IS_SAT("satisfiable");

    private final String description;

    /**
     * Constructs a new entry.
     * @param description the description of this entry
     */
    PredicateCacheEntry(final String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return "PredicateCacheEntry{description=" + description + "}";
    }
}
