// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import com.booleworks.logicng.configurations.Configuration;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;

import java.util.Objects;

/**
 * The configuration object for a SAT solver.
 * @version 2.0.0
 * @since 1.0
 */
public final class SATSolverConfig extends Configuration {

    final boolean proofGeneration;
    final boolean useAtMostClauses;
    final CNFMethod cnfMethod;
    final ClauseMinimization clauseMinimization;
    final boolean initialPhase;

    final SATSolverLowLevelConfig lowLevelConfig;

    /**
     * Constructs a new SAT solver configuration from a given builder.
     * @param builder the builder
     */
    private SATSolverConfig(final Builder builder) {
        super(ConfigurationType.SAT);
        proofGeneration = builder.proofGeneration;
        useAtMostClauses = !builder.proofGeneration && builder.useAtMostClauses;
        cnfMethod = builder.cnfMethod;
        clauseMinimization = builder.clauseMinimization;
        initialPhase = builder.initialPhase;
        lowLevelConfig = builder.lowLevelConfig;
    }

    /**
     * Returns a new builder for the configuration.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a new builder which is initialized with the parameters of the
     * given configuration.
     * @param config the configuration to copy
     * @return the builder
     */
    public static Builder copy(final SATSolverConfig config) {
        return new Builder()
                .proofGeneration(config.proofGeneration)
                .useAtMostClauses(config.useAtMostClauses)
                .cnfMethod(config.cnfMethod)
                .clauseMinimization(config.clauseMinimization)
                .initialPhase(config.initialPhase)
                .lowLevelConfig(config.lowLevelConfig);
    }

    /**
     * Returns whether proof generation should be performed or not.
     * @return whether proof generation should be performed or not
     */
    public boolean proofGeneration() {
        return proofGeneration;
    }

    /**
     * Returns whether at most clauses should be used to encode cardinality
     * constraints or not.
     * @return whether at most clauses should be used to encode cardinality
     * constraints or not
     */
    public boolean useAtMostClauses() {
        return useAtMostClauses;
    }

    /**
     * Returns the CNF method which should be used.
     * @return the CNF method
     */
    public CNFMethod cnfMethod() {
        return cnfMethod;
    }

    /**
     * Returns the kind of clause minimization to be applied.
     * @return the kind of clause minimization to be applied
     */
    public ClauseMinimization clauseMinimization() {
        return clauseMinimization;
    }

    /**
     * Returns the initial phase of the solver.
     * @return the initial phase of the solver
     */
    public boolean initialPhase() {
        return initialPhase;
    }

    /**
     * Returns the low level configuration of the solver.
     * @return the low level configuration of the solver
     */
    public SATSolverLowLevelConfig lowLevelConfig() {
        return lowLevelConfig;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final SATSolverConfig that = (SATSolverConfig) object;
        return proofGeneration == that.proofGeneration &&
                useAtMostClauses == that.useAtMostClauses &&
                initialPhase == that.initialPhase &&
                cnfMethod == that.cnfMethod &&
                clauseMinimization == that.clauseMinimization &&
                Objects.equals(lowLevelConfig, that.lowLevelConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proofGeneration, useAtMostClauses, cnfMethod, clauseMinimization, initialPhase, lowLevelConfig);
    }

    @Override
    public String toString() {
        return "SATSolverConfig{" +
                "proofGeneration=" + proofGeneration +
                ", useAtMostClauses=" + useAtMostClauses +
                ", cnfMethod=" + cnfMethod +
                ", clauseMinimization=" + clauseMinimization +
                ", initialPhase=" + initialPhase +
                ", lowLevelConfig=" + lowLevelConfig +
                '}';
    }

    /**
     * The different methods for clause minimization.
     * <ul>
     * <li>{@code NONE} - no minimization is performed
     * <li>{@code BASIC} - local minimization is performed
     * <li>{@code DEEP} - recursive minimization is performed
     * </ul>
     */
    public enum ClauseMinimization {
        NONE,
        BASIC,
        DEEP
    }

