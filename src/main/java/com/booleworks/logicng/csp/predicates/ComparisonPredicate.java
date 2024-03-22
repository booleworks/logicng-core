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

    public ComparisonPredicate(final CspFactory f, final Type type, final Term left, final Term right) {
        super(f, type, left, right);
    }

    @Override
    public CspPredicate negate() {
        switch (this.type) {
            case EQ:
                return this.cspFactory.ne(this.left, this.right);
            case NE:
                return this.cspFactory.eq(this.left, this.right);
            case LT:
                return this.cspFactory.ge(this.left, this.right);
            case LE:
                return this.cspFactory.gt(this.left, this.right);
            case GT:
                return this.cspFactory.le(this.left, this.right);
            case GE:
                return this.cspFactory.lt(this.left, this.right);
            default:
                throw new IllegalArgumentException("Invalid type of ComparisonPredicate: " + this.type);
        }
    }

    @Override
    protected Set<IntegerClause> calculateDecomposition() {
        switch (this.type) {
            case EQ:
                return decomposeEq();
            case NE:
                return decomposeNe();
            case LE:
                return decomposeLe();
            case LT:
                return decomposeLt();
            case GE:
                return decomposeGe();
            case GT:
                return decomposeGt();
            default:
                throw new IllegalArgumentException("Unexpected type for decomposing a ComparisonPredicate");
        }
    }

    private Set<IntegerClause> decomposeEq() {
        if (this.right.getType() == Term.Type.ZERO) {
            return decomposeEqZero(this.left);
        } else if (this.left.getType() == Term.Type.ZERO) {
            return decomposeEqZero(this.right);
        }
        return decomposeEqZero(cspFactory.sub(this.left, this.right));
    }

    private Set<IntegerClause> decomposeNe() {
        if (this.right.getType() == Term.Type.ZERO) {
            return decomposeNeZero(this.left);
        } else if (this.left.getType() == Term.Type.ZERO) {
            return decomposeNeZero(this.right);
        }
        return decomposeNeZero(this.cspFactory.sub(this.left, this.right));
    }

    private Set<IntegerClause> decomposeLe() {
        // abs(a1) <= x2
        //if (this.left instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) this.left).getOperand();
        //    return this.f.and(
        //            this.cspFactory.decomposeFormula(this.cspFactory.le(a1, this.right), false),
        //            this.cspFactory.decomposeFormula(this.cspFactory.ge(a1, this.cspFactory.minus(this.right)), false)
        //    );
        //}
        // abs(a1) >= x1
        //if (this.right instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) this.right).getOperand();
        //    return this.f.or(
        //            this.cspFactory.decomposeFormula(this.cspFactory.ge(a1, this.left), false),
        //            this.cspFactory.decomposeFormula(this.cspFactory.le(a1, this.cspFactory.minus(this.left)), false)
        //    );
        //}
        return decomposeLeZero(this.cspFactory.sub(this.left, this.right));
    }

    private Set<IntegerClause> decomposeLt() {
        // abs(a1) < x2
        //if (this.left instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) this.left).getOperand();
        //    return this.f.and(
        //            this.cspFactory.decomposeFormula(this.cspFactory.lt(a1, this.right), false),
        //            this.cspFactory.decomposeFormula(this.cspFactory.gt(a1, this.cspFactory.minus(this.right)), false)
        //    );
        //}
        // abs(a1) > x1
        //if (this.right instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) this.right).getOperand();
        //    return this.f.or(
        //            this.cspFactory.decomposeFormula(this.cspFactory.gt(a1, this.left), false),
        //            this.cspFactory.decomposeFormula(this.cspFactory.lt(a1, this.cspFactory.minus(this.left)), false)
        //    );
        //}
        return decomposeLeZero(this.cspFactory.add(this.cspFactory.sub(this.left, this.right), this.cspFactory.one()));
    }

    private Set<IntegerClause> decomposeGe() {
        // abs(a1) >= x2
        //if (this.left instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) this.left).getOperand();
        //    return this.f.or(
        //            this.cspFactory.decomposeFormula(this.cspFactory.ge(a1, this.right), false),
        //            this.cspFactory.decomposeFormula(this.cspFactory.le(a1, this.cspFactory.minus(this.right)), false)
        //    );
        //}
        // abs(a1) <= x1
        //if (this.right instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) this.right).getOperand();
        //    return this.f.and(
        //            this.cspFactory.decomposeFormula(this.cspFactory.le(a1, this.left), false),
        //            this.cspFactory.decomposeFormula(this.cspFactory.ge(a1, this.cspFactory.minus(this.left)), false)
        //    );
        //}
        return decomposeLeZero(this.cspFactory.sub(this.right, this.left));
    }

    private Set<IntegerClause> decomposeGt() {
        // abs(a1) < x2
        //if (this.left instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) this.left).getOperand();
        //    return this.f.and(
        //            this.cspFactory.decomposeFormula(this.cspFactory.lt(a1, this.right), false),
        //            this.cspFactory.decomposeFormula(this.cspFactory.gt(a1, this.cspFactory.minus(this.right)), false)
        //    );
        //}
        // abs(a1) > x1
        //if (this.right instanceof IntegerAbsoluteFunction) {
        //    final IntegerTerm a1 = ((IntegerAbsoluteFunction) this.right).getOperand();
        //    return this.f.or(
        //            this.cspFactory.decomposeFormula(this.cspFactory.gt(a1, this.left), false),
        //            this.cspFactory.decomposeFormula(this.cspFactory.lt(a1, this.cspFactory.minus(this.left)), false)
        //    );
        //}
        return decomposeLeZero(this.cspFactory.add(this.cspFactory.sub(this.right, this.left), this.cspFactory.one()));
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
