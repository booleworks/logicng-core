// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf.datastructures;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.dnnf.functions.DnnfFunction;

import java.util.SortedSet;

/**
 * A DNNF - Decomposable Negation Normal Form.
 * @version 2.0.0
 * @since 2.0.0
 */
public final class Dnnf {

    private final SortedSet<Variable> originalVariables;
    private final Formula formula;

    /**
     * Constructs a new DNNF.
     * @param originalVariables the set of original variables
     * @param dnnf              the formula of the DNNF
     */
    public Dnnf(final SortedSet<Variable> originalVariables, final Formula dnnf) {
        this.originalVariables = originalVariables;
        formula = dnnf;
    }

    /**
     * Executes a given DNNF function on this DNNF.
     * @param function the function
     * @param <RESULT> the result type
     * @return the result of the function application
     */
    public <RESULT> RESULT execute(final DnnfFunction<RESULT> function) {
        return function.apply(originalVariables, formula);
    }

    /**
     * Returns the formula of the DNNF.
     * @return the formula
     */
    public Formula formula() {
        return formula;
    }

    /**
     * Returns the original variables of the formula
     * @return the original variables
     */
    public SortedSet<Variable> getOriginalVariables() {
        return originalVariables;
    }
}
