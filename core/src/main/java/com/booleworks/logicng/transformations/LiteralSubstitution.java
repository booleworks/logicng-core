// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;

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
public final class LiteralSubstitution extends StatelessFormulaTransformation {

    private final Map<Literal, Literal> substitution;

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
    public LNGResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        return LNGResult.of(substitute(formula));
    }

    private Formula substitute(final Formula formula) {
        switch (formula.type()) {
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
                if (!literal.phase()) {
                    lit = substitution.get(literal.variable());
                    return lit != null ? lit.negate(f) : formula;
                }
                return formula;
            case NOT:
                return f.not(apply(((Not) formula).operand()));
            case EQUIV:
            case IMPL:
                final BinaryOperator binOp = (BinaryOperator) formula;
                return f.binaryOperator(formula.type(), apply(binOp.left()), apply(binOp.right()));
            case OR:
            case AND:
                final List<Formula> operands = new ArrayList<>();
                for (final Formula op : formula) {
                    operands.add(apply(op));
                }
                return f.naryOperator(formula.type(), operands);
            case PBC:
                final PBConstraint pbc = (PBConstraint) formula;
                final List<Literal> originalOperands = pbc.operands();
                final List<Literal> literals = new ArrayList<>(originalOperands.size());
                for (final Literal originalOperand : originalOperands) {
                    literals.add((Literal) apply(originalOperand));
                }
                return f.pbc(pbc.comparator(), pbc.rhs(), literals, pbc.coefficients());
            default:
                throw new IllegalArgumentException("Unknown formula type: " + formula.type());
        }
    }
}
