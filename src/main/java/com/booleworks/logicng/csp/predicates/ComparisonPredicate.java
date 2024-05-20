package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.IntegerClause;
import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.LinearExpression;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.terms.MultiplicationFunction;
import com.booleworks.logicng.csp.terms.Term;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class ComparisonPredicate extends BinaryPredicate {

    public ComparisonPredicate(final Type type, final Term left, final Term right) {
        super(type, left, right);
    }

    @Override
    public CspPredicate negate(final CspFactory cf) {
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
    protected Set<IntegerClause> calculateDecomposition(final CspFactory cf) {
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

    private Set<IntegerClause> decomposeEq(final CspFactory cf) {
        if (right.getType() == Term.Type.ZERO) {
            return decomposeEqZero(left);
        } else if (left.getType() == Term.Type.ZERO) {
            return decomposeEqZero(right);
        }
        return decomposeEqZero(cf.sub(left, right));
    }

    private Set<IntegerClause> decomposeNe(final CspFactory cf) {
        if (right.getType() == Term.Type.ZERO) {
            return decomposeNeZero(left);
        } else if (left.getType() == Term.Type.ZERO) {
            return decomposeNeZero(right);
        }
        return decomposeNeZero(cf.sub(left, right));
    }

    private Set<IntegerClause> decomposeLe(final CspFactory cf) {
        // abs(a1) <= x2
        //if (left instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) left).getOperand();
        //    return f.and(
        //            cspFactory.decomposeFormula(cspFactory.le(a1, right), false),
        //            cspFactory.decomposeFormula(cspFactory.ge(a1, cspFactory.minus(right)), false)
        //    );
        //}
        // abs(a1) >= x1
        //if (right instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) right).getOperand();
        //    return f.or(
        //            cspFactory.decomposeFormula(cspFactory.ge(a1, left), false),
        //            cspFactory.decomposeFormula(cspFactory.le(a1, cspFactory.minus(left)), false)
        //    );
        //}
        return decomposeLeZero(cf.sub(left, right));
    }

    private Set<IntegerClause> decomposeLt(final CspFactory cf) {
        // abs(a1) < x2
        //if (left instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) left).getOperand();
        //    return f.and(
        //            cspFactory.decomposeFormula(cspFactory.lt(a1, right), false),
        //            cspFactory.decomposeFormula(cspFactory.gt(a1, cspFactory.minus(right)), false)
        //    );
        //}
        // abs(a1) > x1
        //if (right instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) right).getOperand();
        //    return f.or(
        //            cspFactory.decomposeFormula(cspFactory.gt(a1, left), false),
        //            cspFactory.decomposeFormula(cspFactory.lt(a1, cspFactory.minus(left)), false)
        //    );
        //}
        return decomposeLeZero(cf.add(cf.sub(left, right), cf.one()));
    }

    private Set<IntegerClause> decomposeGe(final CspFactory cf) {
        // abs(a1) >= x2
        //if (left instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) left).getOperand();
        //    return f.or(
        //            cspFactory.decomposeFormula(cspFactory.ge(a1, right), false),
        //            cspFactory.decomposeFormula(cspFactory.le(a1, cspFactory.minus(right)), false)
        //    );
        //}
        // abs(a1) <= x1
        //if (right instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) right).getOperand();
        //    return f.and(
        //            cspFactory.decomposeFormula(cspFactory.le(a1, left), false),
        //            cspFactory.decomposeFormula(cspFactory.ge(a1, cspFactory.minus(left)), false)
        //    );
        //}
        return decomposeLeZero(cf.sub(right, left));
    }

    private Set<IntegerClause> decomposeGt(final CspFactory cf) {
        // abs(a1) < x2
        //if (left instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) left).getOperand();
        //    return f.and(
        //            cspFactory.decomposeFormula(cspFactory.lt(a1, right), false),
        //            cspFactory.decomposeFormula(cspFactory.gt(a1, cspFactory.minus(right)), false)
        //    );
        //}
        // abs(a1) > x1
        //if (right instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) right).getOperand();
        //    return f.or(
        //            cspFactory.decomposeFormula(cspFactory.gt(a1, left), false),
        //            cspFactory.decomposeFormula(cspFactory.lt(a1, cspFactory.minus(left)), false)
        //    );
        //}
        return decomposeLeZero(cf.add(cf.sub(right, left), cf.one()));
    }

    private Set<IntegerClause> decomposeEqZero(final Term term) {
        // a*b = 0 implies a = 0 or b = 0
        if (term instanceof MultiplicationFunction) {
            final MultiplicationFunction mul = (MultiplicationFunction) term;
            final Set<IntegerClause> leftIsZero = decomposeEqZero(mul.getLeft());
            final Set<IntegerClause> rightIsZero = decomposeEqZero(mul.getRight());

            return IntegerClause.factorize(leftIsZero, rightIsZero);
        }

        final Term.Decomposition termDecomposition = term.decompose();
        if (!termDecomposition.getLinearExpression().getDomain().contains(0)) {
            return Collections.singleton(new IntegerClause()); // false
        }
        final Set<IntegerClause> result = new TreeSet<>(termDecomposition.getAdditionalConstraints());
        result.add(new IntegerClause(new LinearLiteral(termDecomposition.getLinearExpression(), LinearLiteral.Operator.EQ)));
        return result;
    }

    private Set<IntegerClause> decomposeNeZero(final Term term) {
        // a*b != 0 implies a != 0 and b != 0
        if (term instanceof MultiplicationFunction) {
            final MultiplicationFunction mul = (MultiplicationFunction) term;
            final Set<IntegerClause> clauses = new TreeSet<>();
            clauses.addAll(decomposeNeZero(mul.getLeft()));
            clauses.addAll(decomposeNeZero(mul.getRight()));
            return clauses;
        }

        final Term.Decomposition termDecomposition = term.decompose();
        if (!termDecomposition.getLinearExpression().getDomain().contains(0)) {
            return Collections.emptySet(); // true
        }
        final Set<IntegerClause> result = new TreeSet<>(termDecomposition.getAdditionalConstraints());
        result.add(new IntegerClause(new LinearLiteral(termDecomposition.getLinearExpression(), LinearLiteral.Operator.NE)));
        return result;
    }

    private Set<IntegerClause> decomposeLeZero(final Term term) {
        // a1*a2 <= 0
        // <=> (a1 <= 0 & a2 >= 0) | (a1 >= 0 & a2 <= 0)
        // <=> (a1 <= 0 | a2 <= 0) & (a2 >= 0 | a1 >= 0)
        if (term instanceof MultiplicationFunction) {
            final Term a1 = ((MultiplicationFunction) term).getLeft();
            final Term a2 = ((MultiplicationFunction) term).getRight();
            final Set<IntegerClause> clauses = new TreeSet<>();
            clauses.addAll(IntegerClause.factorize(decomposeLeZero(a1), decomposeLeZero(a2)));
            clauses.addAll(IntegerClause.factorize(decomposeGeZero(a1), decomposeGeZero(a2)));
            return clauses;
        }
        final Term.Decomposition termDecomposition = term.decompose();
        final IntegerDomain domain = termDecomposition.getLinearExpression().getDomain();

        if (domain.ub() <= 0) {
            return Collections.emptySet(); // true
        }
        if (domain.lb() > 0) {
            return Collections.singleton(new IntegerClause()); // false
        }
        final Set<IntegerClause> result = new TreeSet<>(termDecomposition.getAdditionalConstraints());
        result.add(new IntegerClause(new LinearLiteral(termDecomposition.getLinearExpression(), LinearLiteral.Operator.LE)));
        return result;
    }

    private Set<IntegerClause> decomposeGeZero(final Term term) {
        // a1*a2 >= 0
        // <=> (a1 <= 0 & a2 <= 0) | (a1 >= 0 & a2 >= 0)
        // <=> (a1 <= 0 | a2 >= 0) & (a2 <= 0 | a1 >= 0)
        if (term instanceof MultiplicationFunction) {
            final Term a1 = ((MultiplicationFunction) term).getLeft();
            final Term a2 = ((MultiplicationFunction) term).getRight();
            final Set<IntegerClause> clauses = new TreeSet<>();
            clauses.addAll(IntegerClause.factorize(decomposeLeZero(a1), decomposeGeZero(a2)));
            clauses.addAll(IntegerClause.factorize(decomposeGeZero(a1), decomposeLeZero(a2)));
            return clauses;
        }
        final Term.Decomposition termDecomposition = term.decompose();
        final IntegerDomain domain = termDecomposition.getLinearExpression().getDomain();
        if (domain.lb() >= 0) {
            return Collections.emptySet(); // true
        }
        if (domain.ub() < 0) {
            return Collections.singleton(new IntegerClause()); // false
        }
        final Set<IntegerClause> result = new TreeSet<>(termDecomposition.getAdditionalConstraints());
        result.add(new IntegerClause(new LinearLiteral(LinearExpression.multiply(termDecomposition.getLinearExpression(), -1), LinearLiteral.Operator.LE)));
        return result;
    }
}
