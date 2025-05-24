package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;

import java.util.Collection;
import java.util.TreeSet;

public class SddCartesianProduct {
    private SddCartesianProduct() {
    }

    public static LngResult<TreeSet<SddElement>> cartesianProduct(final Collection<TreeSet<SddElement>> sets,
                                                                  final boolean compress, final Sdd sf,
                                                                  final ComputationHandler handler) {
        TreeSet<SddElement> res = new TreeSet<>();
        Util.pushNewElement(sf.verum(), sf.falsum(), res);
        for (final TreeSet<SddElement> set : sets) {
            final LngResult<TreeSet<SddElement>> resResult = cartesianProduct(res, set, compress, sf, handler);
            if (!resResult.isSuccess()) {
                return resResult;
            }
            res = resResult.getResult();
        }
        return LngResult.of(res);
    }

    private static LngResult<TreeSet<SddElement>> cartesianProduct(final TreeSet<SddElement> left,
                                                                   final TreeSet<SddElement> right,
                                                                   final boolean compress, final Sdd sf,
                                                                   final ComputationHandler handler) {
        final LngResult<TreeSet<SddElement>> product =
                SddMultiply.multiplyDecompositions(left, right, SddApplyOperation.DISJUNCTION, sf, handler);
        if (!product.isSuccess()) {
            return product;
        }
        if (compress) {
            return Util.compress(product.getResult(), sf, handler);
        }
        return product;
    }
}
