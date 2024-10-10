package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

/**
 * A class grouping functions for extracting the integer variables from formulas.
 */
public class IntegerVariablesFunction {
    private IntegerVariablesFunction() {
    }

    /**
     * Adds all integer variables in the formula to the set {@code variables}.
     * @param formula   the formula
     * @param variables the destination for the integer variables
     */
    public static void integerVariablesInplace(final Formula formula, final SortedSet<IntegerVariable> variables) {
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
    }

    /**
     * Returns all integer variables in {@code formula}.
     * @param formula the formula
     * @return all integer variable in {@code formula}
     */
    public static SortedSet<IntegerVariable> integerVariables(final Formula formula) {
        final SortedSet<IntegerVariable> variables = new TreeSet<>();
        integerVariablesInplace(formula, variables);
        return variables;
    }

    public static SortedSet<IntegerVariable> getVariablesOnSolver(final Set<Variable> solverVariables,
                                                                  final Collection<IntegerVariable> variables,
                                                                  final CspEncodingContext context) {
        final TreeSet<IntegerVariable> result = new TreeSet<>();
        for (final IntegerVariable intVar : variables) {
            for (final Variable v : context.getSatVariables(List.of(intVar))) {
                if (solverVariables.contains(v)) {
                    result.add(intVar);
                    break;
                }
            }
        }
        return result;
    }
}
