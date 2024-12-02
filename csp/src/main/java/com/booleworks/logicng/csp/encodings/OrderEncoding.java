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
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;

import java.util.Iterator;
import java.util.Set;

/**
 * A class grouping functions for the order encoding
 */
public class OrderEncoding {
    private OrderEncoding() {
    }

    /**
     * Encodes a CSP problem using the order encoding.
     * <p>
     * Note: The destination of the encoding result may contain incomplete results, if the computation was aborted by
     * the handler.
     * @param csp     the problem
     * @param context the encoding context
     * @param result  destination for the result
     * @param cf      the factory
     * @param handler handler for processing encoding events
     * @return the passed encoding result if the computation was successful otherwise returns the handler event that
     * aborted the computation
     */
    public static LngResult<EncodingResult> encode(final Csp csp, final OrderEncodingContext context,
                                                   final EncodingResult result,
                                                   final CspFactory cf, final ComputationHandler handler) {
        if (!handler.shouldResume(CspEvent.CSP_ENCODING_STARTED)) {
            return LngResult.canceled(CspEvent.CSP_ENCODING_STARTED);
        }
        LngResult<EncodingResult> r;
        for (final IntegerVariable v : csp.getInternalIntegerVariables()) {
            r = encodeVariable(v, context, result, cf, handler);
            if (!r.isSuccess()) {
                return r;
            }
        }
        return encodeClauses(csp.getClauses(), context, result, cf, handler);
    }

    /**
     * Encodes a single integer variable using the order encoding.
     * <p>
     * Note: The destination of the encoding result may contain incomplete results, if the computation was aborted by
     * the handler.
     * @param v       the integer variable
     * @param context the encoding context
     * @param result  destination for the result
     * @param cf      the factory
     * @param handler handler for processing encoding events
     * @return the passed encoding result if the computation was successful otherwise returns the handler event that
     * aborted the computation
     */
    public static LngResult<EncodingResult> encodeVariable(final IntegerVariable v, final OrderEncodingContext context,
                                                           final EncodingResult result, final CspFactory cf,
                                                           final ComputationHandler handler) {
        try {
            final FormulaFactory f = cf.getFormulaFactory();
            final IntegerDomain domain = v.getDomain();

            context.allocateVariable(v, domain.size());

            final Formula[] clause = new Formula[2];
            Formula last_var = createOrGetCodeLE(v, domain.lb(), context, cf.getFormulaFactory(), handler);
            for (int a = domain.lb() + 1; a <= domain.ub(); ++a) {
                if (domain.contains(a)) {
                    clause[0] = last_var.negate(f);
                    clause[1] = createOrGetCodeLE(v, a, context, cf.getFormulaFactory(), handler);
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
     * Note: The destination of the encoding result may contain incomplete results, if the computation was aborted by
     * the handler.
     * @param clauses the set of clauses
     * @param context the encoding context
     * @param result  destination for the result
     * @param cf      the factory
     * @param handler handler for processing encoding events
     * @return the passed encoding result if the computation was successful otherwise returns the handler event that
     * aborted the computation
     */
    public static LngResult<EncodingResult> encodeClauses(final Set<IntegerClause> clauses,
                                                          final OrderEncodingContext context,
                                                          final EncodingResult result, final CspFactory cf,
                                                          final ComputationHandler handler) {
        try {
            final ReductionResult reduced = OrderReduction.reduce(clauses, context, cf, handler);
            LngResult<EncodingResult> r;
            for (final IntegerVariable v : reduced.getFrontierAuxiliaryVariables()) {
                r = encodeVariable(v, context, result, cf, handler);
                if (!r.isSuccess()) {
                    return r;
                }
            }
            for (final IntegerClause c : reduced.getClauses()) {
                if (!c.isValid()) {
                    encodeClause(c, context, result, cf, handler);
                }
            }
        } catch (final CspHandlerException e) {
            return LngResult.canceled(e.getReason());
        }
        return LngResult.of(result);
    }

    /**
     * <B>Directly</B> encodes (without reduction) an arithmetic clause using the order encoding.
     * @param cl      the arithmetic clause
     * @param context the encoding context
     * @param result  destination for the result
     * @param cf      the factory
     * @param handler handler for processing encoding events
     * @throws CspHandlerException if the computation was aborted by the handler
     */
    static void encodeClause(final IntegerClause cl, final OrderEncodingContext context, final EncodingResult result,
                             final CspFactory cf, final ComputationHandler handler) throws CspHandlerException {
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
                clause[i] = getCode((LinearLiteral) literal, context, cf.getFormulaFactory());
                i++;
            } else {
                lit = (LinearLiteral) literal;
            }
        }
        if (lit == null) {
            writeClause(clause, result, handler);
        } else {
            encodeLitClause(lit, clause, context, result, cf, handler);
        }
    }

    private static void encodeLitClause(final LinearLiteral lit, Formula[] clause, final OrderEncodingContext context,
                                        final EncodingResult result, final CspFactory cf,
                                        final ComputationHandler handler)
            throws CspHandlerException {
        if (lit.getOperator() == LinearLiteral.Operator.EQ || lit.getOperator() == LinearLiteral.Operator.NE) {
            throw new RuntimeException("Invalid operator for order encoding " + lit);
        }
        if (isSimpleLiteral(lit)) {
            clause = expandArray(clause, 1);
            clause[0] = getCode(lit, context, cf.getFormulaFactory());
            writeClause(clause, result, handler);
        } else {
            final LinearExpression ls = lit.getSum();
            final IntegerVariable[] vs = lit.getSum().getVariablesSorted();
            final int n = ls.size();
            clause = expandArray(clause, n);
            encodeLinearExpression(ls, vs, 0, lit.getSum().getB(), clause, context, result, cf, handler);
        }
    }

    private static void encodeLinearExpression(final LinearExpression exp, final IntegerVariable[] vs, final int i,
                                               final int s, final Formula[] clause, final OrderEncodingContext context,
                                               final EncodingResult result, final CspFactory cf,
                                               final ComputationHandler handler) throws CspHandlerException {
        if (i >= vs.length - 1) {
            final int a = exp.getA(vs[i]);
            clause[i] = getCodeLE(vs[i], a, -s, context, cf.getFormulaFactory());
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
                    clause[i] = getCodeLE(vs[i], c - 1, context, cf.getFormulaFactory());
                    encodeLinearExpression(exp, vs, i + 1, s + a * c, clause, context, result, cf, handler);
                }
                clause[i] = getCodeLE(vs[i], ub, context, cf.getFormulaFactory());
                encodeLinearExpression(exp, vs, i + 1, s + a * (ub + 1), clause, context, result, cf, handler);
            } else {
                if (-lb0 >= 0) {
                    lb = Math.max(lb, -lb0 / a);
                } else {
                    lb = Math.max(lb, (-lb0 + a + 1) / a);
                }
                clause[i] = getCodeLE(vs[i], lb - 1, context, cf.getFormulaFactory()).negate(
                        cf.getFormulaFactory());
                encodeLinearExpression(exp, vs, i + 1, s + a * (lb - 1), clause, context, result, cf, handler);
                for (final Iterator<Integer> it = domain.values(lb, ub); it.hasNext(); ) {
                    final int c = it.next();
                    clause[i] =
                            getCodeLE(vs[i], c, context, cf.getFormulaFactory()).negate(cf.getFormulaFactory());
                    encodeLinearExpression(exp, vs, i + 1, s + a * c, clause, context, result, cf, handler);
                }
            }
        }
    }

