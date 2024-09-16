// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.primecomputation;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.PRIME_COMPUTATION_STARTED;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.NopHandler;
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
 * Implementation is based on &quot;Prime compilation of non-clausal
 * formulae&quot;, (Previti, Ignatiev, Morgado, &amp; Marques-Silva, 2015).
 * <p>
 * The algorithm computes either <b>all</b> prime implicants and a <b>cover</b>
 * of prime implicates or a <b>cover</b> of prime implicants and <b>all</b>
 * prime implicates. This can be configured via the
 * {@link PrimeResult.CoverageType}.
 * <p>
 * Furthermore, the algorithm comes in two flavors: One which searches for
 * maximum models {@link #getWithMaximization()} and another which searches for
 * minimum models {@link #getWithMaximization()}. From experience, the one with
 * minimum models usually outperforms the one with maximum models.
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
     * Computes prime implicants and prime implicates for a given formula. The
     * coverage type specifies if the implicants or the implicates will be
     * complete, the other one will still be a cover of the given formula.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula
     * @param type    the coverage type
     * @return the prime result
     */
    public PrimeResult compute(final FormulaFactory f, final Formula formula, final PrimeResult.CoverageType type) {
        return compute(f, formula, type, NopHandler.get()).getResult();
    }

    /**
     * Computes prime implicants and prime implicates for a given formula. The
     * coverage type specifies if the implicants or the implicates will be
     * complete, the other one will still be a cover of the given formula.
     * <p>
     * The prime compiler can be called with an {@link ComputationHandler}. The
     * given handler instance will be used for every subsequent
     * {@link OptimizationFunction} call and the handler's SAT handler is used
     * for every subsequent SAT call.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula
     * @param type    the coverage type
     * @param handler an optimization handler, can be {@code null}
     * @return the prime result or null if the computation was canceled by the
     * handler
     */
    public LNGResult<PrimeResult> compute(final FormulaFactory f, final Formula formula,
                                          final PrimeResult.CoverageType type, final ComputationHandler handler) {
        if (!handler.shouldResume(PRIME_COMPUTATION_STARTED)) {
            return LNGResult.canceled(PRIME_COMPUTATION_STARTED);
        }
        final boolean completeImplicants = type == PrimeResult.CoverageType.IMPLICANTS_COMPLETE;
        final Formula formulaForComputation = completeImplicants ? formula : formula.negate(f);
        final LNGResult<Pair<List<SortedSet<Literal>>, List<SortedSet<Literal>>>> genericResult =
                computeGeneric(f, formulaForComputation, handler);
        if (!genericResult.isSuccess()) {
            return LNGResult.canceled(genericResult.getCancelCause());
        }
        final Pair<List<SortedSet<Literal>>, List<SortedSet<Literal>>> result = genericResult.getResult();
        return LNGResult.of(new PrimeResult(
                completeImplicants ? result.getFirst() : negateAll(f, result.getSecond()),
                completeImplicants ? result.getSecond() : negateAll(f, result.getFirst()),
                type));
    }

    private LNGResult<Pair<List<SortedSet<Literal>>, List<SortedSet<Literal>>>> computeGeneric(
            final FormulaFactory f, final Formula formula, final ComputationHandler handler) {
        final SubstitutionResult sub = createSubstitution(f, formula);
        final SATSolver hSolver = SATSolver.newSolver(f,
                SATSolverConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.PG_ON_SOLVER).build());
        hSolver.add(sub.constraintFormula);
        final SATSolver fSolver = SATSolver.newSolver(f,
                SATSolverConfig.builder().cnfMethod(SATSolverConfig.CNFMethod.PG_ON_SOLVER).build());
        fSolver.add(formula.negate(f));
        final NaivePrimeReduction primeReduction = new NaivePrimeReduction(f, formula);
        final List<SortedSet<Literal>> primeImplicants = new ArrayList<>();
        final List<SortedSet<Literal>> primeImplicates = new ArrayList<>();
        while (true) {
            if (!hSolver.sat()) {
                return LNGResult.of(new Pair<>(primeImplicants, primeImplicates));
            }
            final LNGResult<Model> hModelResult = hSolver.execute(computeWithMaximization
                    ? OptimizationFunction.builder().literals(sub.newVar2oldLit.keySet()).maximize().build()
                    : OptimizationFunction.builder().literals(sub.newVar2oldLit.keySet()).minimize().build(), handler);
            if (!hModelResult.isSuccess()) {
                return LNGResult.canceled(hModelResult.getCancelCause());
            }
            final Model hModel = hModelResult.getResult();
            final Model fModel = transformModel(hModel, sub.newVar2oldLit);
            try (final SATCall fCall = fSolver.satCall().handler(handler).addFormulas(fModel.getLiterals()).solve()) {
                if (!fCall.getSatResult().isSuccess()) {
                    return LNGResult.canceled(fCall.getSatResult().getCancelCause());
                }
                if (!fCall.getSatResult().getResult()) {
                    final LNGResult<SortedSet<Literal>> primeImplicantResult = computeWithMaximization
                            ? primeReduction.reduceImplicant(fModel.getLiterals(), handler)
                            : LNGResult.of(new TreeSet<>(fModel.getLiterals()));
                    if (!primeImplicantResult.isSuccess()) {
                        return LNGResult.canceled(primeImplicantResult.getCancelCause());
                    }
                    final SortedSet<Literal> primeImplicant = primeImplicantResult.getResult();
                    primeImplicants.add(primeImplicant);
                    final List<Literal> blockingClause = new ArrayList<>();
                    for (final Literal lit : primeImplicant) {
                        blockingClause.add(((Literal) lit.transform(sub.substitution)).negate(f));
                    }
                    hSolver.add(f.or(blockingClause));
                } else {
                    final SortedSet<Literal> implicate = new TreeSet<>();
                    for (final Literal lit :
                            (computeWithMaximization ? fModel : fCall.model(formula.variables(f))).getLiterals()) {
                        implicate.add(lit.negate(f));
                    }
                    final LNGResult<SortedSet<Literal>> primeImplicateResult =
                            primeReduction.reduceImplicate(f, implicate, handler);
                    if (!primeImplicateResult.isSuccess()) {
                        return LNGResult.canceled(primeImplicateResult.getCancelCause());
                    }
                    final SortedSet<Literal> primeImplicate = primeImplicateResult.getResult();
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
            final Variable posVar = f.variable(variable.getName() + POS);
            newVar2oldLit.put(posVar, variable);
            substitution.put(variable, posVar);
            final Variable negVar = f.variable(variable.getName() + NEG);
            newVar2oldLit.put(negVar, variable.negate(f));
            substitution.put(variable.negate(f), negVar);
            constraintOps.add(f.amo(posVar, negVar));
        }
        final LiteralSubstitution substTransformation = new LiteralSubstitution(f, substitution);
        return new SubstitutionResult(newVar2oldLit, substTransformation, f.and(constraintOps));
    }

    private Model transformModel(final Model model, final Map<Variable, Literal> mapping) {
        final List<Literal> mapped = new ArrayList<>();
        for (final Variable var : model.positiveVariables()) {
            mapped.add(mapping.get(var));
        }
        return new Model(mapped);
    }

    private List<SortedSet<Literal>> negateAll(final FormulaFactory f,
                                               final Collection<SortedSet<Literal>> literalSets) {
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

        private SubstitutionResult(final Map<Variable, Literal> newVar2oldLit, final LiteralSubstitution substitution,
                                   final Formula constraintFormula) {
            this.newVar2oldLit = newVar2oldLit;
            this.substitution = substitution;
            this.constraintFormula = constraintFormula;
        }
    }
}
