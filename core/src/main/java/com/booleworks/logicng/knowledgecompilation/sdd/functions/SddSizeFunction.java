package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddSize;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

public class SddSizeFunction implements SddFunction<Long> {

    @Override
    public LngResult<Long> execute(final SddNode node, final ComputationHandler handler) {
        return LngResult.of(SddSize.size(node));
    }
}
