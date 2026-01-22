// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import java.util.Objects;

public final class SatSolverLowLevelConfig {
    // MiniSat-related configuration
    final double varDecay;
    final double varInc;
    final int restartFirst;
    final double restartInc;
    final double clauseDecay;

    // Glucose-related configuration
    final int lbLbdMinimizingClause;
    final int lbLbdFrozenClause;
    final int lbSizeMinimizingClause;
    final int firstReduceDb;
    final int specialIncReduceDb;
    final int incReduceDb;
    final double factorK;
    final double factorR;
    final int sizeLbdQueue;
    final int sizeTrailQueue;
    final boolean reduceOnSize;
    final int reduceOnSizeSize;
    final double maxVarDecay;

    private SatSolverLowLevelConfig(final Builder builder) {
        varDecay = builder.varDecay;
        varInc = builder.varInc;
        restartFirst = builder.restartFirst;
        restartInc = builder.restartInc;
        clauseDecay = builder.clauseDecay;
        lbLbdMinimizingClause = builder.lbLbdMinimizingClause;
        lbLbdFrozenClause = builder.lbLbdFrozenClause;
        lbSizeMinimizingClause = builder.lbSizeMinimizingClause;
        firstReduceDb = builder.firstReduceDb;
        specialIncReduceDb = builder.specialIncReduceDb;
        incReduceDb = builder.incReduceDb;
        factorK = builder.factorK;
        factorR = builder.factorR;
        sizeLbdQueue = builder.sizeLbdQueue;
        sizeTrailQueue = builder.sizeTrailQueue;
        reduceOnSize = builder.reduceOnSize;
        reduceOnSizeSize = builder.reduceOnSizeSize;
        maxVarDecay = builder.maxVarDecay;
    }

    public static Builder builder() {
        return new Builder();
    }

    public double getVarDecay() {
        return varDecay;
    }

    public double getVarInc() {
        return varInc;
    }

    public int getRestartFirst() {
        return restartFirst;
    }

    public double getRestartInc() {
        return restartInc;
    }

    public double getClauseDecay() {
        return clauseDecay;
    }

    public int getLbLbdMinimizingClause() {
        return lbLbdMinimizingClause;
    }

    public int getLbLbdFrozenClause() {
        return lbLbdFrozenClause;
    }

    public int getLbSizeMinimizingClause() {
        return lbSizeMinimizingClause;
    }

    public int getFirstReduceDb() {
        return firstReduceDb;
    }

    public int getSpecialIncReduceDb() {
        return specialIncReduceDb;
    }

    public int getIncReduceDb() {
        return incReduceDb;
    }

    public double getFactorK() {
        return factorK;
    }

    public double getFactorR() {
        return factorR;
    }

    public int getSizeLbdQueue() {
        return sizeLbdQueue;
    }

    public int getSizeTrailQueue() {
        return sizeTrailQueue;
    }

    public boolean isReduceOnSize() {
        return reduceOnSize;
    }

    public int getReduceOnSizeSize() {
        return reduceOnSizeSize;
    }

