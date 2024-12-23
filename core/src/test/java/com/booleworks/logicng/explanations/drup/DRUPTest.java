// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.explanations.drup;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.explanations.UnsatCore;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.propositions.ExtendedProposition;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.propositions.PropositionBackpack;
import com.booleworks.logicng.propositions.StandardProposition;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.SatCall;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DRUPTest implements LogicNGTest {

    private final FormulaFactory f = FormulaFactory.caching();

    private final Supplier<SatSolver> solverSupplier =
            () -> SatSolver.newSolver(f, SatSolverConfig.builder().proofGeneration(true).build());

    @Test
    @LongRunningTag
    public void testUnsatCoresFromDimacs() throws IOException {
        final List<List<Formula>> cnfs = new ArrayList<>(3);
        cnfs.add(DimacsReader.readCNF(f, "../test_files/drup/simple_input.cnf"));
        cnfs.add(DimacsReader.readCNF(f, "../test_files/drup/pg4_input.cnf"));
        cnfs.add(DimacsReader.readCNF(f, "../test_files/drup/avg_input.cnf", "var"));

        for (final List<Formula> cnf : cnfs) {
            final SatSolver solver = solverSupplier.get();
            solver.add(cnf);
            assertSolverUnsat(solver);
            final UnsatCore<Proposition> unsatCore = solver.satCall().unsatCore();
            verifyCore(unsatCore, cnf);
        }
    }

    @Test
    @LongRunningTag
    public void testUnsatCoresFromLargeTestset() throws IOException {
        final File testFolder = new File("../test_files/sat");
        final File[] files = testFolder.listFiles();
        assert files != null;
        int count = 0;
        for (final File file : files) {
            final String fileName = file.getName();
            if (fileName.endsWith(".cnf")) {
                final List<Formula> cnf = DimacsReader.readCNF(f, file);
                final SatSolver solver = solverSupplier.get();
                solver.add(cnf);
                if (!solver.sat()) {
                    final UnsatCore<Proposition> unsatCore = solver.satCall().unsatCore();
                    verifyCore(unsatCore, cnf);
                    count++;
                }
            }
        }
        assertThat(count).isEqualTo(11);
    }

    @Test
    public void testUnsatCoresAimTestset() throws IOException {
        final File testFolder = new File("../test_files/sat/unsat");
        final File[] files = testFolder.listFiles();
        assert files != null;
        int count = 0;
        for (final File file : files) {
            final String fileName = file.getName();
            if (fileName.endsWith(".cnf")) {
                final List<Formula> cnf = DimacsReader.readCNF(f, file);
                final SatSolver solver = solverSupplier.get();
                solver.add(cnf);
                assertSolverUnsat(solver);
                final UnsatCore<Proposition> unsatCore = solver.satCall().unsatCore();
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

        final SatSolver solver = solverSupplier.get();
        solver.addPropositions(propositions);
        assertThat(solver.sat()).isFalse();
        final UnsatCore<Proposition> unsatCore = solver.satCall().unsatCore();
        assertThat(unsatCore.getPropositions()).containsExactlyInAnyOrder(propositions.get(0), propositions.get(1),
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

        final SatSolver solver = solverSupplier.get();
        solver.addPropositions(p1, p2, p3, p4);
        final SolverState state1 = solver.saveState();
        solver.addPropositions(p5, p6);
        final SolverState state2 = solver.saveState();
        solver.addPropositions(p7, p8);

        assertThat(solver.sat()).isFalse();
        UnsatCore<Proposition> unsatCore = solver.satCall().unsatCore();
        assertThat(unsatCore.getPropositions()).containsExactlyInAnyOrder(p1, p2, p3, p4, p5, p6);

        solver.loadState(state2);
        assertThat(solver.sat()).isFalse();
        unsatCore = solver.satCall().unsatCore();
        assertThat(unsatCore.getPropositions()).containsExactlyInAnyOrder(p1, p2, p3, p4, p5, p6);

        solver.loadState(state1);
        solver.add(p9);
        assertThat(solver.sat()).isFalse();
        unsatCore = solver.satCall().unsatCore();
        assertThat(unsatCore.getPropositions()).containsExactlyInAnyOrder(p1, p2, p3, p4, p9);

        solver.loadState(state1);
        solver.add(p5);
        solver.add(p6);
        assertThat(solver.sat()).isFalse();
        unsatCore = solver.satCall().unsatCore();
        assertThat(unsatCore.getPropositions()).containsExactlyInAnyOrder(p1, p2, p3, p4, p5, p6);

        solver.loadState(state1);
        solver.add(p10);
        solver.add(p11);
        assertThat(solver.sat()).isFalse();
        unsatCore = solver.satCall().unsatCore();
        assertThat(unsatCore.getPropositions()).containsExactlyInAnyOrder(p4, p11);
    }

    @Test
    public void testTrivialCasesPropositions() throws ParserException {
        final SatSolver solver1 = solverSupplier.get();
        assertSolverSat(solver1);
        final StandardProposition p1 = new StandardProposition("P1", f.parse("$false"));
        solver1.add(p1);
        assertSolverUnsat(solver1);
        UnsatCore<Proposition> unsatCore = solver1.satCall().unsatCore();
        assertThat(unsatCore.getPropositions()).containsExactlyInAnyOrder(p1);

        final SatSolver solver2 = solverSupplier.get();
        assertSolverSat(solver2);
        final StandardProposition p2 = new StandardProposition("P2", f.parse("a"));
        solver2.add(p2);
        assertSolverSat(solver2);
        final StandardProposition p3 = new StandardProposition("P3", f.parse("~a"));
        solver2.add(p3);
        assertSolverUnsat(solver2);
        unsatCore = solver2.satCall().unsatCore();
        assertThat(unsatCore.getPropositions()).containsExactlyInAnyOrder(p2, p3);
    }

    @Test
    public void testCoreAndAssumptions() throws ParserException {
        final SatSolver solver = solverSupplier.get();
        final FormulaFactory f = solver.getFactory();
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
        solver.satCall().addFormulas(f.variable("X")).sat();

        solver.add(p5);
        solver.add(p6);
        solver.sat();
        final UnsatCore<Proposition> unsatCore = solver.satCall().unsatCore();
        assertThat(unsatCore.getPropositions()).containsExactlyInAnyOrder(p1, p2, p5, p6);
    }

    @Test
    public void testCoreAndAssumptions2() throws ParserException {
        final SatSolver solver = solverSupplier.get();
        final FormulaFactory f = solver.getFactory();
        solver.add(f.parse("~C => D"));
        solver.add(f.parse("C => D"));
        solver.add(f.parse("D => B | A"));
        solver.add(f.parse("B => X"));
        solver.add(f.parse("B => ~X"));
        solver.satCall().addFormulas(f.literal("A", false)).sat();

        solver.add(f.parse("~A"));
        solver.sat();
        assertThat(solver.satCall().unsatCore()).isNotNull();
    }

    @Test
    public void testCoreAndAssumptions3() throws ParserException {
        // Unit test for DRUP issue which led to
        // java.lang.ArrayIndexOutOfBoundsException: -1
        final SatSolver solver = solverSupplier.get();
        final FormulaFactory f = solver.getFactory();
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
        assertThat(solver.satCall().unsatCore()).isNotNull();
    }

    @Test
    public void testCoreAndAssumptions4() throws ParserException {
        final SatSolver solver = solverSupplier.get();
        solver.add(f.parse("~X1"));
        solver.satCall().addFormulas(f.variable("X1")).sat(); // caused the bug
        solver.add(f.variable("A1"));
        solver.add(f.parse("A1 => A2"));
        solver.add(f.parse("R & A2 => A3"));
        solver.add(f.parse("L & A2 => A3"));
        solver.add(f.parse("R & A3 => A4"));
        solver.add(f.parse("L & A3 => A4"));
        solver.add(f.parse("~A4"));
        solver.add(f.parse("L | R"));
        assertThat(solver.satCall().unsatCore()).isNotNull();
    }

    @Test
    public void testWithAssumptionSolving() throws ParserException {
        final SatSolver solver = solverSupplier.get();
        solver.add(f.parse("A1 => A2"));
        solver.add(f.parse("A2 => ~A1 | ~A3"));
        try (final SatCall satCall = solver.satCall().addFormulas(f.variable("A3"), f.variable("A1")).solve()) {
            assertThat(satCall.getSatResult().getResult()).isFalse();
        }
    }

    @Test
    public void testWithCcPropositions() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SatSolver solver = SatSolver.newSolver(f, SatSolverConfig.builder().proofGeneration(true)
                .cnfMethod(SatSolverConfig.CnfMethod.PG_ON_SOLVER).build());
        final ExtendedProposition<StringBackpack> p1 =
                new ExtendedProposition<>(new StringBackpack("CC"), f.parse("A + B + C <= 1"));
        final StandardProposition p2 = new StandardProposition(f.parse("A"));
        final StandardProposition p3 = new StandardProposition(f.parse("B"));
        final StandardProposition p4 = new StandardProposition(f.parse("X & Y"));
        solver.add(p1);
        solver.add(p2);
        solver.add(p3);
        solver.add(p4);
        assertThat(solver.sat()).isFalse();
        assertThat(solver.satCall().unsatCore().getPropositions()).containsExactlyInAnyOrder(p1, p2, p3);
    }

    @Test
    public void testWithSpecialUnitCase() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SatSolver solver = SatSolver.newSolver(f, SatSolverConfig.builder().proofGeneration(true).build());
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
        assertThat(solver.sat()).isTrue();
        solver.add(f.variable("a"));
        assertThat(solver.sat()).isFalse();
        assertThat(solver.satCall().unsatCore().getPropositions()).contains(p1, p2, p4, p5, p6, p7, p8, p9, p10,
                p11);
    }

    /**
     * Checks that each formula of the core is part of the original problem and
     * that the core is really unsat.
     * @param originalCore the original core
     * @param cnf          the original problem
     */
    private void verifyCore(final UnsatCore<Proposition> originalCore, final List<Formula> cnf) {
        final List<Formula> core = new ArrayList<>(originalCore.getPropositions().size());
        for (final Proposition prop : originalCore.getPropositions()) {
            core.add(prop.getFormula());
        }
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cnf).as("Core contains only original clauses").containsAll(core);
        final SatSolver verifier = SatSolver.newSolver(f, SatSolverConfig.builder().proofGeneration(true).build());
        verifier.add(core);
        softly.assertThat(verifier.sat()).as("Core is unsatisfiable").isFalse();
        softly.assertAll();
    }

    private static final class StringBackpack implements PropositionBackpack {
        private final String string;

        private StringBackpack(final String string) {
            this.string = string;
        }
    }
}
