package com.booleworks.logicng.csp.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import com.booleworks.logicng.csp.datastructures.Csp;
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

        final CspBackbone backbone = CspBackbone.calculateBackbone(solver, csp, context, result, cf);
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
}
