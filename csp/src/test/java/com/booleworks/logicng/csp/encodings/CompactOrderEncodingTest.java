package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ExampleFormulas;
import com.booleworks.logicng.csp.LongRunningTag;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.functions.CspModelEnumeration;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.datastructures.EncodingResult;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.solvers.SatSolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CompactOrderEncodingTest extends ParameterizedCspTest {
    @ParameterizedTest
    @MethodSource("cspFactories")
    @LongRunningTag
    public void compareModelsWithOrderLong(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", -10, 2);
        final IntegerVariable b = cf.variable("b", 2, 100);
        final IntegerVariable c = cf.variable("c", -100, -15);
        final IntegerVariable d = cf.variable("d", -5, 5);
        final IntegerVariable e = cf.variable("e", 0, 12);
        final List<Formula> formulas = List.of(
                cf.eq(a, cf.add(b, c)),
                cf.eq(d, e),
                cf.allDifferent(List.of(a, d, e)),
                ExampleFormulas.arithmJavaCreamSolver(cf)
        );

        for (final Formula formula : formulas) {
            final Csp csp = cf.buildCsp(formula);
            testModels(csp, cf);
        }
    }

    private void testModels(final Csp csp, final CspFactory cf) {
        final CspEncodingContext context0 = CspEncodingContext.order();
        final CspEncodingContext context1 = CspEncodingContext.compactOrder(10);
        final CspEncodingContext context2 = CspEncodingContext.compactOrder(5);
        final CspEncodingContext context3 = CspEncodingContext.compactOrder(20);
        final CspEncodingContext context4 = CspEncodingContext.compactOrder(3);
        final CspEncodingContext context5 = CspEncodingContext.compactOrder(2);

        final List<CspAssignment> results0 = enumerate(csp, context0, cf);
        final List<CspAssignment> results1 = enumerate(csp, context1, cf);
        final List<CspAssignment> results2 = enumerate(csp, context2, cf);
        final List<CspAssignment> results3 = enumerate(csp, context3, cf);
        final List<CspAssignment> results4 = enumerate(csp, context4, cf);
        final List<CspAssignment> results5 = enumerate(csp, context5, cf);

        assertThat(results1).containsExactlyInAnyOrderElementsOf(results0);
        assertThat(results2).containsExactlyInAnyOrderElementsOf(results0);
        assertThat(results3).containsExactlyInAnyOrderElementsOf(results0);
        assertThat(results4).containsExactlyInAnyOrderElementsOf(results0);
        assertThat(results5).containsExactlyInAnyOrderElementsOf(results0);
    }

    private List<CspAssignment> enumerate(final Csp csp, final CspEncodingContext context, final CspFactory cf) {
        final SatSolver solver = SatSolver.newSolver(cf.getFormulaFactory());
        final EncodingResult result =
                EncodingResult.resultForSatSolver(cf.getFormulaFactory(), solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);
        return CspModelEnumeration.enumerate(solver, csp, context, cf);
    }

    private List<CspAssignment> enumerateOrder(final CspPredicate p, final List<IntegerVariable> vars,
                                               final CspFactory cf) {
        final SatSolver solver = SatSolver.newSolver(cf.getFormulaFactory());
        final CspEncodingContext context = CspEncodingContext.order();
        final EncodingResult result = EncodingResult.resultForSatSolver(cf.getFormulaFactory(),
                solver.getUnderlyingSolver(), null);
        final Csp expectedCsp = cf.buildCsp(p);
        cf.encodeCsp(expectedCsp, context, result);
        return CspModelEnumeration.enumerate(solver, vars, expectedCsp.getVisibleBooleanVariables(), context,
                cf);

    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    @LongRunningTag
    public void testIncrementalLong(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", -10, 2);
        final IntegerVariable b = cf.variable("b", 2, 30);
        final IntegerVariable c = cf.variable("c", -20, -15);
        final IntegerVariable d = cf.variable("d", -5, 5);
        final IntegerVariable e = cf.variable("e", 0, 12);
        final List<CspPredicate> formulas = List.of(
                cf.eq(a, cf.add(b, c)),
                cf.eq(d, e),
                cf.allDifferent(List.of(a, d, e))
        );

        final CspEncodingContext context = CspEncodingContext.compactOrder(3);
        for (final CspPredicate p : formulas) {
            final SatSolver solver = SatSolver.newSolver(cf.getFormulaFactory());
            final EncodingResult result =
                    EncodingResult.resultForSatSolver(cf.getFormulaFactory(), solver.getUnderlyingSolver(), null);
            for (final IntegerVariable v : p.variables()) {
                cf.encodeVariable(v, context, result);
            }
            cf.encodeConstraint(p, context, result);
            final List<CspAssignment> models =
                    CspModelEnumeration.enumerate(solver, List.of(a, b, c, d, e), Collections.emptyList(), context, cf);

            final List<CspAssignment> expectedModels = enumerateOrder(p, List.of(a, b, c, d, e), cf);
            assertThat(models).containsExactlyInAnyOrderElementsOf(expectedModels);
        }
    }
}
