// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.knowledgecompilation.bdds;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;

import java.util.ArrayList;
import java.util.List;

public class BDDConstructionTests {

    FormulaFactory f;
    List<Variable> variables;
    BDDKernel kernel;
    Formula initFormula;
    Formula secondFormula;
    BDD initBdd;
    BDD secondBdd;

    @BeforeEach
    public void init() throws ParserException {
        f = FormulaFactory.caching();
        variables = new ArrayList<>(f.variables("a", "b", "c", "d", "e", "f", "g"));
        kernel = new BDDKernel(f, variables, 1000, 10000);
        initFormula = f.parse("(a & b) => (c | d & ~e)");
        secondFormula = f.parse("(g & f) <=> (c | ~a | ~d)");
        initBdd = BDDFactory.build(f, initFormula, kernel);
        secondBdd = BDDFactory.build(f, secondFormula, kernel);
    }

    @Test
    public void testNegation() {
        final BDD negation = initBdd.negate();
        final BDD expected = BDDFactory.build(f, initFormula.negate(f), kernel);
        assertThat(negation).isEqualTo(expected);
    }

    @Test
    public void testImplies() {
        final BDD implication = initBdd.implies(secondBdd);
        final BDD expected = BDDFactory.build(f, f.implication(initFormula, secondFormula), kernel);
        assertThat(implication).isEqualTo(expected);
    }

    @Test
    public void testIsImplied() {
        final BDD implication = initBdd.impliedBy(secondBdd);
        final BDD expected = BDDFactory.build(f, f.implication(secondFormula, initFormula), kernel);
        assertThat(implication).isEqualTo(expected);
    }

    @Test
    public void testEquivalence() {
        final BDD equivalence = initBdd.equivalence(secondBdd);
        final BDD expected = BDDFactory.build(f, f.equivalence(secondFormula, initFormula), kernel);
        assertThat(equivalence).isEqualTo(expected);
    }

    @Test
    public void testAnd() {
        final BDD and = initBdd.and(secondBdd);
        final BDD expected = BDDFactory.build(f, f.and(secondFormula, initFormula), kernel);
        assertThat(and).isEqualTo(expected);
    }

    @Test
    public void testOr() {
        final BDD or = initBdd.or(secondBdd);
        final BDD expected = BDDFactory.build(f, f.or(secondFormula, initFormula), kernel);
        assertThat(or).isEqualTo(expected);
    }
}
