package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiplicationFunctionTest extends ParameterizedCspTest {
    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testType(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);
        final IntegerConstant d = cf.constant(10);

        assertThat(cf.mul(0, a).getType()).isEqualTo(Term.Type.MUL);
        assertThat(cf.mul(c, cf.zero()).getType()).isEqualTo(Term.Type.ZERO);
        assertThat(cf.mul(c, d).getType()).isEqualTo(Term.Type.CONST);
        assertThat(cf.mul(c, a).getType()).isEqualTo(Term.Type.MUL);
        assertThat(cf.mul(c, cf.add(a, b, c)).getType()).isEqualTo(Term.Type.MUL);
        assertThat(cf.mul(c, cf.sub(a, b)).getType()).isEqualTo(Term.Type.MUL);
        assertThat(cf.mul(c, cf.mul(c, a)).getType()).isEqualTo(Term.Type.MUL);
        assertThat(cf.mul(c, cf.minus(a)).getType()).isEqualTo(Term.Type.MUL);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testEquivalence(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);
        final IntegerConstant d = cf.constant(10);

        assertThat(cf.mul(0, a)).isEqualTo(new MultiplicationFunction(cf.constant(0), a));
        assertThat(cf.mul(c, cf.zero())).isEqualTo(cf.zero());
        assertThat(cf.mul(c, d)).isEqualTo(cf.constant(50));
        assertThat(cf.mul(c, a)).isEqualTo(new MultiplicationFunction(c, a));
        assertThat(cf.mul(c, cf.add(a, b, c))).isEqualTo(new MultiplicationFunction(c, cf.add(a, b, c)));
        assertThat(cf.mul(c, cf.sub(a, b))).isEqualTo(new MultiplicationFunction(c, cf.sub(a, b)));
        assertThat(cf.mul(c, cf.mul(c, a))).isEqualTo(new MultiplicationFunction(c, cf.mul(c, a)));
        assertThat(cf.mul(c, cf.minus(a))).isEqualTo(new MultiplicationFunction(c, cf.minus(a)));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testCaching(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);
        final IntegerConstant d = cf.constant(10);

        assertThat(cf.mul(0, a)).isSameAs(cf.mul(cf.zero(), a));
        assertThat(cf.mul(c, cf.zero())).isSameAs(cf.zero());
        assertThat(cf.mul(c, d)).isSameAs(cf.constant(50));
        assertThat(cf.mul(c, a)).isSameAs(cf.mul(c, a));
        assertThat(cf.mul(c, cf.add(a, b, c))).isSameAs(cf.mul(c, cf.add(a, b, c)));
        assertThat(cf.mul(c, cf.sub(a, b))).isSameAs(cf.mul(c, cf.sub(a, b)));
        assertThat(cf.mul(c, cf.mul(c, a))).isSameAs(cf.mul(c, cf.mul(c, a)));
        assertThat(cf.mul(c, cf.minus(a))).isSameAs(cf.mul(c, cf.minus(a)));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testDecomposition(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);
        final Term mul1 = cf.mul(c, a);
        final Term mul2 = cf.mul(c, cf.add(a, b));
        final Term mul3 = cf.mul(c, cf.sub(a, b));
        final Term mul4 = cf.mul(c, cf.add(a, cf.mul(7, b)));
        final Term.Decomposition mul1Decomp = mul1.decompose(cf);
        final Term.Decomposition mul2Decomp = mul2.decompose(cf);
        final Term.Decomposition mul3Decomp = mul3.decompose(cf);
        final Term.Decomposition mul4Decomp = mul4.decompose(cf);

        assertThat(mul1Decomp.getLinearExpression().getB()).isEqualTo(0);
        assertThat(mul1Decomp.getLinearExpression().getCoef()).hasSize(1);
        assertThat(mul1Decomp.getLinearExpression().getA(a)).isEqualTo(5);
        assertThat(mul1Decomp.getAdditionalConstraints()).isEmpty();
        assertThat(mul2Decomp.getLinearExpression().getB()).isEqualTo(0);
        assertThat(mul2Decomp.getLinearExpression().getCoef()).hasSize(2);
        assertThat(mul2Decomp.getLinearExpression().getA(a)).isEqualTo(5);
        assertThat(mul2Decomp.getLinearExpression().getA(b)).isEqualTo(5);
        assertThat(mul2Decomp.getAdditionalConstraints()).isEmpty();
        assertThat(mul3Decomp.getLinearExpression().getB()).isEqualTo(0);
        assertThat(mul3Decomp.getLinearExpression().getCoef()).hasSize(2);
        assertThat(mul3Decomp.getLinearExpression().getA(a)).isEqualTo(5);
        assertThat(mul3Decomp.getLinearExpression().getA(b)).isEqualTo(-5);
        assertThat(mul3Decomp.getAdditionalConstraints()).isEmpty();
        assertThat(mul4Decomp.getLinearExpression().getB()).isEqualTo(0);
        assertThat(mul4Decomp.getLinearExpression().getCoef()).hasSize(2);
        assertThat(mul4Decomp.getLinearExpression().getA(a)).isEqualTo(5);
        assertThat(mul4Decomp.getLinearExpression().getA(b)).isEqualTo(35);
        assertThat(mul4Decomp.getAdditionalConstraints()).isEmpty();
    }
}
