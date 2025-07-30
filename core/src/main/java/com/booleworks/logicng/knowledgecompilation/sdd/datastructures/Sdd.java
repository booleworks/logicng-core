package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import static com.booleworks.logicng.solvers.sat.LngCoreSolver.mkLit;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.sdd.SddApplyOperation;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddApply;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCoreSolver;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeStack;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

public final class Sdd {
    private final FormulaFactory f;
    private int currentVTreeId;
    private int currentSddId;
    private final HashMap<Pair<VTree, VTree>, VTreeInternal> internalVTreeNodes;
    private final HashMap<Integer, VTreeLeaf> leafVTreeNodes;
    private final VTreeStack vTreeStack;
    private final HashMap<ArrayList<SddElement>, SddNodeDecomposition> sddDecompositions;
    private final HashMap<Integer, SddNodeTerminal> sddTerminals;
    private final SddNodeTerminal verumNode;
    private final SddNodeTerminal falsumNode;
    private final HashMap<Pair<SddNode, SddNode>, GSCacheEntry<SddNode>> conjunctions;
    private final HashMap<Pair<SddNode, SddNode>, GSCacheEntry<SddNode>> disjunctions;
    private final SddCoreSolver solver;
    private final HashMap<Variable, Integer> var2idx;
    private final ArrayList<Variable> idx2var;

    private final VSCacheEntry<Integer> activeSize;

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
        vTreeStack = new VTreeStack();
        verumNode = new SddNodeTerminal(0, invariantGSCacheEntry(null), true);
        falsumNode = new SddNodeTerminal(1, invariantGSCacheEntry(null), false);
        var2idx = new HashMap<>();
        idx2var = new ArrayList<>();
        verumNode.setNegationEntry(invariantGSCacheEntry(falsumNode));
        falsumNode.setNegationEntry(invariantGSCacheEntry(verumNode));
        activeSize = new VSCacheEntry<>(0);
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
        final SddNodeTerminal newNode = new SddNodeTerminal(currentSddId++, invariantGSCacheEntry(terminal), phase);
        sddTerminals.put(litIdx, newNode);

        final int negTerminal = mkLit(terminal.getVariable(), phase);
        final SddNodeTerminal newNodeNeg = new SddNodeTerminal(currentSddId++, invariantGSCacheEntry(terminal), !phase);
        sddTerminals.put(negTerminal, newNodeNeg);

