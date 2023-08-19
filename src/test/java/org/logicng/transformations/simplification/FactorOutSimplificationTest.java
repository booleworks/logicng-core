// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.simplification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.RandomTag;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.io.parsers.ParserException;
import org.logicng.predicates.satisfiability.TautologyPredicate;
import org.logicng.util.FormulaCornerCases;
import org.logicng.util.FormulaRandomizer;
import org.logicng.util.FormulaRandomizerConfig;

public class FactorOutSimplificationTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimple(final FormulaContext _c) throws ParserException {
        final FactorOutSimplifier factorOut = new FactorOutSimplifier(_c.f);

        assertThat(_c.f.falsum().transform(factorOut)).isEqualTo(_c.f.falsum());
        assertThat(_c.f.verum().transform(factorOut)).isEqualTo(_c.f.verum());
        assertThat(_c.a.transform(factorOut)).isEqualTo(_c.a);
        assertThat(_c.na.transform(factorOut)).isEqualTo(_c.na);

        assertThat(_c.f.parse("A&~B&~C&~D").transform(factorOut)).isEqualTo(_c.f.parse("A&~B&~C&~D"));
        assertThat(_c.f.parse("~A&~B&~C&~D").transform(factorOut)).isEqualTo(_c.f.parse("~A&~B&~C&~D"));

        assertThat(_c.f.parse("A|A&B").transform(factorOut)).isEqualTo(_c.f.parse("A"));
        assertThat(_c.f.parse("A|A&B|C&D").transform(factorOut)).isEqualTo(_c.f.parse("A|C&D"));
        assertThat(_c.f.parse("~(A&(A|B))").transform(factorOut)).isEqualTo(_c.f.parse("~A"));
        assertThat(_c.f.parse("A|A&B|C").transform(factorOut)).isEqualTo(_c.f.parse("A|C"));

        assertThat(_c.f.parse("A&(A|B)").transform(factorOut)).isEqualTo(_c.f.parse("A"));
        assertThat(_c.f.parse("A&(A|B)&(C|D)").transform(factorOut)).isEqualTo(_c.f.parse("A&(C|D)"));
        assertThat(_c.f.parse("~(A|A&B)").transform(factorOut)).isEqualTo(_c.f.parse("~A"));
        assertThat(_c.f.parse("A&(A|B)&C").transform(factorOut)).isEqualTo(_c.f.parse("A&C"));

        assertThat(_c.f.parse("A&X&Y|A&B&C|B&C&D|A&Z").transform(factorOut)).isEqualTo(_c.f.parse("A&(X&Y|B&C|Z)|B&C&D"));
        assertThat(_c.f.parse("G&(A&X&Y|A&B&C|B&C&D|A&Z)").transform(factorOut)).isEqualTo(_c.f.parse("G&(A&(X&Y|B&C|Z)|B&C&D)"));

        assertThat(_c.f.parse("G&(~(A&X&Y)|~(A&B&C))").transform(factorOut)).isEqualTo(_c.f.parse("G&(~(A&X&Y)|~(A&B&C))"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCornerCases(final FormulaContext _c) {
        final FormulaCornerCases cornerCases = new FormulaCornerCases(_c.f);
        cornerCases.cornerCases().forEach(it -> computeAndVerify(it, _c.f));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void testRandomized(final FormulaContext _c) {
        for (int i = 0; i < 100; i++) {
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f, FormulaRandomizerConfig.builder().numVars(5).weightPbc(2).seed(i * 42).build());
            final Formula formula = randomizer.formula(6);
            computeAndVerify(formula, _c.f);
            computeAndVerify(formula.nnf(_c.f), _c.f);
        }
    }

    private void computeAndVerify(final Formula formula, final FormulaFactory f) {
        final Formula simplified = formula.transform(new FactorOutSimplifier(f));
        assertThat(formula.factory().equivalence(formula, simplified).holds(new TautologyPredicate(f))).isTrue();
        assertThat(simplified.toString().length()).isLessThanOrEqualTo(formula.toString().length());
    }
}
