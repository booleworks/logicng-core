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

public class TermFunctionEncodingTest extends ParameterizedCspTest {

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testAdd(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", 2, 4);
        final IntegerVariable b = cf.variable("B", -2, 1);
        final IntegerVariable t = cf.variable("T", -5, 10);
        final CspPredicate p = cf.eq(t, cf.add(a, b));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, 2, b, -2, t, 0),
                assignmentFrom(a, 2, b, -1, t, 1),
                assignmentFrom(a, 2, b, 0, t, 2),
                assignmentFrom(a, 2, b, 1, t, 3),
                assignmentFrom(a, 3, b, -2, t, 1),
                assignmentFrom(a, 3, b, -1, t, 2),
                assignmentFrom(a, 3, b, 0, t, 3),
                assignmentFrom(a, 3, b, 1, t, 4),
                assignmentFrom(a, 4, b, -2, t, 2),
                assignmentFrom(a, 4, b, -1, t, 3),
                assignmentFrom(a, 4, b, 0, t, 4),
                assignmentFrom(a, 4, b, 1, t, 5)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testAdd2(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", 2, 4);
        final IntegerVariable t = cf.variable("T", 0, 20);
        final CspPredicate p = cf.eq(t, cf.add(a, a, a));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, 2, t, 6),
                assignmentFrom(a, 3, t, 9),
                assignmentFrom(a, 4, t, 12)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testSub(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", 2, 4);
        final IntegerVariable b = cf.variable("B", -2, 1);
        final IntegerVariable t = cf.variable("T", -5, 10);
        final CspPredicate p = cf.eq(t, cf.sub(a, b));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, 2, b, -2, t, 4),
                assignmentFrom(a, 2, b, -1, t, 3),
                assignmentFrom(a, 2, b, 0, t, 2),
                assignmentFrom(a, 2, b, 1, t, 1),
                assignmentFrom(a, 3, b, -2, t, 5),
                assignmentFrom(a, 3, b, -1, t, 4),
                assignmentFrom(a, 3, b, 0, t, 3),
                assignmentFrom(a, 3, b, 1, t, 2),
                assignmentFrom(a, 4, b, -2, t, 6),
                assignmentFrom(a, 4, b, -1, t, 5),
                assignmentFrom(a, 4, b, 0, t, 4),
                assignmentFrom(a, 4, b, 1, t, 3)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testSub2(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", 2, 4);
        final IntegerVariable t = cf.variable("T", 0, 20);
        final CspPredicate p = cf.eq(t, cf.sub(a, a));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, 2, t, 0),
                assignmentFrom(a, 3, t, 0),
                assignmentFrom(a, 4, t, 0)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testConstantMul(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", 2, 5);
        final IntegerVariable t = cf.variable("T", 0, 20);
        final CspPredicate p = cf.eq(t, cf.mul(3, a));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, 2, t, 6),
                assignmentFrom(a, 3, t, 9),
                assignmentFrom(a, 4, t, 12),
                assignmentFrom(a, 5, t, 15)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testMul(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", 2, 5);
        final IntegerVariable b = cf.variable("B", 0, 2);
        final IntegerVariable t = cf.variable("T", -5, 25);
        final CspPredicate p = cf.eq(t, cf.mul(a, b));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, 2, b, 0, t, 0),
                assignmentFrom(a, 2, b, 1, t, 2),
                assignmentFrom(a, 2, b, 2, t, 4),
                assignmentFrom(a, 3, b, 0, t, 0),
                assignmentFrom(a, 3, b, 1, t, 3),
                assignmentFrom(a, 3, b, 2, t, 6),
                assignmentFrom(a, 4, b, 0, t, 0),
                assignmentFrom(a, 4, b, 1, t, 4),
                assignmentFrom(a, 4, b, 2, t, 8),
                assignmentFrom(a, 5, b, 0, t, 0),
                assignmentFrom(a, 5, b, 1, t, 5),
                assignmentFrom(a, 5, b, 2, t, 10)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testSquare(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", 0, 5);
        final IntegerVariable t = cf.variable("T", 0, 30);
        final CspPredicate p = cf.eq(t, cf.mul(a, a));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, 0, t, 0),
                assignmentFrom(a, 1, t, 1),
                assignmentFrom(a, 2, t, 4),
                assignmentFrom(a, 3, t, 9),
                assignmentFrom(a, 4, t, 16),
                assignmentFrom(a, 5, t, 25)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testAbs(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", -5, 5);
        final IntegerVariable t = cf.variable("T", -6, 6);
        final CspPredicate p = cf.eq(t, cf.abs(a));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, -5, t, 5),
                assignmentFrom(a, -4, t, 4),
                assignmentFrom(a, -3, t, 3),
                assignmentFrom(a, -2, t, 2),
                assignmentFrom(a, -1, t, 1),
                assignmentFrom(a, 0, t, 0),
                assignmentFrom(a, 1, t, 1),
                assignmentFrom(a, 2, t, 2),
                assignmentFrom(a, 3, t, 3),
                assignmentFrom(a, 4, t, 4),
                assignmentFrom(a, 5, t, 5)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testDiv(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", -10, 10);
        final IntegerVariable t = cf.variable("T", -5, 5);
        final CspPredicate p = cf.eq(t, cf.div(a, 4));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, -10, t, -3),
                assignmentFrom(a, -9, t, -3),
                assignmentFrom(a, -8, t, -2),
                assignmentFrom(a, -7, t, -2),
                assignmentFrom(a, -6, t, -2),
                assignmentFrom(a, -5, t, -2),
                assignmentFrom(a, -4, t, -1),
                assignmentFrom(a, -3, t, -1),
                assignmentFrom(a, -2, t, -1),
                assignmentFrom(a, -1, t, -1),
                assignmentFrom(a, 0, t, 0),
                assignmentFrom(a, 1, t, 0),
                assignmentFrom(a, 2, t, 0),
                assignmentFrom(a, 3, t, 0),
                assignmentFrom(a, 4, t, 1),
                assignmentFrom(a, 5, t, 1),
                assignmentFrom(a, 6, t, 1),
                assignmentFrom(a, 7, t, 1),
                assignmentFrom(a, 8, t, 2),
                assignmentFrom(a, 9, t, 2),
                assignmentFrom(a, 10, t, 2)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testMod(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", 0, 20);
        final IntegerVariable t = cf.variable("T", -5, 5);
        final CspPredicate p = cf.eq(t, cf.mod(a, 4));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, 0, t, 0),
                assignmentFrom(a, 1, t, 1),
                assignmentFrom(a, 2, t, 2),
                assignmentFrom(a, 3, t, 3),
                assignmentFrom(a, 4, t, 0),
                assignmentFrom(a, 5, t, 1),
                assignmentFrom(a, 6, t, 2),
                assignmentFrom(a, 7, t, 3),
                assignmentFrom(a, 8, t, 0),
                assignmentFrom(a, 9, t, 1),
                assignmentFrom(a, 10, t, 2),
                assignmentFrom(a, 11, t, 3),
                assignmentFrom(a, 12, t, 0),
                assignmentFrom(a, 13, t, 1),
                assignmentFrom(a, 14, t, 2),
                assignmentFrom(a, 15, t, 3),
                assignmentFrom(a, 16, t, 0),
                assignmentFrom(a, 17, t, 1),
                assignmentFrom(a, 18, t, 2),
                assignmentFrom(a, 19, t, 3),
                assignmentFrom(a, 20, t, 0)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testMod2(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", 0, 20);
        final IntegerVariable t = cf.variable("T", -5, 5);
        final CspPredicate p = cf.eq(t, cf.mod(a, 1));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, 0, t, 0),
                assignmentFrom(a, 1, t, 0),
                assignmentFrom(a, 2, t, 0),
                assignmentFrom(a, 3, t, 0),
                assignmentFrom(a, 4, t, 0),
                assignmentFrom(a, 5, t, 0),
                assignmentFrom(a, 6, t, 0),
                assignmentFrom(a, 7, t, 0),
                assignmentFrom(a, 8, t, 0),
                assignmentFrom(a, 9, t, 0),
                assignmentFrom(a, 10, t, 0),
                assignmentFrom(a, 11, t, 0),
                assignmentFrom(a, 12, t, 0),
                assignmentFrom(a, 13, t, 0),
                assignmentFrom(a, 14, t, 0),
                assignmentFrom(a, 15, t, 0),
                assignmentFrom(a, 16, t, 0),
                assignmentFrom(a, 17, t, 0),
                assignmentFrom(a, 18, t, 0),
                assignmentFrom(a, 19, t, 0),
                assignmentFrom(a, 20, t, 0)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testMax(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", -2, 2);
        final IntegerVariable b = cf.variable("B", 0, 3);
        final IntegerVariable t = cf.variable("T", -5, 7);
        final CspPredicate p = cf.eq(t, cf.max(a, b));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, -2, b, 0, t, 0),
                assignmentFrom(a, -2, b, 1, t, 1),
                assignmentFrom(a, -2, b, 2, t, 2),
                assignmentFrom(a, -2, b, 3, t, 3),
                assignmentFrom(a, -1, b, 0, t, 0),
                assignmentFrom(a, -1, b, 1, t, 1),
                assignmentFrom(a, -1, b, 2, t, 2),
                assignmentFrom(a, -1, b, 3, t, 3),
                assignmentFrom(a, 0, b, 0, t, 0),
                assignmentFrom(a, 0, b, 1, t, 1),
                assignmentFrom(a, 0, b, 2, t, 2),
                assignmentFrom(a, 0, b, 3, t, 3),
                assignmentFrom(a, 1, b, 0, t, 1),
                assignmentFrom(a, 1, b, 1, t, 1),
                assignmentFrom(a, 1, b, 2, t, 2),
                assignmentFrom(a, 1, b, 3, t, 3),
                assignmentFrom(a, 2, b, 0, t, 2),
                assignmentFrom(a, 2, b, 1, t, 2),
                assignmentFrom(a, 2, b, 2, t, 2),
                assignmentFrom(a, 2, b, 3, t, 3)
        ));
    }

    @ParameterizedTest
    @MethodSource("algorithmsAndFactories")
    public void testMin(final CspFactory cf, final CspEncodingContext context) {
        final IntegerVariable a = cf.variable("A", -2, 2);
        final IntegerVariable b = cf.variable("B", 0, 3);
        final IntegerVariable t = cf.variable("T", -5, 7);
        final CspPredicate p = cf.eq(t, cf.min(a, b));

        checkModels(p, cf, context, List.of(
                assignmentFrom(a, -2, b, 0, t, -2),
                assignmentFrom(a, -2, b, 1, t, -2),
                assignmentFrom(a, -2, b, 2, t, -2),
                assignmentFrom(a, -2, b, 3, t, -2),
                assignmentFrom(a, -1, b, 0, t, -1),
                assignmentFrom(a, -1, b, 1, t, -1),
                assignmentFrom(a, -1, b, 2, t, -1),
                assignmentFrom(a, -1, b, 3, t, -1),
                assignmentFrom(a, 0, b, 0, t, 0),
                assignmentFrom(a, 0, b, 1, t, 0),
                assignmentFrom(a, 0, b, 2, t, 0),
                assignmentFrom(a, 0, b, 3, t, 0),
                assignmentFrom(a, 1, b, 0, t, 0),
                assignmentFrom(a, 1, b, 1, t, 1),
                assignmentFrom(a, 1, b, 2, t, 1),
                assignmentFrom(a, 1, b, 3, t, 1),
                assignmentFrom(a, 2, b, 0, t, 0),
                assignmentFrom(a, 2, b, 1, t, 1),
                assignmentFrom(a, 2, b, 2, t, 2),
                assignmentFrom(a, 2, b, 3, t, 2)
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
