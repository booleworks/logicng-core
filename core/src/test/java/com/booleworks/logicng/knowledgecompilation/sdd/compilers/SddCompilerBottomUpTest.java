// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtreegeneration.BalancedVTreeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class SddCompilerBottomUpTest {
    private final static List<String> FILES = List.of(
            "../test_files/sdd/compile_example2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_1.cnf",
            "../test_files/dnnf/both_bdd_dnnf_2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_3.cnf",
            "../test_files/dnnf/both_bdd_dnnf_4.cnf",
            "../test_files/dnnf/both_bdd_dnnf_5.cnf"
    );

    private final static List<Supplier<SddCompilerConfig.Builder>> configs = List.of(
            () -> SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.BOTTOM_UP).preprocessing(true),
            () -> SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.BOTTOM_UP).preprocessing(false)
    );

    @ParameterizedTest
    @FieldSource("configs")
    public void testSimple(final Supplier<SddCompilerConfig.Builder> configSupplier) throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SddCompilerConfig config = configSupplier.get().build();
        compileAndCheck(f.verum(), f, config);
        compileAndCheck(f.falsum(), f, config);
        compileAndCheck(f.variable("X"), f, config);
        compileAndCheck(f.literal("X", false), f, config);
        compileAndCheck(f.parse("(Y | ~Z) & (~X | Z) & (X | ~Y) & (X | Q)"), f, config);
    }

    @ParameterizedTest
    @FieldSource("configs")
    public void testIndependentSdd(final Supplier<SddCompilerConfig.Builder> configSupplier) throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final VTreeRoot.Builder builder = VTreeRoot.builder();
        final VTree vtree = new BalancedVTreeGenerator(f.variables("X", "Y", "Z", "Q")).generate(builder);
        final Sdd sdd = new Sdd(f, builder.build(vtree));
        final SddCompilerConfig config = configSupplier.get().sdd(sdd).build();
        compileAndCheck(f.verum(), f, config);
        compileAndCheck(f.falsum(), f, config);
        compileAndCheck(f.variable("X"), f, config);
        compileAndCheck(f.literal("X", false), f, config);
        compileAndCheck(f.parse("(Y | ~Z) & (~X | Z) & (X | ~Y) & (X | Q)"), f, config);
    }

    @Test
    public void testFiles() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        for (final String file : FILES) {
            final Formula cnf = f.and(DimacsReader.readCNF(f, file));
            final SddCompilerConfig config =
                    SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.BOTTOM_UP).build();
            compileAndCheck(cnf, f, config);
        }
    }

    private static void compileAndCheck(final Formula formula, final FormulaFactory f, final SddCompilerConfig config) {
        final SddCompilationResult result = SddCompiler.compile(formula, config, f);
        final Sdd sdd = result.getSdd();
        SddTestUtil.validateMC(result.getNode(), formula, sdd);
        SddTestUtil.validateExport(result.getNode(), formula, sdd);
    }
}
