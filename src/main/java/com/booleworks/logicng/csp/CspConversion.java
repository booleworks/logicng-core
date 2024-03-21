package com.booleworks.logicng.csp;

import com.booleworks.logicng.csp.literals.ArithmeticLiteral;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.predicates.ComparisonPredicate;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Or;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CspConversion {
    public static Csp.Builder convert(final List<Formula> cspFormulas, final CspFactory f) {
        final Csp.Builder csp = new Csp.Builder(f);
        for (final Formula cspFormula : cspFormulas) {
            convertCspFormula(cspFormula, csp);
        }
        return csp;
    }

    static void convertCspFormula(final Formula cspFormula, final Csp.Builder csp) {
        switch (cspFormula.type()) {
            case OR:
                convertClause((Or) cspFormula, csp);
                break;
            case AND:
                convertConjunction((And) cspFormula, csp);
                break;
            case LITERAL:
                convertLiteralAsClause((Literal) cspFormula, csp);
                break;
            case PREDICATE:
                if (cspFormula instanceof ComparisonPredicate) {
                    convertComparisonAsClause((ComparisonPredicate) cspFormula, csp);
                } else {
                    throw new IllegalArgumentException("Invalid predicate for CSP conversion: " + cspFormula.getClass());
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid formula type for CSP conversion: " + cspFormula.type());
        }
    }

    static void convertConjunction(final And conjunction, final Csp.Builder csp) {
        for (final Formula clause : conjunction.operands()) {
            switch (clause.type()) {
                case OR:
                    convertClause((Or) clause, csp);
                    break;
                case LITERAL:
                    convertLiteralAsClause((Literal) clause, csp);
                    break;
                case PREDICATE:
                    if (clause instanceof ComparisonPredicate) {
                        convertComparisonAsClause((ComparisonPredicate) clause, csp);
                    } else {
                        throw new IllegalArgumentException("Invalid predicate for CSP conversion: " + clause.getClass());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid formula type for CSP conversion:" + clause.type());
            }
        }
    }

    static void convertLiteralAsClause(final Literal literal, final Csp.Builder csp) {
        csp.addBooleanVariable(literal.variable());
        csp.addClause(new IntegerClause(Collections.singletonList(literal), Collections.emptyList()));
    }

    static void convertComparisonAsClause(final ComparisonPredicate predicate, final Csp.Builder csp) {
        final LinearLiteral lit = convertComparison(predicate, csp);
        csp.addClause(new IntegerClause(Collections.emptyList(), Collections.singletonList(lit)));
    }

    static void convertClause(final Or clause, final Csp.Builder csp) {
        final List<Literal> boolVariables = new ArrayList<>();
        final List<ArithmeticLiteral> arithVariables = new ArrayList<>();
        for (final Formula literal : clause.operands()) {
            switch (literal.type()) {
                case LITERAL:
                    csp.addBooleanVariable(((Literal) literal).variable());
                    boolVariables.add((Literal) literal);
                    break;
                case PREDICATE:
                    if (literal instanceof ComparisonPredicate) {
                        arithVariables.add(convertComparison((ComparisonPredicate) literal, csp));
                    } else {
                        throw new IllegalArgumentException("Invalid predicate for CSP conversion: " + literal.getClass());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid literal type for CSP conversion" + literal.type());
            }
        }
        csp.addClause(new IntegerClause(boolVariables, arithVariables));
    }

    static LinearLiteral convertComparison(final ComparisonPredicate predicate, final Csp.Builder csp) {
        final LinearLiteral.Operator op;
        switch (predicate.getType()) {
            case EQ:
                op = LinearLiteral.Operator.EQ;
                break;
            case NE:
                op = LinearLiteral.Operator.NE;
                break;
            case LE:
                op = LinearLiteral.Operator.LE;
                break;
            case GE:
                op = LinearLiteral.Operator.GE;
                break;
            default:
                throw new UnsupportedOperationException("Invalid comparison type for csp conversion: " + predicate.getType());
        }
        assert predicate.getRight().getType() == Term.Type.ZERO;
        final LinearExpression sum = predicate.getLeft().decompose().getLinearExpression();
        for (final IntegerVariable variable : sum.getVariables()) {
            csp.addIntegerVariable(variable);
        }
        return new LinearLiteral(sum, op);
    }
}
