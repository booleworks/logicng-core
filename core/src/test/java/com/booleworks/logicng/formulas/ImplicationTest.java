// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

public class ImplicationTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testType(final FormulaContext _c) {
        assertThat(_c.imp1.getType()).isEqualTo(FType.IMPL);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCreator(final FormulaContext _c) {
        assertThat(_c.f.implication(_c.falsum, _c.a)).isEqualTo(_c.verum);
        assertThat(_c.f.implication(_c.a, _c.verum)).isEqualTo(_c.verum);
        assertThat(_c.f.implication(_c.verum, _c.a)).isEqualTo(_c.a);
        assertThat(_c.f.implication(_c.a, _c.falsum)).isEqualTo(_c.na);
        assertThat(_c.f.implication(_c.a, _c.a)).isEqualTo(_c.verum);
        assertThat(_c.f.implication(_c.a, _c.na)).isEqualTo(_c.na);
        assertThat(_c.f.implication(_c.na, _c.a)).isEqualTo(_c.a);
        assertThat(_c.f.implication(_c.imp4, _c.imp4.negate(_c.f))).isEqualTo(_c.imp4.negate(_c.f));
        assertThat(_c.f.implication(_c.imp4.negate(_c.f), _c.imp4)).isEqualTo(_c.imp4);
        assertThat(_c.f.binaryOperator(FType.IMPL, _c.and1, _c.or1)).isEqualTo(_c.imp3);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIllegalCreation(final FormulaContext _c) {
        assertThatThrownBy(() -> _c.f.binaryOperator(FType.NOT, _c.and1, _c.or1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testGetters(final FormulaContext _c) {
        assertThat(((Implication) _c.imp2).getLeft()).isEqualTo(_c.na);
        assertThat(((Implication) _c.imp2).getRight()).isEqualTo(_c.nb);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testVariables(final FormulaContext _c) {
        assertThat(_c.imp3.variables(_c.f).size()).isEqualTo(4);
        SortedSet<Variable> lits = new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.x, _c.y));
        assertThat(_c.imp3.variables(_c.f)).isEqualTo(lits);

        final Formula imp = _c.f.implication(_c.and1, _c.and2);
        assertThat(imp.variables(_c.f).size()).isEqualTo(2);
        lits = new TreeSet<>(Arrays.asList(_c.a, _c.b));
        assertThat(imp.variables(_c.f)).isEqualTo(lits);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        assertThat(_c.imp3.literals(_c.f).size()).isEqualTo(4);
        SortedSet<Literal> lits = new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.x, _c.y));
        assertThat(_c.imp3.literals(_c.f)).isEqualTo(lits);

        Formula imp = _c.f.implication(_c.and1, _c.and2);
        assertThat(imp.literals(_c.f).size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.na, _c.nb));
        assertThat(imp.literals(_c.f)).isEqualTo(lits);

        imp = _c.f.implication(_c.and1, _c.a);
        assertThat(imp.literals(_c.f).size()).isEqualTo(2);
        lits = new TreeSet<>(Arrays.asList(_c.a, _c.b));
        assertThat(imp.literals(_c.f)).isEqualTo(lits);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNegation(final FormulaContext _c) {
        assertThat(_c.imp1.negate(_c.f)).isEqualTo(_c.f.not(_c.imp1));
        assertThat(_c.imp2.negate(_c.f)).isEqualTo(_c.f.not(_c.imp2));
        assertThat(_c.imp3.negate(_c.f)).isEqualTo(_c.f.not(_c.imp3));
        assertThat(_c.imp4.negate(_c.f)).isEqualTo(_c.f.not(_c.imp4));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testToString(final FormulaContext _c) {
        assertThat(_c.imp1.toString()).isEqualTo("a => b");
        assertThat(_c.imp2.toString()).isEqualTo("~a => ~b");
        assertThat(_c.imp3.toString()).isEqualTo("a & b => x | y");
        assertThat(_c.imp4.toString()).isEqualTo("(a <=> b) => (~x <=> ~y)");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquals(final FormulaContext _c) {
        assertThat(_c.f.implication(_c.a, _c.b)).isEqualTo(_c.imp1);
        assertThat(_c.f.implication(_c.and1, _c.or1)).isEqualTo(_c.imp3);
        assertThat(_c.imp2).isEqualTo(_c.imp2);
        assertThat(_c.imp2).isNotEqualTo(_c.imp1);
        assertThat(_c.imp2).isNotEqualTo("String");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEqualsDifferentFormulaFactory(final FormulaContext _c) {
        FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder()
                .formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(g.implication(g.variable("a"), g.variable("b"))).isEqualTo(_c.imp1);
        assertThat(g.implication(_c.and1, _c.or1)).isEqualTo(_c.imp3);
        assertThat(g.implication(g.variable("b"), g.variable("a"))).isNotEqualTo(_c.imp1);
        assertThat(g.implication(g.literal("a", false), g.variable("b"))).isNotEqualTo(_c.imp1);
        assertThat(g.implication(g.variable("a"), g.literal("b", false))).isNotEqualTo(_c.imp1);

        g = FormulaFactory.nonCaching(FormulaFactoryConfig.builder()
                .formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(g.implication(g.variable("a"), g.variable("b"))).isEqualTo(_c.imp1);
        assertThat(g.implication(_c.and1, _c.or1)).isEqualTo(_c.imp3);
        assertThat(g.implication(g.variable("b"), g.variable("a"))).isNotEqualTo(_c.imp1);
        assertThat(g.implication(g.literal("a", false), g.variable("b"))).isNotEqualTo(_c.imp1);
        assertThat(g.implication(g.variable("a"), g.literal("b", false))).isNotEqualTo(_c.imp1);

        g = FormulaFactory.nonCaching(FormulaFactoryConfig.builder()
                .formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.USE_BUT_NO_IMPORT).build());
        assertThat(g.implication(g.variable("a"), g.variable("b"))).isEqualTo(_c.imp1);
        assertThat(g.implication(_c.and1, _c.or1)).isEqualTo(_c.imp3);
        assertThat(g.implication(g.variable("b"), g.variable("a"))).isNotEqualTo(_c.imp1);
        assertThat(g.implication(g.literal("a", false), g.variable("b"))).isNotEqualTo(_c.imp1);
        assertThat(g.implication(g.variable("a"), g.literal("b", false))).isNotEqualTo(_c.imp1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testHash(final FormulaContext _c) {
        final Formula imp = _c.f.implication(_c.na, _c.nb);
        assertThat(imp.hashCode()).isEqualTo(_c.imp2.hashCode());
        assertThat(imp.hashCode()).isEqualTo(_c.imp2.hashCode());
        assertThat(_c.f.implication(_c.and1, _c.or1).hashCode()).isEqualTo(_c.imp3.hashCode());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfAtoms(final FormulaContext _c) {
        assertThat(_c.imp1.numberOfAtoms(_c.f)).isEqualTo(2);
        assertThat(_c.imp3.numberOfAtoms(_c.f)).isEqualTo(4);
        assertThat(_c.imp3.numberOfAtoms(_c.f)).isEqualTo(4);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfNodes(final FormulaContext _c) {
        assertThat(_c.imp1.numberOfNodes(_c.f)).isEqualTo(3);
        assertThat(_c.imp4.numberOfNodes(_c.f)).isEqualTo(7);
        assertThat(_c.imp4.numberOfNodes(_c.f)).isEqualTo(7);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfInternalNodes(final FormulaContext _c) throws ParserException {
        final Formula imp = new PropositionalParser(_c.f).parse("a & (b | c) => (d <=> (b | c))");
        assertThat(_c.imp4.numberOfInternalNodes()).isEqualTo(7);
        assertThat(imp.numberOfInternalNodes()).isEqualTo(8);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfOperands(final FormulaContext _c) {
        assertThat(_c.imp1.numberOfOperands()).isEqualTo(2);
        assertThat(_c.imp3.numberOfOperands()).isEqualTo(2);
        assertThat(_c.imp4.numberOfOperands()).isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsConstantFormula(final FormulaContext _c) {
        assertThat(_c.imp1.isConstantFormula()).isFalse();
        assertThat(_c.imp2.isConstantFormula()).isFalse();
        assertThat(_c.imp3.isConstantFormula()).isFalse();
        assertThat(_c.imp4.isConstantFormula()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAtomicFormula(final FormulaContext _c) {
        assertThat(_c.imp1.isAtomicFormula()).isFalse();
        assertThat(_c.imp4.isAtomicFormula()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testContains(final FormulaContext _c) {
        assertThat(_c.imp4.containsVariable(_c.f.variable("a"))).isTrue();
        assertThat(_c.imp4.containsVariable(_c.f.variable("c"))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsNNF(final FormulaContext _c) {
        assertThat(_c.imp1.isNNF(_c.f)).isFalse();
        assertThat(_c.imp2.isNNF(_c.f)).isFalse();
        assertThat(_c.imp3.isNNF(_c.f)).isFalse();
        assertThat(_c.imp4.isNNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsDNF(final FormulaContext _c) {
        assertThat(_c.imp1.isDNF(_c.f)).isFalse();
        assertThat(_c.imp2.isDNF(_c.f)).isFalse();
        assertThat(_c.imp3.isDNF(_c.f)).isFalse();
        assertThat(_c.imp4.isDNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsCNF(final FormulaContext _c) {
        assertThat(_c.imp1.isCNF(_c.f)).isFalse();
        assertThat(_c.imp2.isCNF(_c.f)).isFalse();
        assertThat(_c.imp3.isCNF(_c.f)).isFalse();
        assertThat(_c.imp4.isCNF(_c.f)).isFalse();
    }
}
