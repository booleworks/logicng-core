package com.booleworks.logicng.csp.encodings;

import static com.booleworks.logicng.csp.encodings.OrderEncoder.isSimpleClause;
import static com.booleworks.logicng.csp.encodings.OrderEncoder.isSimpleLiteral;
import static com.booleworks.logicng.csp.encodings.OrderEncoder.simpleClauseSize;

import com.booleworks.logicng.csp.Csp;
import com.booleworks.logicng.csp.IntegerClause;
import com.booleworks.logicng.csp.LinearExpression;
import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CspReduction {
    public static final int MAX_LINEAR_EXPRESSION_SIZE = 1024;
    public static final int SPLITS = 2;
    public static final String AUX_PREFIX1 = "RS";
    public static final String AUX_PREFIX2 = "R";
    public static final String AUX_PREFIX3 = "RL";

    public static void reduce(final Csp.Builder csp) {
        split(csp);
        simplify(csp);
        toLinearLe(csp);
    }

    private static void split(final Csp.Builder csp) {
        final ArrayList<IntegerClause> newClauses = new ArrayList<>();
        for (final IntegerClause c : csp.getClauses()) {
            final List<ArithmeticLiteral> newArithLits = c.getArithmeticLiterals().stream().map(al -> {
                if (al instanceof LinearLiteral) {
                    final LinearLiteral ll = (LinearLiteral) al;
                    final LinearExpression sum = simplifyLinearExpression(new LinearExpression.Mutable(ll.getLinearExpression()), true, newClauses, csp).build();
                    return new LinearLiteral(sum, ll.getOperator());
                } else {
                    return al;
                }
            }).collect(Collectors.toList());
            newClauses.add(new IntegerClause(c.getBoolLiterals(), newArithLits));
        }
        csp.setClauses(newClauses);
    }

    private static void simplify(final Csp.Builder csp) {
        final List<IntegerClause> newClauses = csp.getClauses().stream().flatMap(clause -> {
            if (clause.isValid()) {
                return null;
            } else if (isSimpleClause(clause)) {
                return Stream.of(clause);
            } else {
                return simplifyClause(clause, csp, Collections.emptyList()).stream();
            }
        }).collect(Collectors.toList());
        csp.setClauses(newClauses);
    }

    private static void toLinearLe(final Csp.Builder csp) {
        final List<IntegerClause> newClauses = csp.getClauses().stream().flatMap(c -> {
            if (c.size() == OrderEncoder.simpleClauseSize(c)) {
                return Stream.of(c);
            } else {
                assert c.size() == simpleClauseSize(c) + 1;
                final ArithmeticLiteral al = c.getArithmeticLiterals().get(0);
                if (al instanceof LinearLiteral) {
                    return reduceLinearLiteralToLinearLE((LinearLiteral) al, c.getBoolLiterals(), csp).stream();
                } else {
                    throw new IllegalArgumentException("Invalid literal for order encoding reduction: " + al.getClass());
                }
            }
        }).collect(Collectors.toList());
        csp.setClauses(newClauses);
    }

    private static List<IntegerClause> reduceLinearLiteralToLinearLE(final LinearLiteral literal, final List<Literal> boolLiterals, final Csp.Builder csp) {
        switch (literal.getOperator()) {
            case LE:
                return Collections.singletonList(new IntegerClause(boolLiterals, Collections.singletonList(literal)));
            case EQ:
                final IntegerClause c1 = new IntegerClause(boolLiterals, Collections.singletonList(new LinearLiteral(literal.getLinearExpression(), LinearLiteral.Operator.LE)));
                final LinearExpression.Mutable ls = new LinearExpression.Mutable(literal.getLinearExpression());
                ls.multiply(-1);
                final IntegerClause c2 = new IntegerClause(boolLiterals, Collections.singletonList(new LinearLiteral(ls, LinearLiteral.Operator.LE)));
                return Arrays.asList(c1, c2);
            case NE:
                final LinearExpression.Mutable ls1 = new LinearExpression.Mutable(literal.getLinearExpression());
                ls1.setB(ls1.getB() + 1);
                final LinearExpression.Mutable ls2 = new LinearExpression.Mutable(literal.getLinearExpression());
                ls2.multiply(-1);
                ls2.setB(ls2.getB() + 1);
                final List<ArithmeticLiteral> lits = new ArrayList<>(2);
                lits.add(new LinearLiteral(ls1, LinearLiteral.Operator.LE));
                lits.add(new LinearLiteral(ls2, LinearLiteral.Operator.LE));
                final IntegerClause newClause = new IntegerClause(Collections.emptyList(), lits);
                return simplifyClause(newClause, csp, boolLiterals);
            default:
                throw new IllegalArgumentException("Invalid operator of linear expression for order encoding reduction: " + literal.getOperator());

        }
    }

    private static List<IntegerClause> simplifyClause(final IntegerClause clause, final Csp.Builder csp, final List<Literal> initBoolLiterals) {
        final List<IntegerClause> newClauses = new ArrayList<>();
        final List<ArithmeticLiteral> newArithLiterals = new ArrayList<>();
        final List<Literal> newBoolLiterals = new ArrayList<>(initBoolLiterals);
        for (final ArithmeticLiteral literal : clause.getArithmeticLiterals()) {
            if (isSimpleLiteral(literal)) {
                newArithLiterals.add(literal);
            } else {
                final Variable p = csp.addAuxBoolVariable(AUX_PREFIX2);
                final Literal notP = p.negate(csp.getCspFactory().getFormulaFactory());
                final IntegerClause newClause = new IntegerClause(Collections.singletonList(notP), Collections.singletonList(literal));
                newClauses.add(newClause);
                newBoolLiterals.add(p);
            }
        }
        final IntegerClause c = new IntegerClause(newBoolLiterals, newArithLiterals);
        newClauses.add(c);
        return newClauses;
    }

    private static LinearExpression.Mutable simplifyLinearExpression(final LinearExpression.Mutable exp, final boolean first, final List<IntegerClause> clauses,
                                                                     final Csp.Builder csp) {
        if (exp.size() <= 1 || !exp.isDomainLargerThan(MAX_LINEAR_EXPRESSION_SIZE)) {
            return exp;
        }
        final LinearExpression.Mutable[] es = split(exp, first ? 3 : SPLITS);
        final LinearExpression.Mutable result = new LinearExpression.Mutable(exp.getB());
        for (final LinearExpression.Mutable eMut : es) {
            final int factor = eMut.factor();
            if (factor > 1) {
                eMut.divide(factor);
            }
            LinearExpression.Mutable simplified = simplifyLinearExpression(eMut, false, clauses, csp);
            if (simplified.size() > 1) {
                final IntegerVariable v = csp.addAuxIntVariable(AUX_PREFIX1, simplified.getDomain());
                simplified.subtract(new LinearExpression(v));
                final IntegerClause aux = new IntegerClause(new LinearLiteral(simplified, LinearLiteral.Operator.EQ));
                clauses.add(aux);
                simplified = new LinearExpression.Mutable(v);
            }
            if (factor > 1) {
                simplified.multiply(factor);
            }
            result.add(simplified);
        }
        return result;
    }

    public static LinearExpression.Mutable[] split(final LinearExpression exp, final int m) {
        final LinearExpression.Mutable[] es = new LinearExpression.Mutable[m];
        for (int i = 0; i < m; ++i) {
            es[i] = new LinearExpression.Mutable(0);
        }
        final IntegerVariable[] vs = exp.getVariablesSorted();
        for (int i = 0; i < vs.length; i++) {
            final IntegerVariable v = vs[i];
            es[i % m].setA(exp.getA(v), v);
        }
        return es;
    }

}
