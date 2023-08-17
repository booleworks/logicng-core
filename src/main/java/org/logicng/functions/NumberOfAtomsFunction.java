// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.functions;

import static org.logicng.formulas.cache.FunctionCacheEntry.NUMBER_OF_ATOMS;

import org.logicng.formulas.BinaryOperator;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFunction;
import org.logicng.formulas.NAryOperator;
import org.logicng.formulas.Not;

/**
 * A function that computes the number of atoms occurring in a given formula.
 * @version 3.0.0
 * @since 2.2.0
 */
public class NumberOfAtomsFunction implements FormulaFunction<Long> {

    private static final NumberOfAtomsFunction CACHING_INSTANCE = new NumberOfAtomsFunction(true);
    private static final NumberOfAtomsFunction NON_CACHING_INSTANCE = new NumberOfAtomsFunction(false);

    private final boolean useCache;

    private NumberOfAtomsFunction(final boolean useCache) {
        this.useCache = useCache;
    }

    public static NumberOfAtomsFunction get(final boolean useCache) {
        return useCache ? CACHING_INSTANCE : NON_CACHING_INSTANCE;
    }

    @Override
    public Long apply(final Formula formula) {
        final Object cached = formula.functionCacheEntry(NUMBER_OF_ATOMS);
        if (cached != null) {
            return (Long) cached;
        }
        long result = 0L;
        switch (formula.type()) {
            case FALSE:
            case TRUE:
            case LITERAL:
            case PREDICATE:
            case PBC:
                result = 1L;
                break;
            case NOT:
                result = apply(((Not) formula).operand());
                break;
            case IMPL:
            case EQUIV:
                final BinaryOperator binary = (BinaryOperator) formula;
                result = apply(binary.left()) + apply(binary.right());
                break;
            case OR:
            case AND:
                final NAryOperator nary = (NAryOperator) formula;
                for (final Formula op : nary) {
                    result += apply(op);
                }
                break;
            default:
                throw new IllegalStateException("Unknown formula type " + formula.type());
        }
        if (useCache) {
            formula.setFunctionCacheEntry(NUMBER_OF_ATOMS, result);
        }
        return result;
    }
}
