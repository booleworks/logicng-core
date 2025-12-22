// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.sdd;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NumberOfModelsHandler;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.SddEvaluation;
import com.booleworks.logicng.knowledgecompilation.sdd.algorithms.VTreeUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddVariableProxy;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddExportFormulaFunction;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddModelCountFunction;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddModelEnumerationFunction;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddVariablesFunction;
import com.booleworks.logicng.modelcounting.ModelCounter;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.transformations.PureExpansionTransformation;
import com.booleworks.logicng.transformations.cnf.CnfConfig;
import com.booleworks.logicng.transformations.cnf.CnfEncoder;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SddTestUtil {
    public static void validateMC(final SddNode node, final Formula originalFormula, final Sdd sdd) {
        final BigInteger models =
                new SddModelCountFunction(originalFormula.variables(sdd.getFactory()), sdd).execute(node);
        final BigInteger expected = ModelCounter.count(sdd.getFactory(), List.of(originalFormula),
                originalFormula.variables(sdd.getFactory()));
        assertThat(models).isEqualTo(expected);
    }

    public static void validateExport(final SddNode node, final Formula originalFormula, final Sdd sdd) {
        final Formula exported = node.execute(new SddExportFormulaFunction(sdd));
        assertThat(sdd.getFactory().equivalence(originalFormula, exported).isTautology(sdd.getFactory())).isTrue();
    }

    public static void sampleModels(final SddNode node, final Formula originalFormula, final Sdd sdd,
                                    final int samples) {
        final Collection<Variable> sddVariables = node.execute(new SddVariablesFunction(sdd));
        final Collection<Variable> formulaVariables = originalFormula.variables(sdd.getFactory());
        final SddModelEnumerationFunction meFunc =
                SddModelEnumerationFunction.builder(sdd, sddVariables).additionalVariables(formulaVariables).build();
        final ModelEnumerationFunction meSolverFunc =
                ModelEnumerationFunction.builder(formulaVariables).build();
        final LngResult<List<Model>> models1 = node.execute(meFunc, new NumberOfModelsHandler(samples));
        final SatSolver solver = SatSolver.newSolver(sdd.getFactory());
        solver.add(originalFormula);
        final LngResult<List<Model>> models2 = solver.execute(meSolverFunc, new NumberOfModelsHandler(samples));
        for (final Model model : models1.getPartialResult()) {
            assertThat(originalFormula.evaluate(model.toAssignment())).isTrue();
        }
        for (final Model model : models2.getPartialResult()) {
            assertThat(SddEvaluation.evaluate(sdd, model.toAssignment(), node)).isTrue();
        }
    }

    public static Formula encodeAsPureCnf(final FormulaFactory f, final Formula formula) {
        final PureExpansionTransformation expander = new PureExpansionTransformation(f);
        final Formula expandedFormula = formula.transform(expander);

        final CnfConfig cnfConfig = CnfConfig.builder()
                .algorithm(CnfConfig.Algorithm.ADVANCED)
                .fallbackAlgorithmForAdvancedEncoding(CnfConfig.Algorithm.TSEITIN).build();

        return CnfEncoder.encode(f, expandedFormula, cnfConfig);
    }

    public static Formula encodeWithFactorization(final FormulaFactory f, final Formula formula) {
        final PureExpansionTransformation expander = new PureExpansionTransformation(f);
        final Formula expandedFormula = formula.transform(expander);

        final CnfConfig cnfConfig = CnfConfig.builder()
                .algorithm(CnfConfig.Algorithm.FACTORIZATION)
                .build();

        return CnfEncoder.encode(f, expandedFormula, cnfConfig);
    }

    public static boolean isCompleteVTree(final FormulaFactory f, final VTree vtree,
                                          final Collection<Variable> expectedVars, final SddVariableProxy proxy) {
        final Set<Variable> vtreeVars = VTreeUtil.vars(vtree, new HashSet<>())
                .stream()
                .map(idx -> proxy.indexToVariable(f, idx))
                .collect(Collectors.toSet());
        return vtreeVars.containsAll(expectedVars) && expectedVars.containsAll(vtreeVars);
    }
}
