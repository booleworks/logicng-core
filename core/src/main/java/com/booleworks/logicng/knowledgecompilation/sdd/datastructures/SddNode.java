// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddFunction;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddVariablesFunction;

import java.util.SortedSet;

/**
 * An SDD node. The building block for SDDs.
 * <p>
 * It has two implementations: 1) {@link SddNodeTerminal} which stores a literal
 * or a constant. 2) {@link SddNodeDecomposition} which stores a compressed and
 * trimmed partition of SDD elements.
 * <p>
 * An SDD node can only be constructed using an {@link Sdd} container.
 * Furthermore, it is suggested to use
 * {@link com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler
 * SddCompiler} to construct SDDs, as manual construction can result in SDDs
 * that violate invariants and therefore in undefined behaviour.
 * <p>
 * Similar as for the caching formula factory, nodes are cached and reused so
 * equal nodes are the same object. Due to the canonicity of trimmed and
 * compressed SDDs, this also means that two nodes semantically equivalent iff
 * they are equal nodes iff they are the same object.
 * @version 3.0.0
 * @since 3.0.0
 */
public abstract class SddNode implements Comparable<SddNode> {
    protected final int id;
    private final VTree vTree;
    private SddNode negation;
    private Sdd.VSCacheEntry<Integer> size;

    SddNode(final int id, final VTree vTree) {
        this.id = id;
        this.vTree = vTree;
        this.negation = null;
        this.size = null;
    }

    /**
     * Returns the unique id of the SDD node.
     * @return the unique id of the SDD node
     */
    public int getId() {
        return id;
    }

    /**
     * Returns whether the node is trivial, i.e., represent {@code true} or
     * {@code false}.
     * @return whether the node is trivial
     */
    abstract public boolean isTrivial();

    /**
     * Returns whether the node represents {@code true}.
     * @return whether the node represents {@code true}
     */
    abstract public boolean isTrue();

    /**
     * Returns whether the node represents {@code false}.
     * @return whether the node represents {@code false}
     */
    abstract public boolean isFalse();

    /**
     * Returns whether the node represents a literal.
     * @return whether the node represents a literal
     */
    abstract public boolean isLiteral();

    /**
     * Returns whether the node is a decomposition node.
     * @return whether the node is a decomposition node
     */
    abstract public boolean isDecomposition();

    /**
     * Casts this node to a decomposition node.
     * @return this node as a decomposition node
     */
    public SddNodeDecomposition asDecomposition() {
        return (SddNodeDecomposition) this;
    }

    /**
     * Casts this node to a terminal node.
     * @return this node as a terminal node
     */
    public SddNodeTerminal asTerminal() {
        return (SddNodeTerminal) this;
    }

    public VTree getVTree() {
        return vTree;
    }

    SddNode getNegation() {
        return negation;
    }

    void setNegation(final SddNode negation) {
        this.negation = negation;
    }

    void setSizeEntry(final Sdd.VSCacheEntry<Integer> size) {
        this.size = size;
    }

    Sdd.VSCacheEntry<Integer> getSizeEntry() {
        return size;
    }

    /**
     * Returns the internal variable indices of this node and all its children.
     * @return the internal variable indices of this node and all its children.
     */
    public SortedSet<Integer> variables() {
        return SddUtil.variables(this);
    }

    /**
     * Returns the variables of this node and all its children.
     * @return the variables of this node and all its children.
     */
    public SortedSet<Variable> variables(final Sdd sdd) {
        return execute(new SddVariablesFunction(sdd));
    }

    /**
     * Runs an SDD function on this node.
     * @param function the SDD function
     * @param <RESULT> the result type of the SDD function
     * @return the result of the SDD function
     */
    public <RESULT> RESULT execute(final SddFunction<RESULT> function) {
        return function.execute(this);
    }

    /**
     * Runs an SDD function on this node with a computation handler.
     * @param function the SDD function
     * @param handler  the computation handler
     * @param <RESULT> the result type of the SDD function
     * @return the result of the SDD function or the cancel cause if the
     * function was aborted by the handler
     */
    public <RESULT> LngResult<RESULT> execute(final SddFunction<RESULT> function, final ComputationHandler handler) {
        return function.execute(this, handler);
    }


    @Override
    public int compareTo(final SddNode o) {
        return id - o.id;
    }

    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof SddNode)) {
            return false;
        }

        final SddNode sddNode = (SddNode) o;
        return id == sddNode.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
