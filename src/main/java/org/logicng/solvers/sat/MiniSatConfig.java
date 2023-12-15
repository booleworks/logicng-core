// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.solvers.sat;

import org.logicng.configurations.Configuration;
import org.logicng.configurations.ConfigurationType;
import org.logicng.formulas.Formula;
import org.logicng.solvers.SATSolver;

/**
 * The configuration object for a MiniSAT-style SAT solver.
 * @version 2.0.0
 * @since 1.0
 */
public final class MiniSatConfig extends Configuration {

    /**
     * The different methods for clause minimization.
     * <ul>
     * <li> {@code NONE} - no minimization is performed
     * <li> {@code BASIC} - local minimization is performed
     * <li> {@code DEEP} - recursive minimization is performed
     * </ul>
     */
    public enum ClauseMinimization {
        NONE, BASIC, DEEP
    }

    /**
     * The different methods for generating a CNF for a formula to put on the solver.
     * <ul>
     * <li> {@code FACTORY_CNF} calls the {@link Formula#cnf(org.logicng.formulas.FormulaFactory)}
     * method on the formula to convert it to CNF.  Therefore the CNF including all its auxiliary
     * variables will be added to the formula factory.
     * <li> {@code PG_ON_SOLVER} uses a solver-internal implementation of Plaisted-Greenbaum.
     * Auxiliary variables are only added on the solver, not on the factory.  This usually
     * leads to a reduced heap usage and often faster performance.
     * Before applying Plaisted-Greenbaum, this method performs an NNF transformation on the
     * input formula first.
     * <li> {@code FULL_PG_ON_SOLVER} uses a solver-internal implementation of Plaisted-Greenbaum.
     * Auxiliary variables are only added on the solver, not on the factory.  This usually
     * leads to a reduced heap usage and often faster performance.
     * In contrast to {@code PG_ON_SOLVER}, this method does not transform the input formula to
     * NNF first. The Plaisted-Greenbaum transformation is applied directly to all operators of
     * the formula, hence prefix {@code FULL}. Without the NNF transformation the formula factory
     * and the heap will not be polluted with intermediate formulas.
     * </ul>
     */
    public enum CNFMethod {
        FACTORY_CNF, PG_ON_SOLVER, FULL_PG_ON_SOLVER
    }

    // Main configuration parameters
    final boolean incremental;
    final boolean proofGeneration;
    final boolean useAtMostClauses;
    final CNFMethod cnfMethod;
    final boolean useBinaryWatchers;
    final boolean useLbdFeatures;

    final boolean initialPhase;
    final boolean auxiliaryVariablesInModels;
    final boolean bbInitialUBCheckForRotatableLiterals;
    final boolean bbCheckForComplementModelLiterals;
    final boolean bbCheckForRotatableLiterals;

    final ClauseMinimization clauseMin;
    final boolean removeSatisfied;

    final SATSolverLowLevelConfig lowLevelConfig;

