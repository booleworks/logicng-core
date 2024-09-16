// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.functions;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.bdds.Bdd;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddOperations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Performs model enumeration on a BDD. The models are returned as a list of
 * {@link Model models}.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class BddModelEnumerationFunction extends BddFunction<List<Model>> {

    private final Collection<Variable> variables;

    /**
     * Constructs a new model enumeration function. The models are projected to
     * a given set of variables.
     * @param f         the formula factory to generate new formulas
     * @param variables the variables to which models are projected
     */
    public BddModelEnumerationFunction(final FormulaFactory f, final Collection<Variable> variables) {
        super(f);
        this.variables = variables;
    }

    @Override
    public List<Model> apply(final Bdd bdd) {
        final Set<Model> res = new HashSet<>();
        final BddKernel kernel = bdd.getUnderlyingKernel();
        final List<byte[]> models = new BddOperations(kernel).allSat(bdd.getIndex());
        final SortedSet<Integer> temp;
        if (variables == null) {
            temp = new TreeSet<>(kernel.var2idx().values());
        } else {
            temp = new TreeSet<>();
            for (final Map.Entry<Variable, Integer> e : kernel.var2idx().entrySet()) {
                if (variables.contains(e.getKey())) {
                    temp.add(e.getValue());
                }
            }
        }
        final int[] relevantIndices = new int[temp.size()];
        int count = 0;
        for (final Integer i : temp) {
            relevantIndices[count++] = i;
        }
        for (final byte[] model : models) {
            final List<Model> allModels = new ArrayList<>();
            generateAllModels(kernel, allModels, model, relevantIndices, 0);
            res.addAll(allModels);
        }
        return new ArrayList<>(res);
    }

    private void generateAllModels(final BddKernel kernel, final List<Model> assignments, final byte[] model,
                                   final int[] relevantIndices, final int position) {
        if (position == relevantIndices.length) {
            final List<Literal> lits = new ArrayList<>();
            for (final int i : relevantIndices) {
                lits.add(model[i] == 0 ? kernel.getVariableForIndex(i).negate(f) : kernel.getVariableForIndex(i));
            }
            assignments.add(new Model(lits));
        } else if (model[relevantIndices[position]] != -1) {
            generateAllModels(kernel, assignments, model, relevantIndices, position + 1);
        } else {
            model[relevantIndices[position]] = 0;
            generateAllModels(kernel, assignments, model, relevantIndices, position + 1);
            model[relevantIndices[position]] = 1;
            generateAllModels(kernel, assignments, model, relevantIndices, position + 1);
            model[relevantIndices[position]] = -1;
        }
    }
}
