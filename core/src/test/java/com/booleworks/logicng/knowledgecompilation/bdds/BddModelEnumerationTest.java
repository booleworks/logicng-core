// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.CType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.CallLimitComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.functions.ModelCountingFunction;
import com.booleworks.logicng.testutils.NQueensGenerator;
import com.booleworks.logicng.testutils.PigeonHoleGenerator;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

public class BddModelEnumerationTest {

    private final FormulaFactory f;

    private final List<Formula> formulas;
    private final List<SortedSet<Variable>> variables;
    private final BigInteger[] expected;

    public BddModelEnumerationTest() {
        final int[] problems = new int[]{3, 4, 5, 6, 7, 8, 9};
        expected = new BigInteger[]{
                BigInteger.valueOf(0),
                BigInteger.valueOf(2),
                BigInteger.valueOf(10),
                BigInteger.valueOf(4),
                BigInteger.valueOf(40),
                BigInteger.valueOf(92),
                BigInteger.valueOf(352)
        };

        f = FormulaFactory.caching();
        final NQueensGenerator generator = new NQueensGenerator(f);
        formulas = new ArrayList<>(problems.length);
        variables = new ArrayList<>(problems.length);

        for (final int problem : problems) {
            final Formula p = generator.generate(problem);
            formulas.add(p);
            variables.add(p.variables(f));
        }
    }

    @Test
    public void testModelCount() {
        for (int i = 0; i < formulas.size(); i++) {
            final BddKernel kernel = new BddKernel(f, variables.get(i).size(), 10000, 10000);
            final Bdd bdd = BddFactory.build(f, formulas.get(i), kernel);
            assertThat(bdd.modelCount()).isEqualTo(expected[i]);
        }
    }

    @Test
    public void testModelEnumeration() {
        for (int i = 0; i < formulas.size(); i++) {
            final BddKernel kernel = new BddKernel(f, variables.get(i).size(), 10000, 10000);
            final Bdd bdd = BddFactory.build(f, formulas.get(i), kernel);
            final Set<Model> models = new HashSet<>(bdd.enumerateAllModels());
            assertThat(models.size()).isEqualTo(expected[i].intValue());
            for (final Model model : models) {
                assertThat(formulas.get(i).evaluate(model.toAssignment())).isTrue();
            }
        }
    }

    @Test
    public void testExo() {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula constraint = f.exo(generateVariables(f, 100)).cnf(f);
        final BddKernel kernel = new BddKernel(f, constraint.variables(f).size(), 100000, 1000000);
        final Bdd bdd = BddFactory.build(f, constraint, kernel);
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.valueOf(100));
        assertThat(bdd.enumerateAllModels()).hasSize(100);
    }

    @Test
    public void testExk() {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula constraint = f.cc(CType.EQ, 8, generateVariables(f, 15)).cnf(f);
        final BddKernel kernel = new BddKernel(f, constraint.variables(f).size(), 100000, 1000000);
        final Bdd bdd = BddFactory.build(f, constraint, kernel);
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.valueOf(6435));
        assertThat(bdd.enumerateAllModels()).hasSize(6435);
    }

    @Test
    public void testAmo() {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula constraint = f.amo(generateVariables(f, 100)).cnf(f);
        final BddKernel kernel = new BddKernel(f, constraint.variables(f).size(), 100000, 1000000);
        final Bdd bdd = BddFactory.build(f, constraint, kernel);
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.valueOf(221));
        assertThat(bdd.enumerateAllModels(generateVariables(f, 100))).hasSize(101);
    }

    @Test
    @LongRunningTag
    public void testComputationHandlerExitPoints() {
        final Formula formula = new PigeonHoleGenerator(f).generate(10).negate(f);
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(formula);
        for (int callLimit = 0; callLimit < 5000; callLimit++) {
            final ModelCountingFunction me = ModelCountingFunction.builder(formula.variables(f)).build();
            final LngResult<BigInteger> result = me.apply(solver, new CallLimitComputationHandler(callLimit));
            assertThat(result.isSuccess()).isFalse();
            assertThatThrownBy(result::getResult).isInstanceOf(IllegalStateException.class);
        }
    }

    private List<Variable> generateVariables(final FormulaFactory f, final int n) {
        final List<Variable> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(f.variable("v" + i));
        }
        return result;
    }
}
