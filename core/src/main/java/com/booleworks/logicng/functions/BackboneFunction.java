// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import com.booleworks.logicng.datastructures.Backbone;
import com.booleworks.logicng.datastructures.BackboneType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFunction;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.BackboneSolverFunction;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;

import java.util.Collection;

/**
 * Formula function for computing the backbone of a formula.
 * @version 3.0.0
 * @since 3.0.0
 */
public class BackboneFunction implements FormulaFunction<Backbone> {
    protected final FormulaFactory f;
    protected final Collection<Variable> variables;
    protected final BackboneType type;

    /**
     * Constructs a new function that computes the backbone of a formula.
     * @param f         the factory
     * @param variables the relevant variables
     * @param type      the backbone type
     */
    public BackboneFunction(final FormulaFactory f, final Collection<Variable> variables, final BackboneType type) {
        this.f = f;
        this.variables = variables;
        this.type = type;
    }

    /**
     * Constructs a new function that computes the backbone of a formula. The
     * backbone includes positive and negative literals.
     * @param f         the factory
     * @param variables the relevant variables
     */
    public BackboneFunction(final FormulaFactory f, final Collection<Variable> variables) {
        this(f, variables, BackboneType.POSITIVE_AND_NEGATIVE);
    }

    @Override
    public LngResult<Backbone> apply(final Formula formula, final ComputationHandler handler) {
        final SatSolver solver = SatSolver.newSolver(f,
                SatSolverConfig.builder().cnfMethod(SatSolverConfig.CnfMethod.PG_ON_SOLVER).build());
        solver.add(formula);
        final Collection<Variable> relevantVariables = variables == null ? formula.variables(f) : variables;
        final BackboneSolverFunction function = BackboneSolverFunction.builder(relevantVariables).type(type).build();
        return solver.execute(function, handler);
    }
}
