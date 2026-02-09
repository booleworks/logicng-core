// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A formula transformation which substitutes literals by other literals.
 * <p>
 * Always the best fit is chosen. So if there are two mappings for e.g.
 * {@code a -> b} and {@code ~a -> c}. Then {@code ~a} will be mapped to
 * {@code c} and not to {@code ~b}. On the other hand if there is only the
 * mapping {@code a -> b}, the literal {@code ~a} will be mapped to {@code ~b}.
 * @version 3.0.0
 * @since 2.0.0
 */
public class LiteralSubstitution extends StatelessFormulaTransformation {

    protected final Map<Literal, Literal> substitution;

    /**
     * Generate a new formula substitution with a given literal-to-literal
     * mapping.
     * @param f            the formula factory to generate new formulas
     * @param substitution a mapping from literals to literals
     */
    public LiteralSubstitution(final FormulaFactory f, final Map<Literal, Literal> substitution) {
        super(f);
        this.substitution = substitution;
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        return LngResult.of(substitute(formula));
    }

    protected Formula substitute(final Formula formula) {
        switch (formula.getType()) {
            case TRUE:
            case FALSE:
            case PREDICATE:
                return formula;
            case LITERAL:
                final Literal literal = (Literal) formula;
                Literal lit = substitution.get(literal);
                if (lit != null) {
                    return lit;
                }
                if (!literal.getPhase()) {
                    lit = substitution.get(literal.variable());
                    return lit != null ? lit.negate(f) : formula;
                }
                return formula;
            case NOT:
                return f.not(apply(((Not) formula).getOperand()));
            case EQUIV:
            case IMPL:
                final BinaryOperator binOp = (BinaryOperator) formula;
                return f.binaryOperator(formula.getType(), apply(binOp.getLeft()), apply(binOp.getRight()));
            case OR:
            case AND:
                final List<Formula> operands = new ArrayList<>();
                for (final Formula op : formula) {
                    operands.add(apply(op));
                }
                return f.naryOperator(formula.getType(), operands);
            case PBC:
                final PbConstraint pbc = (PbConstraint) formula;
                final List<Literal> originalOperands = pbc.getOperands();
                final List<Literal> literals = new ArrayList<>(originalOperands.size());
                for (final Literal originalOperand : originalOperands) {
                    literals.add((Literal) apply(originalOperand));
                }
                return f.pbc(pbc.comparator(), pbc.getRhs(), literals, pbc.getCoefficients());
            default:
                throw new IllegalArgumentException("Unknown formula type: " + formula.getType());
        }
    }
}
