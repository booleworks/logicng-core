// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.pseudobooleans;

import com.booleworks.logicng.cardinalityconstraints.CCConfig;
import com.booleworks.logicng.cardinalityconstraints.CCEncoder;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.PBConstraint;

import java.util.Collections;
import java.util.List;

/**
 * An encoder for pseudo-Boolean constraints.
 * @version 3.0.0
 * @since 1.0
 */
public class PBEncoder {

    private PBEncoder() {
        // Only static methods
    }

    /**
     * Encodes a pseudo-Boolean constraint and returns its CNF encoding.
     * @param f          the formula factory to generate new formulas
     * @param constraint the pseudo-Boolean constraint
     * @return the CNF encoding of the pseudo-Boolean constraint
     */
    public static List<Formula> encode(final FormulaFactory f, final PBConstraint constraint) {
        final EncodingResult result = EncodingResult.resultForFormula(f);
        encode(constraint, result, null, null);
        return Collections.unmodifiableList(result.result());
    }

    /**
     * Encodes a pseudo-Boolean constraint in the given encoding.
     * @param constraint the pseudo-Boolean constraint
     * @param result     the result of the encoding
     */
    public static void encode(final PBConstraint constraint, final EncodingResult result) {
        encode(constraint, result, null, null);
    }

    /**
     * Encodes a pseudo-Boolean constraint and returns its CNF encoding.
     * @param f          the formula factory to generate new formulas
     * @param constraint the pseudo-Boolean constraint
     * @param pbConfig   the pseudo-Boolean encoder configuration
     * @return the CNF encoding of the pseudo-Boolean constraint
     */
    public static List<Formula> encode(final FormulaFactory f, final PBConstraint constraint, final PBConfig pbConfig) {
        final EncodingResult result = EncodingResult.resultForFormula(f);
        encode(constraint, result, pbConfig, null);
        return Collections.unmodifiableList(result.result());
    }

    /**
     * Encodes a pseudo-Boolean constraint and returns its CNF encoding.
     * @param f          the formula factory to generate new formulas
     * @param constraint the pseudo-Boolean constraint
     * @param pbConfig   the pseudo-Boolean encoder configuration
     * @param ccConfig   the cardinality constraints encoder configuration
     * @return the CNF encoding of the pseudo-Boolean constraint
     */
    public static List<Formula> encode(final FormulaFactory f, final PBConstraint constraint, final PBConfig pbConfig,
                                       final CCConfig ccConfig) {
        final EncodingResult result = EncodingResult.resultForFormula(f);
        encode(constraint, result, pbConfig, ccConfig);
        return Collections.unmodifiableList(result.result());
    }

    /**
     * Encodes a pseudo-Boolean constraint in the given encoding result.
     * @param constraint the pseudo-Boolean constraint
     * @param result     the result of the encoding
     * @param pbConfig   the pseudo-Boolean encoder configuration
     * @param ccConfig   the cardinality constraints encoder configuration
     */
    public static void encode(final PBConstraint constraint, final EncodingResult result, final PBConfig pbConfig,
                              final CCConfig ccConfig) {
        final FormulaFactory f = result.factory();
        if (constraint.isCC()) {
            CCEncoder.encode((CardinalityConstraint) constraint, result, ccConfig);
            return;
        }
        final Formula normalized = constraint.normalize(f);
        switch (normalized.type()) {
            case TRUE:
                // do nothing
                return;
            case FALSE:
                result.addClause();
                return;
            case PBC:
                final PBConstraint pbc = (PBConstraint) normalized;
                if (pbc.isCC()) {
                    CCEncoder.encode((CardinalityConstraint) pbc, result, ccConfig != null ? ccConfig :
                            (CCConfig) f.configurationFor(ConfigurationType.CC_ENCODER));
                    return;
                }
                encode(result, pbc.operands(), pbc.coefficients(), pbc.rhs(), pbConfig != null ? pbConfig :
                        (PBConfig) f.configurationFor(ConfigurationType.PB_ENCODER));
                return;
            case AND:
                for (final Formula op : normalized) {
                    switch (op.type()) {
                        case FALSE:
                            result.addClause();
                            continue;
                        case PBC:
                            encode((PBConstraint) op, result, pbConfig, ccConfig);
                            continue;
                        default:
                            throw new IllegalArgumentException("Illegal return value of PBConstraint.normalize");
                    }
                }
                return;
            default:
                throw new IllegalArgumentException("Illegal return value of PBConstraint.normalize");
        }
    }

    /**
     * Builds a pseudo Boolean constraint of the form
     * {@code c_1 * lit_1 + c_2 * lit_2 + ... + c_n * lit_n >= k}.
     * @param result   the result of the encoding
     * @param lits     the literals {@code lit_1 ... lit_n}
     * @param coeffs   the coefficients {@code c_1 ... c_n}
     * @param rhs      the right-hand side {@code k} of the constraint
     * @param pbConfig the configuration for the encoding
     * @throws IllegalArgumentException if the right-hand side of the
     *                                  cardinality constraint is negative or
     *                                  larger than the number of literals
     */
    protected static void encode(final EncodingResult result, final List<Literal> lits, final List<Integer> coeffs,
                                 final int rhs,
                                 final PBConfig pbConfig) {
        final FormulaFactory f = result.factory();
        if (rhs == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Overflow in the Encoding");
        }
        if (rhs < 0) {
            result.addClause();
            return;
        }
        final LNGVector<Literal> simplifiedLits = new LNGVector<>();
        final LNGIntVector simplifiedCoeffs = new LNGIntVector();
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
        switch (pbConfig.pbEncoder) {
            case SWC:
            case BEST:
                PBSWC.get().encode(result, simplifiedLits, simplifiedCoeffs, rhs, pbConfig);
                break;
            case BINARY_MERGE:
                PBBinaryMerge.get().encode(result, simplifiedLits, simplifiedCoeffs, rhs, pbConfig);
                break;
            case ADDER_NETWORKS:
                PBAdderNetworks.get().encode(result, simplifiedLits, simplifiedCoeffs, rhs, pbConfig);
                break;
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean encoder: " + pbConfig.pbEncoder);
        }
    }
}
