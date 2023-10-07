// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.cnf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

import java.util.List;
import java.util.SortedSet;

public class CNFEncoderTest extends TestWithFormulaContext {

    private static final String p1 = "(x1 | x2) & x3 & x4 & ((x1 & x5 & ~(x6 | x7) | x8) | x9)";
    private static final String p2 = "(y1 | y2) & y3 & y4 & ((y1 & y5 & ~(y6 | y7) | y8) | y9)";
    private static final String p3 = "(z1 | z2) & z3 & z4 & ((z1 & z5 & ~(z6 | z7) | z8) | z9)";

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFactorization(final FormulaContext _c) throws ParserException {
        final Formula phi1 = _c.p.parse(p1);
        assertThat(phi1.numberOfAtoms(_c.f)).isEqualTo(10);
        assertThat(phi1.cnf(_c.f)).isEqualTo(_c.p.parse("(x1 | x2) & x3 & x4 & (x1 | x8 | x9) & (x5 | x8 | x9) & (~x6 | x8 | x9) & (~x7 | x8 | x9)"));
        _c.f.putConfiguration(CNFConfig.builder().build());
        assertThat(phi1.cnf(_c.f)).isEqualTo(_c.p.parse("(x1 | x2) & x3 & x4 & (x1 | x8 | x9) & (x5 | x8 | x9) & (~x6 | x8 | x9) & (~x7 | x8 | x9)"));
        _c.f.putConfiguration(CNFConfig.builder().algorithm(CNFConfig.Algorithm.FACTORIZATION).build());
        assertThat(phi1.cnf(_c.f)).isEqualTo(_c.p.parse("(x1 | x2) & x3 & x4 & (x1 | x8 | x9) & (x5 | x8 | x9) & (~x6 | x8 | x9) & (~x7 | x8 | x9)"));
        final CNFConfig config = CNFConfig.builder().algorithm(CNFConfig.Algorithm.FACTORIZATION).build();
        assertThat(CNFEncoder.encode(_c.f, phi1, config))
                .isEqualTo(_c.p.parse("(x1 | x2) & x3 & x4 & (x1 | x8 | x9) & (x5 | x8 | x9) & (~x6 | x8 | x9) & (~x7 | x8 | x9)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testTseitin(final FormulaContext _c) throws ParserException {
        final Formula phi1 = _c.p.parse(p1);
        final Formula phi2 = _c.p.parse(p2);
        _c.f.putConfiguration(CNFConfig.builder().algorithm(CNFConfig.Algorithm.TSEITIN).build());
        assertThat(phi1.cnf(_c.f)).isEqualTo(_c.p.parse("(x1 | x2) & x3 & x4 & (x1 | x8 | x9) & (x5 | x8 | x9) & (~x6 | x8 | x9) & (~x7 | x8 | x9)"));
        _c.f.putConfiguration(CNFConfig.builder().algorithm(CNFConfig.Algorithm.TSEITIN).atomBoundary(8).build());
        assertThat(phi1.cnf(_c.f)).isEqualTo(_c.p.parse("(@RESERVED_CNF_0 | ~x1) & (@RESERVED_CNF_0 | ~x2) & (~@RESERVED_CNF_0 | x1 | x2) & (~@RESERVED_CNF_1 | x1) & (~@RESERVED_CNF_1 | x5) & (~@RESERVED_CNF_1 | ~x6) & (~@RESERVED_CNF_1 | ~x7) & (@RESERVED_CNF_1 | ~x1 | ~x5 | x6 | x7) & (@RESERVED_CNF_2 | ~@RESERVED_CNF_1) & (@RESERVED_CNF_2 | ~x8) & (@RESERVED_CNF_2 | ~x9) & (~@RESERVED_CNF_2 | @RESERVED_CNF_1 | x8 | x9) & @RESERVED_CNF_0 & x3 & x4 & @RESERVED_CNF_2"));
        _c.f.putConfiguration(CNFConfig.builder().algorithm(CNFConfig.Algorithm.TSEITIN).atomBoundary(11).build());
        assertThat(phi2.cnf(_c.f)).isEqualTo(_c.p.parse("(y1 | y2) & y3 & y4 & (y1 | y8 | y9) & (y5 | y8 | y9) & (~y6 | y8 | y9) & (~y7 | y8 | y9)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPG(final FormulaContext _c) throws ParserException {
        final Formula phi1 = _c.p.parse(p1);
        final Formula phi2 = _c.p.parse(p2);
        _c.f.putConfiguration(CNFConfig.builder().algorithm(CNFConfig.Algorithm.PLAISTED_GREENBAUM).build());
        assertThat(phi1.cnf(_c.f)).isEqualTo(_c.p.parse("(x1 | x2) & x3 & x4 & (x1 | x8 | x9) & (x5 | x8 | x9) & (~x6 | x8 | x9) & (~x7 | x8 | x9)"));
        _c.f.putConfiguration(CNFConfig.builder().algorithm(CNFConfig.Algorithm.PLAISTED_GREENBAUM).atomBoundary(8).build());
        assertThat(phi1.cnf(_c.f)).isEqualTo(_c.p.parse("@RESERVED_CNF_1 & x3 & x4 & @RESERVED_CNF_2 & (~@RESERVED_CNF_1 | x1 | x2) & (~@RESERVED_CNF_2 | @RESERVED_CNF_3 | x8 | x9) & (~@RESERVED_CNF_3 | x1) & (~@RESERVED_CNF_3 | x5) & (~@RESERVED_CNF_3 | ~x6) & (~@RESERVED_CNF_3 | ~x7)"));
        _c.f.putConfiguration(CNFConfig.builder().algorithm(CNFConfig.Algorithm.PLAISTED_GREENBAUM).atomBoundary(11).build());
        assertThat(phi2.cnf(_c.f)).isEqualTo(_c.p.parse("(y1 | y2) & y3 & y4 & (y1 | y8 | y9) & (y5 | y8 | y9) & (~y6 | y8 | y9) & (~y7 | y8 | y9)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAdvanced(final FormulaContext _c) throws ParserException {
        final Formula phi1 = _c.p.parse(p1);
        final Formula phi2 = _c.p.parse(p2);
        final Formula phi3 = _c.p.parse(p3);
        assertThat(phi1.cnf(_c.f)).isEqualTo(_c.p.parse("(x1 | x2) & x3 & x4 & (x1 | x8 | x9) & (x5 | x8 | x9) & (~x6 | x8 | x9) & (~x7 | x8 | x9)"));
        _c.f.putConfiguration(CNFConfig.builder().createdClauseBoundary(5).atomBoundary(3).build());
        assertThat(phi2.cnf(_c.f)).isEqualTo(_c.p.parse("(y1 | y2) & y3 & y4 & (~@RESERVED_CNF_0 | y1) & (~@RESERVED_CNF_0 | y5) & (~@RESERVED_CNF_0 | ~y6) & (~@RESERVED_CNF_0 | ~y7) & (@RESERVED_CNF_0 | ~y1 | ~y5 | y6 | y7) & (@RESERVED_CNF_0 | y8 | y9)"));
        _c.f.putConfiguration(CNFConfig.builder().createdClauseBoundary(-1).distributionBoundary(5).atomBoundary(3).build());
        assertThat(phi3.cnf(_c.f)).isEqualTo(_c.p.parse("(z1 | z2) & z3 & z4 & (~@RESERVED_CNF_2 | z1) & (~@RESERVED_CNF_2 | z5) & (~@RESERVED_CNF_2 | ~z6) & (~@RESERVED_CNF_2 | ~z7) & (@RESERVED_CNF_2 | ~z1 | ~z5 | z6 | z7) & (@RESERVED_CNF_2 | z8 | z9)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAdvancedWithPGFallback(final FormulaContext _c) throws ParserException {
        final Formula phi1 = _c.p.parse(p1);
        final Formula phi2 = _c.p.parse(p2);
        final Formula phi3 = _c.p.parse(p3);
        assertThat(phi1.cnf(_c.f)).isEqualTo(_c.p.parse("(x1 | x2) & x3 & x4 & (x1 | x8 | x9) & (x5 | x8 | x9) & (~x6 | x8 | x9) & (~x7 | x8 | x9)"));
        _c.f.putConfiguration(CNFConfig.builder().createdClauseBoundary(5).atomBoundary(3).fallbackAlgorithmForAdvancedEncoding(CNFConfig.Algorithm.PLAISTED_GREENBAUM).build());
        assertThat(phi2.cnf(_c.f)).isEqualTo(_c.p.parse("(y1 | y2) & y3 & y4 & (@RESERVED_CNF_1 | y8 | y9) & (~@RESERVED_CNF_1 | y1) & (~@RESERVED_CNF_1 | y5) & (~@RESERVED_CNF_1 | ~y6) & (~@RESERVED_CNF_1 | ~y7)"));
        _c.f.putConfiguration(CNFConfig.builder().createdClauseBoundary(-1).distributionBoundary(5).atomBoundary(3).fallbackAlgorithmForAdvancedEncoding(CNFConfig.Algorithm.PLAISTED_GREENBAUM).build());
        assertThat(phi3.cnf(_c.f)).isEqualTo(_c.p.parse("(z1 | z2) & z3 & z4 & (@RESERVED_CNF_3 | z8 | z9) & (~@RESERVED_CNF_3 | z1) & (~@RESERVED_CNF_3 | z5) & (~@RESERVED_CNF_3 | ~z6) & (~@RESERVED_CNF_3 | ~z7)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testTseitinEncoder(final FormulaContext _c) throws ParserException {
        final Formula phi1 = _c.p.parse(p1);
        final CNFConfig config1 = CNFConfig.builder().algorithm(CNFConfig.Algorithm.TSEITIN).build();
        assertThat(CNFEncoder.encode(_c.f, phi1, config1))
                .isEqualTo(_c.p.parse("(x1 | x2) & x3 & x4 & (x1 | x8 | x9) & (x5 | x8 | x9) & (~x6 | x8 | x9) & (~x7 | x8 | x9)"));
        final CNFConfig config2 = CNFConfig.builder().algorithm(CNFConfig.Algorithm.TSEITIN).atomBoundary(8).build();
        assertThat(CNFEncoder.encode(_c.f, phi1, config2)).isEqualTo(_c.p.parse("(@RESERVED_CNF_0 | ~x1) & (@RESERVED_CNF_0 | ~x2) & (~@RESERVED_CNF_0 | x1 | x2) & " +
                "(~@RESERVED_CNF_1 | x1) & (~@RESERVED_CNF_1 | x5) & (~@RESERVED_CNF_1 | ~x6) & (~@RESERVED_CNF_1 | ~x7) & (@RESERVED_CNF_1 | ~x1 | ~x5 | x6 | x7) & (@RESERVED_CNF_2 | ~@RESERVED_CNF_1) & (@RESERVED_CNF_2 | ~x8) & (@RESERVED_CNF_2 | ~x9) & (~@RESERVED_CNF_2 | @RESERVED_CNF_1 | x8 | x9) & @RESERVED_CNF_0 & x3 & x4 & @RESERVED_CNF_2"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPGEncoder(final FormulaContext _c) throws ParserException {
        final Formula phi1 = _c.p.parse(p1);
        final CNFConfig config1 = CNFConfig.builder().algorithm(CNFConfig.Algorithm.PLAISTED_GREENBAUM).build();
        assertThat(CNFEncoder.encode(_c.f, phi1, config1))
                .isEqualTo(_c.p.parse("(x1 | x2) & x3 & x4 & (x1 | x8 | x9) & (x5 | x8 | x9) & (~x6 | x8 | x9) & (~x7 | x8 | x9)"));
        final CNFConfig config2 = CNFConfig.builder().algorithm(CNFConfig.Algorithm.PLAISTED_GREENBAUM).atomBoundary(8).build();
        assertThat(CNFEncoder.encode(_c.f, phi1, config2)).isEqualTo(_c.p.parse("@RESERVED_CNF_1 & x3 & x4 & @RESERVED_CNF_2 & (~@RESERVED_CNF_1 | x1 | x2) & " +
                "(~@RESERVED_CNF_2 | @RESERVED_CNF_3 | x8 | x9) & (~@RESERVED_CNF_3 | x1) & (~@RESERVED_CNF_3 | x5) & (~@RESERVED_CNF_3 | ~x6) & (~@RESERVED_CNF_3 | ~x7)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBDDEncoder(final FormulaContext _c) throws ParserException {
        final Formula phi1 = _c.p.parse(p1);
        final Formula phi2 = _c.p.parse(p2);
        final Formula phi3 = _c.p.parse(p3);
        final CNFConfig config = CNFConfig.builder().algorithm(CNFConfig.Algorithm.BDD).build();
        final Formula phi1CNF = CNFEncoder.encode(_c.f, phi1, config);
        assertThat(phi1CNF.isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(phi1, phi1CNF, phi1.variables(_c.f))).isTrue();
        final Formula phi2CNF = CNFEncoder.encode(_c.f, phi2, config);
        assertThat(phi2CNF.isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(phi2, phi2CNF, phi2.variables(_c.f))).isTrue();
        final Formula phi3CNF = CNFEncoder.encode(_c.f, phi3, config);
        assertThat(phi3CNF.isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(phi3, phi3CNF, phi3.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAdvancedEncoder(final FormulaContext _c) throws ParserException {
        final Formula phi1 = _c.p.parse(p1);
        final Formula phi2 = _c.p.parse(p2);
        final Formula phi3 = _c.p.parse(p3);
        assertThat(CNFEncoder.encode(_c.f, phi1)).isEqualTo(_c.p.parse("(x1 | x2) & x3 & x4 & (x1 | x8 | x9) & (x5 | x8 | x9) & (~x6 | x8 | x9) & (~x7 | x8 |" +
                " x9)"));
        final CNFConfig config1 = CNFConfig.builder().createdClauseBoundary(5).atomBoundary(3).build();
        assertThat(CNFEncoder.encode(_c.f, phi2, config1)).isEqualTo(_c.p.parse("(y1 | y2) & y3 & y4 & (~@RESERVED_CNF_0 | y1) & (~@RESERVED_CNF_0 | y5) & " +
                "(~@RESERVED_CNF_0 | ~y6) & (~@RESERVED_CNF_0 | ~y7) & (@RESERVED_CNF_0 | ~y1 | ~y5 | y6 | y7) & (@RESERVED_CNF_0 | y8 | y9)"));
        final CNFConfig config2 = CNFConfig.builder().createdClauseBoundary(-1).distributionBoundary(5).atomBoundary(3).build();
        assertThat(CNFEncoder.encode(_c.f, phi3, config2)).isEqualTo(_c.p.parse("(z1 | z2) & z3 & z4 & (~@RESERVED_CNF_2 | z1) & (~@RESERVED_CNF_2 | z5) & " +
                "(~@RESERVED_CNF_2 | ~z6) & (~@RESERVED_CNF_2 | ~z7) & (@RESERVED_CNF_2 | ~z1 | ~z5 | z6 | z7) & (@RESERVED_CNF_2 | z8 | z9)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBugIssueNo4(final FormulaContext _c) throws ParserException {
        final Formula f1 = _c.p.parse("(x10 & x9 & x3 & x12 | x10 & x9 & x8 | x9 & x8 & x12) & ~x5 & ~x7 & x1 | (x10 & x9 & x3 & x12 | x10 & x9 & x8 | x9 & x8 & x12) & ~(x11 & x3) & ~(x11 & x8) & ~x5 & ~x7 & x0");
        final Formula f2 = _c.p.parse("x1 & x3 & x4");
        final Formula f3 = _c.p.parse("(x10 & x9 & x3 & x12 | x10 & x9 & x8 | x9 & x8 & x12) & ~(x11 & x3) & ~(x11 & x8 & x12) & ~x5 & ~x7 & x1 | (x10 & x9 & x3 & x12 | x10 & x9 & x8 | x9 & x8 & x12) & ~(x11 & x3) & ~(x11 & x8) & ~x5 & ~x7 & x0 | x3 & x4 & ~x5 & ~x7 & x1 | x3 & x4 & ~x5 & ~x7 & x0 | x2 & x6 & ~x5 & ~x7 & x0");
        final Formula f4 = _c.p.parse("(x1 & x3 & x4 | x0 & (x2 & x6 | x3 & x4) | x9 & (x1 & x10 & x8 & ~x12 & x3 | (x1 | x0) & (x12 & (x10 & x3 | x8) | x10 & x8) & ~x11)) & ~x5 & ~x7");
        assertThat(_c.f.not(_c.f.equivalence(f1, f2)).cnf(_c.f)).isNotEqualTo(null);
        assertThat(_c.f.not(_c.f.equivalence(f3, f4)).cnf(_c.f)).isNotEqualTo(null);
    }

    @Test
    public void testStrings() {
        final String expected = String.format("CNFConfig{%n" +
                "algorithm=TSEITIN%n" +
                "fallbackAlgorithmForAdvancedEncoding=PLAISTED_GREENBAUM%n" +
                "distributedBoundary=-1%n" +
                "createdClauseBoundary=1000%n" +
                "atomBoundary=12%n" +
                "}%n");
        final CNFConfig config = CNFConfig.builder().algorithm(CNFConfig.Algorithm.TSEITIN).fallbackAlgorithmForAdvancedEncoding(CNFConfig.Algorithm.PLAISTED_GREENBAUM).build();
        assertThat(config.toString()).isEqualTo(expected);
        assertThat(CNFConfig.Algorithm.valueOf("TSEITIN")).isEqualTo(CNFConfig.Algorithm.TSEITIN);
    }

    @Test
    public void testWrongFallbackForConfig() {
        assertThatThrownBy(() -> CNFConfig.builder().fallbackAlgorithmForAdvancedEncoding(CNFConfig.Algorithm.FACTORIZATION).build()).isInstanceOf(IllegalArgumentException.class);
    }

    private boolean equivalentModels(final Formula f1, final Formula f2, final SortedSet<Variable> vars) {
        final SATSolver s = MiniSat.miniSat(f1.factory());
        s.add(f1);
        final List<Assignment> models1 = s.enumerateAllModels(vars);
        s.reset();
        s.add(f2);
        final List<Assignment> models2 = s.enumerateAllModels(vars);
        if (models1.size() != models2.size()) {
            return false;
        }
        for (final Assignment model : models1) {
            if (!models2.contains(model)) {
                return false;
            }
        }
        return true;
    }
}
