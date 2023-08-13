// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.cnf;

import org.logicng.configurations.ConfigurationType;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
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

    protected final FormulaFactory f;
    protected final CNFConfig config;

    /**
     * Constructs a new CNF encoder with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public CNFEncoder(final FormulaFactory f, final CNFConfig config) {
        this.f = f;
        this.config = config;
    }

    /**
     * Constructs a new CNF encoder which uses the configuration of the formula factory.
     * @param f the formula factory
     */
    public CNFEncoder(final FormulaFactory f) {
        this(f, null);
    }

    /**
     * Encodes a formula to CNF.
     * @param formula formula
     * @return the CNF encoding of the formula
     */
    public Formula encode(final Formula formula) {
        switch (this.config().algorithm) {
            case FACTORIZATION:
                return formula.transform(new CNFFactorization());
            case TSEITIN:
                return formula.transform(new TseitinTransformation(this.config().atomBoundary));
            case PLAISTED_GREENBAUM:
                return formula.transform(new PlaistedGreenbaumTransformation(this.config().atomBoundary));
            case BDD:
                return formula.transform(new BDDCNFTransformation());
            case ADVANCED:
                final var factorizationHandler = new AdvancedFactorizationHandler();
                factorizationHandler.setBounds(this.config().distributionBoundary, this.config().createdClauseBoundary);
                final var advancedFactorization = new CNFFactorization(factorizationHandler);
                return this.advancedEncoding(formula, advancedFactorization);
            default:
                throw new IllegalStateException("Unknown CNF encoding algorithm: " + this.config().algorithm);
        }
    }

    /**
     * Encodes the given formula to CNF by first trying to use Factorization for the single sub-formulas.  When certain
     * user-provided boundaries are met, the method is switched to Tseitin or Plaisted &amp; Greenbaum.
     * @param formula               the formula
     * @param advancedFactorization the advanced factorization method
     * @return the CNF encoding of the formula
     */
    protected Formula advancedEncoding(final Formula formula, final CNFFactorization advancedFactorization) {
        if (formula.type() == FType.AND) {
            final List<Formula> operands = new ArrayList<>(formula.numberOfOperands());
            for (final Formula op : formula) {
                operands.add(singleAdvancedEncoding(op, advancedFactorization));
            }
            return this.f.and(operands);
        }
        return singleAdvancedEncoding(formula, advancedFactorization);
    }

    protected Formula singleAdvancedEncoding(final Formula formula, final CNFFactorization advancedFactorization) {
        Formula result = formula.transform(advancedFactorization);
        if (result == null) {
            switch (this.config().fallbackAlgorithmForAdvancedEncoding) {
                case TSEITIN:
                    result = formula.transform(new TseitinTransformation(this.config().atomBoundary));
                    break;
                case PLAISTED_GREENBAUM:
                    result = formula.transform(new PlaistedGreenbaumTransformation(this.config().atomBoundary));
                    break;
                default:
                    throw new IllegalStateException("Invalid fallback CNF encoding algorithm: " + this.config().fallbackAlgorithmForAdvancedEncoding);
            }
        }
        return result;
    }

    /**
     * Returns the current configuration of this encoder.  If the encoder was constructed with a given configuration, this
     * configuration will always be used.  Otherwise, the current configuration from the formula factory is used.
     * @return the current configuration of
     */
    public CNFConfig config() {
        return this.config != null ? this.config : (CNFConfig) this.f.configurationFor(ConfigurationType.CNF);
    }

    @Override
    public String toString() {
        return this.config().toString();
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
