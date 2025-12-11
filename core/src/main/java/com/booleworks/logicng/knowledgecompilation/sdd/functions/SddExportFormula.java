// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;

import java.util.ArrayList;

/**
 * A function that exports an SDD as a propositional {@link Formula}.
 * <p>
 * Note that, {@code Formula} are not designed to represent SDDs efficiently
 * and the export can really blow up in size.  This function is meant to be
 * used for tests and small demonstrations, but will not scale for real
 * problems.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class SddExportFormula implements SddFunction<Formula> {
    public final Sdd sdd;

    /**
     * Constructs a function that exports an SDD as a propositional
     * {@link Formula}.
     * <p>
     * Note that, {@code Formula} are not designed to represent SDDs efficiently
     * and the export can really blow up in size.  This function is meant to be
     * used for tests and small demonstrations, but will not scale for real
     * problems.
     */
    public SddExportFormula(final Sdd sdd) {
        this.sdd = sdd;
    }

    @Override
    public LngResult<Formula> execute(final SddNode node, final ComputationHandler handler) {
        return LngResult.of(applyRec(node));
    }

    private Formula applyRec(final SddNode node) {
        final FormulaFactory f = sdd.getFactory();
        if (node.isDecomposition()) {
            final SddNodeDecomposition decomp = node.asDecomposition();
            final ArrayList<Formula> elementFormulas = new ArrayList<>(decomp.getElementsUnsafe().size());
            for (final SddElement element : node.asDecomposition()) {
                final Formula sub = applyRec(element.getSub());
                final Formula prime = applyRec(element.getPrime());
                elementFormulas.add(f.and(sub, prime));
            }
            return f.or(elementFormulas);
        } else {
            return node.asTerminal().toFormula(sdd);
        }
    }
}
