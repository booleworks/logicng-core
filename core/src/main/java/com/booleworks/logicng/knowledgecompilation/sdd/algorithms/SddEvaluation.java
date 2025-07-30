package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A collection of functions for evaluating SDDs.
 * <p>
 * These functions are intended to be used internally and might have very
 * specific contracts and use cases.  Nevertheless, it should all be properly
 * documented and tested, so using them is still safe, unless mentioned
 * otherwise.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddEvaluation {
    /**
     * Evaluates an SDD with a given assignment.  A literal not covered by
     * the assignment evaluates to {@code false} if it is positive, otherwise it
     * evaluates to {@code true}.
     * @param assignment the given assignment
     * @param node       the SDD node
     * @param sdd        the SDD container of {@code node}
     * @return the result of the evaluation, {@code true} or {@code false}
     */
    public static boolean evaluate(final Assignment assignment, final SddNode node, final Sdd sdd) {
        return evaluateRecursive(assignment, node, sdd, new HashMap<>());
    }

    private static boolean evaluateRecursive(final Assignment assignment, final SddNode node, final Sdd sdd,
                                             final Map<SddNode, Boolean> cache) {
        final Boolean cached = cache.get(node);
        if (cached != null) {
            return cached;
        }
        boolean result;
        if (node.isFalse()) {
            result = false;
        } else if (node.isTrue()) {
            result = true;
        } else if (node.isLiteral()) {
            final Literal lit = (Literal) node.asTerminal().toFormula(sdd);
            result = assignment.evaluateLit(lit);
        } else {
            result = false;
            for (final SddElement element : node.asDecomposition()) {
                if (evaluateRecursive(assignment, element.getPrime(), sdd, cache)
                        && evaluateRecursive(assignment, element.getSub(), sdd, cache)) {
                    result = true;
                    break;
                }
            }
        }
        cache.put(node, result);
        return result;
    }


    /**
     * Given a partial assignment for {@code node} the function computes a
     * complementary assignment for the variables in
     * {@code additionalVariables}.
     * <p>
     * No variables contained in {@code assignment}, even if they are in
     * {@code additionalVariables} will be in the result.
     * @param assignment          the given assignment
     * @param additionalVariables the variables for the result
     * @param node                the SDD node
     * @param sdd                 the SDD container of {@code node}
     * @return Empty if {@code assignment} is unsatisfiable or a complementary
     * assignment to {@code assignment}
     */
    public static Optional<List<Literal>> partialEvaluate(final Assignment assignment,
                                                          final Set<Variable> additionalVariables,
                                                          final SddNode node, final Sdd sdd) {
        final Set<Integer> additionalVariableIdx =
                SddUtil.varsToIndicesOnlyKnown(additionalVariables, sdd, new HashSet<>());
        final Set<Variable> notKnownVariables =
                additionalVariables.stream().filter(v -> !sdd.knows(v)).collect(Collectors.toSet());
        final Optional<List<Literal>> additionalAssignments =
                partialEvaluateFromIndices(assignment, additionalVariableIdx, node, sdd);
        if (additionalAssignments.isEmpty()) {
            return additionalAssignments;
        }
        for (final Variable v : notKnownVariables) {
            if (!assignment.positiveVariables().contains(v) && !assignment.negativeVariables().contains(v)) {
                additionalAssignments.get().add(v.negate(sdd.getFactory()));
            }
        }
        return additionalAssignments;
    }

    /**
     * Given a partial assignment for {@code node} the function computes a
     * complementary assignment for the variables in
     * {@code additionalVariables}.
     * <p>
     * No variables contained in {@code assignment}, even if they are in
     * {@code additionalVariables} will be in the result.
     * @param assignment            the given assignment
     * @param additionalVariableIdx internal indices of the variables for the result
     * @param node                  the SDD node
     * @param sdd                   the SDD container of {@code node}
     * @return Empty if {@code assignment} is unsatisfiable or a complementary
     * assignment to {@code assignment}
     */
    public static Optional<List<Literal>> partialEvaluateFromIndices(final Assignment assignment,
                                                                     final Set<Integer> additionalVariableIdx,
                                                                     final SddNode node, final Sdd sdd) {
        final Map<Variable, Boolean> additionalValues = new LinkedHashMap<>();
        final boolean satisfiable =
                partialEvaluateRecursive(assignment, additionalVariableIdx, node, sdd, additionalValues);
        if (!satisfiable) {
            return Optional.empty();
        }
        final List<Literal> additionalAssignments = new ArrayList<>();
        for (final Map.Entry<Variable, Boolean> ass : additionalValues.entrySet()) {
            additionalAssignments.add(ass.getValue() ? ass.getKey() : ass.getKey().negate(sdd.getFactory()));
        }
        return Optional.of(additionalAssignments);
    }

    private static boolean partialEvaluateRecursive(final Assignment assignment,
                                                    final Set<Integer> additionalVars,
                                                    final SddNode node, final Sdd sdd,
                                                    final Map<Variable, Boolean> additionalVals) {
        if (node.isFalse()) {
            return false;
        } else if (node.isTrue()) {
            return true;
        } else if (node.isLiteral()) {
            final Literal lit = (Literal) node.asTerminal().toFormula(sdd);
            if (!assignment.positiveVariables().contains(lit.variable()) && !assignment.negativeVariables()
                    .contains(lit.variable())) {
                if (additionalVars.contains(node.asTerminal().getVTree().getVariable())) {
                    additionalVals.put(lit.variable(), lit.getPhase());
                }
                return true;
            } else {
                additionalVals.remove(lit.variable());
                return assignment.evaluateLit(lit);
            }
        } else {
            final boolean result = false;
            for (final SddElement element : node.asDecomposition()) {
                if (!partialEvaluateRecursive(assignment, additionalVars, element.getSub(), sdd, additionalVals)) {
                    continue;
                }
                if (!partialEvaluateRecursive(assignment, additionalVars, element.getPrime(), sdd, additionalVals)) {
                    continue;
                }
                final VTree primeVTree = sdd.vTreeOf(element.getPrime());
                final VTree subVTree = sdd.vTreeOf(element.getSub());
                final VTreeInternal elementVTree = sdd.vTreeOf(node).asInternal();
                final Set<Integer> gapVars = new TreeSet<>();
                VTreeUtil.gapVars(elementVTree.getLeft(), primeVTree, sdd.getVTree(), additionalVars, gapVars);
                VTreeUtil.gapVars(elementVTree.getRight(), subVTree, sdd.getVTree(), additionalVars, gapVars);
                for (final int gapVar : gapVars) {
                    additionalVals.put(sdd.indexToVariable(gapVar), false);
                }
                return true;
            }
            return false;
        }
    }
}
