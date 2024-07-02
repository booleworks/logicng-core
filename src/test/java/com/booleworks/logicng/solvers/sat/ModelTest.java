// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.solvers.sat.SATTest.defaultMeStrategy;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.CNF_METHOD;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.solverSupplierTestSetForParameterizedTests;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.transformations.cnf.TseitinTransformation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ModelTest {

    private static final FormulaFactory f = FormulaFactory.caching();

    public static Collection<Arguments> solverSuppliers() {
        return solverSupplierTestSetForParameterizedTests(Set.of(USE_AT_MOST_CLAUSES, CNF_METHOD), f);
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solverSuppliers")
    public void testNoModel(final Supplier<SATSolver> solverSupplier, final String solverDescription)
            throws ParserException {
        SATSolver solver = solverSupplier.get();
        solver.add(f.falsum());
        assertThat(solver.satCall().model(f.variables())).isNull();
        solver = solverSupplier.get();
        solver.add(f.parse("A & ~A"));
        assertThat(solver.satCall().model(f.variables("A"))).isNull();
        solver = solverSupplier.get();
        solver.add(f.parse("(A => (B & C)) & A & C & (C <=> ~B)"));
        assertThat(solver.satCall().model(f.variables("A", "B", "C"))).isNull();
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solverSuppliers")
    public void testEmptyModel(final Supplier<SATSolver> solverSupplier, final String solverDescription) {
        final SATSolver solver = solverSupplier.get();
        solver.add(f.verum());
        final Assignment model = solver.satCall().model(f.variables());
        assertThat(model.literals()).isEmpty();
        assertThat(model.blockingClause(f)).isEqualTo(f.falsum());
        assertThat(solver.enumerateAllModels(List.of())).hasSize(1);
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solverSuppliers")
    public void testSimpleModel(final Supplier<SATSolver> solverSupplier, final String solverDescription) {
        SATSolver solver = solverSupplier.get();
        solver.add(f.literal("A", true));
        Assignment model = solver.satCall().model(f.variables("A"));
        assertThat(model.literals()).containsExactly(f.literal("A", true));
        assertThat(solver.enumerateAllModels(f.variables("A"))).hasSize(1);
        solver = solverSupplier.get();
        solver.add(f.literal("A", false));
        model = solver.satCall().model(f.variables("A"));
        assertThat(model.literals()).containsExactly(f.literal("A", false));
        assertThat(solver.enumerateAllModels(f.variables("A"))).hasSize(1);
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solverSuppliers")
    public void testCNFFormula(final Supplier<SATSolver> solverSupplier, final String solverDescription)
            throws ParserException {
        final SATSolver solver = solverSupplier.get();
        final Formula formula = f.parse("(A|B|C) & (~A|~B|~C) & (A|~B|~C) & (~A|~B|C)");
        solver.add(formula);
        final Assignment model = solver.satCall().model(f.variables("A", "B", "C"));
        assertThat(formula.evaluate(model)).isTrue();
        assertThat(solver.enumerateAllModels(f.variables("A", "B", "C"))).hasSize(4);
        for (final Model m : solver.enumerateAllModels(f.variables("A", "B", "C"))) {
            assertThat(formula.evaluate(m.assignment())).isTrue();
        }
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solverSuppliers")
    public void testCNFWithAuxiliaryVarsRestrictedToOriginal(final Supplier<SATSolver> solverSupplier,
                                                             final String solverDescription)
            throws ParserException {
        final SATSolver solver = solverSupplier.get();
        final Formula formula = f.parse("(A => B & C) & (~A => C & ~D) & (C => (D & E | ~E & B)) & ~F");
        final Formula cnf = formula.transform(new TseitinTransformation(solver.factory(), 0));
        solver.add(cnf);
        final Assignment model = solver.satCall().model(formula.variables(f));
        assertThat(formula.evaluate(model)).isTrue();
        final List<Model> allModels = solver.enumerateAllModels(formula.variables(f));
        assertThat(allModels).hasSize(4);
        assertThat(model.formula(f).variables(f)).isEqualTo(formula.variables(f));
        for (final Model m : allModels) {
            assertThat(formula.evaluate(m.assignment())).isTrue();
            assertThat(m.formula(f).variables(f)).isEqualTo(formula.variables(f));
        }
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solverSuppliers")
    public void testNonCNFAllVars(final Supplier<SATSolver> solverSupplier, final String solverDescription)
            throws ParserException {
        final SATSolver solver = solverSupplier.get();
        final Formula formula = f.parse("(A => B & C) & (~A => C & ~D) & (C => (D & E | ~E & B)) & ~F");
        solver.add(formula);
        final Assignment model = solver.satCall().model(formula.variables(f));
        assertThat(formula.evaluate(model)).isTrue();
        final List<Model> allModels = solver.enumerateAllModels(formula.variables(f));
        assertThat(allModels).hasSize(4);
        for (final Model m : allModels) {
            assertThat(formula.evaluate(m.assignment())).isTrue();
            assertThat(m.formula(f).variables(f)).isEqualTo(formula.variables(f));
        }
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solverSuppliers")
    public void testNonCNFOnlyFormulaVars(final Supplier<SATSolver> solverSupplier, final String solverDescription)
            throws ParserException {
        final SATSolver solver = solverSupplier.get();
        final Formula formula = f.parse("(A => B & C) & (~A => C & ~D) & (C => (D & E | ~E & B)) & ~F");
        solver.add(formula);
        final Assignment model = solver.satCall().model(formula.variables(f));
        assertThat(formula.evaluate(model)).isTrue();
        assertThat(model.formula(f).variables(f)).isEqualTo(formula.variables(f));
        final List<Model> allModels = solver.enumerateAllModels(formula.variables(f));
        assertThat(allModels).hasSize(4);
        for (final Model m : allModels) {
            assertThat(formula.evaluate(m.assignment())).isTrue();
            assertThat(m.formula(f).variables(f)).isEqualTo(formula.variables(f));
        }
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solverSuppliers")
    public void testNonCNFRestrictedVars(final Supplier<SATSolver> solverSupplier, final String solverDescription)
            throws ParserException {
        final SATSolver solverForMe = solverSupplier.get();
        final Formula formula = f.parse("(A => B & C) & (~A => C & ~D) & (C => (D & E | ~E & B)) & ~F");
        final SATSolver verificationSolver = SATSolver.newSolver(f);
        verificationSolver.add(formula);
        solverForMe.add(formula);
        final SortedSet<Variable> relevantVariables =
                new TreeSet<>(Arrays.asList(f.variable("A"), f.variable("B"), f.variable("C")));
        final Assignment model = solverForMe.satCall().model(relevantVariables);
        assertThat(verificationSolver.satCall().addFormulas(model.literals()).sat().getResult()).isTrue();
        assertThat(model.formula(f).variables(f)).isEqualTo(relevantVariables);
        final List<Model> allModels = solverForMe.enumerateAllModels(relevantVariables);
        assertThat(allModels).hasSize(2);
        for (final Model m : allModels) {
            assertThat(verificationSolver.satCall().addFormulas(m.getLiterals()).sat().getResult()).isTrue();
            assertThat(m.formula(f).variables(f)).isEqualTo(relevantVariables);
        }
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solverSuppliers")
    public void testNonCNFRestrictedAndAdditionalVars(final Supplier<SATSolver> solverSupplier,
                                                      final String solverDescription)
            throws ParserException {
        final SATSolver solverForMe = solverSupplier.get();
        final SATSolver verificationSolver = SATSolver.newSolver(f);
        final Formula formula = f.parse("(A => B & C) & (~A => C & ~D) & (C => (D & E | ~E & B)) & ~F");
        solverForMe.add(formula);
        verificationSolver.add(formula);
        final SortedSet<Variable> relevantVariables =
                new TreeSet<>(Arrays.asList(f.variable("A"), f.variable("B"), f.variable("C")));
        final SortedSet<Variable> additionalVariables =
                new TreeSet<>(Arrays.asList(f.variable("D"), f.variable("X"), f.variable("Y")));
        final SortedSet<Variable> allVariables = new TreeSet<>(relevantVariables);
        allVariables.addAll(additionalVariables);
        final Assignment model = solverForMe.satCall().model(additionalVariables);
        assertThat(verificationSolver.satCall().addFormulas(model.literals()).sat().getResult()).isTrue();
        assertThat(model.formula(f).variables(f)).containsExactlyInAnyOrder(f.variable("D"), f.variable("X"),
                f.variable("Y"));
        final ModelEnumerationFunction me = ModelEnumerationFunction.builder(relevantVariables)
                .additionalVariables(additionalVariables)
                .configuration(ModelEnumerationConfig.builder().strategy(defaultMeStrategy).build())
                .build();
        final List<Model> allModels = solverForMe.execute(me);
        assertThat(allModels).hasSize(2);
        for (final Model m : allModels) {
            assertThat(verificationSolver.satCall().addFormulas(m.getLiterals()).sat().getResult()).isTrue();
            assertThat(m.formula(f).variables(f)).isEqualTo(allVariables);
        }
    }
}
