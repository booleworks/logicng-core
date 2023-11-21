// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.qe;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

public class QETest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final ExistentialQuantifierElimination ex1 = new ExistentialQuantifierElimination(_c.f);
        final ExistentialQuantifierElimination ex2 = new ExistentialQuantifierElimination(_c.f, _c.f.variable("x"));
        final ExistentialQuantifierElimination ex3 = new ExistentialQuantifierElimination(_c.f, Arrays.asList(_c.f.variable("x"), _c.f.variable("y")));
        final UniversalQuantifierElimination uni1 = new UniversalQuantifierElimination(_c.f);
        final UniversalQuantifierElimination uni2 = new UniversalQuantifierElimination(_c.f, _c.f.variable("x"));
        final UniversalQuantifierElimination uni3 = new UniversalQuantifierElimination(_c.f, Arrays.asList(_c.f.variable("x"), _c.f.variable("y")));

        assertThat(_c.f.verum().transform(ex1)).isEqualTo(_c.f.verum());
        assertThat(_c.f.verum().transform(ex2)).isEqualTo(_c.f.verum());
        assertThat(_c.f.verum().transform(ex3)).isEqualTo(_c.f.verum());
        assertThat(_c.f.verum().transform(uni1)).isEqualTo(_c.f.verum());
        assertThat(_c.f.verum().transform(uni2)).isEqualTo(_c.f.verum());
        assertThat(_c.f.verum().transform(uni3)).isEqualTo(_c.f.verum());
        assertThat(_c.f.falsum().transform(ex1)).isEqualTo(_c.f.falsum());
        assertThat(_c.f.falsum().transform(ex2)).isEqualTo(_c.f.falsum());
        assertThat(_c.f.falsum().transform(ex3)).isEqualTo(_c.f.falsum());
        assertThat(_c.f.falsum().transform(uni1)).isEqualTo(_c.f.falsum());
        assertThat(_c.f.falsum().transform(uni2)).isEqualTo(_c.f.falsum());
        assertThat(_c.f.falsum().transform(uni3)).isEqualTo(_c.f.falsum());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final ExistentialQuantifierElimination ex1 = new ExistentialQuantifierElimination(_c.f);
        final ExistentialQuantifierElimination ex2 = new ExistentialQuantifierElimination(_c.f, _c.f.variable("x"));
        final ExistentialQuantifierElimination ex3 = new ExistentialQuantifierElimination(_c.f, Arrays.asList(_c.f.variable("x"), _c.f.variable("y")));
        final UniversalQuantifierElimination uni1 = new UniversalQuantifierElimination(_c.f);
        final UniversalQuantifierElimination uni2 = new UniversalQuantifierElimination(_c.f, _c.f.variable("x"));
        final UniversalQuantifierElimination uni3 = new UniversalQuantifierElimination(_c.f, Arrays.asList(_c.f.variable("x"), _c.f.variable("y")));

        final Formula x = _c.f.variable("x");
        final Formula y = _c.f.literal("y", false);
        final Formula z = _c.f.variable("z");
        assertThat(x.transform(ex1)).isEqualTo(x);
        assertThat(x.transform(ex2)).isEqualTo(_c.f.verum());
        assertThat(x.transform(ex3)).isEqualTo(_c.f.verum());
        assertThat(x.transform(uni1)).isEqualTo(x);
        assertThat(x.transform(uni2)).isEqualTo(_c.f.falsum());
        assertThat(x.transform(uni3)).isEqualTo(_c.f.falsum());
        assertThat(y.transform(ex1)).isEqualTo(y);
        assertThat(y.transform(ex2)).isEqualTo(y);
        assertThat(y.transform(ex3)).isEqualTo(_c.f.verum());
        assertThat(y.transform(uni1)).isEqualTo(y);
        assertThat(y.transform(uni2)).isEqualTo(y);
        assertThat(y.transform(uni3)).isEqualTo(_c.f.falsum());
        assertThat(z.transform(ex1)).isEqualTo(z);
        assertThat(z.transform(ex2)).isEqualTo(z);
        assertThat(z.transform(ex3)).isEqualTo(z);
        assertThat(z.transform(uni1)).isEqualTo(z);
        assertThat(z.transform(uni2)).isEqualTo(z);
        assertThat(z.transform(uni3)).isEqualTo(z);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFormulas(final FormulaContext _c) throws ParserException {
        final ExistentialQuantifierElimination ex1 = new ExistentialQuantifierElimination(_c.f);
        final ExistentialQuantifierElimination ex2 = new ExistentialQuantifierElimination(_c.f, _c.f.variable("x"));
        final ExistentialQuantifierElimination ex3 = new ExistentialQuantifierElimination(_c.f, Arrays.asList(_c.f.variable("x"), _c.f.variable("y")));
        final UniversalQuantifierElimination uni1 = new UniversalQuantifierElimination(_c.f);
        final UniversalQuantifierElimination uni2 = new UniversalQuantifierElimination(_c.f, _c.f.variable("x"));
        final UniversalQuantifierElimination uni3 = new UniversalQuantifierElimination(_c.f, Arrays.asList(_c.f.variable("x"), _c.f.variable("y")));

        final Formula f1 = _c.p.parse("a & (b | ~c)");
        final Formula f2 = _c.p.parse("x & (b | ~c)");
        final Formula f3 = _c.p.parse("x & (y | ~c)");
        assertThat(f1.transform(ex1)).isEqualTo(f1);
        assertThat(f1.transform(ex2)).isEqualTo(f1);
        assertThat(f1.transform(ex3)).isEqualTo(f1);
        assertThat(f1.transform(uni1)).isEqualTo(f1);
        assertThat(f1.transform(uni2)).isEqualTo(f1);
        assertThat(f1.transform(uni3)).isEqualTo(f1);
        assertThat(f2.transform(ex1)).isEqualTo(f2);
        assertThat(f2.transform(ex2)).isEqualTo(_c.p.parse("b | ~c"));
        assertThat(f2.transform(ex3)).isEqualTo(_c.p.parse("b | ~c"));
        assertThat(f2.transform(uni1)).isEqualTo(f2);
        assertThat(f2.transform(uni2)).isEqualTo(_c.f.falsum());
        assertThat(f2.transform(uni3)).isEqualTo(_c.f.falsum());
        assertThat(f3.transform(ex1)).isEqualTo(f3);
        assertThat(f3.transform(ex2)).isEqualTo(_c.p.parse("y | ~c"));
        assertThat(f3.transform(ex3)).isEqualTo(_c.f.verum());
        assertThat(f3.transform(uni1)).isEqualTo(f3);
        assertThat(f3.transform(uni2)).isEqualTo(_c.f.falsum());
        assertThat(f3.transform(uni3)).isEqualTo(_c.f.falsum());
    }
}
