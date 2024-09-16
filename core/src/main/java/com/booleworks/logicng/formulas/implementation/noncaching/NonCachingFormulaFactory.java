// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.noncaching;

import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NonCachingFormulaFactory extends FormulaFactory {

    Map<String, Variable> posLiterals;
    Map<String, Literal> negLiterals;

    /**
     * Constructor for a new formula factory with the default configuration.
     */
    public NonCachingFormulaFactory() {
        this(FormulaFactoryConfig.builder().build());
        posLiterals = new ConcurrentHashMap<>();
        negLiterals = new ConcurrentHashMap<>();
    }

    /**
     * Constructor for a new formula factory.
     * @param config the configuration for this formula factory
     */
    public NonCachingFormulaFactory(final FormulaFactoryConfig config) {
        super(config);
        cFalse = new LngNativeFalse(this);
        cTrue = new LngNativeTrue(this);
        clear();
    }

    @Override
    protected Formula internalImplication(final Formula left, final Formula right) {
        return new LngNativeImplication(left, right, this);
    }

    @Override
    protected Formula internalEquivalence(final Formula left, final Formula right) {
        return new LngNativeEquivalence(left, right, this);
    }

    @Override
    protected Formula internalNot(final Formula operand) {
        return new LngNativeNot(operand, this);
    }

    /**
     * Creates a new conjunction.
     * @param operandsIn the formulas
     * @return a new conjunction
     */
    @Override
    protected Formula internalAnd(final LinkedHashSet<? extends Formula> operandsIn) {
        final LinkedHashSet<? extends Formula> operands = importOrPanicLhs(operandsIn);
        final LinkedHashSet<? extends Formula> condensedOperands =
                operands.size() < 2 ? operands : condenseOperandsAnd(operands);
        if (condensedOperands == null) {
            return falsum();
        }
        if (condensedOperands.isEmpty()) {
            return verum();
        }
        if (condensedOperands.size() == 1) {
            return condensedOperands.iterator().next();
        }
        return new LngNativeAnd(condensedOperands, this);
    }

    @Override
    protected Formula internalCnf(final LinkedHashSet<? extends Formula> clausesIn) {
        final LinkedHashSet<? extends Formula> clauses = importOrPanicLhs(clausesIn);
        if (clauses.isEmpty()) {
            return verum();
        }
        if (clauses.size() == 1) {
            return clauses.iterator().next();
        }
        return new LngNativeAnd(clauses, this);
    }

    @Override
    protected Formula internalOr(final LinkedHashSet<? extends Formula> operandsIn) {
        final LinkedHashSet<? extends Formula> operands = importOrPanicLhs(operandsIn);
        final LinkedHashSet<? extends Formula> condensedOperands =
                operands.size() < 2 ? operands : condenseOperandsOr(operands);
        if (condensedOperands == null) {
            return verum();
        }
        if (condensedOperands.isEmpty()) {
            return falsum();
        }
        if (condensedOperands.size() == 1) {
            return condensedOperands.iterator().next();
        }
        return new LngNativeOr(condensedOperands, this);
    }

    @Override
    protected Formula internalClause(final LinkedHashSet<Literal> literalsIn) {
        final LinkedHashSet<? extends Formula> literals = importOrPanicLhs(literalsIn);
        if (literals.isEmpty()) {
            return falsum();
        }
        if (literals.size() == 1) {
            return literals.iterator().next();
        }
        return new LngNativeOr(literals, this);
    }

    @Override
    public Variable variable(final String name) {
        if (readOnly) {
            throwReadOnlyException();
        }
        Variable var = posLiterals.get(name);
        if (var == null) {
            var = new LngNativeVariable(name, this);
            posLiterals.put(name, var);
        }
        return var;
    }

    @Override
    protected Literal internalNegativeLiteral(final String name) {
        Literal lit = negLiterals.get(name);
        if (lit == null) {
            lit = new LngNativeLiteral(name, false, this);
            negLiterals.put(name, lit);
        }
        return lit;
    }

    @Override
    protected Formula internalPbc(final List<? extends Literal> literals, final List<Integer> coefficients,
                                  final CType comparator, final int rhs) {
        return new LngNativePbConstraint(literals, coefficients, comparator, rhs, this);
    }

    @Override
    protected Formula internalCc(final List<? extends Literal> literals, final CType comparator, final int rhs) {
        return new LngNativeCardinalityConstraint(importOrPanic(literals), comparator, rhs, this);
    }

    @Override
    protected Formula negateOrNull(final Formula formula) {
        return formula.negate(this);
    }

    @Override
    public void clear() {
        super.clear();
        posLiterals = new HashMap<>();
        negLiterals = new HashMap<>();
    }

    @Override
    public String toString() {
        return "Name: " + name;
    }
}
