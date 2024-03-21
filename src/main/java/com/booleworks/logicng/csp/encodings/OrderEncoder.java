package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.collections.LNGVector;
import com.booleworks.logicng.csp.Csp;
import com.booleworks.logicng.csp.IntegerClause;
import com.booleworks.logicng.csp.IntegerDomain;
import com.booleworks.logicng.csp.IntegerSetDomain;
import com.booleworks.logicng.csp.LinearExpression;
import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.literals.CspLiteral;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;

import java.util.Iterator;

public class OrderEncoder {

    private final Csp csp;

    public OrderEncoder(final Csp csp) {
        this.csp = csp;
    }

    public void encode(final OrderEncodingResult result) {
        for (final IntegerVariable v : this.csp.getIntegerVariables()) {
            encodeVariable(v, result);
        }
        for (final IntegerClause c : this.csp.getClauses()) {
            if (!c.isValid()) {
                encodeClause(c, result);
            }
        }
    }

    private void encodeVariable(final IntegerVariable v, final OrderEncodingResult result) {
        final IntegerDomain domain = v.getDomain();
        final Formula[] clause = new Formula[2];
        int a0 = domain.lb();
        for (int a = a0 + 1; a <= domain.ub(); ++a) {
            if (domain.contains(a)) {
                clause[0] = this.csp.getCspFactory().getFormulaFactory().not(getCodeLE(v, a0, result));
                clause[1] = getCodeLE(v, a, result);
                writeClause(clause, result);
                a0 = a;
            }
        }
    }

    private void encodeClause(final IntegerClause cl, final OrderEncodingResult result) {
        if (!isSimpleClause(cl)) {
            throw new IllegalArgumentException("Cannot encode non-simple clause " + cl.toString());
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
                clause[i] = getCode((LinearLiteral) literal, result);
                i++;
            } else {
                lit = (LinearLiteral) literal;
            }
        }
        if (lit == null) {
            writeClause(clause, result);
        } else {
            encodeLitClause(lit, clause, result);
        }
    }

    private void encodeLitClause(final LinearLiteral lit, Formula[] clause, final OrderEncodingResult result) {
        if (lit.getOperator() == LinearLiteral.Operator.EQ || lit.getOperator() == LinearLiteral.Operator.NE) {
            throw new RuntimeException("Invalid operator for order encoding " + lit);
        }
        if (isSimpleLiteral(lit)) {
            clause = expandArray(clause, 1);
            clause[0] = getCode(lit, result);
            writeClause(clause, result);
        } else {
            final LinearExpression ls = lit.getLinearExpression();
            final IntegerVariable[] vs = lit.getLinearExpression().getVariablesSorted();
            final int n = ls.size();
            clause = expandArray(clause, n);
            encodeLinearExpression(ls, vs, 0, lit.getLinearExpression().getB(), clause, result);
        }
    }

    private void encodeLinearExpression(final LinearExpression exp, final IntegerVariable[] vs, final int i, final int s, final Formula[] clause, final OrderEncodingResult result) {
        if (i >= vs.length - 1) {
            final int a = exp.getA(vs[i]);
            clause[i] = getCodeLE(vs[i], a, -s, result);
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
                    clause[i] = getCodeLE(vs[i], c - 1, result);
                    encodeLinearExpression(exp, vs, i + 1, s + a * c, clause, result);
                }
                clause[i] = getCodeLE(vs[i], ub, result);
                encodeLinearExpression(exp, vs, i + 1, s + a * (ub + 1), clause, result);
            } else {
                if (-lb0 >= 0) {
                    lb = Math.max(lb, -lb0 / a);
                } else {
                    lb = Math.max(lb, (-lb0 + a + 1) / a);
                }
                clause[i] = this.csp.getCspFactory().getFormulaFactory().not(getCodeLE(vs[i], lb - 1, result));
                encodeLinearExpression(exp, vs, i + 1, s + a * (lb - 1), clause, result);
                for (final Iterator<Integer> it = domain.values(lb, ub); it.hasNext(); ) {
                    final int c = it.next();
                    clause[i] = this.csp.getCspFactory().getFormulaFactory().not(getCodeLE(vs[i], c, result));
                    encodeLinearExpression(exp, vs, i + 1, s + a * c, clause, result);
                }
            }
        }
    }

    private Formula getCodeLE(final IntegerVariable left, final int right, final OrderEncodingResult result) {
        final IntegerDomain domain = left.getDomain();
        if (right < domain.lb()) {
            return this.csp.getCspFactory().getFormulaFactory().falsum();
        } else if (right >= domain.ub()) {
            return this.csp.getCspFactory().getFormulaFactory().verum();
        }
        final int index = sizeLE(domain, right) - 1;
        return result.intVariableInstance(left, index);
    }

    private Formula getCodeLE(final IntegerVariable left, final int a, final int b, final OrderEncodingResult result) {
        if (a >= 0) {
            final int c;
            if (b >= 0) {
                c = b / a;
            } else {
                c = (b - a + 1) / a;
            }
            return getCodeLE(left, c, result);
        } else {
            final int c;
            if (b >= 0) {
                c = b / a - 1;
            } else {
                c = (b + a + 1) / a - 1;
            }
            return this.csp.getCspFactory().getFormulaFactory().not(getCodeLE(left, c, result));
        }
    }

    private Formula getCode(final LinearLiteral lit, final OrderEncodingResult result) {
        if (!isSimpleLiteral(lit)) {
            throw new IllegalArgumentException("Encountered non-simple literal in order encoding " + lit.toString());
        }
        if (lit.getOperator() == LinearLiteral.Operator.EQ || lit.getOperator() == LinearLiteral.Operator.NE) {
            throw new IllegalArgumentException("Encountered eq/ne literal in order encoding " + lit);
        }
        final LinearExpression sum = lit.getLinearExpression();
        final int b = sum.getB();
        if (sum.size() == 0) {
            return this.csp.getCspFactory().getFormulaFactory().constant(b <= 0);
        } else {
            final IntegerVariable v = sum.getCoef().firstKey();
            final int a = sum.getA(v);
            return getCodeLE(v, a, -b, result);
        }
    }

    private int sizeLE(final IntegerDomain d, final int value) {
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

    protected static boolean isSimpleClause(final IntegerClause clause) {
        return clause.size() - simpleClauseSize(clause) <= 1;
    }

    protected static boolean isSimpleLiteral(final CspLiteral literal) {
        if (literal instanceof LinearLiteral) {
            final LinearLiteral l = (LinearLiteral) literal;
            return l.getLinearExpression().getCoef().size() <= 1 && (l.getOperator() == LinearLiteral.Operator.LE || l.getOperator() == LinearLiteral.Operator.GE);
        }
        return false;
    }

    protected static int simpleClauseSize(final IntegerClause clause) {
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

    private static void writeClause(final Formula[] clause, final OrderEncodingResult result) {
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
