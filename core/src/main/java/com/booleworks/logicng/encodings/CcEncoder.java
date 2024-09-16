// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.encodings;

import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.encodings.cc.CcAmo;
import com.booleworks.logicng.encodings.cc.CcCardinalityNetwork;
import com.booleworks.logicng.encodings.cc.CcModularTotalizer;
import com.booleworks.logicng.encodings.cc.CcTotalizer;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.util.FormulaHelper;
import com.booleworks.logicng.util.Pair;

import java.util.Collections;
import java.util.List;

/**
 * An encoder for cardinality constraints.
 * <p>
 * An encoder is configured with a {@link EncoderConfig} configuration. There
 * are two possible ways:
 * <ol>
 * <li>Initialize the encoder with a given configuration in the constructor.
 * Then this configuration will be bound to the encoder for its whole
 * lifetime.</li>
 * <li>Initialize the encoder only with a {@link FormulaFactory}. Then each
 * encoding will be performed with the current cardinality constraint encoder
 * configuration of the factory or the default configuration if the factory has
 * no associated cardinality constraint encoder configuration. If you change the
 * configuration in the factory, all encoders constructed for this factory will
 * be affected.</li>
 * </ol>
 * @version 3.0.0
 * @since 1.1
 */
public class CcEncoder {

    private CcEncoder() {
        // only static methods
    }

    /**
     * Encodes a cardinality constraint and returns its CNF encoding.
     * @param f  the formula factory to generate new formulas
     * @param cc the cardinality constraint
     * @return the CNF encoding of the cardinality constraint
     */
    public static List<Formula> encode(final FormulaFactory f, final CardinalityConstraint cc) {
        return encode(f, cc, null);
    }

