// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import com.booleworks.logicng.datastructures.ubtrees.UbTree;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.transformations.Subsumption;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * This transformation performs subsumption on a given CNF and returns a new
 * CNF. I.e. performs as many subsumptions as possible. A subsumption in a CNF
 * means, that e.g. a clause {@code A | B | C} is subsumed by another clause
 * {@code A | B} and can therefore be deleted for an equivalent CNF.
 * @version 3.0.0
 * @since 1.5.0
 */
public final class CnfSubsumption extends Subsumption {

    public CnfSubsumption(final FormulaFactory f) {
        super(f);
    }

    @Override
    public LngResult<Formula> apply(final Formula formula, final ComputationHandler handler) {
        if (!formula.isCnf(f)) {
            throw new IllegalArgumentException("CNF subsumption can only be applied to formulas in CNF");
        }
        if (formula.getType().getPrecedence() >= FType.LITERAL.getPrecedence() || formula.getType() == FType.OR) {
            return LngResult.of(formula);
        }
        assert formula.getType() == FType.AND;
        final LngResult<UbTree<Literal>> ubTreeResult = generateSubsumedUbTree(formula, handler);
        if (!ubTreeResult.isSuccess()) {
            return LngResult.canceled(ubTreeResult.getCancelCause());
        } else {
            final UbTree<Literal> ubTree = ubTreeResult.getResult();
            final List<Formula> clauses = new ArrayList<>();
            for (final SortedSet<Literal> literals : ubTree.allSets()) {
                clauses.add(f.clause(literals));
            }
            return LngResult.of(f.cnf(clauses));
        }
    }
}
