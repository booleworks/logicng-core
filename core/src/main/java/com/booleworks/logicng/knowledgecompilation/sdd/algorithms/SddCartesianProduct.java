package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Function related to the cartesian product of SDD nodes and elements.
 * @version 3.0.0
 * @since 3.0.0
 */
final class SddCartesianProduct {
    private SddCartesianProduct() {
    }

    /**
     * Computes a (compressed) cartesian product of a collection of partitions.
     * <ul>
     * <li><i>Preconditions:</i> Each sets need to be a partition</li>
     * </ul>
     * @param sets     the input partitions
     * @param compress whether to compress the result
     * @param sdd      the SDD container
     * @param handler  the computation handler
     * @return a partition that is the cartesian product of {@code sets}
     */
    public static LngResult<ArrayList<SddElement>> cartesianProduct(final Collection<ArrayList<SddElement>> sets,
                                                                    final boolean compress, final Sdd sdd,
                                                                    final ComputationHandler handler) {
        ArrayList<SddElement> res = new ArrayList<>();
        res.add(new SddElement(sdd.verum(), sdd.falsum()));
        for (final ArrayList<SddElement> set : sets) {
            final LngResult<ArrayList<SddElement>> resResult = cartesianProduct(res, set, compress, sdd, handler);
            if (!resResult.isSuccess()) {
                return resResult;
            }
            res = resResult.getResult();
        }
        return LngResult.of(res);
    }

    private static LngResult<ArrayList<SddElement>> cartesianProduct(final ArrayList<SddElement> left,
                                                                     final ArrayList<SddElement> right,
                                                                     final boolean compress, final Sdd sdd,
                                                                     final ComputationHandler handler) {
        final LngResult<ArrayList<SddElement>> product =
                SddMultiply.multiplyDecompositions(left, right, SddApplyOperation.DISJUNCTION, sdd, handler);
        if (!product.isSuccess()) {
            return product;
        }
        if (compress) {
            return SddCompression.compress(product.getResult(), sdd, handler);
        }
        return product;
    }
}
