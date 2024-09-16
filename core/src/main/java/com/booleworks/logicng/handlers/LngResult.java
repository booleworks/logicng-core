package com.booleworks.logicng.handlers;

import com.booleworks.logicng.handlers.events.LngEvent;

import java.util.Objects;
import java.util.function.Function;

/**
 * The result of a computation which is cancelable by a
 * {@link ComputationHandler}. The computation was canceled iff
 * {@link #getCancelCause()} is not {@code null}.
 * <p>
 * A canceled result may provide a {@link #getPartialResult() partial result}.
 * <p>
 * The result of a successful computation must never be {@code null}.
 * @param <RESULT> the result type
 * @version 3.0.0
 * @since 3.0.0
 */
public class LngResult<RESULT> {

    protected final RESULT result;
    protected final LngEvent cancelCause;

    protected LngResult(final RESULT result, final LngEvent cancelCause) {
        this.result = result;
        this.cancelCause = cancelCause;
    }

    /**
     * Creates a new LNG result for a successful computation with the given
     * result.
     * @param result   the result of the successful computation,
     *                 must not be {@code null}
     * @param <RESULT> the type of the result
     * @return the LNG result
     */
    public static <RESULT> LngResult<RESULT> of(final RESULT result) {
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }
        return new LngResult<>(result, null);
    }

    /**
     * Creates a new LNG result for a computation which was canceled by the
     * given event.
     * @param cancelCause the event which canceled the computation,
     *                    must not be {@code null}
     * @param <RESULT>    the type of the result
     * @return the LNG result
     */
    public static <RESULT> LngResult<RESULT> canceled(final LngEvent cancelCause) {
        if (cancelCause == null) {
            throw new IllegalArgumentException("event cannot be null");
        }
        return new LngResult<>(null, cancelCause);
    }

    /**
     * Creates a new LNG result for a computation which was canceled by the
     * given event, but still can provide a partial result.
     * @param partialResult the partial result (allowed to be {@code null})
     * @param cancelCause   the event which canceled the computation,
     *                      must not be {@code null}
     * @param <RESULT>      the type of the result
     * @return the LNG result
     */
    public static <RESULT> LngResult<RESULT> partial(final RESULT partialResult, final LngEvent cancelCause) {
        return new LngResult<>(partialResult, cancelCause);
    }

    /**
     * Returns the result if the computation was not canceled, otherwise an
     * {@link IllegalStateException} is thrown.
     * @return the result if the computation was not canceled
     */
    public RESULT getResult() {
        if (cancelCause != null) {
            throw new IllegalStateException("Cannot return a result because the computation was canceled.");
        }
        return result;
    }

    /**
     * Returns the partial result of the computation. This can be {@code null}
     * if the computation could not provide a partial result, or it can also
     * represent the result of a successful and complete computation if the
     * {@link #getCancelCause() cancel cause} is {@code null}.
     * @return the partial result
     */
    public RESULT getPartialResult() {
        return result;
    }

    /**
     * Returns the event which canceled the computation or {@code null} if the
     * computation finished successfully.
     * @return the event which canceled the computation
     */
    public LngEvent getCancelCause() {
        return cancelCause;
    }

    /**
     * Returns {@code true} if the computation finished successfully (i.e. the
     * {@link #getCancelCause() cancel cause} is {@code null}), otherwise
     * {@code false}.
     * @return {@code true} if the computation finished successfully
     */
    public boolean isSuccess() {
        return cancelCause == null;
    }

    /**
     * Returns {@code true} if the computation of this result was canceled and
     * has a partial result. Returns {@code false} if the computation of this
     * result was canceled without partial result or successful.
     * @return {@code true} if the computation of this result was canceled and
     * has a partial result
     */
    public boolean isPartial() {
        return result != null && cancelCause != null;
    }

    /**
     * Transforms the result of a (potentially partially) successful computation
     * using the given transformation if this result is successful. Otherwise,
     * a new LNG result equals to this result is returned.1
     * @param transformation the transformation for the (partially) successful
     *                       result
     * @param <T>            the type of the new result
     * @return the transformed LNG result
     */
    public <T> LngResult<T> map(final Function<? super RESULT, ? extends T> transformation) {
        return new LngResult<>(result != null ? transformation.apply(result) : null, cancelCause);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final LngResult<?> that = (LngResult<?>) o;
        return Objects.equals(result, that.result) && Objects.equals(cancelCause, that.cancelCause);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, cancelCause);
    }

    @Override
    public String toString() {
        final String resultOrPartial = result != null && cancelCause != null ? "partialResult" : "result";
        return "ComputationResult{" +
                resultOrPartial + "=" + result +
                ", cancelCause=" + cancelCause +
                '}';
    }
}
