// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.handlers.FactorizationHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;

public class CNFTest extends TestWithFormulaContext {

    private final FactorizationHandler handler = new TestFactorizationHandler();

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final CNFFactorization cnf = new CNFFactorization(_c.f, handler);

        assertThat(_c.verum.transform(cnf)).isEqualTo(_c.verum);
        assertThat(_c.falsum.transform(cnf)).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final CNFFactorization cnf = new CNFFactorization(_c.f, handler);

        assertThat(_c.a.transform(cnf)).isEqualTo(_c.a);
        assertThat(_c.na.transform(cnf)).isEqualTo(_c.na);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryOperators(final FormulaContext _c) throws ParserException {
        final CNFFactorization cnf = new CNFFactorization(_c.f, handler);

        assertThat(_c.imp1.transform(cnf)).isEqualTo(_c.p.parse("~a | b"));
        assertThat(_c.imp2.transform(cnf)).isEqualTo(_c.p.parse("a | ~b"));
        assertThat(_c.imp3.transform(cnf)).isEqualTo(_c.p.parse("~a | ~b | x | y"));
        assertThat(_c.eq1.transform(cnf)).isEqualTo(_c.p.parse("(a | ~b) & (~a | b)"));
        assertThat(_c.eq2.transform(cnf)).isEqualTo(_c.p.parse("(~a | b) & (a | ~b)"));
        assertThat(_c.imp1.transform(cnf).isCNF(_c.f)).isTrue();
        assertThat(_c.imp2.transform(cnf).isCNF(_c.f)).isTrue();
        assertThat(_c.imp3.transform(cnf).isCNF(_c.f)).isTrue();
        assertThat(_c.eq1.transform(cnf).isCNF(_c.f)).isTrue();
        assertThat(_c.eq1.transform(cnf).isDNF(_c.f)).isFalse();
        assertThat(_c.eq2.transform(cnf).isCNF(_c.f)).isTrue();
        assertThat(_c.eq2.transform(cnf).isDNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperators(final FormulaContext _c) throws ParserException {
        final CNFFactorization cnf = new CNFFactorization(_c.f, handler);

        assertThat(_c.and1.transform(cnf)).isEqualTo(_c.and1);
        assertThat(_c.or1.transform(cnf)).isEqualTo(_c.or1);
        Assertions.assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(cnf))
                .isEqualTo(_c.p.parse("~a & ~b & c & (~x | y) & (~w | z)"));
        Assertions.assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").transform(cnf))
                .isEqualTo(_c.p.parse("(~a | ~b | c | ~x) & (~a  | ~b | c | y)"));
        Assertions.assertThat(_c.p.parse("a | b | (~x & ~y)").transform(cnf))
                .isEqualTo(_c.p.parse("(a | b | ~x) & (a | b | ~y)"));
        assertThat(_c.and1.transform(cnf).isCNF(_c.f)).isTrue();
        assertThat(_c.or1.transform(cnf).isCNF(_c.f)).isTrue();
        Assertions.assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(cnf).isCNF(_c.f)).isTrue();
        Assertions.assertThat(_c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(cnf).isDNF(_c.f)).isFalse();
        Assertions.assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").transform(cnf).isCNF(_c.f)).isTrue();
        Assertions.assertThat(_c.p.parse("~(a & b) | c | ~(x | ~y)").transform(cnf).isDNF(_c.f)).isFalse();
        Assertions.assertThat(_c.p.parse("a | b | (~x & ~y)").transform(cnf).isCNF(_c.f)).isTrue();
        Assertions.assertThat(_c.p.parse("a | b | (~x & ~y)").transform(cnf).isDNF(_c.f)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        final CNFFactorization cnf = new CNFFactorization(_c.f, handler);

        final TestFactorizationHandler handler2 = new TestFactorizationHandler();
        final CNFFactorization cnf2 = new CNFFactorization(_c.f, handler2, new HashMap<>());
        Assertions.assertThat(_c.p.parse("~a2").transform(cnf)).isEqualTo(_c.p.parse("~a2"));
        Assertions.assertThat(_c.p.parse("~~a2").transform(cnf)).isEqualTo(_c.p.parse("a2"));
        Assertions.assertThat(_c.p.parse("~(a2 => b2)").transform(cnf)).isEqualTo(_c.p.parse("a2 & ~b2"));
        Assertions.assertThat(_c.p.parse("~(~(a2 | b2) => ~(x2 | y2))").transform(cnf))
                .isEqualTo(_c.p.parse("~a2 & ~b2 & (x2 | y2)"));
        Assertions.assertThat(_c.p.parse("~(a2 <=> b2)").transform(cnf))
                .isEqualTo(_c.p.parse("(~a2 | ~b2) & (a2 | b2)"));
        Assertions.assertThat(_c.p.parse("~(~(a2 | b2) <=> ~(x2 | y2))").transform(cnf2))
                .isEqualTo(_c.p.parse("(a2 | b2 | x2 | y2) & (~a2 | ~x2) & (~a2 | ~y2) & (~b2 | ~x2) & (~b2 | ~y2)"));
        Assertions.assertThat(_c.p.parse("~(a2 & b2 & ~x2 & ~y2)").transform(cnf))
                .isEqualTo(_c.p.parse("~a2 | ~b2 | x2 | y2"));
        Assertions.assertThat(_c.p.parse("~(a2 | b2 | ~x2 | ~y2)").transform(cnf))
                .isEqualTo(_c.p.parse("~a2 & ~b2 & x2 & y2"));
        Assertions.assertThat(_c.p.parse("~(a2 | b2 | ~x2 | ~y2)").transform(cnf))
                .isEqualTo(_c.p.parse("~a2 & ~b2 & x2 & y2"));
        assertThat(handler2.distCount).isEqualTo(10);
        assertThat(handler2.clauseCount).isEqualTo(7);
        assertThat(handler2.longestClause).isEqualTo(4);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCC(final FormulaContext _c) throws ParserException {
        Assertions.assertThat(_c.p.parse("a <=> (1 * b <= 1)").cnf(_c.f)).isEqualTo(_c.p.parse("a"));
        Assertions.assertThat(_c.p.parse("~(1 * b <= 1)").cnf(_c.f)).isEqualTo(_c.p.parse("$false"));
        Assertions.assertThat(_c.p.parse("(1 * b + 1 * c + 1 * d <= 1)").cnf(_c.f))
                .isEqualTo(_c.p.parse("(~b | ~c) & (~b | ~d) & (~c | ~d)"));
        Assertions.assertThat(_c.p.parse("~(1 * b + 1 * c + 1 * d <= 1)").cnf(_c.f)).isEqualTo(_c.p.parse(String.format(
                "(d | @AUX_%1$s_CC_1 | @AUX_%1$s_CC_4) & (~@AUX_%1$s_CC_3 | @AUX_%1$s_CC_1 | @AUX_%1$s_CC_4) & (~@AUX_%1$s_CC_3 | d | @AUX_%1$s_CC_4) & (~@AUX_%1$s_CC_4 | @AUX_%1$s_CC_0) & (~@AUX_%1$s_CC_2 | @AUX_%1$s_CC_0) & (~@AUX_%1$s_CC_4 | ~@AUX_%1$s_CC_2) & (c | @AUX_%1$s_CC_3 | @AUX_%1$s_CC_5) & (b | @AUX_%1$s_CC_3 | @AUX_%1$s_CC_5) & (b | c | @AUX_%1$s_CC_5) & (~@AUX_%1$s_CC_5 | @AUX_%1$s_CC_2) & ~@AUX_%1$s_CC_0",
                _c.f.name())));
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
                aborted = clauses >= 2;
                return !aborted;
            }
        };
        final CNFFactorization factorization = new CNFFactorization(_c.f, handler, null);
        Formula cnf = factorization.apply(formula);
        assertThat(handler.aborted()).isTrue();
        assertThat(cnf).isNull();

        formula = _c.p.parse("~(a | b)");
        cnf = factorization.apply(formula);
        assertThat(handler.aborted()).isFalse();
        assertThat(cnf).isNotNull();
    }

    private static class TestFactorizationHandler implements FactorizationHandler {

        private boolean aborted;
        private int distCount = 0;
        private int clauseCount = 0;
        private long longestClause = 0;

        @Override
        public boolean aborted() {
            return aborted;
        }

        @Override
        public void started() {
            aborted = false;
            distCount = 0;
            clauseCount = 0;
            longestClause = 0;
        }

        @Override
        public boolean performedDistribution() {
            distCount++;
            return true;
        }

        @Override
        public boolean createdClause(final Formula clause) {
            clauseCount++;
            longestClause = Math.max(clause.numberOfAtoms(clause.factory()), longestClause);
            return true;
        }
    }
}
