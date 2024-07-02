// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.ADVANCED_SIMPLIFICATION_STARTED;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneGeneration;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.explanations.smus.SmusComputation;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.UnsatResult;
import com.booleworks.logicng.primecomputation.PrimeCompiler;
import com.booleworks.logicng.primecomputation.PrimeResult;
import com.booleworks.logicng.transformations.StatelessFormulaTransformation;
import com.booleworks.logicng.util.FormulaHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * An advanced simplifier for formulas.
 * <p>
 * The aim of the simplification is to minimize the formula with respect to a
 * given rating function, e.g. finding a formula with a minimal number of
 * symbols when represented as string.
 * <p>
 * The simplification performs the following steps:
 * <ul>
 * <li>Restricting the formula to its backbone</li>
 * <li>Computation of all prime implicants</li>
 * <li>Finding a minimal coverage (by finding a smallest MUS)</li>
 * <li>Building a DNF from the minimal prime implicant coverage</li>
 * <li>Factoring out: Applying the Distributive Law heuristically for a smaller
 * formula</li>
 * <li>Minimizing negations: Applying De Morgan's Law heuristically for a
 * smaller formula</li>
 * </ul>
 * The first and the last two steps can be configured using the
 * {@link AdvancedSimplifierConfig}. Also, the handler and the rating function
 * can be configured. If no rating function is specified, the
 * {@link DefaultRatingFunction} is chosen.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class AdvancedSimplifier extends StatelessFormulaTransformation {

    private final AdvancedSimplifierConfig config;

    /**
     * Constructs a new simplifier with the advanced simplifier configuration
     * from the formula factory.
     * @param f the formula factory to generate new formulas
     */
    public AdvancedSimplifier(final FormulaFactory f) {
        this(f, (AdvancedSimplifierConfig) f.configurationFor(ConfigurationType.ADVANCED_SIMPLIFIER));
    }

    /**
     * Constructs a new simplifier with the given configuration.
     * @param f      the formula factory to generate new formulas
     * @param config The configuration for the advanced simplifier, including a
     *               handler, a rating function and flags for which steps
     *               should pe performed during the computation.
     */
    public AdvancedSimplifier(final FormulaFactory f, final AdvancedSimplifierConfig config) {
        super(f);
        this.config = config;
    }

    @Override
    public LNGResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        if (!handler.shouldResume(ADVANCED_SIMPLIFICATION_STARTED)) {
            return LNGResult.aborted(ADVANCED_SIMPLIFICATION_STARTED);
        }
        Formula simplified = formula;
        final SortedSet<Literal> backboneLiterals = new TreeSet<>();
        if (config.restrictBackbone) {
            final LNGResult<Backbone> backboneResult = BackboneGeneration.compute(f, Collections.singletonList(formula),
                    formula.variables(f), BackboneType.POSITIVE_AND_NEGATIVE, handler);
            if (!backboneResult.isSuccess()) {
                return LNGResult.aborted(backboneResult.getAbortionEvent());
            }
            final Backbone backbone = backboneResult.getResult();
            if (!backbone.isSat()) {
                return LNGResult.of(f.falsum());
            }
            backboneLiterals.addAll(backbone.getCompleteBackbone(f));
            simplified = formula.restrict(f, new Assignment(backboneLiterals));
        }
        final LNGResult<Formula> simplifyMinDnf = computeMinDnf(f, simplified, handler);
        if (!simplifyMinDnf.isSuccess()) {
            return LNGResult.aborted(simplifyMinDnf.getAbortionEvent());
        }
        simplified = simplifyWithRating(simplified, simplifyMinDnf.getResult(), config);
        if (config.factorOut) {
            final Formula factoredOut = simplified.transform(new FactorOutSimplifier(f, config.ratingFunction));
            simplified = simplifyWithRating(simplified, factoredOut, config);
        }
        if (config.restrictBackbone) {
            simplified = f.and(f.and(backboneLiterals), simplified);
        }
        if (config.simplifyNegations) {
            final Formula negationSimplified = simplified.transform(new NegationSimplifier(f));
            simplified = simplifyWithRating(simplified, negationSimplified, config);
        }
        return LNGResult.of(simplified);
    }

    private LNGResult<Formula> computeMinDnf(final FormulaFactory f, final Formula simplified,
                                             final ComputationHandler handler) {
        final LNGResult<PrimeResult> primeResult = PrimeCompiler.getWithMinimization()
                .compute(f, simplified, PrimeResult.CoverageType.IMPLICANTS_COMPLETE, handler);
        if (!primeResult.isSuccess()) {
            return LNGResult.aborted(primeResult.getAbortionEvent());
        }
        final List<SortedSet<Literal>> primeImplicants = primeResult.getResult().getPrimeImplicants();
        final LNGResult<UnsatResult<List<Formula>>> minimizedPIsResult =
                SmusComputation.computeSmusForFormulas(f, negateAllLiterals(f, primeImplicants),
                        Collections.singletonList(simplified), handler);
        if (!minimizedPIsResult.isSuccess()) {
            return LNGResult.aborted(minimizedPIsResult.getAbortionEvent());
        } else if (!minimizedPIsResult.getResult().isUnsat()) {
            return LNGResult.of(f.falsum());
        } else {
            final List<Formula> minimizedPIs = minimizedPIsResult.getResult().getResult();
            return LNGResult.of(f.or(
                    negateAllLiteralsInFormulas(f, minimizedPIs).stream().map(f::and).collect(Collectors.toList())));
        }
    }

    private List<Formula> negateAllLiterals(final FormulaFactory f, final Collection<SortedSet<Literal>> literalSets) {
        final List<Formula> result = new ArrayList<>();
        for (final SortedSet<Literal> literals : literalSets) {
            result.add(f.or(FormulaHelper.negateLiterals(f, literals, ArrayList::new)));
        }
        return result;
    }

    private List<Formula> negateAllLiteralsInFormulas(final FormulaFactory f, final Collection<Formula> formulas) {
        final List<Formula> result = new ArrayList<>();
        for (final Formula formula : formulas) {
            result.add(f.and(FormulaHelper.negateLiterals(f, formula.literals(f), ArrayList::new)));
        }
        return result;
    }

    private Formula simplifyWithRating(final Formula formula, final Formula simplifiedOneStep,
                                       final AdvancedSimplifierConfig config) {
        if (!config.useRatingFunction) {
            return simplifiedOneStep;
        }
        final Number ratingSimplified = config.ratingFunction.apply(simplifiedOneStep);
        final Number ratingFormula = config.ratingFunction.apply(formula);
        return ratingSimplified.intValue() < ratingFormula.intValue() ? simplifiedOneStep : formula;
    }
}
