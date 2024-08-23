// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.dnf;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.FACTORIZATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.DISTRIBUTION_PERFORMED;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.events.FactorizationCreatedClauseEvent;
import com.booleworks.logicng.handlers.events.LNGEvent;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
        assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").transform(dnfFactorization))
                .isEqualTo(_c.p.parse("~a | ~b | c | (~x & y)"));
        assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y)").transform(dnfFactorization))
                .isEqualTo(_c.p.parse("(~a & ~b & c & ~x) | (~a & ~b & c & y)"));
        assertThat(_c.p.parse("a & b & (~x | ~y)").transform(dnfFactorization))
                .isEqualTo(_c.p.parse("(a & b & ~x) | (a & b & ~y)"));
        assertThat(_c.and1.transform(dnfFactorization).isDNF(_c.f)).isTrue();
        assertThat(_c.or1.transform(dnfFactorization).isDNF(_c.f)).isTrue();
        assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(dnfFactorization).isDNF(_c.f))
                .isTrue();
        assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(dnfFactorization).isCNF(_c.f))
                .isFalse();
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
        assertThat(_c.p.parse("~(~(a | b) => ~(x | y))").transform(dnfFactorization))
                .isEqualTo(_c.p.parse("(~a & ~b & x) | (~a & ~b & y)"));
        assertThat(_c.p.parse("~(a <=> b)").transform(dnfFactorization))
                .isEqualTo(_c.p.parse("(~a & b) | (a & ~b)"));
        assertThat(_c.p.parse("~(a & b & ~x & ~y)").transform(dnfFactorization))
                .isEqualTo(_c.p.parse("~a | ~b | x | y"));
        assertThat(_c.p.parse("~(a | b | ~x | ~y)").transform(dnfFactorization))
                .isEqualTo(_c.p.parse("~a & ~b & x & y"));
        assertThat(_c.p.parse("~(a | b | ~x | ~y)").transform(dnfFactorization))
                .isEqualTo(_c.p.parse("~a & ~b & x & y"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCDNF(final FormulaContext _c) throws ParserException {
        final Formula formula = _c.p.parse("x0 & x1 & x3 | ~x1 & ~x2 | x2 & ~x3");
        final Formula cdnf = _c.p.parse(
                "x0 & x1 & x2 & x3 | x0 & x1 & x2 & ~x3 | x0 & ~x1 & x2 & ~x3 | ~x0 & ~x1 & x2 & ~x3 | ~x0 & ~x1 & ~x2 & ~x3 | x0 & ~x1 & ~x2 & ~x3 | x0 & ~x1 & ~x2 & x3 | x0 & x1 & ~x2 & x3 | ~x0 & x1 & x2 & ~x3 | ~x0 & ~x1 & ~x2 & x3");
        assertThat(formula.transform(new CanonicalDNFEnumeration(_c.f))).isEqualTo(cdnf);
        assertThat(_c.f.and(_c.a, _c.na).transform(new CanonicalDNFEnumeration(_c.f))).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testWithHandler(final FormulaContext _c) throws ParserException {
        Formula formula = _c.p.parse("(~(~(a | b) => ~(x | y))) & ((a | x) => ~(b | y))");
        final ComputationHandler handler = new ComputationHandler() {
            private boolean canceled;
            private int dists = 0;
            private int clauses = 0;

            @Override
            public boolean shouldResume(final LNGEvent event) {
                if (event == FACTORIZATION_STARTED) {
                    canceled = false;
                    dists = 0;
                    clauses = 0;
                } else if (event == DISTRIBUTION_PERFORMED) {
                    dists++;
                    canceled = dists >= 100;
                } else if (event instanceof FactorizationCreatedClauseEvent) {
                    clauses++;
                    canceled = clauses >= 5;
                }
                return !canceled;
            }
        };
        final DNFFactorization factorization = new DNFFactorization(_c.f, null);
        LNGResult<Formula> dnf = factorization.apply(formula, handler);
        assertThat(dnf.isSuccess()).isFalse();
        assertThat(dnf.getResult()).isNull();

        formula = _c.p.parse("~(a | b)");
        dnf = factorization.apply(formula, handler);
        assertThat(dnf.isSuccess()).isTrue();
        assertThat(dnf.getResult()).isNotNull();
    }
}
