package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.handlers.events.SddMinimizationStepEvent;

public class SddMinimizationConfig {
    private final Sdd sdd;
    private final long operationTimeout;

    private final long totalTimeout;
    private final int absoluteTargetSize;
    private final double relativeTargetSize;
    private final double relativeImprovementThreshold;
    private final int absoluteImprovementThreshold;
    private final ComputationHandler userHandler;

    private SddMinimizationConfig(final Sdd sdd, final long operationTimeout, final long totalTimeout,
                                  final int absoluteTargetSize,
                                  final double relativeTargetSize, final double relativeImprovementThreshold,
                                  final int absoluteImprovementThreshold, final ComputationHandler userHandler) {
        this.sdd = sdd;
        this.operationTimeout = operationTimeout;
        this.totalTimeout = totalTimeout;
        this.absoluteTargetSize = absoluteTargetSize;
        this.relativeTargetSize = relativeTargetSize;
        this.relativeImprovementThreshold = relativeImprovementThreshold;
        this.absoluteImprovementThreshold = absoluteImprovementThreshold;
        this.userHandler = userHandler;
    }

    public OperationHandler operationHandler() {
        return new OperationHandler();
    }

    public IterationHandler iterationHandler() {
        return new IterationHandler();
    }

    public class OperationHandler implements ComputationHandler {
        long deadline = -1;

        public OperationHandler() {
        }

        @Override
        public boolean shouldResume(final LngEvent event) {
            if (event instanceof SddGlobalTransformationEvent) {
                if (((SddGlobalTransformationEvent) event).isStart()) {
                    if (operationTimeout != -1) {
                        deadline = System.currentTimeMillis() + operationTimeout;
                    }
                }
            }
            return deadline == -1 || System.currentTimeMillis() < deadline;
        }
    }

    public class IterationHandler implements ComputationHandler {
        long deadline = -1;
        long expectedSize = -1;
        long targetSize = -1;

        @Override
        public boolean shouldResume(final LngEvent event) {
            if (event == ComputationStartedEvent.SDD_MINIMIZATION) {
                if (totalTimeout != -1) {
                    deadline = System.currentTimeMillis() + totalTimeout;
                }
                if (absoluteTargetSize != -1) {
                    targetSize = absoluteTargetSize;
                }
                final int sddSize = sdd.getActiveSize();
                if (relativeTargetSize != -1) {
                    final int size = (int) (sddSize * relativeTargetSize);
                    targetSize = targetSize == -1 ? size : Math.max(targetSize, size);
                }
                calculateNextSize(sddSize);
            }
            if (deadline != -1 && deadline <= System.currentTimeMillis()) {
                return false;
            }
            if (event instanceof SddMinimizationStepEvent) {
                final int newSize = ((SddMinimizationStepEvent) event).getNewSize();
                if (expectedSize != -1 && newSize >= expectedSize) {
                    return false;
                }
                if (targetSize != -1 && newSize <= targetSize) {
                    return false;
                }
                calculateNextSize(newSize);
            }
            return userHandler.shouldResume(event);
        }

        private void calculateNextSize(final int currentSize) {
            if (absoluteImprovementThreshold != -1) {
                expectedSize = currentSize - absoluteImprovementThreshold;
            }
            if (relativeImprovementThreshold != -1) {
                final int relSize = (int) (currentSize * (1 - relativeImprovementThreshold));
                expectedSize = expectedSize == -1 ? relSize : Math.min(relSize, expectedSize);
            }
        }
    }


    public static SddMinimizationConfig unlimited(final Sdd sdd) {
        return new Builder(sdd).build();
    }

    public static class Builder {
        private final Sdd sdd;
        private long operationTimeout = -1;

        private long totalTimeout = -1;
        private int absoluteTargetSize = -1;
        private double relativeTargetSize = -1;
        private double relativeImprovementThreshold = -1;
        private int absoluteImprovementThreshold = -1;
        private ComputationHandler userHandler = NopHandler.get();

        public Builder(final Sdd sdd) {
            this.sdd = sdd;
        }

        public SddMinimizationConfig build() {
            return new SddMinimizationConfig(sdd, operationTimeout, totalTimeout, absoluteTargetSize,
                    relativeTargetSize, relativeImprovementThreshold, absoluteImprovementThreshold, userHandler);
        }

        public Builder withOperationTimeout(final long operationTimeout) {
            this.operationTimeout = operationTimeout;
            return this;
        }

        public Builder withTotalTimeout(final long totalTimeout) {
            this.totalTimeout = totalTimeout;
            return this;
        }

        public Builder withAbsoluteTargetSize(final int absoluteTargetSize) {
            this.absoluteTargetSize = absoluteTargetSize;
            return this;
        }

        public Builder withRelativeTargetSize(final double relativeTargetSize) {
            this.relativeTargetSize = relativeTargetSize;
            return this;
        }

        public Builder withRelativeImprovementThreshold(final double relativeImprovementThreshold) {
            this.relativeImprovementThreshold = relativeImprovementThreshold;
            return this;
        }

        public Builder withAbsoluteImprovementThreshold(final int absoluteImprovementThreshold) {
            this.absoluteImprovementThreshold = absoluteImprovementThreshold;
            return this;
        }

        public Builder withUserHandler(final ComputationHandler userHandler) {
            this.userHandler = userHandler;
            return this;
        }
    }
}
