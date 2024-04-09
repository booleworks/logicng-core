package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.InternalAuxVarType;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class CspEncodingContext {
    private final Map<IntegerVariable, Map<Integer, Variable>> variableMap;
    private final Set<Variable> booleanAuxVariables;
    private final Set<IntegerVariable> integerAuxVariables;
    private final CspFactory cspFactory;
    private final CspEncoder.Algorithm algorithm;
    private int booleanVariables = 0;
    private int integerVariables = 0;

    public CspEncodingContext(final CspFactory f) {
        this(f, CspEncoder.Algorithm.Order);
    }

    public CspEncodingContext(final CspFactory f, final CspEncoder.Algorithm algorithm) {
        variableMap = new TreeMap<>();
        booleanAuxVariables = new TreeSet<>();
        integerAuxVariables = new TreeSet<>();
        cspFactory = f;
        this.algorithm = algorithm;
    }

    public CspEncodingContext(final CspEncodingContext context) {
        variableMap = new TreeMap<>(context.variableMap);
        booleanAuxVariables = new TreeSet<>(context.booleanAuxVariables);
        integerAuxVariables = new TreeSet<>(context.integerAuxVariables);
        cspFactory = context.cspFactory;
        booleanVariables = context.booleanVariables;
        integerVariables = context.integerVariables;
        algorithm = context.algorithm;
    }

    public Variable intVariableInstance(final IntegerVariable group, final int index, final EncodingResult result) {
        final Map<Integer, Variable> intMap = variableMap.computeIfAbsent(group, k -> new TreeMap<>());
        return intMap.computeIfAbsent(index, i -> result.newVariable(InternalAuxVarType.CSP));
    }

    public Map<IntegerVariable, Map<Integer, Variable>> getVariableMap() {
        return variableMap;
    }

    public Set<Variable> getRelevantSatVariables() {
        return variableMap.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toSet());
    }

    public Set<IntegerVariable> getIntegerVariables() {
        return variableMap.keySet();
    }

    public Set<Variable> getBooleanAuxVariables() {
        return booleanAuxVariables;
    }

    public Set<IntegerVariable> getIntegerAuxVariables() {
        return integerAuxVariables;
    }

    public CspEncoder.Algorithm getAlgorithm() {
        return algorithm;
    }

    public CspFactory factory() {
        return cspFactory;
    }

    IntegerVariable newAuxIntVariable(final String prefix, final IntegerDomain domain) {
        final IntegerVariable var = IntegerVariable.auxVar(prefix + (++integerVariables), domain);
        integerAuxVariables.add(var);
        return var;
    }

    Variable newAuxBoolVariable() {
        final Variable var = cspFactory.getFormulaFactory().newAuxVariable(InternalAuxVarType.CSP);
        booleanAuxVariables.add(var);
        return var;
    }

    Literal negate(final Variable v) {
        return v.negate(v.factory());
    }
}
