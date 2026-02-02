// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.handlers.CspEvent;
import com.booleworks.logicng.csp.handlers.CspHandlerException;
import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.literals.ProductLiteral;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResult;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A class grouping functions for the order encoding
 * @version 3.0.0
 * @since 3.0.0
 */
public class OrderEncoding {
    /**
     * Maximum domain size for linear expressions before they get simplified.
     */
    public static final int MAX_LINEAR_EXPRESSION_SIZE = 1024;

    /**
     * Number of splits performed for simplifying linear expressions.
     */
    public static final int SPLITS = 2;

    /**
     * Prefix for auxiliary variables used for simplifying linear expressions.
     */
    public static final String AUX_SIMPLE = "OE_SIMPLE";

    protected final CspFactory cf;
    protected final OrderEncodingContext context;

    /**
     * Construct a new order encoding instance-
     * @param context the encoding context
     * @param cf      the factory
     */
    public OrderEncoding(final OrderEncodingContext context, final CspFactory cf) {
        this.context = context;
        this.cf = cf;
    }

    /**
     * Encodes a CSP problem using the order encoding.
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
        LngResult<EncodingResult> r;
        for (final IntegerVariable v : csp.getInternalIntegerVariables()) {
            r = encodeVariable(v, result, handler);
            if (!r.isSuccess()) {
                return r;
            }
        }
        return encodeClauses(csp.getClauses(), result, handler);
    }

    /**
     * Encodes a single integer variable using the order encoding.
     * <p>
     * Note: The destination of the encoding result may contain incomplete
     * results, if the computation was cancelled by the handler.
     * @param v       the integer variable
     * @param result  destination for the result
     * @param handler handler for processing encoding events
     * @return the passed encoding result if the computation was successful
     * otherwise returns the handler event that cancelled the computation
     */
    public LngResult<EncodingResult> encodeVariable(final IntegerVariable v, final EncodingResult result,
                                                    final ComputationHandler handler) {
        try {
            final FormulaFactory f = cf.getFormulaFactory();
            final IntegerDomain domain = v.getDomain();

            context.allocateVariable(v, domain.size());

            final Formula[] clause = new Formula[2];
            Formula last_var = createOrGetCodeLE(v, domain.lb(), handler);
            for (int a = domain.lb() + 1; a <= domain.ub(); ++a) {
                if (domain.contains(a)) {
                    clause[0] = last_var.negate(f);
                    clause[1] = createOrGetCodeLE(v, a, handler);
                    writeClause(clause, result, handler);
                    last_var = clause[1];
                }
            }
        } catch (final CspHandlerException e) {
            return LngResult.canceled(e.getReason());
        }
        return LngResult.of(result);
    }

    /**
     * Encodes a set of arithmetic clauses using the order encoding.
     * <p>
     * Note: The destination of the encoding result may contain incomplete
     * results, if the computation was cancelled by the handler.
     * @param clauses the set of clauses
     * @param result  destination for the result
     * @param handler handler for processing encoding events
     * @return the passed encoding result if the computation was successful
     * otherwise returns the handler event that cancelled the computation
     */
    public LngResult<EncodingResult> encodeClauses(final Set<IntegerClause> clauses, final EncodingResult result,
                                                   final ComputationHandler handler) {
        try {
            final ReductionResult reduced = reduce(clauses, handler);
            LngResult<EncodingResult> r;
            for (final IntegerVariable v : reduced.getFrontierAuxiliaryVariables()) {
                r = encodeVariable(v, result, handler);
                if (!r.isSuccess()) {
                    return r;
                }
            }
            for (final IntegerClause c : reduced.getClauses()) {
                if (!c.isValid()) {
                    encodeClause(c, result, handler);
                }
            }
        } catch (final CspHandlerException e) {
            return LngResult.canceled(e.getReason());
        }
        return LngResult.of(result);
    }

