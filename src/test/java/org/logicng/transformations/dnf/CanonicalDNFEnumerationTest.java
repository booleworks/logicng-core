// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.dnf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.RandomTag;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.io.parsers.ParserException;
import org.logicng.predicates.DNFPredicate;
import org.logicng.predicates.satisfiability.ContradictionPredicate;
import org.logicng.predicates.satisfiability.TautologyPredicate;
import org.logicng.util.FormulaCornerCases;
import org.logicng.util.FormulaRandomizer;
import org.logicng.util.FormulaRandomizerConfig;

public class CanonicalDNFEnumerationTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSamples(final FormulaContext _c) throws ParserException {
        final CanonicalDNFEnumeration de = new CanonicalDNFEnumeration(_c.f);
        assertThat(_c.f.falsum().transform(de)).isEqualTo(_c.f.parse("$false"));
        assertThat(_c.f.verum().transform(de)).isEqualTo(_c.f.parse("$true"));
        assertThat(_c.f.parse("a").transform(de)).isEqualTo(_c.f.parse("a"));
        assertThat(_c.f.parse("~a").transform(de)).isEqualTo(_c.f.parse("~a"));
        assertThat(_c.f.parse("~a & b").transform(de)).isEqualTo(_c.f.parse("~a & b"));
        assertThat(_c.f.parse("~a | b").transform(de)).isEqualTo(_c.f.parse("~a & ~b | ~a & b | a & b"));
        assertThat(_c.f.parse("a => b").transform(de)).isEqualTo(_c.f.parse("~a & ~b | ~a & b | a & b"));
        assertThat(_c.f.parse("a <=> b").transform(de)).isEqualTo(_c.f.parse("a & b | ~a & ~b"));
        assertThat(_c.f.parse("a + b = 1").transform(de)).isEqualTo(_c.f.parse("~a & b | a & ~b"));
        assertThat(_c.f.parse("a & (b | ~c)").transform(de)).isEqualTo(_c.f.parse("a & b & c | a & b & ~c | a & ~b & ~c"));
        assertThat(_c.f.parse("a & b & (~a | ~b)").transform(de)).isEqualTo(_c.f.parse("$false"));
        assertThat(_c.f.parse("a | b | ~a & ~b").transform(de)).isEqualTo(_c.f.parse("~a & b | a & b | a & ~b | ~a & ~b"));
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
        final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f, FormulaRandomizerConfig.builder().numVars(5).weightPbc(0.5).seed(42).build());
        for (int i = 0; i < 1000; i++) {
            final Formula formula = randomizer.formula(3);
            test(formula);
        }
    }

    private void test(final Formula formula) {
        final FormulaFactory f = formula.factory();
        final Formula dnf = new CanonicalDNFEnumeration(f).apply(formula);
        assertThat(dnf.holds(new DNFPredicate(f))).isTrue();
        assertThat(f.equivalence(formula, dnf).holds(new TautologyPredicate(f))).isTrue();
        if (formula.holds(new ContradictionPredicate(f))) {
            assertThat(dnf).isEqualTo(f.falsum());
        } else {
            assertThat(hasConstantTermSize(dnf)).isTrue();
        }
    }

    private static boolean hasConstantTermSize(final Formula dnf) {
        switch (dnf.type()) {
            case LITERAL:
            case TRUE:
            case FALSE:
            case AND:
                return true;
            case OR:
                return dnf.stream().map(Formula::numberOfOperands).distinct().count() == 1;
            default:
                throw new IllegalStateException("Unexpected type: " + dnf.type());
        }
    }
}
