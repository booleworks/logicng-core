package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompilerConfig;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.DecisionVTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddModelEnumerationFunction;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SddSatisfiabilityTest {
    @Test
    public void testEvaluate() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        checkForFormula(f.verum(), f);
        checkForFormula(f.falsum(), f);
        checkForFormula(f.parse("(A & B) | (B & C) | (C & D)"), f);
        checkForFormula(f.parse("(A | B) & (B | C)"), f);
    }

    private static void checkForFormula(final Formula formula, final FormulaFactory f) {
        final Sdd sdd = Sdd.independent(f);
        final Formula cnf = SddTestUtil.encodeWithFactorization(f, formula);
        final Formula cnfNeg = SddTestUtil.encodeWithFactorization(f, formula.negate(f));
        final VTree vtree = new DecisionVTreeGenerator(cnf).generate(sdd);
        if (vtree != null) {
            sdd.defineVTree(vtree);
        }
        final SddCompilerConfig config = SddCompilerConfig.builder()
                .compiler(SddCompilerConfig.Compiler.BOTTOM_UP)
                .sdd(sdd)
                .build();
        final SddNode res = SddCompiler.compile(cnf, config, f).getNode();
        final SddNode neg = SddCompiler.compile(cnfNeg, config, f).getNode();
        final List<Model> models =
                res.execute(SddModelEnumerationFunction.builder(cnf.variables(f), sdd).build());
        final List<Model> negModels =
                neg.execute(SddModelEnumerationFunction.builder(cnf.variables(f), sdd).build());
        for (final Model model : models) {
            assertThat(SddSatisfiability.evaluate(model.toAssignment(), res, sdd)).isTrue();
        }
        for (final Model notModel : negModels) {
            assertThat(SddSatisfiability.evaluate(notModel.toAssignment(), res, sdd)).isFalse();
        }
    }

}
