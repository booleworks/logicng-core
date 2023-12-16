// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.orderings;

import static com.booleworks.logicng.knowledgecompilation.bdds.orderings.MinToMaxOrdering.sortProfileByOccurrence;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * An interface for variable ordering providers for BDDs.
 * @version 1.4.0
 * @since 1.4.0
 */
public interface VariableOrderingProvider {

    /**
     * Generates a variable ordering for a given formula.  Such a variable ordering can then be
     * used for the initialization of the BDD Kernel in {@link BDDKernel#BDDKernel(FormulaFactory, List, int, int)}.
     * @param f       the factory for caching and generating new formulas
     * @param formula the formula
     * @return the variable ordering
     */
    List<Variable> getOrder(final FormulaFactory f, final Formula formula);

    static List<Variable> sortProfile(final Map<Variable, Integer> profile, final Comparator<Map.Entry<Variable, Integer>> comparator) {
        final Map<Variable, Integer> sortedProfile = sortProfileByOccurrence(profile, comparator);
        final List<Variable> order = new ArrayList<>(sortedProfile.size());
        for (final Map.Entry<Variable, Integer> entry : sortedProfile.entrySet()) {
            order.add(entry.getKey());
        }
        return order;
    }
}
