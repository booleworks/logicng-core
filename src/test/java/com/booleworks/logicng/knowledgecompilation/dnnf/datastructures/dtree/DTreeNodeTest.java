// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.api.Test;

public class DTreeNodeTest {

    @Test
    public void testToString() throws ParserException {
        final FormulaFactory f = FormulaFactory.nonCaching();
        final DTreeNode node =
                new DTreeNode(f, new DTreeLeaf(f, 1, f.parse("a | b")), new DTreeLeaf(f, 2, f.parse("c | d")));
        assertThat(node.toString()).isEqualTo("DTreeNode: [DTreeLeaf: 1, a | b, DTreeLeaf: 2, c | d]");
    }
}
