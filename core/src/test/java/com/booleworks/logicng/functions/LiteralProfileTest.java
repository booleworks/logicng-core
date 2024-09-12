// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class LiteralProfileTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final var caching = new LiteralProfileFunction(_c.f, new HashMap<>());
        final var nonCaching = new LiteralProfileFunction(_c.f, null);

        assertThat(_c.f.verum().apply(caching)).isEqualTo(new HashMap<>());
        assertThat(_c.f.verum().apply(nonCaching)).isEqualTo(new HashMap<>());
        assertThat(_c.f.falsum().apply(caching)).isEqualTo(new HashMap<>());
        assertThat(_c.f.falsum().apply(nonCaching)).isEqualTo(new HashMap<>());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final var caching = new LiteralProfileFunction(_c.f, new HashMap<>());
        final var nonCaching = new LiteralProfileFunction(_c.f, null);

        final Map<Literal, Integer> expectedPos = new HashMap<>();
        expectedPos.put(_c.f.variable("a"), 1);
        final Map<Literal, Integer> expectedNeg = new HashMap<>();
        expectedNeg.put(_c.f.literal("a", false), 1);
        assertThat(_c.f.literal("a", true).apply(caching)).isEqualTo(expectedPos);
        assertThat(_c.f.literal("a", true).apply(nonCaching)).isEqualTo(expectedPos);
        assertThat(_c.f.literal("a", false).apply(caching)).isEqualTo(expectedNeg);
        assertThat(_c.f.literal("a", false).apply(nonCaching)).isEqualTo(expectedNeg);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPBC(final FormulaContext _c) {
        final var caching = new LiteralProfileFunction(_c.f, new HashMap<>());
        final var nonCaching = new LiteralProfileFunction(_c.f, null);

        final Variable[] lits1 = new Variable[]{_c.f.variable("a")};
        final List<Literal> lits2 = List.of(_c.f.variable("a"), _c.f.literal("b", false), _c.f.variable("c"));
        final List<Variable> litsCC2 = List.of(_c.f.variable("a"), _c.f.variable("b"), _c.f.variable("c"));
        final int[] coeffs1 = new int[]{3};
        final List<Integer> coeffs2 = List.of(3, -2, 7);
        final Formula pb1 = _c.f.pbc(CType.LE, 2, lits1, coeffs1);
        final Formula pb2 = _c.f.pbc(CType.LE, 8, lits2, coeffs2);
        final Formula cc1 = _c.f.cc(CType.LT, 1, lits1);
        final Formula cc2 = _c.f.cc(CType.GE, 2, litsCC2);
        final Formula amo1 = _c.f.amo(lits1);
        final Formula amo2 = _c.f.amo(litsCC2);
        final Formula exo1 = _c.f.exo(lits1);
        final Formula exo2 = _c.f.exo(litsCC2);

        final SortedMap<Literal, Integer> exp1 = new TreeMap<>();
        exp1.put(_c.f.variable("a"), 1);
        final SortedMap<Literal, Integer> exp2 = new TreeMap<>();
        exp2.put(_c.f.variable("a"), 1);
        exp2.put(_c.f.literal("b", false), 1);
        exp2.put(_c.f.variable("c"), 1);
        final SortedMap<Literal, Integer> exp2CC = new TreeMap<>();
        exp2CC.put(_c.f.variable("a"), 1);
        exp2CC.put(_c.f.variable("b"), 1);
        exp2CC.put(_c.f.variable("c"), 1);

        assertThat(pb1.apply(caching)).isEqualTo(exp1);
        assertThat(pb2.apply(caching)).isEqualTo(exp2);
        assertThat(cc1.apply(caching)).isEqualTo(exp1);
        assertThat(cc2.apply(caching)).isEqualTo(exp2CC);
        assertThat(amo1.apply(caching)).isEqualTo(exp1);
        assertThat(amo2.apply(caching)).isEqualTo(exp2CC);
        assertThat(exo1.apply(caching)).isEqualTo(exp1);
        assertThat(exo2.apply(caching)).isEqualTo(exp2CC);

        assertThat(pb1.apply(nonCaching)).isEqualTo(exp1);
        assertThat(pb2.apply(nonCaching)).isEqualTo(exp2);
        assertThat(cc1.apply(nonCaching)).isEqualTo(exp1);
        assertThat(cc2.apply(nonCaching)).isEqualTo(exp2CC);
        assertThat(amo1.apply(nonCaching)).isEqualTo(exp1);
        assertThat(amo2.apply(nonCaching)).isEqualTo(exp2CC);
        assertThat(exo1.apply(nonCaching)).isEqualTo(exp1);
        assertThat(exo2.apply(nonCaching)).isEqualTo(exp2CC);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        final var caching = new LiteralProfileFunction(_c.f, new HashMap<>());
        final var nonCaching = new LiteralProfileFunction(_c.f, null);

        final Map<Literal, Integer> expected = new HashMap<>();
        expected.put(_c.f.variable("a"), 2);
        expected.put(_c.f.variable("b"), 1);
        expected.put(_c.f.variable("c"), 1);
        expected.put(_c.f.literal("b", false), 1);
        expected.put(_c.f.literal("c", false), 2);
        final Formula formula = _c.p.parse("~(a & (b | c) & ((~b | ~c) => (~c & a)))");
        assertThat(formula.apply(caching)).isEqualTo(expected);
        assertThat(formula.apply(nonCaching)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryOperator(final FormulaContext _c) throws ParserException {
        final var caching = new LiteralProfileFunction(_c.f, new HashMap<>());
        final var nonCaching = new LiteralProfileFunction(_c.f, null);

        final Map<Literal, Integer> expected = new HashMap<>();
        expected.put(_c.f.variable("a"), 2);
        expected.put(_c.f.variable("b"), 1);
        expected.put(_c.f.variable("c"), 1);
        expected.put(_c.f.literal("b", false), 1);
        expected.put(_c.f.literal("c", false), 2);
        final Formula impl = _c.p.parse("(a & (b | c) & (~b | ~c)) => (~c & a)");
        final Formula equiv = _c.p.parse("(a & (b | c) & (~b | ~c)) <=> (~c & a)");
        assertThat(impl.apply(caching)).isEqualTo(expected);
        assertThat(impl.apply(nonCaching)).isEqualTo(expected);
        assertThat(equiv.apply(caching)).isEqualTo(expected);
        assertThat(equiv.apply(nonCaching)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperator(final FormulaContext _c) throws ParserException {
        final var caching = new LiteralProfileFunction(_c.f, new HashMap<>());
        final var nonCaching = new LiteralProfileFunction(_c.f, null);

        final Map<Literal, Integer> expected = new HashMap<>();
        expected.put(_c.f.variable("a"), 2);
        expected.put(_c.f.variable("b"), 1);
        expected.put(_c.f.variable("c"), 1);
        expected.put(_c.f.literal("b", false), 1);
        expected.put(_c.f.literal("c", false), 2);
        final Formula formula = _c.p.parse("a & (b | c) & (~b | ~c) | (~c & a)");
        assertThat(formula.apply(caching)).isEqualTo(expected);
        assertThat(formula.apply(nonCaching)).isEqualTo(expected);
    }
}
