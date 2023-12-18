// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.pseudobooleans;

import com.booleworks.logicng.configurations.Configuration;
import com.booleworks.logicng.configurations.ConfigurationType;

/**
 * The configuration for a pseudo-Boolean encoder.
 * @version 2.0.0
 * @since 1.1
 */
public final class PBConfig extends Configuration {

    /**
     * The pseudo-Boolean encoder.
     */
    public enum PB_ENCODER {
        SWC,
        BINARY_MERGE,
        ADDER_NETWORKS,
        BEST
    }

    final PB_ENCODER pbEncoder;
    final boolean binaryMergeUseGAC;
    final boolean binaryMergeNoSupportForSingleBit;
    final boolean binaryMergeUseWatchDog;

    /**
     * Constructs a new pseudo-Boolean encoder configuration from a given
     * builder.
     * @param builder the builder
     */
    private PBConfig(final Builder builder) {
        super(ConfigurationType.PB_ENCODER);
        pbEncoder = builder.pbEncoder;
        binaryMergeUseGAC = builder.binaryMergeUseGAC;
        binaryMergeNoSupportForSingleBit = builder.binaryMergeNoSupportForSingleBit;
        binaryMergeUseWatchDog = builder.binaryMergeUseWatchDog;
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
        final StringBuilder sb = new StringBuilder("PBConfig{").append(System.lineSeparator());
        sb.append("pbEncoder=").append(pbEncoder).append(System.lineSeparator());
        sb.append("binaryMergeUseGAC=").append(binaryMergeUseGAC).append(System.lineSeparator());
        sb.append("binaryMergeNoSupportForSingleBit=").append(binaryMergeNoSupportForSingleBit)
                .append(System.lineSeparator());
        sb.append("binaryMergeUseWatchDog=").append(binaryMergeUseWatchDog).append(System.lineSeparator());
        sb.append("}").append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * The builder for a pseudo-Boolean encoder configuration.
     */
    public static class Builder {
        private PB_ENCODER pbEncoder = PB_ENCODER.BEST;
        private boolean binaryMergeUseGAC = true;
        private boolean binaryMergeNoSupportForSingleBit = false;
        private boolean binaryMergeUseWatchDog = true;

        private Builder() {
            // Initialize only via factory
        }

        /**
         * Sets the encoder for pseudo-Boolean constraints. The default value is
         * {@code BEST}.
         * @param pbEncoder the pseudo-Boolean encoder
         * @return the builder
         */
        public Builder pbEncoding(final PB_ENCODER pbEncoder) {
            this.pbEncoder = pbEncoder;
            return this;
        }

        /**
         * Sets whether general arc consistency should be used in the binary
         * merge encoding. The default value is {@code
         * true}.
         * @param binaryMergeUseGAC {@code true} if general arc consistency
         *                          should be used, {@code false} otherwise
         * @return the builder
         */
        public Builder binaryMergeUseGAC(final boolean binaryMergeUseGAC) {
            this.binaryMergeUseGAC = binaryMergeUseGAC;
            return this;
        }

        /**
         * Sets the support for single bits in the binary merge encoding. The
         * default value is {@code false}.
         * @param binaryMergeNoSupportForSingleBit {@code true} if the support
         *                                         for single bits should be
         *                                         disabled, {@code false}
         *                                         otherwise
         * @return the builder
         */
        public Builder binaryMergeNoSupportForSingleBit(final boolean binaryMergeNoSupportForSingleBit) {
            this.binaryMergeNoSupportForSingleBit = binaryMergeNoSupportForSingleBit;
            return this;
        }

        /**
         * Sets whether the watchdog encoding should be used in the binary merge
         * encoding. The default value is {@code true}.
         * @param binaryMergeUseWatchDog {@code true} if the watchdog encoding
         *                               should be used, {@code false} otherwise
         * @return the builder
         */
        public Builder binaryMergeUseWatchDog(final boolean binaryMergeUseWatchDog) {
            this.binaryMergeUseWatchDog = binaryMergeUseWatchDog;
            return this;
        }

        /**
         * Builds the pseudo-Boolean encoder configuration.
         * @return the configuration
         */
        public PBConfig build() {
            return new PBConfig(this);
        }
    }
}
