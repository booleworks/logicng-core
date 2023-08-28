// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.functions;

import static org.logicng.formulas.cache.FunctionCacheEntry.VARIABLES;

import org.logicng.formulas.BinaryOperator;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.NAryOperator;
import org.logicng.formulas.Not;
import org.logicng.formulas.PBConstraint;
import org.logicng.formulas.Variable;
import org.logicng.util.FormulaHelper;

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
     * Constructs a new function.  For a caching formula factory, the cache of the factory will be used,
     * for a non-caching formula factory no cache will be used.
     * @param f the formula factory to generate new formulas
     */
    public VariablesFunction(final FormulaFactory f) {
        super(f, VARIABLES);
    }

    /**
     * Constructs a new function.  For all factory type the provided cache will be used.
     * If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public VariablesFunction(final FormulaFactory f, final Map<Formula, SortedSet<Variable>> cache) {
        super(f, cache);
    }

    @Override
    public SortedSet<Variable> apply(final Formula formula) {
        final SortedSet<Variable> cached = lookupCache(formula);
        if (cached != null) {
            return cached;
        }
        SortedSet<Variable> result = new TreeSet<>();
        switch (formula.type()) {
            case FALSE:
            case TRUE:
                result = new TreeSet<>();
                break;
            case LITERAL:
                final Literal lit = (Literal) formula;
                result.add(lit.variable());
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
                result = FormulaHelper.variables(pbc.literals(f));
                break;
            default:
                throw new IllegalStateException("Unknown formula type " + formula.type());
        }
        result = Collections.unmodifiableSortedSet(result);
        setCache(formula, result);
        return result;
    }
}
