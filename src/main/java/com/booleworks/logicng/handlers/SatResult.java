package com.booleworks.logicng.handlers;

import java.util.Objects;

public class SatResult<R> {
    private final R result;
    private final boolean sat;

    private SatResult(final R result, final boolean sat) {
        this.result = result;
        this.sat = sat;
    }

    public static <R> SatResult<R> sat(final R result) {
        return new SatResult<>(result, true);
    }

    public static <R> SatResult<R> unsat() {
        return new SatResult<>(null, false);
    }

    public R getResult() {
        return result;
    }

    public boolean isSat() {
        return sat;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SatResult<?> satResult = (SatResult<?>) o;
        return sat == satResult.sat && Objects.equals(result, satResult.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, sat);
    }

    @Override
    public String toString() {
        return "SatResult{" +
                "result=" + result +
                ", sat=" + sat +
                '}';
    }
}
