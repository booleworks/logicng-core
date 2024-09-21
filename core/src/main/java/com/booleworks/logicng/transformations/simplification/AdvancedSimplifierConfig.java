// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.simplification;

import com.booleworks.logicng.configurations.Configuration;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;

/**
 * The configuration object for the {@link AdvancedSimplifier}.
 * @version 3.0.0
 * @since 2.3.0
 */

public class AdvancedSimplifierConfig extends Configuration {

    final boolean restrictBackbone;
    final boolean factorOut;
    final boolean simplifyNegations;
    final boolean useRatingFunction;
    final RatingFunction<?> ratingFunction;
    final MaxSatConfig maxSatConfig;

    @Override
    public String toString() {
        return "AdvancedSimplifierConfig{" +
                "restrictBackbone=" + restrictBackbone +
                ", factorOut=" + factorOut +
                ", simplifyNegations=" + simplifyNegations +
                ", useRatingFunction=" + useRatingFunction +
                ", ratingFunction=" + ratingFunction +
                ", maxSatConfig=" + maxSatConfig +
                '}';
    }

    /**
     * Constructs a new configuration with a given type.
     * @param builder the builder
     */
    private AdvancedSimplifierConfig(final Builder builder) {
        super(ConfigurationType.ADVANCED_SIMPLIFIER);
        restrictBackbone = builder.restrictBackbone;
        factorOut = builder.factorOut;
        simplifyNegations = builder.simplifyNegations;
        useRatingFunction = builder.useRatingFunction;
        ratingFunction = builder.ratingFunction;
        maxSatConfig = builder.maxSatConfig;
    }

    /**
     * Returns a new builder for the configuration.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder for the advanced simplifier configuration.
     */
    public static class Builder {

        private boolean restrictBackbone = true;
        private boolean factorOut = true;
        private boolean simplifyNegations = true;
        private boolean useRatingFunction = true;
        private RatingFunction<?> ratingFunction = DefaultRatingFunction.get();
        private MaxSatConfig maxSatConfig = MaxSatConfig.CONFIG_OLL;

        private Builder() {
            // Initialize only via factory
        }

        /**
         * Sets the flag for whether the formula should be restricted with the
         * backbone. The default is 'true'.
         * @param restrictBackbone flag for the restriction
         * @return the current builder
         */
        public Builder restrictBackbone(final boolean restrictBackbone) {
            this.restrictBackbone = restrictBackbone;
            return this;
        }

        /**
         * Sets the flag for whether the formula should be factorized. The
         * default is 'true'.
         * @param factorOut flag for the factorisation
         * @return the current builder
         */
        public Builder factorOut(final boolean factorOut) {
            this.factorOut = factorOut;
            return this;
        }

        /**
         * Sets the flag for whether negations shall be simplified. The default
         * is 'true'.
         * @param simplifyNegations flag
         * @return the current builder
         */
        public Builder simplifyNegations(final boolean simplifyNegations) {
            this.simplifyNegations = simplifyNegations;
            return this;
        }

        /**
         * Sets the flag for whether the rating function should be considered in
         * the main simplification steps.
         * @param useRatingFunction flag
         * @return the current builder
         */
        public Builder useRatingFunction(final boolean useRatingFunction) {
            this.useRatingFunction = useRatingFunction;
            return this;
        }

        /**
         * Sets the rating function. The aim of the simplification is to
         * minimize the formula with respect to this rating function, e.g.
         * finding a formula with a minimal number of symbols when represented
         * as string. The default is the {@code DefaultRatingFunction}.
         * @param ratingFunction the desired rating function
         * @return the current builder
         */
        public Builder ratingFunction(final RatingFunction<?> ratingFunction) {
            this.ratingFunction = ratingFunction;
            return this;
        }

        /**
         * Sets the configuration for the underlying MaxSAT solver which is
         * used to compute the minimum DNF via prime implicants and SMUS.
         * The default is the OLL algorithm.
         * @param maxSatConfig the MaxSAT solver configuration
         * @return the current builder
         */
        public Builder maxSatConfig(final MaxSatConfig maxSatConfig) {
            this.maxSatConfig = maxSatConfig;
            return this;
        }

        /**
         * Builds the configuration.
         * @return the configuration.
         */
        public AdvancedSimplifierConfig build() {
            return new AdvancedSimplifierConfig(this);
        }
    }
}
