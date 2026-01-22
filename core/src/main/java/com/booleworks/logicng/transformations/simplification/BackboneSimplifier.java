// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Backbone;
import com.booleworks.logicng.datastructures.BackboneType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.BackboneSolverFunction;
import com.booleworks.logicng.transformations.StatelessFormulaTransformation;

/**
 * This class simplifies a formula by computing its backbone and propagating it
 * through the formula. E.g. in the formula
 * {@code A & B & (A | B | C) & (~B | D)} the backbone {@code A, B} is computed
 * and propagated, yielding the simplified formula {@code A & B & D}.
 * @version 3.0.0
 * @since 1.5.0
 */
public class BackboneSimplifier extends StatelessFormulaTransformation {

    /**
     * Constructs a new instance.
     * @param f the formula factory
     */
    public BackboneSimplifier(final FormulaFactory f) {
        super(f);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(formula);
        final LngResult<Backbone> backboneResult = solver.execute(BackboneSolverFunction.builder(formula.variables(f))
                .type(BackboneType.POSITIVE_AND_NEGATIVE)
                .build(), handler);
        if (!backboneResult.isSuccess()) {
            return LngResult.canceled(backboneResult.getCancelCause());
        }
        final Backbone backbone = backboneResult.getResult();
        if (!backbone.isSat()) {
            return LngResult.of(f.falsum());
        }
        if (!backbone.getNegativeBackbone().isEmpty() || !backbone.getPositiveBackbone().isEmpty()) {
            final Formula backboneFormula = backbone.toFormula(f);
            final Assignment assignment = new Assignment(backbone.getCompleteBackbone(f));
            final Formula restrictedFormula = formula.restrict(f, assignment);
            return LngResult.of(f.and(backboneFormula, restrictedFormula));
        } else {
            return LngResult.of(formula);
        }
    }
}
