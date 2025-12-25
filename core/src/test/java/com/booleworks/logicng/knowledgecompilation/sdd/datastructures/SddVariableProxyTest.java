// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCoreSolver;
import org.junit.jupiter.api.Test;

public class SddVariableProxyTest {
    @Test
    public void testEmpty() {
        final FormulaFactory f = FormulaFactory.caching();
        final Variable v1 = f.variable("v1");
        final Variable v2 = f.variable("v2");
        final Variable v3 = f.variable("v3");
        final SddVariableProxy proxy = SddVariableProxy.empty();
        final int idx1 = proxy.newVar(v1);
        final int idx2 = proxy.newVar(v2);
        assertThat(idx1).isNotEqualTo(idx2);
        assertThat(proxy.knows(v1)).isTrue();
        assertThat(proxy.knows(v2)).isTrue();
        assertThat(proxy.knows(v3)).isFalse();
        assertThat(proxy.variableToIndex(v1)).isEqualTo(idx1);
        assertThat(proxy.variableToIndex(v2)).isEqualTo(idx2);
        assertThat(proxy.variableToIndex(v3)).isEqualTo(-1);
        assertThat(proxy.indexToVariable(f, idx1).getName()).isEqualTo(v1.getName());
        assertThat(proxy.indexToVariable(f, idx2).getName()).isEqualTo(v2.getName());

        final int litIdx1 = proxy.literalToIndex(idx1, true);
        final int litIdx2 = proxy.literalToIndex(v2);
        assertThat(litIdx1).isNotEqualTo(litIdx2);
        assertThat(proxy.litIdxToVarIdx(litIdx1)).isEqualTo(idx1);
        assertThat(proxy.litIdxToVarIdx(litIdx2)).isEqualTo(idx2);
        assertThat(proxy.indexToLiteral(f, litIdx1).getName()).isEqualTo(v1.getName());
        assertThat(proxy.indexToLiteral(f, litIdx1).getPhase()).isTrue();
        assertThat(proxy.indexToLiteral(f, litIdx2).getName()).isEqualTo(v2.getName());
        assertThat(proxy.indexToLiteral(f, litIdx2).getPhase()).isTrue();

        final int notLitIdx1 = proxy.negateLitIdx(litIdx1);
        final int notLitIdx2 = proxy.negateLitIdx(litIdx2);
        assertThat(proxy.negateLitIdx(notLitIdx1)).isEqualTo(litIdx1);
        assertThat(proxy.negateLitIdx(notLitIdx2)).isEqualTo(litIdx2);
        assertThat(proxy.litIdxToVarIdx(notLitIdx1)).isEqualTo(idx1);
        assertThat(proxy.litIdxToVarIdx(notLitIdx2)).isEqualTo(idx2);
        assertThat(proxy.indexToLiteral(f, notLitIdx1).getName()).isEqualTo(v1.getName());
        assertThat(proxy.indexToLiteral(f, notLitIdx1).getPhase()).isFalse();
        assertThat(proxy.indexToLiteral(f, notLitIdx2).getName()).isEqualTo(v2.getName());
        assertThat(proxy.indexToLiteral(f, notLitIdx2).getPhase()).isFalse();
    }

    @Test
    public void testSolver() {
        final FormulaFactory f = FormulaFactory.caching();
        final SddCoreSolver solver = new SddCoreSolver(f, 2);
        final Variable v1 = f.variable("v1");
        final Variable v2 = f.variable("v2");
        final Variable v3 = f.variable("v3");
        solver.add(v1);
        solver.add(v2);

        final SddVariableProxy proxy = SddVariableProxy.fromSolver(solver);
        final int idx1 = proxy.variableToIndex(v1);
        final int idx2 = proxy.variableToIndex(v2);
        assertThrows(UnsupportedOperationException.class, () -> proxy.newVar(v1));
        assertThrows(UnsupportedOperationException.class, () -> proxy.newVar(v2));
        assertThrows(UnsupportedOperationException.class, () -> proxy.newVar(v3));
        assertThat(idx1).isNotEqualTo(idx2);
        assertThat(proxy.knows(v1)).isTrue();
        assertThat(proxy.knows(v2)).isTrue();
        assertThat(proxy.knows(v3)).isFalse();
        assertThat(proxy.variableToIndex(v3)).isEqualTo(-1);
        assertThat(proxy.indexToVariable(f, idx1).getName()).isEqualTo(v1.getName());
        assertThat(proxy.indexToVariable(f, idx2).getName()).isEqualTo(v2.getName());

        final int litIdx1 = proxy.literalToIndex(idx1, true);
        final int litIdx2 = proxy.literalToIndex(v2);
        assertThat(litIdx1).isNotEqualTo(litIdx2);
        assertThat(proxy.litIdxToVarIdx(litIdx1)).isEqualTo(idx1);
        assertThat(proxy.litIdxToVarIdx(litIdx2)).isEqualTo(idx2);
        assertThat(proxy.indexToLiteral(f, litIdx1).getName()).isEqualTo(v1.getName());
        assertThat(proxy.indexToLiteral(f, litIdx1).getPhase()).isTrue();
        assertThat(proxy.indexToLiteral(f, litIdx2).getName()).isEqualTo(v2.getName());
        assertThat(proxy.indexToLiteral(f, litIdx2).getPhase()).isTrue();

        final int notLitIdx1 = proxy.negateLitIdx(litIdx1);
        final int notLitIdx2 = proxy.negateLitIdx(litIdx2);
        assertThat(proxy.negateLitIdx(notLitIdx1)).isEqualTo(litIdx1);
        assertThat(proxy.negateLitIdx(notLitIdx2)).isEqualTo(litIdx2);
        assertThat(proxy.litIdxToVarIdx(notLitIdx1)).isEqualTo(idx1);
        assertThat(proxy.litIdxToVarIdx(notLitIdx2)).isEqualTo(idx2);
        assertThat(proxy.indexToLiteral(f, notLitIdx1).getName()).isEqualTo(v1.getName());
        assertThat(proxy.indexToLiteral(f, notLitIdx1).getPhase()).isFalse();
        assertThat(proxy.indexToLiteral(f, notLitIdx2).getName()).isEqualTo(v2.getName());
        assertThat(proxy.indexToLiteral(f, notLitIdx2).getPhase()).isFalse();
    }
}
