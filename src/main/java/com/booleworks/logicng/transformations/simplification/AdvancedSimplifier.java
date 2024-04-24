// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneGeneration;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.explanations.smus.SmusComputation;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.Handler;
import com.booleworks.logicng.handlers.OptimizationHandler;
import com.booleworks.logicng.primecomputation.PrimeCompiler;
import com.booleworks.logicng.primecomputation.PrimeResult;
import com.booleworks.logicng.transformations.AbortableFormulaTransformation;
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
public final class AdvancedSimplifier extends AbortableFormulaTransformation<OptimizationHandler> {

    private final AdvancedSimplifierConfig config;

    /**
     * Constructs a new simplifier with the advanced simplifier configuration
     * from the formula factory.
     * @param f the formula factory to generate new formulas
     */
    public AdvancedSimplifier(final FormulaFactory f) {
        this(f, (AdvancedSimplifierConfig) f.configurationFor(ConfigurationType.ADVANCED_SIMPLIFIER), null);
    }

    /**
     * Constructs a new simplifier with the advanced simplifier configuration
     * from the formula factory.
     * @param f       the formula factory to generate new formulas
     * @param handler the optimization handler to abort the simplification
     */
    public AdvancedSimplifier(final FormulaFactory f, final OptimizationHandler handler) {
        this(f, (AdvancedSimplifierConfig) f.configurationFor(ConfigurationType.ADVANCED_SIMPLIFIER), handler);
    }

    /**
     * Constructs a new simplifier with the given configuration.
     * @param f      the formula factory to generate new formulas
     * @param config The configuration for the advanced simplifier, including a
     *               handler, a rating function and flags for which steps should
     *               pe performed during the computation.
     */
    public AdvancedSimplifier(final FormulaFactory f, final AdvancedSimplifierConfig config) {
        this(f, config, null);
    }

    /**
     * Constructs a new simplifier with the given configuration.
     * @param f       the formula factory to generate new formulas
     * @param handler the optimization handler to abort the simplification
     * @param config  The configuration for the advanced simplifier, including a
     *                handler, a rating function and flags for which steps
     *                should pe performed during the computation.
     */
    public AdvancedSimplifier(final FormulaFactory f, final AdvancedSimplifierConfig config,
                              final OptimizationHandler handler) {
        super(f, handler);
        this.config = config;
    }

    @Override
    public Formula apply(final Formula formula) {
        Handler.start(handler);
        Formula simplified = formula;
        final SortedSet<Literal> backboneLiterals = new TreeSet<>();
        if (config.restrictBackbone) {
            final Backbone backbone = BackboneGeneration.compute(f, Collections.singletonList(formula),
                    formula.variables(f), BackboneType.POSITIVE_AND_NEGATIVE, OptimizationHandler.satHandler(handler));
            if (backbone == null || Handler.aborted(handler)) {
                return null;
            }
            if (!backbone.isSat()) {
                return f.falsum();
            }
            backboneLiterals.addAll(backbone.getCompleteBackbone(f));
            simplified = formula.restrict(f, new Assignment(backboneLiterals));
        }
        final Formula simplifyMinDnf = computeMinDnf(f, simplified);
        if (simplifyMinDnf == null) {
            return null;
        }
        simplified = simplifyWithRating(simplified, simplifyMinDnf, config);
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
        return simplified;
    }

    private Formula computeMinDnf(final FormulaFactory f, Formula simplified) {
        final PrimeResult primeResult =
                PrimeCompiler.getWithMinimization().compute(f, simplified, PrimeResult.CoverageType.IMPLICANTS_COMPLETE,
                        handler);
        if (primeResult == null || Handler.aborted(handler)) {
            return null;
        }
        final List<SortedSet<Literal>> primeImplicants = primeResult.getPrimeImplicants();
        final List<Formula> minimizedPIs =
                SmusComputation.computeSmusForFormulas(f, negateAllLiterals(f, primeImplicants),
                        Collections.singletonList(simplified), handler);
        if (minimizedPIs == null || Handler.aborted(handler)) {
            return null;
        }
        simplified =
                f.or(negateAllLiteralsInFormulas(f, minimizedPIs).stream().map(f::and).collect(Collectors.toList()));
        return simplified;
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
