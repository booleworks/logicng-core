// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations;

import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.cache.TransformationCacheEntry;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.knowledgecompilation.bdds.BDD;
import com.booleworks.logicng.knowledgecompilation.bdds.BDDFactory;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;

import java.util.Map;

/**
 * Transformation of a formula in a normal form (DNF or CNF) by converting it to
 * a BDD.
 * @version 3.0.0
 * @since 2.3.0
 */
public abstract class BDDNormalFormTransformation extends CacheableAndStatefulFormulaTransformation<BDDKernel> {

    private final boolean cnf;

    /**
     * Constructs a new BDD-based normal form transformation with an optional
     * BDD kernel.
     * <p>
     * Warning: You can use this object for arbitrarily many transformations,
     * <b>but</b> the number of different variables in all applied formulas
     * <b>must not exceed</b> the number of variables in the kernel.
     * @param f      the formula factory to generate new formulas
     * @param cnf    true when a CNF transformation, false for a DNF
     *               transformation
     * @param kernel the optional BDD kernel
     */
    public BDDNormalFormTransformation(final FormulaFactory f, final boolean cnf, final BDDKernel kernel) {
        super(f, cnf ? TransformationCacheEntry.BDD_CNF : TransformationCacheEntry.BDD_DNF, kernel);
        this.cnf = cnf;
    }

    /**
     * Constructs a new BDD-based normal form transformation with an optional
     * BDD kernel.
     * <p>
     * Warning: You can use this object for arbitrarily many transformations,
     * <b>but</b> the number of different variables in all applied formulas
     * <b>must not exceed</b> the number of variables in the kernel.
     * @param f      the formula factory to generate new formulas
     * @param cnf    true when a CNF transformation, false for a DNF
     *               transformation
     * @param kernel the optional BDD kernel
     * @param cache  the cache to use for the transformation
     */
    public BDDNormalFormTransformation(final FormulaFactory f, final boolean cnf, final BDDKernel kernel,
                                       final Map<Formula, Formula> cache) {
        super(f, cache, kernel);
        this.cnf = cnf;
    }

    /**
     * Computes the CNF or DNF from the given formula by using a BDD.
     * @param formula the formula to transform
     * @param handler the computation handler
     * @return a (potentially canceled) LNGResult with the normal form (CNF or
     * DNF) of the formula
     */
    protected LNGResult<Formula> compute(final Formula formula, final ComputationHandler handler) {
        if (formula.getType().getPrecedence() >= FType.LITERAL.getPrecedence() || hasNormalForm(formula, cnf)) {
            return LNGResult.of(formula);
        }
        final Formula cached = lookupCache(formula);
        if (cached != null) {
            return LNGResult.of(cached);
        }
        final LNGResult<BDD> bdd = BDDFactory.build(f, formula, state, handler);
        if (!bdd.isSuccess()) {
            return LNGResult.canceled(bdd.getCancelCause());
        }
        final Formula normalForm = cnf ? bdd.getResult().cnf() : bdd.getResult().dnf();
        final LNGResult<Formula> simplifiedNormalForm;
        final UnitPropagation up = new UnitPropagation(f);
        if (cnf) {
            simplifiedNormalForm = normalForm.transform(up, handler);
        } else {
            // unit propagation simplification creates a CNF, so we use the
            // negated DNF to negate the result back to DNF again
            simplifiedNormalForm = normalForm.negate(f).nnf(f)
                    .transform(up, handler)
                    .map(result -> result.negate(f).nnf(f));
        }
        if (simplifiedNormalForm.isSuccess()) {
            setCache(formula, simplifiedNormalForm.getResult());
        }
        return simplifiedNormalForm;
    }

    private boolean hasNormalForm(final Formula formula, final boolean cnf) {
        return cnf ? formula.isCNF(f) : formula.isDNF(f);
    }
}
