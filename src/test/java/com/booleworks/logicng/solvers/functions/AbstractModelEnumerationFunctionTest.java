// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.solvers.functions.modelenumeration.TestVariableProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class AbstractModelEnumerationFunctionTest {

    private FormulaFactory f;

    @BeforeEach
    public void init() {
        f = FormulaFactory.caching();
    }

    @Test
    @RandomTag
    public void testEmptySplitVariables() throws ParserException {
        final Formula formula = f.parse("A | B | C | D | E");

        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(formula);

        final ModelEnumerationConfig config = ModelEnumerationConfig.builder()
                .strategy(DefaultModelEnumerationStrategy.builder()
                        .splitVariableProvider(new TestVariableProvider.EmptyVariableProvider())
                        .maxNumberOfModels(5)
                        .build())
                .build();
        final List<Model> models =
                solver.execute(ModelEnumerationFunction.builder(formula.variables(f)).configuration(config).build());
        assertThat(models.size()).isEqualTo(31);
    }

    @Test
    @RandomTag
    public void testNullSplitVariables() throws ParserException {
        final Formula formula = f.parse("A | B | C | D | E");

        final SATSolver solver = SATSolver.newSolver(f);
        solver.add(formula);

        final ModelEnumerationConfig config = ModelEnumerationConfig.builder()
                .strategy(DefaultModelEnumerationStrategy.builder()
                        .splitVariableProvider(new TestVariableProvider.NullVariableProvider())
                        .maxNumberOfModels(5)
                        .build())
                .build();
        final List<Model> models =
                solver.execute(ModelEnumerationFunction.builder(formula.variables(f)).configuration(config).build());
        assertThat(models.size()).isEqualTo(31);
    }
}
