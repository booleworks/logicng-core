// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

public final class SATSolverLowLevelConfig {
    // MiniSat-related configuration
    final double varDecay;
    final double varInc;
    final int restartFirst;
    final double restartInc;
    final double clauseDecay;

    // Glucose-related configuration
    final int lbLBDMinimizingClause;
    final int lbLBDFrozenClause;
    final int lbSizeMinimizingClause;
    final int firstReduceDB;
    final int specialIncReduceDB;
    final int incReduceDB;
    final double factorK;
    final double factorR;
    final int sizeLBDQueue;
    final int sizeTrailQueue;
    final boolean reduceOnSize;
    final int reduceOnSizeSize;
    final double maxVarDecay;

    private SATSolverLowLevelConfig(final Builder builder) {
        varDecay = builder.varDecay;
        varInc = builder.varInc;
        restartFirst = builder.restartFirst;
        restartInc = builder.restartInc;
        clauseDecay = builder.clauseDecay;
        lbLBDMinimizingClause = builder.lbLBDMinimizingClause;
        lbLBDFrozenClause = builder.lbLBDFrozenClause;
        lbSizeMinimizingClause = builder.lbSizeMinimizingClause;
        firstReduceDB = builder.firstReduceDB;
        specialIncReduceDB = builder.specialIncReduceDB;
        incReduceDB = builder.incReduceDB;
        factorK = builder.factorK;
        factorR = builder.factorR;
        sizeLBDQueue = builder.sizeLBDQueue;
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

    public int getLbLBDMinimizingClause() {
        return lbLBDMinimizingClause;
    }

    public int getLbLBDFrozenClause() {
        return lbLBDFrozenClause;
    }

    public int getLbSizeMinimizingClause() {
        return lbSizeMinimizingClause;
    }

    public int getFirstReduceDB() {
        return firstReduceDB;
    }

    public int getSpecialIncReduceDB() {
        return specialIncReduceDB;
    }

    public int getIncReduceDB() {
        return incReduceDB;
    }

    public double getFactorK() {
        return factorK;
    }

    public double getFactorR() {
        return factorR;
    }

    public int getSizeLBDQueue() {
        return sizeLBDQueue;
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
        final StringBuilder sb = new StringBuilder("SATSolverLowLevelConfig{").append(System.lineSeparator());
        sb.append("varDecay=").append(varDecay).append(System.lineSeparator());
        sb.append("varInc=").append(varInc).append(System.lineSeparator());
        sb.append("restartFirst=").append(restartFirst).append(System.lineSeparator());
        sb.append("restartInc=").append(restartInc).append(System.lineSeparator());
        sb.append("clauseDecay=").append(clauseDecay).append(System.lineSeparator());
        sb.append("lbLBDMinimizingClause=").append(lbLBDMinimizingClause).append(System.lineSeparator());
        sb.append("lbLBDFrozenClause=").append(lbLBDFrozenClause).append(System.lineSeparator());
        sb.append("lbSizeMinimizingClause=").append(lbSizeMinimizingClause).append(System.lineSeparator());
        sb.append("firstReduceDB=").append(firstReduceDB).append(System.lineSeparator());
        sb.append("specialIncReduceDB=").append(specialIncReduceDB).append(System.lineSeparator());
        sb.append("incReduceDB=").append(incReduceDB).append(System.lineSeparator());
        sb.append("factorK=").append(factorK).append(System.lineSeparator());
        sb.append("factorR=").append(factorR).append(System.lineSeparator());
        sb.append("sizeLBDQueue=").append(sizeLBDQueue).append(System.lineSeparator());
        sb.append("sizeTrailQueue=").append(sizeTrailQueue).append(System.lineSeparator());
        sb.append("reduceOnSize=").append(reduceOnSize).append(System.lineSeparator());
        sb.append("reduceOnSizeSize=").append(reduceOnSizeSize).append(System.lineSeparator());
        sb.append("maxVarDecay=").append(maxVarDecay).append(System.lineSeparator());
        sb.append("}");
        return sb.toString();
    }

    public static class Builder {
        private double varDecay = 0.95;
        private double varInc = 1.0;
        private int restartFirst = 100;
        private double restartInc = 2.0;
        private double clauseDecay = 0.999;
        private int lbLBDMinimizingClause = 6;
        private int lbLBDFrozenClause = 30;
        private int lbSizeMinimizingClause = 30;
        private int firstReduceDB = 2000;
        private int specialIncReduceDB = 1000;
        private int incReduceDB = 300;
        private double factorK = 0.8;
        private double factorR = 1.4;
        private int sizeLBDQueue = 50;
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
         * @param lbLBDMinimizingClause the value (should be at least 3)
         * @return the builder
         */
        public Builder lbLBDMinimizingClause(final int lbLBDMinimizingClause) {
            this.lbLBDMinimizingClause = lbLBDMinimizingClause;
            return this;
        }

        /**
         * Sets the value to protect clauses if their LBD decrease and is lower
         * than it (for one turn). The default value is 30.
         * @param lbLBDFrozenClause the value
         * @return the builder
         */
        public Builder lbLBDFrozenClause(final int lbLBDFrozenClause) {
            this.lbLBDFrozenClause = lbLBDFrozenClause;
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
         * @param firstReduceDB the value
         * @return the builder
         */
        public Builder firstReduceDB(final int firstReduceDB) {
            this.firstReduceDB = firstReduceDB;
            return this;
        }

        /**
         * Sets the special increment for the DB reduction to a given value. The
         * default value is 1000.
         * @param specialIncReduceDB the value
         * @return the builder
         */
        public Builder specialIncReduceDB(final int specialIncReduceDB) {
            this.specialIncReduceDB = specialIncReduceDB;
            return this;
        }

        /**
         * Sets the increment for the DB reduction to a given value. The default
         * value is 300.
         * @param incReduceDB the value
         * @return the builder
         */
        public Builder incReduceDB(final int incReduceDB) {
            this.incReduceDB = incReduceDB;
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
         * @param sizeLBDQueue the value (should be at least 10)
         * @return the builder
         */
        public Builder sizeLBDQueue(final int sizeLBDQueue) {
            this.sizeLBDQueue = sizeLBDQueue;
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
        public SATSolverLowLevelConfig build() {
            return new SATSolverLowLevelConfig(this);
        }
    }
}
