package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A function term representing the minimum operation.
 */
public class MinFunction extends BinaryFunction {
    /**
     * Prefix for auxiliary variables introduced by the decomposition.
     */
    public final static String MIN_AUX_VARIABLE = "MIN";

    /**
     * Constructs a new min function term.
     * <p>
     * <B>This constructor should not be used!</B> Use {@link CspFactory} to create new terms.
     * @param left  the first argument
     * @param right the second argument
     */
    public MinFunction(final Term left, final Term right) {
        super(Term.Type.MIN, left, right);
    }

    @Override
    protected Decomposition calculateDecomposition(final CspFactory cf) {
        final Decomposition resultLeft = this.left.decompose(cf);
        final Decomposition resultRight = this.right.decompose(cf);
        final IntegerDomain domainLeft = resultLeft.getLinearExpression().getDomain();
        final IntegerDomain domainRight = resultRight.getLinearExpression().getDomain();

        if (domainLeft.ub() <= domainRight.lb()) {
            return resultLeft;
        } else if (domainRight.ub() <= domainLeft.lb()) {
            return resultRight;
        }

        final Set<IntegerClause> constraints = new LinkedHashSet<>(resultLeft.getAdditionalConstraints());
        final Set<IntegerVariable> intVars = new LinkedHashSet<>(resultLeft.getAuxiliaryIntegerVariables());
        final Set<Variable> boolVars = new LinkedHashSet<>(resultLeft.getAuxiliaryBooleanVariables());
        constraints.addAll(resultRight.getAdditionalConstraints());
        intVars.addAll(resultRight.getAuxiliaryIntegerVariables());
        boolVars.addAll(resultRight.getAuxiliaryBooleanVariables());

        final IntegerDomain newDomain = domainLeft.min(domainRight);
        final IntegerVariable x = cf.auxVariable(MIN_AUX_VARIABLE, newDomain);
        intVars.add(x);

        final CspPredicate.Decomposition d1 = cf.le(x, this.left).decompose(cf);
        final CspPredicate.Decomposition d2 = cf.le(x, this.right).decompose(cf);
        final Formula leLeft = cf.ge(x, this.left);
        final Formula leRight = cf.ge(x, this.right);
        final CspPredicate.Decomposition d3 = cf.decompose(cf.getFormulaFactory().or(leLeft, leRight));
        final Decomposition newTerm = new Decomposition(new LinearExpression(x), constraints, intVars, boolVars);
        return Term.Decomposition.merge(newTerm, List.of(d1, d2, d3));
    }
}
