// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.datastructures.Substitution;

public class LiteralTest extends TestWithExampleFormulas {

    @Test
    public void testType() {
        assertThat(A.type()).isEqualTo(FType.LITERAL);
        assertThat(NA.type()).isEqualTo(FType.LITERAL);
    }

    @Test
    public void testShortcutCreators() {
        assertThat(f.literal("a", true) == f.variable("a")).isTrue();
        assertThat(f.literal("name", true) == f.variable("name")).isTrue();
    }

    @Test
    public void testNegation() {
        assertThat(A.negate() == NA).isTrue();
        assertThat(NA.negate() == A).isTrue();
    }

    @Test
    public void testGetters() {
        assertThat(A.name()).isEqualTo("a");
        assertThat(NA.name()).isEqualTo("a");
        assertThat(A.phase()).isEqualTo(true);
        assertThat(NA.phase()).isEqualTo(false);
    }

    @Test
    public void testVariables() {
        assertThat(A.variables())
                .hasSize(1)
                .containsExactly(A);
        assertThat(NA.variables())
                .hasSize(1)
                .containsExactly(A);
    }

    @Test
    public void testLiterals() {
        assertThat(A.literals())
                .hasSize(1)
                .containsExactly(A);
        assertThat(NA.literals())
                .hasSize(1)
                .containsExactly(NA);
    }

    @Test
    public void testExpSubstitution() {
        final Substitution substitution = new Substitution();
        substitution.addMapping(f.variable("a"), f.literal("b", false));
        substitution.addMapping(f.variable("c"), f.variable("d"));
        substitution.addMapping(f.variable("x"), f.and(f.variable("y"), f.variable("z")));
    }

    @Test
    public void testToString() {
        assertThat(A.toString()).isEqualTo("a");
        assertThat(NA.toString()).isEqualTo("~a");
    }

    @Test
    public void testEquals() {
        assertThat(f.literal("a", true).equals(A)).isTrue();
        assertThat(f.literal("a", false).equals(NA)).isTrue();
        assertThat(A.equals(A)).isTrue();
        assertThat(B.equals(A)).isFalse();
        assertThat(NA.equals(A)).isFalse();
        assertThat(f.falsum()).isNotEqualTo(A);
    }

    @Test
    public void testEqualsDifferentFormulaFactory() {
        assertThat(g.literal("a", true).equals(A)).isTrue();
        assertThat(g.literal("a", false).equals(NA)).isTrue();
        assertThat(g.literal("a", false).equals(A)).isFalse();
        assertThat(g.literal("b", true).equals(A)).isFalse();
        assertThat(g.falsum()).isNotEqualTo(A);
    }

    @Test
    public void testCompareTo() {
        assertThat(A.compareTo(A) == 0).isTrue();
        assertThat(NA.compareTo(NA) == 0).isTrue();
        assertThat(A.compareTo(NA) < 0).isTrue();
        assertThat(A.compareTo(NB) < 0).isTrue();
        assertThat(A.compareTo(B) < 0).isTrue();
        assertThat(A.compareTo(X) < 0).isTrue();
        assertThat(NA.compareTo(NX) < 0).isTrue();
    }

    @Test
    public void testHash() {
        assertThat(f.literal("a", true).hashCode()).isEqualTo(A.hashCode());
        assertThat(f.literal("a", false).hashCode()).isEqualTo(NA.hashCode());
    }

    @Test
    public void testNumberOfAtoms() {
        assertThat(A.numberOfAtoms()).isEqualTo(1);
        assertThat(NA.numberOfAtoms()).isEqualTo(1);
        assertThat(NA.numberOfAtoms()).isEqualTo(1);
    }

    @Test
    public void testNumberOfNodes() {
        assertThat(A.numberOfNodes()).isEqualTo(1);
        assertThat(NA.numberOfNodes()).isEqualTo(1);
        assertThat(NA.numberOfNodes()).isEqualTo(1);
    }

    @Test
    public void testNumberOfInternalNodes() {
        assertThat(A.numberOfInternalNodes()).isEqualTo(1);
        assertThat(NA.numberOfInternalNodes()).isEqualTo(1);
    }

    @Test
    public void testNumberOfOperands() {
        assertThat(A.numberOfOperands()).isEqualTo(0);
        assertThat(NA.numberOfOperands()).isEqualTo(0);
    }

    @Test
    public void testIsConstantFormula() {
        assertThat(A.isConstantFormula()).isFalse();
        assertThat(NA.isConstantFormula()).isFalse();
    }

    @Test
    public void testAtomicFormula() {
        assertThat(A.isAtomicFormula()).isTrue();
        assertThat(NA.isAtomicFormula()).isTrue();
    }

    @Test
    public void testContains() {
        assertThat(A.containsVariable(f.variable("b"))).isFalse();
        assertThat(A.containsVariable(f.variable("a"))).isTrue();
        assertThat(NA.containsVariable(f.variable("b"))).isFalse();
        assertThat(NA.containsVariable(f.variable("a"))).isTrue();
    }

    @Test
    public void testIsNNF() {
        assertThat(A.isNNF()).isTrue();
        assertThat(NA.isNNF()).isTrue();
    }

    @Test
    public void testIsDNF() {
        assertThat(A.isDNF()).isTrue();
        assertThat(NA.isDNF()).isTrue();
    }

    @Test
    public void testIsCNF() {
        assertThat(A.isCNF()).isTrue();
        assertThat(NA.isCNF()).isTrue();
    }

    @Test
    public void testPosNeg() {
        assertThat(A.variable() == A).isTrue();
        assertThat(NA.variable() == A).isTrue();
    }
}
