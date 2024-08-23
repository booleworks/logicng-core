// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

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

import java.util.HashMap;

public class CNFTest extends TestWithFormulaContext {

    private final ComputationHandler handler = new TestFactorizationHandler();

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final CNFFactorization cnf = new CNFFactorization(_c.f);

        assertThat(_c.verum.transform(cnf, handler).getResult()).isEqualTo(_c.verum);
        assertThat(_c.falsum.transform(cnf, handler).getResult()).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final CNFFactorization cnf = new CNFFactorization(_c.f);

        assertThat(_c.a.transform(cnf, handler).getResult()).isEqualTo(_c.a);
        assertThat(_c.na.transform(cnf, handler).getResult()).isEqualTo(_c.na);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryOperators(final FormulaContext _c) throws ParserException {
        final CNFFactorization cnf = new CNFFactorization(_c.f);

        assertThat(_c.imp1.transform(cnf, handler).getResult()).isEqualTo(_c.p.parse("~a | b"));
        assertThat(_c.imp2.transform(cnf, handler).getResult()).isEqualTo(_c.p.parse("a | ~b"));
        assertThat(_c.imp3.transform(cnf, handler).getResult()).isEqualTo(_c.p.parse("~a | ~b | x | y"));
        assertThat(_c.eq1.transform(cnf, handler).getResult()).isEqualTo(_c.p.parse("(a | ~b) & (~a | b)"));
        assertThat(_c.eq2.transform(cnf, handler).getResult()).isEqualTo(_c.p.parse("(~a | b) & (a | ~b)"));
        assertThat(_c.imp1.transform(cnf, handler).getResult().isCNF(_c.f)).isTrue();
        assertThat(_c.imp2.transform(cnf, handler).getResult().isCNF(_c.f)).isTrue();
        assertThat(_c.imp3.transform(cnf, handler).getResult().isCNF(_c.f)).isTrue();
        assertThat(_c.eq1.transform(cnf, handler).getResult().isCNF(_c.f)).isTrue();
        assertThat(_c.eq1.transform(cnf, handler).getResult().isDNF(_c.f)).isFalse();
        assertThat(_c.eq2.transform(cnf, handler).getResult().isCNF(_c.f)).isTrue();
        assertThat(_c.eq2.transform(cnf, handler).getResult().isDNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperators(final FormulaContext _c) throws ParserException {
        final CNFFactorization cnf = new CNFFactorization(_c.f);

        assertThat(_c.and1.transform(cnf, handler).getResult()).isEqualTo(_c.and1);
        assertThat(_c.or1.transform(cnf, handler).getResult()).isEqualTo(_c.or1);
        assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(cnf, handler)
                .getResult())
                .isEqualTo(_c.p.parse("~a & ~b & c & (~x | y) & (~w | z)"));
        assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").transform(cnf, handler).getResult())
                .isEqualTo(_c.p.parse("(~a | ~b | c | ~x) & (~a  | ~b | c | y)"));
        assertThat(_c.p.parse("a | b | (~x & ~y)").transform(cnf, handler).getResult())
                .isEqualTo(_c.p.parse("(a | b | ~x) & (a | b | ~y)"));
        assertThat(_c.and1.transform(cnf, handler).getResult().isCNF(_c.f)).isTrue();
        assertThat(_c.or1.transform(cnf, handler).getResult().isCNF(_c.f)).isTrue();
        assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(cnf, handler)
                .getResult().isCNF(_c.f)).isTrue();
        assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(cnf, handler)
                .getResult().isDNF(_c.f)).isFalse();
        assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").transform(cnf, handler)
                .getResult().isCNF(_c.f)).isTrue();
        assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").transform(cnf, handler)
                .getResult().isDNF(_c.f)).isFalse();
        assertThat(_c.p.parse("a | b | (~x & ~y)").transform(cnf, handler)
                .getResult().isCNF(_c.f)).isTrue();
        assertThat(_c.p.parse("a | b | (~x & ~y)").transform(cnf, handler)
                .getResult().isDNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        final CNFFactorization cnf = new CNFFactorization(_c.f);

        final TestFactorizationHandler handler2 = new TestFactorizationHandler();
        final CNFFactorization cnf2 = new CNFFactorization(_c.f, new HashMap<>());
        assertThat(_c.p.parse("~a2").transform(cnf, handler).getResult()).isEqualTo(_c.p.parse("~a2"));
        assertThat(_c.p.parse("~~a2").transform(cnf, handler).getResult()).isEqualTo(_c.p.parse("a2"));
        assertThat(_c.p.parse("~(a2 => b2)").transform(cnf, handler).getResult()).isEqualTo(_c.p.parse("a2 & ~b2"));
        assertThat(_c.p.parse("~(~(a2 | b2) => ~(x2 | y2))").transform(cnf, handler).getResult())
                .isEqualTo(_c.p.parse("~a2 & ~b2 & (x2 | y2)"));
        assertThat(_c.p.parse("~(a2 <=> b2)").transform(cnf, handler).getResult())
                .isEqualTo(_c.p.parse("(~a2 | ~b2) & (a2 | b2)"));
        assertThat(_c.p.parse("~(~(a2 | b2) <=> ~(x2 | y2))").transform(cnf2, handler2).getResult())
                .isEqualTo(_c.p.parse("(a2 | b2 | x2 | y2) & (~a2 | ~x2) & (~a2 | ~y2) & (~b2 | ~x2) & (~b2 | ~y2)"));
        assertThat(_c.p.parse("~(a2 & b2 & ~x2 & ~y2)").transform(cnf, handler).getResult())
                .isEqualTo(_c.p.parse("~a2 | ~b2 | x2 | y2"));
        assertThat(_c.p.parse("~(a2 | b2 | ~x2 | ~y2)").transform(cnf, handler).getResult())
                .isEqualTo(_c.p.parse("~a2 & ~b2 & x2 & y2"));
        assertThat(_c.p.parse("~(a2 | b2 | ~x2 | ~y2)").transform(cnf, handler).getResult())
                .isEqualTo(_c.p.parse("~a2 & ~b2 & x2 & y2"));
        assertThat(handler2.distCount).isEqualTo(10);
        assertThat(handler2.clauseCount).isEqualTo(7);
        assertThat(handler2.longestClause).isEqualTo(4);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCC(final FormulaContext _c) throws ParserException {
        assertThat(_c.p.parse("a <=> (1 * b <= 1)").cnf(_c.f)).isEqualTo(_c.p.parse("a"));
        assertThat(_c.p.parse("~(1 * b <= 1)").cnf(_c.f)).isEqualTo(_c.p.parse("$false"));
        assertThat(_c.p.parse("(1 * b + 1 * c + 1 * d <= 1)").cnf(_c.f))
                .isEqualTo(_c.p.parse("(~b | ~c) & (~b | ~d) & (~c | ~d)"));
        assertThat(_c.p.parse("~(1 * b + 1 * c + 1 * d <= 1)").cnf(_c.f)).isEqualTo(_c.p.parse(String.format(
                "(d | @AUX_%1$s_CC_1 | @AUX_%1$s_CC_4) & (~@AUX_%1$s_CC_3 | @AUX_%1$s_CC_1 | @AUX_%1$s_CC_4) & (~@AUX_%1$s_CC_3 | d | @AUX_%1$s_CC_4) & (~@AUX_%1$s_CC_4 | @AUX_%1$s_CC_0) & (~@AUX_%1$s_CC_2 | @AUX_%1$s_CC_0) & (~@AUX_%1$s_CC_4 | ~@AUX_%1$s_CC_2) & (c | @AUX_%1$s_CC_3 | @AUX_%1$s_CC_5) & (b | @AUX_%1$s_CC_3 | @AUX_%1$s_CC_5) & (b | c | @AUX_%1$s_CC_5) & (~@AUX_%1$s_CC_5 | @AUX_%1$s_CC_2) & ~@AUX_%1$s_CC_0",
                _c.f.name())));
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
                    canceled = clauses >= 2;
                }
                return !canceled;
            }
        };
        final CNFFactorization factorization = new CNFFactorization(_c.f, null);
        LNGResult<Formula> cnf = factorization.apply(formula, handler);
        assertThat(cnf.isSuccess()).isFalse();
        assertThat(cnf.getResult()).isNull();

        formula = _c.p.parse("~(a | b)");
        cnf = factorization.apply(formula, handler);
        assertThat(cnf.isSuccess()).isTrue();
        assertThat(cnf.getResult()).isNotNull();
    }

    private static class TestFactorizationHandler implements ComputationHandler {
        private int distCount = 0;
        private int clauseCount = 0;
        private long longestClause = 0;

        @Override
        public boolean shouldResume(final LNGEvent event) {
            if (event == FACTORIZATION_STARTED) {
                distCount = 0;
                clauseCount = 0;
                longestClause = 0;
            } else if (event == DISTRIBUTION_PERFORMED) {
                distCount++;
            } else if (event instanceof FactorizationCreatedClauseEvent) {
                final Formula clause = ((FactorizationCreatedClauseEvent) event).getClause();
                clauseCount++;
                longestClause = Math.max(clause.numberOfAtoms(clause.factory()), longestClause);
            }
            return true;
        }
    }
}
