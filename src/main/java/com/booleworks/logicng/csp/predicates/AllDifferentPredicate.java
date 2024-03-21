package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AllDifferentPredicate extends CspPredicate {

    List<Term> terms;

    public AllDifferentPredicate(final CspFactory f, final Collection<Term> terms) {
        super(f, CspPredicate.Type.ALLDIFFERENT);
        this.terms = new ArrayList<>(terms);
    }

    @Override
    public CspPredicate negate() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    protected Formula calculateDecomposition() {
        final FormulaFactory f = this.factory();
        final List<Formula> operands = new ArrayList<>();
        for (int i = 0; i < this.terms.size(); i++) {
            for (int j = i + 1; j < this.terms.size(); j++) {
                final Formula decomposition = this.cspFactory.ne(this.terms.get(i), this.terms.get(j)).decompose();
                operands.add(decomposition);
            }
        }
        int lb = Integer.MAX_VALUE;
        int ub = Integer.MIN_VALUE;
        for (final Term term : this.terms) {
            final Term.Decomposition decompositionResult = term.decompose();
            final IntegerDomain d = decompositionResult.getLinearExpression().getDomain();
            lb = Math.min(lb, d.lb());
            ub = Math.max(ub, d.ub());
        }
        final List<Formula> xs1 = new ArrayList<>(this.terms.size());
        final List<Formula> xs2 = new ArrayList<>(this.terms.size());
        for (int i = 0; i < this.terms.size(); i++) {
            xs1.add(this.cspFactory.lt(this.terms.get(i), this.cspFactory.constant(lb + this.terms.size() - 1)).negate().decompose());
            xs2.add(this.cspFactory.gt(this.terms.get(i), this.cspFactory.constant(ub - this.terms.size() + 1)).negate().decompose());
        }
        operands.add(f.or(xs1));
        operands.add(f.or(xs2));
        return f.and(operands);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (getClass() == other.getClass()) {
            if (this.factory() == ((AllDifferentPredicate) other).factory()) {
                return false; // the same factory would have produced a == object
            }
            return Objects.equals(this.terms, ((AllDifferentPredicate) other).terms);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.terms);
    }

    @Override
    public String toString() {
        return this.type + "(" + this.terms.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
    }
}