    /**
     * <B>Directly</B> encodes (without reduction) an arithmetic clause using
     * the order encoding.
     * @param cl      the arithmetic clause
     * @param result  destination for the result
     * @param handler handler for processing encoding events
     * @throws CspHandlerException if the computation was cancelled by the
     *                             handler
     */
    protected void encodeClause(final IntegerClause cl, final EncodingResult result, final ComputationHandler handler)
            throws CspHandlerException {
        if (!isSimpleClause(cl)) {
            throw new IllegalArgumentException("Cannot encode non-simple clause " + cl);
        }
        if (cl.isValid()) {
            return;
        }
        final Formula[] clause = new Formula[simpleClauseSize(cl)];
        LinearLiteral lit = null;
        int i = 0;
        for (final Literal literal : cl.getBoolLiterals()) {
            clause[i] = literal;
            i++;
        }
        for (final ArithmeticLiteral literal : cl.getArithmeticLiterals()) {
            if (isSimpleLiteral(literal)) {
                clause[i] = getCode((LinearLiteral) literal);
                i++;
            } else {
                lit = (LinearLiteral) literal;
            }
        }
        if (lit == null) {
            writeClause(clause, result, handler);
        } else {
            encodeLitClause(lit, clause, result, handler);
        }
    }

    protected void encodeLitClause(final LinearLiteral lit, Formula[] clause, final EncodingResult result,
                                   final ComputationHandler handler)
            throws CspHandlerException {
        if (lit.getOperator() == LinearLiteral.Operator.EQ || lit.getOperator() == LinearLiteral.Operator.NE) {
            throw new RuntimeException("Invalid operator for order encoding " + lit);
        }
        if (isSimpleLiteral(lit)) {
            clause = expandArray(clause, 1);
            clause[0] = getCode(lit);
            writeClause(clause, result, handler);
        } else {
            final LinearExpression ls = lit.getSum();
            final IntegerVariable[] vs = lit.getSum().getVariablesSorted();
            final int n = ls.size();
            clause = expandArray(clause, n);
            encodeLinearExpression(ls, vs, 0, lit.getSum().getB(), clause, context, result, cf, handler);
        }
    }

    protected void encodeLinearExpression(final LinearExpression exp, final IntegerVariable[] vs, final int i,
                                          final int s, final Formula[] clause, final OrderEncodingContext context,
                                          final EncodingResult result, final CspFactory cf,
                                          final ComputationHandler handler) throws CspHandlerException {
        if (i >= vs.length - 1) {
            final int a = exp.getA(vs[i]);
            clause[i] = getCodeLE(vs[i], a, -s);
            writeClause(clause, result, handler);
        } else {
            int lb0 = s;
            for (int j = i + 1; j < vs.length; ++j) {
                final int a = exp.getA(vs[j]);
                if (a > 0) {
                    lb0 += a * vs[j].getDomain().lb();
                } else {
                    lb0 += a * vs[j].getDomain().ub();
                }
            }
            final int a = exp.getA(vs[i]);
            final IntegerDomain domain = vs[i].getDomain();
            int lb = domain.lb();
            int ub = domain.ub();
            if (a >= 0) {
                if (-lb0 >= 0) {
                    ub = Math.min(ub, -lb0 / a);
                } else {
                    ub = Math.min(ub, (-lb0 - a + 1) / a);
                }
                for (final Iterator<Integer> it = domain.values(lb, ub); it.hasNext(); ) {
                    final int c = it.next();
                    clause[i] = getCodeLE(vs[i], c - 1);
                    encodeLinearExpression(exp, vs, i + 1, s + a * c, clause, context, result, cf, handler);
                }
                clause[i] = getCodeLE(vs[i], ub);
                encodeLinearExpression(exp, vs, i + 1, s + a * (ub + 1), clause, context, result, cf, handler);
            } else {
                if (-lb0 >= 0) {
                    lb = Math.max(lb, -lb0 / a);
                } else {
                    lb = Math.max(lb, (-lb0 + a + 1) / a);
                }
                clause[i] = getCodeLE(vs[i], lb - 1).negate(
                        cf.getFormulaFactory());
                encodeLinearExpression(exp, vs, i + 1, s + a * (lb - 1), clause, context, result, cf, handler);
                for (final Iterator<Integer> it = domain.values(lb, ub); it.hasNext(); ) {
                    final int c = it.next();
                    clause[i] =
                            getCodeLE(vs[i], c).negate(cf.getFormulaFactory());
                    encodeLinearExpression(exp, vs, i + 1, s + a * c, clause, context, result, cf, handler);
                }
            }
        }
    }

