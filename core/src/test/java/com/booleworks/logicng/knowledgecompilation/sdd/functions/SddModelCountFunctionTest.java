package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompilerTopDown;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.modelcounting.ModelCounter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class SddModelCountFunctionTest {
    @Test
    public void test() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SddFactory sf = new SddFactory(f);
        final Formula formula = f.parse("(A & B) | (B & C) | (C & D)");
        final SddCompilationResult res = SddCompilerTopDown.compile(formula, sf, NopHandler.get()).getResult();
        final BigInteger modelCount =
                sf.apply(new SddModelCountFunction(f.variables("A", "B", "C", "D", "E"), res.getSdd(), res.getVTree()));
        final BigInteger modelCountExpected =
                ModelCounter.count(f, List.of(formula), f.variables("A", "B", "C", "D", "E"));
        assertThat(modelCount).isEqualTo(modelCountExpected);
    }

    @Test
    public void testSubtree() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SddFactory sf = new SddFactory(f);
        final Formula formula = f.parse("(A & B) | (B & C) | (C & D)");
        final SddCompilationResult res = SddCompilerTopDown.compile(formula, sf, NopHandler.get()).getResult();
        final SddNode descendant = res.getSdd().asDecomposition().getElements().first().getSub();
        final Formula subformula = sf.apply(new SddExportFormula(descendant));
        final BigInteger models =
                sf.apply(new SddModelCountFunction(subformula.variables(f), descendant, res.getVTree()));
        final BigInteger expected = ModelCounter.count(f, List.of(subformula), subformula.variables(f));
        assertThat(models).isEqualTo(expected);
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
            final SddFactory sf = new SddFactory(f);
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult result =
                    SddCompilerTopDown.compile(formula, sf, NopHandler.get()).getResult();
            final BigInteger sddCount =
                    sf.apply(new SddModelCountFunction(formula.variables(f), result.getSdd(), result.getVTree()));
            final BigInteger formulaCount = ModelCounter.count(f, List.of(formula), formula.variables(f));
            assertThat(sddCount).isEqualTo(formulaCount);
        }
    }
}
