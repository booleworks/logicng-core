// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static com.booleworks.logicng.util.CollectionHelper.union;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.events.SimpleEvent;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.ModelCountingFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.DefaultModelEnumerationStrategy;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.SortedSet;
import java.util.TreeSet;

public class ModelEnumerationHandlerTest {

    FormulaFactory f;

    @BeforeEach
    public void init() {
        f = FormulaFactory.caching();
    }

    @Test
    public void testTimeoutHandler() throws ParserException, InterruptedException {
        final SatSolver solver = SatSolver.newSolver(f);
        final Formula formula =
                f.parse("A | B | C | D | E | F | G | H | I | J | K | L | N | M | O | P | Q | R | S | T | U | V | W");
        solver.add(formula);
        final TimeoutHandler handler = new TimeoutHandler(100);
        final ModelCountingFunction enumeration = ModelCountingFunction.builder(formula.variables(f))
                .configuration(ModelEnumerationConfig.builder().build())
                .build();
        Thread.sleep(150);
        assertThat(handler.shouldResume(SimpleEvent.NO_EVENT)).isTrue();

        final long start = System.currentTimeMillis();
        final LngResult<BigInteger> result = solver.execute(enumeration, handler);
        final long finish = System.currentTimeMillis();
        final long timeElapsed = finish - start;

        // Should be very unlikely that the formula can be fully enumerated in
        // 100ms. Thus, we expect the handler to stop the execution.
        assertThat(result.isSuccess()).isFalse();
        assertThat(timeElapsed).isGreaterThanOrEqualTo(100L);
    }

    @Test
    public void testNumberOfModelsHandler() throws ParserException {
        final Formula formula =
                f.parse("A | B | C | D | E | F | G | H | I | J | K | L | N | M | O | P | Q | R | S | T | U | V | W");
        final SortedSet<Variable> vars = union(formula.variables(f), f.variables("X", "Y", "Z"), TreeSet::new);
        for (int i = 1; i <= 1000; i += 7) {
            final SatSolver solver = SatSolver.newSolver(f);
            solver.add(formula);
            final NumberOfModelsHandler handler = new NumberOfModelsHandler(i);
            final ModelCountingFunction enumeration = ModelCountingFunction.builder(vars)
                    .configuration(ModelEnumerationConfig.builder()
                            .strategy(DefaultModelEnumerationStrategy.builder().maxNumberOfModels(200).build())
                            .build()
                    ).build();
            final LngResult<BigInteger> numberOfModels = solver.execute(enumeration, handler);
            assertThat(numberOfModels.isSuccess()).isFalse();
            assertThat(numberOfModels.isPartial()).isTrue();
            assertThat(numberOfModels.getPartialResult().longValueExact()).isLessThanOrEqualTo(i + 8); // because of 3 dont cares
        }
    }
}
