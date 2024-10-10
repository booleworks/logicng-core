package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * A predicate that evaluates whether all operands have different values.
 */
public class AllDifferentPredicate extends CspPredicate {

    List<Term> terms;

    /**
     * Constructs a new all-different predicate.
     * <p>
     * <B>This constructor should not be used!</B> Use {@link CspFactory} to create new predicates.
     * @param terms operands which are tested to be different
     * @param f     the formula factory
     */
    public AllDifferentPredicate(final Collection<Term> terms, final FormulaFactory f) {
        super(CspPredicate.Type.ALLDIFFERENT, f);
        this.terms = new ArrayList<>(terms);
    }

    @Override
    public Formula negate(final CspFactory cf) {
        final List<CspPredicate> eqs = new ArrayList<>();
        for (int i = 0; i < terms.size(); i++) {
            for (int j = i + 1; j < terms.size(); j++) {
                eqs.add(cf.eq(terms.get(i), terms.get(j)));
            }
        }
        return cf.getFormulaFactory().or(eqs);
    }

    @Override
    public void variablesInplace(final SortedSet<IntegerVariable> variables) {
        for (final Term term : terms) {
            term.variablesInplace(variables);
        }
    }

    @Override
    protected Decomposition calculateDecomposition(final CspFactory cf) {
        final FormulaFactory f = getFactory();
        final List<Decomposition> decomps = new ArrayList<>();
        for (int i = 0; i < terms.size(); i++) {
            for (int j = i + 1; j < terms.size(); j++) {
                decomps.add(cf.ne(terms.get(i), terms.get(j)).decompose(cf));
            }
        }
        int lb = Integer.MAX_VALUE;
        int ub = Integer.MIN_VALUE;
        for (final Term term : terms) {
            final Term.Decomposition decompositionResult = term.decompose(cf);
            final IntegerDomain d = decompositionResult.getLinearExpression().getDomain();
            lb = Math.min(lb, d.lb());
            ub = Math.max(ub, d.ub());
        }
        final Set<CspPredicate> xs1 = new LinkedHashSet<>();
        final Set<CspPredicate> xs2 = new LinkedHashSet<>();
        for (int i = 0; i < terms.size(); i++) {
            xs1.add(cf.ge(terms.get(i), cf.constant(lb + terms.size() - 1)));
            xs2.add(cf.le(terms.get(i), cf.constant(ub - terms.size() + 1)));
        }
        decomps.add(cf.decompose(f.or(xs1)));
        decomps.add(cf.decompose(f.or(xs2)));
        return Decomposition.merge(decomps);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (getClass() == other.getClass()) {
            if (getFactory() == ((AllDifferentPredicate) other).getFactory()) {
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
