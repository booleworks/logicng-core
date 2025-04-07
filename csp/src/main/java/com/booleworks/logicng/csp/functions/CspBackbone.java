package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.BackboneFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A backbone for CSPs storing mandatory and forbidden integer assignments and a boolean backbone.
 * <p>
 * The integer assignments are represented in a compressed manner. First there is a mapping for mandatory assignments
 * that assigns a mandatory integer value to an integer variable. Conversely, this means that all other values for this
 * variable are forbidden. (They are not stored in the map for forbidden values)
 * Variables that have no mandatory value, can have assignments in the mapping for forbidden values, which maps integer
 * variables to the set of forbidden variables, i.e., there is no model that assigns this integer variable to those
 * values.
 * <p>
 * Boolean variables are stored in a {@link Backbone}
 */
public class CspBackbone {
    private static final CspBackbone UNSAT_BACKBONE = new CspBackbone(null, null, Backbone.unsatBackbone());

    private final Map<IntegerVariable, Integer> mandatory;
    private final Map<IntegerVariable, SortedSet<Integer>> forbidden;
    private final Backbone booleanBackbone;

    private CspBackbone(final Map<IntegerVariable, Integer> mandatory,
                        final Map<IntegerVariable, SortedSet<Integer>> forbidden, final Backbone booleanBackbone) {
        this.mandatory = mandatory;
        this.forbidden = forbidden;
        this.booleanBackbone = booleanBackbone;
    }

    /**
     * Constructs a new backbone from a map of mandatory and forbidden assignments and a boolean backbone.
     * @param mandatory       mapping for mandatory assignments
     * @param forbidden       mapping for forbidden assignments
     * @param booleanBackbone boolean backbone
     * @return new backbone
     */
    public static CspBackbone satBackbone(final Map<IntegerVariable, Integer> mandatory,
                                          final Map<IntegerVariable, SortedSet<Integer>> forbidden,
                                          final Backbone booleanBackbone) {
        return new CspBackbone(mandatory, forbidden, booleanBackbone);
    }

    /**
     * Returns an unsatisfiable backbone.
     * @return an unsatisfiable backbone
     */
    public static CspBackbone unsatBackbone() {
        return UNSAT_BACKBONE;
    }

    /**
     * Returns whether a variable value pair is mandatory
     * @param v     the integer variable
     * @param value the value
     * @return whether the variable value pair is mandatory
     */
    public boolean isMandatory(final IntegerVariable v, final int value) {
        return mandatory.get(v) == value;
    }

    /**
     * Returns whether a variable value pair is forbidden
     * @param v     the integer variable
     * @param value the value
     * @return whether the variable value pair is forbidden
     */
    public boolean isForbidden(final IntegerVariable v, final int value) {
        return (mandatory.containsKey(v) && mandatory.get(v) != value)
                || (forbidden.containsKey(v) && forbidden.get(v).contains(value));
    }

    /**
     * Returns whether a variable value pair is optional
     * @param v     the integer variable
     * @param value the value
     * @return whether the variable value pair is optional
     */
    public boolean isOptional(final IntegerVariable v, final int value) {
        return !isMandatory(v, value) && !isForbidden(v, value);
    }

    /**
     * Returns the map for mandatory assignments
     * @return the map for mandatory assignments
     */
    public Map<IntegerVariable, Integer> getMandatory() {
        return Collections.unmodifiableMap(mandatory);
    }

    /**
     * Returns the map for forbidden assignments
     * @return the map for forbidden assignments
     */
    public Map<IntegerVariable, SortedSet<Integer>> getForbidden() {
        return Collections.unmodifiableMap(forbidden);
    }

