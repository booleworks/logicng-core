// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

public class VariableOccurrencesOnSolverFunctionTest {
    final FormulaFactory f = FormulaFactory.caching();

    private final Variable A = f.variable("a");
    private final Variable B = f.variable("b");
    private final Variable C = f.variable("c");
    private final Variable D = f.variable("d");
    private final Variable E = f.variable("e");
    private final Variable G = f.variable("g");
    private final Variable H = f.variable("h");
    private final Variable I = f.variable("i");
    private final Variable J = f.variable("j");
    private final Variable X = f.variable("x");
    private final Variable Y = f.variable("y");

    @Test
    public void testWithEmptySolver() {
        final SATSolver solver = MiniSat.miniSat(f);
        assertThat(solver.execute(new VariableOccurrencesOnSolverFunction())).isEmpty();
        final Map<Variable, Integer> countWithRelevantVariables = solver.execute(
                new VariableOccurrencesOnSolverFunction(new HashSet<>(Arrays.asList(A, B, C))));
        assertThat(countWithRelevantVariables).hasSize(3);
        assertThat(countWithRelevantVariables).containsKeys(A, B, C);
        assertThat(countWithRelevantVariables.values()).containsOnly(0);
    }

    @Test
    public void testWithAllVariables() throws ParserException {
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("(a | b | c) & (~b | c) & (d | ~e) & x & (~a | e) & (a | d | b | g | h) & (~h | i) & y"));
        final Map<Variable, Integer> counts = solver.execute(new VariableOccurrencesOnSolverFunction());
        assertThat(counts).hasSize(10);
        assertThat(counts).containsEntry(A, 3);
        assertThat(counts).containsEntry(B, 3);
        assertThat(counts).containsEntry(C, 2);
        assertThat(counts).containsEntry(D, 2);
        assertThat(counts).containsEntry(E, 2);
        assertThat(counts).containsEntry(G, 1);
        assertThat(counts).containsEntry(H, 2);
        assertThat(counts).containsEntry(I, 1);
        assertThat(counts).containsEntry(X, 1);
        assertThat(counts).containsEntry(Y, 1);
    }

    @Test
    public void testWithRelevantVariables() throws ParserException {
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("(a | b | c) & (~b | c) & (d | ~e) & x & (~a | e) & (a | d | b | g | h) & (~h | i) & y"));
        final Map<Variable, Integer> counts = solver.execute(
                new VariableOccurrencesOnSolverFunction(new HashSet<>(Arrays.asList(A, C, X, J))));
        assertThat(counts).hasSize(4);
        assertThat(counts).containsEntry(A, 3);
        assertThat(counts).containsEntry(C, 2);
        assertThat(counts).containsEntry(X, 1);
        assertThat(counts).containsEntry(J, 0);
    }
}
