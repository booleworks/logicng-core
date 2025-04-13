package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class SddBackboneFunction implements SddFunction<Backbone> {
    private final SortedSet<Variable> variables;
    private final SddNode originalNode;
    private final VTreeRoot root;

    public SddBackboneFunction(final SortedSet<Variable> variables, final SddNode originalNode, final VTreeRoot root) {
        this.variables = variables;
        this.originalNode = originalNode;
        this.root = root;
    }

    @Override
    public LngResult<Backbone> apply(final SddFactory sf, final ComputationHandler handler) {
        final HashMap<Variable, Tristate> backboneMap = new HashMap<>();
        if (originalNode.isFalse()) {
            return LngResult.of(Backbone.unsatBackbone());
        }
        applyRec(originalNode, backboneMap);
        final SortedSet<Variable> posVars = new TreeSet<>();
        final SortedSet<Variable> negVars = new TreeSet<>();
        final SortedSet<Variable> optVars = new TreeSet<>();
        for (final Variable var : variables) {
            final Tristate state = backboneMap.get(var);
            if (state == null) {
                optVars.add(var);
            } else if (state == Tristate.TRUE) {
                posVars.add(var);
            } else if (state == Tristate.FALSE) {
                negVars.add(var);
            } else {
                optVars.add(var);
            }
        }
        final Backbone backbone = Backbone.satBackbone(posVars, negVars, optVars);
        return LngResult.of(backbone);
    }

    public void applyRec(final SddNode node, final Map<Variable, Tristate> backbone) {
        if (node.isDecomposition()) {
            final Set<Variable> gapVars = new HashSet<>();
            final VTreeInternal targetVTree = root.getVTree(node).asInternal();
            for (final SddElement element : node.asDecomposition().getElements()) {
                if (!element.getSub().isFalse()) {
                    applyRec(element.getPrime(), backbone);
                    applyRec(element.getSub(), backbone);
                    VTreeUtil.gapVars(targetVTree.getLeft(), root.getVTree(element.getPrime()), root, variables,
                            gapVars);
                    if (element.getSub().isTrue()) {
                        VTreeUtil.vars(targetVTree.getRight(), variables, gapVars);
                    } else {
                        VTreeUtil.gapVars(targetVTree.getRight(), root.getVTree(element.getSub()), root, variables,
                                gapVars);
                    }
                }
            }
            for (final Variable var : gapVars) {
                backbone.put(var, Tristate.UNDEF);
            }
        } else if (node.isLiteral()) {
            final Literal lit = (Literal) node.asTerminal().getTerminal();
            if (variables.contains(lit.variable())) {
                addToBackbone(lit.variable(), lit.getPhase(), backbone);
            }
        }
    }

    private void addToBackbone(final Variable var, final boolean phase, final Map<Variable, Tristate> backbone) {
        Tristate state = backbone.get(var);
        if (state == null) {
            state = Tristate.fromBool(phase);
        } else {
            if (phase) {
                if (state != Tristate.TRUE) {
                    state = Tristate.UNDEF;
                }
            } else {
                if (state != Tristate.FALSE) {
                    state = Tristate.UNDEF;
                }
            }
        }
        backbone.put(var, state);
    }
}
