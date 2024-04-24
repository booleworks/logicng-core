// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions.modelenumeration;

import com.booleworks.logicng.configurations.Configuration;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.handlers.ModelEnumerationHandler;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.splitprovider.MostCommonVariablesProvider;

/**
 * The configuration object for the {@link ModelEnumerationFunction}.
 * @version 3.0.0
 * @since 3.0.0
 */
public class ModelEnumerationConfig extends Configuration {

    final ModelEnumerationHandler handler;
    final ModelEnumerationStrategy strategy;

    /**
     * Constructs a new configuration with a given type.
     * @param builder the builder
     */
    private ModelEnumerationConfig(final Builder builder) {
        super(ConfigurationType.MODEL_ENUMERATION);
        handler = builder.handler;
        strategy = builder.strategy;
    }

    /**
     * Returns a new builder for the configuration.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder for a model enumeration configuration.
     * @version 3.0.0
     * @since 3.0.0
     */
    public static class Builder {
        private ModelEnumerationHandler handler = null;
        private ModelEnumerationStrategy strategy = DefaultModelEnumerationStrategy.builder().build();

        private Builder() {
            // Initialize only via factory
        }

        /**
         * Sets the model enumeration handler for this function. The default is
         * no handler.
         * @param handler the handler, may be {@code null}
         * @return the current builder
         */
        public Builder handler(final ModelEnumerationHandler handler) {
            this.handler = handler;
            return this;
        }

        /**
         * Sets the model enumeration strategy for this function. The default is
         * the {@link DefaultModelEnumerationStrategy} with the
         * {@link MostCommonVariablesProvider} and a maximum number of models of
         * 500.
         * <p>
         * In case of {@code null} the computation will fall back to the default
         * model enumeration without split assignments
         * @param strategy the strategy
         * @return the current builder
         */
        public Builder strategy(final ModelEnumerationStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        /**
         * Builds the model enumeration configuration with the current builder's
         * configuration.
         * @return the model enumeration configuration
         */
        public ModelEnumerationConfig build() {
            return new ModelEnumerationConfig(this);
        }
    }
}
