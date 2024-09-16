// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.orderings;

import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * A breadth-first-search BDD variable ordering. Traverses the formula in a BFS
 * manner and gathers all variables in the occurrence.
 * @version 2.0.0
 * @since 1.4.0
 */
public final class BFSOrdering implements VariableOrderingProvider {

    @Override
    public List<Variable> getOrder(final FormulaFactory f, final Formula formula) {
        return new ArrayList<>(bfs(formula));
    }

    private LinkedHashSet<Variable> bfs(final Formula formula) {
        final LinkedHashSet<Variable> variables = new LinkedHashSet<>();
        final Queue<Formula> queue = new LinkedList<>();
        queue.add(formula);
        while (!queue.isEmpty()) {
            final Formula current = queue.remove();
            switch (current.getType()) {
                case LITERAL:
                    final Literal lit = (Literal) current;
                    if (lit.getPhase()) {
                        variables.add(lit.variable());
                    } else {
                        queue.add(lit.variable());
                    }
                    break;
                case NOT:
                    queue.add(((Not) current).getOperand());
                    break;
                case IMPL:
                case EQUIV:
                    final BinaryOperator op = (BinaryOperator) current;
                    queue.add(op.getLeft());
                    queue.add(op.getRight());
                    break;
                case AND:
                case OR:
                    for (final Formula operand : current) {
                        queue.add(operand);
                    }
                    break;
                case PBC:
                    final PBConstraint pbc = (PBConstraint) current;
                    for (final Literal literal : pbc.getOperands()) {
                        variables.add(literal.variable());
                    }
                    break;
                case PREDICATE:
                    throw new IllegalArgumentException(
                            "Cannot generate a variable ordering for a formula with predicates in it");
            }
        }
        return variables;
    }
}
