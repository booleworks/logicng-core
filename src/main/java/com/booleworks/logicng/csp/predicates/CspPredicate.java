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
        return type;
    }

    public abstract CspPredicate negate();

    protected abstract Set<IntegerClause> calculateDecomposition();

    public Set<IntegerClause> decompose() {
        if (decomposition == null) {
            decomposition = calculateDecomposition();
        }
        return decomposition;
    }

    @Override
    public FormulaFactory factory() {
        return cspFactory.getFormulaFactory();
    }

    public enum Type {
        EQ, NE, LE, LT, GE, GT, ALLDIFFERENT
    }
}
