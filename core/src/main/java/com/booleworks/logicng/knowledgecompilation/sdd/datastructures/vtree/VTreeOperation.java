package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

/**
 * Global vtree transformations
 */
public enum VTreeOperation {
    /**
     * Rotates the vtree to the left
     */
    ROTATE_LEFT,

    /**
     * Rotates the vtree to the right
     */
    ROTATE_RIGHT,

    /**
     * Swaps the two children of a vtree node
     */
    SWAP
}
