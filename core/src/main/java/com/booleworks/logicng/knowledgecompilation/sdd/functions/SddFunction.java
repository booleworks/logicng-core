package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;

public interface SddFunction<RESULT> {
    LngResult<RESULT> apply(final Sdd sf, final ComputationHandler handler);

    default RESULT apply(final Sdd sf) {
        return apply(sf, NopHandler.get()).getResult();
    }
}
