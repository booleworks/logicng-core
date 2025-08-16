package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.io.IOException;
import java.util.List;

public class SddCompilerTopDownTest {
    private final static List<String> FILES = List.of(
            "../test_files/sdd/compile_example1.cnf",
            "../test_files/dnnf/both_bdd_dnnf_1.cnf",
            "../test_files/dnnf/both_bdd_dnnf_2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_3.cnf",
            "../test_files/dnnf/both_bdd_dnnf_4.cnf",
            "../test_files/dnnf/both_bdd_dnnf_5.cnf"
    );

    private final static List<SddCompilerConfig> configs = List.of(
            SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.TOP_DOWN).preprocessing(true).build(),
            SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.TOP_DOWN).preprocessing(false).build()
    );

    @ParameterizedTest
    @FieldSource("configs")
    public void testSimple(final SddCompilerConfig config) throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        compileAndCheck(f.verum(), f, config);
        compileAndCheck(f.falsum(), f, config);
        compileAndCheck(f.variable("X"), f, config);
        compileAndCheck(f.literal("X", false), f, config);
        compileAndCheck(f.parse("(Y | ~Z) & (~X | Z) & (X | ~Y) & (X | Q)"), f, config);
    }

    @ParameterizedTest
    @FieldSource("configs")
    public void testFormulas(final SddCompilerConfig config) throws ParserException, IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            compileAndCheck(formula, f, config);
        }
    }

    private static void compileAndCheck(final Formula formula, final FormulaFactory f, final SddCompilerConfig config) {
        final SddCompilationResult result = SddCompiler.compile(formula, config, f);
        final Sdd sdd = result.getSdd();
        SddTestUtil.validateMC(result.getNode(), formula, sdd);
        SddTestUtil.validateExport(result.getNode(), formula, sdd);
    }
}
