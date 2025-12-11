// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.terms;

import java.util.Objects;

/**
 * Represents a function term. A function term is composed of other terms.
 * @version 3.0.0
 * @since 3.0.0
 */
public abstract class Function extends Term {
    Function(final Term.Type type) {
        super(type);
    }

    @Override
    public boolean isAtom() {
        return false;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Function that = (Function) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
