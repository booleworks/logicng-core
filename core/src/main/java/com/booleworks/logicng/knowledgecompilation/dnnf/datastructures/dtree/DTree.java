// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree;

import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.dnnf.DnnfSatSolver;

import java.util.BitSet;
import java.util.List;
import java.util.SortedSet;

/**
 * Super class for a Decomposition Tree (DTree) for the DNNF Compiler This is
 * either a {@link DTreeNode} or a {@link DTreeLeaf}.
 * @version 3.0.0
 * @since 2.0.0
 */
public abstract class DTree {

    protected int[] staticVariables;
    protected BitSet staticVarSet;
    protected int[] staticSeparator;
    protected int[] staticClauseIds;
    protected DnnfSatSolver solver;

    /**
     * Initializes the DTree.
     * @param solver a specializes DNNF SAT solver
     */
    public abstract void initialize(final DnnfSatSolver solver);

    /**
     * Returns the size of the DTree.
     * @return the size of the DTree
     */
    public abstract int size();

    /**
     * Returns all variables of this DTree.
     * <p>
     * Since this set of variables can be cached, this is a constant time
     * operation.
     * @return all variables of this DTree
     */
    int[] staticVarSetArray() {
        return staticVariables;
    }

    /**
     * Returns all variables of this DTree.
     * <p>
     * Since this set of variables can be cached, this is a constant time
     * operation.
     * @return all variables of this DTree
     */
    public BitSet getStaticVarSet() {
        return staticVarSet;
    }

    /**
     * Returns all variables of this DTree.
     * <p>
     * Since this set of variables can be cached, this is a constant time
     * operation.
     * @param f the formula factory to use for caching
     * @return all variables of this DTree
     */
    abstract SortedSet<Variable> staticVariableSet(final FormulaFactory f);

    /**
     * The dynamic separator of this DTree. "Dynamic" means that subsumed
     * clauses are ignored during the separator computation.
     * @return The dynamic separator of this DTree
     */
    public abstract BitSet dynamicSeparator();

    /**
     * The ids clauses in this DTree.
     * @return The clause ids
     */
    public int[] staticClauseIds() {
        return staticClauseIds;
    }

    /**
     * Counts the number of unsubsumed occurrences for each variable in
     * occurrences.
     * <p>
     * The parameter occurrences should be modified by the method accordingly.
     * @param occurrences The current number of occurrences for each variable
     *                    which should be modified accordingly
     */
    public abstract void countUnsubsumedOccurrences(final int[] occurrences);

    /**
     * Returns the depth of this tree.
     * @return the depth of this tree
     */
    public abstract int depth();

    /**
     * Returns the widest separator of this tree.
     * @return the widest separator
     */
    public abstract int widestSeparator();

    /**
     * Returns all leafs of this tree.
     * @return all leafs of this tree
     */
    abstract List<DTreeLeaf> leafs();
}