    private static Formula createOrGetCodeLE(final IntegerVariable left, final int right,
                                             final OrderEncodingContext context, final FormulaFactory f,
                                             final ComputationHandler handler) throws CspHandlerException {
        final IntegerDomain domain = left.getDomain();
        if (right < domain.lb()) {
            return f.falsum();
        } else if (right >= domain.ub()) {
            return f.verum();
        }
        final int index = sizeLE(domain, right) - 1;
        return context.newVariableInstance(left, index, f, handler);
    }

    private static Formula getCodeLE(final IntegerVariable left, final int right, final OrderEncodingContext context,
                                     final FormulaFactory f) {
        final IntegerDomain domain = left.getDomain();
        if (right < domain.lb()) {
            return f.falsum();
        } else if (right >= domain.ub()) {
            return f.verum();
        }
        final int index = sizeLE(domain, right) - 1;
        return context.getVariableInstance(left, index);
    }

    private static Formula getCodeLE(final IntegerVariable left, final int a, final int b,
                                     final OrderEncodingContext context,
                                     final FormulaFactory f) {
        if (a >= 0) {
            final int c;
            if (b >= 0) {
                c = b / a;
            } else {
                c = (b - a + 1) / a;
            }
            return getCodeLE(left, c, context, f);
        } else {
            final int c;
            if (b >= 0) {
                c = b / a - 1;
            } else {
                c = (b + a + 1) / a - 1;
            }
            return getCodeLE(left, c, context, f).negate(f);
        }
    }

    private static Formula getCode(final LinearLiteral lit, final OrderEncodingContext context,
                                   final FormulaFactory f) {
        if (!isSimpleLiteral(lit)) {
            throw new IllegalArgumentException("Encountered non-simple literal in order encoding " + lit.toString());
        }
        if (lit.getOperator() == LinearLiteral.Operator.EQ || lit.getOperator() == LinearLiteral.Operator.NE) {
            throw new IllegalArgumentException("Encountered eq/ne literal in order encoding " + lit);
        }
        final LinearExpression sum = lit.getSum();
        final int b = sum.getB();
        if (sum.size() == 0) {
            return f.constant(b <= 0);
        } else {
            final IntegerVariable v = sum.getCoef().firstKey();
            final int a = sum.getA(v);
            return getCodeLE(v, a, -b, context, f);
        }
    }

    private static int sizeLE(final IntegerDomain d, final int value) {
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
    static boolean isSimpleClause(final IntegerClause clause) {
        return clause.size() - simpleClauseSize(clause) <= 1;
    }

    /**
     * Returns whether an arithmetic literal is simple.
     * <p>
     * A literal is <I>simple</I> if it will encode as a single boolean variable.
     * @param literal the arithmetic literal
     * @return {@code true} if the literal is simple
     */
    static boolean isSimpleLiteral(final ArithmeticLiteral literal) {
        if (literal instanceof LinearLiteral) {
            final LinearLiteral l = (LinearLiteral) literal;
            return l.getSum().getCoef().size() <= 1 && l.getOperator() == LinearLiteral.Operator.LE;
        }
        return false;
    }

    /**
     * Returns the number of simple literals (simple arithmetic literals and all boolean literals).
     * @param clause the clause
     * @return number of simple literals
     */
    static int simpleClauseSize(final IntegerClause clause) {
        int simpleLiterals = clause.getBoolLiterals().size();
        for (final ArithmeticLiteral lit : clause.getArithmeticLiterals()) {
            if (isSimpleLiteral(lit)) {
                ++simpleLiterals;
            }
        }
        return simpleLiterals;
    }

    private static Formula[] expandArray(final Formula[] clause0, final int n) {
        final Formula[] clause = new Formula[clause0.length + n];
        System.arraycopy(clause0, 0, clause, n, clause0.length);
        return clause;
    }

    private static void writeClause(final Formula[] clause, final EncodingResult result,
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
}
