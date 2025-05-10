package com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;

public interface VTreeGenerator {
    default VTree generate(final Sdd sf) {
        return generate(sf, NopHandler.get()).getResult();
    }

    LngResult<VTree> generate(final Sdd sf, ComputationHandler handler);
}
