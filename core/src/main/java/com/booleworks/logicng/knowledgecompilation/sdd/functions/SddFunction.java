package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;

public interface SddFunction<RESULT> {
    LngResult<RESULT> apply(final SddFactory sf, final ComputationHandler handler);

    default RESULT apply(final SddFactory sf) {
        return apply(sf, NopHandler.get()).getResult();
    }
}
