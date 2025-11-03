package com.booleworks.logicng.csp.encodings;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ExampleFormulas;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.handlers.CspEvent;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

public class HandlerTest extends ParameterizedCspTest {
    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testEvents(final CspFactory cf) throws ParserException, IOException {
        final CounterHandler handler = new CounterHandler();

        final Formula formula = ExampleFormulas.arithmJavaCreamSolver(cf);
        final Csp csp = cf.buildCsp(formula);
        final CspEncodingContext context = CspEncodingContext.order();
        cf.encodeCsp(csp, context, handler);
        assertThat(handler.started_counter).isEqualTo(1);
        assertThat(handler.clauses_counter).isGreaterThan(0);
        assertThat(handler.vars_counter).isGreaterThan(0);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testInterrupt(final CspFactory cf) throws ParserException, IOException {
        final InterruptHandler handler1 = new InterruptHandler();
        final InterruptHandler handler2 = new InterruptHandler();
        final IntegerVariable v1 = cf.variable("v1", 0, 10);
        final IntegerVariable v2 = cf.variable("v2", 0, 11);
        final CspEncodingContext context = CspEncodingContext.order();

        assertThat(cf.encodeVariable(v1, context, handler1).isSuccess()).isTrue();
        assertThat(cf.encodeVariable(v2, context, handler2).isSuccess()).isFalse();
    }

    private static class CounterHandler implements ComputationHandler {
        int started_counter = 0;
        int vars_counter = 0;
        int clauses_counter = 0;

        @Override
        public boolean shouldResume(final LngEvent event) {
            if (event == CspEvent.CSP_ENCODING_STARTED) {
                started_counter++;
            } else if (event == CspEvent.CSP_ENCODING_VAR_CREATED) {
                vars_counter++;
            } else if (event == CspEvent.CSP_ENCODING_CLAUSE_CREATED) {
                clauses_counter++;
            }
            return true;
        }

    }

    private static class InterruptHandler implements ComputationHandler {
        int vars_counter = 0;

        @Override
        public boolean shouldResume(final LngEvent event) {
            if (event == CspEvent.CSP_ENCODING_VAR_CREATED) {
                vars_counter++;
            }
            return vars_counter <= 10;
        }

    }
}
