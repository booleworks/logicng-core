// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.cnf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.handlers.FactorizationHandler;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.io.parsers.PseudoBooleanParser;

public class CNFTest extends TestWithExampleFormulas {

    private final FactorizationHandler handler = new TestFactorizationHandler();
    private final CNFFactorization cnf = new CNFFactorization(f, handler);

    @Test
    public void testConstants() {
        assertThat(TRUE.transform(cnf)).isEqualTo(TRUE);
        assertThat(FALSE.transform(cnf)).isEqualTo(FALSE);
    }

    @Test
    public void testLiterals() {
        assertThat(A.transform(cnf)).isEqualTo(A);
        assertThat(NA.transform(cnf)).isEqualTo(NA);
    }

    @Test
    public void testBinaryOperators() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(IMP1.transform(cnf)).isEqualTo(p.parse("~a | b"));
        assertThat(IMP2.transform(cnf)).isEqualTo(p.parse("a | ~b"));
        assertThat(IMP3.transform(cnf)).isEqualTo(p.parse("~a | ~b | x | y"));
        assertThat(EQ1.transform(cnf)).isEqualTo(p.parse("(a | ~b) & (~a | b)"));
        assertThat(EQ2.transform(cnf)).isEqualTo(p.parse("(~a | b) & (a | ~b)"));
        assertThat(IMP1.transform(cnf).isCNF()).isTrue();
        assertThat(IMP2.transform(cnf).isCNF()).isTrue();
        assertThat(IMP3.transform(cnf).isCNF()).isTrue();
        assertThat(EQ1.transform(cnf).isCNF()).isTrue();
        assertThat(EQ1.transform(cnf).isDNF()).isFalse();
        assertThat(EQ2.transform(cnf).isCNF()).isTrue();
        assertThat(EQ2.transform(cnf).isDNF()).isFalse();
    }

    @Test
    public void testNAryOperators() throws ParserException {
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(AND1.transform(cnf)).isEqualTo(AND1);
        assertThat(OR1.transform(cnf)).isEqualTo(OR1);
        assertThat(p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(cnf)).isEqualTo(p.parse("~a & ~b & c & (~x | y) & (~w | z)"));
        assertThat(p.parse("~(a & b) | c | ~(x | ~y)").transform(cnf)).isEqualTo(p.parse("(~a | ~b | c | ~x) & (~a  | ~b | c | y)"));
        assertThat(p.parse("a | b | (~x & ~y)").transform(cnf)).isEqualTo(p.parse("(a | b | ~x) & (a | b | ~y)"));
        assertThat(AND1.transform(cnf).isCNF()).isTrue();
        assertThat(OR1.transform(cnf).isCNF()).isTrue();
        assertThat(p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(cnf).isCNF()).isTrue();
        assertThat(p.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(cnf).isDNF()).isFalse();
        assertThat(p.parse("~(a & b) | c | ~(x | ~y)").transform(cnf).isCNF()).isTrue();
        assertThat(p.parse("~(a & b) | c | ~(x | ~y)").transform(cnf).isDNF()).isFalse();
        assertThat(p.parse("a | b | (~x & ~y)").transform(cnf).isCNF()).isTrue();
        assertThat(p.parse("a | b | (~x & ~y)").transform(cnf).isDNF()).isFalse();
    }

    @Test
    public void testNot() throws ParserException {
        final TestFactorizationHandler handler2 = new TestFactorizationHandler();
        final CNFFactorization cnf2 = new CNFFactorization(f, handler2);
        final PropositionalParser p = new PropositionalParser(f);
        assertThat(p.parse("~a2").transform(cnf)).isEqualTo(p.parse("~a2"));
        assertThat(p.parse("~~a2").transform(cnf)).isEqualTo(p.parse("a2"));
        assertThat(p.parse("~(a2 => b2)").transform(cnf)).isEqualTo(p.parse("a2 & ~b2"));
        assertThat(p.parse("~(~(a2 | b2) => ~(x2 | y2))").transform(cnf)).isEqualTo(p.parse("~a2 & ~b2 & (x2 | y2)"));
        assertThat(p.parse("~(a2 <=> b2)").transform(cnf)).isEqualTo(p.parse("(~a2 | ~b2) & (a2 | b2)"));
        assertThat(p.parse("~(~(a2 | b2) <=> ~(x2 | y2))").transform(cnf2)).isEqualTo(p.parse("(a2 | b2 | x2 | y2) & (~a2 | ~x2) & (~a2 | ~y2) & (~b2 | ~x2) & (~b2 | ~y2)"));
        assertThat(p.parse("~(a2 & b2 & ~x2 & ~y2)").transform(cnf)).isEqualTo(p.parse("~a2 | ~b2 | x2 | y2"));
        assertThat(p.parse("~(a2 | b2 | ~x2 | ~y2)").transform(cnf)).isEqualTo(p.parse("~a2 & ~b2 & x2 & y2"));
        assertThat(p.parse("~(a2 | b2 | ~x2 | ~y2)").transform(cnf)).isEqualTo(p.parse("~a2 & ~b2 & x2 & y2"));
        assertThat(handler2.distCount).isEqualTo(7);
        assertThat(handler2.clauseCount).isEqualTo(4);
        assertThat(handler2.longestClause).isEqualTo(2);
    }

    @Test
    public void testCC() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PseudoBooleanParser p = new PseudoBooleanParser(f);
        assertThat(p.parse("a <=> (1 * b <= 1)").cnf()).isEqualTo(p.parse("a"));
        assertThat(p.parse("~(1 * b <= 1)").cnf()).isEqualTo(p.parse("$false"));
        assertThat(p.parse("(1 * b + 1 * c + 1 * d <= 1)").cnf()).isEqualTo(p.parse("(~b | ~c) & (~b | ~d) & (~c | ~d)"));
        assertThat(p.parse("~(1 * b + 1 * c + 1 * d <= 1)").cnf()).isEqualTo(p.parse("(d | @RESERVED_CC_1 | @RESERVED_CC_4) & (~@RESERVED_CC_3 | @RESERVED_CC_1 | @RESERVED_CC_4) & (~@RESERVED_CC_3 | d | @RESERVED_CC_4) & (~@RESERVED_CC_4 | @RESERVED_CC_0) & (~@RESERVED_CC_2 | @RESERVED_CC_0) & (~@RESERVED_CC_4 | ~@RESERVED_CC_2) & (c | @RESERVED_CC_3 | @RESERVED_CC_5) & (b | @RESERVED_CC_3 | @RESERVED_CC_5) & (b | c | @RESERVED_CC_5) & (~@RESERVED_CC_5 | @RESERVED_CC_2) & ~@RESERVED_CC_0"));
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
            longestClause = Math.max(clause.numberOfAtoms(), longestClause);
            return true;
        }
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
                aborted = clauses >= 2;
                return !aborted;
            }
        };
        final CNFFactorization factorization = new CNFFactorization(f, handler, null);
        Formula cnf = factorization.apply(formula);
        assertThat(handler.aborted()).isTrue();
        assertThat(cnf).isNull();

        formula = p.parse("~(a | b)");
        cnf = factorization.apply(formula);
        assertThat(handler.aborted()).isFalse();
        assertThat(cnf).isNotNull();
    }
}
