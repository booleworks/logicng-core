// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.logicng.formulas.FormulaFactoryConfig.FormulaMergeStrategy.IMPORT;
import static org.logicng.formulas.FormulaFactoryConfig.FormulaMergeStrategy.PANIC;
import static org.logicng.formulas.FormulaFactoryConfig.FormulaMergeStrategy.USE_BUT_NO_IMPORT;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FormulaMergeTest {

    public static Collection<Object[]> panics() {
        final FormulaFactoryConfig config = FormulaFactoryConfig.builder().formulaMergeStrategy(PANIC).build();
        final List<Object[]> contexts = new ArrayList<>();
        contexts.add(new Object[]{new FormulaContext(FormulaFactory.caching(config))});
        contexts.add(new Object[]{new FormulaContext(FormulaFactory.nonCaching(config))});
        return contexts;
    }

    public static Collection<Object[]> imports() {
        final FormulaFactoryConfig config = FormulaFactoryConfig.builder().formulaMergeStrategy(IMPORT).build();
        final List<Object[]> contexts = new ArrayList<>();
        contexts.add(new Object[]{new FormulaContext(FormulaFactory.caching(config))});
        contexts.add(new Object[]{new FormulaContext(FormulaFactory.nonCaching(config))});
        return contexts;
    }

    @ParameterizedTest
    @MethodSource("panics")
    public void testPanic(final FormulaContext _c) {
        final FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(IMPORT).build());

        final Variable a1 = _c.f.variable("A");
        final Variable b1 = _c.f.variable("B");
        final Variable c1 = _c.f.variable("C");
        final Variable a2 = g.variable("A");
        final Variable b2 = g.variable("B");
        final Variable c2 = g.variable("C");
        assertThatThrownBy(() -> _c.f.not(a2)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.and(a2, b1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.and(a2, b2)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.and(a1, b1, c2)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.or(a2, b1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.or(a2, b2)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.or(a1, b1, c2)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.clause(a2, b1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.clause(a2, b2)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.clause(a1, b1, c2)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.cnf(_c.f.clause(a1, b1), _c.f.clause(a1, c1), c2)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.cnf(_c.f.clause(a1, b1), g.clause(a2, c2), c1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.pbc(CType.GE, 1, new Literal[]{a1, b2.negate(), c1}, new int[]{1, 2, 3})).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.pbc(CType.GE, 1, new Literal[]{a2, b2, c2.negate()}, new int[]{1, 2, 3})).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.cc(CType.GE, 1, a1, b2, c1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.cc(CType.GE, 1, a2, b2, c2)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.amo(a1, b2, c1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.amo(a2, b2, c2)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.exo(a1, b2, c1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> _c.f.exo(a2, b2, c2)).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("imports")
    public void testImport(final FormulaContext _c) {
        final FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(PANIC).build());

        final Variable a1 = _c.f.variable("A");
        final Variable b1 = _c.f.variable("B");
        final Variable c1 = _c.f.variable("C");
        final Variable a2 = g.variable("A");
        final Variable b2 = g.variable("B");
        final Variable c2 = g.variable("C");
        assertThat(_c.f.not(a2)).isEqualTo(g.not(a2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.and(a2, b1)).isEqualTo(g.and(a2, b2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.and(a2, b2)).isEqualTo(g.and(a2, b2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.and(a1, b1, c2)).isEqualTo(g.and(a2, b2, c2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.or(a2, b1)).isEqualTo(g.or(a2, b2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.or(a2, b2)).isEqualTo(g.or(a2, b2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.or(a1, b1, c2)).isEqualTo(g.or(a2, b2, c2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.clause(a2, b1)).isEqualTo(g.clause(a2, b2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.clause(a2, b2)).isEqualTo(g.clause(a2, b2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.clause(a1, b1, c2)).isEqualTo(g.clause(a2, b2, c2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.cnf(_c.f.clause(a1, b1), _c.f.clause(a1, c1), c2)).isNotNull().allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.cnf(_c.f.clause(a1, b1), g.clause(a2, c2), c1)).isNotNull().allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.pbc(CType.GE, 1, new Literal[]{a1, b2.negate(), c1}, new int[]{1, 2, 3})).isNotNull().allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.pbc(CType.GE, 1, new Literal[]{a2, b2, c2.negate()}, new int[]{1, 2, 3})).isNotNull().allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.cc(CType.GE, 1, a1, b2, c1)).isEqualTo(g.cc(CType.GE, 1, a2, b2, c2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.cc(CType.GE, 1, a2, b2, c2)).isEqualTo(g.cc(CType.GE, 1, a2, b2, c2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.amo(a1, b2, c1)).isEqualTo(g.amo(a2, b2, c2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.amo(a2, b2, c2)).isEqualTo(g.amo(a2, b2, c2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.exo(a1, b2, c1)).isEqualTo(g.exo(a2, b2, c2)).allMatch(it -> it.factory() == _c.f);
        assertThat(_c.f.exo(a2, b2, c2)).isEqualTo(g.exo(a2, b2, c2)).allMatch(it -> it.factory() == _c.f);
    }

    @Test
    public void testUsage() {
        final FormulaFactory f = FormulaFactory.nonCaching(FormulaFactoryConfig.builder().formulaMergeStrategy(USE_BUT_NO_IMPORT).build());
        final FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(PANIC).build());

        final Variable a1 = f.variable("A");
        final Variable b1 = f.variable("B");
        final Variable c1 = f.variable("C");
        final Variable a2 = g.variable("A");
        final Variable b2 = g.variable("B");
        final Variable c2 = g.variable("C");
        assertThat(f.not(a2).factory()).isEqualTo(f);
        assertThat(f.not(a2)).isEqualTo(g.not(a2)).allMatch(it -> it.factory() == g);
        assertThat(f.and(a2, b1)).isEqualTo(g.and(a2, b2));
        assertThat(f.and(a2, b1).factory()).isEqualTo(f);
        assertThat(f.and(a2, b2)).isEqualTo(g.and(a2, b2)).allMatch(it -> it.factory() == g);
        assertThat(f.and(a2, b2).factory()).isEqualTo(f);
        assertThat(f.and(a1, b1, c2)).isEqualTo(g.and(a2, b2, c2));
        assertThat(f.and(a1, b1, c2).factory()).isEqualTo(f);
        assertThat(((And) f.and(a1, b1, c2)).operands().get(0).factory()).isEqualTo(f);
        assertThat(((And) f.and(a1, b1, c2)).operands().get(1).factory()).isEqualTo(f);
        assertThat(((And) f.and(a1, b1, c2)).operands().get(2).factory()).isEqualTo(g);
        assertThat(f.and(a2, b2, c2)).isEqualTo(g.and(a2, b2, c2)).allMatch(it -> it.factory() == g);
        assertThat(f.or(a2, b1)).isEqualTo(g.or(a2, b2));
        assertThat(f.or(a2, b1).factory()).isEqualTo(f);
        assertThat(f.or(a2, b2)).isEqualTo(g.or(a2, b2)).allMatch(it -> it.factory() == g);
        assertThat(f.or(a1, b1, c2)).isEqualTo(g.or(a2, b2, c2));
        assertThat(f.or(a1, b1, c2).factory()).isEqualTo(f);
        assertThat(f.or(a2, b2, c2)).isEqualTo(g.or(a2, b2, c2)).allMatch(it -> it.factory() == g);
        assertThat(f.clause(a2, b1)).isEqualTo(g.clause(a2, b2));
        assertThat(f.clause(a2, b1).factory()).isEqualTo(f);
        assertThat(f.clause(a2, b2)).isEqualTo(g.clause(a2, b2)).allMatch(it -> it.factory() == g);
        assertThat(f.clause(a2, b2).factory()).isEqualTo(f);
        assertThat(f.clause(a1, b1, c2)).isEqualTo(g.clause(a2, b2, c2));
        assertThat(f.cnf(f.clause(a1, b1), f.clause(a1, c1), c2)).isNotNull();
        assertThat(f.cnf(g.clause(a2, b2), g.clause(a2, c2), c2)).isNotNull().allMatch(it -> it.factory() == g);
        assertThat(f.cnf(g.clause(a2, b2), g.clause(a2, c2), c2).factory()).isEqualTo(f);
        assertThat(f.cnf(f.clause(a1, b1), g.clause(a2, c2), c1)).isNotNull();
        assertThat(f.pbc(CType.GE, 1, new Literal[]{a1, b2.negate(), c1}, new int[]{1, 2, 3})).isNotNull().allMatch(it -> it.factory() == g);
        assertThat(f.pbc(CType.GE, 1, new Literal[]{a2, b2, c2.negate()}, new int[]{1, 2, 3})).isNotNull().allMatch(it -> it.factory() == g);
        assertThat(f.cc(CType.GE, 1, a1, b2, c1)).isEqualTo(g.cc(CType.GE, 1, a2, b2, c2)).allMatch(it -> it.factory() == g);
        assertThat(f.cc(CType.GE, 1, a2, b2, c2)).isEqualTo(g.cc(CType.GE, 1, a2, b2, c2)).allMatch(it -> it.factory() == g);
        assertThat(f.amo(a1, b2, c1)).isEqualTo(g.amo(a2, b2, c2)).allMatch(it -> it.factory() == g);
        assertThat(f.amo(a2, b2, c2)).isEqualTo(g.amo(a2, b2, c2)).allMatch(it -> it.factory() == g);
        assertThat(f.exo(a1, b2, c1)).isEqualTo(g.exo(a2, b2, c2)).allMatch(it -> it.factory() == g);
        assertThat(f.exo(a2, b2, c2)).isEqualTo(g.exo(a2, b2, c2)).allMatch(it -> it.factory() == g);
    }
}
