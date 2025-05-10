package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class SddCompilerTopDownTest {
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
    public void testComp() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = f.parse("(Y | ~Z) & (~X | Z) & (X | ~Y) & (X | Q)");
        final SddCompilationResult result = SddCompilerTopDown.compile(formula, f, NopHandler.get()).getResult();
        final Sdd sdd = result.getSdd();
        SddTestUtil.validateMC(result.getNode(), result.getVTree(), formula, sdd);
        SddTestUtil.validateExport(result.getNode(), formula, sdd);
    }

    @Test
    public void testFormulas() throws ParserException, IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult result =
                    SddCompilerTopDown.compile(formula, f, NopHandler.get()).getResult();
            final Sdd sdd = result.getSdd();
            SddTestUtil.validateMC(result.getNode(), result.getVTree(), formula, sdd);
            SddTestUtil.validateExport(result.getNode(), formula, sdd);
        }
    }
}
