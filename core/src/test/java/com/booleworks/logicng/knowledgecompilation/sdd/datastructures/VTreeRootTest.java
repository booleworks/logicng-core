// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.util.Pair;
import org.junit.jupiter.api.Test;

public class VTreeRootTest {
    @Test
    public void testBuilder() {
        final FormulaFactory f = FormulaFactory.caching();
        final VTreeRoot.Builder builder = VTreeRoot.builder();
        final Variable v1 = f.variable("v1");
        final Variable v2 = f.variable("v2");

        final VTreeLeaf leaf1 = builder.vTreeLeaf(f.variable("v1"));
        final VTreeLeaf leaf2 = builder.vTreeLeaf(f.variable("v2"));
        assertThat(leaf1.getVariable()).isEqualTo(builder.getVariables().variableToIndex(v1));
        assertThat(leaf2.getVariable()).isEqualTo(builder.getVariables().variableToIndex(v2));

        final VTreeInternal node1 = builder.vTreeInternal(leaf1, leaf2);
        assertThat(node1.getLeft()).isEqualTo(leaf1);
        assertThat(node1.getRight()).isEqualTo(leaf2);

        final VTreeLeaf leaf3 = builder.vTreeLeaf(f.variable("v3"));
        final VTreeInternal node2 = builder.vTreeInternal(node1, leaf3);
        assertThat(node2.getLeft()).isEqualTo(node1);
        assertThat(node2.getRight()).isEqualTo(leaf3);

        final VTreeRoot root = builder.build(node2);
        assertThat(root.getRoot()).isEqualTo(node2);
        assertThat(root.getVTreeLeaf(v1)).isEqualTo(leaf1);
        assertThat(root.getVTreeLeaf(root.getVariables().variableToIndex(v2))).isEqualTo(leaf2);
    }

    @Test
    public void testVTreeParents() {
        final VTreeRoot root = buildSimpleTree();
        assertThat(root.getRoot().getParent()).isNull();
        assertThat(root.getRoot().asInternal().getRight().getParent()).isEqualTo(root.getRoot());
        final VTreeInternal node2 = root.getRoot().asInternal().getLeft().asInternal();
        assertThat(node2.getParent()).isEqualTo(root.getRoot());
        assertThat(node2.getLeft().getParent()).isEqualTo(node2);
        assertThat(node2.getRight().getParent()).isEqualTo(node2);
    }

    @Test
    public void testVTreePosition() {
        final VTreeRoot root = buildSimpleTree();
        assertThat(root.getRoot().getPosition()).isEqualTo(3);
        assertThat(root.getRoot().asInternal().getRight().getPosition()).isEqualTo(4);
        final VTreeInternal node2 = root.getRoot().asInternal().getLeft().asInternal();
        assertThat(node2.getPosition()).isEqualTo(1);
        assertThat(node2.getLeft().getPosition()).isEqualTo(0);
        assertThat(node2.getRight().getPosition()).isEqualTo(2);
    }

    @Test
    public void testLookup() {
        final VTreeRoot root = buildSimpleTree();
        final VTreeLeaf leaf3 = root.getRoot().asInternal().getRight().asLeaf();
        final VTreeInternal node1 = root.getRoot().asInternal().getLeft().asInternal();
        final VTreeLeaf leaf1 = node1.getLeft().asLeaf();
        final VTreeLeaf leaf2 = node1.getRight().asLeaf();
        assertThat(root.getVTreeInternal(leaf1, leaf2)).isEqualTo(node1);
        assertThat(root.getVTreeInternal(leaf2, leaf1)).isNull();
        assertThat(root.getVTreeInternal(node1, leaf3)).isEqualTo(root.getRoot());
        assertThat(root.getVTreeInternal(leaf3, node1)).isNull();
        assertThat(root.getVTreeLeaf(leaf1.getVariable())).isEqualTo(leaf1);
        assertThat(root.getVTreeLeaf(leaf2.getVariable())).isEqualTo(leaf2);
        assertThat(root.getVTreeLeaf(leaf3.getVariable())).isEqualTo(leaf3);
    }

