package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.terms.MultiplicationFunction;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.formulas.Formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    protected Formula calculateDecomposition() {
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

    private Formula decomposeEq() {
        MultiplicationFunction mul = null;
        // a*b = 0 implies a = 0 or b = 0
        if (this.left instanceof MultiplicationFunction && this.right.getType() == Term.Type.ZERO) {
            mul = (MultiplicationFunction) this.left;
        }
        if (this.right instanceof MultiplicationFunction && this.left.getType() == Term.Type.ZERO) {
            mul = (MultiplicationFunction) this.right;
        }
        if (mul != null) {
            final Formula leftIsZero = this.cspFactory.eq(mul.getLeft(), this.cspFactory.zero()).decompose();
            final Formula rightIsZero = this.cspFactory.eq(mul.getRight(), this.cspFactory.zero()).decompose();
            return this.cspFactory.getFormulaFactory().or(leftIsZero, rightIsZero);
        }

        //return this.cspFactory.decomposeFormulas(decomposeEqZero(this.cspFactory.sub(this.left, this.right)));
        return this.factory().and(decomposeEqZero(this.cspFactory.sub(this.left, this.right)));
    }

    private Formula decomposeNe() {
        MultiplicationFunction mul = null;
        // a*b != 0 implies a != 0 and b != 0
        if (this.left instanceof MultiplicationFunction && this.right.getType() == Term.Type.ZERO) {
            mul = (MultiplicationFunction) this.left;
        }
        if (this.right instanceof MultiplicationFunction && this.left.getType() == Term.Type.ZERO) {
            mul = (MultiplicationFunction) this.right;
        }
        if (mul != null) {
            final Formula leftIsNotZero = this.cspFactory.decomposeFormula(this.cspFactory.ne(mul.getLeft(), this.cspFactory.zero()));
            final Formula rightIsNotZero = this.cspFactory.decomposeFormula(this.cspFactory.ne(mul.getRight(), this.cspFactory.zero()));
            return this.cspFactory.getFormulaFactory().and(leftIsNotZero, rightIsNotZero);
        }
        return this.factory().and(decomposeNeZero(this.cspFactory.sub(this.left, this.right)));
    }

    private Formula decomposeLe() {
        // a1*a2 <= 0
        if (this.left instanceof MultiplicationFunction && this.right.getType() == Term.Type.ZERO) {
            final Term a1 = ((MultiplicationFunction) this.left).getLeft();
            final Term a2 = ((MultiplicationFunction) this.left).getRight();
            return leZeroFormula(a1, a2);
        }
        // a1*a2 >= 0
        if (this.right instanceof MultiplicationFunction && this.left.getType() == Term.Type.ZERO) {
            final Term a1 = ((MultiplicationFunction) this.right).getLeft();
            final Term a2 = ((MultiplicationFunction) this.right).getRight();
            return geZeroFormula(a1, a2);
        }
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
        return this.factory().and(decomposeLeZero(this.cspFactory.sub(this.left, this.right)));
    }

    private Formula decomposeLt() {
        // a1*a2 < 0
        if (this.left instanceof MultiplicationFunction && this.right.getType() == Term.Type.ZERO) {
            final Term a1 = ((MultiplicationFunction) this.left).getLeft();
            final Term a2 = ((MultiplicationFunction) this.left).getRight();
            return ltZeroFormula(a1, a2);
        }
        // a1*a2 > 0
        if (this.right instanceof MultiplicationFunction && this.left.getType() == Term.Type.ZERO) {
            final Term a1 = ((MultiplicationFunction) this.right).getLeft();
            final Term a2 = ((MultiplicationFunction) this.right).getRight();
            return gtZeroFormula(a1, a2);
        }
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
        return this.factory().and(decomposeLeZero(this.cspFactory.add(this.cspFactory.sub(this.left, this.right), this.cspFactory.one())));
    }

    private Formula decomposeGe() {
        // a1*a2 >= 0
        if (this.left instanceof MultiplicationFunction && this.right.getType() == Term.Type.ZERO) {
            final Term a1 = ((MultiplicationFunction) this.left).getLeft();
            final Term a2 = ((MultiplicationFunction) this.left).getRight();
            return geZeroFormula(a1, a2);
        }
        // a1*a2 <= 0
        if (this.right instanceof MultiplicationFunction && this.left.getType() == Term.Type.ZERO) {
            final Term a1 = ((MultiplicationFunction) this.right).getLeft();
            final Term a2 = ((MultiplicationFunction) this.right).getRight();
            return leZeroFormula(a1, a2);
        }
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
        return this.factory().and(decomposeLeZero(this.cspFactory.sub(this.right, this.left)));
    }

    private Formula decomposeGt() {
        // a1*a2 > 0
        if (this.left instanceof MultiplicationFunction && this.right.getType() == Term.Type.ZERO) {
            final Term a1 = ((MultiplicationFunction) this.left).getLeft();
            final Term a2 = ((MultiplicationFunction) this.left).getRight();
            return gtZeroFormula(a1, a2);
        }
        // a1*a2 < 0
        if (this.right instanceof MultiplicationFunction && this.left.getType() == Term.Type.ZERO) {
            final Term a1 = ((MultiplicationFunction) this.right).getLeft();
            final Term a2 = ((MultiplicationFunction) this.right).getRight();
            return ltZeroFormula(a1, a2);
        }
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
        return this.factory().and(decomposeLeZero(this.cspFactory.add(this.cspFactory.sub(this.right, this.left), this.cspFactory.one())));
    }

    private Formula leZeroFormula(final Term a1, final Term a2) {
        return this.factory().or(
                this.factory().and(
                        this.cspFactory.decomposeFormula(this.cspFactory.lt(a1, this.cspFactory.zero())),
                        this.cspFactory.decomposeFormula(this.cspFactory.gt(a2, this.cspFactory.zero()))
                ),
                this.factory().and(
                        this.cspFactory.decomposeFormula(this.cspFactory.gt(a1, this.cspFactory.zero())),
                        this.cspFactory.decomposeFormula(this.cspFactory.lt(a2, this.cspFactory.zero()))
                ),
                this.cspFactory.decomposeFormula(this.cspFactory.eq(a1, this.cspFactory.zero())),
                this.cspFactory.decomposeFormula(this.cspFactory.eq(a2, this.cspFactory.zero()))
        );
    }

    private Formula geZeroFormula(final Term a1, final Term a2) {
        return this.factory().or(
                this.factory().and(
                        this.cspFactory.decomposeFormula(this.cspFactory.lt(a1, this.cspFactory.zero())),
                        this.cspFactory.decomposeFormula(this.cspFactory.lt(a2, this.cspFactory.zero()))
                ),
                this.factory().and(
                        this.cspFactory.decomposeFormula(this.cspFactory.gt(a1, this.cspFactory.zero())),
                        this.cspFactory.decomposeFormula(this.cspFactory.gt(a2, this.cspFactory.zero()))
                ),
                this.cspFactory.decomposeFormula(this.cspFactory.eq(a1, this.cspFactory.zero())),
                this.cspFactory.decomposeFormula(this.cspFactory.eq(a2, this.cspFactory.zero()))
        );
    }

    private Formula ltZeroFormula(final Term a1, final Term a2) {
        return this.factory().or(
                this.factory().and(
                        this.cspFactory.decomposeFormula(this.cspFactory.lt(a1, this.cspFactory.zero())),
                        this.cspFactory.decomposeFormula(this.cspFactory.gt(a2, this.cspFactory.zero()))
                ),
                this.factory().and(
                        this.cspFactory.decomposeFormula(this.cspFactory.gt(a1, this.cspFactory.zero())),
                        this.cspFactory.decomposeFormula(this.cspFactory.lt(a2, this.cspFactory.zero()))
                )
        );
    }

    private Formula gtZeroFormula(final Term a1, final Term a2) {
        return this.factory().or(
                this.factory().and(
                        this.cspFactory.decomposeFormula(this.cspFactory.lt(a1, this.cspFactory.zero())),
                        this.cspFactory.decomposeFormula(this.cspFactory.lt(a2, this.cspFactory.zero()))
                ),
                this.factory().and(
                        this.cspFactory.decomposeFormula(this.cspFactory.gt(a1, this.cspFactory.zero())),
                        this.cspFactory.decomposeFormula(this.cspFactory.gt(a2, this.cspFactory.zero()))
                )
        );
    }

    private List<Formula> decomposeEqZero(final Term term) {
        final Term.Decomposition termDecomposition = term.decompose();
        if (!termDecomposition.getLinearExpression().getDomain().contains(0)) {
            return Collections.singletonList(this.factory().falsum());
        }
        final List<Formula> result = new ArrayList<>(termDecomposition.getAdditionalConstraints());
        final Term comp_term = termDecomposition.getLinearExpression().toTerm(this.cspFactory);
        result.add(this.cspFactory.eq(comp_term, this.cspFactory.zero()));
        return result;
    }

    private List<Formula> decomposeNeZero(final Term term) {
        final Term.Decomposition termDecomposition = term.decompose();
        if (!termDecomposition.getLinearExpression().getDomain().contains(0)) {
            return Collections.singletonList(this.factory().verum());
        }
        final List<Formula> result = new ArrayList<>(termDecomposition.getAdditionalConstraints());
        final Term comp_term = termDecomposition.getLinearExpression().toTerm(this.cspFactory);
        result.add(this.cspFactory.ne(comp_term, this.cspFactory.zero()));
        return result;
    }

    private List<Formula> decomposeLeZero(final Term term) {
        final Term.Decomposition termDecomposition = term.decompose();
        final IntegerDomain domain = termDecomposition.getLinearExpression().getDomain();

        if (domain.ub() <= 0) {
            return Collections.singletonList(this.factory().verum());
        }
        if (domain.lb() > 0) {
            return Collections.singletonList(this.factory().falsum());
        }
        final List<Formula> result = new ArrayList<>(termDecomposition.getAdditionalConstraints());
        final Term comp_term = termDecomposition.getLinearExpression().toTerm(this.cspFactory);
        result.add(this.cspFactory.le(comp_term, this.cspFactory.zero()));
        return result;
    }
}
