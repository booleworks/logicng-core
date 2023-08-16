// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.cardinalityconstraints;

import static org.logicng.util.FormulaHelper.literalsAsVariables;

import org.logicng.configurations.ConfigurationType;
import org.logicng.datastructures.EncodingResult;
import org.logicng.formulas.CardinalityConstraint;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.util.Pair;

import java.util.Collections;
import java.util.List;

/**
 * An encoder for cardinality constraints.
 * <p>
 * An encoder is configured with a {@link CCConfig} configuration.  There are two possible ways:
 * <ol>
 * <li>Initialize the encoder with a given configuration in the constructor.  Then this configuration will be bound
 * to the encoder for its whole lifetime.</li>
 * <li>Initialize the encoder only with a {@link FormulaFactory}.  Then each encoding will be performed with the
 * current cardinality constraint encoder configuration of the factory or the default configuration if the factory
 * has no associated cardinality constraint encoder configuration.  If you change the configuration in the factory,
 * all encoders constructed for this factory will be affected.</li>
 * </ol>
 * @version 3.0.0
 * @since 1.1
 */
public class CCEncoder {

    private CCEncoder() {
        // only static methods
    }

    /**
     * Encodes a cardinality constraint and returns its CNF encoding.
     * @param cc the cardinality constraint
     * @param f  the formula factory to generate new formulas
     * @return the CNF encoding of the cardinality constraint
     */
    public static List<Formula> encode(final CardinalityConstraint cc, final FormulaFactory f) {
        return encode(cc, f, null);
    }

    /**
     * Encodes a cardinality constraint and returns its CNF encoding.
     * @param cc     the cardinality constraint
     * @param f      the formula factory to generate new formulas
     * @param config the configuration for the encoder
     * @return the CNF encoding of the cardinality constraint
     */
    public static List<Formula> encode(final CardinalityConstraint cc, final FormulaFactory f, final CCConfig config) {
        final EncodingResult result = EncodingResult.resultForFormula(f);
        encodeConstraint(cc, result, config);
        return Collections.unmodifiableList(result.result());
    }

    /**
     * Encodes a cardinality constraint in a given result.
     * @param cc     the cardinality constraint
     * @param result the result of the encoding
     */
    public static void encode(final CardinalityConstraint cc, final EncodingResult result) {
        encodeConstraint(cc, result, null);
    }

    /**
     * Encodes a cardinality constraint in a given result.
     * @param cc     the cardinality constraint
     * @param result the result of the encoding
     * @param config the configuration for the encoder
     */
    public static void encode(final CardinalityConstraint cc, final EncodingResult result, final CCConfig config) {
        encodeConstraint(cc, result, config);
    }

    /**
     * Encodes an incremental cardinality constraint and returns its encoding.
     * @param cc the cardinality constraint
     * @param f  the formula factory to generate new formulas
     * @return the encoding of the constraint and the incremental data
     */
    public static Pair<List<Formula>, CCIncrementalData> encodeIncremental(final CardinalityConstraint cc, final FormulaFactory f) {
        final EncodingResult result = EncodingResult.resultForFormula(f);
        final CCIncrementalData incData = encodeIncremental(cc, result);
        return new Pair<>(Collections.unmodifiableList(result.result()), incData);
    }

    /**
     * Encodes an incremental cardinality constraint and returns its encoding.
     * @param cc     the cardinality constraint
     * @param f      the formula factory to generate new formulas
     * @param config the configuration for the encoder
     * @return the encoding of the constraint and the incremental data
     */
    public static Pair<List<Formula>, CCIncrementalData> encodeIncremental(final CardinalityConstraint cc, final FormulaFactory f, final CCConfig config) {
        final EncodingResult result = EncodingResult.resultForFormula(f);
        final CCIncrementalData incData = encodeIncremental(cc, result, config);
        return new Pair<>(Collections.unmodifiableList(result.result()), incData);
    }

    /**
     * Encodes an incremental cardinality constraint on a given solver.
     * @param cc     the cardinality constraint
     * @param result the result of the encoding
     * @return the incremental data
     */
    public static CCIncrementalData encodeIncremental(final CardinalityConstraint cc, final EncodingResult result) {
        return encodeIncrementalConstraint(cc, result, null);
    }

    /**
     * Encodes an incremental cardinality constraint on a given solver.
     * @param cc     the cardinality constraint
     * @param result the result of the encoding
     * @param config the configuration for the encoder
     * @return the incremental data
     */
    public static CCIncrementalData encodeIncremental(final CardinalityConstraint cc, final EncodingResult result, final CCConfig config) {
        return encodeIncrementalConstraint(cc, result, config);
    }

