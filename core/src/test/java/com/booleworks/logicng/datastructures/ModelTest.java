// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for the class {@link Model}.
 * @version 2.5.0
 * @since 2.5.0
 */
public class ModelTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCreators(final FormulaContext _c) {
        assertThat(new Model(Collections.emptyList()).getLiterals()).isEmpty();
        assertThat(new Model(Arrays.asList(_c.a, _c.nb, _c.x)).getLiterals())
                .containsExactly(_c.a, _c.nb, _c.x);
        assertThat(new Model().getLiterals()).isEmpty();
        assertThat(new Model(_c.a, _c.nb, _c.x).getLiterals())
                .containsExactly(_c.a, _c.nb, _c.x);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSize(final FormulaContext _c) {
        assertThat(new Model(Collections.emptyList()).size()).isEqualTo(0);
        assertThat(new Model(Collections.singletonList(_c.a)).size()).isEqualTo(1);
        assertThat(new Model(Arrays.asList(_c.a, _c.nb, _c.x)).size()).isEqualTo(3);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testToAssignment(final FormulaContext _c) {
        assertThat(new Model(Collections.emptyList()).toAssignment()).isEqualTo(new Assignment());
        assertThat(new Model(Arrays.asList(_c.a, _c.nb, _c.x)).toAssignment())
                .isEqualTo(new Assignment(_c.a, _c.nb, _c.x));
        assertThat(new Model(Collections.emptyList()).toAssignment()).isEqualTo(new Assignment());
        assertThat(new Model(Arrays.asList(_c.a, _c.nb, _c.x)).toAssignment())
                .isEqualTo(new Assignment(_c.a, _c.nb, _c.x));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFormula(final FormulaContext _c) throws ParserException {
        final PropositionalParser p = new PropositionalParser(_c.f);
        assertThat(new Model(Collections.singletonList(_c.a)).formula(_c.f)).isEqualTo(p.parse("a"));
        assertThat(new Model(Collections.singletonList(_c.na)).formula(_c.f)).isEqualTo(p.parse("~a"));
        assertThat(new Model(Arrays.asList(_c.a, _c.b)).formula(_c.f)).isEqualTo(p.parse("a & b"));
        assertThat(new Model(Arrays.asList(_c.a, _c.b, _c.nx, _c.ny)).formula(_c.f))
                .isEqualTo(p.parse("a & b & ~x & ~y"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBlockingClause(final FormulaContext _c) throws ParserException {
        final Model ass = new Model(_c.a, _c.b, _c.nx, _c.ny);
        final Formula bc01 = ass.blockingClause(_c.f);
        assertThat(bc01.containsVariable(_c.c)).isFalse();
        assertThat(bc01).isEqualTo(_c.f.parse("~a | ~b | x | y"));
        final Formula bc02 = ass.blockingClause(_c.f, null);
        assertThat(bc02.containsVariable(_c.c)).isFalse();
        assertThat(bc02).isEqualTo(_c.f.parse("~a | ~b | x | y"));
        final List<Literal> lits = Arrays.asList(_c.a, _c.x, _c.c);
        final Formula bcProjected = ass.blockingClause(_c.f, lits);
        assertThat(bcProjected.containsVariable(_c.c)).isFalse();
        assertThat(bcProjected).isEqualTo(_c.f.parse("~a | x"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testHashCode(final FormulaContext _c) {
        final Model model = new Model(_c.a, _c.b, _c.nx, _c.ny);
        assertThat(model).hasSameHashCodeAs(model);
        assertThat(model).hasSameHashCodeAs(new Model(Arrays.asList(_c.a, _c.b, _c.nx, _c.ny)));
        assertThat(model).hasSameHashCodeAs(new Model(_c.a, _c.b, _c.nx, _c.ny));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquals(final FormulaContext _c) {
        final Model model = new Model(_c.a, _c.b, _c.nx, _c.ny);
        assertThat(model).isNotEqualTo(null);
        assertThat(model.equals(null)).isFalse();
        assertThat(new Model(Arrays.asList(_c.a, _c.b, _c.nx, _c.ny))).isEqualTo(model);
        assertThat(new Model(_c.a, _c.b, _c.nx, _c.ny)).isEqualTo(model);
        assertThat(model).isEqualTo(model);
        assertThat(model.equals(model)).isTrue();
        assertThat(new Model(Arrays.asList(_c.a, _c.b, _c.nx))).isNotEqualTo(model);
        assertThat(new Model(Arrays.asList(_c.a, _c.b, _c.nx, _c.ny, _c.c))).isNotEqualTo(model);
        assertThat(new Model(Arrays.asList(_c.b, _c.a, _c.nx, _c.ny))).isNotEqualTo(model);
        assertThat(_c.verum).isNotEqualTo(model);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testToString(final FormulaContext _c) {
        assertThat(new Model().toString()).isEqualTo("Model{literals=[]}");
        assertThat(new Model(Collections.singletonList(_c.a)).toString()).isEqualTo("Model{literals=[a]}");
        assertThat(new Model(Collections.singletonList(_c.na)).toString()).isEqualTo("Model{literals=[~a]}");
        assertThat(new Model(Arrays.asList(_c.a, _c.b, _c.nx, _c.ny, _c.c)).toString())
                .isEqualTo("Model{literals=[a, b, ~x, ~y, c]}");
    }
}
