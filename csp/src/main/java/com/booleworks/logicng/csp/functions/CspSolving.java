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
public final class CspSolving {
    private CspSolving() {
    }

    /**
     * Calculates whether a CSP is satisfiable.
     * @param csp     the csp
     * @param context the encoding context
     * @param cf      the factory
     * @return whether the CSP is satisfiable
     */
    public static boolean sat(final Csp csp, final CspEncodingContext context, final CspFactory cf) {
        return sat(csp, null, context, cf, null).getResult();
    }

    /**
     * Calculates whether a CSP is satisfiable.
     * @param csp     the csp
     * @param context the encoding context
     * @param cf      the factory
     * @param handler handler for processing events
     * @return whether the CSP is satisfiable or the event cancelling the
     * computation
     */
    public static LngResult<Boolean> sat(final Csp csp, final CspEncodingContext context, final CspFactory cf, final
    ComputationHandler handler) {
        return sat(csp, null, context, cf, handler);
    }

    /**
     * Calculates whether a CSP is satisfiable under a set of restrictions.  A
     * restriction maps an integer variable to a specific value that must hold.
     * @param csp          the csp
     * @param restrictions the restriction map
     * @param context      the encoding context
     * @param cf           the factory
     * @return whether the CSP is satisfiable
     */
    public static boolean sat(final Csp csp, final Map<IntegerVariable, Integer> restrictions,
                              final CspEncodingContext context, final CspFactory cf) {
        return sat(csp, restrictions, context, cf, null).getResult();
    }

    /**
     * Calculates whether a CSP is satisfiable under a set of restrictions.  A
     * restriction maps an integer variable to a specific value that must hold.
     * @param csp          the csp
     * @param restrictions the restriction map
     * @param context      the encoding context
     * @param cf           the factory
     * @param handler      handler for processing events
     * @return whether the CSP is satisfiable or the event cancelling the
     * computation
     */
    public static LngResult<Boolean> sat(final Csp csp, final Map<IntegerVariable, Integer> restrictions,
                                         final CspEncodingContext context, final CspFactory cf,
                                         final ComputationHandler handler) {
        final FormulaFactory f = cf.getFormulaFactory();
        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResult result = new EncodingResultSolver(f, solver.getUnderlyingSolver(), null);
        final LngResult<EncodingResult> r = cf.encodeCsp(csp, context, result, handler);
        if (!r.isSuccess()) {
            return LngResult.canceled(r.getCancelCause());
        }
        final SatCallBuilder scb = setupSatCall(solver, restrictions, handler, context, cf);
        return scb.sat();
    }

    /**
     * Calculates a model of a CSP.
     * @param csp     the csp
     * @param context the encoding context
     * @param cf      the factory
     * @return a model of the CSP or {@code null} if it is unsatisfiable
     */
    public static CspAssignment model(final Csp csp, final CspEncodingContext context, final CspFactory cf) {
        return model(csp, null, context, cf);
    }

    /**
     * Calculates a model of a CSP under a set of restrictions.  A restriction
     * maps an integer variable to a specific value that must hold.
     * @param csp          the csp
     * @param restrictions the restriction map
     * @param context      the encoding context
     * @param cf           the factory
     * @return a model of the CSP or {@code null} if it is unsatisfiable
     */
    public static CspAssignment model(final Csp csp, final Map<IntegerVariable, Integer> restrictions,
                                      final CspEncodingContext context, final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResult result = new EncodingResultSolver(f, solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);
        return model(solver, csp, restrictions, context, cf);
    }

    /**
     * Calculates a model of a CSP.
     * @param csp     the csp
     * @param context the encoding context
     * @param cf      the factory
     * @param handler handler for processing events
     * @return a model of the CSP, empty optional if it is unsatisfiable, or the
     * event cancelling the computation.
     */
    public static LngResult<Optional<CspAssignment>> model(final Csp csp,
                                                           final CspEncodingContext context, final CspFactory cf,
                                                           final ComputationHandler handler) {
        return model(csp, null, context, cf, handler);
    }

    /**
     * Calculates a model of a CSP under a set of restrictions.  A restriction
     * maps an integer variable to a specific value that must hold.
     * @param csp          the csp
     * @param restrictions the restriction map
     * @param context      the encoding context
     * @param cf           the factory
     * @param handler      handler for processing events
     * @return a model of the CSP, empty optional if it is unsatisfiable, or the
     * event cancelling the computation.
     */
    public static LngResult<Optional<CspAssignment>> model(final Csp csp,
                                                           final Map<IntegerVariable, Integer> restrictions,
                                                           final CspEncodingContext context, final CspFactory cf,
                                                           final ComputationHandler handler) {
        final FormulaFactory f = cf.getFormulaFactory();
        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResult result = new EncodingResultSolver(f, solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);
        return model(solver, csp, restrictions, context, cf, handler);
    }

    /**
     * Calculates a model of a CSP.  The function assumes that the CSP is
     * already encoded on the solver.
     * @param solver  the solver holding the encoded csp
     * @param csp     the csp
     * @param context the encoding context
     * @param cf      the factory
     * @return a model of the CSP or null if it is unsatisfiable.
     */
    public static CspAssignment model(final SatSolver solver, final Csp csp, final CspEncodingContext context,
                                      final CspFactory cf) {
        return model(solver, csp, null, context, cf);
    }

