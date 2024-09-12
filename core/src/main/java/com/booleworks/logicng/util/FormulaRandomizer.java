// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.util;

import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Constant;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A randomizer for formulas.
 * <p>
 * The formula types included in the generated formulas can be configured with a
 * {@link FormulaRandomizerConfig}.
 * @version 2.3.0
 * @since 2.0.0
 */
public final class FormulaRandomizer {

    private final FormulaFactory f;
    private final FormulaRandomizerConfig config;
    private final Random random;
    private final Variable[] variables;

    private final FormulaTypeProbabilities formulaTypeProbabilities;
    private final CTypeProbabilities cTypeProbabilities;
    private final double phaseProbability;
    private final double coefficientNegativeProbability;

    /**
     * Generates a new formula randomizer. With the given formula factory and
     * the randomizer configuration from the formula factory.
     * @param f the formula factory
     */
    public FormulaRandomizer(final FormulaFactory f) {
        this(f, null);
    }

    /**
     * Generates a new formula randomizer. With the given formula factory and
     * configuration.
     * @param f      the formula factory
     * @param config the formula randomizer configuration
     */
    public FormulaRandomizer(final FormulaFactory f, final FormulaRandomizerConfig config) {
        this.f = f;
        this.config = config != null ? config
                : (FormulaRandomizerConfig) f.configurationFor(ConfigurationType.FORMULA_RANDOMIZER);
        random = this.config.seed != 0 ? new Random(this.config.seed) : new Random();
        variables = generateVars(f, this.config);
        formulaTypeProbabilities = new FormulaTypeProbabilities(this.config);
        cTypeProbabilities = new CTypeProbabilities(this.config);
        phaseProbability = generatePhaseProbability(this.config);
        coefficientNegativeProbability = this.config.weightPbcCoeffNegative /
                (this.config.weightPbcCoeffPositive + this.config.weightPbcCoeffNegative);
    }

    /**
     * Returns a random constant.
     * @return the random constant
     */
    public Constant constant() {
        return f.constant(random.nextBoolean());
    }

    /**
     * Returns a random variable.
     * @return the random variable
     */
    public Variable variable() {
        return variables[random.nextInt(variables.length)];
    }

    /**
     * Returns a random literal. The probability of whether it is positive or
     * negative depends on the configuration.
     * @return the random literal
     */
    public Literal literal() {
        return f.literal(variables[random.nextInt(variables.length)].name(), random.nextDouble() < phaseProbability);
    }

    /**
     * Returns a random atom. This includes constants, literals, pseudo boolean
     * constraints, and cardinality constraints (including amo and exo).
     * @return the random atom
     */
    public Formula atom() {
        final double n = random.nextDouble() * formulaTypeProbabilities.exo;
        if (n < formulaTypeProbabilities.constant) {
            return constant();
        } else if (n < formulaTypeProbabilities.literal) {
            return literal();
        } else if (n < formulaTypeProbabilities.pbc) {
            return pbc();
        } else if (n < formulaTypeProbabilities.cc) {
            return cc();
        } else if (n < formulaTypeProbabilities.amo) {
            return amo();
        } else {
            return exo();
        }
    }

    /**
     * Returns a random negation with a given maximal depth.
     * @param maxDepth the maximal depth
     * @return the random negation
     */
    public Formula not(final int maxDepth) {
        if (maxDepth == 0) {
            return atom();
        }
        final Formula not = f.not(formula(maxDepth - 1));
        if (maxDepth >= 2 && not.type() != FType.NOT) {
            return not(maxDepth);
        }
        return not;
    }

    /**
     * Returns a random implication with a given maximal depth.
     * @param maxDepth the maximal depth
     * @return the random implication
     */
    public Formula impl(final int maxDepth) {
        if (maxDepth == 0) {
            return atom();
        }
        final Formula implication = f.implication(formula(maxDepth - 1), formula(maxDepth - 1));
        if (implication.type() != FType.IMPL) {
            return impl(maxDepth);
        }
        return implication;
    }

    /**
     * Returns a random equivalence with a given maximal depth.
     * @param maxDepth the maximal depth
     * @return the random equivalence
     */
    public Formula equiv(final int maxDepth) {
        if (maxDepth == 0) {
            return atom();
        }
        final Formula equiv = f.equivalence(formula(maxDepth - 1), formula(maxDepth - 1));
        if (equiv.type() != FType.EQUIV) {
            return equiv(maxDepth);
        }
        return equiv;
    }

