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
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.solvers.SatSolver;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SddModelEnumerationFunctionTest {
    @Test
    public void testTrivial() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        sdd.vTreeLeaf(f.variable("A"));
        final SddModelEnumerationFunction meFunc1 =
                SddModelEnumerationFunction.builder(f.variables(), sdd).build();
        final SddModelEnumerationFunction meFunc2 =
                SddModelEnumerationFunction.builder(f.variables("A", "B"), sdd).build();
        check(sdd.verum(), f.verum(), meFunc1, sdd);
        check(sdd.verum(), f.verum(), meFunc2, sdd);
        check(sdd.falsum(), f.falsum(), meFunc1, sdd);
        check(sdd.falsum(), f.falsum(), meFunc2, sdd);
    }

    @Test
    public void test() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(A & B) | (B & C) | (C & D)"));
        final SddCompilationResult res = SddCompiler.compile(formula, f);
        final Sdd sdd = res.getSdd();
        final SddModelEnumerationFunction meFunc =
                SddModelEnumerationFunction.builder(f.variables("A", "B", "C", "D", "E"), sdd).build();
        check(res.getNode(), formula, meFunc, sdd);
    }

    @Test
    public void testEvents() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(A & B) | (B & C) | (C & D)"));
        final SddCompilationResult res = SddCompiler.compile(formula, f);
        final Sdd sdd = res.getSdd();
        final SddModelEnumerationFunction meFunc =
                SddModelEnumerationFunction.builder(f.variables("A", "B", "C", "D", "E"), sdd).build();

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
        final SddCompilationResult res = SddCompiler.compile(formula, f);
        final Sdd sdd = res.getSdd();
        final SddModelEnumerationFunction meFunc =
                SddModelEnumerationFunction.builder(f.variables("A", "B", "C", "D", "E"), sdd).build();

        final LngResult<List<Model>> models = res.getNode().execute(meFunc, new NumberOfModelsHandler(4));
        assertThat(models.isPartial()).isTrue();
        assertThat(models.getPartialResult()).hasSize(4);
    }

    @Test
    public void testProjected() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = f.parse("(A | ~C) & (B | C | D) & (B | D) & (X | C)");
        final SddCompilationResult res = SddCompiler.compile(formula, f);
        final Sdd sdd = res.getSdd();
        final SddModelEnumerationFunction meFunc =
                SddModelEnumerationFunction.builder(f.variables("A", "D", "X"), sdd).build();
        check(res.getNode(), formula, meFunc, sdd);
    }

    @Test
    public void testSubtree() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(A & B) | (B & C) | (C & D)"));
        final SddCompilationResult res = SddCompiler.compile(formula, f);
        final Sdd sdd = res.getSdd();
        final SddNode descendant = res.getNode().asDecomposition().getElementsUnsafe().get(0).getSub();
        final Formula subformula = descendant.execute(new SddExportFormula(sdd));
        final SddModelEnumerationFunction meFunc =
                SddModelEnumerationFunction.builder(subformula.variables(f), sdd).build();
        check(descendant, subformula, meFunc, sdd);
    }

    private static void check(final SddNode node, final Formula formula, final SddModelEnumerationFunction meFunc,
                              final Sdd sdd) {
        final List<Model> models = node.execute(meFunc);
        final List<Assignment> expected = enumerateFormula(meFunc.getVariables(), formula, sdd.getFactory());
        assertThat(models.stream().map(Model::toAssignment)).containsExactlyInAnyOrderElementsOf(expected);
    }

    private static List<Assignment> enumerateFormula(final Collection<Variable> variables, final Formula formula,
                                                     final FormulaFactory f) {
        final SatSolver solverVerum = SatSolver.newSolver(f);
        solverVerum.add(formula);
        final List<Model> expected = solverVerum.enumerateAllModels(variables);
        return expected.stream()
                .map(Model::toAssignment)
                .collect(Collectors.toList());
    }

}
