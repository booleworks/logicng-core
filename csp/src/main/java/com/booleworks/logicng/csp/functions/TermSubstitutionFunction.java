// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

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
 * Functions for substituting integer variables in formulas, predicates, and
 * terms.
 * @version 3.0.0
 * @since 3.0.0
 */
public class TermSubstitutionFunction {
    protected TermSubstitutionFunction() {
    }

    /**
     * Substitutes integer variables in the terms of a formula.
     * @param cf                the factory
     * @param formula           the formula
     * @param substitutionTable the substitutions
     * @return formula where the substitutions are applied
     */
    public static Formula substituteFormula(final CspFactory cf, final Formula formula,
                                            final IntegerVariableSubstitution substitutionTable) {
        return substituteFormula(cf, formula, substitutionTable.getMap());
    }

    /**
     * Substitutes integer variables in the terms of a formula.
     * @param <T>               the target type of the substitution
     * @param cf                the factory
     * @param formula           the formula
     * @param substitutionTable the substitutions
     * @return formula where the substitutions are applied
     */
    public static <T extends Term> Formula substituteFormula(final CspFactory cf, final Formula formula,
                                                             final Map<IntegerVariable, T> substitutionTable) {
        final FormulaFactory f = cf.getFormulaFactory();
        switch (formula.getType()) {
            case TRUE:
            case FALSE:
            case LITERAL:
            case PBC:
                return formula;
            case PREDICATE:
                if (formula instanceof CspPredicate) {
                    return substitutePredicate(cf, (CspPredicate) formula, substitutionTable);
                } else {
                    return formula;
                }
            case NOT:
                return f.not(substituteFormula(cf, ((Not) formula).getOperand(), substitutionTable));
            case EQUIV:
                final BinaryOperator binOp = (BinaryOperator) formula;
                return f.binaryOperator(
                        binOp.getType(),
                        substituteFormula(cf, binOp.getLeft(), substitutionTable),
                        substituteFormula(cf, binOp.getRight(), substitutionTable)
                );
            case OR:
            case AND:
                final List<Formula> operands = new ArrayList<>(formula.numberOfOperands());
                for (final Formula op : formula) {
                    operands.add(substituteFormula(cf, op, substitutionTable));
                }
                return f.naryOperator(formula.getType(), operands);
            default:
                throw new IllegalArgumentException("Unknown formula type: " + formula.getType());
        }
    }

    /**
     * Substitutes integer variables in a predicate.
     * @param cf                the factory
     * @param predicate         the predicate
     * @param substitutionTable the substitutions
     * @return predicate where the substitutions are applied
     */
    public static CspPredicate substitutePredicate(final CspFactory cf, final CspPredicate predicate,
                                                   final IntegerVariableSubstitution substitutionTable) {
        return substitutePredicate(cf, predicate, substitutionTable.getMap());
    }

    /**
     * Substitutes integer variables in a predicate.
     * @param <T>               the target type of the substitution
     * @param cf                the factory
     * @param predicate         the predicate
     * @param substitutionTable the substitutions
     * @return predicate where the substitutions are applied
     */
    public static <T extends Term> CspPredicate substitutePredicate(final CspFactory cf, final CspPredicate predicate,
                                                                    final Map<IntegerVariable, T> substitutionTable) {
        switch (predicate.getPredicateType()) {
            case EQ:
            case NE:
            case LE:
            case LT:
            case GE:
            case GT:
                return cf.comparison(
                        substituteTerm(cf, ((BinaryPredicate) predicate).getLeft(), substitutionTable),
                        substituteTerm(cf, ((BinaryPredicate) predicate).getRight(), substitutionTable),
                        predicate.getPredicateType()
                );
            case ALLDIFFERENT:
                final AllDifferentPredicate adp = (AllDifferentPredicate) predicate;
                final List<Term> terms = new ArrayList<>(adp.getTerms().size());
                for (final Term t : adp.getTerms()) {
                    terms.add(substituteTerm(cf, t, substitutionTable));
                }
                return cf.allDifferent(terms);
            default:
                throw new IllegalArgumentException("Unknown predicate type: " + predicate.getPredicateType());
        }
    }

    /**
     * Substitutes integer variables in a term.
     * @param cf                the factory
     * @param term              the term
     * @param substitutionTable the substitutions
     * @return term where the substitutions are applied
     */
    public static Term substituteTerm(final CspFactory cf, final Term term,
                                      final IntegerVariableSubstitution substitutionTable) {
        return substituteTerm(cf, term, substitutionTable.getMap());
    }

    /**
     * Substitutes integer variables in a term.
     * @param <T>               the target type of the substitution
     * @param cf                the factory
     * @param term              the term
     * @param substitutionTable the substitutions
     * @return term where the substitutions are applied
     */
    public static <T extends Term> Term substituteTerm(final CspFactory cf, final Term term,
                                                       final Map<IntegerVariable, T> substitutionTable) {
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
                return cf.minus(substituteTerm(cf, ((NegationFunction) term).getOperand(), substitutionTable));
            case ADD:
                final List<Term> newOps = new ArrayList<>();
                for (final Term op : ((AdditionFunction) term).getOperands()) {
                    newOps.add(substituteTerm(cf, op, substitutionTable));
                }
                return cf.add(newOps);
            case SUB:
                return cf.sub(
                        substituteTerm(cf, ((BinaryFunction) term).getLeft(), substitutionTable),
                        substituteTerm(cf, ((BinaryFunction) term).getRight(), substitutionTable)
                );
            case MUL:
                return cf.mul(
                        substituteTerm(cf, ((BinaryFunction) term).getLeft(), substitutionTable),
                        substituteTerm(cf, ((BinaryFunction) term).getRight(), substitutionTable)
                );
            case MOD:
                return cf.mod(
                        substituteTerm(cf, ((ModuloFunction) term).getLeft(), substitutionTable),
                        ((ModuloFunction) term).getRight()
                );
            case DIV:
                return cf.div(
                        substituteTerm(cf, ((DivisionFunction) term).getLeft(), substitutionTable),
                        ((DivisionFunction) term).getRight()
                );
            case MAX:
                return cf.max(
                        substituteTerm(cf, ((BinaryFunction) term).getLeft(), substitutionTable),
                        substituteTerm(cf, ((BinaryFunction) term).getRight(), substitutionTable)
                );
            case MIN:
                return cf.min(
                        substituteTerm(cf, ((BinaryFunction) term).getLeft(), substitutionTable),
                        substituteTerm(cf, ((BinaryFunction) term).getRight(), substitutionTable)
                );
            case ABS:
                return cf.abs(substituteTerm(cf, ((AbsoluteFunction) term).getOperand(), substitutionTable));
            default:
                throw new IllegalArgumentException("Unknown term type: " + term.getType());
        }
    }
}
