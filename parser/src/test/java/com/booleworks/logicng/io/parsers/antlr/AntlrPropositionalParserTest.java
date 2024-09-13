// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.parsers.antlr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AntlrPropositionalParserTest {

    final FormulaFactory f = FormulaFactory.caching();

    @Test
    public void testExceptions() throws ParserException {
        final AntlrPropositionalParser parser = new AntlrPropositionalParser(f);
        assertThat(parser.parse("")).isEqualTo(f.verum());
        assertThat(parser.parse((String) null)).isEqualTo(f.verum());
    }

    @Test
    public void testParseConstants() throws ParserException {
        final AntlrPropositionalParser parser = new AntlrPropositionalParser(f);
        assertThat(parser.parse("$true")).isEqualTo(f.verum());
        assertThat(parser.parse("$false")).isEqualTo(f.falsum());
    }

    @Test
    public void testParseLiterals() throws ParserException {
        final AntlrPropositionalParser parser = new AntlrPropositionalParser(f);
        assertThat(parser.parse("A")).isEqualTo(f.variable("A"));
        assertThat(parser.parse("a")).isEqualTo(f.variable("a"));
        assertThat(parser.parse("a1")).isEqualTo(f.variable("a1"));
        assertThat(parser.parse("aA_Bb_Cc_12_3")).isEqualTo(f.variable("aA_Bb_Cc_12_3"));
        assertThat(parser.parse("~A")).isEqualTo(f.literal("A", false));
        assertThat(parser.parse("~a")).isEqualTo(f.literal("a", false));
        assertThat(parser.parse("~aA_Bb_Cc_12_3")).isEqualTo(f.literal("aA_Bb_Cc_12_3", false));
        assertThat(parser.parse("#")).isEqualTo(f.literal("#", true));
        assertThat(parser.parse("~#")).isEqualTo(f.literal("#", false));
        assertThat(parser.parse("~A#B")).isEqualTo(f.literal("A#B", false));
        assertThat(parser.parse("A#B")).isEqualTo(f.literal("A#B", true));
        assertThat(parser.parse("~A#B")).isEqualTo(f.literal("A#B", false));
        assertThat(parser.parse("#A#B_")).isEqualTo(f.literal("#A#B_", true));
        assertThat(parser.parse("~#A#B_")).isEqualTo(f.literal("#A#B_", false));
    }

    @Test
    public void testParseOperators() throws ParserException {
        final AntlrPropositionalParser parser = new AntlrPropositionalParser(f);
        assertThat(parser.parse("~a")).isEqualTo(f.not(f.variable("a")));
        assertThat(parser.parse("~Var")).isEqualTo(f.not(f.variable("Var")));
        assertThat(parser.parse("a & b")).isEqualTo(f.and(f.variable("a"), f.variable("b")));
        assertThat(parser.parse("~a & ~b")).isEqualTo(f.and(f.literal("a", false), f.literal("b", false)));
        assertThat(parser.parse("~a & b & ~c & d")).isEqualTo(f.and(f.literal("a", false), f.variable("b"), f.literal("c", false), f.variable("d")));
        assertThat(parser.parse("a | b")).isEqualTo(f.or(f.variable("a"), f.variable("b")));
        assertThat(parser.parse("~a | ~b")).isEqualTo(f.or(f.literal("a", false), f.literal("b", false)));
        assertThat(parser.parse("~a | b | ~c | d")).isEqualTo(f.or(f.literal("a", false), f.variable("b"), f.literal("c", false), f.variable("d")));
        assertThat(parser.parse("a => b")).isEqualTo(f.implication(f.variable("a"), f.variable("b")));
        assertThat(parser.parse("~a => ~b")).isEqualTo(f.implication(f.literal("a", false), f.literal("b", false)));
        assertThat(parser.parse("a <=> b")).isEqualTo(f.equivalence(f.variable("a"), f.variable("b")));
        assertThat(parser.parse("~a <=> ~b")).isEqualTo(f.equivalence(f.literal("a", false), f.literal("b", false)));
    }

    @Test
    public void testParseMultiplication() throws ParserException {
        final AntlrPropositionalParser parser = new AntlrPropositionalParser(f);
        assertThat(parser.parse("13 * abc = 4")).isEqualTo(f.pbc(CType.EQ, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        assertThat(parser.parse("-13 * a = 4")).isEqualTo(f.pbc(CType.EQ, 4, new Literal[]{f.variable("a")}, new int[]{-13}));
        assertThat(parser.parse("13 * ~abc = -442")).isEqualTo(f.pbc(CType.EQ, -442, new Literal[]{f.literal("abc", false)}, new int[]{13}));
        assertThat(parser.parse("-13 * ~a = -442")).isEqualTo(f.pbc(CType.EQ, -442, new Literal[]{f.literal("a", false)}, new int[]{-13}));
        assertThat(parser.parse("13 * abc = 4")).isEqualTo(f.pbc(CType.EQ, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        assertThat(parser.parse("13 * abc > 4")).isEqualTo(f.pbc(CType.GT, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        assertThat(parser.parse("13 * abc >= 4")).isEqualTo(f.pbc(CType.GE, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        assertThat(parser.parse("13 * abc < 4")).isEqualTo(f.pbc(CType.LT, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        assertThat(parser.parse("13 * abc <= 4")).isEqualTo(f.pbc(CType.LE, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
    }

    @Test
    public void testParseAddition() throws ParserException {
        final AntlrPropositionalParser parser = new AntlrPropositionalParser(f);
        assertThat(parser.parse("4 * c + -4 * ~d < -4")).isEqualTo(
                f.pbc(CType.LT, -4, new Literal[]{f.variable("c"), f.literal("d", false)}, new int[]{4, -4}));
        assertThat(parser.parse("5 * c + -5 * ~c >= -5")).isEqualTo(
                f.pbc(CType.GE, -5, new Literal[]{f.variable("c"), f.literal("c", false)}, new int[]{5, -5}));
        assertThat(parser.parse("6 * a + -6 * ~b + 12 * ~c > -6")).isEqualTo(
                f.pbc(CType.GT, -6, new Literal[]{f.variable("a"), f.literal("b", false), f.literal("c", false)}, new int[]{6, -6, 12}));
        assertThat(parser.parse("c + -4 * ~d < -4")).isEqualTo(
                f.pbc(CType.LT, -4, new Literal[]{f.variable("c"), f.literal("d", false)}, new int[]{1, -4}));
        assertThat(parser.parse("5 * c + ~c >= -5")).isEqualTo(
                f.pbc(CType.GE, -5, new Literal[]{f.variable("c"), f.literal("c", false)}, new int[]{5, 1}));
        assertThat(parser.parse("c + d >= -5")).isEqualTo(f.pbc(CType.GE, -5, new Literal[]{f.variable("c"), f.literal("d", true)}, new int[]{1, 1}));
        assertThat(parser.parse("~c + ~d >= -5")).isEqualTo(
                f.pbc(CType.GE, -5, new Literal[]{f.literal("c", false), f.literal("d", false)}, new int[]{1, 1}));
        assertThat(parser.parse("~c = -5")).isEqualTo(f.pbc(CType.EQ, -5, new Literal[]{f.literal("c", false)}, new int[]{1}));
        assertThat(parser.parse("~(c = -5)")).isEqualTo(f.not(f.pbc(CType.EQ, -5, new Literal[]{f.literal("c", true)}, new int[]{1})));
    }

    @Test
    public void testCombination() throws ParserException {
        final AntlrPropositionalParser parser = new AntlrPropositionalParser(f);
        final Formula pbc = f.pbc(CType.GT, -6, new Literal[]{f.variable("a"), f.literal("b", false), f.literal("c", false)}, new int[]{6, -6, 12});
        assertThat(parser.parse("(x => y & z) & (6 * a + -6 * ~b + 12 * ~c > -6)")).isEqualTo(
                f.and(f.implication(f.variable("x"), f.and(f.variable("y"), f.variable("z"))), pbc));
        assertThat(parser.parse("~(6 * a - 6 * ~b - -12 * ~c > -6)")).isEqualTo(f.not(pbc));
    }

    @Test
    public void testParseUnsafe() {
        final AntlrPropositionalParser parser = new AntlrPropositionalParser(f);
        final Formula pbc = f.pbc(CType.GT, -6, new Literal[]{f.variable("a"), f.literal("b", false), f.literal("c", false)}, new int[]{6, -6, 12});
        assertThat(parser.parseUnsafe("(x => y & z) & (6 * a + -6 * ~b + 12 * ~c > -6)")).isEqualTo(
                f.and(f.implication(f.variable("x"), f.and(f.variable("y"), f.variable("z"))), pbc));
        assertThat(parser.parseUnsafe("~(6 * a - 6 * ~b - -12 * ~c > -6)")).isEqualTo(f.not(pbc));
    }

    @Test
    public void testParsePrecedences() throws ParserException {
        final AntlrPropositionalParser parser = new AntlrPropositionalParser(f);
        assertThat(parser.parse("x | y & z")).isEqualTo(f.or(f.variable("x"), f.and(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x & y | z")).isEqualTo(f.or(f.and(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x => y & z")).isEqualTo(f.implication(f.variable("x"), f.and(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x & y => z")).isEqualTo(f.implication(f.and(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x <=> y & z")).isEqualTo(f.equivalence(f.variable("x"), f.and(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x & y <=> z")).isEqualTo(f.equivalence(f.and(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x => y | z")).isEqualTo(f.implication(f.variable("x"), f.or(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x | y => z")).isEqualTo(f.implication(f.or(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x <=> y | z")).isEqualTo(f.equivalence(f.variable("x"), f.or(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x | y <=> z")).isEqualTo(f.equivalence(f.or(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x => y => z")).isEqualTo(f.implication(f.variable("x"), f.implication(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x <=> y <=> z")).isEqualTo(f.equivalence(f.variable("x"), f.equivalence(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("(x | y) & z")).isEqualTo(f.and(f.or(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x & (y | z)")).isEqualTo(f.and(f.variable("x"), f.or(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("(x => y) & z")).isEqualTo(f.and(f.implication(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x & (y => z)")).isEqualTo(f.and(f.variable("x"), f.implication(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("(x => y) | z")).isEqualTo(f.or(f.implication(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x | (y => z)")).isEqualTo(f.or(f.variable("x"), f.implication(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("(x <=> y) & z")).isEqualTo(f.and(f.equivalence(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x & (y <=> z)")).isEqualTo(f.and(f.variable("x"), f.equivalence(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("(x <=> y) | z")).isEqualTo(f.or(f.equivalence(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x | (y <=> z)")).isEqualTo(f.or(f.variable("x"), f.equivalence(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x => y <=> z")).isEqualTo(f.equivalence(f.implication(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x => (y <=> z)")).isEqualTo(f.implication(f.variable("x"), f.equivalence(f.variable("y"), f.variable("z"))));
    }

    @Test
    public void parseEmptyString() throws ParserException {
        final AntlrPropositionalParser parser = new AntlrPropositionalParser(f);
        assertThat(parser.parse("")).isEqualTo(f.verum());
    }

    @Test
    public void testSkipSymbols() throws ParserException {
        final AntlrPropositionalParser parser = new AntlrPropositionalParser(f);
        assertThat(parser.parse(" ")).isEqualTo(f.verum());
        assertThat(parser.parse("\t")).isEqualTo(f.verum());
        assertThat(parser.parse("\n")).isEqualTo(f.verum());
        assertThat(parser.parse("\r")).isEqualTo(f.verum());
        assertThat(parser.parse(" \r\n\n  \t")).isEqualTo(f.verum());
        assertThat(parser.parse("a\n&\tb")).isEqualTo(f.and(f.variable("a"), f.variable("b")));
        assertThat(parser.parse(" a\r=>\t\tb")).isEqualTo(f.implication(f.variable("a"), f.variable("b")));
        assertThat(parser.parse(" 2\n*a\r+\n\n-4*\tb    +3*x=2")).isEqualTo(
                f.pbc(CType.EQ, 2, Arrays.asList(f.variable("a"), f.variable("b"), f.variable("x")), Arrays.asList(2, -4, 3)));
    }

    @Test
    public void testNumberLiterals() throws ParserException {
        final FormulaFactory f = FormulaFactory.nonCaching();
        final AntlrPropositionalParser parser = new AntlrPropositionalParser(f);
        assertThat(parser.parse("12 & A")).isEqualTo(f.and(f.variable("12"), f.variable("A")));
        assertThat(parser.parse("~12 & A")).isEqualTo(f.and(f.literal("12", false), f.variable("A")));
        assertThat(parser.parse("12 * 12 + 13 * A + 10 * B <= 25")).isEqualTo(
                f.pbc(CType.LE, 25, new Literal[]{f.variable("12"), f.variable("A"), f.variable("B")}, new int[]{12, 13, 10}));
        assertThat(parser.parse("-12 * ~12 + 13 * A + 10 * B <= 25")).isEqualTo(
                f.pbc(CType.LE, 25, new Literal[]{f.literal("12", false), f.variable("A"), f.variable("B")}, new int[]{-12, 13, 10}));
    }

    @Test
    public void testIllegalVariable1() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("$$%")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalVariable3() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse(";;23")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalVariable4() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("{0}")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator1() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("A + B")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator2() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("A &")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator3() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("A /")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator4() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("-A")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator5() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("A * B")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalBrackets1() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("(A & B")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula1() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("((A & B)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula2() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("(A & (C & D) B)")).isInstanceOf(
                ParserException.class);
    }

    @Test
    public void testIllegalFormula3() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("A | A + (C | B + C)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula4() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("A | A & (C | B & C")).isInstanceOf(
                ParserException.class);
    }

    @Test
    public void testIllegalFormula5() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("A & ~B)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula6() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("12)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula7() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("ab@cd)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalSkipPosition() {
        assertThatThrownBy(() -> new AntlrPropositionalParser(f).parse("- 1*x <= 3")).isInstanceOf(
                ParserException.class);
    }

    @Test
    public void testAsFactoryParser() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        f.setParser(new AntlrPropositionalParser(f));
        final Formula pbc = f.pbc(CType.GT, -6, new Literal[]{f.variable("a"), f.literal("b", false), f.literal("c", false)}, new int[]{6, -6, 12});

        assertThat(f.parse("(x => y & z) & (6 * a + -6 * ~b + 12 * ~c > -6)")).isEqualTo(
                f.and(f.implication(f.variable("x"), f.and(f.variable("y"), f.variable("z"))), pbc));
        assertThat(f.parse("~(6 * a - 6 * ~b - -12 * ~c > -6)")).isEqualTo(f.not(pbc));
    }

    @Test
    public void testCompareWithStockParser() throws IOException {
        final var strings = Files.readAllLines(Paths.get("../test_files/formulas/largest_formula.txt"));
        final var f1 = FormulaFactory.caching();
        final var f2 = FormulaFactory.caching();
        final var p1 = new PropositionalParser(f1);
        final var p2 = new AntlrPropositionalParser(f2);
        final var formulasStock = strings.stream().map(p1::parseUnsafe).collect(Collectors.toList());
        final var formulasAntlr = strings.stream().map(p2::parseUnsafe).collect(Collectors.toList());
        assertThat(formulasAntlr).isEqualTo(formulasStock);
    }
}