    /**
     * Calculates a model of a CSP under a set of restrictions.  A restriction
     * maps an integer variable to a specific value that must hold.  The
     * function assumes that the CSP is already encoded on the solver.
     * @param solver       the solver holding the encoded csp
     * @param csp          the csp
     * @param restrictions the restriction map
     * @param context      the encoding context
     * @param cf           the factory
     * @return a model of the CSP or null if it is unsatisfiable.
     */
    public static CspAssignment model(final SatSolver solver, final Csp csp,
                                      final Map<IntegerVariable, Integer> restrictions,
                                      final CspEncodingContext context, final CspFactory cf) {
        return model(solver,
                csp.getPropagateSubstitutions().getAllOrSelf(csp.getVisibleIntegerVariables()),
                csp.getVisibleBooleanVariables(), restrictions, context, cf
        );
    }

    /**
     * Calculates a model of a CSP.  The function assumes that the CSP is
     * already encoded on the solver.
     * @param solver  the solver holding the encoded csp
     * @param csp     the csp
     * @param context the encoding context
     * @param cf      the factory
     * @param handler handler for processing events
     * @return a model of the CSP, empty optional if it is unsatisfiable, or
     * the event cancelling the computation.
     */
    public static LngResult<Optional<CspAssignment>> model(final SatSolver solver, final Csp csp,
                                                           final CspEncodingContext context,
                                                           final CspFactory cf, final ComputationHandler handler) {
        return model(solver, csp, null, context, cf, handler);
    }

    /**
     * Calculates a model of a CSP under a set of restrictions.  A restriction
     * maps an integer variable to a specific value that must hold.  The
     * function assumes that the CSP is already encoded on the solver.
     * @param solver       the solver holding the encoded csp
     * @param csp          the csp
     * @param restrictions the restriction map
     * @param context      the encoding context
     * @param cf           the factory
     * @param handler      handler for processing events
     * @return a model of the CSP, empty optional if it is unsatisfiable, or the
     * event cancelling the computation.
     */
    public static LngResult<Optional<CspAssignment>> model(final SatSolver solver, final Csp csp,
                                                           final Map<IntegerVariable, Integer> restrictions,
                                                           final CspEncodingContext context,
                                                           final CspFactory cf, final ComputationHandler handler) {
        return model(solver,
                csp.getPropagateSubstitutions().getAllOrSelf(csp.getVisibleIntegerVariables()),
                csp.getVisibleBooleanVariables(), restrictions, context, cf, handler
        );
    }

    /**
     * Calculates a model of a CSP.  The function assumes that the CSP is
     * already encoded on the solver.
     * @param solver           the solver holding the encoded csp
     * @param integerVariables the relevant integer variables for the decoding
     * @param booleanVariables the relevant boolean variables for the decoding
     * @param context          the encoding context
     * @param cf               the factory
     * @return a model of the CSP or null if it is unsatisfiable.
     */
    public static CspAssignment model(final SatSolver solver, final Collection<IntegerVariable> integerVariables,
                                      final Collection<Variable> booleanVariables, final CspEncodingContext context,
                                      final CspFactory cf) {
        return model(solver, integerVariables, booleanVariables, null, context, cf);
    }

    /**
     * Calculates a model of a CSP under a set of restrictions.  A restriction
     * maps an integer variable to a specific value that must hold.  The
     * function assumes that the CSP is already encoded on the solver.
     * @param solver           the solver holding the encoded csp
     * @param integerVariables the relevant integer variables for the decoding
     * @param booleanVariables the relevant boolean variables for the decoding
     * @param restrictions     the restriction map
     * @param context          the encoding context
     * @param cf               the factory
     * @return a model of the CSP or null if it is unsatisfiable.
     */
    public static CspAssignment model(final SatSolver solver,
                                      final Collection<IntegerVariable> integerVariables,
                                      final Collection<Variable> booleanVariables,
                                      final Map<IntegerVariable, Integer> restrictions,
                                      final CspEncodingContext context,
                                      final CspFactory cf) {
        final List<Variable> allVars = new ArrayList<>(context.getSatVariables(integerVariables));
        allVars.addAll(booleanVariables);
        final SatCallBuilder scb = setupSatCall(solver, restrictions, null, context, cf);
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
     * @param solver           the solver holding the encoded csp
     * @param integerVariables the relevant integer variables for the decoding
     * @param booleanVariables the relevant boolean variables for the decoding
     * @param restrictions     the restriction map
     * @param context          the encoding context
     * @param cf               the factory
     * @param handler          handler for processing events
     * @return a model of the CSP, empty optional if it is unsatisfiable, or the
     * event cancelling the computation.
     */
    public static LngResult<Optional<CspAssignment>> model(final SatSolver solver,
                                                           final Collection<IntegerVariable> integerVariables,
                                                           final Collection<Variable> booleanVariables,
                                                           final Map<IntegerVariable, Integer> restrictions,
                                                           final CspEncodingContext context,
                                                           final CspFactory cf, final ComputationHandler handler) {
        final List<Variable> allVars = new ArrayList<>(context.getSatVariables(integerVariables));
        allVars.addAll(booleanVariables);
        final SatCallBuilder scb = setupSatCall(solver, restrictions, handler, context, cf);

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

    private static SatCallBuilder setupSatCall(final SatSolver solver, final Map<IntegerVariable, Integer> restrictions,
                                               final ComputationHandler handler, final CspEncodingContext context,
                                               final CspFactory cf) {
        final SatCallBuilder scb = solver.satCall();
        if (handler != null) {
            scb.handler(handler);
        }
        if (restrictions != null) {
            final List<Literal> projectedRestrictions = new ArrayList<>();
            for (final Map.Entry<IntegerVariable, Integer> e : restrictions.entrySet()) {
                projectedRestrictions.addAll(
                        CspValueHookEncoding.computeRestrictionAssignments(e.getKey(), e.getValue(), context, cf));
            }
            scb.addFormulas(projectedRestrictions);
        }
        return scb;
    }
}
