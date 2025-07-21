package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;

import java.util.ArrayList;
import java.util.Collection;

public class SddCartesianProduct {
    private SddCartesianProduct() {
    }

    public static LngResult<ArrayList<SddElement>> cartesianProduct(final Collection<ArrayList<SddElement>> sets,
                                                                    final boolean compress, final Sdd sf,
                                                                    final ComputationHandler handler) {
        ArrayList<SddElement> res = new ArrayList<>();
        res.add(new SddElement(sf.verum(), sf.falsum()));
        for (final ArrayList<SddElement> set : sets) {
            final LngResult<ArrayList<SddElement>> resResult = cartesianProduct(res, set, compress, sf, handler);
            if (!resResult.isSuccess()) {
                return resResult;
            }
            res = resResult.getResult();
        }
        return LngResult.of(res);
    }

    private static LngResult<ArrayList<SddElement>> cartesianProduct(final ArrayList<SddElement> left,
                                                                     final ArrayList<SddElement> right,
                                                                     final boolean compress, final Sdd sf,
                                                                     final ComputationHandler handler) {
        final LngResult<ArrayList<SddElement>> product =
                SddMultiply.multiplyDecompositions(left, right, SddApplyOperation.DISJUNCTION, sf, handler);
        if (!product.isSuccess()) {
            return product;
        }
        if (compress) {
            return SddCompression.compress(product.getResult(), sf, handler);
        }
        return product;
    }
}
