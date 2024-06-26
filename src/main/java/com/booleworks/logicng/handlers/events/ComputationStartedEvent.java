package com.booleworks.logicng.handlers.events;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.TimeoutHandler;

/**
 * A class for {@link LogicNGEvent}s which indicate the start of a computation.
 * By convention, these events are instantiated as {@code static final} fields
 * s.t. they can be checked in {@link ComputationHandler handlers} by using
 * referential equality. On the other hand, some handlers (like the
 * {@link TimeoutHandler}) may just check for <i>any</i>
 * {@link ComputationStartedEvent} via {@code instanceof}.
 * <p>
 * The {@link #computationType} should only be used for debugging purposes.
 * @version 3.0.0
 * @since 3.0.0
 */
public class ComputationStartedEvent implements LogicNGEvent {

    public static final ComputationStartedEvent FACTORIZATION_STARTED = new ComputationStartedEvent("Factorization");
    public static final ComputationStartedEvent BDD_COMPUTATION_STARTED = new ComputationStartedEvent("BDD Computation");
    public static final ComputationStartedEvent DNNF_COMPUTATION_STARTED = new ComputationStartedEvent("DNNF Computation");
    public static final ComputationStartedEvent SAT_CALL_STARTED = new ComputationStartedEvent("SAT Call");
    public static final ComputationStartedEvent MAX_SAT_CALL_STARTED = new ComputationStartedEvent("MaxSAT Call");
    public static final ComputationStartedEvent BACKBONE_COMPUTATION_STARTED = new ComputationStartedEvent("Backbone Computation");
    public static final ComputationStartedEvent ADVANCED_SIMPLIFICATION_STARTED = new ComputationStartedEvent("Advanced Simplification");
    public static final ComputationStartedEvent PRIME_COMPUTATION_STARTED = new ComputationStartedEvent("Prime Computation");
    public static final ComputationStartedEvent IMPLICATE_REDUCTION_STARTED = new ComputationStartedEvent("Implicate Reduction");
    public static final ComputationStartedEvent MUS_COMPUTATION_STARTED = new ComputationStartedEvent("MUS Computation");
    public static final ComputationStartedEvent SMUS_COMPUTATION_STARTED = new ComputationStartedEvent("SMUS Computation");
    public static final ComputationStartedEvent OPTIMIZATION_FUNCTION_STARTED = new ComputationStartedEvent("Optimization Function");
    public static final ComputationStartedEvent MODEL_ENUMERATION_STARTED = new ComputationStartedEvent("Model Enumeration");

    private final String computationType;

    /**
     * Creates a new event for the given computation type.
     * @param computationType the computation type
     */
    public ComputationStartedEvent(final String computationType) {
        this.computationType = computationType;
    }

    /**
     * Returns the computation type.
     * @return the computation type
     */
    public String getComputationType() {
        return computationType;
    }

    @Override
    public String toString() {
        return "Event: Started " + computationType;
    }
}
