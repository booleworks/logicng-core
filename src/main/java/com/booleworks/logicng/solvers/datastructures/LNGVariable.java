// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.datastructures;

import com.booleworks.logicng.datastructures.Tristate;

import java.util.Locale;

/**
 * A variable of the SAT solver.
 * @version 2.0.0
 * @since 1.0
 */
public final class LNGVariable {
    private Tristate assignment;
    private int level;
    private LNGClause reason;
    private double activity;
    private boolean polarity;
    private boolean decision;

    /**
     * Constructs a new variable with a given initial polarity.
     * @param polarity the initial polarity
     */
    public LNGVariable(final boolean polarity) {
        assignment = Tristate.UNDEF;
        level = -1;
        reason = null;
        activity = 0;
        this.polarity = polarity;
        decision = false;
    }

    LNGVariable(final Tristate assignment, final int level, final LNGClause reason, final double activity,
                final boolean polarity, final boolean decision) {
        this.assignment = assignment;
        this.level = level;
        this.reason = reason;
        this.activity = activity;
        this.polarity = polarity;
        this.decision = decision;
    }

    /**
     * Sets the decision level of this variable.
     * @param level the decision level
     */
    public void setLevel(final int level) {
        this.level = level;
    }

    /**
     * Returns the decision level of this variable.
     * @return the decision level of this variable
     */
    public int level() {
        return level;
    }

    /**
     * Sets the reason for this variable.
     * @param reason the reason for this variable
     */
    public void setReason(final LNGClause reason) {
        this.reason = reason;
    }

    /**
     * Returns the reason for this variable.
     * @return the reason for this variable
     */
    public LNGClause reason() {
        return reason;
    }

    /**
     * Assigns this variable to a given lifted Boolean.
     * @param assignment the lifted Boolean
     */
    public void assign(final Tristate assignment) {
        this.assignment = assignment;
    }

    /**
     * Returns the current assignment of this variable.
     * @return the current assignment of this variable
     */
    public Tristate assignment() {
        return assignment;
    }

    /**
     * Rescales this variable's activity.
     */
    public void rescaleActivity() {
        activity *= 1e-100;
    }

    /**
     * Increments this variable's activity by a given value
     * @param inc the increment value
     */
    public void incrementActivity(final double inc) {
        activity += inc;
    }

    /**
     * Returns the activity of this variable.
     * @return the activity of this variable
     */
    public double activity() {
        return activity;
    }

    /**
     * Sets the polarity of this variable.
     * @param polarity the polarity of this variable
     */
    public void setPolarity(final boolean polarity) {
        this.polarity = polarity;
    }

    /**
     * Returns the polarity of this variable.
     * @return the polarity of this variable
     */
    public boolean polarity() {
        return polarity;
    }

    /**
     * Returns {@code true} if this variable should be used as a decision
     * variable during solving, {@code false} otherwise.
     * @return {@code true} if this variable should be used as a decision
     *         variable
     */
    public boolean decision() {
        return decision;
    }

    /**
     * Sets whether this variable can be used as a decision variable during
     * solving or not.
     * @param decision {@code true} if it can be used as decision variable,
     *                 {@code false} otherwise
     */
    public void setDecision(final boolean decision) {
        this.decision = decision;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                "MSVariable{assignment=%s, level=%d, reason=%s, activity=%f, polarity=%s, decision=%s}",
                assignment, level, reason, activity, polarity, decision);
    }
}
