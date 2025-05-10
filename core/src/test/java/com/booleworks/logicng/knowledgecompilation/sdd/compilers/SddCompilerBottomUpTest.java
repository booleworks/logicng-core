package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.BalancedVTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class SddCompilerBottomUpTest {
    private final static List<String> FILES = List.of(
            "../test_files/sdd/big-swap.cnf",
            "../test_files/sdd/compile_example1.cnf",
            "../test_files/sdd/compile_example2.cnf"
            //"../test_files/dnnf/both_bdd_dnnf_1.cnf"
            //"../test_files/dnnf/both_bdd_dnnf_2.cnf"
            //"../test_files/dnnf/both_bdd_dnnf_3.cnf"
            //"../test_files/dnnf/both_bdd_dnnf_4.cnf",
            //"../test_files/dnnf/both_bdd_dnnf_5.cnf"
    );

    @Test
    public void test() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sf = Sdd.independent(f);

        final Formula formula = f.parse("(A | C | D) & (A | B)");
        final VTree vTree = new BalancedVTreeGenerator(formula.variables(f)).generate(sf);
        final VTreeRoot root = sf.constructRoot(vTree);
        final SddNode node = SddCompilerBottomUp.cnfToSdd(formula, root, sf, NopHandler.get()).getResult();
        SddTestUtil.validateMC(node, root, formula, sf);
        SddTestUtil.validateExport(node, formula, sf);
    }

    @Test
    public void testFiles() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        for (final String file : FILES) {
            final Sdd sf = Sdd.independent(f);
            final Formula cnf = f.and(DimacsReader.readCNF(f, file));
            final VTree vTree = new BalancedVTreeGenerator(cnf.variables(f)).generate(sf);
            final VTreeRoot root = sf.constructRoot(vTree);
            final SddNode node = SddCompilerBottomUp.cnfToSdd(cnf, root, sf, NopHandler.get()).getResult();
            SddTestUtil.validateMC(node, root, cnf, sf);
            SddTestUtil.validateExport(node, cnf, sf);
        }
    }
}
