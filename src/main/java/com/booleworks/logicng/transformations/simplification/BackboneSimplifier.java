// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
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
    public Formula apply(final Formula formula) {
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(formula);
        final Backbone backbone = solver.execute(BackboneFunction.builder().variables(formula.variables(f))
                .type(BackboneType.POSITIVE_AND_NEGATIVE).build());
        if (!backbone.isSat()) {
            return f.falsum();
        }
        if (!backbone.getNegativeBackbone().isEmpty() || !backbone.getPositiveBackbone().isEmpty()) {
            final Formula backboneFormula = backbone.toFormula(f);
            final Assignment assignment = new Assignment(backbone.getCompleteBackbone(f));
            final Formula restrictedFormula = formula.restrict(f, assignment);
            return f.and(backboneFormula, restrictedFormula);
        } else {
            return formula;
        }
    }
}
