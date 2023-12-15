// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.solvers.maxsat.algorithms;

import org.logicng.configurations.Configuration;
import org.logicng.configurations.ConfigurationType;

import java.io.PrintStream;

/**
 * The configuration object for a MaxSAT solver.
 * @version 2.0.0
 * @since 1.0
 */
public final class MaxSATConfig extends Configuration {

    /**
     * The incremental strategy for cardinality and pseudo-boolean constraints.
     */
    public enum IncrementalStrategy {
        NONE, ITERATIVE
    }

    /**
     * The AMO encoding.
     */
    public enum AMOEncoding {
        LADDER
    }

    /**
     * The pseudo Boolean encoding.
     */
    public enum PBEncoding {
        SWC
    }

    /**
     * The cardinality constraint encoding.
     */
    public enum CardinalityEncoding {
        TOTALIZER, MTOTALIZER
    }

    /**
     * The weight strategy.
     */
    public enum WeightStrategy {
        NONE, NORMAL, DIVERSIFY
    }

    /**
     * The verbosity of the solver.
     */
    public enum Verbosity {
        NONE, SOME
    }

    final IncrementalStrategy incrementalStrategy;
    final AMOEncoding amoEncoding;
    final PBEncoding pbEncoding;
    final CardinalityEncoding cardinalityEncoding;
    final WeightStrategy weightStrategy;
    final Verbosity verbosity;
    final PrintStream output;
    final boolean symmetry;
    final int limit;
    final boolean bmo;

    /**
     * Constructor for a MaxSAT configuration.
     * @param builder the builder
     */
    private MaxSATConfig(final Builder builder) {
        super(ConfigurationType.MAXSAT);
        incrementalStrategy = builder.incrementalStrategy;
        amoEncoding = builder.amoEncoding;
        pbEncoding = builder.pbEncoding;
        cardinalityEncoding = builder.cardinalityEncoding;
        weightStrategy = builder.weightStrategy;
        verbosity = builder.verbosity;
        output = builder.output;
        symmetry = builder.symmetry;
        limit = builder.limit;
        bmo = builder.bmo;
    }

    /**
     * Copy Constructor with another cardinality encoding.
     * @param config              the configuration to copy
     * @param cardinalityEncoding the cardinality encoding
     */
    public MaxSATConfig(final MaxSATConfig config, final CardinalityEncoding cardinalityEncoding) {
        super(ConfigurationType.MAXSAT);
        incrementalStrategy = config.incrementalStrategy;
        amoEncoding = config.amoEncoding;
        pbEncoding = config.pbEncoding;
        this.cardinalityEncoding = cardinalityEncoding;
        weightStrategy = config.weightStrategy;
        verbosity = config.verbosity;
        output = config.output;
        symmetry = config.symmetry;
        limit = config.limit;
        bmo = config.bmo;
    }

    /**
     * Copy Constructor with another incrementality strategy.
     * @param config              the configuration to copy
     * @param incrementalStrategy the incrementality strategy
     */
    public MaxSATConfig(final MaxSATConfig config, final IncrementalStrategy incrementalStrategy) {
        super(ConfigurationType.MAXSAT);
        this.incrementalStrategy = incrementalStrategy;
        amoEncoding = config.amoEncoding;
        pbEncoding = config.pbEncoding;
        cardinalityEncoding = config.cardinalityEncoding;
        weightStrategy = config.weightStrategy;
        verbosity = config.verbosity;
        output = config.output;
        symmetry = config.symmetry;
        limit = config.limit;
        bmo = config.bmo;
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
        final StringBuilder sb = new StringBuilder("MaxSATConfig{").append(System.lineSeparator());
        sb.append("incrementalStrategy=").append(incrementalStrategy).append(System.lineSeparator());
        sb.append("pbEncoding=").append(amoEncoding).append(System.lineSeparator());
        sb.append("pbEncoding=").append(pbEncoding).append(System.lineSeparator());
        sb.append("cardinalityEncoding=").append(cardinalityEncoding).append(System.lineSeparator());
        sb.append("weightStrategy=").append(weightStrategy).append(System.lineSeparator());
        sb.append("verbosity=").append(verbosity).append(System.lineSeparator());
        sb.append("symmetry=").append(symmetry).append(System.lineSeparator());
        sb.append("limit=").append(limit).append(System.lineSeparator());
        sb.append("bmo=").append(bmo).append(System.lineSeparator());
        sb.append("}");
        return sb.toString();
    }

    /**
     * The builder for a MaxSAT configuration.
     */
    public static class Builder {
        private final AMOEncoding amoEncoding;
        private final PBEncoding pbEncoding;
        private IncrementalStrategy incrementalStrategy = IncrementalStrategy.NONE;
        private CardinalityEncoding cardinalityEncoding = CardinalityEncoding.TOTALIZER;
        private WeightStrategy weightStrategy = WeightStrategy.NONE;
        private Verbosity verbosity = Verbosity.NONE;
        private PrintStream output = System.out;
        private boolean symmetry = true;
        private int limit = Integer.MAX_VALUE;
        private boolean bmo = true;

        /**
         * Constructor for the builder.
         */
        private Builder() {
            amoEncoding = AMOEncoding.LADDER;
            pbEncoding = PBEncoding.SWC;
        }

        /**
         * Sets the incremental strategy. The default value is {@code NONE}.
         * @param inc the incremental strategy
         * @return the builder
         */
        public Builder incremental(final IncrementalStrategy inc) {
            incrementalStrategy = inc;
            return this;
        }

        /**
         * Sets the cardinality encoding. The default value is {@code TOTALIZER}.
         * @param card the cardinality encoding
         * @return the builder
         */
        public Builder cardinality(final CardinalityEncoding card) {
            cardinalityEncoding = card;
            return this;
        }

        /**
         * Sets the weight strategy. The default value is {@code NONE}.
         * @param weight the weight strategy
         * @return the builder
         */
        public Builder weight(final WeightStrategy weight) {
            weightStrategy = weight;
            return this;
        }

        /**
         * Enables symmetry handling. The default value is {@code true}.
         * @param symm {code true} if symmetry handling should be activated, {@code false} otherwise
         * @return the builder
         */
        public Builder symmetry(final boolean symm) {
            symmetry = symm;
            return this;
        }

        /**
         * Sets the symmetry limit. The default value is {@code Integer.MAX_VALUE}.
         * @param lim the symmetry limit
         * @return the builder
         */
        public Builder limit(final int lim) {
            limit = lim;
            return this;
        }

        /**
         * Enables BMO (Boolean Multilevel Optimization). The default value is {@code true}.
         * @param bmo {code true} if BMO should be activated, {@code false} otherwise
         * @return the builder
         */
        public Builder bmo(final boolean bmo) {
            this.bmo = bmo;
            return this;
        }

        /**
         * Sets the verbosity. The default value is {@code NONE}.  If you set the verbosity to {@code SOME} you have also to
         * set an output stream.
         * @param verb the verbosity level
         * @return the builder
         */
        public Builder verbosity(final Verbosity verb) {
            verbosity = verb;
            return this;
        }

        /**
         * Sets the output stream for logging information.  The default ist {@code System.out}.
         * @param output the output stream for logging information
         * @return the builder
         */
        public Builder output(final PrintStream output) {
            this.output = output;
            return this;
        }

        /**
         * Builds the configuration.
         * @return the configuration.
         */
        public MaxSATConfig build() {
            return new MaxSATConfig(this);
        }
    }
}
