// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import com.booleworks.logicng.configurations.Configuration;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.formulas.implementation.noncaching.NonCachingFormulaFactory;
import com.booleworks.logicng.formulas.printer.DefaultStringRepresentation;
import com.booleworks.logicng.formulas.printer.FormulaStringRepresentation;

import java.util.function.Supplier;

/**
 * The configuration object for a formula factory.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class FormulaFactoryConfig extends Configuration {

    /**
     * Strategies for combining formulas of different formula factories.
     * Possible values are:
     * <ul>
     * <li>{@link #PANIC}: If an operand of a formula comes from a different
     * formula factory an {@link UnsupportedOperationException} is thrown. This
     * also means, that cached and non-cached formulas cannot be mixed.</li>
     * <li>{@link #IMPORT}: Operands from different formula factories are
     * {@link FormulaFactory#importFormula(Formula) imported} before the new
     * formula is constructed. This works also when importing a cached formula
     * into a non-caching formula factory and vice versa.</li>
     * <li>{@link #USE_BUT_NO_IMPORT}: This strategy only works for the
     * {@link NonCachingFormulaFactory}. In this case, a non-caching formula
     * factory can use a cached or uncached formula from another factory without
     * importing it.</li>
     * </ul>
     */
    public enum FormulaMergeStrategy {
        PANIC,
        IMPORT,
        USE_BUT_NO_IMPORT
    }

    final String name;
    final FormulaMergeStrategy formulaMergeStrategy;
    final Supplier<FormulaStringRepresentation> stringRepresentation;
    final boolean simplifyComplementaryOperands;

    private FormulaFactoryConfig(final Builder builder) {
        super(ConfigurationType.FORMULA_FACTORY);
        name = builder.name;
        formulaMergeStrategy = builder.formulaMergeStrategy;
        stringRepresentation = builder.stringRepresentation;
        simplifyComplementaryOperands = builder.simplifyComplementaryOperands;
    }

    /**
     * Returns a new builder for the configuration.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder for a formula factory configuration.
     * @version 2.0.0
     * @since 2.0.0
     */
    public static class Builder {
        private String name = "";
        private FormulaMergeStrategy formulaMergeStrategy = FormulaMergeStrategy.PANIC;
        private Supplier<FormulaStringRepresentation> stringRepresentation = DefaultStringRepresentation::new;
        private boolean simplifyComplementaryOperands = true;

        /**
         * Sets the name of this formula factory. The default is an empty
         * string.
         * <p>
         * Setting a name is only useful when multiple formula factories are
         * used in the same context. The name is used to create individual names
         * for generated variables s.t. the generated variables of different
         * formula factories will not clash.
         * @param name the name
         * @return the builder
         */
        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the strategy defining how to proceed if one or more operands of
         * a formula were created by another formula factory. The default value
         * is {@link FormulaMergeStrategy#PANIC}.
         * @param formulaMergeStrategy the strategy
         * @return the builder
         */
        public Builder formulaMergeStrategy(final FormulaMergeStrategy formulaMergeStrategy) {
            this.formulaMergeStrategy = formulaMergeStrategy;
            return this;
        }

        /**
         * Sets the formula string representation which should be used by
         * default for creating strings from a formula. The default is
         * {@link DefaultStringRepresentation}.
         * @param stringRepresentation the formula string representation
         * @return the builder
         */
        public Builder stringRepresentation(final Supplier<FormulaStringRepresentation> stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
            return this;
        }

        /**
         * Sets the flag whether trivial contradictions and tautologies are
         * simplified in formulas. If set to false, a formula like
         * {@code A & ~A} or {@code A | ~A} can be generated on the formula
         * factory. If set to true, the formulas will be simplified to
         * {@code $false} or {@code true} respectively. The default is
         * {@code true}.
         * @param simplifyComplementaryOperands the flag whether to simplify
         *                                      trivial contradictions and
         *                                      tautologies or not
         * @return the builder
         */
        public Builder simplifyComplementaryOperands(final boolean simplifyComplementaryOperands) {
            this.simplifyComplementaryOperands = simplifyComplementaryOperands;
            return this;
        }

        /**
         * Builds the configuration.
         * @return the configuration.
         */
        public FormulaFactoryConfig build() {
            return new FormulaFactoryConfig(this);
        }
    }
}
