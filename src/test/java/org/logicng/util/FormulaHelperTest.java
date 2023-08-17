// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

public class FormulaHelperTest extends TestWithExampleFormulas {

    @Test
    public void testVariables() {
        assertThat(FormulaHelper.variables(TRUE)).isEqualTo(new TreeSet<>());
        assertThat(FormulaHelper.variables(FALSE)).isEqualTo(new TreeSet<>());
        assertThat(FormulaHelper.variables(A)).isEqualTo(new TreeSet<>(Collections.singletonList(A)));
        assertThat(FormulaHelper.variables(NA)).isEqualTo(new TreeSet<>(Collections.singletonList(A)));
        assertThat(FormulaHelper.variables(IMP1, IMP2, IMP3)).isEqualTo(new TreeSet<>(Arrays.asList(A, B, X, Y)));
        assertThat(FormulaHelper.variables(IMP1, Y)).isEqualTo(new TreeSet<>(Arrays.asList(A, B, Y)));

        assertThat(FormulaHelper.variables(Arrays.asList(TRUE, FALSE))).isEqualTo(new TreeSet<>());
        assertThat(FormulaHelper.variables(Arrays.asList(IMP1, IMP2, IMP3))).isEqualTo(new TreeSet<>(Arrays.asList(A, B, X, Y)));
        assertThat(FormulaHelper.variables(Arrays.asList(IMP1, Y))).isEqualTo(new TreeSet<>(Arrays.asList(A, B, Y)));
    }

    @Test
    public void testLiterals() {
        assertThat(FormulaHelper.literals(TRUE)).isEqualTo(new TreeSet<>());
        assertThat(FormulaHelper.literals(FALSE)).isEqualTo(new TreeSet<>());
        assertThat(FormulaHelper.literals(A)).isEqualTo(new TreeSet<>(Collections.singletonList(A)));
        assertThat(FormulaHelper.literals(NA)).isEqualTo(new TreeSet<>(Collections.singletonList(NA)));
        assertThat(FormulaHelper.literals(IMP1, IMP2, IMP3)).isEqualTo(new TreeSet<>(Arrays.asList(A, B, X, Y, NA, NB)));
        assertThat(FormulaHelper.literals(IMP1, NY)).isEqualTo(new TreeSet<>(Arrays.asList(A, B, NY)));

        assertThat(FormulaHelper.literals(Arrays.asList(TRUE, FALSE))).isEqualTo(new TreeSet<>());
        assertThat(FormulaHelper.literals(Arrays.asList(IMP1, IMP2, IMP3))).isEqualTo(new TreeSet<>(Arrays.asList(A, B, X, Y, NA,
                NB)));
        assertThat(FormulaHelper.literals(Arrays.asList(IMP1, NY))).isEqualTo(new TreeSet<>(Arrays.asList(A, B, NY)));
    }

    @Test
    public void testNegateLiterals() {
        assertThat((ArrayList<Literal>) FormulaHelper.negateLiterals(f, Collections.emptyList(), ArrayList::new))
                .isEqualTo(new ArrayList<Formula>());
        assertThat((ArrayList<Literal>) FormulaHelper.negateLiterals(f, Arrays.asList(A, NB), ArrayList::new))
                .isEqualTo(Arrays.asList(NA, B));
        assertThat((HashSet<Literal>) FormulaHelper.negateLiterals(f, Arrays.asList(A, NB), HashSet::new))
                .isEqualTo(new HashSet<>(Arrays.asList(NA, B)));
        final List<Variable> variables = Arrays.asList(A, B);
        assertThat((HashSet<Literal>) FormulaHelper.negateLiterals(f, variables, HashSet::new))
                .isEqualTo(new HashSet<>(Arrays.asList(NA, NB)));
    }

