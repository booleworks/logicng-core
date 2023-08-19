// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.dnf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.formulas.Formula;
import org.logicng.handlers.FactorizationHandler;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

public class DNFFactorizationTest extends TestWithExampleFormulas {

    private final DNFFactorization dnfFactorization = new DNFFactorization(f);

    @Test
    public void testConstants() {
        assertThat(TRUE.transform(dnfFactorization)).isEqualTo(TRUE);
        assertThat(FALSE.transform(dnfFactorization)).isEqualTo(FALSE);
    }

    @Test
    public void testLiterals() {
        assertThat(A.transform(dnfFactorization)).isEqualTo(A);
        assertThat(NA.transform(dnfFactorization)).isEqualTo(NA);
    }

    @Test
    public void testBinaryOperators() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(IMP1.transform(dnfFactorization)).isEqualTo(p.parse("~a | b"));
        assertThat(IMP2.transform(dnfFactorization)).isEqualTo(p.parse("a | ~b"));
        assertThat(IMP3.transform(dnfFactorization)).isEqualTo(p.parse("~a | ~b | x | y"));
        assertThat(EQ1.transform(dnfFactorization)).isEqualTo(p.parse("(a & b) | (~a & ~b)"));
        assertThat(EQ2.transform(dnfFactorization)).isEqualTo(p.parse("(a & b) | (~a & ~b)"));
        assertThat(IMP1.transform(dnfFactorization).isDNF()).isTrue();
        assertThat(IMP2.transform(dnfFactorization).isDNF()).isTrue();
        assertThat(IMP3.transform(dnfFactorization).isDNF()).isTrue();
        assertThat(EQ1.transform(dnfFactorization).isDNF()).isTrue();
        assertThat(EQ1.transform(dnfFactorization).isCNF()).isFalse();
        assertThat(EQ2.transform(dnfFactorization).isDNF()).isTrue();
        assertThat(EQ2.transform(dnfFactorization).isCNF()).isFalse();
    }

    @Test
    public void testNAryOperators() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(AND1.transform(dnfFactorization)).isEqualTo(AND1);
        assertThat(OR1.transform(dnfFactorization)).isEqualTo(OR1);
        assertThat(p.parse("~(a & b) | c | ~(x | ~y)").transform(dnfFactorization)).isEqualTo(p.parse("~a | ~b | c | (~x & y)"));
        assertThat(p.parse("~(a | b) & c & ~(x & ~y)").transform(dnfFactorization)).isEqualTo(p.parse("(~a & ~b & c & ~x) | (~a & ~b & c & y)"));
        assertThat(p.parse("a & b & (~x | ~y)").transform(dnfFactorization)).isEqualTo(p.parse("(a & b & ~x) | (a & b & ~y)"));
        assertThat(AND1.transform(dnfFactorization).isDNF()).isTrue();
        assertThat(OR1.transform(dnfFactorization).isDNF()).isTrue();
        assertThat(p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(dnfFactorization).isDNF()).isTrue();
        assertThat(p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(dnfFactorization).isCNF()).isFalse();
        assertThat(p.parse("~(a & b) | c | ~(x | ~y)").transform(dnfFactorization).isDNF()).isTrue();
        assertThat(p.parse("~(a & b) | c | ~(x | ~y)").transform(dnfFactorization).isCNF()).isFalse();
        assertThat(p.parse("a | b | (~x & ~y)").transform(dnfFactorization).isDNF()).isTrue();
        assertThat(p.parse("a | b | (~x & ~y)").transform(dnfFactorization).isCNF()).isFalse();
    }

    @Test
    public void testNot() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(p.parse("~a").transform(dnfFactorization)).isEqualTo(p.parse("~a"));
        assertThat(p.parse("~~a").transform(dnfFactorization)).isEqualTo(p.parse("a"));
        assertThat(p.parse("~(a => b)").transform(dnfFactorization)).isEqualTo(p.parse("a & ~b"));
        assertThat(p.parse("~(~(a | b) => ~(x | y))").transform(dnfFactorization)).isEqualTo(p.parse("(~a & ~b & x) | (~a & ~b & y)"));
        assertThat(p.parse("~(a <=> b)").transform(dnfFactorization)).isEqualTo(p.parse("(~a & b) | (a & ~b)"));
        assertThat(p.parse("~(a & b & ~x & ~y)").transform(dnfFactorization)).isEqualTo(p.parse("~a | ~b | x | y"));
        assertThat(p.parse("~(a | b | ~x | ~y)").transform(dnfFactorization)).isEqualTo(p.parse("~a & ~b & x & y"));
        assertThat(p.parse("~(a | b | ~x | ~y)").transform(dnfFactorization)).isEqualTo(p.parse("~a & ~b & x & y"));
    }

    @Test
    public void testCDNF() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        final Formula formula = p.parse("x0 & x1 & x3 | ~x1 & ~x2 | x2 & ~x3");
        final Formula cdnf = p.parse("x0 & x1 & x2 & x3 | x0 & x1 & x2 & ~x3 | x0 & ~x1 & x2 & ~x3 | ~x0 & ~x1 & x2 & ~x3 | ~x0 & ~x1 & ~x2 & ~x3 | x0 & ~x1 & ~x2 & ~x3 | x0 & ~x1 & ~x2 & x3 | x0 & x1 & ~x2 & x3 | ~x0 & x1 & x2 & ~x3 | ~x0 & ~x1 & ~x2 & x3");
        assertThat(formula.transform(new CanonicalDNFEnumeration(f))).isEqualTo(cdnf);
        assertThat(f.and(A, NA).transform(new CanonicalDNFEnumeration(f))).isEqualTo(f.falsum());
    }

    @Test
    public void testWithHandler() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        Formula formula = p.parse("(~(~(a | b) => ~(x | y))) & ((a | x) => ~(b | y))");
        final FactorizationHandler handler = new FactorizationHandler() {
            private boolean aborted;
            private int dists = 0;
            private int clauses = 0;

            @Override
            public boolean aborted() {
                return aborted;
            }

            @Override
            public void started() {
                aborted = false;
                dists = 0;
                clauses = 0;
            }

            @Override
            public boolean performedDistribution() {
                dists++;
                aborted = dists >= 100;
                return !aborted;
            }

            @Override
            public boolean createdClause(final Formula clause) {
                clauses++;
                aborted = clauses >= 5;
                return !aborted;
            }
        };
        final DNFFactorization factorization = new DNFFactorization(f, handler, null);
        Formula dnf = factorization.apply(formula);
        assertThat(handler.aborted()).isTrue();
        assertThat(dnf).isNull();

        formula = p.parse("~(a | b)");
        dnf = factorization.apply(formula);
        assertThat(handler.aborted()).isFalse();
        assertThat(dnf).isNotNull();
    }
}
