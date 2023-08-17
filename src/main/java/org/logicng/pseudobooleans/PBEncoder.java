// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.pseudobooleans;

import org.logicng.cardinalityconstraints.CCConfig;
import org.logicng.cardinalityconstraints.CCEncoder;
import org.logicng.collections.LNGIntVector;
import org.logicng.collections.LNGVector;
import org.logicng.configurations.ConfigurationType;
import org.logicng.formulas.CardinalityConstraint;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.PBConstraint;

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
     * @param constraint the pseudo-Boolean constraint
     * @param pbConfig   the pseudo-Boolean encoder configuration
     * @param f          the formula factory to generate new formulas
     * @return the CNF encoding of the pseudo-Boolean constraint
     */
    public static List<Formula> encode(final PBConstraint constraint, final FormulaFactory f, final PBConfig pbConfig) {
        return encode(constraint, f, pbConfig, null);
    }

    /**
     * Encodes a pseudo-Boolean constraint and returns its CNF encoding.
     * @param constraint the pseudo-Boolean constraint
     * @param f          the formula factory to generate new formulas
     * @return the CNF encoding of the pseudo-Boolean constraint
     */
    public static List<Formula> encode(final PBConstraint constraint, final FormulaFactory f) {
        return encode(constraint, f, null, null);
    }

    /**
     * Encodes a pseudo-Boolean constraint and returns its CNF encoding.
     * @param constraint the pseudo-Boolean constraint
     * @return the CNF encoding of the pseudo-Boolean constraint
     */
    public static List<Formula> encode(final PBConstraint constraint) {
        return encode(constraint, constraint.factory(), null, null);
    }

    /**
     * Encodes a pseudo-Boolean constraint and returns its CNF encoding.
     * @param constraint the pseudo-Boolean constraint
     * @param f          the formula factory to generate new formulas
     * @param pbConfig   the pseudo-Boolean encoder configuration
     * @param ccConfig   the cardinality constraints encoder configuration
     * @return the CNF encoding of the pseudo-Boolean constraint
     */
    public static List<Formula> encode(final PBConstraint constraint, final FormulaFactory f, final PBConfig pbConfig, final CCConfig ccConfig) {
        if (constraint.isCC()) {
            return CCEncoder.encode((CardinalityConstraint) constraint, f, ccConfig != null ? ccConfig :
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
                    return CCEncoder.encode((CardinalityConstraint) pbc, f, ccConfig != null ? ccConfig :
                            (CCConfig) f.configurationFor(ConfigurationType.CC_ENCODER));
                }
                return encode(pbc.operands(), pbc.coefficients(), pbc.rhs(), f, pbConfig != null ? pbConfig :
                        (PBConfig) f.configurationFor(ConfigurationType.PB_ENCODER));
            case AND:
                final List<Formula> list = new ArrayList<>();
                for (final Formula op : normalized) {
                    switch (op.type()) {
                        case FALSE:
                            return Collections.singletonList(f.falsum());
                        case PBC:
                            list.addAll(encode((PBConstraint) op, f, pbConfig, ccConfig));
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
     * @param lits   the literals {@code lit_1 ... lit_n}
     * @param coeffs the coefficients {@code c_1 ... c_n}
     * @param rhs    the right-hand side {@code k} of the constraint
     * @param f      the formula factory to generate new formulas
     * @return the CNF encoding of the pseudo Boolean constraint
     * @throws IllegalArgumentException if the right-hand side of the cardinality constraint is negative or
     *                                  larger than the number of literals
     */
    protected static List<Formula> encode(final List<Literal> lits, final List<Integer> coeffs, final int rhs, final FormulaFactory f,
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
