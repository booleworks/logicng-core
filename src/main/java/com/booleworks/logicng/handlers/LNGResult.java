package com.booleworks.logicng.handlers;

import com.booleworks.logicng.handlers.events.LNGEvent;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class LNGResult<RESULT> {

    protected final RESULT result;
    protected final LNGEvent abortionEvent;

    protected LNGResult(final RESULT result, final LNGEvent abortionEvent) {
        this.result = result;
        this.abortionEvent = abortionEvent;
    }

    public static <RESULT> LNGResult<RESULT> of(final RESULT result) {
        assert result != null;
        return new LNGResult<>(result, null);
    }

    public static <RESULT> LNGResult<RESULT> aborted(final LNGEvent event) {
        assert event != null;
        return new LNGResult<>(null, event);
    }

    public RESULT getResult() {
        return result;
    }

    public LNGEvent getAbortionEvent() {
        return abortionEvent;
    }

    public boolean isSuccess() {
        return abortionEvent == null;
    }

    public <T> LNGResult<T> map(final Function<? super RESULT, ? extends T> transformation) {
        return new LNGResult<>(isSuccess() ? transformation.apply(result) : null, abortionEvent);
    }

    @SuppressWarnings("unchecked")
    public <T> LNGResult<T> flatMap(final Function<? super RESULT, ? extends LNGResult<? extends T>> transformation) {
        return isSuccess() ? (LNGResult<T>) transformation.apply(result) : LNGResult.aborted(abortionEvent);
    }

    public RESULT orElse(final RESULT alternative) {
        return isSuccess() ? result : alternative;
    }

    public <X extends Throwable> RESULT orElseThrow(final Supplier<X> exception) throws X {
        if (isSuccess()) {
            return result;
        } else {
            throw exception.get();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final LNGResult<?> that = (LNGResult<?>) o;
        return Objects.equals(result, that.result) && Objects.equals(abortionEvent, that.abortionEvent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, abortionEvent);
    }

    @Override
    public String toString() {
        return "ComputationResult{" +
                "result=" + result +
                ", abortionEvent=" + abortionEvent +
                '}';
    }
}
