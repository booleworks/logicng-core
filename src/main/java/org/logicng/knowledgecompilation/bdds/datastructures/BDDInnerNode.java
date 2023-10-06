// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.knowledgecompilation.bdds.datastructures;

import org.logicng.formulas.Variable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A node in a BDD.
 * @version 1.4.0
 * @since 1.4.0
 */
public final class BDDInnerNode implements BDDNode {

    private final Variable var;
    private final BDDNode low;
    private final BDDNode high;

    /**
     * Constructor for a new inner BDD node holding a variable.
     * @param var  the variable
     * @param low  the low child node
     * @param high the high child node
     */
    public BDDInnerNode(final Variable var, final BDDNode low, final BDDNode high) {
        this.var = var;
        this.low = low;
        this.high = high;
    }

    @Override
    public Variable label() {
        return var;
    }

    @Override
    public boolean isInnerNode() {
        return true;
    }

    @Override
    public BDDNode low() {
        return low;
    }

    @Override
    public BDDNode high() {
        return high;
    }

    @Override
    public Set<BDDNode> nodes() {
        final Set<BDDNode> res = new HashSet<>(Collections.singleton(this));
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
        if (other instanceof BDDInnerNode) {
            final BDDInnerNode o = (BDDInnerNode) other;
            return Objects.equals(var, o.var)
                    && Objects.equals(low, o.low)
                    && Objects.equals(high, o.high);
        }
        return false;
    }

    @Override
    public String toString() {
        return "<" + var + " | low=" + low + " high=" + high + ">";
    }
}
