// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds;

import static com.booleworks.logicng.TestWithExampleFormulas.parse;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.NumberOfNodesBddHandler;
import com.booleworks.logicng.handlers.TimeoutHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;
import com.booleworks.logicng.testutils.NQueensGenerator;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

public class LargeBddTest {

    @Test
    public void testPigeonHole() {
        final FormulaFactory f = FormulaFactory.caching();
        final PigeonHoleGenerator generator = new PigeonHoleGenerator(f);
        testPigeonHole(f, generator, 2);
        testPigeonHole(f, generator, 3);
        testPigeonHole(f, generator, 4);
        testPigeonHole(f, generator, 5);
        testPigeonHole(f, generator, 6);
        testPigeonHole(f, generator, 7);
        testPigeonHole(f, generator, 8);
        testPigeonHole(f, generator, 9);
    }

    private void testPigeonHole(final FormulaFactory f, final PigeonHoleGenerator generator, final int size) {
        final Formula pigeon = generator.generate(size);
        final BddKernel kernel = new BddKernel(f, pigeon.variables(f).size(), 10000, 10000);
        final Bdd bdd = BddFactory.build(f, pigeon, kernel);
        assertThat(bdd.isContradiction()).isTrue();
    }

    @Test
    public void testQueens() {
        final FormulaFactory f = FormulaFactory.caching();
        final NQueensGenerator generator = new NQueensGenerator(f);
        testQueens(f, generator, 4, 2);
        testQueens(f, generator, 5, 10);
        testQueens(f, generator, 6, 4);
        testQueens(f, generator, 7, 40);
        testQueens(f, generator, 8, 92);
    }

    private void testQueens(final FormulaFactory f, final NQueensGenerator generator, final int size,
                            final int models) {
        final Formula queens = generator.generate(size);
        final BddKernel kernel = new BddKernel(f, queens.variables(f).size(), 10000, 10000);
        final Bdd bdd = BddFactory.build(f, queens, kernel);
        final Formula cnf = bdd.cnf();
        assertThat(cnf.isCnf(f)).isTrue();
        final Bdd cnfBdd = BddFactory.build(f, cnf, kernel);
        assertThat(cnfBdd).isEqualTo(bdd);
        assertThat(bdd.support()).isEqualTo(queens.variables(f));
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.valueOf(models));
    }

    @Test
    public void testTimeoutHandlerNgSmall() {
        final FormulaFactory f = FormulaFactory.caching();
        final NQueensGenerator generator = new NQueensGenerator(f);
        final Formula queens = generator.generate(4);
        final BddKernel kernel = new BddKernel(f, queens.variables(f).size(), 10000, 10000);
        final TimeoutHandler handler = new TimeoutHandler(2000L);
        final LngResult<Bdd> bdd = BddFactory.build(f, queens, kernel, handler);
        assertThat(bdd.isSuccess()).isTrue();
        assertThat(BddKernel.isAborted(bdd.getResult().getIndex())).isFalse();
    }

    @Test
    @LongRunningTag
    public void testTimeoutHandlerNgLarge() {
        final FormulaFactory f = FormulaFactory.caching();
        final NQueensGenerator generator = new NQueensGenerator(f);
        final Formula queens = generator.generate(10);
        final BddKernel kernel = new BddKernel(f, queens.variables(f).size(), 10000, 10000);
        final TimeoutHandler handler = new TimeoutHandler(1000L);
        final LngResult<Bdd> bdd = BddFactory.build(f, queens, kernel, handler);
        assertThat(bdd.isSuccess()).isFalse();
    }

    @Test
    public void testNumberOfNodesHandlerSmall() {
        final FormulaFactory f = FormulaFactory.caching();
        final NQueensGenerator generator = new NQueensGenerator(f);
        final Formula queens = generator.generate(4);
        final BddKernel kernel = new BddKernel(f, queens.variables(f).size(), 10000, 10000);
        final NumberOfNodesBddHandler handler = new NumberOfNodesBddHandler(1000);
        final LngResult<Bdd> bdd = BddFactory.build(f, queens, kernel, handler);
        assertThat(bdd.isSuccess()).isTrue();
        assertThat(BddKernel.isAborted(bdd.getResult().getIndex())).isFalse();
    }

    @Test
    public void testNumberOfNodesHandlerLarge() {
        final FormulaFactory f = FormulaFactory.caching();
        final NQueensGenerator generator = new NQueensGenerator(f);
        final Formula queens = generator.generate(10);
        final BddKernel kernel = new BddKernel(f, queens.variables(f).size(), 10000, 10000);
        final NumberOfNodesBddHandler handler = new NumberOfNodesBddHandler(5);
        final LngResult<Bdd> bdd = BddFactory.build(f, queens, kernel, handler);
        assertThat(bdd.isSuccess()).isFalse();
    }

    @Test
    public void testNumberOfNodesHandler() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = parse(f, "A <=> ~(B => C & F & G & ~H | A & D & ~E)");
        final BddKernel kernel = new BddKernel(f, formula.variables(f).size(), 10000, 10000);
        final NumberOfNodesBddHandler handler = new NumberOfNodesBddHandler(5);
        final LngResult<Bdd> bdd = BddFactory.build(f, formula, kernel, handler);
        assertThat(bdd.isSuccess()).isFalse();
    }
}
