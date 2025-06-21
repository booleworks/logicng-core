package com.booleworks.logicng.handlers.events;

import com.booleworks.logicng.handlers.ComputationHandler;

/**
 * A class for simple {@link LngEvent} types which can basically be used
 * as singletons. By convention, simple events are instantiated as
 * {@code static final} fields s.t. they can be checked in
 * {@link ComputationHandler handlers} by using referential equality.
 * <p>
 * The {@link #description} should only be used for debugging purposes.
 * @version 3.0.0
 * @since 3.0.0
 */
public class SimpleEvent implements LngEvent {

    public static final SimpleEvent NO_EVENT = new SimpleEvent("No event");
    public static final SimpleEvent DISTRIBUTION_PERFORMED = new SimpleEvent("Distribution performed");
    public static final SimpleEvent BDD_NEW_REF_ADDED = new SimpleEvent("New reference added in BDD");
    public static final SimpleEvent DNNF_DTREE_MIN_FILL_GRAPH_INITIALIZED =
            new SimpleEvent("DNNF DTree MinFill Graph initialized");
    public static final SimpleEvent DNNF_DTREE_MIN_FILL_NEW_ITERATION =
            new SimpleEvent("DNNF DTree MinFill new iteration");
    public static final SimpleEvent DNNF_DTREE_PROCESSING_NEXT_ORDER_VARIABLE =
            new SimpleEvent("DNNF DTree processing next order variable");
    public static final SimpleEvent DNNF_SHANNON_EXPANSION = new SimpleEvent("DNNF Shannon Expansion");
    public static final SimpleEvent VTREE_CUTSET_GENERATION = new SimpleEvent("VTree Cutset Generation");
    public static final SimpleEvent SDD_APPLY = new SimpleEvent("SDD Apply");
    public static final SimpleEvent SDD_SHANNON_EXPANSION = new SimpleEvent("SDD Shannon Expansion");
    public static final SimpleEvent SAT_CONFLICT_DETECTED = new SimpleEvent("SAT conflict detected");
    public static final SimpleEvent MODEL_ENUMERATION_COMMIT = new SimpleEvent("Model Enumeration Commit");
    public static final SimpleEvent MODEL_ENUMERATION_ROLLBACK = new SimpleEvent("Model Enumeration Rollback");
    public static final SimpleEvent SUBSUMPTION_STARTING_UB_TREE_GENERATION =
            new SimpleEvent("Starting UB Tree generation");
    public static final SimpleEvent SUBSUMPTION_ADDED_NEW_SET = new SimpleEvent("Adding a new set to the UB Tree");
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
