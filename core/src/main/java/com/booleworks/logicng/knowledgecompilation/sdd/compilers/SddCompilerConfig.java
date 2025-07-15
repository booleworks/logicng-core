package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.DecisionVTreeGenerator;

import java.util.Set;

public class SddCompilerConfig {
    private final boolean inputSimplification;
    private final DecisionVTreeGenerator.PrioritizationStrategy prioritizationStrategy;
    private final Sdd sdd;
    private final Compiler compiler;
    private final Set<Variable> variables;

    private SddCompilerConfig(final boolean inputSimplification,
                              final DecisionVTreeGenerator.PrioritizationStrategy prioritizationStrategy,
                              final Sdd sdd, final Compiler compiler,
                              final Set<Variable> variables) {
        this.inputSimplification = inputSimplification;
        this.prioritizationStrategy = prioritizationStrategy;
        this.sdd = sdd;
        this.compiler = compiler;
        this.variables = variables;
    }

    public boolean isInputSimplification() {
        return inputSimplification;
    }

    public DecisionVTreeGenerator.PrioritizationStrategy getPrioritizationStrategy() {
        return prioritizationStrategy;
    }

    public Compiler getCompiler() {
        return compiler;
    }

    public Set<Variable> getVariables() {
        return variables;
    }

    public Sdd getSdd() {
        return sdd;
    }

    public enum Compiler {
        TOP_DOWN,
        BOTTOM_UP,
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Compiler compiler = Compiler.TOP_DOWN;
        private Set<Variable> variables = null;
        private Sdd sdd = null;
        private boolean inputSimplification = true;
        private DecisionVTreeGenerator.PrioritizationStrategy prioritizationStrategy =
                DecisionVTreeGenerator.PrioritizationStrategy.VAR_DOWN;

        public Builder inputSimplification(final boolean inputSimplification) {
            this.inputSimplification = inputSimplification;
            return this;
        }

        public Builder prioritizationStrategy(
                final DecisionVTreeGenerator.PrioritizationStrategy prioritizationStrategy) {
            this.prioritizationStrategy = prioritizationStrategy;
            return this;
        }

        public Builder sdd(final Sdd sdd) {
            this.sdd = sdd;
            return this;
        }

        public Builder compiler(final Compiler compiler) {
            this.compiler = compiler;
            return this;
        }

        public Builder variables(final Set<Variable> variables) {
            this.variables = variables;
            return this;
        }

        public SddCompilerConfig build() {
            if (sdd != null && compiler == Compiler.TOP_DOWN) {
                throw new IllegalArgumentException("Top-Down SDD compiler does not allow user-defined vtrees or sdd");
            }
            return new SddCompilerConfig(inputSimplification, prioritizationStrategy, sdd, compiler, variables);
        }
    }

}
