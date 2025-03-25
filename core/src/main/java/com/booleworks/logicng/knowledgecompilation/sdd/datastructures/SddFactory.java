package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
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
import java.util.HashSet;
import java.util.Stack;
import java.util.TreeSet;

public class SddFactory {
    private final FormulaFactory f;
    private int currentVTreeId;
    private int currentSddId;
    private final HashMap<Pair<VTree, VTree>, VTreeInternal> internalVTreeNodes;
    private final HashMap<Variable, VTreeLeaf> leafVTreeNodes;
    private final HashMap<VTree, VTreeRoot> vTreeRoots;
    private final HashMap<VTree, Integer> vTreeUsages;
    private final HashMap<VTree, Integer> vTreeRootUsages;
    private final HashMap<TreeSet<SddElement>, SddNodeDecomposition> sddDecompositions;
    private final HashMap<Formula, SddNodeTerminal> sddTerminals;
    private final HashMap<SddNode, Integer> sddNodeUsages;
    private final HashMap<VTree, HashSet<SddNode>> vTreeToSddNodes;
    private final SddNodeTerminal verumNode;
    private final SddNodeTerminal falsumNode;
    private final HashMap<SddNode, SddNode> negations;
    private final HashMap<Pair<SddNode, SddNode>, SddNode> conjunctions;
    private final HashMap<Pair<SddNode, SddNode>, SddNode> disjunctions;

