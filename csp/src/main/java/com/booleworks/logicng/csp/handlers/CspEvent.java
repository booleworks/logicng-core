package com.booleworks.logicng.csp.handlers;

import com.booleworks.logicng.handlers.events.SimpleEvent;

public class CspEvent {
    /**
     * Event that is raised if a CSP encoding algorithm is started.
     */
    public static final SimpleEvent CSP_ENCODING_STARTED = new SimpleEvent("Start CSP Encoding");
    /**
     * Event that is raised if a CSP encoding algorithm creates a new boolean variable.
     */
    public static final SimpleEvent CSP_ENCODING_VAR_CREATED = new SimpleEvent("CSP Encoding Variable created");
    /**
     * Event that is raised if a CSP encoding algorithm adds a new clause to its result-
     */
    public static final SimpleEvent CSP_ENCODING_CLAUSE_CREATED = new SimpleEvent("CSP Encoding Clause created");
}
