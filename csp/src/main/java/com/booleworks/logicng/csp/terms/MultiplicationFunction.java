package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.literals.ProductLiteral;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.formulas.Variable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A function term representing the multiplication operation.
 */
public final class MultiplicationFunction extends BinaryFunction {
    /**
     * Prefix for auxiliary variables introduced by the decomposition
     */
    public final static String MUL_AUX_VARIABLE = "MUL";

    /**
     * Constructs a new multiplication function term.
     * <p>
     * <B>This constructor should not be used!</B> Use {@link CspFactory} to create new terms.
     * @param left  the constant factor
     * @param right the variable
     */
    public MultiplicationFunction(final Term left, final Term right) {
        super(Term.Type.MUL, left, right);
    }

    @Override
    public Decomposition calculateDecomposition(final CspFactory cf) {
        final Decomposition resultLeft = left.decompose(cf);
        final Decomposition resultRight = right.decompose(cf);
        final IntegerDomain domainLeft = resultLeft.getLinearExpression().getDomain();
        final IntegerDomain domainRight = resultRight.getLinearExpression().getDomain();
        final Set<IntegerClause> additionalConstraints = new LinkedHashSet<>();
        final Set<IntegerVariable> additionalIntegerVariables = new LinkedHashSet<>();
        final Set<Variable> additionalBooleanVariables = new LinkedHashSet<>();
        if (domainLeft.size() == 1) {
            final LinearExpression exp = LinearExpression.multiply(resultRight.getLinearExpression(), domainLeft.lb());
            return new Decomposition(exp, additionalConstraints, additionalIntegerVariables,
                    additionalBooleanVariables);
        } else if (domainRight.size() == 1) {
            final LinearExpression exp = LinearExpression.multiply(resultLeft.getLinearExpression(), domainRight.lb());
            return new Decomposition(exp, additionalConstraints, additionalIntegerVariables,
                    additionalBooleanVariables);
        } else if (domainLeft.size() <= domainRight.size()) {
            return decomposeNonConstantMultiplication(left, right, additionalConstraints, additionalIntegerVariables,
                    additionalBooleanVariables, cf);
        } else {
            return decomposeNonConstantMultiplication(right, left, additionalConstraints, additionalIntegerVariables,
                    additionalBooleanVariables, cf);
        }
    }

    private Decomposition decomposeNonConstantMultiplication(final Term left, final Term right,
                                                             final Set<IntegerClause> additionalClauses,
                                                             final Set<IntegerVariable> additionalIntegerVariables,
                                                             final Set<Variable> additionalBooleanVariables,
                                                             final CspFactory cf) {
        final Decomposition resultLeft = left.decompose(cf);
        final Decomposition resultRight = right.decompose(cf);
        final IntegerDomain domainLeft = resultLeft.getLinearExpression().getDomain();
        final IntegerDomain domainRight = resultRight.getLinearExpression().getDomain();

        additionalClauses.addAll(resultLeft.getAdditionalConstraints());
        additionalClauses.addAll(resultRight.getAdditionalConstraints());
        additionalIntegerVariables.addAll(resultLeft.getAuxiliaryIntegerVariables());
        additionalIntegerVariables.addAll(resultRight.getAuxiliaryIntegerVariables());
        additionalBooleanVariables.addAll(resultLeft.getAuxiliaryBooleanVariables());
        additionalBooleanVariables.addAll(resultRight.getAuxiliaryBooleanVariables());

        final IntegerVariable atomLeft =
                simplifyTerm(left, additionalClauses, additionalIntegerVariables, additionalBooleanVariables, cf);
        final IntegerVariable atomRight =
                simplifyTerm(right, additionalClauses, additionalIntegerVariables, additionalBooleanVariables, cf);

        final IntegerDomain newDomain = domainLeft.mul(domainRight);
        final IntegerVariable newInt = cf.auxVariable(MUL_AUX_VARIABLE, newDomain);
        additionalClauses.add(new IntegerClause(new ProductLiteral(newInt, atomLeft, atomRight)));

        return new Decomposition(new LinearExpression(newInt), additionalClauses, additionalIntegerVariables,
                additionalBooleanVariables);
    }

    private IntegerVariable simplifyTerm(final Term term,
                                         final Set<IntegerClause> additionalClauses,
                                         final Set<IntegerVariable> additionalIntegerVariables,
                                         final Set<Variable> additionalBooleanVariables,
                                         final CspFactory cf) {
        final Decomposition decomp = term.decompose(cf);
        final IntegerDomain domain = decomp.getLinearExpression().getDomain();
        final IntegerVariable atom;
        if (term.isAtom()) {
            // Cannot be a constant because we checked for 1-sized domains before.
            assert term instanceof IntegerVariable;
            atom = (IntegerVariable) term;
        } else {
            atom = cf.auxVariable(MUL_AUX_VARIABLE, domain);
            additionalIntegerVariables.add((IntegerVariable) atom);
            final CspPredicate.Decomposition eq = cf.eq(atom, term).decompose(cf);
            additionalClauses.addAll(eq.getClauses());
            additionalIntegerVariables.addAll(eq.getAuxiliaryIntegerVariables());
            additionalBooleanVariables.addAll(eq.getAuxiliaryBooleanVariables());
        }
        return atom;
    }

    @Override
    public boolean equals(final Object o) {
        return equals(o, false);
    }

    @Override
    public int hashCode() {
        return hashCode(false);
    }
}
