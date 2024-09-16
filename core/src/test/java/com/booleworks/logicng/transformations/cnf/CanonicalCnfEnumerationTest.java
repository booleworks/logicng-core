// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.predicates.CnfPredicate;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import com.booleworks.logicng.util.FormulaCornerCases;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CanonicalCnfEnumerationTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSamples(final FormulaContext _c) throws ParserException {
        assertThat(_c.f.falsum().transform(new CanonicalCnfEnumeration(_c.f))).isEqualTo(_c.f.parse("$false"));
        assertThat(_c.f.verum().transform(new CanonicalCnfEnumeration(_c.f))).isEqualTo(_c.f.parse("$true"));
        assertThat(_c.f.parse("a").transform(new CanonicalCnfEnumeration(_c.f))).isEqualTo(_c.f.parse("a"));
        assertThat(_c.f.parse("~a").transform(new CanonicalCnfEnumeration(_c.f))).isEqualTo(_c.f.parse("~a"));
        assertThat(_c.f.parse("~a & b").transform(new CanonicalCnfEnumeration(_c.f)))
                .isEqualTo(_c.f.parse("(~a | b) & (~a | ~b) & (a | b)"));
        assertThat(_c.f.parse("~a | b").transform(new CanonicalCnfEnumeration(_c.f))).isEqualTo(_c.f.parse("~a | b"));
        assertThat(_c.f.parse("a => b").transform(new CanonicalCnfEnumeration(_c.f))).isEqualTo(_c.f.parse("~a | b"));
        assertThat(_c.f.parse("a <=> b").transform(new CanonicalCnfEnumeration(_c.f)))
                .isEqualTo(_c.f.parse("(~a | b) & (a | ~b)"));
        assertThat(_c.f.parse("a + b = 1").transform(new CanonicalCnfEnumeration(_c.f)))
                .isEqualTo(_c.f.parse("(a | b) & (~a | ~b)"));
        assertThat(_c.f.parse("a & (b | ~c)").transform(new CanonicalCnfEnumeration(_c.f)))
                .isEqualTo(_c.f.parse("(a | b | c) & (a | b | ~c) & (a | ~b | c) & (a | ~b | ~c) & (~a | b | ~c)"));
        assertThat(_c.f.parse("a & b & (~a | ~b)").transform(new CanonicalCnfEnumeration(_c.f)))
                .isEqualTo(_c.f.parse("(a | b) & (~a | b) & (~a | ~b) & (a | ~b)"));
        assertThat(_c.f.parse("a | b | ~a & ~b").transform(new CanonicalCnfEnumeration(_c.f)))
                .isEqualTo(_c.f.parse("$true"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCornerCases(final FormulaContext _c) {
        final FormulaCornerCases cornerCases = new FormulaCornerCases(_c.f);
        for (final Formula formula : cornerCases.cornerCases()) {
            test(formula);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void random(final FormulaContext _c) {
        final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f,
                FormulaRandomizerConfig.builder().numVars(5).weightPbc(0.5).seed(42).build());
        for (int i = 0; i < 1000; i++) {
            final Formula formula = randomizer.formula(3);
            test(formula);
        }
    }

    private void test(final Formula formula) {
        final FormulaFactory f = formula.getFactory();
        final Formula cnf = new CanonicalCnfEnumeration(f).apply(formula);
        assertThat(cnf.holds(new CnfPredicate(f))).isTrue();
        assertThat(f.equivalence(formula, cnf).holds(new TautologyPredicate(f))).isTrue();
        if (formula.holds(new TautologyPredicate(f))) {
            assertThat(cnf).isEqualTo(f.verum());
        } else {
            assertThat(hasConstantTermSize(cnf)).isTrue();
        }
    }

    private static boolean hasConstantTermSize(final Formula cnf) {
        switch (cnf.getType()) {
            case LITERAL:
            case TRUE:
            case FALSE:
            case OR:
                return true;
            case AND:
                return cnf.stream().map(Formula::numberOfOperands).distinct().count() == 1L;
            default:
                throw new IllegalStateException("Unexpected type: " + cnf.getType());
        }
    }
}
