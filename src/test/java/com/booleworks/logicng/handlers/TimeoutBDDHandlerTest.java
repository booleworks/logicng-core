// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.handlers;

import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.BDD_COMPUTATION_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.BDD_NEW_REF_ADDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.knowledgecompilation.bdds.BDD;
import com.booleworks.logicng.knowledgecompilation.bdds.BDDFactory;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import com.booleworks.logicng.knowledgecompilation.bdds.orderings.BFSOrdering;
import com.booleworks.logicng.knowledgecompilation.bdds.orderings.VariableOrderingProvider;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class TimeoutBDDHandlerTest {

    private FormulaFactory f;
    private PigeonHoleGenerator pg;

    @BeforeEach
    public void init() {
        f = FormulaFactory.caching();
        pg = new PigeonHoleGenerator(f);
    }

    @Test
    public void testNewRefAdded() throws InterruptedException {
        final TimeoutHandler handler = new TimeoutHandler(100, TimeoutHandler.TimerType.SINGLE_TIMEOUT);
        handler.shouldResume(BDD_COMPUTATION_STARTED);
        assertThat(handler.shouldResume(BDD_NEW_REF_ADDED)).isTrue();
        Thread.sleep(200);
        assertThat(handler.shouldResume(BDD_NEW_REF_ADDED)).isFalse();
    }

    @Test
    public void testThatMethodsAreCalled() throws ParserException {
        final Formula formula = f.parse("(A => ~B) & ((A & C) | (D & ~C)) & (A | Y | X)");
        final VariableOrderingProvider provider = new BFSOrdering();
        final BDDKernel kernel = new BDDKernel(f, provider.getOrder(f, formula), 100, 100);
        final TimeoutHandler handler = Mockito.mock(TimeoutHandler.class);
        when(handler.shouldResume(any())).thenReturn(true);

        BDDFactory.build(f, formula, kernel, handler);
        verify(handler, times(1)).shouldResume(eq(BDD_COMPUTATION_STARTED));
        verify(handler, atLeast(1)).shouldResume(eq(BDD_NEW_REF_ADDED));
    }

    @Test
    public void testThatNewRefAddedHandledProperly() throws ParserException {
        final Formula formula = f.parse("(A => ~B) & ((A & C) | ~(D & ~C)) & (A | Y | X)");
        final VariableOrderingProvider provider = new BFSOrdering();
        final BDDKernel kernel = new BDDKernel(f, provider.getOrder(f, formula), 100, 100);
        final ComputationHandler handler = Mockito.mock(ComputationHandler.class);
        final AtomicInteger count = new AtomicInteger(0);
        when(handler.shouldResume(eq(BDD_COMPUTATION_STARTED))).thenReturn(true);
        when(handler.shouldResume(eq(BDD_NEW_REF_ADDED))).thenAnswer(invocationOnMock -> count.addAndGet(1) < 5);

        final LNGResult<BDD> result = BDDFactory.build(f, formula, kernel, handler);
        assertThat(result.isSuccess()).isFalse();
        verify(handler, times(1)).shouldResume(eq(BDD_COMPUTATION_STARTED));
        verify(handler, times(5)).shouldResume(eq(BDD_NEW_REF_ADDED));
    }

    @Test
    public void testTimeoutHandlerSingleTimeout() {
        final Formula formula = pg.generate(10);
        final VariableOrderingProvider provider = new BFSOrdering();
        final BDDKernel kernel = new BDDKernel(f, provider.getOrder(f, formula), 100, 100);
        final TimeoutHandler handler = new TimeoutHandler(100L);

        final LNGResult<BDD> result = BDDFactory.build(f, formula, kernel, handler);
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void testTimeoutHandlerFixedEnd() {
        final Formula formula = pg.generate(10);
        final VariableOrderingProvider provider = new BFSOrdering();
        final BDDKernel kernel = new BDDKernel(f, provider.getOrder(f, formula), 100, 100);
        final TimeoutHandler handler =
                new TimeoutHandler(System.currentTimeMillis() + 100L, TimeoutHandler.TimerType.FIXED_END);

        final LNGResult<BDD> result = BDDFactory.build(f, formula, kernel, handler);
        assertThat(result.isSuccess()).isFalse();
    }
}
