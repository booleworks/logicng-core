package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.terms.AbsoluteFunction;
import com.booleworks.logicng.csp.terms.MultiplicationFunction;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.formulas.FormulaFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A predicate representing different types of comparisons between two terms.
 */
public class ComparisonPredicate extends BinaryPredicate {

    /**
     * Constructs a new comparison operator
     * <p>
     * <B>This constructor should not be used!</B> Use {@link CspFactory} to create new predicates.
     * @param type  type of the comparison
     * @param left  left side of the comparison
     * @param right right side of the comparison
     * @param f     the factory
     */
    public ComparisonPredicate(final Type type, final Term left, final Term right, final FormulaFactory f) {
        super(type, left, right, f);
    }

    @Override
    public ComparisonPredicate negate(final CspFactory cf) {
        switch (type) {
            case EQ:
                return cf.ne(left, right);
            case NE:
                return cf.eq(left, right);
            case LT:
                return cf.ge(left, right);
            case LE:
                return cf.gt(left, right);
            case GT:
                return cf.le(left, right);
            case GE:
                return cf.lt(left, right);
            default:
                throw new IllegalArgumentException("Invalid type of ComparisonPredicate: " + type);
        }
    }

    @Override
    protected Decomposition calculateDecomposition(final CspFactory cf) {
        switch (type) {
            case EQ:
                return decomposeEq(cf);
            case NE:
                return decomposeNe(cf);
            case LE:
                return decomposeLe(cf);
            case LT:
                return decomposeLt(cf);
            case GE:
                return decomposeGe(cf);
            case GT:
                return decomposeGt(cf);
            default:
                throw new IllegalArgumentException("Unexpected type for decomposing a ComparisonPredicate");
        }
    }

    private Decomposition decomposeEq(final CspFactory cf) {
        if (right.getType() == Term.Type.ZERO) {
            return decomposeEqZero(left, cf);
        } else if (left.getType() == Term.Type.ZERO) {
            return decomposeEqZero(right, cf);
        }
        return decomposeEqZero(cf.sub(left, right), cf);
    }

    private Decomposition decomposeNe(final CspFactory cf) {
        if (right.getType() == Term.Type.ZERO) {
            return decomposeNeZero(left, cf);
        } else if (left.getType() == Term.Type.ZERO) {
            return decomposeNeZero(right, cf);
        }
        return decomposeNeZero(cf.sub(left, right), cf);
    }

    private Decomposition decomposeLe(final CspFactory cf) {
        // abs(a1) <= x2
        if (left instanceof AbsoluteFunction) {
            final Term a1 = ((AbsoluteFunction) left).getOperand();
            return cf.decompose(cf.getFormulaFactory().and(cf.le(a1, right), cf.ge(a1, cf.minus(right))));
        }
        // abs(a1) >= x1
        if (right instanceof AbsoluteFunction) {
            final Term a1 = ((AbsoluteFunction) right).getOperand();
            return cf.decompose(cf.getFormulaFactory().or(cf.ge(a1, left), cf.le(a1, cf.minus(left))));
        }
        return decomposeLeZero(cf.sub(left, right), cf);
    }

    private Decomposition decomposeLt(final CspFactory cf) {
        // abs(a1) < x2
        if (left instanceof AbsoluteFunction) {
            final Term a1 = ((AbsoluteFunction) left).getOperand();
            return cf.decompose(cf.getFormulaFactory().and(cf.lt(a1, right), cf.gt(a1, cf.minus(right))));
        }
        // abs(a1) > x1
        if (right instanceof AbsoluteFunction) {
            final Term a1 = ((AbsoluteFunction) right).getOperand();
            return cf.decompose(cf.getFormulaFactory().or(cf.gt(a1, left), cf.lt(a1, cf.minus(left))));
        }
        return decomposeLeZero(cf.add(cf.sub(left, right), cf.one()), cf);
    }

    private Decomposition decomposeGe(final CspFactory cf) {
        // abs(a1) >= x2
        if (left instanceof AbsoluteFunction) {
            final Term a1 = ((AbsoluteFunction) left).getOperand();
            return cf.decompose(cf.getFormulaFactory().or(
                    cf.ge(a1, right),
                    cf.le(a1, cf.minus(right))
            ));
        }
        // abs(a1) <= x1
        if (right instanceof AbsoluteFunction) {
            final Term a1 = ((AbsoluteFunction) right).getOperand();
            return cf.decompose(cf.getFormulaFactory().and(cf.le(a1, left), cf.ge(a1, cf.minus(left))));
        }
        return decomposeLeZero(cf.sub(right, left), cf);
    }

    private Decomposition decomposeGt(final CspFactory cf) {
        // abs(a1) > x2
        if (left instanceof AbsoluteFunction) {
            final Term a1 = ((AbsoluteFunction) left).getOperand();
            return cf.decompose(cf.getFormulaFactory().and(cf.gt(a1, right), cf.lt(a1, cf.minus(right))));
        }
        // abs(a1) < x1
        if (right instanceof AbsoluteFunction) {
            final Term a1 = ((AbsoluteFunction) right).getOperand();
            return cf.decompose(cf.getFormulaFactory().or(cf.lt(a1, left), cf.gt(a1, cf.minus(left))
            ));
        }
        return decomposeLeZero(cf.add(cf.sub(right, left), cf.one()), cf);
    }

