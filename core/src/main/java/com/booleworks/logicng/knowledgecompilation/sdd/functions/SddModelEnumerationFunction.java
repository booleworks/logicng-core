package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.EnumerationFoundModelsEvent;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddEvaluation;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.VTreeUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.CompactModel;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SddModelEnumerationFunction implements SddFunction<List<Model>> {
    private final Sdd sdd;
    private final Set<Variable> variables;
    private final Set<Integer> variableIdxs;
    private final Set<Variable> additionalVariables;

    private SddModelEnumerationFunction(final Set<Variable> variables, final Set<Variable> additionalVariables,
                                        final Sdd sdd) {
        this.sdd = sdd;
        this.variableIdxs = SddUtil.varsToIndicesOnlyKnown(variables, sdd, new HashSet<>());
        this.variables = variables;
        this.additionalVariables =
                additionalVariables.stream().filter(v -> !variables.contains(v)).collect(Collectors.toSet());
    }

    public Sdd getSdd() {
        return sdd;
    }

    public Set<Variable> getVariables() {
        return Collections.unmodifiableSet(variables);
    }

    public Set<Variable> getAdditionalVariables() {
        return Collections.unmodifiableSet(additionalVariables);
    }

    @Override
    public LngResult<List<Model>> execute(final SddNode node, final ComputationHandler handler) {
        if (!handler.shouldResume(ComputationStartedEvent.MODEL_ENUMERATION_STARTED)) {
            return LngResult.canceled(ComputationStartedEvent.MODEL_ENUMERATION_STARTED);
        }
        if (node.isFalse()) {
            return LngResult.of(List.of());
        }

        final LngResult<SddNode> projectedNodeResult = projectNode(node, handler);
        if (!projectedNodeResult.isSuccess()) {
            return LngResult.canceled(projectedNodeResult.getCancelCause());
        }
        final SddNode projectedNode = projectedNodeResult.getResult();

        final List<Variable> dontCareVariables = computeDontCareVariables(projectedNode);
        final Pair<List<Literal>, Set<Integer>> additionalVarSets = splitAdditionalVariables(node);
        final List<Literal> additionalLitsOutside = additionalVarSets.getFirst();
        final Set<Integer> additionalVarIndicesInside = additionalVarSets.getSecond();

        final HashMap<SddNodeDecomposition, IterationState> states = new HashMap<>();
        final List<Model> models = new ArrayList<>();
        do {
            final CompactModel model = new CompactModel(new ArrayList<>(), new ArrayList<>());
            write(projectedNode, model, states);
            model.getDontCareVariables().addAll(dontCareVariables);
            model.getLiterals().addAll(additionalLitsOutside);
            final LngResult<List<List<Literal>>> expandedResult = expandModel(model, handler);
            if (!expandedResult.isSuccess() && !expandedResult.isPartial()) {
                return LngResult.canceled(expandedResult.getCancelCause());
            }
            final List<List<Literal>> expandedWithAdditionalVars =
                    evaluateAdditionalVars(expandedResult.getPartialResult(), additionalVarIndicesInside, node, sdd);
            for (final List<Literal> m : expandedWithAdditionalVars) {
                models.add(new Model(m));
            }
            if (expandedResult.isPartial()) {
                return LngResult.partial(models, expandedResult.getCancelCause());
            }
        } while (goNext(projectedNode, states));
        return LngResult.of(models);
    }

    protected LngResult<SddNode> projectNode(final SddNode node, final ComputationHandler handler) {
        return node.execute(new SddProjectionFunction(variables, sdd), handler);
    }

    protected List<Variable> computeDontCareVariables(final SddNode node) {
        final Set<Integer> variablesInProjVTree = new HashSet<>();
        VTreeUtil.vars(sdd.vTreeOf(node), variableIdxs, variablesInProjVTree);
        return variables.stream()
                .filter(v -> !sdd.knows(v) || !variablesInProjVTree.contains(sdd.variableToIndex(v)))
                .collect(Collectors.toList());
    }

    protected Pair<List<Literal>, Set<Integer>> splitAdditionalVariables(final SddNode node) {
        final Set<Integer> additionalVarIdxs =
                SddUtil.varsToIndicesOnlyKnown(additionalVariables, sdd, new HashSet<>());
        final Set<Integer> variablesInNodeVTree = new HashSet<>();
        VTreeUtil.vars(sdd.vTreeOf(node), additionalVarIdxs, variablesInNodeVTree);
        final List<Variable> additionalVarsOutside = additionalVariables
                .stream()
                .filter(v -> !sdd.knows(v) || !variablesInNodeVTree.contains(sdd.variableToIndex(v)))
                .collect(Collectors.toList());
        final List<Literal> additionalLitsOutside =
                additionalVarsOutside.stream().map(v -> v.negate(sdd.getFactory())).collect(Collectors.toList());

        final Set<Variable> additionalVarsInside = additionalVariables
                .stream()
                .filter(v -> !additionalVarsOutside.contains(v))
                .collect(Collectors.toSet());
        final Set<Integer> additionalVarIndicesInside =
                SddUtil.varsToIndicesExpectKnown(additionalVarsInside, sdd, new HashSet<>());
        return new Pair<>(additionalLitsOutside, additionalVarIndicesInside);
    }

    protected static List<List<Literal>> evaluateAdditionalVars(final List<List<Literal>> models,
                                                                final Set<Integer> additionalVariables,
                                                                final SddNode node,
                                                                final Sdd sdd) {
        if (!additionalVariables.isEmpty()) {
            for (final List<Literal> model : models) {
                final Assignment modelAsAssignment = new Assignment(model);
                final Optional<List<Literal>> additionalAssignments =
                        SddEvaluation.partialEvaluateFromIndices(modelAsAssignment, additionalVariables, node, sdd);
                assert additionalAssignments.isPresent(); // Input assignment must be satisfiable
                model.addAll(additionalAssignments.get());
            }
        }
        return models;
    }

    public static Builder builder(final Collection<Variable> variables, final Sdd sdd) {
        return new Builder(variables, sdd);
    }

    public static class Builder {
        private Set<Variable> variables;
        private Set<Variable> additionalVariables;
        private final Sdd sdd;

        public Builder(final Collection<Variable> variables, final Sdd sdd) {
            this.variables = new TreeSet<>(variables);
            this.additionalVariables = new TreeSet<>();
            this.sdd = sdd;
        }

        public Builder variables(final Collection<Variable> variables) {
            this.variables = new TreeSet<>(variables);
            return this;
        }

        public Builder additionalVariables(final Collection<Variable> additionalVariables) {
            this.additionalVariables = new TreeSet<>(additionalVariables);
            return this;
        }

        public SddModelEnumerationFunction build() {
            return new SddModelEnumerationFunction(variables, additionalVariables, sdd);
        }
    }


    private LngResult<List<List<Literal>>> expandModel(final CompactModel model, final ComputationHandler handler) {
        List<List<Literal>> result = List.of(model.getLiterals());

        boolean abort = false;
        LngEvent event = new EnumerationFoundModelsEvent(1);
        if (!handler.shouldResume(event)) {
            abort = true;
        }
        for (final Variable var : model.getDontCareVariables()) {
            final List<List<Literal>> extended;
            if (abort) {
                extended = new ArrayList<>(result.size());
                for (final List<Literal> literals : result) {
                    extended.add(extendedByLiteral(literals, var.negate(var.getFactory())));
                }
            } else {
                extended = new ArrayList<>(result.size() * 2);
                for (final List<Literal> literals : result) {
                    extended.add(extendedByLiteral(literals, var));
                    extended.add(extendedByLiteral(literals, var.negate(var.getFactory())));
                }
                event = new EnumerationFoundModelsEvent(extended.size() / 2);
                if (!handler.shouldResume(event)) {
                    abort = true;
                }
            }
            result = extended;
        }
        if (abort) {
            return LngResult.partial(result, event);
        } else {
            return LngResult.of(result);
        }
    }

    private static List<Literal> extendedByLiteral(final List<Literal> literals, final Literal lit) {
        final ArrayList<Literal> extended = new ArrayList<>(literals);
        extended.add(lit);
        return extended;
    }

    private boolean goNext(final SddNode node, final Map<SddNodeDecomposition, IterationState> states) {
        if (node.isDecomposition()) {
            final SddNodeDecomposition decomp = node.asDecomposition();
            final IterationState state = states.get(decomp);
            SddElement activeElement = state.getActiveElement();
            if (!goNext(activeElement, states)) {
                activeElement = state.next();
                if (activeElement == null) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean goNext(final SddElement element, final Map<SddNodeDecomposition, IterationState> states) {
        if (!goNext(element.getPrime(), states)) {
            if (element.getPrime().isDecomposition()) {
                states.get(element.getPrime().asDecomposition()).reset();
            }
            if (!goNext(element.getSub(), states)) {
                if (element.getSub().isDecomposition()) {
                    states.get(element.getSub().asDecomposition()).reset();
                }
                return false;
            }
        }
        return true;
    }

    private void write(final SddNode node, final CompactModel model,
                       final Map<SddNodeDecomposition, IterationState> states) {
        if (node.isFalse()) {
            throw new RuntimeException("Cannot write model of unsatisfiable node");
        } else if (node.isTrue()) {
        } else if (node.isLiteral()) {
            final int varIdx = node.asTerminal().getVTree().getVariable();
            final boolean phase = node.asTerminal().getPhase();
            final Variable var = sdd.indexToVariable(varIdx);
            final Literal lit = phase ? var : var.negate(sdd.getFactory());
            model.getLiterals().add(lit);
        } else {
            final SddNodeDecomposition decomp = node.asDecomposition();
            IterationState state = states.get(decomp);
            if (state == null) {
                state = new IterationState(decomp);
                states.put(decomp, state);
            }
            final SddElement activeElement = state.getActiveElement();
            assert activeElement != null;
            write(activeElement.getPrime(), model, states);
            write(activeElement.getSub(), model, states);
            final VTreeInternal vtree = sdd.vTreeOf(node).asInternal();
            final VTree primeVtree = sdd.vTreeOf(activeElement.getPrime());
            final VTree subVtree = sdd.vTreeOf(activeElement.getSub());
            addGapVars(model.getDontCareVariables(), primeVtree, vtree.getLeft(), sdd);
            addGapVars(model.getDontCareVariables(), subVtree, vtree.getRight(), sdd);
        }
    }

    private void addGapVars(final Collection<Variable> model, final VTree usedVTree,
                            final VTree targetVTree, final Sdd sdd) {
        final SortedSet<Integer> gapVarIdxs = new TreeSet<>();
        VTreeUtil.gapVars(targetVTree, usedVTree, sdd.getVTree(), variableIdxs, gapVarIdxs);
        for (final int idx : gapVarIdxs) {
            final Variable var = sdd.indexToVariable(idx);
            model.add(var);
        }
    }


    private static class IterationState {
        private final SddNodeDecomposition parent;
        private Iterator<SddElement> iterator;
        private SddElement current;

        IterationState(final SddNodeDecomposition parent) {
            this.parent = parent;
            reset();
        }

        void reset() {
            this.iterator = parent.iterator();
            this.current = next();
        }

        SddElement next() {
            if (!iterator.hasNext()) {
                return null;
            }
            current = iterator.next();
            if (current.getSub().isFalse()) {
                current = null;
                return next();
            }
            return current;
        }

        SddElement getActiveElement() {
            return current;
        }
    }
}
