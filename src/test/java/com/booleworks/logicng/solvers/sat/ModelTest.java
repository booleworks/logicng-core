// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.sat;

import static com.booleworks.logicng.solvers.sat.SATTest.strategy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.datastructures.Tristate;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.functions.ModelEnumerationFunction;
import com.booleworks.logicng.solvers.functions.modelenumeration.ModelEnumerationConfig;
import com.booleworks.logicng.transformations.cnf.TseitinTransformation;
import com.booleworks.logicng.util.Pair;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class ModelTest {

    private static final FormulaFactory f = FormulaFactory.caching();

    public static Collection<Object[]> solvers() {
        final MiniSatConfig configNoPGAux = MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FACTORY_CNF).auxiliaryVariablesInModels(true).build();
        final MiniSatConfig configNoPGNoAux = MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FACTORY_CNF).auxiliaryVariablesInModels(false).build();
        final MiniSatConfig configPGAux = MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER).auxiliaryVariablesInModels(true).build();
        final MiniSatConfig configPGNoAux = MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER).auxiliaryVariablesInModels(false).build();
        final List<Pair<MiniSatConfig, String>> configs = Arrays.asList(
                new Pair<>(configNoPGAux, "FF CNF, +AUX"),
                new Pair<>(configNoPGNoAux, "FF CNF, -AUX"),
                new Pair<>(configPGAux, "PG CNF, +AUX"),
                new Pair<>(configPGNoAux, "PG CNF, -AUX")
        );
        final List<Object[]> solvers = new ArrayList<>();
        for (final Pair<MiniSatConfig, String> config : configs) {
            solvers.add(new Object[]{MiniSat.miniSat(f, config.first()), "MiniSat (" + config.second() + ")"});
            solvers.add(new Object[]{MiniSat.miniCard(f, config.first()), "MiniCard (" + config.second() + ")"});
            solvers.add(new Object[]{MiniSat.glucose(f, config.first(), GlucoseConfig.builder().build()), "Glucose (" + config.second() + ")"});
        }
        return solvers;
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testNoModel(final SATSolver solver) throws ParserException {
        solver.reset();
        solver.add(f.falsum());
        solver.sat();
        Assertions.assertThat(solver.model()).isNull();
        solver.reset();
        solver.add(f.parse("A & ~A"));
        solver.sat();
        Assertions.assertThat(solver.model()).isNull();
        solver.reset();
        solver.add(f.parse("(A => (B & C)) & A & C & (C <=> ~B)"));
        solver.sat();
        Assertions.assertThat(solver.model()).isNull();
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testEmptyModel(final SATSolver solver) {
        solver.reset();
        solver.add(f.verum());
        solver.sat();
        final Assignment model = solver.model();
        assertThat(model.literals()).isEmpty();
        assertThat(model.blockingClause(f)).isEqualTo(f.falsum());
        Assertions.assertThat(solver.enumerateAllModels(List.of())).hasSize(1);
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testSimpleModel(final SATSolver solver) {
        solver.reset();
        solver.add(f.literal("A", true));
        solver.sat();
        Assignment model = solver.model();
        assertThat(model.literals()).containsExactly(f.literal("A", true));
        Assertions.assertThat(solver.enumerateAllModels(f.variables("A"))).hasSize(1);
        solver.reset();
        solver.add(f.literal("A", false));
        solver.sat();
        model = solver.model();
        assertThat(model.literals()).containsExactly(f.literal("A", false));
        Assertions.assertThat(solver.enumerateAllModels(f.variables("A"))).hasSize(1);
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testCNFFormula(final SATSolver solver) throws ParserException {
        solver.reset();
        final Formula formula = f.parse("(A|B|C) & (~A|~B|~C) & (A|~B|~C) & (~A|~B|C)");
        solver.add(formula);
        solver.sat();
        final Assignment model = solver.model();
        assertThat(formula.evaluate(model)).isTrue();
        Assertions.assertThat(solver.enumerateAllModels(f.variables("A", "B", "C"))).hasSize(4);
        for (final Model m : solver.enumerateAllModels(f.variables("A", "B", "C"))) {
            assertThat(formula.evaluate(m.assignment())).isTrue();
        }
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testCNFWithAuxiliaryVarsRestrictedToOriginal(final SATSolver solver) throws ParserException {
        solver.reset();
        final Formula formula = f.parse("(A => B & C) & (~A => C & ~D) & (C => (D & E | ~E & B)) & ~F");
        final Formula cnf = formula.transform(new TseitinTransformation(solver.factory(), 0));
        solver.add(cnf);
        solver.sat();
        final Assignment model = solver.model(formula.variables(f));
        assertThat(formula.evaluate(model)).isTrue();
        final List<Model> allModels = solver.enumerateAllModels(formula.variables(f));
        assertThat(allModels).hasSize(4);
        assertThat(model.formula(f).variables(f)).isEqualTo(formula.variables(f));
        for (final Model m : allModels) {
            assertThat(formula.evaluate(m.assignment())).isTrue();
            assertThat(m.formula(f).variables(f)).isEqualTo(formula.variables(f));
        }
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testNonCNFAllVars(final MiniSat solver) throws ParserException {
        solver.reset();
        final Formula formula = f.parse("(A => B & C) & (~A => C & ~D) & (C => (D & E | ~E & B)) & ~F");
        solver.add(formula);
        solver.sat();
        final Assignment model = solver.model();
        assertThat(formula.evaluate(model)).isTrue();
        final List<Model> allModels = solver.enumerateAllModels(formula.variables(f));
        assertThat(allModels).hasSize(4);
        for (final Model m : allModels) {
            assertThat(formula.evaluate(m.assignment())).isTrue();
            assertThat(m.formula(f).variables(f)).isEqualTo(formula.variables(f));
        }
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testNonCNFOnlyFormulaVars(final SATSolver solver) throws ParserException {
        solver.reset();
        final Formula formula = f.parse("(A => B & C) & (~A => C & ~D) & (C => (D & E | ~E & B)) & ~F");
        solver.add(formula);
        solver.sat();
        final Assignment model = solver.model(formula.variables(f));
        assertThat(formula.evaluate(model)).isTrue();
        assertThat(model.formula(f).variables(f)).isEqualTo(formula.variables(f));
        final List<Model> allModels = solver.enumerateAllModels(formula.variables(f));
        assertThat(allModels).hasSize(4);
        for (final Model m : allModels) {
            assertThat(formula.evaluate(m.assignment())).isTrue();
            assertThat(m.formula(f).variables(f)).isEqualTo(formula.variables(f));
        }
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testNonCNFRestrictedVars(final SATSolver solver) throws ParserException {
        solver.reset();
        final Formula formula = f.parse("(A => B & C) & (~A => C & ~D) & (C => (D & E | ~E & B)) & ~F");
        final MiniSat miniSat = MiniSat.miniSat(f);
        miniSat.add(formula);
        solver.add(formula);
        solver.sat();
        final SortedSet<Variable> relevantVariables = new TreeSet<>(Arrays.asList(f.variable("A"), f.variable("B"), f.variable("C")));
        final Assignment model = solver.model(relevantVariables);
        Assertions.assertThat(miniSat.sat(model.literals())).isEqualTo(Tristate.TRUE);
        assertThat(model.formula(f).variables(f)).isEqualTo(relevantVariables);
        final List<Model> allModels = solver.enumerateAllModels(relevantVariables);
        assertThat(allModels).hasSize(2);
        for (final Model m : allModels) {
            Assertions.assertThat(miniSat.sat(m.getLiterals())).isEqualTo(Tristate.TRUE);
            assertThat(m.formula(f).variables(f)).isEqualTo(relevantVariables);
        }
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testNonCNFRestrictedAndAdditionalVars(final SATSolver solver) throws ParserException {
        solver.reset();
        final Formula formula = f.parse("(A => B & C) & (~A => C & ~D) & (C => (D & E | ~E & B)) & ~F");
        final MiniSat miniSat = MiniSat.miniSat(f);
        miniSat.add(formula);
        solver.add(formula);
        solver.sat();
        final SortedSet<Variable> relevantVariables = new TreeSet<>(Arrays.asList(f.variable("A"), f.variable("B"), f.variable("C")));
        final SortedSet<Variable> additionalVariables = new TreeSet<>(Arrays.asList(f.variable("D"), f.variable("X"), f.variable("Y")));
        final SortedSet<Variable> allVariables = new TreeSet<>(relevantVariables);
        allVariables.addAll(additionalVariables);
        final Assignment model = solver.model(additionalVariables);
        Assertions.assertThat(miniSat.sat(model.literals())).isEqualTo(Tristate.TRUE);
        assertThat(model.formula(f).variables(f)).containsExactly(f.variable("D"));
        final ModelEnumerationFunction me = ModelEnumerationFunction.builder(relevantVariables)
                .additionalVariables(additionalVariables)
                .configuration(ModelEnumerationConfig.builder().strategy(strategy(solver)).build())
                .build();
        final List<Model> allModels = solver.execute(me);
        assertThat(allModels).hasSize(2);
        for (final Model m : allModels) {
            Assertions.assertThat(miniSat.sat(m.getLiterals())).isEqualTo(Tristate.TRUE);
            assertThat(m.formula(f).variables(f)).isEqualTo(allVariables);
        }
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testUnsolvedFormula(final SATSolver solver) throws ParserException {
        solver.reset();
        final Formula formula = f.parse("(A => B & C) & (~A => C & ~D) & (C => (D & E | ~E & B)) & ~F");
        solver.add(formula);
        assertThatThrownBy(solver::model).isInstanceOf(IllegalStateException.class);
    }
}
