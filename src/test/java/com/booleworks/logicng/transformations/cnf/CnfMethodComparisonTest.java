// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.transformations.cnf;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CnfMethodComparisonTest {

    public static Collection<Object[]> cnfConfigurations() {
        final List<Object[]> configs = new ArrayList<>();
        configs.add(new Object[]{CNFConfig.builder().algorithm(CNFConfig.Algorithm.PLAISTED_GREENBAUM).atomBoundary(12).build(),
                MiniSatConfig.CNFMethod.FACTORY_CNF});
        configs.add(new Object[]{CNFConfig.builder().algorithm(CNFConfig.Algorithm.PLAISTED_GREENBAUM).atomBoundary(0).build(),
                MiniSatConfig.CNFMethod.FACTORY_CNF});
        configs.add(new Object[]{CNFConfig.builder().algorithm(CNFConfig.Algorithm.TSEITIN).atomBoundary(0).build(),
                MiniSatConfig.CNFMethod.FACTORY_CNF});
        configs.add(new Object[]{CNFConfig.builder().algorithm(CNFConfig.Algorithm.TSEITIN).atomBoundary(12).build(),
                MiniSatConfig.CNFMethod.FACTORY_CNF});
        configs.add(new Object[]{CNFConfig.builder()
                .algorithm(CNFConfig.Algorithm.ADVANCED).fallbackAlgorithmForAdvancedEncoding(CNFConfig.Algorithm.PLAISTED_GREENBAUM).build(),
                MiniSatConfig.CNFMethod.FACTORY_CNF});
        configs.add(new Object[]{CNFConfig.builder()
                .algorithm(CNFConfig.Algorithm.ADVANCED).fallbackAlgorithmForAdvancedEncoding(CNFConfig.Algorithm.TSEITIN).build(),
                MiniSatConfig.CNFMethod.FACTORY_CNF});
        configs.add(new Object[]{CNFConfig.builder().build(),
                MiniSatConfig.CNFMethod.PG_ON_SOLVER});
        configs.add(new Object[]{CNFConfig.builder().build(),
                MiniSatConfig.CNFMethod.FULL_PG_ON_SOLVER});
        return configs;
    }

    @ParameterizedTest
    @MethodSource("cnfConfigurations")
    @LongRunningTag
    public void compareFullBackbonesOnLargeFormulas(final CNFConfig cnfConfig, final MiniSatConfig.CNFMethod cnfMethod) throws IOException, ParserException {
        final String baseDir = "src/test/resources/formulas/";
        final List<String> fileNames = Arrays.asList("formula1.txt", "formula2.txt", "formula3.txt", "large_formula.txt", "small_formulas.txt");
        for (final String fileName : fileNames) {
            final String filePath = baseDir + fileName;
            final Backbone backboneReference = computeBackbone(filePath, CNFConfig.builder().build(), MiniSatConfig.builder().build().getCnfMethod());
            final Backbone backbone = computeBackbone(filePath, cnfConfig, cnfMethod);
            assertThat(backboneReference).isEqualTo(backbone);
        }
    }

    @Test
    @LongRunningTag
    public void compareBackbonesForVariablesOnLargeFormulas() throws IOException, ParserException {
        compareBackbonePerVariable("src/test/resources/formulas/formula1.txt");
        compareBackbonePerVariable("src/test/resources/formulas/large_formula.txt");
        compareBackbonePerVariable("src/test/resources/formulas/small_formulas.txt");
    }

    private Backbone computeBackbone(final String fileName, final CNFConfig cnfConfig, final MiniSatConfig.CNFMethod cnfMethod) throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        f.putConfiguration(cnfConfig);
        final Formula formula = FormulaReader.readPropositionalFormula(f, fileName);
        final SATSolver solver = MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(cnfMethod).build());
        solver.add(formula);
        return solver.backbone(formula.variables(f));
    }

    private void compareBackbonePerVariable(final String fileName) throws IOException, ParserException {
        final Map<Variable, Backbone> backboneFactory = computeBackbonePerVariable(fileName,
                CNFConfig.builder().algorithm(CNFConfig.Algorithm.ADVANCED).fallbackAlgorithmForAdvancedEncoding(CNFConfig.Algorithm.TSEITIN).build(),
                MiniSatConfig.CNFMethod.FACTORY_CNF);
        final Map<Variable, Backbone> backbonePg = computeBackbonePerVariable(fileName, CNFConfig.builder().build(), MiniSatConfig.CNFMethod.PG_ON_SOLVER);
        final Map<Variable, Backbone> backboneFullPg = computeBackbonePerVariable(fileName, CNFConfig.builder().build(), MiniSatConfig.CNFMethod.FULL_PG_ON_SOLVER);
        assertThat(backboneFactory).isEqualTo(backbonePg);
        assertThat(backboneFactory).isEqualTo(backboneFullPg);
    }

    private Map<Variable, Backbone> computeBackbonePerVariable(final String fileName, final CNFConfig cnfConfig, final MiniSatConfig.CNFMethod cnfMethod)
            throws IOException, ParserException {
        final long start = System.currentTimeMillis();
        final FormulaFactory f = FormulaFactory.caching();
        f.putConfiguration(cnfConfig);
        final Formula formula = FormulaReader.readPropositionalFormula(f, fileName);
        final SATSolver solver = MiniSat.miniSat(f, MiniSatConfig.builder().cnfMethod(cnfMethod).build());
        solver.add(formula);
        final SolverState solverState = solver.saveState();
        final Map<Variable, Backbone> result = new TreeMap<>();
        int counter = 1000;
        for (final Variable variable : formula.variables(f)) {
            if (counter-- > 0) {
                solver.add(variable);
                if (solver.satCall().sat() == Tristate.TRUE) {
                    final Backbone backbone = solver.backbone(formula.variables(f));
                    result.put(variable, backbone);
                }
                solver.loadState(solverState);
            }
        }
        final long stop = System.currentTimeMillis();
        System.out.println(fileName + " " + cnfConfig.algorithm + " " + cnfConfig.fallbackAlgorithmForAdvancedEncoding + " " + cnfMethod + ": " + (stop - start) + " ms.");
        return result;
    }

}
