package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.csp.Csp;
import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.IntegerClause;
import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.IntegerSetDomain;
import com.booleworks.logicng.csp.LinearExpression;
import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.literals.CspLiteral;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;

import java.util.Iterator;
import java.util.Set;

public class OrderEncoding {
    public static void encode(final Csp csp, final CspEncodingContext context, final EncodingResult result, final CspFactory cf) {
        for (final IntegerVariable v : csp.getIntegerVariables()) {
            encodeVariable(v, context, result, cf);
        }
        encodeClauses(csp.getClauses(), context, result, cf);
    }

    public static void encodeVariable(final IntegerVariable v, final CspEncodingContext context, final EncodingResult result, final CspFactory cf) {
        final FormulaFactory f = cf.formulaFactory();
        final IntegerDomain domain = v.getDomain();
        final Formula[] clause = new Formula[2];
        int a0 = domain.lb();
        for (int a = a0 + 1; a <= domain.ub(); ++a) {
            if (domain.contains(a)) {
                clause[0] = getCodeLE(v, a0, context, result, cf.formulaFactory()).negate(f);
                clause[1] = getCodeLE(v, a, context, result, cf.formulaFactory());
                writeClause(clause, result);
                a0 = a;
            }
        }
    }

    public static void encodeClauses(final Set<IntegerClause> clauses, final CspEncodingContext context, final EncodingResult result, final CspFactory cf) {
        for (final IntegerClause c : clauses) {
            if (!c.isValid()) {
                encodeClause(c, context, result, cf);
            }
        }
    }

