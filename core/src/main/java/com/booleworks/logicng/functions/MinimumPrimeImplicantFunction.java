// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.functions;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFunction;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.solvers.MaxSatResult;
import com.booleworks.logicng.solvers.MaxSatSolver;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.transformations.LiteralSubstitution;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Computes a minimum-size prime implicant for the given formula. The formula
 * must be satisfiable, otherwise an {@link IllegalArgumentException} is thrown.
 * @version 3.0.0
 * @since 2.0.0
 */
public class MinimumPrimeImplicantFunction implements FormulaFunction<SortedSet<Literal>> {

    protected static final String POS = "_POS";
    protected static final String NEG = "_NEG";
    protected final FormulaFactory f;
    protected final MaxSatConfig config;

    /**
     * Constructs a new function which computes a minimum-size prime implicant.
     * @param f the formula factory
     */
    public MinimumPrimeImplicantFunction(final FormulaFactory f) {
        this(f, MaxSatConfig.CONFIG_OLL);
    }

    /**
     * Constructs a new function which computes a minimum-size prime implicant.
     * @param f      the formula factory
     * @param config the configuration for the underlying MaxSAT solver
     */
    public MinimumPrimeImplicantFunction(final FormulaFactory f, final MaxSatConfig config) {
        this.f = f;
        this.config = config;
    }

    @Override
    public LngResult<SortedSet<Literal>> apply(final Formula formula, final ComputationHandler handler) {
        final Formula nnf = formula.nnf(f);
        final Map<Variable, Literal> newVar2oldLit = new HashMap<>();
        final Map<Literal, Literal> substitution = new HashMap<>();
        for (final Literal literal : nnf.literals(f)) {
            final Variable newVar = f.variable(literal.getName() + (literal.getPhase() ? POS : NEG));
            newVar2oldLit.put(newVar, literal);
            substitution.put(literal, newVar);
        }
        final LiteralSubstitution substTransformation = new LiteralSubstitution(f, substitution);
        final Formula substituted = nnf.transform(substTransformation);
        final MaxSatSolver solver = MaxSatSolver.newSolver(f, config);
        solver.addHardFormula(substituted);
        for (final Literal literal : newVar2oldLit.values()) {
            if (literal.getPhase() && newVar2oldLit.containsValue(literal.negate(f))) {
                final Formula amo = f.amo(f.variable(literal.getName() + POS), f.variable(literal.getName() + NEG));
                solver.addHardFormula(amo);
            }
        }
        for (final Variable v : newVar2oldLit.keySet()) {
            solver.addSoftFormula(v.negate(f), 1);
        }
        final LngResult<MaxSatResult> res = solver.solve(handler);
        if (!res.isSuccess()) {
            return LngResult.canceled(res.getCancelCause());
        }

        if (!res.getResult().isSatisfiable()) {
            throw new IllegalArgumentException("The given formula must be satisfiable");
        }

        final SortedSet<Literal> primeImplicant = new TreeSet<>();
        for (final Variable variable : res.getResult().getModel().positiveVariables()) {
            final Literal literal = newVar2oldLit.get(variable);
            if (literal != null) {
                primeImplicant.add(literal);
            }
        }
        return LngResult.of(primeImplicant);
    }
}
