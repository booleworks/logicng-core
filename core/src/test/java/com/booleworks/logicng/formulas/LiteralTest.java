// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Substitution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class LiteralTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testType(final FormulaContext _c) {
        assertThat(_c.a.getType()).isEqualTo(FType.LITERAL);
        assertThat(_c.na.getType()).isEqualTo(FType.LITERAL);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testShortcutCreators(final FormulaContext _c) {
        assertThat(_c.f.literal("a", true) == _c.f.variable("a")).isTrue();
        assertThat(_c.f.literal("name", true) == _c.f.variable("name")).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNegation(final FormulaContext _c) {
        assertThat(_c.a.negate(_c.f) == _c.na).isTrue();
        assertThat(_c.na.negate(_c.f) == _c.a).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testGetters(final FormulaContext _c) {
        assertThat(_c.a.getName()).isEqualTo("a");
        assertThat(_c.na.getName()).isEqualTo("a");
        assertThat(_c.a.getPhase()).isEqualTo(true);
        assertThat(_c.na.getPhase()).isEqualTo(false);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testVariables(final FormulaContext _c) {
        assertThat(_c.a.variables(_c.f))
                .hasSize(1)
                .containsExactly(_c.a);
        assertThat(_c.na.variables(_c.f))
                .hasSize(1)
                .containsExactly(_c.a);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        assertThat(_c.a.literals(_c.f))
                .hasSize(1)
                .containsExactly(_c.a);
        assertThat(_c.na.literals(_c.f))
                .hasSize(1)
                .containsExactly(_c.na);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testExpSubstitution(final FormulaContext _c) {
        final Substitution substitution = new Substitution();
        substitution.addMapping(_c.f.variable("a"), _c.f.literal("b", false));
        substitution.addMapping(_c.f.variable("c"), _c.f.variable("d"));
        substitution.addMapping(_c.f.variable("x"), _c.f.and(_c.f.variable("y"), _c.f.variable("z")));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testToString(final FormulaContext _c) {
        assertThat(_c.a.toString()).isEqualTo("a");
        assertThat(_c.na.toString()).isEqualTo("~a");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquals(final FormulaContext _c) {
        assertThat(_c.f.literal("a", true).equals(_c.a)).isTrue();
        assertThat(_c.f.literal("a", false).equals(_c.na)).isTrue();
        assertThat(_c.a.equals(_c.a)).isTrue();
        assertThat(_c.b.equals(_c.a)).isFalse();
        assertThat(_c.na.equals(_c.a)).isFalse();
        assertThat(_c.f.falsum()).isNotEqualTo(_c.a);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEqualsDifferentFormulaFactory(final FormulaContext _c) {
        assertThat(FormulaFactory.caching().literal("a", true).equals(_c.a)).isTrue();
        assertThat(FormulaFactory.caching().literal("a", false).equals(_c.na)).isTrue();
        assertThat(FormulaFactory.caching().literal("a", false).equals(_c.a)).isFalse();
        assertThat(FormulaFactory.caching().literal("b", true).equals(_c.a)).isFalse();
        assertThat(FormulaFactory.caching().falsum()).isNotEqualTo(_c.a);

        assertThat(FormulaFactory.nonCaching().literal("a", true).equals(_c.a)).isTrue();
        assertThat(FormulaFactory.nonCaching().literal("a", false).equals(_c.na)).isTrue();
        assertThat(FormulaFactory.nonCaching().literal("a", false).equals(_c.a)).isFalse();
        assertThat(FormulaFactory.nonCaching().literal("b", true).equals(_c.a)).isFalse();
        assertThat(FormulaFactory.nonCaching().falsum()).isNotEqualTo(_c.a);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCompareTo(final FormulaContext _c) {
        assertThat(_c.a.compareTo(_c.a) == 0).isTrue();
        assertThat(_c.na.compareTo(_c.na) == 0).isTrue();
        assertThat(_c.a.compareTo(_c.na) < 0).isTrue();
        assertThat(_c.a.compareTo(_c.nb) < 0).isTrue();
        assertThat(_c.a.compareTo(_c.b) < 0).isTrue();
        assertThat(_c.a.compareTo(_c.x) < 0).isTrue();
        assertThat(_c.na.compareTo(_c.nx) < 0).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testHash(final FormulaContext _c) {
        assertThat(_c.f.literal("a", true).hashCode()).isEqualTo(_c.a.hashCode());
        assertThat(_c.f.literal("a", false).hashCode()).isEqualTo(_c.na.hashCode());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfAtoms(final FormulaContext _c) {
        assertThat(_c.a.numberOfAtoms(_c.f)).isEqualTo(1);
        assertThat(_c.na.numberOfAtoms(_c.f)).isEqualTo(1);
        assertThat(_c.na.numberOfAtoms(_c.f)).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfNodes(final FormulaContext _c) {
        assertThat(_c.a.numberOfNodes(_c.f)).isEqualTo(1);
        assertThat(_c.na.numberOfNodes(_c.f)).isEqualTo(1);
        assertThat(_c.na.numberOfNodes(_c.f)).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfInternalNodes(final FormulaContext _c) {
        assertThat(_c.a.numberOfInternalNodes()).isEqualTo(1);
        assertThat(_c.na.numberOfInternalNodes()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfOperands(final FormulaContext _c) {
        assertThat(_c.a.numberOfOperands()).isEqualTo(0);
        assertThat(_c.na.numberOfOperands()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsConstantFormula(final FormulaContext _c) {
        assertThat(_c.a.isConstantFormula()).isFalse();
        assertThat(_c.na.isConstantFormula()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAtomicFormula(final FormulaContext _c) {
        assertThat(_c.a.isAtomicFormula()).isTrue();
        assertThat(_c.na.isAtomicFormula()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testContains(final FormulaContext _c) {
        assertThat(_c.a.containsVariable(_c.f.variable("b"))).isFalse();
        assertThat(_c.a.containsVariable(_c.f.variable("a"))).isTrue();
        assertThat(_c.na.containsVariable(_c.f.variable("b"))).isFalse();
        assertThat(_c.na.containsVariable(_c.f.variable("a"))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsNNF(final FormulaContext _c) {
        assertThat(_c.a.isNNF(_c.f)).isTrue();
        assertThat(_c.na.isNNF(_c.f)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsDNF(final FormulaContext _c) {
        assertThat(_c.a.isDNF(_c.f)).isTrue();
        assertThat(_c.na.isDNF(_c.f)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsCNF(final FormulaContext _c) {
        assertThat(_c.a.isCNF(_c.f)).isTrue();
        assertThat(_c.na.isCNF(_c.f)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPosNeg(final FormulaContext _c) {
        assertThat(_c.a.variable() == _c.a).isTrue();
        assertThat(_c.na.variable() == _c.a).isTrue();
    }
}
