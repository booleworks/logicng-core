// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.predicates;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.Equivalence;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaPredicate;
import com.booleworks.logicng.formulas.Implication;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Checks if the given formula is evaluated to false/true for a given (partial)
 * assignment.
 * <p>
 * Note: If a partial assignment is given, the check only recognizes simple
 * unsatisfiable/tautology cases
 * <ul>
 * <li>operand of an AND/OR is false/true</li>
 * <li>all operators of an OR/AND are false/true</li>
 * <li>AND/OR has two operands with complementary negations</li>
 * </ul>
 * This evaluation differs from the standard formula evaluation
 * {@link Formula#evaluate(Assignment)} in two ways. It accepts partial
 * assignments, and it tries to avoid the generation of intermediate formula by
 * the formula factory objects in order to speed up the performance.
 * <p>
 * Example 01: When evaluation to false the formula (a | b) &amp; (~a | c) with
 * partial assignment [b -&gt; false, c -&gt; false] yields to a &amp; ~a which
 * is recognized as unsatisfiable.
 * <p>
 * Example 02: When evaluation to true the formula (a &amp; b) | (~a &amp; c)
 * with partial assignment [b -&gt; false, c -&gt; false] yields to a | ~a which
 * is recognized as a tautology.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class EvaluatesToConstantPredicate implements FormulaPredicate {

    private final FormulaFactory f;
    private final boolean evaluatesToTrue;
    private final Map<Variable, Boolean> mapping;

    /**
     * Constructs a new evaluation predicate.
     * @param f               the formula factory to generate new formulas
     * @param evaluatesToTrue {@code false} if the check aims for true,
     *                        {@code false} if the check aims for false
     * @param mapping         the (partial) assignment
     */
    public EvaluatesToConstantPredicate(final FormulaFactory f, final boolean evaluatesToTrue,
                                        final Map<Variable, Boolean> mapping) {
        this.f = f;
        this.evaluatesToTrue = evaluatesToTrue;
        this.mapping = mapping;
    }

    /**
     * Returns the (partial) assignment.
     * @return the (partial) assignment
     */
    public Map<Variable, Boolean> getMapping() {
        return Collections.unmodifiableMap(mapping);
    }

    /**
     * Checks if the formula evaluates to false (or true) when the (partial)
     * assignment is applied.
     * @param formula the formula
     */
    @Override
    public boolean test(final Formula formula) {
        return innerTest(formula, true).getType() == getConstantType(evaluatesToTrue);
    }

    /**
     * Restricts and possibly simplifies the formula by applying the (partial)
     * assignment in order to decide if the restriction yields to the specified
     * constant.
     * @param formula  the formula
     * @param topLevel indicator if the formula is the top level operator
     * @return Falsum resp. Verum if the (partial) assignment resulted not to
     * the specified constant, otherwise the restricted and simplified
     * formula
     */
    private Formula innerTest(final Formula formula, final boolean topLevel) {
        switch (formula.getType()) {
            case TRUE:
            case FALSE:
                return formula;
            case LITERAL:
                final Literal lit = (Literal) formula;
                final Boolean found = mapping.get(lit.variable());
                return found == null ? lit : f.constant(lit.getPhase() == found);
            case NOT:
                return handleNot((Not) formula, topLevel);
            case IMPL:
                return handleImplication((Implication) formula, topLevel);
            case EQUIV:
                return handleEquivalence((Equivalence) formula, topLevel);
            case OR:
                return handleOr((Or) formula, topLevel);
            case AND:
                return handleAnd((And) formula, topLevel);
            case PBC:
                return handlePbc((PbConstraint) formula);
            case PREDICATE:
                throw new UnsupportedOperationException(
                        "Cannot evaluate a formula with predicates with a boolean assignment");
            default:
                throw new IllegalArgumentException("Unknown formula type " + formula.getType());
        }
    }

    private Formula handleNot(final Not formula, final boolean topLevel) {
        final Formula opResult = innerTest(formula.getOperand(), false);
        if (topLevel && !opResult.isConstantFormula()) {
            return f.constant(!evaluatesToTrue);
        }
        return opResult.isConstantFormula() ? f.constant(isFalsum(opResult)) : f.not(opResult);
    }

    private Formula handleImplication(final Implication formula, final boolean topLevel) {
        final Formula left = formula.getLeft();
        final Formula right = formula.getRight();
        final Formula leftResult = innerTest(left, false);
        if (leftResult.isConstantFormula()) {
            if (evaluatesToTrue) {
                return isFalsum(leftResult) ? f.verum() : innerTest(right, topLevel);
            } else {
                return isVerum(leftResult) ? innerTest(right, topLevel) : f.verum();
            }
        }
        if (!evaluatesToTrue && topLevel) {
            return f.verum();
        }
        final Formula rightResult = innerTest(right, false);
        if (rightResult.isConstantFormula()) {
            return isVerum(rightResult) ? f.verum() : f.not(leftResult);
        }
        return f.implication(leftResult, rightResult);
    }

    private Formula handleEquivalence(final Equivalence formula, final boolean topLevel) {
        final Formula left = formula.getLeft();
        final Formula right = formula.getRight();
        final Formula leftResult = innerTest(left, false);
        if (leftResult.isConstantFormula()) {
            return isVerum(leftResult) ? innerTest(right, topLevel) : innerTest(f.not(right), topLevel);
        }
        final Formula rightResult = innerTest(right, false);
        if (rightResult.isConstantFormula()) {
            if (topLevel) {
                return f.constant(!evaluatesToTrue);
            }
            return isVerum(rightResult) ? leftResult : f.not(leftResult);
        }
        return f.equivalence(leftResult, rightResult);
    }

    private Formula handleOr(final Or formula, final boolean topLevel) {
        final List<Formula> nops = new ArrayList<>();
        for (final Formula op : formula) {
            final Formula opResult = innerTest(op, !evaluatesToTrue && topLevel);
            if (isVerum(opResult)) {
                return f.verum();
            }
            if (!opResult.isConstantFormula()) {
                if (!evaluatesToTrue && topLevel) {
                    return f.verum();
                }
                nops.add(opResult);
            }
        }
        return f.or(nops);
    }

    private Formula handleAnd(final And formula, final boolean topLevel) {
        final List<Formula> nops = new ArrayList<>();
        for (final Formula op : formula) {
            final Formula opResult = innerTest(op, evaluatesToTrue && topLevel);
            if (isFalsum(opResult)) {
                return f.falsum();
            }
            if (!opResult.isConstantFormula()) {
                if (evaluatesToTrue && topLevel) {
                    return f.falsum();
                }
                nops.add(opResult);
            }
        }
        return f.and(nops);
    }

    private Formula handlePbc(final PbConstraint formula) {
        final Assignment assignment = new Assignment();
        for (final Map.Entry<Variable, Boolean> entry : mapping.entrySet()) {
            assignment.addLiteral(f.literal(entry.getKey().getName(), entry.getValue()));
        }
        return formula.restrict(f, assignment);
    }

    private static FType getConstantType(final boolean constant) {
        return constant ? FType.TRUE : FType.FALSE;
    }

    private static boolean isFalsum(final Formula formula) {
        return formula.getType() == FType.FALSE;
    }

    private static boolean isVerum(final Formula formula) {
        return formula.getType() == FType.TRUE;
    }
}
