// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.pseudobooleans;

import com.booleworks.logicng.cardinalityconstraints.CCConfig;
import com.booleworks.logicng.cardinalityconstraints.CCEncoder;
import com.booleworks.logicng.collections.LNGIntVector;
import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.PBConstraint;

import java.util.ArrayList;
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
     * @param pbConfig   the pseudo-Boolean encoder configuration
     * @return the CNF encoding of the pseudo-Boolean constraint
     */
    public static List<Formula> encode(final FormulaFactory f, final PBConstraint constraint, final PBConfig pbConfig) {
        return encode(f, constraint, pbConfig, null);
    }

    /**
     * Encodes a pseudo-Boolean constraint and returns its CNF encoding.
     * @param f          the formula factory to generate new formulas
     * @param constraint the pseudo-Boolean constraint
     * @return the CNF encoding of the pseudo-Boolean constraint
     */
    public static List<Formula> encode(final FormulaFactory f, final PBConstraint constraint) {
        return encode(f, constraint, null, null);
    }

    /**
     * Encodes a pseudo-Boolean constraint and returns its CNF encoding.
     * @param f          the formula factory to generate new formulas
     * @param constraint the pseudo-Boolean constraint
     * @param pbConfig   the pseudo-Boolean encoder configuration
     * @param ccConfig   the cardinality constraints encoder configuration
     * @return the CNF encoding of the pseudo-Boolean constraint
     */
    public static List<Formula> encode(final FormulaFactory f, final PBConstraint constraint, final PBConfig pbConfig, final CCConfig ccConfig) {
        if (constraint.isCC()) {
            return CCEncoder.encode(f, (CardinalityConstraint) constraint, ccConfig != null ? ccConfig :
                    (CCConfig) f.configurationFor(ConfigurationType.CC_ENCODER));
        }
        final Formula normalized = constraint.normalize(f);
        switch (normalized.type()) {
            case TRUE:
                return Collections.emptyList();
            case FALSE:
                return Collections.singletonList(f.falsum());
            case PBC:
                final PBConstraint pbc = (PBConstraint) normalized;
                if (pbc.isCC()) {
                    return CCEncoder.encode(f, (CardinalityConstraint) pbc, ccConfig != null ? ccConfig :
                            (CCConfig) f.configurationFor(ConfigurationType.CC_ENCODER));
                }
                return encode(f, pbc.operands(), pbc.coefficients(), pbc.rhs(), pbConfig != null ? pbConfig :
                        (PBConfig) f.configurationFor(ConfigurationType.PB_ENCODER));
            case AND:
                final List<Formula> list = new ArrayList<>();
                for (final Formula op : normalized) {
                    switch (op.type()) {
                        case FALSE:
                            return Collections.singletonList(f.falsum());
                        case PBC:
                            list.addAll(encode(f, (PBConstraint) op, pbConfig, ccConfig));
                            break;
                        default:
                            throw new IllegalArgumentException("Illegal return value of PBConstraint.normalize");
                    }
                }
                return Collections.unmodifiableList(list);
            default:
                throw new IllegalArgumentException("Illegal return value of PBConstraint.normalize");
        }
    }

    /**
     * Builds a pseudo Boolean constraint of the form {@code c_1 * lit_1 + c_2 * lit_2 + ... + c_n * lit_n >= k}.
     * @param f        the formula factory to generate new formulas
     * @param lits     the literals {@code lit_1 ... lit_n}
     * @param coeffs   the coefficients {@code c_1 ... c_n}
     * @param rhs      the right-hand side {@code k} of the constraint
     * @param pbConfig the configuration for the encoding
     * @return the CNF encoding of the pseudo Boolean constraint
     * @throws IllegalArgumentException if the right-hand side of the cardinality constraint is negative or
     *                                  larger than the number of literals
     */
    protected static List<Formula> encode(final FormulaFactory f, final List<Literal> lits, final List<Integer> coeffs, final int rhs,
                                          final PBConfig pbConfig) {
        if (rhs == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Overflow in the Encoding");
        }
        if (rhs < 0) {
            return Collections.singletonList(f.falsum());
        }
        final LNGVector<Literal> simplifiedLits = new LNGVector<>();
        final LNGIntVector simplifiedCoeffs = new LNGIntVector();
        final List<Formula> result = new ArrayList<>();
        if (rhs == 0) {
            for (final Literal lit : lits) {
                result.add(lit.negate(f));
            }
            return result;
        }
        for (int i = 0; i < lits.size(); i++) {
            if (coeffs.get(i) <= rhs) {
                simplifiedLits.push(lits.get(i));
                simplifiedCoeffs.push(coeffs.get(i));
            } else {
                result.add(lits.get(i).negate(f));
            }
        }
        if (simplifiedLits.size() <= 1) {
            return result;
        }
        switch (pbConfig.pbEncoder) {
            case SWC:
            case BEST:
                return PBSWC.get().encode(f, simplifiedLits, simplifiedCoeffs, rhs, result, pbConfig);
            case BINARY_MERGE:
                return PBBinaryMerge.get().encode(f, simplifiedLits, simplifiedCoeffs, rhs, result, pbConfig);
            case ADDER_NETWORKS:
                return PBAdderNetworks.get().encode(f, simplifiedLits, simplifiedCoeffs, rhs, result, pbConfig);
            default:
                throw new IllegalStateException("Unknown pseudo-Boolean encoder: " + pbConfig.pbEncoder);
        }
    }
}
