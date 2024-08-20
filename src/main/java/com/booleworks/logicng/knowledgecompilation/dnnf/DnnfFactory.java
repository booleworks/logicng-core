// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LNGResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.Dnnf;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTree;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.MinFillDTreeGenerator;
import com.booleworks.logicng.predicates.satisfiability.SATPredicate;
import com.booleworks.logicng.transformations.cnf.CNFSubsumption;
import com.booleworks.logicng.transformations.simplification.BackboneSimplifier;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.List;
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
    public static Dnnf compile(final FormulaFactory f, final Formula formula) {
        return compile(f, formula, NopHandler.get()).getResult();
    }

    /**
     * Compiles the given formula to a DNNF instance.
     * @param f       the formula factory to generate new formulas
     * @param formula the formula
     * @param handler the computation handler
     * @return the compiled DNNF
     */
    public static LNGResult<Dnnf> compile(final FormulaFactory f, final Formula formula, final ComputationHandler handler) {
        final SortedSet<Variable> originalVariables = new TreeSet<>(formula.variables(f));
        final Formula cnf = formula.cnf(f);
        originalVariables.addAll(cnf.variables(f));
        final LNGResult<Formula> simplifiedFormula = simplifyFormula(f, cnf, handler);
        if (!simplifiedFormula.isSuccess()) {
            return LNGResult.canceled(simplifiedFormula.getCancelCause());
        }
        return runCompiler(simplifiedFormula.getResult(), f, handler)
                .map(dnnf -> new Dnnf(originalVariables, dnnf));
    }

    protected static LNGResult<Formula> simplifyFormula(final FormulaFactory f, final Formula formula, final ComputationHandler handler) {
        final LNGResult<Formula> backboneSimplified = formula.transform(new BackboneSimplifier(f), handler);
        if (!backboneSimplified.isSuccess()) {
            return LNGResult.canceled(backboneSimplified.getCancelCause());
        }
        return backboneSimplified.getResult().transform(new CNFSubsumption(f), handler);
    }

    private static LNGResult<Formula> runCompiler(final Formula cnf, final FormulaFactory f, final ComputationHandler handler) {
        final Pair<Formula, Formula> unitAndNonUnitClauses = splitCnfClauses(cnf, f);
        final Formula unitClauses = unitAndNonUnitClauses.first();
        final Formula nonUnitClauses = unitAndNonUnitClauses.second();
        if (nonUnitClauses.isAtomicFormula()) {
            return LNGResult.of(cnf);
        }
        if (!cnf.holds(new SATPredicate(f))) {
            return LNGResult.of(f.falsum());
        }
        final LNGResult<DTree> dTreeResult = generateDTree(nonUnitClauses, f, handler);
        if (!dTreeResult.isSuccess()) {
            return LNGResult.canceled(dTreeResult.getCancelCause());
        } else {
            return new DnnfCompiler(f, cnf, dTreeResult.getResult(), unitClauses, nonUnitClauses).start(dTreeResult.getResult(), handler);
        }
    }

    protected static Pair<Formula, Formula> splitCnfClauses(final Formula originalCnf, final FormulaFactory f) {
        final List<Formula> units = new ArrayList<>();
        final List<Formula> nonUnits = new ArrayList<>();
        switch (originalCnf.type()) {
            case AND:
                for (final Formula clause : originalCnf) {
                    if (clause.isAtomicFormula()) {
                        units.add(clause);
                    } else {
                        nonUnits.add(clause);
                    }
                }
                break;
            case OR:
                nonUnits.add(originalCnf);
                break;
            default:
                units.add(originalCnf);
        }
        return new Pair<>(f.and(units), f.and(nonUnits));
    }

    protected static LNGResult<DTree> generateDTree(final Formula nonUnitClauses, final FormulaFactory f, final ComputationHandler handler) {
        return new MinFillDTreeGenerator().generate(f, nonUnitClauses, handler);
    }
}