        newNode.setNegationEntry(invariantGSCacheEntry(newNodeNeg));
        newNodeNeg.setNegationEntry(invariantGSCacheEntry(newNode));
        return newNode;
    }

    public SddNode decompOfPartition(final ArrayList<SddElement> newElements) {
        return decompOfPartition(newElements, NopHandler.get()).getResult();
    }

    public LngResult<SddNode> decompOfPartition(final ArrayList<SddElement> newElements,
                                                final ComputationHandler handler) {
        newElements.sort(SddElement::compareTo);
        final LngResult<Pair<SddNode, ArrayList<SddElement>>> res =
                SddCompression.compressAndTrim(newElements, this, handler);
        if (!res.isSuccess()) {
            return LngResult.canceled(res.getCancelCause());
        }
        if (res.getResult().getFirst() != null) {
            return LngResult.of(res.getResult().getFirst());
        } else {
            return LngResult.of(decomposition(res.getResult().getSecond()));
        }
    }

    public SddNode decompOfCompressedPartition(final ArrayList<SddElement> newElements) {
        assert !newElements.isEmpty();
        newElements.sort(SddElement::compareTo);
        assert SddCompression.isCompressed(newElements);
        return decomposition(newElements);
    }

    private SddNodeDecomposition decomposition(final ArrayList<SddElement> elements) {
        final SddNodeDecomposition cached = sddDecompositions.get(elements);
        if (cached != null) {
            return cached;
        }
        final VTree vTree = SddUtil.lcaOfCompressedElements(elements, this);
        final SddNodeDecomposition newNode =
                new SddNodeDecomposition(currentSddId++, new GSCacheEntry<>(vTree), elements);
        sddDecompositions.put(elements, newNode);
        return newNode;
    }

    public VTree vTreeOf(final SddNode node) {
        final GSCacheEntry<VTree> entry = node.getVTreeEntry();
        if (entry.isValid()) {
            return entry.getElement();
        } else if (node.isDecomposition()) {
            final VTree newVTree = SddUtil.lcaOfCompressedElements(node.asDecomposition().getElementsUnsafe(), this);
            entry.update(newVTree);
            return entry.getElement();
        } else {
            entry.update(entry.getElement());
            return entry.getElement();
        }
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

        final GSCacheEntry<SddNode> cached = lookupApplyComputation(left, right, SddApplyOperation.CONJUNCTION);
        if (cached != null && cached.isValid()) {
            return cached.getElement();
        }

        final ArrayList<SddElement> newElements = new ArrayList<>();
        newElements.add(new SddElement(left, right));
        newElements.add(new SddElement(negate(left), falsum()));
        final SddNode newNode = decompOfCompressedPartition(newElements);
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
        final GSCacheEntry<SddNode> cached = lookupApplyComputation(left, right, op);
        if (cached != null && cached.isValid()) {
            return LngResult.of(cached.getElement());
        }
        final LngResult<SddNode> result = SddApply.apply(left, right, op, this, handler);
        if (!result.isSuccess()) {
            return result;
        }
        final SddNode newNode = result.getResult();
        cacheApplyComputation(left, right, newNode, op);
        return result;
    }

    public SddNode binaryOperation(final SddNode left, final SddNode right, final SddApplyOperation op) {
        return binaryOperation(left, right, op, NopHandler.get()).getResult();
    }

    private GSCacheEntry<SddNode> lookupApplyComputation(final SddNode left, final SddNode right,
                                                         final SddApplyOperation op) {
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
                if (conjunctions.containsKey(key)) {
                    conjunctions.get(key).update(result);
                } else {
                    conjunctions.put(key, new GSCacheEntry<>(result));
                }
                return;
            }
            case DISJUNCTION: {
                final Pair<SddNode, SddNode> key =
                        left.getId() < right.getId() ? new Pair<>(left, right) : new Pair<>(right, left);
                if (disjunctions.containsKey(key)) {
                    disjunctions.get(key).update(result);
                } else {
                    disjunctions.put(key, new GSCacheEntry<>(result));
                }
                return;
            }
            default:
                throw new RuntimeException("Unknown operation type");
        }
    }

    public void defineVTree(final VTree vTree) {
        final VTreeRoot root = constructRoot(vTree);
        this.vTreeStack.initialize(root);
    }

    public VTreeRoot constructRoot(final VTree rootNode) {
        return new VTreeRoot(rootNode);
    }

    public VTreeRoot getVTree() {
        return vTreeStack.getActive();
    }

    public VTreeStack getVTreeStack() {
        return vTreeStack;
    }

    public boolean isVTreeDefined() {
        return !vTreeStack.isEmpty();
    }

    public SddNode negate(final SddNode node) {
        final GSCacheEntry<SddNode> cached = node.getNegationEntry();
        if (cached != null && cached.isValid()) {
            return cached.getElement();
        }

        final SddNode nodeNeg;
        if (node.isDecomposition()) {
            final SddNodeDecomposition decomp = node.asDecomposition();
            //Note: compression is not possible here
            final ArrayList<SddElement> newElements = new ArrayList<>();
            for (final SddElement element : decomp) {
                final SddNode subNeg = negate(element.getSub());
                newElements.add(new SddElement(element.getPrime(), subNeg));
            }
            nodeNeg = decompOfCompressedPartition(newElements);
        } else if (node.isLiteral()) {
            final SddNodeTerminal t = node.asTerminal();
            nodeNeg = terminal(t.getVTree(), !t.getPhase());
        } else {
            final SddNodeTerminal t = node.asTerminal();
            nodeNeg = t.getPhase() ? falsum() : verum();
        }
        if (node.getNegationEntry() == null) {
            node.setNegationEntry(new GSCacheEntry<>(nodeNeg));
        } else {
            node.getNegationEntry().update(nodeNeg);
        }
        if (nodeNeg.getNegationEntry() == null) {
            nodeNeg.setNegationEntry(new GSCacheEntry<>(node));
        } else {
            nodeNeg.getNegationEntry().update(node);
        }
        return nodeNeg;
    }

    public SddNode getNegationIfCached(final SddNode node) {
        if (node.getNegationEntry() == null || !node.getNegationEntry().isValid()) {
            return null;
        } else {
            return node.getNegationEntry().getElement();
        }
    }

    public void pin(final SddNode node) {
        if (node.isDecomposition()) {
            vTreeStack.pin(node.asDecomposition());
        }
    }

    public void unpin(final SddNode node) {
        if (node.isDecomposition()) {
            vTreeStack.unpin(node.asDecomposition());
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
                for (final SddElement element : node.asDecomposition()) {
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
        sddDecompositions.entrySet().removeIf(
                e -> idsToRemove.contains(e.getValue().id));
        conjunctions.entrySet().removeIf(e ->
                idsToRemove.contains(e.getValue().getElement().id)
                        || idsToRemove.contains(e.getKey().getFirst().id)
                        || idsToRemove.contains(e.getKey().getSecond().id));
        disjunctions.entrySet().removeIf(e ->
                idsToRemove.contains(e.getValue().getElement().id)
                        || idsToRemove.contains(e.getKey().getFirst().id)
                        || idsToRemove.contains(e.getKey().getSecond().id));
    }

    public int getActiveSize() {
        if (activeSize.isValid()) {
            return activeSize.getElement();
        } else {
            int size = 0;
            for (final SddNode node : getVTree().getPinnedNodes()) {
                if (node.getSizeEntry() == null || !node.getSizeEntry().isValid()) {
                    size += getActiveSize(node);
                }
            }
            activeSize.update(size);
            return size;
        }
    }

    private int getActiveSize(final SddNode node) {
        if (node.getSizeEntry() != null && node.getSizeEntry().isValid()) {
            return 0;
        } else {
            int size = 1;
            if (node.isDecomposition()) {
                for (final SddElement element : node.asDecomposition()) {
                    if (element.getPrime().getSizeEntry() == null || !element.getPrime().getSizeEntry().isValid()) {
                        size += getActiveSize(element.getPrime());
                    }
                    if (element.getSub().getSizeEntry() == null || !element.getSub().getSizeEntry().isValid()) {
                        size += getActiveSize(element.getSub());
                    }
                }
            }
            if (node.getSizeEntry() == null) {
                node.setSizeEntry(new VSCacheEntry<>(size));
            } else {
                node.getSizeEntry().update(size);
            }
            return size;
        }
    }

    public Collection<SddNodeDecomposition> getDecompositionNodes() {
        return Collections.unmodifiableCollection(sddDecompositions.values());
    }

    public Collection<SddNodeTerminal> getTerminalNodes() {
        return Collections.unmodifiableCollection(sddTerminals.values());
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

    private <E> GSCacheEntry<E> invariantGSCacheEntry(final E element) {
        final GSCacheEntry<E> entry = new GSCacheEntry<>(element);
        entry.generation = -1;
        return entry;
    }

    private <E> VSCacheEntry<E> invariantPSCacheEntry(final E element) {
        final VSCacheEntry<E> entry = new VSCacheEntry<>(element);
        entry.version = -1;
        return entry;
    }

    class GSCacheEntry<T> {
        int generation;
        T element;

        GSCacheEntry(final T element) {
            this.generation = getVTreeStack().getGeneration();
            this.element = element;
        }

        int getGeneration() {
            return generation;
        }

        T getElement() {
            return element;
        }

        void update(final T element) {
            assert generation != -1;
            this.element = element;
            this.generation = getVTreeStack().getGeneration();
        }

        boolean isValid() {
            return generation == -1 || generation == vTreeStack.getGeneration();
        }
    }

    class VSCacheEntry<T> {
        int version;
        T element;

        VSCacheEntry(final T element) {
            this.version = getVTreeStack().getVersion();
            this.element = element;
        }

        int getVersion() {
            return version;
        }

        T getElement() {
            return element;
        }

        void update(final T element) {
            assert version != -1;
            this.element = element;
            this.version = getVTreeStack().getVersion();
        }

        boolean isValid() {
            return version == -1 || version == vTreeStack.getVersion();
        }
    }
}
