// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;

import java.util.Set;

/**
 * A configuration for {@link SddCompiler}.
 * @version 3.0.0
 * @since 3.0.0
 */
public class SddCompilerConfig {
    private final boolean preprocessing;
    private final Sdd sdd;
    private final Compiler compiler;
    private final Set<Variable> variables;

    private SddCompilerConfig(final boolean preprocessing,
                              final Sdd sdd, final Compiler compiler,
                              final Set<Variable> variables) {
        this.preprocessing = preprocessing;
        this.sdd = sdd;
        this.compiler = compiler;
        this.variables = variables;
    }

    /**
     * Returns whether preprocessing of the input is enabled.
     * @return whether preprocessing of the input is enabled
     */
    public boolean hasPreprocessing() {
        return preprocessing;
    }

    /**
     * Returns the used compiler implementation.
     * @return the used compiler implementation
     */
    public Compiler getCompiler() {
        return compiler;
    }

    /**
     * Returns the relevant variables for the compilation. Is {@code null} if
     * the variables of the input should be used.
     * @return the relevant variables
     */
    public Set<Variable> getVariables() {
        return variables;
    }

    /**
     * Returns the used SDD container. Is {@code null} if a new SDD container should be
     * generated.
     * @return the used SDD container.
     */
    public Sdd getSdd() {
        return sdd;
    }

    /**
     * Selection of available SDD compilers.
     */
    public enum Compiler {
        TOP_DOWN,
        BOTTOM_UP,
    }

    /**
     * Returns a new builder for a configuration.
     * @return a new builder for a configuration
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for {@link SddCompilerConfig}.
     */
    public static class Builder {
        private Compiler compiler = Compiler.TOP_DOWN;
        private Set<Variable> variables = null;
        private Sdd sdd = null;
        private boolean preprocessing = true;

        /**
         * Set whether the formula should be simplified before they are
         * compiled. On by default.
         * @param preprocessing whether preprocessing is enabled
         * @return this builder
         */
        public Builder preprocessing(final boolean preprocessing) {
            this.preprocessing = preprocessing;
            return this;
        }

        /**
         * Defines am SDD container that should be used for the compilation,
         * otherwise, a new SDD container and a vtree are constructed.
         * <p>
         * Expects that an appropriate vtree is defined in the SDD container.
         * Not allowed if compiler is {@link Compiler#TOP_DOWN}.
         * @param sdd the SDD container
         * @return this builder
         */
        public Builder sdd(final Sdd sdd) {
            this.sdd = sdd;
            return this;
        }

        /**
         * Defines the compiler implementation used for the compilation.
         * Default: {@link Compiler#TOP_DOWN}
         * @param compiler the compiler implementation
         * @return this builder
         */
        public Builder compiler(final Compiler compiler) {
            this.compiler = compiler;
            return this;
        }

        /**
         * Defines the relevant variables for the compilation. If {@code null}
         * or undefined, the compiler will use the variables of the formula.
         * <p>
         * Needs to be a superset of the formula's variable for
         * {@link Compiler#BOTTOM_UP}.
         * @param variables the relevant variables
         * @return this builder
         */
        public Builder variables(final Set<Variable> variables) {
            this.variables = variables;
            return this;
        }

        /**
         * Build the configuration.
         * @return the configuration
         */
        public SddCompilerConfig build() {
            if (sdd != null && compiler == Compiler.TOP_DOWN) {
                throw new IllegalArgumentException("Top-Down SDD compiler does not allow user-defined vtrees or sdd");
            }
            return new SddCompilerConfig(preprocessing, sdd, compiler, variables);
        }
    }
}
