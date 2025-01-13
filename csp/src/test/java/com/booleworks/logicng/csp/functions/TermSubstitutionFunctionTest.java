package com.booleworks.logicng.csp.functions;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.csp.CspFactory;
import com.booleworks.logicng.csp.ParameterizedCspTest;
import com.booleworks.logicng.csp.datastructures.IntegerVariableSubstitution;
import com.booleworks.logicng.csp.terms.IntegerVariable;
import com.booleworks.logicng.csp.terms.Term;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TermSubstitutionFunctionTest extends ParameterizedCspTest {
    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testVariableSubstitution(final CspFactory cf) {
        final FormulaFactory f = cf.getFormulaFactory();
        final Variable A = f.variable("A");
        final Variable B = f.variable("B");
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable as = cf.variable("as", 0, 2);
        final IntegerVariable b = cf.variable("b", 0, 2);
        final IntegerVariable bs = cf.variable("bs", 0, 10);
        final IntegerVariable c = cf.variable("c", 0, 10);
        final IntegerVariable d = cf.variable("d", 0, 10);
        final IntegerVariable F = cf.variable("F", 0, 10);

        final IntegerVariableSubstitution table = new IntegerVariableSubstitution();
        table.add(a, as);
        table.add(b, bs);
        table.add(F, F);

        assertThat(TermSubstitutionFunction.substituteTerm(cf.one(), table, cf)).isEqualTo(cf.one());
        assertThat(TermSubstitutionFunction.substituteTerm(a, table, cf)).isEqualTo(as);
        assertThat(TermSubstitutionFunction.substituteTerm(b, table, cf)).isEqualTo(bs);
        assertThat(TermSubstitutionFunction.substituteTerm(c, table, cf)).isEqualTo(c);
        assertThat(TermSubstitutionFunction.substituteTerm(F, table, cf)).isEqualTo(F);
        assertThat(TermSubstitutionFunction.substituteTerm(cf.add(a, as), table, cf)).isEqualTo(cf.add(as, as));
        assertThat(TermSubstitutionFunction.substituteTerm(cf.add(c, d), table, cf)).isEqualTo(cf.add(c, d));
        assertThat(TermSubstitutionFunction.substituteTerm(cf.sub(c, d), table, cf)).isEqualTo(cf.sub(c, d));
        assertThat(TermSubstitutionFunction.substituteTerm(cf.add(c, b), table, cf)).isEqualTo(cf.add(c, bs));
        assertThat(TermSubstitutionFunction.substituteTerm(cf.sub(c, b), table, cf)).isEqualTo(cf.sub(c, bs));
        assertThat(TermSubstitutionFunction.substituteTerm(cf.add(a, F), table, cf)).isEqualTo(cf.add(as, F));
        assertThat(TermSubstitutionFunction.substituteTerm(cf.sub(a, F), table, cf)).isEqualTo(cf.sub(as, F));

        assertThat(TermSubstitutionFunction.substituteFormula(f.and(A, B), table, cf)).isEqualTo(f.and(A, B));
        assertThat(TermSubstitutionFunction.substituteFormula(f.and(A, cf.eq(a, F)), table, cf)).isEqualTo(
                f.and(A, cf.eq(as, F)));
        assertThat(
                TermSubstitutionFunction.substituteFormula(f.or(A, cf.allDifferent(List.of(a, b, c, F))), table, cf))
                .isEqualTo(f.or(A, cf.allDifferent(List.of(as, bs, c, F))));
    }

    @ParameterizedTest
    @MethodSource("cspFactories")
    public void testTermSubstitution(final CspFactory cf) {
        final IntegerVariable a = cf.variable("a", 0, 10);
        final IntegerVariable b = cf.variable("b", 0, 2);
        final IntegerVariable bs = cf.variable("bs", 0, 10);
        final IntegerVariable c = cf.variable("c", 0, 10);
        final IntegerVariable d = cf.variable("d", 0, 10);
        final IntegerVariable F = cf.variable("F", 0, 10);

        final Map<IntegerVariable, Term> table = new HashMap<>();
        table.put(a, cf.one());
        table.put(b, cf.sub(bs, c));
        table.put(F, cf.add(F, F));

        assertThat(TermSubstitutionFunction.substituteTerm(a, table, cf)).isEqualTo(cf.one());
        assertThat(TermSubstitutionFunction.substituteTerm(b, table, cf)).isEqualTo(cf.sub(bs, c));
        assertThat(TermSubstitutionFunction.substituteTerm(F, table, cf)).isEqualTo(cf.add(F, F));
        assertThat(TermSubstitutionFunction.substituteTerm(cf.add(a, cf.one()), table, cf)).isEqualTo(cf.constant(2));
        assertThat(TermSubstitutionFunction.substituteTerm(cf.add(F, F), table, cf)).isEqualTo(cf.add(F, F, F, F));
    }
}
