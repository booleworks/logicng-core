// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

/**
 * A class storing the result of an SDD compilation.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddCompilationResult {
    private final Sdd sdd;
    private final SddNode node;

    /**
     * Constructs a new compilation result.
     * @param sdd  the SDD container used for the compilation
     * @param node the node constructed by the compiler
     */
    public SddCompilationResult(final Sdd sdd, final SddNode node) {
        this.node = node;
        this.sdd = sdd;
    }

    /**
     * Returns the SDD container used for the compilation.
     * @return the SDD container used for the compilation
     */
    public Sdd getSdd() {
        return sdd;
    }

    /**
     * Return the SDD node constructed by the compiler.
     * @return the SDD node constructed by the compiler
     */
    public SddNode getNode() {
        return node;
    }
}
