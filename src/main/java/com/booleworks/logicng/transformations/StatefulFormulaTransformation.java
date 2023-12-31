// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaTransformation;

/**
 * A stateful formula transformation does hold an internal mutable state. So you
 * must think about, in which situations a transformation can and should be
 * shared for different formulas. One example for such a transformation is a CNF
 * transformation with Tseitin or Plaisted-Greenbaum, which re-use newly
 * introduced variables. You *can* share such a transformation between different
 * calls with the effect, that introduced variables will be re-used for equal
 * sub-formulas. This works for both caching and non-caching formula factories.
 * For a caching formula factory this mutable state often can be deduced from
 * the factory itself.
 * @param <S> the type of the mutable state
 * @version 3.0.0
 * @since 3.0.0
 */
public abstract class StatefulFormulaTransformation<S> implements FormulaTransformation {
    protected final FormulaFactory f;
    protected final S state;

    /**
     * Constructor.
     * @param f the formula factory to generate new formulas
     **/
    protected StatefulFormulaTransformation(final FormulaFactory f) {
        this.f = f;
        state = inititialState();
    }

    /**
     * Constructor.
     * @param f     the formula factory to generate new formulas
     * @param state the internal state
     **/
    protected StatefulFormulaTransformation(final FormulaFactory f, final S state) {
        this.f = f;
        this.state = state;
    }

    /**
     * Returns the state of this transformation.
     * @return the state
     */
    public S getState() {
        return state;
    }

    protected abstract S inititialState();
}
