// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.handlers;

import com.booleworks.logicng.handlers.events.LngEvent;

/**
 * Exception that is used inside CSP Encoding algorithms to handle a
 * cancellation of the computation.
 * @version 3.0.0
 * @since 3.0.0
 */
public class CspHandlerException extends Exception {
    private final LngEvent reason;

    /**
     * Constructs a new exception from an {@code LngEvent} as cancel cause.
     * @param reason cancel cause
     */
    public CspHandlerException(final LngEvent reason) {
        super("CSP computation interrupted at event:" + reason);
        this.reason = reason;
    }

    /**
     * Return the cancel cause.
     * @return cancel cause
     */
    public LngEvent getReason() {
        return reason;
    }
}
