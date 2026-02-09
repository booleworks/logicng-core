// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.functions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.io.parsers.CspParser;
import com.booleworks.logicng.csp.io.readers.CspReader;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

public class CspRestrictionFunctionTest extends ParameterizedCspTest {

    private CspAssignment generateAssignment(final CspFactory cf) {
        final CspAssignment assignment = new CspAssignment();
        assignment.addIntAssignment(cf.variable("a", 0, 10), 2);
        assignment.addIntAssignment(cf.variable("b", 0, 10), 10);
        assignment.addIntAssignment(cf.variable("c", -5, 5), 2);
        assignment.addLiteral(cf.getFormulaFactory().literal("A", true));
        assignment.addLiteral(cf.getFormulaFactory().literal("B", false));
        return assignment;
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testSimpleFormulas(final CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final CspParser p = new CspParser(cf);
        final CspAssignment restrictions = generateAssignment(cf);
        final IntegerVariable d = cf.variable("d", 0, 5);
        final CspRestrictionFunction function = new CspRestrictionFunction(cf, restrictions);
        assertThat(f.verum().transform(function)).isEqualTo(f.verum());
        assertThat(f.falsum().transform(function)).isEqualTo(f.falsum());
        assertThat(f.variable("A").transform(function)).isEqualTo(f.verum());
        assertThat(f.variable("B").transform(function)).isEqualTo(f.falsum());
        assertThat(f.variable("C").transform(function)).isEqualTo(f.variable("C"));
        assertThat(p.parseFormula("C & A").transform(function)).isEqualTo(f.variable("C"));
        assertThat(p.parseFormula("[a = 2]").transform(function)).isEqualTo(cf.eq(cf.constant(2), cf.constant(2)));
        assertThat(p.parseFormula("[a < d]").transform(function)).isEqualTo(cf.lt(cf.constant(2), d));
        assertThat(p.parseFormula("ALLDIFFERENT[a, b, c, d]").transform(function))
                .isEqualTo(p.parseFormula("ALLDIFFERENT[2, 10, 2, d]"));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testTerms(final CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final CspParser p = new CspParser(cf);
        final CspAssignment restrictions = generateAssignment(cf);
        final IntegerVariable d = cf.variable("d", 0, 5);
        final CspRestrictionFunction function = new CspRestrictionFunction(cf, restrictions);
        assertThat(p.parseFormula("EQ[abs(a), abs(d)]").transform(function))
                .isEqualTo(p.parseFormula("EQ[abs(2), abs(d)]"));
        assertThat(p.parseFormula("EQ[neg(a), neg(d)]").transform(function))
                .isEqualTo(p.parseFormula("EQ[-2, -d]"));
        assertThat(p.parseFormula("EQ[div(a, 2), div(d, 3)]").transform(function))
                .isEqualTo(p.parseFormula("EQ[div(2, 2), div(d, 3)]"));
        assertThat(p.parseFormula("EQ[mod(a, 2), mod(d, 3)]").transform(function))
                .isEqualTo(p.parseFormula("EQ[mod(2, 2), mod(d, 3)]"));
        assertThat(p.parseFormula("EQ[mul(2, a), mul(b, d)]").transform(function))
                .isEqualTo(p.parseFormula("EQ[mul(2, 2), mul(10, d)]"));
        assertThat(p.parseFormula("EQ[add(a, b), add(c, d)]").transform(function))
                .isEqualTo(p.parseFormula("EQ[add(2, 10), add(2, d)]"));
        assertThat(p.parseFormula("EQ[sub(a, b), sub(c, d)]").transform(function))
                .isEqualTo(p.parseFormula("EQ[sub(2, 10), sub(2, d)]"));
        assertThat(p.parseFormula("EQ[max(a, b), max(c, d)]").transform(function))
                .isEqualTo(p.parseFormula("EQ[max(2, 10), max(2, d)]"));
        assertThat(p.parseFormula("EQ[min(a, b), min(c, d)]").transform(function))
                .isEqualTo(p.parseFormula("EQ[min(2, 10), min(2, d)]"));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testInvalidRestriction(final CspFactory cf) throws ParserException, IOException {
        final FormulaFactory f = cf.getFormulaFactory();
        final Formula formula = CspReader.readCsp(cf, "../test_files/csp/simple1.csp");
        final CspAssignment restrictions = new CspAssignment();
        restrictions.addIntAssignment(cf.getVariable("a"), 11);
        assertThrows(IllegalArgumentException.class,
                () -> formula.transform(new CspRestrictionFunction(cf, restrictions)));
    }
}
