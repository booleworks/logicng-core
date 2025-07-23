package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.util.Pair;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

/**
 * A collection of uncategorized utility functions for SDDs.
 * <p>
 * These functions are intended to be used internally and might have very
 * specific contracts and use cases.  Nevertheless, it should all be properly
 * documented and tested, so using them is still safe, unless mentioned
 * otherwise.
 * @version 3.0.0
 * @since 3.0.0
 */
public class SddUtil {
    private SddUtil() {

    }

    /**
     * A utility function that computes the variables used in an SDD node and
     * its children.
     * <p>
     * Note that, this is a low-level function. The function returns the
     * internal indices of the variables.  There are higher level alternatives
     * that return {@link Variable}
     * @param node the SDD node
     * @return a set with the variables of the sdd node
     * @see com.booleworks.logicng.knowledgecompilation.sdd.functions.SddVariablesFunction SddVariablesFunction
     * @see SddNode#variables(Sdd)
     * @see SddNode#variables()
     */
    public static SortedSet<Integer> variables(final SddNode node) {
        final SortedSet<Integer> result = new TreeSet<>();
        final Stack<SddNode> stack = new Stack<>();
        final Set<SddNode> visited = new HashSet<>();
        stack.push(node);
        visited.add(node);
        while (!stack.isEmpty()) {
            final SddNode current = stack.pop();
            if (current.isLiteral()) {
                result.add(current.asTerminal().getVTree().getVariable());
            } else if (current.isDecomposition()) {
                for (final SddElement element : current.asDecomposition()) {
                    if (visited.add(element.getPrime())) {
                        stack.add(element.getPrime());
                    }
                    if (visited.add(element.getSub())) {
                        stack.add(element.getSub());
                    }
                }
            }
        }
        return result;
    }

    /**
     * Converts a set of variables into the internal index representation.
     * Variables that are currently not known to the SDD container are ignored.
     * <p>
     * The function takes a collection as destination for the result. The same
     * collection is also returned.
     * @param variables the input variables
     * @param sdd       the SDD container
     * @param dst       the collection to which the result is written
     * @param <C>       the type of the collection for the result
     * @return the destination collection
     * @see SddUtil#varsToIndicesExpectKnown(Set, Sdd, Collection)
     * @see SddUtil#indicesToVars(Set, Sdd, Collection)
     */
    public static <C extends Collection<Integer>> C varsToIndicesOnlyKnown(final Set<Variable> variables, final Sdd sdd,
                                                                           final C dst) {
        for (final Variable var : variables) {
            final int idx = sdd.variableToIndex(var);
            if (idx != -1) {
                dst.add(idx);
            }
        }
        return dst;
    }

    /**
     * Converts a set of variables into the internal index representation.
     * The function expects to know all variables otherwise it will throw a
     * runtime exception.
     * <p>
     * The function takes a collection as destination for the result. The same
     * collection is also returned.
     * @param variables the input variables
     * @param sdd       the SDD container
     * @param dst       the collection to which the result is written
     * @param <C>       the type of the collection for the result
     * @return the destination collection
     * @throws IllegalArgumentException if the function encounters an unknown variable
     * @see SddUtil#varsToIndicesExpectKnown(Set, Sdd, Collection)
     * @see SddUtil#indicesToVars(Set, Sdd, Collection)
     */
    public static <C extends Collection<Integer>> C varsToIndicesExpectKnown(final Set<Variable> variables,
                                                                             final Sdd sdd, final C dst) {
        for (final Variable var : variables) {
            final int idx = sdd.variableToIndex(var);
            if (idx == -1) {
                throw new IllegalArgumentException("Variable is not known to SDD: " + var);
            } else {
                dst.add(idx);
            }
        }
        return dst;
    }

    /**
     * Converts a set of internal indices into to variables.
     * <p>
     * The function takes a collection as destination for the result. The same
     * collection is also returned.
     * <p>
     * Warning: Passing integers that do not represent variables is undefined behaviour.
     * @param indices the internal indices
     * @param sdd     the SDD container
     * @param dst     the collection to which the result is written
     * @param <C>     the type of the collection for the result
     * @return the destination collection
     * @see SddUtil#varsToIndicesOnlyKnown(Set, Sdd, Collection)
     * @see SddUtil#varsToIndicesExpectKnown(Set, Sdd, Collection)
     */
    public static <C extends Collection<Variable>> C indicesToVars(final Set<Integer> indices, final Sdd sdd,
                                                                   final C dst) {
        for (final int idx : indices) {
            dst.add(sdd.indexToVariable(idx));
        }
        return dst;
    }

    /**
     * Computes the lowest common ancestor from a collection of compressed SDD
     * elements.
     * <p>
     * Passing not compressed elements results in undefined behaviour.
     * @param elements compressed elements
     * @param sdd      the SDD container
     * @return the lowest common ancestor of the elements
     * @see VTreeRoot#lcaOf(VTree, VTree)
     * @see VTreeUtil#lcaFromVariables(Collection, Sdd)
     */
    public static VTree lcaOfCompressedElements(final Collection<SddElement> elements, final Sdd sdd) {
        assert !elements.isEmpty();

        final VTreeRoot root = sdd.getVTree();
        VTree lLca = null;
        VTree rLca = null;

        for (final SddElement element : elements) {
            final VTree pVTree = sdd.vTreeOf(element.getPrime());
            final VTree sVTree = sdd.vTreeOf(element.getSub());
            assert pVTree != null;

            lLca = lLca == null ? pVTree : root.lcaOf(pVTree, lLca);
            if (sVTree != null && rLca != null) {
                rLca = root.lcaOf(sVTree, rLca);
            } else if (sVTree != null) {
                rLca = sVTree;
            }
        }

        assert lLca != null && rLca != null;
        assert lLca.getPosition() < rLca.getPosition();

        final Pair<VTree, VTreeRoot.CmpType> lca = root.cmpVTrees(lLca, rLca);

        assert lca.getSecond() == VTreeRoot.CmpType.INCOMPARABLE;
        assert lca.getFirst() != null;

        return lca.getFirst();
    }
}
