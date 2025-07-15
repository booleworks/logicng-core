package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.solvers.SatSolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SddBackboneFunctionTest {
    @Test
    public void test() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(~C & ~B & A) | (A & ~B & (C => D))"));
        final SddCompilationResult res = SddCompiler.compile(formula, f);
        final Sdd sdd = res.getSdd();
        final Backbone backbone =
                res.getNode().execute(new SddBackboneFunction(f.variables("A", "B", "C", "D", "E"), sdd));
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(formula);
        final Backbone expected = solver.backbone(f.variables("A", "B", "C", "D", "E"));
        assertThat(backbone).isEqualTo(expected);
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
    public void testFiles() throws IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SortedSet<Variable> variables = formula.variables(f);
            final SddCompilationResult result = SddCompiler.compile(formula, f);
            final Sdd sdd = result.getSdd();
            final Backbone backbone =
                    result.getNode().execute(new SddBackboneFunction(variables, sdd));
            final SatSolver solver = SatSolver.newSolver(f);
            solver.add(formula);
            final Backbone expected = solver.backbone(variables);
            assertThat(backbone).isEqualTo(expected);
        }
    }

    @Test
    public void testFilesLimited() throws IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SortedSet<Variable> variables = formula.variables(f)
                    .stream()
                    .limit(30)
                    .collect(Collectors.toCollection(TreeSet::new));
            final SddCompilationResult result = SddCompiler.compile(formula, f);
            final Sdd sdd = result.getSdd();
            final Backbone backbone =
                    result.getNode().execute(new SddBackboneFunction(variables, sdd));
            final SatSolver solver = SatSolver.newSolver(f);
            solver.add(formula);
            final Backbone expected = solver.backbone(variables);
            assertThat(backbone).isEqualTo(expected);
        }
    }
}
