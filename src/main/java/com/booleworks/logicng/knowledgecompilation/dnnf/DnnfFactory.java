// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.DnnfCompilationHandler;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.Dnnf;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.MinFillDTreeGenerator;
import com.booleworks.logicng.transformations.cnf.CNFSubsumption;
import com.booleworks.logicng.transformations.simplification.BackboneSimplifier;

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
     * @return the compiled DNNF
     */
    public Dnnf compile(final FormulaFactory f, final Formula formula) {
        return compile(f, formula, null);
    }

    /**
     * Compiles the given formula to a DNNF instance.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula
     * @param handler the DNNF handler
     * @return the compiled DNNF
     */
    public Dnnf compile(final FormulaFactory f, final Formula formula, final DnnfCompilationHandler handler) {
        final SortedSet<Variable> originalVariables = new TreeSet<>(formula.variables(f));
        final Formula cnf = formula.cnf(f);
        originalVariables.addAll(cnf.variables(f));
        final Formula simplifiedFormula = simplifyFormula(f, cnf);
        final DnnfCompiler compiler = new DnnfCompiler(f, simplifiedFormula);
        final Formula dnnf = compiler.compile(new MinFillDTreeGenerator(), handler);
        return dnnf == null ? null : new Dnnf(originalVariables, dnnf);
    }

    protected Formula simplifyFormula(final FormulaFactory f, final Formula formula) {
        return formula
                .transform(new BackboneSimplifier(f))
                .transform(new CNFSubsumption(f));
    }
}
