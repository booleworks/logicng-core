package com.booleworks.logicng.csp.datastructures;

import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A data structure storing all information relevant for a CSP problem.
 */
public class Csp {
    private Set<IntegerVariable> visibleIntegerVariables;
    private Set<IntegerVariable> internalIntegerVariables;
    private Set<Variable> visibleBooleanVariables;
    private Set<Variable> internalBooleanVariables;
    private IntegerVariableSubstitution propagateSubstitutions;
    private Set<IntegerClause> clauses;

    private Csp() {
        this.internalIntegerVariables = new LinkedHashSet<>();
        this.visibleIntegerVariables = new LinkedHashSet<>();
        this.internalBooleanVariables = new LinkedHashSet<>();
        this.visibleBooleanVariables = new LinkedHashSet<>();
        this.clauses = new LinkedHashSet<>();
        this.propagateSubstitutions = new IntegerVariableSubstitution();
    }

    private Csp(final Csp other) {
        this.internalIntegerVariables = new LinkedHashSet<>(other.internalIntegerVariables);
        this.visibleIntegerVariables = new LinkedHashSet<>(other.visibleIntegerVariables);
        this.internalBooleanVariables = new LinkedHashSet<>(other.internalBooleanVariables);
        this.visibleBooleanVariables = new LinkedHashSet<>(other.visibleBooleanVariables);
        this.clauses = new LinkedHashSet<>(other.clauses);
        this.propagateSubstitutions = new IntegerVariableSubstitution(other.propagateSubstitutions);
    }

    /**
     * Returns all visible integer variables of this problem.
     * <p>
     * Visible (or original) variables are the variables that were originally in the input passed by the user. After
     * performing optimizations or reduction/encoding steps some original variables might be substituted by auxiliary
     * variables or are completely removed. So we need to keep track of them, so that we still know which variables
     * were important in the beginning.
     * @return all visible integer variables of this problem.
     */
    public Set<IntegerVariable> getVisibleIntegerVariables() {
        return visibleIntegerVariables;
    }

    /**
     * Returns all visible boolean variables of this problem.
     * <p>
     * Visible (or original) variables are the variables that were originally in the input passed by the user. After
     * performing optimizations or reduction/encoding steps some original variables might be substituted by auxiliary
     * variables or are completely removed. So we need to keep track of them, so that we still know which variables
     * were important in the beginning.
     * @return all visible boolean variables of this problem.
     */
    public Set<Variable> getVisibleBooleanVariables() {
        return visibleBooleanVariables;
    }

    /**
     * Returns all internal integer variables of this problem.
     * <p>
     * After performing optimizations or reduction/encoding steps some original variables might be substituted by
     * auxiliary variables or are completely removed. Further processing steps only care for the variables that are
     * currently relevant in the problem: All original variables that were not replaced or removed, the auxiliary
     * variables that some original variables were replace with, and all other auxiliary variables.
     * @return all internal integer variables of this problem.
     */
    public Set<IntegerVariable> getInternalIntegerVariables() {
        return internalIntegerVariables;
    }

    /**
     * Returns all internal boolean variables of this problem.
     * <p>
     * After performing optimizations or reduction/encoding steps some original variables might be substituted by
     * auxiliary variables or are completely removed. Further processing steps only care for the variables that are
     * currently relevant in the problem: All original variables that were not replaced or removed, the auxiliary
     * variables that some original variables were replace with, and all other auxiliary variables.
     * @return all internal boolean variables of this problem.
     */
    public Set<Variable> getInternalBooleanVariables() {
        return internalBooleanVariables;
    }

    /**
     * Returns all arithmetic clauses of this problem.
     * @return all arithmetic clauses of this problem
     */
    public Set<IntegerClause> getClauses() {
        return clauses;
    }

    /**
     * Returns substitutions performed by the propagation optimization.
     * @return substitutions performed by the propagation optimization
     */
    public IntegerVariableSubstitution getPropagateSubstitutions() {
        return propagateSubstitutions;
    }

