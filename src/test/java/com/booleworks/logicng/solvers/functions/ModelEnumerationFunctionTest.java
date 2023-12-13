// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.util.FormulaHelper.variables;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ModelEnumerationFunctionTest {

    private final FormulaFactory f;

    public ModelEnumerationFunctionTest() {
        f = FormulaFactory.caching();
    }

    @Test
    public void testModelEnumerationSimple() throws ParserException {
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("A & (B | C)"));
        final List<Assignment> models = solver.execute(ModelEnumerationFunction.builder().build());
        assertThat(models).containsExactlyInAnyOrder(
                new Assignment(f.variable("A"), f.variable("B"), f.variable("C")),
                new Assignment(f.variable("A"), f.variable("B"), f.literal("C", false)),
                new Assignment(f.variable("A"), f.literal("B", false), f.variable("C"))
        );
    }

    @Test
    public void testFastEvaluable() throws ParserException {
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("A & (B | C)"));
        List<Assignment> models = solver.execute(ModelEnumerationFunction.builder().build());
        assertThat(models).extracting(Assignment::fastEvaluable).containsOnly(false);
        models = solver.execute(ModelEnumerationFunction.builder().fastEvaluable(false).build());
        assertThat(models).extracting(Assignment::fastEvaluable).containsOnly(false);
        models = solver.execute(ModelEnumerationFunction.builder().fastEvaluable(true).build());
        assertThat(models).extracting(Assignment::fastEvaluable).containsOnly(true);
    }

    @Test
    public void testVariableRemovedBySimplificationOccursInModels() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().simplifyComplementaryOperands(true).build());
        final SATSolver solver = MiniSat.miniSat(this.f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER).build());
        final Variable a = f.variable("A");
        final Variable b = f.variable("B");
        final Formula formula = this.f.parse("A & B => A");
        solver.add(formula); // during NNF conversion, used by the PG transformation, the formula simplifies to verum when added to the solver
        final List<Assignment> models = solver.execute(ModelEnumerationFunction.builder().variables(formula.variables(f)).build());
        assertThat(models).hasSize(4);
        for (final Assignment model : models) {
            assertThat(variables(f, model.literals())).containsExactlyInAnyOrder(a, b);
        }
    }

    @Test
    public void testUnknownVariableNotOccurringInModel() {
        final SATSolver solver = MiniSat.miniSat(f);
        final Variable a = f.variable("A");
        solver.add(a);
        final List<Assignment> models = solver.execute(ModelEnumerationFunction.builder().variables(f.variables("A", "X")).build());
        assertThat(models).hasSize(1);
        assertThat(models.get(0).literals()).containsExactly(a);
    }
}
