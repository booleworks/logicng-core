package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.Util;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddFunction;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

public class SddFactory {
    private final FormulaFactory f;
    private int currentVTreeId;
    private int currentSddId;
    private final HashMap<Pair<VTree, VTree>, VTreeInternal> internalVTreeNodes;
    private final HashMap<Variable, VTreeLeaf> leafVTreeNodes;
    private final HashMap<VTree, VTreeRoot> vTreeRoots;
    private final HashMap<TreeSet<SddElement>, SddNodeDecomposition> sddDecompositions;
    private final HashMap<Formula, SddNodeTerminal> sddTerminals;
    private final SddNodeTerminal verumNode;
    private final SddNodeTerminal falsumNode;
    private final HashMap<SddNode, SddNode> negations;
    private final HashMap<Pair<SddNode, SddNode>, SddNode> conjunctions;
    private final HashMap<Pair<SddNode, SddNode>, SddNode> disjunctions;
    private final HashMap<SddNode, SortedSet<Variable>> variables;

    public SddFactory(final FormulaFactory f) {
        this.f = f;
        currentVTreeId = 0;
        currentSddId = 2;
        internalVTreeNodes = new HashMap<>();
        leafVTreeNodes = new HashMap<>();
        vTreeRoots = new HashMap<>();
        sddDecompositions = new HashMap<>();
        sddTerminals = new HashMap<>();
        negations = new HashMap<>();
        conjunctions = new HashMap<>();
        disjunctions = new HashMap<>();
        verumNode = new SddNodeTerminal(0, null, f.verum());
        falsumNode = new SddNodeTerminal(1, null, f.falsum());
        variables = new HashMap<>();
        negations.put(verumNode, falsumNode);
        negations.put(falsumNode, verumNode);
    }

    public VTreeLeaf vTreeLeaf(final Variable variable) {
        final VTreeLeaf cached = leafVTreeNodes.get(variable);
        if (cached != null) {
            return cached;
        }
        final VTreeLeaf newNode = new VTreeLeaf(currentVTreeId++, variable);
        leafVTreeNodes.put(variable, newNode);
        return newNode;
    }

    public VTreeInternal vTreeInternal(final VTree left, final VTree right) {
        final Pair<VTree, VTree> pair = new Pair<>(left, right);
        final VTreeInternal cached = internalVTreeNodes.get(pair);
        if (cached != null) {
            return cached;
        }
        final VTreeInternal newNode = new VTreeInternal(currentVTreeId++, left, right);
        internalVTreeNodes.put(pair, newNode);
        return newNode;
    }

    public SddNodeTerminal verum() {
        return verumNode;
    }

    public SddNodeTerminal falsum() {
        return falsumNode;
    }

    public SddNodeTerminal terminal(final Literal terminal, final VTreeRoot root) {
        final SddNodeTerminal cached = sddTerminals.get(terminal);
        final VTreeLeaf vTree = root.getLeaf(terminal.variable());
        if (cached != null) {
            return cached;
        }
        final SddNodeTerminal newNode = new SddNodeTerminal(currentSddId++, vTree, terminal);
        sddTerminals.put(terminal, newNode);

        final Literal negTerminal = terminal.negate(f);
        final SddNodeTerminal newNodeNeg = new SddNodeTerminal(currentSddId++, vTree, negTerminal);
        sddTerminals.put(negTerminal, newNodeNeg);

        negations.put(newNode, newNodeNeg);
        negations.put(newNodeNeg, newNode);

        return newNode;
    }

    public SddNodeDecomposition decomposition(final TreeSet<SddElement> elements, final VTreeRoot root) {
        final SddNodeDecomposition cached = sddDecompositions.get(elements);
        if (cached != null) {
            final VTree lca = Util.lcaOfCompressedElements(elements, root);
            cached.setVTree(lca);
            return cached;
        }
        assert Util.elementsCompressed(elements);
        final VTree vTree = Util.lcaOfCompressedElements(elements, root);
        final TreeSet<SddElement> elementsCopy = new TreeSet<>(elements);
        final SddNodeDecomposition newNode = new SddNodeDecomposition(currentSddId++, vTree, elementsCopy);
        sddDecompositions.put(elementsCopy, newNode);
        return newNode;
    }

    public SddNode lookupApplyComputation(final SddNode left, final SddNode right, final SddApplyOperation op) {
        switch (op) {
            case CONJUNCTION: {
                final Pair<SddNode, SddNode> key =
                        left.getId() < right.getId() ? new Pair<>(left, right) : new Pair<>(right, left);
                return conjunctions.get(key);
            }
            case DISJUNCTION: {
                final Pair<SddNode, SddNode> key =
                        left.getId() < right.getId() ? new Pair<>(left, right) : new Pair<>(right, left);
                return disjunctions.get(key);
            }
            default:
                throw new RuntimeException("Unknown operation type");
        }
    }

