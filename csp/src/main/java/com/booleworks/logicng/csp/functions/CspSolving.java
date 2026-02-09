// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResult;
import com.booleworks.logicng.datastructures.encodingresult.EncodingResultSolver;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.sat.SatCall;
import com.booleworks.logicng.solvers.sat.SatCallBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Functions for solving CSP problems.
 * @version 3.0.0
 * @since 3.0.0
 */
public class CspSolving {
    protected CspSolving() {
    }

    /**
     * Calculates whether a CSP is satisfiable.
     * @param cf      the factory
     * @param csp     the csp
     * @param context the encoding context
     * @return whether the CSP is satisfiable
     */
    public static boolean sat(final CspFactory cf, final Csp csp, final CspEncodingContext context) {
        return sat(cf, csp, null, context, null).getResult();
    }

    /**
     * Calculates whether a CSP is satisfiable.
     * @param cf      the factory
     * @param csp     the csp
     * @param context the encoding context
     * @param handler handler for processing events
     * @return whether the CSP is satisfiable or the event cancelling the
     * computation
     */
    public static LngResult<Boolean> sat(final CspFactory cf, final Csp csp, final CspEncodingContext context,
                                         final ComputationHandler handler) {
        return sat(cf, csp, null, context, handler);
    }

    /**
     * Calculates whether a CSP is satisfiable under a set of restrictions.  A
     * restriction maps an integer variable to a specific value that must hold.
     * @param cf           the factory
     * @param csp          the csp
     * @param restrictions the restriction map
     * @param context      the encoding context
     * @return whether the CSP is satisfiable
     */
    public static boolean sat(final CspFactory cf, final Csp csp, final Map<IntegerVariable, Integer> restrictions,
                              final CspEncodingContext context) {
        return sat(cf, csp, restrictions, context, null).getResult();
    }

    /**
     * Calculates whether a CSP is satisfiable under a set of restrictions.  A
     * restriction maps an integer variable to a specific value that must hold.
     * @param cf           the factory
     * @param csp          the csp
     * @param restrictions the restriction map
     * @param context      the encoding context
     * @param handler      handler for processing events
     * @return whether the CSP is satisfiable or the event cancelling the
     * computation
     */
    public static LngResult<Boolean> sat(final CspFactory cf, final Csp csp,
                                         final Map<IntegerVariable, Integer> restrictions,
                                         final CspEncodingContext context,
                                         final ComputationHandler handler) {
        final FormulaFactory f = cf.getFormulaFactory();
        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResult result = new EncodingResultSolver(f, solver.getUnderlyingSolver(), null);
        final LngResult<EncodingResult> r = cf.encodeCsp(csp, context, result, handler);
        if (!r.isSuccess()) {
            return LngResult.canceled(r.getCancelCause());
        }
        final SatCallBuilder scb = setupSatCall(cf, solver, restrictions, handler, context);
        return scb.sat();
    }

    /**
     * Calculates a model of a CSP.
     * @param cf      the factory
     * @param csp     the csp
     * @param context the encoding context
     * @return a model of the CSP or {@code null} if it is unsatisfiable
     */
    public static CspAssignment model(final CspFactory cf, final Csp csp, final CspEncodingContext context) {
        return model(cf, csp, null, context);
    }

    /**
     * Calculates a model of a CSP under a set of restrictions.  A restriction
     * maps an integer variable to a specific value that must hold.
     * @param cf           the factory
     * @param csp          the csp
     * @param restrictions the restriction map
     * @param context      the encoding context
     * @return a model of the CSP or {@code null} if it is unsatisfiable
     */
    public static CspAssignment model(final CspFactory cf, final Csp csp,
                                      final Map<IntegerVariable, Integer> restrictions,
                                      final CspEncodingContext context) {
        final FormulaFactory f = cf.getFormulaFactory();
        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResult result = new EncodingResultSolver(f, solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);
        return model(cf, solver, csp, restrictions, context);
    }

    /**
     * Calculates a model of a CSP.
     * @param cf      the factory
     * @param csp     the csp
     * @param context the encoding context
     * @param handler handler for processing events
     * @return a model of the CSP, empty optional if it is unsatisfiable, or the
     * event cancelling the computation.
     */
    public static LngResult<Optional<CspAssignment>> model(final CspFactory cf, final Csp csp,
                                                           final CspEncodingContext context,
                                                           final ComputationHandler handler) {
        return model(cf, csp, null, context, handler);
    }

