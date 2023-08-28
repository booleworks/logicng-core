// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.knowledgecompilation.dnnf.datastructures.dtree;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;

public class DTreeNodeTest {

    @Test
    public void testToString() throws ParserException {
        final FormulaFactory f = FormulaFactory.nonCaching();
        final DTreeNode node = new DTreeNode(new DTreeLeaf(1, f.parse("a | b"), f), new DTreeLeaf(2, f.parse("c | d"), f), f);
        assertThat(node.toString()).isEqualTo("DTreeNode: [DTreeLeaf: 1, a | b, DTreeLeaf: 2, c | d]");
    }
}
