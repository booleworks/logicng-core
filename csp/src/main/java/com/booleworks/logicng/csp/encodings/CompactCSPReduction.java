package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.handlers.CspHandlerException;
import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.literals.EqMul;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.literals.OpAdd;
import com.booleworks.logicng.csp.literals.OpXY;
import com.booleworks.logicng.csp.literals.RCSPLiteral;
import com.booleworks.logicng.csp.terms.IntegerConstant;
import com.booleworks.logicng.csp.terms.IntegerHolder;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Operations for reducing RCSP literals to CCSP literals.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class CompactCSPReduction {
    /**
     * Prefix for CCSP auxiliary variables.
     */
    public static final String AUX_CCSP = "COE_CCSP";

    /**
     * Prefix for digits.
     */
    public static final String AUX_DIGIT = "COE_DIGIT";

    private final CompactOrderEncodingContext context;
    private final CspFactory cf;

    /**
     * Constructs a new instance for compact order reduction.
     * @param context the encoding context
     * @param cf      the factory
     */
    CompactCSPReduction(final CompactOrderEncodingContext context, final CspFactory cf) {
        this.context = context;
        this.cf = cf;
    }

    /**
     * Reduces a set of arithmetic clauses with RCSP literals to clauses with
     * CCSP literals.
     * @param clauses   RCSP clauses
     * @param variables all variables used in {@code clauses}
     * @param handler   handler for processing encoding events
     * @return reduced clauses and relevant variables
     * @throws CspHandlerException if the computation is aborted by the handler
     */
    ReductionResult toCCSP(final Set<IntegerClause> clauses, final List<IntegerVariable> variables,
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
                            convertToCCSP(ll, result.getFrontierAuxiliaryVariables(), context, cf, handler);
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

    ReductionResult variablesToCCSP(final Collection<IntegerVariable> variables,
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
                newClauses.addAll(convertToCCSP(new OpXY(OpXY.Operator.LE, v, cf.constant(ub)), context, cf, handler));
            }
            if (m > 1 && lb != 0) {
                newClauses.addAll(convertToCCSP(new OpXY(OpXY.Operator.LE, cf.constant(lb), v), context, cf, handler));
            }
        }
        return new ReductionResult(newClauses, frontierVariables);
    }

    private Set<IntegerClause> convertToCCSP(final RCSPLiteral literal,
                                             final List<IntegerVariable> frontierVariables,
                                             final CompactOrderEncodingContext context,
                                             final CspFactory cf, final ComputationHandler handler)
            throws CspHandlerException {
        if (literal instanceof EqMul) {
            return convertToCCSP((EqMul) literal, frontierVariables, context, cf, handler);
        } else if (literal instanceof OpAdd) {
            return convertToCCSP((OpAdd) literal, frontierVariables, context, cf, handler);
        } else if (literal instanceof OpXY) {
            return convertToCCSP((OpXY) literal, context, cf, handler);
        } else {
            throw new RuntimeException("Unknown RCSP Literal: " + literal.getClass());
        }
    }

    private Set<IntegerClause> convertToCCSP(final OpXY lit, final CompactOrderEncodingContext context,
                                             final CspFactory cf, final ComputationHandler handler)
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

    private Set<IntegerClause> convertToCCSP(final OpAdd lit, final List<IntegerVariable> frontierVariables,
                                             final CompactOrderEncodingContext context,
                                             final CspFactory cf, final ComputationHandler handler)
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

    private Set<IntegerClause> convertToCCSP(final EqMul lit, final List<IntegerVariable> frontierVariables,
                                             final CompactOrderEncodingContext context,
                                             final CspFactory cf, final ComputationHandler handler)
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
                return convertToCCSP(new OpXY(OpXY.Operator.LE, z, cf.constant(0)), context, cf, handler);
            } else if (((IntegerConstant) x).getValue() == 1) {
                return convertToCCSP(new OpXY(OpXY.Operator.EQ, z, y), context, cf, handler);
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
                    ret.addAll(convertToCCSP(newLit, frontierVariables, context, cf, handler));
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
                        for (final IntegerClause c : convertToCCSP(newLit, context, cf, handler)) {
                            final IntegerClause.Builder newClause = IntegerClause.clone(c);
                            newClause.addArithmeticLiterals(als);
                            ret.add(newClause.build());
                        }
                    }
                }

                for (int a = 0; a < b; ++a) {
                    final EqMul newLit = new EqMul(ya[a], cf.constant(a), y);
                    ret.addAll(convertToCCSP(newLit, frontierVariables, context, cf, handler));
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
    private Set<IntegerClause> shiftAddToCCSP(final IntegerHolder u, final IntegerHolder s,
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

    private List<IntegerVariable> splitToDigits(final IntegerVariable v) {
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

    private int nDigits(final IntegerHolder v) {
        if (v instanceof IntegerConstant) {
            return calculateOrGetConstDigits((IntegerConstant) v).size();
        } else {
            return context.getDigits((IntegerVariable) v).size();
        }
    }

    private List<Integer> calculateOrGetConstDigits(final IntegerConstant c) {
        if (!context.hasConstDigits(c)) {
            context.addConstDigits(c, intToDigits(c.getValue(), context.getBase()));
        }
        return context.getConstDigits(c);
    }

    static List<Integer> intToDigits(final int c, final int b) {
        final int m = (int) Math.ceil(Math.log(c + 1) / Math.log(b));
        int ub = c;
        final List<Integer> digits = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            digits.add(ub % b);
            ub /= b;
        }
        return digits;
    }

    private LinearExpression nth(final IntegerHolder v, final int n) {
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

    private int nthValue(final IntegerConstant v, final int n) {
        return context.getConstDigits(v).size() > n ? context.getConstDigits(v).get(n) : 0;
    }

    private IntegerVariable newCCSPVariable(final IntegerDomain d, final List<IntegerVariable> frontierVariables) {
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
    Set<IntegerClause> simplifyClause(final IntegerClause clause, final ComputationHandler handler)
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

    private static LinearLiteral le(final LinearExpression lhs, final LinearExpression rhs) {
        final LinearExpression l = LinearExpression.subtract(lhs, rhs);
        return new LinearLiteral(l, LinearLiteral.Operator.LE);
    }

    private static LinearLiteral le(final LinearExpression lhs, final int e) {
        final LinearExpression.Builder l = LinearExpression.builder(lhs);
        l.setB(l.getB() - e);
        return new LinearLiteral(l.build(), LinearLiteral.Operator.LE);
    }

    private static LinearLiteral ge(final LinearExpression lhs, final LinearExpression rhs) {
        return le(rhs, lhs);
    }

    private static LinearLiteral ge(final LinearExpression lhs, final int e) {
        final LinearExpression.Builder l = LinearExpression.builder(lhs);
        l.setB(l.getB() - e);
        l.multiply(-1);
        return new LinearLiteral(l.build(), LinearLiteral.Operator.LE);
    }

    private static LinearExpression add(final LinearExpression... es) {
        final LinearExpression.Builder l = LinearExpression.builder(0);
        for (final LinearExpression e : es) {
            l.add(e);
        }
        return l.build();
    }

    private static LinearExpression add(final LinearExpression lhs, final int e) {
        final LinearExpression.Builder l = LinearExpression.builder(lhs);
        l.setB(l.getB() + e);
        return l.build();
    }

    private static LinearExpression sub(final LinearExpression lhs, final int e) {
        return add(lhs, -e);
    }

    private static LinearExpression mul(final LinearExpression lhs, final int c) {
        final LinearExpression.Builder l = LinearExpression.builder(lhs);
        l.multiply(c);
        return l.build();
    }
}
