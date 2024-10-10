package com.booleworks.logicng.csp.terms;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public class NegationFunctionTest extends ParameterizedCspTest {

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testType(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);

        assertThat(cf.minus(c).getType()).isEqualTo(Term.Type.CONST);
        assertThat(cf.minus(a).getType()).isEqualTo(Term.Type.NEG);
        assertThat(cf.minus(cf.minus(c)).getType()).isEqualTo(c.getType());
        assertThat(cf.minus(cf.minus(a)).getType()).isEqualTo(a.getType());
        assertThat(cf.minus(cf.add(a, b, c)).getType()).isEqualTo(Term.Type.NEG);
        assertThat(cf.minus(cf.sub(a, b)).getType()).isEqualTo(Term.Type.NEG);
        assertThat(cf.minus(cf.mul(c, a)).getType()).isEqualTo(Term.Type.NEG);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testEquivalence(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);

        assertThat(cf.minus(c)).isEqualTo(cf.constant(c.getValue() * -1));
        assertThat(cf.minus(a)).isEqualTo(new NegationFunction(a));
        assertThat(cf.minus(cf.minus(c))).isEqualTo(c);
        assertThat(cf.minus(cf.minus(a))).isEqualTo(a);
        assertThat(cf.minus(cf.add(a, b, c))).isEqualTo(new NegationFunction(cf.add(a, b, c)));
        assertThat(cf.minus(cf.sub(a, b))).isEqualTo(new NegationFunction(cf.sub(a, b)));
        assertThat(cf.minus(cf.mul(c, a))).isEqualTo(new NegationFunction(cf.mul(c, a)));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testCaching(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);

        assertThat(cf.minus(a)).isSameAs(cf.minus(a));
        assertThat(cf.minus(cf.minus(c))).isSameAs(c);
        assertThat(cf.minus(cf.minus(a))).isSameAs(a);
        assertThat(cf.minus(cf.add(a, b, c))).isSameAs(cf.minus(cf.add(a, b, c)));
        assertThat(cf.minus(cf.sub(a, b))).isSameAs(cf.minus(cf.sub(a, b)));
        assertThat(cf.minus(cf.mul(c, a))).isSameAs(cf.minus(cf.mul(c, a)));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testDecomposition(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 10);
        final IntegerConstant c = cf.constant(5);
        final Term neg1 = cf.minus(c);
        final Term neg2 = cf.minus(a);
        final Term neg3 = cf.minus(cf.add(a, cf.mul(2, b), c));
        final Term.Decomposition neg1Decomp = neg1.decompose(cf);
        final Term.Decomposition neg2Decomp = neg2.decompose(cf);
        final Term.Decomposition neg3Decomp = neg3.decompose(cf);

        assertThat(neg1Decomp.getLinearExpression().getB()).isEqualTo(-5);
        assertThat(neg1Decomp.getLinearExpression().getCoef()).hasSize(0);
        assertThat(neg1Decomp.getAdditionalConstraints()).isEmpty();
        assertThat(neg2Decomp.getLinearExpression().getB()).isEqualTo(0);
        assertThat(neg2Decomp.getLinearExpression().getCoef()).hasSize(1);
        assertThat(neg2Decomp.getLinearExpression().getA(a)).isEqualTo(-1);
        assertThat(neg2Decomp.getAdditionalConstraints()).isEmpty();
        assertThat(neg3Decomp.getLinearExpression().getB()).isEqualTo(-5);
        assertThat(neg3Decomp.getLinearExpression().getCoef()).hasSize(2);
        assertThat(neg3Decomp.getLinearExpression().getA(a)).isEqualTo(-1);
        assertThat(neg3Decomp.getLinearExpression().getA(b)).isEqualTo(-2);
        assertThat(neg3Decomp.getAdditionalConstraints()).isEmpty();
    }
}
