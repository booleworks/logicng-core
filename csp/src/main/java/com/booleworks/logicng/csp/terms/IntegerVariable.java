// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.datastructures.domains.IntegerDomain;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;

/**
 * An integer variable.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class IntegerVariable extends Term implements IntegerHolder {
    private final String name;
    private final IntegerDomain domain;
    private final boolean aux;

    /**
     * Generates a new variable in a given domain.
     * @param name   the variable's name
     * @param domain the variable's domain
     * @param aux    auxiliary tag
     */
    public IntegerVariable(final String name, final IntegerDomain domain, final boolean aux) {
        super(Type.VAR);
        this.name = name;
        this.domain = domain;
        this.aux = aux;
    }

    /**
     * Returns whether this variable is unsatisfiable, e.g. has an empty domain.
     * @return whether this variable is unsatisfiable
     */
    public boolean isUnsatisfiable() {
        return domain.isEmpty();
    }

    /**
     * Returns whether the variable is an auxiliary variable.
     * @return whether the variable is an auxiliary variable
     */
    public boolean isAux() {
        return aux;
    }

    /**
     * Returns the name of the variable.
     * @return the name of the variable
     */
    public String getName() {
        return name;
    }

    @Override
    public void variablesInplace(final SortedSet<IntegerVariable> variables) {
        variables.add(this);
    }

    @Override
    public boolean isAtom() {
        return true;
    }

    @Override
    public Decomposition calculateDecomposition(final CspFactory cf) {
        return new Decomposition(new LinearExpression(this), Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet());
    }

    @Override
    public IntegerDomain getDomain() {
        return domain;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof IntegerVariable) {
            return Objects.equals(name, ((IntegerVariable) other).name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Creates a new uncached auxiliary variable.
     * @param name   the name of the variable
     * @param domain the domain of the variable
     * @return a new uncached auxiliary variable
     */
    public static IntegerVariable auxVar(final String name, final IntegerDomain domain) {
        return new IntegerVariable(name, domain, true);
    }
}

