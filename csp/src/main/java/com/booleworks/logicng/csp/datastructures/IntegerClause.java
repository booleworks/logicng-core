package com.booleworks.logicng.csp.datastructures;

import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a hybrid arithmetic/boolean clause with arithmetic literals and boolean literals.
 */
public class IntegerClause {
    private final Set<Literal> boolLiterals;
    private final Set<ArithmeticLiteral> arithLiterals;

    /**
     * Constructs new arithmetic clause.
     * @param boolLiterals  boolean literals of the clause
     * @param arithLiterals arithmetic literals of the clause
     */
    public IntegerClause(final Set<Literal> boolLiterals, final Set<ArithmeticLiteral> arithLiterals) {
        this.boolLiterals = boolLiterals;
        this.arithLiterals = arithLiterals;
    }

    /**
     * Constructs new arithmetic clause with only boolean literals.
     * @param boolLiteral boolean literals
     */
    public IntegerClause(final Literal boolLiteral) {
        this(Collections.singleton(boolLiteral), Collections.emptySet());
    }

    /**
     * Constructs new arithmetic clause with only arithmetic literals.
     * @param arithLiteral arithmetic literals
     */
    public IntegerClause(final ArithmeticLiteral arithLiteral) {
        this(Collections.emptySet(), Collections.singleton(arithLiteral));
    }

    /**
     * Constructs new arithmetic clause with only boolean literals.
     * @param booleanLiterals boolean literals
     */
    public IntegerClause(final Literal... booleanLiterals) {
        this(new LinkedHashSet<>(List.of(booleanLiterals)), Collections.emptySet());
    }

    /**
     * Constructs new arithmetic clause with only arithmetic literals.
     * @param arithmeticLiterals arithmetic literals
     */
    public IntegerClause(final ArithmeticLiteral... arithmeticLiterals) {
        this(Collections.emptySet(), new LinkedHashSet<>(List.of(arithmeticLiterals)));
    }

    /**
     * Constructs new arithmetic clause with one boolean literal and one arithmetic literal.
     * @param boolLiteral       boolean literal
     * @param arithmeticLiteral arithmetic literal
     */
    public IntegerClause(final Literal boolLiteral, final ArithmeticLiteral arithmeticLiteral) {
        this(Collections.singleton(boolLiteral), Collections.singleton(arithmeticLiteral));
    }

    /**
     * Constructs new empty arithmetic clause.
     */
    public IntegerClause() {
        this.boolLiterals = Collections.emptySortedSet();
        this.arithLiterals = Collections.emptySortedSet();
    }

    /**
     * Returns all boolean literals of this clause.
     * @return all boolean literals of this clause
     */
    public Set<Literal> getBoolLiterals() {
        return boolLiterals;
    }

    /**
     * Returns all arithmetic literals of this clause.
     * @return all arithmetic literals of this clause
     */
    public Set<ArithmeticLiteral> getArithmeticLiterals() {
        return arithLiterals;
    }

    /**
     * Returns all integer variables that are contained in all literals of this clause.
     * @return all integer variables that are contained in all literals of this clause
     */
    public Set<IntegerVariable> getCommonVariables() {
        if (!boolLiterals.isEmpty()) {
            return Collections.emptySet();
        }
        Set<IntegerVariable> commonVars = null;
        for (final ArithmeticLiteral lit : arithLiterals) {
            final Set<IntegerVariable> vs = lit.getVariables();
            if (commonVars == null) {
                commonVars = vs;
            } else {
                commonVars = commonVars.stream().filter(vs::contains).collect(Collectors.toSet());
            }
        }
        return Objects.requireNonNullElse(commonVars, Collections.emptySet());
    }

    /**
     * Returns the number of literals in this clause.
     * @return the number of literals in this clause
     */
    public int size() {
        return boolLiterals.size() + arithLiterals.size();
    }

