package com.booleworks.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.configurations.Configuration;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FormulaFactoryTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPutConfigurationWithInvalidArgument(final FormulaContext context) {
        final FormulaFactory f = context.f;
        assertThatThrownBy(() -> {
            f.putConfiguration(FormulaFactoryConfig.builder().build());
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Configurations for the formula factory itself can only be passed in the constructor.");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConstant(final FormulaContext context) {
        final FormulaFactory f = context.f;
        assertThat(f.constant(true)).isEqualTo(context.f.verum());
        assertThat(f.constant(false)).isEqualTo(context.f.falsum());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testDefaultName(final FormulaContext context) {
        final FormulaFactory f = context.f;
        assertThat(f.getName().length()).isEqualTo(4);
        assertThat(f.getName().chars()).allMatch(c -> c >= 65 && c <= 90);
    }

    @Test
    public void testRandomName() {
        final FormulaFactory f1 = FormulaFactory.caching();
        final FormulaFactory f2 = FormulaFactory.caching();
        final FormulaFactory f3 = FormulaFactory.nonCaching();
        final FormulaFactory f4 = FormulaFactory.nonCaching();
        assertThat(f1.getName()).isNotEqualTo(f2.getName());
        assertThat(f1.getName()).isNotEqualTo(f3.getName());
        assertThat(f1.getName()).isNotEqualTo(f4.getName());
        assertThat(f2.getName()).isNotEqualTo(f3.getName());
        assertThat(f2.getName()).isNotEqualTo(f4.getName());
        assertThat(f3.getName()).isNotEqualTo(f4.getName());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testConfigurations(final FormulaContext context) {
        final FormulaFactory f = context.f;
        final Configuration configMaxSat = MaxSatConfig.builder().build();
        final Configuration configSat = SatSolverConfig.builder().build();
        f.putConfiguration(configMaxSat);
        f.putConfiguration(configSat);
        assertThat(f.configurationFor(ConfigurationType.MAXSAT)).isEqualTo(configMaxSat);
        assertThat(f.configurationFor(ConfigurationType.SAT)).isEqualTo(configSat);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCNF(final FormulaContext context) {
        final FormulaFactory f = context.f;
        final Variable a = f.variable("A");
        final Variable b = f.variable("B");
        final Variable c = f.variable("C");
        final Variable d = f.variable("D");
        final Formula clause1 = f.or(a, b);
        final Formula clause2 = f.or(c, d.negate(f));
        final Formula nClause1 = f.implication(a, c);

        final List<Formula> clauses = new ArrayList<>();
        clauses.add(clause1);
        clauses.add(clause2);

        final List<Formula> nClauses = new ArrayList<>();
        nClauses.add(clause1);
        nClauses.add(clause2);
        nClauses.add(nClause1);

        final Formula cnf = f.cnf(clauses);
        final Formula nCnf = f.cnf(nClauses);
        assertThat(cnf.cnf(f)).isEqualTo(cnf);
        assertThat(nCnf.cnf(f)).isNotEqualTo(nCnf);
        assertThat(f.cnf(Collections.emptyList())).isEqualTo(f.verum());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testGeneratedVariables(final FormulaContext context) {
        FormulaFactory f = context.f;
        Variable ccVar = f.newCcVariable();
        Variable cnfVar = f.newCnfVariable();
        Variable pbVar = f.newPbVariable();
        Variable var = f.variable("x");
        assertThat(ccVar.getName().startsWith("@AUX_")).isTrue();
        assertThat(cnfVar.getName().startsWith("@AUX_")).isTrue();
        assertThat(pbVar.getName().startsWith("@AUX_")).isTrue();
        assertThat(var.getName().startsWith("@AUX_")).isFalse();
        assertThat(ccVar.getName()).isEqualTo("@AUX_" + f.getName() + "_CC_0");
        assertThat(pbVar.getName()).isEqualTo("@AUX_" + f.getName() + "_PB_0");
        assertThat(cnfVar.getName()).isEqualTo("@AUX_" + f.getName() + "_CNF_0");

        f = FormulaFactory.caching(FormulaFactoryConfig.builder().name("f").build());
        ccVar = f.newCcVariable();
        cnfVar = f.newCnfVariable();
        pbVar = f.newPbVariable();
        var = f.variable("x");
        assertThat(ccVar.getName().startsWith("@AUX_")).isTrue();
        assertThat(cnfVar.getName().startsWith("@AUX_")).isTrue();
        assertThat(pbVar.getName().startsWith("@AUX_")).isTrue();
        assertThat(var.getName().startsWith("@AUX_")).isFalse();
        assertThat(ccVar.getName()).isEqualTo("@AUX_f_CC_0");
        assertThat(pbVar.getName()).isEqualTo("@AUX_f_PB_0");
        assertThat(cnfVar.getName()).isEqualTo("@AUX_f_CNF_0");
    }
}
