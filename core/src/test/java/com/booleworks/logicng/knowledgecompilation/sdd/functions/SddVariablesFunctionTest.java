// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.backbones.BackboneGeneration;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompiler;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class SddVariablesFunctionTest {
    @Test
    public void testSimple() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = SddTestUtil.encodeAsPureCnf(f, f.parse("(A & B) | (B & C) | (C & D)"));
        final SddCompilationResult res = SddCompiler.compile(f, formula);
        final Sdd sdd = res.getSdd();
        final SddNode node = res.getNode();
        final Set<Variable> vars = node.execute(new SddVariablesFunction(sdd));
        assertThat(vars).containsExactlyInAnyOrderElementsOf(f.variables("A", "B", "C", "D"));
    }

    private final static List<String> FILES = List.of(
            "../test_files/sdd/compile_example1.cnf",
            "../test_files/sdd/compile_example2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_1.cnf",
            "../test_files/dnnf/both_bdd_dnnf_2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_3.cnf",
            "../test_files/dnnf/both_bdd_dnnf_4.cnf",
            "../test_files/dnnf/both_bdd_dnnf_5.cnf"
    );

    @Test
    public void testFiles() throws IOException {
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult res = SddCompiler.compile(f, formula);
            final Sdd sdd = res.getSdd();
            final SddNode node = res.getNode();
            final Set<Variable> vars = node.execute(new SddVariablesFunction(sdd));
            final Backbone backbone = BackboneGeneration.compute(f, formula);
            assertThat(vars).containsAll(backbone.getNegativeBackbone());
            assertThat(vars).containsAll(backbone.getPositiveBackbone());
            assertThat(vars).isSubsetOf(formula.variables(f));
        }
    }
}
