package com.booleworks.logicng.solvers;

import com.booleworks.logicng.datastructures.Assignment;

import java.util.Objects;

/**
 * The result of a MaxSAT solve operation.
 * <p>
 * The result contains the information if the problem is satisfiable, and, if it
 * is, the computed optimum value and optimum model.
 * @version 3.0.0
 * @since 3.0.0
 */
public class MaxSATResult {

    private static final MaxSATResult UNSAT = new MaxSATResult(false, -1, null);

    private final boolean satisfiable;
    private final int optimum;
    private final Assignment model;

    /**
     * Creates a new MaxSATResult with the given parameters.
     * @param satisfiable whether the problem is satisfiable or not
     * @param optimum     the optimum value
     * @param model       the model for optimum solution
     */
    private MaxSATResult(final boolean satisfiable, final int optimum, final Assignment model) {
        this.satisfiable = satisfiable;
        this.optimum = optimum;
        this.model = model;
    }

    /**
     * Creates a new result for an unsatisfiable MaxSAT problem.
     * @return the result
     */
    public static MaxSATResult unsatisfiable() {
        return UNSAT;
    }

    /**
     * Creates a new result for a satisfiable MaxSAT problem with the given
     * optimum and model.
     * @param optimum the optimum value
     * @param model   the model for the optimum solution
     * @return the result
     */
    public static MaxSATResult optimum(final int optimum, final Assignment model) {
        return new MaxSATResult(true, optimum, model);
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
    public Assignment getModel() {
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
        final MaxSATResult that = (MaxSATResult) o;
        return satisfiable == that.satisfiable && optimum == that.optimum && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(satisfiable, optimum, model);
    }

    @Override
    public String toString() {
        return "MaxSATResult{" +
                "satisfiable=" + satisfiable +
                ", optimum=" + optimum +
                ", model=" + model +
                '}';
    }
}
