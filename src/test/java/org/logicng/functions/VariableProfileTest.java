// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.functions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.formulas.CType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.Literal;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableProfileTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final var caching = new VariableProfileFunction(_c.f, new HashMap<>());
        final var nonCaching = new VariableProfileFunction(_c.f, null);

        assertThat(_c.f.verum().apply(caching)).isEqualTo(new HashMap<>());
        assertThat(_c.f.verum().apply(nonCaching)).isEqualTo(new HashMap<>());
        assertThat(_c.f.falsum().apply(caching)).isEqualTo(new HashMap<>());
        assertThat(_c.f.falsum().apply(nonCaching)).isEqualTo(new HashMap<>());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final var caching = new VariableProfileFunction(_c.f, new HashMap<>());
        final var nonCaching = new VariableProfileFunction(_c.f, null);

        final Map<Literal, Integer> expected = new HashMap<>();
        expected.put(_c.f.variable("a"), 1);

        assertThat(_c.f.literal("a", true).apply(caching)).isEqualTo(expected);
        assertThat(_c.f.literal("a", true).apply(nonCaching)).isEqualTo(expected);
        assertThat(_c.f.literal("a", false).apply(caching)).isEqualTo(expected);
        assertThat(_c.f.literal("a", false).apply(nonCaching)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPBC(final FormulaContext _c) {
        final var caching = new VariableProfileFunction(_c.f, new HashMap<>());
        final var nonCaching = new VariableProfileFunction(_c.f, null);

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

        final Map<Literal, Integer> exp1 = new HashMap<>();
        exp1.put(_c.f.variable("a"), 1);
        final Map<Literal, Integer> exp2 = new HashMap<>();
        exp2.put(_c.f.variable("a"), 1);
        exp2.put(_c.f.variable("b"), 1);
        exp2.put(_c.f.variable("c"), 1);

        assertThat(pb1.apply(caching)).isEqualTo(exp1);
        assertThat(pb2.apply(caching)).isEqualTo(exp2);
        assertThat(cc1.apply(caching)).isEqualTo(exp1);
        assertThat(cc2.apply(caching)).isEqualTo(exp2);
        assertThat(amo1.apply(caching)).isEqualTo(exp1);
        assertThat(amo2.apply(caching)).isEqualTo(exp2);
        assertThat(exo1.apply(caching)).isEqualTo(exp1);
        assertThat(exo2.apply(caching)).isEqualTo(exp2);

        assertThat(pb1.apply(nonCaching)).isEqualTo(exp1);
        assertThat(pb2.apply(nonCaching)).isEqualTo(exp2);
        assertThat(cc1.apply(nonCaching)).isEqualTo(exp1);
        assertThat(cc2.apply(nonCaching)).isEqualTo(exp2);
        assertThat(amo1.apply(nonCaching)).isEqualTo(exp1);
        assertThat(amo2.apply(nonCaching)).isEqualTo(exp2);
        assertThat(exo1.apply(nonCaching)).isEqualTo(exp1);
        assertThat(exo2.apply(nonCaching)).isEqualTo(exp2);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        final var caching = new VariableProfileFunction(_c.f, new HashMap<>());
        final var nonCaching = new VariableProfileFunction(_c.f, null);

        final Map<Literal, Integer> expected = new HashMap<>();
        expected.put(_c.f.variable("a"), 1);
        expected.put(_c.f.variable("b"), 2);
        expected.put(_c.f.variable("c"), 3);
        final Formula formula = _c.p.parse("~(a & (b | c) & ((~b | ~c) => c))");
        assertThat(formula.apply(caching)).isEqualTo(expected);
        assertThat(formula.apply(nonCaching)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryOperator(final FormulaContext _c) throws ParserException {
        final var caching = new VariableProfileFunction(_c.f, new HashMap<>());
        final var nonCaching = new VariableProfileFunction(_c.f, null);

        final Map<Literal, Integer> expected = new HashMap<>();
        expected.put(_c.f.variable("a"), 1);
        expected.put(_c.f.variable("b"), 2);
        expected.put(_c.f.variable("c"), 3);
        final Formula impl = _c.p.parse("(a & (b | c) & (~b | ~c)) => c");
        final Formula equiv = _c.p.parse("(a & (b | c) & (~b | ~c)) <=> c");
        assertThat(impl.apply(caching)).isEqualTo(expected);
        assertThat(impl.apply(nonCaching)).isEqualTo(expected);
        assertThat(equiv.apply(caching)).isEqualTo(expected);
        assertThat(equiv.apply(nonCaching)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperator(final FormulaContext _c) throws ParserException {
        final var caching = new VariableProfileFunction(_c.f, new HashMap<>());
        final var nonCaching = new VariableProfileFunction(_c.f, null);

        final Map<Literal, Integer> expected = new HashMap<>();
        expected.put(_c.f.variable("a"), 1);
        expected.put(_c.f.variable("b"), 2);
        expected.put(_c.f.variable("c"), 3);
        final Formula formula = _c.p.parse("a & (b | c) & (~b | ~c) & c");
        assertThat(formula.apply(caching)).isEqualTo(expected);
        assertThat(formula.apply(nonCaching)).isEqualTo(expected);
    }
}
