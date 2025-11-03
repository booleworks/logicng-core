package com.booleworks.logicng.csp.datastructures;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;

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
}
