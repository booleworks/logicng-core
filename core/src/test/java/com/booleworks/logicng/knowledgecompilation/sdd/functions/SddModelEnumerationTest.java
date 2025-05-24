package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompilerTopDown;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.solvers.SatSolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

public class SddModelEnumerationTest {
    @Test
    public void test() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = f.parse("(A & B) | (B & C) | (C & D)");
        final SddCompilationResult res = SddCompilerTopDown.compile(formula, f, NopHandler.get()).getResult();
        final Sdd sdd = res.getSdd();
        final List<Model> models =
                sdd.apply(new SddModelEnumeration(f.variables("A", "B", "C", "D", "E"), res.getNode()));
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(formula);
        final List<Model> expected = solver.enumerateAllModels(f.variables("A", "B", "C", "D", "E"));
        final List<Assignment> expectedAssignments =
                expected.stream().map(Model::toAssignment).collect(Collectors.toList());
        assertThat(models.stream().map(Model::toAssignment)).containsExactlyInAnyOrderElementsOf(expectedAssignments);
    }

    @Test
    public void testProjected() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = f.parse("(A | ~C) & (B | C | D) & (B | D) & (X | C)");
        final SddCompilationResult res = SddCompilerTopDown.compile(formula, f, NopHandler.get()).getResult();
        final Sdd sdd = res.getSdd();
        final List<Model> models =
                sdd.apply(new SddProjectedModelEnumeration(f.variables("A", "D", "X"), res.getNode()));
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(formula);
        final List<Model> expected = solver.enumerateAllModels(f.variables("A", "D", "X"));
        final List<Assignment> expectedAssignments =
                expected.stream().map(Model::toAssignment).collect(Collectors.toList());
        assertThat(models.stream().map(Model::toAssignment)).containsExactlyInAnyOrderElementsOf(expectedAssignments);
    }

    @Test
    public void testSubtree() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = f.parse("(A & B) | (B & C) | (C & D)");
        final SddCompilationResult res = SddCompilerTopDown.compile(formula, f, NopHandler.get()).getResult();
        final Sdd sdd = res.getSdd();
        final SddNode descendant = res.getNode().asDecomposition().getElements().first().getSub();
        final Formula subformula = sdd.apply(new SddExportFormula(descendant));
        final List<Model> models =
                sdd.apply(new SddModelEnumeration(subformula.variables(f), descendant));

        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(subformula);
        final List<Model> expected = solver.enumerateAllModels(subformula.variables(f));
        final List<Assignment> expectedAssignments =
                expected.stream().map(Model::toAssignment).collect(Collectors.toList());
        assertThat(models.stream().map(Model::toAssignment)).containsExactlyInAnyOrderElementsOf(expectedAssignments);
    }
}
