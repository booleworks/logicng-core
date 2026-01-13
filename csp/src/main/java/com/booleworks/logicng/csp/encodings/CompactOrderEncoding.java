// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.handlers.CspEvent;
import com.booleworks.logicng.csp.handlers.CspHandlerException;
import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.literals.EqMul;
import com.booleworks.logicng.csp.literals.OpAdd;
import com.booleworks.logicng.csp.literals.OpXY;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResult;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Functions for compact order encoding.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class CompactOrderEncoding {

    private final CompactOrderEncodingContext context;
    private final CompactOrderReduction reductionObject;
    private final OrderEncoding orderEncodingObject;

    /**
     * Constructs a new instance for compact order encoding.
     * @param context the encoding context
     * @param cf      the factory
     */
    public CompactOrderEncoding(final CompactOrderEncodingContext context, final CspFactory cf) {
        this.context = context;
        this.reductionObject = new CompactOrderReduction(context, cf);
        this.orderEncodingObject = new OrderEncoding(context.getOrderContext(), cf);
    }

    /**
     * Encodes a CSP problem using the compact order encoding.
     * <p>
     * Note: The destination of the encoding result may contain incomplete
     * results, if the computation was aborted by the handler.
     * @param csp     the problem
     * @param result  destination for the result
     * @param handler handler for processing encoding events
     * @return the passed encoding result if the computation was successful
     * otherwise returns the handler event that aborted the computation
     */
    public LngResult<EncodingResult> encode(final Csp csp, final EncodingResult result,
                                            final ComputationHandler handler) {
        if (!handler.shouldResume(CspEvent.CSP_ENCODING_STARTED)) {
            return LngResult.canceled(CspEvent.CSP_ENCODING_STARTED);
        }
        try {
            final ReductionResult reduction =
                    reductionObject.reduce(csp.getClauses(), csp.getInternalIntegerVariables(), handler);
            encodeIntern(reduction, result, handler);
        } catch (final CspHandlerException e) {
            return LngResult.canceled(e.getReason());
        }
        return LngResult.of(result);
    }

    /**
     * Encodes an integer variable using the compact order encoding.
     * <p>
     * Note: The destination of the encoding result may contain incomplete
     * results, if the computation was aborted by the handler.
     * @param v       the variable
     * @param result  destination for the result
     * @param handler handler for processing encoding events
     * @return the passed encoding result if the computation was successful
     * otherwise returns the handler event that aborted the computation
     */
    public LngResult<EncodingResult> encodeVariable(final IntegerVariable v, final EncodingResult result,
                                                    final ComputationHandler handler) {
        try {
            final ReductionResult reduction = reductionObject.reduceVariables(List.of(v), handler);
            encodeIntern(reduction, result, handler);
        } catch (final CspHandlerException e) {
            return LngResult.canceled(e.getReason());
        }
        return LngResult.of(result);
    }

    /**
     * Encodes a list of integer variables using the compact order encoding.
     * <p>
     * Note: The destination of the encoding result may contain incomplete
     * results, if the computation was aborted by the handler.
     * @param variables the variables
     * @param result    destination for the result
     * @param handler   handler for processing encoding events
     * @return the passed encoding result if the computation was successful
     * otherwise returns the handler event that aborted the computation
     */
    public LngResult<EncodingResult> encodeVariables(final Collection<IntegerVariable> variables,
                                                     final EncodingResult result, final ComputationHandler handler) {
        try {
            final ReductionResult reduction = reductionObject.reduceVariables(variables, handler);
            encodeIntern(reduction, result, handler);
        } catch (final CspHandlerException e) {
            return LngResult.canceled(e.getReason());
        }
        return LngResult.of(result);
    }

    /**
     * Encodes a set of arithmetic clauses using the compact order encoding.
     * <p>
     * Note: The destination of the encoding result may contain incomplete
     * results, if the computation was aborted by the handler.
     * @param clauses the arithmetic clauses
     * @param result  destination for the result
     * @param handler handler for processing encoding events
     * @return the passed encoding result if the computation was successful
     * otherwise returns the handler event that aborted the computation
     */
    public LngResult<EncodingResult> encodeClauses(final Set<IntegerClause> clauses,
                                                   final EncodingResult result,
                                                   final ComputationHandler handler) {
        try {
            final ReductionResult reduction = reductionObject.reduceClauses(clauses, handler);
            encodeIntern(reduction, result, handler);
        } catch (final CspHandlerException e) {
            return LngResult.canceled(e.getReason());
        }
        return LngResult.of(result);
    }

    private void encodeIntern(final ReductionResult reduction, final EncodingResult result,
                              final ComputationHandler handler)
            throws CspHandlerException {
        encodeVariablesIntern(reduction.getFrontierAuxiliaryVariables(), result, handler);
        encodeClausesIntern(reduction.getClauses(), result, handler);
    }

    private void encodeVariablesIntern(final List<IntegerVariable> variables, final EncodingResult result,
                                       final ComputationHandler handler) {
        for (final IntegerVariable v : variables) {
            assert context.getDigits(v) == null || context.getDigits(v).size() == 1;
            orderEncodingObject.encodeVariable(v, result, handler);
        }
    }

    private void encodeClausesIntern(final Set<IntegerClause> clauses, final EncodingResult result,
                                     final ComputationHandler handler)
            throws CspHandlerException {
        for (final IntegerClause c : clauses) {
            encodeClause(c, result, handler);
        }
    }

    private void encodeClause(final IntegerClause clause, final EncodingResult result, final ComputationHandler handler)
            throws CspHandlerException {
        orderEncodingObject.encodeClause(clause, result, handler);
    }

    /**
     * Returns whether an arithmetic literal is simple.
     * <p>
     * A literal is <I>simple</I> if it will encode as a single boolean
     * variable.
     * @param lit the arithmetic literal
     * @return {@code true} if the literal is simple
     */
    static boolean isSimpleLiteral(final ArithmeticLiteral lit, final CompactOrderEncodingContext context) {
        if (lit instanceof OpXY) {
            final OpXY l = (OpXY) lit;
            assert !l.getVariables().isEmpty();
            if (l.getOp() == OpXY.Operator.EQ) {
                return false;
            }
            return l.getVariables().size() == 1 && l.getUpperBound() < context.getBase();
        } else if (lit instanceof EqMul) {
            return false;
        } else if (lit instanceof OpAdd) {
            return false;
        }
        return OrderEncoding.isSimpleLiteral(lit);
    }

    /**
     * Returns whether an arithmetic clauses is simple.
     * <p>
     * A clause is <I>simple</I> if it contains at most one non-simple literal.
     * @param clause the clause
     * @return {@code true} if the clause is simple
     */
    static boolean isSimpleClause(final IntegerClause clause, final CompactOrderEncodingContext context) {
        return clause.size() - simpleClauseSize(clause, context) <= 1;
    }

    /**
     * Returns the number of simple literals (simple arithmetic literals and all
     * boolean literals).
     * @param clause the clause
     * @return number of simple literals
     */
    static int simpleClauseSize(final IntegerClause clause, final CompactOrderEncodingContext context) {
        int simpleLiterals = clause.getBoolLiterals().size();
        for (final ArithmeticLiteral lit : clause.getArithmeticLiterals()) {
            if (isSimpleLiteral(lit, context)) {
                ++simpleLiterals;
            }
        }
        return simpleLiterals;
    }
}
