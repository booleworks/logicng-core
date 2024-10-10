package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Not;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class grouping functions related to decomposing formula into CSP problems.
 */
public class CspDecomposition {
    private CspDecomposition() {
    }

    /**
     * Decompose a formula into arithmetic clauses.
     * @param formula the formula
     * @param cf      the factory
     * @return the decomposition result
     */
    public static CspPredicate.Decomposition decompose(final Formula formula, final CspFactory cf) {
        final Formula nnf = formula.nnf(cf.getFormulaFactory());
        final Set<CspPredicate.Decomposition> decompositions = new LinkedHashSet<>();
        decomposeRecursive(nnf, cf, decompositions);
        return CspPredicate.Decomposition.merge(decompositions);
    }

    private static void decomposeRecursive(final Formula formula, final CspFactory cf,
                                           final Set<CspPredicate.Decomposition> decompositions) {
        switch (formula.getType()) {
            case AND:
                for (final Formula op : formula) {
                    decomposeRecursive(op, cf, decompositions);
                }
                break;
            case OR:
                CspPredicate.Decomposition factorized = null;
                for (final Formula op : formula) {
                    final Set<CspPredicate.Decomposition> disj = new LinkedHashSet<>();
                    decomposeRecursive(op, cf, disj);
                    final CspPredicate.Decomposition disjMerged = CspPredicate.Decomposition.merge(disj);
                    if (factorized == null) {
                        factorized = disjMerged;
                    } else {
                        factorized = IntegerClause.factorize(factorized, disjMerged);
                    }
                    if (factorized.getClauses().isEmpty()) {
                        break;
                    }
                }
                if (factorized != null) {
                    decompositions.add(factorized);
                }
                break;
            case LITERAL:
                decompositions.add(
                        new CspPredicate.Decomposition(Collections.singleton(new IntegerClause((Literal) formula)),
                                Collections.emptySet(), Collections.emptySet()));
                break;
            case PREDICATE:
                if (formula instanceof CspPredicate) {
                    decompositions.add(((CspPredicate) formula).decompose(cf));
                } else {
                    throw new RuntimeException("Cannot decompose predicates of type: " + formula.getClass());
                }
                break;
            case NOT:
                final Not not = (Not) formula;
                assert (not.getOperand().getType() == FType.PREDICATE);
                if (not.getOperand() instanceof CspPredicate) {
                    decomposeRecursive(((CspPredicate) not.getOperand()).negate(cf).nnf(cf.getFormulaFactory()), cf,
                            decompositions);
                } else {
                    throw new RuntimeException("Cannot decompose predicates of type: " + not.getOperand().getClass());
                }
                break;
            default:
                throw new RuntimeException("Cannot decompose formula of type: " + formula.getClass());
        }
    }
}
