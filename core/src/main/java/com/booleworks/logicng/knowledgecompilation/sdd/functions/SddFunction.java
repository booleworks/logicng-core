package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;

public interface SddFunction<RESULT> {
    RESULT apply(final SddFactory sf);
}
