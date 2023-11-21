// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaTransformation;

/**
 * A stateless formula transformation does not hold an internal mutable state.  It can still hold in internal
 * state which is read-only (e.g. a simplifier function can hold its rating function, or a substitution can
 * hold its substitution map) but this state is never changed by the transformation.  So once created,
 * such a transformation can be re-used or even be used in a multi-threaded context.
 * @version 3.0.0
 * @since 3.0.0
 */
public abstract class StatelessFormulaTransformation implements FormulaTransformation {
    protected final FormulaFactory f;

    /**
     * Constructor.
     * @param f the formula factory to generate new formulas
     **/
    protected StatelessFormulaTransformation(final FormulaFactory f) {
        this.f = f;
    }
}
