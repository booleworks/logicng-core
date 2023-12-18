// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.util.FormulaCornerCases;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class NegationMinimizerTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimple(final FormulaContext _c) throws ParserException {
        final NegationSimplifier minimizer = new NegationSimplifier(_c.f);

        assertThat(_c.f.falsum().transform(minimizer)).isEqualTo(_c.f.falsum());
        assertThat(_c.f.verum().transform(minimizer)).isEqualTo(_c.f.verum());
        assertThat(_c.a.transform(minimizer)).isEqualTo(_c.a);
        assertThat(_c.na.transform(minimizer)).isEqualTo(_c.na);

        assertThat(_c.f.parse("A&~B&~C&~D").transform(minimizer)).isEqualTo(_c.f.parse("A&~B&~C&~D"));
        assertThat(_c.f.parse("~A&~B&~C&~D").transform(minimizer)).isEqualTo(_c.f.parse("~(A|B|C|D)"));

        assertThat(_c.f.parse("A|~B|~C|~D").transform(minimizer)).isEqualTo(_c.f.parse("A|~B|~C|~D"));
        assertThat(_c.f.parse("~A|~B|~C|~D").transform(minimizer)).isEqualTo(_c.f.parse("~(A&B&C&D)"));

        assertThat(_c.f.parse("~A|~B|~C|D|~E|~G").transform(minimizer)).isEqualTo(_c.f.parse("D|~(A&B&C&E&G)"));
        assertThat(_c.f.parse("~A&~B&~C&D&~E&~G").transform(minimizer)).isEqualTo(_c.f.parse("D&~(A|B|C|E|G)"));

        assertThat(_c.f.parse("~A|~B|~E&G|~H&~B&~C|~X").transform(minimizer))
                .isEqualTo(_c.f.parse("~E&G|~(A&B&(H|B|C)&X)"));
        assertThat(_c.f.parse("~(A&B&~(~E&G)&(H|B|C)&X)").transform(minimizer))
                .isEqualTo(_c.f.parse("~E&G|~(A&B&(H|B|C)&X)"));

        assertThat(_c.f.parse("~A|B|(~E&~G&~H&~K)").transform(minimizer)).isEqualTo(_c.f.parse("~A|B|~(E|G|H|K)"));

        assertThat(_c.f.parse("~A|~B").transform(minimizer)).isEqualTo(_c.f.parse("~A|~B"));
        assertThat(_c.f.parse("~A|~B|~C").transform(minimizer)).isEqualTo(_c.f.parse("~A|~B|~C"));
        assertThat(_c.f.parse("~A|~B|~C|~D").transform(minimizer)).isEqualTo(_c.f.parse("~(A&B&C&D)"));

        assertThat(_c.f.parse("X&(~A|~B)").transform(minimizer)).isEqualTo(_c.f.parse("X&~(A&B)"));
        assertThat(_c.f.parse("X&(~A|~B|~C)").transform(minimizer)).isEqualTo(_c.f.parse("X&~(A&B&C)"));
        assertThat(_c.f.parse("X&(~A|~B|~C|~D)").transform(minimizer)).isEqualTo(_c.f.parse("X&~(A&B&C&D)"));

        assertThat(_c.f.parse("~A&~B").transform(minimizer)).isEqualTo(_c.f.parse("~A&~B"));
        assertThat(_c.f.parse("~A&~B&~C").transform(minimizer)).isEqualTo(_c.f.parse("~A&~B&~C"));
        assertThat(_c.f.parse("~A&~B&~C&~D").transform(minimizer)).isEqualTo(_c.f.parse("~(A|B|C|D)"));

        assertThat(_c.f.parse("X|~A&~B").transform(minimizer)).isEqualTo(_c.f.parse("X|~A&~B"));
        assertThat(_c.f.parse("X|~A&~B&~C").transform(minimizer)).isEqualTo(_c.f.parse("X|~A&~B&~C"));
        assertThat(_c.f.parse("X|~A&~B&~C&~D").transform(minimizer)).isEqualTo(_c.f.parse("X|~(A|B|C|D)"));

        assertThat(_c.f.parse("A&(~B|~C|~D|~E|~G|X|Y|H)").transform(minimizer))
                .isEqualTo(_c.f.parse("A&(~(B&C&D&E&G)|X|Y|H)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCornerCases(final FormulaContext _c) {
        final FormulaCornerCases cornerCases = new FormulaCornerCases(_c.f);
        cornerCases.cornerCases().forEach(NegationMinimizerTest::computeAndVerify);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void testRandomized(final FormulaContext _c) {
        for (int i = 0; i < 100; i++) {
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f,
                    FormulaRandomizerConfig.builder().numVars(5).weightPbc(1).seed(i * 42).build());
            computeAndVerify(randomizer.formula(6));
        }
    }

    private static void computeAndVerify(final Formula formula) {
        final FormulaFactory f = formula.factory();
        final Formula simplified = formula.transform(new NegationSimplifier(f));
        assertThat(formula.isEquivalentTo(f, simplified)).isTrue();
        assertThat(simplified.toString().length()).isLessThanOrEqualTo(formula.toString().length());
    }
}
