// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.backbones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.BoundedSatHandler;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.functions.BackboneFunction;
import com.booleworks.logicng.util.FormulaHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class BackboneGenerationTest {
    private final FormulaFactory f = FormulaFactory.caching();

    @Test
    public void testNoFormulas() {
        assertThatThrownBy(() -> BackboneGeneration.compute(f, Collections.emptyList(), new TreeSet<>(),
                BackboneType.POSITIVE_AND_NEGATIVE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testBackboneGeneration() {
        final Variable x = f.variable("x");
        final Variable y = f.variable("y");
        final Variable z = f.variable("z");

        final Formula formula1 = f.and(x.negate(f), y);
        final Formula formula2 = f.or(x, z.negate(f));
        final Collection<Formula> collection = new ArrayList<>(Arrays.asList(formula1, formula2));

        assertThat(BackboneGeneration.compute(f, formula1).getCompleteBackbone(f)).containsExactly(x.negate(f), y);
        assertThat(BackboneGeneration.compute(f, formula1, BackboneType.ONLY_NEGATIVE).getCompleteBackbone(f))
                .containsExactly(x.negate(f));
        assertThat(BackboneGeneration.compute(f, formula1, new ArrayList<>(Arrays.asList(x, z))).getCompleteBackbone(f))
                .containsExactly(x.negate(f));
        assertThat(BackboneGeneration
                .compute(f, formula1, new ArrayList<>(Arrays.asList(x, z)), BackboneType.ONLY_NEGATIVE)
                .getCompleteBackbone(f)).containsExactly(x.negate(f));
        assertThat(BackboneGeneration.compute(f, collection).getCompleteBackbone(f)).containsExactly(x.negate(f), y,
                z.negate(f));
        assertThat(BackboneGeneration.compute(f, collection, BackboneType.ONLY_NEGATIVE).getCompleteBackbone(f))
                .containsExactly(x.negate(f), z.negate(f));
        assertThat(
                BackboneGeneration.compute(f, collection, new ArrayList<>(Arrays.asList(x, y))).getCompleteBackbone(f))
                .containsExactly(x.negate(f), y);
        assertThat(BackboneGeneration
                .compute(f, collection, new ArrayList<>(Arrays.asList(x, y)), BackboneType.ONLY_NEGATIVE)
                .getCompleteBackbone(f)).containsExactly(x.negate(f));

        assertThat(BackboneGeneration.computePositive(f, formula1).getCompleteBackbone(f)).containsExactly(y);
        assertThat(BackboneGeneration.computePositive(f, formula1, new ArrayList<>(Arrays.asList(x, z)))
                .getCompleteBackbone(f)).isEmpty();
        assertThat(BackboneGeneration.computePositive(f, collection).getCompleteBackbone(f)).containsExactly(y);
        assertThat(BackboneGeneration.computePositive(f, collection, new ArrayList<>(Arrays.asList(x, y)))
                .getCompleteBackbone(f)).containsExactly(y);

        assertThat(BackboneGeneration.computeNegative(f, formula1).getCompleteBackbone(f)).containsExactly(x.negate(f));
        assertThat(BackboneGeneration.computeNegative(f, formula1, new ArrayList<>(Arrays.asList(x, z)))
                .getCompleteBackbone(f)).containsExactly(x.negate(f));
        assertThat(BackboneGeneration.computeNegative(f, collection).getCompleteBackbone(f))
                .containsExactly(x.negate(f), z.negate(f));
        assertThat(BackboneGeneration.computeNegative(f, collection, new ArrayList<>(Arrays.asList(x, y)))
                .getCompleteBackbone(f)).containsExactly(x.negate(f));
    }

    @Test
    public void testSimpleBackbones() {
        final SATSolver solver = SATSolver.newSolver(f);

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
        assertThat(solver.execute(BackboneFunction.builder().variables(Collections.emptyList()).build())
                .getCompleteBackbone(f)).isEmpty();
        solver.loadState(before);

        formula = x;
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneFunction.builder().variables(variables).build()).getCompleteBackbone(f))
                .containsExactly(x);
        solver.loadState(before);

        formula = f.and(x, y);
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneFunction.builder().variables(variables).build()).getCompleteBackbone(f))
                .containsExactly(x, y);
        solver.loadState(before);

        formula = f.or(x, y);
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneFunction.builder().variables(variables).build()).getCompleteBackbone(f))
                .isEmpty();
        solver.loadState(before);

        formula = x.negate(f);
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneFunction.builder().variables(variables).build()).getCompleteBackbone(f))
                .containsExactly(x.negate(f));
        solver.loadState(before);

        formula = f.or(f.and(x, y, z), f.and(x, y, u), f.and(x, u, z));
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneFunction.builder().variables(variables).build()).getCompleteBackbone(f))
                .containsExactly(x);
        solver.loadState(before);

        formula = f.and(f.or(x, y, z), f.or(x, y, u), f.or(x, u, z));
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneFunction.builder().variables(variables).build()).getCompleteBackbone(f))
                .isEmpty();
        solver.loadState(before);

        formula = f.and(f.or(x.negate(f), y), x);
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneFunction.builder().variables(variables).build()).getCompleteBackbone(f))
                .containsExactly(x, y);
        solver.loadState(before);

        formula = f.and(f.or(x, y), f.or(x.negate(f), y));
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneFunction.builder().variables(variables).build()).getCompleteBackbone(f))
                .containsExactly(y);
        solver.loadState(before);

        formula = f.and(f.and(f.or(x.negate(f), y), x.negate(f)), f.and(z, f.or(x, y)));
        before = solver.saveState();
        solver.add(formula);
        assertThat(
                solver.execute(BackboneFunction.builder().variables(variables).build()).getCompleteBackbone(f))
                .containsExactly(x.negate(f), y, z);
        solver.loadState(before);

        formula = f.and(f.or(x, y), f.or(u, v), z);
        solver.add(formula);
        assertThat(
                solver.execute(BackboneFunction.builder().variables(variables).build()).getCompleteBackbone(f))
                .containsExactly(z);
    }

    @Test
    @LongRunningTag
    public void testSmallFormulas() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/small_formulas.txt");
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(formula);
        final Backbone backbone = solver.execute(BackboneFunction.builder().variables(formula.variables(f)).build());
        assertThat(verifyBackbone(backbone, formula, formula.variables(f))).isTrue();
    }

    @Test
    public void testLargeFormula() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/large_formula.txt");
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(formula);
        final Backbone backbone = solver.execute(BackboneFunction.builder().variables(formula.variables(f)).build());
        assertThat(verifyBackbone(backbone, formula, formula.variables(f))).isTrue();
    }

    private boolean verifyBackbone(final Backbone backbone, final Formula formula,
                                   final Collection<Variable> variables) {
        final SATSolver solver = SATSolver.newSolver(formula.getFactory());
        solver.add(formula);
        for (final Variable bbVar : backbone.getPositiveBackbone()) {
            if (solver.satCall().addFormulas(bbVar.negate(f)).sat().getResult()) {
                return false;
            }
        }
        for (final Variable bbVar : backbone.getNegativeBackbone()) {
            if (solver.satCall().addFormulas(bbVar).sat().getResult()) {
                return false;
            }
        }
        for (final Variable variable : variables) {
            if (!backbone.getPositiveBackbone().contains(variable) &&
                    !backbone.getNegativeBackbone().contains(variable)) {
                if (!solver.satCall().addFormulas(variable).sat().getResult()) {
                    return false;
                }
                if (!solver.satCall().addFormulas(variable.negate(f)).sat().getResult()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void testBackboneType() {
        final FormulaFactory f = FormulaFactory.caching();
        final SATSolver solver = SATSolver.newSolver(f);

        final Literal x = f.literal("x", true);
        final Literal y = f.literal("y", true);
        final Literal z = f.literal("z", true);

        Formula formula = f.not(x);
        SolverState before = solver.saveState();
        solver.add(formula);
        Backbone backbone = solver.execute(BackboneFunction.builder().variables(formula.variables(f))
                .type(BackboneType.POSITIVE_AND_NEGATIVE).build());
        Backbone backbonePositive = solver.execute(
                BackboneFunction.builder().variables(formula.variables(f)).type(BackboneType.ONLY_POSITIVE).build());
        Backbone backboneNegative = solver.execute(
                BackboneFunction.builder().variables(formula.variables(f)).type(BackboneType.ONLY_NEGATIVE).build());
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
        backbone = solver.execute(BackboneFunction.builder().variables(formula.variables(f))
                .type(BackboneType.POSITIVE_AND_NEGATIVE).build());
        backbonePositive = solver.execute(
                BackboneFunction.builder().variables(formula.variables(f)).type(BackboneType.ONLY_POSITIVE).build());
        backboneNegative = solver.execute(
                BackboneFunction.builder().variables(formula.variables(f)).type(BackboneType.ONLY_NEGATIVE).build());
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
        backbone = solver.execute(BackboneFunction.builder().variables(formula.variables(f))
                .type(BackboneType.POSITIVE_AND_NEGATIVE).build());
        backbonePositive = solver.execute(
                BackboneFunction.builder().variables(formula.variables(f)).type(BackboneType.ONLY_POSITIVE).build());
        backboneNegative = solver.execute(
                BackboneFunction.builder().variables(formula.variables(f)).type(BackboneType.ONLY_NEGATIVE).build());
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
        backbone = solver.execute(BackboneFunction.builder().variables(formula.variables(f))
                .type(BackboneType.POSITIVE_AND_NEGATIVE).build());
        backbonePositive = solver.execute(
                BackboneFunction.builder().variables(formula.variables(f)).type(BackboneType.ONLY_POSITIVE).build());
        backboneNegative = solver.execute(
                BackboneFunction.builder().variables(formula.variables(f)).type(BackboneType.ONLY_NEGATIVE).build());
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
        final List<Formula> formulas = DimacsReader.readCNF(f, "../test_files/sat/term1_gr_rcs_w4.shuffled.cnf");
        for (int numStarts = 0; numStarts < 10; numStarts++) {
            final ComputationHandler handler = new BoundedSatHandler(numStarts);
            final LNGResult<Backbone> result = BackboneGeneration.compute(f, formulas, FormulaHelper.variables(f, formulas),
                    BackboneType.POSITIVE_AND_NEGATIVE, handler);
            assertThat(result.isSuccess()).isFalse();
        }
    }
}