    /**
     * Returns the boolean backbone.
     * @return the boolean backbone
     */
    public Backbone getBooleanBackbone() {
        return booleanBackbone;
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * This function adds new value hooks to the solver. There are alternative functions that take existing value hooks
     * as argument.
     * @param solver  the solver with the csp on it
     * @param type    the backbone type
     * @param csp     the csp
     * @param context the encoding context
     * @param result  the destination for the new value hooks
     * @param cf      the factory
     * @return the backbone
     */
    public static CspBackbone calculateBackbone(final SatSolver solver, final BackboneType type,
                                                final Csp csp, final CspEncodingContext context,
                                                final EncodingResult result, final CspFactory cf) {
        return calculateBackbone(solver, type, csp.getVisibleIntegerVariables(), csp.getVisibleBooleanVariables(),
                context, result, cf);
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * This function adds new value hooks to the solver. There are alternative functions that take existing value hooks
     * as argument.
     * @param solver  the solver with the csp on it
     * @param type    the backbone type
     * @param csp     the csp
     * @param context the encoding context
     * @param result  the destination for the new value hooks
     * @param cf      the factory
     * @param handler handler for processing events
     * @return the backbone
     */
    public static LngResult<CspBackbone> calculateBackbone(final SatSolver solver, final BackboneType type,
                                                           final Csp csp, final CspEncodingContext context,
                                                           final EncodingResult result, final CspFactory cf,
                                                           final ComputationHandler handler) {
        return calculateBackbone(solver, type, csp.getVisibleIntegerVariables(), csp.getVisibleBooleanVariables(),
                context, result, cf, handler);
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * This function adds new value hooks to the solver. There are alternative functions that take existing value hooks
     * as argument.
     * @param solver           the solver with the csp on it
     * @param type             the backbone type
     * @param integerVariables the relevant integer variables for the backbone
     * @param booleanVariables the relevant boolean variables for the backbone
     * @param context          the encoding context
     * @param result           the destination for the new value hooks
     * @param cf               the factory
     * @return the backbone
     */
    public static CspBackbone calculateBackbone(final SatSolver solver, final BackboneType type,
                                                final Collection<IntegerVariable> integerVariables,
                                                final Collection<Variable> booleanVariables,
                                                final CspEncodingContext context,
                                                final EncodingResult result, final CspFactory cf) {
        final Map<IntegerVariable, Map<Variable, Integer>> valueHooks =
                CspValueHook.encodeValueHooks(integerVariables, context, result, cf);
        return calculateBackbone(solver, type, integerVariables, booleanVariables, context, valueHooks, cf);
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * This function adds new value hooks to the solver. There are alternative functions that take existing value hooks
     * as argument.
     * @param solver           the solver with the csp on it
     * @param type             the backbone type
     * @param integerVariables the relevant integer variables for the backbone
     * @param booleanVariables the relevant boolean variables for the backbone
     * @param context          the encoding context
     * @param result           the destination for the new value hooks
     * @param cf               the factory
     * @param handler          handler for processing events
     * @return the backbone
     */
    public static LngResult<CspBackbone> calculateBackbone(final SatSolver solver, final BackboneType type,
                                                           final Collection<IntegerVariable> integerVariables,
                                                           final Collection<Variable> booleanVariables,
                                                           final CspEncodingContext context,
                                                           final EncodingResult result, final CspFactory cf,
                                                           final ComputationHandler handler) {
        final Map<IntegerVariable, Map<Variable, Integer>> valueHooks =
                CspValueHook.encodeValueHooks(integerVariables, context, result, cf);
        return calculateBackbone(solver, type, integerVariables, booleanVariables, context, valueHooks, cf, handler);
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * It assumes that the necessary value hooks are already encoded on the solver.
     * @param solver     the solver with the csp on it
     * @param type       the backbone type
     * @param csp        the csp
     * @param context    the encoding context
     * @param valueHooks the value hooks
     * @param cf         the factory
     * @return the backbone
     */
    public static CspBackbone calculateBackbone(final SatSolver solver, final BackboneType type, final Csp csp,
                                                final CspEncodingContext context,
                                                final Map<IntegerVariable, Map<Variable, Integer>> valueHooks,
                                                final CspFactory cf) {
        return calculateBackbone(solver, type, csp.getVisibleIntegerVariables(), csp.getVisibleBooleanVariables(),
                context, valueHooks, cf);
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * It assumes that the necessary value hooks are already encoded on the solver.
     * @param solver     the solver with the csp on it
     * @param type       the backbone type
     * @param csp        the csp
     * @param context    the encoding context
     * @param valueHooks the value hooks
     * @param cf         the factory
     * @param handler    handler for processing events
     * @return the backbone
     */
    public static LngResult<CspBackbone> calculateBackbone(final SatSolver solver,
                                                           final BackboneType type, final Csp csp,
                                                           final CspEncodingContext context,
                                                           final Map<IntegerVariable, Map<Variable, Integer>> valueHooks,
                                                           final CspFactory cf, final ComputationHandler handler) {
        return calculateBackbone(solver, type, csp.getVisibleIntegerVariables(), csp.getVisibleBooleanVariables(),
                context, valueHooks, cf, handler);
    }


    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * It assumes that the necessary value hooks are already encoded on the solver.
     * @param solver           the solver with the csp on it
     * @param type             the backbone type
     * @param integerVariables the relevant integer variables for the backbone
     * @param booleanVariables the relevant boolean variables for the backbone
     * @param context          the encoding context
     * @param valueHooks       the value hooks
     * @param cf               the factory
     * @return the backbone
     */
    public static CspBackbone calculateBackbone(final SatSolver solver,
                                                final BackboneType type,
                                                final Collection<IntegerVariable> integerVariables,
                                                final Collection<Variable> booleanVariables,
                                                final CspEncodingContext context,
                                                final Map<IntegerVariable, Map<Variable, Integer>> valueHooks,
                                                final CspFactory cf) {
        return calculateBackbone(solver, type, integerVariables, booleanVariables, context, valueHooks, cf,
                NopHandler.get()).getResult();
    }

    /**
     * Calculates the backbone of a CSP that is encoded on the solver.
     * <p>
     * It assumes that the necessary value hooks are already encoded on the solver.
     * @param solver           the solver with the csp on it
     * @param type             the backbone type
     * @param integerVariables the relevant integer variables for the backbone
     * @param booleanVariables the relevant boolean variables for the backbone
     * @param context          the encoding context
     * @param valueHooks       the value hooks
     * @param cf               the factory
     * @param handler          handler for processing events
     * @return the backbone
     */
    public static LngResult<CspBackbone> calculateBackbone(final SatSolver solver,
                                                           final BackboneType type,
                                                           final Collection<IntegerVariable> integerVariables,
                                                           final Collection<Variable> booleanVariables,
                                                           final CspEncodingContext context,
                                                           final Map<IntegerVariable, Map<Variable, Integer>> valueHooks,
                                                           final CspFactory cf, final ComputationHandler handler) {
        final List<Variable> hookVariables = valueHooks.values().stream()
                .flatMap(m -> m.keySet().stream()).collect(Collectors.toList());
        final List<Variable> relevantVariables = new ArrayList<>(booleanVariables);
        relevantVariables.addAll(hookVariables);
        final BackboneFunction backboneFunction =
                BackboneFunction.builder().variables(relevantVariables).type(type).build();
        final LngResult<Backbone> backboneResult = solver.execute(backboneFunction, handler);
        if (!backboneResult.isSuccess()) {
            return LngResult.canceled(backboneResult.getCancelCause());
        }
        final Backbone backbone = backboneResult.getResult();
        if (!backbone.isSat()) {
            return LngResult.of(CspBackbone.unsatBackbone());
        }
        final Backbone filteredBackbone = filterBackbone(backbone, booleanVariables);
        final CspBackbone cspBackbone = new CspBackbone(new LinkedHashMap<>(), new LinkedHashMap<>(), filteredBackbone);
        for (final IntegerVariable iv : integerVariables) {
            valueHooks.get(iv).forEach((k, v) -> {
                if (backbone.getPositiveBackbone().contains(k)) {
                    cspBackbone.mandatory.put(iv, v);
                }
            });
        }
        for (final IntegerVariable iv : integerVariables) {
            if (!cspBackbone.mandatory.containsKey(iv)) {
                final SortedSet<Integer> forbidden = new TreeSet<>();
                valueHooks.get(iv).forEach((k, v) -> {
                    if (backbone.getNegativeBackbone().contains(k)) {
                        forbidden.add(v);
                    }
                });
                if (!forbidden.isEmpty()) {
                    cspBackbone.forbidden.put(iv, forbidden);
                }
            }
        }
        return LngResult.of(cspBackbone);
    }

    private static Backbone filterBackbone(final Backbone backbone, final Collection<Variable> relevantVariables) {
        return Backbone.satBackbone(
                backbone.getPositiveBackbone().stream().filter(relevantVariables::contains)
                        .collect(Collectors.toCollection(TreeSet::new)),
                backbone.getNegativeBackbone().stream().filter(relevantVariables::contains)
                        .collect(Collectors.toCollection(TreeSet::new)),
                backbone.getOptionalVariables().stream().filter(relevantVariables::contains)
                        .collect(Collectors.toCollection(TreeSet::new))
        );
    }
}
