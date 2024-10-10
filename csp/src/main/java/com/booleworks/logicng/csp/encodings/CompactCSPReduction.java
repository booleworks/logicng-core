package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;
import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.literals.EqMul;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.literals.OpAdd;
import com.booleworks.logicng.csp.literals.OpXY;
import com.booleworks.logicng.csp.literals.RCSPLiteral;
import com.booleworks.logicng.csp.terms.IntegerConstant;
import com.booleworks.logicng.csp.terms.IntegerHolder;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A class grouping operations for reducing RCSP literals to CCSP literals.
 */
public class CompactCSPReduction {
    /**
     * Prefix for CCSP auxiliary variables.
     */
    public static final String AUX_CCSP = "COE_CCSP";

    /**
     * Prefix for digits.
     */
    public static final String AUX_DIGIT = "COE_DIGIT";

    private CompactCSPReduction() {
    }

    /**
     * Reduces a set of arithmetic clauses with RCSP literals to clauses with CCSP literals.
     * @param clauses   RCSP clauses
     * @param variables all variables used in {@code clauses}
     * @param context   the encoding context
     * @param cf        the factory
     * @return reduced clauses and relevant variables
     */
    static ReductionResult toCCSP(final Set<IntegerClause> clauses, final List<IntegerVariable> variables,
                                  final CompactOrderEncodingContext context,
                                  final CspFactory cf) {
        final ReductionResult result = variablesToCCSP(variables, context, cf);
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
                            convertToCCSP(ll, result.getFrontierAuxiliaryVariables(), context, cf);
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
                        final IntegerClause.Builder newC = new IntegerClause.Builder(c);
                        newC.addBooleanLiterals(clause.getBoolLiterals());
                        newC.addArithmeticLiterals(simpleLiterals);
                        result.getClauses().add(newC.build());
                    }
                }
            }
        }
        return result;
    }

    static ReductionResult variablesToCCSP(final Collection<IntegerVariable> variables,
                                           final CompactOrderEncodingContext context, final CspFactory cf) {
        final Set<IntegerClause> newClauses = new LinkedHashSet<>();
        final List<IntegerVariable> frontierVariables = new ArrayList<>();
        for (final IntegerVariable v : variables) {
            final List<IntegerVariable> digits;
            if (context.hasDigits(v)) {
                digits = context.getDigits(v);
            } else {
                digits = splitToDigits(v, context, cf);
                context.addDigits(v, digits);
            }
            frontierVariables.addAll(digits);
            final int lb = v.getDomain().lb();
            final int ub = v.getDomain().ub();
            final int m = context.getDigits(v).size();
            if (m > 1 || ub <= Math.pow(context.getBase(), m) - 1) {
                newClauses.addAll(convertToCCSP(new OpXY(OpXY.Operator.LE, v, cf.constant(ub)), context, cf));
            }
            if (m > 1 && lb != 0) {
                newClauses.addAll(convertToCCSP(new OpXY(OpXY.Operator.LE, cf.constant(lb), v), context, cf));
            }
        }
        return new ReductionResult(newClauses, frontierVariables);
    }

    private static Set<IntegerClause> convertToCCSP(final RCSPLiteral literal,
                                                    final List<IntegerVariable> frontierVariables,
                                                    final CompactOrderEncodingContext context,
                                                    final CspFactory cf) {
        if (literal instanceof EqMul) {
            return convertToCCSP((EqMul) literal, frontierVariables, context, cf);
        } else if (literal instanceof OpAdd) {
            return convertToCCSP((OpAdd) literal, frontierVariables, context, cf);
        } else if (literal instanceof OpXY) {
            return convertToCCSP((OpXY) literal, context, cf);
        } else {
            throw new RuntimeException("Unknown RCSP Literal: " + literal.getClass());
        }
    }

    private static Set<IntegerClause> convertToCCSP(final OpXY lit, final CompactOrderEncodingContext context,
                                                    final CspFactory cf) {
        final Set<IntegerClause> ret = new LinkedHashSet<>();
        final IntegerHolder x = lit.getX();
        final IntegerHolder y = lit.getY();
        final int m = Math.max(nDigits(x, context, cf), nDigits(y, context, cf));

        switch (lit.getOp()) {
            case LE:
                if (x instanceof IntegerConstant || y instanceof IntegerConstant) {
                    for (int i = 0; i < m; ++i) {
                        final IntegerClause.Builder newClause = new IntegerClause.Builder();
                        newClause.addArithmeticLiteral(le(nth(x, i, context), nth(y, i, context)));
                        for (int j = i + 1; j < m; ++j) {
                            newClause.addArithmeticLiteral(le(nth(x, j, context), sub(nth(y, j, context), 1)));
                        }
                        ret.add(newClause.build());
                    }
                } else {
                    final Variable[] s = new Variable[m];
                    for (int i = 1; i < m; ++i) {
                        s[i] = context.newCCSPBoolVariable(cf.getFormulaFactory());
                    }
                    // -s(i+1) or x(i) <= y(i) (when 0 <= i < m - 1)
                    for (int i = 0; i < m - 1; ++i) {
                        ret.add(new IntegerClause(
                                s[i + 1].negate(cf.getFormulaFactory()),
                                le(nth(x, i, context), nth(y, i, context))
                        ));
                    }
                    // x(i) <= y(i) (when i == m - 1)
                    ret.add(new IntegerClause(le(nth(x, m - 1, context), nth(y, m - 1, context))));

                    // -s(i+1) or (x(i) <= y(i) - 1) or s(i) (when 1 <= i < m - 1)
                    for (int i = 1; i < m - 1; ++i) {
                        final IntegerClause.Builder newClause = new IntegerClause.Builder();
                        newClause.addBooleanLiterals(s[i + 1].negate(cf.getFormulaFactory()), s[i]);
                        newClause.addArithmeticLiteral(le(nth(x, i, context), sub(nth(y, i, context), 1)));
                        ret.add(newClause.build());
                    }
                    if (m > 1) {
                        // (x(i) <= y(i) - 1) or s(i) (when i == m - 1)
                        ret.add(new IntegerClause(
                                s[m - 1],
                                le(nth(x, m - 1, context), sub(nth(y, m - 1, context), 1))
                        ));
                    }
                }
                break;
            case EQ:
                for (int i = 0; i < m; ++i) {
                    ret.add(new IntegerClause(le(nth(x, i, context), nth(y, i, context))));
                    ret.add(new IntegerClause(ge(nth(x, i, context), nth(y, i, context))));
                }
                break;
            case NE:
                final IntegerClause.Builder newClause = new IntegerClause.Builder();
                for (int i = 0; i < m; ++i) {
                    newClause.addArithmeticLiterals(le(nth(x, i, context), sub(nth(y, i, context), 1)));
                    newClause.addArithmeticLiterals(ge(sub(nth(x, i, context), 1), nth(y, i, context)));
                }
                ret.addAll(CompactOrderReduction.simplifyClause(newClause.build(), context, cf.getFormulaFactory()));
                break;
        }
        return ret;
    }

    private static Set<IntegerClause> convertToCCSP(final OpAdd lit, final List<IntegerVariable> frontierVariables,
                                                    final CompactOrderEncodingContext context,
                                                    final CspFactory cf) {
        final Set<IntegerClause> ret = new LinkedHashSet<>();
        final int b = context.getBase();
        final IntegerHolder x = lit.getX();
        final IntegerHolder y = lit.getY();
        final IntegerHolder z = lit.getZ();
        final int m = Math.max(Math.max(nDigits(x, context, cf), nDigits(y, context, cf)), nDigits(z, context, cf));
        final LinearExpression[] c = new LinearExpression[m];

        for (int i = 1; i < m; ++i) {
            c[i] = new LinearExpression(newCCSPVariable(IntegerDomain.of(0, 1), frontierVariables, context, cf));
        }

        // lhs = { z_0 + c_1 * b, ..., z_{m-1} }
        final LinearExpression[] lhs = new LinearExpression[m];
        for (int i = 0; i < m - 1; ++i) {
            lhs[i] = add(nth(z, i, context), mul(c[i + 1], b));
        }
        lhs[m - 1] = nth(z, m - 1, context);

        // rhs = { x_0 + y_0, x_1 + y_1 + c_1, ... }
        final LinearExpression[] rhs = new LinearExpression[m];
        rhs[0] = add(nth(x, 0, context), nth(y, 0, context));
        for (int i = 1; i < m; ++i) {
            rhs[i] = add(nth(x, i, context), nth(y, i, context), c[i]);
        }

        switch (lit.getOp()) {
            case LE: {
                final Variable[] s = new Variable[m];
                for (int i = 1; i < m; ++i) {
                    s[i] = context.newCCSPBoolVariable(cf.getFormulaFactory());
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
                    final IntegerClause.Builder newClause = new IntegerClause.Builder();
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
                    s[i] = context.newCCSPBoolVariable(cf.getFormulaFactory());
                }

                // -s(i+1) or (z(i) + B*c(i+1) <= x(i) + y(i) + c(i)) (when 0 <= i < m - 1)
                for (int i = 0; i < m - 1; i++) {
                    ret.add(new IntegerClause(s[i + 1].negate(cf.getFormulaFactory()), le(lhs[i], rhs[i])));
                }
                // z(i) >= x(i) + y(i) + c(i) (when i == m - 1)
                ret.add(new IntegerClause(ge(lhs[m - 1], rhs[m - 1])));

                // -s(i+1) or (z(i) + B * c(i+1) <= x(i) + y(i) + c(i) - 1) or s(i)
                // (when 1 <= i < m - 1)
                for (int i = 1; i < m - 1; ++i) {
                    final IntegerClause.Builder newClause = new IntegerClause.Builder();
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
                final IntegerClause.Builder newClause = new IntegerClause.Builder();
                for (int i = 0; i < m; ++i) {
                    newClause.addArithmeticLiterals(
                            le(lhs[i], sub(rhs[i], 1)),
                            ge(sub(lhs[i], 1), rhs[i])
                    );
                }
                ret.addAll(CompactOrderReduction.simplifyClause(newClause.build(), context, cf.getFormulaFactory()));

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

    private static Set<IntegerClause> convertToCCSP(final EqMul lit, final List<IntegerVariable> frontierVariables,
                                                    final CompactOrderEncodingContext context,
                                                    final CspFactory cf) {
        final int b = context.getBase();
        final IntegerHolder x = lit.getX();
        final IntegerVariable y = lit.getY();
        final IntegerHolder z = lit.getZ();
        final int m = Math.max(Math.max(nDigits(x, context, cf), nDigits(y, context, cf)), nDigits(z, context, cf));
        final Set<IntegerClause> ret = new LinkedHashSet<>();

        if (x instanceof IntegerConstant && ((IntegerConstant) x).getValue() < b) {
            if (((IntegerConstant) x).getValue() == 0) {
                assert z instanceof IntegerVariable;
                return convertToCCSP(new OpXY(OpXY.Operator.LE, z, cf.constant(0)), context, cf);
            } else if (((IntegerConstant) x).getValue() == 1) {
                return convertToCCSP(new OpXY(OpXY.Operator.EQ, z, y), context, cf);
            }
            final IntegerHolder[] v = new IntegerHolder[m];
            final int a = ((IntegerConstant) x).getValue();
            for (int i = 0; i < m; ++i) {
                final IntegerDomain d = IntegerDomain.of(0, a * nth(y, i, context).getDomain().ub());
                final IntegerVariable vi = newCCSPVariable(d, frontierVariables, context, cf);
                v[i] = vi;
            }

            for (int i = 0; i < m; ++i) {
                final LinearExpression left = add(mul(nth(v[i], 1, context), b), nth(v[i], 0, context));
                final LinearExpression right = mul(nth(y, i, context), a);
                ret.add(new IntegerClause(le(left, right)));
                ret.add(new IntegerClause(ge(left, right)));
            }

            final LinearExpression[] c = new LinearExpression[m];
            final IntegerDomain d = IntegerDomain.of(0, 1);
            for (int i = 2; i < m; ++i) {
                c[i] = new LinearExpression(newCCSPVariable(d, frontierVariables, context, cf));
            }

            for (int i = 0; i < m; ++i) {
                final LinearExpression lhs;
                if (i == 0 || i == m - 1) {
                    lhs = nth(z, i, context);
                } else {
                    lhs = add(nth(z, i, context), mul(c[i + 1], b));
                }

                final LinearExpression rhs;
                if (i == 0) {
                    rhs = nth(v[i], 0, context);
                } else if (i == 1) {
                    rhs = add(nth(v[i], 0, context), nth(v[i - 1], 1, context));
                } else {
                    rhs = add(nth(v[i], 0, context), nth(v[i - 1], 1, context), c[i]);
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
                    d = IntegerDomain.of(0, Math.min(nthValue((IntegerConstant) x, i, context) * uby, ubz));
                } else {
                    d = IntegerDomain.of(0, Math.min((b - 1) * uby, ubz));
                }
                w[i] = newCCSPVariable(d, frontierVariables, context, cf);
                ubz /= b;
            }

            if (x instanceof IntegerConstant) {
                for (int i = 0; i < m; ++i) {
                    final EqMul newLit = new EqMul(w[i], cf.constant(nthValue((IntegerConstant) x, i, context)), y);
                    ret.addAll(convertToCCSP(newLit, frontierVariables, context, cf));
                }
            } else {
                final IntegerVariable[] ya = new IntegerVariable[b];
                for (int a = 0; a < b; ++a) {
                    ya[a] = newCCSPVariable(IntegerDomain.of(0, a * uby), frontierVariables, context, cf);
                }

                for (int i = 0; i < m; ++i) {
                    for (int a = 0; a < b; ++a) {
                        final List<ArithmeticLiteral> als = List.of(
                                le(nth(x, i, context), a - 1),
                                ge(nth(x, i, context), a + 1)
                        );

                        final OpXY newLit = new OpXY(OpXY.Operator.EQ, w[i], ya[a]);
                        for (final IntegerClause c : convertToCCSP(newLit, context, cf)) {
                            final IntegerClause.Builder newClause = new IntegerClause.Builder(c);
                            newClause.addArithmeticLiterals(als);
                            ret.add(newClause.build());
                        }
                    }
                }

                for (int a = 0; a < b; ++a) {
                    final EqMul newLit = new EqMul(ya[a], cf.constant(a), y);
                    ret.addAll(convertToCCSP(newLit, frontierVariables, context, cf));
                }
            }

            // [z = Sum_(i = 0)^(m - 1) B^i w_i]
            final IntegerHolder[] zi = new IntegerHolder[m];
            zi[m - 1] = w[m - 1];
            for (int i = m - 2; i > 0; --i) {
                final IntegerDomain d = IntegerDomain.of(0, b * zi[i + 1].getDomain().ub() + w[i].getDomain().ub());
                final IntegerVariable zii = newCCSPVariable(d, frontierVariables, context, cf);
                zi[i] = zii;
            }
            zi[0] = z;

            if (m == 1) {
                final LinearExpression exp1 = nth(z, 0, context);
                final LinearExpression exp2 = nth(w[0], 0, context);
                ret.add(new IntegerClause(le(exp1, exp2)));
                ret.add(new IntegerClause(ge(exp1, exp2)));
            } else {
                for (int i = 0; i < m - 1; ++i) {
                    ret.addAll(shiftAddToCCSP(zi[i], zi[i + 1], w[i], frontierVariables, context, cf));
                }
            }
        }
        return ret;
    }

    /**
     * u = b*s+t
     */
    private static Set<IntegerClause> shiftAddToCCSP(final IntegerHolder u, final IntegerHolder s,
                                                     final IntegerHolder t,
                                                     final List<IntegerVariable> frontierVariables,
                                                     final CompactOrderEncodingContext context, final CspFactory cf) {
        final int b = context.getBase();
        final int m = 1 + Math.max(nDigits(s, context, cf), nDigits(t, context, cf));
        final Set<IntegerClause> ret = new LinkedHashSet<>();

        final LinearExpression[] c = new LinearExpression[m];
        final IntegerDomain d = IntegerDomain.of(0, 1);
        for (int i = 2; i < m; i++) {
            c[i] = new LinearExpression(newCCSPVariable(d, frontierVariables, context, cf));
        }

        for (int i = 0; i < m; ++i) {
            final LinearExpression lhs;
            if (i == 0 || i == m - 1) {
                lhs = nth(u, i, context);
            } else {
                lhs = add(nth(u, i, context), mul(c[i + 1], b));
            }

            final LinearExpression rhs;
            if (i == 0) {
                rhs = nth(t, i, context);
            } else if (i == 1) {
                rhs = add(nth(t, 1, context), nth(s, 0, context));
            } else if (i == m - 1) {
                rhs = add(nth(s, i - 1, context), c[i]);
            } else {
                rhs = add(nth(t, i, context), nth(s, i - 1, context), c[i]);
            }

            ret.add(new IntegerClause(le(lhs, rhs)));
            ret.add(new IntegerClause(ge(lhs, rhs)));
        }
        return ret;
    }

    private static List<IntegerVariable> splitToDigits(final IntegerVariable v,
                                                       final CompactOrderEncodingContext context, final CspFactory cf) {
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

    private static int nDigits(final IntegerHolder v, final CompactOrderEncodingContext context, final CspFactory cf) {
        if (v instanceof IntegerConstant) {
            return calculateOrGetConstDigits((IntegerConstant) v, context, cf).size();
        } else {
            return context.getDigits((IntegerVariable) v).size();
        }
    }

    private static List<Integer> calculateOrGetConstDigits(final IntegerConstant c,
                                                           final CompactOrderEncodingContext context,
                                                           final CspFactory cf) {
        if (!context.hasConstDigits(c)) {
            context.addConstDigits(c, intToDigits(c, context.getBase()));
        }
        return context.getConstDigits(c);
    }

    private static List<Integer> intToDigits(final IntegerConstant c, final int b) {
        final int m = (int) Math.ceil(Math.log(c.getValue() + 1) / Math.log(b));
        int ub = c.getValue();
        final List<Integer> digits = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            digits.add(ub % b);
            ub /= b;
        }
        return digits;
    }

    private static LinearExpression nth(final IntegerHolder v, final int n, final CompactOrderEncodingContext context) {
        if (v instanceof IntegerConstant) {
            assert context.getConstDigits((IntegerConstant) v) != null;
            return new LinearExpression(nthValue((IntegerConstant) v, n, context));
        } else {
            final List<IntegerVariable> digits = context.getDigits((IntegerVariable) v);
            if (digits.size() > n) {
                return new LinearExpression(digits.get(n));
            } else {
                return new LinearExpression(0);
            }
        }
    }

    private static int nthValue(final IntegerConstant v, final int n, final CompactOrderEncodingContext context) {
        return context.getConstDigits(v).size() > n ? context.getConstDigits(v).get(n) : 0;
    }

    private static IntegerVariable newCCSPVariable(final IntegerDomain d, final List<IntegerVariable> frontierVariables,
                                                   final CompactOrderEncodingContext context,
                                                   final CspFactory cf) {
        final IntegerVariable v = context.newCCSPVariable(d, cf);
        final List<IntegerVariable> digits = splitToDigits(v, context, cf);
        context.addDigits(v, digits);
        frontierVariables.addAll(digits);
        return v;
    }

    private static LinearLiteral le(final LinearExpression lhs, final LinearExpression rhs) {
        final LinearExpression l = LinearExpression.subtract(lhs, rhs);
        return new LinearLiteral(l, LinearLiteral.Operator.LE);
    }

    private static LinearLiteral le(final LinearExpression lhs, final int e) {
        final LinearExpression.Builder l = new LinearExpression.Builder(lhs);
        l.setB(l.getB() - e);
        return new LinearLiteral(l.build(), LinearLiteral.Operator.LE);
    }

    private static LinearLiteral ge(final LinearExpression lhs, final LinearExpression rhs) {
        return le(rhs, lhs);
    }

    private static LinearLiteral ge(final LinearExpression lhs, final int e) {
        final LinearExpression.Builder l = new LinearExpression.Builder(lhs);
        l.setB(l.getB() - e);
        l.multiply(-1);
        return new LinearLiteral(l.build(), LinearLiteral.Operator.LE);
    }

    private static LinearExpression add(final LinearExpression... es) {
        final LinearExpression.Builder l = new LinearExpression.Builder(0);
        for (final LinearExpression e : es) {
            l.add(e);
        }
        return l.build();
    }

    private static LinearExpression add(final LinearExpression lhs, final int e) {
        final LinearExpression.Builder l = new LinearExpression.Builder(lhs);
        l.setB(l.getB() + e);
        return l.build();
    }

    private static LinearExpression sub(final LinearExpression lhs, final int e) {
        return add(lhs, -e);
    }

    private static LinearExpression mul(final LinearExpression lhs, final int c) {
        final LinearExpression.Builder l = new LinearExpression.Builder(lhs);
        l.multiply(c);
        return l.build();
    }
}
