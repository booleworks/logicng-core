// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations;

import static org.logicng.formulas.FType.LITERAL;
import static org.logicng.formulas.cache.TransformationCacheEntry.BDD_CNF;
import static org.logicng.formulas.cache.TransformationCacheEntry.BDD_DNF;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.knowledgecompilation.bdds.BDD;
import org.logicng.knowledgecompilation.bdds.BDDFactory;
import org.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;

import java.util.Map;

/**
 * Transformation of a formula in a normal form (DNF or CNF) by converting it to a BDD.
 * @version 3.0.0
 * @since 2.3.0
 */
public abstract class BDDNormalFormTransformation extends CacheableAndStatefulFormulaTransformation<BDDKernel> {

    private final boolean cnf;

    /**
     * Constructs a new BDD-based normal form transformation with an optional BDD kernel.
     * <p>
     * Warning: You can use this object for arbitrarily many transformations, <b>but</b>
     * the number of different variables in all applied formulas <b>must not exceed</b>
     * the number of variables in the kernel.
     * @param cnf    true when a CNF transformation, false for a DNF transformation
     * @param f      the formula factory to generate new formulas
     * @param kernel the optional BDD kernel
     */
    public BDDNormalFormTransformation(final boolean cnf, final FormulaFactory f, final BDDKernel kernel) {
        super(f, cnf ? BDD_CNF : BDD_DNF, kernel);
        this.cnf = cnf;
    }

    /**
     * Constructs a new BDD-based normal form transformation with an optional BDD kernel.
     * <p>
     * Warning: You can use this object for arbitrarily many transformations, <b>but</b>
     * the number of different variables in all applied formulas <b>must not exceed</b>
     * the number of variables in the kernel.
     * @param cnf    true when a CNF transformation, false for a DNF transformation
     * @param f      the formula factory to generate new formulas
     * @param kernel the optional BDD kernel
     * @param cache  the cache to use for the transformation
     */
    public BDDNormalFormTransformation(final boolean cnf, final FormulaFactory f, final BDDKernel kernel, final Map<Formula, Formula> cache) {
        super(f, cnf ? BDD_CNF : BDD_DNF, cache, kernel);
        this.cnf = cnf;
    }

    /**
     * Computes the CNF or DNF from the given formula by using a BDD.
     * @param formula the formula to transform
     * @return the normal form (CNF or DNF) of the formula
     */
    protected Formula compute(final Formula formula) {
        if (formula.type().precedence() >= LITERAL.precedence()) {
            return formula;
        }
        if (hasNormalForm(formula, cnf)) {
            return formula;
        }
        final Formula cached = lookupCache(formula);
        if (cached != null) {
            return cached;
        }
        final BDD bdd = BDDFactory.build(f, formula, state, null);
        final Formula normalForm = cnf ? bdd.cnf() : bdd.dnf();
        final Formula simplifiedNormalForm;
        final UnitPropagation up = new UnitPropagation(f);
        if (cnf) {
            simplifiedNormalForm = normalForm.transform(up);
        } else {
            // unit propagation simplification creates a CNF, so we use the negated DNF to negate the result back to DNF again
            simplifiedNormalForm = normalForm.negate(f).nnf(f).transform(up).negate(f).nnf(f);
        }
        setCache(formula, simplifiedNormalForm);
        return simplifiedNormalForm;
    }

    private boolean hasNormalForm(final Formula formula, final boolean cnf) {
        return cnf ? formula.isCNF() : formula.isDNF();
    }
}
