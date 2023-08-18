// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.knowledgecompilation.dnnf;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.knowledgecompilation.dnnf.datastructures.Dnnf;
import org.logicng.knowledgecompilation.dnnf.datastructures.dtree.MinFillDTreeGenerator;
import org.logicng.transformations.cnf.CNFSubsumption;
import org.logicng.transformations.simplification.BackboneSimplifier;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A DNNF factory that can be used to compute DNNFs from formulas.
 * @version 3.0.0
 * @since 2.0.0
 */
public class DnnfFactory {

    /**
     * Compiles the given formula to a DNNF instance.
     * @param formula the formula
     * @param f       the formula factory to generate new formulas
     * @return the compiled DNNF
     */
    public Dnnf compile(final Formula formula, final FormulaFactory f) {
        final SortedSet<Variable> originalVariables = new TreeSet<>(formula.variables());
        final Formula cnf = formula.cnf(f);
        originalVariables.addAll(cnf.variables());
        final Formula simplifedFormula = simplifyFormula(cnf, f);
        final DnnfCompiler compiler = new DnnfCompiler(simplifedFormula, f);
        final Formula dnnf = compiler.compile(new MinFillDTreeGenerator());
        return new Dnnf(originalVariables, dnnf);
    }

    protected Formula simplifyFormula(final Formula formula, final FormulaFactory f) {
        return formula
                .transform(new BackboneSimplifier(f))
                .transform(new CNFSubsumption(f));
    }
}