    /**
     * Calculates a model of a CSP under a set of restrictions.  A restriction
     * maps an integer variable to a specific value that must hold.
     * @param cf           the factory
     * @param csp          the csp
     * @param restrictions the restriction map
     * @param context      the encoding context
     * @param handler      handler for processing events
     * @return a model of the CSP, empty optional if it is unsatisfiable, or the
     * event cancelling the computation.
     */
    public static LngResult<Optional<CspAssignment>> model(final CspFactory cf, final Csp csp,
                                                           final Map<IntegerVariable, Integer> restrictions,
                                                           final CspEncodingContext context,
                                                           final ComputationHandler handler) {
        final FormulaFactory f = cf.getFormulaFactory();
        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResult result = new EncodingResultSolver(f, solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);
        return model(cf, solver, csp, restrictions, context, handler);
    }

    /**
     * Calculates a model of a CSP.  The function assumes that the CSP is
     * already encoded on the solver.
     * @param cf      the factory
     * @param solver  the solver holding the encoded csp
     * @param csp     the csp
     * @param context the encoding context
     * @return a model of the CSP or null if it is unsatisfiable.
     */
    public static CspAssignment model(final CspFactory cf, final SatSolver solver, final Csp csp,
                                      final CspEncodingContext context) {
        return model(cf, solver, csp, null, context);
    }

    /**
     * Calculates a model of a CSP under a set of restrictions.  A restriction
     * maps an integer variable to a specific value that must hold.  The
     * function assumes that the CSP is already encoded on the solver.
     * @param cf           the factory
     * @param solver       the solver holding the encoded csp
     * @param csp          the csp
     * @param restrictions the restriction map
     * @param context      the encoding context
     * @return a model of the CSP or null if it is unsatisfiable.
     */
    public static CspAssignment model(final CspFactory cf, final SatSolver solver, final Csp csp,
                                      final Map<IntegerVariable, Integer> restrictions,
                                      final CspEncodingContext context) {
        return model(cf, solver,
                csp.getPropagateSubstitutions().getAllOrSelf(csp.getVisibleIntegerVariables()),
                csp.getVisibleBooleanVariables(), restrictions, context
        );
    }

    /**
     * Calculates a model of a CSP.  The function assumes that the CSP is
     * already encoded on the solver.
     * @param cf      the factory
     * @param solver  the solver holding the encoded csp
     * @param csp     the csp
     * @param context the encoding context
     * @param handler handler for processing events
     * @return a model of the CSP, empty optional if it is unsatisfiable, or
     * the event cancelling the computation.
     */
    public static LngResult<Optional<CspAssignment>> model(final CspFactory cf, final SatSolver solver, final Csp csp,
                                                           final CspEncodingContext context,
                                                           final ComputationHandler handler) {
        return model(cf, solver, csp, null, context, handler);
    }

    /**
     * Calculates a model of a CSP under a set of restrictions.  A restriction
     * maps an integer variable to a specific value that must hold.  The
     * function assumes that the CSP is already encoded on the solver.
     * @param cf           the factory
     * @param solver       the solver holding the encoded csp
     * @param csp          the csp
     * @param restrictions the restriction map
     * @param context      the encoding context
     * @param handler      handler for processing events
     * @return a model of the CSP, empty optional if it is unsatisfiable, or the
     * event cancelling the computation.
     */
    public static LngResult<Optional<CspAssignment>> model(final CspFactory cf, final SatSolver solver, final Csp csp,
                                                           final Map<IntegerVariable, Integer> restrictions,
                                                           final CspEncodingContext context,
                                                           final ComputationHandler handler) {
        return model(cf, solver,
                csp.getPropagateSubstitutions().getAllOrSelf(csp.getVisibleIntegerVariables()),
                csp.getVisibleBooleanVariables(), restrictions, context, handler
        );
    }

