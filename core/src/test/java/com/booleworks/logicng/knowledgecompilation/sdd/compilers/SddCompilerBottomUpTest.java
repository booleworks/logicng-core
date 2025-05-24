package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.DecisionVTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.transformations.cnf.CnfSubsumption;
import com.booleworks.logicng.transformations.simplification.BackboneSimplifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class SddCompilerBottomUpTest {
    private final static List<String> FILES = List.of(
            "../test_files/sdd/compile_example2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_1.cnf",
            "../test_files/dnnf/both_bdd_dnnf_2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_3.cnf",
            "../test_files/dnnf/both_bdd_dnnf_4.cnf",
            "../test_files/dnnf/both_bdd_dnnf_5.cnf"
    );

    @Test
    public void test() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sf = Sdd.independent(f);

        final Formula formula = f.parse("(A | C | D) & (A | B)");
        final VTree vTree = new DecisionVTreeGenerator(formula).generate(sf);
        sf.defineVTree(vTree);
        final SddNode node = SddCompilerBottomUp.cnfToSdd(formula, sf, NopHandler.get()).getResult();
        SddTestUtil.validateMC(node, formula, sf);
        SddTestUtil.validateExport(node, formula, sf);
    }

    @Test
    public void testFiles() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        for (final String file : FILES) {
            final Sdd sf = Sdd.independent(f);
            final Formula cnf = simplifyFormula(f, f.and(DimacsReader.readCNF(f, file)));
            final VTree vTree = new DecisionVTreeGenerator(cnf).generate(sf);
            sf.defineVTree(vTree);
            final SddNode node = SddCompilerBottomUp.cnfToSdd(cnf, sf, NopHandler.get()).getResult();
            SddTestUtil.validateMC(node, cnf, sf);
            SddTestUtil.validateExport(node, cnf, sf);
        }
    }

    protected static Formula simplifyFormula(final FormulaFactory f, final Formula formula) {
        final Formula backboneSimplified = formula.transform(new BackboneSimplifier(f));
        return backboneSimplified.transform(new CnfSubsumption(f));
    }
}
