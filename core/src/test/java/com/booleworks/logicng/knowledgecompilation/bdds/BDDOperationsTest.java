// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.RandomTag;
import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import com.booleworks.logicng.util.FormulaRandomizer;
import com.booleworks.logicng.util.FormulaRandomizerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class BDDOperationsTest {

    private FormulaFactory f;
    private PropositionalParser parser;
    private BDDKernel kernel;
    private BDD bddVerum;
    private BDD bddFalsum;
    private BDD bddPosLit;
    private BDD bddNegLit;
    private BDD bddImpl;
    private BDD bddEquiv;
    private BDD bddOr;
    private BDD bddAnd;

    @BeforeEach
    public void init() throws ParserException {
        f = FormulaFactory.caching();
        parser = new PropositionalParser(f);
        kernel = new BDDKernel(f, 3, 1000, 1000);
        bddVerum = BDDFactory.build(f, f.verum(), kernel);
        bddFalsum = BDDFactory.build(f, f.falsum(), kernel);
        bddPosLit = BDDFactory.build(f, f.literal("A", true), kernel);
        bddNegLit = BDDFactory.build(f, f.literal("A", false), kernel);
        bddImpl = BDDFactory.build(f, parser.parse("A => ~B"), kernel);
        bddEquiv = BDDFactory.build(f, parser.parse("A <=> ~B"), kernel);
        bddOr = BDDFactory.build(f, parser.parse("A | B | ~C"), kernel);
        bddAnd = BDDFactory.build(f, parser.parse("A & B & ~C"), kernel);
    }

    @Test
    public void testToFormula() throws ParserException {
        assertThat(bddVerum.toFormula()).isEqualTo(f.verum());
        assertThat(bddFalsum.toFormula()).isEqualTo(f.falsum());
        assertThat(bddPosLit.toFormula()).isEqualTo(f.literal("A", true));
        assertThat(bddNegLit.toFormula()).isEqualTo(f.literal("A", false));
        assertThat(BDDFactory.build(f, f.literal("C", true)).toFormula()).isEqualTo(f.literal("C", true));
        assertThat(BDDFactory.build(f, f.literal("C", false)).toFormula()).isEqualTo(f.literal("C", false));
        compareFormula(bddImpl, "A => ~B");
        compareFormula(bddEquiv, "A <=> ~B");
        compareFormula(bddOr, "A | B | ~C");
        compareFormula(bddAnd, "A & B & ~C");
    }

    @Test
    public void testToFormulaStyles() throws ParserException {
        final BDD bdd = BDDFactory.build(f, f.parse("~A | ~B | ~C"), kernel);
        final Formula expFollowPathsToTrue = f.parse("~A | A & (~B | B & ~C)");
        assertThat(bdd.toFormula()).isEqualTo(expFollowPathsToTrue);
        assertThat(bdd.toFormula(true)).isEqualTo(expFollowPathsToTrue);
        assertThat(bdd.toFormula(false)).isEqualTo(f.parse("~(A & B & C)"));
    }

    @RandomTag
    @Test
    public void testToFormulaRandom() {
        final FormulaFactory f = FormulaFactory.caching();
        for (int i = 0; i < 100; i++) {
            final Formula formula =
                    new FormulaRandomizer(f, FormulaRandomizerConfig.builder().seed(i).build()).formula(6);
            final BDD bdd = BDDFactory.build(f, formula);
            compareFormula(bdd, formula);
        }
    }

    @Test
    public void testRestriction() throws ParserException {
        final Literal a = f.literal("A", true);
        final List<Literal> resNotA = Collections.singletonList(f.literal("A", false));
        final List<Literal> resAB = Arrays.asList(f.literal("A", true), f.literal("B", true));
        assertThat(bddPosLit.construction.restrict(0, 1)).isEqualTo(0);
        assertThat(bddPosLit.construction.restrict(1, 1)).isEqualTo(1);
        assertThat(bddVerum.restrict(a)).isEqualTo(bddVerum);
        assertThat(bddVerum.restrict(resNotA)).isEqualTo(bddVerum);
        assertThat(bddVerum.restrict(resAB)).isEqualTo(bddVerum);
        assertThat(bddFalsum.restrict(a)).isEqualTo(bddFalsum);
        assertThat(bddFalsum.restrict(resNotA)).isEqualTo(bddFalsum);
        assertThat(bddFalsum.restrict(resAB)).isEqualTo(bddFalsum);
        assertThat(bddPosLit.restrict(a)).isEqualTo(bddVerum);
        assertThat(bddPosLit.restrict(resNotA)).isEqualTo(bddFalsum);
        assertThat(bddPosLit.restrict(resAB)).isEqualTo(bddVerum);
        assertThat(bddNegLit.restrict(a)).isEqualTo(bddFalsum);
        assertThat(bddNegLit.restrict(resNotA)).isEqualTo(bddVerum);
        assertThat(bddNegLit.restrict(resAB)).isEqualTo(bddFalsum);
        assertThat(bddImpl.restrict(a)).isEqualTo(BDDFactory.build(f, f.literal("B", false), kernel));
        assertThat(bddImpl.restrict(resNotA)).isEqualTo(bddVerum);
        assertThat(bddImpl.restrict(resAB)).isEqualTo(bddFalsum);
        assertThat(bddEquiv.restrict(a)).isEqualTo(BDDFactory.build(f, f.literal("B", false), kernel));
        assertThat(bddEquiv.restrict(resNotA)).isEqualTo(BDDFactory.build(f, f.literal("B", true), kernel));
        assertThat(bddEquiv.restrict(resAB)).isEqualTo(bddFalsum);
        assertThat(bddOr.restrict(a)).isEqualTo(bddVerum);
        assertThat(bddOr.restrict(resNotA)).isEqualTo(BDDFactory.build(f, parser.parse("B | ~C"), kernel));
        assertThat(bddOr.restrict(resAB)).isEqualTo(bddVerum);
        assertThat(bddAnd.restrict(a)).isEqualTo(BDDFactory.build(f, parser.parse("B & ~C"), kernel));
        assertThat(bddAnd.restrict(resNotA)).isEqualTo(bddFalsum);
        assertThat(bddAnd.restrict(resAB)).isEqualTo(BDDFactory.build(f, f.literal("C", false), kernel));
    }

    @Test
    public void testExistentialQuantification() throws ParserException {
        final Variable a = f.variable("A");
        final List<Variable> resAB = Arrays.asList(f.variable("A"), f.variable("B"));
        assertThat(bddPosLit.construction.exists(0, 1)).isEqualTo(0);
        assertThat(bddPosLit.construction.exists(1, 1)).isEqualTo(1);
        assertThat(bddVerum.exists(a)).isEqualTo(bddVerum);
        assertThat(bddVerum.exists(resAB)).isEqualTo(bddVerum);
        assertThat(bddFalsum.exists(a)).isEqualTo(bddFalsum);
        assertThat(bddFalsum.exists(resAB)).isEqualTo(bddFalsum);
        assertThat(bddPosLit.exists(a)).isEqualTo(bddVerum);
        assertThat(bddPosLit.exists(resAB)).isEqualTo(bddVerum);
        assertThat(bddNegLit.exists(a)).isEqualTo(bddVerum);
        assertThat(bddNegLit.exists(resAB)).isEqualTo(bddVerum);
        assertThat(bddImpl.exists(a)).isEqualTo(bddVerum);
        assertThat(bddImpl.exists(resAB)).isEqualTo(bddVerum);
        assertThat(bddEquiv.exists(a)).isEqualTo(bddVerum);
        assertThat(bddEquiv.exists(resAB)).isEqualTo(bddVerum);
        assertThat(bddOr.exists(a)).isEqualTo(bddVerum);
        assertThat(bddOr.exists(resAB)).isEqualTo(bddVerum);
        assertThat(bddAnd.exists(a)).isEqualTo(BDDFactory.build(f, parser.parse("B & ~C"), kernel));
        assertThat(bddAnd.exists(resAB)).isEqualTo(BDDFactory.build(f, parser.parse("~C"), kernel));
    }

    @Test
    public void testUniversalQuantification() throws ParserException {
        final Variable a = f.variable("A");
        final List<Variable> resAB = Arrays.asList(f.variable("A"), f.variable("B"));
        assertThat(bddPosLit.construction.forAll(0, 1)).isEqualTo(0);
        assertThat(bddPosLit.construction.forAll(1, 1)).isEqualTo(1);
        assertThat(bddVerum.forall(a)).isEqualTo(bddVerum);
        assertThat(bddVerum.forall(resAB)).isEqualTo(bddVerum);
        assertThat(bddFalsum.forall(a)).isEqualTo(bddFalsum);
        assertThat(bddFalsum.forall(resAB)).isEqualTo(bddFalsum);
        assertThat(bddPosLit.forall(a)).isEqualTo(bddFalsum);
        assertThat(bddPosLit.forall(resAB)).isEqualTo(bddFalsum);
        assertThat(bddNegLit.forall(a)).isEqualTo(bddFalsum);
        assertThat(bddNegLit.forall(resAB)).isEqualTo(bddFalsum);
        assertThat(bddImpl.forall(a)).isEqualTo(BDDFactory.build(f, parser.parse("~B"), kernel));
        assertThat(bddImpl.forall(resAB)).isEqualTo(bddFalsum);
        assertThat(bddEquiv.forall(a)).isEqualTo(bddFalsum);
        assertThat(bddEquiv.forall(resAB)).isEqualTo(bddFalsum);
        assertThat(bddOr.forall(a)).isEqualTo(BDDFactory.build(f, parser.parse("B | ~C"), kernel));
        assertThat(bddOr.forall(resAB)).isEqualTo(BDDFactory.build(f, parser.parse("~C"), kernel));
        assertThat(bddAnd.forall(a)).isEqualTo(bddFalsum);
        assertThat(bddAnd.forall(resAB)).isEqualTo(bddFalsum);
    }

    @Test
    public void testModel() {
        assertThat(bddVerum.model()).isEqualTo(new Model());
        assertThat(bddFalsum.model()).isEqualTo(null);
        assertThat(bddPosLit.model()).isEqualTo(new Model(f.literal("A", true)));
        assertThat(bddNegLit.model()).isEqualTo(new Model(f.literal("A", false)));
        assertThat(bddImpl.model()).isEqualTo(new Model(f.literal("A", false)));
        assertThat(bddEquiv.model().toAssignment()).isEqualTo(new Assignment(f.literal("A", false), f.literal("B", true)));
        assertThat(bddOr.model().toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", false), f.literal("C", false)));
        assertThat(bddAnd.model().toAssignment())
                .isEqualTo(new Assignment(f.literal("A", true), f.literal("B", true), f.literal("C", false)));
    }

    @Test
    public void testModelWithGivenVars() {
        final Variable a = f.variable("A");
        final List<Variable> ab = Arrays.asList(f.variable("A"), f.variable("B"));
        assertThat(bddVerum.model(true, a)).isEqualTo(new Model(f.literal("A", true)));
        assertThat(bddVerum.model(true, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", true), f.literal("B", true)));
        assertThat(bddVerum.model(false, a)).isEqualTo(new Model(f.literal("A", false)));
        assertThat(bddVerum.model(false, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", false)));
        assertThat(bddFalsum.model(true, a)).isEqualTo(null);
        assertThat(bddFalsum.model(true, ab)).isEqualTo(null);
        assertThat(bddFalsum.model(false, a)).isEqualTo(null);
        assertThat(bddFalsum.model(false, ab)).isEqualTo(null);
        assertThat(bddPosLit.model(true, a).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", true)));
        assertThat(bddPosLit.model(true, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", true), f.literal("B", true)));
        assertThat(bddPosLit.model(false, a).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", true)));
        assertThat(bddPosLit.model(false, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", true), f.literal("B", false)));
        assertThat(bddNegLit.model(true, a).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false)));
        assertThat(bddNegLit.model(true, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", true)));
        assertThat(bddNegLit.model(false, a).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false)));
        assertThat(bddNegLit.model(false, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", false)));
        assertThat(bddImpl.model(true, a).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false)));
        assertThat(bddImpl.model(true, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", true)));
        assertThat(bddImpl.model(false, a).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false)));
        assertThat(bddImpl.model(false, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", false)));
        assertThat(bddEquiv.model(true, a).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", true)));
        assertThat(bddEquiv.model(true, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", true)));
        assertThat(bddEquiv.model(false, a).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", true)));
        assertThat(bddEquiv.model(false, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", true)));
        assertThat(bddOr.model(true, a).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", false), f.literal("C", false)));
        assertThat(bddOr.model(true, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", false), f.literal("C", false)));
        assertThat(bddOr.model(false, a).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", false), f.literal("C", false)));
        assertThat(bddOr.model(false, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", false), f.literal("C", false)));
        assertThat(bddAnd.model(true, a).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", true), f.literal("B", true), f.literal("C", false)));
        assertThat(bddAnd.model(true, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", true), f.literal("B", true), f.literal("C", false)));
        assertThat(bddAnd.model(false, a).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", true), f.literal("B", true), f.literal("C", false)));
        assertThat(bddAnd.model(false, ab).toAssignment())
                .isEqualTo(new Assignment(f.literal("A", true), f.literal("B", true), f.literal("C", false)));
    }

    @Test
    public void testFullModel() {
        assertThat(bddVerum.fullModel().toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", false), f.literal("C", false)));
        assertThat(bddFalsum.fullModel()).isEqualTo(null);
        assertThat(bddPosLit.fullModel().toAssignment())
                .isEqualTo(new Assignment(f.literal("A", true), f.literal("B", false), f.literal("C", false)));
        assertThat(bddNegLit.fullModel().toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", false), f.literal("C", false)));
        assertThat(bddImpl.fullModel().toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", false), f.literal("C", false)));
        assertThat(bddEquiv.fullModel().toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", true), f.literal("C", false)));
        assertThat(bddOr.fullModel().toAssignment())
                .isEqualTo(new Assignment(f.literal("A", false), f.literal("B", false), f.literal("C", false)));
        assertThat(bddAnd.fullModel().toAssignment())
                .isEqualTo(new Assignment(f.literal("A", true), f.literal("B", true), f.literal("C", false)));
    }

    @Test
    public void testPathCount() {
        assertThat(bddVerum.pathCountOne()).isEqualTo(BigInteger.ONE);
        assertThat(bddVerum.pathCountZero()).isEqualTo(BigInteger.ZERO);
        assertThat(bddFalsum.pathCountOne()).isEqualTo(BigInteger.ZERO);
        assertThat(bddFalsum.pathCountZero()).isEqualTo(BigInteger.ONE);
        assertThat(bddPosLit.pathCountOne()).isEqualTo(BigInteger.ONE);
        assertThat(bddPosLit.pathCountZero()).isEqualTo(BigInteger.ONE);
        assertThat(bddNegLit.pathCountOne()).isEqualTo(BigInteger.ONE);
        assertThat(bddNegLit.pathCountZero()).isEqualTo(BigInteger.ONE);
        assertThat(bddImpl.pathCountOne()).isEqualTo(BigInteger.valueOf(2));
        assertThat(bddImpl.pathCountZero()).isEqualTo(BigInteger.valueOf(1));
        assertThat(bddEquiv.pathCountOne()).isEqualTo(BigInteger.valueOf(2));
        assertThat(bddEquiv.pathCountZero()).isEqualTo(BigInteger.valueOf(2));
        assertThat(bddOr.pathCountOne()).isEqualTo(BigInteger.valueOf(3));
        assertThat(bddOr.pathCountZero()).isEqualTo(BigInteger.valueOf(1));
        assertThat(bddAnd.pathCountOne()).isEqualTo(BigInteger.valueOf(1));
        assertThat(bddAnd.pathCountZero()).isEqualTo(BigInteger.valueOf(3));
    }

    @Test
    public void testSupport() {
        assertThat(bddVerum.support()).isEqualTo(new TreeSet<>());
        assertThat(bddFalsum.support()).isEqualTo(new TreeSet<>());
        assertThat(bddPosLit.support()).isEqualTo(new TreeSet<>(Collections.singletonList(f.variable("A"))));
        assertThat(bddNegLit.support()).isEqualTo(new TreeSet<>(Collections.singletonList(f.variable("A"))));
        assertThat(bddImpl.support()).isEqualTo(new TreeSet<>(Arrays.asList(f.variable("A"), f.variable("B"))));
        assertThat(bddEquiv.support()).isEqualTo(new TreeSet<>(Arrays.asList(f.variable("A"), f.variable("B"))));
        assertThat(bddOr.support())
                .isEqualTo(new TreeSet<>(Arrays.asList(f.variable("A"), f.variable("B"), f.variable("C"))));
        assertThat(bddAnd.support())
                .isEqualTo(new TreeSet<>(Arrays.asList(f.variable("A"), f.variable("B"), f.variable("C"))));
    }

    @Test
    public void testNodeCount() {
        assertThat(bddVerum.nodeCount()).isEqualTo(0);
        assertThat(bddFalsum.nodeCount()).isEqualTo(0);
        assertThat(bddPosLit.nodeCount()).isEqualTo(1);
        assertThat(bddNegLit.nodeCount()).isEqualTo(1);
        assertThat(bddImpl.nodeCount()).isEqualTo(2);
        assertThat(bddEquiv.nodeCount()).isEqualTo(3);
        assertThat(bddOr.nodeCount()).isEqualTo(3);
        assertThat(bddAnd.nodeCount()).isEqualTo(3);
    }

    @Test
    public void testVariableProfile() {
        final Variable a = f.variable("A");
        final Variable b = f.variable("B");
        final Variable c = f.variable("C");
        final Map.Entry<Variable, Integer> a0 = new AbstractMap.SimpleEntry<>(a, 0);
        final Map.Entry<Variable, Integer> a1 = new AbstractMap.SimpleEntry<>(a, 1);
        final Map.Entry<Variable, Integer> b0 = new AbstractMap.SimpleEntry<>(b, 0);
        final Map.Entry<Variable, Integer> b1 = new AbstractMap.SimpleEntry<>(b, 1);
        final Map.Entry<Variable, Integer> b2 = new AbstractMap.SimpleEntry<>(b, 2);
        final Map.Entry<Variable, Integer> c0 = new AbstractMap.SimpleEntry<>(c, 0);
        final Map.Entry<Variable, Integer> c1 = new AbstractMap.SimpleEntry<>(c, 1);
        assertThat(bddVerum.variableProfile()).containsExactly(a0, b0, c0);
        assertThat(bddFalsum.variableProfile()).containsExactly(a0, b0, c0);
        assertThat(bddPosLit.variableProfile()).containsExactly(a1, b0, c0);
        assertThat(bddNegLit.variableProfile()).containsExactly(a1, b0, c0);
        assertThat(bddImpl.variableProfile()).containsExactly(a1, b1, c0);
        assertThat(bddEquiv.variableProfile()).containsExactly(a1, b2, c0);
        assertThat(bddOr.variableProfile()).containsExactly(a1, b1, c1);
        assertThat(bddAnd.variableProfile()).containsExactly(a1, b1, c1);
    }

    private void compareFormula(final BDD bdd, final String formula) throws ParserException {
        compareFormula(bdd, bdd.kernel.factory().parse(formula));
    }

    private void compareFormula(final BDD bdd, final Formula compareFormula) {
        final Formula bddFormulaFollowPathsToTrue = bdd.toFormula(true);
        final Formula bddFormulaFollowPathsToFalse = bdd.toFormula(false);
        assertThat(bddFormulaFollowPathsToTrue.isEquivalentTo(compareFormula.getFactory(), compareFormula)).isTrue();
        assertThat(bddFormulaFollowPathsToFalse.isEquivalentTo(compareFormula.getFactory(), compareFormula)).isTrue();
    }
}
