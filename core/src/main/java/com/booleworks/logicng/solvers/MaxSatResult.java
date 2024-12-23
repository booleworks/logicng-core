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
// TODO rename or redefine or fix documentation of 'optimum'
public class MaxSatResult {

    private final boolean satisfiable;
    private final int optimum;
    private final Model model;

    /**
     * Creates a new MaxSATResult with the given parameters.
     * @param satisfiable whether the problem is satisfiable or not
     * @param optimum     the optimum value
     * @param model       the model for optimum solution
     */
    public MaxSatResult(final boolean satisfiable, final int optimum, final Model model) {
        this.satisfiable = satisfiable;
        this.optimum = optimum;
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
     * Returns the optimum of the MaxSAT problem, or -1 if it is not
     * satisfiable.
     * @return the optimum of the MaxSAT problem
     */
    public int getOptimum() {
        return optimum;
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
        return satisfiable == that.satisfiable && optimum == that.optimum && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(satisfiable, optimum, model);
    }

    @Override
    public String toString() {
        return "MaxSatResult{" +
                "satisfiable=" + satisfiable +
                ", optimum=" + optimum +
                ", model=" + model +
                '}';
    }
}
