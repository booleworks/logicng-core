package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

public interface SddFunction<RESULT> {
    LngResult<RESULT> execute(final SddNode node, final ComputationHandler handler);

    default RESULT execute(final SddNode node) {
        return execute(node, NopHandler.get()).getResult();
    }
}
