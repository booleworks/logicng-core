package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.collections.LngIntVector;
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
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeIterationState;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        final BitSet additionalVarInsideMap = new BitSet();
        for (final int additionalVar : additionalVarIndicesInside) {
            additionalVarInsideMap.set(additionalVar);
        }

        final SddEvaluation.PartialEvalState partialEvalState = new SddEvaluation.PartialEvalState(sdd);
        final HashMap<SddNodeDecomposition, SddNodeIterationState> states = new HashMap<>();
        final List<Model> models = new ArrayList<>();
        do {
            final CompactModel model = new CompactModel(new ArrayList<>(), new ArrayList<>());
            final BitSet modelMask;
            if (additionalVarIndicesInside.isEmpty()) {
                modelMask = null;
            } else {
                modelMask = new BitSet();
            }
            write(projectedNode, model, modelMask, states);
            model.getDontCareVariables().addAll(dontCareVariables);
            model.getLiterals().addAll(additionalLitsOutside);
            final LngEvent expandedResult =
                    expandModel(model, modelMask, additionalVarInsideMap, node, partialEvalState, handler, models);
            if (expandedResult != null) {
                return LngResult.partial(models, expandedResult);
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

    private LngEvent expandModel(final CompactModel model, final BitSet modelMask,
                                 final BitSet additionalVars, final SddNode node,
                                 final SddEvaluation.PartialEvalState state,
                                 final ComputationHandler handler, final List<Model> extendedModels) {
        final LngIntVector dontCareLitIdxs = new LngIntVector(model.getDontCareVariables().size());
        final int offset = model.getLiterals().size();
        for (final Variable dontCare : model.getDontCareVariables()) {
            dontCareLitIdxs.push(sdd.literalToIndex(dontCare));
            model.getLiterals().add(dontCare.negate(sdd.getFactory()));
        }
        return extendModelsInplace(offset, 0, model.getDontCareVariables(), dontCareLitIdxs, modelMask, additionalVars,
                node, state, handler, model.getLiterals(), extendedModels);
    }

    private LngEvent extendModelsInplace(final int offset, final int index,
                                         final List<Variable> dontCareVariables,
                                         final LngIntVector dontCareIdxs, final BitSet modelMask,
                                         final BitSet additionalVars, final SddNode node,
                                         final SddEvaluation.PartialEvalState state,
                                         final ComputationHandler handler, final List<Literal> dst,
                                         final List<Model> extendedModels) {
        if (index == dontCareVariables.size()) {
            final List<Literal> extendedModel = new ArrayList<>(dst);
            if (!additionalVars.isEmpty()) {
                SddEvaluation.partialEvaluateInplace((BitSet) modelMask.clone(), additionalVars, node, state,
                        extendedModel);
            }
            extendedModels.add(new Model(extendedModel));
            if (!handler.shouldResume(EnumerationFoundModelsEvent.FOUND_ONE_MODEL)) {
                return EnumerationFoundModelsEvent.FOUND_ONE_MODEL;
            }
            return null;
        } else {
            dst.set(offset + index, dontCareVariables.get(index).negate(sdd.getFactory()));
            if (modelMask != null && dontCareIdxs.get(index) != -1) {
                modelMask.clear(dontCareIdxs.get(index));
                modelMask.set(sdd.negateLitIdx(dontCareIdxs.get(index)));
            }
            final LngEvent event = extendModelsInplace(offset, index + 1, dontCareVariables, dontCareIdxs, modelMask,
                    additionalVars, node, state, handler, dst, extendedModels);
            if (event != null) {
                return event;
            }
            dst.set(offset + index, dontCareVariables.get(index));
            if (modelMask != null && dontCareIdxs.get(index) != -1) {
                modelMask.set(dontCareIdxs.get(index));
                modelMask.clear(sdd.negateLitIdx(dontCareIdxs.get(index)));
            }
            return extendModelsInplace(offset, index + 1, dontCareVariables, dontCareIdxs, modelMask,
                    additionalVars, node, state, handler, dst, extendedModels);
        }
    }

    private boolean goNext(final SddNode node, final Map<SddNodeDecomposition, SddNodeIterationState> states) {
        if (node.isDecomposition()) {
            final SddNodeDecomposition decomp = node.asDecomposition();
            final SddNodeIterationState state = states.get(decomp);
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

    private boolean goNext(final SddElement element, final Map<SddNodeDecomposition, SddNodeIterationState> states) {
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

    private void write(final SddNode node, final CompactModel model, final BitSet modelMask,
                       final Map<SddNodeDecomposition, SddNodeIterationState> states) {
        if (node.isFalse()) {
            throw new RuntimeException("Cannot write model of unsatisfiable node");
        } else if (node.isTrue()) {
        } else if (node.isLiteral()) {
            final int varIdx = node.asTerminal().getVTree().getVariable();
            final boolean phase = node.asTerminal().getPhase();
            final Variable var = sdd.indexToVariable(varIdx);
            final Literal lit = phase ? var : var.negate(sdd.getFactory());
            model.getLiterals().add(lit);
            if (modelMask != null) {
                modelMask.set(sdd.literalToIndex(varIdx, phase));
            }
        } else {
            final SddNodeDecomposition decomp = node.asDecomposition();
            SddNodeIterationState state = states.get(decomp);
            if (state == null) {
                state = new SddNodeIterationState(decomp);
                states.put(decomp, state);
            }
            final SddElement activeElement = state.getActiveElement();
            assert activeElement != null;
            write(activeElement.getPrime(), model, modelMask, states);
            write(activeElement.getSub(), model, modelMask, states);
            final VTreeInternal vtree = sdd.vTreeOf(node).asInternal();
            final VTree primeVtree = sdd.vTreeOf(activeElement.getPrime());
            final VTree subVtree = sdd.vTreeOf(activeElement.getSub());
            addGapVars(model.getDontCareVariables(), primeVtree, vtree.getLeft(), sdd);
            addGapVars(model.getDontCareVariables(), subVtree, vtree.getRight(), sdd);
        }
    }

    private void addGapVars(final Collection<Variable> model, final VTree usedVTree,
                            final VTree targetVTree, final Sdd sdd) {
        final ArrayList<Integer> gapVarIdxs = new ArrayList<>();
        VTreeUtil.gapVars(targetVTree, usedVTree, sdd.getVTree(), variableIdxs, gapVarIdxs);
        for (final int idx : gapVarIdxs) {
            final Variable var = sdd.indexToVariable(idx);
            model.add(var);
        }
    }
}
