// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResult;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResultFF;
import com.booleworks.logicng.encodings.pbc.PbAdderNetwork;
import com.booleworks.logicng.encodings.pbc.PbBinaryMerge;
import com.booleworks.logicng.encodings.pbc.PbSwc;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.PbConstraint;

import java.util.Collections;
import java.util.List;

/**
 * An encoder for pseudo-Boolean constraints.
 * @version 3.0.0
 * @since 1.0
 */
public class PbEncoder {

    private PbEncoder() {
        // Only static methods
    }

    /**
     * Encodes a pseudo-Boolean constraint and returns its CNF encoding.
     * @param f          the formula factory to generate new formulas
     * @param constraint the pseudo-Boolean constraint
     * @return the CNF encoding of the pseudo-Boolean constraint
     */
    public static List<Formula> encode(final FormulaFactory f, final PbConstraint constraint) {
        final EncodingResultFF result = new EncodingResultFF(f);
        encode(result, constraint, null);
        return Collections.unmodifiableList(result.getResult());
    }

    /**
     * Encodes a pseudo-Boolean constraint in the given encoding.
     * @param result     the result of the encoding
     * @param constraint the pseudo-Boolean constraint
     */
    public static void encode(final EncodingResult result, final PbConstraint constraint) {
        encode(result, constraint, null);
    }

    /**
     * Encodes a pseudo-Boolean constraint and returns its CNF encoding.
     * @param f          the formula factory to generate new formulas
     * @param constraint the pseudo-Boolean constraint
     * @param config     the encoder configuration
     * @return the CNF encoding of the pseudo-Boolean constraint
     */
    public static List<Formula> encode(final FormulaFactory f, final PbConstraint constraint,
                                       final EncoderConfig config) {
        final EncodingResultFF result = new EncodingResultFF(f);
        encode(result, constraint, config);
        return Collections.unmodifiableList(result.getResult());
    }

    /**
     * Encodes a pseudo-Boolean constraint in the given encoding result.
     * @param result     the result of the encoding
     * @param constraint the pseudo-Boolean constraint
     * @param initConfig the encoder configuration
     */
    public static void encode(final EncodingResult result, final PbConstraint constraint,
                              final EncoderConfig initConfig) {
        final FormulaFactory f = result.getFactory();
        final EncoderConfig config =
                initConfig != null ? initConfig : (EncoderConfig) f.configurationFor(ConfigurationType.ENCODER);
        if (constraint.isCc()) {
            CcEncoder.encode(result, (CardinalityConstraint) constraint, config);
            return;
        }
        final Formula normalized = constraint.normalize(f);
        switch (normalized.getType()) {
            case TRUE:
                // do nothing
                return;
            case FALSE:
                result.addClause();
                return;
            case PBC:
                final PbConstraint pbc = (PbConstraint) normalized;
                if (pbc.isCc()) {
                    CcEncoder.encode(result, (CardinalityConstraint) pbc, config);
                    return;
                }
                encode(result, pbc.getOperands(), pbc.getCoefficients(), pbc.getRhs(), config);
                return;
            case AND:
                for (final Formula op : normalized) {
                    switch (op.getType()) {
                        case FALSE:
                            result.addClause();
                            continue;
                        case PBC:
                            encode(result, (PbConstraint) op, config);
                            continue;
                        default:
                            throw new IllegalArgumentException("Illegal return value of PbConstraint.normalize");
                    }
                }
                return;
            default:
                throw new IllegalArgumentException("Illegal return value of PbConstraint.normalize");
        }
    }

    /**
     * Builds a pseudo Boolean constraint of the form
     * {@code c_1 * lit_1 + c_2 * lit_2 + ... + c_n * lit_n >= k}.
     * @param result the result of the encoding
     * @param lits   the literals {@code lit_1 ... lit_n}
     * @param coeffs the coefficients {@code c_1 ... c_n}
     * @param rhs    the right-hand side {@code k} of the constraint
     * @param config the configuration for the encoding
     * @throws IllegalArgumentException if the right-hand side of the
     *                                  cardinality constraint is negative or
     *                                  larger than the number of literals
     */
    protected static void encode(final EncodingResult result, final List<Literal> lits, final List<Integer> coeffs,
                                 final int rhs, final EncoderConfig config) {
        final FormulaFactory f = result.getFactory();
        if (rhs == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Overflow in the Encoding");
        }
        if (rhs < 0) {
            result.addClause();
            return;
        }
        final LngVector<Literal> simplifiedLits = new LngVector<>();
        final LngIntVector simplifiedCoeffs = new LngIntVector();
        if (rhs == 0) {
            for (final Literal lit : lits) {
                result.addClause(lit.negate(f));
            }
            return;
        }
        for (int i = 0; i < lits.size(); i++) {
            if (coeffs.get(i) <= rhs) {
                simplifiedLits.push(lits.get(i));
                simplifiedCoeffs.push(coeffs.get(i));
            } else {
                result.addClause(lits.get(i).negate(f));
            }
        }
        if (simplifiedLits.size() <= 1) {
            return;
        }
        switch (config.pbEncoder) {
            case SWC:
            case BEST:
                PbSwc.encode(result, simplifiedLits, simplifiedCoeffs, rhs);
                break;
            case BINARY_MERGE:
                PbBinaryMerge.encode(result, simplifiedLits, simplifiedCoeffs, rhs,
                        config.binaryMergeUseGac, config.binaryMergeNoSupportForSingleBit,
                        config.binaryMergeUseWatchDog);
                break;
            case ADDER_NETWORKS:
                PbAdderNetwork.encode(result, simplifiedLits, simplifiedCoeffs, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean encoder: " + config.pbEncoder);
        }
    }
}
