// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.CNF_METHOD;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.PROOF_GENERATION;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.solvers.SATSolver;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class AssumeTest implements LogicNGTest {

    private final FormulaFactory f;
    private final List<SATSolver> solvers;
    private final PropositionalParser parser;

    public AssumeTest() {
        f = FormulaFactory.caching();
        parser = new PropositionalParser(f);
        solvers = SolverTestSet.solverTestSet(Set.of(SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES, PROOF_GENERATION, CNF_METHOD), f);
    }

    @Test
    public void testAssume() throws ParserException {
        final List<Literal> assumptions1 = Arrays.asList(f.literal("c", true), f.literal("d", true));
        final List<Literal> assumptions2 = Arrays.asList(f.literal("x", false), f.literal("y", true), f.literal("d", true));
        final List<Literal> assumptions3 = Arrays.asList(f.literal("a", false), f.literal("c", true), f.literal("a", false));
        final List<Literal> assumptions4 = Arrays.asList(f.literal("c", false), f.literal("d", true));
        final List<Literal> assumptions5 = Arrays.asList(f.literal("x", true), f.literal("x", false));
        final List<Literal> assumptions6 = Arrays.asList(f.literal("a", true), f.literal("a", false));
        for (final SATSolver s : solvers) {
            s.add(parser.parse("~a"));
            s.add(parser.parse("b"));
            s.add(parser.parse("b => c"));
            s.add(parser.parse("c => d"));
            s.add(parser.parse("d => e"));
            s.add(parser.parse("e => f"));
            assertThat(s.satCall().assumptions(f.literal("a", false)).sat()).isEqualTo(Tristate.TRUE);
            assertThat(s.satCall().assumptions(f.variable("b")).sat()).isEqualTo(Tristate.TRUE);
            assertThat(s.satCall().assumptions(f.variable("c")).sat()).isEqualTo(Tristate.TRUE);
            assertThat(s.satCall().assumptions(f.variable("d")).sat()).isEqualTo(Tristate.TRUE);
            assertThat(s.satCall().assumptions(f.variable("e")).sat()).isEqualTo(Tristate.TRUE);
            assertThat(s.satCall().assumptions(f.variable("f")).sat()).isEqualTo(Tristate.TRUE);
            assertThat(s.satCall().assumptions(f.variable("g")).sat()).isEqualTo(Tristate.TRUE);
            assertThat(s.satCall().assumptions(f.variable("a")).sat()).isEqualTo(Tristate.FALSE);
            assertThat(s.satCall().assumptions(f.literal("b", false)).sat()).isEqualTo(Tristate.FALSE);
            assertThat(s.satCall().assumptions(f.literal("c", false)).sat()).isEqualTo(Tristate.FALSE);
            assertThat(s.satCall().assumptions(f.literal("d", false)).sat()).isEqualTo(Tristate.FALSE);
            assertThat(s.satCall().assumptions(f.literal("e", false)).sat()).isEqualTo(Tristate.FALSE);
            assertThat(s.satCall().assumptions(f.literal("f", false)).sat()).isEqualTo(Tristate.FALSE);
            assertThat(s.satCall().assumptions(f.literal("g", false)).sat()).isEqualTo(Tristate.TRUE);
            assertThat(s.satCall().assumptions(assumptions1).sat()).isEqualTo(Tristate.TRUE);
            assertThat(s.satCall().assumptions(assumptions2).sat()).isEqualTo(Tristate.TRUE);
            assertThat(s.satCall().assumptions(assumptions3).sat()).isEqualTo(Tristate.TRUE);
            assertThat(s.satCall().assumptions(assumptions4).sat()).isEqualTo(Tristate.FALSE);
            assertThat(s.satCall().assumptions(assumptions5).sat()).isEqualTo(Tristate.FALSE);
            assertThat(s.satCall().assumptions(assumptions6).sat()).isEqualTo(Tristate.FALSE);
        }
    }
}
