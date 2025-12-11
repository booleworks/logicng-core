// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * A compact representation of a model that stores dont-care variables
 * separately.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class CompactModel {
    private final List<Literal> literals;
    private final List<Variable> dontCareVariables;

    /**
     * Construct a new model.
     * @param literals          the literals of the model
     * @param dontCareVariables the dont-care variables of the model
     */
    public CompactModel(final List<Literal> literals, final List<Variable> dontCareVariables) {
        this.literals = literals;
        this.dontCareVariables = dontCareVariables;
    }

    /**
     * Copy constructor
     * @param model existing model
     */
    public CompactModel(final CompactModel model) {
        this.literals = new ArrayList<>(model.literals);
        this.dontCareVariables = new ArrayList<>(model.dontCareVariables);
    }

    /**
     * Returns the list of literals.
     * @return the list of literals
     */
    public List<Literal> getLiterals() {
        return literals;
    }

    /**
     * Returns the list of dont-care variables.
     * @return the list of dont-care variables
     */
    public List<Variable> getDontCareVariables() {
        return dontCareVariables;
    }
}
