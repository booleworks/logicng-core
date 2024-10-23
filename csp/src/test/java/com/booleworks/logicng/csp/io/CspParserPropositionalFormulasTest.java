package com.booleworks.logicng.csp.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import com.booleworks.logicng.csp.io.parsers.CspParser;
import com.booleworks.logicng.csp.io.readers.CspReader;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.io.readers.FormulaReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class CspParserPropositionalFormulasTest extends ParameterizedCspTest {
    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testExceptions(CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("")).isEqualTo(f.verum());
        assertThat(parser.parse((String) null)).isEqualTo(f.verum());
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testParseConstants(CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("$true")).isEqualTo(f.verum());
        assertThat(parser.parse("$false")).isEqualTo(f.falsum());
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testParseLiterals(CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final PropositionalParser parser = new PropositionalParser(f);
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

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testParseOperators(CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("~a")).isEqualTo(f.not(f.variable("a")));
        assertThat(parser.parse("~Var")).isEqualTo(f.not(f.variable("Var")));
        assertThat(parser.parse("a & b")).isEqualTo(f.and(f.variable("a"), f.variable("b")));
        assertThat(parser.parse("~a & ~b")).isEqualTo(f.and(f.literal("a", false), f.literal("b", false)));
        assertThat(parser.parse("~a & b & ~c & d"))
                .isEqualTo(f.and(f.literal("a", false), f.variable("b"), f.literal("c", false), f.variable("d")));
        assertThat(parser.parse("a | b")).isEqualTo(f.or(f.variable("a"), f.variable("b")));
        assertThat(parser.parse("~a | ~b")).isEqualTo(f.or(f.literal("a", false), f.literal("b", false)));
        assertThat(parser.parse("~a | b | ~c | d"))
                .isEqualTo(f.or(f.literal("a", false), f.variable("b"), f.literal("c", false), f.variable("d")));
        assertThat(parser.parse("a => b")).isEqualTo(f.implication(f.variable("a"), f.variable("b")));
        assertThat(parser.parse("~a => ~b"))
                .isEqualTo(f.implication(f.literal("a", false), f.literal("b", false)));
        assertThat(parser.parse("a <=> b")).isEqualTo(f.equivalence(f.variable("a"), f.variable("b")));
        assertThat(parser.parse("~a <=> ~b"))
                .isEqualTo(f.equivalence(f.literal("a", false), f.literal("b", false)));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testParseMultiplication(CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("13 * abc = 4"))
                .isEqualTo(f.pbc(CType.EQ, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        assertThat(parser.parse("-13 * a = 4"))
                .isEqualTo(f.pbc(CType.EQ, 4, new Literal[]{f.variable("a")}, new int[]{-13}));
        assertThat(parser.parse("13 * ~abc = -442"))
                .isEqualTo(f.pbc(CType.EQ, -442, new Literal[]{f.literal("abc", false)}, new int[]{13}));
        assertThat(parser.parse("-13 * ~a = -442"))
                .isEqualTo(f.pbc(CType.EQ, -442, new Literal[]{f.literal("a", false)}, new int[]{-13}));
        assertThat(parser.parse("13 * abc = 4"))
                .isEqualTo(f.pbc(CType.EQ, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        assertThat(parser.parse("13 * abc > 4"))
                .isEqualTo(f.pbc(CType.GT, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        assertThat(parser.parse("13 * abc >= 4"))
                .isEqualTo(f.pbc(CType.GE, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        assertThat(parser.parse("13 * abc < 4"))
                .isEqualTo(f.pbc(CType.LT, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
        assertThat(parser.parse("13 * abc <= 4"))
                .isEqualTo(f.pbc(CType.LE, 4, new Literal[]{f.variable("abc")}, new int[]{13}));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testParseAddition(CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("4 * c + -4 * ~d < -4")).isEqualTo(
                f.pbc(CType.LT, -4, new Literal[]{f.variable("c"), f.literal("d", false)}, new int[]{4, -4}));
        assertThat(parser.parse("5 * c + -5 * ~c >= -5")).isEqualTo(
                f.pbc(CType.GE, -5, new Literal[]{f.variable("c"), f.literal("c", false)}, new int[]{5, -5}));
        assertThat(parser.parse("6 * a + -6 * ~b + 12 * ~c > -6")).isEqualTo(f.pbc(CType.GT, -6,
                new Literal[]{f.variable("a"), f.literal("b", false), f.literal("c", false)}, new int[]{6, -6, 12}));
        assertThat(parser.parse("c + -4 * ~d < -4")).isEqualTo(
                f.pbc(CType.LT, -4, new Literal[]{f.variable("c"), f.literal("d", false)}, new int[]{1, -4}));
        assertThat(parser.parse("5 * c + ~c >= -5"))
                .isEqualTo(f.pbc(CType.GE, -5, new Literal[]{f.variable("c"), f.literal("c", false)}, new int[]{5, 1}));
        assertThat(parser.parse("c + d >= -5"))
                .isEqualTo(f.pbc(CType.GE, -5, new Literal[]{f.variable("c"), f.literal("d", true)}, new int[]{1, 1}));
        assertThat(parser.parse("~c + ~d >= -5")).isEqualTo(
                f.pbc(CType.GE, -5, new Literal[]{f.literal("c", false), f.literal("d", false)}, new int[]{1, 1}));
        assertThat(parser.parse("~c = -5"))
                .isEqualTo(f.pbc(CType.EQ, -5, new Literal[]{f.literal("c", false)}, new int[]{1}));
        assertThat(parser.parse("~(c = -5)"))
                .isEqualTo(f.not(f.pbc(CType.EQ, -5, new Literal[]{f.literal("c", true)}, new int[]{1})));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testCombination(CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final PropositionalParser parser = new PropositionalParser(f);
        final Formula pbc = f.pbc(CType.GT, -6,
                new Literal[]{f.variable("a"), f.literal("b", false), f.literal("c", false)}, new int[]{6, -6, 12});
        assertThat(parser.parse("(x => y & z) & (6 * a + -6 * ~b + 12 * ~c > -6)"))
                .isEqualTo(f.and(f.implication(f.variable("x"), f.and(f.variable("y"), f.variable("z"))), pbc));
        assertThat(parser.parse("~(6 * a - 6 * ~b - -12 * ~c > -6)")).isEqualTo(f.not(pbc));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testParsePrecedences(CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("x | y & z"))
                .isEqualTo(f.or(f.variable("x"), f.and(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x & y | z"))
                .isEqualTo(f.or(f.and(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x => y & z"))
                .isEqualTo(f.implication(f.variable("x"), f.and(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x & y => z"))
                .isEqualTo(f.implication(f.and(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x <=> y & z"))
                .isEqualTo(f.equivalence(f.variable("x"), f.and(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x & y <=> z"))
                .isEqualTo(f.equivalence(f.and(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x => y | z"))
                .isEqualTo(f.implication(f.variable("x"), f.or(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x | y => z"))
                .isEqualTo(f.implication(f.or(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x <=> y | z"))
                .isEqualTo(f.equivalence(f.variable("x"), f.or(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x | y <=> z"))
                .isEqualTo(f.equivalence(f.or(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x => y => z"))
                .isEqualTo(f.implication(f.variable("x"), f.implication(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x <=> y <=> z"))
                .isEqualTo(f.equivalence(f.variable("x"), f.equivalence(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("(x | y) & z"))
                .isEqualTo(f.and(f.or(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x & (y | z)"))
                .isEqualTo(f.and(f.variable("x"), f.or(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("(x => y) & z"))
                .isEqualTo(f.and(f.implication(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x & (y => z)"))
                .isEqualTo(f.and(f.variable("x"), f.implication(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("(x => y) | z"))
                .isEqualTo(f.or(f.implication(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x | (y => z)"))
                .isEqualTo(f.or(f.variable("x"), f.implication(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("(x <=> y) & z"))
                .isEqualTo(f.and(f.equivalence(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x & (y <=> z)"))
                .isEqualTo(f.and(f.variable("x"), f.equivalence(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("(x <=> y) | z"))
                .isEqualTo(f.or(f.equivalence(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x | (y <=> z)"))
                .isEqualTo(f.or(f.variable("x"), f.equivalence(f.variable("y"), f.variable("z"))));
        assertThat(parser.parse("x => y <=> z"))
                .isEqualTo(f.equivalence(f.implication(f.variable("x"), f.variable("y")), f.variable("z")));
        assertThat(parser.parse("x => (y <=> z)"))
                .isEqualTo(f.implication(f.variable("x"), f.equivalence(f.variable("y"), f.variable("z"))));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void parseEmptyString(CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("")).isEqualTo(f.verum());
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testSkipSymbols(CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse(" ")).isEqualTo(f.verum());
        assertThat(parser.parse("\t")).isEqualTo(f.verum());
        assertThat(parser.parse("\n")).isEqualTo(f.verum());
        assertThat(parser.parse("\r")).isEqualTo(f.verum());
        assertThat(parser.parse(" \r\n\n  \t")).isEqualTo(f.verum());
        assertThat(parser.parse("a\n&\tb")).isEqualTo(f.and(f.variable("a"), f.variable("b")));
        assertThat(parser.parse(" a\r=>\t\tb")).isEqualTo(f.implication(f.variable("a"), f.variable("b")));
        assertThat(parser.parse(" 2\n*a\r+\n\n-4*\tb    +3*x=2"))
                .isEqualTo(f.pbc(CType.EQ, 2, List.of(f.variable("a"), f.variable("b"), f.variable("x")), List.of(2, -4, 3)));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testNumberLiterals(CspFactory cf) throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser parser = new PropositionalParser(f);
        assertThat(parser.parse("12 & A")).isEqualTo(f.and(f.variable("12"), f.variable("A")));
        assertThat(parser.parse("~12 & A")).isEqualTo(f.and(f.literal("12", false), f.variable("A")));
        assertThat(parser.parse("12 * 12 + 13 * A + 10 * B <= 25")).isEqualTo(f.pbc(CType.LE, 25,
                new Literal[]{f.variable("12"), f.variable("A"), f.variable("B")}, new int[]{12, 13, 10}));
        assertThat(parser.parse("-12 * ~12 + 13 * A + 10 * B <= 25")).isEqualTo(f.pbc(CType.LE, 25,
                new Literal[]{f.literal("12", false), f.variable("A"), f.variable("B")}, new int[]{-12, 13, 10}));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testFormulaGetFactoryParser(CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        assertThat(cf.parseFormula("a & b")).isEqualTo(f.and(f.variable("a"), f.variable("b")));
        assertThat(cf.parseFormula("2*a + -4*b + 3*x = 2"))
                .isEqualTo(f.pbc(CType.EQ, 2, List.of(f.variable("a"), f.variable("b"), f.variable("x")), List.of(2, -4, 3)));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalVariable1(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("$$%")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalVariable3(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula(";;23")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalVariable4(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("{0}")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalOperator1(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("A + B")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalOperator2(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("A &")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalOperator3(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("A /")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalOperator4(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("-A")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalOperator5(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("A * B")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalBrackets1(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("(A & B")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalFormula1(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("((A & B)")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalFormula2(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("(A & (C & D) B)"))
                .isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalFormula3(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("A | A + (C | B + C)"))
                .isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalFormula4(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("A | A & (C | B & C"))
                .isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalFormula5(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("A & ~B)")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalFormula6(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("12)")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalFormula7(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("ab@cd)")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testIllegalSkipPosition(CspFactory cf) {
        assertThatThrownBy(() -> new CspParser(cf).parseFormula("- 1*x <= 3")).isInstanceOf(ParserException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testCompareWithFiles(CspFactory cf) throws ParserException, IOException {
        FormulaFactory f = cf.getFormulaFactory();
        CspParser p = new CspParser(cf);
        for(final File file : Objects.requireNonNull(new File("../test_files/formulas/").listFiles())) {
            Formula formula1 = CspReader.readCsp(p, file);
            Formula formula2 = FormulaReader.readFormula(f, file);
            assertThat(formula1).isEqualTo(formula2);
        }
    }
}
