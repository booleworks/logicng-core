// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.util.FormulaHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Superclass for canonical normal form enumeration (CNF or DNF) via enumeration
 * of the falsifying/satisfying assignments.
 * @version 3.0.0
 * @since 2.3.0
 */
public abstract class CanonicalEnumeration extends StatelessFormulaTransformation {

    /**
     * Constructor.
     * @param f the formula factory to generate new formulas
     **/
    public CanonicalEnumeration(final FormulaFactory f) {
        super(f);
    }

    /**
     * Constructs the canonical CNF/DNF of the given formula by enumerating the
     * falsifying/satisfying assignments.
     * @param formula the formula
     * @param handler the computation handler
     * @param cnf     {@code true} if the canonical CNF should be computed,
     *                {@code false} if the canonical DNF should be computed
     * @return the (potentially canceled) result with the canonical normal form
     *         (CNF or DNF) of the formula
     */
    protected LNGResult<Formula> compute(final Formula formula, final ComputationHandler handler, final boolean cnf) {
        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(cnf ? formula.negate(f) : formula);
        final LNGResult<List<Model>> enumerationResult = solver.execute(
                ModelEnumerationFunction.builder(formula.variables(f)).build(), handler);
        if (!enumerationResult.isSuccess()) {
            return LNGResult.canceled(enumerationResult.getCancelCause());
        } else {
            final List<Model> enumeration = enumerationResult.getResult();
            if (enumeration.isEmpty()) {
                return LNGResult.of(f.constant(cnf));
            }
            final List<Formula> ops = new ArrayList<>();
            for (final Model model : enumeration) {
                final List<Literal> literals = model.getLiterals();
                final Formula term =
                        cnf ? f.or(FormulaHelper.negate(f, literals, ArrayList::new)) : f.and(model.getLiterals());
                ops.add(term);
            }
            return LNGResult.of(cnf ? f.and(ops) : f.or(ops));
        }
    }
}