    public SddFactory(final FormulaFactory f) {
        this.f = f;
        currentVTreeId = 0;
        currentSddId = 2;
        internalVTreeNodes = new HashMap<>();
        leafVTreeNodes = new HashMap<>();
        vTreeUsages = new HashMap<>();
        vTreeRootUsages = new HashMap<>();
        vTreeRoots = new HashMap<>();
        sddDecompositions = new HashMap<>();
        sddTerminals = new HashMap<>();
        sddNodeUsages = new HashMap<>();
        vTreeToSddNodes = new HashMap<>();
        negations = new HashMap<>();
        conjunctions = new HashMap<>();
        disjunctions = new HashMap<>();
        verumNode = new SddNodeTerminal(0, f.verum());
        falsumNode = new SddNodeTerminal(1, f.falsum());
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
            registerSddNode(cached, vTree, root);
            return cached;
        }
        final SddNodeTerminal newNode = new SddNodeTerminal(currentSddId++, terminal);
        sddTerminals.put(terminal, newNode);
        registerSddNode(newNode, vTree, root);
        return newNode;
    }

    public SddNodeDecomposition decomposition(final TreeSet<SddElement> elements, final VTree vTree,
                                              final VTreeRoot root) {
        final SddNodeDecomposition cached = sddDecompositions.get(elements);
        if (cached != null) {
            assert vTree == Util.lcaOfCompressedElements(elements, root);
            registerSddNode(cached, vTree, root);
            return cached;
        }
        assert Util.elementsCompressed(elements);
        final SddNodeDecomposition newNode = new SddNodeDecomposition(currentSddId++, elements);
        sddDecompositions.put(elements, newNode);
        registerSddNode(newNode, vTree, root);
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

    private void registerSddNode(final SddNode node, final VTree vTree, final VTreeRoot root) {
        final HashSet<SddNode> nodes = vTreeToSddNodes.get(vTree);
        if (nodes == null) {
            final HashSet<SddNode> set = new HashSet<>();
            set.add(node);
            vTreeToSddNodes.put(vTree, set);
            if (sddNodeUsages.containsKey(node)) {
                sddNodeUsages.put(node, sddNodeUsages.get(node) + 1);
            } else {
                sddNodeUsages.put(node, 1);
            }
        } else {
            if (!nodes.contains(node)) {
                nodes.add(node);
                if (sddNodeUsages.containsKey(node)) {
                    sddNodeUsages.put(node, sddNodeUsages.get(node) + 1);
                } else {
                    sddNodeUsages.put(node, 1);
                }
            }
        }
        root.addNode(node, vTree);
    }

    public VTreeRoot constructRoot(final VTree rootNode) {
        final VTreeRoot cached = vTreeRoots.get(rootNode);
        if (cached != null) {
            vTreeRootUsages.put(cached.getRoot(), vTreeRootUsages.get(cached.getRoot()) + 1);
            return cached;
        }

        final HashMap<VTree, VTreeInternal> parents = new HashMap<>();
        final HashMap<Variable, VTreeLeaf> variableToLeaf = new HashMap<>();
        final ArrayList<Variable> variables = new ArrayList<>();
        final Stack<VTree> stack = new Stack<>();
        stack.push(rootNode);
        while (!stack.isEmpty()) {
            final VTree current = stack.pop();
            if (vTreeUsages.containsKey(current)) {
                vTreeUsages.put(current, vTreeUsages.get(current) + 1);
            } else {
                vTreeUsages.put(current, 1);
            }
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
        vTreeRootUsages.put(newRoot.getRoot(), 1);
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
        if (!vTreeRootUsages.containsKey(vTree.getRoot())) {
            return;
        }
        final int newRootRef = vTreeRootUsages.get(vTree.getRoot()) - 1;
        if (newRootRef > 0) {
            vTreeRootUsages.put(vTree.getRoot(), newRootRef);
            return;
        }

        vTreeRootUsages.remove(vTree.getRoot());
        vTreeRoots.remove(vTree.getRoot());

        final Stack<VTree> stack = new Stack<>();
        stack.push(vTree.getRoot());
        while (!stack.isEmpty()) {
            final VTree current = stack.pop();
            final int newRef = vTreeUsages.get(current) - 1;
            if (newRef <= 0 && !vTreeRootUsages.containsKey(current)) {
                vTreeUsages.remove(current);
                freeVTree(current);
            } else {
                vTreeUsages.put(current, newRef);
            }
            if (current instanceof VTreeInternal) {
                stack.push(((VTreeInternal) current).getLeft());
                stack.push(((VTreeInternal) current).getRight());
            }
        }
    }

    private void freeVTree(final VTree vTree) {
        if (vTree instanceof VTreeInternal) {
            internalVTreeNodes.entrySet().removeIf(e -> e.getValue() == vTree);
        } else {
            leafVTreeNodes.entrySet().removeIf(e -> e.getValue() == vTree);
        }
        final HashSet<SddNode> normalizedSddNodes = vTreeToSddNodes.remove(vTree);
        if (normalizedSddNodes != null) {
            for (final SddNode node : normalizedSddNodes) {
                final int newRef = sddNodeUsages.get(node) - 1;
                if (newRef == 0) {
                    freeSddNode(node);
                    sddNodeUsages.remove(node);
                } else {
                    sddNodeUsages.put(node, newRef);
                }
            }
        }
    }

    private void freeSddNode(final SddNode node) {
        if (node instanceof SddNodeDecomposition) {
            sddDecompositions.entrySet().removeIf(e -> e.getValue() == node);
        } else {
            sddTerminals.entrySet().removeIf(e -> e.getValue() == node);
        }
    }


    public SddNode negate(final SddNode node, final VTreeRoot root) {
        final SddNode cached = negations.get(node);
        if (cached != null) {
            return cached;
        }

        final SddNode nodeNeg;
        if (node instanceof SddNodeDecomposition) {
            final SddNodeDecomposition decomp = node.asDecomposition();
            final VTree vTree = root.getVTree(node);

            //Note: compression is not possible here
            final TreeSet<SddElement> newElements = new TreeSet<>();
            for (final SddElement element : decomp.getElements()) {
                final SddNode subNeg = negate(element.getSub(), root);
                Util.pushNewElement(element.getPrime(), subNeg, vTree, root, newElements);
            }
            nodeNeg = decomposition(newElements, vTree, root);
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

    public <RESULT> RESULT apply(final SddFunction<RESULT> function) {
        return function.apply(this);
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
                "\nvTreeUsages=" + vTreeUsages +
                "\nvTreeRootUsages=" + vTreeRootUsages +
                "\nsddDecompositions=" + sddDecompositions +
                "\nsddTerminals=" + sddTerminals +
                "\nsddNodeUsages=" + sddNodeUsages +
                "\nvTreeToSddNodes=" + vTreeToSddNodes +
                "\n}";
    }
}