    protected Formula createOrGetCodeLE(final IntegerVariable left, final int right, final ComputationHandler handler)
            throws CspHandlerException {
        final IntegerDomain domain = left.getDomain();
        if (right < domain.lb()) {
            return cf.getFormulaFactory().falsum();
        } else if (right >= domain.ub()) {
            return cf.getFormulaFactory().verum();
        }
        final int index = sizeLE(domain, right) - 1;
        return context.newVariableInstance(left, index, cf.getFormulaFactory(), handler);
    }

    protected Formula getCodeLE(final IntegerVariable left, final int right) {
        final IntegerDomain domain = left.getDomain();
        if (right < domain.lb()) {
            return cf.getFormulaFactory().falsum();
        } else if (right >= domain.ub()) {
            return cf.getFormulaFactory().verum();
        }
        final int index = sizeLE(domain, right) - 1;
        return context.getVariableInstance(left, index);
    }

    protected Formula getCodeLE(final IntegerVariable left, final int a, final int b) {
        if (a >= 0) {
            final int c;
            if (b >= 0) {
                c = b / a;
            } else {
                c = (b - a + 1) / a;
            }
            return getCodeLE(left, c);
        } else {
            final int c;
            if (b >= 0) {
                c = b / a - 1;
            } else {
                c = (b + a + 1) / a - 1;
            }
            return getCodeLE(left, c).negate(cf.getFormulaFactory());
        }
    }

    protected Formula getCode(final LinearLiteral lit) {
        if (!isSimpleLiteral(lit)) {
            throw new IllegalArgumentException("Encountered non-simple literal in order encoding " + lit.toString());
        }
        if (lit.getOperator() == LinearLiteral.Operator.EQ || lit.getOperator() == LinearLiteral.Operator.NE) {
            throw new IllegalArgumentException("Encountered eq/ne literal in order encoding " + lit);
        }
        final LinearExpression sum = lit.getSum();
        final int b = sum.getB();
        if (sum.size() == 0) {
            return cf.getFormulaFactory().constant(b <= 0);
        } else {
            final IntegerVariable v = sum.getCoef().firstKey();
            final int a = sum.getA(v);
            return getCodeLE(v, a, -b);
        }
    }

    protected static void writeClause(final Formula[] clause, final EncodingResult result,
                                      final ComputationHandler handler)
            throws CspHandlerException {
        final LngVector<Literal> vec = new LngVector<>();
        for (final Formula literal : clause) {
            switch (literal.getType()) {
                case TRUE:
                    return;
                case FALSE:
                    break;
                case LITERAL:
                    vec.push((Literal) literal);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unsupported formula type in order encoding:" + literal.getType());
            }
        }
        if (!handler.shouldResume(CspEvent.CSP_ENCODING_CLAUSE_CREATED)) {
            throw new CspHandlerException(CspEvent.CSP_ENCODING_CLAUSE_CREATED);
        }
        result.addClause(vec);
    }

    /**
     * Reduces a set of arithmetic clauses so that it can be encoded with the
     * order encoding.
     * @param clauses the clauses
     * @param handler for processing encoding events
     * @return the reduced problem
     * @throws CspHandlerException if the computation was cancelled by the
     *                             handler
     */
    protected ReductionResult reduce(final Set<IntegerClause> clauses, final ComputationHandler handler)
            throws CspHandlerException {
        final List<IntegerVariable> auxVars = new ArrayList<>();
        final Set<IntegerClause> newClauses = toLinearLe(simplify(split(clauses, auxVars), handler), handler);
        return new ReductionResult(newClauses, auxVars);
    }

    protected Set<IntegerClause> split(final Set<IntegerClause> clauses,
                                       final List<IntegerVariable> newFrontierAuxVars) {
        final Set<IntegerClause> newClauses = new LinkedHashSet<>();
        for (final IntegerClause c : clauses) {
            final Set<ArithmeticLiteral> newArithLits = new LinkedHashSet<>();
            for (final ArithmeticLiteral al : c.getArithmeticLiterals()) {
                if (al instanceof LinearLiteral) {
                    final LinearLiteral ll = (LinearLiteral) al;
                    final LinearExpression sum =
                            simplifyLinearExpression(LinearExpression.builder(ll.getSum()), true, newClauses,
                                    newFrontierAuxVars).build();
                    newArithLits.add(new LinearLiteral(sum, ll.getOperator()));
                } else {
                    newArithLits.add(al);
                }
            }
            newClauses.add(new IntegerClause(c.getBoolLiterals(), newArithLits));
        }
        return newClauses;
    }

