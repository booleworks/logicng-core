// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NumberOfModelsHandler;
import com.booleworks.logicng.handlers.events.ComputationStartedEvent;
import com.booleworks.logicng.handlers.events.EnumerationFoundModelsEvent;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SddModelEnumerationFunctionTest {
    @Test
    public void testTrivial() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SddCompilationResult compRes = SddCompiler.compile(f, f.verum());
        final Sdd sdd = compRes.getSdd();
        final SddModelEnumerationFunction meFunc1 =
                SddModelEnumerationFunction.builder(sdd, f.variables()).build();
        final SddModelEnumerationFunction meFunc2 =
                SddModelEnumerationFunction.builder(sdd, f.variables("A", "B")).build();
        check(sdd.verum(), f.verum(), meFunc1, sdd);
        check(sdd.verum(), f.verum(), meFunc2, sdd);
        check(sdd.falsum(), f.falsum(), meFunc1, sdd);
        check(sdd.falsum(), f.falsum(), meFunc2, sdd);
    }

    @Test
    public void test() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(A & B) | (B & C) | (C & D)"));
        final SddCompilationResult res = SddCompiler.compile(f, formula);
        final Sdd sdd = res.getSdd();
        final SddModelEnumerationFunction meFunc =
                SddModelEnumerationFunction.builder(sdd, f.variables("A", "B", "C", "D", "E")).build();
        check(res.getNode(), formula, meFunc, sdd);
    }

    @Test
    public void testSubtree() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(A & B) | (B & C) | (C & D)"));
        final SddCompilationResult res = SddCompiler.compile(f, formula);
        final Sdd sdd = res.getSdd();
        final SddNode descendant = res.getNode().asDecomposition().getElementsUnsafe().get(0).getSub();
        final Formula subformula = descendant.execute(new SddExportFormula(sdd));
        final SddModelEnumerationFunction meFunc =
                SddModelEnumerationFunction.builder(sdd, subformula.variables(f)).build();
        check(descendant, subformula, meFunc, sdd);
    }

    @Test
    public void testEvents() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(A & B) | (B & C) | (C & D)"));
        final SddCompilationResult res = SddCompiler.compile(f, formula);
        final Sdd sdd = res.getSdd();
        final SddModelEnumerationFunction meFunc =
                SddModelEnumerationFunction.builder(sdd, f.variables("A", "B", "C", "D", "E")).build();

        final AtomicInteger startedCalls = new AtomicInteger(0);
        final AtomicInteger foundModels = new AtomicInteger(0);
        final AtomicInteger otherCalls = new AtomicInteger(0);
        final ComputationHandler handler = event -> {
            if (event == ComputationStartedEvent.MODEL_ENUMERATION_STARTED) {
                startedCalls.incrementAndGet();
            } else if (event instanceof EnumerationFoundModelsEvent) {
                foundModels.addAndGet(((EnumerationFoundModelsEvent) event).getNumberOfModels());
            } else {
                otherCalls.incrementAndGet();
            }
            return true;
        };

        final LngResult<List<Model>> models = res.getNode().execute(meFunc, handler);
        assertThat(models.isSuccess()).isTrue();
        assertThat(startedCalls.get()).isEqualTo(1);
        assertThat(foundModels.get()).isEqualTo(models.getResult().size());
        assertThat(otherCalls.get()).isEqualTo(0);
    }

    @Test
    public void testLimitModels() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(A & B) | (B & C) | (C & D)"));
        final SddCompilationResult res = SddCompiler.compile(f, formula);
        final Sdd sdd = res.getSdd();
        final SddModelEnumerationFunction meFunc =
                SddModelEnumerationFunction.builder(sdd, f.variables("A", "B", "C", "D", "E")).build();

        final LngResult<List<Model>> models = res.getNode().execute(meFunc, new NumberOfModelsHandler(4));
        assertThat(models.isPartial()).isTrue();
        assertThat(models.getPartialResult()).hasSize(4);
    }

    @Test
    public void testProjected() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = f.parse("(A | ~C) & (B | C | D) & (B | D) & (X | C)");
        compileAndCheck(f.verum(), f.variables("A", "D"), f.variables("X", "Y"), f);
        compileAndCheck(f.falsum(), f.variables("A", "D"), f.variables("X", "Y"), f);
        compileAndCheck(f.variable("A"), f.variables("A"), f.variables(), f);
        compileAndCheck(f.variable("A"), f.variables("A"), f.variables("B"), f);
        compileAndCheck(f.variable("A"), f.variables(), f.variables("B"), f);
        compileAndCheck(f.variable("A"), f.variables(), f.variables("A"), f);
        compileAndCheck(formula, f.variables(), f.variables(), f);
        compileAndCheck(formula, f.variables(), f.variables("A"), f);
        compileAndCheck(formula, f.variables(), f.variables("A"), f);
        compileAndCheck(formula, f.variables("A", "D", "X"), Set.of(), f);
        compileAndCheck(formula, f.variables("A", "D", "X", "Y"), Set.of(), f);
        compileAndCheck(formula, f.variables("A", "D", "X"), f.variables("Y"), f);
        compileAndCheck(formula, f.variables("A", "D"), f.variables("X", "Y"), f);
        compileAndCheck(formula, f.variables("A", "D", "X", "Y"), f.variables("X", "Y"), f);
        compileAndCheck(formula, f.variables("A", "D", "X", "Y"), f.variables("X"), f);
    }

    private final static List<String> FILES = List.of(
            "../test_files/sdd/compile_example1.cnf",
            "../test_files/sdd/compile_example2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_1.cnf",
            "../test_files/dnnf/both_bdd_dnnf_2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_3.cnf",
            "../test_files/dnnf/both_bdd_dnnf_4.cnf",
            "../test_files/dnnf/both_bdd_dnnf_5.cnf"
    );

    @Test
    public void testProjectedFiles() throws ParserException, IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final Set<Variable> vars = formula.variables(f);
            final Set<Variable> p = vars.stream().limit(10).collect(Collectors.toSet());
            final Set<Variable> a = vars.stream().limit(30).collect(Collectors.toSet());
            compileAndCheck(formula, p, a, f);
        }
    }

    private static void compileAndCheck(final Formula formula, final Set<Variable> vars,
                                        final Set<Variable> additionals,
                                        final FormulaFactory f) {
        final SddCompilationResult res = SddCompiler.compile(f, formula);
        final Sdd sdd = res.getSdd();
        final SddModelEnumerationFunction meFunc = SddModelEnumerationFunction
                .builder(sdd, vars)
                .additionalVariables(additionals)
                .build();
        check(res.getNode(), formula, meFunc, sdd);
    }

    private static void check(final SddNode node, final Formula formula, final SddModelEnumerationFunction meFunc,
                              final Sdd sdd) {
        final List<Model> models = node.execute(meFunc);
        final List<Assignment> assignments = models.stream().map(Model::toAssignment).collect(Collectors.toList());
        for (final Assignment assignment : assignments) {
            assertThat(formula.restrict(sdd.getFactory(), assignment).isSatisfiable(sdd.getFactory())).isTrue();
        }
        final ModelEnumerationFunction solverFunc = ModelEnumerationFunction
                .builder(meFunc.getVariables())
                .additionalVariables(meFunc.getAdditionalVariables())
                .build();
        final List<Assignment> expected = enumerateFormula(solverFunc, formula, sdd.getFactory());
        assertThat(models).hasSize(expected.size());
        if (!expected.isEmpty()) {
            final int expectedModelSize = expected.get(0).size();
            for (final Assignment assignment : assignments) {
                assertThat(assignment.size()).isEqualTo(expectedModelSize);
            }
        }
    }

    private static List<Assignment> enumerateFormula(final ModelEnumerationFunction meFunc, final Formula formula,
                                                     final FormulaFactory f) {
        final SatSolver solverVerum = SatSolver.newSolver(f);
        solverVerum.add(formula);
        final List<Model> expected = solverVerum.execute(meFunc);
        return expected.stream()
                .map(Model::toAssignment)
                .collect(Collectors.toList());
    }

}
