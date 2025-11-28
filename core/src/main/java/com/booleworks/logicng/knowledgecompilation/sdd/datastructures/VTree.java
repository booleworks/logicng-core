package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import java.util.BitSet;
import java.util.Map;

/**
 * The parent class for vtree nodes.
 * <p>
 * VTree are complete binary trees where each leaf holds a unique variable.
 * They are used to dictate the structure of an SDD by recursively partitioning
 * a set of variables.
 * <p>
 * [1] Darwiche, Adnan (2011). "SDD: A New Canonical Representation of
 * Propositional Knowledge Bases". International Joint Conference on Artificial
 * Intelligence.
 * @version 3.0.0
 * @since 3.0.0
 */
public abstract class VTree {
    protected final int id;
    private VTree parent = null;
    private int position = -1;

    protected BitSet contextClauseMask = null;
    protected BitSet contextLitsInMask = null;
    protected Map<BitSet, Map<BitSet, SddNode>> cache;

    /**
     * Casts the node to an internal node.
     * <p>
     * The caller is responsible for ensuring that the node is an internal node.
     * @return this node as internal node
     */
    public VTreeInternal asInternal() {
        return (VTreeInternal) this;
    }

    /**
     * Casts the node to a leaf.
     * <p>
     * The caller is responsible for ensuring that the node is a leaf.
     * @return this node as leaf
     */
    public VTreeLeaf asLeaf() {
        return (VTreeLeaf) this;
    }

    protected VTree(final int id) {
        this.id = id;
    }

    /**
     * Returns whether the node is a leaf.
     * @return whether the node is a leaf
     */
    public abstract boolean isLeaf();

    /**
     * Returns the first (leftest) leaf of this node.
     * @return the first (leftest) leaf of this node
     */
    public abstract VTree getFirst();

    /**
     * Returns the last (rightest) leaf of this node.
     * @return the last (rightest) leaf of this node
     */
    public abstract VTree getLast();

    /**
     * Returns the unique node of this node.
     * @return the unique node of this node
     */
    public int getId() {
        return id;
    }

    void setPosition(final int position) {
        this.position = position;
    }

    /**
     * Returns the position (with respect to the inorder traversal order) of
     * this node within the active vtree root of the SDD container.
     * @return the position within in active vtree root
     */
    public int getPosition() {
        return position;
    }

    /**
     * Returns the parent node of this node in the active vtree root of the
     * SDD container.
     * @return the parent node in the active vtree root
     */
    public VTree getParent() {
        return parent;
    }

    void setParent(final VTree parent) {
        this.parent = parent;
    }

    /**
     * Returns a cached mask for a clauses that are in the context closure of
     * this vtree with respect to an unknown formula.
     * <p>
     * <strong>Important:</strong> Do not use this value. This value is usually
     * {@code null}.  It is used by the top-down compiler to cache
     * information and is removed after the compilation.  Furthermore, this
     * value was computed for a formula internal to the compiler and doesn't
     * make sense for any other computation.
     * @return clauses in the context closure if this vtree.
     */
    public BitSet getContextClauseMask() {
        return contextClauseMask;
    }

    /**
     * Caches a mask for clauses that are in the context closure of a formula.
     * <p>
     * <strong>Important:</strong> Do not use this function.  This is a caching
     * slot reserved for the top-down compiler.  Changing this value during a
     * compilation might result in wrong results, and stored values in this slot
     * might be overridden by a compiler.
     */
    public void setContextClauseMask(final BitSet contextClauseMask) {
        this.contextClauseMask = contextClauseMask;
    }

    /**
     * Returns a cached mask for a literals that are in the context closure of
     * this vtree with respect to an unknown formula.
     * <p>
     * <strong>Important:</strong> Do not use this value. This value is usually
     * {@code null}.  It is used by the top-down compiler to cache
     * information and is removed after the compilation.  Furthermore, this
     * value was computed for a formula internal to the compiler and doesn't
     * make sense for any other computation.
     * @return literals in the context closure if this vtree.
     */
    public BitSet getContextLitsInMask() {
        return contextLitsInMask;
    }

    /**
     * Caches a mask for literals that are in the context closure of a formula.
     * <p>
     * <strong>Important:</strong> Do not use this function.  This is a caching
     * slot reserved for the top-down compiler.  Changing this value during a
     * compilation might result in wrong results, and stored values in this slot
     * might be overridden by a compiler.
     */
    public void setContextLitsInMask(final BitSet contextLitsInMask) {
        this.contextLitsInMask = contextLitsInMask;
    }

    /**
     * Returns a cache that stores intermediate results during the SDD
     * compilation.
     * <p>
     * <strong>Important:</strong> Do not use this value. This value is usually
     * {@code null}.  It is used by the top-down compiler to cache
     * information and is removed after the compilation.  Furthermore, this
     * value was computed for a formula internal to the compiler and doesn't
     * make sense for any other computation.
     * @return literals in the context closure if this vtree.
     */
    public Map<BitSet, Map<BitSet, SddNode>> getCache() {
        return cache;
    }

    /**
     * Cache for intermediate results of the SDD compiler.
     * <p>
     * <strong>Important:</strong> Do not use this function.  This is a caching
     * slot reserved for the top-down compiler.  Changing this value during a
     * compilation might result in wrong results, and stored values in this slot
     * might be overridden by a compiler.
     */
    public void setCache(
            final Map<BitSet, Map<BitSet, SddNode>> cache) {
        this.cache = cache;
    }

    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof VTree)) {
            return false;
        }

        final VTree vTree = (VTree) o;
        return id == vTree.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
