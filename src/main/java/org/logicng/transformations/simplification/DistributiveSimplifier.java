// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.simplification;

import static org.logicng.formulas.FType.dual;

import org.logicng.formulas.Equivalence;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Implication;
import org.logicng.formulas.Not;
import org.logicng.formulas.cache.TransformationCacheEntry;
import org.logicng.transformations.StatelessFormulaTransformation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A formula transformation which performs simplifications by applying the distributive laws.
 * @version 3.0.0
 * @since 1.3
 */
public final class DistributiveSimplifier extends StatelessFormulaTransformation {

    private final boolean useCache;

    public DistributiveSimplifier(final FormulaFactory f) {
        this(f, true);
    }

    public DistributiveSimplifier(final FormulaFactory f, final boolean useCache) {
        super(f);
        this.useCache = useCache;
    }

    @Override
    public Formula apply(final Formula formula) {
        final Formula result;
        switch (formula.type()) {
            case FALSE:
            case TRUE:
            case LITERAL:
            case PBC:
            case PREDICATE:
                result = formula;
                break;
            case EQUIV:
                final Equivalence equiv = (Equivalence) formula;
                result = f.equivalence(apply(equiv.left()), apply(equiv.right()));
                break;
            case IMPL:
                final Implication impl = (Implication) formula;
                result = f.implication(apply(impl.left()), apply(impl.right()));
                break;
            case NOT:
                final Not not = (Not) formula;
                result = f.not(apply(not.operand()));
                break;
            case OR:
            case AND:
                result = distributeNAry(formula);
                break;
            default:
                throw new IllegalStateException("Unknown formula type: " + formula.type());
        }
        if (useCache) {
            formula.setTransformationCacheEntry(TransformationCacheEntry.DISTRIBUTIVE_SIMPLIFICATION, result);
        }
        return result;
    }

    private Formula distributeNAry(final Formula formula) {
        final Formula result;
        final FType outerType = formula.type();
        final FType innerType = dual(outerType);
        final Set<Formula> operands = new LinkedHashSet<>();
        for (final Formula op : formula) {
            operands.add(apply(op));
        }
        final Map<Formula, Set<Formula>> part2Operands = new LinkedHashMap<>();
        Formula mostCommon = null;
        int mostCommonAmount = 0;
        for (final Formula op : operands) {
            if (op.type() == innerType) {
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
