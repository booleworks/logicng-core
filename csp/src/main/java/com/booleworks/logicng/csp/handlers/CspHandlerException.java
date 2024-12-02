package com.booleworks.logicng.csp.handlers;

import com.booleworks.logicng.handlers.events.LngEvent;

/**
 * Exception that is used inside CSP Encoding algorithms to handle an abortion of the computation.
 */
public class CspHandlerException extends Exception {
    private final LngEvent reason;

    /**
     * Constructs a new exception from an {@code LngEvent} as abortion reason.
     * @param reason abortion reason
     */
    public CspHandlerException(final LngEvent reason) {
        super("CSP computation interrupted at event:" + reason);
        this.reason = reason;
    }

    /**
     * Return the abortion reason.
     * @return abortion reason
     */
    public LngEvent getReason() {
        return reason;
    }
}
