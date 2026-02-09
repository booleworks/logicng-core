// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import com.booleworks.logicng.formulas.Equivalence;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Implication;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.cache.TransformationCacheEntry;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.transformations.CacheableFormulaTransformation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A formula transformation which performs simplifications by applying the
 * distributive laws.
 * @version 3.0.0
 * @since 1.3
 */
public class DistributiveSimplifier extends CacheableFormulaTransformation {

    /**
     * Constructs a new transformation. For a caching formula factory, the cache
     * of the factory will be used, for a non-caching formula factory no cache
     * will be used.
     * @param f the formula factory to generate new formulas
     */
    public DistributiveSimplifier(final FormulaFactory f) {
        super(f, TransformationCacheEntry.DISTRIBUTIVE_SIMPLIFICATION);
    }

    /**
     * Constructs a new transformation. For all factory type the provided cache
     * will be used. If it is null, no cache will be used.
     * @param f     the formula factory to generate new formulas
     * @param cache the cache to use for the transformation
     */
    public DistributiveSimplifier(final FormulaFactory f, final Map<Formula, Formula> cache) {
        super(f, cache);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        Formula result = lookupCache(formula);
        if (result != null) {
            return LngResult.of(result);
        }
        switch (formula.getType()) {
            case FALSE:
            case TRUE:
            case LITERAL:
            case PBC:
            case PREDICATE:
                result = formula;
                break;
            case EQUIV:
                final Equivalence equiv = (Equivalence) formula;
                result = f.equivalence(apply(equiv.getLeft()), apply(equiv.getRight()));
                break;
            case IMPL:
                final Implication impl = (Implication) formula;
                result = f.implication(apply(impl.getLeft()), apply(impl.getRight()));
                break;
            case NOT:
                final Not not = (Not) formula;
                result = f.not(apply(not.getOperand()));
                break;
            case OR:
            case AND:
                result = distributeNAry(formula);
                break;
            default:
                throw new IllegalStateException("Unknown formula type: " + formula.getType());
        }
        setCache(formula, result);
        return LngResult.of(result);
    }

    protected Formula distributeNAry(final Formula formula) {
        final Formula result;
        final FType outerType = formula.getType();
        final FType innerType = FType.dual(outerType);
        final Set<Formula> operands = new LinkedHashSet<>();
        for (final Formula op : formula) {
            operands.add(apply(op));
        }
        final Map<Formula, Set<Formula>> part2Operands = new LinkedHashMap<>();
        Formula mostCommon = null;
        int mostCommonAmount = 0;
        for (final Formula op : operands) {
            if (op.getType() == innerType) {
                for (final Formula part : op) {
                    final Set<Formula> partOperands = part2Operands.computeIfAbsent(part, k -> new LinkedHashSet<>());
                    partOperands.add(op);
                    if (partOperands.size() > mostCommonAmount) {
                        mostCommon = part;
                        mostCommonAmount = partOperands.size();
                    }
                }
            }
        }
        if (mostCommon == null || mostCommonAmount == 1) {
            result = f.naryOperator(outerType, operands);
            return result;
        }
        operands.removeAll(part2Operands.get(mostCommon));
        final Set<Formula> relevantFormulas = new LinkedHashSet<>();
        for (final Formula preRelevantFormula : part2Operands.get(mostCommon)) {
            final Set<Formula> relevantParts = new LinkedHashSet<>();
            for (final Formula part : preRelevantFormula) {
                if (!part.equals(mostCommon)) {
                    relevantParts.add(part);
                }
            }
            relevantFormulas.add(f.naryOperator(innerType, relevantParts));
        }
        operands.add(f.naryOperator(innerType, mostCommon, f.naryOperator(outerType, relevantFormulas)));
        result = f.naryOperator(outerType, operands);
        return result;
    }
}
