package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.formulas.Variable;

import java.util.BitSet;

/**
 * The implementation of a vtree leaf.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class VTreeLeaf extends VTree {
    private final int variable;

    private BitSet clausePosMask = null;
    private BitSet clauseNegMask = null;

    /**
     * <strong>Do not use this constructor to construct new nodes.</strong> Use
     * {@link com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd#vTreeLeaf(Variable)  Sdd.vTreeLeaf()},
     * as it ensure necessary invariants.
     * @param id       the unique id of this node
     * @param variable the variable of this leaf
     */
    public VTreeLeaf(final int id, final int variable) {
        super(id);
        this.variable = variable;
    }

    /**
     * Returns the variable of this leaf.
     * @return the variable of this leaf
     */
    public int getVariable() {
        return variable;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public VTree getFirst() {
        return this;
    }

    @Override
    public VTree getLast() {
        return this;
    }

    /**
     * Returns a cached mask for clauses in which the variable of this leaf
     * appears as a positive literal.
     * <p>
     * <strong>Important:</strong> Do not use this value. This value is usually
     * {@code null}.  It is used by the top-down compiler to cache
     * information and is removed after the compilation.  Furthermore, this
     * value was computed for a formula internal to the compiler and doesn't
     * make sense for any other computation.
     * @return clauses containing this variable as positive literal
     */
    public BitSet getClausePosMask() {
        return clausePosMask;
    }

    /**
     * Caches a mask for clauses in which the variable of this leaf appears as
     * a positive literal.
     * <p>
     * <strong>Important:</strong> Do not use this function.  This is a caching
     * slot reserved for the top-down compiler.  Changing this value during a
     * compilation might result in wrong results, and stored values in this slot
     * might be overridden by a compiler.
     */
    public void setClausePosMask(final BitSet clausePosMask) {
        this.clausePosMask = clausePosMask;
    }

    /**
     * Returns a cached mask for clauses in which the variable of this leaf
     * appears as a negative literal.
     * <p>
     * <strong>Important:</strong> Do not use this value. This value is usually
     * {@code null}.  It is used by the top-down compiler to cache
     * information and is removed after the compilation.  Furthermore, this
     * value was computed for a formula internal to the compiler and doesn't
     * make sense for any other computation.
     * @return clauses containing this variable as negative literal
     */
    public BitSet getClauseNegMask() {
        return clauseNegMask;
    }

    /**
     * Caches a mask for clauses in which the variable of this leaf appears as
     * a negative literal.
     * <p>
     * <strong>Important:</strong> Do not use this function.  This is a caching
     * slot reserved for the top-down compiler.  Changing this value during a
     * compilation might result in wrong results, and stored values in this slot
     * might be overridden by a compiler.
     */
    public void setClauseNegMask(final BitSet clauseNegMask) {
        this.clauseNegMask = clauseNegMask;
    }

    @Override
    public String toString() {
        return "(" + getId() + ": " + variable + " )";
    }
}
