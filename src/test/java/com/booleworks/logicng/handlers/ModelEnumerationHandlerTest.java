// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static com.booleworks.logicng.util.CollectionHelper.union;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MiniSat;
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
        final MiniSat solver = MiniSat.miniSat(f);
        solver.add(f.parse("A | B | C | D | E | F | G | H | I | J | K | L | N | M | O | P | Q | R | S | T | U | V | W"));
        final TimeoutModelEnumerationHandler handler = new TimeoutModelEnumerationHandler(100);
        final ModelCountingFunction enumeration = ModelCountingFunction.builder().configuration(ModelEnumerationConfig.builder().handler(handler).build()).build();

        Thread.sleep(150);
        assertThat(handler.aborted()).isFalse();

        final long start = System.currentTimeMillis();
        solver.execute(enumeration);
        final long finish = System.currentTimeMillis();
        final long timeElapsed = finish - start;

        // Should be very unlikely that the formula can be fully enumerated in 100ms.
        // Thus, we expect the handler to stop the execution.
        assertThat(handler.aborted()).isTrue();
        assertThat(timeElapsed).isGreaterThanOrEqualTo(100L);
    }

    @Test
    public void testNumberOfModelsHandler() throws ParserException {
        final Formula formula = f.parse("A | B | C | D | E | F | G | H | I | J | K | L | N | M | O | P | Q | R | S | T | U | V | W");
        final SortedSet<Variable> vars = union(formula.variables(f), f.variables("X", "Y", "Z"), TreeSet::new);
        for (int i = 1; i <= 1000; i += 7) {
            final MiniSat solver = MiniSat.miniSat(f);
            solver.add(formula);
            final NumberOfModelsHandler handler = new NumberOfModelsHandler(i);
            final ModelCountingFunction enumeration = ModelCountingFunction.builder()
                    .variables(vars).configuration(ModelEnumerationConfig.builder().handler(handler).strategy(
                                    DefaultModelEnumerationStrategy.builder().maxNumberOfModels(200).build()
                            ).build()
                    ).build();
            final BigInteger numberOfModels = solver.execute(enumeration);
            assertThat(handler.aborted()).isTrue();
            assertThat(numberOfModels.longValueExact()).isLessThanOrEqualTo(i);
        }
    }
}
