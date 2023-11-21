// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx _c.booleWorks GmbH

package com.booleworks.logicng.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

public class FormulaHelperTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testVariables(final FormulaContext _c) {
        Assertions.assertThat(FormulaHelper.variables(_c.f, _c.verum)).isEqualTo(new TreeSet<>());
        Assertions.assertThat(FormulaHelper.variables(_c.f, _c.falsum)).isEqualTo(new TreeSet<>());
        Assertions.assertThat(FormulaHelper.variables(_c.f, _c.a)).isEqualTo(new TreeSet<>(Collections.singletonList(_c.a)));
        Assertions.assertThat(FormulaHelper.variables(_c.f, _c.na)).isEqualTo(new TreeSet<>(Collections.singletonList(_c.a)));
        Assertions.assertThat(FormulaHelper.variables(_c.f, _c.imp1, _c.imp2, _c.imp3)).isEqualTo(new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.x, _c.y)));
        Assertions.assertThat(FormulaHelper.variables(_c.f, _c.imp1, _c.y)).isEqualTo(new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.y)));

        Assertions.assertThat(FormulaHelper.variables(_c.f, Arrays.asList(_c.verum, _c.falsum))).isEqualTo(new TreeSet<>());
        Assertions.assertThat(FormulaHelper.variables(_c.f, Arrays.asList(_c.imp1, _c.imp2, _c.imp3))).isEqualTo(new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.x, _c.y)));
        Assertions.assertThat(FormulaHelper.variables(_c.f, Arrays.asList(_c.imp1, _c.y))).isEqualTo(new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.y)));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLiterals(final FormulaContext _c) {
        Assertions.assertThat(FormulaHelper.literals(_c.f, _c.verum)).isEqualTo(new TreeSet<>());
        Assertions.assertThat(FormulaHelper.literals(_c.f, _c.falsum)).isEqualTo(new TreeSet<>());
        Assertions.assertThat(FormulaHelper.literals(_c.f, _c.a)).isEqualTo(new TreeSet<>(Collections.singletonList(_c.a)));
        Assertions.assertThat(FormulaHelper.literals(_c.f, _c.na)).isEqualTo(new TreeSet<>(Collections.singletonList(_c.na)));
        Assertions.assertThat(FormulaHelper.literals(_c.f, _c.imp1, _c.imp2, _c.imp3)).isEqualTo(new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.x, _c.y, _c.na, _c.nb)));
        Assertions.assertThat(FormulaHelper.literals(_c.f, _c.imp1, _c.ny)).isEqualTo(new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.ny)));

        Assertions.assertThat(FormulaHelper.literals(_c.f, Arrays.asList(_c.verum, _c.falsum))).isEqualTo(new TreeSet<>());
        Assertions.assertThat(FormulaHelper.literals(_c.f, Arrays.asList(_c.imp1, _c.imp2, _c.imp3))).isEqualTo(new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.x, _c.y, _c.na, _c.nb)));
        Assertions.assertThat(FormulaHelper.literals(_c.f, Arrays.asList(_c.imp1, _c.ny))).isEqualTo(new TreeSet<>(Arrays.asList(_c.a, _c.b, _c.ny)));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNegateLiterals(final FormulaContext _c) {
        Assertions.assertThat((ArrayList<Literal>) FormulaHelper.negateLiterals(_c.f, Collections.emptyList(), ArrayList::new))
                .isEqualTo(new ArrayList<Formula>());
        assertThat((ArrayList<Literal>) FormulaHelper.negateLiterals(_c.f, Arrays.asList(_c.a, _c.nb), ArrayList::new))
                .isEqualTo(Arrays.asList(_c.na, _c.b));
        assertThat((HashSet<Literal>) FormulaHelper.negateLiterals(_c.f, Arrays.asList(_c.a, _c.nb), HashSet::new))
                .isEqualTo(new HashSet<>(Arrays.asList(_c.na, _c.b)));
        final List<Variable> variables = Arrays.asList(_c.a, _c.b);
        assertThat((HashSet<Literal>) FormulaHelper.negateLiterals(_c.f, variables, HashSet::new))
                .isEqualTo(new HashSet<>(Arrays.asList(_c.na, _c.nb)));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNegate(final FormulaContext _c) {
        assertThat((ArrayList<Formula>) FormulaHelper.negate(_c.f, Collections.emptyList(), ArrayList::new))
                .isEqualTo(new ArrayList<Formula>());
        assertThat((ArrayList<Formula>) FormulaHelper.negate(_c.f, Arrays.asList(_c.a, _c.verum, _c.nb, _c.and1), ArrayList::new))
                .isEqualTo(Arrays.asList(_c.na, _c.falsum, _c.b, _c.f.not(_c.and1)));
        assertThat((HashSet<Formula>) FormulaHelper.negate(_c.f, Arrays.asList(_c.a, _c.verum, _c.nb, _c.and1), HashSet::new))
                .isEqualTo(new HashSet<>(Arrays.asList(_c.na, _c.falsum, _c.b, _c.f.not(_c.and1))));
        final List<Variable> variables = Arrays.asList(_c.a, _c.b);
        assertThat((HashSet<Formula>) FormulaHelper.negate(_c.f, variables, HashSet::new))
                .isEqualTo(new HashSet<>(Arrays.asList(_c.na, _c.nb)));
        final List<Literal> literals = Arrays.asList(_c.na, _c.b);
        assertThat((HashSet<Formula>) FormulaHelper.negate(_c.f, literals, HashSet::new))
                .isEqualTo(new HashSet<>(Arrays.asList(_c.a, _c.nb)));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSplitTopLevelAnd(final FormulaContext _c) {
        Assertions.assertThat(FormulaHelper.splitTopLevelAnd(_c.verum)).isEqualTo(Collections.singletonList(_c.verum));
        Assertions.assertThat(FormulaHelper.splitTopLevelAnd(_c.falsum)).isEqualTo(Collections.singletonList(_c.falsum));
        Assertions.assertThat(FormulaHelper.splitTopLevelAnd(_c.or1)).isEqualTo(Collections.singletonList(_c.or1));
        Assertions.assertThat(FormulaHelper.splitTopLevelAnd(_c.imp1)).isEqualTo(Collections.singletonList(_c.imp1));

        Assertions.assertThat(FormulaHelper.splitTopLevelAnd(_c.and1)).isEqualTo(Arrays.asList(_c.a, _c.b));
        Assertions.assertThat(FormulaHelper.splitTopLevelAnd(_c.and3)).isEqualTo(Arrays.asList(_c.or1, _c.or2));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testStrings2Vars(final FormulaContext _c) {
        Assertions.assertThat(FormulaHelper.strings2vars(_c.f, null)).isEmpty();
        Assertions.assertThat(FormulaHelper.strings2vars(_c.f, new TreeSet<>())).isEmpty();
        Assertions.assertThat(FormulaHelper.strings2vars(_c.f, Arrays.asList("a", "b", "c")))
                .containsExactly(_c.a, _c.b, _c.c);
        Assertions.assertThat(FormulaHelper.strings2vars(_c.f, Arrays.asList("a", "b", "c", "a", "a")))
                .containsExactly(_c.a, _c.b, _c.c);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testStrings2Literals(final FormulaContext _c) {
        Assertions.assertThat(FormulaHelper.strings2literals(_c.f, null, "~")).isEmpty();
        Assertions.assertThat(FormulaHelper.strings2literals(_c.f, new TreeSet<>(), "~")).isEmpty();
        Assertions.assertThat(FormulaHelper.strings2literals(_c.f, Arrays.asList("a", "~b", "c"), "~"))
                .containsExactly(_c.a, _c.nb, _c.c);
        Assertions.assertThat(FormulaHelper.strings2literals(_c.f, Arrays.asList("~a", "b", "c", "a", "a"), "~"))
                .containsExactly(_c.a, _c.na, _c.b, _c.c);
        Assertions.assertThat(FormulaHelper.strings2literals(_c.f, Arrays.asList("-a", "b", "c", "a", "a"), "-"))
                .containsExactly(_c.a, _c.na, _c.b, _c.c);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testVars2Strings(final FormulaContext _c) {
        assertThat(FormulaHelper.vars2strings(null)).isEmpty();
        assertThat(FormulaHelper.vars2strings(new TreeSet<>())).isEmpty();
        assertThat(FormulaHelper.vars2strings(Arrays.asList(_c.a, _c.b, _c.c)))
                .containsExactly("a", "b", "c");
        assertThat(FormulaHelper.vars2strings(Arrays.asList(_c.a, _c.b, _c.c, _c.a, _c.a)))
                .containsExactly("a", "b", "c");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testVars2Literals(final FormulaContext _c) {
        assertThat(FormulaHelper.literals2strings(null, "~")).isEmpty();
        assertThat(FormulaHelper.literals2strings(new TreeSet<>(), "~")).isEmpty();
        assertThat(FormulaHelper.literals2strings(Arrays.asList(_c.a, _c.nb, _c.c), "~"))
                .containsExactly("a", "c", "~b");
        assertThat(FormulaHelper.literals2strings(Arrays.asList(_c.na, _c.b, _c.c, _c.a, _c.a), "~"))
                .containsExactly("a", "b", "c", "~a");
        assertThat(FormulaHelper.literals2strings(Arrays.asList(_c.na, _c.b, _c.c, _c.a, _c.a), "-"))
                .containsExactly("-a", "a", "b", "c");
    }
}
