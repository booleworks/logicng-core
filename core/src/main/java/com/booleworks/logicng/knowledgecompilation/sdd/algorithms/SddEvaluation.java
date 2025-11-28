package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeIterationState;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeTerminal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeInternal;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * <p>
     * Note: the API is so specific because it is closely coupled with the model
     * enumeration and tuned for
     * @param assignmentMap       the given assignment as a literal bitmap
     * @param additionalVariables the additional variables as a variable bitmap
     * @param node                the SDD node
     * @param states              the partial evaluation state
     * @param dst                 the collection where the result is written to
     */
    public static void partialEvaluateInplace(final BitSet assignmentMap,
                                              final BitSet additionalVariables,
                                              final SddNode node, final PartialEvalState states,
                                              final Collection<Literal> dst) {
        states.updateAssignment(assignmentMap);
        final boolean satisfiable = findPath(assignmentMap, node, states);
        if (!satisfiable) {
            throw new RuntimeException("Assignment results in conflict");
        }
        writePath(assignmentMap, additionalVariables, node, states, dst);
    }

    private static boolean findPath(final BitSet assignmentMap, final SddNode node, final PartialEvalState states) {
        if (node.isFalse()) {
            return false;
        } else if (node.isTrue()) {
            return true;
        } else if (node.isLiteral()) {
            final SddNodeTerminal terminal = node.asTerminal();
            final int var = terminal.getVTree().getVariable();
            final boolean phase = terminal.getPhase();
            final int lit = states.sdd.literalToIndex(var, phase);
            final int litNeg = states.sdd.negateLitIdx(lit);
            return !assignmentMap.get(litNeg);
        } else {
            final SddNodeDecomposition decomp = node.asDecomposition();
            SddNodeIterationState state = states.iterators.get(decomp);
            final VTreeInternal vtree = decomp.getVTree().asInternal();
            if (state == null) {
                state = new SddNodeIterationState(decomp);
                state.setGeneration(states.generation);
                states.iterators.put(decomp, state);
            } else if (states.changes != null && (state.getGeneration() == states.generation || (
                    state.getGeneration() == states.generation - 1
                            && !vtree.getVariableMask().intersects(states.changes)))) {
                state.setGeneration(states.generation);
                return state.getActiveElement() != null;
            } else {
                state.reset();
                state.setGeneration(states.generation);
            }
            SddElement activeElement = state.getActiveElement();
            while (activeElement != null) {
                if (findPath(assignmentMap, activeElement.getPrime(), states)
                        && findPath(assignmentMap, activeElement.getSub(), states)) {
                    break;
                }
                activeElement = state.next();
            }
            return activeElement != null;
        }
    }

    private static void writePath(final BitSet assignmentMap, final BitSet additionalVars, final SddNode node,
                                  final PartialEvalState states, final Collection<Literal> dst) {
        if (node.isFalse()) {
            throw new RuntimeException("Cannot write model of unsatisfiable node");
        } else if (node.isTrue()) {
        } else if (node.isLiteral()) {
            final int varIdx = node.asTerminal().getVTree().getVariable();
            final boolean phase = node.asTerminal().getPhase();
            final int litIdx = states.sdd.literalToIndex(varIdx, phase);
            if (!assignmentMap.get(litIdx) && additionalVars.get(varIdx)) {
                final Variable var = states.sdd.indexToVariable(varIdx);
                final Literal lit = phase ? var : var.negate(states.sdd.getFactory());
                dst.add(lit);
            }
        } else {
            final SddNodeDecomposition decomp = node.asDecomposition();
            final SddNodeIterationState state = states.iterators.get(decomp);
            if (state == null) {
                throw new RuntimeException("Expected state");
            }
            final SddElement activeElement = state.getActiveElement();
            if (activeElement == null) {
                throw new RuntimeException("Active element was null, expected element");
            }
            writePath(assignmentMap, additionalVars, activeElement.getPrime(), states, dst);
            writePath(assignmentMap, additionalVars, activeElement.getSub(), states, dst);
            final VTree primeVTree = activeElement.getPrime().getVTree();
            final VTree subVTree = activeElement.getSub().getVTree();
            final VTreeInternal elementVTree = node.getVTree().asInternal();
            final List<Integer> gapVars = new ArrayList<>();
            VTreeUtil.gapVarsMasked(elementVTree.getLeft(), primeVTree, states.sdd.getVTree(), additionalVars, gapVars);
            VTreeUtil.gapVarsMasked(elementVTree.getRight(), subVTree, states.sdd.getVTree(), additionalVars, gapVars);
            for (final int gapVar : gapVars) {
                final Variable variable = states.sdd.indexToVariable(gapVar);
                dst.add(variable.negate(states.sdd.getFactory()));
            }
        }

    }

    /**
     * A state storing the last configuration used for partial evaluation and
     * tracking the changes of the assignments between two calls.
     */
    public static class PartialEvalState {
        private final Sdd sdd;
        private final Map<SddNode, SddNodeIterationState> iterators = new HashMap<>();
        private BitSet lastModel = null;
        private BitSet changes = null;
        private int generation = 0;

        public PartialEvalState(final Sdd sdd) {
            this.sdd = sdd;
        }

        void updateAssignment(final BitSet newModel) {
            if (lastModel != null) {
                generation++;
                lastModel.xor(newModel);
                changes = new BitSet();
                for (int i = lastModel.nextSetBit(0); i != -1; i = lastModel.nextSetBit(i + 2)) {
                    changes.set(sdd.litIdxToVarIdx(i));
                }
            }
            lastModel = newModel;
        }
    }
}
