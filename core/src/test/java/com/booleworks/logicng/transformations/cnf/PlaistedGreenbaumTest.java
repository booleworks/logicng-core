// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import static com.booleworks.logicng.testutils.TestUtil.equivalentModels;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class PlaistedGreenbaumTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final PlaistedGreenbaumTransformation pg = new PlaistedGreenbaumTransformation(_c.f, 0);

        assertThat(_c.verum.transform(pg)).isEqualTo(_c.verum);
        assertThat(_c.falsum.transform(pg)).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final PlaistedGreenbaumTransformation pg = new PlaistedGreenbaumTransformation(_c.f, 0);

        assertThat(_c.a.transform(pg)).isEqualTo(_c.a);
        assertThat(_c.na.transform(pg)).isEqualTo(_c.na);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryOperators(final FormulaContext _c) {
        final PlaistedGreenbaumTransformation pg = new PlaistedGreenbaumTransformation(_c.f, 0);

        assertThat(_c.imp1.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.imp1, _c.imp1.transform(pg), _c.imp1.variables(_c.f))).isTrue();
        assertThat(_c.imp2.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.imp2, _c.imp2.transform(pg), _c.imp2.variables(_c.f))).isTrue();
        assertThat(_c.imp3.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.imp3, _c.imp3.transform(pg), _c.imp3.variables(_c.f))).isTrue();
        assertThat(_c.eq1.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.eq1, _c.eq1.transform(pg), _c.eq1.variables(_c.f))).isTrue();
        assertThat(_c.eq2.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.eq2, _c.eq2.transform(pg), _c.eq2.variables(_c.f))).isTrue();
        assertThat(_c.eq3.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.eq3, _c.eq3.transform(pg), _c.eq3.variables(_c.f))).isTrue();
        assertThat(_c.eq4.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.eq4, _c.eq4.transform(pg), _c.eq4.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperators(final FormulaContext _c) throws ParserException {
        final PlaistedGreenbaumTransformation pg = new PlaistedGreenbaumTransformation(_c.f, 0);

        assertThat(_c.and1.transform(pg)).isEqualTo(_c.and1);
        assertThat(_c.or1.transform(pg)).isEqualTo(_c.or1);
        final Formula f1 = _c.p.parse("(a & b & x) | (c & d & ~y)");
        final Formula f2 = _c.p.parse("(a & b & x) | (c & d & ~y) | (~z | (c & d & ~y)) ");
        final Formula f3 = _c.p.parse("a | b | (~x & ~y)");
        assertThat(f1.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f1, f1.transform(pg), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f2, f2.transform(pg), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f3, f3.transform(pg), f3.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNotNary(final FormulaContext _c) throws ParserException {
        final PlaistedGreenbaumTransformation pg = new PlaistedGreenbaumTransformation(_c.f, 0);

        assertThat(_c.p.parse("~a").transform(pg)).isEqualTo(_c.p.parse("~a"));
        assertThat(_c.p.parse("~~a").transform(pg)).isEqualTo(_c.p.parse("a"));
        final Formula f0 = _c.p.parse("~(~a | b)");
        final Formula f1 = _c.p.parse("~((a | b) | ~(x | y))");
        final Formula f2 = _c.p.parse("~(a & b | ~a & ~b)");
        final Formula f3 = _c.p.parse("~(~(a | b) & ~(x | y) | (a | b) & (x | y))");
        final Formula f4 = _c.p.parse("~(a & b & ~x & ~y)");
        final Formula f5 = _c.p.parse("~(a | b | ~x | ~y)");
        final Formula f6 = _c.p.parse("~(a & b) & (c | (a & b))");
        assertThat(f0.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f0, f0.transform(pg), f0.variables(_c.f))).isTrue();
        assertThat(f1.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f1, f1.transform(pg), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f2, f2.transform(pg), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f3, f3.transform(pg), f3.variables(_c.f))).isTrue();
        assertThat(f4.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f4, f4.transform(pg), f4.variables(_c.f))).isTrue();
        assertThat(f5.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f5, f5.transform(pg), f5.variables(_c.f))).isTrue();
        assertThat(f5.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f5, f5.transform(pg), f5.variables(_c.f))).isTrue();
        assertThat(f6.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f6, f6.transform(pg), f6.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNotBinary(final FormulaContext _c) throws ParserException {
        final PlaistedGreenbaumTransformation pg = new PlaistedGreenbaumTransformation(_c.f, 0);

        assertThat(_c.p.parse("~a").transform(pg)).isEqualTo(_c.p.parse("~a"));
        assertThat(_c.p.parse("~~a").transform(pg)).isEqualTo(_c.p.parse("a"));
        final Formula f1 = _c.p.parse("~(~(a | b) => ~(x | y))");
        final Formula f2 = _c.p.parse("~(a <=> b)");
        final Formula f3 = _c.p.parse("~(~(a | b) <=> ~(x | y))");
        assertThat(f1.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f1, f1.transform(pg), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f2, f2.transform(pg), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f3, f3.transform(pg), f3.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCC(final FormulaContext _c) throws ParserException {
        final PlaistedGreenbaumTransformation pg = new PlaistedGreenbaumTransformation(_c.f, 0);

        assertThat(_c.p.parse("a <=> (1 * b <= 1)").transform(pg)).isEqualTo(_c.p.parse("a"));
        assertThat(_c.p.parse("~(1 * b <= 1)").transform(pg)).isEqualTo(_c.p.parse("$false"));
        assertThat(_c.p.parse("(1 * b + 1 * c + 1 * d <= 1)").transform(pg))
                .isEqualTo(_c.p.parse("(~b | ~c) & (~b | ~d) & (~c | ~d)"));
        assertThat(_c.p.parse("~(1 * b + 1 * c + 1 * d <= 1)").transform(pg)).isEqualTo(_c.p.parse(
                String.format(
                        "(d | @AUX_%1$s_CC_1 | @AUX_%1$s_CC_4) & (~@AUX_%1$s_CC_3 | @AUX_%1$s_CC_1 | @AUX_%1$s_CC_4) & (~@AUX_%1$s_CC_3 | d | @AUX_%1$s_CC_4)" +
                                " & (~@AUX_%1$s_CC_4 |" +
                                "@AUX_%1$s_CC_0) & (~@AUX_%1$s_CC_2 | @AUX_%1$s_CC_0) & (~@AUX_%1$s_CC_4 | ~@AUX_%1$s_CC_2) & (c | @AUX_%1$s_CC_3 | " +
                                "@AUX_%1$s_CC_5) & (b | @AUX_%1$s_CC_3" +
                                " | @AUX_%1$s_CC_5) & (b | c | @AUX_%1$s_CC_5) & (~@AUX_%1$s_CC_5 | @AUX_%1$s_CC_2) & ~@AUX_%1$s_CC_0", _c.f.getName())));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFormulas(final FormulaContext _c) throws ParserException {
        final PlaistedGreenbaumTransformation pg = new PlaistedGreenbaumTransformation(_c.f, 0);

        final Formula f1 = _c.p.parse("(a | b) => c");
        final Formula f2 = _c.p.parse("~x & ~y");
        final Formula f3 = _c.p.parse("d & ((a | b) => c)");
        final Formula f4 = _c.p.parse("d & ((a | b) => c) | ~x & ~y");
        assertThat(f1.transform(pg))
                .isEqualTo(_c.p.parse(String.format("(@AUX_%1$s_CNF_1 | c) & (~@AUX_%1$s_CNF_1 | ~a) & (~@AUX_%1$s_CNF_1 | ~b)", _c.f.getName())));
        assertThat(f2.transform(pg)).isEqualTo(_c.p.parse("~x & ~y"));
        assertThat(f3.transform(pg))
                .isEqualTo(_c.p.parse(String.format("d & @AUX_%1$s_CNF_0 & (~@AUX_%1$s_CNF_0 | @AUX_%1$s_CNF_1 | c) & " +
                        "(~@AUX_%1$s_CNF_1 | ~a) & (~@AUX_%1$s_CNF_1 | ~b)", _c.f.getName())));
        assertThat(f4.transform(pg))
                .isEqualTo(_c.p.parse(String.format("(@AUX_%1$s_CNF_2 | @AUX_%1$s_CNF_4) & (~@AUX_%1$s_CNF_2 | d) & " +
                        "(~@AUX_%1$s_CNF_2 | @AUX_%1$s_CNF_0) & (~@AUX_%1$s_CNF_0 | @AUX_%1$s_CNF_1 | c) & " +
                        "(~@AUX_%1$s_CNF_1 | ~a) & (~@AUX_%1$s_CNF_1 | ~b) & (~@AUX_%1$s_CNF_4 | ~x) & " +
                        "(~@AUX_%1$s_CNF_4 | ~y)", _c.f.getName())));
        assertThat(f1.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f1, f1.transform(pg), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f2, f2.transform(pg), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f3, f3.transform(pg), f3.variables(_c.f))).isTrue();
        assertThat(f4.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f4, f4.transform(pg), f4.variables(_c.f))).isTrue();
        assertThat(f4.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f4, f4.transform(pg), f4.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFactorization(final FormulaContext _c) throws ParserException {
        final PlaistedGreenbaumTransformation pg = new PlaistedGreenbaumTransformation(_c.f);

        final Formula f1 = _c.p.parse("(a | b) => c");
        final Formula f2 = _c.p.parse("~x & ~y");
        final Formula f3 = _c.p.parse("d & ((a | b) => c)");
        final Formula f4 = _c.p.parse("d & ((a | b) => c) | ~x & ~y");
        assertThat(f1.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(f1.transform(pg).variables(_c.f).size()).isEqualTo(f1.variables(_c.f).size());
        assertThat(f2.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(f2.transform(pg).variables(_c.f).size()).isEqualTo(f2.variables(_c.f).size());
        assertThat(f3.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(f3.transform(pg).variables(_c.f).size()).isEqualTo(f3.variables(_c.f).size());
        assertThat(f4.transform(pg).isCNF(_c.f)).isTrue();
        assertThat(f4.transform(pg).variables(_c.f).size()).isEqualTo(f4.variables(_c.f).size());
    }

    @Test
    public void testToString() {
        final PlaistedGreenbaumTransformation pGTransformation =
                new PlaistedGreenbaumTransformation(FormulaFactory.caching(), 5);
        assertThat(pGTransformation.toString()).isEqualTo("PlaistedGreenbaumTransformation{boundary=5}");
    }
}
