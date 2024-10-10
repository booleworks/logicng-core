package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.datastructures.IntegerVariableSubstitution;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Class grouping functions for decoding problems encoded with the order encoding.
 */
public class OrderDecoding {
    private OrderDecoding() {
    }

    /**
     * Decodes a problem that was encoded with the order encoding.
     * <p>
     * It takes a propositional model {@code model} and a list of integer and boolean variables, which are the
     * variables that should be decoded from {@code model}. Variables not contained in the model will be assigned to
     * any valid value for this variable.
     * <p>
     * {@code propagateSubstitution} is used to resolve addition substitutions that were not done by the encoding.
     * @param model                 propositional model
     * @param integerVariables      included integer variables
     * @param booleanVariables      included boolean variables
     * @param propagateSubstitution extern substitutions
     * @param context               the context
     * @param cf                    the factory
     * @return the decoded assignment
     */
    public static CspAssignment decode(final Assignment model, final Collection<IntegerVariable> integerVariables,
                                       final Collection<Variable> booleanVariables,
                                       final IntegerVariableSubstitution propagateSubstitution,
                                       final OrderEncodingContext context,
                                       final CspFactory cf) {
        final CspAssignment result = new CspAssignment();
        for (final IntegerVariable v : integerVariables) {
            final int value = decodeIntVar(propagateSubstitution.getOrSelf(v), model, context);
            result.addIntAssignment(v, value);
        }
        for (final Variable v : booleanVariables) {
            if (model.positiveVariables().contains(v)) {
                result.addPos(v);
            }
            final Literal negV = v.negate(cf.getFormulaFactory());
            if (model.negativeLiterals().contains(negV)) {
                result.addNeg(negV);
            }
        }
        return result;
    }

    /**
     * Decodes a problem that was encoded with the order encoding.
     * <p>
     * It takes a propositional model {@code model} and a list of integer and boolean variables, which are the
     * variables that should be decoded from {@code model}. Variables not contained in the model will be assigned to
     * any valid value for this variable.
     * @param model            propositional model
     * @param integerVariables included integer variables
     * @param booleanVariables included boolean variables
     * @param context          the context
     * @param cf               the factory
     * @return the decoded assignment
     */
    public static CspAssignment decode(final Assignment model, final Collection<IntegerVariable> integerVariables,
                                       final Collection<Variable> booleanVariables,
                                       final OrderEncodingContext context, final CspFactory cf) {
        return decode(model, integerVariables, booleanVariables, new IntegerVariableSubstitution(), context, cf);
    }

    /**
     * Decodes a problem that was encoded with the order encoding.
     * <p>
     * It takes a propositional model {@code model} and a list of integer variables, which are the variables that
     * should be decoded from {@code model}. Variables not contained in the model will be assigned to any valid value
     * for this variable.
     * @param model            propositional model
     * @param integerVariables included integer variables
     * @param context          the context
     * @param cf               the factory
     * @return the decoded assignment
     */
    public static CspAssignment decode(final Assignment model, final Collection<IntegerVariable> integerVariables,
                                       final OrderEncodingContext context, final CspFactory cf) {
        return decode(model, integerVariables, Collections.emptyList(), new IntegerVariableSubstitution(), context, cf);
    }

    /**
     * Decodes a problem that was encoded with the order encoding.
     * @param model   propositional model
     * @param csp     csp data structure
     * @param context the context
     * @param cf      the factory
     * @return the decoded assignment
     */
    public static CspAssignment decode(final Assignment model, final Csp csp, final OrderEncodingContext context,
                                       final CspFactory cf) {
        return decode(model, csp.getVisibleIntegerVariables(), csp.getVisibleBooleanVariables(),
                csp.getPropagateSubstitutions(), context, cf);
    }

    /**
     * Decodes a single integer variable. If the variable is not encoded in the model, it will return any valid value
     * of the variable.
     * @param var     the integer variable to decode
     * @param model   the propositional model
     * @param context the context
     * @return the decoded value
     */
    static int decodeIntVar(final IntegerVariable var, final Assignment model, final OrderEncodingContext context) {
        final IntegerDomain domain = var.getDomain();
        final int lb = domain.lb();
        final int ub = domain.ub();
        int value = ub;
        final Map<Integer, Variable> varMap = context.getVariableMap().get(var);
        if (varMap == null) {
            return value;
        }
        int index = 0;
        for (int c = lb; c < ub; c++) {
            if (domain.contains(c)) {
                final Variable satVar = varMap.get(index);
                if (satVar != null && model.positiveVariables().contains(satVar)) {
                    value = c;
                    break;
                }
                ++index;
            }
        }
        return value;
    }
}
