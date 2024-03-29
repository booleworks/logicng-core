// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.testutils;

import static com.booleworks.logicng.util.CollectionHelper.difference;
import static com.booleworks.logicng.util.CollectionHelper.union;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public final class TestUtil {

    /**
     * Tests if the two given formulas have the same models when projected to the given set of variables.
     * @param f1   first formula
     * @param f2   second formula
     * @param vars the set of variables to which the models should be projected
     * @return {@code true} if the two formulas have the same models when projected to the given set of variables, otherwise {@code false}
     */
    public static boolean equivalentModels(final Formula f1, final Formula f2, final SortedSet<Variable> vars) {
        final SATSolver s1 = MiniSat.miniSat(f1.factory());
        s1.add(f1);
        final List<Model> models1 = s1.enumerateAllModels(vars);
        final SATSolver s2 = MiniSat.miniSat(f2.factory());
        s2.add(f2);
        final List<Model> models2 = s2.enumerateAllModels(vars);
        if (models1.size() != models2.size()) {
            return false;
        }
        for (final Model model : models1) {
            if (!models2.contains(model)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of models when extending the given models by all don't care variables.
     * <p>
     * Assumption, the given models all contain the same set of variables, e.g. the result of a model enumeration.
     * @param models    the assignments
     * @param variables the variables, a superset of the variables in the models
     * @return the number of models
     */
    public static BigInteger modelCount(final List<Model> models, final SortedSet<Variable> variables) {
        if (models.isEmpty()) {
            return BigInteger.ZERO;
        } else {
            final SortedSet<Variable> dontCareVars = getDontCareVariables(models, variables);
            return BigInteger.valueOf(models.size()).multiply(BigInteger.valueOf(2).pow(dontCareVars.size()));
        }
    }

    /**
     * Returns the don't care variables.
     * <p>
     * Assumption, the given models all contain the same set of variables, e.g. the result of a model enumeration.
     * @param models    the models
     * @param variables the variables, a superset of the variables in the models
     * @return the don't care variables
     */
    public static SortedSet<Variable> getDontCareVariables(final List<Model> models, final SortedSet<Variable> variables) {
        if (models.isEmpty()) {
            return Collections.emptySortedSet();
        } else {
            final Model firstModel = models.get(0);
            final SortedSet<Variable> assignmentVars = union(firstModel.positiveVariables(), firstModel.negativeVariables(), TreeSet::new);
            return difference(variables, assignmentVars, TreeSet::new);
        }
    }
}
