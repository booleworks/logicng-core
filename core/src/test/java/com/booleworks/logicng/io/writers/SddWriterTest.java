package com.booleworks.logicng.io.writers;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.io.readers.SddReader;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompilerConfig;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddExportFormula;
import com.booleworks.logicng.util.Pair;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SddWriterTest {
    @Test
    public void testReimportSimple() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SddCompilationResult result = SddCompiler.compile(f, f.parse("(A | B) & (V | W | X) & (A | ~X)"));
        final Pair<SddNode, Sdd> reimport = reimportSdd(result.getNode(), result.getSdd());
        checkFormulaOfReimport(reimport, result.getNode(), result.getSdd());
    }

    @Test
    public void testVTreeReimportSimple() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = f.parse("(A | B) & (V | W | X) & (A | ~X)");
        final SddCompilationResult result = SddCompiler.compile(f, formula);
        final Pair<VTree, VTreeRoot.Builder> reimport = reimportVTree(result.getSdd());
        final VTreeRoot root = reimport.getSecond().build(reimport.getFirst());
        final Sdd sdd = new Sdd(f, root);
        recompileFormulaAndCheck(sdd, result.getNode(), result.getSdd(), formula);
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
    public void testReimportFiles() throws IOException, ParserException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult result = SddCompiler.compile(f, formula);
            final Pair<SddNode, Sdd> reimport = reimportSdd(result.getNode(), result.getSdd());
            checkFormulaOfReimport(reimport, result.getNode(), result.getSdd());
        }
    }

    @Test
    public void testVTreeReimportFiles() throws IOException, ParserException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult result = SddCompiler.compile(f, formula);
            final Pair<VTree, VTreeRoot.Builder> reimport = reimportVTree(result.getSdd());
            final VTreeRoot root = reimport.getSecond().build(reimport.getFirst());
            final Sdd sdd = new Sdd(f, root);
            recompileFormulaAndCheck(sdd, result.getNode(), result.getSdd(), formula);
        }
    }

    private static void checkFormulaOfReimport(final Pair<SddNode, Sdd> reimport, final SddNode node, final Sdd sdd) {
        final Formula formula = node.execute(new SddExportFormula(sdd));
        final Formula reimportedFormula = reimport.getFirst().execute(new SddExportFormula(reimport.getSecond()));
        assertThat(formula).isSameAs(reimportedFormula);
    }

    private static void recompileFormulaAndCheck(final Sdd reimport, final SddNode node, final Sdd sdd,
                                                 final Formula formula) {
        final SddCompilerConfig config =
                SddCompilerConfig.builder().compiler(SddCompilerConfig.Compiler.BOTTOM_UP).sdd(sdd).build();
        final SddCompilationResult recompiled = SddCompiler.compile(reimport.getFactory(), formula, config);
        final Formula formulaOfNode = node.execute(new SddExportFormula(sdd));
        final Formula reimportedFormula = recompiled.getNode().execute(new SddExportFormula(recompiled.getSdd()));
        assertThat(formulaOfNode).isSameAs(reimportedFormula);
    }

    private static Pair<SddNode, Sdd> reimportSdd(final SddNode node, final Sdd sdd)
            throws IOException, ParserException {
        final File tempFile = new File("../test_files/writers/temp/temp.sdd");
        SddWriter.writeSdd(tempFile, node, sdd);
        return SddReader.readSdd(tempFile, sdd.getFactory());
    }

    private static Pair<VTree, VTreeRoot.Builder> reimportVTree(final Sdd sdd)
            throws IOException, ParserException {
        final File tempFile = new File("../test_files/writers/temp/temp.vtree");
        SddWriter.writeVTree(tempFile, sdd);
        return SddReader.readVTree(tempFile, sdd.getFactory());
    }
}
