// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class BddConstructionTests {

    FormulaFactory f;
    List<Variable> variables;
    BddKernel kernel;
    Formula initFormula;
    Formula secondFormula;
    Bdd initBdd;
    Bdd secondBdd;

    @BeforeEach
    public void init() throws ParserException {
        f = FormulaFactory.caching();
        variables = new ArrayList<>(f.variables("a", "b", "c", "d", "e", "f", "g"));
        kernel = new BddKernel(f, variables, 1000, 10000);
        initFormula = f.parse("(a & b) => (c | d & ~e)");
        secondFormula = f.parse("(g & f) <=> (c | ~a | ~d)");
        initBdd = BddFactory.build(f, initFormula, kernel);
        secondBdd = BddFactory.build(f, secondFormula, kernel);
    }

    @Test
    public void testNegation() {
        final Bdd negation = initBdd.negate();
        final Bdd expected = BddFactory.build(f, initFormula.negate(f), kernel);
        assertThat(negation).isEqualTo(expected);
    }

    @Test
    public void testImplies() {
        final Bdd implication = initBdd.implies(secondBdd);
        final Bdd expected = BddFactory.build(f, f.implication(initFormula, secondFormula), kernel);
        assertThat(implication).isEqualTo(expected);
    }

    @Test
    public void testIsImplied() {
        final Bdd implication = initBdd.impliedBy(secondBdd);
        final Bdd expected = BddFactory.build(f, f.implication(secondFormula, initFormula), kernel);
        assertThat(implication).isEqualTo(expected);
    }

    @Test
    public void testEquivalence() {
        final Bdd equivalence = initBdd.equivalence(secondBdd);
        final Bdd expected = BddFactory.build(f, f.equivalence(secondFormula, initFormula), kernel);
        assertThat(equivalence).isEqualTo(expected);
    }

    @Test
    public void testAnd() {
        final Bdd and = initBdd.and(secondBdd);
        final Bdd expected = BddFactory.build(f, f.and(secondFormula, initFormula), kernel);
        assertThat(and).isEqualTo(expected);
    }

    @Test
    public void testOr() {
        final Bdd or = initBdd.or(secondBdd);
        final Bdd expected = BddFactory.build(f, f.or(secondFormula, initFormula), kernel);
        assertThat(or).isEqualTo(expected);
    }
}
