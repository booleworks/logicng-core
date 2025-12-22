// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.datastructures;

import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collections;
import java.util.Map;

/**
 * A data structure storing value hooks for multiple integer variables.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class CspValueHookMap {
    private final Map<IntegerVariable, Map<Variable, Integer>> hooks;

    /**
     * Constructs a new value hook map.
     * @param hooks hooks for all integer variables
     */
    public CspValueHookMap(final Map<IntegerVariable, Map<Variable, Integer>> hooks) {
        this.hooks = hooks;
    }

    /**
     * Returns the values hook for the given integer variable.
     * @param iv the integer variable
     * @return the value hooks
     */
    public Map<Variable, Integer> get(final IntegerVariable iv) {
        return Collections.unmodifiableMap(hooks.get(iv));
    }

    /**
     * Returns the value hooks of all variables
     * @return the value hooks of all variables
     */
    public Map<IntegerVariable, Map<Variable, Integer>> getHooks() {
        return Collections.unmodifiableMap(hooks);
    }

    /**
     * Adds or overwrites hooks for an integer variable.
     * <p>
     * The function returns the existing hooks or {@code null} if there were no
     * hooks for the integer variable.
     * @param iv    the integer variable
     * @param hooks the new hooks
     * @return the old hooks or {@code null} if there were no hooks.
     */
    public Map<Variable, Integer> put(final IntegerVariable iv, final Map<Variable, Integer> hooks) {
        return this.hooks.put(iv, hooks);
    }

    /**
     * Adds all hook mappings to this map. Existing mappings get overwritten.
     * @param other the other mapping
     */
    public void putAll(final Map<IntegerVariable, Map<Variable, Integer>> other) {
        this.hooks.putAll(other);
    }

    /**
     * Adds all hook mappings to this map. Existing mappings get overwritten.
     * @param other the other mapping
     */
    public void putAll(final CspValueHookMap other) {
        this.hooks.putAll(other.hooks);
    }
}
