package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public class SubtractionFunctionTest extends ParameterizedCspTest {
    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testType(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);
        final IntegerConstant d = cf.constant(10);

        assertThat(cf.sub(cf.zero(), c).getType()).isEqualTo(c.getType());
        assertThat(cf.sub(cf.zero(), a).getType()).isEqualTo(Term.Type.NEG);
        assertThat(cf.sub(c, cf.zero()).getType()).isEqualTo(c.getType());
        assertThat(cf.sub(a, cf.zero()).getType()).isEqualTo(a.getType());
        assertThat(cf.sub(c, c).getType()).isEqualTo(Term.Type.ZERO);
        assertThat(cf.sub(a, a).getType()).isEqualTo(Term.Type.SUB);
        assertThat(cf.sub(c, d).getType()).isEqualTo(Term.Type.CONST);
        assertThat(cf.sub(a, b).getType()).isEqualTo(Term.Type.SUB);
        assertThat(cf.sub(a, c).getType()).isEqualTo(Term.Type.SUB);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testEquivalence(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);
        final IntegerConstant d = cf.constant(10);

        assertThat(cf.sub(cf.zero(), c)).isEqualTo(new IntegerConstant(c.getValue() * -1));
        assertThat(cf.sub(cf.zero(), a)).isEqualTo(new NegationFunction(a));
        assertThat(cf.sub(c, cf.zero())).isEqualTo(c);
        assertThat(cf.sub(a, cf.zero())).isEqualTo(a);
        assertThat(cf.sub(c, c)).isEqualTo(cf.zero());
        assertThat(cf.sub(a, a)).isEqualTo(new SubtractionFunction(a, a));
        assertThat(cf.sub(c, d)).isEqualTo(new IntegerConstant(c.getValue() - d.getValue()));
        assertThat(cf.sub(a, b)).isEqualTo(new SubtractionFunction(a, b));
        assertThat(cf.sub(a, c)).isEqualTo(new SubtractionFunction(a, c));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testCaching(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);
        final IntegerConstant d = cf.constant(10);

        assertThat(cf.sub(cf.zero(), c)).isSameAs(cf.constant(c.getValue() * -1));
        assertThat(cf.sub(cf.zero(), a)).isSameAs(cf.minus(a));
        assertThat(cf.sub(c, cf.zero())).isSameAs(c);
        assertThat(cf.sub(a, cf.zero())).isSameAs(a);
        assertThat(cf.sub(c, c)).isSameAs(cf.zero());
        assertThat(cf.sub(a, a)).isSameAs(cf.sub(a, a));
        assertThat(cf.sub(c, d)).isSameAs(cf.sub(c, d));
        assertThat(cf.sub(a, b)).isSameAs(cf.sub(a, b));
        assertThat(cf.sub(a, c)).isSameAs(cf.sub(a, c));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testDecomposition(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);
        final Term sub1 = cf.minus(c);
        final Term sub2 = cf.minus(a);
        final Term sub3 = cf.minus(cf.add(a, cf.mul(2, b), c));
        final Term.Decomposition sub1Decomp = sub1.decompose(cf);
        final Term.Decomposition sub2Decomp = sub2.decompose(cf);
        final Term.Decomposition sub3Decomp = sub3.decompose(cf);

        assertThat(sub1Decomp.getLinearExpression().getB()).isEqualTo(-5);
        assertThat(sub1Decomp.getLinearExpression().getCoef()).hasSize(0);
        assertThat(sub1Decomp.getAdditionalConstraints()).isEmpty();
        assertThat(sub2Decomp.getLinearExpression().getB()).isEqualTo(0);
        assertThat(sub2Decomp.getLinearExpression().getCoef()).hasSize(1);
        assertThat(sub2Decomp.getLinearExpression().getA(a)).isEqualTo(-1);
        assertThat(sub2Decomp.getAdditionalConstraints()).isEmpty();
        assertThat(sub3Decomp.getLinearExpression().getB()).isEqualTo(-5);
        assertThat(sub3Decomp.getLinearExpression().getCoef()).hasSize(2);
        assertThat(sub3Decomp.getLinearExpression().getA(a)).isEqualTo(-1);
        assertThat(sub3Decomp.getLinearExpression().getA(b)).isEqualTo(-2);
        assertThat(sub3Decomp.getAdditionalConstraints()).isEmpty();
    }
}
