// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFunction;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.OptimizationFunction;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import com.booleworks.logicng.transformations.LiteralSubstitution;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Computes a minimum-size prime implicant for the given formula.
 * Returns {@code null} if the formula was not satisfiable.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class MinimumPrimeImplicantFunction implements FormulaFunction<SortedSet<Literal>> {

    private static final String POS = "_POS";
    private static final String NEG = "_NEG";
    private final FormulaFactory f;

    public MinimumPrimeImplicantFunction(final FormulaFactory f) {
        this.f = f;
    }

    @Override
    public SortedSet<Literal> apply(final Formula formula) {
        final Formula nnf = formula.nnf(f);
        final Map<Variable, Literal> newVar2oldLit = new HashMap<>();
        final Map<Literal, Literal> substitution = new HashMap<>();
        for (final Literal literal : nnf.literals(f)) {
            final Variable newVar = f.variable(literal.name() + (literal.phase() ? POS : NEG));
            newVar2oldLit.put(newVar, literal);
            substitution.put(literal, newVar);
        }
        final LiteralSubstitution substTransformation = new LiteralSubstitution(f, substitution);
        final Formula substituted = nnf.transform(substTransformation);
        final SATSolver solver = SATSolver.miniSat(f, SATSolverConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.PG_ON_SOLVER).build());
        solver.add(substituted);
        for (final Literal literal : newVar2oldLit.values()) {
            if (literal.phase() && newVar2oldLit.containsValue(literal.negate(f))) {
                solver.add(f.amo(f.variable(literal.name() + POS), f.variable(literal.name() + NEG)));
            }
        }

        if (!solver.sat()) {
            return null;
        }
        final Assignment minimumModel = solver.execute(OptimizationFunction.minimize(newVar2oldLit.keySet()));
        final SortedSet<Literal> primeImplicant = new TreeSet<>();
        for (final Variable variable : minimumModel.positiveVariables()) {
            final Literal literal = newVar2oldLit.get(variable);
            if (literal != null) {
                primeImplicant.add(literal);
            }
        }
        return primeImplicant;
    }
}
