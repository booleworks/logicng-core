// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

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
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtreegeneration.DecisionVTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddModelEnumerationFunction;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SddEvaluationTest {
    @Test
    public void testEvaluate() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        checkForFormula(f.verum(), f);
        checkForFormula(f.falsum(), f);
        checkForFormula(f.parse("(A & B) | (B & C) | (C & D)"), f);
        checkForFormula(f.parse("(A | B) & (B | C)"), f);
    }

    private static void checkForFormula(final Formula formula, final FormulaFactory f) {
        final VTreeRoot.Builder builder = VTreeRoot.builder();
        final Formula cnf = SddTestUtil.encodeWithFactorization(f, formula);
        final Formula cnfNeg = SddTestUtil.encodeWithFactorization(f, formula.negate(f));
        final VTree vtree = new DecisionVTreeGenerator(cnf).generate(builder);
        final Sdd sdd = new Sdd(f, builder.build(vtree));
        final SddCompilerConfig config = SddCompilerConfig.builder()
                .compiler(SddCompilerConfig.Compiler.BOTTOM_UP)
                .sdd(sdd)
                .build();
        final SddNode res = SddCompiler.compile(f, cnf, config).getNode();
        final SddNode neg = SddCompiler.compile(f, cnfNeg, config).getNode();
        final List<Model> models =
                res.execute(SddModelEnumerationFunction.builder(sdd, cnf.variables(f)).build());
        final List<Model> negModels =
                neg.execute(SddModelEnumerationFunction.builder(sdd, cnf.variables(f)).build());
        for (final Model model : models) {
            assertThat(SddEvaluation.evaluate(sdd, model.toAssignment(), res)).isTrue();
        }
        for (final Model notModel : negModels) {
            assertThat(SddEvaluation.evaluate(sdd, notModel.toAssignment(), res)).isFalse();
        }
    }
}
