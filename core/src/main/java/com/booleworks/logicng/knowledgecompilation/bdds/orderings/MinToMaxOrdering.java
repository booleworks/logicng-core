// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.orderings;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.functions.VariableProfileFunction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A BDD variable ordering sorting the variables from minimal to maximal
 * occurrence in the input formula. If two variables have the same number of
 * occurrences, their ordering according to their DFS ordering will be
 * considered.
 * @version 3.0.0
 * @since 1.4.0
 */
public final class MinToMaxOrdering implements VariableOrderingProvider {

    private final DfsOrdering dfsOrdering = new DfsOrdering();

    @Override
    public List<Variable> getOrder(final FormulaFactory f, final Formula formula) {
        final VariableProfileFunction profileFunction = new VariableProfileFunction(f);
        final Map<Variable, Integer> profile = formula.apply(profileFunction);
        final List<Variable> dfs = dfsOrdering.getOrder(f, formula);

        final Comparator<Map.Entry<Variable, Integer>> comparator = (o1, o2) -> {
            final int occComp = o1.getValue().compareTo(o2.getValue());
            if (occComp != 0) {
                return -occComp;
            }
            final int index1 = dfs.indexOf(o1.getKey());
            final int index2 = dfs.indexOf(o2.getKey());
            return index1 - index2;
        };
        return VariableOrderingProvider.sortProfile(profile, comparator);
    }

    static Map<Variable, Integer> sortProfileByOccurrence(final Map<Variable, Integer> map,
                                                          final Comparator<Map.Entry<Variable, Integer>> comparator) {
        final List<Map.Entry<Variable, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort(comparator);
        final Map<Variable, Integer> result = new LinkedHashMap<>();
        for (final Map.Entry<Variable, Integer> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
