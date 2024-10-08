// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.cache;

/**
 * The pre-defined function cache entries.
 * @version 3.0.0
 * @since 1.0
 */
public enum FunctionCacheEntry implements CacheEntry {
    SUBFORMULAS("sub-formulas"),
    VARPROFILE("variable profile"),
    LITPROFILE("literal profile"),
    DNNF_MODELCOUNT("DNNF model count"),
    DEPTH("Formula depth"),
    NUMBER_OF_ATOMS("number of atoms"),
    NUMBER_OF_NODES("number of nodes"),
    VARIABLES("variables"),
    LITERALS("literals");

    private final String description;

    /**
     * Constructs a new entry.
     * @param description the description of this entry
     */
    FunctionCacheEntry(final String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return "FunctionCacheEntry{description=" + description + "}";
    }
}
