package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerVariableSubstitution;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerConstant;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encoding context for compact oder encoding.
 */
public class CompactOrderEncodingContext implements CspEncodingContext {
    private final OrderEncodingContext orderContext;
    private final int base;
    private final Map<IntegerVariable, List<IntegerVariable>> digits;
    private final Map<IntegerConstant, List<Integer>> constDigits;
    private final List<IntegerVariable> auxiliaryDigitVariables;
    private final Map<IntegerVariable, Integer> offsets;
    private final IntegerVariableSubstitution adjustedVariablesSubstitution;
    private final List<IntegerVariable> adjustedVariables;
    private final List<Variable> adjustedBoolVariables;
    private final List<IntegerVariable> ternarySimplificationVariables;
    private final List<IntegerVariable> rcspVariables;
    private final List<IntegerVariable> ccspVariables;
    private final List<Variable> ccspBoolVariables;

    /**
     * Constructs a new encoding context for compact order encoding for a given base.
     * @param base the base of the context
     */
    CompactOrderEncodingContext(final int base) {
        this.base = base;
        this.orderContext = CspEncodingContext.order();
        this.digits = new HashMap<>();
        this.constDigits = new HashMap<>();
        this.auxiliaryDigitVariables = new ArrayList<>();
        this.offsets = new HashMap<>();
        this.adjustedVariablesSubstitution = new IntegerVariableSubstitution();
        this.adjustedVariables = new ArrayList<>();
        this.adjustedBoolVariables = new ArrayList<>();
        this.ternarySimplificationVariables = new ArrayList<>();
        this.rcspVariables = new ArrayList<>();
        this.ccspVariables = new ArrayList<>();
        this.ccspBoolVariables = new ArrayList<>();
    }

    /**
     * Copies the encoding context.
     * @param context the context to copy
     */
    public CompactOrderEncodingContext(final CompactOrderEncodingContext context) {
        this.base = context.base;
        this.orderContext = new OrderEncodingContext(context.orderContext);
        this.digits = new HashMap<>(context.digits);
        this.constDigits = new HashMap<>(context.constDigits);
        this.auxiliaryDigitVariables = new ArrayList<>(context.auxiliaryDigitVariables);
        this.offsets = new HashMap<>(context.offsets);
        this.adjustedVariablesSubstitution = new IntegerVariableSubstitution(context.adjustedVariablesSubstitution);
        this.adjustedVariables = new ArrayList<>(context.adjustedVariables);
        this.adjustedBoolVariables = new ArrayList<>(context.adjustedBoolVariables);
        this.ternarySimplificationVariables = new ArrayList<>(context.ternarySimplificationVariables);
        this.rcspVariables = new ArrayList<>(context.rcspVariables);
        this.ccspVariables = new ArrayList<>(context.ccspVariables);
        this.ccspBoolVariables = new ArrayList<>(context.ccspBoolVariables);
    }

    /**
     * Returns the base of the encoding context.
     * @return the base of the encoding context
     */
    public int getBase() {
        return base;
    }

    /**
     * Associates an integer variables with other integer variables representing its digits.
     * @param v      the variable
     * @param digits the digits
     */
    public void addDigits(final IntegerVariable v, final List<IntegerVariable> digits) {
        this.digits.put(v, digits);
    }

    /**
     * Creates and stores a new integer variable representing a digit.
     * @param d  the domain of the digit
     * @param cf the factory
     * @return the integer variable representing a digit.
     */
    IntegerVariable newAuxiliaryDigitVariable(final IntegerDomain d, final CspFactory cf) {
        final IntegerVariable v = cf.auxVariable(CompactCSPReduction.AUX_DIGIT, d);
        auxiliaryDigitVariables.add(v);
        return v;
    }

    /**
     * Returns all auxiliary variables that are digits.
     * @return all auxiliary variables that are digits.
     */
    public List<IntegerVariable> getAuxiliaryDigitVariables() {
        return auxiliaryDigitVariables;
    }

    /**
     * Returns whether digits are associated with this integer variable.
     * @param v the variable
     * @return {@code true} if digits are associated with this integer variable, {@code false} otherwise
     */
    public boolean hasDigits(final IntegerVariable v) {
        return this.digits.containsKey(v);
    }

