// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.handlers;

import com.booleworks.logicng.handlers.events.LngEvent;

/**
 * A collection of simple computation events for CSP encoding algorithms.
 * @version 3.0.0
 * @since 3.0.0
 */
public class CspEvent implements LngEvent {
    protected final String description;

    public CspEvent(final String description) {
        this.description = description;
    }

    /**
     * Event that is raised if a CSP encoding algorithm is started.
     */
    public static final CspEvent CSP_ENCODING_STARTED = new CspEvent("Start CSP Encoding");
    /**
     * Event that is raised if a CSP encoding algorithm creates a new boolean
     * variable.
     */
    public static final CspEvent CSP_ENCODING_VAR_CREATED = new CspEvent("CSP Encoding Variable created");
    /**
     * Event that is raised if a CSP encoding algorithm adds a new clause to its
     * result.
     */
    public static final CspEvent CSP_ENCODING_CLAUSE_CREATED = new CspEvent("CSP Encoding Clause created");
}
