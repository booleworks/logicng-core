package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.literals.ProductLiteral;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class grouping functions for reducing a problem for the order encoding.
 */
public class OrderReduction {
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

    private OrderReduction() {
    }

    /**
     * Reduces a set of arithmetic clauses so that it can be encoded with the order encoding.
     * @param clauses the clauses
     * @param context the encoding context
     * @param cf      the factory
     * @return the reduced problem
     */
    static ReductionResult reduce(final Set<IntegerClause> clauses, final OrderEncodingContext context,
                                  final CspFactory cf) {
        final List<IntegerVariable> auxVars = new ArrayList<>();
        final Set<IntegerClause> newClauses =
                toLinearLe(simplify(split(clauses, auxVars, context, cf), context, cf.getFormulaFactory()), context,
                        cf.getFormulaFactory());
        return new ReductionResult(newClauses, auxVars);
    }

    private static Set<IntegerClause> split(final Set<IntegerClause> clauses,
                                            final List<IntegerVariable> newFrontierAuxVars,
                                            final OrderEncodingContext context,
                                            final CspFactory cf) {
        final Set<IntegerClause> newClauses = new LinkedHashSet<>();
        for (final IntegerClause c : clauses) {
            final Set<ArithmeticLiteral> newArithLits = c.getArithmeticLiterals().stream().map(al -> {
                if (al instanceof LinearLiteral) {
                    final LinearLiteral ll = (LinearLiteral) al;
                    final LinearExpression sum =
                            simplifyLinearExpression(new LinearExpression.Builder(ll.getSum()), true,
                                    newClauses, newFrontierAuxVars, context, cf).build();
                    return new LinearLiteral(sum, ll.getOperator());
                } else {
                    return al;
                }
            }).collect(Collectors.toCollection(LinkedHashSet::new));
            newClauses.add(new IntegerClause(c.getBoolLiterals(), newArithLits));
        }
        return newClauses;
    }

    private static Set<IntegerClause> simplify(final Set<IntegerClause> clauses, final OrderEncodingContext context,
                                               final FormulaFactory f) {
        return clauses.stream().flatMap(clause -> {
            if (clause.isValid()) {
                return null;
            } else if (OrderEncoding.isSimpleClause(clause)) {
                return Stream.of(clause);
            } else {
                return simplifyClause(clause, clause.getBoolLiterals(), context, f).stream();
            }
        }).collect(Collectors.toSet());
    }

