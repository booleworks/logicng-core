package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.modelcounting.ModelCounter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SddModelCountFunctionTest {
    @Test
    public void testTrivial() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        sdd.vTreeLeaf(f.variable("A"));
        check(sdd.verum(), f.verum(), f.variables(), sdd);
        check(sdd.verum(), f.verum(), f.variables("A", "B"), sdd);
        check(sdd.falsum(), f.falsum(), f.variables(), sdd);
        check(sdd.falsum(), f.falsum(), f.variables("A", "B"), sdd);
    }

    @Test
    public void test() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(A & B) | (B & C) | (C & D)"));
        final SddCompilationResult res = SddCompiler.compile(formula, f);
        final Sdd sdd = res.getSdd();
        check(res.getNode(), formula, f.variables("A", "B", "C", "D", "E"), sdd);
    }

    @Test
    public void testSubtree() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(A & B) | (B & C) | (C & D)"));
        final SddCompilationResult res = SddCompiler.compile(formula, f);
        final Sdd sdd = res.getSdd();
        final SddNode descendant = res.getNode().asDecomposition().getElementsUnsafe().get(0).getSub();
        final Formula subformula = descendant.execute(new SddExportFormula(sdd));
        check(descendant, subformula, subformula.variables(f), sdd);
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
            final SddCompilationResult result = SddCompiler.compile(formula, f);
            final Sdd sdd = result.getSdd();
            check(result.getNode(), formula, formula.variables(f), sdd);
        }
    }

    @Test
    public void testFilesProjected() throws IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final Set<Variable> remainingVars = formula.variables(f).stream()
                    .limit(formula.variables(f).size() / 2)
                    .collect(Collectors.toSet());
            final SddCompilationResult result = SddCompiler.compile(formula, f);
            final SddNode projected =
                    result.getNode().execute(new SddProjectionFunction(remainingVars, result.getSdd()));
            final Formula projectedFormula = projected.execute(new SddExportFormula(result.getSdd()));
            final Sdd sdd = result.getSdd();
            check(result.getNode(), projectedFormula, new TreeSet<>(remainingVars), sdd);
        }
    }

    private static void check(final SddNode node, final Formula formula, final SortedSet<Variable> variables,
                              final Sdd sdd) {
        final BigInteger modelCount = node.execute(new SddModelCountFunction(variables, sdd));
        final BigInteger modelCountExpected = ModelCounter.count(sdd.getFactory(), List.of(formula), variables);
        assertThat(modelCount).isEqualTo(modelCountExpected);
    }

}
