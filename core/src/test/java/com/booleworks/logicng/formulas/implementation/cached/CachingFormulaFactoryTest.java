// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas.implementation.cached;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booleworks.logicng.configurations.Configuration;
import com.booleworks.logicng.configurations.ConfigurationType;
import com.booleworks.logicng.formulas.And;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.FormulaFactoryConfig;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Or;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.solvers.maxsat.algorithms.MaxSatConfig;
import com.booleworks.logicng.solvers.sat.SatSolverConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CachingFormulaFactoryTest {

    @Test
    public void testPutConfigurationWithInvalidArgument() {
        assertThatThrownBy(() -> {
            final FormulaFactory f = FormulaFactory.caching();
            f.putConfiguration(FormulaFactoryConfig.builder().build());
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Configurations for the formula factory itself can only be passed in the constructor.");
    }

    @Test
    public void testConstant() {
        final FormulaFactory f = FormulaFactory.caching();
        assertThat(f.constant(true)).isEqualTo(f.verum());
        assertThat(f.constant(false)).isEqualTo(f.falsum());
    }

    @Test
    public void testToString() {
        final FormulaFactory f =
                FormulaFactory.caching(FormulaFactoryConfig.builder().name("MyFormulaFactory").build());
        f.variable("a");
        f.literal("b", false);
        f.and(f.variable("a"), f.literal("b", false));
        f.or(f.variable("a"), f.literal("b", false), f.variable("x"), f.implication(f.variable("a"), f.variable("x")));
        final String expected = String.format("Name:              MyFormulaFactory%n" +
                "Positive Literals: 3%n" +
                "Negative Literals: 1%n" +
                "Negations:         0%n" +
                "Implications:      1%n" +
                "Equivalences:      0%n" +
                "Conjunctions (2):  1%n" +
                "Conjunctions (3):  0%n" +
                "Conjunctions (4):  0%n" +
                "Conjunctions (>4): 0%n" +
                "Disjunctions (2):  0%n" +
                "Disjunctions (3):  0%n" +
                "Disjunctions (4):  1%n" +
                "Disjunctions (>4): 0%n" +
                "Pseudo Booleans:   0%n" +
                "CCs:               0%n");
        assertThat(f.toString()).isEqualTo(expected);
    }

    @Test
    public void testDefaultName() {
        final FormulaFactory f = FormulaFactory.caching();
        assertThat(f.getName().length()).isEqualTo(4);
        assertThat(f.getName().chars()).allMatch(c -> c >= 65 && c <= 90);
    }

    @Test
    public void testRandomName() {
        final FormulaFactory f1 = FormulaFactory.caching();
        final FormulaFactory f2 = FormulaFactory.caching();
        assertThat(f1.getName()).isNotEqualTo(f2.getName());
    }

    @Test
    public void testConfigurations() {
        final FormulaFactory f = FormulaFactory.caching();
        final Configuration configMaxSat = MaxSatConfig.builder().build();
        final Configuration configSat = SatSolverConfig.builder().build();
        f.putConfiguration(configMaxSat);
        f.putConfiguration(configSat);
        assertThat(f.configurationFor(ConfigurationType.MAXSAT)).isEqualTo(configMaxSat);
        assertThat(f.configurationFor(ConfigurationType.SAT)).isEqualTo(configSat);
    }

    @Test
    public void testGeneratedVariables() {
        FormulaFactory f = FormulaFactory.caching();
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

    @Test
    public void testCNF() {
        final FormulaFactory f = FormulaFactory.caching();
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

    @Test
    public void testImportFormula() throws ParserException {
        final CachingFormulaFactory f =
                new CachingFormulaFactory(FormulaFactoryConfig.builder().name("Factory F").build());
        final CachingFormulaFactory g =
                new CachingFormulaFactory(FormulaFactoryConfig.builder().name("Factory G").build());
        final PropositionalParser pf = new PropositionalParser(f);
        final String formula = "x1 & x2 & ~x3 => (x4 | (x5 <=> ~x1))";
        final Formula ff = pf.parse(formula);
        final Formula fg = g.importFormula(ff);
        assertThat(fg).isEqualTo(ff);
        assertThat(ff.getFactory()).isSameAs(f);
        assertThat(fg.getFactory()).isSameAs(g);
        assertThat(f.statistics()).usingRecursiveComparison().comparingOnlyFields("positiveLiterals",
                        "negativeLiterals", "negations", "implications", "equivalences", "conjunctions2", "conjunctions3",
                        "conjunctions4", "conjunctionsN", "disjunctions2", "disjunctions3", "disjunctions4")
                .isEqualTo(g.statistics());
        for (final Literal litF : ff.literals(f)) {
            assertThat(litF.getFactory()).isSameAs(f);
        }
        for (final Literal litG : fg.literals(f)) {
            assertThat(litG.getFactory()).isSameAs(f);
        }
    }

    @Test
    public void testStatistics() {
        final CachingFormulaFactory f =
                new CachingFormulaFactory(FormulaFactoryConfig.builder().name("Factory F").build());
        final CachingFormulaFactory g =
                new CachingFormulaFactory(FormulaFactoryConfig.builder().name("Factory F").build());
        final CachingFormulaFactory.Statistics statisticsF1 = f.statistics();
        final CachingFormulaFactory.Statistics statisticsG = g.statistics();

        assertThat(statisticsF1.name()).isEqualTo("Factory F");
        assertThat(statisticsF1.positiveLiterals()).isEqualTo(0);
        assertThat(statisticsF1.negativeLiterals()).isEqualTo(0);
        assertThat(statisticsF1.negations()).isEqualTo(0);
        assertThat(statisticsF1.implications()).isEqualTo(0);
        assertThat(statisticsF1.equivalences()).isEqualTo(0);
        assertThat(statisticsF1.conjunctions2()).isEqualTo(0);
        assertThat(statisticsF1.conjunctions3()).isEqualTo(0);
        assertThat(statisticsF1.conjunctions4()).isEqualTo(0);
        assertThat(statisticsF1.conjunctionsN()).isEqualTo(0);
        assertThat(statisticsF1.disjunctions2()).isEqualTo(0);
        assertThat(statisticsF1.disjunctions3()).isEqualTo(0);
        assertThat(statisticsF1.disjunctions4()).isEqualTo(0);
        assertThat(statisticsF1.disjunctionsN()).isEqualTo(0);
        assertThat(statisticsF1.pbcs()).isEqualTo(0);
        assertThat(statisticsF1.ccs()).isEqualTo(0);
        assertThat(statisticsF1.ccCounter()).isEqualTo(0);
        assertThat(statisticsF1.pbCounter()).isEqualTo(0);
        assertThat(statisticsF1.cnfCounter()).isEqualTo(0);
        assertThat(statisticsF1.formulas()).isEqualTo(0);

        assertThat(statisticsF1.equals(statisticsF1)).isTrue();
        assertThat(statisticsF1).isEqualTo(statisticsF1);
        assertThat(statisticsF1.equals(42)).isFalse();
        assertThat(statisticsF1).isEqualTo(statisticsG);
        assertThat(statisticsG).isEqualTo(statisticsF1);
        assertThat(statisticsF1.hashCode()).isEqualTo(statisticsG.hashCode());

        assertThat(statisticsF1.toString()).isEqualTo(
                "FormulaFactoryStatistics{" + "name='Factory F'" + ", positiveLiterals=0" + ", negativeLiterals=0" +
                        ", negations=0" + ", implications=0" + ", equivalences=0" + ", conjunctions2=0" +
                        ", conjunctions3=0" + ", conjunctions4=0" + ", conjunctionsN=0" + ", disjunctions2=0" +
                        ", disjunctions3=0" + ", disjunctions4=0" + ", disjunctionsN=0" + ", pbcs=0" + ", ccs=0" +
                        ", ccCounter=0" + ", pbCounter=0" + ", cnfCounter=0" + '}');

        final Variable a = f.variable("A");
        final Variable b = f.variable("B");
        final Variable c = f.variable("C");
        final Variable d = f.variable("D");
        final Variable e = f.variable("E");
        final And and = (And) f.and(a, b);
        final And and3 = (And) f.and(a, b, c);
        final And and4 = (And) f.and(a, b, c, d);
        final And and5 = (And) f.and(a, b, c, d, e);
        final Or or2 = (Or) f.or(a, b);
        final Or or3 = (Or) f.or(a, b, c);
        final Or or4 = (Or) f.or(a, b, c, d);
        final Or or5 = (Or) f.or(a, b, c, d, e);
        assertThat(f.posLiterals).containsValue(b);
        assertThat(f.ands2).containsValue(and);
        assertThat(f.ands3).containsValue(and3);
        assertThat(f.ands4).containsValue(and4);
        assertThat(f.andsN).containsValue(and5);
        assertThat(f.ors2).containsValue(or2);
        assertThat(f.ors3).containsValue(or3);
        assertThat(f.ors4).containsValue(or4);
        assertThat(f.orsN).containsValue(or5);

        final CachingFormulaFactory.Statistics statisticsF2 = f.statistics();

        assertThat(statisticsF2.name()).isEqualTo("Factory F");
        assertThat(statisticsF2.positiveLiterals()).isEqualTo(5);
        assertThat(statisticsF2.negativeLiterals()).isEqualTo(0);
        assertThat(statisticsF2.negations()).isEqualTo(0);
        assertThat(statisticsF2.implications()).isEqualTo(0);
        assertThat(statisticsF2.equivalences()).isEqualTo(0);
        assertThat(statisticsF2.conjunctions2()).isEqualTo(1);
        assertThat(statisticsF2.conjunctions3()).isEqualTo(1);
        assertThat(statisticsF2.conjunctions4()).isEqualTo(1);
        assertThat(statisticsF2.conjunctionsN()).isEqualTo(1);
        assertThat(statisticsF2.disjunctions2()).isEqualTo(1);
        assertThat(statisticsF2.disjunctions3()).isEqualTo(1);
        assertThat(statisticsF2.disjunctions4()).isEqualTo(1);
        assertThat(statisticsF2.disjunctionsN()).isEqualTo(1);
        assertThat(statisticsF2.pbcs()).isEqualTo(0);
        assertThat(statisticsF2.ccs()).isEqualTo(0);
        assertThat(statisticsF2.ccCounter()).isEqualTo(0);
        assertThat(statisticsF2.pbCounter()).isEqualTo(0);
        assertThat(statisticsF2.cnfCounter()).isEqualTo(0);
        assertThat(statisticsF2.formulas()).isEqualTo(13);

        assertThat(statisticsF2).isNotEqualTo(statisticsF1);
        assertThat(statisticsF1).isNotEqualTo(statisticsF2);

        assertThat(statisticsF2.toString()).isEqualTo(
                "FormulaFactoryStatistics{" + "name='Factory F'" + ", positiveLiterals=5" + ", negativeLiterals=0" +
                        ", negations=0" + ", implications=0" + ", equivalences=0" + ", conjunctions2=1" +
                        ", conjunctions3=1" + ", conjunctions4=1" + ", conjunctionsN=1" + ", disjunctions2=1" +
                        ", disjunctions3=1" + ", disjunctions4=1" + ", disjunctionsN=1" + ", pbcs=0" + ", ccs=0" +
                        ", ccCounter=0" + ", pbCounter=0" + ", cnfCounter=0" + '}');
    }
}
