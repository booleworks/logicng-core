// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.cnf;

import static org.logicng.transformations.cnf.PlaistedGreenbaumTransformation.PGState;
import static org.logicng.transformations.cnf.TseitinTransformation.TseitinState;

import org.logicng.configurations.ConfigurationType;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.FormulaTransformation;
import org.logicng.formulas.implementation.cached.CachingFormulaFactory;
import org.logicng.formulas.implementation.noncaching.NonCachingFormulaFactory;
import org.logicng.handlers.ComputationHandler;
import org.logicng.handlers.FactorizationHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * An encoder for conjunctive normal form (CNF).
 * @version 3.0.0
 * @since 1.1
 */
public class CNFEncoder {

    /**
     * Encodes a formula to CNF.
     * @param formula formula
     * @return the CNF encoding of the formula
     */
    public static Formula encode(final Formula formula) {
        return encode(formula, formula.factory(), null);
    }

    /**
     * Encodes a formula to CNF.
     * @param formula formula
     * @param f       the formula factory to generate new formulas
     * @return the CNF encoding of the formula
     */
    public static Formula encode(final Formula formula, final FormulaFactory f) {
        return encode(formula, f, null);
    }

    /**
     * Encodes a formula to CNF.
     * @param formula    formula
     * @param initConfig the configuration for the encoder
     * @return the CNF encoding of the formula
     */
    public static Formula encode(final Formula formula, final CNFConfig initConfig) {
        return encode(formula, formula.factory(), initConfig);
    }

    /**
     * Encodes a formula to CNF.
     * @param formula    formula
     * @param f          the formula factory to generate new formulas
     * @param initConfig the configuration for the encoder
     * @return the CNF encoding of the formula
     */
    public static Formula encode(final Formula formula, final FormulaFactory f, final CNFConfig initConfig) {
        final CNFConfig config = initConfig != null ? initConfig : (CNFConfig) f.configurationFor(ConfigurationType.CNF);
        switch (config.algorithm) {
            case FACTORIZATION:
                return formula.transform(new CNFFactorization(f));
            case TSEITIN:
                return formula.transform(getTseitinTransformation(f, config));
            case PLAISTED_GREENBAUM:
                return formula.transform(getPgTransformation(f, config));
            case BDD:
                return formula.transform(new BDDCNFTransformation(f));
            case ADVANCED:
                return advancedEncoding(formula, f, config);
            default:
                throw new IllegalStateException("Unknown CNF encoding algorithm: " + config.algorithm);
        }
    }

    /**
     * Encodes the given formula to CNF by first trying to use Factorization for the single sub-formulas.  When certain
     * user-provided boundaries are met, the method is switched to Tseitin or Plaisted &amp; Greenbaum.
     * @param formula the formula
     * @param f       the formula factory to generate new formulas
     * @param config  the CNF configuration
     * @return the CNF encoding of the formula
     */
    protected static Formula advancedEncoding(final Formula formula, final FormulaFactory f, final CNFConfig config) {
        final var factorizationHandler = new AdvancedFactorizationHandler();
        factorizationHandler.setBounds(config.distributionBoundary, config.createdClauseBoundary);
        final var advancedFactorization = new CNFFactorization(f, factorizationHandler, true);
        final FormulaTransformation fallbackTransformation;
        switch (config.fallbackAlgorithmForAdvancedEncoding) {
            case TSEITIN:
                fallbackTransformation = getTseitinTransformation(f, config);
                break;
            case PLAISTED_GREENBAUM:
                fallbackTransformation = getPgTransformation(f, config);
                break;
            default:
                throw new IllegalStateException("Invalid fallback CNF encoding algorithm: " + config.fallbackAlgorithmForAdvancedEncoding);
        }
        if (formula.type() == FType.AND) {
            final List<Formula> operands = new ArrayList<>(formula.numberOfOperands());
            for (final Formula op : formula) {
                operands.add(singleAdvancedEncoding(op, advancedFactorization, fallbackTransformation));
            }
            return f.and(operands);
        }
        return singleAdvancedEncoding(formula, advancedFactorization, fallbackTransformation);
    }

    protected static Formula singleAdvancedEncoding(final Formula formula, final CNFFactorization advancedFactorization, final FormulaTransformation fallback) {
        Formula result = formula.transform(advancedFactorization);
        if (result == null) {
            result = formula.transform(fallback);
        }
        return result;
    }

    private static FormulaTransformation getTseitinTransformation(final FormulaFactory f, final CNFConfig config) {
        if (f instanceof CachingFormulaFactory) {
            return new TseitinTransformation((CachingFormulaFactory) f, config.atomBoundary);
        } else {
            return new TseitinTransformation((NonCachingFormulaFactory) f, config.atomBoundary, new TseitinState());
        }
    }

    private static FormulaTransformation getPgTransformation(final FormulaFactory f, final CNFConfig config) {
        if (f instanceof CachingFormulaFactory) {
            return new PlaistedGreenbaumTransformation((CachingFormulaFactory) f, config.atomBoundary);
        } else {
            return new PlaistedGreenbaumTransformation((NonCachingFormulaFactory) f, config.atomBoundary, new PGState());
        }
    }

    /**
     * The factorization handler for the advanced CNF encoding.
     */
    protected static class AdvancedFactorizationHandler extends ComputationHandler implements FactorizationHandler {

        protected int distributionBoundary;
        protected int createdClauseBoundary;
        protected int currentDistributions;
        protected int currentClauses;

        protected void setBounds(final int distributionBoundary, final int createdClauseBoundary) {
            this.distributionBoundary = distributionBoundary;
            this.createdClauseBoundary = createdClauseBoundary;
        }

        @Override
        public void started() {
            super.started();
            this.currentDistributions = 0;
            this.currentClauses = 0;
        }

        @Override
        public boolean performedDistribution() {
            this.aborted = this.distributionBoundary != -1 && ++this.currentDistributions > this.distributionBoundary;
            return !this.aborted;
        }

        @Override
        public boolean createdClause(final Formula clause) {
            this.aborted = this.createdClauseBoundary != -1 && ++this.currentClauses > this.createdClauseBoundary;
            return !this.aborted;
        }
    }
}
