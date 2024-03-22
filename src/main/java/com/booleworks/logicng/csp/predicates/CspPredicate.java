package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.IntegerClause;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Predicate;

import java.util.Set;

public abstract class CspPredicate implements Predicate {
    protected final CspFactory cspFactory;
    protected final Type type;
    protected Set<IntegerClause> decomposition;

    protected CspPredicate(final CspFactory cspFactory, final Type type) {
        this.cspFactory = cspFactory;
        this.type = type;
    }

    public Type getType() {
        return this.type;
    }

    public abstract CspPredicate negate();

    protected abstract Set<IntegerClause> calculateDecomposition();

    public Set<IntegerClause> decompose() {
        if (this.decomposition == null) {
            this.decomposition = calculateDecomposition();
        }
        return this.decomposition;
    }

    @Override
    public FormulaFactory factory() {
        return this.cspFactory.getFormulaFactory();
    }

    public enum Type {
        EQ, NE, LE, LT, GE, GT, ALLDIFFERENT
    }
}
