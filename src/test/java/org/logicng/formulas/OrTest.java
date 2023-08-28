// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.logicng.formulas.FormulaFactoryConfig.FormulaMergeStrategy.IMPORT;
import static org.logicng.formulas.FormulaFactoryConfig.FormulaMergeStrategy.USE_BUT_NO_IMPORT;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class OrTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testType(final FormulaContext _c) {
        assertThat(_c.or1.type()).isEqualTo(FType.OR);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCreator(final FormulaContext _c) {
        assertThat(_c.f.or()).isEqualTo(_c.falsum);
        assertThat(_c.f.or(_c.verum)).isEqualTo(_c.verum);
        assertThat(_c.f.or(_c.falsum)).isEqualTo(_c.falsum);
        assertThat(_c.f.or(_c.verum, _c.falsum)).isEqualTo(_c.verum);
        assertThat(_c.f.or(_c.falsum, _c.verum)).isEqualTo(_c.verum);
        assertThat(_c.f.or(_c.na)).isEqualTo(_c.na);
        assertThat(_c.f.or(_c.x, _c.y, _c.x, _c.y, _c.x)).isEqualTo(_c.or1);
        assertThat(_c.f.or(_c.f.or(_c.x, _c.y), _c.x, _c.f.or(_c.x, _c.y))).isEqualTo(_c.or1);
        assertThat(_c.f.or(_c.falsum, _c.x, _c.y, _c.falsum)).isEqualTo(_c.or1);
        assertThat(_c.f.or(_c.na, _c.na, _c.na)).isEqualTo(_c.na);
        assertThat(_c.f.or(_c.na, _c.na, _c.falsum, _c.falsum)).isEqualTo(_c.na);
        assertThat(_c.f.or(_c.na, _c.na, _c.verum, _c.falsum)).isEqualTo(_c.verum);
        final List<Literal> lits = Arrays.asList(_c.x, _c.y);
        assertThat(_c.f.or(lits)).isEqualTo(_c.or1);
        assertThat(_c.f.or(_c.a, _c.b, _c.x, _c.verum)).isEqualTo(_c.verum);
        assertThat(_c.f.or(_c.f.or(_c.a, _c.b), _c.f.or(_c.x, _c.y))).isEqualTo(_c.f.or(_c.a, _c.b, _c.x, _c.y));
        assertThat(_c.f.or(_c.f.and(_c.a, _c.b), _c.f.or(_c.f.and(_c.f.and(_c.na, _c.nb)), _c.f.and(_c.f.or(_c.na, _c.falsum), _c.nb)))).isEqualTo(_c.or3);
        assertThat(_c.f.naryOperator(FType.OR, Arrays.asList(_c.x, _c.y, _c.x, _c.y, _c.x))).isEqualTo(_c.or1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testComplementaryCheck(final FormulaContext _c) {
        assertThat(_c.f.or(_c.a, _c.na)).isEqualTo(_c.verum);
        assertThat(_c.f.or(_c.a, _c.b, _c.f.or(_c.c, _c.x, _c.nb))).isEqualTo(_c.verum);
        assertThat(_c.f.or(_c.a, _c.b, _c.f.or(_c.nx, _c.b, _c.x))).isEqualTo(_c.verum);
        assertThat(_c.f.or(_c.x, _c.y, _c.f.and(_c.nx, _c.b, _c.x))).isEqualTo(_c.or1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testVariables(final FormulaContext _c) {
        assertThat(_c.or2.variables().size()).isEqualTo(2);
        SortedSet<Variable> lits = new TreeSet<>(Arrays.asList(_c.x, _c.y));
        assertThat(_c.or2.variables()).isEqualTo(lits);

        final Formula or = _c.f.or(_c.a, _c.a, _c.b, _c.imp3);
        assertThat(or.variables().size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.x, _c.y));
        assertThat(or.variables()).isEqualTo(lits);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        assertThat(_c.or2.literals(_c.f).size()).isEqualTo(2);
        SortedSet<Literal> lits = new TreeSet<>(Arrays.asList(_c.nx, _c.ny));
        assertThat(_c.or2.literals(_c.f)).isEqualTo(lits);

        final Formula or = _c.f.or(_c.a, _c.a, _c.b, _c.f.implication(_c.nb, _c.na));
        assertThat(or.literals(_c.f).size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(_c.a, _c.na, _c.b, _c.nb));
        assertThat(or.literals(_c.f)).isEqualTo(lits);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testToString(final FormulaContext _c) {
        final FormulaFactory f =
                FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(IMPORT).build());
        assertThat(_c.or1.toString()).isEqualTo("x | y");
        assertThat(_c.or2.toString()).isEqualTo("~x | ~y");
        assertThat(_c.or3.toString()).isEqualTo("a & b | ~a & ~b");
        assertThat(_c.f.or(_c.a, _c.b, _c.nx, _c.ny).toString()).isEqualTo("a | b | ~x | ~y");
        assertThat(_c.f.or(_c.imp1, _c.imp2).toString()).isEqualTo("(a => b) | (~a => ~b)");
        assertThat(_c.f.or(_c.eq1, _c.eq2).toString()).isEqualTo("(a <=> b) | (~a <=> ~b)");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquals(final FormulaContext _c) {
        assertThat(_c.f.or(_c.x, _c.y)).isEqualTo(_c.or1);
        assertThat(_c.f.or(_c.and1, _c.and2)).isEqualTo(_c.or3);
        assertThat(_c.or2).isEqualTo(_c.or2);
        assertThat(_c.f.or(_c.nx, _c.a, _c.nb, _c.and1)).isEqualTo(_c.f.or(_c.a, _c.nb, _c.and1, _c.nx));
        assertThat(_c.or2).isNotEqualTo(_c.or1);
        assertThat(_c.f.or(_c.a, _c.b, _c.c)).isNotEqualTo(_c.or1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEqualsDifferentFormulaFactory(final FormulaContext _c) {
        FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(IMPORT).build());
        assertThat(g.or(g.variable("x"), g.variable("y"))).isEqualTo(_c.or1);
        assertThat(g.or(_c.and1, _c.and2)).isEqualTo(_c.or3);
        assertThat(g.or(g.and(g.literal("y", false), g.variable("x")), g.and(g.variable("b"), g.variable("a")))).isEqualTo(_c.f.or(_c.f.and(_c.f.variable("a"), _c.f.variable("b")), _c.f.and(_c.f.variable("x"), _c.f.literal("y", false))));
        assertThat(g.or(g.literal("x", false), g.variable("a"), g.literal("b", false), g.and(g.variable("a"), g.variable("b")))).isEqualTo(_c.f.or(_c.a, _c.nb, _c.and1, _c.nx));
        assertThat(g.or(g.literal("a", false), g.variable("b"))).isNotEqualTo(_c.or1);
        assertThat(g.or(g.variable("a"), g.literal("b", false))).isNotEqualTo(_c.or1);
        assertThat(_c.f.or(_c.a, _c.b, _c.f.variable("c"))).isNotEqualTo(_c.or1);

        g = FormulaFactory.nonCaching(FormulaFactoryConfig.builder().formulaMergeStrategy(IMPORT).build());
        assertThat(g.or(g.variable("x"), g.variable("y"))).isEqualTo(_c.or1);
        assertThat(g.or(_c.and1, _c.and2)).isEqualTo(_c.or3);
        assertThat(g.or(g.and(g.literal("y", false), g.variable("x")), g.and(g.variable("b"), g.variable("a")))).isEqualTo(_c.f.or(_c.f.and(_c.f.variable("a"), _c.f.variable("b")), _c.f.and(_c.f.variable("x"), _c.f.literal("y", false))));
        assertThat(g.or(g.literal("x", false), g.variable("a"), g.literal("b", false), g.and(g.variable("a"), g.variable("b")))).isEqualTo(_c.f.or(_c.a, _c.nb, _c.and1, _c.nx));
        assertThat(g.or(g.literal("a", false), g.variable("b"))).isNotEqualTo(_c.or1);
        assertThat(g.or(g.variable("a"), g.literal("b", false))).isNotEqualTo(_c.or1);
        assertThat(_c.f.or(_c.a, _c.b, _c.f.variable("c"))).isNotEqualTo(_c.or1);

        g = FormulaFactory.nonCaching(FormulaFactoryConfig.builder().formulaMergeStrategy(USE_BUT_NO_IMPORT).build());
        assertThat(g.or(g.variable("x"), g.variable("y"))).isEqualTo(_c.or1);
        assertThat(g.or(_c.and1, _c.and2)).isEqualTo(_c.or3);
        assertThat(g.or(g.and(g.literal("y", false), g.variable("x")), g.and(g.variable("b"), g.variable("a")))).isEqualTo(_c.f.or(_c.f.and(_c.f.variable("a"), _c.f.variable("b")), _c.f.and(_c.f.variable("x"), _c.f.literal("y", false))));
        assertThat(g.or(g.literal("x", false), g.variable("a"), g.literal("b", false), g.and(g.variable("a"), g.variable("b")))).isEqualTo(_c.f.or(_c.a, _c.nb, _c.and1, _c.nx));
        assertThat(g.or(g.literal("a", false), g.variable("b"))).isNotEqualTo(_c.or1);
        assertThat(g.or(g.variable("a"), g.literal("b", false))).isNotEqualTo(_c.or1);
        assertThat(_c.f.or(_c.a, _c.b, _c.f.variable("c"))).isNotEqualTo(_c.or1);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testHash(final FormulaContext _c) {
        final Formula or = _c.f.or(_c.and1, _c.and2);
        assertThat(or.hashCode()).isEqualTo(_c.or3.hashCode());
        assertThat(or.hashCode()).isEqualTo(_c.or3.hashCode());
        assertThat(_c.f.or(_c.nx, _c.ny).hashCode()).isEqualTo(_c.or2.hashCode());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfAtoms(final FormulaContext _c) {
        assertThat(_c.or1.numberOfAtoms(_c.f)).isEqualTo(2);
        assertThat(_c.or2.numberOfAtoms(_c.f)).isEqualTo(2);
        assertThat(_c.or3.numberOfAtoms(_c.f)).isEqualTo(4);
        assertThat(_c.or3.numberOfAtoms(_c.f)).isEqualTo(4);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfNodes(final FormulaContext _c) {
        assertThat(_c.or1.numberOfNodes(_c.f)).isEqualTo(3);
        assertThat(_c.or2.numberOfNodes(_c.f)).isEqualTo(3);
        assertThat(_c.or3.numberOfNodes(_c.f)).isEqualTo(7);
        assertThat(_c.or3.numberOfNodes(_c.f)).isEqualTo(7);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfInternalNodes(final FormulaContext _c) throws ParserException {
        final Formula or = new PropositionalParser(_c.f).parse("a & (b | c) => (d <=> (b | c))");
        assertThat(_c.or3.numberOfInternalNodes()).isEqualTo(7);
        assertThat(or.numberOfInternalNodes()).isEqualTo(8);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNumberOfOperands(final FormulaContext _c) {
        assertThat(_c.or1.numberOfOperands()).isEqualTo(2);
        assertThat(_c.or3.numberOfOperands()).isEqualTo(2);
        assertThat(_c.f.or(_c.a, _c.nx, _c.eq1).numberOfOperands()).isEqualTo(3);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsConstantFormula(final FormulaContext _c) {
        assertThat(_c.or1.isConstantFormula()).isFalse();
        assertThat(_c.or2.isConstantFormula()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAtomicFormula(final FormulaContext _c) {
        assertThat(_c.or1.isAtomicFormula()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testContains(final FormulaContext _c) throws ParserException {
        assertThat(_c.or1.containsVariable(_c.f.variable("x"))).isTrue();
        assertThat(_c.or1.containsVariable(_c.f.variable("a"))).isFalse();
        final PropositionalParser parser = new PropositionalParser(_c.f);
        final Formula contAnd = parser.parse("a | b | (c & (d | e))");
        assertThat(contAnd.containsNode(parser.parse("d | e"))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsNNF(final FormulaContext _c) {
        assertThat(_c.or1.isNNF(_c.f)).isTrue();
        assertThat(_c.or2.isNNF(_c.f)).isTrue();
        assertThat(_c.or3.isNNF(_c.f)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsDNF(final FormulaContext _c) {
        assertThat(_c.or1.isDNF(_c.f)).isTrue();
        assertThat(_c.or2.isDNF(_c.f)).isTrue();
        assertThat(_c.or3.isDNF(_c.f)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testIsCNF(final FormulaContext _c) {
        assertThat(_c.or1.isCNF(_c.f)).isTrue();
        assertThat(_c.or2.isCNF(_c.f)).isTrue();
        assertThat(_c.or3.isCNF(_c.f)).isFalse();
    }
}
