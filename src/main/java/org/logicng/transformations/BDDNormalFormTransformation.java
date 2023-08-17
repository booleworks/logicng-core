// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations;

import static org.logicng.formulas.FType.LITERAL;
import static org.logicng.formulas.cache.TransformationCacheEntry.BDD_CNF;
import static org.logicng.formulas.cache.TransformationCacheEntry.BDD_DNF;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.implementation.cached.CachingFormulaFactory;
import org.logicng.knowledgecompilation.bdds.BDD;
import org.logicng.knowledgecompilation.bdds.BDDFactory;
import org.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;

/**
 * Transformation of a formula in a normal form (DNF or CNF) by converting it to a BDD.
 * @version 3.0.0
 * @since 2.3.0
 */
public abstract class BDDNormalFormTransformation extends StatefulFormulaTransformation<BDDKernel> {

    private final boolean useCache;
    private final UnitPropagation up;

    /**
     * Constructs a new BDD-based normal form transformation for a given number of variables.
     * <p>
     * Warning: You can use this object for arbitrarily many transformations, <b>but</b>
     * the number of different variables in all applied formulas <b>must not exceed</b>
     * {@code numVars}.
     * <p>
     * To improve performance you might want to use {@link #BDDNormalFormTransformation(FormulaFactory, BDDKernel)},
     * where you have full control over the node and cache size in the used BDD kernel.
     * @param f       the formula factory to use
     * @param numVars the number of variables
     */
    public BDDNormalFormTransformation(final FormulaFactory f, final int numVars) {
        this(f, new BDDKernel(f, numVars, Math.max(numVars * 30, 20_000), Math.max(numVars * 20, 20_000)));
    }

    /**
     * Constructs a new BDD-based normal form transformation with an optional BDD kernel.
     * <p>
     * Warning: You can use this object for arbitrarily many transformations, <b>but</b>
     * the number of different variables in all applied formulas <b>must not exceed</b>
     * the number of variables in the kernel.
     * @param f      the formula factory to generate new formulas
     * @param kernel the optional BDD kernel
     */
    public BDDNormalFormTransformation(final FormulaFactory f, final BDDKernel kernel) {
        this(f, kernel, true);
    }

    /**
     * Constructs a new BDD-based normal form transformation with an optional BDD kernel.
     * <p>
     * Warning: You can use this object for arbitrarily many transformations, <b>but</b>
     * the number of different variables in all applied formulas <b>must not exceed</b>
     * the number of variables in the kernel.
     * @param f        the formula factory to generate new formulas
     * @param kernel   the optional BDD kernel
     * @param useCache a flag whether the result per formula should be cached
     *                 (only relevant for caching formula factory)
     */
    public BDDNormalFormTransformation(final FormulaFactory f, final BDDKernel kernel, final boolean useCache) {
        super(f, kernel);
        this.up = new UnitPropagation(f);
        this.useCache = useCache;
    }

    /**
     * Computes the CNF or DNF from the given formula by using a BDD.
     * @param formula the formula to transform
     * @param cnf     {@code true} if a CNF should be computed, {@code false} if a canonical DNF should be computed
     * @return the normal form (CNF or DNF) of the formula
     */
    protected Formula compute(final Formula formula, final boolean cnf) {
        if (formula.type().precedence() >= LITERAL.precedence()) {
            return formula;
        }
        if (hasNormalForm(formula, cnf)) {
            return formula;
        }
        final Formula cached = formula.transformationCacheEntry(cnf ? BDD_CNF : BDD_DNF);
        if (useCache && cached != null) {
            return cached;
        }
        final BDD bdd = BDDFactory.build(formula, state, null);
        final Formula normalForm = cnf ? bdd.cnf() : bdd.dnf();
        final Formula simplifiedNormalForm;
        if (cnf) {
            simplifiedNormalForm = normalForm.transform(up);
        } else {
            // unit propagation simplification creates a CNF, so we use the negated DNF to negate the result back to DNF again
            simplifiedNormalForm = normalForm.negate(f).nnf(f).transform(up).negate(f).nnf(f);
        }
        if (useCache) {
            formula.setTransformationCacheEntry(cnf ? BDD_CNF : BDD_DNF, simplifiedNormalForm);
        }
        return simplifiedNormalForm;
    }

    private boolean hasNormalForm(final Formula formula, final boolean cnf) {
        return cnf ? formula.isCNF() : formula.isDNF();
    }

    @Override
    protected BDDKernel initStateForCachingFactory(final CachingFormulaFactory f) {
        return null; // not used
    }
}
