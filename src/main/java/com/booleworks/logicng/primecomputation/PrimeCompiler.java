// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.primecomputation;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.Handler;
import com.booleworks.logicng.handlers.OptimizationHandler;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.OptimizationFunction;
import com.booleworks.logicng.solvers.sat.SATCall;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import com.booleworks.logicng.transformations.LiteralSubstitution;
import com.booleworks.logicng.util.FormulaHelper;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Compiler for prime implicants and prime implicates of formulas.
 * <p>
 * Implementation is based on &quot;Prime compilation of non-clausal formulae&quot;,
 * (Previti, Ignatiev, Morgado, &amp; Marques-Silva, 2015).
 * <p>
 * The algorithm computes either <b>all</b> prime implicants and a <b>cover</b> of
 * prime implicates or a <b>cover</b> of prime implicants and <b>all</b> prime implicates.
 * This can be configured via the {@link PrimeResult.CoverageType}.
 * <p>
 * Furthermore, the algorithm comes in two flavors: One which searches for maximum models
 * {@link #getWithMaximization()} and another which searches for minimum models
 * {@link #getWithMaximization()}. From experience, the one with minimum models usually
 * outperforms the one with maximum models.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class PrimeCompiler {

    private static final String POS = "_POS";
    private static final String NEG = "_NEG";
    private static final PrimeCompiler INSTANCE_MIN = new PrimeCompiler(false);
    private static final PrimeCompiler INSTANCE_MAX = new PrimeCompiler(true);

    private final boolean computeWithMaximization;

    private PrimeCompiler(final boolean computeWithMaximization) {
        this.computeWithMaximization = computeWithMaximization;
    }

    /**
     * Returns a compiler which uses minimum models to compute the primes.
     * @return a compiler which uses minimum models to compute the primes
     */
    public static PrimeCompiler getWithMinimization() {
        return INSTANCE_MIN;
    }

    /**
     * Returns a compiler which uses maximum models to compute the primes.
     * @return a compiler which uses maximum models to compute the primes
     */
    public static PrimeCompiler getWithMaximization() {
        return INSTANCE_MAX;
    }

    /**
     * Computes prime implicants and prime implicates for a given formula.
     * The coverage type specifies if the implicants or the implicates will
     * be complete, the other one will still be a cover of the given formula.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula
     * @param type    the coverage type
     * @return the prime result
     */
    public PrimeResult compute(final FormulaFactory f, final Formula formula, final PrimeResult.CoverageType type) {
        return compute(f, formula, type, null);
    }

    /**
     * Computes prime implicants and prime implicates for a given formula.
     * The coverage type specifies if the implicants or the implicates will
     * be complete, the other one will still be a cover of the given formula.
     * <p>
     * The prime compiler can be called with an {@link OptimizationHandler}.
     * The given handler instance will be used for every subsequent
     * {@link OptimizationFunction} call and
     * the handler's SAT handler is used for every subsequent SAT call.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula
     * @param type    the coverage type
     * @param handler an optimization handler, can be {@code null}
     * @return the prime result or null if the computation was aborted by the handler
     */
    public PrimeResult compute(final FormulaFactory f, final Formula formula, final PrimeResult.CoverageType type, final OptimizationHandler handler) {
        Handler.start(handler);
        final boolean completeImplicants = type == PrimeResult.CoverageType.IMPLICANTS_COMPLETE;
        final Formula formulaForComputation = completeImplicants ? formula : formula.negate(f);
        final Pair<List<SortedSet<Literal>>, List<SortedSet<Literal>>> result = computeGeneric(f, formulaForComputation, handler);
        if (result == null || Handler.aborted(handler)) {
            return null;
        }
        return new PrimeResult(
                completeImplicants ? result.first() : negateAll(f, result.second()),
                completeImplicants ? result.second() : negateAll(f, result.first()),
                type);
    }

    private Pair<List<SortedSet<Literal>>, List<SortedSet<Literal>>> computeGeneric(final FormulaFactory f, final Formula formula,
                                                                                    final OptimizationHandler handler) {
        final SubstitutionResult sub = createSubstitution(f, formula);
        final SATSolver hSolver = SATSolver.miniSat(f, SATSolverConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.PG_ON_SOLVER).build());
        hSolver.add(sub.constraintFormula);
        final SATSolver fSolver = SATSolver.miniSat(f, SATSolverConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.PG_ON_SOLVER).build());
        fSolver.add(formula.negate(f));
        final NaivePrimeReduction primeReduction = new NaivePrimeReduction(f, formula);
        final List<SortedSet<Literal>> primeImplicants = new ArrayList<>();
        final List<SortedSet<Literal>> primeImplicates = new ArrayList<>();
        while (true) {
            final Assignment hModel = hSolver.execute(computeWithMaximization
                    ? OptimizationFunction.builder().handler(handler).literals(sub.newVar2oldLit.keySet()).maximize().build()
                    : OptimizationFunction.builder().handler(handler).literals(sub.newVar2oldLit.keySet()).minimize().build());
            if (Handler.aborted(handler)) {
                return null;
            }
            if (hModel == null) {
                return new Pair<>(primeImplicants, primeImplicates);
            }
            final Assignment fModel = transformModel(hModel, sub.newVar2oldLit);
            try (final SATCall fCall = fSolver.satCall().handler(OptimizationHandler.satHandler(handler)).assumptions(fModel.literals()).solve()) {
                if (Handler.aborted(handler)) {
                    return null;
                }
                if (fCall.getSatResult() == Tristate.FALSE) {
                    final SortedSet<Literal> primeImplicant = computeWithMaximization ? primeReduction.reduceImplicant(fModel.literals(), OptimizationHandler.satHandler(handler)) : fModel.literals();
                    if (primeImplicant == null || Handler.aborted(handler)) {
                        return null;
                    }
                    primeImplicants.add(primeImplicant);
                    final List<Literal> blockingClause = new ArrayList<>();
                    for (final Literal lit : primeImplicant) {
                        blockingClause.add(((Literal) lit.transform(sub.substitution)).negate(f));
                    }
                    hSolver.add(f.or(blockingClause));
                } else {
                    final SortedSet<Literal> implicate = new TreeSet<>();
                    for (final Literal lit : (computeWithMaximization ? fModel : fCall.model(formula.variables(f))).literals()) {
                        implicate.add(lit.negate(f));
                    }
                    final SortedSet<Literal> primeImplicate = primeReduction.reduceImplicate(f, implicate, OptimizationHandler.satHandler(handler));
                    if (primeImplicate == null || Handler.aborted(handler)) {
                        return null;
                    }
                    primeImplicates.add(primeImplicate);
                    hSolver.add(f.or(primeImplicate).transform(sub.substitution));
                }
            }
        }
    }

    private SubstitutionResult createSubstitution(final FormulaFactory f, final Formula formula) {
        final Map<Variable, Literal> newVar2oldLit = new HashMap<>();
        final Map<Literal, Literal> substitution = new HashMap<>();
        final List<Formula> constraintOps = new ArrayList<>();
        for (final Variable variable : formula.variables(f)) {
            final Variable posVar = f.variable(variable.name() + POS);
            newVar2oldLit.put(posVar, variable);
            substitution.put(variable, posVar);
            final Variable negVar = f.variable(variable.name() + NEG);
            newVar2oldLit.put(negVar, variable.negate(f));
            substitution.put(variable.negate(f), negVar);
            constraintOps.add(f.amo(posVar, negVar));
        }
        final LiteralSubstitution substTransformation = new LiteralSubstitution(f, substitution);
        return new SubstitutionResult(newVar2oldLit, substTransformation, f.and(constraintOps));
    }

    private Assignment transformModel(final Assignment model, final Map<Variable, Literal> mapping) {
        final Assignment mapped = new Assignment();
        for (final Variable var : model.positiveVariables()) {
            mapped.addLiteral(mapping.get(var));
        }
        return mapped;
    }

    private List<SortedSet<Literal>> negateAll(final FormulaFactory f, final Collection<SortedSet<Literal>> literalSets) {
        final List<SortedSet<Literal>> result = new ArrayList<>();
        for (final SortedSet<Literal> literals : literalSets) {
            result.add(FormulaHelper.negateLiterals(f, literals, TreeSet::new));
        }
        return result;
    }

    private static class SubstitutionResult {
        private final Map<Variable, Literal> newVar2oldLit;
        private final LiteralSubstitution substitution;
        private final Formula constraintFormula;

        private SubstitutionResult(final Map<Variable, Literal> newVar2oldLit, final LiteralSubstitution substitution, final Formula constraintFormula) {
            this.newVar2oldLit = newVar2oldLit;
            this.substitution = substitution;
            this.constraintFormula = constraintFormula;
        }
    }
}