    /**
     * Returns a random conjunction with a given maximal depth.
     * @param maxDepth the maximal depth
     * @return the random conjunction
     */
    public Formula and(final int maxDepth) {
        if (maxDepth == 0) {
            return atom();
        }
        final Formula[] operands = new Formula[2 + random.nextInt(config.maximumOperandsAnd - 2)];
        for (int i = 0; i < operands.length; i++) {
            operands[i] = formula(maxDepth - 1);
        }
        final Formula formula = f.and(operands);
        if (formula.type() != FType.AND) {
            return and(maxDepth);
        }
        return formula;
    }

    /**
     * Returns a random disjunction with a given maximal depth.
     * @param maxDepth the maximal depth
     * @return the random disjunction
     */
    public Formula or(final int maxDepth) {
        if (maxDepth == 0) {
            return atom();
        }
        final Formula[] operands = new Formula[2 + random.nextInt(config.maximumOperandsOr - 2)];
        for (int i = 0; i < operands.length; i++) {
            operands[i] = formula(maxDepth - 1);
        }
        final Formula formula = f.or(operands);
        if (formula.type() != FType.OR) {
            return or(maxDepth);
        }
        return formula;
    }

    /**
     * Returns a random cardinality constraint.
     * @return the random cardinality constraint
     */
    public Formula cc() {
        final Variable[] variables = variables();
        final CType type = cType();
        int rhsBound = variables.length;
        if (type == CType.GT) {
            rhsBound = variables.length + 1;
        } else if (type == CType.LT) {
            rhsBound = variables.length + 1;
        }
        int rhsOffset = 0;
        if (type == CType.GT) {
            rhsOffset = -1;
        } else if (type == CType.LT) {
            rhsOffset = 1;
        }
        final int rhs = rhsOffset + random.nextInt(rhsBound);
        final Formula cc = f.cc(type, rhs, variables);
        if (cc.isConstantFormula()) {
            return cc();
        }
        return cc;
    }

    /**
     * Returns a random at-most-one constraint.
     * @return the random at-most-one constraint
     */
    public Formula amo() {
        return f.amo(variables());
    }

    /**
     * Returns a random exactly-one constraint.
     * @return the random exactly-one constraint
     */
    public Formula exo() {
        return f.exo(variables());
    }

    /**
     * Returns a random pseudo boolean constraint.
     * @return the random pseudo boolean constraint
     */
    public Formula pbc() {
        final int numOps = random.nextInt(config.maximumOperandsPbc);
        final Literal[] literals = new Literal[numOps];
        final int[] coefficients = new int[numOps];
        int minSum = 0; // (positive) sum of all negative coefficients
        int maxSum = 0; // sum of all positive coefficients
        for (int i = 0; i < numOps; i++) {
            literals[i] = literal();
            coefficients[i] = random.nextInt(config.maximumCoefficientPbc) + 1;
            if (random.nextDouble() < coefficientNegativeProbability) {
                minSum += coefficients[i];
                coefficients[i] = -coefficients[i];
            } else {
                maxSum += coefficients[i];
            }
        }
        final CType type = cType();
        final int rhs = random.nextInt(maxSum + minSum + 1) - minSum;
        final Formula pbc = f.pbc(type, rhs, literals, coefficients);
        if (pbc.isConstantFormula()) {
            return pbc();
        }
        return pbc;
    }

    /**
     * Returns a random formula with a given maximal depth.
     * @param maxDepth the maximal depth
     * @return the random formula
     */
    public Formula formula(final int maxDepth) {
        if (maxDepth == 0) {
            return atom();
        } else {
            final double n = random.nextDouble();
            if (n < formulaTypeProbabilities.constant) {
                return constant();
            } else if (n < formulaTypeProbabilities.literal) {
                return literal();
            } else if (n < formulaTypeProbabilities.pbc) {
                return pbc();
            } else if (n < formulaTypeProbabilities.cc) {
                return cc();
            } else if (n < formulaTypeProbabilities.amo) {
                return amo();
            } else if (n < formulaTypeProbabilities.exo) {
                return exo();
            } else if (n < formulaTypeProbabilities.or) {
                return or(maxDepth);
            } else if (n < formulaTypeProbabilities.and) {
                return and(maxDepth);
            } else if (n < formulaTypeProbabilities.not) {
                return not(maxDepth);
            } else if (n < formulaTypeProbabilities.impl) {
                return impl(maxDepth);
            } else {
                return equiv(maxDepth);
            }
        }
    }

