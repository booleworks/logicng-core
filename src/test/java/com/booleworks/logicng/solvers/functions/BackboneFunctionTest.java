// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.solvers.sat.SATSolverConfig.CNFMethod.FACTORY_CNF;
import static com.booleworks.logicng.solvers.sat.SATSolverConfig.CNFMethod.PG_ON_SOLVER;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.SATSolverConfig;
import com.booleworks.logicng.solvers.sat.SATSolverLowLevelConfig;
import com.booleworks.logicng.util.Pair;
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
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class BackboneFunctionTest {

    private static final FormulaFactory f = FormulaFactory.caching();

    private static SATSolverConfig.Builder config(final boolean useAtMostClauses, final SATSolverConfig.CNFMethod cnfMethod,
                                                  final boolean bbCheckForRotatableLiterals, final boolean bbCheckForComplementModelLiterals,
                                                  final boolean bbInitialUBCheckForRotatableLiterals) {
        return SATSolverConfig.builder()
                .useAtMostClauses(useAtMostClauses)
                .cnfMethod(cnfMethod)
                .lowLevelConfig(SATSolverLowLevelConfig.builder()
                        .bbCheckForRotatableLiterals(bbCheckForRotatableLiterals)
                        .bbCheckForComplementModelLiterals(bbCheckForComplementModelLiterals)
                        .bbInitialUBCheckForRotatableLiterals(bbInitialUBCheckForRotatableLiterals)
                        .build());
    }

    public static List<Arguments> solvers() {
        final Supplier<SATSolverConfig.Builder> configNoAmNoPG1 = () -> config(false, FACTORY_CNF, false, false, false);
        final Supplier<SATSolverConfig.Builder> configNoAmNoPG2 = () -> config(false, FACTORY_CNF, true, false, false);
        final Supplier<SATSolverConfig.Builder> configNoAmNoPG3 = () -> config(false, FACTORY_CNF, false, true, false);
        final Supplier<SATSolverConfig.Builder> configNoAmNoPG4 = () -> config(false, FACTORY_CNF, false, false, true);
        final Supplier<SATSolverConfig.Builder> configNoAmNoPG5 = () -> config(false, FACTORY_CNF, true, true, true);
        final Supplier<SATSolverConfig.Builder> configNoAmPG1 = () -> config(false, PG_ON_SOLVER, false, false, false);
        final Supplier<SATSolverConfig.Builder> configNoAmPG2 = () -> config(false, PG_ON_SOLVER, true, false, false);
        final Supplier<SATSolverConfig.Builder> configNoAmPG3 = () -> config(false, PG_ON_SOLVER, false, true, false);
        final Supplier<SATSolverConfig.Builder> configNoAmPG4 = () -> config(false, PG_ON_SOLVER, false, false, true);
        final Supplier<SATSolverConfig.Builder> configNoAmPG5 = () -> config(false, PG_ON_SOLVER, true, true, true);
        final Supplier<SATSolverConfig.Builder> configNoPG1 = () -> config(true, FACTORY_CNF, false, false, false);
        final Supplier<SATSolverConfig.Builder> configNoPG2 = () -> config(true, FACTORY_CNF, true, false, false);
        final Supplier<SATSolverConfig.Builder> configNoPG3 = () -> config(true, FACTORY_CNF, false, true, false);
        final Supplier<SATSolverConfig.Builder> configNoPG4 = () -> config(true, FACTORY_CNF, false, false, true);
        final Supplier<SATSolverConfig.Builder> configNoPG5 = () -> config(true, FACTORY_CNF, true, true, true);
        final Supplier<SATSolverConfig.Builder> configPG1 = () -> config(true, PG_ON_SOLVER, false, false, false);
        final Supplier<SATSolverConfig.Builder> configPG2 = () -> config(true, PG_ON_SOLVER, true, false, false);
        final Supplier<SATSolverConfig.Builder> configPG3 = () -> config(true, PG_ON_SOLVER, false, true, false);
        final Supplier<SATSolverConfig.Builder> configPG4 = () -> config(true, PG_ON_SOLVER, false, false, true);
        final Supplier<SATSolverConfig.Builder> configPG5 = () -> config(true, PG_ON_SOLVER, true, true, true);
        final List<Pair<Supplier<SATSolverConfig.Builder>, String>> configs = Arrays.asList(
                new Pair<>(configNoAmNoPG1, "FF CNF -ATM -ROT -COMP -UB"),
                new Pair<>(configNoAmNoPG2, "FF CNF -ATM +ROT -COMP -UB"),
                new Pair<>(configNoAmNoPG3, "FF CNF -ATM -ROT +COMP -UB"),
                new Pair<>(configNoAmNoPG4, "FF CNF -ATM -ROT -COMP +UB"),
                new Pair<>(configNoAmNoPG5, "FF CNF -ATM +ROT +COMP +UB"),
                new Pair<>(configNoAmPG1, "PG CNF -ATM -ROT -COMP -UB"),
                new Pair<>(configNoAmPG2, "PG CNF -ATM +ROT -COMP -UB"),
                new Pair<>(configNoAmPG3, "PG CNF -ATM -ROT +COMP -UB"),
                new Pair<>(configNoAmPG4, "PG CNF -ATM -ROT -COMP +UB"),
                new Pair<>(configNoAmPG5, "PG CNF -ATM +ROT +COMP +UB"),
                new Pair<>(configNoPG1, "FF CNF +ATM -ROT -COMP -UB"),
                new Pair<>(configNoPG2, "FF CNF +ATM +ROT -COMP -UB"),
                new Pair<>(configNoPG3, "FF CNF +ATM -ROT +COMP -UB"),
                new Pair<>(configNoPG4, "FF CNF +ATM -ROT -COMP +UB"),
                new Pair<>(configNoPG5, "FF CNF +ATM +ROT +COMP +UB"),
                new Pair<>(configPG1, "PG CNF +ATM -ROT -COMP -UB"),
                new Pair<>(configPG2, "PG CNF +ATM +ROT -COMP -UB"),
                new Pair<>(configPG3, "PG CNF +ATM -ROT +COMP -UB"),
                new Pair<>(configPG4, "PG CNF +ATM -ROT -COMP +UB"),
                new Pair<>(configPG5, "PG CNF +ATM +ROT +COMP +UB")
        );
        return configs.stream()
                .map(config -> Arguments.of(SATSolver.newSolver(f, config.first().get().build()), config.second()))
                .collect(Collectors.toList());
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    public void testConstants(final SATSolver solver, final String solverDescription) {
        final SolverState state = solver.saveState();
        solver.add(f.falsum());
        Backbone backbone = solver.backbone(v("a b c"));
        assertThat(backbone.isSat()).isFalse();
        assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        solver.loadState(state);
        solver.add(f.verum());
        backbone = solver.backbone(v("a b c"));
        assertThat(backbone.isSat()).isTrue();
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEmpty();
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    public void testSimpleBackbones(final SATSolver solver, final String solverDescription) throws ParserException {
        final SolverState state = solver.saveState();
        solver.add(f.parse("a & b & ~c"));
        Backbone backbone = solver.backbone(v("a b c"));
        assertThat(backbone.isSat()).isTrue();
        assertThat(backbone.getCompleteBackbone(f)).containsExactly(f.variable("a"), f.variable("b"), f.literal("c", false));
        solver.loadState(state);
        solver.add(f.parse("~a & ~b & c"));
        backbone = solver.backbone(v("a c"));
        assertThat(backbone.isSat()).isTrue();
        Assertions.assertThat(backbone.getCompleteBackbone(f)).containsExactly(f.literal("a", false), f.variable("c"));
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    public void testSimpleFormulas(final SATSolver solver, final String solverDescription) throws ParserException {
        solver.add(f.parse("(a => c | d) & (b => d | ~e) & (a | b)"));
        Backbone backbone = solver.backbone(v("a b c d e f"));
        assertThat(backbone.isSat()).isTrue();
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        solver.add(f.parse("a => b"));
        solver.add(f.parse("b => a"));
        solver.add(f.parse("~d"));
        backbone = solver.backbone(v("a b c d e f g h"));
        assertThat(backbone.isSat()).isTrue();
        Assertions.assertThat(backbone.getCompleteBackbone(f)).containsExactly(f.variable("a"), f.variable("b"), f.variable("c"),
                f.literal("d", false), f.literal("e", false));
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    public void testSimpleFormulasWithBuilderUsage(final SATSolver solver, final String solverDescription) throws ParserException {
        solver.add(f.parse("(a => c | d) & (b => d | ~e) & (a | b)"));
        Backbone backbone = solver.execute(BackboneFunction.builder().variables(
                        f.variable("a"), f.variable("b"), f.variable("c"), f.variable("d"), f.variable("e"), f.variable("f"))
                .build());
        assertThat(backbone.isSat()).isTrue();
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        solver.add(f.parse("a => b"));
        solver.add(f.parse("b => a"));
        solver.add(f.parse("~d"));
        backbone = solver.backbone(v("a b c d e f g h"));
        assertThat(backbone.isSat()).isTrue();
        Assertions.assertThat(backbone.getCompleteBackbone(f)).containsExactly(f.variable("a"), f.variable("b"), f.variable("c"),
                f.literal("d", false), f.literal("e", false));
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    @LongRunningTag
    public void testRealFormulaIncremental1(final SATSolver solver, final String solverDescription) throws IOException, ParserException {
        final Formula formula = FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/large_formula.txt");
        solver.add(formula);
        final List<String> expectedBackbones = new ArrayList<>();
        final BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/backbones/backbone_large_formula.txt"));
        while (reader.ready()) {
            expectedBackbones.add(reader.readLine());
        }
        reader.close();
        Backbone backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(0)));
        solver.add(f.variable("v411"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(1)));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v385"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(2)));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v275"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(3)));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v188"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(4)));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v103"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(5)));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v404"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        assertThat(backbone.isSat()).isFalse();
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    @LongRunningTag
    public void testRealFormulaIncremental2(final SATSolver solver, final String solverDescription) throws IOException, ParserException {
        final Formula formula = FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/small_formulas.txt");
        solver.add(formula);
        final List<String> expectedBackbones = new ArrayList<>();
        final BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/backbones/backbone_small_formulas.txt"));
        while (reader.ready()) {
            expectedBackbones.add(reader.readLine());
        }
        reader.close();
        Backbone backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(0)));
        solver.add(f.variable("v2609"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(1)));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v2416"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(2)));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v2048"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(3)));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v39"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(4)));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v1663"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(5)));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v2238"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        assertThat(backbone.isSat()).isFalse();
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    @LongRunningTag
    public void testRealFormulaIncrementalDecremental1(final SATSolver solver, final String solverDescription) throws IOException, ParserException {
        final Formula formula = FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/large_formula.txt");
        solver.add(formula);
        final SolverState state = solver.saveState();
        final List<String> expectedBackbones = new ArrayList<>();
        final BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/backbones/backbone_large_formula.txt"));
        while (reader.ready()) {
            expectedBackbones.add(reader.readLine());
        }
        reader.close();
        Backbone backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(0)));
        solver.add(f.variable("v411"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(1)));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(f.parse("v411 & v385"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(2)));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(f.parse("v411 & v385 & v275"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(3)));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(f.parse("v411 & v385 & v275 & v188"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(4)));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(f.parse("v411 & v385 & v275 & v188 & v103"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(5)));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(f.parse("v411 & v385 & v275 & v188 & v103 & v404"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        assertThat(backbone.isSat()).isFalse();
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    @LongRunningTag
    public void testRealFormulaIncrementalDecremental2(final SATSolver solver, final String solverDescription) throws IOException, ParserException {
        final Formula formula = FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/small_formulas.txt");
        solver.add(formula);
        final SolverState state = solver.saveState();
        final List<String> expectedBackbones = new ArrayList<>();
        final BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/backbones/backbone_small_formulas.txt"));
        while (reader.ready()) {
            expectedBackbones.add(reader.readLine());
        }
        reader.close();
        Backbone backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(0)));
        solver.add(f.variable("v2609"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(1)));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(f.parse("v2609 & v2416"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(2)));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(f.parse("v2609 & v2416 & v2048"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(3)));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(f.parse("v2609 & v2416 & v2048 & v39"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(4)));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(f.parse("v2609 & v2416 & v2048 & v39 & v1663"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(5)));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(f.parse("v2609 & v2416 & v2048 & v39 & v1663 & v2238"));
        backbone = solver.backbone(formula.variables(f));
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        assertThat(backbone.isSat()).isFalse();
    }

    @Test
    public void testMiniCardSpecialCase() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SATSolver miniCard = SATSolver.newSolver(f, SATSolverConfig.builder().useAtMostClauses(true).build());
        miniCard.add(f.parse("v1 + v2 + v3 + v4 + v5 + v6 = 1"));
        miniCard.add(f.parse("v1234 + v50 + v60 = 1"));
        miniCard.add(f.parse("(v1 => v1234) & (v2 => v1234) & (v3 => v1234) & (v4 => v1234) & (v5 => v50) & (v6 => v60)"));
        miniCard.add(f.parse("~v6"));
        final Backbone backboneMC = miniCard.backbone(Arrays.asList(f.variable("v6"), f.variable("v60")));
        Assertions.assertThat(backboneMC.getNegativeBackbone()).extracting(Variable::name).containsExactlyInAnyOrder("v6", "v60");
    }

    private SortedSet<Variable> v(final String s) {
        final SortedSet<Variable> vars = new TreeSet<>();
        for (final String name : s.split(" ")) {
            vars.add(f.variable(name));
        }
        return vars;
    }

    private SortedSet<Literal> parseBackbone(final String string) throws ParserException {
        final SortedSet<Literal> literals = new TreeSet<>();
        for (final String lit : string.split(" ")) {
            literals.add((Literal) f.parse(lit));
        }
        return literals;
    }
}
