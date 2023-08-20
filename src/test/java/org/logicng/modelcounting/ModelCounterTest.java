// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.modelcounting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.RandomTag;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.PBConstraint;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.knowledgecompilation.bdds.orderings.ForceOrdering;
import org.logicng.solvers.MiniSat;
import org.logicng.testutils.NQueensGenerator;
import org.logicng.transformations.cnf.CNFConfig;
import org.logicng.util.FormulaCornerCases;
import org.logicng.util.FormulaHelper;
import org.logicng.util.FormulaRandomizer;
import org.logicng.util.FormulaRandomizerConfig;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ModelCounterTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testWrongArgument(final FormulaContext _c) {
        assertThrows(IllegalArgumentException.class, () ->
                ModelCounter.count(Collections.singletonList(_c.f.parse("a & b")), new TreeSet<>(Collections.singletonList(_c.a)), _c.f));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        assertThat(ModelCounter.count(Collections.singleton(_c.f.falsum()), Collections.emptySortedSet(), _c.f))
                .isEqualTo(BigInteger.valueOf(0));
        assertThat(ModelCounter.count(Collections.singleton(_c.f.falsum()), _c.f.variables("a", "b"), _c.f))
                .isEqualTo(BigInteger.valueOf(0));

        assertThat(ModelCounter.count(Collections.singleton(_c.f.verum()), Collections.emptySortedSet(), _c.f))
                .isEqualTo(BigInteger.valueOf(1));
        assertThat(ModelCounter.count(Collections.singleton(_c.f.verum()), _c.f.variables("a", "b"), _c.f))
                .isEqualTo(BigInteger.valueOf(4));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimple(final FormulaContext _c) throws ParserException {
        final Formula formula01 = _c.f.parse("(~v1 => ~v0) | ~v1 | v0");
        assertThat(ModelCounter.count(Collections.singletonList(formula01), formula01.variables(), _c.f)).isEqualTo(BigInteger.valueOf(4));

        final List<Formula> formulas02 = Arrays.asList(_c.f.parse("(a & b) | ~b"), _c.f.parse("a"));
        assertThat(ModelCounter.count(formulas02, FormulaHelper.variables(formulas02), _c.f)).isEqualTo(BigInteger.valueOf(2));

        final List<Formula> formulas03 = Arrays.asList(_c.f.parse("a & b & c"), _c.f.parse("c & d"));
        assertThat(ModelCounter.count(formulas03, FormulaHelper.variables(formulas03), _c.f)).isEqualTo(BigInteger.valueOf(1));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAmoAndExo(final FormulaContext _c) throws ParserException {
        final List<Formula> formulas01 = Arrays.asList(_c.f.parse("a & b"), _c.f.parse("a + b + c + d <= 1"));
        assertThat(ModelCounter.count(formulas01, FormulaHelper.variables(formulas01), _c.f)).isEqualTo(BigInteger.valueOf(0));

        final List<Formula> formulas02 = Arrays.asList(_c.f.parse("a & b & (a + b + c + d <= 1)"), _c.f.parse("a | b"));
        assertThat(ModelCounter.count(formulas02, FormulaHelper.variables(formulas02), _c.f)).isEqualTo(BigInteger.valueOf(0));

        final List<Formula> formulas03 = Arrays.asList(_c.f.parse("a & (a + b + c + d <= 1)"), _c.f.parse("a | b"));
        assertThat(ModelCounter.count(formulas03, FormulaHelper.variables(formulas03), _c.f)).isEqualTo(BigInteger.valueOf(1));

        final List<Formula> formulas04 = Arrays.asList(_c.f.parse("a & (a + b + c + d = 1)"), _c.f.parse("a | b"));
        assertThat(ModelCounter.count(formulas04, FormulaHelper.variables(formulas04), _c.f)).isEqualTo(BigInteger.valueOf(1));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNonAmoAndExo(final FormulaContext _c) throws ParserException {
        final List<Formula> formulas01 = Arrays.asList(_c.f.parse("a & b"), _c.f.parse("a + b + c + d = 2"));
        assertThatThrownBy(() -> ModelCounter.count(formulas01, FormulaHelper.variables(formulas01), _c.f))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Pure encoding for a PBC of type other than AMO or EXO is currently not supported.");

        final List<Formula> formulas02 = Arrays.asList(_c.f.parse("a & b"), _c.f.parse("c | a & (b + c + d <= 4)"));
        assertThatThrownBy(() -> ModelCounter.count(formulas02, FormulaHelper.variables(formulas02), _c.f))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Pure encoding for a PBC of type other than AMO or EXO is currently not supported.");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testQueens(final FormulaContext _c) {
        final NQueensGenerator generator = new NQueensGenerator(_c.f);
        testQueens(generator, 4, 2);
        testQueens(generator, 5, 10);
        testQueens(generator, 6, 4);
        testQueens(generator, 7, 40);
        testQueens(generator, 8, 92);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCornerCases(final FormulaContext _c) {
        final FormulaCornerCases cornerCases = new FormulaCornerCases(_c.f);
        for (final Formula formula : cornerCases.cornerCases()) {
            if (formula.type() == FType.PBC) {
                final PBConstraint pbc = (PBConstraint) formula;
                if (!pbc.isAmo() && !pbc.isExo()) {
                    assertThatThrownBy(() -> ModelCounter.count(Collections.singletonList(formula), formula.variables(), _c.f))
                            .isInstanceOf(UnsupportedOperationException.class);
                    continue;
                }
            }
            final BigInteger expCount = enumerationBasedModelCount(Collections.singletonList(formula), _c.f);
            final BigInteger count = ModelCounter.count(Collections.singleton(formula), formula.variables(), _c.f);
            assertThat(count).isEqualTo(expCount);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void testRandom(final FormulaContext _c) {
        for (int i = 0; i < 500; i++) {
            _c.f.putConfiguration(CNFConfig.builder().algorithm(CNFConfig.Algorithm.PLAISTED_GREENBAUM).build());
            final FormulaRandomizerConfig config = FormulaRandomizerConfig.builder()
                    .numVars(5)
                    .weightAmo(5)
                    .weightExo(5)
                    .seed(i * 42).build();
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f, config);

            final Formula formula = randomizer.formula(4);
            final BigInteger expCount = enumerationBasedModelCount(Collections.singletonList(formula), _c.f);
            final BigInteger count = ModelCounter.count(Collections.singleton(formula), formula.variables(), _c.f);
            assertThat(count).isEqualTo(expCount);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void testRandomWithFormulaList(final FormulaContext _c) {
        for (int i = 0; i < 500; i++) {
            _c.f.putConfiguration(CNFConfig.builder().algorithm(CNFConfig.Algorithm.PLAISTED_GREENBAUM).build());
            final FormulaRandomizerConfig config = FormulaRandomizerConfig.builder()
                    .numVars(5)
                    .weightAmo(5)
                    .weightExo(5)
                    .seed(i * 42).build();
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f, config);

            final List<Formula> formulas = IntStream.range(1, 5).mapToObj(j -> randomizer.formula(4)).collect(Collectors.toList());
            final BigInteger expCount = enumerationBasedModelCount(formulas, _c.f);
            final BigInteger count = ModelCounter.count(formulas, FormulaHelper.variables(formulas), _c.f);
            assertThat(count).isEqualTo(expCount);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void testRandomWithFormulaListWithoutPBC(final FormulaContext _c) {
        for (int i = 0; i < 500; i++) {
            _c.f.putConfiguration(CNFConfig.builder().algorithm(CNFConfig.Algorithm.PLAISTED_GREENBAUM).build());
            final FormulaRandomizerConfig config = FormulaRandomizerConfig.builder()
                    .numVars(5)
                    .weightPbc(0)
                    .seed(i * 42).build();
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f, config);

            final List<Formula> formulas = IntStream.range(1, 5).mapToObj(j -> randomizer.formula(4)).collect(Collectors.toList());
            final BigInteger expCount = enumerationBasedModelCount(formulas, _c.f);
            final BigInteger count = ModelCounter.count(formulas, FormulaHelper.variables(formulas), _c.f);
            assertThat(count).isEqualTo(expCount);
            final Formula formula = _c.f.and(formulas);
            if (!formula.variables().isEmpty()) {
                // Without PB constraints we can use the BDD model count as reference
                assertThat(count).isEqualTo(formula.bdd(new ForceOrdering(_c.f)).modelCount());
            }
        }
    }

    private void testQueens(final NQueensGenerator generator, final int size, final int models) {
        final Formula queens = generator.generate(size);
        assertThat(ModelCounter.count(Collections.singletonList(queens), queens.variables(), queens.factory())).isEqualTo(BigInteger.valueOf(models));
    }

    private static BigInteger enumerationBasedModelCount(final List<Formula> formulas, final FormulaFactory f) {
        final MiniSat solver = MiniSat.miniSat(f);
        solver.add(formulas);
        final SortedSet<Variable> variables = FormulaHelper.variables(formulas);
        final List<Assignment> models = solver.enumerateAllModels(variables);
        return modelCount(models, variables);
    }

    private static BigInteger modelCount(final List<Assignment> models, final SortedSet<Variable> variables) {
        if (models.isEmpty()) {
            return BigInteger.ZERO;
        } else {
            final Assignment firstModel = models.get(0);
            final SortedSet<Variable> modelVars = new TreeSet<>(firstModel.positiveVariables());
            modelVars.addAll(firstModel.negativeVariables());
            final SortedSet<Variable> dontCareVars = variables.stream()
                    .filter(var -> !modelVars.contains(var))
                    .collect(Collectors.toCollection(TreeSet::new));
            return BigInteger.valueOf(models.size()).multiply(BigInteger.valueOf(2).pow(dontCareVars.size()));
        }
    }
}
