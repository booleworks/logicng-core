// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.LongRunningTag;
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

public class SddCompilerTest {
    private final static List<String> FILES = List.of(
            "../test_files/sdd/compile_example1.cnf",
            "../test_files/sdd/compile_example2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_1.cnf",
            "../test_files/dnnf/both_bdd_dnnf_2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_3.cnf",
            "../test_files/dnnf/both_bdd_dnnf_4.cnf",
            "../test_files/dnnf/both_bdd_dnnf_5.cnf"
    );

    private final static List<SddCompilerConfig> configs = List.of(
            SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.TOP_DOWN).preprocessing(true).build(),
            SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.TOP_DOWN).preprocessing(false).build(),
            SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.BOTTOM_UP).preprocessing(true).build(),
            SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.BOTTOM_UP).preprocessing(false).build()
    );

    @ParameterizedTest
    @FieldSource("configs")
    public void testSimple(final SddCompilerConfig config) throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        compileAndCheckExport(f.parse("$true"), f, config);
        compileAndCheckExport(f.parse("$false"), f, config);
        compileAndCheckExport(f.parse("a"), f, config);
        compileAndCheckExport(f.parse("~a"), f, config);
        compileAndCheckExport(f.parse("a & b"), f, config);
        compileAndCheckExport(f.parse("a | b"), f, config);
        compileAndCheckExport(f.parse("a => b"), f, config);
        compileAndCheckExport(f.parse("a <=> b"), f, config);
        compileAndCheckExport(f.parse("a | b | c"), f, config);
        compileAndCheckExport(f.parse("a & b & c"), f, config);
        compileAndCheckExport(f.parse("f & ((~b | c) <=> ~a & ~c)"), f, config);
        compileAndCheckExport(f.parse("a | ((b & ~c) | (c & (~d | ~a & b)) & e)"), f, config);
        compileAndCheckExport(f.parse("a + b + c + d <= 1"), f, config);
        compileAndCheckExport(f.parse("a + b + c + d <= 3"), f, config);
        compileAndCheckExport(f.parse("2*a + 3*b + -2*c + d < 5"), f, config);
        compileAndCheckExport(f.parse("2*a + 3*b + -2*c + d >= 5"), f, config);
        compileAndCheckExport(f.parse("~a & (~a | b | c | d)"), f, config);
    }

    @ParameterizedTest
    @FieldSource("configs")
    public void testFiles(final SddCompilerConfig config) throws ParserException, IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            compileAndCheck(formula, f, config);
        }
    }

    @ParameterizedTest
    @FieldSource("configs")
    @LongRunningTag
    public void testFilesLong(final SddCompilerConfig config) throws ParserException, IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            compileAndCheckExport(formula, f, config);
        }
    }

    private static void compileAndCheck(final Formula formula, final FormulaFactory f, final SddCompilerConfig config) {
        final SddCompilationResult result = SddCompiler.compile(f, formula, config);
        final Sdd sdd = result.getSdd();
        SddTestUtil.validateMC(result.getNode(), formula, sdd);
        SddTestUtil.sampleModels(result.getNode(), formula, sdd, 100);
    }

    private static void compileAndCheckExport(final Formula formula, final FormulaFactory f,
                                              final SddCompilerConfig config) {
        final SddCompilationResult result = SddCompiler.compile(f, formula.cnf(f), config);
        final Sdd sdd = result.getSdd();
        SddTestUtil.validateExport(result.getNode(), formula, sdd);
    }
}
