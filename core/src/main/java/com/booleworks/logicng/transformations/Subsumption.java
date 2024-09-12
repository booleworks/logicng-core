// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import static com.booleworks.logicng.handlers.events.SimpleEvent.SUBSUMPTION_ADDED_NEW_SET;
import static com.booleworks.logicng.handlers.events.SimpleEvent.SUBSUMPTION_STARTING_UB_TREE_GENERATION;

import com.booleworks.logicng.datastructures.ubtrees.UBTree;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.NopHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * A superclass for subsumptions (CNF or DNF).
 * @version 2.0.0
 * @since 1.5.0
 */
public abstract class Subsumption extends StatelessFormulaTransformation {

    /**
     * Constructor.
     * @param f the formula factory to generate new formulas
     */
    public Subsumption(final FormulaFactory f) {
        super(f);
    }

    /**
     * Generates a UBTree from the formulas operands (clauses in CNF, minterms
     * in DNF) where all subsumed operands are already deleted.
     * @param formula the formula (must be an n-ary operator and CNF or DNF)
     * @return the UBTree with the operands and deleted subsumed operands
     */
    protected UBTree<Literal> generateSubsumedUBTree(final Formula formula) {
        return generateSubsumedUBTree(formula, NopHandler.get()).getResult();
    }

    /**
     * Generates a UBTree from the formulas operands (clauses in CNF, minterms
     * in DNF) where all subsumed operands are already deleted.
     * @param formula the formula (must be an n-ary operator and CNF or DNF)
     * @param handler the computation handler
     * @return the UBTree with the operands and deleted subsumed operands
     */
    protected LNGResult<UBTree<Literal>> generateSubsumedUBTree(final Formula formula, final ComputationHandler handler) {
        final SortedMap<Integer, List<SortedSet<Literal>>> mapping = new TreeMap<>();
        for (final Formula term : formula) {
            mapping.computeIfAbsent(term.literals(f).size(), k -> new ArrayList<>()).add(term.literals(f));
        }
        if (!handler.shouldResume(SUBSUMPTION_STARTING_UB_TREE_GENERATION)) {
            return LNGResult.canceled(SUBSUMPTION_STARTING_UB_TREE_GENERATION);
        }
        final UBTree<Literal> ubTree = new UBTree<>();
        for (final Map.Entry<Integer, List<SortedSet<Literal>>> entry : mapping.entrySet()) {
            for (final SortedSet<Literal> set : entry.getValue()) {
                if (ubTree.firstSubset(set) == null) {
                    ubTree.addSet(set);
                    if (!handler.shouldResume(SUBSUMPTION_ADDED_NEW_SET)) {
                        return LNGResult.canceled(SUBSUMPTION_ADDED_NEW_SET);
                    }
                }
            }
        }
        return LNGResult.of(ubTree);
    }
}