    @Test
    public void testSubtree() {
        final VTreeRoot root = buildSimpleTree();
        final VTreeInternal node2 = root.getRoot().asInternal();
        final VTreeInternal node1 = node2.getLeft().asInternal();
        assertThat(root.isSubtree(node2, node2)).isTrue();
        assertThat(root.isSubtree(node1, node2)).isTrue();
        assertThat(root.isSubtree(node2, node1)).isFalse();
        assertThat(root.isSubtree(node1, node1)).isTrue();
        assertThat(root.isSubtree(node2.getRight(), node2)).isTrue();
        assertThat(root.isSubtree(node2.getRight(), node1)).isFalse();
        assertThat(root.isSubtree(node1.getLeft(), node2)).isTrue();
    }

    @Test
    public void testLcaOf() {
        final VTreeRoot root = buildSimpleTree();
        final VTreeInternal node2 = root.getRoot().asInternal();
        final VTreeInternal node1 = node2.getLeft().asInternal();
        assertThat(root.lcaOf(node2, node2)).isEqualTo(node2);
        assertThat(root.lcaOf(node1.getLeft(), node1.getRight())).isEqualTo(node1);
        assertThat(root.lcaOf(node1, node2)).isEqualTo(node2);
        assertThat(root.lcaOf(node1.getRight(), node2)).isEqualTo(node2);
        assertThat(root.lcaOf(node1.getRight(), node2.getRight())).isEqualTo(node2);

        assertThat(root.lcaOf(node2.getPosition(), node2.getPosition())).isEqualTo(node2);
        assertThat(root.lcaOf(node1.getLeft().getPosition(), node1.getRight().getPosition())).isEqualTo(node1);
        assertThat(root.lcaOf(node1.getPosition(), node2.getPosition())).isEqualTo(node2);
        assertThat(root.lcaOf(node1.getRight().getPosition(), node2.getPosition())).isEqualTo(node2);
        assertThat(root.lcaOf(node1.getRight().getPosition(), node2.getRight().getPosition())).isEqualTo(node2);
    }

    @Test
    public void testCmpVTrees() {
        final VTreeRoot root = buildSimpleTree();
        final VTreeInternal node2 = root.getRoot().asInternal();
        final VTreeInternal node1 = node2.getLeft().asInternal();
        assertThat(root.cmpVTrees(node2, node2)).isEqualTo(new Pair<>(node2, VTreeRoot.CmpType.EQUALS));
        assertThat(root.cmpVTrees(node1, node1)).isEqualTo(new Pair<>(node1, VTreeRoot.CmpType.EQUALS));
        assertThat(root.cmpVTrees(node2, node2.getRight()))
                .isEqualTo(new Pair<>(node2, VTreeRoot.CmpType.RIGHT_SUBTREE));
        assertThat(root.cmpVTrees(node1, node2)).isEqualTo(new Pair<>(node2, VTreeRoot.CmpType.LEFT_SUBTREE));
        assertThat(root.cmpVTrees(node1.getLeft(), node2)).isEqualTo(new Pair<>(node2, VTreeRoot.CmpType.LEFT_SUBTREE));
        assertThat(root.cmpVTrees(node1.getLeft(), node2.getRight()))
                .isEqualTo(new Pair<>(node2, VTreeRoot.CmpType.INCOMPARABLE));
        assertThat(root.cmpVTrees(node1.getLeft(), node1.getRight()))
                .isEqualTo(new Pair<>(node1, VTreeRoot.CmpType.INCOMPARABLE));
    }

    private VTreeRoot buildSimpleTree() {
        final FormulaFactory f = FormulaFactory.caching();
        final VTreeRoot.Builder builder = VTreeRoot.builder();
        final Variable v1 = f.variable("v1");
        final Variable v2 = f.variable("v2");
        final VTreeLeaf leaf1 = builder.vTreeLeaf(v1);
        final VTreeLeaf leaf2 = builder.vTreeLeaf(v2);
        final VTreeInternal node1 = builder.vTreeInternal(leaf1, leaf2);
        final VTreeLeaf leaf3 = builder.vTreeLeaf(f.variable("v3"));
        final VTreeInternal node2 = builder.vTreeInternal(node1, leaf3);
        return builder.build(node2);
    }
}
