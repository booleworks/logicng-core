// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.dnf;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import com.booleworks.logicng.predicates.DNFPredicate;
import com.booleworks.logicng.testutils.TestUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BDDDNFTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final BDDDNFTransformation bdddnf = new BDDDNFTransformation(_c.f);

        assertThat(_c.verum.transform(bdddnf)).isEqualTo(_c.verum);
        assertThat(_c.falsum.transform(bdddnf)).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final BDDDNFTransformation bdddnf = new BDDDNFTransformation(_c.f);

        assertThat(_c.a.transform(bdddnf)).isEqualTo(_c.a);
        assertThat(_c.na.transform(bdddnf)).isEqualTo(_c.na);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryOperators(final FormulaContext _c) {
        final DNFPredicate dnfPredicate = new DNFPredicate(_c.f);
        final BDDDNFTransformation bdddnf = new BDDDNFTransformation(_c.f);

        assertThat(_c.imp1.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(_c.imp1, _c.imp1.transform(bdddnf), _c.imp1.variables(_c.f))).isTrue();
        assertThat(_c.imp2.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(_c.imp2, _c.imp2.transform(bdddnf), _c.imp2.variables(_c.f))).isTrue();
        assertThat(_c.imp3.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(_c.imp3, _c.imp3.transform(bdddnf), _c.imp3.variables(_c.f))).isTrue();
        assertThat(_c.eq1.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(_c.eq1, _c.eq1.transform(bdddnf), _c.eq1.variables(_c.f))).isTrue();
        assertThat(_c.eq2.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(_c.eq2, _c.eq2.transform(bdddnf), _c.eq2.variables(_c.f))).isTrue();
        assertThat(_c.eq3.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(_c.eq3, _c.eq3.transform(bdddnf), _c.eq3.variables(_c.f))).isTrue();
        assertThat(_c.eq4.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(_c.eq4, _c.eq4.transform(bdddnf), _c.eq4.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperators(final FormulaContext _c) throws ParserException {
        final DNFPredicate dnfPredicate = new DNFPredicate(_c.f);
        final BDDDNFTransformation bdddnf = new BDDDNFTransformation(_c.f);

        assertThat(_c.and1.transform(bdddnf)).isEqualTo(_c.and1);
        assertThat(_c.or1.transform(bdddnf)).isEqualTo(_c.or1);
        final Formula f1 = _c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)");
        final Formula f2 = _c.p.parse("~(a & b) | c | ~(x | ~y)");
        final Formula f3 = _c.p.parse("a | b | (~x & ~y)");
        assertThat(f1.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f1, f1.transform(bdddnf), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f2, f2.transform(bdddnf), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f3, f3.transform(bdddnf), f3.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperatorsWithExternalFactory(final FormulaContext _c) throws ParserException {
        final DNFPredicate dnfPredicate = new DNFPredicate(_c.f);
        final BDDDNFTransformation bdddnf = new BDDDNFTransformation(_c.f);

        final BDDDNFTransformation transformation = new BDDDNFTransformation(_c.f, new BDDKernel(_c.f, 7, 100, 1000));
        assertThat(_c.and1.transform(bdddnf)).isEqualTo(_c.and1);
        assertThat(_c.or1.transform(bdddnf)).isEqualTo(_c.or1);
        final Formula f1 = _c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)");
        final Formula f2 = _c.p.parse("~(a & b) | c | ~(x | ~y)");
        final Formula f3 = _c.p.parse("a | b | (~x & ~y)");
        assertThat(f1.transform(transformation).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f1, f1.transform(transformation), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(transformation).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f2, f2.transform(transformation), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(transformation).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f3, f3.transform(transformation), f3.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperatorsWithExternalFactory2(final FormulaContext _c) throws ParserException {
        final DNFPredicate dnfPredicate = new DNFPredicate(_c.f);
        final BDDDNFTransformation bdddnf = new BDDDNFTransformation(_c.f);

        final BDDDNFTransformation transformation = new BDDDNFTransformation(_c.f, new BDDKernel(_c.f, 7, 50, 50));
        assertThat(_c.and1.transform(bdddnf)).isEqualTo(_c.and1);
        assertThat(_c.or1.transform(bdddnf)).isEqualTo(_c.or1);
        final Formula f1 = _c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)");
        final Formula f2 = _c.p.parse("~(a & b) | c | ~(x | ~y)");
        final Formula f3 = _c.p.parse("a | b | (~x & ~y)");
        assertThat(f1.transform(transformation).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f1, f1.transform(transformation), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(transformation).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f2, f2.transform(transformation), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(transformation).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f3, f3.transform(transformation), f3.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        final DNFPredicate dnfPredicate = new DNFPredicate(_c.f);
        final BDDDNFTransformation bdddnf = new BDDDNFTransformation(_c.f);

        Assertions.assertThat(_c.p.parse("~a").transform(bdddnf)).isEqualTo(_c.p.parse("~a"));
        Assertions.assertThat(_c.p.parse("~~a").transform(bdddnf)).isEqualTo(_c.p.parse("a"));
        Assertions.assertThat(_c.p.parse("~(a => b)").transform(bdddnf)).isEqualTo(_c.p.parse("a & ~b"));
        final Formula f1 = _c.p.parse("~(~(a | b) => ~(x | y))");
        final Formula f2 = _c.p.parse("~(a <=> b)");
        final Formula f3 = _c.p.parse("~(~(a | b) <=> ~(x | y))");
        final Formula f4 = _c.p.parse("~(a & b & ~x & ~y)");
        final Formula f5 = _c.p.parse("~(a | b | ~x | ~y)");
        assertThat(f1.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f1, f1.transform(bdddnf), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f2, f2.transform(bdddnf), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f3, f3.transform(bdddnf), f3.variables(_c.f))).isTrue();
        assertThat(f4.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f4, f4.transform(bdddnf), f4.variables(_c.f))).isTrue();
        assertThat(f5.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f5, f5.transform(bdddnf), f5.variables(_c.f))).isTrue();
        assertThat(f5.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f5, f5.transform(bdddnf), f5.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCC(final FormulaContext _c) throws ParserException {
        final DNFPredicate dnfPredicate = new DNFPredicate(_c.f);
        final BDDDNFTransformation bdddnf = new BDDDNFTransformation(_c.f);

        final PropositionalParser p = new PropositionalParser(_c.f);
        final Formula f1 = p.parse("a <=> (1 * b <= 1)");
        final Formula f2 = p.parse("~(1 * b <= 1)");
        final Formula f3 = p.parse("(1 * b + 1 * c + 1 * d <= 1)");
        final Formula f4 = p.parse("~(1 * b + 1 * c + 1 * d <= 1)");
        assertThat(f1.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f1, f1.transform(bdddnf), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f2, f2.transform(bdddnf), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f3, f3.transform(bdddnf), f3.variables(_c.f))).isTrue();
        assertThat(f4.transform(bdddnf).holds(dnfPredicate)).isTrue();
        Assertions.assertThat(TestUtil.equivalentModels(f4, f4.transform(bdddnf), f4.variables(_c.f))).isTrue();
    }
}