    protected static CCIncrementalData encodeIncrementalConstraint(final CardinalityConstraint cc, final EncodingResult result, final CCConfig initConfig) {
        final var config = initConfig != null ? initConfig : (CCConfig) result.factory().configurationFor(ConfigurationType.CC_ENCODER);
        final Variable[] ops = literalsAsVariables(cc.operands());
        if (cc.isAmo()) {
            throw new IllegalArgumentException("Incremental encodings are not supported for at-most-one constraints");
        }
        switch (cc.comparator()) {
            case LE:
                return amkIncremental(result, config, ops, cc.rhs());
            case LT:
                return amkIncremental(result, config, ops, cc.rhs() - 1);
            case GE:
                return alkIncremental(result, config, ops, cc.rhs());
            case GT:
                return alkIncremental(result, config, ops, cc.rhs() + 1);
            default:
                throw new IllegalArgumentException("Incremental encodings are only supported for at-most-k and at-least k constraints.");
        }
    }

    /**
     * Encodes the constraint in the given result.
     * @param cc     the constraint
     * @param result the result
     */
    protected static void encodeConstraint(final CardinalityConstraint cc, final EncodingResult result, final CCConfig initConfig) {
        final var config = initConfig != null ? initConfig : (CCConfig) result.factory().configurationFor(ConfigurationType.CC_ENCODER);
        final Variable[] ops = literalsAsVariables(cc.operands());
        switch (cc.comparator()) {
            case LE:
                if (cc.rhs() == 1) {
                    amo(result, config, ops);
                } else {
                    amk(result, config, ops, cc.rhs());
                }
                break;
            case LT:
                if (cc.rhs() == 2) {
                    amo(result, config, ops);
                } else {
                    amk(result, config, ops, cc.rhs() - 1);
                }
                break;
            case GE:
                alk(result, config, ops, cc.rhs());
                break;
            case GT:
                alk(result, config, ops, cc.rhs() + 1);
                break;
            case EQ:
                if (cc.rhs() == 1) {
                    exo(result, config, ops);
                } else {
                    exk(result, config, ops, cc.rhs());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown pseudo-Boolean comparator: " + cc.comparator());
        }
    }

    private static void amo(final EncodingResult result, final CCConfig config, final Variable... vars) {
        if (vars.length <= 1) {
            return;
        }
        switch (config.amoEncoder) {
            case PURE:
                CCAMOPure.get().build(result, config, vars);
                break;
            case LADDER:
                CCAMOLadder.get().build(result, config, vars);
                break;
            case PRODUCT:
                CCAMOProduct.get().build(result, config, vars);
                break;
            case NESTED:
                CCAMONested.get().build(result, config, vars);
                break;
            case COMMANDER:
                CCAMOCommander.get().build(result, config, vars);
                break;
            case BINARY:
                CCAMOBinary.get().build(result, config, vars);
                break;
            case BIMANDER:
                CCAMOBimander.get().build(result, config, vars);
                break;
            case BEST:
                bestAMO(vars.length).build(result, vars);
                break;
            default:
                throw new IllegalStateException("Unknown at-most-one encoder: " + config.amoEncoder);
        }
    }

    protected static void exo(final EncodingResult result, final CCConfig config, final Variable... vars) {
        if (vars.length == 0) {
            result.addClause();
            return;
        }
        if (vars.length == 1) {
            result.addClause(vars[0]);
            return;
        }
        amo(result, config, vars);
        result.addClause(vars);
    }

    protected static void amk(final EncodingResult result, final CCConfig config, final Variable[] vars, final int rhs) {
        if (rhs < 0) {
            throw new IllegalArgumentException("Invalid right hand side of cardinality constraint: " + rhs);
        }
        if (rhs >= vars.length) { // there is no constraint
            return;
        }
        if (rhs == 0) { // no variable can be true
            for (final Variable var : vars) {
                result.addClause(var.negate(result.factory()));
            }
            return;
        }
        switch (config.amkEncoder) {
            case TOTALIZER:
                CCAMKTotalizer.get().build(result, vars, rhs);
                break;
            case MODULAR_TOTALIZER:
                CCAMKModularTotalizer.get().build(result, vars, rhs);
                break;
            case CARDINALITY_NETWORK:
                CCAMKCardinalityNetwork.get().build(result, vars, rhs);
                break;
            case BEST:
                bestAMK(vars.length).build(result, vars, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown at-most-k encoder: " + config.amkEncoder);
        }
    }

    protected static CCIncrementalData amkIncremental(final EncodingResult result, final CCConfig config, final Variable[] vars, final int rhs) {
        if (rhs < 0) {
            throw new IllegalArgumentException("Invalid right hand side of cardinality constraint: " + rhs);
        }
        if (rhs >= vars.length) { // there is no constraint
            return null;
        }
        if (rhs == 0) { // no variable can be true
            for (final Variable var : vars) {
                result.addClause(var.negate(result.factory()));
            }
            return null;
        }
        switch (config.amkEncoder) {
            case TOTALIZER:
                return CCAMKTotalizer.get().build(result, vars, rhs);
            case MODULAR_TOTALIZER:
                return CCAMKModularTotalizer.get().build(result, vars, rhs);
            case CARDINALITY_NETWORK:
                return CCAMKCardinalityNetwork.get().buildForIncremental(result, vars, rhs);
            case BEST:
                return bestAMK(vars.length).build(result, vars, rhs);
            default:
                throw new IllegalStateException("Unknown at-most-k encoder: " + config.amkEncoder);
        }
    }

    protected static void alk(final EncodingResult result, final CCConfig config, final Variable[] vars, final int rhs) {
        if (rhs < 0) {
            throw new IllegalArgumentException("Invalid right hand side of cardinality constraint: " + rhs);
        }
        if (rhs > vars.length) {
            result.addClause();
            return;
        }
        if (rhs == 0) {
            return;
        }
        if (rhs == 1) {
            result.addClause(vars);
            return;
        }
        if (rhs == vars.length) {
            for (final Variable var : vars) {
                result.addClause(var);
            }
            return;
        }
        switch (config.alkEncoder) {
            case TOTALIZER:
                CCALKTotalizer.get().build(result, vars, rhs);
                break;
            case MODULAR_TOTALIZER:
                CCALKModularTotalizer.get().build(result, vars, rhs);
                break;
            case CARDINALITY_NETWORK:
                CCALKCardinalityNetwork.get().build(result, vars, rhs);
                break;
            case BEST:
                bestALK(vars.length).build(result, vars, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown at-least-k encoder: " + config.alkEncoder);
        }
    }

    protected static CCIncrementalData alkIncremental(final EncodingResult result, final CCConfig config, final Variable[] vars, final int rhs) {
        if (rhs < 0) {
            throw new IllegalArgumentException("Invalid right hand side of cardinality constraint: " + rhs);
        }
        if (rhs > vars.length) {
            result.addClause();
            return null;
        }
        if (rhs == 0) {
            return null;
        }
        if (rhs == 1) {
            result.addClause(vars);
            return null;
        }
        if (rhs == vars.length) {
            for (final Variable var : vars) {
                result.addClause(var);
            }
            return null;
        }
        switch (config.alkEncoder) {
            case TOTALIZER:
                return CCALKTotalizer.get().build(result, vars, rhs);
            case MODULAR_TOTALIZER:
                return CCALKModularTotalizer.get().build(result, vars, rhs);
            case CARDINALITY_NETWORK:
                return CCALKCardinalityNetwork.get().buildForIncremental(result, vars, rhs);
            case BEST:
                return bestALK(vars.length).build(result, vars, rhs);
            default:
                throw new IllegalStateException("Unknown at-least-k encoder: " + config.alkEncoder);
        }
    }

    protected static void exk(final EncodingResult result, final CCConfig config, final Variable[] vars, final int rhs) {
        if (rhs < 0) {
            throw new IllegalArgumentException("Invalid right hand side of cardinality constraint: " + rhs);
        }
        if (rhs > vars.length) {
            result.addClause();
            return;
        }
        if (rhs == 0) {
            for (final Variable var : vars) {
                result.addClause(var.negate());
            }
            return;
        }
        if (rhs == vars.length) {
            for (final Variable var : vars) {
                result.addClause(var);
            }
            return;
        }
        switch (config.exkEncoder) {
            case TOTALIZER:
                CCEXKTotalizer.get().build(result, vars, rhs);
                break;
            case CARDINALITY_NETWORK:
                CCEXKCardinalityNetwork.get().build(result, vars, rhs);
                break;
            case BEST:
                bestEXK(vars.length).build(result, vars, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown exactly-k encoder: " + config.exkEncoder);
        }
    }

    /**
     * Returns the best at-most-one encoder for a given number of variables.  The valuation is based on theoretical and
     * practical observations.  For &lt;= 10 the pure encoding without introduction of new variables is used, otherwise
     * the product encoding is chosen.
     * @param n the number of variables
     * @return the best at-most-one encoder
     */
    protected static CCAtMostOne bestAMO(final int n) {
        if (n <= 10) {
            return CCAMOPure.get();
        } else {
            return CCAMOProduct.get();
        }
    }

    /**
     * Returns the best at-most-k encoder for a given number of variables.  The valuation is based on theoretical and
     * practical observations.  Currently, the modular totalizer is the best encoder for all sizes and therefore is always
     * chosen.
     * @param n the number of variables
     * @return the best at-most-one encoder
     */
    protected static CCAtMostK bestAMK(final int n) {
        return CCAMKModularTotalizer.get();
    }

    /**
     * Returns the best at-least-k encoder for a given number of variables.  The valuation is based on theoretical and
     * practical observations.  Currently, the modular totalizer is the best encoder for all sizes and therefore is always
     * chosen.
     * @param n the number of variables
     * @return the best at-most-one encoder
     */
    protected static CCAtLeastK bestALK(final int n) {
        return CCALKModularTotalizer.get();
    }

    /**
     * Returns the best exactly-k encoder for a given number of variables.  The valuation is based on theoretical and
     * practical observations.  Currently, the totalizer is the best encoder for all sizes and therefore is always
     * chosen.
     * @param n the number of variables
     * @return the best at-most-one encoder
     */
    protected static CCExactlyK bestEXK(final int n) {
        return CCEXKTotalizer.get();
    }
}
