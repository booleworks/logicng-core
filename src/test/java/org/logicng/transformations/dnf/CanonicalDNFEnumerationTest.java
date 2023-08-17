// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.dnf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.RandomTag;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;
import org.logicng.predicates.DNFPredicate;
import org.logicng.predicates.satisfiability.ContradictionPredicate;
import org.logicng.predicates.satisfiability.TautologyPredicate;
import org.logicng.util.FormulaCornerCases;
import org.logicng.util.FormulaRandomizer;
import org.logicng.util.FormulaRandomizerConfig;

public class CanonicalDNFEnumerationTest {

    @Test
    public void testSamples() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        assertThat(f.falsum().transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.parse("$false"));
        assertThat(f.verum().transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.parse("$true"));
        assertThat(f.parse("a").transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.parse("a"));
        assertThat(f.parse("~a").transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.parse("~a"));
        assertThat(f.parse("~a & b").transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.parse("~a & b"));
        assertThat(f.parse("~a | b").transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.parse("~a & ~b | ~a & b | a & b"));
        assertThat(f.parse("a => b").transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.parse("~a & ~b | ~a & b | a & b"));
        assertThat(f.parse("a <=> b").transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.parse("a & b | ~a & ~b"));
        assertThat(f.parse("a + b = 1").transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.parse("~a & b | a & ~b"));
        assertThat(f.parse("a & (b | ~c)").transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.parse("a & b & c | a & b & ~c | a & ~b & ~c"));
        assertThat(f.parse("a & b & (~a | ~b)").transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.parse("$false"));
        assertThat(f.parse("a | b | ~a & ~b").transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.parse("~a & b | a & b | a & ~b | ~a & ~b"));
    }

    @Test
    public void testCornerCases() {
        final FormulaFactory f = FormulaFactory.caching();
        final FormulaCornerCases cornerCases = new FormulaCornerCases(f);
        for (final Formula formula : cornerCases.cornerCases()) {
            test(formula);
        }
    }

    @Test
    @RandomTag
    public void random() {
        final FormulaFactory f = FormulaFactory.caching();
        final FormulaRandomizer randomizer = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().numVars(5).weightPbc(0.5).seed(42).build());
        for (int i = 0; i < 1000; i++) {
            final Formula formula = randomizer.formula(3);
            test(formula);
        }
    }

    private void test(final Formula formula) {
        final FormulaFactory f = formula.factory();
        final Formula dnf = new CanonicalDNFEnumeration(f).apply(formula);
        assertThat(dnf.holds(new DNFPredicate())).isTrue();
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
