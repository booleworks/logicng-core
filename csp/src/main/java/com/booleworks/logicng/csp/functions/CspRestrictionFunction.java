// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.NAryOperator;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.transformations.StatelessFormulaTransformation;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Function for restricting boolean variables and interger variables.
 */
public class CspRestrictionFunction extends StatelessFormulaTransformation {
    protected final CspFactory cf;
    protected final CspAssignment restrictions;
    protected final Assignment booleanRestrictions;

    /**
     * Constructor.
     * @param cf the factory
     **/
    public CspRestrictionFunction(final CspFactory cf, final CspAssignment restrictions) {
        super(cf.getFormulaFactory());
        this.cf = cf;
        this.restrictions = restrictions;
        this.booleanRestrictions = new Assignment(Stream.concat(
                restrictions.negativeBooleans().stream().map(l -> l.variable().negate(f)),
                restrictions.positiveBooleans().stream()
        ).collect(Collectors.toList()));
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        return LngResult.of(applyRec(formula));
    }

    protected Formula applyRec(final Formula formula) {
        switch (formula.getType()) {
            case PBC:
                return formula.restrict(f, booleanRestrictions);
            case EQUIV:
            case IMPL:
                final BinaryOperator binOp = (BinaryOperator) formula;
                return f.binaryOperator(formula.getType(), applyRec(binOp.getLeft()), applyRec(binOp.getRight()));
            case OR:
            case AND:
                final NAryOperator naryOp = (NAryOperator) formula;
                final LinkedHashSet<Formula> nops = new LinkedHashSet<>();
                for (final Formula op : naryOp.getOperands()) {
                    nops.add(applyRec(op));
                }
                return f.naryOperator(formula.getType(), nops);
            case NOT:
                return f.not(applyRec(((Not) formula).getOperand()));
            case LITERAL:
                final Literal lit = (Literal) formula;
                if (lit.getPhase() && restrictions.positiveBooleans().contains(lit.variable())) {
                    return f.verum();
                } else if (lit.getPhase() && restrictions.negativeBooleans().contains(lit)
                        || restrictions.negativeBooleans().contains(lit.negate(f))) {
                    return f.falsum();
                } else if (!lit.getPhase() && restrictions.negativeBooleans().contains(lit)
                        || restrictions.negativeBooleans().contains(lit.negate(f))) {
                    return f.verum();
                } else if (!lit.getPhase() && restrictions.positiveBooleans().contains(lit.variable())) {
                    return f.falsum();
                } else {
                    return formula;
                }
            case PREDICATE:
                if (formula instanceof CspPredicate) {
                    return ((CspPredicate) formula).restrict(cf, restrictions);
                } else {
                    return formula.restrict(f, booleanRestrictions);
                }
            case TRUE:
            case FALSE:
                return formula;
            default:
                throw new IllegalArgumentException("Unknown formula type");
        }
    }
}
