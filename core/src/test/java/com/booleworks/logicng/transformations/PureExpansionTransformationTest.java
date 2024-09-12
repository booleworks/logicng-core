// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.modelcounting.ModelCounter;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import com.booleworks.logicng.util.FormulaCornerCases;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;

public class PureExpansionTransformationTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        final PureExpansionTransformation transformation = new PureExpansionTransformation(_c.f);

        computeAndVerify(_c.falsum, transformation);
        computeAndVerify(_c.verum, transformation);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        final PureExpansionTransformation transformation = new PureExpansionTransformation(_c.f);

        assertThat(_c.a.transform(transformation)).isEqualTo(_c.a);
        assertThat(_c.na.transform(transformation)).isEqualTo(_c.na);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNot(final FormulaContext _c) throws ParserException {
        final PureExpansionTransformation transformation = new PureExpansionTransformation(_c.f);

        assertThat(_c.not1.transform(transformation)).isEqualTo(_c.not1);
        assertThat(_c.not2.transform(transformation)).isEqualTo(_c.not2);

        assertThat(_c.f.parse("~a").transform(transformation)).isEqualTo(_c.f.parse("~a"));
        assertThat(_c.f.parse("~(a => b)").transform(transformation)).isEqualTo(_c.f.parse("~(a => b)"));
        assertThat(_c.f.parse("~(~(a | b) => ~(x | y))").transform(transformation))
                .isEqualTo(_c.f.parse("~(~(a | b) => ~(x | y))"));
        assertThat(_c.f.parse("~(a <=> b)").transform(transformation)).isEqualTo(_c.f.parse("~(a <=> b)"));
        assertThat(_c.f.parse("~(a & b & ~x & ~y)").transform(transformation))
                .isEqualTo(_c.f.parse("~(a & b & ~x & ~y)"));
        assertThat(_c.f.parse("~(a | b | (a + b <= 1) | ~y)").transform(transformation))
                .isEqualTo(_c.f.parse("~(a | b | (~a | ~b) | ~y)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBinaryOperators(final FormulaContext _c) throws ParserException {
        final PureExpansionTransformation transformation = new PureExpansionTransformation(_c.f);

        assertThat(_c.imp1.transform(transformation)).isEqualTo(_c.imp1);
        assertThat(_c.imp2.transform(transformation)).isEqualTo(_c.imp2);
        assertThat(_c.imp3.transform(transformation)).isEqualTo(_c.imp3);
        assertThat(_c.imp4.transform(transformation)).isEqualTo(_c.imp4);
        assertThat(_c.eq1.transform(transformation)).isEqualTo(_c.eq1);
        assertThat(_c.eq2.transform(transformation)).isEqualTo(_c.eq2);
        assertThat(_c.eq3.transform(transformation)).isEqualTo(_c.eq3);
        assertThat(_c.eq4.transform(transformation)).isEqualTo(_c.eq4);

        assertThat(_c.f.parse("~(a => (a + b = 1))").transform(transformation))
                .isEqualTo(_c.f.parse("~(a => (a | b) & (~a | ~b))"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNAryOperators(final FormulaContext _c) throws ParserException {
        final PureExpansionTransformation transformation = new PureExpansionTransformation(_c.f);

        assertThat(_c.and1.transform(transformation)).isEqualTo(_c.and1);
        assertThat(_c.and2.transform(transformation)).isEqualTo(_c.and2);
        assertThat(_c.and3.transform(transformation)).isEqualTo(_c.and3);
        assertThat(_c.or1.transform(transformation)).isEqualTo(_c.or1);
        assertThat(_c.or2.transform(transformation)).isEqualTo(_c.or2);
        assertThat(_c.or3.transform(transformation)).isEqualTo(_c.or3);

        assertThat(_c.f.parse("~(a & b) | c | ~(x | ~y)").transform(transformation))
                .isEqualTo(_c.f.parse("~(a & b) | c | ~(x | ~y)"));
        assertThat(_c.f.parse("~(a | b) & (a + b = 1) & ~(x & ~(z + x = 1))").transform(transformation))
                .isEqualTo(_c.f.parse("~(a | b) & ((a | b) & (~a | ~b)) & ~(x & ~((z | x) & (~z | ~x)))"));
        assertThat(_c.f.parse("a & b & (~x | ~y)").transform(transformation))
                .isEqualTo(_c.f.parse("a & b & (~x | ~y)"));
        assertThat(_c.f.parse("~(a | b) & c & ~(x & ~y) & (w => z)").transform(transformation))
                .isEqualTo(_c.f.parse("~(a | b) & c & ~(x & ~y) & (w => z)"));
        assertThat(_c.f.parse("~(a & b) | c | ~(x | ~y)").transform(transformation))
                .isEqualTo(_c.f.parse("~(a & b) | c | ~(x | ~y)"));
        assertThat(_c.f.parse("a | b | (~x & ~y)").transform(transformation))
                .isEqualTo(_c.f.parse("a | b | (~x & ~y)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPBCs(final FormulaContext _c) throws ParserException {
        final PureExpansionTransformation transformation = new PureExpansionTransformation(_c.f);

        assertThat(_c.f.parse("a + b <= 1").transform(transformation)).isEqualTo(_c.f.parse("~a | ~b"));
        assertThat(_c.f.parse("a + b < 2").transform(transformation)).isEqualTo(_c.f.parse("~a | ~b"));
        assertThat(_c.f.parse("a + b = 1").transform(transformation)).isEqualTo(_c.f.parse("(a | b) & (~a | ~b)"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testExceptionalBehavior(final FormulaContext _c) {
        final PureExpansionTransformation transformation = new PureExpansionTransformation(_c.f);

        assertThatThrownBy(() -> _c.pbc1.transform(transformation))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.pbc2.transform(transformation))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.pbc3.transform(transformation))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.pbc4.transform(transformation))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.pbc5.transform(transformation))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCornerCases(final FormulaContext _c) {
        final PureExpansionTransformation transformation = new PureExpansionTransformation(_c.f);

        final FormulaCornerCases cornerCases = new FormulaCornerCases(_c.f);
        for (final Formula formula : cornerCases.cornerCases()) {
            if (formula.type() == FType.PBC) {
                final PBConstraint pbc = (PBConstraint) formula;
                if (!pbc.isAmo() && !pbc.isExo()) {
                    assertThatThrownBy(
                            () -> ModelCounter.count(_c.f, Collections.singletonList(formula), formula.variables(_c.f)))
                                    .isInstanceOf(UnsupportedOperationException.class);
                    continue;
                }
            }
            computeAndVerify(formula, transformation);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void testRandom(final FormulaContext _c) {
        final PureExpansionTransformation transformation = new PureExpansionTransformation(_c.f);

        for (int i = 0; i < 200; i++) {
            final FormulaRandomizerConfig config = FormulaRandomizerConfig.builder()
                    .numVars(12).weightAmo(5).weightExo(5).seed(i * 42).build();
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f, config);
            final Formula formula = randomizer.formula(5);
            computeAndVerify(formula, transformation);
        }
    }

    private static void computeAndVerify(final Formula formula, final PureExpansionTransformation transformation) {
        final Formula expandedFormula = formula.transform(transformation);
        verify(formula, expandedFormula);
    }

    private static void verify(final Formula formula, final Formula expandedFormula) {
        final FormulaFactory f = formula.factory();
        assertThat(f.equivalence(formula, expandedFormula).holds(new TautologyPredicate(f))).isTrue();
        assertThat(isFreeOfPBCs(expandedFormula)).isTrue();
    }

    private static boolean isFreeOfPBCs(final Formula formula) {
        switch (formula.type()) {
            case FALSE:
            case TRUE:
            case LITERAL:
                return true;
            case NOT:
                return isFreeOfPBCs(((Not) formula).operand());
            case OR:
            case AND:
                for (final Formula op : formula) {
                    if (!isFreeOfPBCs(op)) {
                        return false;
                    }
                }
                return true;
            case IMPL:
            case EQUIV:
                final BinaryOperator binary = (BinaryOperator) formula;
                return isFreeOfPBCs(binary.left()) && isFreeOfPBCs(binary.right());
            case PBC:
                return false;
            default:
                throw new IllegalStateException("Unknown formula type: " + formula.type());
        }
    }
}