    @Test
    public void testNegate() {
        assertThat((ArrayList<Formula>) FormulaHelper.negate(f, Collections.emptyList(), ArrayList::new))
                .isEqualTo(new ArrayList<Formula>());
        assertThat((ArrayList<Formula>) FormulaHelper.negate(f, Arrays.asList(A, TRUE, NB, AND1), ArrayList::new))
                .isEqualTo(Arrays.asList(NA, FALSE, B, f.not(AND1)));
        assertThat((HashSet<Formula>) FormulaHelper.negate(f, Arrays.asList(A, TRUE, NB, AND1), HashSet::new))
                .isEqualTo(new HashSet<>(Arrays.asList(NA, FALSE, B, f.not(AND1))));
        final List<Variable> variables = Arrays.asList(A, B);
        assertThat((HashSet<Formula>) FormulaHelper.negate(f, variables, HashSet::new))
                .isEqualTo(new HashSet<>(Arrays.asList(NA, NB)));
        final List<Literal> literals = Arrays.asList(NA, B);
        assertThat((HashSet<Formula>) FormulaHelper.negate(f, literals, HashSet::new))
                .isEqualTo(new HashSet<>(Arrays.asList(A, NB)));
    }

    @Test
    public void testSplitTopLevelAnd() {
        assertThat(FormulaHelper.splitTopLevelAnd(TRUE)).isEqualTo(Collections.singletonList(TRUE));
        assertThat(FormulaHelper.splitTopLevelAnd(FALSE)).isEqualTo(Collections.singletonList(FALSE));
        assertThat(FormulaHelper.splitTopLevelAnd(OR1)).isEqualTo(Collections.singletonList(OR1));
        assertThat(FormulaHelper.splitTopLevelAnd(IMP1)).isEqualTo(Collections.singletonList(IMP1));

        assertThat(FormulaHelper.splitTopLevelAnd(AND1)).isEqualTo(Arrays.asList(A, B));
        assertThat(FormulaHelper.splitTopLevelAnd(AND3)).isEqualTo(Arrays.asList(OR1, OR2));
    }

    @Test
    public void testStrings2Vars() {
        assertThat(FormulaHelper.strings2vars(null, f)).isEmpty();
        assertThat(FormulaHelper.strings2vars(new TreeSet<>(), f)).isEmpty();
        assertThat(FormulaHelper.strings2vars(Arrays.asList("a", "b", "c"), f))
                .containsExactly(A, B, C);
        assertThat(FormulaHelper.strings2vars(Arrays.asList("a", "b", "c", "a", "a"), f))
                .containsExactly(A, B, C);
    }

    @Test
    public void testStrings2Literals() {
        assertThat(FormulaHelper.strings2literals(null, "~", f)).isEmpty();
        assertThat(FormulaHelper.strings2literals(new TreeSet<>(), "~", f)).isEmpty();
        assertThat(FormulaHelper.strings2literals(Arrays.asList("a", "~b", "c"), "~", f))
                .containsExactly(A, NB, C);
        assertThat(FormulaHelper.strings2literals(Arrays.asList("~a", "b", "c", "a", "a"), "~", f))
                .containsExactly(A, NA, B, C);
        assertThat(FormulaHelper.strings2literals(Arrays.asList("-a", "b", "c", "a", "a"), "-", f))
                .containsExactly(A, NA, B, C);
    }

    @Test
    public void testVars2Strings() {
        assertThat(FormulaHelper.vars2strings(null)).isEmpty();
        assertThat(FormulaHelper.vars2strings(new TreeSet<>())).isEmpty();
        assertThat(FormulaHelper.vars2strings(Arrays.asList(A, B, C)))
                .containsExactly("a", "b", "c");
        assertThat(FormulaHelper.vars2strings(Arrays.asList(A, B, C, A, A)))
                .containsExactly("a", "b", "c");
    }

    @Test
    public void testVars2Literals() {
        assertThat(FormulaHelper.literals2strings(null, "~")).isEmpty();
        assertThat(FormulaHelper.literals2strings(new TreeSet<>(), "~")).isEmpty();
        assertThat(FormulaHelper.literals2strings(Arrays.asList(A, NB, C), "~"))
                .containsExactly("a", "c", "~b");
        assertThat(FormulaHelper.literals2strings(Arrays.asList(NA, B, C, A, A), "~"))
                .containsExactly("a", "b", "c", "~a");
        assertThat(FormulaHelper.literals2strings(Arrays.asList(NA, B, C, A, A), "-"))
                .containsExactly("-a", "a", "b", "c");
    }
}
