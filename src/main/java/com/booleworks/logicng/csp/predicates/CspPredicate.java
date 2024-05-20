package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.IntegerClause;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Predicate;

import java.util.Set;

public abstract class CspPredicate implements Predicate {

    protected final Type type;
    protected Set<IntegerClause> decomposition;

    protected CspPredicate(final Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public abstract CspPredicate negate(final CspFactory cf);

    protected abstract Set<IntegerClause> calculateDecomposition(final CspFactory cf);

    public Set<IntegerClause> decompose(final CspFactory cf) {
        if (decomposition == null) {
            decomposition = calculateDecomposition(cf);
        }
        return decomposition;
    }

    @Override
    public FormulaFactory factory() {
        return null;
    }

    public enum Type {
        EQ, NE, LE, LT, GE, GT, ALLDIFFERENT
    }
}
