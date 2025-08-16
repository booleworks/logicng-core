package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.handlers.events.SddMinimizationEvent;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddMinimization;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddMinimizationStrategy;

/**
 * A class providing common features for SDD minimization.
 * <p>
 * The class composes the settings as two computation handlers
 * {@code operationHandler} and {@code iterationHandler} that are used to build
 * a {@link com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddMinimization.SearchHandler SearchHandler}.
 * {@code operationHandler} is used for soft aborts during the minimization and
 * {@code iterationHandler} for hard aborts.
 * @version 3.0.0
 * @since 3.0.0
 */
public class SddMinimizationConfig {
    private final Sdd sdd;
    private SddMinimizationStrategy.Strategies strategy = SddMinimizationStrategy.Strategies.DEC_THRESHOLD;

    private long totalTimeout = -1;
    private long operationTimeout = -1;
    private long nodeLimit = -1;
    private ComputationHandler userHandler = NopHandler.get();

    /**
     * Construct a new configuration with the default values.
     * @param sdd the SDD container
     */
    public SddMinimizationConfig(final Sdd sdd) {
        this.sdd = sdd;
    }

    /**
     * Copy constructor.
     * <p>
     * Does <em>no</em> deep copy of the computation handler!
     * @param other the other configuration
     */
    public SddMinimizationConfig(final SddMinimizationConfig other) {
        this.sdd = other.sdd;
        this.strategy = other.strategy;
        this.operationTimeout = other.operationTimeout;
        this.totalTimeout = other.totalTimeout;
        this.nodeLimit = other.nodeLimit;
        this.userHandler = other.userHandler;
    }

    /**
     * Returns the operation handler for this configuration.
     * <p>
     * This handler is used for soft aborts.
     * @return the operation handler for this configuration
     */
    public OperationHandler getOperationHandler() {
        return new OperationHandler();
    }

    /**
     * Returns the iteration handler for this configuration.
     * <p>
     * This handler is used for hard aborts.
     * @return the iteration handler for this configuration
     */
    public IterationHandler getIterationHandler() {
        return new IterationHandler();
    }

    /**
     * Returns the search handler for this configuration.
     * @return the search handler for this configuration
     */
    public SddMinimization.SearchHandler getSearchHandler() {
        return new SddMinimization.SearchHandler(getOperationHandler(), getIterationHandler());
    }

    /**
     * This handler is responsible for soft aborts.
     * <p>
     * It checks whether the operation timeout or the node limit is exceeded if
     * they are defined.
     */
    public class OperationHandler implements ComputationHandler {
        long deadline = -1;

        private OperationHandler() {
        }

        @Override
        public boolean shouldResume(final LngEvent event) {
            if (event instanceof SddGlobalTransformationEvent) {
                if (((SddGlobalTransformationEvent) event).isStart()) {
                    if (operationTimeout != -1) {
                        deadline = System.currentTimeMillis() + operationTimeout;
                    }
                } else {
                    deadline = -1;
                }
            }
            if (nodeLimit != -1 && outsideNodeLimit()) {
                return false;
            }
            return deadline == -1 || System.currentTimeMillis() < deadline;
        }
    }

    /**
     * This handler is responsible for hard aborts.
     * <p>
     * It checks the user handler and whether the timeout is exceeded if they
     * are defined.
     */
    public class IterationHandler implements ComputationHandler {
        long deadline = -1;

        @Override
        public boolean shouldResume(final LngEvent event) {
            if (event == ComputationStartedEvent.SDD_MINIMIZATION) {
                if (totalTimeout != -1) {
                    deadline = System.currentTimeMillis() + totalTimeout;
                }
            }
            if (deadline != -1 && deadline <= System.currentTimeMillis()) {
                return false;
            }
            if (event instanceof SddMinimizationEvent) {
                final long newSize = ((SddMinimizationEvent) event).getNewSize();
                if (nodeLimit != -1 && outsideNodeLimit()) {
                    return false;
                }
            }
            return userHandler.shouldResume(event);
        }
    }

    private boolean outsideNodeLimit() {
        return sdd.getSddNodeCount() >= nodeLimit;
    }

    /**
     * Returns the SDD container.
     * @return the SDD container
     */
    public Sdd getSdd() {
        return sdd;
    }

    /**
     * Returns the minimization strategy.
     * @return the minimization strategy
     */
    public SddMinimizationStrategy.Strategies getStrategy() {
        return strategy;
    }

    /**
     * Returns the timeout for single operations.
     * @return the timeout for single operations.
     */
    public long getOperationTimeout() {
        return operationTimeout;
    }

    /**
     * Returns the total timeout.
     * @return the total timeout
     */
    public long getTotalTimeout() {
        return totalTimeout;
    }

    /**
     * Returns the maximal number of nodes that should be in the SDD container.
     * @return the maximal number of nodes that should be in the SDD container
     */
    public long getNodeLimit() {
        return nodeLimit;
    }

    /**
     * Returns the user handler.
     * @return the user handler
     */
    public ComputationHandler getUserHandler() {
        return userHandler;
    }

    /**
     * Set the minimization strategy for this configuration
     * @param strategy the minimization strategy
     * @return this configuration
     */
    public SddMinimizationConfig strategy(final SddMinimizationStrategy.Strategies strategy) {
        this.strategy = strategy;
        return this;
    }

    /**
     * Set the timout for single operations (soft abort).
     * @param operationTimeout the operation timeout
     * @return this configuration
     */
    public SddMinimizationConfig operationTimeout(final long operationTimeout) {
        this.operationTimeout = operationTimeout;
        return this;
    }

    /**
     * Set the timout for the whole minimization (hard abort)
     * @param totalTimeout the timeout
     * @return this configuration
     */
    public SddMinimizationConfig totalTimeout(final long totalTimeout) {
        this.totalTimeout = totalTimeout;
        return this;
    }

    /**
     * Set the maximal number of nodes that should be in the SDD container.
     * @param nodeLimit the maximal number of nodes
     * @return this configuration
     */
    public SddMinimizationConfig nodeLimit(final long nodeLimit) {
        this.nodeLimit = nodeLimit;
        return this;
    }

    /**
     * Set a user-provided computation handler (hard abort).
     * @param userHandler the handler
     * @return this configuration
     */
    public SddMinimizationConfig userHandler(final ComputationHandler userHandler) {
        this.userHandler = userHandler;
        return this;
    }
}
