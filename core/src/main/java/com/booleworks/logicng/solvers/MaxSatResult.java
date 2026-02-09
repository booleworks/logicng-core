// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers;

import com.booleworks.logicng.datastructures.Model;

import java.util.Objects;

/**
 * The result of a MaxSAT solve operation.
 * <p>
 * The result contains the information if the problem is satisfiable, and, if it
 * is, the computed optimum value and optimum model.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class MaxSatResult {

    private final boolean satisfiable;
    private final int satisfiedWeight;
    private final int unsatisfiedWeight;
    private final Model model;

    /**
     * Creates a new MaxSATResult with the given parameters.
     * @param satisfiable       whether the problem is satisfiable or not
     * @param satisfiedWeight   the sum of the weights of all satisfied clauses
     * @param unsatisfiedWeight the sum of the weights of all unsatisfied clauses
     * @param model             the model for optimum solution
     */
    public MaxSatResult(final boolean satisfiable, final int satisfiedWeight, final int unsatisfiedWeight,
                        final Model model) {
        this.satisfiable = satisfiable;
        this.satisfiedWeight = satisfiedWeight;
        this.unsatisfiedWeight = unsatisfiedWeight;
        this.model = model;
    }

    /**
     * Returns {@code true} if the MaxSAT problem is satisfiable, otherwise
     * {@code false}.
     * @return {@code true} if the MaxSAT problem is satisfiable
     */
    public boolean isSatisfiable() {
        return satisfiable;
    }

    /**
     * Returns the sum of the weights of all satisfied clauses of the MaxSAT
     * problem, or -1 if it is not satisfiable. This value is maximized by the
     * solver.
     * @return the sum of the weights of all satisfied clauses
     */
    public int getSatisfiedWeight() {
        return satisfiedWeight;
    }

    /**
     * Returns the sum of the weights of all unsatisfied clauses of the MaxSAT
     * problem, or -1 if it is not satisfiable. This value is minimized by the
     * solver.
     * @return the sum of the weights of all unsatisfied clauses
     */
    public int getUnsatisfiedWeight() {
        return unsatisfiedWeight;
    }

    /**
     * Returns the optimum model of the MaxSAT problem, or {@code null} if it is
     * not satisfiable.
     * @return the optimum model
     */
    public Model getModel() {
        return model;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MaxSatResult that = (MaxSatResult) o;
        return satisfiable == that.satisfiable && unsatisfiedWeight == that.unsatisfiedWeight
                && satisfiedWeight == that.satisfiedWeight && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(satisfiable, satisfiedWeight, unsatisfiedWeight, model);
    }

    @Override
    public String toString() {
        return "MaxSatResult{" +
                "satisfiable=" + satisfiable +
                ", satisfiedWeight=" + satisfiedWeight +
                ", unsatisfiedWeight=" + unsatisfiedWeight +
                ", model=" + model +
                '}';
    }
}
