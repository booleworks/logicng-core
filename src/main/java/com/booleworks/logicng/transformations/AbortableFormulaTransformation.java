// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.Handler;

/**
 * An abortable formula transformation is a stateless transformation which can
 * be aborted via a handler. So it does still not mutate any internal state, but
 * *can* change the handler's state. Therefore, an abortable transformation with
 * a given handler is *NOT* thread-safe.
 * @version 3.0.0
 * @since 3.0.0
 */
public abstract class AbortableFormulaTransformation<H extends Handler> extends StatelessFormulaTransformation {
    protected final H handler;

    /**
     * Constructor.
     * @param f       the formula factory to generate new formulas
     * @param handler the handler for the transformation
     **/
    protected AbortableFormulaTransformation(final FormulaFactory f, final H handler) {
        super(f);
        this.handler = handler;
    }
}