    /**
     * Returns the digits associated with the given integer variable.
     * @param v the variable
     * @return the digits associated with the given integer variable.
     */
    public List<IntegerVariable> getDigits(final IntegerVariable v) {
        return digits.get(v);
    }

    /**
     * Associates integer constants with its digits.
     * @param c      the constant
     * @param digits the digits
     */
    void addConstDigits(final IntegerConstant c, final List<Integer> digits) {
        this.constDigits.put(c, digits);
    }

    /**
     * Returns whether digits are associated with an integer constant.
     * @param c the constant
     * @return {@code true} if digits are associated with the integer constant, {@code false} otherwise.
     */
    public boolean hasConstDigits(final IntegerConstant c) {
        return constDigits.containsKey(c);
    }

    /**
     * Returns the digits associated with an integer constant.
     * @param c the constant
     * @return the digits associated with the constant.
     */
    public List<Integer> getConstDigits(final IntegerConstant c) {
        return constDigits.get(c);
    }

    /**
     * Returns whether an offset is associated with the integer variable.
     * @param v the variable
     * @return {@code true} whether an offset is associated with the variable, {@code false} otherwise.
     */
    public boolean hasOffset(final IntegerVariable v) {
        return offsets.containsKey(v);
    }

    /**
     * Returns the offset associated with the integer variable.
     * @param v the variable
     * @return the offset
     */
    public int getOffset(final IntegerVariable v) {
        return offsets.get(v);
    }

    /**
     * Associates an offset with an integer variable.
     * @param v      the variable
     * @param offset the offset
     */
    public void addOffset(final IntegerVariable v, final int offset) {
        offsets.put(v, offset);
    }

    /**
     * Returns the order encoding context of this context
     * @return the order encoding context
     */
    public OrderEncodingContext getOrderContext() {
        return orderContext;
    }

    /**
     * Creates and stores new auxiliary variable for adjusted variables.
     * @param prefix the prefix of variable's name
     * @param d      the domain of the variable
     * @param cf     the factory
     * @return the new variable
     */
    IntegerVariable newAdjustedVariable(final String prefix, final IntegerDomain d, final CspFactory cf) {
        final IntegerVariable v = cf.auxVariable(prefix, d);
        adjustedVariables.add(v);
        return v;
    }

    /**
     * Returns all variables that are adjusted.
     * @return all variables that are adjusted
     */
    public List<IntegerVariable> getAdjustedVariables() {
        return adjustedVariables;
    }

    /**
     * Associates an integer variable with its adjusted variable.
     * @param original   the original variable
     * @param substitute the adjusted variable
     */
    void addAdjustedVariable(final IntegerVariable original, final IntegerVariable substitute) {
        adjustedVariablesSubstitution.add(original, substitute);
    }

    boolean hasAdjustedVariable(final IntegerVariable original) {
        return adjustedVariablesSubstitution.containsKey(original);
    }

    /**
     * Returns the adjusted variable of a variable.
     * @param original the variable
     * @return the adjusted variable
     */
    public IntegerVariable getAdjustedVariable(final IntegerVariable original) {
        return adjustedVariablesSubstitution.get(original);
    }

    /**
     * Returns the adjusted variable of a variable or the variable itself if there is no adjusted variable.
     * @param original the variable
     * @return the adjusted variable or the variable itself if there is no adjusted variable
     */
    public IntegerVariable getAdjustedVariableOrSelf(final IntegerVariable original) {
        return adjustedVariablesSubstitution.getOrSelf(original);
    }

    /**
     * Maps a list of integer variables to the adjusted variables.
     * @param originals the original variables
     * @return the adjusted variables
     */
    public List<IntegerVariable> mapToAdjustedVariableOrSelf(final Collection<IntegerVariable> originals) {
        return originals.stream().map(adjustedVariablesSubstitution::getOrSelf).collect(Collectors.toList());
    }

    /**
     * Returns the substitutions for adjusted variables.
     * @return the substitutions for adjusted variables
     */
    public IntegerVariableSubstitution getAdjustedVariablesSubstitution() {
        return adjustedVariablesSubstitution;
    }

