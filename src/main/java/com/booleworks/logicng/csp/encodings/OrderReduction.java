package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.Csp;
import com.booleworks.logicng.csp.IntegerClause;
import com.booleworks.logicng.csp.LinearExpression;
import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrderReduction {
    public static final int MAX_LINEAR_EXPRESSION_SIZE = 1024;
    public static final int SPLITS = 2;
    public static final String AUX_PREFIX1 = "RS";

    public static Csp reduce(final Csp csp, final CspEncodingContext context, final FormulaFactory f) {
        final Csp.Builder newCsp = new Csp.Builder(csp);

        Set<IntegerClause> clauses = split(newCsp.getClauses(), context, newCsp);
        clauses = simplify(clauses, context, newCsp, f);
        clauses = toLinearLe(clauses, context, newCsp, f);
        newCsp.updateClauses(clauses);
        return newCsp.build();
    }

    public static Set<IntegerClause> reduce(final Set<IntegerClause> clauses, final CspEncodingContext context, final FormulaFactory f) {
        return toLinearLe(simplify(split(clauses, context, null), context, null, f), context, null, f);
    }

    private static Set<IntegerClause> split(final Set<IntegerClause> clauses, final CspEncodingContext context, final Csp.Builder csp) {
        final TreeSet<IntegerClause> newClauses = new TreeSet<>();
        for (final IntegerClause c : clauses) {
            final SortedSet<ArithmeticLiteral> newArithLits = c.getArithmeticLiterals().stream().map(al -> {
                if (al instanceof LinearLiteral) {
                    final LinearLiteral ll = (LinearLiteral) al;
                    final LinearExpression sum = simplifyLinearExpression(new LinearExpression.Builder(ll.getLinearExpression()), true, newClauses, context, csp).build();
                    return new LinearLiteral(sum, ll.getOperator());
                } else {
                    return al;
                }
            }).collect(Collector.of(TreeSet::new, TreeSet::add, (left, right) -> {
                left.addAll(right);
                return left;
            }));
            newClauses.add(new IntegerClause(c.getBoolLiterals(), newArithLits));
        }
        return newClauses;
    }

    private static Set<IntegerClause> simplify(final Set<IntegerClause> clauses, final CspEncodingContext context, final Csp.Builder csp, final FormulaFactory f) {
        return clauses.stream().flatMap(clause -> {
            if (clause.isValid()) {
                return null;
            } else if (OrderEncoding.isSimpleClause(clause)) {
                return Stream.of(clause);
            } else {
                return simplifyClause(clause, Collections.emptySortedSet(), context, csp, f).stream();
            }
        }).collect(Collectors.toSet());
    }

    private static Set<IntegerClause> toLinearLe(final Set<IntegerClause> clauses, final CspEncodingContext context, final Csp.Builder csp, final FormulaFactory f) {
        return clauses.stream().flatMap(c -> {
            if (c.size() == OrderEncoding.simpleClauseSize(c)) {
                return Stream.of(c);
            } else {
                assert c.size() == OrderEncoding.simpleClauseSize(c) + 1;
                final ArithmeticLiteral al = c.getArithmeticLiterals().first();
                if (al instanceof LinearLiteral) {
                    return reduceLinearLiteralToLinearLE((LinearLiteral) al, c.getBoolLiterals(), context, csp, f).stream();
                } else {
                    throw new IllegalArgumentException("Invalid literal for order encoding reduction: " + al.getClass());
                }
            }
        }).collect(Collectors.toSet());
    }

    private static Set<IntegerClause> reduceLinearLiteralToLinearLE(final LinearLiteral literal, final SortedSet<Literal> boolLiterals,
                                                                    final CspEncodingContext context, final Csp.Builder csp, final FormulaFactory f) {
        switch (literal.getOperator()) {
            case LE:
                final TreeSet<ArithmeticLiteral> lits = new TreeSet<>();
                lits.add(literal);
                return Collections.singleton(new IntegerClause(boolLiterals, lits));
            case EQ:
                final TreeSet<ArithmeticLiteral> litsA = new TreeSet<>();
                litsA.add(new LinearLiteral(literal.getLinearExpression(), LinearLiteral.Operator.LE));
                final IntegerClause c1 = new IntegerClause(boolLiterals, litsA);
                final LinearExpression.Builder ls = new LinearExpression.Builder(literal.getLinearExpression());
                ls.multiply(-1);
                final TreeSet<ArithmeticLiteral> litsB = new TreeSet<>();
                litsB.add(new LinearLiteral(ls.build(), LinearLiteral.Operator.LE));
                final IntegerClause c2 = new IntegerClause(boolLiterals, litsB);
                return Set.of(c1, c2);
            case NE:
                final LinearExpression.Builder ls1 = new LinearExpression.Builder(literal.getLinearExpression());
                ls1.setB(ls1.getB() + 1);
                final LinearExpression.Builder ls2 = new LinearExpression.Builder(literal.getLinearExpression());
                ls2.multiply(-1);
                ls2.setB(ls2.getB() + 1);
                final TreeSet<ArithmeticLiteral> litsNe = new TreeSet<>();
                litsNe.add(new LinearLiteral(ls1.build(), LinearLiteral.Operator.LE));
                litsNe.add(new LinearLiteral(ls2.build(), LinearLiteral.Operator.LE));
                final IntegerClause newClause = new IntegerClause(Collections.emptySortedSet(), litsNe);
                return simplifyClause(newClause, boolLiterals, context, csp, f);
            default:
                throw new IllegalArgumentException("Invalid operator of linear expression for order encoding reduction: " + literal.getOperator());

        }
    }

    private static Set<IntegerClause> simplifyClause(final IntegerClause clause, final SortedSet<Literal> initBoolLiterals, final CspEncodingContext context,
                                                     final Csp.Builder csp, final FormulaFactory f) {
        final TreeSet<IntegerClause> newClauses = new TreeSet<>();
        final TreeSet<ArithmeticLiteral> newArithLiterals = new TreeSet<>();
        final TreeSet<Literal> newBoolLiterals = new TreeSet<>(initBoolLiterals);
        for (final ArithmeticLiteral literal : clause.getArithmeticLiterals()) {
            if (OrderEncoding.isSimpleLiteral(literal)) {
                newArithLiterals.add(literal);
            } else {
                final Variable p = context.newAuxBoolVariable(f);
                if (csp != null) {
                    csp.addBooleanVariable(p);
                }
                final Literal notP = context.negate(p);
                final TreeSet<Literal> boolLiterals = new TreeSet<>();
                final TreeSet<ArithmeticLiteral> arithLiterals = new TreeSet<>();
                boolLiterals.add(notP);
                arithLiterals.add(literal);
                final IntegerClause newClause = new IntegerClause(boolLiterals, arithLiterals);
                newClauses.add(newClause);
                newBoolLiterals.add(p);
            }
        }
        final IntegerClause c = new IntegerClause(newBoolLiterals, newArithLiterals);
        newClauses.add(c);
        return newClauses;
    }

    private static LinearExpression.Builder simplifyLinearExpression(final LinearExpression.Builder exp, final boolean first, final SortedSet<IntegerClause> clauses,
                                                                     final CspEncodingContext context, final Csp.Builder csp) {
        if (exp.size() <= 1 || !exp.isDomainLargerThan(MAX_LINEAR_EXPRESSION_SIZE)) {
            return exp;
        }
        final int b = exp.getB();
        final LinearExpression.Builder[] es = split(exp.build(), first ? 3 : SPLITS);
        final LinearExpression.Builder result = new LinearExpression.Builder(b);
        for (final LinearExpression.Builder eMut : es) {
            final int factor = eMut.factor();
            if (factor > 1) {
                eMut.divide(factor);
            }
            LinearExpression.Builder simplified = simplifyLinearExpression(eMut, false, clauses, context, csp);
            if (simplified.size() > 1) {
                final IntegerVariable v = context.newAuxIntVariable(AUX_PREFIX1, simplified.getDomain());
                if (csp != null) {
                    csp.addIntegerVariable(v);
                }
                simplified.subtract(new LinearExpression(v));
                final IntegerClause aux = new IntegerClause(new LinearLiteral(simplified.build(), LinearLiteral.Operator.EQ));
                clauses.add(aux);
                simplified = new LinearExpression.Builder(v);
            }
            if (factor > 1) {
                simplified.multiply(factor);
            }
            result.add(simplified.build());
        }
        return result;
    }

    private static LinearExpression.Builder[] split(final LinearExpression exp, final int m) {
        final LinearExpression.Builder[] es = new LinearExpression.Builder[m];
        for (int i = 0; i < m; ++i) {
            es[i] = new LinearExpression.Builder(0);
        }
        final IntegerVariable[] vs = exp.getVariablesSorted();
        for (int i = 0; i < vs.length; i++) {
            final IntegerVariable v = vs[i];
            es[i % m].setA(exp.getA(v), v);
        }
        return es;
    }
}