    static void encodeClause(final IntegerClause cl, final CspEncodingContext context, final EncodingResult result, final CspFactory cf) {
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
                clause[i] = getCode((LinearLiteral) literal, context, result, cf.formulaFactory());
                i++;
            } else {
                lit = (LinearLiteral) literal;
            }
        }
        if (lit == null) {
            writeClause(clause, result);
        } else {
            encodeLitClause(lit, clause, context, result, cf);
        }
    }

    static void encodeLitClause(final LinearLiteral lit, Formula[] clause, final CspEncodingContext context, final EncodingResult result, final CspFactory cf) {
        if (lit.getOperator() == LinearLiteral.Operator.EQ || lit.getOperator() == LinearLiteral.Operator.NE) {
            throw new RuntimeException("Invalid operator for order encoding " + lit);
        }
        if (isSimpleLiteral(lit)) {
            clause = expandArray(clause, 1);
            clause[0] = getCode(lit, context, result, cf.formulaFactory());
            writeClause(clause, result);
        } else {
            final LinearExpression ls = lit.getLinearExpression();
            final IntegerVariable[] vs = lit.getLinearExpression().getVariablesSorted();
            final int n = ls.size();
            clause = expandArray(clause, n);
            encodeLinearExpression(ls, vs, 0, lit.getLinearExpression().getB(), clause, context, result, cf);
        }
    }

    static void encodeLinearExpression(final LinearExpression exp, final IntegerVariable[] vs, final int i, final int s, final Formula[] clause,
                                       final CspEncodingContext context, final EncodingResult result, final CspFactory cf) {
        if (i >= vs.length - 1) {
            final int a = exp.getA(vs[i]);
            clause[i] = getCodeLE(vs[i], a, -s, context, result, cf.formulaFactory());
            writeClause(clause, result);
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
                    clause[i] = getCodeLE(vs[i], c - 1, context, result, cf.formulaFactory());
                    encodeLinearExpression(exp, vs, i + 1, s + a * c, clause, context, result, cf);
                }
                clause[i] = getCodeLE(vs[i], ub, context, result, cf.formulaFactory());
                encodeLinearExpression(exp, vs, i + 1, s + a * (ub + 1), clause, context, result, cf);
            } else {
                if (-lb0 >= 0) {
                    lb = Math.max(lb, -lb0 / a);
                } else {
                    lb = Math.max(lb, (-lb0 + a + 1) / a);
                }
                clause[i] = getCodeLE(vs[i], lb - 1, context, result, cf.formulaFactory()).negate(cf.formulaFactory());
                encodeLinearExpression(exp, vs, i + 1, s + a * (lb - 1), clause, context, result, cf);
                for (final Iterator<Integer> it = domain.values(lb, ub); it.hasNext(); ) {
                    final int c = it.next();
                    clause[i] = getCodeLE(vs[i], c, context, result, cf.formulaFactory()).negate(cf.formulaFactory());
                    encodeLinearExpression(exp, vs, i + 1, s + a * c, clause, context, result, cf);
                }
            }
        }
    }

    static Formula getCodeLE(final IntegerVariable left, final int right, final CspEncodingContext context, final EncodingResult result, final FormulaFactory f) {
        final IntegerDomain domain = left.getDomain();
        if (right < domain.lb()) {
            return f.falsum();
        } else if (right >= domain.ub()) {
            return f.verum();
        }
        final int index = sizeLE(domain, right) - 1;
        return context.intVariableInstance(left, index, result);
    }

    static Formula getCodeLE(final IntegerVariable left, final int a, final int b, final CspEncodingContext context, final EncodingResult result, final FormulaFactory f) {
        if (a >= 0) {
            final int c;
            if (b >= 0) {
                c = b / a;
            } else {
                c = (b - a + 1) / a;
            }
            return getCodeLE(left, c, context, result, f);
        } else {
            final int c;
            if (b >= 0) {
                c = b / a - 1;
            } else {
                c = (b + a + 1) / a - 1;
            }
            return getCodeLE(left, c, context, result, f).negate(f);
        }
    }

    static Formula getCode(final LinearLiteral lit, final CspEncodingContext context, final EncodingResult result, final FormulaFactory f) {
        if (!isSimpleLiteral(lit)) {
            throw new IllegalArgumentException("Encountered non-simple literal in order encoding " + lit.toString());
        }
        if (lit.getOperator() == LinearLiteral.Operator.EQ || lit.getOperator() == LinearLiteral.Operator.NE) {
            throw new IllegalArgumentException("Encountered eq/ne literal in order encoding " + lit);
        }
        final LinearExpression sum = lit.getLinearExpression();
        final int b = sum.getB();
        if (sum.size() == 0) {
            return f.constant(b <= 0);
        } else {
            final IntegerVariable v = sum.getCoef().firstKey();
            final int a = sum.getA(v);
            return getCodeLE(v, a, -b, context, result, f);
        }
    }

    static int sizeLE(final IntegerDomain d, final int value) {
        if (value < d.lb()) {
            return 0;
        }
        if (value >= d.ub()) {
            return d.size();
        }
        if (d.isContiguous()) {
            return value - d.lb() + 1;
        } else {
            return ((IntegerSetDomain) d).headSet(value + 1).size();
        }
    }

    static boolean isSimpleClause(final IntegerClause clause) {
        return clause.size() - simpleClauseSize(clause) <= 1;
    }

    static boolean isSimpleLiteral(final CspLiteral literal) {
        if (literal instanceof LinearLiteral) {
            final LinearLiteral l = (LinearLiteral) literal;
            return l.getLinearExpression().getCoef().size() <= 1 && l.getOperator() == LinearLiteral.Operator.LE;
        }
        return false;
    }

    static int simpleClauseSize(final IntegerClause clause) {
        int simpleLiterals = clause.getBoolLiterals().size();
        for (final ArithmeticLiteral lit : clause.getArithmeticLiterals()) {
            if (isSimpleLiteral(lit)) {
                ++simpleLiterals;
            }
        }
        return simpleLiterals;
    }

    static Formula[] expandArray(final Formula[] clause0, final int n) {
        final Formula[] clause = new Formula[clause0.length + n];
        System.arraycopy(clause0, 0, clause, n, clause0.length);
        return clause;
    }

    static void writeClause(final Formula[] clause, final EncodingResult result) {
        final LNGVector<Literal> vec = new LNGVector<>();
        for (final Formula literal : clause) {
            switch (literal.type()) {
                case TRUE:
                    return;
                case FALSE:
                    break;
                case LITERAL:
                    vec.push((Literal) literal);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported formula type in order encoding:" + literal.type());
            }
        }
        result.addClause(vec);
    }
}
