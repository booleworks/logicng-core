package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class represents an arithmetic term.
 * <p>
 * {@link IntegerConstant} and {@link IntegerVariable} are the atomic terms. {@link Function}s are complex terms
 * composed of other terms.
 * <p>
 * All terms are created using a {@link CspFactory}.
 */
public abstract class Term {

    /**
     * Type of this term.
     */
    protected final Type type;
    /**
     * Cached decomposition of this term.
     */
    protected Decomposition decompositionResult;

    /**
     * Constructs new term of a given type.
     * @param type the type of the term
     */
    protected Term(final Type type) {
        this.type = type;
    }

    /**
     * Returns the type of the term.
     * @return the type of the term
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns {@code true} if and only if the term is an atomic term.
     * @return {@code true} if and only if the term is an atomic term.
     */
    public abstract boolean isAtom();

    /**
     * Return a set of all variables used in the term.
     * @param cf the factory
     * @return a set of a variables used in the term
     */
    public SortedSet<IntegerVariable> variables(final CspFactory cf) {
        final SortedSet<IntegerVariable> set = new TreeSet<>();
        variablesInplace(set);
        return set;
    }

    /**
     * Adds all variables used in the term to {@code variables}.
     * @param variables set to add the result to
     */
    public abstract void variablesInplace(SortedSet<IntegerVariable> variables);

    /**
     * Calculates the decomposition of this term. (without caching)
     * @param cf the factory
     * @return the decomposition result
     */
    protected abstract Decomposition calculateDecomposition(final CspFactory cf);

    /**
     * Decomposes the term into a linear sum and addition constraints.
     * The result is cached and reused for further calls.
     * @param cf the factory
     * @return the decomposition result
     */
    public final Decomposition decompose(final CspFactory cf) {
        if (decompositionResult == null) {
            decompositionResult = calculateDecomposition(cf);
        }
        return decompositionResult;
    }

    /**
     * Types a term can have.
     */
    public enum Type {
        /**
         * Constant value: zero.
         */
        ZERO,
        /**
         * Constant value: one.
         */
        ONE,
        /**
         * Any constant value (not zero or one).
         */
        CONST,
        /**
         * Integer Variable.
         */
        VAR,
        /**
         * Negation.
         */
        NEG,
        /**
         * Addition.
         */
        ADD,
        /**
         * Subtraction.
         */
        SUB,
        /**
         * Multiplication with constant.
         */
        MUL,
        /**
         * Modulo with constant.
         */
        MOD,
        /**
         * Division with constant.
         */
        DIV,
        /**
         * Maximum
         */
        MAX,
        /**
         * Minimum
         */
        MIN,
        /**
         * Absolute
         */
        ABS
    }

    /**
     * Decomposition result of a term.
     */
    public static final class Decomposition {
        private final LinearExpression linearExpression;
        private final Set<IntegerClause> additionalConstraints;

        private final Set<IntegerVariable> auxiliaryIntegerVariables;
        private final Set<Variable> auxiliaryBooleanVariables;

        /**
         * Constructs a new term decomposition.
         * @param linearExpression          the linear sum
         * @param additionalConstraints     the additional constraints
         * @param auxiliaryIntegerVariables integer variables produced by the decomposition
         * @param auxiliaryBooleanVariables boolean variables produced by the decomposition
         */
        public Decomposition(final LinearExpression linearExpression, final Set<IntegerClause> additionalConstraints,
                             final Set<IntegerVariable> auxiliaryIntegerVariables,
                             final Set<Variable> auxiliaryBooleanVariables) {
            this.linearExpression = linearExpression;
            this.additionalConstraints = additionalConstraints;
            this.auxiliaryIntegerVariables = auxiliaryIntegerVariables;
            this.auxiliaryBooleanVariables = auxiliaryBooleanVariables;
        }

        /**
         * Returns the linear expression
         * @return the linear expression
         */
        public LinearExpression getLinearExpression() {
            return linearExpression;
        }

        /**
         * Returns additional constraints
         * @return additional constraints
         */
        public Set<IntegerClause> getAdditionalConstraints() {
            return Collections.unmodifiableSet(additionalConstraints);
        }

        /**
         * Returns auxiliary integer variables.
         * @return auxiliary integer variables
         */
        public Set<IntegerVariable> getAuxiliaryIntegerVariables() {
            return Collections.unmodifiableSet(auxiliaryIntegerVariables);
        }

        /**
         * Returns auxiliary boolean variables.
         * @return auxiliary boolean variables
         */
        public Set<Variable> getAuxiliaryBooleanVariables() {
            return Collections.unmodifiableSet(auxiliaryBooleanVariables);
        }

        /**
         * Merges a term composition with a collection of predicate decompositions into one term decomposition.
         * <p>
         * It preserves the linear sum, merges all auxiliary variables, and the predicates' clauses are added as
         * additional constraints.
         * @param term                    term decomposition
         * @param predicateDecompositions collection of predicate decompositions to merge
         * @return merged term decomposition
         */
        public static Decomposition merge(final Decomposition term,
                                          final Collection<CspPredicate.Decomposition> predicateDecompositions) {
            final Set<IntegerClause> clauses = new LinkedHashSet<>();
            final Set<IntegerVariable> intVars = new LinkedHashSet<>(term.getAuxiliaryIntegerVariables());
            final Set<Variable> boolVars = new LinkedHashSet<>(term.getAuxiliaryBooleanVariables());
            for (final CspPredicate.Decomposition predDecomp : predicateDecompositions) {
                clauses.addAll(predDecomp.getClauses());
                intVars.addAll(predDecomp.getAuxiliaryIntegerVariables());
                boolVars.addAll(predDecomp.getAuxiliaryBooleanVariables());
            }
            return new Decomposition(term.linearExpression, clauses, intVars, boolVars);
        }

        @Override
        public String toString() {
            return "IntegerTerm.Decomposition{" +
                    "linearExpression=" + linearExpression.toString() +
                    ", additionalConstraints=" + additionalConstraints.toString() +
                    ", auxiliaryIntegerVariables=" + auxiliaryIntegerVariables.toString() +
                    ", auxiliaryBooleanVariables=" + auxiliaryBooleanVariables.toString() +
                    '}';
        }
    }
}
