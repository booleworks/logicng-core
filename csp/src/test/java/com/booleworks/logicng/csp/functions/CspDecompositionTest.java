package com.booleworks.logicng.csp.functions;

import com.booleworks.logicng.csp.Common;
import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import com.booleworks.logicng.csp.datastructures.IntegerClause;
import com.booleworks.logicng.csp.datastructures.LinearExpression;
import com.booleworks.logicng.csp.literals.LinearLiteral;
import com.booleworks.logicng.csp.predicates.CspPredicate;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

public class CspDecompositionTest extends ParameterizedCspTest {

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testFormulas(final CspFactory cf) throws ParserException {
        final FormulaFactory f = cf.getFormulaFactory();
        final IntegerVariable a = cf.variable("a", 0, 3);
        final IntegerVariable b = cf.variable("b", 3, 5);
        final IntegerVariable c = cf.variable("c", 1, 2);
        final Variable A = f.variable("A");
        final Variable B = f.variable("B");
        final Variable C = f.variable("C");
        final Variable D = f.variable("D");

        final Formula formula1 = f.parse("A & B");
        final Formula formula2 = f.parse("A & ~(B | C)");
        final Formula formula3 = f.or(
                cf.eq(a, cf.constant(2)),
                f.and(C, D)
        );
        final Formula formula4 = f.not(cf.allDifferent(List.of(a, b, c, cf.add(a, b), cf.add(b, c), cf.add(a, c))));

        final CspPredicate.Decomposition decomp1 = cf.decompose(formula1);
        final CspPredicate.Decomposition decomp2 = cf.decompose(formula2);
        final CspPredicate.Decomposition decomp3 = cf.decompose(formula3);
        final CspPredicate.Decomposition decomp4 = cf.decompose(formula4);

        assertThat(decomp1.getClauses()).containsExactlyInAnyOrder(new IntegerClause(A), new IntegerClause(B));
        assertThat(decomp1.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(decomp1.getAuxiliaryIntegerVariables()).isEmpty();
        assertThat(decomp2.getClauses()).containsExactlyInAnyOrder(new IntegerClause(A), new IntegerClause(B.negate(f)),
                new IntegerClause(C.negate(f)));
        assertThat(decomp2.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(decomp2.getAuxiliaryIntegerVariables()).isEmpty();
        final LinearLiteral l = new LinearLiteral(new LinearExpression(1, a, -2), LinearLiteral.Operator.EQ);
        assertThat(decomp3.getClauses()).containsExactlyInAnyOrder(Common.integerClauseFrom(C, l),
                Common.integerClauseFrom(D, l));
        assertThat(decomp3.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(decomp3.getAuxiliaryIntegerVariables()).isEmpty();
        assertThat(decomp4.getClauses()).containsExactlyInAnyOrder(
                new IntegerClause(Collections.emptySet(), Common.setFrom(lt(-2, a), lt(-1, a, 1, b),
                        lt(1, a, -1, b), lt(1, a, -1, c), lt(-1, a, 1, b, -1, c)))
        );
        assertThat(decomp4.getAuxiliaryBooleanVariables()).isEmpty();
        assertThat(decomp4.getAuxiliaryIntegerVariables()).isEmpty();
    }

    private static LinearLiteral lt(final int c0, final IntegerVariable a0) {
        final SortedMap<IntegerVariable, Integer> coefs = new TreeMap<>();
        coefs.put(a0, c0);
        return new LinearLiteral(new LinearExpression(coefs, 0), LinearLiteral.Operator.EQ);
    }

    private static LinearLiteral lt(final int c0, final IntegerVariable a0, final int c1, final IntegerVariable a1) {
        final SortedMap<IntegerVariable, Integer> coefs = new TreeMap<>();
        coefs.put(a0, c0);
        coefs.put(a1, c1);
        return new LinearLiteral(new LinearExpression(coefs, 0), LinearLiteral.Operator.EQ);
    }

    private static LinearLiteral lt(final int c0, final IntegerVariable a0, final int c1, final IntegerVariable a1,
                                    final int c2, final IntegerVariable a2) {
        final SortedMap<IntegerVariable, Integer> coefs = new TreeMap<>();
        coefs.put(a0, c0);
        coefs.put(a1, c1);
        coefs.put(a2, c2);
        return new LinearLiteral(new LinearExpression(coefs, 0), LinearLiteral.Operator.EQ);
    }
}
