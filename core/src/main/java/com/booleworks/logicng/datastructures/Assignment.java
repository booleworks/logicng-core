// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.datastructures;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A class representing a Boolean assignment, i.e. a mapping from variables to
 * truth values. In contrast to an {@link Model} an assignment stores the
 * variables internally in such a way that it can be efficiently evaluated.
 * <p>
 * The primary use case for assignments is their usage in the evaluation and
 * restriction of formulas. Assignments and models can be converted to each
 * other.
 * <p>
 * Two assignments are equal, if their literal are equal - independent of their
 * order.
 * @version 3.0.0
 * @since 1.0
 */
public final class Assignment {

    private final SortedSet<Variable> pos = new TreeSet<>();
    private final SortedSet<Literal> neg = new TreeSet<>();

    /**
     * Constructs a new empty assignment.
     */
    public Assignment() {
        // do nothing
    }

    /**
     * Constructs a new assignment for a given collection of literals.
     * @param lits the set of literals
     */
    public Assignment(final Collection<? extends Literal> lits) {
        for (final Literal lit : lits) {
            addLiteral(lit);
        }
    }

    /**
     * Constructs a new assignment for a given array of literals (without fast
     * evaluation).
     * @param lits a new assignment for a given array of literals
     */
    public Assignment(final Literal... lits) {
        for (final Literal lit : lits) {
            addLiteral(lit);
        }
    }

    /**
     * Constructs a new assignment with a single literal assignment.
     * @param lit the literal
     */
    public Assignment(final Literal lit) {
        addLiteral(lit);
    }

    /**
     * Constructs a new assignment from a given model.
     * @param model the model
     */
    public Assignment(final Model model) {
        this(model.getLiterals());
    }

    /**
     * Returns the number of literals in this assignment.
     * @return the number of literals in this assignment
     */
    public int size() {
        return pos.size() + neg.size();
    }

    /**
     * Returns the positive literals of this assignment as variables.
     * @return the positive literals of this assignment
     */
    public SortedSet<Variable> positiveVariables() {
        return Collections.unmodifiableSortedSet(pos);
    }

    /**
     * Returns the negative literals of this assignment.
     * @return the negative literals of this assignment
     */
    public SortedSet<Literal> negativeLiterals() {
        return Collections.unmodifiableSortedSet(neg);
    }

    /**
     * Returns the negative literals of this assignment as variables.
     * @return the negative literals of this assignment
     */
    public SortedSet<Variable> negativeVariables() {
        final SortedSet<Variable> negatedVariables = new TreeSet<>();
        for (final Literal lit : neg) {
            negatedVariables.add(lit.variable());
        }
        return negatedVariables;
    }

    /**
     * Returns all literals of this assignment.
     * @return all literals of this assignment
     */
    public SortedSet<Literal> literals() {
        final SortedSet<Literal> set = new TreeSet<>();
        set.addAll(pos);
        set.addAll(neg);
        return set;
    }

    /**
     * Add a single literal to this assignment.
     * @param lit the literal
     */
    public void addLiteral(final Literal lit) {
        if (lit.getPhase()) {
            pos.add(lit.variable());
        } else {
            neg.add(lit);
        }
    }

    /**
     * Evaluates a given literal. A literal not covered by the assignment
     * evaluates to {@code false} if it is positive, otherwise it evaluates to
     * {@code true}.
     * @param lit the literal
     * @return the evaluation of the literal
     */
    public boolean evaluateLit(final Literal lit) {
        return lit.getPhase() ? pos.contains(lit.variable()) : neg.contains(lit) || !pos.contains(lit.variable());
    }

    /**
     * Restricts a given literal to a constant. Returns the literal itself, if
     * the literal's variable is not known.
     * @param f   the formula factory to create the restricted formula
     * @param lit the literal
     * @return the restriction of the literal or the literal itself, if the
     * literal's variable is not known
     */
    public Formula restrictLit(final FormulaFactory f, final Literal lit) {
        final Variable var = lit.variable();
        if (pos.contains(var)) {
            return f.constant(lit.getPhase());
        }
        if (neg.contains(var.negate(f))) {
            return f.constant(!lit.getPhase());
        }
        return lit;
    }

    /**
     * Returns the assignment as a formula.
     * @param f the formula factory
     * @return the assignment as a formula
     */
    public Formula formula(final FormulaFactory f) {
        return f.and(literals());
    }

    /**
     * Converts this assignment to a model.
     * @return the model
     */
    public Model toModel() {
        return new Model(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, neg);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (getClass() == other.getClass()) {
            final Assignment o = (Assignment) other;
            return Objects.equals(pos, o.pos) && Objects.equals(neg, o.neg);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("Assignment{pos=%s, neg=%s}", pos, neg);
    }
}
