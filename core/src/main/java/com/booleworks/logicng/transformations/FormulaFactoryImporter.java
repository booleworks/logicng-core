// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.Equivalence;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Implication;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.NAryOperator;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * A formula transformation which imports a given formula into a new formula
 * factory. If the current factory of the formula and the new formula factory
 * are equal, no action is performed.
 * @version 3.0.0
 * @since 1.3.1
 */
public final class FormulaFactoryImporter extends StatelessFormulaTransformation {

    /**
     * Constructs a new formula factory importer with a given formula factory.
     * This is the formula factory where the formulas should be imported to.
     * @param f the formula factory where the formulas should be imported to
     */
    public FormulaFactoryImporter(final FormulaFactory f) {
        super(f);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        return LngResult.of(applyRec(formula));
    }

    private Formula applyRec(final Formula formula) {
        if (formula.getFactory() == f) {
            return formula;
        }
        switch (formula.getType()) {
            case TRUE:
                return f.verum();
            case FALSE:
                return f.falsum();
            case LITERAL:
                final Literal literal = (Literal) formula;
                return f.literal(literal.getName(), literal.getPhase());
            case PREDICATE:
                throw new UnsupportedOperationException("Cannot import a predicate in a Boolean formula factory");
            case NOT:
                final Not not = (Not) formula;
                return f.not(apply(not.getOperand()));
            case IMPL:
                final Implication implication = (Implication) formula;
                return f.implication(apply(implication.getLeft()), apply(implication.getRight()));
            case EQUIV:
                final Equivalence equivalence = (Equivalence) formula;
                return f.equivalence(apply(equivalence.getLeft()), apply(equivalence.getRight()));
            case OR:
                final Or or = (Or) formula;
                return f.or(gatherAppliedOperands(or));
            case AND:
                final And and = (And) formula;
                return f.and(gatherAppliedOperands(and));
            case PBC:
                final PbConstraint pbc = (PbConstraint) formula;
                final List<Literal> literals = new ArrayList<>(pbc.getOperands().size());
                for (final Literal op : pbc.getOperands()) {
                    literals.add((Literal) apply(op));
                }
                return f.pbc(pbc.comparator(), pbc.getRhs(), literals, pbc.getCoefficients());
            default:
                throw new IllegalArgumentException("Unknown LogicNG formula type: " + formula.getType());
        }
    }

    /**
     * Gather the operands of an n-ary operator and returns its applied
     * operands.
     * @param operator the n-ary operator
     * @return the applied operands of the given operator
     */
    private LinkedHashSet<Formula> gatherAppliedOperands(final NAryOperator operator) {
        final LinkedHashSet<Formula> applied = new LinkedHashSet<>();
        for (final Formula operand : operator) {
            applied.add(apply(operand));
        }
        return applied;
    }
}
