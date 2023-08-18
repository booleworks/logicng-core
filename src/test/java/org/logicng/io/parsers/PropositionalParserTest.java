// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.io.parsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PropositionalParserTest extends TestWithExampleFormulas {

    @Test
    public void testExceptions() throws ParserException {
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("")).isEqualTo(f.verum());
        final String s = null;
        assertThat(parser.parse(s)).isEqualTo(f.verum());
    }

    @Test
    public void testParseConstants() throws ParserException {
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("$true")).isEqualTo(f.verum());
        assertThat(parser.parse("$false")).isEqualTo(f.falsum());
    }

    @Test
    public void testParseLiterals() throws ParserException {
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("A")).isEqualTo(f.variable("A"));
        assertThat(parser.parse("a")).isEqualTo(f.variable("a"));
        assertThat(parser.parse("a1")).isEqualTo(f.variable("a1"));
        assertThat(parser.parse("aA_Bb_Cc_12_3")).isEqualTo(f.variable("aA_Bb_Cc_12_3"));
        assertThat(parser.parse("~A")).isEqualTo(f.literal("A", false));
        assertThat(parser.parse("~a")).isEqualTo(f.literal("a", false));
        assertThat(parser.parse("~a1")).isEqualTo(f.literal("a1", false));
        assertThat(parser.parse("~aA_Bb_Cc_12_3")).isEqualTo(f.literal("aA_Bb_Cc_12_3", false));
        assertThat(parser.parse("~@aA_Bb_Cc_12_3")).isEqualTo(f.literal("@aA_Bb_Cc_12_3", false));
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
        final PropositionalParser parser = new PropositionalParser(f);
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
    public void testParsePrecedences() throws ParserException {
        final PropositionalParser parser = new PropositionalParser(f);
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
    public void parseInputStream() throws ParserException {
        final PropositionalParser parser = new PropositionalParser(f);
        final String string = "A & B => D | ~C";
        final InputStream stream = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
        assertThat(parser.parse(stream)).isEqualTo(parser.parse(string));
        assertThat(parser.parse((InputStream) null)).isEqualTo(f.verum());
    }

    @Test
    public void parseEmptyString() throws ParserException {
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("")).isEqualTo(f.verum());
        assertThat(parser.parse((String) null)).isEqualTo(f.verum());
    }

    @Test
    public void testFormulaFactory() {
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.factory()).isEqualTo(f);
    }

    @Test
    public void testSkipSymbols() throws ParserException {
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse(" ")).isEqualTo(f.verum());
        assertThat(parser.parse("\t")).isEqualTo(f.verum());
        assertThat(parser.parse("\n")).isEqualTo(f.verum());
        assertThat(parser.parse("\r")).isEqualTo(f.verum());
        assertThat(parser.parse(" \r\n\n  \t")).isEqualTo(f.verum());
        assertThat(parser.parse("a\n&\tb")).isEqualTo(AND1);
        assertThat(parser.parse(" a\r=>\t\tb")).isEqualTo(IMP1);
    }

    @Test
    public void testNumericalLiteral() throws ParserException {
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("12")).isEqualTo(f.variable("12"));
    }

    @Test
    public void testIllegalVariable1() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("$$%")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalVariable3() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse(";;23")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalVariable4() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("{0}")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator1() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("A + B")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator2() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("A &")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator3() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("A /")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator4() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("-A")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalOperator5() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("A * B")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalBrackets1() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("(A & B")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula1() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("((A & B)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula2() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("(A & (C & D) B)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula3() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("A | A + (C | B + C)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula4() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("A | A & (C | B & C")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula5() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("A & ~B)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula6() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("12)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula7() {
        assertThatThrownBy(() -> new PropositionalParser(f).parse("ab@cd)")).isInstanceOf(ParserException.class);
    }

    @Test
    public void testIllegalFormula8() {
        final String string = "A & B => D | ~";
        final InputStream stream = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> new PropositionalParser(f).parse(stream)).isInstanceOf(ParserException.class);

    }

    @Test
    public void testIllegalFormula9() {
        final String string = "@A@B";
        final InputStream stream = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> new PropositionalParser(f).parse(stream)).isInstanceOf(ParserException.class);
    }

    @Test
    public void testToStrings() {
        assertThat(new PropositionalLexer(null).toString()).isEqualTo("PropositionalLexer");
        assertThat(new PropositionalParser(f).toString()).isEqualTo("PropositionalParser");
    }
}
