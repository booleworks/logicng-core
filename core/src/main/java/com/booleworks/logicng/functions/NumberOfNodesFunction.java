// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.NAryOperator;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.formulas.cache.FunctionCacheEntry;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;

import java.util.Map;

/**
 * A function that computes the number of nodes of a given formula.
 * @version 3.0.0
 * @since 2.2.0
 */
public class NumberOfNodesFunction extends CacheableFormulaFunction<Long> {

    /**
     * Constructs a new function. For a caching formula factory, the cache of
     * the factory will be used, for a non-caching formula factory no cache will
     * be used.
     * @param f the formula factory to generate new formulas
     */
    public NumberOfNodesFunction(final FormulaFactory f) {
        super(f, FunctionCacheEntry.NUMBER_OF_NODES);
    }

    /**
     * Constructs a new function. For all factory type the provided cache will
     * be used. If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public NumberOfNodesFunction(final FormulaFactory f, final Map<Formula, Long> cache) {
        super(f, cache);
    }

    @Override
    public LngResult<Long> apply(final Formula formula, final ComputationHandler handler) {
        final Long cached = lookupCache(formula);
        if (cached != null) {
            return LngResult.of(cached);
        }
        long result;
        switch (formula.getType()) {
            case FALSE:
            case TRUE:
            case LITERAL:
                result = 1L;
                break;
            case NOT:
                result = apply(((Not) formula).getOperand()) + 1L;
                break;
            case IMPL:
            case EQUIV:
                final BinaryOperator binary = (BinaryOperator) formula;
                result = apply(binary.getLeft()) + apply(binary.getRight()) + 1L;
                break;
            case OR:
            case AND:
                final NAryOperator nary = (NAryOperator) formula;
                result = 1L;
                for (final Formula op : nary) {
                    result += apply(op);
                }
                break;
            case PBC:
                final PbConstraint pbc = (PbConstraint) formula;
                result = 1L + pbc.getOperands().size();
                break;
            case PREDICATE:
                result = formula.numberOfNodes(f);
                break;
            default:
                throw new IllegalStateException("Unknown formula type " + formula.getType());
        }
        setCache(formula, result);
        return LngResult.of(result);
    }
}
