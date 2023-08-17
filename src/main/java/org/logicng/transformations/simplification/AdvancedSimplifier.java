// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.simplification;

import static org.logicng.handlers.Handler.aborted;
import static org.logicng.handlers.Handler.start;
import static org.logicng.handlers.OptimizationHandler.satHandler;

import org.logicng.backbones.Backbone;
import org.logicng.backbones.BackboneGeneration;
import org.logicng.backbones.BackboneType;
import org.logicng.configurations.ConfigurationType;
import org.logicng.datastructures.Assignment;
import org.logicng.explanations.smus.SmusComputation;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.handlers.OptimizationHandler;
import org.logicng.primecomputation.PrimeCompiler;
import org.logicng.primecomputation.PrimeResult;
import org.logicng.transformations.AbortableFormulaTransformation;
import org.logicng.util.FormulaHelper;

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
 * The aim of the simplification is to minimize the formula with respect to a given rating function,
 * e.g. finding a formula with a minimal number of symbols when represented as string.
 * <p>
 * The simplification performs the following steps:
 * <ul>
 *     <li>Restricting the formula to its backbone</li>
 *     <li>Computation of all prime implicants</li>
 *     <li>Finding a minimal coverage (by finding a smallest MUS)</li>
 *     <li>Building a DNF from the minimal prime implicant coverage</li>
 *     <li>Factoring out: Applying the Distributive Law heuristically for a smaller formula</li>
 *     <li>Minimizing negations: Applying De Morgan's Law heuristically for a smaller formula</li>
 * </ul>
 * The first and the last two steps can be configured using the {@link AdvancedSimplifierConfig}. Also, the handler and the rating
 * function can be configured. If no rating function is specified, the {@link DefaultRatingFunction} is chosen.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class AdvancedSimplifier extends AbortableFormulaTransformation<OptimizationHandler> {

    private final AdvancedSimplifierConfig config;

    /**
     * Constructs a new simplifier with the advanced simplifier configuration from the formula factory.
     * @param f the formula factory to generate new formulas
     */
    public AdvancedSimplifier(final FormulaFactory f) {
        this(f, (AdvancedSimplifierConfig) f.configurationFor(ConfigurationType.ADVANCED_SIMPLIFIER), null);
    }

    /**
     * Constructs a new simplifier with the advanced simplifier configuration from the formula factory.
     * @param f       the formula factory to generate new formulas
     * @param handler the optimization handler to abort the simplification
     */
    public AdvancedSimplifier(final FormulaFactory f, final OptimizationHandler handler) {
        this(f, (AdvancedSimplifierConfig) f.configurationFor(ConfigurationType.ADVANCED_SIMPLIFIER), handler);
    }

    /**
     * Constructs a new simplifier with the given configuration.
     * @param f      the formula factory to generate new formulas
     * @param config The configuration for the advanced simplifier, including a handler, a rating function and flags
     *               for which steps should pe performed during the computation.
     */
    public AdvancedSimplifier(final FormulaFactory f, final AdvancedSimplifierConfig config) {
        this(f, config, null);
    }

    /**
     * Constructs a new simplifier with the given configuration.
     * @param f       the formula factory to generate new formulas
     * @param handler the optimization handler to abort the simplification
     * @param config  The configuration for the advanced simplifier, including a handler, a rating function and flags
     *                for which steps should pe performed during the computation.
     */
    public AdvancedSimplifier(final FormulaFactory f, final AdvancedSimplifierConfig config, final OptimizationHandler handler) {
        super(f, handler);
        this.config = config;
    }

    @Override
    public Formula apply(final Formula formula) {
        start(handler);
        Formula simplified = formula;
        final SortedSet<Literal> backboneLiterals = new TreeSet<>();
        if (config.restrictBackbone) {
            final Backbone backbone = BackboneGeneration
                    .compute(Collections.singletonList(formula), formula.variables(), BackboneType.POSITIVE_AND_NEGATIVE, satHandler(handler));
            if (backbone == null || aborted(handler)) {
                return null;
            }
            if (!backbone.isSat()) {
                return f.falsum();
            }
            backboneLiterals.addAll(backbone.getCompleteBackbone());
            simplified = formula.restrict(new Assignment(backboneLiterals), f);
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
                PrimeCompiler.getWithMinimization().compute(simplified, PrimeResult.CoverageType.IMPLICANTS_COMPLETE, handler);
        if (primeResult == null || aborted(handler)) {
            return null;
        }
        final List<SortedSet<Literal>> primeImplicants = primeResult.getPrimeImplicants();
        final List<Formula> minimizedPIs = SmusComputation.computeSmusForFormulas(negateAllLiterals(primeImplicants, f),
                Collections.singletonList(simplified), f, handler);
        if (minimizedPIs == null || aborted(handler)) {
            return null;
        }
        simplified = f.or(negateAllLiteralsInFormulas(minimizedPIs, f).stream().map(f::and).collect(Collectors.toList()));
        return simplified;
    }

    private List<Formula> negateAllLiterals(final Collection<SortedSet<Literal>> literalSets, final FormulaFactory f) {
        final List<Formula> result = new ArrayList<>();
        for (final SortedSet<Literal> literals : literalSets) {
            result.add(f.or(FormulaHelper.negateLiterals(literals, ArrayList::new)));
        }
        return result;
    }

    private List<Formula> negateAllLiteralsInFormulas(final Collection<Formula> formulas, final FormulaFactory f) {
        final List<Formula> result = new ArrayList<>();
        for (final Formula formula : formulas) {
            result.add(f.and(FormulaHelper.negateLiterals(formula.literals(), ArrayList::new)));
        }
        return result;
    }

    private Formula simplifyWithRating(final Formula formula, final Formula simplifiedOneStep, final AdvancedSimplifierConfig config) {
        if (!config.useRatingFunction) {
            return simplifiedOneStep;
        }
        final Number ratingSimplified = config.ratingFunction.apply(simplifiedOneStep);
        final Number ratingFormula = config.ratingFunction.apply(formula);
        return ratingSimplified.intValue() < ratingFormula.intValue() ? simplifiedOneStep : formula;
    }
}
