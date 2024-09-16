// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.datastructures;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import org.junit.jupiter.api.Test;

public class BddNodeTest {

    @Test
    public void testSimpleMethods() {
        final FormulaFactory f = FormulaFactory.caching();
        final BddNode verumNode = BddConstant.getVerumNode(f);
        final BddNode falsumNode = BddConstant.getFalsumNode(f);
        final BddNode innerNode = new BddInnerNode(f.variable("A"), verumNode, falsumNode);
        assertThat(verumNode.isInnerNode()).isFalse();
        assertThat(falsumNode.isInnerNode()).isFalse();
        assertThat(innerNode.isInnerNode()).isTrue();
        assertThat(verumNode.getLabel()).isEqualTo(f.verum());
        assertThat(falsumNode.getLabel()).isEqualTo(f.falsum());
        assertThat(innerNode.getLabel()).isEqualTo(f.variable("A"));
        assertThat(verumNode.getLow()).isNull();
        assertThat(verumNode.getHigh()).isNull();
        assertThat(falsumNode.getLow()).isNull();
        assertThat(falsumNode.getHigh()).isNull();
        assertThat(innerNode.getLow()).isEqualTo(verumNode);
        assertThat(innerNode.getHigh()).isEqualTo(falsumNode);
    }

    @Test
    public void testNodes() {
        final FormulaFactory f = FormulaFactory.caching();
        final BddNode verumNode = BddConstant.getVerumNode(f);
        final BddNode falsumNode = BddConstant.getFalsumNode(f);
        final BddNode innerNode = new BddInnerNode(f.variable("A"), verumNode, falsumNode);
        assertThat(verumNode.nodes()).containsExactly(verumNode);
        assertThat(falsumNode.nodes()).containsExactly(falsumNode);
        assertThat(innerNode.nodes()).containsExactlyInAnyOrder(verumNode, falsumNode, innerNode);
    }

    @Test
    public void testHashCode() {
        final FormulaFactory f = FormulaFactory.caching();
        final BddNode verumNode = BddConstant.getVerumNode(f);
        final BddNode falsumNode = BddConstant.getFalsumNode(f);
        final BddNode innerNode1 = new BddInnerNode(f.variable("A"), verumNode, falsumNode);
        final BddNode innerNode2 = new BddInnerNode(f.variable("A"), verumNode, falsumNode);
        assertThat(verumNode.hashCode()).isEqualTo(verumNode.hashCode());
        assertThat(falsumNode.hashCode()).isEqualTo(falsumNode.hashCode());
        assertThat(innerNode1.hashCode()).isEqualTo(innerNode2.hashCode());
    }

    @Test
    public void testEquals() {
        final FormulaFactory f = FormulaFactory.caching();
        final BddNode verumNode = BddConstant.getVerumNode(f);
        final BddNode falsumNode = BddConstant.getFalsumNode(f);
        final BddNode innerNode1 = new BddInnerNode(f.variable("A"), verumNode, falsumNode);
        final BddNode innerNode1a = new BddInnerNode(f.variable("A"), verumNode, falsumNode);
        final BddNode innerNode2 = new BddInnerNode(f.variable("A"), falsumNode, verumNode);
        final BddNode innerNode3 = new BddInnerNode(f.variable("B"), verumNode, falsumNode);
        assertThat(verumNode).isEqualTo(verumNode);
        assertThat(falsumNode).isEqualTo(falsumNode);
        assertThat(innerNode1).isEqualTo(innerNode1);
        assertThat(innerNode1.equals(innerNode1)).isTrue();
        assertThat(innerNode1).isEqualTo(innerNode1a);
        assertThat(innerNode1).isNotEqualTo(innerNode2);
        assertThat(innerNode1).isNotEqualTo(innerNode3);
        assertThat(innerNode1).isNotEqualTo(null);
        assertThat(innerNode1).isNotEqualTo("String");
        assertThat(verumNode).isNotEqualTo(falsumNode);
        assertThat(verumNode).isNotEqualTo(innerNode3);
        assertThat(verumNode).isNotEqualTo(null);
        assertThat(verumNode).isNotEqualTo("String");
    }
}
