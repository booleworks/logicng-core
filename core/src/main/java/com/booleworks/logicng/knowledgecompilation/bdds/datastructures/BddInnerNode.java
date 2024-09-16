// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.datastructures;

import com.booleworks.logicng.formulas.Variable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A node in a BDD.
 * @version 3.0.0
 * @since 1.4.0
 */
public final class BddInnerNode implements BddNode {

    private final Variable var;
    private final BddNode low;
    private final BddNode high;

    /**
     * Constructor for a new inner BDD node holding a variable.
     * @param var  the variable
     * @param low  the low child node
     * @param high the high child node
     */
    public BddInnerNode(final Variable var, final BddNode low, final BddNode high) {
        this.var = var;
        this.low = low;
        this.high = high;
    }

    @Override
    public Variable getLabel() {
        return var;
    }

    @Override
    public boolean isInnerNode() {
        return true;
    }

    @Override
    public BddNode getLow() {
        return low;
    }

    @Override
    public BddNode getHigh() {
        return high;
    }

    @Override
    public Set<BddNode> nodes() {
        final Set<BddNode> res = new HashSet<>(Collections.singleton(this));
        res.addAll(low.nodes());
        res.addAll(high.nodes());
        return res;
    }

    @Override
    public int hashCode() {
        return Objects.hash(var, low, high);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof BddInnerNode) {
            final BddInnerNode o = (BddInnerNode) other;
            return Objects.equals(var, o.var) && Objects.equals(low, o.low) && Objects.equals(high, o.high);
        }
        return false;
    }

    @Override
    public String toString() {
        return "<" + var + " | low=" + low + " high=" + high + ">";
    }
}
