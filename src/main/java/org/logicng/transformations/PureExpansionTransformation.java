// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations;

import static org.logicng.util.FormulaHelper.literalsAsVariables;

import org.logicng.cardinalityconstraints.CCAMOPure;
import org.logicng.datastructures.EncodingResult;
import org.logicng.formulas.BinaryOperator;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.NAryOperator;
import org.logicng.formulas.Not;
import org.logicng.formulas.PBConstraint;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Transformation of a formula to a formula with expanded at-most-one and exactly-one cardinality constraints.
 * Each sub-formula of the formula that is a pseudo-Boolean constraint of type AMO or EXO gets replaced by a pure encoding such that
 * the resulting formula is equivalent and free of pseudo-Boolean constraints.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class PureExpansionTransformation extends StatelessFormulaTransformation {

    public PureExpansionTransformation(final FormulaFactory f) {
        super(f);
    }

    @Override
    public Formula apply(final Formula formula) {
        switch (formula.type()) {
            case FALSE:
            case TRUE:
            case LITERAL:
                return formula;
            case NOT:
                final Not not = (Not) formula;
                return f.not(apply(not.operand()));
            case OR:
            case AND:
                final NAryOperator nary = (NAryOperator) formula;
                final List<Formula> newOps = new ArrayList<>(nary.numberOfOperands());
                for (final Formula op : nary) {
                    newOps.add(apply(op));
                }
                return f.naryOperator(formula.type(), newOps);
            case IMPL:
            case EQUIV:
                final BinaryOperator binary = (BinaryOperator) formula;
                final Formula newLeft = apply(binary.left());
                final Formula newRight = apply(binary.right());
                return f.binaryOperator(formula.type(), newLeft, newRight);
            case PBC:
                final PBConstraint pbc = (PBConstraint) formula;
                if (pbc.isAmo() || pbc.isExo()) {
                    final EncodingResult encodingResult = EncodingResult.resultForFormula(f);
                    final Variable[] vars = literalsAsVariables(pbc.operands());
                    CCAMOPure.get().build(encodingResult, vars);
                    final List<Formula> encoding = encodingResult.result();
                    if (pbc.isExo()) {
                        encoding.add(f.or(vars));
                    }
                    return f.and(encoding);
                } else {
                    throw new UnsupportedOperationException("Pure encoding for a PBC of type other than AMO or EXO is currently not supported.");
                }
            default:
                throw new IllegalStateException("Unknown formula type: " + formula.type());
        }
    }
}
