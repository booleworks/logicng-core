package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.CompactModel;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SddModelEnumeration implements SddFunction<List<Model>> {
    private final SddNode originalNode;
    private final VTreeRoot root;
    private final Set<Variable> variables;

    public SddModelEnumeration(final Collection<Variable> variables, final SddNode originalNode, final VTreeRoot root) {
        this.originalNode = originalNode;
        this.root = root;
        this.variables = new HashSet<>(variables);
    }

    @Override
    public LngResult<List<Model>> apply(final SddFactory sf, final ComputationHandler handler) {
        final LngResult<List<CompactModel>> compactResult = applyNoExpand(sf, handler);
        if (!compactResult.isSuccess()) {
            return LngResult.canceled(compactResult.getCancelCause());
        }
        final List<CompactModel> compact = compactResult.getResult();
        final List<Model> expanded = new ArrayList<>();
        for (final CompactModel model : compact) {
            expanded.addAll(model.expand());
        }
        return LngResult.of(expanded);
    }

    public LngResult<List<CompactModel>> applyNoExpand(final SddFactory sf, final ComputationHandler handler) {
        final LngResult<SortedSet<Variable>> sddVariablesResult =
                sf.apply(new SddVariablesFunction(originalNode), handler);
        if (!sddVariablesResult.isSuccess()) {
            return LngResult.canceled(sddVariablesResult.getCancelCause());
        }
        final SortedSet<Variable> sddVariables = sddVariablesResult.getResult();
        if (!variables.containsAll(sddVariables)) {
            throw new IllegalArgumentException(
                    "Model Counting variables must be a superset of the variables contained on the SDD");
        }
        if (originalNode.isTrue()) {
            return LngResult.of(List.of(new CompactModel(List.of(), List.of())));
        }
        if (originalNode.isFalse()) {
            return LngResult.of(List.of());
        }

        final SortedSet<Variable> variablesInVTree = new TreeSet<>();
        VTreeUtil.vars(originalNode.getVTree(), variables, variablesInVTree);
        final Set<Variable> variablesNotInVTree = variables
                .stream()
                .filter(v -> !variablesInVTree.contains(v))
                .collect(Collectors.toSet());

        final NodePC producer = buildPCNode(originalNode, new HashMap<>(), new HashMap<>());
        final List<CompactModel> models = new ArrayList<>();
        producer.consumerRoot = models;
        while (!producer.isDone()) {
            producer.produce();
        }
        final List<CompactModel> modelsWithDontCares = new ArrayList<>(models.size());
        for (final CompactModel model : models) {
            modelsWithDontCares.add(model.withDontCare(variablesNotInVTree));
        }
        return LngResult.of(modelsWithDontCares);
    }

    private NodePC buildPCNode(final SddNode node, final HashMap<SddNode, NodePC> nodeCache,
                               final HashMap<SddElement, ElementPC> elementCache) {
        final NodePC cached = nodeCache.get(node);
        if (cached != null) {
            return cached;
        }
        final NodePC nodePC = new NodePC(node);
        if (node.isDecomposition()) {
            for (final SddElement element : node.asDecomposition().getElements()) {
                final ElementPC pc = buildPCElement(element, nodeCache, elementCache);
                if (pc != null) {
                    nodePC.producers.add(pc);
                    pc.consumers.add(nodePC);
                }
            }
            assert !nodePC.producers.isEmpty();
        }
        nodeCache.put(node, nodePC);
        return nodePC;
    }

    private ElementPC buildPCElement(final SddElement element, final HashMap<SddNode, NodePC> nodeCache,
                                     final HashMap<SddElement, ElementPC> elementCache) {
        final ElementPC cached = elementCache.get(element);
        if (cached != null) {
            return cached;
        }
        if (element.getSub().isFalse()) {
            return null;
        }
        final NodePC prime = buildPCNode(element.getPrime(), nodeCache, elementCache);
        final NodePC sub;
        if (element.getSub().isTrue()) {
            sub = null;
        } else {
            sub = buildPCNode(element.getSub(), nodeCache, elementCache);
        }
        final VTree primeTree = prime.node.getVTree();
        final ElementPC elementPC;
        if (sub == null) {
            elementPC = new ElementPC(prime, null, primeTree, null, primeTree);
        } else {
            final VTreeInternal lca = root.lcaOf(primeTree, sub.node.getVTree()).asInternal();
            elementPC = new ElementPC(prime, sub, lca.getLeft(), lca.getRight(), lca);
        }
        prime.consumersAsPrime.add(elementPC);
        if (sub != null) {
            sub.consumersAsSub.add(elementPC);
        }
        elementCache.put(element, elementPC);
        return elementPC;
    }

    private class ElementPC {
        private final VTree primeVTree;
        private final VTree subVTree;
        private final VTree targetVTree;
        private final NodePC prime;
        private final NodePC sub;
        private final List<NodePC> consumers = new ArrayList<>();
        private List<CompactModel> allPrimeResults = new ArrayList<>();
        private List<CompactModel> currentSubResults = new ArrayList<>();

        public ElementPC(final NodePC prime, final NodePC sub, final VTree primeVTree, final VTree subVTree,
                         final VTree targetVTree) {
            this.prime = prime;
            this.sub = sub;
            this.primeVTree = primeVTree;
            this.subVTree = subVTree;
            this.targetVTree = targetVTree;

            if (sub == null) {
                allPrimeResults = new ArrayList<>();
            }
        }

        private void consumePrime(final List<CompactModel> models, final VTree producerVTree) {
            final List<CompactModel> extended = extendModels(models, producerVTree, primeVTree);
            allPrimeResults.addAll(extended);
        }

        private void consumeSub(final List<CompactModel> models, final VTree producerVTree) {
            final List<CompactModel> extended = extendModels(models, producerVTree, subVTree);
            currentSubResults.addAll(extended);
        }

        private void produce() {
            if (isDone()) {
                throw new IllegalStateException("Producer called after it was done");
            }
            if (prime == null) {
                if (currentSubResults.isEmpty()) {
                    sub.produce();
                }
                push(currentSubResults);
                currentSubResults = new ArrayList<>();
            } else if (sub == null) {
                if (allPrimeResults.isEmpty()) {
                    prime.produce();
                }
                push(allPrimeResults);
                allPrimeResults = new ArrayList<>();
            } else {
                while (!prime.isDone()) {
                    prime.produce();
                }
                assert !allPrimeResults.isEmpty();
                if (currentSubResults.isEmpty()) {
                    sub.produce();
                }
                final List<CompactModel> models = new ArrayList<>();
                for (final CompactModel subRes : currentSubResults) {
                    for (final CompactModel primeRes : allPrimeResults) {
                        final CompactModel merged =
                                subRes.with(primeRes.getLiterals(), primeRes.getDontCareVariables());
                        models.add(merged);
                    }
                }
                currentSubResults = new ArrayList<>();
                if (sub.isDone()) {
                    allPrimeResults = null;
                }
                push(models);
            }
        }

        private boolean isDone() {
            return (prime == null || prime.isDone()) && (sub == null || (currentSubResults.isEmpty() && sub.isDone()));
        }

        private void push(final List<CompactModel> models) {
            for (final NodePC consumer : consumers) {
                consumer.consume(models, targetVTree);
            }
        }
    }

    private class NodePC {
        private final SddNode node;
        private List<CompactModel> consumerRoot = null;
        private final List<ElementPC> consumersAsPrime = new ArrayList<>();
        private final List<ElementPC> consumersAsSub = new ArrayList<>();
        private final List<ElementPC> producers = new ArrayList<>();
        private ArrayList<CompactModel> currentModels = new ArrayList<>();
        private int currentProducer = 0;

        public NodePC(final SddNode node) {
            this.node = node;
        }

        private void consume(final List<CompactModel> models, final VTree producerVTree) {
            final List<CompactModel> extended = extendModels(models, producerVTree, node.getVTree());
            currentModels.addAll(extended);
        }

        private void produce() {
            if (isDone()) {
                throw new IllegalStateException("Producer called after it was done");
            }
            if (node.isLiteral()) {
                final CompactModel model =
                        new CompactModel(List.of((Literal) node.asTerminal().getTerminal()), List.of());
                push(List.of(model));
                currentProducer = 1;
            } else {
                final ElementPC p = producers.get(currentProducer);
                if (currentModels.isEmpty()) {
                    assert !p.isDone();
                    p.produce();
                }
                while (currentProducer < producers.size() && producers.get(currentProducer).isDone()) {
                    ++currentProducer;
                }
                push(currentModels);
                currentModels = new ArrayList<>();
            }
        }

        private boolean isDone() {
            return (node.isDecomposition() && currentProducer >= producers.size()) || (!node.isDecomposition()
                    && currentProducer == 1);
        }

        private void push(final List<CompactModel> models) {
            if (consumerRoot != null) {
                consumerRoot.addAll(models);
            }
            for (final ElementPC consumer : consumersAsPrime) {
                consumer.consumePrime(models, node.getVTree());
            }
            for (final ElementPC consumer : consumersAsSub) {
                consumer.consumeSub(models, node.getVTree());
            }
        }
    }

    private List<CompactModel> extendModels(final List<CompactModel> models, final VTree usedVTree,
                                            final VTree targetVTree) {
        final SortedSet<Variable> gapVars = new TreeSet<>();
        VTreeUtil.gapVars(targetVTree, usedVTree, root, variables, gapVars);
        final List<CompactModel> extended = new ArrayList<>(models.size());
        for (final CompactModel model : models) {
            extended.add(model.withDontCare(gapVars));
        }
        return extended;
    }
}
