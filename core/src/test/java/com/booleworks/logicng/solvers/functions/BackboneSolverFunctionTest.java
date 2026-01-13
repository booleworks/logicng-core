// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.solvers.functions;

import static com.booleworks.logicng.TestWithExampleFormulas.parse;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.CNF_METHOD;
import static com.booleworks.logicng.solvers.sat.SolverTestSet.SATSolverConfigParam.USE_AT_MOST_CLAUSES;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.datastructures.Backbone;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.FormulaParser;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.solvers.SatSolver;
import com.booleworks.logicng.solvers.SolverState;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import com.booleworks.logicng.solvers.sat.SolverTestSet;
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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

@SuppressWarnings("unused")
public class BackboneSolverFunctionTest {

    public static List<Arguments> solvers() {
        final FormulaFactory f = FormulaFactory.caching();
        return SolverTestSet.solverTestSetForParameterizedTests(Set.of(USE_AT_MOST_CLAUSES, CNF_METHOD), f);
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    public void testConstants(final SatSolver solver, final String solverDescription) {
        final FormulaFactory f = solver.getFactory();
        final SolverState state = solver.saveState();
        solver.add(f.falsum());
        Backbone backbone = solver.backbone(v("a b c", f));
        assertThat(backbone.isSat()).isFalse();
        assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        solver.loadState(state);
        solver.add(f.verum());
        backbone = solver.backbone(v("a b c", f));
        assertThat(backbone.isSat()).isTrue();
        assertThat(backbone.getCompleteBackbone(f)).isEmpty();
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    public void testSimpleBackbones(final SatSolver solver, final String solverDescription) {
        final FormulaFactory f = solver.getFactory();
        final SolverState state = solver.saveState();
        solver.add(parse(f, "a & b & ~c"));
        Backbone backbone = solver.backbone(v("a b c", f));
        assertThat(backbone.isSat()).isTrue();
        assertThat(backbone.getCompleteBackbone(f)).containsExactly(f.variable("a"), f.variable("b"),
                f.literal("c", false));
        solver.loadState(state);
        solver.add(parse(f, "~a & ~b & c"));
        backbone = solver.backbone(v("a c", f));
        assertThat(backbone.isSat()).isTrue();
        assertThat(backbone.getCompleteBackbone(f)).containsExactly(f.literal("a", false), f.variable("c"));
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    public void testSimpleFormulas(final SatSolver solver, final String solverDescription) {
        final FormulaFactory f = solver.getFactory();
        solver.add(parse(f, "(a => c | d) & (b => d | ~e) & (a | b)"));
        Backbone backbone = solver.backbone(v("a b c d e f", f));
        assertThat(backbone.isSat()).isTrue();
        assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        solver.add(parse(f, "a => b"));
        solver.add(parse(f, "b => a"));
        solver.add(parse(f, "~d"));
        backbone = solver.backbone(v("a b c d e f g h", f));
        assertThat(backbone.isSat()).isTrue();
        assertThat(backbone.getCompleteBackbone(f)).containsExactly(f.variable("a"), f.variable("b"),
                f.variable("c"),
                f.literal("d", false), f.literal("e", false));
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    public void testSimpleFormulasWithBuilderUsage(final SatSolver solver, final String solverDescription) {
        final FormulaFactory f = solver.getFactory();
        solver.add(parse(f, "(a => c | d) & (b => d | ~e) & (a | b)"));
        Backbone backbone = solver.execute(BackboneSolverFunction.builder(List.of(
                        f.variable("a"), f.variable("b"), f.variable("c"), f.variable("d"), f.variable("e"),
                        f.variable("f")
                )).build());
        assertThat(backbone.isSat()).isTrue();
        assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        solver.add(parse(f, "a => b"));
        solver.add(parse(f, "b => a"));
        solver.add(parse(f, "~d"));
        backbone = solver.backbone(v("a b c d e f g h", f));
        assertThat(backbone.isSat()).isTrue();
        assertThat(backbone.getCompleteBackbone(f)).containsExactly(f.variable("a"), f.variable("b"),
                f.variable("c"),
                f.literal("d", false), f.literal("e", false));
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    @LongRunningTag
    public void testRealFormulaIncremental1(final SatSolver solver, final String solverDescription)
            throws IOException, ParserException {
        final FormulaFactory f = solver.getFactory();
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/large_formula.txt");
        solver.add(formula);
        final List<String> expectedBackbones = new ArrayList<>();
        final BufferedReader reader =
                new BufferedReader(new FileReader("../test_files/backbones/backbone_large_formula.txt"));
        while (reader.ready()) {
            expectedBackbones.add(reader.readLine());
        }
        reader.close();
        Backbone backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(0), f));
        solver.add(f.variable("v411"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(1), f));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v385"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(2), f));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v275"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(3), f));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v188"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(4), f));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v103"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(5), f));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v404"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        assertThat(backbone.isSat()).isFalse();
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    @LongRunningTag
    public void testRealFormulaIncremental2(final SatSolver solver, final String solverDescription)
            throws IOException, ParserException {
        final FormulaFactory f = solver.getFactory();
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/small_formulas.txt");
        solver.add(formula);
        final List<String> expectedBackbones = new ArrayList<>();
        final BufferedReader reader =
                new BufferedReader(new FileReader("../test_files/backbones/backbone_small_formulas.txt"));
        while (reader.ready()) {
            expectedBackbones.add(reader.readLine());
        }
        reader.close();
        Backbone backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(0), f));
        solver.add(f.variable("v2609"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(1), f));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v2416"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(2), f));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v2048"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(3), f));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v39"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(4), f));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v1663"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(5), f));
        assertThat(backbone.isSat()).isTrue();
        solver.add(f.variable("v2238"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        assertThat(backbone.isSat()).isFalse();
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    @LongRunningTag
    public void testRealFormulaIncrementalDecremental1(final SatSolver solver, final String solverDescription)
            throws IOException, ParserException {
        final FormulaFactory f = solver.getFactory();
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/large_formula.txt");
        solver.add(formula);
        final SolverState state = solver.saveState();
        final List<String> expectedBackbones = new ArrayList<>();
        final BufferedReader reader =
                new BufferedReader(new FileReader("../test_files/backbones/backbone_large_formula.txt"));
        while (reader.ready()) {
            expectedBackbones.add(reader.readLine());
        }
        reader.close();
        Backbone backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(0), f));
        solver.add(f.variable("v411"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(1), f));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(parse(f, "v411 & v385"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(2), f));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(parse(f, "v411 & v385 & v275"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(3), f));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(parse(f, "v411 & v385 & v275 & v188"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(4), f));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(parse(f, "v411 & v385 & v275 & v188 & v103"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(5), f));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(parse(f, "v411 & v385 & v275 & v188 & v103 & v404"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        assertThat(backbone.isSat()).isFalse();
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("solvers")
    @LongRunningTag
    public void testRealFormulaIncrementalDecremental2(final SatSolver solver, final String solverDescription)
            throws IOException, ParserException {
        final FormulaFactory f = solver.getFactory();
        final Formula formula =
                FormulaReader.readFormula(f, "../test_files/formulas/small_formulas.txt");
        solver.add(formula);
        final SolverState state = solver.saveState();
        final List<String> expectedBackbones = new ArrayList<>();
        final BufferedReader reader =
                new BufferedReader(new FileReader("../test_files/backbones/backbone_small_formulas.txt"));
        while (reader.ready()) {
            expectedBackbones.add(reader.readLine());
        }
        reader.close();
        Backbone backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(0), f));
        solver.add(f.variable("v2609"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(1), f));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(parse(f, "v2609 & v2416"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(2), f));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(parse(f, "v2609 & v2416 & v2048"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(3), f));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(parse(f, "v2609 & v2416 & v2048 & v39"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(4), f));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(parse(f, "v2609 & v2416 & v2048 & v39 & v1663"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEqualTo(parseBackbone(expectedBackbones.get(5), f));
        assertThat(backbone.isSat()).isTrue();

        solver.loadState(state);
        solver.add(parse(f, "v2609 & v2416 & v2048 & v39 & v1663 & v2238"));
        backbone = solver.backbone(formula.variables(f));
        assertThat(backbone.getCompleteBackbone(f)).isEmpty();
        assertThat(backbone.isSat()).isFalse();
    }

    @Test
    public void testMiniCardSpecialCase() {
        final FormulaFactory f = FormulaFactory.caching();
        final FormulaParser p = new PropositionalParser(f);
        final SatSolver miniCard = SatSolver.newSolver(f, SatSolverConfig.builder().useAtMostClauses(true).build());
        miniCard.add(parse(f, "v1 + v2 + v3 + v4 + v5 + v6 = 1"));
        miniCard.add(parse(f, "v1234 + v50 + v60 = 1"));
        miniCard.add(
                parse(f, "(v1 => v1234) & (v2 => v1234) & (v3 => v1234) & (v4 => v1234) & (v5 => v50) & (v6 => v60)"));
        miniCard.add(parse(f, "~v6"));
        final Backbone backboneMC = miniCard.backbone(Arrays.asList(f.variable("v6"), f.variable("v60")));
        assertThat(backboneMC.getNegativeBackbone()).extracting(Variable::getName)
                .containsExactlyInAnyOrder("v6", "v60");
    }

    private SortedSet<Variable> v(final String s, final FormulaFactory f) {
        final SortedSet<Variable> vars = new TreeSet<>();
        for (final String name : s.split(" ")) {
            vars.add(f.variable(name));
        }
        return vars;
    }

    private SortedSet<Literal> parseBackbone(final String string, final FormulaFactory f) throws ParserException {
        final SortedSet<Literal> literals = new TreeSet<>();
        final FormulaParser parser = new PropositionalParser(f);
        for (final String lit : string.split(" ")) {
            literals.add((Literal) parser.parse(lit));
        }
        return literals;
    }
}