    public void cacheApplyComputation(final SddNode left, final SddNode right, final SddNode result,
                                      final SddApplyOperation op) {
        switch (op) {
            case CONJUNCTION: {
                final Pair<SddNode, SddNode>
                        key =
                        left.getId() < right.getId() ? new Pair<>(left, right) : new Pair<>(right, left);
                conjunctions.put(key, result);
                return;
            }
            case DISJUNCTION: {
                final Pair<SddNode, SddNode> key =
                        left.getId() < right.getId() ? new Pair<>(left, right) : new Pair<>(right, left);
                disjunctions.put(key, result);
                return;
            }
            default:
                throw new RuntimeException("Unknown operation type");
        }
    }

    public VTreeRoot constructRoot(final VTree rootNode) {
        final VTreeRoot cached = vTreeRoots.get(rootNode);
        if (cached != null) {
            return cached;
        }

        final HashMap<VTree, VTreeInternal> parents = new HashMap<>();
        final HashMap<Variable, VTreeLeaf> variableToLeaf = new HashMap<>();
        final ArrayList<Variable> variables = new ArrayList<>();
        final Stack<VTree> stack = new Stack<>();
        stack.push(rootNode);
        while (!stack.isEmpty()) {
            final VTree current = stack.pop();
            if (current instanceof VTreeInternal) {
                final VTree left = ((VTreeInternal) current).getLeft();
                final VTree right = ((VTreeInternal) current).getRight();
                parents.put(left, (VTreeInternal) current);
                parents.put(right, (VTreeInternal) current);
                stack.push(left);
                stack.push(right);
            } else {
                variables.add(((VTreeLeaf) current).getVariable());
                variableToLeaf.put(((VTreeLeaf) current).getVariable(), (VTreeLeaf) current);
            }
        }
        final HashMap<VTree, Integer> positions = new HashMap<>();
        final HashMap<Integer, VTree> pos2vtree = new HashMap<>();
        calculateInorder(rootNode, 0, positions, pos2vtree);
        final VTreeRoot newRoot = new VTreeRoot(parents, rootNode, variables, positions, pos2vtree, variableToLeaf);
        vTreeRoots.put(rootNode, newRoot);
        return newRoot;
    }

    private int calculateInorder(final VTree vTree, final int base, final HashMap<VTree, Integer> positions,
                                 final HashMap<Integer, VTree> pos2vtree) {
        if (vTree instanceof VTreeInternal) {
            final int b = calculateInorder(((VTreeInternal) vTree).getLeft(), base, positions, pos2vtree);
            positions.put(vTree, b + 1);
            pos2vtree.put(b + 1, vTree);
            return calculateInorder(((VTreeInternal) vTree).getRight(), b + 2, positions, pos2vtree);
        } else {
            positions.put(vTree, base);
            pos2vtree.put(base, vTree);
            return base;
        }
    }

    public void deregisterVTree(final VTreeRoot vTree) {
        vTreeRoots.remove(vTree.getRoot());
    }

    public SddNode negate(final SddNode node, final VTreeRoot root) {
        final SddNode cached = negations.get(node);
        if (cached != null) {
            return cached;
        }

        final SddNode nodeNeg;
        if (node instanceof SddNodeDecomposition) {
            final SddNodeDecomposition decomp = node.asDecomposition();
            //Note: compression is not possible here
            final TreeSet<SddElement> newElements = new TreeSet<>();
            for (final SddElement element : decomp.getElements()) {
                final SddNode subNeg = negate(element.getSub(), root);
                Util.pushNewElement(element.getPrime(), subNeg, newElements);
            }
            nodeNeg = decomposition(newElements, root);
        } else {
            final SddNodeTerminal t = node.asTerminal();
            nodeNeg = terminal((Literal) t.getTerminal().negate(f), root);
        }
        negations.put(node, nodeNeg);
        negations.put(nodeNeg, node);
        return nodeNeg;
    }

    public SddNode getNegationIfCached(final SddNode node) {
        return negations.get(node);
    }

    public Map<SddNode, SortedSet<Variable>> getVariablesCache() {
        return variables;
    }

    public <RESULT> RESULT apply(final SddFunction<RESULT> function) {
        return function.apply(this);
    }

    public <RESULT> LngResult<RESULT> apply(final SddFunction<RESULT> function, final ComputationHandler handler) {
        return function.apply(this, handler);
    }

    public int getSddNodeCount() {
        return sddTerminals.size() + sddDecompositions.size();
    }

    public FormulaFactory getFactory() {
        return f;
    }

    @Override
    public String toString() {
        return "SddFactory{" +
                "\ncurrentVTreeId=" + currentVTreeId +
                "\ncurrentSddId=" + currentSddId +
                "\ninternalVTreeNodes=" + internalVTreeNodes +
                "\nleafVTreeNodes=" + leafVTreeNodes +
                "\nvTreeRoots=" + vTreeRoots +
                "\nsddDecompositions=" + sddDecompositions +
                "\nsddTerminals=" + sddTerminals +
                "\n}";
    }
}
