// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFunction;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;

import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

/**
 * Function for collecting the integer variables from formulas.
 * @version 3.0.0
 * @since 3.0.0
 */
public class IntegerVariablesFunction implements FormulaFunction<SortedSet<IntegerVariable>> {
    protected final SortedSet<IntegerVariable> variables;

    protected IntegerVariablesFunction(final SortedSet<IntegerVariable> variables) {
        this.variables = variables;
    }

    /**
     * Constructs a new integer variable function, which collects all integer
     * variables from formulas.
     * @return the integer variable function
     */
    public static IntegerVariablesFunction get() {
        return new IntegerVariablesFunction(new TreeSet<>());
    }

    /**
     * Constructs a new integer variable function, which collects all integer
     * variables from formulas. The variables are added to the passed set.
     * @param variables destination for the variables
     * @return the integer variable function
     */
    public static IntegerVariablesFunction addToExistingSet(final SortedSet<IntegerVariable> variables) {
        return new IntegerVariablesFunction(variables);
    }

    @Override
    public LngResult<SortedSet<IntegerVariable>> apply(final Formula formula, final ComputationHandler handler) {
        final Stack<Formula> stack = new Stack<>();
        stack.push(formula);
        while (!stack.isEmpty()) {
            final Formula current = stack.pop();
            switch (current.getType()) {
                case EQUIV:
                case IMPL:
                case OR:
                case AND:
                case NOT:
                    for (final Formula op : current) {
                        stack.add(op);
                    }
                    break;
                case PREDICATE:
                    if (current instanceof CspPredicate) {
                        ((CspPredicate) current).variablesInplace(variables);
                    }
                    break;
                default:
                    break;
            }
        }
        return LngResult.of(variables);
    }
}
