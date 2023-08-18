// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;

public class CFalseTest extends TestWithExampleFormulas {

    @Test
    public void testType() {
        assertThat(FALSE.type()).isEqualTo(FType.FALSE);
    }

    @Test
    public void testNumberOfAtoms() {
        assertThat(FALSE.numberOfAtoms()).isEqualTo(1);
    }

    @Test
    public void testNegation() {
        assertThat(FALSE.negate()).isEqualTo(TRUE);
    }

    @Test
    public void testVariables() {
        assertThat(FALSE.variables().size()).isEqualTo(0);
    }

    @Test
    public void testLiterals() {
        assertThat(FALSE.literals().size()).isEqualTo(0);
    }

    @Test
    public void testToString() {
        assertThat(FALSE.toString()).isEqualTo("$false");
    }

    @Test
    public void testEquals() {
        assertThat(f.falsum()).isEqualTo(FALSE);
        assertThat(f.verum()).isNotEqualTo(FALSE);
    }

    @Test
    public void testEqualsDifferentFormulaFactory() {
        assertThat(g.falsum()).isEqualTo(FALSE);
        assertThat(g.verum()).isNotEqualTo(FALSE);
    }

    @Test
    public void testHash() {
        assertThat(FALSE.hashCode()).isEqualTo(f.falsum().hashCode());
    }

    @Test
    public void testNumberOfNodes() {
        assertThat(FALSE.numberOfNodes()).isEqualTo(1);
    }

    @Test
    public void testNumberOfInternalNodes() {
        assertThat(FALSE.numberOfInternalNodes()).isEqualTo(1);
    }

    @Test
    public void testNumberOfOperands() {
        assertThat(FALSE.numberOfOperands()).isEqualTo(0);
    }

    @Test
    public void testIsConstantFormula() {
        assertThat(FALSE.isConstantFormula()).isTrue();
    }

    @Test
    public void testAtomicFormula() {
        assertThat(FALSE.isAtomicFormula()).isTrue();
    }

    @Test
    public void testContains() {
        assertThat(FALSE.containsVariable(f.variable("a"))).isFalse();
    }

    @Test
    public void testIsNNF() {
        assertThat(FALSE.isNNF()).isTrue();
    }

    @Test
    public void testIsDNF() {
        assertThat(FALSE.isDNF()).isTrue();
    }

    @Test
    public void testIsCNF() {
        assertThat(FALSE.isCNF()).isTrue();
    }
}
