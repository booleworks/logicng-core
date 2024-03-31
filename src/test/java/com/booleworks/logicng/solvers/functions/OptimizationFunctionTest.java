// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.CNF_METHOD;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.INITIAL_PHASE;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.PROOF_GENERATION;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LogicNGTest;
import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.CardinalityConstraint;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.BoundedOptimizationHandler;
import com.booleworks.logicng.handlers.OptimizationHandler;
import com.booleworks.logicng.handlers.SATHandler;
import com.booleworks.logicng.handlers.TimeoutOptimizationHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.predicates.satisfiability.SATPredicate;
import com.booleworks.logicng.solvers.MaxSATSolver;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import com.booleworks.logicng.solvers.sat.SolverTestSet;
import com.booleworks.logicng.util.FormulaCornerCases;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class OptimizationFunctionTest implements LogicNGTest {

    public static List<Arguments> solverSuppliers() {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        final List<Arguments> solverSuppliers = SolverTestSet.solverSupplierTestSetForParameterizedTests(Set.of(USE_AT_MOST_CLAUSES, CNF_METHOD, INITIAL_PHASE, PROOF_GENERATION), f);
        return solverSuppliers.stream().map(args -> Arguments.of(args.get()[0], f, args.get()[1])).collect(Collectors.toList());
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    public void testUnsatFormula(final Supplier<SATSolver> solver, final FormulaFactory f, final String solverDescription) throws ParserException {
        final Formula formula = f.parse("a & b & (a => ~b)");
        final Assignment minimumModel = optimize(Collections.singleton(formula), formula.variables(f), Collections.emptyList(), false, solver.get(), null);
        assertThat(minimumModel).isNull();
        final Assignment maximumModel = optimize(Collections.singleton(formula), formula.variables(f), Collections.emptyList(), true, solver.get(), null);
        assertThat(maximumModel).isNull();
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    public void testSingleModel(final Supplier<SATSolver> solver, final FormulaFactory f, final String solverDescription) throws ParserException {
        final Formula formula = f.parse("~a & ~b & ~c");
        final Assignment minimumModel = optimize(Collections.singleton(formula), formula.variables(f), Collections.emptyList(), false, solver.get(), null);
        testMinimumModel(formula, minimumModel, formula.variables(f));
        final Assignment maximumModel = optimize(Collections.singleton(formula), formula.variables(f), Collections.emptyList(), true, solver.get(), null);
        testMaximumModel(formula, maximumModel, formula.variables(f));
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    public void testExoModel(final Supplier<SATSolver> solver, final FormulaFactory f, final String solverDescription) {
        final CardinalityConstraint exo = (CardinalityConstraint) f.exo(f.variable("a"), f.variable("b"), f.variable("c"));
        final Assignment minimumModel = optimize(Collections.singleton(exo), exo.variables(f), Collections.emptyList(), false, solver.get(), null);
        testMinimumModel(exo, minimumModel, exo.variables(f));
        final Assignment maximumModel = optimize(Collections.singleton(exo), exo.variables(f), Collections.emptyList(), true, solver.get(), null);
        testMaximumModel(exo, maximumModel, exo.variables(f));
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    public void testCornerCases(final Supplier<SATSolver> solver, final FormulaFactory f, final String solverDescription) {
        final FormulaCornerCases cornerCases = new FormulaCornerCases(f);
        for (final Formula formula : cornerCases.cornerCases()) {
            final Set<Variable> targetLiterals = cornerCases.getVariables();

            final Assignment minimumModel = optimize(Collections.singleton(formula), targetLiterals, Collections.emptySet(), false, solver.get(), null);
            testMinimumModel(formula, minimumModel, targetLiterals);

            final Assignment maximumModel = optimize(Collections.singleton(formula), targetLiterals, Collections.emptySet(), true, solver.get(), null);
            testMaximumModel(formula, maximumModel, targetLiterals);
        }
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @RandomTag
    public void testRandomSmall(final Supplier<SATSolver> solver, final FormulaFactory f0, final String solverDescription) {
        final FormulaFactory f = FormulaFactory.nonCaching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build()); // caching factory goes out of heap
        final Random random = new Random(42);
        final FormulaRandomizer randomizer = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().numVars(6).weightPbc(2).seed(42).build());
        for (int i = 0; i < 1000; i++) {
            final Formula formula = randomizer.formula(2);
            final List<Variable> variables = new ArrayList<>(formula.variables(f));

            final Set<Literal> targetLiterals = randomTargetLiterals(f, random, randomSubset(random, variables, Math.min(variables.size(), 5)));
            final Set<Variable> additionalVariables = randomSubset(random, variables, Math.min(variables.size(), 3));

            final Assignment minimumModel = optimize(Collections.singleton(formula), targetLiterals, additionalVariables, false, solver.get(), null);
            testMinimumModel(formula, minimumModel, targetLiterals);

            final Assignment maximumModel = optimize(Collections.singleton(formula), targetLiterals, additionalVariables, true, solver.get(), null);
            testMaximumModel(formula, maximumModel, targetLiterals);
        }
    }

    private static <T> Set<T> randomSubset(final Random random, final List<T> elements, final int subsetSize) {
        if (subsetSize > elements.size()) {
            throw new IllegalArgumentException();
        }
        final Set<T> subset = new HashSet<>();
        while (subset.size() < subsetSize) {
            subset.add(elements.get(random.nextInt(elements.size())));
        }
        return subset;
    }

    private static SortedSet<Literal> randomTargetLiterals(final FormulaFactory f, final Random random, final Collection<Variable> variables) {
        return variables.stream().map(var -> f.literal(var.name(), random.nextBoolean())).collect(toCollection(TreeSet::new));
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    public void testIncrementalityMinimizeAndMaximize(final Supplier<SATSolver> solverSupplier, final FormulaFactory f) throws ParserException {
        Formula formula = f.parse("(a|b|c|d|e) & (p|q) & (x|y|z)");
        final SATSolver solver = solverSupplier.get();
        final SortedSet<Variable> vars = new TreeSet<>(formula.variables(f));
        solver.add(formula);

        Assignment minimumModel = solver.execute(OptimizationFunction.builder().minimize().literals(vars).build());
        Assignment maximumModel = solver.execute(OptimizationFunction.builder().maximize().literals(vars).build());
        Assertions.assertThat(minimumModel.positiveVariables()).hasSize(3);
        Assertions.assertThat(maximumModel.positiveVariables()).hasSize(10);

        formula = f.parse("~p");
        vars.addAll(formula.variables(f));
        solver.add(formula);
        minimumModel = solver.execute(OptimizationFunction.builder().minimize().literals(vars).build());
        maximumModel = solver.execute(OptimizationFunction.builder().maximize().literals(vars).build());
        Assertions.assertThat(minimumModel.positiveVariables()).hasSize(3).contains(f.variable("q"));
        Assertions.assertThat(maximumModel.positiveVariables()).hasSize(9).contains(f.variable("q"));

        formula = f.parse("(x => n) & (y => m) & (a => ~b & ~c)");
        vars.addAll(formula.variables(f));
        solver.add(formula);
        minimumModel = solver.execute(OptimizationFunction.builder().minimize().literals(vars).build());
        maximumModel = solver.execute(OptimizationFunction.builder().maximize().literals(vars).build());
        Assertions.assertThat(minimumModel.positiveVariables()).hasSize(3).contains(f.variable("q"), f.variable("z"));
        Assertions.assertThat(maximumModel.positiveVariables()).hasSize(10)
                .contains(f.variable("q"), f.variable("z"))
                .doesNotContain(f.variable("a"));

        formula = f.parse("(z => v & w) & (m => v) & (b => ~c & ~d & ~e)");
        vars.addAll(formula.variables(f));
        solver.add(formula);
        minimumModel = solver.execute(OptimizationFunction.builder().minimize().literals(vars).build());
        maximumModel = solver.execute(OptimizationFunction.builder().maximize().literals(vars).build());
        Assertions.assertThat(minimumModel.positiveVariables()).hasSize(4).contains(f.variable("q"), f.variable("x"), f.variable("n"));
        Assertions.assertThat(maximumModel.positiveVariables()).hasSize(11)
                .contains(f.variable("q"), f.variable("x"), f.variable("n"), f.variable("v"), f.variable("w"))
                .doesNotContain(f.variable("b"));

        formula = f.parse("~q");
        vars.addAll(formula.variables(f));
        solver.add(formula);
        minimumModel = solver.execute(OptimizationFunction.builder().minimize().literals(vars).build());
        maximumModel = solver.execute(OptimizationFunction.builder().maximize().literals(vars).build());
        assertThat(minimumModel).isNull();
        assertThat(maximumModel).isNull();
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    public void testAdditionalVariables(final Supplier<SATSolver> solver, final FormulaFactory f, final String solverDescription) throws ParserException {
        final Variable a = f.variable("a");
        final Literal na = f.literal("a", false);
        final Variable b = f.variable("b");
        final Literal nb = f.literal("b", false);
        final Variable c = f.variable("c");
        final Variable x = f.variable("x");
        final Literal nx = f.literal("x", false);
        final Variable y = f.variable("y");

        final Formula formula = f.parse("(a|b) & (~a => c) & (x|y)");

        final List<Literal> literalsANBX = Arrays.asList(a, nb, x);
        final Assignment minimumModel = optimize(Collections.singleton(formula), literalsANBX, Collections.emptyList(), false, solver.get(), null);
        Assertions.assertThat(minimumModel.literals()).containsExactlyInAnyOrder(na, b, nx);
        final Assignment minimumModelWithY = optimize(Collections.singleton(formula), literalsANBX, Collections.singleton(y), false, solver.get(), null);
        Assertions.assertThat(minimumModelWithY.literals()).containsExactlyInAnyOrder(na, b, nx, y);
        final Assignment minimumModelWithCY = optimize(Collections.singleton(formula), literalsANBX, Arrays.asList(c, y), false, solver.get(), null);
        Assertions.assertThat(minimumModelWithCY.literals()).containsExactlyInAnyOrder(na, b, c, nx, y);

        final List<Literal> literalsNBNX = Arrays.asList(na, nx);
        final Assignment maximumModel = optimize(Collections.singleton(formula), literalsNBNX, Collections.emptyList(), true, solver.get(), null);
        Assertions.assertThat(maximumModel.literals()).containsExactlyInAnyOrder(na, nx);
        final Assignment maximumModelWithC = optimize(Collections.singleton(formula), literalsNBNX, Collections.singleton(c), true, solver.get(), null);
        Assertions.assertThat(maximumModelWithC.literals()).containsExactlyInAnyOrder(na, c, nx);
        final Assignment maximumModelWithACY = optimize(Collections.singleton(formula), literalsNBNX, Arrays.asList(a, c, y), true, solver.get(), null);
        Assertions.assertThat(maximumModelWithACY.literals()).containsExactlyInAnyOrder(na, c, nx, y);
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @LongRunningTag
    public void testLargeFormulaMinimize(final Supplier<SATSolver> solver, final FormulaFactory f, final String solverDescription) throws IOException, ParserException {
        final Formula formula = FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/large_formula.txt");
        final List<Variable> variables = randomSubset(formula.variables(f), 300);
        final Assignment minimumModel = optimize(Collections.singleton(formula), variables, Collections.emptyList(), false, solver.get(), null);
        testMinimumModel(formula, minimumModel, variables);
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @LongRunningTag
    public void testLargeFormulaMaximize(final Supplier<SATSolver> solver, final FormulaFactory f, final String solverDescription) throws IOException, ParserException {
        final Formula formula = FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/large_formula.txt");
        final Assignment maximumModel = optimize(Collections.singleton(formula), formula.variables(f), Collections.emptyList(), true, solver.get(), null);
        testMaximumModel(formula, maximumModel, formula.variables(f));
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @LongRunningTag
    public void testLargerFormulaMinimize(final Supplier<SATSolver> solver, final FormulaFactory f, final String solverDescription) throws IOException, ParserException {
        final Formula formula = FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/small_formulas.txt");
        final Assignment minimumModel = optimize(Collections.singleton(formula), formula.variables(f), Collections.emptyList(), false, solver.get(), null);
        testMinimumModel(formula, minimumModel, formula.variables(f));
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @LongRunningTag
    public void testLargerFormulaMaximize(final Supplier<SATSolver> solver, final FormulaFactory f, final String solverDescription) throws IOException, ParserException {
        final Formula formula = FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/small_formulas.txt");
        final List<Variable> variables = randomSubset(formula.variables(f), 300);
        final Assignment maximumModel = optimize(Collections.singleton(formula), variables, Collections.emptyList(), true, solver.get(), null);
        testMaximumModel(formula, maximumModel, variables);
    }

    @Test
    public void compareWithMaxSat() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/formulas/large_formula.txt"));
        final List<Formula> formulas = new ArrayList<>();
        final SortedSet<Variable> variables = new TreeSet<>();
        while (reader.ready()) {
            final Formula parsed = p.parse(reader.readLine());
            formulas.add(parsed);
            variables.addAll(parsed.variables(f));
        }
        final int expected = 25;
        assertThat(solveMaxSat(formulas, variables, MaxSATSolver.incWBO(f))).isEqualTo(expected);
        assertThat(solveMaxSat(formulas, variables, MaxSATSolver.linearSU(f))).isEqualTo(expected);
        assertThat(solveMaxSat(formulas, variables, MaxSATSolver.linearUS(f))).isEqualTo(expected);
        assertThat(solveMaxSat(formulas, variables, MaxSATSolver.msu3(f))).isEqualTo(expected);
        assertThat(solveMaxSat(formulas, variables, MaxSATSolver.wbo(f))).isEqualTo(expected);
        assertThat(satisfiedLiterals(optimize(formulas, variables, Collections.emptyList(), false, SATSolver.miniSat(f, SATSolverConfig.builder().useAtMostClauses(false).build()), null), variables).size()).isEqualTo(expected);
        assertThat(satisfiedLiterals(optimize(formulas, variables, Collections.emptyList(), false, SATSolver.miniSat(f, SATSolverConfig.builder().useAtMostClauses(true).build()), null), variables).size()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @LongRunningTag
    public void testTimeoutOptimizationHandler(final Supplier<SATSolver> solver, final FormulaFactory f, final String solverDescription) throws IOException, ParserException {
        final Formula formula = FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/large_formula.txt");
        final TimeoutOptimizationHandler handlerMax = new TimeoutOptimizationHandler(1L);
        final Assignment maximumModel = optimize(Collections.singleton(formula), formula.variables(f), Collections.emptyList(), true, solver.get(), handlerMax);
        assertThat(maximumModel).isNull();
        assertThat(handlerMax.aborted()).isTrue();

        final TimeoutOptimizationHandler handlerTooShort = new TimeoutOptimizationHandler(0L);
        final Assignment model = optimize(Collections.singleton(formula), formula.variables(f), Collections.emptyList(), false, solver.get(), handlerTooShort);
        assertThat(model).isNull();
        assertThat(handlerTooShort.aborted()).isTrue();
        Assertions.assertThat(handlerTooShort.getIntermediateResult()).isNull(); // SATHandler aborted before a model could be computed

        final CustomOptimizationHandler customHandler = new CustomOptimizationHandler();
        final Assignment modelCustom = optimize(Collections.singleton(formula), formula.variables(f), Collections.emptyList(), true, solver.get(), customHandler);
        assertThat(modelCustom).isNull();
        assertThat(customHandler.aborted()).isTrue();
        assertThat(customHandler.currentResult).isNotNull();
    }

    @ParameterizedTest(name = "{index} {2}")
    @MethodSource("solverSuppliers")
    @LongRunningTag
    public void testCancellationPoints(final Supplier<SATSolver> solverSupplier, final FormulaFactory f0, final String solverDescription) throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final SortedSet<Variable> selVars = new TreeSet<>();
        final List<Formula> clauses = DimacsReader.readCNF(f, "src/test/resources/sat/c499_gr_rcs_w6.shuffled.cnf");
        final List<Formula> formulas = new ArrayList<>();
        for (final Formula clause : clauses) {
            final Variable selVar = f.variable("@SEL_" + selVars.size());
            selVars.add(selVar);
            formulas.add(f.equivalence(selVar, clause));
        }
        final SATSolver solver = solverSupplier.get();
        for (int numSatHandlerStarts = 1; numSatHandlerStarts < 4; numSatHandlerStarts++) {
            solver.add(formulas);
            final OptimizationHandler handler = new BoundedOptimizationHandler(numSatHandlerStarts, -1);
            final OptimizationFunction optimizationFunction = OptimizationFunction.builder()
                    .handler(handler)
                    .literals(selVars)
                    .maximize().build();

            final Assignment result = solver.execute(optimizationFunction);

            assertThat(handler.aborted()).isTrue();
            assertThat(result).isNull();
        }
    }

    private int solveMaxSat(final List<Formula> formulas, final SortedSet<Variable> variables, final MaxSATSolver solver) {
        formulas.forEach(solver::addHardFormula);
        variables.forEach(v -> solver.addSoftFormula(v.negate(formulas.iterator().next().factory()), 1));
        solver.solve();
        return solver.result();
    }

    private SortedSet<Literal> satisfiedLiterals(final Assignment assignment, final Collection<? extends Literal> literals) {
        final SortedSet<Literal> modelLiterals = assignment.literals();
        return literals.stream().filter(modelLiterals::contains).collect(toCollection(TreeSet::new));
    }

    private static Assignment optimize(final Collection<Formula> formulas, final Collection<? extends Literal> literals,
                                       final Collection<Variable> additionalVariables, final boolean maximize, final SATSolver solver,
                                       final OptimizationHandler handler) {
        formulas.forEach(solver::add);
        if (maximize) {
            return solver.execute(OptimizationFunction.builder().maximize().literals(literals)
                    .additionalVariables(additionalVariables).handler(handler).build());
        } else {
            return solver.execute(OptimizationFunction.builder().minimize().literals(literals)
                    .additionalVariables(additionalVariables).handler(handler).build());
        }
    }

    private void testMinimumModel(final Formula formula, final Assignment resultModel, final Collection<? extends Literal> literals) {
        testOptimumModel(formula, resultModel, literals, false);
    }

    private void testMaximumModel(final Formula formula, final Assignment resultModel, final Collection<? extends Literal> literals) {
        testOptimumModel(formula, resultModel, literals, true);
    }

    private void testOptimumModel(final Formula formula, final Assignment optimumModel, final Collection<? extends Literal> literals, final boolean maximize) {
        final FormulaFactory f = formula.factory();
        final SATPredicate satPredicate = new SATPredicate(f);
        if (formula.holds(satPredicate)) {
            if (literals.isEmpty()) {
                assertThat(optimumModel.literals()).isEmpty();
            } else {
                assertThat(f.and(formula, f.and(optimumModel.literals())).holds(satPredicate)).isTrue();
                final int actualNumSatisfied = satisfiedLiterals(optimumModel, literals).size();
                final MaxSATSolver solver = MaxSATSolver.oll(f);
                solver.addHardFormula(formula);
                literals.forEach(l -> solver.addSoftFormula(maximize ? l : l.negate(f), 1));
                solver.solve();
                final int numSatisfiedOll = satisfiedLiterals(solver.model(), literals).size();
                assertThat(actualNumSatisfied).isEqualTo(numSatisfiedOll);
            }
        } else {
            assertThat(optimumModel).isNull();
        }
    }

    private static List<Variable> randomSubset(final Collection<Variable> original, final int size) {
        final List<Variable> variables = new ArrayList<>(original);
        Collections.shuffle(variables, new Random(42));
        return variables.subList(0, size);
    }

    private static class CustomOptimizationHandler implements OptimizationHandler {
        public Assignment currentResult;
        private boolean aborted;

        @Override
        public SATHandler satHandler() {
            return null;
        }

        @Override
        public boolean aborted() {
            return aborted;
        }

        @Override
        public boolean foundBetterBound(final Supplier<Assignment> currentResultProvider) {
            currentResult = currentResultProvider.get();
            aborted = currentResult.positiveVariables().size() >= 161;
            return !aborted;
        }
    }
}
