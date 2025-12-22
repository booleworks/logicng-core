// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.datastructures.IntegerVariableSubstitution;
import com.booleworks.logicng.csp.functions.CspUtil;
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
 * Functions for decoding problems with the compact order
 * encoding.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class CompactOrderDecoding {
    private final CspFactory cf;
    private final CompactOrderEncodingContext context;

    /**
     * Constructs a new instance for compact order decoding.
     * @param context the encoding context
     * @param cf      the factory
     */
    public CompactOrderDecoding(final CompactOrderEncodingContext context, final CspFactory cf) {
        this.cf = cf;
        this.context = context;
    }

    /**
     * Decodes a problem that was encoded with the compact order encoding.
     * <p>
     * It takes a propositional model {@code model} and a list of integer and
     * boolean variables, which are the variables that should be decoded from
     * {@code model}. Variables not contained in the model will be assigned to
     * any valid value for this variable.
     * <p>
     * {@code propagateSubstitution} is used to resolve addition substitutions
     * that were not done by the encoding.
     * @param model                 propositional model
     * @param integerVariables      included integer variables
     * @param booleanVariables      included boolean variables
     * @param propagateSubstitution extern substitutions
     * @return the decoded assignment
     */
    public CspAssignment decode(final Assignment model, final Collection<IntegerVariable> integerVariables,
                                final Collection<Variable> booleanVariables,
                                final IntegerVariableSubstitution propagateSubstitution) {
        final CspAssignment result = new CspAssignment();
        final SortedSet<Variable> solverVariables = new TreeSet<>();
        solverVariables.addAll(model.positiveVariables());
        solverVariables.addAll(model.negativeVariables());
        final SortedSet<IntegerVariable> variablesOnSolver =
                CspUtil.getVariablesOnSolver(solverVariables, integerVariables, context);
        for (final IntegerVariable v : integerVariables) {
            if (variablesOnSolver.contains(v)) {
                final int value = decodeIntVar(propagateSubstitution.getOrSelf(v), model);
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
     * It takes a propositional model {@code model} and a list of integer and
     * boolean variables, which are the variables that should be decoded from
     * {@code model}. Variables not contained in the model will be assigned to
     * any valid value for this variable.
     * @param model            propositional model
     * @param integerVariables included integer variables
     * @param booleanVariables included boolean variables
     * @return the decoded assignment
     */
    public CspAssignment decode(final Assignment model, final Collection<IntegerVariable> integerVariables,
                                final Collection<Variable> booleanVariables) {
        return decode(model, integerVariables, booleanVariables, new IntegerVariableSubstitution());
    }

    /**
     * Decodes a problem that was encoded with the compact order encoding.
     * <p>
     * It takes a propositional model {@code model} and a list of integer
     * variables, which are the variables that should be decoded from
     * {@code model}. Variables not contained in the model will be assigned to
     * any valid value for this variable.
     * @param model            propositional model
     * @param integerVariables included integer variables
     * @return the decoded assignment
     */
    public CspAssignment decode(final Assignment model, final Collection<IntegerVariable> integerVariables) {
        return decode(model, integerVariables, Collections.emptyList(), new IntegerVariableSubstitution());
    }

    /**
     * Decodes a problem that was encoded with the compact order encoding.
     * @param model propositional model
     * @param csp   csp data structure
     * @return the decoded assignment
     */
    public CspAssignment decode(final Assignment model, final Csp csp) {
        return decode(model, csp.getVisibleIntegerVariables(), csp.getVisibleBooleanVariables(),
                csp.getPropagateSubstitutions());
    }

    private int decodeIntVar(final IntegerVariable var, final Assignment model) {
        if (context.isEncoded(var)) {
            final IntegerVariable adjusted = context.getAdjustedVariableOrSelf(var);
            final List<IntegerVariable> digits = context.getDigits(adjusted);
            assert Objects.nonNull(digits);
            return decodeBigIntVar(adjusted, model);
        } else {
            return var.getDomain().ub();
        }
    }

    private int decodeBigIntVar(final IntegerVariable var, final Assignment model) {
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
