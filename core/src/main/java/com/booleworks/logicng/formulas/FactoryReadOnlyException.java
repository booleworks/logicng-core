//  SPDX-License-Identifier: Apache-2.0 and MIT
//  Copyright 2015-2023 Christoph Zengler
//  Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

/**
 * A runtime exception that is thrown if it is tried to alter a formula factory
 * while it is in READ-ONLY mode.
 */
public final class FactoryReadOnlyException extends RuntimeException {

    /**
     * Constructs a new exception.
     */
    public FactoryReadOnlyException() {
        super("Tried to alter a formula factory in read-only mode.");
    }
}
