// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.drup;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.explanations.UNSATCore;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.propositions.ExtendedProposition;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.PropositionBackpack;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DRUPTest implements LogicNGTest {

    private final FormulaFactory f = FormulaFactory.caching();

    private final Supplier<SATSolver> solverSupplier = () -> MiniSat.miniSat(f, MiniSatConfig.builder().proofGeneration(true).build());

    @Test
    @LongRunningTag
    public void testUnsatCoresFromDimacs() throws IOException {
        final List<List<Formula>> cnfs = new ArrayList<>(3);
        cnfs.add(DimacsReader.readCNF(f, "src/test/resources/drup/simple_input.cnf"));
        cnfs.add(DimacsReader.readCNF(f, "src/test/resources/drup/pg4_input.cnf"));
        cnfs.add(DimacsReader.readCNF(f, "src/test/resources/drup/avg_input.cnf", "var"));

        for (final List<Formula> cnf : cnfs) {
            final SATSolver solver = solverSupplier.get();
            solver.add(cnf);
            assertSolverUnsat(solver);
                final UNSATCore<Proposition> unsatCore = solver.unsatCore();
            verifyCore(unsatCore, cnf);
        }
    }

    @Test
    @LongRunningTag
    public void testUnsatCoresFromLargeTestset() throws IOException {
        final File testFolder = new File("src/test/resources/sat");
        final File[] files = testFolder.listFiles();
        assert files != null;
        int count = 0;
        for (final File file : files) {
            final String fileName = file.getName();
            if (fileName.endsWith(".cnf")) {
                final List<Formula> cnf = DimacsReader.readCNF(f, file);
                final SATSolver solver = solverSupplier.get();
                solver.add(cnf);
                    if (solver.sat() == Tristate.FALSE) {
                        final UNSATCore<Proposition> unsatCore = solver.unsatCore();
                    verifyCore(unsatCore, cnf);
                    count++;
                }
            }
        }
        assertThat(count).isEqualTo(11);
    }

    @Test
    public void testUnsatCoresAimTestset() throws IOException {
        final File testFolder = new File("src/test/resources/sat/unsat");
        final File[] files = testFolder.listFiles();
        assert files != null;
        int count = 0;
        for (final File file : files) {
            final String fileName = file.getName();
            if (fileName.endsWith(".cnf")) {
                final List<Formula> cnf = DimacsReader.readCNF(f, file);
                final SATSolver solver = solverSupplier.get();
                solver.add(cnf);
                assertSolverUnsat(solver);
                    final UNSATCore<Proposition> unsatCore = solver.unsatCore();
                verifyCore(unsatCore, cnf);
                count++;
            }
        }
        assertThat(count).isEqualTo(36);
    }

    @Test
    public void testPropositionHandling() throws ParserException {
        final List<Proposition> propositions = new ArrayList<>();
        propositions.add(new StandardProposition("P1", f.parse("((a & b) => c) &  ((a & b) => d)")));
        propositions.add(new StandardProposition("P2", f.parse("(c & d) <=> ~e")));
        propositions.add(new StandardProposition("P3", f.parse("~e => f | g")));
        propositions.add(new StandardProposition("P4", f.parse("(f => ~a) & (g => ~b) & p & q")));
        propositions.add(new StandardProposition("P5", f.parse("a => b")));
        propositions.add(new StandardProposition("P6", f.parse("a")));
        propositions.add(new StandardProposition("P7", f.parse("g | h")));
        propositions.add(new StandardProposition("P8", f.parse("(x => ~y | z) & (z | w)")));

        final SATSolver solver = solverSupplier.get();
        solver.addPropositions(propositions);
            Assertions.assertThat(solver.sat()).isEqualTo(Tristate.FALSE);
            final UNSATCore<Proposition> unsatCore = solver.unsatCore();
        assertThat(unsatCore.propositions()).containsExactlyInAnyOrder(propositions.get(0), propositions.get(1),
                propositions.get(2), propositions.get(3), propositions.get(4), propositions.get(5));
    }

    @Test
    public void testPropositionIncDec() throws ParserException {
        final StandardProposition p1 = new StandardProposition("P1", f.parse("((a & b) => c) &  ((a & b) => d)"));
        final StandardProposition p2 = new StandardProposition("P2", f.parse("(c & d) <=> ~e"));
        final StandardProposition p3 = new StandardProposition("P3", f.parse("~e => f | g"));
        final StandardProposition p4 = new StandardProposition("P4", f.parse("(f => ~a) & (g => ~b) & p & q"));
        final StandardProposition p5 = new StandardProposition("P5", f.parse("a => b"));
        final StandardProposition p6 = new StandardProposition("P6", f.parse("a"));
        final StandardProposition p7 = new StandardProposition("P7", f.parse("g | h"));
        final StandardProposition p8 = new StandardProposition("P8", f.parse("(x => ~y | z) & (z | w)"));
        final StandardProposition p9 = new StandardProposition("P9", f.parse("a & b"));
        final StandardProposition p10 = new StandardProposition("P10", f.parse("(p => q) & p"));
        final StandardProposition p11 = new StandardProposition("P11", f.parse("a & ~q"));

        final SATSolver solver = solverSupplier.get();
        solver.addPropositions(p1, p2, p3, p4);
        final SolverState state1 = solver.saveState();
        solver.addPropositions(p5, p6);
        final SolverState state2 = solver.saveState();
        solver.addPropositions(p7, p8);

        Assertions.assertThat(solver.sat()).isEqualTo(Tristate.FALSE);
        UNSATCore<Proposition> unsatCore = solver.unsatCore();
        assertThat(unsatCore.propositions()).containsExactlyInAnyOrder(p1, p2, p3, p4, p5, p6);

        solver.loadState(state2);
        Assertions.assertThat(solver.sat()).isEqualTo(Tristate.FALSE);
        unsatCore = solver.unsatCore();
        assertThat(unsatCore.propositions()).containsExactlyInAnyOrder(p1, p2, p3, p4, p5, p6);

        solver.loadState(state1);
        solver.add(p9);
        Assertions.assertThat(solver.sat()).isEqualTo(Tristate.FALSE);
        unsatCore = solver.unsatCore();
        assertThat(unsatCore.propositions()).containsExactlyInAnyOrder(p1, p2, p3, p4, p9);

        solver.loadState(state1);
        solver.add(p5);
        solver.add(p6);
        Assertions.assertThat(solver.sat()).isEqualTo(Tristate.FALSE);
        unsatCore = solver.unsatCore();
        assertThat(unsatCore.propositions()).containsExactlyInAnyOrder(p1, p2, p3, p4, p5, p6);

        solver.loadState(state1);
        solver.add(p10);
        solver.add(p11);
        Assertions.assertThat(solver.sat()).isEqualTo(Tristate.FALSE);
        unsatCore = solver.unsatCore();
        assertThat(unsatCore.propositions()).containsExactlyInAnyOrder(p4, p11);
    }

    @Test
    public void testTrivialCasesPropositions() throws ParserException {
        final SATSolver solver1 = solverSupplier.get();
        assertSolverSat(solver1);
        final StandardProposition p1 = new StandardProposition("P1", f.parse("$false"));
        solver1.add(p1);
        assertSolverUnsat(solver1);
        UNSATCore<Proposition> unsatCore = solver1.unsatCore();
        assertThat(unsatCore.propositions()).containsExactlyInAnyOrder(p1);

        final SATSolver solver2 = solverSupplier.get();
        assertSolverSat(solver2);
        final StandardProposition p2 = new StandardProposition("P2", f.parse("a"));
        solver2.add(p2);
        assertSolverSat(solver2);
        final StandardProposition p3 = new StandardProposition("P3", f.parse("~a"));
        solver2.add(p3);
        assertSolverUnsat(solver2);
        unsatCore = solver2.unsatCore();
        assertThat(unsatCore.propositions()).containsExactlyInAnyOrder(p2, p3);
    }

    @Test
    public void testCoreAndAssumptions() throws ParserException {
        final SATSolver solver = solverSupplier.get();
        final FormulaFactory f = solver.factory();
        final StandardProposition p1 = new StandardProposition(f.parse("A => B"));
        final StandardProposition p2 = new StandardProposition(f.parse("A & B => G"));
        final StandardProposition p3 = new StandardProposition(f.or(f.literal("X", false), f.literal("A", true)));
        final StandardProposition p4 = new StandardProposition(f.or(f.literal("X", false), f.literal("G", false)));
        final StandardProposition p5 = new StandardProposition(f.literal("G", false));
        final StandardProposition p6 = new StandardProposition(f.literal("A", true));
        solver.add(p1);
        solver.add(p2);
        solver.add(p3);
        solver.add(p4);

        // Assumption call
            solver.satCall().assumptions(f.variable("X")).sat();

        solver.add(p5);
        solver.add(p6);
            solver.sat();
            final UNSATCore<Proposition> unsatCore = solver.unsatCore();
        assertThat(unsatCore.propositions()).containsExactlyInAnyOrder(p1, p2, p5, p6);
    }

    @Test
    public void testCoreAndAssumptions2() throws ParserException {
        final SATSolver solver = solverSupplier.get();
        final FormulaFactory f = solver.factory();
        solver.add(f.parse("~C => D"));
        solver.add(f.parse("C => D"));
        solver.add(f.parse("D => B | A"));
        solver.add(f.parse("B => X"));
        solver.add(f.parse("B => ~X"));
            solver.satCall().assumptions(f.literal("A", false)).sat();

        solver.add(f.parse("~A"));
            solver.sat();
            Assertions.assertThat(solver.unsatCore()).isNotNull();
    }

    @Test
    public void testCoreAndAssumptions3() throws ParserException {
        // Unit test for DRUP issue which led to java.lang.ArrayIndexOutOfBoundsException: -1
        final SATSolver solver = solverSupplier.get();
        final FormulaFactory f = solver.factory();
        solver.add(f.parse("X => Y"));
        solver.add(f.parse("X => Z"));
        solver.add(f.parse("C => E"));
        solver.add(f.parse("D => ~F"));
        solver.add(f.parse("B => M"));
        solver.add(f.parse("D => N"));
        solver.add(f.parse("G => O"));
        solver.add(f.parse("A => B"));
        solver.add(f.parse("T1 <=> A & K & ~B & ~C"));
        solver.add(f.parse("T2 <=> A & B & C & K"));
        solver.add(f.parse("T1 + T2 = 1"));
            solver.sat(); // required for DRUP issue

        solver.add(f.parse("Y => ~X & D"));
        solver.add(f.parse("X"));

            solver.sat();
            Assertions.assertThat(solver.unsatCore()).isNotNull();
    }

    @Test
    public void testCoreAndAssumptions4() throws ParserException {
        final SATSolver solver = solverSupplier.get();
        solver.add(f.parse("~X1"));
            solver.satCall().assumptions(f.variable("X1")).sat(); // caused the bug
        solver.add(f.variable("A1"));
        solver.add(f.parse("A1 => A2"));
        solver.add(f.parse("R & A2 => A3"));
        solver.add(f.parse("L & A2 => A3"));
        solver.add(f.parse("R & A3 => A4"));
        solver.add(f.parse("L & A3 => A4"));
        solver.add(f.parse("~A4"));
        solver.add(f.parse("L | R"));
            Assertions.assertThat(solver.unsatCore()).isNotNull();
    }

    @Test
    public void testWithCcPropositions() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SATSolver solver = MiniSat.miniSat(f, MiniSatConfig.builder().proofGeneration(true).cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER).build());
        final ExtendedProposition<StringBackpack> p1 = new ExtendedProposition<>(new StringBackpack("CC"), f.parse("A + B + C <= 1"));
        final StandardProposition p2 = new StandardProposition(f.parse("A"));
        final StandardProposition p3 = new StandardProposition(f.parse("B"));
        final StandardProposition p4 = new StandardProposition(f.parse("X & Y"));
        solver.add(p1);
        solver.add(p2);
        solver.add(p3);
        solver.add(p4);
        Assertions.assertThat(solver.sat()).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(solver.unsatCore().propositions()).containsExactlyInAnyOrder(p1, p2, p3);
    }

    @Test
    public void testWithSpecialUnitCaseMiniSat() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SATSolver solver = MiniSat.miniSat(f, MiniSatConfig.builder().proofGeneration(true).build());
        final StandardProposition p1 = new StandardProposition(f.parse("a => b"));
        final StandardProposition p2 = new StandardProposition(f.parse("a => c | d"));
        final StandardProposition p3 = new StandardProposition(f.parse("b => c | d"));
        final StandardProposition p4 = new StandardProposition(f.parse("e | f | g | h => i"));
        final StandardProposition p5 = new StandardProposition(f.parse("~j => k | j"));
        final StandardProposition p6 = new StandardProposition(f.parse("b => ~(e | f)"));
        final StandardProposition p7 = new StandardProposition(f.parse("c => ~j"));
        final StandardProposition p8 = new StandardProposition(f.parse("l | m => ~i"));
        final StandardProposition p9 = new StandardProposition(f.parse("j => (f + g + h = 1)"));
        final StandardProposition p10 = new StandardProposition(f.parse("d => (l + m + e + f = 1)"));
        final StandardProposition p11 = new StandardProposition(f.parse("~k"));
        solver.addPropositions(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11);
        Assertions.assertThat(solver.sat()).isEqualTo(Tristate.TRUE);
        solver.add(f.variable("a"));
        Assertions.assertThat(solver.sat()).isEqualTo(Tristate.FALSE);
        Assertions.assertThat(solver.unsatCore().propositions()).contains(p1, p2, p4, p5, p6, p7, p8, p9, p10, p11);
    }

    /**
     * Checks that each formula of the core is part of the original problem and that the core is really unsat.
     * @param originalCore the original core
     * @param cnf          the original problem
     */
    private void verifyCore(final UNSATCore<Proposition> originalCore, final List<Formula> cnf) {
        final List<Formula> core = new ArrayList<>(originalCore.propositions().size());
        for (final Proposition prop : originalCore.propositions()) {
            core.add(prop.formula());
        }
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cnf).as("Core contains only original clauses").containsAll(core);
        final MiniSat verifier = MiniSat.miniSat(f, MiniSatConfig.builder().proofGeneration(true).build());
        verifier.add(core);
        softly.assertThat(verifier.sat()).as("Core is unsatisfiable").isEqualTo(Tristate.FALSE);
        softly.assertAll();
    }

    private static final class StringBackpack implements PropositionBackpack {
        private final String string;

        private StringBackpack(final String string) {
            this.string = string;
        }
    }
}
