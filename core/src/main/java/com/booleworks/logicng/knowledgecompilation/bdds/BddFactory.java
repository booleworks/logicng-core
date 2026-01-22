// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.BDD_COMPUTATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.BDD_MAKE_NEW_NODE;
import static com.booleworks.logicng.handlers.events.SimpleEvent.BDD_NEW_REF_ADDED;

import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.BinaryOperator;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Implication;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Not;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddConstruction;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;

import java.util.Collection;
import java.util.Iterator;

/**
 * The factory for the jBuddy implementation.
 * @version 3.0.0
 * @since 1.4.0
 */
public final class BddFactory {

    private BddFactory() {
        // not to be instantiated
    }

    /**
     * Builds a BDD for a given formula. BDDs support all Boolean formula types
     * but not pseudo-Boolean constraints. The reason is that before converting
     * a formula to a BDD one must specify the number of variables. In case of
     * pseudo-Boolean constraints this number depends on the translation of the
     * constraint. Therefore, the caller first has to transform any
     * pseudo-Boolean constraints in their respective CNF representation before
     * converting them to a BDD.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula
     * @return the top node of the BDD
     */
    public static Bdd build(final FormulaFactory f, final Formula formula) {
        return build(f, formula, null, NopHandler.get()).getResult();
    }

    /**
     * Builds a BDD for a given formula. BDDs support all Boolean formula types
     * but not pseudo-Boolean constraints. The reason is that before converting
     * a formula to a BDD one must specify the number of variables. In case of
     * pseudo-Boolean constraints this number depends on the translation of the
     * constraint. Therefore, the caller first has to transform any
     * pseudo-Boolean constraints in their respective CNF representation before
     * converting them to a BDD.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula
     * @param kernel  the BBD kernel to use
     * @return the top node of the BDD
     */
    public static Bdd build(final FormulaFactory f, final Formula formula, final BddKernel kernel) {
        return build(f, formula, kernel, NopHandler.get()).getResult();
    }

    /**
     * Builds a BDD for a given formula. BDDs support all Boolean formula types
     * but not pseudo-Boolean constraints. The reason is that before converting
     * a formula to a BDD one must specify the number of variables. In case of
     * pseudo-Boolean constraints this number depends on the translation of the
     * constraint. Therefore, the caller first has to transform any
     * pseudo-Boolean constraints in their respective CNF representation before
     * converting them to a BDD.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula
     * @param kernel  the BBD kernel to use
     * @param handler the handler
     * @return the top node of the BDD or the canceling reason if the
     * computation was canceled
     */
    public static LngResult<Bdd> build(final FormulaFactory f, final Formula formula, final BddKernel kernel,
                                       final ComputationHandler handler) {
        if (!handler.shouldResume(BDD_COMPUTATION_STARTED)) {
            return LngResult.canceled(BDD_COMPUTATION_STARTED);
        }
        final int varNum = formula.variables(f).size();
        final BddKernel bddKernel = kernel == null ? new BddKernel(f, varNum, varNum * 30, varNum * 20) : kernel;
        final int bddIndex = buildRec(f, formula, bddKernel, new BddConstruction(bddKernel), handler);
        if (bddIndex == BddKernel.BDD_ABORT_NEW_NODE) {
            return LngResult.canceled(BDD_MAKE_NEW_NODE);
        } else if (bddIndex == BddKernel.BDD_ABORT_NEW_REF) {
            return LngResult.canceled(BDD_NEW_REF_ADDED);
        } else {
            return LngResult.of(new Bdd(bddIndex, bddKernel));
        }
    }

