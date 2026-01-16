// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.datastructures.Backbone;
import com.booleworks.logicng.datastructures.BackboneType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.BoundedSatHandler;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.functions.BackboneSolverFunction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class BackboneFunctionTest {
    private final FormulaFactory f = FormulaFactory.caching();

    @Test
    public void testBackboneGeneration() {
        final Variable x = f.variable("x");
        final Variable y = f.variable("y");
        final Variable z = f.variable("z");

        final Formula formula1 = f.and(x.negate(f), y);
        final Formula formula2 = f.or(x, z.negate(f));
        final Formula formula3 = f.and(formula1, formula2);

        assertThat(formula1.apply(new BackboneFunction(f, formula1.variables(f))).getCompleteBackbone(f))
                .containsExactly(x.negate(f), y);
        assertThat(formula1.apply(new BackboneFunction(f, formula1.variables(f), BackboneType.ONLY_NEGATIVE))
                .getCompleteBackbone(f))
                .containsExactly(x.negate(f));
        assertThat(formula1.apply(new BackboneFunction(f, List.of(x, z)))
                .getCompleteBackbone(f))
                .containsExactly(x.negate(f));
        assertThat(formula1.apply(new BackboneFunction(f, List.of(x, z), BackboneType.ONLY_NEGATIVE))
                .getCompleteBackbone(f))
                .containsExactly(x.negate(f));
        assertThat(formula3.apply(new BackboneFunction(f, formula3.variables(f)))
                .getCompleteBackbone(f))
                .containsExactly(x.negate(f), y, z.negate(f));
        assertThat(formula3.apply(new BackboneFunction(f, formula3.variables(f), BackboneType.ONLY_NEGATIVE))
                .getCompleteBackbone(f))
                .containsExactly(x.negate(f), z.negate(f));
        assertThat(formula3.apply(new BackboneFunction(f, new ArrayList<>(Arrays.asList(x, y))))
                .getCompleteBackbone(f))
                .containsExactly(x.negate(f), y);
        assertThat(formula3.apply(
                        new BackboneFunction(f, new ArrayList<>(Arrays.asList(x, y)), BackboneType.ONLY_NEGATIVE))
                .getCompleteBackbone(f))
                .containsExactly(x.negate(f));
    }

    @Test
    public void testSimpleBackbones() {
        final SatSolver solver = SatSolver.newSolver(f);

        final Literal x = f.literal("x", true);
        final Literal y = f.literal("y", true);
        final Literal z = f.literal("z", true);
        final Literal u = f.literal("u", true);
        final Literal v = f.literal("v", true);

        final Collection<Variable> variables = new ArrayList<>(Arrays.asList(f.variable("x"), f.variable("y"),
                f.variable("z"), f.variable("u"), f.variable("v")));

        Formula formula = f.verum();

        SolverState before = solver.saveState();
        solver.add(formula);
        assertThat(solver.execute(BackboneSolverFunction.builder(Collections.emptyList()).build())
                .getCompleteBackbone(f)).isEmpty();
        solver.loadState(before);

        formula = x;
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneSolverFunction.builder(variables).build()).getCompleteBackbone(f))
                .containsExactly(x);
        solver.loadState(before);

        formula = f.and(x, y);
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneSolverFunction.builder(variables).build()).getCompleteBackbone(f))
                .containsExactly(x, y);
        solver.loadState(before);

        formula = f.or(x, y);
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneSolverFunction.builder(variables).build()).getCompleteBackbone(f))
                .isEmpty();
        solver.loadState(before);

        formula = x.negate(f);
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneSolverFunction.builder(variables).build()).getCompleteBackbone(f))
                .containsExactly(x.negate(f));
        solver.loadState(before);

        formula = f.or(f.and(x, y, z), f.and(x, y, u), f.and(x, u, z));
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneSolverFunction.builder(variables).build()).getCompleteBackbone(f))
                .containsExactly(x);
        solver.loadState(before);

        formula = f.and(f.or(x, y, z), f.or(x, y, u), f.or(x, u, z));
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneSolverFunction.builder(variables).build()).getCompleteBackbone(f))
                .isEmpty();
        solver.loadState(before);

        formula = f.and(f.or(x.negate(f), y), x);
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneSolverFunction.builder(variables).build()).getCompleteBackbone(f))
                .containsExactly(x, y);
        solver.loadState(before);

        formula = f.and(f.or(x, y), f.or(x.negate(f), y));
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneSolverFunction.builder(variables).build()).getCompleteBackbone(f))
                .containsExactly(y);
        solver.loadState(before);

        formula = f.and(f.and(f.or(x.negate(f), y), x.negate(f)), f.and(z, f.or(x, y)));
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneSolverFunction.builder(variables).build()).getCompleteBackbone(f))
                .containsExactly(x.negate(f), y, z);
        solver.loadState(before);

        formula = f.and(f.or(x, y), f.or(u, v), z);
        solver.add(formula);
        assertThat(
                solver.execute(BackboneSolverFunction.builder(variables).build()).getCompleteBackbone(f))
                .containsExactly(z);
    }

    @Test
    @LongRunningTag
    public void testSmallFormulas() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/small_formulas.txt");
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(formula);
        final Backbone backbone = solver.execute(BackboneSolverFunction.builder(formula.variables(f)).build());
        assertThat(verifyBackbone(backbone, formula, formula.variables(f))).isTrue();
    }

    @Test
    public void testLargeFormula() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/large_formula.txt");
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(formula);
        final Backbone backbone = solver.execute(BackboneSolverFunction.builder(formula.variables(f)).build());
        assertThat(verifyBackbone(backbone, formula, formula.variables(f))).isTrue();
    }

    private boolean verifyBackbone(final Backbone backbone, final Formula formula,
                                   final Collection<Variable> variables) {
        final SatSolver solver = SatSolver.newSolver(formula.getFactory());
        solver.add(formula);
        for (final Variable bbVar : backbone.getPositiveBackbone()) {
            if (solver.satCall().addFormula(bbVar.negate(f)).sat().getResult()) {
                return false;
            }
        }
        for (final Variable bbVar : backbone.getNegativeBackbone()) {
            if (solver.satCall().addFormula(bbVar).sat().getResult()) {
                return false;
            }
        }
        for (final Variable variable : variables) {
            if (!backbone.getPositiveBackbone().contains(variable) &&
                    !backbone.getNegativeBackbone().contains(variable)) {
                if (!solver.satCall().addFormula(variable).sat().getResult()) {
                    return false;
                }
                if (!solver.satCall().addFormula(variable.negate(f)).sat().getResult()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void testBackboneType() {
        final FormulaFactory f = FormulaFactory.caching();
        final SatSolver solver = SatSolver.newSolver(f);

        final Literal x = f.literal("x", true);
        final Literal y = f.literal("y", true);
        final Literal z = f.literal("z", true);

        Formula formula = f.not(x);
        SolverState before = solver.saveState();
        solver.add(formula);
        Backbone backbone = solver.execute(BackboneSolverFunction.builder(formula.variables(f))
                .type(BackboneType.POSITIVE_AND_NEGATIVE).build());
        Backbone backbonePositive = solver.execute(
                BackboneSolverFunction.builder(formula.variables(f)).type(BackboneType.ONLY_POSITIVE).build());
        Backbone backboneNegative = solver.execute(
                BackboneSolverFunction.builder(formula.variables(f)).type(BackboneType.ONLY_NEGATIVE).build());
        assertThat(backbone.getCompleteBackbone(f)).containsExactly(x.negate(f));
        assertThat(backbonePositive.getCompleteBackbone(f)).isEmpty();
        assertThat(backboneNegative.getCompleteBackbone(f)).containsExactly(x.negate(f));
        SortedSet<Literal> combinedPosNegBackbone = new TreeSet<>(backbonePositive.getCompleteBackbone(f));
        combinedPosNegBackbone.addAll(backboneNegative.getCompleteBackbone(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(combinedPosNegBackbone);
        solver.loadState(before);

        formula = f.and(f.or(x, y.negate(f)), x.negate(f));
        before = solver.saveState();
        solver.add(formula);
        backbone = solver.execute(
                BackboneSolverFunction.builder(formula.variables(f)).type(BackboneType.POSITIVE_AND_NEGATIVE).build());
        backbonePositive = solver.execute(
                BackboneSolverFunction.builder(formula.variables(f)).type(BackboneType.ONLY_POSITIVE).build());
        backboneNegative = solver.execute(
                BackboneSolverFunction.builder(formula.variables(f)).type(BackboneType.ONLY_NEGATIVE)
                        .build());
        assertThat(backbone.getCompleteBackbone(f)).containsExactly(x.negate(f), y.negate(f));
        assertThat(backbonePositive.getCompleteBackbone(f)).isEmpty();
        assertThat(backboneNegative.getCompleteBackbone(f)).containsExactly(x.negate(f), y.negate(f));
        combinedPosNegBackbone = new TreeSet<>(backbonePositive.getCompleteBackbone(f));
        combinedPosNegBackbone.addAll(backboneNegative.getCompleteBackbone(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(combinedPosNegBackbone);
        solver.loadState(before);

        formula = f.and(f.or(x, y), f.or(x.negate(f), y));
        before = solver.saveState();
        solver.add(formula);
        backbone = solver.execute(
                BackboneSolverFunction.builder(formula.variables(f)).type(BackboneType.POSITIVE_AND_NEGATIVE).build());
        backbonePositive = solver.execute(
                BackboneSolverFunction.builder(formula.variables(f)).type(BackboneType.ONLY_POSITIVE).build());
        backboneNegative = solver.execute(
                BackboneSolverFunction.builder(formula.variables(f)).type(BackboneType.ONLY_NEGATIVE).build());
        assertThat(backbone.getCompleteBackbone(f)).containsExactly(y);
        assertThat(backbonePositive.getCompleteBackbone(f)).containsExactly(y);
        assertThat(backboneNegative.getCompleteBackbone(f)).isEmpty();
        combinedPosNegBackbone = new TreeSet<>(backbonePositive.getCompleteBackbone(f));
        combinedPosNegBackbone.addAll(backboneNegative.getCompleteBackbone(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(combinedPosNegBackbone);
        solver.loadState(before);

        formula = f.and(f.and(f.or(x.negate(f), y), x.negate(f)), f.and(z, f.or(x, y)));
        before = solver.saveState();
        solver.add(formula);
        backbone = solver.execute(
                BackboneSolverFunction.builder(formula.variables(f)).type(BackboneType.POSITIVE_AND_NEGATIVE).build());
        backbonePositive = solver.execute(
                BackboneSolverFunction.builder(formula.variables(f)).type(BackboneType.ONLY_POSITIVE).build());
        backboneNegative = solver.execute(
                BackboneSolverFunction.builder(formula.variables(f)).type(BackboneType.ONLY_NEGATIVE).build());
        assertThat(backbone.getCompleteBackbone(f)).containsExactly(x.negate(f), y, z);
        assertThat(backbone.getOptionalVariables()).containsExactly();
        assertThat(backbonePositive.getCompleteBackbone(f)).containsExactly(y, z);
        assertThat(backboneNegative.getCompleteBackbone(f)).containsExactly(x.negate(f));
        combinedPosNegBackbone = new TreeSet<>(backbonePositive.getCompleteBackbone(f));
        combinedPosNegBackbone.addAll(backboneNegative.getCompleteBackbone(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(combinedPosNegBackbone);
        solver.loadState(before);
    }

    @Test
    public void testCancellationPoints() throws IOException {
        final Formula formula = f.and(DimacsReader.readCNF(f, "../test_files/sat/term1_gr_rcs_w4.shuffled.cnf"));
        for (int numStarts = 0; numStarts < 10; numStarts++) {
            final ComputationHandler handler = new BoundedSatHandler(numStarts);
            final LngResult<Backbone> result =
                    new BackboneFunction(f, formula.variables(f), BackboneType.POSITIVE_AND_NEGATIVE)
                            .apply(formula, handler);
            assertThat(result.isSuccess()).isFalse();
        }
    }
}