    protected Set<IntegerClause> simplify(final Set<IntegerClause> clauses, final ComputationHandler handler)
            throws CspHandlerException {
        final Set<IntegerClause> newClauses = new LinkedHashSet<>();
        for (final IntegerClause clause : clauses) {
            if (clause.isValid()) {
                continue;
            } else if (OrderEncoding.isSimpleClause(clause)) {
                newClauses.add(clause);
            } else {
                newClauses.addAll(simplifyClause(clause, clause.getBoolLiterals(), handler));
            }
        }
        return newClauses;
    }

    protected Set<IntegerClause> toLinearLe(final Set<IntegerClause> clauses, final ComputationHandler handler)
            throws CspHandlerException {
        final Set<IntegerClause> newClauses = new LinkedHashSet<>();
        for (final IntegerClause c : clauses) {
            if (c.size() == OrderEncoding.simpleClauseSize(c)) {
                newClauses.add(c);
            } else {
                assert c.size() == OrderEncoding.simpleClauseSize(c) + 1;
                final Set<ArithmeticLiteral> simpleLiterals = new LinkedHashSet<>();
                ArithmeticLiteral nonSimpleLiteral = null;
                for (final ArithmeticLiteral al : c.getArithmeticLiterals()) {
                    if (OrderEncoding.isSimpleLiteral(al)) {
                        simpleLiterals.add(al);
                    } else {
                        nonSimpleLiteral = al;
                    }
                }
                assert nonSimpleLiteral != null;
                if (nonSimpleLiteral instanceof LinearLiteral) {
                    newClauses.addAll(reduceLinearLiteralToLinearLE((LinearLiteral) nonSimpleLiteral, simpleLiterals,
                            c.getBoolLiterals(), handler));
                } else if (nonSimpleLiteral instanceof ProductLiteral) {
                    newClauses.addAll(reduceProductLiteralToLinearLE((ProductLiteral) nonSimpleLiteral, simpleLiterals,
                            c.getBoolLiterals()));
                } else {
                    throw new IllegalArgumentException(
                            "Invalid literal for order encoding reduction: " + nonSimpleLiteral.getClass());
                }
            }
        }
        return newClauses;
    }

    protected Set<IntegerClause> reduceLinearLiteralToLinearLE(final LinearLiteral literal,
                                                               final Set<ArithmeticLiteral> simpleLiterals,
                                                               final Set<Literal> boolLiterals,
                                                               final ComputationHandler handler)
            throws CspHandlerException {
        switch (literal.getOperator()) {
            case LE:
                final Set<ArithmeticLiteral> lits = new LinkedHashSet<>(simpleLiterals);
                lits.add(literal);
                return Collections.singleton(new IntegerClause(boolLiterals, lits));
            case EQ:
                final Set<ArithmeticLiteral> litsA = new LinkedHashSet<>(simpleLiterals);
                litsA.add(new LinearLiteral(literal.getSum(), LinearLiteral.Operator.LE));
                final IntegerClause c1 = new IntegerClause(boolLiterals, litsA);
                final LinearExpression.Builder ls = LinearExpression.builder(literal.getSum());
                ls.multiply(-1);
                final Set<ArithmeticLiteral> litsB = new LinkedHashSet<>(simpleLiterals);
                litsB.add(new LinearLiteral(ls.build(), LinearLiteral.Operator.LE));
                final IntegerClause c2 = new IntegerClause(boolLiterals, litsB);
                return Set.of(c1, c2);
            case NE:
                final LinearExpression.Builder ls1 = LinearExpression.builder(literal.getSum());
                ls1.setB(ls1.getB() + 1);
                final LinearExpression.Builder ls2 = LinearExpression.builder(literal.getSum());
                ls2.multiply(-1);
                ls2.setB(ls2.getB() + 1);
                final Set<ArithmeticLiteral> litsNe = new LinkedHashSet<>(simpleLiterals);
                litsNe.add(new LinearLiteral(ls1.build(), LinearLiteral.Operator.LE));
                litsNe.add(new LinearLiteral(ls2.build(), LinearLiteral.Operator.LE));
                final IntegerClause newClause = new IntegerClause(Collections.emptySortedSet(), litsNe);
                return simplifyClause(newClause, boolLiterals, handler);
            default:
                throw new IllegalArgumentException(
                        "Invalid operator of linear expression for order encoding reduction: " + literal.getOperator());

        }
    }