    private Decomposition decomposeEqZero(final Term term, final CspFactory cf) {
        // a*b = 0 implies a = 0 or b = 0
        if (term instanceof MultiplicationFunction) {
            final MultiplicationFunction mul = (MultiplicationFunction) term;
            final Decomposition leftIsZero = decomposeEqZero(mul.getLeft(), cf);
            final Decomposition rightIsZero = decomposeEqZero(mul.getRight(), cf);

            return IntegerClause.factorize(leftIsZero, rightIsZero);
        }

        final Term.Decomposition termDecomposition = term.decompose(cf);
        if (!termDecomposition.getLinearExpression().getDomain().contains(0)) {
            return Decomposition.emptyClause(); // false
        }
        final Set<IntegerClause> result = new LinkedHashSet<>(termDecomposition.getAdditionalConstraints());
        result.add(new IntegerClause(
                new LinearLiteral(termDecomposition.getLinearExpression(), LinearLiteral.Operator.EQ)));
        return new Decomposition(result, termDecomposition.getAuxiliaryIntegerVariables(),
                termDecomposition.getAuxiliaryBooleanVariables());
    }

    private Decomposition decomposeNeZero(final Term term, final CspFactory cf) {
        // a*b != 0 implies a != 0 and b != 0
        if (term instanceof MultiplicationFunction) {
            final MultiplicationFunction mul = (MultiplicationFunction) term;
            final Decomposition d1 = decomposeNeZero(mul.getLeft(), cf);
            final Decomposition d2 = decomposeNeZero(mul.getRight(), cf);
            return Decomposition.merge(d1, d2);
        }

        final Term.Decomposition termDecomposition = term.decompose(cf);
        if (!termDecomposition.getLinearExpression().getDomain().contains(0)) {
            return Decomposition.empty(); // true
        }
        final Set<IntegerClause> result = new LinkedHashSet<>(termDecomposition.getAdditionalConstraints());
        result.add(new IntegerClause(
                new LinearLiteral(termDecomposition.getLinearExpression(), LinearLiteral.Operator.NE)));
        return new Decomposition(result, termDecomposition.getAuxiliaryIntegerVariables(),
                termDecomposition.getAuxiliaryBooleanVariables());
    }

    private Decomposition decomposeLeZero(final Term term, final CspFactory cf) {
        // a1*a2 <= 0
        // <=> (a1 <= 0 & a2 >= 0) | (a1 >= 0 & a2 <= 0)
        // <=> (a1 <= 0 | a2 <= 0) & (a2 >= 0 | a1 >= 0)
        if (term instanceof MultiplicationFunction) {
            final Term a1 = ((MultiplicationFunction) term).getLeft();
            final Term a2 = ((MultiplicationFunction) term).getRight();
            final Decomposition d1 = IntegerClause.factorize(decomposeLeZero(a1, cf), decomposeLeZero(a2, cf));
            final Decomposition d2 = IntegerClause.factorize(decomposeGeZero(a1, cf), decomposeGeZero(a2, cf));
            return Decomposition.merge(d1, d2);
        }
        final Term.Decomposition termDecomposition = term.decompose(cf);
        final IntegerDomain domain = termDecomposition.getLinearExpression().getDomain();

        if (domain.ub() <= 0) {
            return Decomposition.empty(); // true
        }
        if (domain.lb() > 0) {
            return Decomposition.emptyClause(); //false
        }
        final Set<IntegerClause> result = new LinkedHashSet<>(termDecomposition.getAdditionalConstraints());
        result.add(new IntegerClause(
                new LinearLiteral(termDecomposition.getLinearExpression(), LinearLiteral.Operator.LE)));
        return new Decomposition(result, termDecomposition.getAuxiliaryIntegerVariables(),
                termDecomposition.getAuxiliaryBooleanVariables());
    }

    private Decomposition decomposeGeZero(final Term term, final CspFactory cf) {
        // a1*a2 >= 0
        // <=> (a1 <= 0 & a2 <= 0) | (a1 >= 0 & a2 >= 0)
        // <=> (a1 <= 0 | a2 >= 0) & (a2 <= 0 | a1 >= 0)
        if (term instanceof MultiplicationFunction) {
            final Term a1 = ((MultiplicationFunction) term).getLeft();
            final Term a2 = ((MultiplicationFunction) term).getRight();
            final Decomposition d1 = IntegerClause.factorize(decomposeLeZero(a1, cf), decomposeGeZero(a2, cf));
            final Decomposition d2 = IntegerClause.factorize(decomposeGeZero(a1, cf), decomposeLeZero(a2, cf));
            return Decomposition.merge(d1, d2);
        }
        final Term.Decomposition termDecomposition = term.decompose(cf);
        final IntegerDomain domain = termDecomposition.getLinearExpression().getDomain();
        if (domain.lb() >= 0) {
            return Decomposition.empty(); // true
        }
        if (domain.ub() < 0) {
            return Decomposition.emptyClause(); // false
        }
        final Set<IntegerClause> result = new LinkedHashSet<>(termDecomposition.getAdditionalConstraints());
        result.add(new IntegerClause(
                new LinearLiteral(LinearExpression.multiply(termDecomposition.getLinearExpression(), -1),
                        LinearLiteral.Operator.LE)));
        return new Decomposition(result, termDecomposition.getAuxiliaryIntegerVariables(),
                termDecomposition.getAuxiliaryBooleanVariables());
    }
}
