package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SatSolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * Class grouping functions for enumerate models from CSP problems.
 */
public class CspModelEnumeration {
    private CspModelEnumeration() {
    }

    /**
     * Enumerate models from a solver given a CSP problem.
     * @param solver  the solver with the encoded problem
     * @param csp     the corresponding csp problem
     * @param context the encoding context
     * @param cf      the factory
     * @return a list with all models for this problem
     */
    public static List<CspAssignment> enumerate(final SatSolver solver, final Csp csp,
                                                final CspEncodingContext context,
                                                final CspFactory cf) {
        return enumerate(solver, csp.getPropagateSubstitutions().getAllOrSelf(csp.getVisibleIntegerVariables()),
                csp.getVisibleBooleanVariables(), context, cf);
    }

    /**
     * Enumerate models from a solver and a set of relevant integer and boolean variables. The relevant variables are
     * all in the produced models. If a variable was not encoded on the solver, the function will assume that all
     * values of the variable are allowed.
     * @param solver           the solver with the encoded problem
     * @param integerVariables the relevant integer variables
     * @param booleanVariables the relevant boolean variables
     * @param context          the encoding context
     * @param cf               the factory
     * @return a list with all models for this problem
     */
    public static List<CspAssignment> enumerate(final SatSolver solver,
                                                final Collection<IntegerVariable> integerVariables,
                                                final Collection<Variable> booleanVariables,
                                                final CspEncodingContext context, final CspFactory cf) {
        final SortedSet<IntegerVariable> intVariablesOnSolver =
                IntegerVariablesFunction.getVariablesOnSolver(solver.getUnderlyingSolver().knownVariables(),
                        integerVariables, context);
        final List<IntegerVariable> intVariablesNotOnSolver = integerVariables
                .stream()
                .filter(v -> !intVariablesOnSolver.contains(v))
                .collect(Collectors.toList());
        final Set<Variable> allVars = context.getSatVariables(intVariablesOnSolver);
        allVars.addAll(booleanVariables);
        final List<CspAssignment> decodedModels = solver.enumerateAllModels(allVars).stream()
                .map(m -> cf.decode(m.toAssignment(), intVariablesOnSolver, booleanVariables, context))
                .collect(Collectors.toList());
        if (intVariablesNotOnSolver.isEmpty() || decodedModels.isEmpty()) {
            return decodedModels;
        } else {
            return enumerateAdditionalVariables(decodedModels, intVariablesNotOnSolver);
        }
    }

    private static List<CspAssignment> enumerateAdditionalVariables(final List<CspAssignment> decodedModels,
                                                                    final List<IntegerVariable> integerVariables) {
        final List<Iterator<Integer>> iterators =
                integerVariables.stream().map(v -> v.getDomain().iterator()).collect(Collectors.toList());
        final List<Integer> values =
                iterators.stream().map(Iterator::next).collect(Collectors.toList()); //Domains cannot be empty
        final List<CspAssignment> models = new ArrayList<>();
        int leaderIndex = 0;
        for (int index = 0; index < integerVariables.size(); ) {
            for (final CspAssignment model : decodedModels) {
                final CspAssignment newModel = new CspAssignment(model);
                for (int i = 0; i < integerVariables.size(); ++i) {
                    newModel.addIntAssignment(integerVariables.get(i), values.get(i));
                }
                models.add(newModel);
            }
            if (iterators.get(index).hasNext()) {
                values.set(index, iterators.get(index).next());
            } else {
                while (index < integerVariables.size() && !iterators.get(index).hasNext()) {
                    ++index;
                }
                if (index == integerVariables.size()) {
                    break;
                }
                values.set(index, iterators.get(index).next());
                for (int i = 0; i < index; ++i) {
                    iterators.set(i, integerVariables.get(i).getDomain().iterator());
                    values.set(i, iterators.get(i).next());
                }
                if (index > leaderIndex) {
                    leaderIndex = index;
                }
                index = 0;
            }
        }
        return models;
    }
}
