// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.dnf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.handlers.FactorizationHandler;
import org.logicng.io.parsers.ParserException;

public class DNFFactorizationTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final DNFFactorization dnfFactorization = new DNFFactorization(_c.f);

        assertThat(_c.verum.transform(dnfFactorization)).isEqualTo(_c.verum);
        assertThat(_c.falsum.transform(dnfFactorization)).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final DNFFactorization dnfFactorization = new DNFFactorization(_c.f);

        assertThat(_c.a.transform(dnfFactorization)).isEqualTo(_c.a);
        assertThat(_c.na.transform(dnfFactorization)).isEqualTo(_c.na);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryOperators(final FormulaContext _c) throws ParserException {
        final DNFFactorization dnfFactorization = new DNFFactorization(_c.f);

        assertThat(_c.imp1.transform(dnfFactorization)).isEqualTo(_c.p.parse("~a | b"));
        assertThat(_c.imp2.transform(dnfFactorization)).isEqualTo(_c.p.parse("a | ~b"));
        assertThat(_c.imp3.transform(dnfFactorization)).isEqualTo(_c.p.parse("~a | ~b | x | y"));
        assertThat(_c.eq1.transform(dnfFactorization)).isEqualTo(_c.p.parse("(a & b) | (~a & ~b)"));
        assertThat(_c.eq2.transform(dnfFactorization)).isEqualTo(_c.p.parse("(a & b) | (~a & ~b)"));
        assertThat(_c.imp1.transform(dnfFactorization).isDNF(_c.f)).isTrue();
        assertThat(_c.imp2.transform(dnfFactorization).isDNF(_c.f)).isTrue();
        assertThat(_c.imp3.transform(dnfFactorization).isDNF(_c.f)).isTrue();
        assertThat(_c.eq1.transform(dnfFactorization).isDNF(_c.f)).isTrue();
        assertThat(_c.eq1.transform(dnfFactorization).isCNF(_c.f)).isFalse();
        assertThat(_c.eq2.transform(dnfFactorization).isDNF(_c.f)).isTrue();
        assertThat(_c.eq2.transform(dnfFactorization).isCNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperators(final FormulaContext _c) throws ParserException {
        final DNFFactorization dnfFactorization = new DNFFactorization(_c.f);

        assertThat(_c.and1.transform(dnfFactorization)).isEqualTo(_c.and1);
        assertThat(_c.or1.transform(dnfFactorization)).isEqualTo(_c.or1);
        assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").transform(dnfFactorization)).isEqualTo(_c.p.parse("~a | ~b | c | (~x & y)"));
        assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y)").transform(dnfFactorization)).isEqualTo(_c.p.parse("(~a & ~b & c & ~x) | (~a & ~b & c & y)"));
        assertThat(_c.p.parse("a & b & (~x | ~y)").transform(dnfFactorization)).isEqualTo(_c.p.parse("(a & b & ~x) | (a & b & ~y)"));
        assertThat(_c.and1.transform(dnfFactorization).isDNF(_c.f)).isTrue();
        assertThat(_c.or1.transform(dnfFactorization).isDNF(_c.f)).isTrue();
        assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(dnfFactorization).isDNF(_c.f)).isTrue();
        assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(dnfFactorization).isCNF(_c.f)).isFalse();
        assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").transform(dnfFactorization).isDNF(_c.f)).isTrue();
        assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").transform(dnfFactorization).isCNF(_c.f)).isFalse();
        assertThat(_c.p.parse("a | b | (~x & ~y)").transform(dnfFactorization).isDNF(_c.f)).isTrue();
        assertThat(_c.p.parse("a | b | (~x & ~y)").transform(dnfFactorization).isCNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        final DNFFactorization dnfFactorization = new DNFFactorization(_c.f);

        assertThat(_c.p.parse("~a").transform(dnfFactorization)).isEqualTo(_c.p.parse("~a"));
        assertThat(_c.p.parse("~~a").transform(dnfFactorization)).isEqualTo(_c.p.parse("a"));
        assertThat(_c.p.parse("~(a => b)").transform(dnfFactorization)).isEqualTo(_c.p.parse("a & ~b"));
        assertThat(_c.p.parse("~(~(a | b) => ~(x | y))").transform(dnfFactorization)).isEqualTo(_c.p.parse("(~a & ~b & x) | (~a & ~b & y)"));
        assertThat(_c.p.parse("~(a <=> b)").transform(dnfFactorization)).isEqualTo(_c.p.parse("(~a & b) | (a & ~b)"));
        assertThat(_c.p.parse("~(a & b & ~x & ~y)").transform(dnfFactorization)).isEqualTo(_c.p.parse("~a | ~b | x | y"));
        assertThat(_c.p.parse("~(a | b | ~x | ~y)").transform(dnfFactorization)).isEqualTo(_c.p.parse("~a & ~b & x & y"));
        assertThat(_c.p.parse("~(a | b | ~x | ~y)").transform(dnfFactorization)).isEqualTo(_c.p.parse("~a & ~b & x & y"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCDNF(final FormulaContext _c) throws ParserException {
        final Formula formula = _c.p.parse("x0 & x1 & x3 | ~x1 & ~x2 | x2 & ~x3");
        final Formula cdnf = _c.p.parse("x0 & x1 & x2 & x3 | x0 & x1 & x2 & ~x3 | x0 & ~x1 & x2 & ~x3 | ~x0 & ~x1 & x2 & ~x3 | ~x0 & ~x1 & ~x2 & ~x3 | x0 & ~x1 & ~x2 & ~x3 | x0 & ~x1 & ~x2 & x3 | x0 & x1 & ~x2 & x3 | ~x0 & x1 & x2 & ~x3 | ~x0 & ~x1 & ~x2 & x3");
        assertThat(formula.transform(new CanonicalDNFEnumeration(_c.f))).isEqualTo(cdnf);
        assertThat(_c.f.and(_c.a, _c.na).transform(new CanonicalDNFEnumeration(_c.f))).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testWithHandler(final FormulaContext _c) throws ParserException {
        Formula formula = _c.p.parse("(~(~(a | b) => ~(x | y))) & ((a | x) => ~(b | y))");
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
        final DNFFactorization factorization = new DNFFactorization(_c.f, handler, null);
        Formula dnf = factorization.apply(formula);
        assertThat(handler.aborted()).isTrue();
        assertThat(dnf).isNull();

        formula = _c.p.parse("~(a | b)");
        dnf = factorization.apply(formula);
        assertThat(handler.aborted()).isFalse();
        assertThat(dnf).isNotNull();
    }
}
