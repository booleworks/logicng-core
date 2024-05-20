package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.IntegerClause;
import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.formulas.FormulaFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class AllDifferentPredicate extends CspPredicate {

    List<Term> terms;

    public AllDifferentPredicate(final Collection<Term> terms) {
        super(CspPredicate.Type.ALLDIFFERENT);
        this.terms = new ArrayList<>(terms);
    }

    @Override
    public CspPredicate negate(final CspFactory cf) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    protected Set<IntegerClause> calculateDecomposition(final CspFactory cf) {
        final FormulaFactory f = factory();
        final Set<IntegerClause> clauses = new TreeSet<>();
        for (int i = 0; i < terms.size(); i++) {
            for (int j = i + 1; j < terms.size(); j++) {
                clauses.addAll(cf.ne(terms.get(i), terms.get(j)).decompose(cf));
            }
        }
        int lb = Integer.MAX_VALUE;
        int ub = Integer.MIN_VALUE;
        for (final Term term : terms) {
            final Term.Decomposition decompositionResult = term.decompose();
            final IntegerDomain d = decompositionResult.getLinearExpression().getDomain();
            lb = Math.min(lb, d.lb());
            ub = Math.max(ub, d.ub());
        }
        Set<IntegerClause> xs1 = new TreeSet<>();
        Set<IntegerClause> xs2 = new TreeSet<>();
        boolean first = true;
        for (int i = 0; i < terms.size(); i++) {
            final Set<IntegerClause> new1 = cf.lt(terms.get(i), cf.constant(lb + terms.size() - 1)).negate(cf).decompose(cf);
            final Set<IntegerClause> new2 = cf.gt(terms.get(i), cf.constant(ub - terms.size() + 1)).negate(cf).decompose(cf);
            if (first) {
                xs1 = new1;
                xs2 = new2;
                first = false;
            } else {
                xs1 = IntegerClause.factorize(xs1, new1);
                xs2 = IntegerClause.factorize(xs2, new2);
            }
        }
        clauses.addAll(xs1);
        clauses.addAll(xs2);
        return clauses;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (getClass() == other.getClass()) {
            if (factory() == ((AllDifferentPredicate) other).factory()) {
                return false; // the same factory would have produced a == object
            }
            return Objects.equals(terms, ((AllDifferentPredicate) other).terms);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(terms);
    }

    @Override
    public String toString() {
        return type + "(" + terms.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
    }
}
