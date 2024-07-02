// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import com.booleworks.logicng.testutils.TestUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BDDCNFTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final BDDCNFTransformation bddcnf = new BDDCNFTransformation(_c.f);

        assertThat(_c.verum.transform(bddcnf)).isEqualTo(_c.verum);
        assertThat(_c.falsum.transform(bddcnf)).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final BDDCNFTransformation bddcnf = new BDDCNFTransformation(_c.f);

        assertThat(_c.a.transform(bddcnf)).isEqualTo(_c.a);
        assertThat(_c.na.transform(bddcnf)).isEqualTo(_c.na);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryOperators(final FormulaContext _c) {
        final BDDCNFTransformation bddcnf = new BDDCNFTransformation(_c.f);

        assertThat(_c.imp1.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(_c.imp1, _c.imp1.transform(bddcnf), _c.imp1.variables(_c.f)))
                .isTrue();
        assertThat(_c.imp2.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(_c.imp2, _c.imp2.transform(bddcnf), _c.imp2.variables(_c.f)))
                .isTrue();
        assertThat(_c.imp3.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(_c.imp3, _c.imp3.transform(bddcnf), _c.imp3.variables(_c.f)))
                .isTrue();
        assertThat(_c.eq1.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(_c.eq1, _c.eq1.transform(bddcnf), _c.eq1.variables(_c.f)))
                .isTrue();
        assertThat(_c.eq2.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(_c.eq2, _c.eq2.transform(bddcnf), _c.eq2.variables(_c.f)))
                .isTrue();
        assertThat(_c.eq3.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(_c.eq3, _c.eq3.transform(bddcnf), _c.eq3.variables(_c.f)))
                .isTrue();
        assertThat(_c.eq4.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(_c.eq4, _c.eq4.transform(bddcnf), _c.eq4.variables(_c.f)))
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperators(final FormulaContext _c) throws ParserException {
        final BDDCNFTransformation bddcnf = new BDDCNFTransformation(_c.f);

        assertThat(_c.and1.transform(bddcnf)).isEqualTo(_c.and1);
        assertThat(_c.or1.transform(bddcnf)).isEqualTo(_c.or1);
        final Formula f1 = _c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)");
        final Formula f2 = _c.p.parse("~(a & b) | c | ~(x | ~y)");
        final Formula f3 = _c.p.parse("a | b | (~x & ~y)");
        assertThat(f1.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f1, f1.transform(bddcnf), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f2, f2.transform(bddcnf), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f3, f3.transform(bddcnf), f3.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperatorsWithExternalFactory(final FormulaContext _c) throws ParserException {
        final BDDCNFTransformation bddcnf = new BDDCNFTransformation(_c.f);

        final BDDCNFTransformation transformation = new BDDCNFTransformation(_c.f, new BDDKernel(_c.f, 7, 100, 1000));
        assertThat(_c.and1.transform(bddcnf)).isEqualTo(_c.and1);
        assertThat(_c.or1.transform(bddcnf)).isEqualTo(_c.or1);
        final Formula f1 = _c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)");
        final Formula f2 = _c.p.parse("~(a & b) | c | ~(x | ~y)");
        final Formula f3 = _c.p.parse("a | b | (~x & ~y)");
        assertThat(f1.transform(transformation).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f1, f1.transform(transformation), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(transformation).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f2, f2.transform(transformation), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(transformation).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f3, f3.transform(transformation), f3.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperatorsWithExternalFactory2(final FormulaContext _c) throws ParserException {
        final BDDCNFTransformation bddcnf = new BDDCNFTransformation(_c.f);

        final BDDCNFTransformation transformation = new BDDCNFTransformation(_c.f, new BDDKernel(_c.f, 7, 50, 50));
        assertThat(_c.and1.transform(bddcnf)).isEqualTo(_c.and1);
        assertThat(_c.or1.transform(bddcnf)).isEqualTo(_c.or1);
        final Formula f1 = _c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)");
        final Formula f2 = _c.p.parse("~(a & b) | c | ~(x | ~y)");
        final Formula f3 = _c.p.parse("a | b | (~x & ~y)");
        assertThat(f1.transform(transformation).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f1, f1.transform(transformation), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(transformation).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f2, f2.transform(transformation), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(transformation).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f3, f3.transform(transformation), f3.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        final BDDCNFTransformation bddcnf = new BDDCNFTransformation(_c.f);

        assertThat(_c.p.parse("~a").transform(bddcnf)).isEqualTo(_c.p.parse("~a"));
        assertThat(_c.p.parse("~~a").transform(bddcnf)).isEqualTo(_c.p.parse("a"));
        assertThat(_c.p.parse("~(a => b)").transform(bddcnf)).isEqualTo(_c.p.parse("a & ~b"));
        final Formula f1 = _c.p.parse("~(~(a | b) => ~(x | y))");
        final Formula f2 = _c.p.parse("~(a <=> b)");
        final Formula f3 = _c.p.parse("~(~(a | b) <=> ~(x | y))");
        final Formula f4 = _c.p.parse("~(a & b & ~x & ~y)");
        final Formula f5 = _c.p.parse("~(a | b | ~x | ~y)");
        assertThat(f1.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f1, f1.transform(bddcnf), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f2, f2.transform(bddcnf), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f3, f3.transform(bddcnf), f3.variables(_c.f))).isTrue();
        assertThat(f4.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f4, f4.transform(bddcnf), f4.variables(_c.f))).isTrue();
        assertThat(f5.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f5, f5.transform(bddcnf), f5.variables(_c.f))).isTrue();
        assertThat(f5.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f5, f5.transform(bddcnf), f5.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCC(final FormulaContext _c) throws ParserException {
        final BDDCNFTransformation bddcnf = new BDDCNFTransformation(_c.f);

        final Formula f1 = _c.p.parse("a <=> (1 * b <= 0)");
        final Formula f2 = _c.p.parse("~(1 * b <= 1)");
        final Formula f3 = _c.p.parse("(1 * b + 1 * c + 1 * d <= 1)");
        final Formula f4 = _c.p.parse("~(1 * b + 1 * c + 1 * d <= 1)");
        assertThat(f1.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f1, f1.transform(bddcnf), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f2, f2.transform(bddcnf), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f3, f3.transform(bddcnf), f3.variables(_c.f))).isTrue();
        assertThat(f4.transform(bddcnf).isCNF(_c.f)).isTrue();
        assertThat(TestUtil.equivalentModels(f4, f4.transform(bddcnf), f4.variables(_c.f))).isTrue();
    }
}
