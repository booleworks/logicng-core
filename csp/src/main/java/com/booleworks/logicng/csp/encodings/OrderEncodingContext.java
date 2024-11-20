package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encoding context for order encoding.
 */
public class OrderEncodingContext implements CspEncodingContext {
    private final Map<IntegerVariable, Variable[]> variableMap;
    private final List<Variable> simplifyBoolVariables;
    private final List<IntegerVariable> simplifyIntVariables;

    /**
     * Constructs a new encoding context for order encoding.
     */
    OrderEncodingContext() {
        this.variableMap = new LinkedHashMap<>();
        this.simplifyBoolVariables = new ArrayList<>();
        this.simplifyIntVariables = new ArrayList<>();
    }

    /**
     * Copies the encoding context.
     * @param context the context to copy
     */
    public OrderEncodingContext(final OrderEncodingContext context) {
        this.variableMap = new LinkedHashMap<>(context.variableMap);
        this.simplifyBoolVariables = new ArrayList<>(context.simplifyBoolVariables);
        this.simplifyIntVariables = new ArrayList<>(context.simplifyIntVariables);
    }

    @Override
    public CspEncodingAlgorithm getAlgorithm() {
        return CspEncodingAlgorithm.Order;
    }

    /**
     * Creates and stores a new auxiliary variable used for simplifying linear expressions.
     * @param domain the domain
     * @param cf     the factory
     * @return new auxiliary variable
     */
    IntegerVariable newSimplifyIntVariable(final IntegerDomain domain, final CspFactory cf) {
        final IntegerVariable var = cf.auxVariable(OrderReduction.AUX_SIMPLE, domain);
        this.simplifyIntVariables.add(var);
        return var;
    }

    /**
     * Creates and stores a new boolean auxiliary variable for simplifying arithmetic clauses.
     * @param f the factory
     * @return new auxiliary variable
     */
    Variable newSimplifyBooleanVariable(final FormulaFactory f) {
        final Variable var = f.newAuxVariable(CSP_AUX_LNG_VARIABLE);
        this.simplifyBoolVariables.add(var);
        return var;
    }

    boolean allocateVariable(final IntegerVariable group, final int size) {
        if (variableMap.containsKey(group)) {
            return false;
        } else {
            variableMap.put(group, new Variable[size]);
            return true;
        }
    }

    /**
     * Create a boolean variable representing of a certain index of an integer variable. If an instance already exists,
     * no new instance is created and the existing one is returned.
     * @param group the integer variable
     * @param index the queried index
     * @param f     the formula factory
     * @return the boolean variable
     */
    Variable newVariableInstance(final IntegerVariable group, final int index, final FormulaFactory f) {
        final Variable[] intMap = this.variableMap.get(group);
        assert index < intMap.length;
        if (intMap[index] == null) {
            intMap[index] = f.newAuxVariable(CSP_AUX_LNG_VARIABLE);
        }
        return intMap[index];
    }

    /**
     * Get a boolean variable representing of a certain index of an integer variable.
     * @param group the integer variable
     * @param index the queried index
     * @return the boolean variable
     */
    Variable getVariableInstance(final IntegerVariable group, final int index) {
        final Variable[] intMap = this.variableMap.get(group);
        assert index < intMap.length;
        return intMap[index];
    }

    /**
     * Returns the mapping between integer variables and their indices and associated boolean variables.
     * @return the mapping between integer variables and their indices and associated boolean variables
     */
    public Map<IntegerVariable, Variable[]> getVariableMap() {
        return Collections.unmodifiableMap(this.variableMap);
    }

    @Override
    public Set<Variable> getSatVariables(final Collection<IntegerVariable> variables) {
        return variables.stream().map(variableMap::get).filter(Objects::nonNull).flatMap(Arrays::stream)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Override
    public boolean isEncoded(final IntegerVariable v) {
        return variableMap.containsKey(v);
    }

    /**
     * Returns all integer variables encoded in this context.
     * @return all integer variables encoded in this context
     */
    public Set<IntegerVariable> getIntegerVariables() {
        return this.variableMap.keySet();
    }

    /**
     * Returns all boolean auxiliary variables that are used for simplifications.
     * @return all boolean auxiliary variables that are used for simplifications
     */
    public List<Variable> getSimplifyBoolVariables() {
        return this.simplifyBoolVariables;
    }

    /**
     * Returns all integer auxiliary variables that are used for simplifications.
     * @return all integer auxiliary variables that are used for simplifications
     */
    public List<IntegerVariable> getSimplifyIntVariables() {
        return this.simplifyIntVariables;
    }
}
