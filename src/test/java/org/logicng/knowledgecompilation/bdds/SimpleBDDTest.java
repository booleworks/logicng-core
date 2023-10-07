// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.knowledgecompilation.bdds;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.knowledgecompilation.bdds.datastructures.BDDConstant;
import org.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class SimpleBDDTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testTrue(final FormulaContext _c) {
        final BDD bdd = BDDFactory.build(_c.f, _c.f.verum());
        assertThat(bdd.isTautology()).isTrue();
        assertThat(bdd.isContradiction()).isFalse();
        assertThat(bdd.cnf()).isEqualTo(_c.f.verum());
        assertThat(bdd.dnf()).isEqualTo(_c.f.verum());
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.ONE);
        assertThat(bdd.underlyingKernel().factory()).isSameAs(_c.f);
        assertThat(bdd.enumerateAllModels()).containsExactly(new Assignment());
        assertThat(bdd.numberOfClausesCNF()).isEqualTo(BigInteger.ZERO);
        assertThat(bdd.toLngBdd()).isEqualTo(BDDConstant.getVerumNode(_c.f));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFalse(final FormulaContext _c) {
        final BDDKernel kernel = new BDDKernel(_c.f, 0, 100, 100);
        final BDD bdd = BDDFactory.build(_c.f, _c.f.falsum(), kernel, null);
        assertThat(bdd.isTautology()).isFalse();
        assertThat(bdd.isContradiction()).isTrue();
        assertThat(bdd.cnf()).isEqualTo(_c.f.falsum());
        assertThat(bdd.dnf()).isEqualTo(_c.f.falsum());
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.ZERO);
        assertThat(bdd.underlyingKernel()).isSameAs(kernel);
        assertThat(bdd.underlyingKernel().factory()).isSameAs(_c.f);
        assertThat(bdd.enumerateAllModels()).isEmpty();
        assertThat(bdd.numberOfClausesCNF()).isEqualTo(BigInteger.ONE);
        assertThat(bdd.toLngBdd()).isEqualTo(BDDConstant.getFalsumNode(_c.f));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPositiveLiteral(final FormulaContext _c) {
        final BDD bdd = BDDFactory.build(_c.f, _c.f.literal("A", true));
        assertThat(bdd.isTautology()).isFalse();
        assertThat(bdd.isContradiction()).isFalse();
        assertThat(bdd.cnf()).isEqualTo(_c.f.literal("A", true));
        assertThat(bdd.dnf()).isEqualTo(_c.f.literal("A", true));
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.ONE);
        assertThat(bdd.underlyingKernel().factory()).isSameAs(_c.f);
        assertThat(bdd.enumerateAllModels()).containsExactly(new Assignment(_c.f.literal("A", true)));
        assertThat(bdd.numberOfClausesCNF()).isEqualTo(BigInteger.ONE);
        assertThat(bdd.toLngBdd().toString()).isEqualTo("<A | low=<$false> high=<$true>>");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNegativeLiteral(final FormulaContext _c) {
        final BDD bdd = BDDFactory.build(_c.f, _c.f.literal("A", false));
        assertThat(bdd.isTautology()).isFalse();
        assertThat(bdd.isContradiction()).isFalse();
        assertThat(bdd.cnf()).isEqualTo(_c.f.literal("A", false));
        assertThat(bdd.dnf()).isEqualTo(_c.f.literal("A", false));
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.ONE);
        assertThat(bdd.underlyingKernel().factory()).isSameAs(_c.f);
        assertThat(bdd.enumerateAllModels()).containsExactly(new Assignment(_c.f.literal("A", false)));
        assertThat(bdd.numberOfClausesCNF()).isEqualTo(BigInteger.ONE);
        assertThat(bdd.toLngBdd().toString()).isEqualTo("<A | low=<$true> high=<$false>>");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testImplication(final FormulaContext _c) throws ParserException {
        final BDD bdd = BDDFactory.build(_c.f, _c.p.parse("A => ~B"));
        assertThat(bdd.isTautology()).isFalse();
        assertThat(bdd.isContradiction()).isFalse();
        assertThat(bdd.cnf()).isEqualTo(_c.p.parse("~A | ~B"));
        assertThat(bdd.dnf()).isEqualTo(_c.p.parse("~A | A & ~B"));
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.valueOf(3));
        assertThat(bdd.underlyingKernel().factory()).isSameAs(_c.f);
        assertThat(bdd.enumerateAllModels()).containsExactlyInAnyOrder(
                new Assignment(_c.f.literal("A", false), _c.f.literal("B", false)),
                new Assignment(_c.f.literal("A", true), _c.f.literal("B", false)),
                new Assignment(_c.f.literal("A", false), _c.f.literal("B", true))
        );
        assertThat(bdd.numberOfClausesCNF()).isEqualTo(BigInteger.ONE);
        assertThat(bdd.toLngBdd().toString()).isEqualTo("<A | low=<$true> high=<B | low=<$true> high=<$false>>>");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEquivalence(final FormulaContext _c) throws ParserException {
        final BDD bdd = BDDFactory.build(_c.f, _c.p.parse("A <=> ~B"));
        assertThat(bdd.isTautology()).isFalse();
        assertThat(bdd.isContradiction()).isFalse();
        assertThat(bdd.cnf()).isEqualTo(_c.p.parse("(A | B) & (~A | ~B)"));
        assertThat(bdd.dnf()).isEqualTo(_c.p.parse("~A & B | A & ~B"));
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.valueOf(2));
        assertThat(bdd.underlyingKernel().factory()).isSameAs(_c.f);
        assertThat(bdd.enumerateAllModels()).containsExactlyInAnyOrder(
                new Assignment(_c.f.literal("A", true), _c.f.literal("B", false)),
                new Assignment(_c.f.literal("A", false), _c.f.literal("B", true))
        );
        assertThat(bdd.numberOfClausesCNF()).isEqualTo(BigInteger.valueOf(2));
        assertThat(bdd.toLngBdd().toString()).isEqualTo("<A | low=<B | low=<$false> high=<$true>> high=<B | low=<$true> high=<$false>>>");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testOr(final FormulaContext _c) throws ParserException {
        final BDD bdd = BDDFactory.build(_c.f, _c.p.parse("A | B | ~C"));
        assertThat(bdd.isTautology()).isFalse();
        assertThat(bdd.isContradiction()).isFalse();
        assertThat(bdd.cnf()).isEqualTo(_c.p.parse("A | B | ~C"));
        assertThat(bdd.dnf()).isEqualTo(_c.p.parse("~A & ~B & ~C | ~A & B | A"));
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.valueOf(7));
        assertThat(bdd.underlyingKernel().factory()).isSameAs(_c.f);
        assertThat(bdd.enumerateAllModels()).containsExactlyInAnyOrder(
                new Assignment(_c.f.literal("A", false), _c.f.literal("B", false), _c.f.literal("C", false)),
                new Assignment(_c.f.literal("A", false), _c.f.literal("B", true), _c.f.literal("C", false)),
                new Assignment(_c.f.literal("A", false), _c.f.literal("B", true), _c.f.literal("C", true)),
                new Assignment(_c.f.literal("A", true), _c.f.literal("B", false), _c.f.literal("C", false)),
                new Assignment(_c.f.literal("A", true), _c.f.literal("B", false), _c.f.literal("C", true)),
                new Assignment(_c.f.literal("A", true), _c.f.literal("B", true), _c.f.literal("C", false)),
                new Assignment(_c.f.literal("A", true), _c.f.literal("B", true), _c.f.literal("C", true))
        );
        assertThat(bdd.numberOfClausesCNF()).isEqualTo(BigInteger.ONE);
        assertThat(bdd.toLngBdd().toString()).isEqualTo("<A | low=<B | low=<C | low=<$true> high=<$false>> high=<$true>> high=<$true>>");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testAnd(final FormulaContext _c) throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser parser = new PropositionalParser(f);
        final List<Variable> ordering = Arrays.asList(f.variable("A"), f.variable("B"), f.variable("C"));
        final BDDKernel kernel = new BDDKernel(f, ordering, 1000, 1000);
        final BDD bdd = BDDFactory.build(f, parser.parse("A & B & ~C"), kernel, null);
        assertThat(bdd.isTautology()).isFalse();
        assertThat(bdd.isContradiction()).isFalse();
        assertThat(bdd.cnf()).isEqualTo(parser.parse("A & (~A | B) & (~A | ~B | ~C)"));
        assertThat(bdd.dnf()).isEqualTo(parser.parse("A & B & ~C"));
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.valueOf(1));
        assertThat(bdd.underlyingKernel().factory()).isSameAs(f);
        assertThat(bdd.enumerateAllModels()).containsExactlyInAnyOrder(
                new Assignment(f.literal("A", true), f.literal("B", true), f.literal("C", false))
        );
        assertThat(bdd.numberOfClausesCNF()).isEqualTo(BigInteger.valueOf(3));
        assertThat(bdd.toLngBdd().toString()).isEqualTo("<A | low=<$false> high=<B | low=<$false> high=<C | low=<$true> high=<$false>>>>");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFormula(final FormulaContext _c) throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser parser = new PropositionalParser(f);
        final List<Variable> ordering = Arrays.asList(f.variable("A"), f.variable("B"), f.variable("C"));
        final BDDKernel kernel = new BDDKernel(f, ordering, 1000, 1000);
        final BDD bdd = BDDFactory.build(f, parser.parse("(A => ~C) | (B & ~C)"), kernel, null);
        assertThat(bdd.isTautology()).isFalse();
        assertThat(bdd.isContradiction()).isFalse();
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.valueOf(6));
        assertThat(bdd.underlyingKernel().factory()).isSameAs(f);
        assertThat(bdd.enumerateAllModels()).hasSize(6);
        assertThat(bdd.enumerateAllModels(f.variable("A"))).hasSize(2);
        assertThat(bdd.hashCode()).isEqualTo(BDDFactory.build(f, parser.parse("(A => ~C) | (B & ~C)"), kernel, null).hashCode());
        assertThat(bdd.toString()).isEqualTo("BDD{8}");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testCC(final FormulaContext _c) throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser parser = new PropositionalParser(f);
        final BDDKernel kernel = new BDDKernel(f, 3, 1000, 1000);
        final BDD bdd = BDDFactory.build(f, parser.parse("A + B + C = 1"), kernel, null);
        assertThat(bdd.isTautology()).isFalse();
        assertThat(bdd.isContradiction()).isFalse();
        assertThat(bdd.cnf()).isEqualTo(parser.parse("(A | B | C) & (A | ~B | ~C) & (~A | B | ~C) & (~A | ~B)"));
        assertThat(bdd.dnf()).isEqualTo(parser.parse("~A & ~B & C | ~A & B & ~C | A & ~B & ~C"));
        assertThat(bdd.modelCount()).isEqualTo(BigInteger.valueOf(3));
        assertThat(bdd.underlyingKernel().factory()).isSameAs(f);
        assertThat(bdd.enumerateAllModels()).containsExactlyInAnyOrder(
                new Assignment(f.literal("A", true), f.literal("B", false), f.literal("C", false)),
                new Assignment(f.literal("A", false), f.literal("B", true), f.literal("C", false)),
                new Assignment(f.literal("A", false), f.literal("B", false), f.literal("C", true))
        );
        assertThat(bdd.numberOfClausesCNF()).isEqualTo(BigInteger.valueOf(4));
    }
}
