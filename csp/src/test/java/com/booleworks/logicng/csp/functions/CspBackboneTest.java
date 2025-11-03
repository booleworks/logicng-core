package com.booleworks.logicng.csp.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.backbones.BackboneType;
import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspBackbone;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.io.readers.CspReader;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.solvers.SatSolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.SortedSet;

public class CspBackboneTest extends ParameterizedCspTest {
    @ParameterizedTest
    @MethodSource("algorithms")
    public void backboneTest(final CspEncodingContext context) throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final CspFactory cf = new CspFactory(f);
        final Formula formula = CspReader.readCsp(cf, "../test_files/csp/simple3.csp");
        final Csp csp = cf.buildCsp(formula);

        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResult result = EncodingResult.resultForSatSolver(f, solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);

        final CspBackbone backbone =
                CspBackboneGeneration.fromCsp(BackboneType.POSITIVE_AND_NEGATIVE, csp, cf)
                        .compute(solver, context, result);
        assertThat(backbone.getMandatory().keySet()).containsExactly(cf.getVariable("a"));
        assertThat(backbone.getMandatory().values()).containsExactly(3);
        assertThat(backbone.getForbidden().keySet()).containsExactly(cf.getVariable("c"));
        assertThat(backbone.getForbidden().size()).isEqualTo(1);
        final SortedSet<Integer> forbiddenVals = backbone.getForbidden().values().iterator().next();
        assertThat(forbiddenVals).containsExactly(22);
        assertThat(backbone.getBooleanBackbone().isSat()).isTrue();
        assertThat(backbone.getBooleanBackbone().getPositiveBackbone()).containsExactly(f.variable("A"));
        assertThat(backbone.getBooleanBackbone().getNegativeBackbone()).containsExactly(f.variable("B"));
        assertThat(backbone.getBooleanBackbone().getOptionalVariables()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    public void backbonePositiveTest(final CspEncodingContext context) throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final CspFactory cf = new CspFactory(f);
        final Formula formula = CspReader.readCsp(cf, "../test_files/csp/simple3.csp");
        final Csp csp = cf.buildCsp(formula);

        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResult result = EncodingResult.resultForSatSolver(f, solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);

        final CspBackbone backbone =
                CspBackboneGeneration.fromCsp(BackboneType.ONLY_POSITIVE, csp, cf).compute(solver, context, result);
        assertThat(backbone.getMandatory().keySet()).containsExactly(cf.getVariable("a"));
        assertThat(backbone.getMandatory().values()).containsExactly(3);
        assertThat(backbone.getForbidden()).isEmpty();
        assertThat(backbone.getBooleanBackbone().isSat()).isTrue();
        assertThat(backbone.getBooleanBackbone().getPositiveBackbone()).containsExactly(f.variable("A"));
        assertThat(backbone.getBooleanBackbone().getOptionalVariables()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    public void backboneNegativeTest(final CspEncodingContext context) throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final CspFactory cf = new CspFactory(f);
        final Formula formula = CspReader.readCsp(cf, "../test_files/csp/simple3.csp");
        final Csp csp = cf.buildCsp(formula);

        final SatSolver solver = SatSolver.newSolver(f);
        final EncodingResult result = EncodingResult.resultForSatSolver(f, solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);

        final CspBackbone backbone =
                CspBackboneGeneration.fromCsp(BackboneType.ONLY_NEGATIVE, csp, cf).compute(solver, context, result);
        assertThat(backbone.getMandatory()).isEmpty();
        assertThat(backbone.getForbidden()).hasSize(2);
        assertThat(backbone.getForbidden().containsKey(cf.getVariable("a"))).isTrue();
        assertThat(backbone.getForbidden().containsKey(cf.getVariable("c"))).isTrue();
        final SortedSet<Integer> forbiddenValsA = backbone.getForbidden().get(cf.getVariable("a"));
        final SortedSet<Integer> forbiddenValsC = backbone.getForbidden().get(cf.getVariable("c"));
        assertThat(forbiddenValsA).containsExactly(0, 1, 2, 4, 5, 6, 7, 8, 9, 10);
        assertThat(forbiddenValsC).containsExactly(22);
        assertThat(backbone.getBooleanBackbone().isSat()).isTrue();
        assertThat(backbone.getBooleanBackbone().getPositiveBackbone()).isEmpty();
        assertThat(backbone.getBooleanBackbone().getNegativeBackbone()).containsExactly(f.variable("B"));
        assertThat(backbone.getBooleanBackbone().getOptionalVariables()).isEmpty();
    }
}
