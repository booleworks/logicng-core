// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

public class NotTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testType(final FormulaContext _c) {
        assertThat(_c.not1.getType()).isEqualTo(FType.NOT);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCreator(final FormulaContext _c) {
        assertThat(_c.f.not(_c.falsum)).isEqualTo(_c.verum);
        assertThat(_c.f.not(_c.verum)).isEqualTo(_c.falsum);
        assertThat(_c.f.not(_c.na)).isEqualTo(_c.a);
        assertThat(_c.f.not(_c.a)).isEqualTo(_c.na);
        assertThat(_c.f.not(_c.f.not(_c.imp3))).isEqualTo(_c.imp3);
        assertThat(_c.f.not(_c.and1)).isEqualTo(_c.not1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testGetters(final FormulaContext _c) {
        assertThat(((Not) _c.not1).getOperand()).isEqualTo(_c.and1);
        assertThat(((Not) _c.not2).getOperand()).isEqualTo(_c.or1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testVariables(final FormulaContext _c) {
        assertThat(_c.not1.variables(_c.f).size()).isEqualTo(2);
        SortedSet<Variable> lits = new TreeSet<>(Arrays.asList(_c.a, _c.b));
        assertThat(_c.not1.variables(_c.f)).isEqualTo(lits);

        assertThat(_c.not2.variables(_c.f).size()).isEqualTo(2);
        lits = new TreeSet<>(Arrays.asList(_c.x, _c.y));
        assertThat(_c.not2.variables(_c.f)).isEqualTo(lits);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        assertThat(_c.not1.literals(_c.f).size()).isEqualTo(2);
        SortedSet<? extends Literal> lits = new TreeSet<>(Arrays.asList(_c.a, _c.b));
        assertThat(_c.not1.literals(_c.f)).isEqualTo(lits);

        final Formula not = _c.f.not(_c.f.and(_c.a, _c.nb, _c.f.implication(_c.b, _c.na)));
        assertThat(not.literals(_c.f).size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(_c.a, _c.na, _c.b, _c.nb));
        assertThat(not.literals(_c.f)).isEqualTo(lits);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testToString(final FormulaContext _c) {
        assertThat(_c.not1.toString()).isEqualTo("~(a & b)");
        assertThat(_c.not2.toString()).isEqualTo("~(x | y)");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquals(final FormulaContext _c) {
        assertThat(_c.f.not(_c.and1)).isEqualTo(_c.not1);
        assertThat(_c.f.not(_c.or1)).isEqualTo(_c.not2);
        assertThat(_c.not1).isEqualTo(_c.not1);
        assertThat(_c.not2).isNotEqualTo(_c.not1);
        assertThat(_c.not2).isNotEqualTo("String");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEqualsDifferentFormulaFactory(final FormulaContext _c) {
        FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder()
                .formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(g.not(_c.and1)).isEqualTo(_c.not1);
        assertThat(g.not(g.or(g.variable("x"), g.variable("y")))).isEqualTo(_c.not2);
        assertThat(g.not(g.or(g.variable("a"), g.variable("b")))).isNotEqualTo(_c.not2);

        g = FormulaFactory.nonCaching(FormulaFactoryConfig.builder()
                .formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(g.not(_c.and1)).isEqualTo(_c.not1);
        assertThat(g.not(g.or(g.variable("x"), g.variable("y")))).isEqualTo(_c.not2);
        assertThat(g.not(g.or(g.variable("a"), g.variable("b")))).isNotEqualTo(_c.not2);

        g = FormulaFactory.nonCaching(FormulaFactoryConfig.builder()
                .formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.USE_BUT_NO_IMPORT).build());
        assertThat(g.not(_c.and1)).isEqualTo(_c.not1);
        assertThat(g.not(g.or(g.variable("x"), g.variable("y")))).isEqualTo(_c.not2);
        assertThat(g.not(g.or(g.variable("a"), g.variable("b")))).isNotEqualTo(_c.not2);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testHash(final FormulaContext _c) {
        final Formula not = _c.f.not(_c.and1);
        assertThat(not.hashCode()).isEqualTo(_c.not1.hashCode());
        assertThat(not.hashCode()).isEqualTo(_c.not1.hashCode());
        assertThat(_c.f.not(_c.or1).hashCode()).isEqualTo(_c.not2.hashCode());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfAtoms(final FormulaContext _c) {
        assertThat(_c.not1.numberOfAtoms(_c.f)).isEqualTo(2);
        assertThat(_c.not1.numberOfAtoms(_c.f)).isEqualTo(2);
        assertThat(_c.not2.numberOfAtoms(_c.f)).isEqualTo(2);
        assertThat(_c.or1.numberOfAtoms(_c.f)).isEqualTo(2);
        assertThat(_c.or1.numberOfAtoms(_c.f)).isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfNodes(final FormulaContext _c) {
        assertThat(_c.not1.numberOfNodes(_c.f)).isEqualTo(4);
        assertThat(_c.not2.numberOfNodes(_c.f)).isEqualTo(4);
        assertThat(_c.not2.numberOfNodes(_c.f)).isEqualTo(4);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfInternalNodes(final FormulaContext _c) throws ParserException {
        final Formula eq = new PropositionalParser(_c.f).parse("a & (b | c) <=> ~(d => (b | c))");
        assertThat(_c.not1.numberOfInternalNodes()).isEqualTo(4);
        assertThat(eq.numberOfInternalNodes()).isEqualTo(9);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfOperands(final FormulaContext _c) {
        assertThat(_c.not1.numberOfOperands()).isEqualTo(1);
        assertThat(_c.f.not(_c.eq1).numberOfOperands()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsConstantFormula(final FormulaContext _c) {
        assertThat(_c.not1.isConstantFormula()).isFalse();
        assertThat(_c.not2.isConstantFormula()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAtomicFormula(final FormulaContext _c) {
        assertThat(_c.not1.isAtomicFormula()).isFalse();
        assertThat(_c.not2.isAtomicFormula()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testContains(final FormulaContext _c) {
        assertThat(_c.not1.containsVariable(_c.f.variable("a"))).isTrue();
        assertThat(_c.not1.containsVariable(_c.f.variable("x"))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsNNF(final FormulaContext _c) {
        assertThat(_c.not1.isNNF(_c.f)).isFalse();
        assertThat(_c.not2.isNNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsDNF(final FormulaContext _c) {
        assertThat(_c.not1.isDNF(_c.f)).isFalse();
        assertThat(_c.not2.isDNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsCNF(final FormulaContext _c) {
        assertThat(_c.not1.isCNF(_c.f)).isFalse();
        assertThat(_c.not2.isCNF(_c.f)).isFalse();
    }
}
