package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
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
        final LngResult<SortedSet<Variable>> sddVariablesResult =
                sf.apply(new SddVariablesFunction(originalNode), handler);
        if (!sddVariablesResult.isSuccess()) {
            return LngResult.canceled(sddVariablesResult.getCancelCause());
        }
        if (!variables.containsAll(sddVariablesResult.getResult())) {
            throw new IllegalArgumentException(
                    "Model Enumeration variables must be a superset of the variables contained on the SDD");
        }
        final SortedSet<Variable> variablesInVTree = new TreeSet<>();
        VTreeUtil.vars(root.getVTree(originalNode), variables, variablesInVTree);
        final Set<Variable> variablesNotInVTree = variables
                .stream()
                .filter(v -> !variablesInVTree.contains(v))
                .collect(Collectors.toSet());

        final NodePC producer = buildPCNode(originalNode, new HashMap<>(), new HashMap<>());
        final List<List<Literal>> models = new ArrayList<>();
        producer.consumerRoot = models;
        while (!producer.isDone()) {
            producer.produce();
        }
        final List<List<Literal>> extended = extendModels(models, variablesNotInVTree);
        return LngResult.of(extended.stream().map(Model::new).collect(Collectors.toList()));
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
        final VTree primeTree = root.getVTree(prime.node);
        final ElementPC elementPC;
        if (sub == null) {
            elementPC = new ElementPC(prime, null, primeTree, null, primeTree);
        } else {
            final VTreeInternal lca = root.lcaOf(primeTree, root.getVTree(sub.node)).asInternal();
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
        private List<List<Literal>> allPrimeResults = null;
        private List<List<Literal>> currentSubResults = new ArrayList<>();

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

        private void consumePrime(final List<List<Literal>> models, final VTree producerVTree) {
            final List<List<Literal>> extendedModels = extendModels(models, producerVTree, primeVTree);
            allPrimeResults.addAll(extendedModels);
        }

        private void consumeSub(final List<List<Literal>> models, final VTree producerVTree) {
            final List<List<Literal>> extendedModels = extendModels(models, producerVTree, subVTree);
            currentSubResults.addAll(extendedModels);
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
                if (allPrimeResults == null) {
                    allPrimeResults = new ArrayList<>();
                    while (!prime.isDone()) {
                        prime.produce();
                    }
                }
                assert !allPrimeResults.isEmpty();
                if (currentSubResults.isEmpty()) {
                    sub.produce();
                }
                final List<List<Literal>> models = new ArrayList<>();
                for (final List<Literal> subRes : currentSubResults) {
                    final List<Literal> model = new ArrayList<>(subRes);
                    for (final List<Literal> primeRes : allPrimeResults) {
                        model.addAll(primeRes);
                    }
                    models.add(model);
                }
                currentSubResults = new ArrayList<>();
                if (sub.isDone()) {
                    allPrimeResults = null;
                }
                push(models);
            }
        }

        private boolean isDone() {
            return (prime == null || prime.isDone()) && (sub == null || sub.isDone());
        }

        private void push(final List<List<Literal>> models) {
            for (final NodePC consumer : consumers) {
                consumer.consume(models, targetVTree);
            }
        }
    }

    private class NodePC {
        private final SddNode node;
        private List<List<Literal>> consumerRoot = null;
        private final List<ElementPC> consumersAsPrime = new ArrayList<>();
        private final List<ElementPC> consumersAsSub = new ArrayList<>();
        private final List<ElementPC> producers = new ArrayList<>();
        private ArrayList<List<Literal>> currentModels = new ArrayList<>();
        private int currentProducer = 0;

        public NodePC(final SddNode node) {
            this.node = node;
        }

        private void consume(final List<List<Literal>> models, final VTree producerVTree) {
            final List<List<Literal>> extendedModels = extendModels(models, producerVTree, root.getVTree(node));
            currentModels.addAll(extendedModels);
        }

        private void produce() {
            if (isDone()) {
                throw new IllegalStateException("Producer called after it was done");
            }
            if (node.isLiteral()) {
                push(List.of(List.of((Literal) node.asTerminal().getTerminal())));
                currentProducer = 1;
            } else {
                final ElementPC p = producers.get(currentProducer);
                p.produce();
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

        private void push(final List<List<Literal>> models) {
            if (consumerRoot != null) {
                consumerRoot.addAll(models);
            }
            for (final ElementPC consumer : consumersAsPrime) {
                consumer.consumePrime(models, root.getVTree(node));
            }
            for (final ElementPC consumer : consumersAsSub) {
                consumer.consumeSub(models, root.getVTree(node));
            }
        }
    }

    private List<List<Literal>> extendModels(final List<List<Literal>> models, final VTree usedVTree,
                                             final VTree targetVTree) {
        final SortedSet<Variable> gapVars = new TreeSet<>();
        VTreeUtil.gapVars(targetVTree, usedVTree, root, variables, gapVars);
        return extendModels(models, gapVars);
    }

    private List<List<Literal>> extendModels(final List<List<Literal>> models, final Set<Variable> variables) {
        List<List<Literal>> result = models;
        for (final Variable var : variables) {
            final List<List<Literal>> extended = new ArrayList<>(result.size() * 2);
            for (final List<Literal> literals : result) {
                extended.add(extendedByLiteral(literals, var));
                extended.add(extendedByLiteral(literals, var.negate(var.getFactory())));
            }
            result = extended;
        }
        return result;

    }

    private static List<Literal> extendedByLiteral(final List<Literal> literals, final Literal lit) {
        final ArrayList<Literal> extended = new ArrayList<>(literals);
        extended.add(lit);
        return extended;
    }

}
