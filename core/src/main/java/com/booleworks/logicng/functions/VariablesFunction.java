// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.NAryOperator;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.formulas.cache.FunctionCacheEntry;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.util.FormulaHelper;

import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A function that computes all variables occurring in a given formula.
 * @version 3.0.0
 * @since 2.2.0
 */
public class VariablesFunction extends CacheableFormulaFunction<SortedSet<Variable>> {

    /**
     * Constructs a new function. For a caching formula factory, the cache of
     * the factory will be used, for a non-caching formula factory no cache will
     * be used.
     * @param f the formula factory to generate new formulas
     */
    public VariablesFunction(final FormulaFactory f) {
        super(f, FunctionCacheEntry.VARIABLES);
    }

    /**
     * Constructs a new function. For all factory type the provided cache will
     * be used. If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public VariablesFunction(final FormulaFactory f, final Map<Formula, SortedSet<Variable>> cache) {
        super(f, cache);
    }

    @Override
    public LngResult<SortedSet<Variable>> apply(final Formula formula, final ComputationHandler handler) {
        final SortedSet<Variable> cached = lookupCache(formula);
        if (cached != null) {
            return LngResult.of(cached);
        }
        SortedSet<Variable> result = new TreeSet<>();
        switch (formula.getType()) {
            case FALSE:
            case TRUE:
            case PREDICATE:
                result = new TreeSet<>();
                break;
            case LITERAL:
                final Literal lit = (Literal) formula;
                result.add(lit.variable());
                break;
            case NOT:
                final Not not = (Not) formula;
                result = apply(not.getOperand());
                break;
            case IMPL:
            case EQUIV:
                final BinaryOperator binary = (BinaryOperator) formula;
                result.addAll(apply(binary.getLeft()));
                result.addAll(apply(binary.getRight()));
                break;
            case OR:
            case AND:
                final NAryOperator nary = (NAryOperator) formula;
                for (final Formula op : nary) {
                    result.addAll(apply(op));
                }
                break;
            case PBC:
                final PbConstraint pbc = (PbConstraint) formula;
                result = FormulaHelper.variables(f, pbc.literals(f));
                break;
            default:
                throw new IllegalStateException("Unknown formula type " + formula.getType());
        }
        result = Collections.unmodifiableSortedSet(result);
        setCache(formula, result);
        return LngResult.of(result);
    }
}
