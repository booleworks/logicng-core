// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.collections.LngVector;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.dtree.DTree;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.VTreeUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeLeaf;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtreegeneration.DecisionVTreeGenerator;
import com.booleworks.logicng.predicates.satisfiability.SatPredicate;
import com.booleworks.logicng.transformations.cnf.CnfSubsumption;
import com.booleworks.logicng.transformations.simplification.BackboneSimplifier;
import com.booleworks.logicng.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * High-level SDD compiler.
 * @version 3.0.0
 * @since 3.0.0
 */
public class SddCompiler {
    /**
     * Compile an SDD from a formula in CNF using the default configuration.
     * @param f   the factory
     * @param cnf the formula in CNF
     * @return the compiled SDD and the used SDD container
     */
    public static SddCompilationResult compile(final FormulaFactory f, final Formula cnf) {
        return compile(f, cnf, SddCompilerConfig.builder().build());
    }

    /**
     * Compile an SDD from a formula in CNF using the passed configuration.
     * @param f   the factory
     * @param cnf the formula in CNF
     * @return the compiled SDD and the used SDD container
     */
    public static SddCompilationResult compile(final FormulaFactory f, final Formula cnf,
                                               final SddCompilerConfig config) {
        return compile(f, cnf, config, NopHandler.get()).getResult();
    }

    /**
     * Compile an SDD from a formula in CNF using the passed configuration.
     * @param f       the factory
     * @param cnf     the formula in CNF
     * @param config  the configuration
     * @param handler the computation handler
     * @return the compiled SDD and the used SDD container, or the canceling
     * cause if the computation was aborted by the handler.
     */
    public static LngResult<SddCompilationResult> compile(final FormulaFactory f, final Formula cnf,
                                                          final SddCompilerConfig config,
                                                          final ComputationHandler handler) {
        if (!cnf.isCnf(f)) {
            throw new IllegalArgumentException("Expected formula in CNF");
        }

        final Set<Variable> variables;
        if (config.getVariables() == null) {
            variables = cnf.variables(f);
        } else {
            variables = config.getVariables();
        }

        final Formula simplified;
        if (config.hasPreprocessing()) {
            final LngResult<Formula> simplificationResult = simplifyFormula(f, cnf, handler);
            if (!simplificationResult.isSuccess()) {
                return LngResult.canceled(simplificationResult.getCancelCause());
            }
            simplified = simplificationResult.getResult();
        } else {
            simplified = cnf;
        }
        final SddCoreSolver solver;
        final Sdd sdd;
        final VTree vtree;
        final DTree dtree;
        if (config.getSdd() == null) {
            solver = new SddCoreSolver(f, cnf.variables(f).size());
            solver.add(simplified);
            final VTreeRoot.Builder vtreeBuilder = VTreeRoot.builderFromSolver(solver);

            if (simplified.getType() == FType.TRUE) {
                sdd = new Sdd(f, vtreeBuilder.build(null));
                return LngResult.of(new SddCompilationResult(sdd, sdd.verum()));
            }
            if (!simplified.holds(new SatPredicate(f))) {
                sdd = new Sdd(f, vtreeBuilder.build(null));
                return LngResult.of(new SddCompilationResult(sdd, sdd.falsum()));
            }

            final LngResult<Pair<DTree, VTree>> vTreeResult =
                    DecisionVTreeGenerator.generateDecisionVTree(simplified, solver, vtreeBuilder, handler);
            if (!vTreeResult.isSuccess()) {
                return LngResult.canceled(vTreeResult.getCancelCause());
            }
            vtree = vTreeResult.getResult().getSecond();
            dtree = vTreeResult.getResult().getFirst();
            sdd = new Sdd(f, vtreeBuilder.build(vtree));
            solver.init(leavesInSolverOrder(vtree));
        } else {
            sdd = config.getSdd();
            if (simplified.getType() == FType.TRUE) {
                return LngResult.of(new SddCompilationResult(sdd, sdd.verum()));
            }
            if (!simplified.holds(new SatPredicate(f))) {
                return LngResult.of(new SddCompilationResult(sdd, sdd.falsum()));
            }

            final List<Integer> varsInFormula =
                    SddUtil.varsToIndicesExpectKnown(config.getSdd(), simplified.variables(f), new ArrayList<>());
            final Set<Integer> varsInVTree =
                    VTreeUtil.vars(config.getSdd().getVTree().getRoot(), new TreeSet<>());
            if (!varsInVTree.containsAll(varsInFormula)) {
                throw new IllegalArgumentException("VTree misses variables that are contained in the formula");
            }

            vtree = sdd.getVTree().getRoot();
            solver = null;
            dtree = null;
        }

        if (!sdd.hasVTree()) {
            throw new IllegalArgumentException("Cannot use the vtree of the sdd since none is defined");
        }

        if (config.getCompiler() == SddCompilerConfig.Compiler.BOTTOM_UP) {
            final LngResult<SddNode> node = SddCompilerBottomUp.compile(sdd, simplified, handler);
            return node.map(n -> new SddCompilationResult(sdd, n));
        } else {
            assert vtree != null;
            final LngResult<SddNode> node = SddCompilerTopDown.compile(sdd, solver, variables, dtree, handler);
            return node.map(n -> new SddCompilationResult(sdd, n));
        }
    }

    private static LngVector<VTreeLeaf> leavesInSolverOrder(final VTree vTree) {
        final TreeSet<VTreeLeaf> leavesSet = new TreeSet<>(Comparator.comparingInt(VTreeLeaf::getVariable));
        VTreeUtil.leavesInOrder(vTree, leavesSet);
        final LngVector<VTreeLeaf> leaves = new LngVector<>();
        for (final VTreeLeaf leaf : leavesSet) {
            assert leaf.getVariable() == leaves.size();
            leaves.push(leaf);
        }
        return leaves;
    }

    protected static LngResult<Formula> simplifyFormula(final FormulaFactory f, final Formula formula,
                                                        final ComputationHandler handler) {
        final LngResult<Formula> backboneSimplified = formula.transform(new BackboneSimplifier(f), handler);
        if (!backboneSimplified.isSuccess()) {
            return LngResult.canceled(backboneSimplified.getCancelCause());
        }
        return backboneSimplified.getResult().transform(new CnfSubsumption(f), handler);
    }
}
