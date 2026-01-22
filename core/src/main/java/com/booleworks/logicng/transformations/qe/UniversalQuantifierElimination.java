// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.qe;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.transformations.StatelessFormulaTransformation;

import java.util.Arrays;
import java.util.Collection;

/**
 * This transformation eliminates a number of universally quantified variables
 * by replacing them with the Shannon expansion. If {@code x} is eliminated from
 * a formula {@code f}, the resulting formula is {@code f[true/x] & f[false/x]}.
 * <p>
 * This transformation cannot be cached since it is dependent on the set of
 * literals to eliminate.
 * @version 3.0.0
 * @since 1.0
 */
public class UniversalQuantifierElimination extends StatelessFormulaTransformation {

    protected final Variable[] elimination;

    /**
     * Constructs a new universal quantifier elimination for the given
     * variables.
     * @param f         the formula factory to generate new formulas
     * @param variables the variables
     */
    public UniversalQuantifierElimination(final FormulaFactory f, final Variable... variables) {
        super(f);
        elimination = Arrays.copyOf(variables, variables.length);
    }

    /**
     * Constructs a new universal quantifier elimination for a given collection
     * of variables.
     * @param f         the formula factory to generate new formulas
     * @param variables the collection of variables
     */
    public UniversalQuantifierElimination(final FormulaFactory f, final Collection<Variable> variables) {
        super(f);
        elimination = variables.toArray(new Variable[0]);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, ComputationHandler handler) {
        Formula result = formula;
        for (final Variable var : elimination) {
            result = f.and(result.restrict(f, new Assignment(var)), result.restrict(f, new Assignment(var.negate(f))));
        }
        return LngResult.of(result);
    }
}