    /**
     * Creates and stores new auxiliary boolean variable for adjusting variables
     * @param f the factory
     * @return the auxiliary variable
     */
    Variable newAdjustedBoolVariable(final FormulaFactory f) {
        final Variable var = f.newAuxVariable(CSP_AUX_LNG_VARIABLE);
        adjustedBoolVariables.add(var);
        return var;
    }

    /**
     * Returns all auxiliary boolean variables that are for adjusting variables.
     * @return all auxiliary variables for adjusting variables
     */
    public List<Variable> getAdjustedBoolVariables() {
        return adjustedBoolVariables;
    }

    /**
     * Creates and stores new auxiliary variable for splitting arithmetic literals to ternary literals.
     * @param d  the domain of the variable
     * @param cf the factory
     * @return the auxiliary variable
     */
    IntegerVariable newTernarySimplificationVariable(final IntegerDomain d, final CspFactory cf) {
        final IntegerVariable v = cf.auxVariable(CompactOrderReduction.AUX_TERNARY, d);
        ternarySimplificationVariables.add(v);
        return v;
    }

    /**
     * Returns auxiliary variables that are used to split arithmetic literals to ternary literals.
     * @return auxiliary variables for splitting arithmetic literals to ternary literals
     */
    public List<IntegerVariable> getTernarySimplificationVariables() {
        return ternarySimplificationVariables;
    }

    /**
     * Creates and stores an auxiliary variable that is used to created RCSP literals.
     * @param d  the domain
     * @param cf the factory
     * @return the auxiliary variable
     */
    IntegerVariable newRCSPVariable(final IntegerDomain d, final CspFactory cf) {
        final IntegerVariable v = cf.auxVariable(CompactOrderReduction.AUX_RCSP, d);
        rcspVariables.add(v);
        return v;
    }

    /**
     * Returns all auxiliary variables used for RCSP literal creation.
     * @return all auxiliary variables used for RCSP literal creation
     */
    public List<IntegerVariable> getRCSPVariables() {
        return rcspVariables;
    }

    /**
     * Create and store an auxiliary variable used for CCSP clauses.
     * @param d  the domain
     * @param cf the factory
     * @return new auxiliary variable
     */
    IntegerVariable newCCSPVariable(final IntegerDomain d, final CspFactory cf) {
        final IntegerVariable v = cf.auxVariable(CompactCSPReduction.AUX_CCSP, d);
        ccspVariables.add(v);
        return v;
    }

    /**
     * Returns all auxiliary variables used for CCSP clauses.
     * @return all auxiliary variables used for CCSP clauses.
     */
    public List<IntegerVariable> getCCSPVariables() {
        return ccspVariables;
    }

    /**
     * Create and store a boolean auxiliary variable for CCSP clauses.
     * @param f the factory
     * @return new auxiliary variable
     */
    Variable newCCSPBoolVariable(final FormulaFactory f) {
        final Variable v = f.newAuxVariable(CSP_AUX_LNG_VARIABLE);
        ccspBoolVariables.add(v);
        return v;
    }

    /**
     * Returns all boolean auxiliary variables used for CCSP clauses.
     * @return all boolean auxiliary variables used for CCSP clauses
     */
    public List<Variable> getCCSPBoolVariables() {
        return ccspBoolVariables;
    }

    @Override
    public Set<Variable> getSatVariables(final Collection<IntegerVariable> variables) {
        final Collection<IntegerVariable> subs = adjustedVariablesSubstitution
                .getAllOrSelf(variables).stream()
                .flatMap(v -> hasDigits(v) ? getDigits(v).stream() : Stream.empty())
                .collect(Collectors.toList());
        return orderContext.getSatVariables(subs);
    }

    @Override
    public boolean isEncoded(final IntegerVariable v) {
        final List<IntegerVariable> digits = getDigits(adjustedVariablesSubstitution.getOrSelf(v));
        if (digits == null) {
            return false;
        } else {
            return digits.stream().anyMatch(orderContext::isEncoded);
        }
    }

    @Override
    public CspEncodingAlgorithm getAlgorithm() {
        return CspEncodingAlgorithm.CompactOrder;
    }
}
