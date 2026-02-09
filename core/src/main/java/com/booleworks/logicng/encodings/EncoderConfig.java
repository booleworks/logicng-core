// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import com.booleworks.logicng.configurations.Configuration;
import com.booleworks.logicng.configurations.ConfigurationType;

/**
 * The configuration for a cardinality constraint or pseudo-Boolean encoder.
 * @version 3.0.0
 * @since 1.1
 */
public final class EncoderConfig extends Configuration {

    /**
     * The encoder for at-most-one and exactly-one constraints.
     */
    public enum AmoEncoder {
        PURE,
        LADDER,
        PRODUCT,
        NESTED,
        COMMANDER,
        BINARY,
        BIMANDER,
        BEST
    }

    /**
     * The encoder for at-most-k constraints.
     */
    public enum AmkEncoder {
        TOTALIZER,
        MODULAR_TOTALIZER,
        CARDINALITY_NETWORK,
        BEST
    }

    /**
     * The encoder for at-least-k constraints.
     */
    public enum AlkEncoder {
        TOTALIZER,
        MODULAR_TOTALIZER,
        CARDINALITY_NETWORK,
        BEST
    }

    /**
     * The encoder for exactly-k constraints.
     */
    public enum ExkEncoder {
        TOTALIZER,
        CARDINALITY_NETWORK,
        BEST
    }

    /**
     * The pseudo-Boolean encoder.
     */
    public enum PbEncoder {
        SWC,
        BINARY_MERGE,
        ADDER_NETWORKS,
        BEST
    }

    /**
     * The group size for the Bimander encoding.
     */
    public enum BimanderGroupSize {
        HALF,
        SQRT,
        FIXED
    }

    final AmoEncoder amoEncoder;
    final AmkEncoder amkEncoder;
    final AlkEncoder alkEncoder;
    final ExkEncoder exkEncoder;
    final PbEncoder pbEncoder;

    final BimanderGroupSize bimanderGroupSize;
    final int bimanderFixedGroupSize;
    final int nestingGroupSize;
    final int productRecursiveBound;
    final int commanderGroupSize;

    final boolean binaryMergeUseGac;
    final boolean binaryMergeNoSupportForSingleBit;
    final boolean binaryMergeUseWatchDog;