    /**
     * The different methods for generating a CNF for a formula to put on the
     * solver.
     * <ul>
     * <li>{@code FACTORY_CNF} calls the {@link Formula#cnf(FormulaFactory)}
     * method on the formula to convert it to CNF. Therefore the CNF including
     * all its auxiliary variables will be added to the formula factory.
     * <li>{@code PG_ON_SOLVER} uses a solver-internal implementation of
     * Plaisted-Greenbaum. Auxiliary variables are only added on the solver, not
     * on the factory. This usually leads to a reduced heap usage and often
     * faster performance. Before applying Plaisted-Greenbaum, this method
     * performs an NNF transformation on the input formula first.
     * <li>{@code FULL_PG_ON_SOLVER} uses a solver-internal implementation of
     * Plaisted-Greenbaum. Auxiliary variables are only added on the solver, not
     * on the factory. This usually leads to a reduced heap usage and often
     * faster performance. In contrast to {@code PG_ON_SOLVER}, this method does
     * not transform the input formula to NNF first. The Plaisted-Greenbaum
     * transformation is applied directly to all operators of the formula, hence
     * prefix {@code FULL}. Without the NNF transformation the formula factory
     * and the heap will not be polluted with intermediate formulas.
     * </ul>
     */
    public enum CNFMethod {
        FACTORY_CNF,
        PG_ON_SOLVER,
        FULL_PG_ON_SOLVER
    }

    /**
     * The builder for a SAT solver configuration.
     * @version 2.0.0
     * @since 1.0
     */
    public static class Builder {
        private boolean proofGeneration = false;
        private boolean useAtMostClauses = false;
        private CNFMethod cnfMethod = CNFMethod.PG_ON_SOLVER;
        private boolean initialPhase = false;
        private ClauseMinimization clauseMinimization = ClauseMinimization.DEEP;
        private SATSolverLowLevelConfig lowLevelConfig = SATSolverLowLevelConfig.builder().build();

        private Builder() {
            // Initialize only via factory
        }

        /**
         * Sets whether the information for generating a proof with DRUP should
         * be recorded or not. The default value is {@code false}. Activating
         * proof generation will always disable {@link #useAtMostClauses() at
         * most clauses}.
         * @param proofGeneration {@code true} if proof generating information
         *                        should be recorded, {@code false} otherwise
         * @return the builder
         */
        public Builder proofGeneration(final boolean proofGeneration) {
            this.proofGeneration = proofGeneration;
            return this;
        }

        /**
         * Sets whether the solver should use special at most clauses for
         * cardinality constraints. At most clauses are not compatible with
         * {@link #proofGeneration proof generation}, so this option will be
         * ignored if proof generation is enabled. The default value is
         * {@code false}.
         * @param useAtMostClauses {@code true} if at most clauses should be
         *                         used, {@code false} otherwise
         * @return the builder
         */
        public Builder useAtMostClauses(final boolean useAtMostClauses) {
            this.useAtMostClauses = useAtMostClauses;
            return this;
        }

        /**
         * Sets the CNF method for converting formula which are not in CNF for
         * the solver. The default value is {@code FACTORY_CNF}.
         * @param cnfMethod the CNF method
         * @return the builder
         */
        public Builder cnfMethod(final CNFMethod cnfMethod) {
            this.cnfMethod = cnfMethod;
            return this;
        }

        /**
         * Sets the clause minimization method. The default value is
         * {@code DEEP}.
         * @param clauseMinimization the value
         * @return the builder
         */
        public Builder clauseMinimization(final ClauseMinimization clauseMinimization) {
            this.clauseMinimization = clauseMinimization;
            return this;
        }

        /**
         * Sets the initial phase of the solver. The default value is
         * {@code false}.
         * @param initialPhase the initial phase
         * @return the builder
         */
        public Builder initialPhase(final boolean initialPhase) {
            this.initialPhase = initialPhase;
            return this;
        }

        /**
         * Sets the low level configuration.
         * @param lowLevelConfig the low level configuration
         * @return the builder
         */
        public Builder lowLevelConfig(final SATSolverLowLevelConfig lowLevelConfig) {
            this.lowLevelConfig = lowLevelConfig;
            return this;
        }

        /**
         * Builds the SAT solver configuration.
         * @return the configuration
         */
        public SATSolverConfig build() {
            return new SATSolverConfig(this);
        }
    }
}
