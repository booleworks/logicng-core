package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Csp {
    //private int[] bases;
    private final List<IntegerVariable> integerVariables;
    private final List<Variable> booleanVariables;
    private List<IntegerClause> clauses;
    private final Map<String, IntegerVariable> integerVariableMap;
    private final Map<String, Variable> booleanVariableMap;
    private final CspFactory f;

    private Csp(final CspFactory f) {
        this.integerVariables = new ArrayList<>();
        this.booleanVariables = new ArrayList<>();
        this.clauses = new ArrayList<>();
        this.integerVariableMap = new HashMap<>();
        this.booleanVariableMap = new HashMap<>();
        this.f = f;
    }

    private Csp(final Csp other) {
        this.integerVariableMap = new HashMap<>(other.integerVariableMap);
        this.booleanVariableMap = new HashMap<>(other.booleanVariableMap);
        this.integerVariables = new ArrayList<>(other.integerVariables);
        this.booleanVariables = new ArrayList<>(other.booleanVariables);
        this.clauses = new ArrayList<>(other.clauses);
        this.f = other.f;
    }

    public List<IntegerVariable> getIntegerVariables() {
        return this.integerVariables;
    }

    public List<Variable> getBooleanVariables() {
        return this.booleanVariables;
    }

    public List<IntegerClause> getClauses() {
        return this.clauses;
    }

    public CspFactory getCspFactory() {
        return this.f;
    }

    @Override
    public String toString() {
        return "IntegerCsp{" +
                "integerVariables=" + this.integerVariables +
                ", booleanVariables=" + this.booleanVariables +
                ", clauses=" + this.clauses +
                '}';
    }

    public static Csp fromFormula(final CspPredicate formula, final CspFactory cf) {
        final Formula decomposedFormula = formula.decompose();
        final Csp.Builder converted = CspConversion.convert(Collections.singletonList(decomposedFormula), cf);
        return converted.build();
    }

    public static Csp fromFormulas(final Collection<CspPredicate> formulas, final CspFactory cf) {
        final var decomposedFormulas = formulas.stream().map(CspPredicate::decompose).collect(Collectors.toList());
        final Csp.Builder converted = CspConversion.convert(decomposedFormulas, cf);
        return converted.build();
    }

    public static class Builder {
        private Csp csp;
        private int auxIntegerVariables = 0;
        private int auxBoolVariables = 0;

        public Builder(final CspFactory f) {
            this.csp = new Csp(f);
        }

        public void addClause(final IntegerClause clause) {
            this.csp.clauses.add(clause);
        }

        public void setClauses(final List<IntegerClause> clauses) {
            this.csp.clauses = clauses;
        }

        public boolean addIntegerVariable(final IntegerVariable v) {
            final String name = v.getName();
            if (this.csp.integerVariableMap.containsKey(name)) {
                return false;
            } else {
                this.csp.integerVariableMap.put(name, v);
                this.csp.integerVariables.add(v);
                return true;
            }
        }

        public boolean addBooleanVariable(final Variable v) {
            final String name = v.name();
            if (this.csp.booleanVariableMap.containsKey(name)) {
                return false;
            } else {
                this.csp.booleanVariableMap.put(name, v);
                this.csp.booleanVariables.add(v);
                return true;
            }
        }

        public IntegerVariable addAuxIntVariable(final String prefix, final IntegerDomain domain) {
            final IntegerVariable v = IntegerVariable.auxVar(prefix + (this.auxIntegerVariables + 1), domain);
            ++this.auxIntegerVariables;
            addIntegerVariable(v);
            return v;
        }

        public Variable addAuxBoolVariable(final String prefix) {
            final Variable v = this.csp.getCspFactory().getFormulaFactory().variable(prefix + (this.auxBoolVariables + 1));
            ++this.auxBoolVariables;
            addBooleanVariable(v);
            return v;
        }

        public Csp buildClone() {
            return new Csp(this.csp);
        }

        public Csp build() {
            final Csp csp = this.csp;
            this.csp = null;
            return csp;
        }

        public List<IntegerVariable> getIntegerVariables() {
            return this.csp.integerVariables;
        }

        public List<Variable> getBooleanVariables() {
            return this.csp.booleanVariables;
        }

        public List<IntegerClause> getClauses() {
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
