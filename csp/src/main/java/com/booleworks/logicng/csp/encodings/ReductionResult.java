package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A class collecting data produced by a csp reduction.
 * @version 3.0.0
 * @since 3.0.0
 */
final class ReductionResult {
    private final Set<IntegerClause> clauses;
    private final List<IntegerVariable> frontierAuxiliaryVariables;

    /**
     * Constructs a new reduction result.
     * @param clauses                    the arithmetic clauses
     * @param frontierAuxiliaryVariables the frontier variables are the
     *                                   variables in the clauses relevant for
     *                                   the encoder. This might not include all
     *                                   original variables, if they are
     *                                   substituted by auxiliary variables.
     *                                   Then the auxiliary variable should be
     *                                   encoded and not the original.
     */
    ReductionResult(final Set<IntegerClause> clauses, final List<IntegerVariable> frontierAuxiliaryVariables) {
        this.clauses = clauses;
        this.frontierAuxiliaryVariables = frontierAuxiliaryVariables;
    }

    /**
     * Returns the set of arithmetic clauses.
     * @return the set of arithmetic clauses
     */
    public Set<IntegerClause> getClauses() {
        return clauses;
    }

    /**
     * Returns the frontier variables.
     * @return the frontier variables
     */
    public List<IntegerVariable> getFrontierAuxiliaryVariables() {
        return frontierAuxiliaryVariables;
    }

    public static ReductionResult merge(final Collection<ReductionResult> results) {
        final ReductionResult merged = new ReductionResult(new LinkedHashSet<>(), new ArrayList<>());
        for (final ReductionResult r : results) {
            merged.getClauses().addAll(r.getClauses());
            merged.getFrontierAuxiliaryVariables().addAll(r.getFrontierAuxiliaryVariables());
        }
        return merged;
    }
}
