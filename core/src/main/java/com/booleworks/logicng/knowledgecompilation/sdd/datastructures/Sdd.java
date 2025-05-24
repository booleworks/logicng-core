package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import static com.booleworks.logicng.solvers.sat.LngCoreSolver.mkLit;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddApply;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.Util;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCoreSolver;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeStack;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddFunction;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Sdd {
    private final FormulaFactory f;
    private int currentVTreeId;
    private int currentSddId;
    private final HashMap<Pair<VTree, VTree>, VTreeInternal> internalVTreeNodes;
    private final HashMap<Integer, VTreeLeaf> leafVTreeNodes;
    private final VTreeStack vTree;
    private final HashMap<SortedSet<SddElement>, SddNodeDecomposition> sddDecompositions;
    private final HashMap<Integer, SddNodeTerminal> sddTerminals;
    private final SddNodeTerminal verumNode;
    private final SddNodeTerminal falsumNode;
    private final HashMap<Pair<SddNode, SddNode>, SddNode> conjunctions;
    private final HashMap<Pair<SddNode, SddNode>, SddNode> disjunctions;
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
        sddDecompositions = new HashMap<>();
        sddTerminals = new HashMap<>();
        conjunctions = new HashMap<>();
        disjunctions = new HashMap<>();
        vTree = new VTreeStack();
        verumNode = new SddNodeTerminal(0, null, true);
        falsumNode = new SddNodeTerminal(1, null, false);
        var2idx = new HashMap<>();
        idx2var = new ArrayList<>();
        verumNode.setNegation(falsumNode);
        falsumNode.setNegation(verumNode);
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

    public SddNodeTerminal terminal(final VTreeLeaf terminal, final boolean phase) {
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

        newNode.setNegation(newNodeNeg);
        newNodeNeg.setNegation(newNode);
        return newNode;
    }

    public SddNodeDecomposition decomposition(final SortedSet<SddElement> elements) {
        assert Util.elementsCompressed(elements);
        final SddNodeDecomposition cached = sddDecompositions.get(elements);
        if (cached != null) {
            final VTree lca = Util.lcaOfCompressedElements(elements, getVTree());
            cached.updateVTree(lca);
            return cached;
        }
        final VTree vTree = Util.lcaOfCompressedElements(elements, getVTree());
        final TreeSet<SddElement> elementsCopy = new TreeSet<>(elements);
        final SddNodeDecomposition newNode = new SddNodeDecomposition(currentSddId++, vTree, elementsCopy);
        sddDecompositions.put(elementsCopy, newNode);
        return newNode;
    }

    public LngResult<SddNode> conjunction(final SddNode left, final SddNode right, final ComputationHandler handler) {
        return binaryOperation(left, right, SddApplyOperation.CONJUNCTION, handler);
    }

    public SddNode conjunction(final SddNode left, final SddNode right) {
        return conjunction(left, right, NopHandler.get()).getResult();
    }

    public SddNode conjunctionUnsafe(final SddNode left, final SddNode right) {
        assert left != null && right != null;
        if (left.isFalse() || right.isFalse()) {
            return falsum();
        }
        if (left.isTrue()) {
            return right;
        }
        if (right.isTrue()) {
            return left;
        }

        final SddNode cached = lookupApplyComputation(left, right, SddApplyOperation.CONJUNCTION);
        if (cached != null) {
            return cached;
        }

        final TreeSet<SddElement> newElements = new TreeSet<>();
        newElements.add(new SddElement(left, right));
        newElements.add(new SddElement(negate(left), falsum()));
        final SddNode newNode = decomposition(newElements);
        cacheApplyComputation(left, right, newNode, SddApplyOperation.CONJUNCTION);
        return newNode;
    }

    public LngResult<SddNode> disjunction(final SddNode left, final SddNode right, final ComputationHandler handler) {
        return binaryOperation(left, right, SddApplyOperation.DISJUNCTION, handler);
    }

    public SddNode disjunction(final SddNode left, final SddNode right) {
        return disjunction(left, right, NopHandler.get()).getResult();
    }

    public LngResult<SddNode> binaryOperation(final SddNode left, final SddNode right, final SddApplyOperation op,
                                              final ComputationHandler handler) {
        final SddNode cached = lookupApplyComputation(left, right, op);
        if (cached != null) {
            return LngResult.of(cached);
        }
        final LngResult<SddNode> result = SddApply.apply(left, right, op, this, handler);
        if (!result.isSuccess()) {
            return result;
        }
        cacheApplyComputation(left, right, result.getResult(), op);
        return result;
    }

    public SddNode binaryOperation(final SddNode left, final SddNode right, final SddApplyOperation op) {
        return binaryOperation(left, right, op, NopHandler.get()).getResult();
    }

    private SddNode lookupApplyComputation(final SddNode left, final SddNode right, final SddApplyOperation op) {
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

    private void cacheApplyComputation(final SddNode left, final SddNode right, final SddNode result,
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

    public void defineVTree(final VTree vTree) {
        final VTreeRoot root = constructRoot(vTree);
        this.vTree.initialize(root);
    }

    public VTreeRoot constructRoot(final VTree rootNode) {
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
        return new VTreeRoot(parents, rootNode, positions, pos2vtree);
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

    public VTreeRoot getVTree() {
        return vTree.getActive();
    }

    public VTreeStack getVTreeStack() {
        return vTree;
    }

    public SddNode negate(final SddNode node) {
        final SddNode cached = node.getNegation();
        if (cached != null) {
            return cached;
        }

        final SddNode nodeNeg;
        if (node.isDecomposition()) {
            final SddNodeDecomposition decomp = node.asDecomposition();
            //Note: compression is not possible here
            final TreeSet<SddElement> newElements = new TreeSet<>();
            for (final SddElement element : decomp.getElements()) {
                final SddNode subNeg = negate(element.getSub());
                Util.pushNewElement(element.getPrime(), subNeg, newElements);
            }
            nodeNeg = decomposition(newElements);
        } else if (node.isLiteral()) {
            final SddNodeTerminal t = node.asTerminal();
            nodeNeg = terminal(t.getVTree().asLeaf(), !t.getPhase());
        } else {
            final SddNodeTerminal t = node.asTerminal();
            nodeNeg = t.getPhase() ? falsum() : verum();
        }
        node.setNegation(nodeNeg);
        nodeNeg.setNegation(node);
        return nodeNeg;
    }

    public SddNode getNegationIfCached(final SddNode node) {
        return node.getNegation();
    }

    public <RESULT> RESULT apply(final SddFunction<RESULT> function) {
        return function.apply(this);
    }

    public <RESULT> LngResult<RESULT> apply(final SddFunction<RESULT> function, final ComputationHandler handler) {
        return function.apply(this, handler);
    }

    public void pin(final SddNode node) {
        if (node.isDecomposition()) {
            node.asDecomposition().ref();
            vTree.getActive().pin(node);
        }
    }

    public void unpin(final SddNode node) {
        if (node.isDecomposition()) {
            node.asDecomposition().deref();
            vTree.getActive().unpin(node);
        }
    }

    public void garbageCollectAll() {
        final List<SddNode> unusedNodes = sddDecompositions.values().stream()
                .filter(d -> d.getRefs() == 0)
                .collect(Collectors.toList());
        garbageCollectSelection(unusedNodes);
    }

    public void garbageCollectSelection(final Collection<SddNode> nodes) {
        final HashSet<Integer> idsToRemove = new HashSet<>();
        final Queue<SddNode> children = new ArrayDeque<>(nodes);
        while (!children.isEmpty()) {
            final SddNode node = children.poll();
            if (node.isDecomposition() && node.asDecomposition().getRefs() == 0) {
                node.asDecomposition().free();
                idsToRemove.add(node.getId());
                for (final SddElement element : node.asDecomposition().getElements()) {
                    final SddNode prime = element.getPrime();
                    final SddNode sub = element.getSub();
                    if (prime.isDecomposition() && prime.asDecomposition().getRefs() == 0) {
                        children.add(prime.asDecomposition());
                    }
                    if (sub.isDecomposition() && sub.asDecomposition().getRefs() == 0) {
                        children.add(sub.asDecomposition());
                    }
                }
            }
        }
        sddDecompositions.entrySet().removeIf(e -> idsToRemove.contains(e.getValue().id));
        conjunctions.entrySet().removeIf(
                e -> idsToRemove.contains(e.getValue().id) || idsToRemove.contains(e.getKey().getFirst().id)
                        || idsToRemove.contains(e.getKey().getSecond().id));
        disjunctions.entrySet().removeIf(
                e -> idsToRemove.contains(e.getValue().id) || idsToRemove.contains(e.getKey().getFirst().id)
                        || idsToRemove.contains(e.getKey().getSecond().id));
    }

    public int getSddNodeCount() {
        return sddTerminals.size() + sddDecompositions.size();
    }

    public int getTerminalCount() {
        return sddTerminals.size();
    }

    public int getDecompositionCount() {
        return sddDecompositions.size();
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
                "\nsddDecompositions=" + sddDecompositions +
                "\nsddTerminals=" + sddTerminals +
                "\n}";
    }
}
