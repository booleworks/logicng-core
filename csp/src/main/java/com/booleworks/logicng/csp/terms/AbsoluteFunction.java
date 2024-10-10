package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A function term representing the absolut operation, i.e, the value of the operand is negated if it is negative.
 */
public class AbsoluteFunction extends UnaryFunction {
    /**
     * Prefix for auxiliary variables created by absolut function decomposition.
     */
    public final static String ABS_AUX_VARIABLE = "ABS";

    /**
     * Constructs a new absolut function term.
     * <p>
     * <B>This constructor should not be used!</B> Use {@link CspFactory} to create new terms.
     * @param operand the operand
     */
    public AbsoluteFunction(final Term operand) {
        super(Term.Type.ABS, operand);
    }

    @Override
    protected Decomposition calculateDecomposition(final CspFactory cf) {
        final Decomposition decomposition = this.getOperand().decompose(cf);
        final LinearExpression expression = decomposition.getLinearExpression();
        final IntegerDomain domain = expression.getDomain();
        if (domain.lb() >= 0) {
            return decomposition;
        } else if (domain.ub() <= 0) {
            return new Decomposition(LinearExpression.multiply(expression, -1),
                    decomposition.getAdditionalConstraints(), decomposition.getAuxiliaryIntegerVariables(),
                    decomposition.getAuxiliaryBooleanVariables());
        }
        final Set<IntegerClause> constraints = new LinkedHashSet<>(decomposition.getAdditionalConstraints());
        final Set<IntegerVariable> auxIntVars = new LinkedHashSet<>(decomposition.getAuxiliaryIntegerVariables());
        final Set<Variable> auxBoolVars = new LinkedHashSet<>(decomposition.getAuxiliaryBooleanVariables());
        final IntegerDomain newDomain = domain.abs();
        final IntegerVariable newVariable = cf.auxVariable(ABS_AUX_VARIABLE, newDomain);
        auxIntVars.add(newVariable);
        final Formula positiveFormula = cf.eq(newVariable, this.getOperand());
        final Formula negativeFormula = cf.eq(cf.minus(newVariable), this.getOperand());
        final CspPredicate.Decomposition decomp =
                cf.decompose(cf.getFormulaFactory().or(positiveFormula, negativeFormula));
        constraints.addAll(decomp.getClauses());
        auxIntVars.addAll(decomp.getAuxiliaryIntegerVariables());
        auxBoolVars.addAll(decomp.getAuxiliaryBooleanVariables());
        return new Decomposition(new LinearExpression(newVariable), constraints, auxIntVars, auxBoolVars);
    }
}