    /**
     * Encodes a cardinality constraint and returns its CNF encoding.
     * @param f      the formula factory to generate new formulas
     * @param cc     the cardinality constraint
     * @param config the configuration for the encoder
     * @return the CNF encoding of the cardinality constraint
     */
    public static List<Formula> encode(final FormulaFactory f, final CardinalityConstraint cc,
                                       final EncoderConfig config) {
        final EncodingResult result = EncodingResult.resultForFormula(f);
        encodeConstraint(cc, result, config);
        return Collections.unmodifiableList(result.getResult());
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
    public static void encode(final CardinalityConstraint cc, final EncodingResult result, final EncoderConfig config) {
        encodeConstraint(cc, result, config);
    }

    /**
     * Encodes an incremental cardinality constraint and returns its encoding.
     * @param f  the formula factory to generate new formulas
     * @param cc the cardinality constraint
     * @return the encoding of the constraint and the incremental data
     */
    public static Pair<List<Formula>, CcIncrementalData> encodeIncremental(final FormulaFactory f,
                                                                           final CardinalityConstraint cc) {
        final EncodingResult result = EncodingResult.resultForFormula(f);
        final CcIncrementalData incData = encodeIncremental(cc, result);
        return new Pair<>(Collections.unmodifiableList(result.getResult()), incData);
    }

    /**
     * Encodes an incremental cardinality constraint and returns its encoding.
     * @param f      the formula factory to generate new formulas
     * @param cc     the cardinality constraint
     * @param config the configuration for the encoder
     * @return the encoding of the constraint and the incremental data
     */
    public static Pair<List<Formula>, CcIncrementalData>
    encodeIncremental(final FormulaFactory f, final CardinalityConstraint cc, final EncoderConfig config) {
        final EncodingResult result = EncodingResult.resultForFormula(f);
        final CcIncrementalData incData = encodeIncremental(cc, result, config);
        return new Pair<>(Collections.unmodifiableList(result.getResult()), incData);
    }

    /**
     * Encodes an incremental cardinality constraint on a given solver.
     * @param cc     the cardinality constraint
     * @param result the result of the encoding
     * @return the incremental data
     */
    public static CcIncrementalData encodeIncremental(final CardinalityConstraint cc, final EncodingResult result) {
        return encodeIncrementalConstraint(cc, result, null);
    }

    /**
     * Encodes an incremental cardinality constraint on a given solver.
     * @param cc     the cardinality constraint
     * @param result the result of the encoding
     * @param config the configuration for the encoder
     * @return the incremental data
     */
    public static CcIncrementalData encodeIncremental(final CardinalityConstraint cc, final EncodingResult result,
                                                      final EncoderConfig config) {
        return encodeIncrementalConstraint(cc, result, config);
    }

    protected static CcIncrementalData encodeIncrementalConstraint(final CardinalityConstraint cc,
                                                                   final EncodingResult result,
                                                                   final EncoderConfig initConfig) {
        final var config = initConfig != null ? initConfig
                : (EncoderConfig) result.getFactory().configurationFor(ConfigurationType.ENCODER);
        final Variable[] ops = FormulaHelper.literalsAsVariables(cc.getOperands());
        if (cc.isAmo()) {
            throw new IllegalArgumentException("Incremental encodings are not supported for at-most-one constraints");
        }
        switch (cc.comparator()) {
            case LE:
                return amkIncremental(result, config, ops, cc.getRhs());
            case LT:
                return amkIncremental(result, config, ops, cc.getRhs() - 1);
            case GE:
                return alkIncremental(result, config, ops, cc.getRhs());
            case GT:
                return alkIncremental(result, config, ops, cc.getRhs() + 1);
            default:
                throw new IllegalArgumentException(
                        "Incremental encodings are only supported for at-most-k and at-least k constraints.");
        }
    }

    protected static void encodeConstraint(final CardinalityConstraint cc, final EncodingResult result,
                                           final EncoderConfig initConfig) {
        final var config = initConfig != null ? initConfig
                : (EncoderConfig) result.getFactory().configurationFor(ConfigurationType.ENCODER);
        final Variable[] ops = FormulaHelper.literalsAsVariables(cc.getOperands());
        switch (cc.comparator()) {
            case LE:
                if (cc.getRhs() == 1) {
                    amo(result, config, ops);
                } else {
                    amk(result, config, ops, cc.getRhs());
                }
                break;
            case LT:
                if (cc.getRhs() == 2) {
                    amo(result, config, ops);
                } else {
                    amk(result, config, ops, cc.getRhs() - 1);
                }
                break;
            case GE:
                alk(result, config, ops, cc.getRhs());
                break;
            case GT:
                alk(result, config, ops, cc.getRhs() + 1);
                break;
            case EQ:
                if (cc.getRhs() == 1) {
                    exo(result, config, ops);
                } else {
                    exk(result, config, ops, cc.getRhs());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown pseudo-Boolean comparator: " + cc.comparator());
        }
    }

    private static void amo(final EncodingResult result, final EncoderConfig config, final Variable... vars) {
        if (vars.length <= 1) {
            return;
        }
        switch (config.amoEncoder) {
            case PURE:
                CcAmo.pure(result, vars);
                break;
            case LADDER:
                CcAmo.ladder(result, vars);
                break;
            case PRODUCT:
                CcAmo.product(result, config.productRecursiveBound, vars);
                break;
            case NESTED:
                CcAmo.nested(result, config.nestingGroupSize, vars);
                break;
            case COMMANDER:
                CcAmo.commander(result, config.commanderGroupSize, vars);
                break;
            case BINARY:
                CcAmo.binary(result, vars);
                break;
            case BIMANDER:
                CcAmo.bimander(result, computeBimanderGroupSize(config, vars.length), vars);
                break;
            case BEST:
                bestAMO(result, config, vars);
                break;
            default:
                throw new IllegalStateException("Unknown at-most-one encoder: " + config.amoEncoder);
        }
    }

    protected static void exo(final EncodingResult result, final EncoderConfig config, final Variable... vars) {
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

    protected static void amk(final EncodingResult result, final EncoderConfig config, final Variable[] vars,
                              final int rhs) {
        if (rhs < 0) {
            throw new IllegalArgumentException("Invalid right hand side of cardinality constraint: " + rhs);
        }
        if (rhs >= vars.length) { // there is no constraint
            return;
        }
        if (rhs == 0) { // no variable can be true
            for (final Variable var : vars) {
                result.addClause(var.negate(result.getFactory()));
            }
            return;
        }
        switch (config.amkEncoder) {
            case TOTALIZER:
                CcTotalizer.amk(result, vars, rhs);
                break;
            case MODULAR_TOTALIZER:
            case BEST:
                CcModularTotalizer.amk(result, vars, rhs);
                break;
            case CARDINALITY_NETWORK:
                CcCardinalityNetwork.amk(result, vars, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown at-most-k encoder: " + config.amkEncoder);
        }
    }

    protected static CcIncrementalData amkIncremental(final EncodingResult result, final EncoderConfig config,
                                                      final Variable[] vars, final int rhs) {
        if (rhs < 0) {
            throw new IllegalArgumentException("Invalid right hand side of cardinality constraint: " + rhs);
        }
        if (rhs >= vars.length) { // there is no constraint
            return null;
        }
        if (rhs == 0) { // no variable can be true
            for (final Variable var : vars) {
                result.addClause(var.negate(result.getFactory()));
            }
            return null;
        }
        switch (config.amkEncoder) {
            case TOTALIZER:
                return CcTotalizer.amk(result, vars, rhs);
            case MODULAR_TOTALIZER:
            case BEST:
                return CcModularTotalizer.amk(result, vars, rhs);
            case CARDINALITY_NETWORK:
                return CcCardinalityNetwork.amkForIncremental(result, vars, rhs);
            default:
                throw new IllegalStateException("Unknown at-most-k encoder: " + config.amkEncoder);
        }
    }

    protected static void alk(final EncodingResult result, final EncoderConfig config, final Variable[] vars,
                              final int rhs) {
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
                CcTotalizer.alk(result, vars, rhs);
                break;
            case MODULAR_TOTALIZER:
            case BEST:
                CcModularTotalizer.alk(result, vars, rhs);
                break;
            case CARDINALITY_NETWORK:
                CcCardinalityNetwork.alk(result, vars, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown at-least-k encoder: " + config.alkEncoder);
        }
    }

    protected static CcIncrementalData alkIncremental(final EncodingResult result, final EncoderConfig config,
                                                      final Variable[] vars, final int rhs) {
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
                return CcTotalizer.alk(result, vars, rhs);
            case MODULAR_TOTALIZER:
            case BEST:
                return CcModularTotalizer.alk(result, vars, rhs);
            case CARDINALITY_NETWORK:
                return CcCardinalityNetwork.alkForIncremental(result, vars, rhs);
            default:
                throw new IllegalStateException("Unknown at-least-k encoder: " + config.alkEncoder);
        }
    }

    protected static void exk(final EncodingResult result, final EncoderConfig config, final Variable[] vars,
                              final int rhs) {
        if (rhs < 0) {
            throw new IllegalArgumentException("Invalid right hand side of cardinality constraint: " + rhs);
        }
        if (rhs > vars.length) {
            result.addClause();
            return;
        }
        if (rhs == 0) {
            for (final Variable var : vars) {
                result.addClause(var.negate(result.getFactory()));
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
            case BEST:
                CcTotalizer.exk(result, vars, rhs);
                break;
            case CARDINALITY_NETWORK:
                CcCardinalityNetwork.exk(result, vars, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown exactly-k encoder: " + config.exkEncoder);
        }
    }

    /**
     * Returns the best at-most-one encoding for a given number of variables.
     * The valuation is based on theoretical and practical observations. For
     * &lt;= 10 the pure encoding without introduction of new variables is used,
     * otherwise the product encoding is chosen.
     */
    private static void bestAMO(final EncodingResult result, final EncoderConfig config, final Variable... vars) {
        if (vars.length <= 10) {
            CcAmo.pure(result, vars);
        } else {
            CcAmo.product(result, config.productRecursiveBound, vars);
        }
    }

    private static int computeBimanderGroupSize(final EncoderConfig config, final int numVars) {
        switch (config.bimanderGroupSize) {
            case FIXED:
                return config.bimanderFixedGroupSize;
            case HALF:
                return numVars / 2;
            case SQRT:
                return (int) Math.sqrt(numVars);
            default:
                throw new IllegalStateException("Unknown bimander group size: " + config.bimanderGroupSize);
        }
    }
}
