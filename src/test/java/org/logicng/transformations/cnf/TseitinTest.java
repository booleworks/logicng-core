// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.cnf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

import java.util.List;
import java.util.SortedSet;

public class TseitinTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final TseitinTransformation ts = new TseitinTransformation(_c.f, 0);

        assertThat(_c.verum.transform(ts)).isEqualTo(_c.verum);
        assertThat(_c.falsum.transform(ts)).isEqualTo(_c.falsum);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final TseitinTransformation ts = new TseitinTransformation(_c.f, 0);

        assertThat(_c.a.transform(ts)).isEqualTo(_c.a);
        assertThat(_c.na.transform(ts)).isEqualTo(_c.na);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryOperators(final FormulaContext _c) {
        final TseitinTransformation ts = new TseitinTransformation(_c.f, 0);

        assertThat(_c.imp1.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.imp1, _c.imp1.transform(ts), _c.imp1.variables(_c.f))).isTrue();
        assertThat(_c.imp2.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.imp2, _c.imp2.transform(ts), _c.imp2.variables(_c.f))).isTrue();
        assertThat(_c.imp3.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.imp3, _c.imp3.transform(ts), _c.imp3.variables(_c.f))).isTrue();
        assertThat(_c.eq1.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.eq1, _c.eq1.transform(ts), _c.eq1.variables(_c.f))).isTrue();
        assertThat(_c.eq2.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.eq2, _c.eq2.transform(ts), _c.eq2.variables(_c.f))).isTrue();
        assertThat(_c.eq3.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.eq3, _c.eq3.transform(ts), _c.eq3.variables(_c.f))).isTrue();
        assertThat(_c.eq4.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(_c.eq4, _c.eq4.transform(ts), _c.eq4.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperators(final FormulaContext _c) throws ParserException {
        final TseitinTransformation ts = new TseitinTransformation(_c.f, 0);

        assertThat(_c.and1.transform(ts)).isEqualTo(_c.and1);
        assertThat(_c.or1.transform(ts)).isEqualTo(_c.or1);
        final Formula f1 = _c.p.parse("~(a | b) & c & ~(x & ~y) & (w => z)");
        final Formula f2 = _c.p.parse("~(a & b) | c | ~(x | ~y)");
        final Formula f3 = _c.p.parse("a | b | (~x & ~y)");
        assertThat(f1.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f1, f1.transform(ts), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f2, f2.transform(ts), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f3, f3.transform(ts), f3.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        final TseitinTransformation ts = new TseitinTransformation(_c.f, 0);

        assertThat(_c.p.parse("~a").transform(ts)).isEqualTo(_c.p.parse("~a"));
        assertThat(_c.p.parse("~~a").transform(ts)).isEqualTo(_c.p.parse("a"));
        assertThat(_c.p.parse("~(a => b)").transform(ts)).isEqualTo(_c.p.parse("a & ~b"));
        final Formula f1 = _c.p.parse("~(~(a | b) => ~(x | y))");
        final Formula f2 = _c.p.parse("~(a <=> b)");
        final Formula f3 = _c.p.parse("~(~(a | b) <=> ~(x | y))");
        final Formula f4 = _c.p.parse("~(a & b & ~x & ~y)");
        final Formula f5 = _c.p.parse("~(a | b | ~x | ~y)");
        assertThat(f1.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f1, f1.transform(ts), f1.variables(_c.f))).isTrue();
        assertThat(f2.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f2, f2.transform(ts), f2.variables(_c.f))).isTrue();
        assertThat(f3.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f3, f3.transform(ts), f3.variables(_c.f))).isTrue();
        assertThat(f4.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f4, f4.transform(ts), f4.variables(_c.f))).isTrue();
        assertThat(f5.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f5, f5.transform(ts), f5.variables(_c.f))).isTrue();
        assertThat(f5.transform(ts).isCNF(_c.f)).isTrue();
        assertThat(equivalentModels(f5, f5.transform(ts), f5.variables(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFactorization(final FormulaContext _c) throws ParserException {
        final TseitinTransformation pgf = new TseitinTransformation(_c.f);
        final Formula f1 = _c.p.parse("(a | b) => c");
        final Formula f2 = _c.p.parse("~x & ~y");
        final Formula f3 = _c.p.parse("d & ((a | b) => c)");
        final Formula f4 = _c.p.parse("d & ((a | b) => c) | ~x & ~y");
        assertThat(f1.transform(pgf).isCNF(_c.f)).isTrue();
        assertThat(f1.transform(pgf).variables(_c.f).size()).isEqualTo(f1.variables(_c.f).size());
        assertThat(f2.transform(pgf).isCNF(_c.f)).isTrue();
        assertThat(f2.transform(pgf).variables(_c.f).size()).isEqualTo(f2.variables(_c.f).size());
        assertThat(f3.transform(pgf).isCNF(_c.f)).isTrue();
        assertThat(f3.transform(pgf).variables(_c.f).size()).isEqualTo(f3.variables(_c.f).size());
        assertThat(f4.transform(pgf).isCNF(_c.f)).isTrue();
        assertThat(f4.transform(pgf).variables(_c.f).size()).isEqualTo(f4.variables(_c.f).size());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCC(final FormulaContext _c) throws ParserException {
        final TseitinTransformation ts = new TseitinTransformation(_c.f, 0);

        assertThat(_c.p.parse("a <=> (1 * b <= 1)").transform(ts)).isEqualTo(_c.p.parse("a"));
        assertThat(_c.p.parse("~(1 * b <= 1)").transform(ts)).isEqualTo(_c.p.parse("$false"));
        assertThat(_c.p.parse("(1 * b + 1 * c + 1 * d <= 1)").transform(ts)).isEqualTo(_c.p.parse("(~b | ~c) & (~b | ~d) & (~c | ~d)"));
        assertThat(_c.p.parse("~(1 * b + 1 * c + 1 * d <= 1)").transform(ts)).isEqualTo(_c.p.parse("(d | @RESERVED_CC_1 | @RESERVED_CC_4) & (~@RESERVED_CC_3 | @RESERVED_CC_1 | @RESERVED_CC_4) & (~@RESERVED_CC_3 | d | @RESERVED_CC_4) & (~@RESERVED_CC_4 | @RESERVED_CC_0) & (~@RESERVED_CC_2 | @RESERVED_CC_0) & (~@RESERVED_CC_4 | ~@RESERVED_CC_2) & (c | @RESERVED_CC_3 | @RESERVED_CC_5) & (b | @RESERVED_CC_3 | @RESERVED_CC_5) & (b | c | @RESERVED_CC_5) & (~@RESERVED_CC_5 | @RESERVED_CC_2) & ~@RESERVED_CC_0"));
    }

    @Test
    public void testToString() {
        final TseitinTransformation tseitinTransformation = new TseitinTransformation(FormulaFactory.nonCaching(), 5);
        assertThat(tseitinTransformation.toString()).isEqualTo("TseitinTransformation{boundary=5}");
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
