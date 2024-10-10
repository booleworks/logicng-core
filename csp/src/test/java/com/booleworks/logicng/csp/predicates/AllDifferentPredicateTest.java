package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.Common;
import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.Or;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

public class AllDifferentPredicateTest extends ParameterizedCspTest {
    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testType(final CspFactory cf) {
        assertThat(cf.allDifferent(List.of()).getPredicateType()).isEqualTo(CspPredicate.Type.ALLDIFFERENT);
        assertThat(cf.allDifferent(List.of(cf.variable("a", 0, 20), cf.one(), cf.constant(2)))
                .getPredicateType()).isEqualTo(
                CspPredicate.Type.ALLDIFFERENT);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testSimpleExamples(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 5);
        final IntegerVariable b = cf.variable("b", 10, 15);
        final Term term1 = cf.add(a, b);
        final Term term2 = cf.mul(2, a);
        final AllDifferentPredicate pred1 = cf.allDifferent(List.of(cf.zero(), cf.one()));
        final AllDifferentPredicate pred2 = cf.allDifferent(List.of(term1, term2));
        final AllDifferentPredicate pred3 =
                cf.allDifferent(List.of(term1, term2, cf.zero(), cf.one(), cf.constant(20)));

        assertThat(pred1.terms).containsExactlyInAnyOrder(cf.zero(), cf.one());
        assertThat(pred1.type).isEqualTo(CspPredicate.Type.ALLDIFFERENT);

        assertThat(pred2.terms).containsExactlyInAnyOrder(term1, term2);
        assertThat(pred2.type).isEqualTo(CspPredicate.Type.ALLDIFFERENT);

        assertThat(pred3.terms).containsExactlyInAnyOrder(term1, term2, cf.zero(), cf.one(), cf.constant(20));
        assertThat(pred3.type).isEqualTo(CspPredicate.Type.ALLDIFFERENT);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testNegation(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 5);
        final IntegerVariable b = cf.variable("b", 10, 15);
        final Term term1 = cf.add(a, b);
        final Term term2 = cf.mul(2, a);
        final Formula pred1 = cf.allDifferent(List.of(cf.zero(), cf.one())).negate(cf);
        final Formula pred2 = cf.allDifferent(List.of(term1, term2)).negate(cf);
        final Formula pred3 = cf.allDifferent(List.of(term1, term2, cf.zero(), cf.one(), cf.constant(20))).negate(cf);

        assertThat(pred1).isEqualTo(cf.eq(cf.zero(), cf.one()));
        assertThat(pred2).isEqualTo(cf.eq(cf.add(a, b), cf.mul(2, a)));
        assertThat(pred3.getType()).isEqualTo(FType.OR);
        assertThat(((Or) pred3).getOperands()).containsExactlyInAnyOrder(
                cf.eq(cf.add(a, b), cf.mul(2, a)), cf.eq(cf.add(a, b), cf.zero()), cf.eq(cf.add(a, b), cf.one()),
                cf.eq(cf.add(a, b), cf.constant(20)), cf.eq(cf.mul(2, a), cf.zero()), cf.eq(cf.mul(2, a), cf.one()),
                cf.eq(cf.mul(2, a), cf.constant(20)), cf.eq(cf.zero(), cf.one()), cf.eq(cf.zero(), cf.constant(20)),
                cf.eq(cf.one(), cf.constant(20))
        );
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testCaching(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 5);
        final IntegerVariable b = cf.variable("b", 10, 15);
        final Term term1 = cf.add(a, b);
        final Term term2 = cf.mul(2, a);
        final AllDifferentPredicate pred1 = cf.allDifferent(List.of(cf.zero(), cf.one()));
        final AllDifferentPredicate pred2 = cf.allDifferent(List.of(term1, term2));
        final AllDifferentPredicate pred3 =
                cf.allDifferent(List.of(term1, term2, cf.zero(), cf.one(), cf.constant(20)));

        assertThat(pred1).isSameAs(cf.allDifferent(List.of(cf.zero(), cf.one())));
        assertThat(pred2).isSameAs(cf.allDifferent(List.of(term1, term2)));
        assertThat(pred3).isSameAs(cf.allDifferent(List.of(term1, term2, cf.zero(), cf.one(), cf.constant(20))));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testDecomposition(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 2);
        final IntegerVariable b = cf.variable("b", 0, 2);
        final IntegerVariable c = cf.variable("c", 0, 2);
        final IntegerVariable d = cf.variable("d", 0, 2);
        final CspPredicate.Decomposition pred1 = cf.allDifferent(List.of(cf.zero(), cf.one(), a)).decompose(cf);
        final CspPredicate.Decomposition pred2 = cf.allDifferent(List.of(a, b, c)).decompose(cf);
        final CspPredicate.Decomposition pred3 = cf.allDifferent(List.of(a, b, c, d)).decompose(cf);

        final SortedMap<IntegerVariable, Integer> coefsAB = new TreeMap<>();
        final SortedMap<IntegerVariable, Integer> coefsAC = new TreeMap<>();
        final SortedMap<IntegerVariable, Integer> coefsAD = new TreeMap<>();
        final SortedMap<IntegerVariable, Integer> coefsBC = new TreeMap<>();
        final SortedMap<IntegerVariable, Integer> coefsBD = new TreeMap<>();
        final SortedMap<IntegerVariable, Integer> coefsCD = new TreeMap<>();
        coefsAB.put(a, 1);
        coefsAB.put(b, -1);
        coefsAC.put(a, 1);
        coefsAC.put(c, -1);
        coefsAD.put(a, 1);
        coefsAD.put(d, -1);
        coefsBC.put(b, 1);
        coefsBC.put(c, -1);
        coefsBD.put(b, 1);
        coefsBD.put(d, -1);
        coefsCD.put(c, 1);
        coefsCD.put(d, -1);

        assertThat(pred1.getClauses()).hasSize(3);
        assertThat(pred1.getClauses()).containsExactlyInAnyOrder(
                new IntegerClause(new LinearLiteral(new LinearExpression(-1, a, 1), LinearLiteral.Operator.NE)),
                new IntegerClause(new LinearLiteral(new LinearExpression(1, a, 0), LinearLiteral.Operator.NE)),
                new IntegerClause(new LinearLiteral(new LinearExpression(-1, a, 2), LinearLiteral.Operator.LE))
        );
        assertThat(pred1.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(pred1.getAuxiliaryIntegerVariables()).isEmpty();
        assertThat(pred2.getClauses()).hasSize(5);
        assertThat(pred2.getClauses()).containsExactlyInAnyOrder(
                new IntegerClause(new LinearLiteral(new LinearExpression(coefsAB, 0), LinearLiteral.Operator.NE)),
                new IntegerClause(new LinearLiteral(new LinearExpression(coefsAC, 0), LinearLiteral.Operator.NE)),
                new IntegerClause(new LinearLiteral(new LinearExpression(coefsBC, 0), LinearLiteral.Operator.NE)),
                new IntegerClause(Collections.emptySet(), Common.setFrom(
                        new LinearLiteral(new LinearExpression(-1, a, 2), LinearLiteral.Operator.LE),
                        new LinearLiteral(new LinearExpression(-1, b, 2), LinearLiteral.Operator.LE),
                        new LinearLiteral(new LinearExpression(-1, c, 2), LinearLiteral.Operator.LE)
                )),
                new IntegerClause(Collections.emptySet(), Common.setFrom(
                        new LinearLiteral(new LinearExpression(1, a, 0), LinearLiteral.Operator.LE),
                        new LinearLiteral(new LinearExpression(1, b, 0), LinearLiteral.Operator.LE),
                        new LinearLiteral(new LinearExpression(1, c, 0), LinearLiteral.Operator.LE)
                )));
        assertThat(pred2.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(pred2.getAuxiliaryIntegerVariables()).isEmpty();
        assertThat(pred3.getClauses()).hasSize(7);
        assertThat(pred3.getClauses()).containsExactlyInAnyOrder(
                new IntegerClause(),
                new IntegerClause(new LinearLiteral(new LinearExpression(coefsAB, 0), LinearLiteral.Operator.NE)),
                new IntegerClause(new LinearLiteral(new LinearExpression(coefsAC, 0), LinearLiteral.Operator.NE)),
                new IntegerClause(new LinearLiteral(new LinearExpression(coefsAD, 0), LinearLiteral.Operator.NE)),
                new IntegerClause(new LinearLiteral(new LinearExpression(coefsBC, 0), LinearLiteral.Operator.NE)),
                new IntegerClause(new LinearLiteral(new LinearExpression(coefsBD, 0), LinearLiteral.Operator.NE)),
                new IntegerClause(new LinearLiteral(new LinearExpression(coefsCD, 0), LinearLiteral.Operator.NE))
        );
        assertThat(pred3.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(pred3.getAuxiliaryIntegerVariables()).isEmpty();
    }
}