    /**
     * Returns a list of {@code numConstraints} random formula with a given
     * maximal depth.
     * @param numConstraints the number of constraints to be generated
     * @param maxDepth       the maximal depth
     * @return the random formula
     */
    public List<Formula> constraintSet(final int numConstraints, final int maxDepth) {
        return Stream.generate(() -> formula(maxDepth)).limit(numConstraints).collect(Collectors.toList());
    }

    private Variable[] variables() {
        final Variable[] variables = new Variable[random.nextInt(config.maximumOperandsCc - 1) + 2];
        for (int i = 0; i < variables.length; i++) {
            variables[i] = variable();
        }
        return variables;
    }

    private static Variable[] generateVars(final FormulaFactory f, final FormulaRandomizerConfig config) {
        if (config.variables != null) {
            return config.variables.toArray(new Variable[0]);
        } else {
            final Variable[] variables = new Variable[config.numVars];
            final int decimalPlaces = (int) Math.ceil(Math.log10(config.numVars));
            for (int i = 0; i < variables.length; i++) {
                variables[i] = f.variable("v" + String.format("%0" + decimalPlaces + "d", i));
            }
            return variables;
        }
    }

    private double generatePhaseProbability(final FormulaRandomizerConfig config) {
        return config.weightPositiveLiteral / (config.weightPositiveLiteral + config.weightNegativeLiteral);
    }

    private CType cType() {
        final CType type;
        final double n = random.nextDouble();
        if (n < cTypeProbabilities.le) {
            type = CType.LE;
        } else if (n < cTypeProbabilities.lt) {
            type = CType.LT;
        } else if (n < cTypeProbabilities.ge) {
            type = CType.GE;
        } else if (n < cTypeProbabilities.gt) {
            type = CType.GT;
        } else {
            type = CType.EQ;
        }
        return type;
    }

    private static class FormulaTypeProbabilities {
        private final double constant;
        private final double literal;
        private final double pbc;
        private final double cc;
        private final double amo;
        private final double exo;
        private final double or;
        private final double and;
        private final double not;
        private final double impl;
        private final double equiv;

        private FormulaTypeProbabilities(final FormulaRandomizerConfig config) {
            final double total = config.weightConstant + config.weightPositiveLiteral + config.weightNegativeLiteral +
                    config.weightOr +
                    config.weightAnd + config.weightNot + config.weightImpl + config.weightEquiv +
                    config.weightPbc + config.weightCc + config.weightAmo + config.weightExo;
            constant = config.weightConstant / total;
            literal = constant + (config.weightPositiveLiteral + config.weightNegativeLiteral) / total;
            pbc = literal + config.weightPbc / total;
            cc = pbc + config.weightCc / total;
            amo = cc + config.weightAmo / total;
            exo = amo + config.weightExo / total;
            or = exo + config.weightOr / total;
            and = or + config.weightAnd / total;
            not = and + config.weightNot / total;
            impl = not + config.weightImpl / total;
            equiv = impl + config.weightEquiv / total;
            assert Math.abs(equiv - 1) < 0.00000001;
        }
    }

    private static class CTypeProbabilities {
        private final double le;
        private final double lt;
        private final double ge;
        private final double gt;
        private final double eq;

        private CTypeProbabilities(final FormulaRandomizerConfig config) {
            final double total = config.weightPbcTypeLe + config.weightPbcTypeLt + config.weightPbcTypeGe +
                    config.weightPbcTypeGt + config.weightPbcTypeEq;
            le = config.weightPbcTypeLe / total;
            lt = le + config.weightPbcTypeLt / total;
            ge = lt + config.weightPbcTypeGe / total;
            gt = ge + config.weightPbcTypeGt / total;
            eq = gt + config.weightPbcTypeEq / total;
            assert Math.abs(eq - 1) < 0.00000001;
        }
    }
}
