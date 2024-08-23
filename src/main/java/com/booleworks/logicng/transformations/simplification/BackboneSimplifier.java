// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.BackboneFunction;
import com.booleworks.logicng.transformations.StatelessFormulaTransformation;

/**
 * This class simplifies a formula by computing its backbone and propagating it
 * through the formula. E.g. in the formula
 * {@code A & B & (A | B | C) & (~B | D)} the backbone {@code A, B} is computed
 * and propagated, yielding the simplified formula {@code A & B & D}.
 * @version 3.0.0
 * @since 1.5.0
 */
public final class BackboneSimplifier extends StatelessFormulaTransformation {

    public BackboneSimplifier(final FormulaFactory f) {
        super(f);
    }

    @Override
    public LNGResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(formula);
        final LNGResult<Backbone> backboneResult = solver.execute(BackboneFunction.builder()
                .variables(formula.variables(f))
                .type(BackboneType.POSITIVE_AND_NEGATIVE)
                .build(), handler);
        if (!backboneResult.isSuccess()) {
            return LNGResult.canceled(backboneResult.getCancelCause());
        }
        final Backbone backbone = backboneResult.getResult();
        if (!backbone.isSat()) {
            return LNGResult.of(f.falsum());
        }
        if (!backbone.getNegativeBackbone().isEmpty() || !backbone.getPositiveBackbone().isEmpty()) {
            final Formula backboneFormula = backbone.toFormula(f);
            final Assignment assignment = new Assignment(backbone.getCompleteBackbone(f));
            final Formula restrictedFormula = formula.restrict(f, assignment);
            return LNGResult.of(f.and(backboneFormula, restrictedFormula));
        } else {
            return LNGResult.of(formula);
        }
    }
}
