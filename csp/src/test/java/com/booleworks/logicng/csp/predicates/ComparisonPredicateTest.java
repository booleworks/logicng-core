package com.booleworks.logicng.csp.predicates;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.csp.terms.Term;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.SortedMap;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

public class ComparisonPredicateTest extends ParameterizedCspTest {

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testType(final CspFactory cf) {
        assertThat(cf.eq(cf.zero(), cf.one()).getPredicateType()).isEqualTo(CspPredicate.Type.EQ);
        assertThat(cf.ne(cf.zero(), cf.one()).getPredicateType()).isEqualTo(CspPredicate.Type.NE);
        assertThat(cf.le(cf.zero(), cf.one()).getPredicateType()).isEqualTo(CspPredicate.Type.LE);
        assertThat(cf.lt(cf.zero(), cf.one()).getPredicateType()).isEqualTo(CspPredicate.Type.LT);
        assertThat(cf.ge(cf.zero(), cf.one()).getPredicateType()).isEqualTo(CspPredicate.Type.GE);
        assertThat(cf.gt(cf.zero(), cf.one()).getPredicateType()).isEqualTo(CspPredicate.Type.GT);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testSimpleExamples(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 5);
        final IntegerVariable b = cf.variable("b", 10, 15);
        final Term term1 = cf.add(a, b);
        final Term term2 = cf.mul(2, a);
        final ComparisonPredicate pred1 = cf.eq(cf.zero(), cf.one());
        final ComparisonPredicate pred2 = cf.le(term1, term2);
        final ComparisonPredicate pred3 = cf.gt(term1, cf.constant(20));
        final ComparisonPredicate pred4 = cf.lt(cf.zero(), cf.one());
        final ComparisonPredicate pred5 = cf.ge(term1, term2);
        final ComparisonPredicate pred6 = cf.ne(term1, cf.constant(20));

        assertThat(pred1.left).isEqualTo(cf.zero());
        assertThat(pred1.right).isEqualTo(cf.one());
        assertThat(pred1.type).isEqualTo(CspPredicate.Type.EQ);

        assertThat(pred2.left).isEqualTo(term1);
        assertThat(pred2.right).isEqualTo(term2);
        assertThat(pred2.type).isEqualTo(CspPredicate.Type.LE);

        assertThat(pred3.left).isEqualTo(term1);
        assertThat(pred3.right).isEqualTo(cf.constant(20));
        assertThat(pred3.type).isEqualTo(CspPredicate.Type.GT);

        assertThat(pred4.left).isEqualTo(cf.zero());
        assertThat(pred4.right).isEqualTo(cf.one());
        assertThat(pred4.type).isEqualTo(CspPredicate.Type.LT);

        assertThat(pred5.left).isEqualTo(term1);
        assertThat(pred5.right).isEqualTo(term2);
        assertThat(pred5.type).isEqualTo(CspPredicate.Type.GE);

        assertThat(pred6.left).isEqualTo(term1);
        assertThat(pred6.right).isEqualTo(cf.constant(20));
        assertThat(pred6.type).isEqualTo(CspPredicate.Type.NE);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testNegation(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 5);
        final IntegerVariable b = cf.variable("b", 10, 15);
        final Term term1 = cf.add(a, b);
        final Term term2 = cf.mul(2, a);
        final ComparisonPredicate pred1 = cf.eq(cf.zero(), cf.one()).negate(cf);
        final ComparisonPredicate pred2 = cf.le(term1, term2).negate(cf);
        final ComparisonPredicate pred3 = cf.gt(term1, cf.constant(20)).negate(cf);
        final ComparisonPredicate pred4 = cf.lt(cf.zero(), cf.one()).negate(cf);
        final ComparisonPredicate pred5 = cf.ge(term1, term2).negate(cf);
        final ComparisonPredicate pred6 = cf.ne(term1, cf.constant(20)).negate(cf);

        assertThat(pred1.left).isEqualTo(cf.zero());
        assertThat(pred1.right).isEqualTo(cf.one());
        assertThat(pred1.type).isEqualTo(CspPredicate.Type.NE);

        assertThat(pred2.left).isEqualTo(term1);
        assertThat(pred2.right).isEqualTo(term2);
        assertThat(pred2.type).isEqualTo(CspPredicate.Type.GT);

        assertThat(pred3.left).isEqualTo(term1);
        assertThat(pred3.right).isEqualTo(cf.constant(20));
        assertThat(pred3.type).isEqualTo(CspPredicate.Type.LE);

        assertThat(pred4.left).isEqualTo(cf.zero());
        assertThat(pred4.right).isEqualTo(cf.one());
        assertThat(pred4.type).isEqualTo(CspPredicate.Type.GE);

        assertThat(pred5.left).isEqualTo(term1);
        assertThat(pred5.right).isEqualTo(term2);
        assertThat(pred5.type).isEqualTo(CspPredicate.Type.LT);

        assertThat(pred6.left).isEqualTo(term1);
        assertThat(pred6.right).isEqualTo(cf.constant(20));
        assertThat(pred6.type).isEqualTo(CspPredicate.Type.EQ);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testCaching(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 5);
        final IntegerVariable b = cf.variable("b", 10, 15);
        final Term term1 = cf.add(a, b);
        final Term term2 = cf.mul(2, a);
        final ComparisonPredicate pred1 = cf.eq(cf.zero(), cf.one());
        final ComparisonPredicate pred2 = cf.le(term1, term2);
        final ComparisonPredicate pred3 = cf.gt(term1, cf.constant(20));
        final ComparisonPredicate pred4 = cf.lt(cf.zero(), cf.one());
        final ComparisonPredicate pred5 = cf.ge(term1, term2);
        final ComparisonPredicate pred6 = cf.ne(term1, cf.constant(20));

        assertThat(pred1).isSameAs(cf.eq(cf.zero(), cf.one()));
        assertThat(pred1).isNotSameAs(cf.ne(cf.zero(), cf.one()));
        assertThat(pred2).isSameAs(cf.le(term1, term2));
        assertThat(pred2).isNotSameAs(cf.lt(term1, term2));
        assertThat(pred3).isSameAs(cf.gt(term1, cf.constant(20)));
        assertThat(pred3).isNotSameAs(cf.ge(term1, cf.constant(20)));
        assertThat(pred4).isSameAs(cf.lt(cf.zero(), cf.one()));
        assertThat(pred4).isNotSameAs(cf.le(cf.zero(), cf.one()));
        assertThat(pred5).isSameAs(cf.ge(term1, term2));
        assertThat(pred5).isNotSameAs(cf.gt(term1, term2));
        assertThat(pred6).isSameAs(cf.ne(term1, cf.constant(20)));
        assertThat(pred6).isNotSameAs(cf.eq(term1, cf.constant(20)));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testDecomposition(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 5);
        final IntegerVariable b = cf.variable("b", 10, 15);
        final Term term1 = cf.add(a, b);
        final Term term2 = cf.mul(2, a);
        final CspPredicate.Decomposition pred1 = cf.eq(cf.zero(), cf.one()).decompose(cf);
        final CspPredicate.Decomposition pred2 = cf.le(term1, term2).decompose(cf);
        final CspPredicate.Decomposition pred3 = cf.gt(term1, cf.constant(18)).decompose(cf);
        final CspPredicate.Decomposition pred4 = cf.lt(cf.zero(), cf.one()).decompose(cf);
        final CspPredicate.Decomposition pred5 = cf.ge(term1, term2).decompose(cf);
        final CspPredicate.Decomposition pred6 = cf.ne(term1, cf.constant(20)).decompose(cf);

        assertThat(pred1.getClauses()).hasSize(1);
        assertThat(pred1.getClauses().iterator().next()).isEqualTo(new IntegerClause());
        assertThat(pred1.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(pred1.getAuxiliaryIntegerVariables()).isEmpty();
        assertThat(pred2.getClauses()).hasSize(1);
        assertThat(pred2.getClauses().iterator().next()).isEqualTo(new IntegerClause());
        assertThat(pred2.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(pred2.getAuxiliaryIntegerVariables()).isEmpty();
        assertThat(pred3.getClauses()).hasSize(1);
        final SortedMap<IntegerVariable, Integer> coef3 = new TreeMap<>();
        coef3.put(a, -1);
        coef3.put(b, -1);
        assertThat(pred3.getClauses().iterator().next()).isEqualTo(
                new IntegerClause(new LinearLiteral(new LinearExpression(coef3, 19), LinearLiteral.Operator.LE)));
        assertThat(pred3.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(pred3.getAuxiliaryIntegerVariables()).isEmpty();
        assertThat(pred4.getClauses()).isEmpty();
        assertThat(pred4.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(pred4.getAuxiliaryIntegerVariables()).isEmpty();
        assertThat(pred5.getClauses()).isEmpty();
        assertThat(pred5.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(pred5.getAuxiliaryIntegerVariables()).isEmpty();
        assertThat(pred6.getClauses()).hasSize(1);
        final SortedMap<IntegerVariable, Integer> coef6 = new TreeMap<>();
        coef6.put(a, 1);
        coef6.put(b, 1);
        assertThat(pred6.getClauses().iterator().next()).isEqualTo(
                new IntegerClause(new LinearLiteral(new LinearExpression(coef6, -20), LinearLiteral.Operator.NE)));
        assertThat(pred6.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(pred6.getAuxiliaryIntegerVariables()).isEmpty();
    }
}
