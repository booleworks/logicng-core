package com.booleworks.logicng.solvers.maxsat;

import com.booleworks.logicng.collections.LNGBooleanVector;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.solvers.MaxSATResult;

import java.util.Objects;
import java.util.function.Function;

/**
 * Internal MaxSAT result, very similar to {@link MaxSATResult}, just with a
 * {@link LNGBooleanVector} as model.
 * @version 3.0.0
 * @since 3.0.0
 */
public class InternalMaxSATResult {
    private static final InternalMaxSATResult UNSAT = new InternalMaxSATResult(false, -1, null);

    private final boolean satisfiable;
    private final int optimum;
    private final LNGBooleanVector model;

    private InternalMaxSATResult(final boolean satisfiable, final int optimum, final LNGBooleanVector model) {
        this.satisfiable = satisfiable;
        this.optimum = optimum;
        this.model = model;
    }

    /**
     * Creates a new internal result for an unsatisfiable MaxSAT problem.
     * @return the result
     */
    public static InternalMaxSATResult unsatisfiable() {
        return UNSAT;
    }

    /**
     * Creates a new internal result for a satisfiable MaxSAT problem with the
     * given optimum and model.
     * @param optimum the optimum value
     * @param model   the model for the optimum solution
     * @return the result
     */
    public static InternalMaxSATResult optimum(final int optimum, final LNGBooleanVector model) {
        return new InternalMaxSATResult(true, optimum, model);
    }

    /**
     * Converts this internal result to a {@link MaxSATResult}.
     * @param modelConversion a function to convert the internal model to an
     *                        assignment
     * @return the converted result
     */
    public MaxSATResult toMaxSATResult(final Function<LNGBooleanVector, Model> modelConversion) {
        return satisfiable ? MaxSATResult.optimum(optimum, modelConversion.apply(model)) : MaxSATResult.unsatisfiable();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final InternalMaxSATResult that = (InternalMaxSATResult) o;
        return satisfiable == that.satisfiable && optimum == that.optimum && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(satisfiable, optimum, model);
    }

    @Override
    public String toString() {
        return "InternalMaxSATResult{" +
                "satisfiable=" + satisfiable +
                ", optimum=" + optimum +
                ", model=" + model +
                '}';
    }
}
