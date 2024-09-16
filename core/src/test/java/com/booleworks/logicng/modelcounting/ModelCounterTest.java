// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.modelcounting;

import static com.booleworks.logicng.testutils.TestUtil.modelCount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaContext;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.formulas.TestWithFormulaContext;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.knowledgecompilation.bdds.orderings.ForceOrdering;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.testutils.NQueensGenerator;
import com.booleworks.logicng.transformations.cnf.CnfConfig;
import com.booleworks.logicng.util.FormulaCornerCases;
import com.booleworks.logicng.util.FormulaHelper;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
        assertThrows(IllegalArgumentException.class, () -> ModelCounter.count(_c.f,
                Collections.singletonList(_c.f.parse("a & b")), new TreeSet<>(Collections.singletonList(_c.a))));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstants(final FormulaContext _c) {
        assertThat(ModelCounter.count(_c.f, Collections.singleton(_c.f.falsum()), Collections.emptySortedSet()))
                .isEqualTo(BigInteger.valueOf(0));
        assertThat(ModelCounter.count(_c.f, Collections.singleton(_c.f.falsum()), _c.f.variables("a", "b")))
                .isEqualTo(BigInteger.valueOf(0));

        assertThat(ModelCounter.count(_c.f, Collections.singleton(_c.f.verum()), Collections.emptySortedSet()))
                .isEqualTo(BigInteger.valueOf(1));
        assertThat(ModelCounter.count(_c.f, Collections.singleton(_c.f.verum()), _c.f.variables("a", "b")))
                .isEqualTo(BigInteger.valueOf(4));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimple(final FormulaContext _c) throws ParserException {
        final Formula formula01 = _c.f.parse("(~v1 => ~v0) | ~v1 | v0");
        assertThat(ModelCounter.count(_c.f, Collections.singletonList(formula01), formula01.variables(_c.f)))
                .isEqualTo(BigInteger.valueOf(4));

        final List<Formula> formulas02 = Arrays.asList(_c.f.parse("(a & b) | ~b"), _c.f.parse("a"));
        assertThat(ModelCounter.count(_c.f, formulas02, FormulaHelper.variables(_c.f, formulas02)))
                .isEqualTo(BigInteger.valueOf(2));

        final List<Formula> formulas03 = Arrays.asList(_c.f.parse("a & b & c"), _c.f.parse("c & d"));
        assertThat(ModelCounter.count(_c.f, formulas03, FormulaHelper.variables(_c.f, formulas03)))
                .isEqualTo(BigInteger.valueOf(1));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAmoAndExo(final FormulaContext _c) throws ParserException {
        final List<Formula> formulas01 = Arrays.asList(_c.f.parse("a & b"), _c.f.parse("a + b + c + d <= 1"));
        assertThat(ModelCounter.count(_c.f, formulas01, FormulaHelper.variables(_c.f, formulas01)))
                .isEqualTo(BigInteger.valueOf(0));

        final List<Formula> formulas02 = Arrays.asList(_c.f.parse("a & b & (a + b + c + d <= 1)"), _c.f.parse("a | b"));
        assertThat(ModelCounter.count(_c.f, formulas02, FormulaHelper.variables(_c.f, formulas02)))
                .isEqualTo(BigInteger.valueOf(0));

        final List<Formula> formulas03 = Arrays.asList(_c.f.parse("a & (a + b + c + d <= 1)"), _c.f.parse("a | b"));
        assertThat(ModelCounter.count(_c.f, formulas03, FormulaHelper.variables(_c.f, formulas03)))
                .isEqualTo(BigInteger.valueOf(1));

        final List<Formula> formulas04 = Arrays.asList(_c.f.parse("a & (a + b + c + d = 1)"), _c.f.parse("a | b"));
        assertThat(ModelCounter.count(_c.f, formulas04, FormulaHelper.variables(_c.f, formulas04)))
                .isEqualTo(BigInteger.valueOf(1));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNonAmoAndExo(final FormulaContext _c) throws ParserException {
        final List<Formula> formulas01 = Arrays.asList(_c.f.parse("a & b"), _c.f.parse("a + b + c + d = 2"));
        assertThatThrownBy(() -> ModelCounter.count(_c.f, formulas01, FormulaHelper.variables(_c.f, formulas01)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Pure encoding for a PBC of type other than AMO or EXO is currently not supported.");

        final List<Formula> formulas02 = Arrays.asList(_c.f.parse("a & b"), _c.f.parse("c | a & (b + c + d <= 4)"));
        assertThatThrownBy(() -> ModelCounter.count(_c.f, formulas02, FormulaHelper.variables(_c.f, formulas02)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Pure encoding for a PBC of type other than AMO or EXO is currently not supported.");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testQueens(final FormulaContext _c) {
        final NQueensGenerator generator = new NQueensGenerator(_c.f);
        testQueens(_c.f, generator, 4, 2);
        testQueens(_c.f, generator, 5, 10);
        testQueens(_c.f, generator, 6, 4);
        testQueens(_c.f, generator, 7, 40);
        testQueens(_c.f, generator, 8, 92);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCornerCases(final FormulaContext _c) {
        final FormulaCornerCases cornerCases = new FormulaCornerCases(_c.f);
        for (final Formula formula : cornerCases.cornerCases()) {
            if (formula.getType() == FType.PBC) {
                final PbConstraint pbc = (PbConstraint) formula;
                if (!pbc.isAmo() && !pbc.isExo()) {
                    assertThatThrownBy(
                            () -> ModelCounter.count(_c.f, Collections.singletonList(formula), formula.variables(_c.f)))
                            .isInstanceOf(UnsupportedOperationException.class);
                    continue;
                }
            }
            final BigInteger expCount = enumerationBasedModelCount(_c.f, Collections.singletonList(formula));
            final BigInteger count = ModelCounter.count(_c.f, Collections.singleton(formula), formula.variables(_c.f));
            assertThat(count).isEqualTo(expCount);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void testRandom(final FormulaContext _c) {
        for (int i = 0; i < 500; i++) {
            _c.f.putConfiguration(CnfConfig.builder().algorithm(CnfConfig.Algorithm.PLAISTED_GREENBAUM).build());
            final FormulaRandomizerConfig config = FormulaRandomizerConfig.builder()
                    .numVars(5)
                    .weightAmo(5)
                    .weightExo(5)
                    .seed(i * 42).build();
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f, config);

            final Formula formula = randomizer.formula(4);
            final BigInteger expCount = enumerationBasedModelCount(_c.f, Collections.singletonList(formula));
            final BigInteger count = ModelCounter.count(_c.f, Collections.singleton(formula), formula.variables(_c.f));
            assertThat(count).isEqualTo(expCount);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void testRandomWithFormulaList(final FormulaContext _c) {
        for (int i = 0; i < 500; i++) {
            _c.f.putConfiguration(CnfConfig.builder().algorithm(CnfConfig.Algorithm.PLAISTED_GREENBAUM).build());
            final FormulaRandomizerConfig config = FormulaRandomizerConfig.builder()
                    .numVars(5)
                    .weightAmo(5)
                    .weightExo(5)
                    .seed(i * 42).build();
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f, config);

            final List<Formula> formulas =
                    IntStream.range(1, 5).mapToObj(j -> randomizer.formula(4)).collect(Collectors.toList());
            final BigInteger expCount = enumerationBasedModelCount(_c.f, formulas);
            final BigInteger count = ModelCounter.count(_c.f, formulas, FormulaHelper.variables(_c.f, formulas));
            assertThat(count).isEqualTo(expCount);
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    @RandomTag
    public void testRandomWithFormulaListWithoutPBC(final FormulaContext _c) {
        for (int i = 0; i < 500; i++) {
            _c.f.putConfiguration(CnfConfig.builder().algorithm(CnfConfig.Algorithm.PLAISTED_GREENBAUM).build());
            final FormulaRandomizerConfig config = FormulaRandomizerConfig.builder()
                    .numVars(5)
                    .weightPbc(0)
                    .seed(i * 42).build();
            final FormulaRandomizer randomizer = new FormulaRandomizer(_c.f, config);

            final List<Formula> formulas =
                    IntStream.range(1, 5).mapToObj(j -> randomizer.formula(4)).collect(Collectors.toList());
            final BigInteger expCount = enumerationBasedModelCount(_c.f, formulas);
            final BigInteger count = ModelCounter.count(_c.f, formulas, FormulaHelper.variables(_c.f, formulas));
            assertThat(count).isEqualTo(expCount);
            final Formula formula = _c.f.and(formulas);
            if (!formula.variables(_c.f).isEmpty()) {
                // Without PB constraints we can use the BDD model count as
                // reference
                assertThat(count).isEqualTo(formula.bdd(_c.f, new ForceOrdering()).modelCount());
            }
        }
    }

    private void testQueens(final FormulaFactory f, final NQueensGenerator generator, final int size,
                            final int models) {
        final Formula queens = generator.generate(size);
        assertThat(ModelCounter.count(queens.getFactory(), Collections.singletonList(queens), queens.variables(f)))
                .isEqualTo(BigInteger.valueOf(models));
    }

    private static BigInteger enumerationBasedModelCount(final FormulaFactory f, final List<Formula> formulas) {
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(formulas);
        final SortedSet<Variable> variables = FormulaHelper.variables(f, formulas);
        final List<Model> models = solver.enumerateAllModels(variables);
        return modelCount(models, variables);
    }
}
