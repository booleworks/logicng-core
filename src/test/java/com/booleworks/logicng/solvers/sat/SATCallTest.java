package com.booleworks.logicng.solvers.sat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.handlers.TimeoutSATHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.functions.FormulaOnSolverFunction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class SATCallTest {

    final CachingFormulaFactory f = FormulaFactory.caching();

    @Test
    public void testIllegalOperationsOnOpenSatCall() throws ParserException {
        final SATSolver solver = SATSolver.newSolver(f, SATSolverConfig.builder().proofGeneration(true).build());
        final SATCall openCall = solver.satCall().solve();

        final SATCallBuilder newCallBuilder = solver.satCall().handler(new TimeoutSATHandler(1000))
                .addFormulas(f.variable("a")).selectionOrder(List.of(f.variable("a")));
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

        assertThat(solver.satCall().addFormulas(List.of(f.parse("(~a | ~b)"), f.parse("a"), f.parse("b"))).unsatCore())
                .isNotNull();
        assertThat(solver.execute(FormulaOnSolverFunction.get())).isEqualTo(Set.of());
        final SolverState newState = solver.saveState();
        assertThat(newState).isNotNull();
        solver.loadState(newState);
        solver.add(f.variable("a"));
        assertThat(solver.sat()).isTrue();
        assertThat(solver.satCall().model(List.of(f.variable("a")))).isEqualTo(new Assignment(f.variable("a")));
    }

    @Test
    public void testDirectSatMethod() throws ParserException {
        final SATSolver solver = SATSolver.newSolver(f, SATSolverConfig.builder().build());
        solver.add(f.parse("a | b"));
        solver.add(f.parse("c & (~c | ~a)"));
        assertThat(solver.satCall().sat()).isEqualTo(Tristate.TRUE);
        assertThat(solver.satCall().addFormulas(f.literal("b", false)).sat()).isEqualTo(Tristate.FALSE);
        assertThat(solver.sat()).isTrue();
    }

    @Test
    public void testDirectModelMethod() throws ParserException {
        final SATSolver solver = SATSolver.newSolver(f, SATSolverConfig.builder().build());
        solver.add(f.parse("a | b"));
        solver.add(f.parse("c & (~c | ~a)"));
        final Set<Variable> abc = Set.of(f.variable("a"), f.variable("b"), f.variable("c"));
        final Set<Variable> abcd = Set.of(f.variable("a"), f.variable("b"), f.variable("c"), f.variable("d"));
        assertThat(solver.satCall().model(abc))
                .isEqualTo(new Assignment(f.literal("a", false), f.variable("b"), f.variable("c")));
        assertThat(solver.satCall().addFormulas(f.parse("c | d")).model(abcd)).isIn(
                new Assignment(f.literal("a", false), f.variable("b"), f.variable("c"), f.literal("d", false)),
                new Assignment(f.literal("a", false), f.variable("b"), f.variable("c"), f.literal("d", true))
        );
        assertThat(solver.satCall().model(abc))
                .isEqualTo(new Assignment(f.literal("a", false), f.variable("b"), f.variable("c")));
    }

    @Test
    public void testDirectUnsatCoreMethod() throws ParserException {
        final SATSolver solver = SATSolver.newSolver(f, SATSolverConfig.builder().proofGeneration(true).build());
        solver.add(f.parse("a | b"));
        solver.add(f.parse("c & (~c | ~a)"));
        assertThat(solver.satCall().addFormulas(f.literal("b", false)).unsatCore().propositions())
                .containsExactlyInAnyOrder(
                        new StandardProposition(f.parse("a | b")),
                        new StandardProposition(f.parse("c")),
                        new StandardProposition(f.parse("~c | ~a")),
                        new StandardProposition(f.parse("~b"))
                );
        assertThat(solver.satCall().addFormulas(f.parse("~b | a")).unsatCore().propositions())
                .containsExactlyInAnyOrder(
                        new StandardProposition(f.parse("a | b")),
                        new StandardProposition(f.parse("c")),
                        new StandardProposition(f.parse("~c | ~a")),
                        new StandardProposition(f.parse("~b | a"))
                );
        assertThat(solver.sat()).isTrue();
        solver.add(f.parse("~b"));
        assertThat(solver.satCall().unsatCore().propositions()).containsExactlyInAnyOrder(
                new StandardProposition(f.parse("a | b")),
                new StandardProposition(f.parse("c")),
                new StandardProposition(f.parse("~c | ~a")),
                new StandardProposition(f.parse("~b"))
        );
    }

    @Test
    public void testDisallowNullVariablesInModel() {
        final SATSolver solver = SATSolver.newSolver(f, SATSolverConfig.builder().build());
        assertThatThrownBy(() -> solver.satCall().model(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The given variables must not be null.");
    }

    @Test
    public void testHandler() throws IOException, ParserException {
        final SATSolver solver = SATSolver.newSolver(f, SATSolverConfig.builder().proofGeneration(true).build());
        solver.add(FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/small_formulas.txt"));

        try (final SATCall satCall = solver.satCall().handler(new MaxConflictsHandler(0)).solve()) {
            assertThat(satCall.getSatResult()).isEqualTo(Tristate.UNDEF);
            assertThat(satCall.model(solver.underlyingSolver().knownVariables())).isNull();
            assertThat(satCall.unsatCore()).isNull();
        }

        assertThat(solver.satCall().handler(new MaxConflictsHandler(0)).sat()).isEqualTo(Tristate.UNDEF);
        assertThat(
                solver.satCall().handler(new MaxConflictsHandler(0)).model(solver.underlyingSolver().knownVariables()))
                        .isNull();
        assertThat(solver.satCall().handler(new MaxConflictsHandler(0)).unsatCore()).isNull();

        try (final SATCall satCall = solver.satCall().handler(new MaxConflictsHandler(100)).solve()) {
            assertThat(satCall.getSatResult()).isEqualTo(Tristate.TRUE);
            assertThat(satCall.model(solver.underlyingSolver().knownVariables())).isNotNull();
            assertThat(satCall.unsatCore()).isNull();
        }
    }

    @Test
    public void testSelectionOrder() throws ParserException {
        final SATSolver solver = SATSolver.newSolver(f, SATSolverConfig.builder().proofGeneration(true).build());
        solver.add(f.parse("a | b | c | d"));
        solver.add(f.parse("~a | ~b"));
        solver.add(f.parse("~c | ~d"));
        final Variable a = f.variable("a");
        final Variable b = f.variable("b");
        final Variable c = f.variable("c");
        final Variable d = f.variable("d");

        assertThat(solver.satCall().selectionOrder(List.of(a, b, c, d)).model(List.of(a, b, c, d)))
                .isEqualTo(new Assignment(a, b.negate(f), c, d.negate(f)));
        assertThat(solver.satCall().selectionOrder(List.of(b, a, d, c)).model(List.of(a, b, c, d)))
                .isEqualTo(new Assignment(a.negate(f), b, c.negate(f), d));
        assertThat(solver.satCall().selectionOrder(List.of(a.negate(f), b.negate(f), c.negate(f), d.negate(f)))
                .model(List.of(a, b, c, d)))
                        .isEqualTo(new Assignment(a.negate(f), b.negate(f), c.negate(f), d));
        assertThat(solver.satCall().selectionOrder(List.of(a.negate(f), b.negate(f), d.negate(f), c.negate(f)))
                .model(List.of(a, b, c, d)))
                        .isEqualTo(new Assignment(a.negate(f), b.negate(f), c, d.negate(f)));
    }

    @Test
    public void testAdditionalFormulasAndPropositions() throws ParserException {
        final SATSolver solver = SATSolver.newSolver(f, SATSolverConfig.builder()
                .cnfMethod(SATSolverConfig.CNFMethod.FACTORY_CNF).proofGeneration(true).build());
        solver.add(f.parse("a | b | c | d"));
        solver.add(new StandardProposition(f.parse("a => b")));
        solver.add(new StandardProposition(f.parse("c => d")));
        solver.add(new StandardProposition(f.parse("e => ~a & ~b")));
        solver.add(new StandardProposition(f.parse("~f => ~c & ~d")));

        assertThat(
                solver.satCall().addFormulas(f.parse("e <=> ~f")).addFormulas(f.parse("~f")).unsatCore().propositions())
                        .containsExactlyInAnyOrder(
                                new StandardProposition(f.parse("a | b | c | d")),
                                new StandardProposition(f.parse("e => ~a & ~b")),
                                new StandardProposition(f.parse("~f => ~c & ~d")),
                                new StandardProposition(f.parse("e <=> ~f")),
                                new StandardProposition(f.parse("~f"))
                        );

        assertThat(solver.satCall().addFormulas(f.parse("e <=> ~f"), f.parse("e")).unsatCore().propositions())
                .containsExactlyInAnyOrder(
                        new StandardProposition(f.parse("a | b | c | d")),
                        new StandardProposition(f.parse("e => ~a & ~b")),
                        new StandardProposition(f.parse("~f => ~c & ~d")),
                        new StandardProposition(f.parse("e <=> ~f")),
                        new StandardProposition(f.parse("e"))
                );

        assertThat(solver.satCall()
                .addPropositions(new StandardProposition(f.parse("e <=> ~f")), new StandardProposition(f.parse("e")))
                .unsatCore().propositions())
                        .containsExactlyInAnyOrder(
                                new StandardProposition(f.parse("a | b | c | d")),
                                new StandardProposition(f.parse("e => ~a & ~b")),
                                new StandardProposition(f.parse("~f => ~c & ~d")),
                                new StandardProposition(f.parse("e <=> ~f")),
                                new StandardProposition(f.parse("e"))
                        );

        assertThat(
                solver.satCall().addFormulas(f.parse("e <=> f")).addPropositions(new StandardProposition(f.parse("~e")))
                        .model(solver.underlyingSolver().knownVariables())).isIn(
                                new Assignment(f.literal("a", true), f.literal("b", true), f.literal("c", false),
                                        f.literal("d", false), f.literal("e", false), f.literal("f", false)),
                                new Assignment(f.literal("a", false), f.literal("b", true), f.literal("c", false),
                                        f.literal("d", false), f.literal("e", false), f.literal("f", false)),
                                new Assignment(f.literal("a", true), f.literal("b", false), f.literal("c", false),
                                        f.literal("d", false), f.literal("e", false), f.literal("f", false))
                        );

        assertThat(solver.satCall().addPropositions(new StandardProposition(f.parse("e <=> f")))
                .addPropositions(new StandardProposition(f.parse("~e")))
                .model(solver.underlyingSolver().knownVariables())).isIn(
                        new Assignment(f.literal("a", true), f.literal("b", true), f.literal("c", false),
                                f.literal("d", false), f.literal("e", false), f.literal("f", false)),
                        new Assignment(f.literal("a", false), f.literal("b", true), f.literal("c", false),
                                f.literal("d", false), f.literal("e", false), f.literal("f", false)),
                        new Assignment(f.literal("a", true), f.literal("b", false), f.literal("c", false),
                                f.literal("d", false), f.literal("e", false), f.literal("f", false))
                );
    }

    private static class MaxConflictsHandler implements SATHandler {
        private final int maxConflicts;
        private int numConflicts;
        private boolean aborted;

        public MaxConflictsHandler(final int maxConflicts) {
            this.maxConflicts = maxConflicts;
            numConflicts = 0;
        }

        @Override
        public boolean aborted() {
            return aborted;
        }

        @Override
        public boolean detectedConflict() {
            aborted = numConflicts++ > maxConflicts;
            return !aborted;
        }
    }
}
