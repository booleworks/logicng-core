package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import com.booleworks.logicng.csp.datastructures.Csp;
import com.booleworks.logicng.csp.datastructures.CspAssignment;
import com.booleworks.logicng.csp.encodings.CspEncodingContext;
import com.booleworks.logicng.csp.encodings.OrderEncodingContext;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.solvers.SatSolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static com.booleworks.logicng.csp.Common.assignmentFrom;
import static org.assertj.core.api.Assertions.assertThat;

public class CspModelEnumerationTest extends ParameterizedCspTest {

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testOnlyIntVariables(final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        final IntegerVariable a = cf.variable("a", 1, 2);
        final IntegerVariable b = cf.variable("b", 10, 12);
        final IntegerVariable c = cf.variable("c", -5, 12);
        final Formula formula = cf.eq(cf.add(a, c), b);
        final Csp csp = cf.buildCsp(formula);
        final OrderEncodingContext context = CspEncodingContext.order();
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(cf.encodeCsp(csp, context));
        final List<CspAssignment> models = CspModelEnumeration.enumerate(solver, csp, context, cf);
        assertThat(models).hasSize(6);
        assertThat(models).containsExactlyInAnyOrder(
                assignmentFrom(a, 2, c, 8, b, 10),
                assignmentFrom(a, 1, c, 9, b, 10),
                assignmentFrom(a, 2, c, 9, b, 11),
                assignmentFrom(a, 1, c, 10, b, 11),
                assignmentFrom(a, 2, c, 10, b, 12),
                assignmentFrom(a, 1, c, 11, b, 12)
        );
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testOnlyBooleanVariables(final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        final Variable a = f.variable("A");
        final Variable b = f.variable("B");
        final Variable c = f.variable("C");
        final Formula formula = f.or(a, f.and(b, c));
        final Csp csp = cf.buildCsp(formula);
        final OrderEncodingContext context = CspEncodingContext.order();
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(cf.encodeCsp(csp, context));
        final List<CspAssignment> models = CspModelEnumeration.enumerate(solver, csp, context, cf);
        assertThat(models).hasSize(5);
        assertThat(models).containsExactlyInAnyOrder(
                assignmentFrom(a, b, c),
                assignmentFrom(a, b.negate(f), c.negate(f)),
                assignmentFrom(a, b, c.negate(f)),
                assignmentFrom(a, b.negate(f), c),
                assignmentFrom(a.negate(f), b, c)
        );
    }
}
