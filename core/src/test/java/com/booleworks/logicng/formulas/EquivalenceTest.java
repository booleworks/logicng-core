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

public class EquivalenceTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testType(final FormulaContext _c) {
        assertThat(_c.eq1.getType()).isEqualTo(FType.EQUIV);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCreator(final FormulaContext _c) {
        assertThat(_c.f.equivalence(_c.verum, _c.and1)).isEqualTo(_c.and1);
        assertThat(_c.f.equivalence(_c.and1, _c.verum)).isEqualTo(_c.and1);
        assertThat(_c.f.equivalence(_c.falsum, _c.and1)).isEqualTo(_c.not1);
        assertThat(_c.f.equivalence(_c.and1, _c.falsum)).isEqualTo(_c.not1);
        assertThat(_c.f.equivalence(_c.or1, _c.or1)).isEqualTo(_c.verum);
        assertThat(_c.f.equivalence(_c.not1, _c.and1)).isEqualTo(_c.falsum);
        assertThat(_c.f.equivalence(_c.and1, _c.not1)).isEqualTo(_c.falsum);
        assertThat(_c.f.equivalence(_c.or1, _c.not2)).isEqualTo(_c.falsum);
        assertThat(_c.f.equivalence(_c.not2, _c.or1)).isEqualTo(_c.falsum);
        assertThat(_c.f.binaryOperator(FType.EQUIV, _c.and1, _c.or1)).isEqualTo(_c.eq3);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testGetters(final FormulaContext _c) {
        assertThat(((Equivalence) _c.eq2).getLeft()).isEqualTo(_c.na);
        assertThat(((Equivalence) _c.eq2).getRight()).isEqualTo(_c.nb);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testVariables(final FormulaContext _c) {
        assertThat(_c.imp3.variables(_c.f).size()).isEqualTo(4);
        SortedSet<Variable> lits = new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.x, _c.y));
        assertThat(_c.imp3.variables(_c.f)).isEqualTo(lits);

        final Formula equiv = _c.f.equivalence(_c.and1, _c.and2);
        assertThat(equiv.variables(_c.f).size()).isEqualTo(2);
        lits = new TreeSet<>(Arrays.asList(_c.a, _c.b));
        assertThat(equiv.variables(_c.f)).isEqualTo(lits);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        assertThat(_c.imp3.literals(_c.f).size()).isEqualTo(4);
        SortedSet<Literal> lits = new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.x, _c.y));
        assertThat(_c.imp3.literals(_c.f)).isEqualTo(lits);

        Formula equiv = _c.f.equivalence(_c.and1, _c.and2);
        assertThat(equiv.literals(_c.f).size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.na, _c.nb));
        assertThat(equiv.literals(_c.f)).isEqualTo(lits);

        equiv = _c.f.equivalence(_c.and1, _c.a);
        assertThat(equiv.literals(_c.f).size()).isEqualTo(2);
        lits = new TreeSet<>(Arrays.asList(_c.a, _c.b));
        assertThat(equiv.literals(_c.f)).isEqualTo(lits);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNegation(final FormulaContext _c) {
        assertThat(_c.eq1.negate(_c.f)).isEqualTo(_c.f.not(_c.eq1));
        assertThat(_c.eq2.negate(_c.f)).isEqualTo(_c.f.not(_c.eq2));
        assertThat(_c.eq3.negate(_c.f)).isEqualTo(_c.f.not(_c.eq3));
        assertThat(_c.eq4.negate(_c.f)).isEqualTo(_c.f.not(_c.eq4));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testToString(final FormulaContext _c) {
        assertThat(_c.eq1.toString()).isEqualTo("a <=> b");
        assertThat(_c.eq2.toString()).isEqualTo("~a <=> ~b");
        assertThat(_c.eq3.toString()).isEqualTo("a & b <=> x | y");
        assertThat(_c.eq4.toString()).isEqualTo("a => b <=> ~a => ~b");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquals(final FormulaContext _c) {
        assertThat(_c.f.equivalence(_c.a, _c.b)).isEqualTo(_c.eq1);
        assertThat(_c.f.equivalence(_c.b, _c.a)).isEqualTo(_c.eq1);
        assertThat(_c.f.equivalence(_c.and1, _c.or1)).isEqualTo(_c.eq3);
        assertThat(_c.eq4).isEqualTo(_c.eq4);
        assertThat(_c.eq2).isNotEqualTo(_c.eq1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEqualsDifferentFormulaFactory(final FormulaContext _c) {
        FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder()
                .formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(g.equivalence(g.variable("a"), g.variable("b"))).isEqualTo(_c.eq1);
        assertThat(g.equivalence(_c.b, _c.a)).isEqualTo(_c.eq1);
        assertThat(g.equivalence(_c.and1, _c.or1)).isEqualTo(_c.eq3);
        assertThat(g.equivalence(g.literal("a", false), g.variable("b"))).isNotEqualTo(_c.eq1);
        assertThat(g.equivalence(g.variable("a"), g.literal("b", false))).isNotEqualTo(_c.eq1);

        g = FormulaFactory.nonCaching(FormulaFactoryConfig.builder()
                .formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(g.equivalence(g.variable("a"), g.variable("b"))).isEqualTo(_c.eq1);
        assertThat(g.equivalence(_c.b, _c.a)).isEqualTo(_c.eq1);
        assertThat(g.equivalence(_c.and1, _c.or1)).isEqualTo(_c.eq3);
        assertThat(g.equivalence(g.literal("a", false), g.variable("b"))).isNotEqualTo(_c.eq1);
        assertThat(g.equivalence(g.variable("a"), g.literal("b", false))).isNotEqualTo(_c.eq1);

        g = FormulaFactory.nonCaching(FormulaFactoryConfig.builder()
                .formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.USE_BUT_NO_IMPORT).build());
        assertThat(g.equivalence(g.variable("a"), g.variable("b"))).isEqualTo(_c.eq1);
        assertThat(g.equivalence(_c.b, _c.a)).isEqualTo(_c.eq1);
        assertThat(g.equivalence(_c.and1, _c.or1)).isEqualTo(_c.eq3);
        assertThat(g.equivalence(g.literal("a", false), g.variable("b"))).isNotEqualTo(_c.eq1);
        assertThat(g.equivalence(g.variable("a"), g.literal("b", false))).isNotEqualTo(_c.eq1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testHash(final FormulaContext _c) {
        final Formula eq = _c.f.equivalence(_c.imp1, _c.imp2);
        assertThat(eq.hashCode()).isEqualTo(_c.eq4.hashCode());
        assertThat(eq.hashCode()).isEqualTo(_c.eq4.hashCode());
        assertThat(_c.f.equivalence(_c.and1, _c.or1).hashCode()).isEqualTo(_c.eq3.hashCode());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfAtoms(final FormulaContext _c) {
        assertThat(_c.eq1.numberOfAtoms(_c.f)).isEqualTo(2);
        assertThat(_c.eq4.numberOfAtoms(_c.f)).isEqualTo(4);
        assertThat(_c.eq4.numberOfAtoms(_c.f)).isEqualTo(4);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfNodes(final FormulaContext _c) {
        assertThat(_c.eq1.numberOfNodes(_c.f)).isEqualTo(3);
        assertThat(_c.eq4.numberOfNodes(_c.f)).isEqualTo(7);
        assertThat(_c.eq4.numberOfNodes(_c.f)).isEqualTo(7);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfInternalNodes(final FormulaContext _c) throws ParserException {
        final Formula eq = new PropositionalParser(_c.f).parse("a & (b | c) <=> (d => (b | c))");
        assertThat(_c.eq4.numberOfInternalNodes()).isEqualTo(7);
        assertThat(eq.numberOfInternalNodes()).isEqualTo(8);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfOperands(final FormulaContext _c) {
        assertThat(_c.eq1.numberOfOperands()).isEqualTo(2);
        assertThat(_c.eq3.numberOfOperands()).isEqualTo(2);
        assertThat(_c.eq4.numberOfOperands()).isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsConstantFormula(final FormulaContext _c) {
        assertThat(_c.eq1.isConstantFormula()).isFalse();
        assertThat(_c.eq2.isConstantFormula()).isFalse();
        assertThat(_c.eq3.isConstantFormula()).isFalse();
        assertThat(_c.eq4.isConstantFormula()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAtomicFormula(final FormulaContext _c) {
        assertThat(_c.eq1.isAtomicFormula()).isFalse();
        assertThat(_c.eq4.isAtomicFormula()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testContains(final FormulaContext _c) {
        assertThat(_c.eq4.containsVariable(_c.f.variable("a"))).isTrue();
        assertThat(_c.eq4.containsVariable(_c.f.variable("x"))).isFalse();
        assertThat(_c.eq4.containsNode(_c.imp1)).isTrue();
        assertThat(_c.eq4.containsNode(_c.imp4)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsNNF(final FormulaContext _c) {
        assertThat(_c.eq1.isNnf(_c.f)).isFalse();
        assertThat(_c.eq2.isNnf(_c.f)).isFalse();
        assertThat(_c.eq3.isNnf(_c.f)).isFalse();
        assertThat(_c.eq4.isNnf(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsDNF(final FormulaContext _c) {
        assertThat(_c.eq1.isDnf(_c.f)).isFalse();
        assertThat(_c.eq2.isDnf(_c.f)).isFalse();
        assertThat(_c.eq3.isDnf(_c.f)).isFalse();
        assertThat(_c.eq4.isDnf(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsCNF(final FormulaContext _c) {
        assertThat(_c.eq1.isCnf(_c.f)).isFalse();
        assertThat(_c.eq2.isCnf(_c.f)).isFalse();
        assertThat(_c.eq3.isCnf(_c.f)).isFalse();
        assertThat(_c.eq4.isCnf(_c.f)).isFalse();
    }
}