    @Override
    public String toString() {
        return "Csp{" +
                "visibleIntegerVariables=" + visibleIntegerVariables +
                ", internIntegerVariables=" + internalIntegerVariables +
                ", visibleBooleanVariables=" + visibleBooleanVariables +
                ", internBooleanVariables=" + internalBooleanVariables +
                ", propagateSubstitutions=" + propagateSubstitutions +
                ", clauses=" + clauses +
                '}';
    }

    /**
     * Build CSP problem from set of clauses and visible integer variables.
     * <p>
     * Internal variables are extracted from the clauses.
     * @param clauses          the clauses
     * @param integerVariables the visible integer variables.
     * @return CSP problem from set of clauses and visible integer variables
     */
    public static Csp fromClauses(final Set<IntegerClause> clauses, final Set<IntegerVariable> integerVariables) {
        return fromClauses(clauses, integerVariables, Collections.emptySet(), new IntegerVariableSubstitution());
    }

    /**
     * Build CSP problem from set of clauses and visible integer and boolean variables.
     * <p>
     * Internal variables are extracted from the clauses.
     * @param clauses          the clauses
     * @param integerVariables the visible integer variables
     * @param booleanVariables the visible boolean variables
     * @return CSP problem from the set of clauses and visible variables
     */
    public static Csp fromClauses(final Set<IntegerClause> clauses, final Set<IntegerVariable> integerVariables,
                                  final Set<Variable> booleanVariables) {
        return fromClauses(clauses, integerVariables, booleanVariables, new IntegerVariableSubstitution());
    }

    /**
     * Build CSP problem from set of clauses and visible variables and additional substitutions.
     * <p>
     * Internal variables are extracted from the clauses.
     * @param clauses                the clauses
     * @param integerVariables       the visible integer variables
     * @param booleanVariables       the visible boolean variables
     * @param propagateSubstitutions additional substitutions
     * @return CSP problem from the set of clauses
     */
    public static Csp fromClauses(final Set<IntegerClause> clauses, final Set<IntegerVariable> integerVariables,
                                  final Set<Variable> booleanVariables,
                                  final IntegerVariableSubstitution propagateSubstitutions) {
        final Set<IntegerVariable> intVars = new LinkedHashSet<>();
        final Set<Variable> boolVars = new LinkedHashSet<>();
        for (final IntegerClause clause : clauses) {
            intVars.addAll(clause.getArithmeticLiterals().stream().flatMap(v -> v.getVariables().stream())
                    .collect(Collectors.toSet()));
            boolVars.addAll(clause.getBoolLiterals().stream().map(Literal::variable).collect(Collectors.toSet()));
        }
        return new Csp.Builder()
                .updateInternalIntegerVariables(intVars)
                .updateInternalBooleanVariables(boolVars)
                .updatePropagateSubstitutions(propagateSubstitutions)
                .updateClauses(clauses)
                .updateVisibleIntegerVariables(integerVariables)
                .updateVisibleBooleanVariables(booleanVariables)
                .build();
    }

    /**
     * A builder for incrementally building CSP problems.
     */
    public static class Builder {
        private Csp csp;

        /**
         * Constructs a new CSP builder.
         */
        public Builder() {
            csp = new Csp();
        }

        /**
         * Constructs a new CSP builder by coping a CSP problem.
         * @param csp the CSP problem
         */
        public Builder(final Csp csp) {
            this.csp = new Csp(csp);
        }

        /**
         * Adds a clause.
         * @param clause the clause
         * @return this builder
         */
        public Builder addClause(final IntegerClause clause) {
            this.csp.clauses.add(clause);
            return this;
        }

        /**
         * Replaces all clauses with new clauses.
         * @param clauses the new clauses
         * @return this builder
         */
        public Builder updateClauses(final Set<IntegerClause> clauses) {
            csp.clauses = clauses;
            return this;
        }

        /**
         * Replaces all internal integer variables.
         * @param variables the new variables
         * @return this builder
         */
        public Builder updateInternalIntegerVariables(final Set<IntegerVariable> variables) {
            csp.internalIntegerVariables = variables;
            return this;
        }

        /**
         * Replaces all visible integer variables.
         * @param variables the new variables
         * @return this builder
         */
        public Builder updateVisibleIntegerVariables(final Set<IntegerVariable> variables) {
            csp.visibleIntegerVariables = variables;
            return this;
        }