    public double getMaxVarDecay() {
        return maxVarDecay;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SatSolverLowLevelConfig{").append(System.lineSeparator());
        sb.append("varDecay=").append(varDecay).append(System.lineSeparator());
        sb.append("varInc=").append(varInc).append(System.lineSeparator());
        sb.append("restartFirst=").append(restartFirst).append(System.lineSeparator());
        sb.append("restartInc=").append(restartInc).append(System.lineSeparator());
        sb.append("clauseDecay=").append(clauseDecay).append(System.lineSeparator());
        sb.append("lbLbdMinimizingClause=").append(lbLbdMinimizingClause).append(System.lineSeparator());
        sb.append("lbLbdFrozenClause=").append(lbLbdFrozenClause).append(System.lineSeparator());
        sb.append("lbSizeMinimizingClause=").append(lbSizeMinimizingClause).append(System.lineSeparator());
        sb.append("firstReduceDb=").append(firstReduceDb).append(System.lineSeparator());
        sb.append("specialIncReduceDb=").append(specialIncReduceDb).append(System.lineSeparator());
        sb.append("incReduceDb=").append(incReduceDb).append(System.lineSeparator());
        sb.append("factorK=").append(factorK).append(System.lineSeparator());
        sb.append("factorR=").append(factorR).append(System.lineSeparator());
        sb.append("sizeLbdQueue=").append(sizeLbdQueue).append(System.lineSeparator());
        sb.append("sizeTrailQueue=").append(sizeTrailQueue).append(System.lineSeparator());
        sb.append("reduceOnSize=").append(reduceOnSize).append(System.lineSeparator());
        sb.append("reduceOnSizeSize=").append(reduceOnSizeSize).append(System.lineSeparator());
        sb.append("maxVarDecay=").append(maxVarDecay).append(System.lineSeparator());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final SatSolverLowLevelConfig that = (SatSolverLowLevelConfig) object;
        return Double.compare(varDecay, that.varDecay) == 0 &&
                Double.compare(varInc, that.varInc) == 0 &&
                restartFirst == that.restartFirst &&
                Double.compare(restartInc, that.restartInc) == 0 &&
                Double.compare(clauseDecay, that.clauseDecay) == 0 &&
                lbLbdMinimizingClause == that.lbLbdMinimizingClause &&
                lbLbdFrozenClause == that.lbLbdFrozenClause &&
                lbSizeMinimizingClause == that.lbSizeMinimizingClause &&
                firstReduceDb == that.firstReduceDb &&
                specialIncReduceDb == that.specialIncReduceDb &&
                incReduceDb == that.incReduceDb &&
                Double.compare(factorK, that.factorK) == 0
                && Double.compare(factorR, that.factorR) == 0 &&
                sizeLbdQueue == that.sizeLbdQueue &&
                sizeTrailQueue == that.sizeTrailQueue &&
                reduceOnSize == that.reduceOnSize &&
                reduceOnSizeSize == that.reduceOnSizeSize &&
                Double.compare(maxVarDecay, that.maxVarDecay) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(varDecay, varInc, restartFirst, restartInc, clauseDecay, lbLbdMinimizingClause,
                lbLbdFrozenClause, lbSizeMinimizingClause, firstReduceDb, specialIncReduceDb, incReduceDb,
                factorK, factorR, sizeLbdQueue, sizeTrailQueue, reduceOnSize, reduceOnSizeSize, maxVarDecay);
    }

    public static final class Builder {
        private double varDecay = 0.95;
        private double varInc = 1.0;
        private int restartFirst = 100;
        private double restartInc = 2.0;
        private double clauseDecay = 0.999;
        private int lbLbdMinimizingClause = 6;
        private int lbLbdFrozenClause = 30;
        private int lbSizeMinimizingClause = 30;
        private int firstReduceDb = 2000;
        private int specialIncReduceDb = 1000;
        private int incReduceDb = 300;
        private double factorK = 0.8;
        private double factorR = 1.4;
        private int sizeLbdQueue = 50;
        private int sizeTrailQueue = 5000;
        private boolean reduceOnSize = false;
        private int reduceOnSizeSize = 12;
        private double maxVarDecay = 0.95;

        private Builder() {
            // Initialize only via factory
        }

        /**
         * Sets the variable activity decay factor to a given value. The default
         * value is 0.95.
         * @param varDecay the value (should be in the range 0..1)
         * @return the builder
         */
        public Builder varDecay(final double varDecay) {
            this.varDecay = varDecay;
            return this;
        }

        /**
         * Sets the initial value to bump a variable with each time it is used
         * in conflict resolution to a given value. The default value is 1.0.
         * @param varInc the value
         * @return the builder
         */
        public Builder varInc(final double varInc) {
            this.varInc = varInc;
            return this;
        }

        /**
         * Sets the base restart interval to the given value. The default value
         * is 100.
         * @param restartFirst the value (should be at least 1)
         * @return the builder
         */
        public Builder restartFirst(final int restartFirst) {
            this.restartFirst = restartFirst;
            return this;
        }

        /**
         * Sets the restart interval increase factor to the given value. The
         * default value is 2.0.
         * @param restartInc the value (should be at least 1)
         * @return the builder
         */
        public Builder restartInc(final double restartInc) {
            this.restartInc = restartInc;
            return this;
        }

        /**
         * Sets the clause activity decay factor to a given value. The default
         * value is 0.999.
         * @param clauseDecay the value (should be in the range 0..1)
         * @return the builder
         */
        public Builder clauseDecay(final double clauseDecay) {
            this.clauseDecay = clauseDecay;
            return this;
        }

        /**
         * Sets the minimal LBD required to minimize a clause to a given value.
         * The default value is 6.
         * @param lbLbdMinimizingClause the value (should be at least 3)
         * @return the builder
         */
        public Builder lbLbdMinimizingClause(final int lbLbdMinimizingClause) {
            this.lbLbdMinimizingClause = lbLbdMinimizingClause;
            return this;
        }

        /**
         * Sets the value to protect clauses if their LBD decrease and is lower
         * than it (for one turn). The default value is 30.
         * @param lbLbdFrozenClause the value
         * @return the builder
         */
        public Builder lbLbdFrozenClause(final int lbLbdFrozenClause) {
            this.lbLbdFrozenClause = lbLbdFrozenClause;
            return this;
        }

        /**
         * Sets the minimal size required to minimize a clause to a given value.
         * The default value is 30.
         * @param lbSizeMinimizingClause the value (should be at least 3)
         * @return the builder
         */
        public Builder lbSizeMinimizingClause(final int lbSizeMinimizingClause) {
            this.lbSizeMinimizingClause = lbSizeMinimizingClause;
            return this;
        }

        /**
         * Sets the number of conflicts before the first DB reduction to a given
         * value. The default value is 2000.
         * @param firstReduceDb the value
         * @return the builder
         */
        public Builder firstReduceDb(final int firstReduceDb) {
            this.firstReduceDb = firstReduceDb;
            return this;
        }

        /**
         * Sets the special increment for the DB reduction to a given value. The
         * default value is 1000.
         * @param specialIncReduceDb the value
         * @return the builder
         */
        public Builder specialIncReduceDb(final int specialIncReduceDb) {
            this.specialIncReduceDb = specialIncReduceDb;
            return this;
        }

        /**
         * Sets the increment for the DB reduction to a given value. The default
         * value is 300.
         * @param incReduceDb the value
         * @return the builder
         */
        public Builder incReduceDb(final int incReduceDb) {
            this.incReduceDb = incReduceDb;
            return this;
        }

        /**
         * Sets the constant used to force restart to a given value. The default
         * value is 0.8.
         * @param factorK the value (should be in the range 0..1)
         * @return the builder
         */
        public Builder factorK(final double factorK) {
            this.factorK = factorK;
            return this;
        }

        /**
         * Sets the constant used to block restart to a given value. The default
         * value is 1.4.
         * @param factorR the value (should be in the range 1..5)
         * @return the builder
         */
        public Builder factorR(final double factorR) {
            this.factorR = factorR;
            return this;
        }

        /**
         * Sets the size of moving average for LBD (restarts) to a given value.
         * The default value is 50.
         * @param sizeLbdQueue the value (should be at least 10)
         * @return the builder
         */
        public Builder sizeLbdQueue(final int sizeLbdQueue) {
            this.sizeLbdQueue = sizeLbdQueue;
            return this;
        }

        /**
         * Sets the size of moving average for trail (block restarts) to a given
         * value. The default value is 5000.
         * @param sizeTrailQueue the value (should be at least 10)
         * @return the builder
         */
        public Builder sizeTrailQueue(final int sizeTrailQueue) {
            this.sizeTrailQueue = sizeTrailQueue;
            return this;
        }

        /**
         * Turns on the size reduction during LBD computation like described in
         * the XMinisat paper. The default value is {@code false}.
         * @param reduceOnSize {@code true} if the size reduction is turned on,
         *                     {@code false} otherwise
         * @return the builder
         */
        public Builder reduceOnSize(final boolean reduceOnSize) {
            this.reduceOnSize = reduceOnSize;
            return this;
        }

        /**
         * Sets the constant used during size reduction like described in the
         * XMinisat paper to a given value. The default value is 12.
         * @param reduceOnSizeSize the value
         * @return the builder
         */
        public Builder reduceOnSizeSize(final int reduceOnSizeSize) {
            this.reduceOnSizeSize = reduceOnSizeSize;
            return this;
        }

        /**
         * Sets the maximal variable activity decay factor to a given value. The
         * default value is 0.95.
         * @param maxVarDecay the value (should be in the range 0..1)
         * @return the builder
         */
        public Builder maxVarDecay(final double maxVarDecay) {
            this.maxVarDecay = maxVarDecay;
            return this;
        }

        /**
         * Builds the SAT solver configuration.
         * @return the configuration
         */
        public SatSolverLowLevelConfig build() {
            return new SatSolverLowLevelConfig(this);
        }
    }
}
