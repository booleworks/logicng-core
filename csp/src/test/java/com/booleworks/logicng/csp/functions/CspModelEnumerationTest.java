// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.csp.functions;

import static com.booleworks.logicng.csp.Common.assignmentFrom;
import static org.assertj.core.api.Assertions.assertThat;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CspModelEnumerationTest extends ParameterizedCspTest {

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testOnlyIntVariables(final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        final IntegerVariable a = cf.variable("a", 1, 2);
        final IntegerVariable b = cf.variable("b", 10, 12);
        final IntegerVariable c = cf.variable("c", -5, 12);
        final IntegerVariable d = cf.variable("d", 0, 1);
        final Formula formula = cf.eq(cf.add(a, c), b);
        final Csp csp = cf.buildCsp(formula);
        final OrderEncodingContext context = CspEncodingContext.order();
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(cf.encodeCsp(csp, context));
        final List<CspAssignment> models = CspModelEnumeration.builderFromVariables(cf, List.of(a, b, d), List.of())
                .build()
                .enumerate(solver, context);
        assertThat(models).containsExactlyInAnyOrder(
                assignmentFrom(a, 2, b, 10, d, 0),
                assignmentFrom(a, 1, b, 10, d, 0),
                assignmentFrom(a, 2, b, 11, d, 0),
                assignmentFrom(a, 1, b, 11, d, 0),
                assignmentFrom(a, 2, b, 12, d, 0),
                assignmentFrom(a, 1, b, 12, d, 0),
                assignmentFrom(a, 2, b, 10, d, 1),
                assignmentFrom(a, 1, b, 10, d, 1),
                assignmentFrom(a, 2, b, 11, d, 1),
                assignmentFrom(a, 1, b, 11, d, 1),
                assignmentFrom(a, 2, b, 12, d, 1),
                assignmentFrom(a, 1, b, 12, d, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testOnlyBooleanVariables(final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        final Variable a = f.variable("A");
        final Variable b = f.variable("B");
        final Variable c = f.variable("C");
        final Variable d = f.variable("D");
        final Formula formula = f.or(a, f.and(b, c));
        final Csp csp = cf.buildCsp(formula);
        final OrderEncodingContext context = CspEncodingContext.order();
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(cf.encodeCsp(csp, context));
        final List<CspAssignment> models = CspModelEnumeration.builderFromVariables(cf, List.of(), List.of(a, b, d))
                .build()
                .enumerate(solver, context);
        assertThat(models).containsExactlyInAnyOrder(
                assignmentFrom(a, b, d),
                assignmentFrom(a, b.negate(f), d),
                assignmentFrom(a.negate(f), b, d),
                assignmentFrom(a, b, d.negate(f)),
                assignmentFrom(a, b.negate(f), d.negate(f)),
                assignmentFrom(a.negate(f), b, d.negate(f))
        );
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testMixed(final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        final IntegerVariable a = cf.variable("a", 1, 3);
        final IntegerVariable b = cf.variable("b", 2, 4);
        final IntegerVariable c = cf.variable("c", -1, 3);
        final Variable A = f.variable("A");
        final Variable B = f.variable("B");
        final Formula formula = f.and(f.equivalence(A.negate(f), B), cf.eq(a, b), cf.lt(c, a));
        final Csp csp = cf.buildCsp(formula);
        final OrderEncodingContext context = CspEncodingContext.order();
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(cf.encodeCsp(csp, context));
        final List<CspAssignment> models = CspModelEnumeration.builderFromVariables(cf, List.of(a, b, c), List.of(A))
                .build()
                .enumerate(solver, context);
        assertThat(models).containsExactlyInAnyOrder(
                assignmentFrom(a, 2, b, 2, c, -1, A.negate(f)),
                assignmentFrom(a, 3, b, 3, c, -1, A.negate(f)),
                assignmentFrom(a, 2, b, 2, c, 0, A.negate(f)),
                assignmentFrom(a, 3, b, 3, c, 0, A.negate(f)),
                assignmentFrom(a, 2, b, 2, c, 1, A.negate(f)),
                assignmentFrom(a, 3, b, 3, c, 1, A.negate(f)),
                assignmentFrom(a, 3, b, 3, c, 2, A.negate(f)),
                assignmentFrom(a, 2, b, 2, c, -1, A),
                assignmentFrom(a, 3, b, 3, c, -1, A),
                assignmentFrom(a, 2, b, 2, c, 0, A),
                assignmentFrom(a, 3, b, 3, c, 0, A),
                assignmentFrom(a, 2, b, 2, c, 1, A),
                assignmentFrom(a, 3, b, 3, c, 1, A),
                assignmentFrom(a, 3, b, 3, c, 2, A)
        );
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testMixedFromCsp(final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        final IntegerVariable a = cf.variable("a", 1, 3);
        final IntegerVariable b = cf.variable("b", 2, 4);
        final IntegerVariable c = cf.variable("c", -1, 3);
        final Variable A = f.variable("A");
        final Formula formula = f.and(A.negate(f), cf.eq(a, b), cf.lt(c, a));
        final Csp csp = cf.buildCsp(formula);
        final OrderEncodingContext context = CspEncodingContext.order();
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(cf.encodeCsp(csp, context));
        final List<CspAssignment> models = CspModelEnumeration.builderFromCsp(cf, csp)
                .build()
                .enumerate(solver, context);
        assertThat(models).containsExactlyInAnyOrder(
                assignmentFrom(a, 2, b, 2, c, -1, A.negate(f)),
                assignmentFrom(a, 3, b, 3, c, -1, A.negate(f)),
                assignmentFrom(a, 2, b, 2, c, 0, A.negate(f)),
                assignmentFrom(a, 3, b, 3, c, 0, A.negate(f)),
                assignmentFrom(a, 2, b, 2, c, 1, A.negate(f)),
                assignmentFrom(a, 3, b, 3, c, 1, A.negate(f)),
                assignmentFrom(a, 3, b, 3, c, 2, A.negate(f))
        );
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testAdditionalVariables(final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        final IntegerVariable a = cf.variable("a", 1, 3);
        final IntegerVariable b = cf.variable("b", 2, 4);
        final IntegerVariable c = cf.variable("c", -5, 12);
        final IntegerVariable d = cf.variable("d", 5, 8);
        final IntegerVariable e = cf.variable("e", 10, 100);
        final Variable A = f.variable("A");
        final Variable B = f.variable("B");
        final Variable C = f.variable("C");
        final Formula formula = f.and(f.or(A.negate(f), B), cf.eq(a, b), cf.eq(c, d));
        final Csp csp = cf.buildCsp(formula);
        final OrderEncodingContext context = CspEncodingContext.order();
        final SatSolver solver = SatSolver.newSolver(f);
        solver.add(cf.encodeCsp(csp, context));
        final List<CspAssignment> models = CspModelEnumeration.builderFromVariables(cf, List.of(a, b), List.of(A))
                .additionalIntegerVariables(List.of(c, d, e))
                .additionalBooleanVariables(List.of(B, C))
                .build()
                .enumerate(solver, context);
        assertThat(models).hasSize(4);
        assertPartialModels(models, List.of(
                assignmentFrom(a, 2, b, 2, A.negate(f)),
                assignmentFrom(a, 2, b, 2, A),
                assignmentFrom(a, 3, b, 3, A.negate(f)),
                assignmentFrom(a, 3, b, 3, A)
        ), m -> {
            final Formula restricted = formula.transform(new CspRestrictionFunction(cf, m));
            final boolean isCompleteAssignment = CspDecomposition.decompose(cf, restricted).getClauses().isEmpty();
            final boolean containsSmallE = m.getIntegerAssignments().containsKey(e)
                    && e.getDomain().contains(m.getIntegerAssignments().get(e));
            final boolean containsC = m.negativeBooleans().contains(C.negate(f)) || m.positiveBooleans().contains(C);
            return isCompleteAssignment && containsSmallE && containsC;
        });
    }

    private void assertPartialModels(final Collection<CspAssignment> models,
                                     final Collection<CspAssignment> expectedPartialModels,
                                     final Predicate<CspAssignment> additionalCondition) {
        assertThat(models).hasSize(expectedPartialModels.size());
        final List<CspAssignment> violatesCondition = models.stream()
                .filter(m -> !additionalCondition.test(m))
                .collect(Collectors.toList());
        assertThat(violatesCondition).isEmpty();

        final Set<CspAssignment> modelsTempSet = new HashSet<>(models);
        final List<CspAssignment> missingPartialModels = new ArrayList<>();
        for (final CspAssignment expected : expectedPartialModels) {
            final Optional<CspAssignment> got = modelsTempSet.stream().filter(m ->
                    expected.getIntegerAssignments().entrySet().stream().allMatch(e ->
                            Objects.equals(m.getIntegerAssignments().get(e.getKey()), e.getValue()))
                            && m.negativeBooleans().containsAll(expected.negativeBooleans())
                            && m.positiveBooleans().containsAll(expected.positiveBooleans())
            ).findFirst();
            if (got.isEmpty()) {
                missingPartialModels.add(expected);
            } else {
                modelsTempSet.remove(got.get());
            }
        }
        assertThat(missingPartialModels).isEmpty();
        assertThat(modelsTempSet).isEmpty();
    }
}