        /**
         * Replaces all internal boolean variables.
         * @param variables the new variables
         * @return this builder
         */
        public Builder updateInternalBooleanVariables(final Set<Variable> variables) {
            csp.internalBooleanVariables = variables;
            return this;
        }

        /**
         * Replaces all visible boolean variables.
         * @param variables the new variables
         * @return this builder
         */
        public Builder updateVisibleBooleanVariables(final Set<Variable> variables) {
            csp.visibleBooleanVariables = variables;
            return this;
        }

        /**
         * Replaces the current substitutions.
         * @param substitution the new substitutions
         * @return this builder
         */
        public Builder updatePropagateSubstitutions(final IntegerVariableSubstitution substitution) {
            csp.propagateSubstitutions = substitution;
            return this;
        }

        /**
         * Adds an internal integer variable.
         * @param v the variable
         * @return this builder
         */
        public boolean addInternalIntegerVariable(final IntegerVariable v) {
            return csp.internalIntegerVariables.add(v);
        }

        /**
         * Adds a visible integer variable.
         * @param v the variable
         * @return this builder
         */
        public boolean addVisibleIntegerVariable(final IntegerVariable v) {
            return csp.visibleIntegerVariables.add(v);
        }

        /**
         * Adds an internal boolean variable.
         * @param v the variable
         * @return this builder
         */
        public boolean addInternalBooleanVariable(final Variable v) {
            return csp.internalBooleanVariables.add(v);
        }

        /**
         * Adds a visible boolean variable.
         * @param v the variable
         * @return this builder
         */
        public boolean addVisibleBooleanVariable(final Variable v) {
            return csp.visibleBooleanVariables.add(v);
        }

        /**
         * Builds the CSP problem from this builder.
         * <p>
         * <B>This call invalidates this builder!</B>
         * @return the built CSP problem
         */
        public Csp build() {
            final Csp csp = this.csp;
            this.csp = null;
            return csp;
        }

        /**
         * Returns all internal integer variables of this problem.
         * <p>
         * After performing optimizations or reduction/encoding steps some original variables might be substituted by
         * auxiliary variables or are completely removed. Further processing steps only care for the variables that are
         * currently relevant in the problem: All original variables that were not replaced or removed, the auxiliary
         * variables that some original variables were replace with, and all other auxiliary variables.
         * @return all internal integer variables of this problem.
         */
        public Set<IntegerVariable> getInternalIntegerVariables() {
            return csp.internalIntegerVariables;
        }

        /**
         * Returns all visible integer variables of this problem.
         * <p>
         * Visible (or original) variables are the variables that were originally in the input passed by the user.
         * After performing optimizations or reduction/encoding steps some original variables might be substituted by
         * auxiliary variables or are completely removed. So we need to keep track of them, so that we still know
         * which variables were important in the beginning.
         * @return all visible integer variables of this problem.
         */
        public Set<IntegerVariable> getVisibleIntegerVariables() {
            return csp.visibleIntegerVariables;
        }

        /**
         * Returns all internal boolean variables of this problem.
         * <p>
         * After performing optimizations or reduction/encoding steps some original variables might be substituted by
         * auxiliary variables or are completely removed. Further processing steps only care for the variables that are
         * currently relevant in the problem: All original variables that were not replaced or removed, the auxiliary
         * variables that some original variables were replace with, and all other auxiliary variables.
         * @return all internal boolean variables of this problem.
         */
        public Set<Variable> getInternalBooleanVariables() {
            return csp.internalBooleanVariables;
        }

        /**
         * Returns all visible boolean variables of this problem.
         * <p>
         * Visible (or original) variables are the variables that were originally in the input passed by the user. After
         * performing optimizations or reduction/encoding steps some original variables might be substituted by
         * auxiliary variables or are completely removed. So we need to keep track of them, so that we still know
         * which variables were important in the beginning.
         * @return all visible boolean variables of this problem.
         */
        public Set<Variable> getVisibleBooleanVariables() {
            return csp.visibleBooleanVariables;
        }

        /**
         * Returns all clauses of this problem.
         * @return all clauses if this problem
         */
        public Set<IntegerClause> getClauses() {
            return csp.clauses;
        }

        @Override
        public String toString() {
            return csp.toString();
        }
    }
}
