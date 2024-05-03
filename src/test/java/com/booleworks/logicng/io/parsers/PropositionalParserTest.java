// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.io.parsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.TestWithExampleFormulas;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PropositionalParserTest extends TestWithExampleFormulas {

    @Test
    public void testExceptions() throws ParserException {
        final PropositionalParserNew parser = new PropositionalParserNew(f);
        Assertions.assertThat(parser.parse("")).isEqualTo(f.verum());
        Assertions.assertThat(parser.parse((String) null)).isEqualTo(f.verum());
    }

    @Test
    public void testParseConstants() throws ParserException {
        final PropositionalParserNew parser = new PropositionalParserNew(f);
        Assertions.assertThat(parser.parse("$true")).isEqualTo(f.verum());
        Assertions.assertThat(parser.parse("$false")).isEqualTo(f.falsum());
    }

    @Test
    public void testParseLiterals() throws ParserException {
        final PropositionalParserNew parser = new PropositionalParserNew(f);
        Assertions.assertThat(parser.parse("A")).isEqualTo(f.variable("A"));
        Assertions.assertThat(parser.parse("a")).isEqualTo(f.variable("a"));
        Assertions.assertThat(parser.parse("a1")).isEqualTo(f.variable("a1"));
        Assertions.assertThat(parser.parse("aA_Bb_Cc_12_3")).isEqualTo(f.variable("aA_Bb_Cc_12_3"));
        Assertions.assertThat(parser.parse("~A")).isEqualTo(f.literal("A", false));
        Assertions.assertThat(parser.parse("~a")).isEqualTo(f.literal("a", false));
        Assertions.assertThat(parser.parse("~aA_Bb_Cc_12_3")).isEqualTo(f.literal("aA_Bb_Cc_12_3", false));
        Assertions.assertThat(parser.parse("#")).isEqualTo(f.literal("#", true));
        Assertions.assertThat(parser.parse("~#")).isEqualTo(f.literal("#", false));
        Assertions.assertThat(parser.parse("~A#B")).isEqualTo(f.literal("A#B", false));
        Assertions.assertThat(parser.parse("A#B")).isEqualTo(f.literal("A#B", true));
        Assertions.assertThat(parser.parse("~A#B")).isEqualTo(f.literal("A#B", false));
        Assertions.assertThat(parser.parse("#A#B_")).isEqualTo(f.literal("#A#B_", true));
        Assertions.assertThat(parser.parse("~#A#B_")).isEqualTo(f.literal("#A#B_", false));
    }

    @Test
    public void testParseOperators() throws ParserException {
        final PropositionalParserNew parser = new PropositionalParserNew(f);
        Assertions.assertThat(parser.parse("~a")).isEqualTo(f.not(f.variable("a")));
        Assertions.assertThat(parser.parse("~Var")).isEqualTo(f.not(f.variable("Var")));
        Assertions.assertThat(parser.parse("a & b")).isEqualTo(f.and(f.variable("a"), f.variable("b")));
        Assertions.assertThat(parser.parse("~a & ~b")).isEqualTo(f.and(f.literal("a", false), f.literal("b", false)));
        Assertions.assertThat(parser.parse("~a & b & ~c & d"))
                .isEqualTo(f.and(f.literal("a", false), f.variable("b"), f.literal("c", false), f.variable("d")));
        Assertions.assertThat(parser.parse("a | b")).isEqualTo(f.or(f.variable("a"), f.variable("b")));
        Assertions.assertThat(parser.parse("~a | ~b")).isEqualTo(f.or(f.literal("a", false), f.literal("b", false)));
        Assertions.assertThat(parser.parse("~a | b | ~c | d"))
                .isEqualTo(f.or(f.literal("a", false), f.variable("b"), f.literal("c", false), f.variable("d")));
        Assertions.assertThat(parser.parse("a => b")).isEqualTo(f.implication(f.variable("a"), f.variable("b")));
        Assertions.assertThat(parser.parse("~a => ~b"))
                .isEqualTo(f.implication(f.literal("a", false), f.literal("b", false)));
        Assertions.assertThat(parser.parse("a <=> b")).isEqualTo(f.equivalence(f.variable("a"), f.variable("b")));
        Assertions.assertThat(parser.parse("~a <=> ~b"))
                .isEqualTo(f.equivalence(f.literal("a", false), f.literal("b", false)));
    }

    @Test
    public void testParseMultiplication() throws ParserException {
        final PropositionalParserNew parser = new PropositionalParserNew(f);
        Assertions.assertThat(parser.parse("13 * abc = 4"))
                .isEqualTo(f.pbc(CType.EQ, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        Assertions.assertThat(parser.parse("-13 * a = 4"))
                .isEqualTo(f.pbc(CType.EQ, 4, new Literal[]{f.variable("a")}, new int[]{-13}));
        Assertions.assertThat(parser.parse("13 * ~abc = -442"))
                .isEqualTo(f.pbc(CType.EQ, -442, new Literal[]{f.literal("abc", false)}, new int[]{13}));
        Assertions.assertThat(parser.parse("-13 * ~a = -442"))
                .isEqualTo(f.pbc(CType.EQ, -442, new Literal[]{f.literal("a", false)}, new int[]{-13}));
        Assertions.assertThat(parser.parse("13 * abc = 4"))
                .isEqualTo(f.pbc(CType.EQ, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        Assertions.assertThat(parser.parse("13 * abc > 4"))
                .isEqualTo(f.pbc(CType.GT, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        Assertions.assertThat(parser.parse("13 * abc >= 4"))
                .isEqualTo(f.pbc(CType.GE, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        Assertions.assertThat(parser.parse("13 * abc < 4"))
                .isEqualTo(f.pbc(CType.LT, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        Assertions.assertThat(parser.parse("13 * abc <= 4"))
                .isEqualTo(f.pbc(CType.LE, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
    }

    @Test
    public void testParseAddition() throws ParserException {
        final PropositionalParserNew parser = new PropositionalParserNew(f);
        Assertions.assertThat(parser.parse("4 * c + -4 * ~d < -4")).isEqualTo(
                f.pbc(CType.LT, -4, new Literal[]{f.variable("c"), f.literal("d", false)}, new int[]{4, -4}));
        Assertions.assertThat(parser.parse("5 * c + -5 * ~c >= -5")).isEqualTo(
                f.pbc(CType.GE, -5, new Literal[]{f.variable("c"), f.literal("c", false)}, new int[]{5, -5}));
        Assertions.assertThat(parser.parse("6 * a + -6 * ~b + 12 * ~c > -6")).isEqualTo(f.pbc(CType.GT, -6,
                new Literal[]{f.variable("a"), f.literal("b", false), f.literal("c", false)}, new int[]{6, -6, 12}));
        Assertions.assertThat(parser.parse("c + -4 * ~d < -4")).isEqualTo(
                f.pbc(CType.LT, -4, new Literal[]{f.variable("c"), f.literal("d", false)}, new int[]{1, -4}));
        Assertions.assertThat(parser.parse("5 * c + ~c >= -5"))
                .isEqualTo(f.pbc(CType.GE, -5, new Literal[]{f.variable("c"), f.literal("c", false)}, new int[]{5, 1}));
        Assertions.assertThat(parser.parse("c + d >= -5"))
                .isEqualTo(f.pbc(CType.GE, -5, new Literal[]{f.variable("c"), f.literal("d", true)}, new int[]{1, 1}));
        Assertions.assertThat(parser.parse("~c + ~d >= -5")).isEqualTo(
                f.pbc(CType.GE, -5, new Literal[]{f.literal("c", false), f.literal("d", false)}, new int[]{1, 1}));
        Assertions.assertThat(parser.parse("~c = -5"))
                .isEqualTo(f.pbc(CType.EQ, -5, new Literal[]{f.literal("c", false)}, new int[]{1}));
        Assertions.assertThat(parser.parse("~(c = -5)"))
                .isEqualTo(f.not(f.pbc(CType.EQ, -5, new Literal[]{f.literal("c", true)}, new int[]{1})));
    }

    @Test
    public void testCombination() throws ParserException {
        final PropositionalParserNew parser = new PropositionalParserNew(f);
        final Formula pbc = f.pbc(CType.GT, -6,
                new Literal[]{f.variable("a"), f.literal("b", false), f.literal("c", false)}, new int[]{6, -6, 12});
        Assertions.assertThat(parser.parse("(x => y & z) & (6 * a + -6 * ~b + 12 * ~c > -6)"))
                .isEqualTo(f.and(f.implication(f.variable("x"), f.and(f.variable("y"), f.variable("z"))), pbc));
        Assertions.assertThat(parser.parse("~(6 * a - 6 * ~b - -12 * ~c > -6)")).isEqualTo(f.not(pbc));
    }

    @Test
    public void testParsePrecedences() throws ParserException {
        final PropositionalParserNew parser = new PropositionalParserNew(f);
        Assertions.assertThat(parser.parse("x | y & z"))
                .isEqualTo(f.or(f.variable("x"), f.and(f.variable("y"), f.variable("z"))));
        Assertions.assertThat(parser.parse("x & y | z"))
                .isEqualTo(f.or(f.and(f.variable("x"), f.variable("y")), f.variable("z")));
        Assertions.assertThat(parser.parse("x => y & z"))
                .isEqualTo(f.implication(f.variable("x"), f.and(f.variable("y"), f.variable("z"))));
        Assertions.assertThat(parser.parse("x & y => z"))
                .isEqualTo(f.implication(f.and(f.variable("x"), f.variable("y")), f.variable("z")));
        Assertions.assertThat(parser.parse("x <=> y & z"))
                .isEqualTo(f.equivalence(f.variable("x"), f.and(f.variable("y"), f.variable("z"))));
        Assertions.assertThat(parser.parse("x & y <=> z"))
                .isEqualTo(f.equivalence(f.and(f.variable("x"), f.variable("y")), f.variable("z")));
        Assertions.assertThat(parser.parse("x => y | z"))
                .isEqualTo(f.implication(f.variable("x"), f.or(f.variable("y"), f.variable("z"))));
        Assertions.assertThat(parser.parse("x | y => z"))
                .isEqualTo(f.implication(f.or(f.variable("x"), f.variable("y")), f.variable("z")));
        Assertions.assertThat(parser.parse("x <=> y | z"))
                .isEqualTo(f.equivalence(f.variable("x"), f.or(f.variable("y"), f.variable("z"))));
        Assertions.assertThat(parser.parse("x | y <=> z"))
                .isEqualTo(f.equivalence(f.or(f.variable("x"), f.variable("y")), f.variable("z")));
        Assertions.assertThat(parser.parse("x => y => z"))
                .isEqualTo(f.implication(f.variable("x"), f.implication(f.variable("y"), f.variable("z"))));
        Assertions.assertThat(parser.parse("x <=> y <=> z"))
                .isEqualTo(f.equivalence(f.variable("x"), f.equivalence(f.variable("y"), f.variable("z"))));
        Assertions.assertThat(parser.parse("(x | y) & z"))
                .isEqualTo(f.and(f.or(f.variable("x"), f.variable("y")), f.variable("z")));
        Assertions.assertThat(parser.parse("x & (y | z)"))
                .isEqualTo(f.and(f.variable("x"), f.or(f.variable("y"), f.variable("z"))));
        Assertions.assertThat(parser.parse("(x => y) & z"))
                .isEqualTo(f.and(f.implication(f.variable("x"), f.variable("y")), f.variable("z")));
        Assertions.assertThat(parser.parse("x & (y => z)"))
                .isEqualTo(f.and(f.variable("x"), f.implication(f.variable("y"), f.variable("z"))));
        Assertions.assertThat(parser.parse("(x => y) | z"))
                .isEqualTo(f.or(f.implication(f.variable("x"), f.variable("y")), f.variable("z")));
        Assertions.assertThat(parser.parse("x | (y => z)"))
                .isEqualTo(f.or(f.variable("x"), f.implication(f.variable("y"), f.variable("z"))));
        Assertions.assertThat(parser.parse("(x <=> y) & z"))
                .isEqualTo(f.and(f.equivalence(f.variable("x"), f.variable("y")), f.variable("z")));
        Assertions.assertThat(parser.parse("x & (y <=> z)"))
                .isEqualTo(f.and(f.variable("x"), f.equivalence(f.variable("y"), f.variable("z"))));
        Assertions.assertThat(parser.parse("(x <=> y) | z"))
                .isEqualTo(f.or(f.equivalence(f.variable("x"), f.variable("y")), f.variable("z")));
        Assertions.assertThat(parser.parse("x | (y <=> z)"))
                .isEqualTo(f.or(f.variable("x"), f.equivalence(f.variable("y"), f.variable("z"))));
        Assertions.assertThat(parser.parse("x => y <=> z"))
                .isEqualTo(f.equivalence(f.implication(f.variable("x"), f.variable("y")), f.variable("z")));
        Assertions.assertThat(parser.parse("x => (y <=> z)"))
                .isEqualTo(f.implication(f.variable("x"), f.equivalence(f.variable("y"), f.variable("z"))));
    }

    @Test
    public void parseEmptyString() throws ParserException {
        final PropositionalParserNew parser = new PropositionalParserNew(f);
        Assertions.assertThat(parser.parse("")).isEqualTo(f.verum());
    }

    @Test
    public void testSkipSymbols() throws ParserException {
        final PropositionalParserNew parser = new PropositionalParserNew(f);
        Assertions.assertThat(parser.parse(" ")).isEqualTo(f.verum());
        Assertions.assertThat(parser.parse("\t")).isEqualTo(f.verum());
        Assertions.assertThat(parser.parse("\n")).isEqualTo(f.verum());
        Assertions.assertThat(parser.parse("\r")).isEqualTo(f.verum());
        Assertions.assertThat(parser.parse(" \r\n\n  \t")).isEqualTo(f.verum());
        Assertions.assertThat(parser.parse("a\n&\tb")).isEqualTo(AND1);
        Assertions.assertThat(parser.parse(" a\r=>\t\tb")).isEqualTo(IMP1);
        Assertions.assertThat(parser.parse(" 2\n*a\r+\n\n-4*\tb    +3*x=2")).isEqualTo(PBC1);
    }

    @Test
    public void testNumberLiterals() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParserNew parser = new PropositionalParserNew(f);
        Assertions.assertThat(parser.parse("12 & A")).isEqualTo(f.and(f.variable("12"), f.variable("A")));
        Assertions.assertThat(parser.parse("~12 & A")).isEqualTo(f.and(f.literal("12", false), f.variable("A")));
        Assertions.assertThat(parser.parse("12 * 12 + 13 * A + 10 * B <= 25")).isEqualTo(f.pbc(CType.LE, 25,
                new Literal[]{f.variable("12"), f.variable("A"), f.variable("B")}, new int[]{12, 13, 10}));
        Assertions.assertThat(parser.parse("-12 * ~12 + 13 * A + 10 * B <= 25")).isEqualTo(f.pbc(CType.LE, 25,
                new Literal[]{f.literal("12", false), f.variable("A"), f.variable("B")}, new int[]{-12, 13, 10}));
    }

    @Test
    public void testFormulaFactoryParser() throws ParserException {
        assertThat(f.parse("a & b")).isEqualTo(f.and(f.variable("a"), f.variable("b")));
        assertThat(f.parse("2*a + -4*b + 3*x = 2")).isEqualTo(PBC1);
    }

    @Test
    public void testIllegalVariable1() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("$$%")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalVariable3() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse(";;23")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalVariable4() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("{0}")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator1() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("A + B")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator2() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("A &")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator3() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("A /")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator4() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("-A")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator5() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("A * B")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalBrackets1() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("(A & B")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula1() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("((A & B)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula2() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("(A & (C & D) B)"))
                .isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula3() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("A | A + (C | B + C)"))
                .isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula4() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("A | A & (C | B & C"))
                .isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula5() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("A & ~B)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula6() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("12)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula7() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("ab@cd)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalSkipPosition() {
        assertThatThrownBy(() -> new PropositionalParserNew(f).parse("- 1*x <= 3")).isInstanceOf(ParserException.class);
    }

    @Test
    public void parseLargeFormula() throws ParserException, IOException {
        final int num = 100;
        long sumOld = 0;
        long sumNew = 0;
        final String input = Files.readString(Paths.get("src/test/resources/formulas/large_formula.txt"));
        for (int i = 0; i < num; i++) {
            final FormulaFactory f1 = FormulaFactory.caching();
            final var t1 = System.currentTimeMillis();
            final Formula formula1 = new PropositionalParser(f1).parse(input);
            final var t2 = System.currentTimeMillis();
            sumOld += t2 - t1;

            final FormulaFactory f2 = FormulaFactory.caching();
            final var t3 = System.currentTimeMillis();
            final Formula formula2 = new PropositionalParserNew(f2).parse(input);
            final var t4 = System.currentTimeMillis();
            sumNew += t4 - t3;

            assertThat(formula1.equals(formula2)).isTrue();
        }
        System.out.println("ANTLR:  " + (sumOld / num) + " ms.");
        System.out.println("JavaCC: " + (sumNew / num) + " ms.");
    }

    @Test
    public void parseLargeFormulas() throws ParserException, IOException {
        final int num = 20;
        long sumOld = 0;
        long sumNew = 0;
        final List<String> input = Files.readAllLines(Paths.get("src/test/resources/formulas/formula3.txt"));
        for (int i = 0; i < num; i++) {
            final FormulaFactory f1 = FormulaFactory.caching();
            final var p1 = new PropositionalParser(f1);
            final var t1 = System.currentTimeMillis();
            final List<Formula> formula1 = new ArrayList<>();
            for (final String s : input) {
                formula1.add(p1.parse(s));
            }

            final var t2 = System.currentTimeMillis();
            sumOld += t2 - t1;

            final FormulaFactory f2 = FormulaFactory.caching();
            final var p2 = new PropositionalParserNew(f2);
            final var t3 = System.currentTimeMillis();
            final List<Formula> formula2 = new ArrayList<>();
            for (final String s : input) {
                formula2.add(p2.parse(s));
            }
            final var t4 = System.currentTimeMillis();
            sumNew += t4 - t3;

            //assertThat(formula1.equals(formula2)).isTrue();
        }
        System.out.println("ANTLR:  " + (sumOld / num) + " ms.");
        System.out.println("JavaCC: " + (sumNew / num) + " ms.");
    }
}