    /**
     * Constructs a new MiniSAT configuration from a given builder.
     * @param builder the builder
     */
    private MiniSatConfig(final Builder builder) {
        super(ConfigurationType.MINISAT);
        incremental = builder.incremental;
        proofGeneration = builder.proofGeneration;
        useAtMostClauses = builder.useAtMostClauses;
        cnfMethod = builder.cnfMethod;
        useBinaryWatchers = builder.useBinaryWatchers;
        useLbdFeatures = builder.useLbdFeatures;
        initialPhase = builder.initialPhase;
        auxiliaryVariablesInModels = builder.auxiliaryVariablesInModels;
        bbInitialUBCheckForRotatableLiterals = builder.bbInitialUBCheckForRotatableLiterals;
        bbCheckForComplementModelLiterals = builder.bbCheckForComplementModelLiterals;
        bbCheckForRotatableLiterals = builder.bbCheckForRotatableLiterals;
        clauseMin = builder.clauseMin;
        removeSatisfied = builder.removeSatisfied;
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
     * Returns whether the solver is incremental or not.
     * @return {@code true} if the solver is incremental, {@code false} otherwise
     */
    public boolean incremental() {
        return incremental;
    }

    /**
     * Returns the initial phase of the solver.
     * @return the initial phase of the solver
     */
    public boolean initialPhase() {
        return initialPhase;
    }

    /**
     * Returns whether proof generation should be performed or not.
     * @return whether proof generation should be performed or not
     */
    public boolean proofGeneration() {
        return proofGeneration;
    }

    /**
     * Returns whether at most clauses should be used to encode cardinality constraints or not.
     * @return whether at most clauses should be used to encode cardinality constraints or not
     */
    public boolean useAtMostClauses() {
        return useAtMostClauses;
    }

    /**
     * Returns the CNF method which should be used.
     * @return the CNF method
     */
    public CNFMethod getCnfMethod() {
        return cnfMethod;
    }

    /**
     * Returns whether auxiliary Variables should be included in the model or not.
     * @return whether auxiliary Variables should be included in the model or not
     */
    public boolean isAuxiliaryVariablesInModels() {
        return auxiliaryVariablesInModels;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MiniSatConfig{").append(System.lineSeparator());
        sb.append("incremental=").append(incremental).append(System.lineSeparator());
        sb.append("proofGeneration=").append(proofGeneration).append(System.lineSeparator());
        sb.append("useAtMostClauses=").append(useAtMostClauses).append(System.lineSeparator());
        sb.append("cnfMethod=").append(cnfMethod).append(System.lineSeparator());
        sb.append("useBinaryWatchers=").append(useBinaryWatchers).append(System.lineSeparator());
        sb.append("useLbdFeatures=").append(useLbdFeatures).append(System.lineSeparator());
        sb.append("initialPhase=").append(initialPhase).append(System.lineSeparator());
        sb.append("auxiliaryVariablesInModels=").append(auxiliaryVariablesInModels).append(System.lineSeparator());
        sb.append("bbInitialUBCheckForRotatableLiterals=").append(bbInitialUBCheckForRotatableLiterals).append(System.lineSeparator());
        sb.append("bbCheckForComplementModelLiterals=").append(bbCheckForComplementModelLiterals).append(System.lineSeparator());
        sb.append("bbCheckForRotatableLiterals=").append(bbCheckForRotatableLiterals).append(System.lineSeparator());
        sb.append("clauseMin=").append(clauseMin).append(System.lineSeparator());
        sb.append("removeSatisfied=").append(removeSatisfied).append(System.lineSeparator());
        sb.append("}");
        return sb.toString();
    }

    /**
     * The builder for a MiniSAT configuration.
     * @version 2.0.0
     * @since 1.0
     */
    public static class Builder {
        private boolean incremental = true;
        private boolean proofGeneration = false;
        private boolean useAtMostClauses = true;
        private CNFMethod cnfMethod = CNFMethod.PG_ON_SOLVER;
        private boolean useBinaryWatchers = false;
        private boolean useLbdFeatures = false;
        private boolean initialPhase = false;
        private boolean auxiliaryVariablesInModels = false;
        private boolean bbInitialUBCheckForRotatableLiterals = true;
        private boolean bbCheckForComplementModelLiterals = true;
        private boolean bbCheckForRotatableLiterals = true;
        private ClauseMinimization clauseMin = ClauseMinimization.DEEP;
        private boolean removeSatisfied = true;
        private SATSolverLowLevelConfig lowLevelConfig = SATSolverLowLevelConfig.builder().build();

        private Builder() {
            // Initialize only via factory
        }

        /**
         * Turns the incremental mode of the solver off and on.  The default value is {@code true}.
         * @param incremental {@code true} if incremental mode is turned on, {@code false} otherwise
         * @return the builder
         */
        public Builder incremental(final boolean incremental) {
            this.incremental = incremental;
            return this;
        }

        /**
         * Sets whether the information for generating a proof with DRUP should be recorded or not.  The default
         * value is {@code false}.
         * @param proofGeneration {@code true} if proof generating information should be recorded, {@code false} otherwise
         * @return the builder
         */
        public Builder proofGeneration(final boolean proofGeneration) {
            this.proofGeneration = proofGeneration;
            return this;
        }

        /**
         * Sets whether the solver should use special at most clauses for cardinality constraints. At most clauses are not
         * compatible with {@link #proofGeneration proof generation}, so this option will be ignored if proof generation
         * is enabled. The default value is {@code true}.
         * @param useAtMostClauses {@code true} if at most clauses should be used, {@code false} otherwise
         * @return the builder
         */
        public Builder useAtMostClauses(final boolean useAtMostClauses) {
            this.useAtMostClauses = useAtMostClauses;
            return this;
        }

        /**
         * Sets the CNF method for converting formula which are not in CNF for the solver.  The default value
         * is {@code FACTORY_CNF}.
         * @param cnfMethod the CNF method
         * @return the builder
         */
        public Builder cnfMethod(final CNFMethod cnfMethod) {
            this.cnfMethod = cnfMethod;
            return this;
        }

        /**
         * Sets whether binary watchers should be used.
         * @param useBinaryWatchers whether binary watchers should be used or not
         * @return the builder
         */
        public Builder useBinaryWatchers(final boolean useBinaryWatchers) {
            this.useBinaryWatchers = useBinaryWatchers;
            return this;
        }

        /**
         * Sets whether LBD (Literal Block Distance) features should be used.
         * @param useLbdFeatures whether LBD features should be used or not
         * @return the builder
         */
        public Builder useLbdFeatures(final boolean useLbdFeatures) {
            this.useLbdFeatures = useLbdFeatures;
            return this;
        }

        /**
         * Sets the initial phase of the solver.  The default value is {@code true}.
         * @param initialPhase the initial phase
         * @return the builder
         */
        public Builder initialPhase(final boolean initialPhase) {
            this.initialPhase = initialPhase;
            return this;
        }

        /**
         * Sets whether auxiliary variables (CNF, cardinality constraints, pseudo-Boolean constraints) should
         * be included in methods like {@link SATSolver#model()} or {@link SATSolver#enumerateAllModels()}.  If
         * set to {@code true}, all variables will be included in these methods,  if set to {@code false}, variables
         * starting with "@RESERVED_CC_", "@RESERVED_PB_", and "@RESERVED_CNF_" will be excluded from the models.
         * The default value is {@code false}.
         * @param auxiliaryVariablesInModels {@code true} if auxiliary variables should be included in the models,
         *                                   {@code false} otherwise
         * @return the builder
         */
        public Builder auxiliaryVariablesInModels(final boolean auxiliaryVariablesInModels) {
            this.auxiliaryVariablesInModels = auxiliaryVariablesInModels;
            return this;
        }

        /**
         * Sets whether the backbone algorithm should check for rotatable literals.
         * The default value is {@code true}.
         * @param checkForRotatableLiterals the boolean value that is {@code true} if the algorithm should check for
         *                                  rotatables or {@code false} otherwise.
         * @return the builder
         */
        public Builder bbCheckForRotatableLiterals(final boolean checkForRotatableLiterals) {
            bbCheckForRotatableLiterals = checkForRotatableLiterals;
            return this;
        }

        /**
         * Sets whether the backbone algorithm should check for rotatable literals during initial unit propagation.
         * The default value is {@code true}.
         * @param initialUBCheckForRotatableLiterals the boolean value that is {@code true} if the algorithm should
         *                                           check for rotatables or {@code false} otherwise.
         * @return the builder
         */
        public Builder bbInitialUBCheckForRotatableLiterals(final boolean initialUBCheckForRotatableLiterals) {
            bbInitialUBCheckForRotatableLiterals = initialUBCheckForRotatableLiterals;
            return this;
        }

        /**
         * Sets whether the backbone algorithm should check for complement model literals.
         * The default value is {@code true}.
         * @param checkForComplementModelLiterals the boolean value that is {@code true} if the algorithm should check for
         *                                        complement literals or {@code false} otherwise.
         * @return the builder
         */
        public Builder bbCheckForComplementModelLiterals(final boolean checkForComplementModelLiterals) {
            bbCheckForComplementModelLiterals = checkForComplementModelLiterals;
            return this;
        }

        /**
         * Sets the clause minimization method. The default value is {@code DEEP}.
         * @param ccmin the value
         * @return the builder
         */
        public Builder clMinimization(final ClauseMinimization ccmin) {
            clauseMin = ccmin;
            return this;
        }

        /**
         * If turned on, the satisfied original clauses will be removed when simplifying on level 0, when turned off,
         * only the satisfied learnt clauses will be removed.  The default value is {@code true}.
         * @param removeSatisfied {@code true} if the original clauses should be simplified, {@code false} otherwise
         * @return the builder
         */
        public Builder removeSatisfied(final boolean removeSatisfied) {
            this.removeSatisfied = removeSatisfied;
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
         * Builds the MiniSAT configuration.
         * @return the configuration
         */
        public MiniSatConfig build() {
            return new MiniSatConfig(this);
        }
    }
}
