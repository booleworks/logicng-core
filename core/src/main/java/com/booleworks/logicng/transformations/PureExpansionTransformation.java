// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.encodings.cc.CcAmo;
import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.NAryOperator;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.util.FormulaHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Transformation of a formula to a formula with expanded at-most-one and
 * exactly-one cardinality constraints. Each sub-formula of the formula that is
 * a pseudo-Boolean constraint of type AMO or EXO gets replaced by a pure
 * encoding such that the resulting formula is equivalent and free of
 * pseudo-Boolean constraints.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class PureExpansionTransformation extends StatelessFormulaTransformation {

    public PureExpansionTransformation(final FormulaFactory f) {
        super(f);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        return LngResult.of(expand(formula));
    }

    private Formula expand(final Formula formula) {
        switch (formula.getType()) {
            case FALSE:
            case TRUE:
            case LITERAL:
            case PREDICATE:
                return formula;
            case NOT:
                final Not not = (Not) formula;
                return f.not(apply(not.getOperand()));
            case OR:
            case AND:
                final NAryOperator nary = (NAryOperator) formula;
                final List<Formula> newOps = new ArrayList<>(nary.numberOfOperands());
                for (final Formula op : nary) {
                    newOps.add(apply(op));
                }
                return f.naryOperator(formula.getType(), newOps);
            case IMPL:
            case EQUIV:
                final BinaryOperator binary = (BinaryOperator) formula;
                final Formula newLeft = apply(binary.getLeft());
                final Formula newRight = apply(binary.getRight());
                return f.binaryOperator(formula.getType(), newLeft, newRight);
            case PBC:
                final PbConstraint pbc = (PbConstraint) formula;
                if (pbc.isAmo() || pbc.isExo()) {
                    final EncodingResult encodingResult = EncodingResult.resultForFormula(f);
                    final Variable[] vars = FormulaHelper.literalsAsVariables(pbc.getOperands());
                    CcAmo.pure(encodingResult, vars);
                    final List<Formula> encoding = encodingResult.getResult();
                    if (pbc.isExo()) {
                        encoding.add(f.or(vars));
                    }
                    return f.and(encoding);
                } else {
                    throw new UnsupportedOperationException(
                            "Pure encoding for a PBC of type other than AMO or EXO is currently not supported.");
                }
            default:
                throw new IllegalStateException("Unknown formula type: " + formula.getType());
        }
    }
}
