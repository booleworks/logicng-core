// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.cnf;

import org.logicng.datastructures.Assignment;
import org.logicng.formulas.And;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.transformations.StatefulFormulaTransformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transformation of a formula into CNF due to Tseitin.  Results in this implementation will always be cached.
 * <p>
 * ATTENTION: if you mix formulas from different formula factories this can lead to clashes in the naming of newly
 * introduced variables.
 * @version 3.0.0
 * @since 1.0
 */
public final class TseitinTransformation extends StatefulFormulaTransformation<TseitinTransformation.TseitinState> {

    public static final int DEFAULT_BOUNDARY = 12;

    private final int boundaryForFactorization;
    private final CNFFactorization factorization;

    /**
     * Constructor for a Tseitin transformation with the default factorization bound of 12.
     * @param f the caching formula factory to generate new formulas
     **/
    public TseitinTransformation(final FormulaFactory f) {
        this(f, DEFAULT_BOUNDARY);
    }

    /**
     * Constructor for a Tseitin transformation.
     * @param f                        the caching formula factory to generate new formulas
     * @param boundaryForFactorization the boundary of number of atoms up to which classical factorization is used
     */
    public TseitinTransformation(final FormulaFactory f, final int boundaryForFactorization) {
        super(f);
        this.boundaryForFactorization = boundaryForFactorization;
        factorization = new CNFFactorization(f);
    }

    /**
     * Constructor for a Tseitin transformation with the default factorization bound of 12.
     * @param f     the formula factory to generate new formulas
     * @param state the mutable state for a Tseitin transformation
     */
    public TseitinTransformation(final FormulaFactory f, final TseitinState state) {
        this(f, DEFAULT_BOUNDARY, state);
    }

    /**
     * Constructor for a Tseitin transformation.
     * @param f                        the non-caching formula factory to generate new formulas
     * @param boundaryForFactorization the boundary of number of atoms up to which classical factorization is used
     * @param state                    the mutable state for a Tseitin transformation
     */
    public TseitinTransformation(final FormulaFactory f, final int boundaryForFactorization, final TseitinState state) {
        super(f, state);
        this.boundaryForFactorization = boundaryForFactorization;
        factorization = new CNFFactorization(f);
    }

    @Override
    protected TseitinState inititialState() {
        return new TseitinState();
    }

    @Override
    public Formula apply(final Formula formula) {
        final Formula nnf = formula.nnf(f);
        if (nnf.isCNF(f)) {
            return nnf;
        }
        Formula tseitin = state.formula(nnf);
        if (tseitin != null) {
            final Assignment topLevel = new Assignment(state.literal(nnf));
            return state.formula(nnf).restrict(topLevel, f);
        }
        if (nnf.numberOfAtoms(f) < boundaryForFactorization) {
            tseitin = nnf.transform(factorization);
        } else {
            for (final Formula formula1 : nnf.apply(f.subformulaFunction())) {
                computeTseitin(formula1);
            }
            final Assignment topLevel = new Assignment(state.literal(nnf));
            tseitin = state.formula(nnf).restrict(topLevel, f);
        }
        state.literalMap.put(formula, state.literal(nnf));
        return tseitin;
    }

    /**
     * Computes the Tseitin transformation for a given formula and stores it in the formula cache.
     * @param formula the formula
     */
    private void computeTseitin(final Formula formula) {
        if (state.formula(formula) != null) {
            return;
        }
        switch (formula.type()) {
            case LITERAL:
                state.formulaMap.put(formula, formula);
                state.literalMap.put(formula, (Literal) formula);
                break;
            case AND:
            case OR:
                final boolean isConjunction = formula instanceof And;
                final Literal tsLiteral = f.newCNFVariable();
                final List<Formula> nops = new ArrayList<>();
                final List<Formula> operands = new ArrayList<>(formula.numberOfOperands());
                final List<Formula> negOperands = new ArrayList<>(formula.numberOfOperands());
                if (isConjunction) {
                    negOperands.add(tsLiteral);
                    handleNary(formula, nops, operands, negOperands);
                    for (final Formula operand : operands) {
                        nops.add(f.or(tsLiteral.negate(f), operand));
                    }
                    nops.add(f.or(negOperands));
                } else {
                    operands.add(tsLiteral.negate(f));
                    handleNary(formula, nops, operands, negOperands);
                    for (final Formula operand : negOperands) {
                        nops.add(f.or(tsLiteral, operand));
                    }
                    nops.add(f.or(operands));
                }
                state.literalMap.put(formula, tsLiteral);
                state.formulaMap.put(formula, f.and(nops));
                break;
            default:
                throw new IllegalArgumentException("Could not process the formula type " + formula.type());
        }
    }

    private void handleNary(final Formula formula, final List<Formula> nops, final List<Formula> operands, final List<Formula> negOperands) {
        for (final Formula op : formula) {
            if (op.type() != FType.LITERAL) {
                computeTseitin(op);
                nops.add(state.formula(op));
            }
            operands.add(state.literal(op));
            negOperands.add(state.literal(op).negate(f));
        }
    }

    @Override
    public String toString() {
        return String.format("TseitinTransformation{boundary=%d}", boundaryForFactorization);
    }

    public static final class TseitinState {
        private final Map<Formula, Formula> formulaMap;
        private final Map<Formula, Literal> literalMap;

        public TseitinState() {
            formulaMap = new HashMap<>();
            literalMap = new HashMap<>();
        }

        public TseitinState(final Map<Formula, Formula> formulaMap, final Map<Formula, Literal> literalMap) {
            this.formulaMap = formulaMap;
            this.literalMap = literalMap;
        }

        private Formula formula(final Formula formula) {
            return formulaMap.get(formula);
        }

        private Literal literal(final Formula formula) {
            return literalMap.get(formula);
        }
    }
}
