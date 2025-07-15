package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompilerConfig;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddMinimizationConfig;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.TransformationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.BalancedVTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class SddMinimizationTest {
    @Test
    public void testLocalOptimumSimple() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.parse("(A | C) & (B | C | D)");
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config = SddCompilerConfig.builder()
                .compiler(SddCompilerConfig.Compiler.BOTTOM_UP)
                .sdd(sdd)
                .build();
        final SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final TransformationResult res =
                SddMinimization.bestLocalState(new SddMinimization.SearchState(vtree, sdd), NopHandler::get,
                        NopHandler.get()).getResult();
        final SddNode mini = res.getTranslations().get(node);
        SddTestUtil.validateExport(mini, formula, sdd);
    }

    @Test
    public void testLocalOptimumFile() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.and(DimacsReader.readCNF(f, "../test_files/sdd/compile_example1.cnf"));
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config = SddCompilerConfig.builder()
                .compiler(SddCompilerConfig.Compiler.BOTTOM_UP)
                .sdd(sdd)
                .build();
        final SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final TransformationResult res =
                SddMinimization.bestLocalState(new SddMinimization.SearchState(vtree, sdd), NopHandler::get,
                        NopHandler.get()).getResult();
        final SddNode mini = res.getTranslations().get(node);
        SddTestUtil.validateExport(mini, formula, sdd);
    }

    @Test
    public void testLocalOptimumFileInner() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.and(DimacsReader.readCNF(f, "../test_files/sdd/compile_example1.cnf"));
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config = SddCompilerConfig.builder()
                .compiler(SddCompilerConfig.Compiler.BOTTOM_UP)
                .sdd(sdd)
                .build();
        final SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final TransformationResult res = SddMinimization.bestLocalState(
                new SddMinimization.SearchState(vtree.asInternal().getLeft().asInternal().getRight(), sdd),
                NopHandler::get, NopHandler.get()).getResult();
        final SddNode mini = res.getTranslations().get(node);
        SddTestUtil.validateExport(mini, formula, sdd);
    }

    @Test
    public void testSinglePassSimple() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.parse("(A | C) & (B | C | D)");
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config = SddCompilerConfig.builder()
                .compiler(SddCompilerConfig.Compiler.BOTTOM_UP)
                .sdd(sdd)
                .build();
        final SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final TransformationResult res =
                SddMinimization.localSearchPass(0, sdd, NopHandler::get, NopHandler.get()).getResult();
        final SddNode mini = res.getTranslations().get(node);
        SddTestUtil.validateExport(mini, formula, sdd);
    }

    @Test
    public void testSinglePassFile() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.and(DimacsReader.readCNF(f, "../test_files/sdd/compile_example1.cnf"));
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config = SddCompilerConfig.builder()
                .compiler(SddCompilerConfig.Compiler.BOTTOM_UP)
                .sdd(sdd)
                .build();
        final SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final TransformationResult res =
                SddMinimization.localSearchPass(0, sdd, NopHandler::get, NopHandler.get()).getResult();
        final SddNode mini = res.getTranslations().get(node);
        SddTestUtil.validateExport(mini, formula, sdd);
    }


    @Test
    public void testMinimizeSimple() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.parse("(A | C) & (B | C | D)");
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config = SddCompilerConfig.builder()
                .compiler(SddCompilerConfig.Compiler.BOTTOM_UP)
                .sdd(sdd)
                .build();
        final SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final TransformationResult res = SddMinimization.minimize(sdd, NopHandler::get, NopHandler.get()).getResult();
        final SddNode mini = res.getTranslations().get(node);
        SddTestUtil.validateExport(mini, formula, sdd);
    }

    @Test
    public void testMinimizeFile() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.and(DimacsReader.readCNF(f, "../test_files/sdd/compile_example1.cnf"));
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config = SddCompilerConfig.builder()
                .compiler(SddCompilerConfig.Compiler.BOTTOM_UP)
                .sdd(sdd)
                .build();
        final SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final TransformationResult res = SddMinimization.minimize(sdd, NopHandler::get, NopHandler.get()).getResult();
        final SddNode mini = res.getTranslations().get(node);
        SddTestUtil.validateExport(mini, formula, sdd);
    }

    private final static List<String> FILES = List.of(
            "../test_files/sdd/compile_example1.cnf",
            "../test_files/dnnf/both_bdd_dnnf_1.cnf",
            "../test_files/dnnf/both_bdd_dnnf_2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_3.cnf",
            "../test_files/dnnf/both_bdd_dnnf_4.cnf",
            "../test_files/dnnf/both_bdd_dnnf_5.cnf"
    );

    @Test
    @LongRunningTag
    public void testMinimizeFiles() throws IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult comp = SddCompiler.compile(formula, f);
            comp.getSdd().pin(comp.getNode());
            final SddMinimizationConfig config = SddMinimizationConfig.unlimited(comp.getSdd());
            final TransformationResult res =
                    SddMinimization.minimize(config).getPartialResult();
            final SddNode mini = res.getTranslations().get(comp.getNode());
            SddTestUtil.validateMC(mini, formula, comp.getSdd());
            SddTestUtil.sampleModels(mini, formula, comp.getSdd(), 1000);
        }
    }

    @Test
    @LongRunningTag
    public void testMinimizeOpTimeoutFiles() throws IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult comp = SddCompiler.compile(formula, f);
            comp.getSdd().pin(comp.getNode());
            final SddMinimizationConfig config =
                    new SddMinimizationConfig.Builder(comp.getSdd()).withOperationTimeout(10).build();
            final TransformationResult res =
                    SddMinimization.minimize(config).getPartialResult();
            final SddNode mini = res.getTranslations().get(comp.getNode());
            SddTestUtil.validateMC(mini, formula, comp.getSdd());
            SddTestUtil.sampleModels(mini, formula, comp.getSdd(), 1000);
        }
    }

    @Test
    @LongRunningTag
    public void testMinimizeTargetSizeFiles() throws IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult comp = SddCompiler.compile(formula, f);
            comp.getSdd().pin(comp.getNode());
            final SddMinimizationConfig config =
                    new SddMinimizationConfig.Builder(comp.getSdd()).withAbsoluteTargetSize(1100).build();
            final LngResult<TransformationResult> res =
                    SddMinimization.minimize(config);
            final SddNode mini = res.getPartialResult().getTranslations().get(comp.getNode());
            assertThat(res.isSuccess() || comp.getSdd().getActiveSize() <= 1100).isTrue();
            SddTestUtil.validateMC(mini, formula, comp.getSdd());
            SddTestUtil.sampleModels(mini, formula, comp.getSdd(), 1000);
        }
    }

    @Test
    @LongRunningTag
    public void testMinimizeDecTh() throws IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult comp = SddCompiler.compile(formula, f);
            comp.getSdd().pin(comp.getNode());
            final SddMinimizationConfig config =
                    new SddMinimizationConfig.Builder(comp.getSdd()).withAlgorithm(
                            SddMinimizationConfig.Algorithm.DEC_THRESHOLD).build();
            final TransformationResult res =
                    SddMinimization.minimize(config).getPartialResult();
            final SddNode mini = res.getTranslations().get(comp.getNode());
            SddTestUtil.validateMC(mini, formula, comp.getSdd());
            SddTestUtil.sampleModels(mini, formula, comp.getSdd(), 1000);
        }
    }
}
