// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.datastructures;

import com.booleworks.logicng.formulas.Constant;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A terminal node in a BDD.
 * @version 3.0.0
 * @since 1.4.0
 */
public final class BDDConstant implements BDDNode {

    private final Constant value;

    /**
     * Private constructor.
     * @param value the constant value
     */
    private BDDConstant(final Constant value) {
        this.value = value;
    }

    /**
     * Returns the terminal 0 node.
     * @param f the formula factory
     * @return the terminal 0 node
     */
    public static BDDConstant getFalsumNode(final FormulaFactory f) {
        return new BDDConstant(f.falsum());
    }

    /**
     * Returns the terminal 1 node.
     * @param f the formula factory
     * @return the terminal 1 node
     */
    public static BDDConstant getVerumNode(final FormulaFactory f) {
        return new BDDConstant(f.verum());
    }

    @Override
    public Formula getLabel() {
        return value;
    }

    @Override
    public boolean isInnerNode() {
        return false;
    }

    @Override
    public BDDNode getLow() {
        return null;
    }

    @Override
    public BDDNode getHigh() {
        return null;
    }

    @Override
    public Set<BDDNode> nodes() {
        return new HashSet<>(Collections.singletonList(this));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        return this == other || other instanceof BDDConstant && Objects.equals(value, ((BDDConstant) other).value);
    }

    @Override
    public String toString() {
        return "<" + value + ">";
    }
}
