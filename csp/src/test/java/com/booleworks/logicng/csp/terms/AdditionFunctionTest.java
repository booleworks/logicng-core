package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AdditionFunctionTest extends ParameterizedCspTest {

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testType(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);

        assertThat(cf.add(c, c).getType()).isEqualTo(Term.Type.CONST);
        assertThat(cf.add(c, c, c, c).getType()).isEqualTo(Term.Type.CONST);
        assertThat(cf.add(a, b).getType()).isEqualTo(Term.Type.ADD);
        assertThat(cf.add(a, c).getType()).isEqualTo(Term.Type.ADD);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testCompactification(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);
        final IntegerConstant d = cf.constant(10);

        assertThat(cf.add(a, cf.zero())).isSameAs(a);
        assertThat(cf.add(c, cf.zero())).isSameAs(c);
        assertThat(cf.add(a, b, cf.zero())).isSameAs(cf.add(a, b));
        assertThat(cf.add(c, d)).isSameAs(cf.constant(15));
        assertThat(cf.add(a, b, c)).isEqualTo(new AdditionFunction(new LinkedHashSet<>(List.of(a, b, c))));
        assertThat(cf.add(a, a)).isSameAs(cf.mul(2, a));
        assertThat(cf.add(a, a, c)).isSameAs(cf.add(cf.mul(2, a), c));
        assertThat(cf.add(a, cf.add(b, cf.add(a, c)), cf.add(a, c))).isSameAs(
                cf.add(cf.mul(3, a), b, cf.constant(c.getValue() * 2)));
        assertThat(cf.add(cf.mul(2, a), cf.mul(3, a))).isSameAs(cf.mul(5, a));
        assertThat(cf.add(cf.mul(a, b), cf.mul(a, b))).isSameAs(cf.add(cf.mul(2, cf.mul(a, b))));
        assertThat(cf.add(cf.mul(2, cf.mul(a, b)), cf.mul(a, b))).isSameAs(cf.add(cf.mul(3, cf.mul(a, b))));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testCaching(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);

        assertThat(cf.add(a, b, c)).isSameAs(cf.add(a, b, c));
        assertThat(cf.add(a, b, c)).isSameAs(cf.add(a, c, b));
        assertThat(cf.add(a, b, c)).isSameAs(cf.add(b, a, c));
        assertThat(cf.add(a, cf.add(b, c))).isSameAs(cf.add(c, cf.add(b, a)));
        assertThat(cf.add(a, b)).isNotSameAs(cf.add(a, c));
        assertThat(cf.add(a, b)).isNotSameAs(cf.add(a, a));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testDecomposition(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);
        final Term add1 = cf.add(a, b);
        final Term add2 = cf.add(a, cf.mul(c, b));
        final Term add3 = cf.add(add1, add2, c);
        final Term.Decomposition add1Decomp = add1.decompose(cf);
        final Term.Decomposition add2Decomp = add2.decompose(cf);
        final Term.Decomposition add3Decomp = add3.decompose(cf);

        assertThat(add1Decomp.getLinearExpression().getB()).isEqualTo(0);
        assertThat(add1Decomp.getLinearExpression().getCoef()).hasSize(2);
        assertThat(add1Decomp.getLinearExpression().getA(a)).isEqualTo(1);
        assertThat(add1Decomp.getLinearExpression().getA(b)).isEqualTo(1);
        assertThat(add1Decomp.getAdditionalConstraints()).isEmpty();
        assertThat(add2Decomp.getLinearExpression().getB()).isEqualTo(0);
        assertThat(add2Decomp.getLinearExpression().getCoef()).hasSize(2);
        assertThat(add2Decomp.getLinearExpression().getA(a)).isEqualTo(1);
        assertThat(add2Decomp.getLinearExpression().getA(b)).isEqualTo(5);
        assertThat(add2Decomp.getAdditionalConstraints()).isEmpty();
        assertThat(add3Decomp.getLinearExpression().getB()).isEqualTo(5);
        assertThat(add3Decomp.getLinearExpression().getCoef()).hasSize(2);
        assertThat(add3Decomp.getLinearExpression().getA(a)).isEqualTo(2);
        assertThat(add3Decomp.getLinearExpression().getA(b)).isEqualTo(6);
        assertThat(add3Decomp.getAdditionalConstraints()).isEmpty();
    }
}
