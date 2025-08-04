package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddSize;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

/**
 * Function for computing the size of the SDD node, i.e., the number of unique
 * children nodes plus itself.
 * <p>
 * This is a wrapper for {@link SddSize#size(SddNode)}.  In performance critical
 * situation it can be beneficial to use the raw function, as it does not wrap
 * the result in an {@code LngResult}.
 * @version 3.0.0
 * @since 3.0.0
 */
public class SddSizeFunction implements SddFunction<Long> {
    private final static SddSizeFunction INSTANCE = new SddSizeFunction();

    private SddSizeFunction() {
    }

    /**
     * Returns the singleton instance of this class.
     * @return the singleton instance of this class
     */
    public SddSizeFunction get() {
        return INSTANCE;
    }

    @Override
    public LngResult<Long> execute(final SddNode node, final ComputationHandler handler) {
        return LngResult.of(SddSize.size(node));
    }
}
