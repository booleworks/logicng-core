// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.qe;

import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.transformations.StatelessFormulaTransformation;

import java.util.Arrays;
import java.util.Collection;

/**
 * This transformation eliminates a number of universally quantified variables by replacing them with the Shannon
 * expansion.  If {@code x} is eliminated from a formula {@code f}, the resulting formula is
 * {@code f[true/x] & f[false/x]}.
 * <p>
 * This transformation cannot be cached since it is dependent on the set of literals to eliminate.
 * @version 3.0.0
 * @since 1.0
 */
public final class UniversalQuantifierElimination extends StatelessFormulaTransformation {

    private final Variable[] elimination;

    /**
     * Constructs a new universal quantifier elimination for the given variables.
     * @param f         the formula factory to generate new formulas
     * @param variables the variables
     */
    public UniversalQuantifierElimination(final FormulaFactory f, final Variable... variables) {
        super(f);
        elimination = Arrays.copyOf(variables, variables.length);
    }

    /**
     * Constructs a new universal quantifier elimination for a given collection of variables.
     * @param f         the formula factory to generate new formulas
     * @param variables the collection of variables
     */
    public UniversalQuantifierElimination(final FormulaFactory f, final Collection<Variable> variables) {
        super(f);
        elimination = variables.toArray(new Variable[0]);
    }

    @Override
    public Formula apply(final Formula formula) {
        Formula result = formula;
        for (final Variable var : elimination) {
            result = f.and(result.restrict(new Assignment(var), f), result.restrict(new Assignment(var.negate(f)), f));
        }
        return result;
    }
}
