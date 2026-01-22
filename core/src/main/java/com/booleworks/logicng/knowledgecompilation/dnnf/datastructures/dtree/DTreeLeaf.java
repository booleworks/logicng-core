// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree;

import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.dnnf.DnnfSatSolver;
import com.booleworks.logicng.solvers.sat.LngCoreSolver;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.SortedSet;

/**
 * A leaf in a DTree.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class DTreeLeaf extends DTree {

    private final int id;
    private final Formula clause;
    private final int clauseSize;
    private int[] literals;
    private final BitSet separatorBitSet = new BitSet();

    /**
     * Constructs a new leaf with the given id and clause.
     * @param f      the formula factory for caching
     * @param id     the id
     * @param clause the clause
     */
    public DTreeLeaf(final FormulaFactory f, final int id, final Formula clause) {
        this.id = id;
        this.clause = clause;
        staticClauseIds = new int[]{id};
        clauseSize = clause.variables(f).size();
        staticSeparator = new int[0];
        assert clauseSize >= 2;
    }

    @Override
    public void initialize(final DnnfSatSolver solver) {
        this.solver = solver;
        final SortedSet<Literal> lits = clause.literals(solver.getFactory());
        final int size = lits.size();
        staticVarSet = new BitSet();
        staticVariables = new int[size];
        literals = new int[size];
        int i = 0;
        for (final Literal literal : lits) {
            final int var = solver.variableIndex(literal);
            staticVarSet.set(var);
            staticVariables[i] = var;
            literals[i] = LngCoreSolver.mkLit(var, !literal.getPhase());
            i++;
        }
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public SortedSet<Variable> staticVariableSet(final FormulaFactory f) {
        return clause.variables(f);
    }

    @Override
    public BitSet dynamicSeparator() {
        return separatorBitSet;
    }

    private boolean isSubsumed() {
        for (final int literal : literals) {
            if (solver.valueOf(literal) == Tristate.TRUE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void countUnsubsumedOccurrences(final int[] occurrences) {
        if (!isSubsumed()) {
            for (final int var : staticVariables) {
                final int occ = occurrences[var];
                if (occ != -1) {
                    ++occurrences[var];
                }
            }
        }
    }

    @Override
    public int depth() {
        return 1;
    }

    @Override
    public int widestSeparator() {
        return 0;
    }

    @Override
    public List<DTreeLeaf> leafs() {
        final List<DTreeLeaf> result = new ArrayList<>();
        result.add(this);
        return result;
    }

    /**
     * Returns the leaf's clause.
     * @return the leaf's clause
     */
    public Formula clause() {
        return clause;
    }

    @Override
    public String toString() {
        return String.format("DTreeLeaf: %d, %s", id, clause);
    }

    /**
     * Returns the literal integers of the clause.
     * @return literals integers of the clause
     */
    public int[] literals() {
        return literals;
    }

    /**
     * Returns the size of the leaf's clause.
     * @return the size of the leaf's clause
     */
    public int clauseSize() {
        return clauseSize;
    }

    /**
     * Returns the leaf's id.
     * @return the leaf's id
     */
    public int getId() {
        return id;
    }
}
