// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.datastructures;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A Boolean assignment.
 * <p>
 * Note: the internal data structure is a plain list - no checking of the model is performed e.g. if
 * contradictory literals are added. Since assignments are used e.g. in the model enumeration of the SAT solvers these
 * checks would be too costly.
 * @version 3.0.0
 * @since 1.0
 */
public final class Assignment {

    private Collection<Variable> pos;
    private Collection<Literal> neg;
    private boolean fastEvaluable;

    /**
     * Constructs a new empty assignment (without fast evaluation).
     */
    public Assignment() {
        this(false);
    }

    /**
     * Constructs a new empty assignment.
     * @param fastEvaluable indicates whether this assignment should be evaluable fast.  If this parameter is set to
     *                      {@code true} the internal data structures will be optimized for fast evaluation but
     *                      creation of the object or adding literals can take longer.
     */
    public Assignment(final boolean fastEvaluable) {
        this.fastEvaluable = fastEvaluable;
        if (!fastEvaluable) {
            pos = new ArrayList<>();
            neg = new ArrayList<>();
        } else {
            pos = new HashSet<>();
            neg = new HashSet<>();
        }
    }

    /**
     * Constructs a new assignment for a given collection of literals (without fast evaluation).
     * @param lits a new assignment for a given collection of literals
     */
    public Assignment(final Collection<? extends Literal> lits) {
        this(lits, false);
    }

    /**
     * Constructs a new assignment for a given array of literals (without fast evaluation).
     * @param lits a new assignment for a given array of literals
     */
    public Assignment(final Literal... lits) {
        this(false);
        for (final Literal lit : lits) {
            addLiteral(lit);
        }
    }

    /**
     * Constructs a new assignment for a given collection of literals.
     * @param lits          a new assignment for a given collection of literals
     * @param fastEvaluable indicates whether this assignment should be evaluable fast.  If this parameter is set to
     *                      {@code true} the internal data structures will be optimized for fast evaluation but
     *                      creation of the object or adding literals can take longer.
     */
    public Assignment(final Collection<? extends Literal> lits, final boolean fastEvaluable) {
        this(fastEvaluable);
        for (final Literal lit : lits) {
            addLiteral(lit);
        }
    }

    /**
     * Constructs a new assignment with a single literal assignment (without fast evaluation).
     * @param lit the literal
     */
    public Assignment(final Literal lit) {
        this(lit, false);
    }

    /**
     * Constructs a new assignment with a single literal assignment.
     * @param lit           the literal
     * @param fastEvaluable indicates whether this assignment should be evaluable fast.  If this parameter is set to
     *                      {@code true} the internal data structures will be optimized for fast evaluation but
     *                      creation of the object or adding literals can take longer.
     */
    public Assignment(final Literal lit, final boolean fastEvaluable) {
        this(fastEvaluable);
        addLiteral(lit);
    }

    /**
     * Converts this assignment to a fast evaluable assignment.
     */
    public void convertToFastEvaluable() {
        if (!fastEvaluable) {
            pos = new HashSet<>(pos);
            neg = new HashSet<>(neg);
            fastEvaluable = true;
        }
    }

    /**
     * Returns whether this assignment is fast evaluable or not.
     * @return {@code true} if this assignment is fast evaluable, {@code false} otherwise
     */
    public boolean fastEvaluable() {
        return fastEvaluable;
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
    public List<Variable> positiveVariables() {
        return fastEvaluable ? Collections.unmodifiableList(new ArrayList<>(pos)) : Collections.unmodifiableList((List<Variable>) pos);
    }

    /**
     * Returns the negative literals of this assignment.
     * @return the negative literals of this assignment
     */
    public List<Literal> negativeLiterals() {
        return fastEvaluable ? Collections.unmodifiableList(new ArrayList<>(neg)) : Collections.unmodifiableList((List<Literal>) neg);
    }

    /**
     * Returns the negative literals of this assignment as variables.
     * @return the negative literals of this assignment
     */
    public List<Variable> negativeVariables() {
        final ArrayList<Variable> negatedVariables = new ArrayList<>();
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
        if (lit.phase()) {
            pos.add(lit.variable());
        } else {
            neg.add(lit);
        }
    }

    /**
     * Evaluates a given literal.  A literal not covered by the assignment evaluates
     * to {@code false} if it is positive, otherwise it evaluates to {@code true}.
     * @param lit the literal
     * @return the evaluation of the literal
     */
    public boolean evaluateLit(final Literal lit) {
        return lit.phase() ? pos.contains(lit.variable()) : neg.contains(lit) || !pos.contains(lit.variable());
    }

    /**
     * Restricts a given literal to a constant.  Returns the literal itself, if the literal's variable is not known.
     * @param lit the literal
     * @param f   the formula factory to create the restricted formula
     * @return the restriction of the literal or the literal itself, if the literal's variable is not known
     */
    public Formula restrictLit(final Literal lit, final FormulaFactory f) {
        final Variable var = lit.variable();
        if (pos.contains(var)) {
            return f.constant(lit.phase());
        }
        if (neg.contains(var.negate(f))) {
            return f.constant(!lit.phase());
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
     * Creates the blocking clause for this assignment.
     * @param f the formula factory
     * @return the blocking clause for this assignment
     */
    public Formula blockingClause(final FormulaFactory f) {
        final List<Literal> ops = new ArrayList<>();
        for (final Literal lit : pos) {
            ops.add(lit.negate(f));
        }
        for (final Literal lit : neg) {
            ops.add(lit.negate(f));
        }
        return f.or(ops);
    }

    /**
     * Creates the blocking clause for this assignment wrt. a given set of literals.  If the set is {@code null},
     * all literals are considered relevant.
     * @param f        the formula factory
     * @param literals the set of literals
     * @return the blocking clause for this assignment
     */
    public Formula blockingClause(final FormulaFactory f, final Collection<? extends Literal> literals) {
        if (literals == null) {
            return blockingClause(f);
        }
        final List<Literal> ops = new ArrayList<>();
        for (final Literal lit : literals) {
            final Variable var = lit.variable();
            final Literal negatedVar = var.negate(f);
            if (pos.contains(var)) {
                ops.add(negatedVar);
            } else if (neg.contains(negatedVar)) {
                ops.add(var);
            }
        }
        return f.or(ops);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toHashSet(pos), toHashSet(neg));
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
            return Objects.equals(toHashSet(pos), o.toHashSet(o.pos))
                    && Objects.equals(toHashSet(neg), o.toHashSet(o.neg));
        }
        return false;
    }

    /**
     * Returns a hash set containing the given literals. The given literals must be {@link this#pos} or {@link this#neg}.
     * @param literals the literal collection, either {@link this#pos} or {@link this#neg}
     * @return a hash set with the elements of the given literals
     */
    private Collection<? extends Literal> toHashSet(final Collection<? extends Literal> literals) {
        // invariant: if fastEvaluable is active, the pos and neg collections are already hash sets
        return fastEvaluable ? literals : new HashSet<>(literals);
    }

    @Override
    public String toString() {
        return String.format("Assignment{pos=%s, neg=%s}", pos, neg);
    }
}
