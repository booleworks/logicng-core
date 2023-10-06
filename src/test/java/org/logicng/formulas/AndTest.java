// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.logicng.formulas.FormulaFactoryConfig.FormulaMergeStrategy.IMPORT;
import static org.logicng.formulas.FormulaFactoryConfig.FormulaMergeStrategy.USE_BUT_NO_IMPORT;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class AndTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testType(final FormulaContext _c) {
        assertThat(_c.and1.type()).isEqualTo(FType.AND);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCreator(final FormulaContext _c) {
        assertThat(_c.f.and()).isEqualTo(_c.verum);
        assertThat(_c.f.and(_c.verum)).isEqualTo(_c.verum);
        assertThat(_c.f.and(_c.falsum)).isEqualTo(_c.falsum);
        assertThat(_c.f.and(_c.verum, _c.falsum)).isEqualTo(_c.falsum);
        assertThat(_c.f.and(_c.falsum, _c.verum)).isEqualTo(_c.falsum);
        assertThat(_c.f.and(_c.na)).isEqualTo(_c.na);
        assertThat(_c.f.and(_c.a, _c.b, _c.a, _c.b, _c.a)).isEqualTo(_c.and1);
        assertThat(_c.f.and(_c.f.and(_c.a, _c.b), _c.a, _c.f.and(_c.b, _c.a))).isEqualTo(_c.and1);
        assertThat(_c.f.and(_c.verum, _c.a, _c.b, _c.verum)).isEqualTo(_c.and1);
        assertThat(_c.f.and(_c.na, _c.na, _c.na)).isEqualTo(_c.na);
        assertThat(_c.f.and(_c.na, _c.na, _c.verum, _c.verum)).isEqualTo(_c.na);
        assertThat(_c.f.and(_c.na, _c.na, _c.falsum, _c.verum)).isEqualTo(_c.falsum);
        final List<Literal> lits = new ArrayList<>();
        lits.add(_c.a);
        lits.add(_c.b);
        assertThat(_c.f.and(lits)).isEqualTo(_c.and1);
        assertThat(_c.f.and(_c.a, _c.b, _c.x, _c.falsum)).isEqualTo(_c.falsum);
        assertThat(_c.f.and(_c.f.and(_c.a, _c.b), _c.f.and(_c.x, _c.y))).isEqualTo(_c.f.and(_c.a, _c.b, _c.x, _c.y));
        assertThat(_c.f.cnf(_c.f.clause(_c.x, _c.y), _c.f.and(_c.f.or(_c.f.and(_c.nx, _c.nx), _c.ny), _c.f.or(_c.f.and(_c.nx, _c.verum), _c.ny)))).isEqualTo(_c.and3);
        assertThat(_c.f.naryOperator(FType.AND, _c.a, _c.b, _c.a, _c.b, _c.a)).isEqualTo(_c.and1);
        assertThat(_c.f.naryOperator(FType.AND, Arrays.asList(_c.a, _c.b, _c.a, _c.b, _c.a))).isEqualTo(_c.and1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testComplementaryCheck(final FormulaContext _c) {
        assertThat(_c.f.and(_c.a, _c.na)).isEqualTo(_c.falsum);
        assertThat(_c.f.and(_c.a, _c.b, _c.f.and(_c.c, _c.x, _c.nb))).isEqualTo(_c.falsum);
        assertThat(_c.f.and(_c.a, _c.b, _c.f.and(_c.nx, _c.b, _c.x))).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIllegalCreation(final FormulaContext _c) {
        assertThatThrownBy(() -> _c.f.naryOperator(FType.EQUIV, _c.a, _c.b, _c.c)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testVariables(final FormulaContext _c) {
        assertThat(_c.and2.variables(_c.f).size()).isEqualTo(2);
        SortedSet<Variable> lits = new TreeSet<>(Arrays.asList(_c.a, _c.b));
        assertThat(_c.and2.variables(_c.f)).isEqualTo(lits);

        final Formula and = _c.f.and(_c.a, _c.a, _c.b, _c.imp3);
        assertThat(and.variables(_c.f).size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.x, _c.y));
        assertThat(and.variables(_c.f)).isEqualTo(lits);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        assertThat(_c.and2.literals(_c.f).size()).isEqualTo(2);
        SortedSet<Literal> lits = new TreeSet<>(Arrays.asList(_c.na, _c.nb));
        assertThat(_c.and2.literals(_c.f)).isEqualTo(lits);

        final Formula and = _c.f.and(_c.a, _c.a, _c.b, _c.f.implication(_c.na, _c.nb));
        assertThat(and.literals(_c.f).size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(_c.a, _c.na, _c.b, _c.nb));
        assertThat(and.literals(_c.f)).isEqualTo(lits);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testToString(final FormulaContext _c) {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(IMPORT).build());
        assertThat(_c.and1.toString()).isEqualTo("a & b");
        assertThat(_c.and2.toString()).isEqualTo("~a & ~b");
        assertThat(_c.and3.toString()).isEqualTo("(x | y) & (~x | ~y)");
        assertThat(f.and(_c.a, _c.b, _c.nx, _c.ny).toString()).isEqualTo("a & b & ~x & ~y");
        assertThat(f.and(_c.imp1, _c.imp2).toString()).isEqualTo("(a => b) & (~a => ~b)");
        assertThat(f.and(_c.eq1, _c.eq2).toString()).isEqualTo("(a <=> b) & (~a <=> ~b)");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquals(final FormulaContext _c) {
        assertThat(_c.f.and(_c.a, _c.b)).isEqualTo(_c.and1);
        assertThat(_c.f.and(_c.or1, _c.or2)).isEqualTo(_c.and3);
        assertThat(_c.and2).isEqualTo(_c.and2);
        assertThat(_c.f.and(_c.f.or(_c.f.literal("y", false), _c.f.variable("x")), _c.f.or(_c.f.variable("b"), _c.f.variable("a"))))
                .isEqualTo(_c.f.and(_c.f.or(_c.f.variable("a"), _c.f.variable("b")), _c.f.or(_c.f.variable("x"), _c.f.literal("y", false))));
        assertThat(_c.f.and(_c.nx, _c.a, _c.nb, _c.or1)).isEqualTo(_c.f.and(_c.a, _c.nb, _c.or1, _c.nx));
        assertThat(_c.and2).isNotEqualTo(_c.and1);
        assertThat(_c.f.and(_c.a, _c.b, _c.c)).isNotEqualTo(_c.and1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEqualsDifferentFormulaFactory(final FormulaContext _c) {
        FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(IMPORT).build());
        assertThat(g.and(g.variable("a"), g.variable("b"))).isEqualTo(_c.and1);
        assertThat(g.and(_c.or1, _c.or2)).isEqualTo(_c.and3);
        assertThat(g.and(g.or(g.literal("y", false), g.variable("x")), g.or(g.variable("b"), g.variable("a"))))
                .isEqualTo(_c.f.and(_c.f.or(_c.f.variable("a"), _c.f.variable("b")), _c.f.or(_c.f.variable("x"), _c.f.literal("y", false))));
        assertThat(g.and(g.literal("x", false), g.variable("a"), g.literal("b", false), g.or(g.variable("x"), g.variable("y"))))
                .isEqualTo(_c.f.and(_c.a, _c.nb, _c.or1, _c.nx));
        assertThat(g.and(g.literal("a", false), g.variable("b"))).isNotEqualTo(_c.and1);
        assertThat(g.and(g.variable("a"), g.literal("b", false))).isNotEqualTo(_c.and1);
        assertThat(_c.f.and(_c.a, _c.b, _c.f.variable("c"))).isNotEqualTo(_c.and1);

        g = FormulaFactory.nonCaching(FormulaFactoryConfig.builder().formulaMergeStrategy(IMPORT).build());
        assertThat(g.and(g.variable("a"), g.variable("b"))).isEqualTo(_c.and1);
        assertThat(g.and(_c.or1, _c.or2)).isEqualTo(_c.and3);
        assertThat(g.and(g.or(g.literal("y", false), g.variable("x")), g.or(g.variable("b"), g.variable("a"))))
                .isEqualTo(_c.f.and(_c.f.or(_c.f.variable("a"), _c.f.variable("b")), _c.f.or(_c.f.variable("x"), _c.f.literal("y", false))));
        assertThat(g.and(g.literal("x", false), g.variable("a"), g.literal("b", false), g.or(g.variable("x"), g.variable("y"))))
                .isEqualTo(_c.f.and(_c.a, _c.nb, _c.or1, _c.nx));
        assertThat(g.and(g.literal("a", false), g.variable("b"))).isNotEqualTo(_c.and1);
        assertThat(g.and(g.variable("a"), g.literal("b", false))).isNotEqualTo(_c.and1);
        assertThat(_c.f.and(_c.a, _c.b, _c.f.variable("c"))).isNotEqualTo(_c.and1);

        g = FormulaFactory.nonCaching(FormulaFactoryConfig.builder().formulaMergeStrategy(USE_BUT_NO_IMPORT).build());
        assertThat(g.and(g.variable("a"), g.variable("b"))).isEqualTo(_c.and1);
        assertThat(g.and(_c.or1, _c.or2)).isEqualTo(_c.and3);
        assertThat(g.and(g.or(g.literal("y", false), g.variable("x")), g.or(g.variable("b"), g.variable("a"))))
                .isEqualTo(_c.f.and(_c.f.or(_c.f.variable("a"), _c.f.variable("b")), _c.f.or(_c.f.variable("x"), _c.f.literal("y", false))));
        assertThat(g.and(g.literal("x", false), g.variable("a"), g.literal("b", false), g.or(g.variable("x"), g.variable("y"))))
                .isEqualTo(_c.f.and(_c.a, _c.nb, _c.or1, _c.nx));
        assertThat(g.and(g.literal("a", false), g.variable("b"))).isNotEqualTo(_c.and1);
        assertThat(g.and(g.variable("a"), g.literal("b", false))).isNotEqualTo(_c.and1);
        assertThat(_c.f.and(_c.a, _c.b, _c.f.variable("c"))).isNotEqualTo(_c.and1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testHash(final FormulaContext _c) {
        final Formula and = _c.f.and(_c.or1, _c.or2);
        assertThat(and.hashCode()).isEqualTo(_c.and3.hashCode());
        assertThat(and.hashCode()).isEqualTo(_c.and3.hashCode());
        assertThat(_c.f.and(_c.na, _c.nb).hashCode()).isEqualTo(_c.and2.hashCode());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfAtoms(final FormulaContext _c) {
        assertThat(_c.and1.numberOfAtoms(_c.f)).isEqualTo(2);
        assertThat(_c.and2.numberOfAtoms(_c.f)).isEqualTo(2);
        assertThat(_c.and3.numberOfAtoms(_c.f)).isEqualTo(4);
        assertThat(_c.and3.numberOfAtoms(_c.f)).isEqualTo(4);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfNodes(final FormulaContext _c) {
        assertThat(_c.and1.numberOfNodes(_c.f)).isEqualTo(3);
        assertThat(_c.and2.numberOfNodes(_c.f)).isEqualTo(3);
        assertThat(_c.and3.numberOfNodes(_c.f)).isEqualTo(7);
        assertThat(_c.and3.numberOfNodes(_c.f)).isEqualTo(7);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfInternalNodes(final FormulaContext _c) throws ParserException {
        final Formula and = new PropositionalParser(_c.f).parse("a & (b | c) => (d <=> (b | c))");
        assertThat(_c.and3.numberOfInternalNodes()).isEqualTo(7);
        assertThat(and.numberOfInternalNodes()).isEqualTo(8);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfOperands(final FormulaContext _c) {
        assertThat(_c.and1.numberOfOperands()).isEqualTo(2);
        assertThat(_c.and3.numberOfOperands()).isEqualTo(2);
        assertThat(_c.f.and(_c.a, _c.nx, _c.eq1).numberOfOperands()).isEqualTo(3);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsConstantFormula(final FormulaContext _c) {
        assertThat(_c.and1.isConstantFormula()).isFalse();
        assertThat(_c.and2.isConstantFormula()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAtomicFormula(final FormulaContext _c) {
        assertThat(_c.and1.isAtomicFormula()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testContains(final FormulaContext _c) throws ParserException {
        assertThat(_c.and3.containsVariable(_c.f.variable("x"))).isTrue();
        assertThat(_c.and3.containsVariable(_c.f.variable("a"))).isFalse();
        final PropositionalParser parser = new PropositionalParser(_c.f);
        final Formula contAnd = parser.parse("a & b & (c | (d & e))");
        assertThat(contAnd.containsNode(parser.parse("d & e"))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsNNF(final FormulaContext _c) {
        assertThat(_c.and1.isNNF(_c.f)).isTrue();
        assertThat(_c.and2.isNNF(_c.f)).isTrue();
        assertThat(_c.and3.isNNF(_c.f)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsDNF(final FormulaContext _c) {
        assertThat(_c.and1.isDNF(_c.f)).isTrue();
        assertThat(_c.and2.isDNF(_c.f)).isTrue();
        assertThat(_c.and3.isDNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsCNF(final FormulaContext _c) {
        assertThat(_c.and1.isCNF(_c.f)).isTrue();
        assertThat(_c.and2.isCNF(_c.f)).isTrue();
        assertThat(_c.and3.isCNF(_c.f)).isTrue();
    }
}
