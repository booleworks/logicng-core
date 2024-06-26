package com.booleworks.logicng.handlers.events;

import com.booleworks.logicng.handlers.ComputationHandler;

/**
 * A class for simple {@link LogicNGEvent} types which can basically be used
 * as singletons. By convention, simple events are instantiated as
 * {@code static final} fields s.t. they can be checked in
 * {@link ComputationHandler handlers} by using referential equality.
 * <p>
 * The {@link #description} should only be used for debugging purposes.
 * @version 3.0.0
 * @since 3.0.0
 */
public class SimpleEvent implements LogicNGEvent {

    public static final SimpleEvent NO_EVENT = new SimpleEvent("No event");
    public static final SimpleEvent DISTRIBUTION_PERFORMED = new SimpleEvent("Distribution performed");
    public static final SimpleEvent BDD_NEW_REF_ADDED = new SimpleEvent("New reference added in BDD");
    public static final SimpleEvent DNNF_SHANNON_EXPANSION = new SimpleEvent("DNNF Shannon Expansion");
    public static final SimpleEvent SAT_CONFLICT_DETECTED = new SimpleEvent("SAT conflict detected");
    public static final SimpleEvent MODEL_ENUMERATION_COMMIT = new SimpleEvent("Model Enumeration Commit");
    public static final SimpleEvent MODEL_ENUMERATION_ROLLBACK = new SimpleEvent("Model Enumeration Rollback");

    private final String description;

    public SimpleEvent(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Event: " + description;
    }

    // no equals and hashcode implementation -- should use referential equality
}
