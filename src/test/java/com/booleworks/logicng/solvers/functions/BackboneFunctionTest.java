// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.backbones.Backbone;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.solvers.MiniSat;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.GlucoseConfig;
import com.booleworks.logicng.solvers.sat.MiniSat2Solver;
import com.booleworks.logicng.solvers.sat.MiniSatConfig;
import com.booleworks.logicng.util.Pair;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class BackboneFunctionTest {

    private static final FormulaFactory f = FormulaFactory.caching();

    public static Collection<Object[]> solvers() {
        final MiniSatConfig configNoPG1 = MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FACTORY_CNF)
                .bbCheckForRotatableLiterals(false).bbCheckForComplementModelLiterals(false)
                .bbInitialUBCheckForRotatableLiterals(false).build();
        final MiniSatConfig configNoPG2 =
                MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FACTORY_CNF).bbCheckForRotatableLiterals(true)
                        .bbCheckForComplementModelLiterals(false).bbInitialUBCheckForRotatableLiterals(false).build();
        final MiniSatConfig configNoPG3 = MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FACTORY_CNF)
                .bbCheckForRotatableLiterals(false).bbCheckForComplementModelLiterals(true)
                .bbInitialUBCheckForRotatableLiterals(false).build();
        final MiniSatConfig configNoPG4 = MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FACTORY_CNF)
                .bbCheckForRotatableLiterals(false).bbCheckForComplementModelLiterals(false)
                .bbInitialUBCheckForRotatableLiterals(true).build();
        final MiniSatConfig configNoPG5 =
                MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.FACTORY_CNF).bbCheckForRotatableLiterals(true)
                        .bbCheckForComplementModelLiterals(true).bbInitialUBCheckForRotatableLiterals(true).build();
        final MiniSatConfig configPG1 = MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER)
                .bbCheckForRotatableLiterals(false).bbCheckForComplementModelLiterals(false)
                .bbInitialUBCheckForRotatableLiterals(false).build();
        final MiniSatConfig configPG2 = MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER)
                .bbCheckForRotatableLiterals(true).bbCheckForComplementModelLiterals(false)
                .bbInitialUBCheckForRotatableLiterals(false).build();
        final MiniSatConfig configPG3 = MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER)
                .bbCheckForRotatableLiterals(false).bbCheckForComplementModelLiterals(true)
                .bbInitialUBCheckForRotatableLiterals(false).build();
        final MiniSatConfig configPG4 = MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER)
                .bbCheckForRotatableLiterals(false).bbCheckForComplementModelLiterals(false)
                .bbInitialUBCheckForRotatableLiterals(true).build();
        final MiniSatConfig configPG5 = MiniSatConfig.builder().cnfMethod(MiniSatConfig.CNFMethod.PG_ON_SOLVER)
                .bbCheckForRotatableLiterals(true).bbCheckForComplementModelLiterals(true)
                .bbInitialUBCheckForRotatableLiterals(true).build();
        final List<Pair<MiniSatConfig, String>> configs = Arrays.asList(
                new Pair<>(configNoPG1, "FF CNF -ROT -COMP -UB"),
                new Pair<>(configNoPG2, "FF CNF +ROT -COMP -UB"),
                new Pair<>(configNoPG3, "FF CNF -ROT +COMP -UB"),
                new Pair<>(configNoPG4, "FF CNF -ROT -COMP +UB"),
                new Pair<>(configNoPG5, "FF CNF +ROT +COMP +UB"),
                new Pair<>(configPG1, "PG CNF -ROT -COMP -UB"),
                new Pair<>(configPG2, "PG CNF +ROT -COMP -UB"),
                new Pair<>(configPG3, "PG CNF -ROT +COMP -UB"),
                new Pair<>(configPG4, "PG CNF -ROT -COMP +UB"),
                new Pair<>(configPG5, "PG CNF +ROT +COMP +UB")
        );
        final List<Object[]> solvers = new ArrayList<>();
        for (final Pair<MiniSatConfig, String> config : configs) {
            solvers.add(new Object[]{MiniSat.miniSat(f, config.first()), "MiniSat (" + config.second() + ")"});
            solvers.add(new Object[]{MiniSat.miniCard(f, config.first()), "MiniCard (" + config.second() + ")"});
            solvers.add(new Object[]{MiniSat.glucose(f, config.first(), GlucoseConfig.builder().build()),
                    "Glucose (" + config.second() + ")"});
        }
        return solvers;
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testConstants(final MiniSat solver) {
        solver.reset();
        SolverState state = null;
        if (solver.underlyingSolver() instanceof MiniSat2Solver) {
            state = solver.saveState();
        }
        solver.add(f.falsum());
        Backbone backbone = solver.backbone(v("a b c"));
        assertThat(backbone.isSat()).isFalse();
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        if (solver.underlyingSolver() instanceof MiniSat2Solver) {
            solver.loadState(state);
        } else {
            solver.reset();
        }
        solver.add(f.verum());
        backbone = solver.backbone(v("a b c"));
        assertThat(backbone.isSat()).isTrue();
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testSimpleBackbones(final MiniSat solver) throws ParserException {
        solver.reset();
        SolverState state = null;
        if (solver.underlyingSolver() instanceof MiniSat2Solver) {
            state = solver.saveState();
        }
        solver.add(f.parse("a & b & ~c"));
        Backbone backbone = solver.backbone(v("a b c"));
        assertThat(backbone.isSat()).isTrue();
        Assertions.assertThat(backbone.getCompleteBackbone(f)).containsExactly(f.variable("a"), f.variable("b"),
                f.literal("c", false));
        if (solver.underlyingSolver() instanceof MiniSat2Solver) {
            solver.loadState(state);
        } else {
            solver.reset();
        }
        solver.add(f.parse("~a & ~b & c"));
        backbone = solver.backbone(v("a c"));
        assertThat(backbone.isSat()).isTrue();
        Assertions.assertThat(backbone.getCompleteBackbone(f)).containsExactly(f.literal("a", false), f.variable("c"));
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testSimpleFormulas(final MiniSat solver) throws ParserException {
        solver.reset();
        solver.add(f.parse("(a => c | d) & (b => d | ~e) & (a | b)"));
        Backbone backbone = solver.backbone(v("a b c d e f"));
        assertThat(backbone.isSat()).isTrue();
        Assertions.assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        solver.add(f.parse("a => b"));
        solver.add(f.parse("b => a"));
        solver.add(f.parse("~d"));
        backbone = solver.backbone(v("a b c d e f g h"));
        assertThat(backbone.isSat()).isTrue();
        Assertions.assertThat(backbone.getCompleteBackbone(f)).containsExactly(f.variable("a"), f.variable("b"),
                f.variable("c"),
                f.literal("d", false), f.literal("e", false));
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testSimpleFormulasWithBuilderUsage(final MiniSat solver) throws ParserException {
        solver.reset();
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
        Assertions.assertThat(backbone.getCompleteBackbone(f)).containsExactly(f.variable("a"), f.variable("b"),
                f.variable("c"),
                f.literal("d", false), f.literal("e", false));
    }

    @ParameterizedTest
    @MethodSource("solvers")
    public void testRealFormulaIncremental1(final MiniSat solver) throws IOException, ParserException {
        solver.reset();
        final Formula formula =
                FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/large_formula.txt");
        solver.add(formula);
        final List<String> expectedBackbones = new ArrayList<>();
        final BufferedReader reader =
                new BufferedReader(new FileReader("src/test/resources/backbones/backbone_large_formula.txt"));
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

    @ParameterizedTest
    @MethodSource("solvers")
    @LongRunningTag
    public void testRealFormulaIncremental2(final MiniSat solver) throws IOException, ParserException {
        solver.reset();
        final Formula formula =
                FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/small_formulas.txt");
        solver.add(formula);
        final List<String> expectedBackbones = new ArrayList<>();
        final BufferedReader reader =
                new BufferedReader(new FileReader("src/test/resources/backbones/backbone_small_formulas.txt"));
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

    @ParameterizedTest
    @MethodSource("solvers")
    public void testRealFormulaIncrementalDecremental1(final MiniSat solver) throws IOException, ParserException {
        if (solver.underlyingSolver() instanceof MiniSat2Solver) {
            solver.reset();
            final Formula formula =
                    FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/large_formula.txt");
            solver.add(formula);
            final SolverState state = solver.saveState();
            final List<String> expectedBackbones = new ArrayList<>();
            final BufferedReader reader =
                    new BufferedReader(new FileReader("src/test/resources/backbones/backbone_large_formula.txt"));
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
    }

    @ParameterizedTest
    @MethodSource("solvers")
    @LongRunningTag
    public void testRealFormulaIncrementalDecremental2(final MiniSat solver) throws IOException, ParserException {
        if (solver.underlyingSolver() instanceof MiniSat2Solver) {
            solver.reset();
            final Formula formula =
                    FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/small_formulas.txt");
            solver.add(formula);
            final SolverState state = solver.saveState();
            final List<String> expectedBackbones = new ArrayList<>();
            final BufferedReader reader =
                    new BufferedReader(new FileReader("src/test/resources/backbones/backbone_small_formulas.txt"));
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
    }

    @Test
    public void testMiniCardSpecialCase() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final SATSolver miniCard = MiniSat.miniCard(f);
        miniCard.add(f.parse("v1 + v2 + v3 + v4 + v5 + v6 = 1"));
        miniCard.add(f.parse("v1234 + v50 + v60 = 1"));
        miniCard.add(
                f.parse("(v1 => v1234) & (v2 => v1234) & (v3 => v1234) & (v4 => v1234) & (v5 => v50) & (v6 => v60)"));
        miniCard.add(f.parse("~v6"));
        final Backbone backboneMC = miniCard.backbone(Arrays.asList(f.variable("v6"), f.variable("v60")));
        Assertions.assertThat(backboneMC.getNegativeBackbone()).extracting(Variable::name)
                .containsExactlyInAnyOrder("v6", "v60");
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
