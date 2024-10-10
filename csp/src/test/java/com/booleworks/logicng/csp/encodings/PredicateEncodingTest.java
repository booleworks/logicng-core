package com.booleworks.logicng.csp.encodings;

import com.booleworks.logicng.csp.CspFactory;
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

import java.util.List;

import static com.booleworks.logicng.csp.Common.assignmentFrom;
import static org.assertj.core.api.Assertions.assertThat;

public class PredicateEncodingTest extends ParameterizedCspTest {

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testEq(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", -2, 2);
        final IntegerVariable b = cf.variable("B", -2, 2);
        final CspPredicate p = cf.eq(a, b);

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, -2, b, -2),
                assignmentFrom(a, -1, b, -1),
                assignmentFrom(a, 0, b, 0),
                assignmentFrom(a, 1, b, 1),
                assignmentFrom(a, 2, b, 2)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testNe(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", -2, 2);
        final IntegerVariable b = cf.variable("B", -2, 2);
        final CspPredicate p = cf.ne(a, b);

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, -2, b, -1),
                assignmentFrom(a, -2, b, 0),
                assignmentFrom(a, -2, b, 1),
                assignmentFrom(a, -2, b, 2),
                assignmentFrom(a, -1, b, -2),
                assignmentFrom(a, -1, b, 0),
                assignmentFrom(a, -1, b, 1),
                assignmentFrom(a, -1, b, 2),
                assignmentFrom(a, 0, b, -2),
                assignmentFrom(a, 0, b, -1),
                assignmentFrom(a, 0, b, 1),
                assignmentFrom(a, 0, b, 2),
                assignmentFrom(a, 1, b, -2),
                assignmentFrom(a, 1, b, -1),
                assignmentFrom(a, 1, b, 0),
                assignmentFrom(a, 1, b, 2),
                assignmentFrom(a, 2, b, -2),
                assignmentFrom(a, 2, b, -1),
                assignmentFrom(a, 2, b, 0),
                assignmentFrom(a, 2, b, 1)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testLe(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", -2, 2);
        final IntegerVariable b = cf.variable("B", -2, 2);
        final CspPredicate p = cf.le(a, b);

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, -2, b, -2),
                assignmentFrom(a, -1, b, -1),
                assignmentFrom(a, -2, b, -1),
                assignmentFrom(a, 0, b, 0),
                assignmentFrom(a, -1, b, 0),
                assignmentFrom(a, -2, b, 0),
                assignmentFrom(a, 0, b, 1),
                assignmentFrom(a, -1, b, 1),
                assignmentFrom(a, -2, b, 1),
                assignmentFrom(a, 1, b, 1),
                assignmentFrom(a, 0, b, 2),
                assignmentFrom(a, -1, b, 2),
                assignmentFrom(a, -2, b, 2),
                assignmentFrom(a, 1, b, 2),
                assignmentFrom(a, 2, b, 2)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testLt(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", -2, 2);
        final IntegerVariable b = cf.variable("B", -2, 2);
        final CspPredicate p = cf.lt(a, b);

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, -2, b, -1),
                assignmentFrom(a, -1, b, 0),
                assignmentFrom(a, -2, b, 0),
                assignmentFrom(a, 0, b, 1),
                assignmentFrom(a, -1, b, 1),
                assignmentFrom(a, -2, b, 1),
                assignmentFrom(a, 0, b, 2),
                assignmentFrom(a, -1, b, 2),
                assignmentFrom(a, -2, b, 2),
                assignmentFrom(a, 1, b, 2)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testGe(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", -2, 2);
        final IntegerVariable b = cf.variable("B", -2, 2);
        final CspPredicate p = cf.ge(a, b);

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, -2, b, -2),
                assignmentFrom(a, -1, b, -2),
                assignmentFrom(a, 0, b, -2),
                assignmentFrom(a, 1, b, -2),
                assignmentFrom(a, 2, b, -2),
                assignmentFrom(a, -1, b, -1),
                assignmentFrom(a, 0, b, -1),
                assignmentFrom(a, 1, b, -1),
                assignmentFrom(a, 2, b, -1),
                assignmentFrom(a, 0, b, 0),
                assignmentFrom(a, 1, b, 0),
                assignmentFrom(a, 2, b, 0),
                assignmentFrom(a, 1, b, 1),
                assignmentFrom(a, 2, b, 1),
                assignmentFrom(a, 2, b, 2)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testGt(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", -2, 2);
        final IntegerVariable b = cf.variable("B", -2, 2);
        final CspPredicate p = cf.gt(a, b);

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, -1, b, -2),
                assignmentFrom(a, 0, b, -2),
                assignmentFrom(a, 1, b, -2),
                assignmentFrom(a, 2, b, -2),
                assignmentFrom(a, 0, b, -1),
                assignmentFrom(a, 1, b, -1),
                assignmentFrom(a, 2, b, -1),
                assignmentFrom(a, 1, b, 0),
                assignmentFrom(a, 2, b, 0),
                assignmentFrom(a, 2, b, 1)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testAllDifferent(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", 0, 2);
        final IntegerVariable b = cf.variable("B", 0, 2);
        final IntegerVariable c = cf.variable("C", 0, 2);
        final CspPredicate p = cf.allDifferent(List.of(a, b, c));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, 0, b, 1, c, 2),
                assignmentFrom(a, 0, b, 2, c, 1),
                assignmentFrom(a, 1, b, 0, c, 2),
                assignmentFrom(a, 1, b, 2, c, 0),
                assignmentFrom(a, 2, b, 1, c, 0),
                assignmentFrom(a, 2, b, 0, c, 1)
        ));
    }

    private void checkModels(final Formula formula, final CspFactory cf, final CspEncodingContext context,
                             final List<CspAssignment> expected) {
        final Csp csp = cf.buildCsp(formula);
        final SatSolver solver = SatSolver.newSolver(cf.getFormulaFactory());
        final EncodingResult result =
                EncodingResult.resultForSatSolver(cf.getFormulaFactory(), solver.getUnderlyingSolver(), null);
        cf.encodeCsp(csp, context, result);
        final List<CspAssignment> models = CspModelEnumeration.enumerate(solver, csp, context, cf);
        assertThat(models).containsExactlyInAnyOrderElementsOf(expected);
    }
}
