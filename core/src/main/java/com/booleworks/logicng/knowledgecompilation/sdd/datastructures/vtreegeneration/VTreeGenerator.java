// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtreegeneration;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;

/**
 * An interface for vtree generators.
 * @version 3.0.0
 * @since 3.0.0
 */
public interface VTreeGenerator {
    /**
     * Generates a vtree using the given SDD container.
     * @param builder the vtree builder
     * @return the generated vtree
     */
    default VTree generate(final VTreeRoot.Builder builder) {
        return generate(builder, NopHandler.get()).getResult();
    }

    /**
     * Generates a vtree using the given SDD container.
     * @param builder the vtree builder
     * @param handler computation handler
     * @return the generated vtree or the canceling cause if the computation was
     * aborted by the handler
     */
    LngResult<VTree> generate(final VTreeRoot.Builder builder, ComputationHandler handler);
}