    private static Set<IntegerClause> toLinearLe(final Set<IntegerClause> clauses, final OrderEncodingContext context,
                                                 final FormulaFactory f) {
        return clauses.stream().flatMap(c -> {
            if (c.size() == OrderEncoding.simpleClauseSize(c)) {
                return Stream.of(c);
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
                    return reduceLinearLiteralToLinearLE((LinearLiteral) nonSimpleLiteral, simpleLiterals,
                            c.getBoolLiterals(), context, f).stream();
                } else if (nonSimpleLiteral instanceof ProductLiteral) {
                    return reduceProductLiteralToLinearLE((ProductLiteral) nonSimpleLiteral, simpleLiterals,
                            c.getBoolLiterals(), context, f).stream();
                } else {
                    throw new IllegalArgumentException(
                            "Invalid literal for order encoding reduction: " + nonSimpleLiteral.getClass());
                }
            }
        }).collect(Collectors.toSet());
    }

    private static Set<IntegerClause> reduceLinearLiteralToLinearLE(final LinearLiteral literal,
                                                                    final Set<ArithmeticLiteral> simpleLiterals,
                                                                    final Set<Literal> boolLiterals,
                                                                    final OrderEncodingContext context,
                                                                    final FormulaFactory f) {
        switch (literal.getOperator()) {
            case LE:
                final Set<ArithmeticLiteral> lits = new LinkedHashSet<>(simpleLiterals);
                lits.add(literal);
                return Collections.singleton(new IntegerClause(boolLiterals, lits));
            case EQ:
                final Set<ArithmeticLiteral> litsA = new LinkedHashSet<>(simpleLiterals);
                litsA.add(new LinearLiteral(literal.getSum(), LinearLiteral.Operator.LE));
                final IntegerClause c1 = new IntegerClause(boolLiterals, litsA);
                final LinearExpression.Builder ls = new LinearExpression.Builder(literal.getSum());
                ls.multiply(-1);
                final Set<ArithmeticLiteral> litsB = new LinkedHashSet<>(simpleLiterals);
                litsB.add(new LinearLiteral(ls.build(), LinearLiteral.Operator.LE));
                final IntegerClause c2 = new IntegerClause(boolLiterals, litsB);
                return Set.of(c1, c2);
            case NE:
                final LinearExpression.Builder ls1 = new LinearExpression.Builder(literal.getSum());
                ls1.setB(ls1.getB() + 1);
                final LinearExpression.Builder ls2 = new LinearExpression.Builder(literal.getSum());
                ls2.multiply(-1);
                ls2.setB(ls2.getB() + 1);
                final Set<ArithmeticLiteral> litsNe = new LinkedHashSet<>(simpleLiterals);
                litsNe.add(new LinearLiteral(ls1.build(), LinearLiteral.Operator.LE));
                litsNe.add(new LinearLiteral(ls2.build(), LinearLiteral.Operator.LE));
                final IntegerClause newClause = new IntegerClause(Collections.emptySortedSet(), litsNe);
                return simplifyClause(newClause, boolLiterals, context, f);
            default:
                throw new IllegalArgumentException(
                        "Invalid operator of linear expression for order encoding reduction: " + literal.getOperator());

        }
    }

    private static Set<IntegerClause> reduceProductLiteralToLinearLE(final ProductLiteral literal,
                                                                     final Set<ArithmeticLiteral> simpleLiterals,
                                                                     final Set<Literal> boolLiterals,
                                                                     final OrderEncodingContext context,
                                                                     final FormulaFactory f) {
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

            final SortedMap<IntegerVariable, Integer> ls1Coefs = new TreeMap<>();
            ls1Coefs.put(v, -1);
            ls1Coefs.put(lv, a);
            final LinearLiteral ls1 = new LinearLiteral(new LinearExpression(ls1Coefs, 0), LinearLiteral.Operator.LE);
            final IntegerClause.Builder cls1 = new IntegerClause.Builder();
            cls1.addBooleanLiterals(boolLiterals);
            cls1.addArithmeticLiterals(simpleLiterals);
            cls1.addArithmeticLiterals(ls1, xlea, xgea);
            ret.add(cls1.build());

            final SortedMap<IntegerVariable, Integer> ls2Coefs = new TreeMap<>();
            ls2Coefs.put(v, 1);
            ls2Coefs.put(lv, -a);
            final LinearLiteral ls2 = new LinearLiteral(new LinearExpression(ls2Coefs, 0), LinearLiteral.Operator.LE);
            final IntegerClause.Builder cls2 = new IntegerClause.Builder();
            cls2.addBooleanLiterals(boolLiterals);
            cls2.addArithmeticLiterals(simpleLiterals);
            cls2.addArithmeticLiterals(ls2, xlea, xgea);
            ret.add(cls2.build());
        }
        return ret;
    }

    private static Set<IntegerClause> simplifyClause(final IntegerClause clause, final Set<Literal> initBoolLiterals,
                                                     final OrderEncodingContext context,
                                                     final FormulaFactory f) {
        final Set<IntegerClause> newClauses = new LinkedHashSet<>();
        final Set<ArithmeticLiteral> newArithLiterals = new LinkedHashSet<>();
        final Set<Literal> newBoolLiterals = new LinkedHashSet<>(initBoolLiterals);
        for (final ArithmeticLiteral literal : clause.getArithmeticLiterals()) {
            if (OrderEncoding.isSimpleLiteral(literal)) {
                newArithLiterals.add(literal);
            } else {
                final Variable p = context.newSimplifyBooleanVariable(f);
                final Literal notP = p.negate(f);
                final Set<Literal> boolLiterals = new LinkedHashSet<>();
                final Set<ArithmeticLiteral> arithLiterals = new LinkedHashSet<>();
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

    private static LinearExpression.Builder simplifyLinearExpression(final LinearExpression.Builder exp,
                                                                     final boolean first,
                                                                     final Set<IntegerClause> clauses,
                                                                     final List<IntegerVariable> newFrontierAuxVars,
                                                                     final OrderEncodingContext context,
                                                                     final CspFactory cf) {
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
            LinearExpression.Builder simplified =
                    simplifyLinearExpression(eMut, false, clauses, newFrontierAuxVars, context, cf);
            if (simplified.size() > 1) {
                final IntegerVariable v = context.newSimplifyIntVariable(simplified.getDomain(), cf);
                newFrontierAuxVars.add(v);
                simplified.subtract(new LinearExpression(v));
                final IntegerClause aux =
                        new IntegerClause(new LinearLiteral(simplified.build(), LinearLiteral.Operator.EQ));
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

    /**
     * Split a linear expression into multiple linear expressions.
     * @param exp the linear expression
     * @param m   the number of new linear expressions
     * @return an array with the new linear expressions
     */
    static LinearExpression.Builder[] split(final LinearExpression exp, final int m) {
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
