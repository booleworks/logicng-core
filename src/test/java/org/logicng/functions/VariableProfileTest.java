// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.functions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.formulas.CType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.FormulaFactoryConfig;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableProfileTest {

    private final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
    private final FormulaFactory f2 = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());

    private final VariableProfileFunction caching = VariableProfileFunction.get(true);
    private final VariableProfileFunction nonCaching = VariableProfileFunction.get(false);

    private final Formula pb1;
    private final Formula pb2;
    private final Formula cc1;
    private final Formula cc2;
    private final Formula amo1;
    private final Formula amo2;
    private final Formula exo1;
    private final Formula exo2;

    public VariableProfileTest() {
        final Variable[] lits1 = new Variable[]{this.f.variable("a")};
        final List<Literal> lits2 = Arrays.asList(this.f2.variable("a"), this.f.literal("b", false), this.f.variable("c"));
        final List<Variable> litsCC2 = Arrays.asList(this.f.variable("a"), this.f2.variable("b"), this.f.variable("c"));
        final int[] coeffs1 = new int[]{3};
        final List<Integer> coeffs2 = Arrays.asList(3, -2, 7);
        this.pb1 = this.f.pbc(CType.LE, 2, lits1, coeffs1);
        this.pb2 = this.f.pbc(CType.LE, 8, lits2, coeffs2);
        this.cc1 = this.f.cc(CType.LT, 1, lits1);
        this.cc2 = this.f.cc(CType.GE, 2, litsCC2);
        this.amo1 = this.f.amo(lits1);
        this.amo2 = this.f.amo(litsCC2);
        this.exo1 = this.f.exo(lits1);
        this.exo2 = this.f.exo(litsCC2);
    }

    @Test
    public void testConstants() {
        assertThat(this.f.verum().apply(this.caching)).isEqualTo(new HashMap<>());
        assertThat(this.f.verum().apply(this.nonCaching)).isEqualTo(new HashMap<>());
        assertThat(this.f.falsum().apply(this.caching)).isEqualTo(new HashMap<>());
        assertThat(this.f.falsum().apply(this.nonCaching)).isEqualTo(new HashMap<>());
    }

    @Test
    public void testLiterals() {
        final Map<Literal, Integer> expected = new HashMap<>();
        expected.put(this.f2.variable("a"), 1);
        assertThat(this.f.literal("a", true).apply(this.caching)).isEqualTo(expected);
        assertThat(this.f.literal("a", true).apply(this.nonCaching)).isEqualTo(expected);
        assertThat(this.f.literal("a", false).apply(this.caching)).isEqualTo(expected);
        assertThat(this.f.literal("a", false).apply(this.nonCaching)).isEqualTo(expected);
    }

    @Test
    public void testPBC() {
        final Map<Literal, Integer> exp1 = new HashMap<>();
        exp1.put(this.f.variable("a"), 1);
        final Map<Literal, Integer> exp2 = new HashMap<>();
        exp2.put(this.f.variable("a"), 1);
        exp2.put(this.f2.variable("b"), 1);
        exp2.put(this.f.variable("c"), 1);
        assertThat(this.pb1.apply(this.caching)).isEqualTo(exp1);
        assertThat(this.pb2.apply(this.caching)).isEqualTo(exp2);
        assertThat(this.cc1.apply(this.caching)).isEqualTo(exp1);
        assertThat(this.cc2.apply(this.caching)).isEqualTo(exp2);
        assertThat(this.amo1.apply(this.caching)).isEqualTo(exp1);
        assertThat(this.amo2.apply(this.caching)).isEqualTo(exp2);
        assertThat(this.exo1.apply(this.caching)).isEqualTo(exp1);
        assertThat(this.exo2.apply(this.caching)).isEqualTo(exp2);

        assertThat(this.pb1.apply(this.nonCaching)).isEqualTo(exp1);
        assertThat(this.pb2.apply(this.nonCaching)).isEqualTo(exp2);
        assertThat(this.cc1.apply(this.nonCaching)).isEqualTo(exp1);
        assertThat(this.cc2.apply(this.nonCaching)).isEqualTo(exp2);
        assertThat(this.amo1.apply(this.nonCaching)).isEqualTo(exp1);
        assertThat(this.amo2.apply(this.nonCaching)).isEqualTo(exp2);
        assertThat(this.exo1.apply(this.nonCaching)).isEqualTo(exp1);
        assertThat(this.exo2.apply(this.nonCaching)).isEqualTo(exp2);
    }

    @Test
    public void testNot() throws ParserException {
        final Map<Literal, Integer> expected = new HashMap<>();
        expected.put(this.f2.variable("a"), 1);
        expected.put(this.f2.variable("b"), 2);
        expected.put(this.f2.variable("c"), 3);
        final PropositionalParser p = new PropositionalParser(this.f);
        final Formula formula = p.parse("~(a & (b | c) & ((~b | ~c) => c))");
        assertThat(formula.apply(this.caching)).isEqualTo(expected);
        assertThat(formula.apply(this.nonCaching)).isEqualTo(expected);
    }

    @Test
    public void testBinaryOperator() throws ParserException {
        final Map<Literal, Integer> expected = new HashMap<>();
        expected.put(this.f2.variable("a"), 1);
        expected.put(this.f2.variable("b"), 2);
        expected.put(this.f2.variable("c"), 3);
        final PropositionalParser p = new PropositionalParser(this.f);
        final Formula impl = p.parse("(a & (b | c) & (~b | ~c)) => c");
        final Formula equiv = p.parse("(a & (b | c) & (~b | ~c)) <=> c");
        assertThat(impl.apply(this.caching)).isEqualTo(expected);
        assertThat(impl.apply(this.nonCaching)).isEqualTo(expected);
        assertThat(equiv.apply(this.caching)).isEqualTo(expected);
        assertThat(equiv.apply(this.nonCaching)).isEqualTo(expected);
    }

    @Test
    public void testNAryOperator() throws ParserException {
        final Map<Literal, Integer> expected = new HashMap<>();
        expected.put(this.f2.variable("a"), 1);
        expected.put(this.f2.variable("b"), 2);
        expected.put(this.f2.variable("c"), 3);
        final PropositionalParser p = new PropositionalParser(this.f);
        final Formula formula = p.parse("a & (b | c) & (~b | ~c) & c");
        assertThat(formula.apply(this.caching)).isEqualTo(expected);
        assertThat(formula.apply(this.nonCaching)).isEqualTo(expected);
    }
}
