// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.FACTORIZATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.DISTRIBUTION_PERFORMED;
import static com.booleworks.logicng.transformations.cnf.PlaistedGreenbaumTransformation.PgState;

import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaTransformation;
import com.booleworks.logicng.formulas.implementation.cached.CachingFormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.FactorizationCreatedClauseEvent;
import com.booleworks.logicng.handlers.events.LngEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * An encoder for conjunctive normal form (CNF).
 * @version 3.0.0
 * @since 1.1
 */
public class CnfEncoder {

    /**
     * Encodes a formula to CNF.
     * @param f       the formula factory to generate new formulas
     * @param formula formula
     * @return the CNF encoding of the formula
     */
    public static Formula encode(final FormulaFactory f, final Formula formula) {
        return encode(f, formula, null);
    }

    /**
     * Encodes a formula to CNF.
     * @param f          the formula factory to generate new formulas
     * @param formula    formula
     * @param initConfig the configuration for the encoder
     * @return the CNF encoding of the formula
     */
    public static Formula encode(final FormulaFactory f, final Formula formula, final CnfConfig initConfig) {
        final CnfConfig config =
                initConfig != null ? initConfig : (CnfConfig) f.configurationFor(ConfigurationType.CNF);
        switch (config.algorithm) {
            case FACTORIZATION:
                return formula.transform(new CnfFactorization(f));
            case TSEITIN:
                return formula.transform(getTseitinTransformation(f, config));
            case PLAISTED_GREENBAUM:
                return formula.transform(getPgTransformation(f, config));
            case BDD:
                return formula.transform(new BddCnfTransformation(f));
            case ADVANCED:
                return advancedEncoding(f, formula, config);
            default:
                throw new IllegalStateException("Unknown CNF encoding algorithm: " + config.algorithm);
        }
    }

    /**
     * Encodes the given formula to CNF by first trying to use Factorization for
     * the single sub-formulas. When certain user-provided boundaries are met,
     * the method is switched to Tseitin or Plaisted &amp; Greenbaum.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula
     * @param config  the CNF configuration
     * @return the CNF encoding of the formula
     */
    protected static Formula advancedEncoding(final FormulaFactory f, final Formula formula, final CnfConfig config) {
        final var factorizationHandler = new AdvancedFactorizationHandler(config.distributionBoundary, config.createdClauseBoundary);
        final var advancedFactorization = new CnfFactorization(f);
        final FormulaTransformation fallbackTransformation;
        switch (config.fallbackAlgorithmForAdvancedEncoding) {
            case TSEITIN:
                fallbackTransformation = getTseitinTransformation(f, config);
                break;
            case PLAISTED_GREENBAUM:
                fallbackTransformation = getPgTransformation(f, config);
                break;
            default:
                throw new IllegalStateException(
                        "Invalid fallback CNF encoding algorithm: " + config.fallbackAlgorithmForAdvancedEncoding);
        }
        if (formula.getType() == FType.AND) {
            final List<Formula> operands = new ArrayList<>(formula.numberOfOperands());
            for (final Formula op : formula) {
                operands.add(singleAdvancedEncoding(op, advancedFactorization, factorizationHandler, fallbackTransformation));
            }
            return f.and(operands);
        }
        return singleAdvancedEncoding(formula, advancedFactorization, factorizationHandler, fallbackTransformation);
    }

    protected static Formula singleAdvancedEncoding(final Formula formula,
                                                    final CnfFactorization advancedFactorization,
                                                    final AdvancedFactorizationHandler factorizationHandler,
                                                    final FormulaTransformation fallback) {
        final LngResult<Formula> result = formula.transform(advancedFactorization, factorizationHandler);
        if (result.isSuccess()) {
            return result.getResult();
        } else {
            return formula.transform(fallback);
        }
    }

    private static FormulaTransformation getTseitinTransformation(final FormulaFactory f, final CnfConfig config) {
        if (f instanceof CachingFormulaFactory) {
            return new TseitinTransformation(f, config.atomBoundary);
        } else {
            return new TseitinTransformation(f, config.atomBoundary, new TseitinTransformation.TseitinState());
        }
    }

    private static FormulaTransformation getPgTransformation(final FormulaFactory f, final CnfConfig config) {
        if (f instanceof CachingFormulaFactory) {
            return new PlaistedGreenbaumTransformation(f, config.atomBoundary);
        } else {
            return new PlaistedGreenbaumTransformation(f, config.atomBoundary, new PgState());
        }
    }

    /**
     * The factorization handler for the advanced CNF encoding.
     */
    protected static class AdvancedFactorizationHandler implements ComputationHandler {

        private final int distributionBoundary;
        private final int createdClauseBoundary;
        private boolean canceled = false;
        private int currentDistributions = 0;
        private int currentClauses = 0;

        protected AdvancedFactorizationHandler(final int distributionBoundary, final int createdClauseBoundary) {
            this.distributionBoundary = distributionBoundary;
            this.createdClauseBoundary = createdClauseBoundary;
        }

        @Override
        public boolean shouldResume(final LngEvent event) {
            if (event == FACTORIZATION_STARTED) {
                currentDistributions = 0;
                currentClauses = 0;
            } else if (event == DISTRIBUTION_PERFORMED) {
                canceled = distributionBoundary != -1 && ++currentDistributions > distributionBoundary;
            } else if (event instanceof FactorizationCreatedClauseEvent) {
                canceled = createdClauseBoundary != -1 && ++currentClauses > createdClauseBoundary;
            }
            return !canceled;
        }
    }
}
