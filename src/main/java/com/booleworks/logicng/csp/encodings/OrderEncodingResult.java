package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.propositions.Proposition;
import com.booleworks.logicng.solvers.MiniSat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class OrderEncodingResult {
    public final static String CSP_VAR_PREFIX = "@CSP_";
    private final EncodingResult encodingResult;
    private final Map<IntegerVariable, Map<Integer, Variable>> variableMap;
    private int _createdVariables;

    private OrderEncodingResult(final FormulaFactory f, final MiniSat miniSat, final Proposition proposition) {
        this.encodingResult = EncodingResult.resultForMiniSat(f, miniSat, proposition);
        this.variableMap = new TreeMap<>();
        this._createdVariables = 0;
    }

    public static OrderEncodingResult resultForFormula(final FormulaFactory f) {
        return new OrderEncodingResult(f, null, null);
    }

    public static OrderEncodingResult resultForMiniSat(final FormulaFactory f, final MiniSat miniSat, final Proposition proposition) {
        return new OrderEncodingResult(f, miniSat, proposition);
    }

    public Variable intVariableInstance(final IntegerVariable group, final int index) {
        final Map<Integer, Variable> intMap = this.variableMap.computeIfAbsent(group, k -> new TreeMap<>());
        return intMap.computeIfAbsent(index, i -> newVariable());
    }

    public void addClause(final Literal... literals) {
        this.encodingResult.addClause(literals);
    }

    public void addClause(final LNGVector<Literal> literals) {
        this.encodingResult.addClause(literals);
    }

    private Variable newVariable() {
        final int index = this._createdVariables++;
        return factory().variable(CSP_VAR_PREFIX + index);
    }

    public List<Formula> result() {
        return this.encodingResult.result();
    }

    public FormulaFactory factory() {
        return this.encodingResult.factory();
    }

    public Map<IntegerVariable, Map<Integer, Variable>> getVariableMap() {
        return this.variableMap;
    }

    public Set<Variable> getRelevantSatVariables() {
        return this.variableMap.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toSet());
    }
}
