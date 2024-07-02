package com.booleworks.logicng.handlers;

import java.util.Objects;

public class UnsatResult<R> {
    private final R result;
    private final boolean unsat;

    private UnsatResult(final R result, final boolean unsat) {
        this.result = result;
        this.unsat = unsat;
    }

    public static <R> UnsatResult<R> sat() {
        return new UnsatResult<>(null, false);
    }

    public static <R> UnsatResult<R> unsat(final R result) {
        return new UnsatResult<>(result, true);
    }

    public R getResult() {
        return result;
    }

    public boolean isUnsat() {
        return unsat;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UnsatResult<?> satResult = (UnsatResult<?>) o;
        return unsat == satResult.unsat && Objects.equals(result, satResult.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, unsat);
    }

    @Override
    public String toString() {
        return "UnsatResult{" +
                "result=" + result +
                ", unsat=" + unsat +
                '}';
    }
}
