// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.NAryOperator;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.transformations.StatelessFormulaTransformation;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factor out simplification.
 * <p>
 * Reduces the length for a formula by applying factor out operations.
 * @version 3.0.0
 * @since 2.0.0
 */
public class FactorOutSimplifier extends StatelessFormulaTransformation {

    protected final RatingFunction<? extends Number> ratingFunction;

    /**
     * Constructs a new factor out simplification with the default rating
     * function {@link DefaultRatingFunction}.
     * @param f the formula factory to generate new formulas
     */
    public FactorOutSimplifier(final FormulaFactory f) {
        this(f, new DefaultRatingFunction());
    }

    /**
     * Constructs a new factor out simplification with the given rating
     * function.
     * @param f              the formula factory to generate new formulas
     * @param ratingFunction the rating function
     */
    public FactorOutSimplifier(final FormulaFactory f, final RatingFunction<? extends Number> ratingFunction) {
        super(f);
        this.ratingFunction = ratingFunction;
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        Formula last;
        Formula simplified = formula;
        do {
            last = simplified;
            simplified = applyRec(last);
        } while (!simplified.equals(last));
        return LngResult.of(simplified);
    }

    protected Formula applyRec(final Formula formula) {
        switch (formula.getType()) {
            case OR:
            case AND:
                final List<Formula> newOps = new ArrayList<>();
                for (final Formula op : formula) {
                    newOps.add(apply(op));
                }
                final Formula newFormula = f.naryOperator(formula.getType(), newOps);
                return newFormula instanceof NAryOperator ? simplify((NAryOperator) newFormula) : newFormula;
            case NOT:
                return apply(((Not) formula).getOperand()).negate(f);
            case FALSE:
            case TRUE:
            case LITERAL:
            case IMPL:
            case EQUIV:
            case PBC:
            case PREDICATE:
                return formula;
            default:
                throw new IllegalStateException("Unknown formula type: " + formula.getType());
        }
    }

    protected Formula simplify(final NAryOperator formula) {
        final Formula simplified = factorOut(formula);
        return simplified == null ||
                       ratingFunction.apply(formula).doubleValue() <= ratingFunction.apply(simplified).doubleValue()
               ? formula
               : simplified;
    }

    protected Formula factorOut(final NAryOperator formula) {
        final Formula factorOutFormula = computeMaxOccurringSubformula(formula);
        if (factorOutFormula == null) {
            return null;
        }
        final FType type = formula.getType();
        final List<Formula> formulasWithRemoved = new ArrayList<>();
        final List<Formula> unchangedFormulas = new ArrayList<>();
        for (final Formula operand : formula) {
            if (operand.getType() == FType.LITERAL) {
                if (operand.equals(factorOutFormula)) {
                    formulasWithRemoved.add(f.constant(type == FType.OR));
                } else {
                    unchangedFormulas.add(operand);
                }
            } else if (operand.getType() == FType.AND || operand.getType() == FType.OR) {
                boolean removed = false;
                final List<Formula> newOps = new ArrayList<>();
                for (final Formula op : operand) {
                    if (!op.equals(factorOutFormula)) {
                        newOps.add(op);
                    } else {
                        removed = true;
                    }
                }
                (removed ? formulasWithRemoved : unchangedFormulas).add(f.naryOperator(operand.getType(), newOps));
            } else {
                unchangedFormulas.add(operand);
            }
        }
        return f.naryOperator(type, f.naryOperator(type, unchangedFormulas),
                f.naryOperator(FType.dual(type), factorOutFormula, f.naryOperator(type, formulasWithRemoved)));
    }

    protected static Formula computeMaxOccurringSubformula(final NAryOperator formula) {
        final Map<Formula, Integer> formulaCounts = new HashMap<>();
        for (final Formula operand : formula) {
            if (operand.getType() == FType.LITERAL) {
                formulaCounts.merge(operand, 1, Integer::sum);
            } else if (operand.getType() == FType.AND || operand.getType() == FType.OR) {
                for (final Formula subOperand : operand) {
                    formulaCounts.merge(subOperand, 1, Integer::sum);
                }
            }
        }
        final Pair<Formula, Integer> max = formulaCounts.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(e -> new Pair<>(e.getKey(), e.getValue()))
                .orElse(new Pair<>(null, 0));
        return max.getSecond() < 2 ? null : max.getFirst();
    }
}
