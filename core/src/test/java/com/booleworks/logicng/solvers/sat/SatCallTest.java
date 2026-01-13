// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.TestWithExampleFormulas.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.handlers.events.SimpleEvent;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.functions.FormulaOnSolverFunction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class SatCallTest {

    final CachingFormulaFactory f = FormulaFactory.caching();

    @Test
    public void testIllegalOperationsOnOpenSatCall() {
        final SatSolver solver = SatSolver.newSolver(f, SatSolverConfig.builder().proofGeneration(true).build());
        final SatCall openCall = solver.satCall().solve();

        final SatCallBuilder newCallBuilder = solver.satCall().handler(new TimeoutHandler(1000))
                .addFormula(f.variable("a")).selectionOrder(List.of(f.variable("a")));
        assertThat(newCallBuilder).isNotNull();

        final String expectedMessage = "This operation is not allowed because a SAT call is running on this solver!";
        assertThatThrownBy(newCallBuilder::solve).isInstanceOf(IllegalStateException.class).hasMessage(expectedMessage);
        assertThatThrownBy(solver::sat).isInstanceOf(IllegalStateException.class).hasMessage(expectedMessage);
        assertThatThrownBy(() -> solver.satCall().model(List.of(f.variable("a"))))
                .isInstanceOf(IllegalStateException.class).hasMessage(expectedMessage);
        assertThatThrownBy(() -> solver.satCall().unsatCore()).isInstanceOf(IllegalStateException.class)
                .hasMessage(expectedMessage);
        assertThatThrownBy(() -> solver.execute(FormulaOnSolverFunction.get()))
                .isInstanceOf(IllegalStateException.class).hasMessage(expectedMessage);
        assertThatThrownBy(solver::saveState).isInstanceOf(IllegalStateException.class).hasMessage(expectedMessage);
        assertThatThrownBy(() -> solver.loadState(new SolverState(1, new int[0])))
                .isInstanceOf(IllegalStateException.class).hasMessage(expectedMessage);
        assertThatThrownBy(() -> solver.add(f.variable("a"))).isInstanceOf(IllegalStateException.class)
                .hasMessage(expectedMessage);

        openCall.close();

        assertThat(
                solver.satCall().addFormulas(List.of(parse(f, "(~a | ~b)"), parse(f, "a"), parse(f, "b"))).unsatCore())
                .isNotNull();
        assertThat(solver.execute(FormulaOnSolverFunction.get())).isEqualTo(Set.of());
        final SolverState newState = solver.saveState();
        assertThat(newState).isNotNull();
        solver.loadState(newState);
        solver.add(f.variable("a"));
        assertThat(solver.sat()).isTrue();
        assertThat(solver.satCall().model(List.of(f.variable("a")))).isEqualTo(new Model(f.variable("a")));
    }

    @Test
    public void testDirectSatMethod() {
        final SatSolver solver = SatSolver.newSolver(f, SatSolverConfig.builder().build());
        solver.add(parse(f, "a | b"));
        solver.add(parse(f, "c & (~c | ~a)"));
        assertThat(solver.satCall().sat().getResult()).isTrue();
        assertThat(solver.satCall().addFormula(f.literal("b", false)).sat().getResult()).isFalse();
        assertThat(solver.sat()).isTrue();
    }

    @Test
    public void testDirectModelMethod() {
        final SatSolver solver = SatSolver.newSolver(f, SatSolverConfig.builder().build());
        solver.add(parse(f, "a | b"));
        solver.add(parse(f, "c & (~c | ~a)"));
        final Set<Variable> abc = Set.of(f.variable("a"), f.variable("b"), f.variable("c"));
        final Set<Variable> abcd = Set.of(f.variable("a"), f.variable("b"), f.variable("c"), f.variable("d"));
        assertThat(solver.satCall().model(abc).toAssignment())
                .isEqualTo(new Assignment(f.literal("a", false), f.variable("b"), f.variable("c")));
        assertThat(solver.satCall().addFormula(parse(f, "c | d")).model(abcd).toAssignment()).isIn(
                new Assignment(f.literal("a", false), f.variable("b"), f.variable("c"), f.literal("d", false)),
                new Assignment(f.literal("a", false), f.variable("b"), f.variable("c"), f.literal("d", true))
        );
        assertThat(solver.satCall().model(abc).toAssignment())
                .isEqualTo(new Assignment(f.literal("a", false), f.variable("b"), f.variable("c")));
    }

    @Test
    public void testDirectUnsatCoreMethod() {
        final SatSolver solver = SatSolver.newSolver(f, SatSolverConfig.builder().proofGeneration(true).build());
        solver.add(parse(f, "a | b"));
        solver.add(parse(f, "c & (~c | ~a)"));
        assertThat(solver.satCall().addFormula(f.literal("b", false)).unsatCore().getPropositions())
                .containsExactlyInAnyOrder(
                        new StandardProposition(parse(f, "a | b")),
                        new StandardProposition(parse(f, "c")),
                        new StandardProposition(parse(f, "~c | ~a")),
                        new StandardProposition(parse(f, "~b"))
                );
        assertThat(solver.satCall().addFormula(parse(f, "~b | a")).unsatCore().getPropositions())
                .containsExactlyInAnyOrder(
                        new StandardProposition(parse(f, "a | b")),
                        new StandardProposition(parse(f, "c")),
                        new StandardProposition(parse(f, "~c | ~a")),
                        new StandardProposition(parse(f, "~b | a"))
                );
        assertThat(solver.sat()).isTrue();
        solver.add(parse(f, "~b"));
        assertThat(solver.satCall().unsatCore().getPropositions()).containsExactlyInAnyOrder(
                new StandardProposition(parse(f, "a | b")),
                new StandardProposition(parse(f, "c")),
                new StandardProposition(parse(f, "~c | ~a")),
                new StandardProposition(parse(f, "~b"))
        );
    }

    @Test
    public void testDisallowNullVariablesInModel() {
        final SatSolver solver = SatSolver.newSolver(f, SatSolverConfig.builder().build());
        assertThatThrownBy(() -> solver.satCall().model(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The given variables must not be null.");
    }

    @Test
    public void testHandler() throws IOException, ParserException {
        final SatSolver solver = SatSolver.newSolver(f, SatSolverConfig.builder().proofGeneration(true).build());
        solver.add(FormulaReader.readFormula(f, "../test_files/formulas/small_formulas.txt"));

        try (final SatCall satCall = solver.satCall().handler(new MaxConflictsHandler(0)).solve()) {
            assertThat(satCall.getSatResult().isSuccess()).isFalse();
            assertThat(satCall.model(solver.getUnderlyingSolver().knownVariables())).isNull();
            assertThat(satCall.unsatCore()).isNull();
        }

        assertThat(solver.satCall().handler(new MaxConflictsHandler(0)).sat().isSuccess()).isFalse();
        assertThat(
                solver.satCall().handler(new MaxConflictsHandler(0)).model(solver.getUnderlyingSolver().knownVariables()))
                .isNull();
        assertThat(solver.satCall().handler(new MaxConflictsHandler(0)).unsatCore()).isNull();

        try (final SatCall satCall = solver.satCall().handler(new MaxConflictsHandler(100)).solve()) {
            assertThat(satCall.getSatResult().getResult()).isTrue();
            assertThat(satCall.model(solver.getUnderlyingSolver().knownVariables())).isNotNull();
            assertThat(satCall.unsatCore()).isNull();
        }
    }

    @Test
    public void testSelectionOrder() {
        final SatSolver solver = SatSolver.newSolver(f, SatSolverConfig.builder().proofGeneration(true).build());
        solver.add(parse(f, "a | b | c | d"));
        solver.add(parse(f, "~a | ~b"));
        solver.add(parse(f, "~c | ~d"));
        final Variable a = f.variable("a");
        final Variable b = f.variable("b");
        final Variable c = f.variable("c");
        final Variable d = f.variable("d");

        assertThat(solver.satCall().selectionOrder(List.of(a, b, c, d)).model(List.of(a, b, c, d)).toAssignment())
                .isEqualTo(new Assignment(a, b.negate(f), c, d.negate(f)));
        assertThat(solver.satCall().selectionOrder(List.of(b, a, d, c)).model(List.of(a, b, c, d)).toAssignment())
                .isEqualTo(new Assignment(a.negate(f), b, c.negate(f), d));
        assertThat(solver.satCall().selectionOrder(List.of(a.negate(f), b.negate(f), c.negate(f), d.negate(f)))
                .model(List.of(a, b, c, d)).toAssignment())
                .isEqualTo(new Assignment(a.negate(f), b.negate(f), c.negate(f), d));
        assertThat(solver.satCall().selectionOrder(List.of(a.negate(f), b.negate(f), d.negate(f), c.negate(f)))
                .model(List.of(a, b, c, d)).toAssignment())
                .isEqualTo(new Assignment(a.negate(f), b.negate(f), c, d.negate(f)));
    }

    @Test
    public void testAdditionalFormulasAndPropositions() {
        final SatSolver solver = SatSolver.newSolver(f, SatSolverConfig.builder()
                .cnfMethod(SatSolverConfig.CnfMethod.FACTORY_CNF).proofGeneration(true).build());
        solver.add(parse(f, "a | b | c | d"));
        solver.add(new StandardProposition(parse(f, "a => b")));
        solver.add(new StandardProposition(parse(f, "c => d")));
        solver.add(new StandardProposition(parse(f, "e => ~a & ~b")));
        solver.add(new StandardProposition(parse(f, "~f => ~c & ~d")));

        assertThat(
                solver.satCall().addFormula(parse(f, "e <=> ~f")).addFormula(parse(f, "~f")).unsatCore()
                        .getPropositions())
                .containsExactlyInAnyOrder(
                        new StandardProposition(parse(f, "a | b | c | d")),
                        new StandardProposition(parse(f, "e => ~a & ~b")),
                        new StandardProposition(parse(f, "~f => ~c & ~d")),
                        new StandardProposition(parse(f, "e <=> ~f")),
                        new StandardProposition(parse(f, "~f"))
                );

        assertThat(solver.satCall().addFormulas(List.of(parse(f, "e <=> ~f"), parse(f, "e"))).unsatCore()
                .getPropositions())
                .containsExactlyInAnyOrder(
                        new StandardProposition(parse(f, "a | b | c | d")),
                        new StandardProposition(parse(f, "e => ~a & ~b")),
                        new StandardProposition(parse(f, "~f => ~c & ~d")),
                        new StandardProposition(parse(f, "e <=> ~f")),
                        new StandardProposition(parse(f, "e"))
                );

        assertThat(solver.satCall()
                .addPropositions(
                        List.of(new StandardProposition(parse(f, "e <=> ~f")), new StandardProposition(parse(f, "e"))))
                .unsatCore().getPropositions())
                .containsExactlyInAnyOrder(
                        new StandardProposition(parse(f, "a | b | c | d")),
                        new StandardProposition(parse(f, "e => ~a & ~b")),
                        new StandardProposition(parse(f, "~f => ~c & ~d")),
                        new StandardProposition(parse(f, "e <=> ~f")),
                        new StandardProposition(parse(f, "e"))
                );

        assertThat(
                solver.satCall().addFormula(parse(f, "e <=> f"))
                        .addProposition(new StandardProposition(parse(f, "~e")))
                        .model(solver.getUnderlyingSolver().knownVariables()).toAssignment()).isIn(
                new Assignment(f.literal("a", true), f.literal("b", true), f.literal("c", false),
                        f.literal("d", false), f.literal("e", false), f.literal("f", false)),
                new Assignment(f.literal("a", false), f.literal("b", true), f.literal("c", false),
                        f.literal("d", false), f.literal("e", false), f.literal("f", false)),
                new Assignment(f.literal("a", true), f.literal("b", false), f.literal("c", false),
                        f.literal("d", false), f.literal("e", false), f.literal("f", false))
        );

        assertThat(solver.satCall().addProposition(new StandardProposition(parse(f, "e <=> f")))
                .addProposition(new StandardProposition(parse(f, "~e")))
                .model(solver.getUnderlyingSolver().knownVariables()).toAssignment()).isIn(
                new Assignment(f.literal("a", true), f.literal("b", true), f.literal("c", false),
                        f.literal("d", false), f.literal("e", false), f.literal("f", false)),
                new Assignment(f.literal("a", false), f.literal("b", true), f.literal("c", false),
                        f.literal("d", false), f.literal("e", false), f.literal("f", false)),
                new Assignment(f.literal("a", true), f.literal("b", false), f.literal("c", false),
                        f.literal("d", false), f.literal("e", false), f.literal("f", false))
        );
    }

    private static class MaxConflictsHandler implements ComputationHandler {
        private final int maxConflicts;
        private int numConflicts;
        private boolean canceled;

        public MaxConflictsHandler(final int maxConflicts) {
            this.maxConflicts = maxConflicts;
            numConflicts = 0;
        }

        @Override
        public boolean shouldResume(final LngEvent event) {
            if (event == SimpleEvent.SAT_CONFLICT_DETECTED) {
                canceled = numConflicts++ > maxConflicts;
            }
            return !canceled;
        }
    }
}
