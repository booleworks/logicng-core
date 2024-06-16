// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.modelcounting;

import static com.booleworks.logicng.handlers.Handler.aborted;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.graphs.algorithms.ConnectedComponentsComputation;
import com.booleworks.logicng.graphs.datastructures.Graph;
import com.booleworks.logicng.graphs.datastructures.Node;
import com.booleworks.logicng.graphs.generators.ConstraintGraphGenerator;
import com.booleworks.logicng.handlers.DnnfCompilationHandler;
import com.booleworks.logicng.knowledgecompilation.dnnf.DnnfFactory;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.Dnnf;
import com.booleworks.logicng.knowledgecompilation.dnnf.functions.DnnfModelCountFunction;
import com.booleworks.logicng.transformations.PureExpansionTransformation;
import com.booleworks.logicng.transformations.cnf.CNFConfig;
import com.booleworks.logicng.transformations.cnf.CNFEncoder;
import com.booleworks.logicng.util.FormulaHelper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A model counter for large formulas.
 * @version 3.0.0
 * @since 2.0.0
 */
public final class ModelCounter {

    /**
     * Private empty constructor. Class only contains static utility methods.
     */
    private ModelCounter() {
        // Intentionally left empty
    }

    /**
     * Computes the model count for a given set of formulas (interpreted as conjunction)
     * and a set of relevant variables.  This set can only be a superset of the original
     * formulas' variables.  No projected model counting is supported.
     * @param f         the formula factory to generate new formulas
     * @param formulas  the list of formulas
     * @param variables the relevant variables
     * @return the model count of the formulas for the variables
     */
    public static BigInteger count(final FormulaFactory f, final Collection<Formula> formulas, final SortedSet<Variable> variables) {
        return count(f, formulas, variables, () -> null);
    }

    /**
     * Computes the model count for a given set of formulas (interpreted as conjunction)
     * and a set of relevant variables.  This set can only be a superset of the original
     * formulas' variables.  No projected model counting is supported.
     * @param f                   the formula factory to generate new formulas
     * @param formulas            the list of formulas
     * @param variables           the relevant variables
     * @param dnnfHandlerSupplier a supplier for a DNNF handler, it may return a new handler on each call or always the same.
     *                            Since multiple DNNFs may be computed, the supplier may be called several times. It must not
     *                            be {@code null}, but it may return {@code null} (i.e. return no handler).
     * @return the model count of the formulas for the variables or {@code null} if the DNNF handler aborted the DNNF computation
     */
    public static BigInteger count(final FormulaFactory f, final Collection<Formula> formulas, final SortedSet<Variable> variables,
                                   final Supplier<DnnfCompilationHandler> dnnfHandlerSupplier) {
        if (!variables.containsAll(FormulaHelper.variables(f, formulas))) {
            throw new IllegalArgumentException("Expected variables to contain all of the formulas' variables.");
        }
        if (variables.isEmpty()) {
            final List<Formula> remainingConstants =
                    formulas.stream().filter(formula -> formula.type() != FType.TRUE).collect(Collectors.toList());
            return remainingConstants.isEmpty() ? BigInteger.ONE : BigInteger.ZERO;
        }
        final List<Formula> cnfs = encodeAsCnf(f, formulas);
        final SimplificationResult simplification = simplify(f, cnfs);
        final BigInteger count = count(f, simplification.simplifiedFormulas, dnnfHandlerSupplier);
        if (count == null) {
            return null;
        }
        final SortedSet<Variable> dontCareVariables = simplification.getDontCareVariables(variables);
        return count.multiply(BigInteger.valueOf(2).pow(dontCareVariables.size()));
    }

    private static List<Formula> encodeAsCnf(final FormulaFactory f, final Collection<Formula> formulas) {
        final PureExpansionTransformation expander = new PureExpansionTransformation(f);
        final List<Formula> expandedFormulas =
                formulas.stream().map(formula -> formula.transform(expander)).collect(Collectors.toList());

        final CNFConfig cnfConfig = CNFConfig.builder()
                .algorithm(CNFConfig.Algorithm.ADVANCED)
                .fallbackAlgorithmForAdvancedEncoding(CNFConfig.Algorithm.TSEITIN).build();

        return expandedFormulas.stream().map(it -> CNFEncoder.encode(f, it, cnfConfig)).collect(Collectors.toList());
    }

    private static SimplificationResult simplify(final FormulaFactory f, final Collection<Formula> formulas) {
        final Assignment simpleBackbone = new Assignment();
        final SortedSet<Variable> backboneVariables = new TreeSet<>();
        for (final Formula formula : formulas) {
            if (formula.type() == FType.LITERAL) {
                final Literal lit = (Literal) formula;
                simpleBackbone.addLiteral(lit);
                backboneVariables.add(lit.variable());
            }
        }
        final List<Formula> simplified = new ArrayList<>();
        for (final Formula formula : formulas) {
            final Formula restrict = formula.restrict(f, simpleBackbone);
            if (restrict.type() != FType.TRUE) {
                simplified.add(restrict);
            }
        }
        return new SimplificationResult(f, backboneVariables, simplified);
    }

    private static BigInteger count(final FormulaFactory f, final Collection<Formula> formulas,
                                    final Supplier<DnnfCompilationHandler> dnnfHandlerSupplier) {
        final Graph<Variable> constraintGraph = ConstraintGraphGenerator.generateFromFormulas(f, formulas);
        final Set<Set<Node<Variable>>> ccs = ConnectedComponentsComputation.compute(constraintGraph);
        final List<List<Formula>> components = ConnectedComponentsComputation.splitFormulasByComponent(f, formulas, ccs);
        final DnnfFactory factory = new DnnfFactory();
        BigInteger count = BigInteger.ONE;
        for (final List<Formula> component : components) {
            final DnnfCompilationHandler handler = dnnfHandlerSupplier.get();
            final Dnnf dnnf = factory.compile(f, f.and(component), handler);
            if (dnnf == null || aborted(handler)) {
                return null;
            }
            count = count.multiply(dnnf.execute(new DnnfModelCountFunction(f)));
        }
        return count;
    }

    private static class SimplificationResult {
        private final List<Formula> simplifiedFormulas;
        private final SortedSet<Variable> backboneVariables;
        private final FormulaFactory f;

        public SimplificationResult(final FormulaFactory f, final SortedSet<Variable> backboneVariables,
                                    final List<Formula> simplifiedFormulas) {
            this.simplifiedFormulas = simplifiedFormulas;
            this.backboneVariables = backboneVariables;
            this.f = f;
        }

        public SortedSet<Variable> getDontCareVariables(final SortedSet<Variable> variables) {
            final SortedSet<Variable> dontCareVariables = new TreeSet<>(variables);
            dontCareVariables.removeAll(FormulaHelper.variables(f, simplifiedFormulas));
            dontCareVariables.removeAll(backboneVariables);
            return dontCareVariables;
        }
    }
}
