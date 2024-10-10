package com.booleworks.logicng.csp.datastructures;

import com.booleworks.logicng.csp.terms.IntegerVariable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A class storing substitutions from integer variables to integer variables.
 */
public final class IntegerVariableSubstitution {
    private final Map<IntegerVariable, IntegerVariable> substitutions;

    /**
     * Constructs new substitution.
     */
    public IntegerVariableSubstitution() {
        substitutions = new HashMap<>();
    }

    /**
     * Copies new substitution from an existing substitution.
     * @param other the existing substitution
     */
    public IntegerVariableSubstitution(final IntegerVariableSubstitution other) {
        substitutions = new HashMap<>(other.substitutions);
    }

    /**
     * Returns the number of substitutions stored.
     * @return the number of substitutions stored
     */
    public int size() {
        return substitutions.size();
    }

    /**
     * Returns whether there are substitutions stored.
     * @return {@code true} if there are no substitutions stored, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return substitutions.isEmpty();
    }

    /**
     * Returns whether a variable has a substitution.
     * @param v the variable
     * @return {@code true} if the variable has a substitution, {@code false} otherwise.
     */
    public boolean containsKey(final IntegerVariable v) {
        return substitutions.containsKey(v);
    }

    /**
     * Adds a new substitution.
     * @param original   the original variable
     * @param substitute the substitute
     */
    public void add(final IntegerVariable original, final IntegerVariable substitute) {
        substitutions.put(original, substitute);
        // reverseDirection.put(substitute, original);
    }

    /**
     * Returns the substitute of a variable.
     * @param original the variable
     * @return the substitute
     */
    public IntegerVariable get(final IntegerVariable original) {
        return substitutions.get(original);
    }

    /**
     * Returns the substitute if a variable of the variable itself if there is no substitute.
     * @param original the variable
     * @return the substitute or the variable itself
     */
    public IntegerVariable getOrSelf(final IntegerVariable original) {
        return substitutions.getOrDefault(original, original);
    }

    /**
     * Returns the substitutes of a list of variables. If a substitute does not exist for a variable the variable
     * itself is used.
     * @param originals the variables
     * @return the substitutes
     */
    public List<IntegerVariable> getAllOrSelf(final Collection<IntegerVariable> originals) {
        return originals.stream().map(v -> substitutions.getOrDefault(v, v)).collect(Collectors.toList());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntegerVariableSubstitution)) {
            return false;
        }

        final IntegerVariableSubstitution that = (IntegerVariableSubstitution) o;
        return substitutions.equals(that.substitutions); //&& reverseDirection.equals(that.reverseDirection);
    }

    @Override
    public int hashCode() {
        final int result = substitutions.hashCode();
        // result = 31 * result + reverseDirection.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "IntegerVariableSubstitution{" + substitutions + '}';
    }
}
