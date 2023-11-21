// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.dnf;

import com.booleworks.logicng.datastructures.ubtrees.UBTree;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.transformations.Subsumption;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * This transformation performs subsumption on a given DNF and returns a new DNF.
 * I.e. performs as many subsumptions as possible.  A subsumption in a DNF means,
 * that e.g. a minterm {@code A & B & C} is subsumed by another minterm {@code A & B}
 * and can therefore be deleted for an equivalent DNF.
 * @version 3.0.0
 * @since 1.5.0
 */
public final class DNFSubsumption extends Subsumption {

    public DNFSubsumption(final FormulaFactory f) {
        super(f);
    }

    @Override
    public Formula apply(final Formula formula) {
        if (!formula.isDNF(f)) {
            throw new IllegalArgumentException("DNF subsumption can only be applied to formulas in DNF");
        }
        if (formula.type().precedence() >= FType.LITERAL.precedence() || formula.type() == FType.AND) {
            return formula;
        }
        assert formula.type() == FType.OR;
        final UBTree<Literal> ubTree = generateSubsumedUBTree(formula);
        final List<Formula> minterms = new ArrayList<>();
        for (final SortedSet<Literal> literals : ubTree.allSets()) {
            minterms.add(f.and(literals));
        }
        return f.or(minterms);
    }
}
