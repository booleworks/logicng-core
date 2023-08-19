// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CFalseTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testType(final FormulaContext _c) {
        assertThat(_c.falsum.type()).isEqualTo(FType.FALSE);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfAtoms(final FormulaContext _c) {
        assertThat(_c.falsum.numberOfAtoms()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNegation(final FormulaContext _c) {
        assertThat(_c.falsum.negate()).isEqualTo(_c.verum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testVariables(final FormulaContext _c) {
        assertThat(_c.falsum.variables().size()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        assertThat(_c.falsum.literals().size()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testToString(final FormulaContext _c) {
        assertThat(_c.falsum.toString()).isEqualTo("$false");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquals(final FormulaContext _c) {
        assertThat(_c.f.falsum()).isEqualTo(_c.falsum);
        assertThat(_c.f.verum()).isNotEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEqualsDifferentFormulaFactory(final FormulaContext _c) {
        assertThat(FormulaFactory.caching().falsum()).isEqualTo(_c.falsum);
        assertThat(FormulaFactory.caching().verum()).isNotEqualTo(_c.falsum);
        assertThat(FormulaFactory.nonCaching().falsum()).isEqualTo(_c.falsum);
        assertThat(FormulaFactory.nonCaching().verum()).isNotEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testHash(final FormulaContext _c) {
        assertThat(_c.falsum.hashCode()).isEqualTo(_c.f.falsum().hashCode());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfNodes(final FormulaContext _c) {
        assertThat(_c.falsum.numberOfNodes()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfInternalNodes(final FormulaContext _c) {
        assertThat(_c.falsum.numberOfInternalNodes()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfOperands(final FormulaContext _c) {
        assertThat(_c.falsum.numberOfOperands()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsConstantFormula(final FormulaContext _c) {
        assertThat(_c.falsum.isConstantFormula()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAtomicFormula(final FormulaContext _c) {
        assertThat(_c.falsum.isAtomicFormula()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testContains(final FormulaContext _c) {
        assertThat(_c.falsum.containsVariable(_c.a)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsNNF(final FormulaContext _c) {
        assertThat(_c.falsum.isNNF()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsDNF(final FormulaContext _c) {
        assertThat(_c.falsum.isDNF()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsCNF(final FormulaContext _c) {
        assertThat(_c.falsum.isCNF()).isTrue();
    }
}
