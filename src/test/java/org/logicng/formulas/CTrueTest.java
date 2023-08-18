// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;

public class CTrueTest extends TestWithExampleFormulas {

    @Test
    public void testType() {
        assertThat(TRUE.type()).isEqualTo(FType.TRUE);
    }

    @Test
    public void testNumberOfAtoms() {
        assertThat(TRUE.numberOfAtoms()).isEqualTo(1);
        assertThat(TRUE.numberOfAtoms()).isEqualTo(1);
    }

    @Test
    public void testNegation() {
        assertThat(TRUE.negate()).isEqualTo(FALSE);
    }

    @Test
    public void testVariables() {
        assertThat(TRUE.variables().size()).isEqualTo(0);
    }

    @Test
    public void testLiterals() {
        assertThat(TRUE.literals().size()).isEqualTo(0);
    }

    @Test
    public void testToString() {
        assertThat(TRUE.toString()).isEqualTo("$true");
    }

    @Test
    public void testEquals() {
        assertThat(f.verum()).isEqualTo(TRUE);
        assertThat(f.falsum()).isNotEqualTo(TRUE);
    }

    @Test
    public void testEqualsDifferentFormulaFactory() {
        assertThat(g.verum()).isEqualTo(TRUE);
        assertThat(g.falsum()).isNotEqualTo(TRUE);
    }

    @Test
    public void testHash() {
        assertThat(TRUE.hashCode()).isEqualTo(f.verum().hashCode());
    }

    @Test
    public void testNumberOfNodes() {
        assertThat(TRUE.numberOfNodes()).isEqualTo(1);
    }

    @Test
    public void testNumberOfInternalNodes() {
        assertThat(TRUE.numberOfInternalNodes()).isEqualTo(1);
    }

    @Test
    public void testNumberOfOperands() {
        assertThat(TRUE.numberOfOperands()).isEqualTo(0);
    }

    @Test
    public void testIsConstantFormula() {
        assertThat(TRUE.isConstantFormula()).isTrue();
    }

    @Test
    public void testAtomicFormula() {
        assertThat(TRUE.isAtomicFormula()).isTrue();
    }

    @Test
    public void testContains() {
        assertThat(TRUE.containsVariable(f.variable("a"))).isFalse();
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
