// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.np;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.util.CollectionHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple MaxSAT based implementation of an algorithm finding a minimum set
 * cover for a given collection of sets. This algorithm is really only meant for
 * small set cover problems with perhaps some tens or hundreds of set and
 * hundreds of variables.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class SetCover {

    /**
     * Private empty constructor. Class only contains static utility methods.
     */
    private SetCover() {
        // Intentionally left empty
    }

    /**
     * Computes the minimum set cover for the given collection of sets, i.e. a
     * minimum number of sets s.t. each element is covered at least once by a
     * set in the cover.
     * @param <T>  the type of the elements of the sets. This type must
     *             implement a meaningful equals/hashCode method since it is
     *             internally used in HashSets
     * @param f    the formula factory
     * @param sets the sets to cover
     * @return a minimum cover of the elements in the given sets
     */
    public static <T> List<Set<T>> compute(final FormulaFactory f, final Collection<Set<T>> sets) {
        if (sets.isEmpty()) {
            return Collections.emptyList();
        }
        final Map<Variable, Set<T>> setMap = new HashMap<>();
        final Map<T, Set<Variable>> elementOccurrences = new HashMap<>();
        for (final Set<T> set : sets) {
            final Variable setVar = f.variable("@SET_SEL_" + setMap.size());
            setMap.put(setVar, set);
            for (final T element : set) {
                elementOccurrences.computeIfAbsent(element, i -> new LinkedHashSet<>()).add(setVar);
            }
        }
        final MaxSatSolver solver = MaxSatSolver.newSolver(f);
        for (final Set<Variable> occurrences : elementOccurrences.values()) {
            solver.addHardFormula(f.or(occurrences));
        }
        for (final Variable setVar : setMap.keySet()) {
            solver.addSoftFormula(setVar.negate(f), 1);
        }
        final MaxSatResult maxSatResult = solver.solve();
        if (!maxSatResult.isSatisfiable()) {
            throw new IllegalStateException("Internal optimization problem was not feasible.");
        }
        final ArrayList<Variable> minimumCover =
                CollectionHelper.intersection(maxSatResult.getModel().positiveVariables(), setMap.keySet(), ArrayList::new);
        final List<Set<T>> result = new ArrayList<>();
        for (final Variable setVar : minimumCover) {
            result.add(setMap.get(setVar));
        }
        return result;
    }
}
