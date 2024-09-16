// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.NAryOperator;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.transformations.StatelessFormulaTransformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Negation simplifier.
 * <p>
 * Reduces the number of negations for a formula in a greedy manner. The
 * criterion for the simplification is the length of the resulting formula.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class NegationSimplifier extends StatelessFormulaTransformation {

    public NegationSimplifier(final FormulaFactory f) {
        super(f);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        final Formula nnf = formula.nnf(f);
        if (nnf.isAtomicFormula()) {
            return LngResult.of(getSmallestFormula(true, formula, nnf));
        }
        final MinimizationResult result = minimize(nnf, true);
        return LngResult.of(getSmallestFormula(true, formula, nnf, result.positiveResult));
    }

    private MinimizationResult minimize(final Formula formula, final boolean topLevel) {
        switch (formula.getType()) {
            case LITERAL:
                final Literal lit = (Literal) formula;
                return new MinimizationResult(lit, lit.negate(f));
            case OR:
            case AND:
                final NAryOperator nary = (NAryOperator) formula;
                final List<MinimizationResult> opResults = new ArrayList<>(nary.numberOfOperands());
                for (final Formula op : formula) {
                    opResults.add(minimize(op, false));
                }
                final List<Formula> positiveOpResults = new ArrayList<>(opResults.size());
                final List<Formula> negativeOpResults = new ArrayList<>(opResults.size());
                for (final MinimizationResult result : opResults) {
                    positiveOpResults.add(result.positiveResult);
                    negativeOpResults.add(result.negativeResult);
                }
                final Formula smallestPositive =
                        findSmallestPositive(formula.getType(), positiveOpResults, negativeOpResults, topLevel, f);
                final Formula smallestNegative =
                        findSmallestNegative(formula.getType(), negativeOpResults, smallestPositive, topLevel, f);
                return new MinimizationResult(smallestPositive, smallestNegative);
            case FALSE:
            case TRUE:
            case NOT:
            case EQUIV:
            case IMPL:
            case PBC:
            case PREDICATE:
                throw new IllegalStateException("Unexpected LogicNG formula type: " + formula.getType());
            default:
                throw new IllegalArgumentException("Unknown LogicNG formula type: " + formula.getType());
        }
    }

    private Formula findSmallestPositive(final FType type, final List<Formula> positiveOpResults,
                                         final List<Formula> negativeOpResults, final boolean topLevel,
                                         final FormulaFactory f) {
        final Formula allPositive = f.naryOperator(type, positiveOpResults);
        final List<Formula> smallerPositiveOps = new ArrayList<>();
        final List<Formula> smallerNegativeOps = new ArrayList<>();
        for (int i = 0; i < positiveOpResults.size(); i++) {
            final Formula positiveOp = positiveOpResults.get(i);
            final Formula negativeOp = negativeOpResults.get(i);
            if (formattedLength(positiveOp, false) < formattedLength(negativeOp, false)) {
                smallerPositiveOps.add(positiveOp);
            } else {
                smallerNegativeOps.add(negativeOp);
            }
        }
        final Formula partialNegative = f.naryOperator(type, f.naryOperator(type, smallerPositiveOps),
                f.not(f.naryOperator(FType.dual(type), smallerNegativeOps)));
        return getSmallestFormula(topLevel, allPositive, partialNegative);
    }

    private Formula findSmallestNegative(final FType type, final List<Formula> negativeOpResults,
                                         final Formula smallestPositive, final boolean topLevel,
                                         final FormulaFactory f) {
        final Formula negation = f.not(smallestPositive);
        final Formula flipped = f.naryOperator(FType.dual(type), negativeOpResults);
        return getSmallestFormula(topLevel, negation, flipped);
    }

    private Formula getSmallestFormula(final boolean topLevel, final Formula... formulas) {
        assert formulas.length != 0;
        return Arrays.stream(formulas).min(Comparator.comparingInt(formula -> formattedLength(formula, topLevel)))
                .get();
    }

    private int formattedLength(final Formula formula, final boolean topLevel) {
        final int length = formula.toString().length();
        if (!topLevel && formula.getType() == FType.OR) {
            return length + 2;
        } else {
            return length;
        }
    }

    private static class MinimizationResult {
        private final Formula positiveResult;
        private final Formula negativeResult;

        public MinimizationResult(final Formula positiveResult, final Formula negativeResult) {
            this.positiveResult = positiveResult;
            this.negativeResult = negativeResult;
        }
    }
}