    /**
     * Returns a new builder for the configuration.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Constructs a new encoder configuration from a given builder.
     * @param builder the builder
     */
    private EncoderConfig(final Builder builder) {
        super(ConfigurationType.ENCODER);
        amoEncoder = builder.amoEncoder;
        amkEncoder = builder.amkEncoder;
        alkEncoder = builder.alkEncoder;
        exkEncoder = builder.exkEncoder;
        pbEncoder = builder.pbEncoder;

        bimanderGroupSize = builder.bimanderGroupSize;
        bimanderFixedGroupSize = builder.bimanderFixedGroupSize;
        nestingGroupSize = builder.nestingGroupSize;
        productRecursiveBound = builder.productRecursiveBound;
        commanderGroupSize = builder.commanderGroupSize;

        binaryMergeUseGac = builder.binaryMergeUseGac;
        binaryMergeNoSupportForSingleBit = builder.binaryMergeNoSupportForSingleBit;
        binaryMergeUseWatchDog = builder.binaryMergeUseWatchDog;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EncoderConfig{").append(System.lineSeparator());
        sb.append("amoEncoder=").append(amoEncoder).append(System.lineSeparator());
        sb.append("amkEncoder=").append(amkEncoder).append(System.lineSeparator());
        sb.append("alkEncoder=").append(alkEncoder).append(System.lineSeparator());
        sb.append("exkEncoder=").append(exkEncoder).append(System.lineSeparator());
        sb.append("pbEncoder=").append(pbEncoder).append(System.lineSeparator());
        sb.append("bimanderGroupSize=").append(bimanderGroupSize).append(System.lineSeparator());
        sb.append("bimanderFixedGroupSize=").append(bimanderFixedGroupSize).append(System.lineSeparator());
        sb.append("nestingGroupSize=").append(nestingGroupSize).append(System.lineSeparator());
        sb.append("productRecursiveBound=").append(productRecursiveBound).append(System.lineSeparator());
        sb.append("commanderGroupSize=").append(commanderGroupSize).append(System.lineSeparator());
        sb.append("binaryMergeUseGac=").append(binaryMergeUseGac).append(System.lineSeparator());
        sb.append("binaryMergeNoSupportForSingleBit=").append(binaryMergeNoSupportForSingleBit)
                .append(System.lineSeparator());
        sb.append("binaryMergeUseWatchDog=").append(binaryMergeUseWatchDog).append(System.lineSeparator());
        sb.append("}").append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * The builder for an encoder configuration.
     */
    public static final class Builder {
        private AmoEncoder amoEncoder = AmoEncoder.BEST;
        private AmkEncoder amkEncoder = AmkEncoder.BEST;
        private AlkEncoder alkEncoder = AlkEncoder.BEST;
        private ExkEncoder exkEncoder = ExkEncoder.BEST;
        private PbEncoder pbEncoder = PbEncoder.BEST;

        private BimanderGroupSize bimanderGroupSize = BimanderGroupSize.SQRT;
        private int bimanderFixedGroupSize = 3;
        private int nestingGroupSize = 4;
        private int productRecursiveBound = 20;
        private int commanderGroupSize = 3;

        private boolean binaryMergeUseGac = true;
        private boolean binaryMergeNoSupportForSingleBit = false;
        private boolean binaryMergeUseWatchDog = true;

        private Builder() {
            // Initialize only via factory
        }

        /**
         * Sets the encoder for at-most-one constraints. The default value is
         * {@code BEST}.
         * @param amoEncoder the at-most-one encoder
         * @return the builder
         */
        public Builder amoEncoding(final AmoEncoder amoEncoder) {
            this.amoEncoder = amoEncoder;
            return this;
        }

        /**
         * Sets the encoder for at-most-k constraints. The default value is
         * {@code BEST}.
         * @param amkEncoder the at-most-k encoder
         * @return the builder
         */
        public Builder amkEncoding(final AmkEncoder amkEncoder) {
            this.amkEncoder = amkEncoder;
            return this;
        }

        /**
         * Sets the encoder for at-least-k constraints. The default value is
         * {@code BEST}.
         * @param alkEncoder the at-least-k encoder
         * @return the builder
         */
        public Builder alkEncoding(final AlkEncoder alkEncoder) {
            this.alkEncoder = alkEncoder;
            return this;
        }

        /**
         * Sets the encoder for exactly-k constraints. The default value is
         * {@code BEST}.
         * @param exkEncoder the exactly-k encoder
         * @return the builder
         */
        public Builder exkEncoding(final ExkEncoder exkEncoder) {
            this.exkEncoder = exkEncoder;
            return this;
        }

        /**
         * Sets the encoder for pseudo-Boolean constraints. The default value is
         * {@code BEST}.
         * @param pbEncoder the pseudo-Boolean encoder
         * @return the builder
         */
        public Builder pbEncoding(final PbEncoder pbEncoder) {
            this.pbEncoder = pbEncoder;
            return this;
        }

        /**
         * Sets the group size for the bimander encoding. The default value is
         * {@code SQRT}.
         * @param bimanderGroupSize the bimander encoding group size
         * @return the builder
         */
        public Builder bimanderGroupSize(final BimanderGroupSize bimanderGroupSize) {
            this.bimanderGroupSize = bimanderGroupSize;
            return this;
        }

        /**
         * Sets the fixed group size for the bimander encoding. The default
         * value is 3. This setting is only used if the bimander group size is
         * set to {@code FIXED}.
         * @param bimanderFixedGroupSize the bimander encoding fixed group size
         * @return the builder
         */
        public Builder bimanderFixedGroupSize(final int bimanderFixedGroupSize) {
            this.bimanderFixedGroupSize = bimanderFixedGroupSize;
            return this;
        }

        /**
         * Sets the group size for the nesting encoding. The default value is 4.
         * @param nestingGroupSize the group size for the nesting encoding
         * @return the builder
         */
        public Builder nestingGroupSize(final int nestingGroupSize) {
            this.nestingGroupSize = nestingGroupSize;
            return this;
        }

        /**
         * Sets the recursive bound for the product encoding. The default value
         * is 20.
         * @param productRecursiveBound the recursive bound for the product
         *                              encoding
         * @return the builder
         */
        public Builder productRecursiveBound(final int productRecursiveBound) {
            this.productRecursiveBound = productRecursiveBound;
            return this;
        }

        /**
         * Sets the group size for the nesting encoding. The default value is 4.
         * @param commanderGroupSize the group size for the nesting encoding
         * @return the builder
         */
        public Builder commanderGroupSize(final int commanderGroupSize) {
            this.commanderGroupSize = commanderGroupSize;
            return this;
        }

        /**
         * Sets whether general arc consistency should be used in the binary
         * merge encoding. The default value is {@code
         * true}.
         * @param binaryMergeUseGac {@code true} if general arc consistency
         *                          should be used, {@code false} otherwise
         * @return the builder
         */
        public Builder binaryMergeUseGac(final boolean binaryMergeUseGac) {
            this.binaryMergeUseGac = binaryMergeUseGac;
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
         * Builds the cardinality constraint encoder configuration.
         * @return the configuration
         */
        public EncoderConfig build() {
            return new EncoderConfig(this);
        }
    }
}
