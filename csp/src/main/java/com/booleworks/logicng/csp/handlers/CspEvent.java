// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.handlers;

import com.booleworks.logicng.handlers.events.SimpleEvent;

/**
 * A collection of simple computation events for CSP encoding algorithms.
 * @version 3.0.0
 * @since 3.0.0
 */
public final class CspEvent {
    private CspEvent() {
    }

    /**
     * Event that is raised if a CSP encoding algorithm is started.
     */
    public static final SimpleEvent CSP_ENCODING_STARTED = new SimpleEvent("Start CSP Encoding");
    /**
     * Event that is raised if a CSP encoding algorithm creates a new boolean
     * variable.
     */
    public static final SimpleEvent CSP_ENCODING_VAR_CREATED = new SimpleEvent("CSP Encoding Variable created");
    /**
     * Event that is raised if a CSP encoding algorithm adds a new clause to its
     * result.
     */
    public static final SimpleEvent CSP_ENCODING_CLAUSE_CREATED = new SimpleEvent("CSP Encoding Clause created");
}
