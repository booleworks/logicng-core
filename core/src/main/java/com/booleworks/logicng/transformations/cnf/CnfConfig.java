// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import com.booleworks.logicng.configurations.Configuration;
import com.booleworks.logicng.configurations.ConfigurationType;

/**
 * The configuration object for the CNF encoding.
 * @version 1.2
 * @since 1.1
 */
public final class CnfConfig extends Configuration {

    /**
     * The algorithm for the CNF encoding.
     */
    public enum Algorithm {
        FACTORIZATION,
        TSEITIN,
        PLAISTED_GREENBAUM,
        ADVANCED,
        BDD
    }

    final Algorithm algorithm;
    final Algorithm fallbackAlgorithmForAdvancedEncoding;
    final int distributionBoundary;
    final int createdClauseBoundary;
    final int atomBoundary;

    /**
     * Constructs a new configuration with a given type.
     * @param builder the builder
     */
    private CnfConfig(final Builder builder) {
        super(ConfigurationType.CNF);
        algorithm = builder.algorithm;
        fallbackAlgorithmForAdvancedEncoding = builder.fallbackAlgorithmForAdvancedEncoding;
        distributionBoundary = builder.distributionBoundary;
        createdClauseBoundary = builder.createdClauseBoundary;
        atomBoundary = builder.atomBoundary;
    }

    /**
     * Returns a new builder for the configuration.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(String.format("CnfConfig{%n"));
        sb.append("algorithm=").append(algorithm).append(System.lineSeparator());
        sb.append("fallbackAlgorithmForAdvancedEncoding=").append(fallbackAlgorithmForAdvancedEncoding)
                .append(System.lineSeparator());
        sb.append("distributedBoundary=").append(distributionBoundary).append(System.lineSeparator());
        sb.append("createdClauseBoundary=").append(createdClauseBoundary).append(System.lineSeparator());
        sb.append("atomBoundary=").append(atomBoundary).append(System.lineSeparator());
        sb.append("}").append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * The builder for a CNF configuration.
     */
    public static class Builder {

        private Algorithm algorithm = Algorithm.ADVANCED;
        private Algorithm fallbackAlgorithmForAdvancedEncoding = Algorithm.TSEITIN;
        private int distributionBoundary = -1;
        private int createdClauseBoundary = 1000;
        private int atomBoundary = 12;

        private Builder() {
            // Initialize only via factory
        }

        /**
         * Sets the algorithm for the CNF encoding. The default value is
         * {@code ADVANCED}.
         * @param algorithm the algorithm for the CNF encoding
         * @return the builder
         */
        public Builder algorithm(final Algorithm algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        /**
         * Sets the fallback algorithm for the advanced CNF encoding. When the
         * boundaries for the factorization are met, the encoding switches to
         * this algorithm. The default value is {@code TSEITIN}.
         * @param fallbackAlgorithmForAdvancedEncoding the fallback algorithm
         *                                             for the advanced CNF
         *                                             encoding
         * @return the builder
         */
        public Builder fallbackAlgorithmForAdvancedEncoding(final Algorithm fallbackAlgorithmForAdvancedEncoding) {
            if (fallbackAlgorithmForAdvancedEncoding != Algorithm.TSEITIN &&
                    fallbackAlgorithmForAdvancedEncoding != Algorithm.PLAISTED_GREENBAUM) {
                throw new IllegalArgumentException(
                        "Fallback algorithm for advanced CNF encoding must be one of Tseitin or " +
                                "Plaisted & Greenbaum");
            }
            this.fallbackAlgorithmForAdvancedEncoding = fallbackAlgorithmForAdvancedEncoding;
            return this;
        }

        /**
         * Sets the boundary how many distributions should be performed in the
         * factorization before the method is switched (in the {@code ADVANCED}
         * encoding). Disable this boundary by setting it to -1. The default
         * value is -1.
         * @param distributionBoundary the distribution boundary
         * @return the builder
         */
        public Builder distributionBoundary(final int distributionBoundary) {
            this.distributionBoundary = distributionBoundary;
            return this;
        }

        /**
         * Sets the boundary how many clauses should be created in the
         * factorization before the method is switched (in the {@code ADVANCED}
         * encoding). Disable this boundary by setting it to -1. The default
         * value is 1000.
         * @param createdClauseBoundary the clause creation boundary
         * @return the builder
         */
        public Builder createdClauseBoundary(final int createdClauseBoundary) {
            this.createdClauseBoundary = createdClauseBoundary;
            return this;
        }

        /**
         * Sets the boundary for how many atoms in a formula factorization is
         * performed in Tseitin and Plaisted &amp; Greenbaum. The default value
         * is 12.
         * @param atomBoundary the atom boundary
         * @return the builder
         */
        public Builder atomBoundary(final int atomBoundary) {
            this.atomBoundary = atomBoundary;
            return this;
        }

        /**
         * Builds the configuration.
         * @return the configuration.
         */
        public CnfConfig build() {
            return new CnfConfig(this);
        }
    }
}