    /**
     * Returns whether the clause is valid, i.e., one literal is valid.
     * @return whether the clause is valid
     */
    public boolean isValid() {
        for (final ArithmeticLiteral lit : arithLiterals) {
            if (lit.isValid()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the clause is trivially unsatisfiable, i.e., all literals are unsatisfiable.
     * @return whether the clause is trivially unsatisfiable
     */
    public boolean isUnsat() {
        for (final ArithmeticLiteral lit : arithLiterals) {
            if (!lit.isUnsat()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CLAUSE<");
        final String lits = Stream.concat(
                boolLiterals.stream().map(Object::toString),
                arithLiterals.stream().map(Object::toString)
        ).collect(Collectors.joining(", "));
        builder.append(lits);
        builder.append(">");
        return builder.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final IntegerClause that = (IntegerClause) o;

        if (!boolLiterals.equals(that.boolLiterals)) {
            return false;
        }
        return arithLiterals.equals(that.arithLiterals);
    }

    @Override
    public int hashCode() {
        int result = boolLiterals.hashCode();
        result = 31 * result + arithLiterals.hashCode();
        return result;
    }

    /**
     * Combines two predicate decompositions by factorizing their clauses.
     * <p>
     * This expresses a disjunction of two sets of clauses.
     * <p>
     * TODO: We could use Tseitin instead
     * @param left  the left decomposition
     * @param right the right decomposition
     * @return the factorized decomposition.
     */
    public static CspPredicate.Decomposition factorize(final CspPredicate.Decomposition left,
                                                       final CspPredicate.Decomposition right) {
        final Set<IntegerClause> clauses = new LinkedHashSet<>();
        for (final IntegerClause l : left.getClauses()) {
            for (final IntegerClause r : right.getClauses()) {
                final Set<Literal> newBools = new LinkedHashSet<>(l.boolLiterals);
                final Set<ArithmeticLiteral> newAriths = new LinkedHashSet<>(l.arithLiterals);
                newBools.addAll(r.boolLiterals);
                newAriths.addAll(r.arithLiterals);
                clauses.add(new IntegerClause(newBools, newAriths));
            }
        }
        final Set<IntegerVariable> intVars = new LinkedHashSet<>(left.getAuxiliaryIntegerVariables());
        final Set<Variable> boolVars = new LinkedHashSet<>(left.getAuxiliaryBooleanVariables());
        intVars.addAll(right.getAuxiliaryIntegerVariables());
        boolVars.addAll(right.getAuxiliaryBooleanVariables());
        return new CspPredicate.Decomposition(clauses, intVars, boolVars);
    }

    /**
     * A builder for incrementally building arithmetic clauses.
     */
    public static class Builder {
        private IntegerClause clause;

        /**
         * Constructs a new clause builder.
         */
        public Builder() {
            clause = new IntegerClause(new LinkedHashSet<>(), new LinkedHashSet<>());
        }

        /**
         * Constructs a new clause builder by copying an existing clause.
         * @param clause the existing clause
         */
        public Builder(final IntegerClause clause) {
            this.clause = new IntegerClause(new LinkedHashSet<>(clause.getBoolLiterals()),
                    new LinkedHashSet<>(clause.getArithmeticLiterals()));
        }

        /**
         * Constructs a new clause builder from a set of boolean and arithmetic literals
         * @param boolLiterals  the boolean literals
         * @param arithLiterals the arithmetic literals
         */
        private Builder(final Set<Literal> boolLiterals, final Set<ArithmeticLiteral> arithLiterals) {
            this.clause = new IntegerClause(boolLiterals, arithLiterals);
        }

        /**
         * Constructs a new clause builder by copying only the boolean literals of an existing clause.
         * @param clause the existing clause
         * @return the new builder
         */
        public static Builder cloneOnlyBool(final IntegerClause clause) {
            return new Builder(new LinkedHashSet<>(clause.getBoolLiterals()), new LinkedHashSet<>());
        }

        /**
         * Constructs a new clause builder by copying only the arithmetic literals of an existing clause.
         * @param clause the existing clause
         * @return the new builder
         */
        public static Builder cloneOnlyArith(final IntegerClause clause) {
            return new Builder(new LinkedHashSet<>(), new LinkedHashSet<>(clause.getArithmeticLiterals()));
        }

        /**
         * Adds a boolean literal to the clause.
         * @param literal the boolean literal
         * @return this builder
         */
        public Builder addBooleanLiteral(final Literal literal) {
            clause.boolLiterals.add(literal);
            return this;
        }

        /**
         * Adds an arithmetic literal to the clause.
         * @param literal the arithmetic literal
         * @return this builder
         */
        public Builder addArithmeticLiteral(final ArithmeticLiteral literal) {
            clause.arithLiterals.add(literal);
            return this;
        }

        /**
         * Adds boolean literals to the clause.
         * @param literals the boolean literals
         * @return this builder
         */
        public Builder addBooleanLiterals(final Collection<Literal> literals) {
            clause.boolLiterals.addAll(literals);
            return this;
        }

        /**
         * Adds boolean literals to the clause.
         * @param literals the boolean literals
         * @return this builder
         */
        public Builder addBooleanLiterals(final Literal... literals) {
            return addBooleanLiterals(List.of(literals));
        }

        /**
         * Adds arithmetic literals to the clause.
         * @param literals the arithmetic literals
         * @return this builder
         */
        public Builder addArithmeticLiterals(final Collection<ArithmeticLiteral> literals) {
            clause.arithLiterals.addAll(literals);
            return this;
        }

        /**
         * Adds arithmetic literals to the clause.
         * @param literals the arithmetic literals
         * @return this builder
         */
        public Builder addArithmeticLiterals(final ArithmeticLiteral... literals) {
            clause.arithLiterals.addAll(List.of(literals));
            return this;
        }

        /**
         * Builds the clause. This invalidates this builder.
         * @return the built clause
         */
        public IntegerClause build() {
            final IntegerClause ret = clause;
            clause = null;
            return ret;
        }
    }
}
