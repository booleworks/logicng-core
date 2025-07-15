package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.OptimizationFunction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class SddCardinalityFunctionTest {
    @Test
    public void testMax() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(A & B) | (B & C) | (C & D)"));
        final SddCompilationResult res = SddCompiler.compile(formula, f);
        final Sdd sdd = res.getSdd();
        final int cardinality =
                res.getNode().execute(new SddCardinalityFunction(true, f.variables("A", "B", "C", "D", "E"), sdd));
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(formula);
        final List<Literal> opt =
                solver.execute(OptimizationFunction.maximize(f.variables("A", "B", "C", "D", "E"))).getLiterals();
        final int expected = (int) opt.stream().filter(Literal::getPhase).count();
        assertThat(cardinality).isEqualTo(expected);
    }

    @Test
    public void testMin() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(A & B) | (B & C) | (C & D)"));
        final SddCompilationResult res = SddCompiler.compile(formula, f);
        final Sdd sdd = res.getSdd();
        final int cardinality =
                res.getNode().execute(new SddCardinalityFunction(false, f.variables("A", "B", "C", "D", "E"), sdd));
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(formula);
        final List<Literal> opt =
                solver.execute(OptimizationFunction.minimize(f.variables("A", "B", "C", "D", "E"))).getLiterals();
        final int expected = (int) opt.stream().filter(Literal::getPhase).count();
        assertThat(cardinality).isEqualTo(expected);
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
    public void testFilesMax() throws IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult res = SddCompiler.compile(formula, f);
            final Sdd sdd = res.getSdd();
            final int cardinality =
                    res.getNode().execute(new SddCardinalityFunction(true, formula.variables(f), sdd));
            final SatSolver solver = SatSolver.newSolver(f);
            solver.add(formula);
            final List<Literal> opt =
                    solver.execute(OptimizationFunction.maximize(formula.variables(f))).getLiterals();
            final int expected = (int) opt.stream().filter(Literal::getPhase).count();
            assertThat(cardinality).isEqualTo(expected);
        }
    }

    @Test
    public void testFilesMin() throws IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult res = SddCompiler.compile(formula, f);
            final Sdd sdd = res.getSdd();
            final int cardinality =
                    res.getNode().execute(new SddCardinalityFunction(false, formula.variables(f), sdd));
            final SatSolver solver = SatSolver.newSolver(f);
            solver.add(formula);
            final List<Literal> opt =
                    solver.execute(OptimizationFunction.minimize(formula.variables(f))).getLiterals();
            final int expected = (int) opt.stream().filter(Literal::getPhase).count();
            assertThat(cardinality).isEqualTo(expected);
        }
    }
}
