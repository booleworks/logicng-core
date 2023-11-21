// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;

/**
 * Interface for a handler for the BDD factory.
 * @version 2.1.0
 * @since 1.6.2
 */
public interface BDDHandler extends Handler {

    /**
     * This method is called every time a new reference is added, i.e. the method
     * {@link BDDKernel#addRef(int, BDDHandler)} is called.
     * @return {@code true} if the BDD generation should be continued, otherwise {@code false}
     */
    default boolean newRefAdded() {
        return true;
    }
}
