package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.IntegerVariableSubstitution;
import com.booleworks.logicng.csp.predicates.AllDifferentPredicate;
import com.booleworks.logicng.csp.predicates.BinaryPredicate;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.csp.terms.AbsoluteFunction;
import com.booleworks.logicng.csp.terms.AdditionFunction;
import com.booleworks.logicng.csp.terms.BinaryFunction;
import com.booleworks.logicng.csp.terms.DivisionFunction;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.csp.terms.ModuloFunction;
import com.booleworks.logicng.csp.terms.NegationFunction;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Not;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Functions for substituting integer variables in formulas,predicates, and
 * terms.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class TermSubstitutionFunction {
    private TermSubstitutionFunction() {
    }

    /**
     * Substitutes integer variables in the terms of a formula.
     * @param formula           the formula
     * @param substitutionTable the substitutions
     * @param cf                the factory
     * @return formula where the substitutions are applied
     */
    public static Formula substituteFormula(final Formula formula, final IntegerVariableSubstitution substitutionTable,
                                            final CspFactory cf) {
        return substituteFormula(formula, substitutionTable.getMap(), cf);
    }

    /**
     * Substitutes integer variables in the terms of a formula.
     * @param formula           the formula
     * @param substitutionTable the substitutions
     * @param cf                the factory
     * @param <T>               the target type of the substitution
     * @return formula where the substitutions are applied
     */
    public static <T extends Term> Formula substituteFormula(final Formula formula,
                                                             final Map<IntegerVariable, T> substitutionTable,
                                                             final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        switch (formula.getType()) {
            case TRUE:
            case FALSE:
            case LITERAL:
            case PBC:
                return formula;
            case PREDICATE:
                if (formula instanceof CspPredicate) {
                    return substitutePredicate((CspPredicate) formula, substitutionTable, cf);
                } else {
                    return formula;
                }
            case NOT:
                return f.not(substituteFormula(((Not) formula).getOperand(), substitutionTable, cf));
            case EQUIV:
                final BinaryOperator binOp = (BinaryOperator) formula;
                return f.binaryOperator(
                        binOp.getType(),
                        substituteFormula(binOp.getLeft(), substitutionTable, cf),
                        substituteFormula(binOp.getRight(), substitutionTable, cf)
                );
            case OR:
            case AND:
                final List<Formula> operands = new ArrayList<>(formula.numberOfOperands());
                for (final Formula op : formula) {
                    operands.add(substituteFormula(op, substitutionTable, cf));
                }
                return f.naryOperator(formula.getType(), operands);
            default:
                throw new IllegalArgumentException("Unknown formula type: " + formula.getType());
        }
    }

    /**
     * Substitutes integer variables in a predicate.
     * @param predicate         the predicate
     * @param substitutionTable the substitutions
     * @param cf                the factory
     * @return predicate where the substitutions are applied
     */
    public static CspPredicate substitutePredicate(final CspPredicate predicate,
                                                   final IntegerVariableSubstitution substitutionTable,
                                                   final CspFactory cf) {
        return substitutePredicate(predicate, substitutionTable.getMap(), cf);
    }

    /**
     * Substitutes integer variables in a predicate.
     * @param predicate         the predicate
     * @param substitutionTable the substitutions
     * @param cf                the factory
     * @param <T>               the target type of the substitution
     * @return predicate where the substitutions are applied
     */
    public static <T extends Term> CspPredicate substitutePredicate(final CspPredicate predicate,
                                                                    final Map<IntegerVariable, T> substitutionTable,
                                                                    final CspFactory cf) {
        switch (predicate.getPredicateType()) {
            case EQ:
            case NE:
            case LE:
            case LT:
            case GE:
            case GT:
                return cf.comparison(
                        substituteTerm(((BinaryPredicate) predicate).getLeft(), substitutionTable, cf),
                        substituteTerm(((BinaryPredicate) predicate).getRight(), substitutionTable, cf),
                        predicate.getPredicateType()
                );
            case ALLDIFFERENT:
                final AllDifferentPredicate adp = (AllDifferentPredicate) predicate;
                final List<Term> terms = new ArrayList<>(adp.getTerms().size());
                for (final Term t : adp.getTerms()) {
                    terms.add(substituteTerm(t, substitutionTable, cf));
                }
                return cf.allDifferent(terms);
            default:
                throw new IllegalArgumentException("Unknown predicate type: " + predicate.getPredicateType());
        }
    }

    /**
     * Substitutes integer variables in a term.
     * @param term              the term
     * @param substitutionTable the substitutions
     * @param cf                the factory
     * @return term where the substitutions are applied
     */
    public static Term substituteTerm(final Term term, final IntegerVariableSubstitution substitutionTable,
                                      final CspFactory cf) {
        return substituteTerm(term, substitutionTable.getMap(), cf);
    }

    /**
     * Substitutes integer variables in a term.
     * @param term              the term
     * @param substitutionTable the substitutions
     * @param cf                the factory
     * @param <T>               the target type of the substitution
     * @return term where the substitutions are applied
     */
    public static <T extends Term> Term substituteTerm(final Term term,
                                                       final Map<IntegerVariable, T> substitutionTable,
                                                       final CspFactory cf) {
        switch (term.getType()) {
            case ZERO:
            case ONE:
            case CONST:
                return term;
            case VAR:
                if (substitutionTable.containsKey((IntegerVariable) term)) {
                    return substitutionTable.get((IntegerVariable) term);
                } else {
                    return term;
                }
            case NEG:
                return cf.minus(substituteTerm(((NegationFunction) term).getOperand(), substitutionTable, cf));
            case ADD:
                final List<Term> newOps = new ArrayList<>();
                for (final Term op : ((AdditionFunction) term).getOperands()) {
                    newOps.add(substituteTerm(op, substitutionTable, cf));
                }
                return cf.add(newOps);
            case SUB:
                return cf.sub(
                        substituteTerm(((BinaryFunction) term).getLeft(), substitutionTable, cf),
                        substituteTerm(((BinaryFunction) term).getRight(), substitutionTable, cf)
                );
            case MUL:
                return cf.mul(
                        substituteTerm(((BinaryFunction) term).getLeft(), substitutionTable, cf),
                        substituteTerm(((BinaryFunction) term).getRight(), substitutionTable, cf)
                );
            case MOD:
                return cf.mod(
                        substituteTerm(((ModuloFunction) term).getLeft(), substitutionTable, cf),
                        ((ModuloFunction) term).getRight()
                );
            case DIV:
                return cf.div(
                        substituteTerm(((DivisionFunction) term).getLeft(), substitutionTable, cf),
                        ((DivisionFunction) term).getRight()
                );
            case MAX:
                return cf.max(
                        substituteTerm(((BinaryFunction) term).getLeft(), substitutionTable, cf),
                        substituteTerm(((BinaryFunction) term).getRight(), substitutionTable, cf)
                );
            case MIN:
                return cf.min(
                        substituteTerm(((BinaryFunction) term).getLeft(), substitutionTable, cf),
                        substituteTerm(((BinaryFunction) term).getRight(), substitutionTable, cf)
                );
            case ABS:
                return cf.abs(substituteTerm(((AbsoluteFunction) term).getOperand(), substitutionTable, cf));
            default:
                throw new IllegalArgumentException("Unknown term type: " + term.getType());
        }
    }
}
