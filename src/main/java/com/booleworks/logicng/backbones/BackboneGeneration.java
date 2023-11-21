// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.backbones;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.functions.BackboneFunction;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
import com.booleworks.logicng.util.FormulaHelper;

import java.util.Collection;
import java.util.Collections;

/**
 * Main entry point for backbone computations.
 * <p>
 * This class provides convenient methods for backbone computation for many use cases.
 * @version 3.0.0
 * @since 1.5.0
 */
public final class BackboneGeneration {

    /**
     * Private constructor.
     */
    private BackboneGeneration() {
        // Intentionally left empty.
    }

    /**
     * Computes the backbone for a given collection of formulas w.r.t. a collection of variables and a backbone type.
     * @param f         the formula factory to generate the solver and formulas
     * @param formulas  the given collection of formulas
     * @param variables the given collection of relevant variables for the backbone computation
     * @param type      the type of backbone variables that should be computed
     * @param handler   an optional handler for the backbone computation's SAT solver
     * @return the backbone or {@code null} if the computation was aborted by the handler
     */
    public static Backbone compute(final FormulaFactory f, final Collection<Formula> formulas, final Collection<Variable> variables, final BackboneType type,
                                   final SATHandler handler) {
        if (formulas == null || formulas.isEmpty()) {
            throw new IllegalArgumentException("Provide at least one formula for backbone computation");
        }
        final MiniSat miniSat = MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER).build());
        miniSat.add(formulas);
        return miniSat.execute(BackboneFunction.builder().handler(handler).variables(variables).type(type).build());
    }

    /**
     * Computes the backbone for a given collection of formulas w.r.t. a collection of variables and a backbone type.
     * @param f         the formula factory to generate the solver and formulas
     * @param formulas  the given collection of formulas
     * @param variables the given collection of relevant variables for the backbone computation
     * @param type      the type of backbone variables that should be computed
     * @return the backbone
     */
    public static Backbone compute(final FormulaFactory f, final Collection<Formula> formulas, final Collection<Variable> variables, final BackboneType type) {
        return compute(f, formulas, variables, type, null);
    }

    /**
     * Computes the complete backbone for a given collection of formulas w.r.t. a collection of variables and a backbone type.
     * @param f         the formula factory to generate the solver and formulas
     * @param formulas  the given collection of formulas
     * @param variables the given collection of relevant variables for the backbone computation
     * @return the backbone
     */
    public static Backbone compute(final FormulaFactory f, final Collection<Formula> formulas, final Collection<Variable> variables) {
        return compute(f, formulas, variables, BackboneType.POSITIVE_AND_NEGATIVE);
    }

    /**
     * Computes the backbone for a given collection of formulas w.r.t. a given backbone type.
     * @param f        the formula factory to generate the solver and formulas
     * @param formulas the given collection of formulas
     * @param type     the type of backbone variables that should be computed
     * @return the backbone
     */
    public static Backbone compute(final FormulaFactory f, final Collection<Formula> formulas, final BackboneType type) {
        return compute(f, formulas, FormulaHelper.variables(f, formulas), type);
    }

    /**
     * Computes the complete backbone for a given collection of formulas.
     * @param f        the formula factory to generate the solver and formulas
     * @param formulas the given collection of formulas
     * @return the backbone
     */
    public static Backbone compute(final FormulaFactory f, final Collection<Formula> formulas) {
        return compute(f, formulas, FormulaHelper.variables(f, formulas), BackboneType.POSITIVE_AND_NEGATIVE);
    }

    /**
     * Computes the backbone for a given formula w.r.t. a collection of variables and a backbone type.
     * @param f         the formula factory to generate the solver and formulas
     * @param formula   the given formula
     * @param variables the given collection of relevant variables for the backbone computation
     * @param type      the type of backbone variables that should be computed
     * @return the backbone
     */
    public static Backbone compute(final FormulaFactory f, final Formula formula, final Collection<Variable> variables, final BackboneType type) {
        return compute(f, Collections.singletonList(formula), variables, type);
    }

    /**
     * Computes the complete backbone for a given formula w.r.t. a collection of variables and a backbone type.
     * @param f         the formula factory to generate the solver and formulas
     * @param formula   the given formula
     * @param variables the given collection of relevant variables for the backbone computation
     * @return the backbone
     */
    public static Backbone compute(final FormulaFactory f, final Formula formula, final Collection<Variable> variables) {
        return compute(f, formula, variables, BackboneType.POSITIVE_AND_NEGATIVE);
    }

    /**
     * Computes the backbone for a given formula w.r.t. a given backbone type.
     * @param f       the formula factory to generate the solver and formulas
     * @param formula the given formula
     * @param type    the type of backbone variables that should be computed
     * @return the backbone
     */
    public static Backbone compute(final FormulaFactory f, final Formula formula, final BackboneType type) {
        return compute(f, formula, formula.variables(f), type);
    }

    /**
     * Computes the complete backbone for a given formula.
     * @param f       the formula factory to generate the solver and formulas
     * @param formula the given formula
     * @return the backbone
     */
    public static Backbone compute(final FormulaFactory f, final Formula formula) {
        return compute(f, formula, formula.variables(f), BackboneType.POSITIVE_AND_NEGATIVE);
    }

    /**
     * Computes the positive backbone variables for a given collection of formulas w.r.t. a collection of variables.
     * @param f         the formula factory to generate the solver and formulas
     * @param formulas  the given collection of formulas
     * @param variables the given collection of relevant variables for the backbone computation
     * @return the positive backbone
     */
    public static Backbone computePositive(final FormulaFactory f, final Collection<Formula> formulas, final Collection<Variable> variables) {
        return compute(f, formulas, variables, BackboneType.ONLY_POSITIVE);
    }

    /**
     * Computes the positive backbone variables for a given collection of formulas.
     * @param f        the formula factory to generate the solver and formulas
     * @param formulas the given collection of formulas
     * @return the positive backbone
     */
    public static Backbone computePositive(final FormulaFactory f, final Collection<Formula> formulas) {
        return compute(f, formulas, FormulaHelper.variables(f, formulas), BackboneType.ONLY_POSITIVE);
    }

    /**
     * Computes the positive backbone allVariablesInFormulas for a given formula w.r.t. a collection of variables.
     * @param f         the formula factory to generate the solver and formulas
     * @param formula   the given formula
     * @param variables the given collection of relevant variables for the backbone computation
     * @return the positive backbone
     */
    public static Backbone computePositive(final FormulaFactory f, final Formula formula, final Collection<Variable> variables) {
        return compute(f, formula, variables, BackboneType.ONLY_POSITIVE);
    }

    /**
     * Computes the positive backbone variables for a given formula.
     * @param f       the formula factory to generate the solver and formulas
     * @param formula the given formula
     * @return the positive backbone
     */
    public static Backbone computePositive(final FormulaFactory f, final Formula formula) {
        return compute(f, formula, formula.variables(f), BackboneType.ONLY_POSITIVE);
    }

    /**
     * Computes the negative backbone variables for a given collection of formulas w.r.t. a collection of variables.
     * @param f         the formula factory to generate the solver and formulas
     * @param formulas  the given collection of formulas
     * @param variables the given collection of relevant variables for the backbone computation
     * @return the negative backbone
     */
    public static Backbone computeNegative(final FormulaFactory f, final Collection<Formula> formulas, final Collection<Variable> variables) {
        return compute(f, formulas, variables, BackboneType.ONLY_NEGATIVE);
    }

    /**
     * Computes the negative backbone variables for a given collection of formulas.
     * @param f        the formula factory to generate the solver and formulas
     * @param formulas the given collection of formulas
     * @return the negative backbone
     */
    public static Backbone computeNegative(final FormulaFactory f, final Collection<Formula> formulas) {
        return compute(f, formulas, FormulaHelper.variables(f, formulas), BackboneType.ONLY_NEGATIVE);
    }

    /**
     * Computes the negative backbone variables for a given formula w.r.t. a collection of variables.
     * @param f         the formula factory to generate the solver and formulas
     * @param formula   the given formula
     * @param variables the given collection of relevant variables for the backbone computation
     * @return the negative backbone
     */
    public static Backbone computeNegative(final FormulaFactory f, final Formula formula, final Collection<Variable> variables) {
        return compute(f, formula, variables, BackboneType.ONLY_NEGATIVE);
    }

    /**
     * Computes the negative backbone variables for a given formula.
     * @param f       the formula factory to generate the solver and formulas
     * @param formula the given formula
     * @return the negative backbone
     */
    public static Backbone computeNegative(final FormulaFactory f, final Formula formula) {
        return compute(f, formula, formula.variables(f), BackboneType.ONLY_NEGATIVE);
    }
}
