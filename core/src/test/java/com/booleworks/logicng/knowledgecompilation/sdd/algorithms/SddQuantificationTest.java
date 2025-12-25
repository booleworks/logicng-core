// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompilerConfig;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtreegeneration.BalancedVTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddModelCountFunction;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddModelEnumerationFunction;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.ModelCountingFunction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SddQuantificationTest {
    private final static List<String> FILES = List.of(
            "../test_files/sdd/compile_example1.cnf"
    );
    private final static List<List<Integer>> QUANTIFY_VARS = List.of(
            List.of(26, 11, 2, 13, 10, 19, 25, 24, 9, 4, 2, 12, 15)
    );

    @Test
    public void testMultipleQuantificationSimple() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final VTreeRoot.Builder builder = VTreeRoot.builder();
        final Formula formula = f.parse("(A | ~C) & (B | C | D) & (B | D) & (X | C)");
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(builder);
        final Sdd sdd = new Sdd(f, builder.build(vtree));
        final SddCompilerConfig config = SddCompilerConfig.builder()
                .compiler(SddCompilerConfig.Compiler.BOTTOM_UP)
                .sdd(sdd)
                .build();
        final SddNode node = SddCompiler.compile(f, formula, config).getNode();
        final int bIdx = sdd.variableToIndex(f.variable("B"));
        final int cIdx = sdd.variableToIndex(f.variable("C"));
        final SddNode quantified =
                SddQuantification.exists(sdd, Set.of(bIdx, cIdx), node, NopHandler.get()).getResult();
        checkProjectedModels(f.variables("B", "C"), quantified, formula, sdd);
    }

    @Test
    public void testFilesQuantifyMultiple() throws IOException {
        int fileIndex = 0;
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult result = SddCompiler.compile(f, formula);
            final Sdd sdd = result.getSdd();
            SddNode node = result.getNode();
            final List<Variable> vars = new ArrayList<>(formula.variables(f));
            final Set<Variable> quantifyVars =
                    QUANTIFY_VARS.get(fileIndex).stream().map(vars::get).collect(Collectors.toSet());
            final Set<Integer> quantifyVarIdxs =
                    quantifyVars.stream().map(sdd::variableToIndex).collect(Collectors.toSet());
            final List<Variable> remainingVars =
                    vars.stream().filter(v -> !quantifyVars.contains(v)).collect(Collectors.toList());
            node = SddQuantification.exists(sdd, quantifyVarIdxs, node, NopHandler.get()).getResult();
            checkPMC(remainingVars, node, formula, sdd);
            fileIndex++;
        }
    }

    private static void checkPMC(final Collection<Variable> remainingVars, final SddNode node,
                                 final Formula originalFormula, final Sdd sdd) {
        final BigInteger actual = node.execute(new SddModelCountFunction(remainingVars, sdd));
        final SatSolver solver = SatSolver.newSolver(sdd.getFactory());
        solver.add(originalFormula);
        final BigInteger expected = solver.execute(ModelCountingFunction.builder(remainingVars).build());
        assertThat(expected).isEqualTo(actual);
    }

    private static void checkProjectedModels(final Collection<Variable> quantified, final SddNode node,
                                             final Formula originalFormula, final Sdd sdd) {
        final Set<Variable> variables = new TreeSet<>(originalFormula.variables(sdd.getFactory()));
        variables.removeAll(quantified);
        final SatSolver solver = SatSolver.newSolver(sdd.getFactory());
        solver.add(originalFormula);
        final List<Model> expectedModels = solver.enumerateAllModels(variables);
        final List<Model> actualModels = node.execute(SddModelEnumerationFunction.builder(sdd, variables).build());
        final Set<Assignment> expected = expectedModels.stream().map(Model::toAssignment).collect(Collectors.toSet());
        final Set<Assignment> actual = actualModels.stream().map(Model::toAssignment).collect(Collectors.toSet());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }
}
