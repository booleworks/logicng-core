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

    protected final FormulaFactory f;
    protected final CCConfig config;

    protected CCAMOPure amoPure;
    protected CCAMOLadder amoLadder;
    protected CCAMOBinary amoBinary;

    /**
     * Constructs a new cardinality constraint encoder with a given configuration.
     * @param f      the formula factory
     * @param config the configuration
     */
    public CCEncoder(final FormulaFactory f, final CCConfig config) {
        this.f = f;
        this.config = config;
    }

    /**
     * Constructs a new cardinality constraint encoder which uses the configuration of the formula factory.
     * @param f the formula factory
     */
    public CCEncoder(final FormulaFactory f) {
        this(f, null);
    }

    /**
     * Encodes a cardinality constraint and returns its CNF encoding.
     * @param cc the cardinality constraint
     * @return the CNF encoding of the cardinality constraint
     */
    public List<Formula> encode(final CardinalityConstraint cc) {
        final EncodingResult result = EncodingResult.resultForFormula(f);
        encodeConstraint(cc, result);
        return Collections.unmodifiableList(result.result());
    }

    /**
     * Encodes a cardinality constraint in a given result.
     * @param cc     the cardinality constraint
     * @param result the result of the encoding
     */
    public void encode(final CardinalityConstraint cc, final EncodingResult result) {
        encodeConstraint(cc, result);
    }

    /**
     * Encodes an incremental cardinality constraint and returns its encoding.
     * @param cc the cardinality constraint
     * @return the encoding of the constraint and the incremental data
     */
    public Pair<List<Formula>, CCIncrementalData> encodeIncremental(final CardinalityConstraint cc) {
        final EncodingResult result = EncodingResult.resultForFormula(f);
        final CCIncrementalData incData = encodeIncremental(cc, result);
        return new Pair<>(Collections.unmodifiableList(result.result()), incData);
    }

    /**
     * Encodes an incremental cardinality constraint on a given solver.
     * @param cc     the cardinality constraint
     * @param result the result of the encoding
     * @return the incremental data
     */
    public CCIncrementalData encodeIncremental(final CardinalityConstraint cc, final EncodingResult result) {
        return encodeIncrementalConstraint(cc, result);
    }

    protected CCIncrementalData encodeIncrementalConstraint(final CardinalityConstraint cc, final EncodingResult result) {
        final Variable[] ops = literalsAsVariables(cc.operands());
        if (cc.isAmo()) {
            throw new IllegalArgumentException("Incremental encodings are not supported for at-most-one constraints");
        }
        switch (cc.comparator()) {
            case LE:
                return amkIncremental(result, ops, cc.rhs());
            case LT:
                return amkIncremental(result, ops, cc.rhs() - 1);
            case GE:
                return alkIncremental(result, ops, cc.rhs());
            case GT:
                return alkIncremental(result, ops, cc.rhs() + 1);
            default:
                throw new IllegalArgumentException("Incremental encodings are only supported for at-most-k and at-least k constraints.");
        }
    }

    /**
     * Returns the current configuration of this encoder.  If the encoder was constructed with a given configuration, this
     * configuration will always be used.  Otherwise, the current configuration from the formula factory is used.
     * @return the current configuration of
     */
    public CCConfig config() {
        return config != null ? config : (CCConfig) f.configurationFor(ConfigurationType.CC_ENCODER);
    }

    /**
     * Encodes the constraint in the given result.
     * @param cc     the constraint
     * @param result the result
     */
    protected void encodeConstraint(final CardinalityConstraint cc, final EncodingResult result) {
        final Variable[] ops = literalsAsVariables(cc.operands());
        switch (cc.comparator()) {
            case LE:
                if (cc.rhs() == 1) {
                    amo(result, ops);
                } else {
                    amk(result, ops, cc.rhs());
                }
                break;
            case LT:
                if (cc.rhs() == 2) {
                    amo(result, ops);
                } else {
                    amk(result, ops, cc.rhs() - 1);
                }
                break;
            case GE:
                alk(result, ops, cc.rhs());
                break;
            case GT:
                alk(result, ops, cc.rhs() + 1);
                break;
            case EQ:
                if (cc.rhs() == 1) {
                    exo(result, ops);
                } else {
                    exk(result, ops, cc.rhs());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown pseudo-Boolean comparator: " + cc.comparator());
        }
    }

    /**
     * Encodes an at-most-one constraint.
     * @param result the result
     * @param vars   the variables of the constraint
     */
    protected void amo(final EncodingResult result, final Variable... vars) {
        if (vars.length <= 1) {
            return;
        }
        switch (config().amoEncoder) {
            case PURE:
                if (amoPure == null) {
                    amoPure = new CCAMOPure();
                }
                amoPure.build(result, vars);
                break;
            case LADDER:
                if (amoLadder == null) {
                    amoLadder = new CCAMOLadder();
                }
                amoLadder.build(result, vars);
                break;
            case PRODUCT:
                new CCAMOProduct(config().productRecursiveBound).build(result, vars);
                break;
            case NESTED:
                new CCAMONested(config().nestingGroupSize).build(result, vars);
                break;
            case COMMANDER:
                new CCAMOCommander(config().commanderGroupSize).build(result, vars);
                break;
            case BINARY:
                if (amoBinary == null) {
                    amoBinary = new CCAMOBinary();
                }
                amoBinary.build(result, vars);
                break;
            case BIMANDER:
                final int groupSize;
                switch (config().bimanderGroupSize) {
                    case FIXED:
                        groupSize = config().bimanderFixedGroupSize;
                        break;
                    case HALF:
                        groupSize = vars.length / 2;
                        break;
                    case SQRT:
                        groupSize = (int) Math.sqrt(vars.length);
                        break;
                    default:
                        throw new IllegalStateException("Unknown bimander group size: " + config().bimanderGroupSize);
                }
                new CCAMOBimander(groupSize).build(result, vars);
                break;
            case BEST:
                bestAMO(vars.length).build(result, vars);
                break;
            default:
                throw new IllegalStateException("Unknown at-most-one encoder: " + config().amoEncoder);
        }
    }

    /**
     * Encodes an at-most-one constraint.
     * @param result the result
     * @param vars   the variables of the constraint
     */
    protected void exo(final EncodingResult result, final Variable... vars) {
        if (vars.length == 0) {
            result.addClause();
            return;
        }
        if (vars.length == 1) {
            result.addClause(vars[0]);
            return;
        }
        amo(result, vars);
        result.addClause(vars);
    }

    /**
     * Encodes an at-most-k constraint.
     * @param result the result
     * @param vars   the variables of the constraint
     * @param rhs    the right-hand side of the constraint
     */
    protected void amk(final EncodingResult result, final Variable[] vars, final int rhs) {
        if (rhs < 0) {
            throw new IllegalArgumentException("Invalid right hand side of cardinality constraint: " + rhs);
        }
        if (rhs >= vars.length) // there is no constraint
        {
            return;
        }
        if (rhs == 0) { // no variable can be true
            for (final Variable var : vars) {
                result.addClause(var.negate());
            }
            return;
        }
        switch (config().amkEncoder) {
            case TOTALIZER:
                new CCAMKTotalizer().build(result, vars, rhs);
                break;
            case MODULAR_TOTALIZER:
                new CCAMKModularTotalizer(f).build(result, vars, rhs);
                break;
            case CARDINALITY_NETWORK:
                new CCAMKCardinalityNetwork().build(result, vars, rhs);
                break;
            case BEST:
                bestAMK(vars.length).build(result, vars, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown at-most-k encoder: " + config().amkEncoder);
        }
    }

    /**
     * Encodes an at-most-k constraint for incremental usage.
     * @param result the result
     * @param vars   the variables of the constraint
     * @param rhs    the right-hand side of the constraint
     * @return the incremental data
     */
    protected CCIncrementalData amkIncremental(final EncodingResult result, final Variable[] vars, final int rhs) {
        if (rhs < 0) {
            throw new IllegalArgumentException("Invalid right hand side of cardinality constraint: " + rhs);
        }
        if (rhs >= vars.length) { // there is no constraint

            return null;
        }
        if (rhs == 0) { // no variable can be true
            for (final Variable var : vars) {
                result.addClause(var.negate());
            }
            return null;
        }
        switch (config().amkEncoder) {
            case TOTALIZER:
                final var amkTotalizer = new CCAMKTotalizer();
                amkTotalizer.build(result, vars, rhs);
                return amkTotalizer.incrementalData();
            case MODULAR_TOTALIZER:
                final var amkModularTotalizer = new CCAMKModularTotalizer(f);
                amkModularTotalizer.build(result, vars, rhs);
                return amkModularTotalizer.incrementalData();
            case CARDINALITY_NETWORK:
                final var amkCardinalityNetwork = new CCAMKCardinalityNetwork();
                amkCardinalityNetwork.buildForIncremental(result, vars, rhs);
                return amkCardinalityNetwork.incrementalData();
            case BEST:
                final var bestAmk = bestAMK(vars.length);
                bestAmk.build(result, vars, rhs);
                return bestAmk.incrementalData();
            default:
                throw new IllegalStateException("Unknown at-most-k encoder: " + config().amkEncoder);
        }
    }

    /**
     * Encodes an at-lest-k constraint.
     * @param result the result
     * @param vars   the variables of the constraint
     * @param rhs    the right-hand side of the constraint
     */
    protected void alk(final EncodingResult result, final Variable[] vars, final int rhs) {
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
        switch (config().alkEncoder) {
            case TOTALIZER:
                new CCALKTotalizer().build(result, vars, rhs);
                break;
            case MODULAR_TOTALIZER:
                new CCALKModularTotalizer(f).build(result, vars, rhs);
                break;
            case CARDINALITY_NETWORK:
                new CCALKCardinalityNetwork().build(result, vars, rhs);
                break;
            case BEST:
                bestALK(vars.length).build(result, vars, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown at-least-k encoder: " + config().alkEncoder);
        }
    }

    /**
     * Encodes an at-lest-k constraint for incremental usage.
     * @param result the result
     * @param vars   the variables of the constraint
     * @param rhs    the right-hand side of the constraint
     * @return the incremental data
     */
    protected CCIncrementalData alkIncremental(final EncodingResult result, final Variable[] vars, final int rhs) {
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
        switch (config().alkEncoder) {
            case TOTALIZER:
                final var alkTotalizer = new CCALKTotalizer();
                alkTotalizer.build(result, vars, rhs);
                return alkTotalizer.incrementalData();
            case MODULAR_TOTALIZER:
                final var alkModularTotalizer = new CCALKModularTotalizer(f);
                alkModularTotalizer.build(result, vars, rhs);
                return alkModularTotalizer.incrementalData();
            case CARDINALITY_NETWORK:
                final var alkCardinalityNetwork = new CCALKCardinalityNetwork();
                alkCardinalityNetwork.buildForIncremental(result, vars, rhs);
                return alkCardinalityNetwork.incrementalData();
            case BEST:
                final var bestAlk = bestALK(vars.length);
                bestAlk.build(result, vars, rhs);
                return bestAlk.incrementalData();
            default:
                throw new IllegalStateException("Unknown at-least-k encoder: " + config().alkEncoder);
        }
    }

    /**
     * Encodes an exactly-k constraint.
     * @param result the result
     * @param vars   the variables of the constraint
     * @param rhs    the right-hand side of the constraint
     */
    protected void exk(final EncodingResult result, final Variable[] vars, final int rhs) {
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
        switch (config().exkEncoder) {
            case TOTALIZER:
                new CCEXKTotalizer().build(result, vars, rhs);
                break;
            case CARDINALITY_NETWORK:
                new CCEXKCardinalityNetwork().build(result, vars, rhs);
                break;
            case BEST:
                bestEXK(vars.length).build(result, vars, rhs);
                break;
            default:
                throw new IllegalStateException("Unknown exactly-k encoder: " + config().exkEncoder);
        }
    }

    /**
     * Returns the best at-most-one encoder for a given number of variables.  The valuation is based on theoretical and
     * practical observations.  For &lt;= 10 the pure encoding without introduction of new variables is used, otherwise
     * the product encoding is chosen.
     * @param n the number of variables
     * @return the best at-most-one encoder
     */
    protected CCAtMostOne bestAMO(final int n) {
        if (n <= 10) {
            if (amoPure == null) {
                amoPure = new CCAMOPure();
            }
            return amoPure;
        } else {
            return new CCAMOProduct(config().productRecursiveBound);
        }
    }

    /**
     * Returns the best at-most-k encoder for a given number of variables.  The valuation is based on theoretical and
     * practical observations.  Currently, the modular totalizer is the best encoder for all sizes and therefore is always
     * chosen.
     * @param n the number of variables
     * @return the best at-most-one encoder
     */
    protected CCAtMostK bestAMK(final int n) {
        return new CCAMKModularTotalizer(f);
    }

    /**
     * Returns the best at-least-k encoder for a given number of variables.  The valuation is based on theoretical and
     * practical observations.  Currently, the modular totalizer is the best encoder for all sizes and therefore is always
     * chosen.
     * @param n the number of variables
     * @return the best at-most-one encoder
     */
    protected CCAtLeastK bestALK(final int n) {
        return new CCALKModularTotalizer(f);
    }

    /**
     * Returns the best exactly-k encoder for a given number of variables.  The valuation is based on theoretical and
     * practical observations.  Currently, the totalizer is the best encoder for all sizes and therefore is always
     * chosen.
     * @param n the number of variables
     * @return the best at-most-one encoder
     */
    protected CCExactlyK bestEXK(final int n) {
        return new CCEXKTotalizer();
    }

    @Override
    public String toString() {
        return config().toString();
    }
}
