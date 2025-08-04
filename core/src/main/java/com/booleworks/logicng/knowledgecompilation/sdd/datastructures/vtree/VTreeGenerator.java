package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;

/**
 * An interface for vtree generators.
 * @version 3.0.0
 * @since 3.0.0
 */
public interface VTreeGenerator {
    /**
     * Generates a vtree using the given SDD container.
     * @param sdd the SDD container
     * @return the generated vtree
     */
    default VTree generate(final Sdd sdd) {
        return generate(sdd, NopHandler.get()).getResult();
    }

    /**
     * Generates a vtree using the given SDD container.
     * @param sdd     the SDD container
     * @param handler computation handler
     * @return the generated vtree or the canceling cause if the computation was
     * aborted by the handler
     */
    LngResult<VTree> generate(final Sdd sdd, ComputationHandler handler);
}
