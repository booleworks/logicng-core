// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CTrueTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testType(final FormulaContext _c) {
        assertThat(_c.verum.getType()).isEqualTo(FType.TRUE);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfAtoms(final FormulaContext _c) {
        assertThat(_c.verum.numberOfAtoms(_c.f)).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNegation(final FormulaContext _c) {
        assertThat(_c.verum.negate(_c.f)).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testVariables(final FormulaContext _c) {
        assertThat(_c.verum.variables(_c.f).size()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        assertThat(_c.verum.literals(_c.f).size()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testToString(final FormulaContext _c) {
        assertThat(_c.verum.toString()).isEqualTo("$true");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquals(final FormulaContext _c) {
        assertThat(_c.f.verum()).isEqualTo(_c.verum);
        assertThat(_c.f.falsum()).isNotEqualTo(_c.verum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEqualsDifferentFormulaFactory(final FormulaContext _c) {
        assertThat(FormulaFactory.caching().verum()).isEqualTo(_c.verum);
        assertThat(FormulaFactory.caching().falsum()).isNotEqualTo(_c.verum);
        assertThat(FormulaFactory.nonCaching().verum()).isEqualTo(_c.verum);
        assertThat(FormulaFactory.nonCaching().falsum()).isNotEqualTo(_c.verum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testHash(final FormulaContext _c) {
        assertThat(_c.verum.hashCode()).isEqualTo(_c.f.verum().hashCode());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfNodes(final FormulaContext _c) {
        assertThat(_c.verum.numberOfNodes(_c.f)).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfInternalNodes(final FormulaContext _c) {
        assertThat(_c.verum.numberOfInternalNodes()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfOperands(final FormulaContext _c) {
        assertThat(_c.verum.numberOfOperands()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsConstantFormula(final FormulaContext _c) {
        assertThat(_c.verum.isConstantFormula()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAtomicFormula(final FormulaContext _c) {
        assertThat(_c.verum.isAtomicFormula()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testContains(final FormulaContext _c) {
        assertThat(_c.verum.containsVariable(_c.a)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsNNF(final FormulaContext _c) {
        assertThat(_c.verum.isNNF(_c.f)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsDNF(final FormulaContext _c) {
        assertThat(_c.verum.isDNF(_c.f)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsCNF(final FormulaContext _c) {
        assertThat(_c.verum.isCNF(_c.f)).isTrue();
    }
}
