// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration.splitvariablesprovider;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SplitVariableProviderTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFixedVariableProvider(final FormulaContext _c) {
        assertThat(new FixedVariableProvider(varSet(_c.f, "A B C D E F")).getSplitVars(null, null)).containsExactlyElementsOf(varSet(_c.f, "A B C D E F"));
        assertThat(new FixedVariableProvider(varSet(_c.f, "A B C D E F")).getSplitVars(null, new TreeSet<>())).containsExactlyElementsOf(varSet(_c.f, "A B C D E F"));
        assertThat(new FixedVariableProvider(varSet(_c.f, "A B C D E F")).getSplitVars(null, varSet(_c.f, "A B X U"))).containsExactlyElementsOf(varSet(_c.f, "A B C D E F"));
        assertThat(new FixedVariableProvider(varSet(_c.f, "A B C D E F")).getSplitVars(MiniSat.miniSat(_c.f), varSet(_c.f, "A B X U"))).containsExactlyElementsOf(varSet(_c.f, "A B C D E F"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLeastCommonVariablesProvider(final FormulaContext _c) throws ParserException {
        final SortedSet<Variable> varSet = varSet(_c.f, "a b c d e f g h i j");
        final SATSolver solver = MiniSat.miniSat(_c.f);
        solver.add(_c.f.parse("(a | b | c) & (~b | c) & (d | ~e) & (~a | e) & (a | d | b | g | h) & (~h | i) & (f | g | j) & (f | b | j | ~g) & (g | c)"));
        assertThat(new LeastCommonVariablesProvider(.1, 100).getSplitVars(solver, null)).containsExactly(_c.f.variable("i"));
        assertThat(new LeastCommonVariablesProvider(.1, 100).getSplitVars(solver, varSet)).containsExactly(_c.f.variable("i"));
        assertThat(new LeastCommonVariablesProvider(.0001, 100).getSplitVars(solver, null)).containsExactly(_c.f.variable("i"));
        assertThat(new LeastCommonVariablesProvider(.2, 100).getSplitVars(solver, null))
                .hasSize(2).contains(_c.f.variable("i")).containsAnyElementsOf(varSet(_c.f, "e d f h j"));
        assertThat(new LeastCommonVariablesProvider(.6, 100).getSplitVars(solver, null)).containsExactlyElementsOf(varSet(_c.f, "e d f i h j"));
        assertThat(new LeastCommonVariablesProvider(.6, 1).getSplitVars(solver, null)).containsExactlyElementsOf(varSet(_c.f, "i"));
        assertThat(new LeastCommonVariablesProvider(.6, 2).getSplitVars(solver, null))
                .hasSize(2).contains(_c.f.variable("i")).containsAnyElementsOf(varSet(_c.f, "e d f h j"));
        assertThat(new LeastCommonVariablesProvider(.25, 100).getSplitVars(solver, varSet(_c.f, "a b g"))).containsExactly(_c.a);
        assertThat(new LeastCommonVariablesProvider(.5, 100).getSplitVars(solver, varSet(_c.f, "a c b g"))).containsExactlyElementsOf(varSet(_c.f, "a c"));
        assertThat(new LeastCommonVariablesProvider().getSplitVars(solver, varSet(_c.f, "a c b g"))).containsExactlyElementsOf(varSet(_c.f, "a c"));
        assertThat(new LeastCommonVariablesProvider(1, 100).getSplitVars(solver, varSet(_c.f, "a c b g"))).containsExactlyElementsOf(varSet(_c.f, "a c b g"));
        assertThat(new LeastCommonVariablesProvider(1, 100).getSplitVars(solver, null)).containsExactlyElementsOf(varSet);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testMostCommonVariablesProvider(final FormulaContext _c) throws ParserException {
        final SortedSet<Variable> varSet = varSet(_c.f, "a b c d e f g h i j");
        final SATSolver solver = MiniSat.miniSat(_c.f);
        solver.add(_c.f.parse("(a | b | c) & (~b | c) & (d | ~e) & (~a | e) & (a | d | b | g | h) & (~h | i) & (f | g | j) & (f | b | j | ~g) & (g | c)"));
        assertThat(new MostCommonVariablesProvider(.2, 100).getSplitVars(solver, null)).containsExactlyElementsOf(varSet(_c.f, "b g"));
        assertThat(new MostCommonVariablesProvider(.2, 100).getSplitVars(solver, varSet)).containsExactlyElementsOf(varSet(_c.f, "b g"));
        assertThat(new MostCommonVariablesProvider(.0001, 100).getSplitVars(solver, null)).hasSize(1).containsAnyElementsOf(varSet(_c.f, "b g"));
        assertThat(new MostCommonVariablesProvider(.4, 100).getSplitVars(solver, null)).containsExactlyElementsOf(varSet(_c.f, "b g a c"));
        assertThat(new MostCommonVariablesProvider(.9, 100).getSplitVars(solver, null)).containsAll(varSet(_c.f, "a b c d e f g h j"));
        assertThat(new MostCommonVariablesProvider(.9, 2).getSplitVars(solver, null)).containsExactlyElementsOf(varSet(_c.f, "b g"));
        assertThat(new MostCommonVariablesProvider(.25, 100).getSplitVars(solver, varSet(_c.f, "f i c"))).containsExactly(_c.c);
        assertThat(new MostCommonVariablesProvider(.5, 100).getSplitVars(solver, varSet(_c.f, "c b f h"))).containsExactlyElementsOf(varSet(_c.f, "b c"));
        assertThat(new MostCommonVariablesProvider().getSplitVars(solver, varSet(_c.f, "c b f h"))).containsExactlyElementsOf(varSet(_c.f, "b c"));
        assertThat(new MostCommonVariablesProvider(1, 100).getSplitVars(solver, varSet(_c.f, "a c b g"))).containsExactlyElementsOf(varSet(_c.f, "a c b g"));
        assertThat(new MostCommonVariablesProvider(1, 100).getSplitVars(solver, null)).containsExactlyElementsOf(varSet);
    }

    private SortedSet<Variable> varSet(final FormulaFactory f, final String varString) {
        return Arrays.stream(varString.split(" ")).map(f::variable).collect(Collectors.toCollection(TreeSet::new));
    }
}
