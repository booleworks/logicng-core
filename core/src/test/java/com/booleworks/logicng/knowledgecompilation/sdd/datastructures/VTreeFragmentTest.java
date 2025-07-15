package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompilerConfig;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.BalancedVTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeFragment;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class VTreeFragmentTest {
    @Test
    public void iterateAllVTreeStatesLeft() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.parse("(A | C) & (B | C | D)");
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config =
                SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.BOTTOM_UP).sdd(sdd).build();
        SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final VTreeFragment fragment = new VTreeFragment(true, vtree, sdd);
        while (fragment.hasNext()) {
            final TransformationResult t = fragment.next(NopHandler.get()).getResult();
            node = t.getTranslations().get(node);
            SddTestUtil.validateExport(node, formula, sdd);
        }
    }

    @Test
    public void iterateAllVTreeStatesRight() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.parse("(A | C) & (B | C | D)");
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config =
                SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.BOTTOM_UP).sdd(sdd).build();
        SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final VTreeFragment fragment = new VTreeFragment(false, vtree, sdd);
        while (fragment.hasNext()) {
            final TransformationResult t = fragment.next(NopHandler.get()).getResult();
            node = t.getTranslations().get(node);
            SddTestUtil.validateExport(node, formula, sdd);
        }
    }

    @Test
    public void iterateAllVTreeStatesFileRootRight() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.and(DimacsReader.readCNF(f, "../test_files/sdd/compile_example1.cnf"));
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config =
                SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.BOTTOM_UP).sdd(sdd).build();
        SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final VTreeFragment fragment = new VTreeFragment(false, vtree, sdd);
        while (fragment.hasNext()) {
            final TransformationResult t = fragment.next(NopHandler.get()).getResult();
            node = t.getTranslations().get(node);
            SddTestUtil.validateMC(node, formula, sdd);
            SddTestUtil.sampleModels(node, formula, sdd, 100);
        }
    }

    @Test
    public void iterateAllVTreeStatesFileRootLeft() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.and(DimacsReader.readCNF(f, "../test_files/sdd/compile_example1.cnf"));
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config =
                SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.BOTTOM_UP).sdd(sdd).build();
        SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final VTreeFragment fragment = new VTreeFragment(true, vtree, sdd);
        while (fragment.hasNext()) {
            final TransformationResult t = fragment.next(NopHandler.get()).getResult();
            node = t.getTranslations().get(node);
            SddTestUtil.validateMC(node, formula, sdd);
            SddTestUtil.sampleModels(node, formula, sdd, 100);
        }
    }

    @Test
    public void iterateAllVTreeStatesFileInnerRight() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.and(DimacsReader.readCNF(f, "../test_files/sdd/compile_example1.cnf"));
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config =
                SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.BOTTOM_UP).sdd(sdd).build();
        SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final VTreeFragment fragment =
                new VTreeFragment(false, vtree.asInternal().getLeft().asInternal().getRight(), sdd);
        while (fragment.hasNext()) {
            final TransformationResult t = fragment.next(NopHandler.get()).getResult();
            node = t.getTranslations().get(node);
            SddTestUtil.validateMC(node, formula, sdd);
            SddTestUtil.sampleModels(node, formula, sdd, 100);
        }
    }

    @Test
    public void iterateAllVTreeStatesFileInnerLeft() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sdd = Sdd.independent(f);
        final Formula formula = f.and(DimacsReader.readCNF(f, "../test_files/sdd/compile_example1.cnf"));
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sdd);
        sdd.defineVTree(vtree);
        final SddCompilerConfig config =
                SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.BOTTOM_UP).sdd(sdd).build();
        SddNode node = SddCompiler.compile(formula, config, f).getNode();
        sdd.pin(node);
        final VTreeFragment fragment =
                new VTreeFragment(true, vtree.asInternal().getLeft().asInternal().getRight(), sdd);
        while (fragment.hasNext()) {
            final TransformationResult t = fragment.next(NopHandler.get()).getResult();
            node = t.getTranslations().get(node);
            SddTestUtil.validateMC(node, formula, sdd);
            SddTestUtil.sampleModels(node, formula, sdd, 100);
        }
    }
}
