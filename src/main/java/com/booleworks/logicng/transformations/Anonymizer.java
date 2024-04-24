// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.datastructures.Substitution;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;

/**
 * An anonymizer replaces all variables in a formula with new variables
 * generated from a given prefix and a counter.
 * <p>
 * An instance can be used to anonymize multiple formulas. In this case,
 * variables with the same name will be replaced with the same anonymized
 * variable.
 * <p>
 * After anonymizing one or more formulas, the mapping from original variable to
 * anonymized variable can be accessed via {@link #getSubstitution()}.
 * @version 3.0.0
 * @since 1.4.0
 */
public final class Anonymizer extends StatefulFormulaTransformation<Substitution> {

    private final String prefix;
    private int counter;

    /**
     * Constructs a new anonymizer with the standard variable prefix 'v'.
     * @param f the formula factory to generate new formulas
     */
    public Anonymizer(final FormulaFactory f) {
        this(f, "v");
    }

    /**
     * Constructs a new anonymizer with a given prefix for the newly introduced
     * variables.
     * @param f      the formula factory to generate new formulas
     * @param prefix the prefix for the new variables
     */
    public Anonymizer(final FormulaFactory f, final String prefix) {
        this(f, prefix, 0);
    }

    /**
     * Constructs a new anonymizer with a given prefix for the newly introduced
     * variables.
     * @param f            the formula factory to generate new formulas
     * @param prefix       the prefix for the new variables
     * @param startCounter where should the counter start
     */
    public Anonymizer(final FormulaFactory f, final String prefix, final int startCounter) {
        super(f);
        this.prefix = prefix;
        counter = startCounter;
    }

    @Override
    protected Substitution inititialState() {
        return new Substitution();
    }

    /**
     * Returns the substitution which was used to anonymize the formula(s).
     * <p>
     * Although a substitution maps from variables to formulas, it is guaranteed
     * that the substitution always maps to variables. So the following cast
     * will always be safe:
     * <p>
     * {@code (Variable) getSubstitution().getSubstitution(x)}
     * @return the substitution which was used to anonymize the formula(s)
     */
    public Substitution getSubstitution() {
        return state;
    }

    @Override
    public Formula apply(final Formula formula) {
        if (formula.variables(f).isEmpty()) {
            return formula;
        }
        for (final Variable variable : formula.variables(f)) {
            if (state.getSubstitution(variable) == null) {
                state.addMapping(variable, f.variable(prefix + counter++));
            }
        }
        return formula.substitute(f, state);
    }
}
