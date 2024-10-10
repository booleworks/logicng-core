package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Predicate;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A LogicNG Predicate dedicated to evaluating predicates with arithmetic terms.
 */
public abstract class CspPredicate implements Predicate {

    /**
     * Type of the predicate
     */
    protected final Type type;
    /**
     * Cached decomposition of the predicate
     */
    protected Decomposition decomposition;
    private final FormulaFactory f;

    /**
     * Construct new CSP predicate.
     * @param type type of the predicate
     * @param f    the factory
     */
    protected CspPredicate(final Type type, final FormulaFactory f) {
        this.type = type;
        this.f = f;
    }

    /**
     * Returns the type of the predicate.
     * @return the type of the predicate
     */
    public Type getPredicateType() {
        return type;
    }

    /**
     * Builds a function representing the negated predicate.
     * @param cf the factory
     * @return negated predicate
     */
    public abstract Formula negate(final CspFactory cf);

    /**
     * Returns a set of all variables used in this predicate and its operands.
     * @return variables in the predicate
     */
    public SortedSet<IntegerVariable> variables() {
        final SortedSet<IntegerVariable> variables = new TreeSet<>();
        variablesInplace(variables);
        return variables;
    }

    /**
     * Adds all variables used in this predicate and its operands to {@code variables}.
     * @param variables set to add the variables to
     */
    public abstract void variablesInplace(SortedSet<IntegerVariable> variables);

    /**
     * Calculates the decomposition of this predicate. (without caching)
     * @param cf the factory
     * @return the decomposition result
     */
    protected abstract Decomposition calculateDecomposition(final CspFactory cf);

    /**
     * Decomposes the predicate into arithmetic clauses.
     * The result is cached and reused for further calls.
     * @param cf the factory
     * @return the decomposition result
     */
    public Decomposition decompose(final CspFactory cf) {
        if (decomposition == null) {
            decomposition = calculateDecomposition(cf);
        }
        return decomposition;
    }

    @Override
    public FormulaFactory getFactory() {
        return f;
    }

    /**
     * Types of predicates.
     */
    public enum Type {
        /**
         * Equality {@code =}.
         */
        EQ,
        /**
         * Inequality {@code !=}.
         */
        NE,
        /**
         * Less than or equals {@code <=}.
         */
        LE,
        /**
         * Less than {@code <}.
         */
        LT,
        /**
         * Greater than or equals {@code >=}.
         */
        GE,
        /**
         * Greater than {@code >}.
         */
        GT,
        /**
         * All different values.
         */
        ALLDIFFERENT
    }

    /**
     * Decomposition result of a predicate.
     */
    public final static class Decomposition {
        final Set<IntegerClause> clauses;
        final Set<IntegerVariable> auxiliaryIntegerVariables;
        final Set<Variable> auxiliaryBooleanVariables;

        /**
         * Constructs a new predicate decomposition
         * @param clauses                   arithmetic clauses produced by the decomposition
         * @param auxiliaryIntegerVariables integer variables produced by the decomposition
         * @param auxiliaryBooleanVariables boolean variables produced by the decomposition
         */
        public Decomposition(final Set<IntegerClause> clauses, final Set<IntegerVariable> auxiliaryIntegerVariables,
                             final Set<Variable> auxiliaryBooleanVariables) {
            this.clauses = clauses;
            this.auxiliaryIntegerVariables = auxiliaryIntegerVariables;
            this.auxiliaryBooleanVariables = auxiliaryBooleanVariables;
        }

        /**
         * Returns the arithmetic clauses.
         * @return the arithmetic clauses
         */
        public Set<IntegerClause> getClauses() {
            return Collections.unmodifiableSet(clauses);
        }

        /**
         * Return auxiliary integer variables.
         * @return auxiliary integer variables
         */
        public Set<IntegerVariable> getAuxiliaryIntegerVariables() {
            return Collections.unmodifiableSet(auxiliaryIntegerVariables);
        }

        /**
         * Return auxiliary boolean variables.
         * @return auxiliary boolean variables
         */
        public Set<Variable> getAuxiliaryBooleanVariables() {
            return Collections.unmodifiableSet(auxiliaryBooleanVariables);
        }

        /**
         * Merges multiple decompositions into one.
         * @param decomps decompositions to merge
         * @return the merged decomposition
         */
        public static Decomposition merge(final Collection<Decomposition> decomps) {
            final Set<IntegerClause> clauses = new LinkedHashSet<>();
            final Set<IntegerVariable> auxiliaryIntegerVariables = new LinkedHashSet<>();
            final Set<Variable> auxiliaryBooleanVariables = new LinkedHashSet<>();
            for (final Decomposition decomp : decomps) {
                clauses.addAll(decomp.getClauses());
                auxiliaryIntegerVariables.addAll(decomp.getAuxiliaryIntegerVariables());
                auxiliaryBooleanVariables.addAll(decomp.getAuxiliaryBooleanVariables());
            }
            return new Decomposition(clauses, auxiliaryIntegerVariables, auxiliaryBooleanVariables);
        }

        /**
         * Merges multiple decompositions into one.
         * @param decomps decompositions to merge
         * @return the merged decomposition
         */
        public static Decomposition merge(final Decomposition... decomps) {
            return merge(List.of(decomps));
        }

        /**
         * Constructs an empty decomposition. (Represents {@code true})
         * @return an empty decomposition
         */
        public static Decomposition empty() {
            return new Decomposition(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
        }

        /**
         * Constructs a decomposition with an empty clause. (Represents {@code false})
         * @return a decomposition with an empty clause
         */
        public static Decomposition emptyClause() {
            return new Decomposition(Collections.singleton(new IntegerClause()), Collections.emptySet(),
                    Collections.emptySet());
        }
    }
}
