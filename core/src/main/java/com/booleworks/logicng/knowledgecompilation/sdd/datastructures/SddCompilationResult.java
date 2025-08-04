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
     * @param node the node constructed by the compiler
     * @param sdd  the SDD container used for the compilation
     */
    public SddCompilationResult(final SddNode node, final Sdd sdd) {
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
