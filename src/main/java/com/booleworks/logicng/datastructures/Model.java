// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A simple immutable class representing a model of a formula. In contrast to an
 * {@link Assignment} a model just stores a simple list of literals and cannot
 * be used to evaluate or restrict a formula (because this would be very
 * inefficient). In this case you want to use the {@link #assignment()} method
 * to convert the model to an assignment first.
 * <p>
 * The primary use case for models is to use them in the model enumeration
 * function for minimal heap usage of the enumerated models.
 * <p>
 * For efficiency reasons, two models are only equal, if their literal list has
 * the same order. During a model enumeration this is always true.
 * @version 3.0.0
 * @since 3.0.0
 */
public class Model {
    private final List<Literal> literals;

    /**
     * Constructs a new model with a given list of literals.
     * @param literals the literals
     */
    public Model(final List<Literal> literals) {
        this.literals = literals;
    }

    /**
     * Constructs a new model with a given list of literals.
     * @param literals the literals
     */
    public Model(final Literal... literals) {
        this(Arrays.asList(literals));
    }

    /**
     * Constructs a new model from an assignment.
     * @param assignment the assignment
     */
    public Model(final Assignment assignment) {
        literals = new ArrayList<>(assignment.positiveVariables());
        literals.addAll(assignment.negativeLiterals());
    }

    /**
     * Returns the list of literals of this model.
     * @return the list of literals
     */
    public List<Literal> getLiterals() {
        return Collections.unmodifiableList(literals);
    }

    /**
     * Returns the size of this model.
     * @return the size of this model
     */
    public int size() {
        return literals.size();
    }

    /**
     * Converts this model to an assignment.
     * @return the assignment
     */
    public Assignment assignment() {
        return new Assignment(literals);
    }

    /**
     * Returns the formula for this model. This is a conjunction of all
     * literals.
     * @param f the formula factory
     * @return the conjunction of all literals in this model
     */
    public Formula formula(final FormulaFactory f) {
        return f.and(literals);
    }

    /**
     * Returns the positive literals of this model as variables.
     * @return the positive literals of this model
     */
    public SortedSet<Variable> positiveVariables() {
        final var set = new TreeSet<Variable>();
        for (final Literal literal : literals) {
            if (literal.phase()) {
                set.add(literal.variable());
            }
        }
        return set;
    }

    /**
     * Returns the negative literals of this model.
     * @return the negative literals of this model
     */
    public SortedSet<Literal> negativeLiterals() {
        final var set = new TreeSet<Literal>();
        for (final Literal literal : literals) {
            if (!literal.phase()) {
                set.add(literal);
            }
        }
        return set;
    }

    /**
     * Returns the negative literals of this model as variables.
     * @return the negative literals of this model
     */
    public SortedSet<Variable> negativeVariables() {
        final var set = new TreeSet<Variable>();
        for (final Literal lit : literals) {
            if (!lit.phase()) {
                set.add(lit.variable());
            }
        }
        return set;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Model model = (Model) o;
        return Objects.equals(literals, model.literals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(literals);
    }

    @Override
    public String toString() {
        return "Model{" +
                "literals=" + literals +
                '}';
    }
}
