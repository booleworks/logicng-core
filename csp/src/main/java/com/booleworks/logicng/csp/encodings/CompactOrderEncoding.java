// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.ReductionResult;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.handlers.CspEvent;
import com.booleworks.logicng.csp.handlers.CspHandlerException;
import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.literals.EqMul;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.literals.OpAdd;
import com.booleworks.logicng.csp.literals.OpXY;
import com.booleworks.logicng.csp.literals.ProductLiteral;
import com.booleworks.logicng.csp.literals.RCSPLiteral;
import com.booleworks.logicng.csp.terms.IntegerConstant;
import com.booleworks.logicng.csp.terms.IntegerHolder;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResult;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Functions for compact order encoding.
 * @version 3.0.0
 * @since 3.0.0
 */
public class CompactOrderEncoding {

    /**
     * Prefix for adjusted variables.
     */
    public static final String AUX_ADJUST = "COE_ADJUST";

    /**
     * Prefix for ternary auxiliary variables.
     */
    public static final String AUX_TERNARY = "COE_TERNARY";

    /**
     * Prefix for RCSP auxiliary variables.
     */
    public static final String AUX_RCSP = "COE_RCSP";

    /**
     * Prefix for CCSP auxiliary variables.
     */
    public static final String AUX_CCSP = "COE_CCSP";

    /**
     * Prefix for digits.
     */
    public static final String AUX_DIGIT = "COE_DIGIT";

    protected final CspFactory cf;
    protected final CompactOrderEncodingContext context;
    protected final OrderEncoding orderEncodingObject;

    /**
     * Constructs a new instance for compact order encoding.
     * @param context the encoding context
     * @param cf      the factory
     */
    public CompactOrderEncoding(final CompactOrderEncodingContext context, final CspFactory cf) {
        this.cf = cf;
        this.context = context;
        this.orderEncodingObject = new OrderEncoding(context.getOrderContext(), cf);
    }

