package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import static com.booleworks.logicng.solvers.sat.LngCoreSolver.mkLit;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.Util;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCoreSolver;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddFunction;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

public class Sdd {
    private final FormulaFactory f;
    private int currentVTreeId;
    private int currentSddId;
    private final HashMap<Pair<VTree, VTree>, VTreeInternal> internalVTreeNodes;
    private final HashMap<Integer, VTreeLeaf> leafVTreeNodes;
    private final HashMap<VTree, VTreeRoot> vTreeRoots;
    private final HashMap<TreeSet<SddElement>, SddNodeDecomposition> sddDecompositions;
    private final HashMap<Integer, SddNodeTerminal> sddTerminals;
    private final SddNodeTerminal verumNode;
    private final SddNodeTerminal falsumNode;
    private final HashMap<SddNode, SddNode> negations;
    private final HashMap<Pair<SddNode, SddNode>, SddNode> conjunctions;
    private final HashMap<Pair<SddNode, SddNode>, SddNode> disjunctions;
    private final HashMap<SddNode, SortedSet<Integer>> variables;
    private final SddCoreSolver solver;
    private final HashMap<Variable, Integer> var2idx;
    private final ArrayList<Variable> idx2var;

    private Sdd(final FormulaFactory f, final SddCoreSolver solver) {
        this.f = f;
        this.solver = solver;
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
        verumNode = new SddNodeTerminal(0, null, true);
        falsumNode = new SddNodeTerminal(1, null, false);
        variables = new HashMap<>();
        var2idx = new HashMap<>();
        idx2var = new ArrayList<>();
        negations.put(verumNode, falsumNode);
        negations.put(falsumNode, verumNode);
    }

    public static Sdd solverBased(final SddCoreSolver solver) {
        return new Sdd(solver.f(), solver);
    }

    public static Sdd independent(final FormulaFactory f) {
        return new Sdd(f, null);
    }

    public Variable indexToVariable(final int index) {
        if (solver == null) {
            if (index < idx2var.size()) {
                return idx2var.get(index);
            } else {
                return null;
            }
        } else {
            return f.variable(solver.nameForIdx(index));
        }
    }

    public int variableToIndex(final Variable variable) {
        if (solver == null) {
            final Integer idx = var2idx.get(variable);
            return Objects.requireNonNullElse(idx, -1);
        } else {
            return solver.idxForName(variable.getName());
        }
    }

    public boolean knows(final Variable variable) {
        if (solver == null) {
            return var2idx.containsKey(variable);
        } else {
            return solver.idxForName(variable.getName()) != -1;
        }
    }

    public VTreeLeaf vTreeLeaf(final Variable variable) {
        int idx = variableToIndex(variable);
        if (idx == -1) {
            idx = idx2var.size();
            idx2var.add(variable);
            var2idx.put(variable, idx);
        }
        return vTreeLeaf(variableToIndex(variable));
    }

    public VTreeLeaf vTreeLeaf(final int variable) {
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

    public SddNodeTerminal terminal(final VTreeLeaf terminal, final boolean phase, final VTreeRoot root) {
        final int litIdx = mkLit(terminal.getVariable(), !phase);
        final SddNodeTerminal cached = sddTerminals.get(litIdx);
        if (cached != null) {
            return cached;
        }
        final SddNodeTerminal newNode = new SddNodeTerminal(currentSddId++, terminal, phase);
        sddTerminals.put(litIdx, newNode);

        final int negTerminal = mkLit(terminal.getVariable(), phase);
        final SddNodeTerminal newNodeNeg = new SddNodeTerminal(currentSddId++, terminal, !phase);
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
            }
        }
        final HashMap<VTree, Integer> positions = new HashMap<>();
        final HashMap<Integer, VTree> pos2vtree = new HashMap<>();
        calculateInorder(rootNode, 0, positions, pos2vtree);
        final VTreeRoot newRoot = new VTreeRoot(parents, rootNode, positions, pos2vtree);
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
        if (node.isDecomposition()) {
            final SddNodeDecomposition decomp = node.asDecomposition();
            //Note: compression is not possible here
            final TreeSet<SddElement> newElements = new TreeSet<>();
            for (final SddElement element : decomp.getElements()) {
                final SddNode subNeg = negate(element.getSub(), root);
                Util.pushNewElement(element.getPrime(), subNeg, newElements);
            }
            nodeNeg = decomposition(newElements, root);
        } else if (node.isLiteral()) {
            final SddNodeTerminal t = node.asTerminal();
            nodeNeg = terminal(t.getVTree().asLeaf(), !t.getPhase(), root);
        } else {
            final SddNodeTerminal t = node.asTerminal();
            nodeNeg = t.getPhase() ? falsum() : verum();
        }
        negations.put(node, nodeNeg);
        negations.put(nodeNeg, node);
        return nodeNeg;
    }

    public SortedSet<Integer> variables(final SddNode node) {
        final SortedSet<Integer> cached = this.variables.get(node);
        if (cached != null) {
            return cached;
        }
        final SortedSet<Integer> variables = new TreeSet<>();
        if (node.isDecomposition()) {
            for (final SddElement element : node.asDecomposition().getElements()) {
                variables.addAll(variables(element.getPrime()));
                variables.addAll(variables(element.getSub()));
            }
        } else if (node.isLiteral()) {
            variables.add(node.asTerminal().getVTree().getVariable());
        }
        this.variables.put(node, variables);
        return variables;
    }

    public SddNode getNegationIfCached(final SddNode node) {
        return negations.get(node);
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
