// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.functions;

import static org.logicng.formulas.cache.FunctionCacheEntry.LITERALS;

import org.logicng.formulas.BinaryOperator;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFunction;
import org.logicng.formulas.Literal;
import org.logicng.formulas.NAryOperator;
import org.logicng.formulas.Not;
import org.logicng.formulas.PBConstraint;
import org.logicng.util.FormulaHelper;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A function that computes all literals occurring in a given formula.
 * @version 3.0.0
 * @since 2.2.0
 */
public class LiteralsFunction implements FormulaFunction<SortedSet<Literal>> {

    private static final LiteralsFunction CACHING_INSTANCE = new LiteralsFunction(true);
    private static final LiteralsFunction NON_CACHING_INSTANCE = new LiteralsFunction(false);

    private final boolean useCache;

    private LiteralsFunction(final boolean useCache) {
        this.useCache = useCache;
    }

    public static LiteralsFunction get(final boolean useCache) {
        return useCache ? CACHING_INSTANCE : NON_CACHING_INSTANCE;
    }

    @Override
    public SortedSet<Literal> apply(final Formula formula) {
        final Object cached = formula.functionCacheEntry(LITERALS);
        if (cached != null) {
            return (SortedSet<Literal>) cached;
        }
        SortedSet<Literal> result = new TreeSet<>();
        switch (formula.type()) {
            case FALSE:
            case TRUE:
            case PREDICATE:
                result = new TreeSet<>();
                break;
            case LITERAL:
                final Literal lit = (Literal) formula;
                result.add(lit);
                break;
            case NOT:
                final Not not = (Not) formula;
                result = apply(not.operand());
                break;
            case IMPL:
            case EQUIV:
                final BinaryOperator binary = (BinaryOperator) formula;
                result.addAll(apply(binary.left()));
                result.addAll(apply(binary.right()));
                break;
            case OR:
            case AND:
                final NAryOperator nary = (NAryOperator) formula;
                for (final Formula op : nary) {
                    result.addAll(apply(op));
                }
                break;
            case PBC:
                final PBConstraint pbc = (PBConstraint) formula;
                result = FormulaHelper.literals(pbc.operands());
                break;
            default:
                throw new IllegalStateException("Unknown formula type " + formula.type());
        }
        result = Collections.unmodifiableSortedSet(result);
        if (useCache) {
            formula.setFunctionCacheEntry(LITERALS, result);
        }
        return result;
    }
}
