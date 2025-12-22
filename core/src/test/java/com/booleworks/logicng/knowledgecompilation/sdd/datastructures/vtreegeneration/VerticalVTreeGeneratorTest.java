// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtreegeneration;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

public class VerticalVTreeGeneratorTest {
    @Test
    public void testSimpleCases() {
        final FormulaFactory f = FormulaFactory.caching();

        final Set<Variable> s1 = f.variables("A");
        final VTreeRoot v1 = generateFor(s1);
        assertThat(SddTestUtil.isCompleteVTree(f, v1.getRoot(), s1, v1.getVariables())).isTrue();
        assertThat(isVertical(v1.getRoot())).isTrue();

        final Set<Variable> s2 = f.variables("A", "B");
        final VTreeRoot v2 = generateFor(s2);
        assertThat(SddTestUtil.isCompleteVTree(f, v2.getRoot(), s2, v2.getVariables())).isTrue();
        assertThat(isVertical(v2.getRoot())).isTrue();

        final Set<Variable> s3 = f.variables("A", "B", "C");
        final VTreeRoot v3 = generateFor(s3);
        assertThat(SddTestUtil.isCompleteVTree(f, v3.getRoot(), s3, v3.getVariables())).isTrue();
        assertThat(isVertical(v3.getRoot())).isTrue();

        final Set<Variable> s5 = f.variables("A", "B", "C", "D", "E");
        final VTreeRoot v5 = generateFor(s5);
        assertThat(SddTestUtil.isCompleteVTree(f, v5.getRoot(), s5, v5.getVariables())).isTrue();
        assertThat(isVertical(v5.getRoot())).isTrue();
    }

    @Test
    public void testWithFormula() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = f.and(DimacsReader.readCNF(f, "../test_files/sdd/compile_example1.cnf"));
        final VTreeRoot root = generateFor(formula.variables(f));
        assertThat(SddTestUtil.isCompleteVTree(f, root.getRoot(), formula.variables(f), root.getVariables())).isTrue();
        assertThat(isVertical(root.getRoot())).isTrue();
    }

    private VTreeRoot generateFor(final Set<Variable> vars) {
        final VTreeRoot.Builder builder = VTreeRoot.builder();
        final VTree vtree = new VerticalVTreeGenerator(vars).generate(builder);
        return builder.build(vtree);
    }

    private boolean isVertical(final VTree vtree) {
        return isVertical(vtree, false) || isVertical(vtree, true);
    }

    private boolean isVertical(final VTree vtree, final boolean polarisation) {
        if (vtree.isLeaf()) {
            return true;
        } else {
            final VTreeInternal node = (VTreeInternal) vtree;
            if (polarisation) {
                return node.getLeft().isLeaf() && isVertical(node.getRight(), false);
            } else {
                return node.getRight().isLeaf() && isVertical(node.getLeft(), true);
            }
        }
    }
}