    protected Set<IntegerClause> reduceProductLiteralToLinearLE(final ProductLiteral literal,
                                                                final Set<ArithmeticLiteral> simpleLiterals,
                                                                final Set<Literal> boolLiterals) {
        final Set<IntegerClause> ret = new LinkedHashSet<>();
        final IntegerVariable v = literal.getV();
        final IntegerVariable v1 = literal.getV1();
        final IntegerVariable v2 = literal.getV2();
        final IntegerVariable sv = v1.getDomain().size() <= v2.getDomain().size() ? v1 : v2;
        final IntegerVariable lv = sv == v1 ? v2 : v1;

        final Iterator<Integer> iter = sv.getDomain().iterator();
        while (iter.hasNext()) {
            final int a = iter.next();
            final LinearLiteral xlea =
                    new LinearLiteral(new LinearExpression(1, sv, -a + 1), LinearLiteral.Operator.LE);
            final LinearLiteral xgea =
                    new LinearLiteral(new LinearExpression(-1, sv, a + 1), LinearLiteral.Operator.LE);

            final LinearExpression le1 = LinearExpression.builder(0).setA(-1, v).setA(a, lv).build();
            final LinearLiteral ls1 = new LinearLiteral(le1, LinearLiteral.Operator.LE);
            final IntegerClause.Builder cls1 = IntegerClause.builder();
            cls1.addBooleanLiterals(boolLiterals);
            cls1.addArithmeticLiterals(simpleLiterals);
            cls1.addArithmeticLiterals(ls1, xlea, xgea);
            ret.add(cls1.build());

            final LinearExpression le2 = LinearExpression.builder(0).setA(1, v).setA(-a, lv).build();
            final LinearLiteral ls2 = new LinearLiteral(le2, LinearLiteral.Operator.LE);
            final IntegerClause.Builder cls2 = IntegerClause.builder();
            cls2.addBooleanLiterals(boolLiterals);
            cls2.addArithmeticLiterals(simpleLiterals);
            cls2.addArithmeticLiterals(ls2, xlea, xgea);
            ret.add(cls2.build());
        }
        return ret;
    }

    protected Set<IntegerClause> simplifyClause(final IntegerClause clause, final Set<Literal> initBoolLiterals,
                                                final ComputationHandler handler) throws CspHandlerException {
        final Set<IntegerClause> newClauses = new LinkedHashSet<>();
        final Set<ArithmeticLiteral> newArithLiterals = new LinkedHashSet<>();
        final Set<Literal> newBoolLiterals = new LinkedHashSet<>(initBoolLiterals);
        for (final ArithmeticLiteral literal : clause.getArithmeticLiterals()) {
            if (OrderEncoding.isSimpleLiteral(literal)) {
                newArithLiterals.add(literal);
            } else {
                final Variable p = context.newSimplifyBooleanVariable(cf.getFormulaFactory(), handler);
                final Literal notP = p.negate(cf.getFormulaFactory());
                final IntegerClause newClause =
                        IntegerClause.builder().addBooleanLiteral(notP).addArithmeticLiteral(literal).build();
                newClauses.add(newClause);
                newBoolLiterals.add(p);
                if (context.is_preserve_model_count()) {
                    final LinearLiteral notL = ((LinearLiteral) literal).negate();
                    final IntegerClause newClause2 =
                            IntegerClause.builder().addBooleanLiteral(p).addArithmeticLiteral(notL).build();
                    newClauses.add(newClause2);
                }
            }
        }
        final IntegerClause c = new IntegerClause(newBoolLiterals, newArithLiterals);
        newClauses.add(c);
        return newClauses;
    }

