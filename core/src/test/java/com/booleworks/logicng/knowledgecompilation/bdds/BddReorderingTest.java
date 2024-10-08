// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.knowledgecompilation.bdds.datastructures.BddConstant;
import com.booleworks.logicng.knowledgecompilation.bdds.datastructures.BddInnerNode;
import com.booleworks.logicng.knowledgecompilation.bdds.functions.LngBddFunction;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDVerification;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddOperations;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddReordering;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddReorderingMethod;
import com.booleworks.logicng.predicates.satisfiability.SatPredicate;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BddReorderingTest extends TestWithFormulaContext {

    private final SwapStats stats = new SwapStats();
    private static final List<BddReorderingMethod> REORDER_METHODS =
            List.of(BddReorderingMethod.BDD_REORDER_WIN2, BddReorderingMethod.BDD_REORDER_WIN2ITE,
                    BddReorderingMethod.BDD_REORDER_WIN3, BddReorderingMethod.BDD_REORDER_WIN3ITE,
                    BddReorderingMethod.BDD_REORDER_SIFT,
                    BddReorderingMethod.BDD_REORDER_SIFTITE, BddReorderingMethod.BDD_REORDER_RANDOM);

    @ParameterizedTest
    @MethodSource("contexts")
    public void testExceptionalBehavior(final FormulaContext _c) {
        assertThatThrownBy(() -> {
            final BddKernel kernel = new BddKernel(_c.f, List.of(_c.a, _c.b), 100, 100);
            final BddReordering reordering = new BddReordering(kernel);
            final Formula formula = _c.f.parse("a | b");
            BddFactory.build(_c.f, formula, kernel);
            reordering.swapVariables(0, 2);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown variable number: " + 2);
        assertThatThrownBy(() -> {
            final BddKernel kernel = new BddKernel(_c.f, List.of(_c.a, _c.b), 100, 100);
            final BddReordering reordering = new BddReordering(kernel);
            final Formula formula = _c.f.parse("a | b");
            BddFactory.build(_c.f, formula, kernel);
            reordering.swapVariables(3, 0);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown variable number: " + 3);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSwapping(final FormulaContext _c) throws ParserException {
        final BddKernel kernel = new BddKernel(_c.f, List.of(_c.a, _c.b, _c.c), 100, 100);
        final Formula formula = _c.f.parse("a | b | c");
        final Bdd bdd = BddFactory.build(_c.f, formula, kernel);
        assertThat(bdd.getVariableOrder()).containsExactly(_c.a, _c.b, _c.c);
        bdd.swapVariables(_c.a, _c.b);
        assertThat(bdd.getVariableOrder()).containsExactly(_c.b, _c.a, _c.c);
        bdd.swapVariables(_c.a, _c.b);
        assertThat(bdd.getVariableOrder()).containsExactly(_c.a, _c.b, _c.c);
        bdd.swapVariables(_c.a, _c.a);
        assertThat(bdd.getVariableOrder()).containsExactly(_c.a, _c.b, _c.c);
        bdd.swapVariables(_c.a, _c.c);
        assertThat(bdd.getVariableOrder()).containsExactly(_c.c, _c.b, _c.a);
        bdd.swapVariables(_c.b, _c.c);
        assertThat(bdd.getVariableOrder()).containsExactly(_c.b, _c.c, _c.a);
        assertThat(_c.f.equivalence(formula, bdd.cnf()).holds(new TautologyPredicate(_c.f))).isTrue();
        assertThat(bdd.apply(new LngBddFunction(_c.f))).isEqualTo(
                new BddInnerNode(_c.b,
                        new BddInnerNode(_c.c,
                                new BddInnerNode(_c.a, BddConstant.getFalsumNode(_c.f), BddConstant.getVerumNode(_c.f)),
                                BddConstant.getVerumNode(_c.f)),
                        BddConstant.getVerumNode(_c.f)));
        assertThatThrownBy(() -> bdd.swapVariables(_c.b, _c.x)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSwappingMultipleBdds(final FormulaContext _c) throws ParserException {
        final BddKernel kernel = new BddKernel(_c.f, List.of(_c.a, _c.b, _c.c), 100, 100);
        final Formula formula1 = _c.f.parse("a | b | c");
        final Formula formula2 = _c.f.parse("a & b");
        final Bdd bdd1 = BddFactory.build(_c.f, formula1, kernel);
        final Bdd bdd2 = BddFactory.build(_c.f, formula2, kernel);
        assertThat(bdd1.getVariableOrder()).containsExactly(_c.a, _c.b, _c.c);
        assertThat(bdd2.getVariableOrder()).containsExactly(_c.a, _c.b, _c.c);
        assertThat(bdd2.apply(new LngBddFunction(_c.f))).isEqualTo(
                new BddInnerNode(_c.a, BddConstant.getFalsumNode(_c.f),
                        new BddInnerNode(_c.b, BddConstant.getFalsumNode(_c.f), BddConstant.getVerumNode(_c.f))));
        bdd1.swapVariables(_c.a, _c.b);
        assertThat(bdd1.getVariableOrder()).containsExactly(_c.b, _c.a, _c.c);
        assertThat(bdd2.getVariableOrder()).containsExactly(_c.b, _c.a, _c.c);
        assertThat(bdd2.apply(new LngBddFunction(_c.f))).isEqualTo(
                new BddInnerNode(_c.b, BddConstant.getFalsumNode(_c.f),
                        new BddInnerNode(_c.a, BddConstant.getFalsumNode(_c.f), BddConstant.getVerumNode(_c.f))));
    }

    @Test
    @RandomTag
    public void testRandomReorderingQuick() {
        testRandomReordering(25, 30, false);
    }

    @Test
    @LongRunningTag
    public void testRandomReorderingLongRunning() {
        testRandomReordering(25, 50, false);
    }

    @Test
    public void testReorderOnBuildQuick() {
        testReorderOnBuild(25, 30, false);
    }

    @Test
    @LongRunningTag
    public void testReorderOnBuildLongRunning() {
        testReorderOnBuild(25, 50, false);
    }

    private void testRandomReordering(final int minVars, final int maxVars, final boolean verbose) {
        for (int vars = minVars; vars <= maxVars; vars++) {
            for (int depth = 4; depth <= 6; depth++) {
                final FormulaFactory f = FormulaFactory.caching();
                final Formula formula = randomFormula(f, vars, depth);
                if (verbose) {
                    System.out.printf("vars = %2d, depth = %2d, nodes = %5d%n", vars, depth, formula.numberOfNodes(f));
                }
                for (final BddReorderingMethod method : REORDER_METHODS) {
                    performReorder(f, formula, method, true, verbose);
                }
                for (final BddReorderingMethod method : REORDER_METHODS) {
                    performReorder(f, formula, method, false, verbose);
                }
            }
        }
    }

    private Formula randomFormula(final FormulaFactory f, final int vars, final int depth) {
        final FormulaRandomizer randomizer = new FormulaRandomizer(f, FormulaRandomizerConfig.builder()
                .numVars(vars).seed(vars * depth * 42)
                .weightEquiv(0).weightImpl(0).weightNot(0).build());
        return Stream.generate(() -> randomizer.and(depth))
                .filter(fm -> fm.variables(f).size() == vars && fm.holds(new SatPredicate(f)))
                .findAny().get();
    }

    private void performReorder(final FormulaFactory f, final Formula formula, final BddReorderingMethod reorderMethod,
                                final boolean withBlocks, final boolean verbose) {
        final BddKernel kernel = new BddKernel(f, new ArrayList<>(formula.variables(f)), 1000, 10000);
        final Bdd bdd = BddFactory.build(f, formula, kernel);
        final BigInteger count = bdd.modelCount();
        final int usedBefore = new BddOperations(kernel).nodeCount(bdd.getIndex());
        final long start = System.currentTimeMillis();
        addVariableBlocks(formula.variables(f).size(), withBlocks, kernel);
        kernel.getReordering().reorder(reorderMethod);
        final long duration = System.currentTimeMillis() - start;
        final int usedAfter = new BddOperations(kernel).nodeCount(bdd.getIndex());
        assertThat(verifyBddConsistency(f, formula, bdd, count)).isTrue();
        verifyVariableBlocks(f, formula, withBlocks, bdd);
        if (reorderMethod != BddReorderingMethod.BDD_REORDER_RANDOM) {
            assertThat(usedAfter).isLessThanOrEqualTo(usedBefore);
        }
        final double reduction = (usedBefore - usedAfter) / (double) usedBefore * 100;
        if (verbose) {
            System.out.println(String.format("%-20s: Reduced %7s blocks in %5dms by %.2f%% from %d to %d",
                    reorderMethod, withBlocks ? "with" : "without", duration, reduction, usedBefore, usedAfter));
        }
    }

    private void addVariableBlocks(final int numVars, final boolean withBlocks, final BddKernel kernel) {
        final BddReordering reordering = kernel.getReordering();
        if (withBlocks) {
            reordering.addVariableBlockAll();
            reordering.addVariableBlock(0, 20, true);
            reordering.addVariableBlock(0, 10, false);
            reordering.addVariableBlock(11, 20, false);
            reordering.addVariableBlock(15, 19, false);
            reordering.addVariableBlock(15, 17, true);
            reordering.addVariableBlock(18, 19, false);
            reordering.addVariableBlock(21, numVars - 1, false);
            if (numVars > 33) {
                reordering.addVariableBlock(30, 33, false);
            }
        } else {
            reordering.addVariableBlockAll();
        }
    }

    private void testReorderOnBuild(final int minVars, final int maxVars, final boolean verbose) {
        for (int vars = minVars; vars <= maxVars; vars++) {
            for (int depth = 4; depth <= 6; depth++) {
                final FormulaFactory f = FormulaFactory.caching();
                final Formula formula = randomFormula(f, vars, depth);
                if (verbose) {
                    System.out.println(String.format("vars = %2d, depth = %2d, nodes = %5d", vars, depth,
                            formula.numberOfNodes(f)));
                }
                final BddKernel kernel = new BddKernel(f, new ArrayList<>(formula.variables(f)), 1000, 10000);
                final Bdd bdd = BddFactory.build(f, formula, kernel);
                final int nodeCount = new BddOperations(kernel).nodeCount(bdd.getIndex());
                final BigInteger modelCount = bdd.modelCount();
                for (final BddReorderingMethod method : REORDER_METHODS) {
                    reorderOnBuild(f, formula, method, modelCount, nodeCount, true, verbose);
                }
                for (final BddReorderingMethod method : REORDER_METHODS) {
                    reorderOnBuild(f, formula, method, modelCount, nodeCount, false, verbose);
                }
            }
        }
    }

    private void reorderOnBuild(final FormulaFactory f, final Formula formula, final BddReorderingMethod method,
                                final BigInteger originalCount, final int originalUsedNodes, final boolean withBlocks,
                                final boolean verbose) {
        final BddKernel kernel = new BddKernel(f, new ArrayList<>(formula.variables(f)), 1000, 10000);
        addVariableBlocks(formula.variables(f).size(), withBlocks, kernel);
        kernel.getReordering().setReorderDuringConstruction(method, 10000);
        final long start = System.currentTimeMillis();
        final Bdd bdd = BddFactory.build(f, formula, kernel);
        final long duration = System.currentTimeMillis() - start;
        final int usedAfter = new BddOperations(kernel).nodeCount(bdd.getIndex());
        verifyVariableBlocks(f, formula, withBlocks, bdd);
        verifyBddConsistency(f, formula, bdd, originalCount);
        final double reduction = (originalUsedNodes - usedAfter) / (double) originalUsedNodes * 100;
        if (verbose) {
            System.out.println(String.format("%-20s: Built in %5d ms, reduction by %6.2f%% from %6d to %6d", method,
                    duration, reduction, originalUsedNodes, usedAfter));
        }
    }

    private boolean verifyBddConsistency(final FormulaFactory f, final Formula f1, final Bdd bdd,
                                         final BigInteger modelCount) {
        final BDDVerification verification = new BDDVerification(bdd.getUnderlyingKernel());
        if (!verification.verify(bdd.getIndex())) {
            return false;
        }
        final long nodes = verification.verifyTree(bdd.getIndex());
        if (nodes < 0) {
            return false;
        }
        stats.newBddSize(nodes);
        if (modelCount != null && !modelCount.equals(bdd.modelCount())) {
            System.out.println("Nodecount changed!");
            return false;
        }
        if (modelCount == null && !f.equivalence(f1, bdd.cnf()).holds(new TautologyPredicate(f))) {
            System.out.println("Not equal");
            return false;
        }
        return true;
    }

    private void verifyVariableBlocks(final FormulaFactory f, final Formula formula, final boolean withBlocks,
                                      final Bdd bdd) {
        if (withBlocks) {
            assertThat(findSequence(bdd,
                    IntStream.range(0, 21).mapToObj(i -> String.format("v%02d", i)).collect(Collectors.toSet())))
                    .isTrue();
            assertThat(findSequence(bdd,
                    IntStream.range(0, 11).mapToObj(i -> String.format("v%02d", i)).collect(Collectors.toSet())))
                    .isTrue();
            assertThat(findSequence(bdd,
                    IntStream.range(11, 21).mapToObj(i -> String.format("v%02d", i)).collect(Collectors.toSet())))
                    .isTrue();
            assertThat(findSequence(bdd,
                    IntStream.range(15, 20).mapToObj(i -> String.format("v%02d", i)).collect(Collectors.toSet())))
                    .isTrue();
            assertThat(findSequence(bdd,
                    IntStream.range(15, 18).mapToObj(i -> String.format("v%02d", i)).collect(Collectors.toSet())))
                    .isTrue();
            assertThat(findSequence(bdd,
                    IntStream.range(18, 20).mapToObj(i -> String.format("v%02d", i)).collect(Collectors.toSet())))
                    .isTrue();
            assertThat(findSequence(bdd, IntStream.range(21, formula.variables(f).size())
                    .mapToObj(i -> String.format("v%02d", i)).collect(Collectors.toSet()))).isTrue();
            if (formula.variables(f).size() > 33) {
                assertThat(findSequence(bdd,
                        IntStream.range(30, 34).mapToObj(i -> String.format("v%02d", i)).collect(Collectors.toSet())))
                        .isTrue();
            }
            final List<Variable> order = bdd.getVariableOrder();
            assertThat(order.indexOf(f.variable("v00"))).isLessThan(order.indexOf(f.variable("v11")));
            assertThat(order.indexOf(f.variable("v16"))).isEqualTo(order.indexOf(f.variable("v15")) + 1);
            assertThat(order.indexOf(f.variable("v17"))).isEqualTo(order.indexOf(f.variable("v16")) + 1);
        }
    }

    private boolean findSequence(final Bdd bdd, final Set<String> vars) {
        final Iterator<Variable> it = bdd.getVariableOrder().iterator();
        while (it.hasNext()) {
            if (vars.contains(it.next().getName())) {
                int numFound = 1;
                while (numFound < vars.size()) {
                    if (!vars.contains(it.next().getName())) {
                        return false;
                    } else {
                        numFound++;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static class SwapStats {
        private final int testedFormulas = 0;
        private final int numSwaps = 0;
        private long maxFormulaSize = 0;
        private long maxBddNodes = 0; // physical nodes
        private long maxBddSize = 0; // num nodes without caching

        public void newFormula(final Formula formula) {
            maxFormulaSize = Math.max(maxFormulaSize, formula.numberOfNodes(formula.getFactory()));
        }

        public void newBdd(final Bdd bdd) {
            maxBddNodes = Math.max(maxBddNodes, bdd.nodeCount());
        }

        public void newBddSize(final long size) {
            maxBddSize = Math.max(maxBddSize, size);
        }

        @Override
        public String toString() {
            return "SwapStats{" +
                    "testedFormulas=" + testedFormulas +
                    ", numSwaps=" + numSwaps +
                    ", maxFormulaSize=" + maxFormulaSize +
                    ", maxBddNodes=" + maxBddNodes +
                    ", maxBddSize=" + maxBddSize +
                    '}';
        }
    }
}
