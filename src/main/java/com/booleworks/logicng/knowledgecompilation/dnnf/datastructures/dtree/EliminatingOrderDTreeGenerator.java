// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A generator for a DTree from an arbitrary eliminating order of variables as described in
 * A. Darwiche "Decomposable Negation Normal Form" (algorithm "el2dt").
 * @version 3.0.0
 * @since 2.0.0
 */
public abstract class EliminatingOrderDTreeGenerator implements DTreeGenerator {

    /**
     * Generates the DTree
     * @param f        the formula factory
     * @param cnf      the CNF input formula
     * @param ordering the variable ordering
     * @return the DTree
     */
    public final DTree generateWithEliminatingOrder(final FormulaFactory f, final Formula cnf, final List<Variable> ordering) {
        assert cnf.variables(f).size() == ordering.size();

        if (!cnf.isCNF(f) || cnf.isAtomicFormula()) {
            throw new IllegalArgumentException("Cannot generate DTree from a non-cnf formula or atomic formula");
        } else if (cnf.type() != FType.AND) {
            return new DTreeLeaf(f, 0, cnf);
        }

        final List<DTree> sigma = new ArrayList<>();
        int id = 0;
        for (final Formula clause : cnf) {
            sigma.add(new DTreeLeaf(f, id++, clause));
        }

        for (final Variable variable : ordering) {
            final List<DTree> gamma = new ArrayList<>();
            final Iterator<DTree> sigmaIterator = sigma.iterator();
            while (sigmaIterator.hasNext()) {
                final DTree tree = sigmaIterator.next();
                if (tree.staticVariableSet(f).contains(variable)) {
                    gamma.add(tree);
                    sigmaIterator.remove();
                }
            }
            if (!gamma.isEmpty()) {
                sigma.add(compose(f, gamma));
            }
        }

        return compose(f, sigma);
    }

    protected DTree compose(final FormulaFactory f, final List<DTree> trees) {
        assert !trees.isEmpty();

        if (trees.size() == 1) {
            return trees.get(0);
        } else {
            final DTree left = compose(f, trees.subList(0, trees.size() / 2));
            final DTree right = compose(f, trees.subList(trees.size() / 2, trees.size()));
            return new DTreeNode(f, left, right);
        }
    }
}
