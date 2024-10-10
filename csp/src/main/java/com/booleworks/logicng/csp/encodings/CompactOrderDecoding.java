package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.datastructures.IntegerVariableSubstitution;
import com.booleworks.logicng.csp.functions.IntegerVariablesFunction;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A class grouping functions for decoding problems with the compact order encoding.
 */
public class CompactOrderDecoding {
    private CompactOrderDecoding() {
    }

    /**
     * Decodes a problem that was encoded with the compact order encoding.
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
                                       final CompactOrderEncodingContext context,
                                       final CspFactory cf) {
        final CspAssignment result = new CspAssignment();
        final SortedSet<Variable> solverVariables = new TreeSet<>();
        solverVariables.addAll(model.positiveVariables());
        solverVariables.addAll(model.negativeVariables());
        final SortedSet<IntegerVariable> variablesOnSolver =
                IntegerVariablesFunction.getVariablesOnSolver(solverVariables, integerVariables, context);
        for (final IntegerVariable v : integerVariables) {
            if (variablesOnSolver.contains(v)) {
                final int value = decodeIntVar(propagateSubstitution.getOrSelf(v), model, context);
                result.addIntAssignment(v, value);
            } else {
                result.addIntAssignment(v, v.getDomain().ub());
            }
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
     * Decodes a problem that was encoded with the compact order encoding.
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
                                       final CompactOrderEncodingContext context, final CspFactory cf) {
        return decode(model, integerVariables, booleanVariables, new IntegerVariableSubstitution(), context, cf);
    }

    /**
     * Decodes a problem that was encoded with the compact order encoding.
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
                                       final CompactOrderEncodingContext context, final CspFactory cf) {
        return decode(model, integerVariables, Collections.emptyList(), new IntegerVariableSubstitution(), context, cf);
    }

    /**
     * Decodes a problem that was encoded with the compact order encoding.
     * @param model   propositional model
     * @param csp     csp data structure
     * @param context the context
     * @param cf      the factory
     * @return the decoded assignment
     */
    public static CspAssignment decode(final Assignment model, final Csp csp, final CompactOrderEncodingContext context,
                                       final CspFactory cf) {
        return decode(model, csp.getVisibleIntegerVariables(), csp.getVisibleBooleanVariables(),
                csp.getPropagateSubstitutions(), context, cf);
    }

    private static int decodeIntVar(final IntegerVariable var, final Assignment model,
                                    final CompactOrderEncodingContext context) {
        if (context.isEncoded(var)) {
            final IntegerVariable adjusted = context.getAdjustedVariableOrSelf(var);
            final List<IntegerVariable> digits = context.getDigits(adjusted);
            assert Objects.nonNull(digits);
            return decodeBigIntVar(adjusted, model, context);
        } else {
            return var.getDomain().ub();
        }
    }

    private static int decodeBigIntVar(final IntegerVariable var, final Assignment model,
                                       final CompactOrderEncodingContext context) {
        final List<IntegerVariable> digits = context.getDigits(var);
        assert digits != null;
        final int b = context.getBase();
        int dbase = 1;
        int value = context.hasOffset(var) ? context.getOffset(var) : 0;
        for (final IntegerVariable digit : digits) {
            final int d = OrderDecoding.decodeIntVar(digit, model, context.getOrderContext());
            value += dbase * d;
            dbase *= b;
        }
        return value;
    }
}