    /**
     * Encodes a CSP problem using the compact order encoding.
     * <p>
     * Note: The destination of the encoding result may contain incomplete
     * results, if the computation was cancelled by the handler.
     * @param csp     the problem
     * @param result  destination for the result
     * @param handler handler for processing encoding events
     * @return the passed encoding result if the computation was successful
     * otherwise returns the handler event that cancelled the computation
     */
    public LngResult<EncodingResult> encode(final Csp csp, final EncodingResult result,
                                            final ComputationHandler handler) {
        if (!handler.shouldResume(CspEvent.CSP_ENCODING_STARTED)) {
            return LngResult.canceled(CspEvent.CSP_ENCODING_STARTED);
        }
        try {
            final ReductionResult reduction = reduce(csp.getClauses(), csp.getInternalIntegerVariables(), handler);
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
     * results, if the computation was cancelled by the handler.
     * @param v       the variable
     * @param result  destination for the result
     * @param handler handler for processing encoding events
     * @return the passed encoding result if the computation was successful
     * otherwise returns the handler event that cancelled the computation
     */
    public LngResult<EncodingResult> encodeVariable(final IntegerVariable v, final EncodingResult result,
                                                    final ComputationHandler handler) {
        try {
            final ReductionResult reduction = reduceVariables(List.of(v), handler);
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
     * results, if the computation was cancelled by the handler.
     * @param variables the variables
     * @param result    destination for the result
     * @param handler   handler for processing encoding events
     * @return the passed encoding result if the computation was successful
     * otherwise returns the handler event that cancelled the computation
     */
    public LngResult<EncodingResult> encodeVariables(final Collection<IntegerVariable> variables,
                                                     final EncodingResult result, final ComputationHandler handler) {
        try {
            final ReductionResult reduction = reduceVariables(variables, handler);
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
     * results, if the computation was cancelled by the handler.
     * @param clauses the arithmetic clauses
     * @param result  destination for the result
     * @param handler handler for processing encoding events
     * @return the passed encoding result if the computation was successful
     * otherwise returns the handler event that cancelled the computation
     */
    public LngResult<EncodingResult> encodeClauses(final Set<IntegerClause> clauses, final EncodingResult result,
                                                   final ComputationHandler handler) {
        try {
            final ReductionResult reduction = reduceClauses(clauses, handler);
            encodeIntern(reduction, result, handler);
        } catch (final CspHandlerException e) {
            return LngResult.canceled(e.getReason());
        }
        return LngResult.of(result);
    }

    protected void encodeIntern(final ReductionResult reduction, final EncodingResult result,
                                final ComputationHandler handler)
            throws CspHandlerException {
        encodeVariablesIntern(reduction.getFrontierAuxiliaryVariables(), result, handler);
        encodeClausesIntern(reduction.getClauses(), result, handler);
    }

    protected void encodeVariablesIntern(final List<IntegerVariable> variables, final EncodingResult result,
                                         final ComputationHandler handler) {
        for (final IntegerVariable v : variables) {
            assert context.getDigits(v) == null || context.getDigits(v).size() == 1;
            orderEncodingObject.encodeVariable(v, result, handler);
        }
    }

    protected void encodeClausesIntern(final Set<IntegerClause> clauses, final EncodingResult result,
                                       final ComputationHandler handler)
            throws CspHandlerException {
        for (final IntegerClause c : clauses) {
            encodeClause(c, result, handler);
        }
    }

    protected void encodeClause(final IntegerClause clause, final EncodingResult result,
                                final ComputationHandler handler)
            throws CspHandlerException {
        orderEncodingObject.encodeClause(clause, result, handler);
    }

    /**
     * Reduces a set of arithmetic clauses so that it can be encoded with the
     * compact order encoding.
     * @param clauses          the clauses
     * @param integerVariables the integer variables
     * @param handler          handler for processing encoding events
     * @return the reduced problem
     * @throws CspHandlerException if the computation was cancelled by the
     *                             handler
     */
    protected ReductionResult reduce(final Set<IntegerClause> clauses, final Set<IntegerVariable> integerVariables,
                                     final ComputationHandler handler) throws CspHandlerException {
        final ReductionResult resultVars = reduceVariables(integerVariables, handler);
        final ReductionResult resultClauses = reduceClauses(clauses, handler);
        return ReductionResult.merge(List.of(resultVars, resultClauses));
    }

    protected ReductionResult reduceVariables(final Collection<IntegerVariable> variables,
                                              final ComputationHandler handler)
            throws CspHandlerException {
        final ReductionResult resultAdjust = new ReductionResult(new LinkedHashSet<>(), new ArrayList<>());
        for (final IntegerVariable v : variables) {
            adjustVariable(v, resultAdjust, handler);
        }
        final ReductionResult resultCcsp = variablesToCCSP(resultAdjust.getFrontierAuxiliaryVariables(), handler);
        final ReductionResult resultClauses = reduceClauses(resultAdjust.getClauses(), handler);

        return ReductionResult.merge(List.of(resultCcsp, resultClauses));
    }

    protected ReductionResult reduceClauses(final Set<IntegerClause> clauses, final ComputationHandler handler)
            throws CspHandlerException {
        final ReductionResult adjustedResult = adjustClauses(clauses);
        final ReductionResult toTernaryResult = toTernary(adjustedResult.getClauses(), handler);
        final ReductionResult toRcspResult = toRcsp(toTernaryResult.getClauses());
        final Set<IntegerClause> simplificationResult = simplify(toRcspResult.getClauses(), handler);

        final int size = adjustedResult.getFrontierAuxiliaryVariables().size()
                + toTernaryResult.getFrontierAuxiliaryVariables().size()
                + toRcspResult.getFrontierAuxiliaryVariables().size();
        final List<IntegerVariable> currentVariables = new ArrayList<>(size);
        currentVariables.addAll(adjustedResult.getFrontierAuxiliaryVariables());
        currentVariables.addAll(toTernaryResult.getFrontierAuxiliaryVariables());
        currentVariables.addAll(toRcspResult.getFrontierAuxiliaryVariables());
        return toCCSP(simplificationResult, currentVariables, handler);
    }

    protected ReductionResult adjustClauses(final Set<IntegerClause> clauses) {
        final Set<IntegerClause> newClauses = new LinkedHashSet<>();
        final List<IntegerVariable> auxiliaryVariables = new ArrayList<>();
        for (final IntegerClause c : clauses) {
            if (c.getArithmeticLiterals().isEmpty()) {
                newClauses.add(c);
            } else {
                final IntegerClause.Builder newClause = IntegerClause.cloneOnlyBool(c);
                for (final ArithmeticLiteral lit : c.getArithmeticLiterals()) {
                    if (lit instanceof LinearLiteral) {
                        final LinearLiteral ll =
                                ((LinearLiteral) lit).substitute(context.getAdjustedVariablesSubstitution());
                        final LinearExpression ls = ll.getSum();
                        int b = ls.getB();
                        for (final Map.Entry<IntegerVariable, Integer> es : ls.getCoef().entrySet()) {
                            b += context.getOffset(es.getKey()) * es.getValue();
                        }
                        final LinearExpression newLs = LinearExpression.builder(ls).setB(b).build();
                        final LinearLiteral newLl = new LinearLiteral(newLs, ll.getOperator());
                        newClause.addArithmeticLiteral(newLl);
                    } else if (lit instanceof ProductLiteral) {
                        final ProductLiteral pl =
                                ((ProductLiteral) lit).substitute(context.getAdjustedVariablesSubstitution());
                        final IntegerVariable z = pl.getV();
                        final IntegerVariable x = pl.getV1();
                        final IntegerVariable y = pl.getV2();
                        final int zoffset = context.getOffset(z);
                        final int xoffset = context.getOffset(x);
                        final int yoffset = context.getOffset(y);
                        if (zoffset == 0 && xoffset == 0 && yoffset == 0) {
                            newClause.addArithmeticLiteral(pl);
                        } else {
							/*
								(z+zoffset) = (x+xoffset)(y+yoffset)
								z = p+yoffset*x+xoffset*y+xoffset*yoffset-zoffset
								--> -z + p + yoffset*x + xoffset*y + xoffset*yoffset-zoffset = 0
								p = xy --> p=xy
							*/
                            final IntegerDomain xdom = x.getDomain();
                            final IntegerDomain ydom = y.getDomain();
                            final LinearExpression.Builder ls =
                                    LinearExpression.builder(xoffset * yoffset - zoffset);
                            ls.setA(-1, z);
                            final IntegerDomain pdom = IntegerDomain.of(0, xdom.mul(ydom).ub());
                            final IntegerVariable p = context.newAdjustedVariable(AUX_ADJUST, pdom, cf);
                            auxiliaryVariables.add(p);
                            ls.setA(1, p);
                            ls.setA(yoffset, x);
                            ls.setA(xoffset, y);
                            newClause.addArithmeticLiteral(new LinearLiteral(ls.build(), LinearLiteral.Operator.EQ));
                            newClauses.add(new IntegerClause(new ProductLiteral(p, x, y)));
                        }
                    }
                }
                newClauses.add(newClause.build());
            }
        }
        return new ReductionResult(newClauses, auxiliaryVariables);
    }

    protected void adjustVariable(final IntegerVariable v, final ReductionResult destination,
                                  final ComputationHandler handler) throws CspHandlerException {
        final IntegerVariable adjustedVar;
        if (context.hasAdjustedVariable(v)) {
            adjustedVar = context.getAdjustedVariable(v);
        } else {
            adjustedVar =
                    createAdjustedVariable(v.getDomain(), AUX_ADJUST, true, destination.getClauses(), handler);
            context.addAdjustedVariable(v, adjustedVar);
        }
        destination.getFrontierAuxiliaryVariables().add(adjustedVar);
    }

    protected IntegerVariable createAdjustedVariable(final IntegerDomain d, final String prefix,
                                                     final boolean useOffset,
                                                     final Set<IntegerClause> additionalClauses,
                                                     final ComputationHandler handler)
            throws CspHandlerException {
        final int offset = d.lb();
        final IntegerVariable newVar;
        if (useOffset) {
            final IntegerDomain newD = IntegerDomain.of(0, d.ub() - offset);
            newVar = context.newAdjustedVariable(prefix, newD, cf);
        } else {
            final IntegerDomain newD = IntegerDomain.of(0, d.ub());
            newVar = context.newAdjustedVariable(prefix, newD, cf);
            final IntegerClause c = new IntegerClause(
                    new LinearLiteral(new LinearExpression(-1, newVar, offset), LinearLiteral.Operator.LE));
            additionalClauses.add(c);
        }
        context.addOffset(newVar, offset);

        if (!d.isContiguous()) {
            int lst = d.lb() - 1;
            final Iterator<Integer> iter = d.iterator();
            while (iter.hasNext()) {
                final int i = iter.next();
                if (lst + 2 == i) {
                    final IntegerClause c = new IntegerClause(
                            new LinearLiteral(new LinearExpression(1, newVar, -(lst + 1)), LinearLiteral.Operator.NE));
                    additionalClauses.add(c);
                } else if (lst + 1 != i) {
                    final Variable b = context.newAdjustedBoolVariable(cf.getFormulaFactory(), handler);
                    final IntegerClause clause1 = new IntegerClause(
                            b.negate(cf.getFormulaFactory()),
                            new LinearLiteral(new LinearExpression(1, newVar, -lst), LinearLiteral.Operator.LE)
                    );
                    additionalClauses.add(clause1);

                    final IntegerClause clause2 = new IntegerClause(
                            b,
                            new LinearLiteral(new LinearExpression(-1, newVar, i), LinearLiteral.Operator.LE)
                    );
                    additionalClauses.add(clause2);
                }
                lst = i;
            }
        }
        return newVar;
    }

    protected ReductionResult toTernary(final Set<IntegerClause> clauses, final ComputationHandler handler)
            throws CspHandlerException {
        final Set<IntegerClause> newClauses = new LinkedHashSet<>();
        final List<IntegerVariable> auxiliaryVariables = new ArrayList<>();
        for (final IntegerClause c : clauses) {
            final IntegerClause.Builder newClause = IntegerClause.cloneOnlyBool(c);
            for (final ArithmeticLiteral lit : c.getArithmeticLiterals()) {
                if (lit instanceof LinearLiteral) {
                    final LinearLiteral ll = (LinearLiteral) lit;
                    if (ll.getSum().size() > 3) {
                        final LinearExpression.Builder ls =
                                simplifyToTernary(LinearExpression.builder(ll.getSum()), newClauses,
                                        auxiliaryVariables, handler);
                        newClause.addArithmeticLiteral(new LinearLiteral(ls.build(), ll.getOperator()));
                    } else {
                        newClause.addArithmeticLiteral(ll);
                    }
                } else {
                    newClause.addArithmeticLiteral(lit);
                }
            }
            newClauses.add(newClause.build());
        }
        return new ReductionResult(newClauses, auxiliaryVariables);
    }

    protected LinearExpression.Builder simplifyToTernary(final LinearExpression.Builder exp,
                                                         final Set<IntegerClause> clauses,
                                                         final List<IntegerVariable> auxiliaryVariables,
                                                         final ComputationHandler handler)
            throws CspHandlerException {
        if (exp.size() <= 3) {
            return exp;
        }
        final LinearExpression.Builder lhs = LinearExpression.builder(0);
        final LinearExpression.Builder rhs = LinearExpression.builder(0);
        for (final IntegerVariable v : exp.getVariables()) {
            final int a = exp.getA(v);
            if (a > 0) {
                lhs.setA(a, v);
            } else {
                rhs.setA(-a, v);
            }
        }
        final int b = exp.getB();
        final int rest = b == 0 ? 3 : 2;
        int lhs_len = 0, rhs_len = 0;
        if (lhs.size() == 0) {
            rhs_len = rest;
        } else if (rhs.size() == 0) {
            lhs_len = rest;
        } else if (lhs.getDomain().size() < rhs.getDomain().size()) {
            lhs_len = 1;
            rhs_len = rest - 1;
        } else {
            rhs_len = 1;
            lhs_len = rest - 1;
        }

        final LinearExpression.Builder e = LinearExpression.builder(b);
        for (final LinearExpression.Builder ei : OrderEncoding.split(lhs.build(), lhs_len)) {
            final LinearExpression.Builder simplified =
                    simplifyToTernaryExpression(ei, clauses, auxiliaryVariables, handler);
            e.add(simplified.build());
        }

        for (final LinearExpression.Builder ei : OrderEncoding.split(rhs.build(), rhs_len)) {
            final LinearExpression.Builder simplified =
                    simplifyToTernaryExpression(ei, clauses, auxiliaryVariables, handler);
            simplified.multiply(-1);
            e.add(simplified.build());
        }

        return e;
    }

    protected LinearExpression.Builder simplifyToTernaryExpression(final LinearExpression.Builder exp,
                                                                   final Set<IntegerClause> clauses,
                                                                   final List<IntegerVariable> auxiliaryVariables,
                                                                   final ComputationHandler handler)
            throws CspHandlerException {
        final int factor = exp.factor();
        final LinearExpression.Builder normalized = exp.normalize();
        LinearExpression.Builder simplified =
                simplifyToTernary(normalized, clauses, auxiliaryVariables, handler);
        if (simplified.size() > 1) {
            final IntegerVariable v =
                    createAdjustedVariable(simplified.getDomain(), AUX_TERNARY, false, clauses, handler);
            auxiliaryVariables.add(v);
            final LinearExpression.Builder ls = LinearExpression.builder(v);
            ls.subtract(simplified.build());
            simplified = LinearExpression.builder(v);
            final LinearLiteral ll = new LinearLiteral(ls.build(), LinearLiteral.Operator.EQ);
            final IntegerClause clause = new IntegerClause(ll);
            clauses.add(clause);
        }
        if (factor > 1) {
            simplified.multiply(factor);
        }
        return simplified;
    }

    protected ReductionResult toRcsp(final Set<IntegerClause> clauses) {
        final Set<IntegerClause> newClauses = new LinkedHashSet<>();
        final List<IntegerVariable> auxiliaryVariables = new ArrayList<>();
        for (final IntegerClause c : clauses) {
            if (c.getArithmeticLiterals().isEmpty()) {
                newClauses.add(c);
            } else {
                final IntegerClause.Builder newClause = IntegerClause.cloneOnlyBool(c);
                for (final ArithmeticLiteral al : c.getArithmeticLiterals()) {
                    if (al instanceof LinearLiteral) {
                        final LinearLiteral ll = (LinearLiteral) al;
                        final LinearExpression ls = ll.getSum();
                        if (ll.getOperator() == LinearLiteral.Operator.EQ && ls.size() == 2 && ls.getB() == 0) {
                            final IntegerVariable v1 = ls.getCoef().firstKey();
                            final IntegerVariable v2 = ls.getCoef().lastKey();
                            final int c1 = ls.getA(v1);
                            final int c2 = ls.getA(v2);
                            if (c1 * c2 < 0) {
                                IntegerVariable lhs = Math.abs(c1) < Math.abs(c2) ? v1 : v2;
                                final IntegerVariable rhs = Math.abs(c1) < Math.abs(c2) ? v2 : v1;
                                final int lc = Math.abs(ls.getA(lhs));
                                final int rc = Math.abs(ls.getA(rhs));
                                if (lc > 1) {
                                    final IntegerDomain dom = lhs.getDomain().mul(lc);
                                    final IntegerVariable av = context.newRCSPVariable(dom, cf);
                                    auxiliaryVariables.add(av);
                                    final ArithmeticLiteral lit = new EqMul(av, cf.constant(lc), lhs);
                                    newClauses.add(new IntegerClause(lit));
                                    lhs = av;
                                }
                                if (rc == 1) {
                                    newClause.addArithmeticLiteral(new OpXY(OpXY.Operator.EQ, lhs, rhs));
                                } else {
                                    newClause.addArithmeticLiterals(new EqMul(lhs, cf.constant(rc), rhs));
                                }
                                continue;
                            }
                        } else if (ll.getOperator() == LinearLiteral.Operator.EQ && ls.size() == 1) {
                            final IntegerVariable x = ls.getCoef().firstKey();
                            int a = ls.getA(x);
                            int b = ls.getB();
                            if (a * b <= 0) {
                                a = Math.abs(a);
                                b = Math.abs(b);
                                if (a == 1) {
                                    newClause.addArithmeticLiterals(new OpXY(OpXY.Operator.EQ, x, cf.constant(b)));
                                } else {
                                    newClause.addArithmeticLiterals(new EqMul(cf.constant(b), cf.constant(a), x));
                                }
                                continue;
                            }
                        }
                        LinearExpression.Builder lhs, rhs;
                        if (ls.getB() > 0) {
                            lhs = LinearExpression.builder(ls.getB());
                            rhs = LinearExpression.builder(0);
                        } else {
                            lhs = LinearExpression.builder(0);
                            rhs = LinearExpression.builder(-ls.getB());
                        }
                        for (final Map.Entry<IntegerVariable, Integer> es : ls.getCoef().entrySet()) {
                            int a = es.getValue();
                            final IntegerVariable v = es.getKey();
                            if (a == 1) {
                                lhs.setA(1, v);
                                continue;
                            } else if (a == -1) {
                                rhs.setA(1, v);
                                continue;
                            }
                            a = Math.abs(a);
                            assert v.getDomain().lb() == 0;
                            final IntegerDomain dom = v.getDomain().mul(a);
                            final IntegerVariable av = context.newRCSPVariable(dom, cf);
                            auxiliaryVariables.add(av);
                            final ArithmeticLiteral lit = new EqMul(av, cf.constant(a), v);
                            newClauses.add(new IntegerClause(lit));
                            if (es.getValue() > 0) {
                                lhs.add(new LinearExpression(av));
                            } else {
                                rhs.add(new LinearExpression(av));
                            }
                        }

                        int lsize = lhs.size() + (lhs.getB() == 0 ? 0 : 1);
                        int rsize = rhs.size() + (rhs.getB() == 0 ? 0 : 1);
                        final LinearLiteral.Operator op = ll.getOperator();
                        boolean invert = false;
                        if (lsize > rsize) {
                            final LinearExpression.Builder tmp = lhs;
                            lhs = rhs;
                            rhs = tmp;
                            invert = true;
                            final int tmpsize = lsize;
                            lsize = rsize;
                            rsize = tmpsize;
                        }
                        assert lsize <= rsize;
                        assert lsize <= 2;
                        assert rsize <= 4;

                        if (rsize >= 3) {
                            rhs = simplifyForRCSP(rhs, newClauses, 2, auxiliaryVariables);
                        } else if (rsize == 2 && lsize == 2) {
                            if (rhs.getB() == 0) {
                                rhs = simplifyForRCSP(rhs, newClauses, 1, auxiliaryVariables);
                            } else {
                                final IntegerDomain dom = IntegerDomain.of(0, rhs.getDomain().ub());
                                final List<IntegerHolder> rh = getHolders(rhs);
                                final IntegerVariable ax = context.newRCSPVariable(dom, cf);
                                auxiliaryVariables.add(ax);
                                final ArithmeticLiteral geB = new OpXY(OpXY.Operator.LE, cf.constant(rhs.getB()), ax);
                                final ArithmeticLiteral eqAdd = new OpAdd(OpAdd.Operator.EQ, ax, rh.get(0), rh.get(1));
                                newClauses.add(new IntegerClause(geB));
                                newClauses.add(new IntegerClause(eqAdd));
                                rhs = LinearExpression.builder(ax);
                            }
                        }

                        final List<IntegerHolder> lh = getHolders(lhs);
                        final List<IntegerHolder> rh = getHolders(rhs);
                        assert lh.size() + rh.size() <= 3;

                        ArithmeticLiteral lit = null;
                        if (lh.size() == 1 && rh.size() == 1) {
                            lit = new OpXY(OpXY.Operator.from(op), lh.get(0), rh.get(0), invert);
                        } else if (lh.size() == 1 && rh.size() == 2) {
                            lit = new OpAdd(OpAdd.Operator.from(op, invert), lh.get(0), rh.get(0), rh.get(1));
                        } else if (lh.size() == 2 && rh.size() == 1) {
                            if (op == LinearLiteral.Operator.LE && !invert) {
                                lit = new OpAdd(OpAdd.Operator.GE, rh.get(0), lh.get(0), lh.get(1));
                            } else if (op == LinearLiteral.Operator.LE && invert) {
                                lit = new OpAdd(OpAdd.Operator.LE, rh.get(0), lh.get(0), lh.get(1));
                            } else {
                                lit = new OpAdd(OpAdd.Operator.from(op, invert), rh.get(0), lh.get(0), lh.get(1));
                            }
                        }
                        if (lit != null) {
                            newClause.addArithmeticLiterals(lit);
                        }
                    } else {
                        assert al instanceof ProductLiteral;
                        final ProductLiteral pl = (ProductLiteral) al;
                        newClause.addArithmeticLiteral(new EqMul(pl.getV(), pl.getV1(), pl.getV2()));
                    }
                }
                newClauses.add(newClause.build());
            }
        }
        return new ReductionResult(newClauses, auxiliaryVariables);
    }

    protected LinearExpression.Builder simplifyForRCSP(final LinearExpression.Builder e,
                                                       final Set<IntegerClause> clauses, final int maxlen,
                                                       final List<IntegerVariable> auxiliaryVariables) {
        final int esize = e.size() + (e.getB() == 0 ? 0 : 1);
        if (esize <= maxlen) {
            return e;
        }
        assert (esize == 4 && maxlen == 2) || (esize == 3 && maxlen == 2) || (esize == 2 && maxlen == 1);
        final List<IntegerHolder> holders = getHolders(e);
        assert holders.size() <= 4;
        Collections.sort(holders);

        final IntegerHolder v0 = holders.get(0);
        final IntegerHolder v1 = holders.get(1);
        final IntegerDomain d = v0.getDomain().add(v1.getDomain());
        final IntegerVariable w0 = context.newRCSPVariable(d, cf);
        auxiliaryVariables.add(w0);
        final ArithmeticLiteral lit0 = new OpAdd(OpAdd.Operator.EQ, w0, v0, v1);
        final IntegerClause clause0 = new IntegerClause(lit0);
        clauses.add(clause0);

        if (holders.size() == 2) {
            return LinearExpression.builder(w0);
        } else if (holders.size() == 3) {
            final LinearExpression.Builder ret = LinearExpression.builder(w0);
            final IntegerHolder v2 = holders.get(2);
            if (v2 instanceof IntegerConstant) {
                ret.setB(((IntegerConstant) v2).getValue());
            } else {
                ret.setA(1, (IntegerVariable) v2);
            }
            return ret;
        } else {
            assert holders.size() == 4;
            final IntegerHolder v2 = holders.get(2);
            final IntegerHolder v3 = holders.get(3);
            final IntegerDomain d2 = v2.getDomain().add(v3.getDomain());
            final IntegerVariable w1 = context.newRCSPVariable(d2, cf);
            auxiliaryVariables.add(w1);
            final ArithmeticLiteral lit1 = new OpAdd(OpAdd.Operator.EQ, w1, v2, v3);
            final IntegerClause clause1 = new IntegerClause(lit1);
            clauses.add(clause1);
            final LinearExpression.Builder ret = LinearExpression.builder(w0);
            ret.setA(1, w1);
            return ret;
        }
    }

    protected List<IntegerHolder> getHolders(final LinearExpression.Builder e) {
        final List<IntegerHolder> ret = new ArrayList<>(e.getVariables());
        if (e.size() == 0 || e.getB() > 0) {
            ret.add(cf.constant(e.getB()));
        }
        return ret;
    }

    protected Set<IntegerClause> simplify(final Set<IntegerClause> clauses, final ComputationHandler handler)
            throws CspHandlerException {
        final LinkedHashSet<IntegerClause> newClauses = new LinkedHashSet<>();
        for (final IntegerClause clause : clauses) {
            if (clause.isValid()) {
            } else if (CompactOrderEncoding.isSimpleClause(clause, context)) {
                newClauses.add(clause);
            } else {
                newClauses.addAll(simplifyClause(clause, handler));
            }
        }
        return newClauses;
    }

    /**
     * Reduces a set of arithmetic clauses with RCSP literals to clauses with
     * CCSP literals.
     * @param clauses   RCSP clauses
     * @param variables all variables used in {@code clauses}
     * @param handler   handler for processing encoding events
     * @return reduced clauses and relevant variables
     * @throws CspHandlerException if the computation is cancelled by the
     *                             handler
     */
    protected ReductionResult toCCSP(final Set<IntegerClause> clauses, final List<IntegerVariable> variables,
                                     final ComputationHandler handler) throws CspHandlerException {
        final ReductionResult result = variablesToCCSP(variables, handler);
        for (final IntegerClause clause : clauses) {
            if (clause.getArithmeticLiterals().isEmpty()) {
                result.getClauses().add(clause);
            } else {
                assert clause.size() - CompactOrderEncoding.simpleClauseSize(clause, context) <= 1;

                final Set<ArithmeticLiteral> simpleLiterals = new LinkedHashSet<>();
                final Set<IntegerClause> ccspClauses = new LinkedHashSet<>();
                for (final ArithmeticLiteral al : clause.getArithmeticLiterals()) {
                    final RCSPLiteral ll = (RCSPLiteral) al;
                    final Set<IntegerClause> ccsp =
                            convertToCCSP(ll, result.getFrontierAuxiliaryVariables(), context, handler);
                    if (CompactOrderEncoding.isSimpleLiteral(ll, context)) {
                        assert ccsp.size() == 1;
                        final IntegerClause c = ccsp.iterator().next();
                        assert c.getBoolLiterals().isEmpty();
                        simpleLiterals.addAll(c.getArithmeticLiterals());
                    } else {
                        assert ccspClauses.isEmpty();
                        ccspClauses.addAll(ccsp);
                    }
                }
                if (ccspClauses.isEmpty()) {
                    result.getClauses().add(new IntegerClause(clause.getBoolLiterals(), simpleLiterals));
                } else {
                    for (final IntegerClause c : ccspClauses) {
                        final IntegerClause.Builder newC = IntegerClause.clone(c);
                        newC.addBooleanLiterals(clause.getBoolLiterals());
                        newC.addArithmeticLiterals(simpleLiterals);
                        result.getClauses().add(newC.build());
                    }
                }
            }
        }
        return result;
    }

    protected ReductionResult variablesToCCSP(final Collection<IntegerVariable> variables,
                                              final ComputationHandler handler) throws CspHandlerException {
        final Set<IntegerClause> newClauses = new LinkedHashSet<>();
        final List<IntegerVariable> frontierVariables = new ArrayList<>();
        for (final IntegerVariable v : variables) {
            final List<IntegerVariable> digits;
            if (context.hasDigits(v)) {
                digits = context.getDigits(v);
            } else {
                digits = splitToDigits(v);
                context.addDigits(v, digits);
            }
            frontierVariables.addAll(digits);
            final int lb = v.getDomain().lb();
            final int ub = v.getDomain().ub();
            final int m = context.getDigits(v).size();
            if (m > 1 || ub <= Math.pow(context.getBase(), m) - 1) {
                newClauses.addAll(convertOpXYToCCSP(new OpXY(OpXY.Operator.LE, v, cf.constant(ub)), context, handler));
            }
            if (m > 1 && lb != 0) {
                newClauses.addAll(convertOpXYToCCSP(new OpXY(OpXY.Operator.LE, cf.constant(lb), v), context, handler));
            }
        }
        return new ReductionResult(newClauses, frontierVariables);
    }

    protected Set<IntegerClause> convertToCCSP(final RCSPLiteral literal,
                                               final List<IntegerVariable> frontierVariables,
                                               final CompactOrderEncodingContext context,
                                               final ComputationHandler handler)
            throws CspHandlerException {
        if (literal instanceof EqMul) {
            return convertEqMulToCCSP((EqMul) literal, frontierVariables, context, handler);
        } else if (literal instanceof OpAdd) {
            return convertOpAddToCCSP((OpAdd) literal, frontierVariables, context, handler);
        } else if (literal instanceof OpXY) {
            return convertOpXYToCCSP((OpXY) literal, context, handler);
        } else {
            throw new RuntimeException("Unknown RCSP Literal: " + literal.getClass());
        }
    }

    protected Set<IntegerClause> convertOpXYToCCSP(final OpXY lit, final CompactOrderEncodingContext context,
                                                   final ComputationHandler handler)
            throws CspHandlerException {
        final Set<IntegerClause> ret = new LinkedHashSet<>();
        final IntegerHolder x = lit.getX();
        final IntegerHolder y = lit.getY();
        final int m = Math.max(nDigits(x), nDigits(y));

        switch (lit.getOp()) {
            case LE:
                if (x instanceof IntegerConstant || y instanceof IntegerConstant) {
                    for (int i = 0; i < m; ++i) {
                        final IntegerClause.Builder newClause = IntegerClause.builder();
                        newClause.addArithmeticLiteral(le(nth(x, i), nth(y, i)));
                        for (int j = i + 1; j < m; ++j) {
                            newClause.addArithmeticLiteral(le(nth(x, j), sub(nth(y, j), 1)));
                        }
                        ret.add(newClause.build());
                    }
                } else {
                    final Variable[] s = new Variable[m];
                    for (int i = 1; i < m; ++i) {
                        s[i] = context.newCCSPBoolVariable(cf.getFormulaFactory(), handler);
                    }
                    // -s(i+1) or x(i) <= y(i) (when 0 <= i < m - 1)
                    for (int i = 0; i < m - 1; ++i) {
                        ret.add(new IntegerClause(
                                s[i + 1].negate(cf.getFormulaFactory()),
                                le(nth(x, i), nth(y, i))
                        ));
                    }
                    // x(i) <= y(i) (when i == m - 1)
                    ret.add(new IntegerClause(le(nth(x, m - 1), nth(y, m - 1))));

                    // -s(i+1) or (x(i) <= y(i) - 1) or s(i) (when 1 <= i < m - 1)
                    for (int i = 1; i < m - 1; ++i) {
                        final IntegerClause.Builder newClause = IntegerClause.builder();
                        newClause.addBooleanLiterals(s[i + 1].negate(cf.getFormulaFactory()), s[i]);
                        newClause.addArithmeticLiteral(le(nth(x, i), sub(nth(y, i), 1)));
                        ret.add(newClause.build());
                    }
                    if (m > 1) {
                        // (x(i) <= y(i) - 1) or s(i) (when i == m - 1)
                        ret.add(new IntegerClause(
                                s[m - 1],
                                le(nth(x, m - 1), sub(nth(y, m - 1), 1))
                        ));
                    }
                }
                break;
            case EQ:
                for (int i = 0; i < m; ++i) {
                    ret.add(new IntegerClause(le(nth(x, i), nth(y, i))));
                    ret.add(new IntegerClause(ge(nth(x, i), nth(y, i))));
                }
                break;
            case NE:
                final IntegerClause.Builder newClause = IntegerClause.builder();
                for (int i = 0; i < m; ++i) {
                    newClause.addArithmeticLiterals(le(nth(x, i), sub(nth(y, i), 1)));
                    newClause.addArithmeticLiterals(ge(sub(nth(x, i), 1), nth(y, i)));
                }
                ret.addAll(simplifyClause(newClause.build(), handler));
                break;
        }
        return ret;
    }

    protected Set<IntegerClause> convertOpAddToCCSP(final OpAdd lit, final List<IntegerVariable> frontierVariables,
                                                    final CompactOrderEncodingContext context,
                                                    final ComputationHandler handler)
            throws CspHandlerException {
        final Set<IntegerClause> ret = new LinkedHashSet<>();
        final int b = context.getBase();
        final IntegerHolder x = lit.getX();
        final IntegerHolder y = lit.getY();
        final IntegerHolder z = lit.getZ();
        final int m = Math.max(Math.max(nDigits(x), nDigits(y)), nDigits(z));
        final LinearExpression[] c = new LinearExpression[m];

        for (int i = 1; i < m; ++i) {
            c[i] = new LinearExpression(newCCSPVariable(IntegerDomain.of(0, 1), frontierVariables));
        }

        // lhs = { z_0 + c_1 * b, ..., z_{m-1} }
        final LinearExpression[] lhs = new LinearExpression[m];
        for (int i = 0; i < m - 1; ++i) {
            lhs[i] = add(nth(z, i), mul(c[i + 1], b));
        }
        lhs[m - 1] = nth(z, m - 1);

        // rhs = { x_0 + y_0, x_1 + y_1 + c_1, ... }
        final LinearExpression[] rhs = new LinearExpression[m];
        rhs[0] = add(nth(x, 0), nth(y, 0));
        for (int i = 1; i < m; ++i) {
            rhs[i] = add(nth(x, i), nth(y, i), c[i]);
        }

        switch (lit.getOp()) {
            case LE: {
                final Variable[] s = new Variable[m];
                for (int i = 1; i < m; ++i) {
                    s[i] = context.newCCSPBoolVariable(cf.getFormulaFactory(), handler);
                }

                // -s(i+1) or (z(i) + B*c(i+1) <= x(i) + y(i) + c(i)) (when 0 <= i < m - 1)
                for (int i = 0; i < m - 1; ++i) {
                    ret.add(new IntegerClause(
                            s[i + 1].negate(cf.getFormulaFactory()),
                            le(lhs[i], rhs[i])
                    ));
                }
                //z(i) <= x(i) + y(i) + c(i) (when i == m - 1)
                ret.add(new IntegerClause(le(lhs[m - 1], rhs[m - 1])));

                // -s(i+1) or (z(i) + B * c(i + 1) <= x(i) + y(i) + c(i) - 1) or s(i)
                // (when 1 <= i < m - 1)
                for (int i = 1; i < m - 1; ++i) {
                    final IntegerClause.Builder newClause = IntegerClause.builder();
                    newClause.addBooleanLiterals(s[i + 1].negate(cf.getFormulaFactory()), s[i]);
                    newClause.addArithmeticLiteral(le(lhs[i], sub(rhs[i], 1)));
                    ret.add(newClause.build());
                }
                // (z(i) <= x(i) + y(i) + c(i) - 1) or s(i) (when i == m - 1)
                if (m > 1) {
                    ret.add(new IntegerClause(s[m - 1], le(lhs[m - 1], sub(rhs[m - 1], 1))));
                }

                for (int i = 0; i < m - 1; ++i) {
                    //c(i+1) <= 0 or x(i) + y(i) + c(i) >= B
                    ret.add(new IntegerClause(le(c[i + 1], 0), ge(rhs[i], b)));
                    ret.add(new IntegerClause(ge(c[i + 1], 1), le(rhs[i], b - 1)));
                }
                break;
            }
            case GE: {
                final Variable[] s = new Variable[m];
                for (int i = 1; i < m; i++) {
                    s[i] = context.newCCSPBoolVariable(cf.getFormulaFactory(), handler);
                }

                // -s(i+1) or (z(i) + B*c(i+1) <= x(i) + y(i) + c(i)) (when 0 <= i < m - 1)
                for (int i = 0; i < m - 1; i++) {
                    ret.add(new IntegerClause(s[i + 1].negate(cf.getFormulaFactory()), ge(lhs[i], rhs[i])));
                }
                // z(i) >= x(i) + y(i) + c(i) (when i == m - 1)
                ret.add(new IntegerClause(ge(lhs[m - 1], rhs[m - 1])));

                // -s(i+1) or (z(i) + B * c(i+1) <= x(i) + y(i) + c(i) - 1) or s(i)
                // (when 1 <= i < m - 1)
                for (int i = 1; i < m - 1; ++i) {
                    final IntegerClause.Builder newClause = IntegerClause.builder();
                    newClause.addBooleanLiterals(s[i + 1].negate(cf.getFormulaFactory()), s[i]);
                    newClause.addArithmeticLiteral(ge(sub(lhs[i], 1), rhs[i]));
                    ret.add(newClause.build());
                }
                // (z(i) <= x(i) + y(i) + c(i) - 1) or s(i) (when i == m - 1)
                if (m > 1) {
                    ret.add(new IntegerClause(s[m - 1], ge(sub(lhs[m - 1], 1), rhs[m - 1])));
                }

                for (int i = 0; i < m - 1; i++) {
                    //c(i + 1) <= 0 or x(i) + y(i) + c(i) >= B
                    ret.add(new IntegerClause(le(c[i + 1], 0), ge(rhs[i], b)));

                    //c(i+1) >= 1 or x(i) + y(i) + c(i) <= B - 1
                    ret.add(new IntegerClause(ge(c[i + 1], 1), le(rhs[i], b - 1)));
                }
                break;
            }
            case EQ: {
                for (int i = 0; i < m; ++i) {
                    ret.add(new IntegerClause(le(lhs[i], rhs[i])));
                    ret.add(new IntegerClause(ge(lhs[i], rhs[i])));
                }
                break;
            }
            case NE: {
                final IntegerClause.Builder newClause = IntegerClause.builder();
                for (int i = 0; i < m; ++i) {
                    newClause.addArithmeticLiterals(
                            le(lhs[i], sub(rhs[i], 1)),
                            ge(sub(lhs[i], 1), rhs[i])
                    );
                }
                ret.addAll(simplifyClause(newClause.build(), handler));

                for (int i = 0; i < m - 1; i++) {
                    // carry(i+1) <= 0 or x(i)+y(i)+carry(i) >= B
                    ret.add(new IntegerClause(le(c[i + 1], 0), ge(rhs[i], b)));

                    // carry(i+1) >= 1 or x(i) + y(i) + carry(i) <= B - 1
                    ret.add(new IntegerClause(ge(c[i + 1], 1), le(rhs[i], b - 1)));
                }
                break;
            }
        }
        return ret;
    }

    protected Set<IntegerClause> convertEqMulToCCSP(final EqMul lit, final List<IntegerVariable> frontierVariables,
                                                    final CompactOrderEncodingContext context,
                                                    final ComputationHandler handler)
            throws CspHandlerException {
        final int b = context.getBase();
        final IntegerHolder x = lit.getX();
        final IntegerVariable y = lit.getY();
        final IntegerHolder z = lit.getZ();
        final int m = Math.max(Math.max(nDigits(x), nDigits(y)), nDigits(z));
        final Set<IntegerClause> ret = new LinkedHashSet<>();

        if (x instanceof IntegerConstant && ((IntegerConstant) x).getValue() < b) {
            if (((IntegerConstant) x).getValue() == 0) {
                assert z instanceof IntegerVariable;
                return convertOpXYToCCSP(new OpXY(OpXY.Operator.LE, z, cf.constant(0)), context, handler);
            } else if (((IntegerConstant) x).getValue() == 1) {
                return convertOpXYToCCSP(new OpXY(OpXY.Operator.EQ, z, y), context, handler);
            }
            final IntegerHolder[] v = new IntegerHolder[m];
            final int a = ((IntegerConstant) x).getValue();
            for (int i = 0; i < m; ++i) {
                final IntegerDomain d = IntegerDomain.of(0, a * nth(y, i).getDomain().ub());
                final IntegerVariable vi = newCCSPVariable(d, frontierVariables);
                v[i] = vi;
            }

            for (int i = 0; i < m; ++i) {
                final LinearExpression left = add(mul(nth(v[i], 1), b), nth(v[i], 0));
                final LinearExpression right = mul(nth(y, i), a);
                ret.add(new IntegerClause(le(left, right)));
                ret.add(new IntegerClause(ge(left, right)));
            }

            final LinearExpression[] c = new LinearExpression[m];
            final IntegerDomain d = IntegerDomain.of(0, 1);
            for (int i = 2; i < m; ++i) {
                c[i] = new LinearExpression(newCCSPVariable(d, frontierVariables));
            }

            for (int i = 0; i < m; ++i) {
                final LinearExpression lhs;
                if (i == 0 || i == m - 1) {
                    lhs = nth(z, i);
                } else {
                    lhs = add(nth(z, i), mul(c[i + 1], b));
                }

                final LinearExpression rhs;
                if (i == 0) {
                    rhs = nth(v[i], 0);
                } else if (i == 1) {
                    rhs = add(nth(v[i], 0), nth(v[i - 1], 1));
                } else {
                    rhs = add(nth(v[i], 0), nth(v[i - 1], 1), c[i]);
                }

                ret.add(new IntegerClause(le(lhs, rhs)));
                ret.add(new IntegerClause(ge(lhs, rhs)));
            }
        } else {
            // z = xy
            final IntegerVariable[] w = new IntegerVariable[m];
            final int uby = y.getDomain().ub();
            int ubz = z.getDomain().ub();
            for (int i = 0; i < m; ++i) {
                final IntegerDomain d;
                if (x instanceof IntegerConstant) {
                    d = IntegerDomain.of(0, Math.min(nthValue((IntegerConstant) x, i) * uby, ubz));
                } else {
                    d = IntegerDomain.of(0, Math.min((b - 1) * uby, ubz));
                }
                w[i] = newCCSPVariable(d, frontierVariables);
                ubz /= b;
            }

            if (x instanceof IntegerConstant) {
                for (int i = 0; i < m; ++i) {
                    final EqMul newLit = new EqMul(w[i], cf.constant(nthValue((IntegerConstant) x, i)), y);
                    ret.addAll(convertEqMulToCCSP(newLit, frontierVariables, context, handler));
                }
            } else {
                final IntegerVariable[] ya = new IntegerVariable[b];
                for (int a = 0; a < b; ++a) {
                    ya[a] = newCCSPVariable(IntegerDomain.of(0, a * uby), frontierVariables);
                }

                for (int i = 0; i < m; ++i) {
                    for (int a = 0; a < b; ++a) {
                        final List<ArithmeticLiteral> als = List.of(
                                le(nth(x, i), a - 1),
                                ge(nth(x, i), a + 1)
                        );

                        final OpXY newLit = new OpXY(OpXY.Operator.EQ, w[i], ya[a]);
                        for (final IntegerClause c : convertOpXYToCCSP(newLit, context, handler)) {
                            final IntegerClause.Builder newClause = IntegerClause.clone(c);
                            newClause.addArithmeticLiterals(als);
                            ret.add(newClause.build());
                        }
                    }
                }

                for (int a = 0; a < b; ++a) {
                    final EqMul newLit = new EqMul(ya[a], cf.constant(a), y);
                    ret.addAll(convertEqMulToCCSP(newLit, frontierVariables, context, handler));
                }
            }

            // [z = Sum_(i = 0)^(m - 1) B^i w_i]
            final IntegerHolder[] zi = new IntegerHolder[m];
            zi[m - 1] = w[m - 1];
            for (int i = m - 2; i > 0; --i) {
                final IntegerDomain d = IntegerDomain.of(0, b * zi[i + 1].getDomain().ub() + w[i].getDomain().ub());
                final IntegerVariable zii = newCCSPVariable(d, frontierVariables);
                zi[i] = zii;
            }
            zi[0] = z;

            if (m == 1) {
                final LinearExpression exp1 = nth(z, 0);
                final LinearExpression exp2 = nth(w[0], 0);
                ret.add(new IntegerClause(le(exp1, exp2)));
                ret.add(new IntegerClause(ge(exp1, exp2)));
            } else {
                for (int i = 0; i < m - 1; ++i) {
                    ret.addAll(shiftAddToCCSP(zi[i], zi[i + 1], w[i], frontierVariables));
                }
            }
        }
        return ret;
    }

    /**
     * u = b*s+t
     */
    protected Set<IntegerClause> shiftAddToCCSP(final IntegerHolder u, final IntegerHolder s,
                                                final IntegerHolder t, final List<IntegerVariable> frontierVariables) {
        final int b = context.getBase();
        final int m = 1 + Math.max(nDigits(s), nDigits(t));
        final Set<IntegerClause> ret = new LinkedHashSet<>();

        final LinearExpression[] c = new LinearExpression[m];
        final IntegerDomain d = IntegerDomain.of(0, 1);
        for (int i = 2; i < m; i++) {
            c[i] = new LinearExpression(newCCSPVariable(d, frontierVariables));
        }

        for (int i = 0; i < m; ++i) {
            final LinearExpression lhs;
            if (i == 0 || i == m - 1) {
                lhs = nth(u, i);
            } else {
                lhs = add(nth(u, i), mul(c[i + 1], b));
            }

            final LinearExpression rhs;
            if (i == 0) {
                rhs = nth(t, i);
            } else if (i == 1) {
                rhs = add(nth(t, 1), nth(s, 0));
            } else if (i == m - 1) {
                rhs = add(nth(s, i - 1), c[i]);
            } else {
                rhs = add(nth(t, i), nth(s, i - 1), c[i]);
            }

            ret.add(new IntegerClause(le(lhs, rhs)));
            ret.add(new IntegerClause(ge(lhs, rhs)));
        }
        return ret;
    }

    protected List<IntegerVariable> splitToDigits(final IntegerVariable v) {
        int ub = v.getDomain().ub();
        final int b = context.getBase();

        final List<IntegerVariable> vs = new ArrayList<>();
        if (ub > 0 && ub + 1 <= b) {
            vs.add(v);
        } else if (ub > 0) {
            while (ub > 0) {
                final int ubi = ub < b ? ub : b - 1;
                final IntegerDomain dom = IntegerDomain.of(0, ubi);
                final IntegerVariable dv = context.newAuxiliaryDigitVariable(dom, cf);
                vs.add(dv);
                ub /= b;
            }
        }
        return vs;
    }

    protected int nDigits(final IntegerHolder v) {
        if (v instanceof IntegerConstant) {
            return calculateOrGetConstDigits((IntegerConstant) v).size();
        } else {
            return context.getDigits((IntegerVariable) v).size();
        }
    }

    protected List<Integer> calculateOrGetConstDigits(final IntegerConstant c) {
        if (!context.hasConstDigits(c)) {
            context.addConstDigits(c, intToDigits(c.getValue(), context.getBase()));
        }
        return context.getConstDigits(c);
    }

    protected LinearExpression nth(final IntegerHolder v, final int n) {
        if (v instanceof IntegerConstant) {
            assert context.getConstDigits((IntegerConstant) v) != null;
            return new LinearExpression(nthValue((IntegerConstant) v, n));
        } else {
            final List<IntegerVariable> digits = context.getDigits((IntegerVariable) v);
            if (digits.size() > n) {
                return new LinearExpression(digits.get(n));
            } else {
                return new LinearExpression(0);
            }
        }
    }

    protected int nthValue(final IntegerConstant v, final int n) {
        return context.getConstDigits(v).size() > n ? context.getConstDigits(v).get(n) : 0;
    }

    protected IntegerVariable newCCSPVariable(final IntegerDomain d, final List<IntegerVariable> frontierVariables) {
        final IntegerVariable v = context.newCCSPVariable(d, cf);
        final List<IntegerVariable> digits = splitToDigits(v);
        context.addDigits(v, digits);
        frontierVariables.addAll(digits);
        return v;
    }

    /**
     * Simplifies a clause so that all resulting arithmetic clauses are <I>simple</I>.
     * @param clause the clause
     * @return simplified clauses
     */
    protected Set<IntegerClause> simplifyClause(final IntegerClause clause, final ComputationHandler handler)
            throws CspHandlerException {
        final Set<IntegerClause> newClauses = new LinkedHashSet<>();
        final IntegerClause.Builder c = IntegerClause.cloneOnlyBool(clause);
        for (final ArithmeticLiteral literal : clause.getArithmeticLiterals()) {
            if (CompactOrderEncoding.isSimpleLiteral(literal, context)) {
                c.addArithmeticLiteral(literal);
            } else {
                final Variable p =
                        context.getOrderContext().newSimplifyBooleanVariable(cf.getFormulaFactory(), handler);
                final Literal notP = p.negate(cf.getFormulaFactory());
                final IntegerClause newClause = new IntegerClause(notP, literal);
                newClauses.add(newClause);
                c.addBooleanLiteral(p);
            }
        }
        newClauses.add(c.build());
        return newClauses;
    }

    /**
     * Returns whether an arithmetic literal is simple.
     * <p>
     * A literal is <I>simple</I> if it will encode as a single boolean
     * variable.
     * @param lit the arithmetic literal
     * @return {@code true} if the literal is simple
     */
    protected static boolean isSimpleLiteral(final ArithmeticLiteral lit, final CompactOrderEncodingContext context) {
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
    protected static boolean isSimpleClause(final IntegerClause clause, final CompactOrderEncodingContext context) {
        return clause.size() - simpleClauseSize(clause, context) <= 1;
    }

    /**
     * Returns the number of simple literals (simple arithmetic literals and all
     * boolean literals).
     * @param clause the clause
     * @return number of simple literals
     */
    protected static int simpleClauseSize(final IntegerClause clause, final CompactOrderEncodingContext context) {
        int simpleLiterals = clause.getBoolLiterals().size();
        for (final ArithmeticLiteral lit : clause.getArithmeticLiterals()) {
            if (isSimpleLiteral(lit, context)) {
                ++simpleLiterals;
            }
        }
        return simpleLiterals;
    }

    protected static LinearLiteral le(final LinearExpression lhs, final LinearExpression rhs) {
        final LinearExpression l = LinearExpression.subtract(lhs, rhs);
        return new LinearLiteral(l, LinearLiteral.Operator.LE);
    }

    protected static LinearLiteral le(final LinearExpression lhs, final int e) {
        final LinearExpression.Builder l = LinearExpression.builder(lhs);
        l.setB(l.getB() - e);
        return new LinearLiteral(l.build(), LinearLiteral.Operator.LE);
    }

    protected static LinearLiteral ge(final LinearExpression lhs, final LinearExpression rhs) {
        return le(rhs, lhs);
    }

    protected static LinearLiteral ge(final LinearExpression lhs, final int e) {
        final LinearExpression.Builder l = LinearExpression.builder(lhs);
        l.setB(l.getB() - e);
        l.multiply(-1);
        return new LinearLiteral(l.build(), LinearLiteral.Operator.LE);
    }

    protected static LinearExpression add(final LinearExpression... es) {
        final LinearExpression.Builder l = LinearExpression.builder(0);
        for (final LinearExpression e : es) {
            l.add(e);
        }
        return l.build();
    }

    protected static LinearExpression add(final LinearExpression lhs, final int e) {
        final LinearExpression.Builder l = LinearExpression.builder(lhs);
        l.setB(l.getB() + e);
        return l.build();
    }

    protected static LinearExpression sub(final LinearExpression lhs, final int e) {
        return add(lhs, -e);
    }

    protected static LinearExpression mul(final LinearExpression lhs, final int c) {
        final LinearExpression.Builder l = LinearExpression.builder(lhs);
        l.multiply(c);
        return l.build();
    }

    public static List<Integer> intToDigits(final int c, final int b) {
        final int m = (int) Math.ceil(Math.log(c + 1) / Math.log(b));
        int ub = c;
        final List<Integer> digits = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            digits.add(ub % b);
            ub /= b;
        }
        return digits;
    }
}
