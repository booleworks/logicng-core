// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.orderings;

import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * A depth-first-search BDD variable ordering. Traverses the formula in a DFS
 * manner and gathers all variables in the occurrence.
 * @version 3.0.0
 * @since 1.4.0
 */
public class DfsOrdering implements VariableOrderingProvider {

    /**
     * Constructor.
     */
    public DfsOrdering() {
    }

    @Override
    public List<Variable> getOrder(final FormulaFactory f, final Formula formula) {
        final LinkedHashSet<Variable> order = new LinkedHashSet<>(formula.variables(f).size());
        dfs(formula, order);
        return new ArrayList<>(order);
    }

    protected void dfs(final Formula formula, final LinkedHashSet<Variable> variables) {
        switch (formula.getType()) {
            case LITERAL:
                variables.add(((Literal) formula).variable());
                break;
            case NOT:
                dfs(((Not) formula).getOperand(), variables);
                break;
            case IMPL:
            case EQUIV:
                final BinaryOperator op = (BinaryOperator) formula;
                dfs(op.getLeft(), variables);
                dfs(op.getRight(), variables);
                break;
            case AND:
            case OR:
                for (final Formula o : formula) {
                    dfs(o, variables);
                }
                break;
            case PBC:
                final PbConstraint pbc = (PbConstraint) formula;
                for (final Literal lit : pbc.getOperands()) {
                    variables.add(lit.variable());
                }
                break;
            case PREDICATE:
                throw new IllegalArgumentException(
                        "Cannot generate a variable ordering for a formula with predicates in it");
        }
    }
}