    /**
     * Calculates a model of a CSP.  The function assumes that the CSP is
     * already encoded on the solver.
     * @param cf               the factory
     * @param solver           the solver holding the encoded csp
     * @param integerVariables the relevant integer variables for the decoding
     * @param booleanVariables the relevant boolean variables for the decoding
     * @param context          the encoding context
     * @return a model of the CSP or null if it is unsatisfiable.
     */
    public static CspAssignment model(final CspFactory cf, final SatSolver solver,
                                      final Collection<IntegerVariable> integerVariables,
                                      final Collection<Variable> booleanVariables, final CspEncodingContext context) {
        return model(cf, solver, integerVariables, booleanVariables, null, context);
    }

    /**
     * Calculates a model of a CSP under a set of restrictions.  A restriction
     * maps an integer variable to a specific value that must hold.  The
     * function assumes that the CSP is already encoded on the solver.
     * @param cf               the factory
     * @param solver           the solver holding the encoded csp
     * @param integerVariables the relevant integer variables for the decoding
     * @param booleanVariables the relevant boolean variables for the decoding
     * @param restrictions     the restriction map
     * @param context          the encoding context
     * @return a model of the CSP or null if it is unsatisfiable.
     */
    public static CspAssignment model(final CspFactory cf, final SatSolver solver,
                                      final Collection<IntegerVariable> integerVariables,
                                      final Collection<Variable> booleanVariables,
                                      final Map<IntegerVariable, Integer> restrictions,
                                      final CspEncodingContext context) {
        final List<Variable> allVars = new ArrayList<>(context.getEncodingVariables(integerVariables));
        allVars.addAll(booleanVariables);
        final SatCallBuilder scb = setupSatCall(cf, solver, restrictions, null, context);
        final Model model = scb.model(allVars);
        if (model == null) {
            return null;
        }
        return cf.decode(model.toAssignment(), integerVariables, booleanVariables, context);
    }

    /**
     * Calculates a model of a CSP under a set of restrictions.  A restriction
     * maps an integer variable to a specific value that must hold.  The
     * function assumes that the CSP is already encoded on the solver.
     * @param cf               the factory
     * @param solver           the solver holding the encoded csp
     * @param integerVariables the relevant integer variables for the decoding
     * @param booleanVariables the relevant boolean variables for the decoding
     * @param restrictions     the restriction map
     * @param context          the encoding context
     * @param handler          handler for processing events
     * @return a model of the CSP, empty optional if it is unsatisfiable, or the
     * event cancelling the computation.
     */
    public static LngResult<Optional<CspAssignment>> model(final CspFactory cf, final SatSolver solver,
                                                           final Collection<IntegerVariable> integerVariables,
                                                           final Collection<Variable> booleanVariables,
                                                           final Map<IntegerVariable, Integer> restrictions,
                                                           final CspEncodingContext context,
                                                           final ComputationHandler handler) {
        final List<Variable> allVars = new ArrayList<>(context.getEncodingVariables(integerVariables));
        allVars.addAll(booleanVariables);
        final SatCallBuilder scb = setupSatCall(cf, solver, restrictions, handler, context);

        try (final SatCall call = scb.solve()) {
            if (!call.getSatResult().isSuccess()) {
                return LngResult.canceled(call.getSatResult().getCancelCause());
            } else if (!call.getSatResult().getResult()) {
                return LngResult.of(Optional.empty());
            } else {
                final Model model = call.model(allVars);
                final CspAssignment intModel =
                        cf.decode(model.toAssignment(), integerVariables, booleanVariables, context);
                return LngResult.of(Optional.of(intModel));
            }
        }
    }

    protected static SatCallBuilder setupSatCall(final CspFactory cf, final SatSolver solver,
                                                 final Map<IntegerVariable, Integer> restrictions,
                                                 final ComputationHandler handler, final CspEncodingContext context) {
        final SatCallBuilder scb = solver.satCall();
        if (handler != null) {
            scb.handler(handler);
        }
        if (restrictions != null) {
            final List<Literal> projectedRestrictions = new ArrayList<>();
            for (final Map.Entry<IntegerVariable, Integer> e : restrictions.entrySet()) {
                projectedRestrictions.addAll(
                        CspValueHookEncoding.computeRestrictionAssignments(cf, e.getKey(), e.getValue(), context));
            }
            scb.addFormulas(projectedRestrictions);
        }
        return scb;
    }
}
