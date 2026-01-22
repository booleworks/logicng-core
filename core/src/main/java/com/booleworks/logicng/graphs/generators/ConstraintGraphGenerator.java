// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.graphs.generators;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.graphs.datastructures.Graph;

import java.util.Collection;

/**
 * A graph generator which generates a constraint graph for a given list of
 * formulas.
 * @version 3.0.0
 * @since 2.0.0
 */
public class ConstraintGraphGenerator {

    protected ConstraintGraphGenerator() {
        // Intentionally left empty.
    }

    /**
     * Constructs the constraint graph.
     * @param f        the formula factory to use for caching
     * @param formulas the formulas
     * @return the constraint graph for the given formula
     */
    public static Graph<Variable> generateFromFormulas(final FormulaFactory f, final Formula... formulas) {
        final Graph<Variable> constraintGraph = new Graph<>();
        for (final Formula formula : formulas) {
            addClause(f, formula, constraintGraph);
        }
        return constraintGraph;
    }

    /**
     * Constructs the constraint graph.
     * @param f        the formula factory to use for caching
     * @param formulas the formulas
     * @return the constraint graph for the given formula
     */
    public static Graph<Variable> generateFromFormulas(final FormulaFactory f, final Collection<Formula> formulas) {
        final Graph<Variable> constraintGraph = new Graph<>();
        for (final Formula formula : formulas) {
            addClause(f, formula, constraintGraph);
        }
        return constraintGraph;
    }

    protected static void addClause(final FormulaFactory f, final Formula clause, final Graph<Variable> graph) {
        final Variable[] variables = clause.variables(f).toArray(new Variable[0]);
        if (variables.length == 1) {
            graph.node(variables[0]);
        }
        for (int i = 0; i < variables.length; i++) {
            for (int j = i + 1; j < variables.length; j++) {
                graph.connect(graph.node(variables[i]), graph.node(variables[j]));
            }
        }
    }
}
