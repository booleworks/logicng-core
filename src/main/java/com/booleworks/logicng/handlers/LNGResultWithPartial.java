package com.booleworks.logicng.handlers;

import com.booleworks.logicng.handlers.events.LNGEvent;

import java.util.Objects;
import java.util.Optional;

public class LNGResultWithPartial<RESULT, PARTIAL> extends LNGResult<RESULT> {

    protected final PARTIAL partialResult;

    protected LNGResultWithPartial(final RESULT result, final PARTIAL partialResult, final LNGEvent cancelCause) {
        super(result, cancelCause);
        this.partialResult = partialResult;
    }

    // TODO naming ("of" not possible because of name clash with superclass)
    public static <RESULT, PARTIAL> LNGResultWithPartial<RESULT, PARTIAL> ofResult(final RESULT result) {
        return new LNGResultWithPartial<>(result, null, null);
    }

    public static <RESULT, PARTIAL> LNGResultWithPartial<RESULT, PARTIAL> canceled(
            final PARTIAL partialResult, final LNGEvent event) {
        return new LNGResultWithPartial<>(null, partialResult, event);
    }

    public Optional<PARTIAL> getPartialResult() {
        return Optional.ofNullable(partialResult);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final LNGResultWithPartial<?, ?> that = (LNGResultWithPartial<?, ?>) o;
        return Objects.equals(partialResult, that.partialResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), partialResult);
    }

    @Override
    public String toString() {
        return "LNGResultWithPartial{" +
                "result=" + result +
                ", partialResult=" + partialResult +
                ", cancelCause=" + cancelCause +
                '}';
    }
}