    /**
     * Builds an BDD for a set of literals. The set is interpreted as the
     * conjunction of the literals.
     * @param literals a set of literals
     * @param kernel   the bdd kernel
     * @return the top node of the BDD
     */
    public static Bdd build(final Collection<? extends Literal> literals, final BddKernel kernel) {
        final var construction = new BddConstruction(kernel);
        int bdd;
        if (literals.isEmpty()) {
            bdd = BddKernel.BDD_FALSE;
        } else if (literals.size() == 1) {
            final Literal lit = literals.iterator().next();
            final int idx = kernel.getOrAddVarIndex(lit.variable());
            bdd = lit.getPhase() ? construction.ithVar(idx) : construction.nithVar(idx);
        } else {
            final Iterator<? extends Literal> it = literals.iterator();
            Literal lit = it.next();
            int idx = kernel.getOrAddVarIndex(lit.variable());
            bdd = lit.getPhase() ? construction.ithVar(idx) : construction.nithVar(idx);
            while (it.hasNext()) {
                lit = it.next();
                idx = kernel.getOrAddVarIndex(lit.variable());
                final int operand = lit.getPhase() ? construction.ithVar(idx) : construction.nithVar(idx);
                final int previous = bdd;
                bdd = kernel.addRef(construction.and(bdd, operand, NopHandler.get()), NopHandler.get());
                kernel.delRef(previous);
                kernel.delRef(operand);
            }
        }
        return new Bdd(bdd, kernel);
    }

    /**
     * Recursive build procedure for the BDD.
     * @param formula      the formula
     * @param kernel       the BDD kernel
     * @param construction the BDD construction instance
     * @param handler      the handler
     * @return the BDD index or canceling cause if the computation was canceled
     */
    private static int buildRec(final FormulaFactory f, final Formula formula, final BddKernel kernel,
                                final BddConstruction construction, final ComputationHandler handler) {
        switch (formula.getType()) {
            case FALSE:
                return BddKernel.BDD_FALSE;
            case TRUE:
                return BddKernel.BDD_TRUE;
            case LITERAL:
                final Literal lit = (Literal) formula;
                final int idx = kernel.getOrAddVarIndex(lit.variable());
                return lit.getPhase() ? construction.ithVar(idx) : construction.nithVar(idx);
            case NOT: {
                final Not not = (Not) formula;
                final int operand = buildRec(f, not.getOperand(), kernel, construction, handler);
                if (BddKernel.isAborted(operand)) {
                    return operand;
                }
                final int res = kernel.addRef(construction.not(operand), handler);
                kernel.delRef(operand);
                return res;
            }
            case IMPL:
            case EQUIV: {
                final BinaryOperator binary = (BinaryOperator) formula;
                final int left = buildRec(f, binary.getLeft(), kernel, construction, handler);
                if (BddKernel.isAborted(left)) {
                    return left;
                }
                final int right = buildRec(f, binary.getRight(), kernel, construction, handler);
                if (BddKernel.isAborted(right)) {
                    return right;
                }
                final int constructedNode =
                        binary instanceof Implication ? construction.implication(left, right, handler)
                                                      : construction.equivalence(left, right, handler);
                if (BddKernel.isAborted(constructedNode)) {
                    return constructedNode;
                }
                final int res = kernel.addRef(constructedNode, handler);
                if (BddKernel.isAborted(res)) {
                    return res;
                }
                kernel.delRef(left);
                kernel.delRef(right);
                return res;
            }
            case AND:
            case OR: {
                final Iterator<Formula> it = formula.iterator();
                int res = buildRec(f, it.next(), kernel, construction, handler);
                if (BddKernel.isAborted(res)) {
                    return res;
                }
                while (it.hasNext()) {
                    final int operand = buildRec(f, it.next(), kernel, construction, handler);
                    if (BddKernel.isAborted(operand)) {
                        return operand;
                    }
                    final int previous = res;
                    final int constructedNode = formula instanceof And ? construction.and(res, operand, handler)
                                                                       : construction.or(res, operand, handler);
                    if (BddKernel.isAborted(constructedNode)) {
                        return constructedNode;
                    }
                    res = kernel.addRef(constructedNode, handler);
                    if (BddKernel.isAborted(res)) {
                        return res;
                    }
                    kernel.delRef(previous);
                    kernel.delRef(operand);
                }
                return res;
            }
            case PBC:
                return buildRec(f, formula.nnf(f), kernel, construction, handler);
            case PREDICATE:
                throw new IllegalArgumentException("Cannot generate a BDD from a formula with predicates in it");
            default:
                throw new IllegalArgumentException("Unsupported operator for BDD generation: " + formula.getType());
        }
    }
}