    protected LinearExpression.Builder simplifyLinearExpression(final LinearExpression.Builder exp,
                                                                final boolean first,
                                                                final Set<IntegerClause> clauses,
                                                                final List<IntegerVariable> newFrontierAuxVars) {
        if (exp.size() <= 1 || !exp.isDomainLargerThan(MAX_LINEAR_EXPRESSION_SIZE)) {
            return exp;
        }
        final int b = exp.getB();
        final LinearExpression.Builder[] es = split(exp.build(), first ? 3 : SPLITS);
        final LinearExpression.Builder result = LinearExpression.builder(b);
        for (final LinearExpression.Builder eMut : es) {
            final int factor = eMut.factor();
            if (factor > 1) {
                eMut.divide(factor);
            }
            LinearExpression.Builder simplified =
                    simplifyLinearExpression(eMut, false, clauses, newFrontierAuxVars);
            if (simplified.size() > 1) {
                final IntegerVariable v = context.newSimplifyIntVariable(simplified.getDomain(), cf);
                newFrontierAuxVars.add(v);
                simplified.subtract(new LinearExpression(v));
                final IntegerClause aux =
                        new IntegerClause(new LinearLiteral(simplified.build(), LinearLiteral.Operator.EQ));
                clauses.add(aux);
                simplified = LinearExpression.builder(v);
            }
            if (factor > 1) {
                simplified.multiply(factor);
            }
            result.add(simplified.build());
        }
        return result;
    }

    /**
     * Split a linear expression into multiple linear expressions.
     * @param exp the linear expression
     * @param m   the number of new linear expressions
     * @return an array with the new linear expressions
     */
    protected static LinearExpression.Builder[] split(final LinearExpression exp, final int m) {
        final LinearExpression.Builder[] es = new LinearExpression.Builder[m];
        for (int i = 0; i < m; ++i) {
            es[i] = LinearExpression.builder(0);
        }
        final IntegerVariable[] vs = exp.getVariablesSorted();
        for (int i = 0; i < vs.length; i++) {
            final IntegerVariable v = vs[i];
            es[i % m].setA(exp.getA(v), v);
        }
        return es;
    }

    protected static int sizeLE(final IntegerDomain d, final int value) {
        if (value < d.lb()) {
            return 0;
        }
        if (value >= d.ub()) {
            return d.size();
        }
        if (d.isContiguous()) {
            return value - d.lb() + 1;
        } else {
            return d.headSet(value + 1).size();
        }
    }

    /**
     * Returns whether an arithmetic clauses is simple.
     * <p>
     * A clause is <I>simple</I> if it contains at most one non-simple literal.
     * @param clause
     * @return {@code true} if the clause is simple
     */
    protected static boolean isSimpleClause(final IntegerClause clause) {
        return clause.size() - simpleClauseSize(clause) <= 1;
    }

    /**
     * Returns whether an arithmetic literal is simple.
     * <p>
     * A literal is <I>simple</I> if it will encode as a single boolean
     * variable.
     * @param literal the arithmetic literal
     * @return {@code true} if the literal is simple
     */
    protected static boolean isSimpleLiteral(final ArithmeticLiteral literal) {
        if (literal instanceof LinearLiteral) {
            final LinearLiteral l = (LinearLiteral) literal;
            return l.getSum().getCoef().size() <= 1 && l.getOperator() == LinearLiteral.Operator.LE;
        }
        return false;
    }

    /**
     * Returns the number of simple literals (simple arithmetic literals and all
     * boolean literals).
     * @param clause the clause
     * @return number of simple literals
     */
    protected static int simpleClauseSize(final IntegerClause clause) {
        int simpleLiterals = clause.getBoolLiterals().size();
        for (final ArithmeticLiteral lit : clause.getArithmeticLiterals()) {
            if (isSimpleLiteral(lit)) {
                ++simpleLiterals;
            }
        }
        return simpleLiterals;
    }

    protected static Formula[] expandArray(final Formula[] clause0, final int n) {
        final Formula[] clause = new Formula[clause0.length + n];
        System.arraycopy(clause0, 0, clause, n, clause0.length);
        return clause;
    }
}
