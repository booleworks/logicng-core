package com.booleworks.logicng.csp.terms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;

public class IntegerVariableTest extends ParameterizedCspTest {
    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testCreation(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", Set.of(1, 3, 7));
        final IntegerVariable c = cf.variable("c", Set.of(1, 2, 3, 4));

        assertThat(a.getName()).isEqualTo("a");
        assertThat(a.getType()).isEqualTo(Term.Type.VAR);
        assertThat(a.getDomain().size()).isEqualTo(11);
        assertThat(a.getDomain().isContiguous()).isTrue();
        assertThat(a.getDomain().isEmpty()).isFalse();

        assertThat(b.getName()).isEqualTo("b");
        assertThat(b.getType()).isEqualTo(Term.Type.VAR);
        assertThat(b.getDomain().size()).isEqualTo(3);
        assertThat(b.getDomain().isContiguous()).isFalse();
        assertThat(b.getDomain().isEmpty()).isFalse();

        assertThat(c.getName()).isEqualTo("c");
        assertThat(c.getType()).isEqualTo(Term.Type.VAR);
        assertThat(c.getDomain().size()).isEqualTo(4);
        assertThat(c.getDomain().isContiguous()).isTrue();
        assertThat(c.getDomain().isEmpty()).isFalse();

        assertThatThrownBy(() -> cf.variable("a", 0, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testDecomposition(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", Set.of(1, 3, 7, 8, 22));
        final Term.Decomposition aDecomp = a.decompose(cf);
        final Term.Decomposition bDecomp = b.decompose(cf);

        assertThat(aDecomp.getLinearExpression().getB()).isEqualTo(0);
        assertThat(aDecomp.getLinearExpression().getCoef().size()).isOne();
        assertThat(aDecomp.getLinearExpression().getA(a)).isOne();
        assertThat(aDecomp.getAdditionalConstraints()).isEmpty();
        assertThat(bDecomp.getLinearExpression().getB()).isEqualTo(0);
        assertThat(bDecomp.getLinearExpression().getCoef().size()).isOne();
        assertThat(bDecomp.getAdditionalConstraints()).isEmpty();
    }
}
