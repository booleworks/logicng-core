package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Csp {
    private final Set<IntegerVariable> integerVariables;
    private final Set<Variable> booleanVariables;
    private Set<IntegerClause> clauses;
    private final CspFactory f;

    private Csp(final CspFactory f) {
        this.integerVariables = new TreeSet<>();
        this.booleanVariables = new TreeSet<>();
        this.clauses = new TreeSet<>();
        this.f = f;
    }

    private Csp(final Csp other) {
        this.integerVariables = new TreeSet<>(other.integerVariables);
        this.booleanVariables = new TreeSet<>(other.booleanVariables);
        this.clauses = new TreeSet<>(other.clauses);
        this.f = other.f;
    }

    public Csp(final CspFactory f, final Set<IntegerVariable> integerVariables, final Set<Variable> booleanVariables, final Set<IntegerClause> clauses) {
        this.integerVariables = integerVariables;
        this.booleanVariables = booleanVariables;
        this.clauses = clauses;
        this.f = f;
    }

    public Set<IntegerVariable> getIntegerVariables() {
        return this.integerVariables;
    }

    public Set<Variable> getBooleanVariables() {
        return this.booleanVariables;
    }

    public Set<IntegerClause> getClauses() {
        return this.clauses;
    }

    public CspFactory getCspFactory() {
        return this.f;
    }

    @Override
    public String toString() {
        return "Csp{" +
                "integerVariables=" + integerVariables +
                ", booleanVariables=" + booleanVariables +
                ", clauses=" + clauses +
                '}';
    }

    public static Csp fromClauses(final CspFactory f, final Set<IntegerClause> clauses) {
        final Set<IntegerVariable> intVars = new TreeSet<>();
        final Set<Variable> boolVars = new TreeSet<>();
        for (final IntegerClause clause : clauses) {
            intVars.addAll(clause.getArithmeticLiterals().stream().flatMap(v -> v.getVariables().stream()).collect(Collectors.toSet()));
            boolVars.addAll(clause.getBoolLiterals().stream().map(Literal::variable).collect(Collectors.toSet()));
        }
        return new Csp(f, intVars, boolVars, clauses);
    }

    public static Csp merge(final CspFactory f, final Csp... csps) {
        return merge(f, Arrays.asList(csps));
    }

    public static Csp merge(final CspFactory f, final Collection<Csp> csps) {
        if (csps.isEmpty()) {
            return new Csp(f);
        } else if (csps.size() == 1) {
            return csps.iterator().next();
        } else {
            final Csp newCsp = new Csp(f);
            for (final Csp csp : csps) {
                newCsp.integerVariables.addAll(csp.integerVariables);
                newCsp.booleanVariables.addAll(csp.booleanVariables);
                newCsp.clauses.addAll(csp.clauses);
            }
            return newCsp;
        }
    }

    public static class Builder {
        private Csp csp;

        public Builder(final CspFactory f) {
            csp = new Csp(f);
        }

        public Builder(final Csp csp) {
            this.csp = new Csp(csp);
        }

        public Builder addClause(final IntegerClause clause) {
            this.csp.clauses.add(clause);
            return this;
        }

        public Builder updateClauses(final Set<IntegerClause> clauses) {
            this.csp.clauses = clauses;
            return this;
        }

        public boolean addIntegerVariable(final IntegerVariable v) {
            return this.csp.integerVariables.add(v);
        }

        public boolean addBooleanVariable(final Variable v) {
            return this.csp.booleanVariables.add(v);
        }

        public Csp build() {
            final Csp csp = this.csp;
            this.csp = null;
            return csp;
        }

        public Set<IntegerVariable> getIntegerVariables() {
            return this.csp.integerVariables;
        }

        public Set<Variable> getBooleanVariables() {
            return this.csp.booleanVariables;
        }

        public Set<IntegerClause> getClauses() {
            return this.csp.clauses;
        }

        public CspFactory getCspFactory() {
            return this.csp.f;
        }

        @Override
        public String toString() {
            return this.csp.toString();
        }
    }
}
