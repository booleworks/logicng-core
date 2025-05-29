package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompilerBottomUp;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompilerTopDown;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.BalancedVTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SddSwapTest {
    private final static List<String> FILES = List.of(
            "../test_files/sdd/compile_example1.cnf",
            "../test_files/sdd/compile_example2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_1.cnf",
            "../test_files/dnnf/both_bdd_dnnf_2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_3.cnf",
            "../test_files/dnnf/both_bdd_dnnf_4.cnf",
            "../test_files/dnnf/both_bdd_dnnf_5.cnf"
    );
    private final static List<List<Integer>> VTREE_POSISTIONS = List.of(
            List.of(25, 28, 16, 1, 35, 41),
            List.of(18, 0, 22, 20, 22, 13),
            List.of(382, 363, 12, 195, 428, 403, 246, 140, 203, 91, 377, 25, 376, 311, 42, 49, 275, 295, 418,
                    123, 248, 384, 389, 197, 353, 400, 233, 45, 102, 306, 235, 263, 309, 108, 146, 130, 278, 239, 188,
                    175, 19, 40, 386, 172),
            List.of(267, 298, 2, 336, 221, 30, 43, 147, 207, 365, 379, 395, 239, 264, 275, 51, 40, 110, 337,
                    63, 264, 142, 474, 308, 399, 117, 459, 402, 94, 238, 14, 355, 57, 267, 175, 330, 211, 109, 370, 143,
                    399, 249, 31, 130, 303, 166, 379, 274, 290),
            List.of(186, 401, 465, 81, 57, 24, 137, 461, 340, 228, 335, 105, 117, 289, 272, 487, 52, 88, 133,
                    34, 3, 192, 338, 74, 462, 300, 389, 431, 371, 25, 121, 458, 179, 41, 133, 431, 460, 58, 33, 423,
                    463, 159, 372, 156, 147, 108, 70, 69, 480),
            List.of(287, 260, 378, 69, 157, 225, 219, 477, 432, 418, 179, 317, 336, 50, 47, 357, 484, 377, 288,
                    277, 260, 335, 310, 381, 127, 407, 497, 38, 210, 89, 2, 106, 203, 152, 185, 184, 341, 215, 324,
                    249, 355, 508, 265, 14, 252, 256, 276, 8, 245, 45, 462, 494),
            List.of(308, 241, 103, 74, 404, 377, 201, 384, 124, 252, 334, 232, 138, 287, 122, 322, 227, 60,
                    110, 0, 380, 138, 201, 227, 194, 315, 45, 68, 392, 25, 398, 213, 118, 229, 64, 234, 54, 133, 357,
                    399, 302, 323)
    );

    @Test
    public void testSimple() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sf = Sdd.independent(f);
        final Formula formula = f.parse("(A | C) & (B | C | D)");
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sf);
        sf.defineVTree(vtree);
        final SddNode node = SddCompilerBottomUp.cnfToSdd(formula, sf, NopHandler.get()).getResult();
        sf.pin(node);
        final Map<SddNode, SddNode> translations =
                SddGlobalTransformations.swap(vtree.asInternal(), sf, NopHandler.get()).getResult().getTranslations();
        final SddNode swapped = translations.get(node);
        assert Validation.validVTree(swapped, sf);
        SddTestUtil.validateMC(swapped, formula, sf);
        SddTestUtil.validateExport(swapped, formula, sf);
    }

    @Test
    public void testFilesSwapRoot() throws IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult result = SddCompilerTopDown.compile(formula, f, NopHandler.get()).getResult();
            final Sdd sdd = result.getSdd();
            final SddNode node = result.getNode();
            final VTreeInternal rootNode = sdd.getVTree().getRoot().asInternal();
            sdd.pin(node);
            final Map<SddNode, SddNode> translations =
                    SddGlobalTransformations.swap(rootNode, sdd, NopHandler.get()).getResult().getTranslations();
            final SddNode swapped = translations.get(node);
            assert Validation.validVTree(swapped, sdd);
            SddTestUtil.validateMC(swapped, formula, sdd);
            SddTestUtil.validateExport(swapped, formula, sdd);
        }
    }

    @Test
    public void testFilesSwap5Random() throws IOException {
        int i = 0;
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final List<Integer> vtreeSeq = VTREE_POSISTIONS.get(i);
            i++;
            final SddCompilationResult result = SddCompilerTopDown.compile(formula, f, NopHandler.get()).getResult();
            final Sdd sdd = result.getSdd();
            SddNode node = result.getNode();
            for (int j = 0; j < 5; ++j) {
                final int position = vtreeSeq.get(j);
                final VTree current = sdd.getVTree().getVTreeAtPosition(position);
                if (current == null || current.isLeaf()) {
                    continue;
                }
                sdd.pin(node);
                final Map<SddNode, SddNode> translations =
                        SddGlobalTransformations.swap(current.asInternal(), sdd, NopHandler.get()).getResult()
                                .getTranslations();
                final SddNode swapped = translations.get(node);
                sdd.unpin(swapped);
                assert Validation.validVTree(swapped, sdd);
                SddTestUtil.validateMC(swapped, formula, sdd);
                node = swapped;
            }
        }
    }

    @Test
    @LongRunningTag
    public void testFilesSwapManyRandom2() throws IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            for (final List<Integer> vtreeSeq : VTREE_POSISTIONS) {
                final SddCompilationResult result =
                        SddCompilerTopDown.compile(formula, f, NopHandler.get()).getResult();
                final Sdd sdd = result.getSdd();
                SddNode node = result.getNode();
                for (final int position : vtreeSeq) {
                    final VTree current = sdd.getVTree().getVTreeAtPosition(position);
                    if (current == null || current.isLeaf()) {
                        continue;
                    }
                    sdd.pin(node);
                    final Map<SddNode, SddNode> translations =
                            SddGlobalTransformations.swap(current.asInternal(), sdd, NopHandler.get()).getResult()
                                    .getTranslations();
                    final SddNode swapped = translations.get(node);
                    sdd.unpin(swapped);
                    assert Validation.validVTree(swapped, sdd);
                    SddTestUtil.validateMC(swapped, formula, sdd);
                    node = swapped;
                }
            }
        }
    }
}
