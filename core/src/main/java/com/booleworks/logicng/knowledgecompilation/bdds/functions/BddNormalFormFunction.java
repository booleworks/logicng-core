// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds.functions;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.bdds.Bdd;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * Superclass for the normal form generation from a BDD.
 * @version 3.0.0
 * @since 2.3.0
 */
public abstract class BddNormalFormFunction extends BddFunction<Formula> {

    protected BddNormalFormFunction(final FormulaFactory f) {
        super(f);
    }

    /**
     * Computes a CNF/DNF from the given BDD.
     * @param bdd the BDD
     * @param cnf {@code true} if a CNF should be computed, {@code false} if a
     *            DNF should be computed
     * @return the normal form (CNF or DNF) computed from the BDD
     */
    protected Formula compute(final Bdd bdd, final boolean cnf) {
        final BddKernel kernel = bdd.getUnderlyingKernel();
        final List<byte[]> pathsToConstant =
                cnf ? new BddOperations(kernel).allUnsat(bdd.getIndex()) : new BddOperations(kernel).allSat(bdd.getIndex());
        final List<Formula> terms = new ArrayList<>();
        for (final byte[] path : pathsToConstant) {
            final List<Formula> literals = new ArrayList<>();
            for (int i = 0; i < path.length; i++) {
                final Variable var = kernel.getVariableForIndex(i);
                if (path[i] == 0) {
                    literals.add(cnf ? var : var.negate(f));
                } else if (path[i] == 1) {
                    literals.add(cnf ? var.negate(f) : var);
                }
            }
            final Formula term = cnf ? f.or(literals) : f.and(literals);
            terms.add(term);
        }
        return cnf ? f.and(terms) : f.or(terms);
    }
}
