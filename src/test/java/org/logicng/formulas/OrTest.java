// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class OrTest extends TestWithExampleFormulas {

    @Test
    public void testType() {
        assertThat(OR1.type()).isEqualTo(FType.OR);
    }

    @Test
    public void testCreator() {
        assertThat(f.or()).isEqualTo(FALSE);
        assertThat(f.or(TRUE)).isEqualTo(TRUE);
        assertThat(f.or(FALSE)).isEqualTo(FALSE);
        assertThat(f.or(TRUE, FALSE)).isEqualTo(TRUE);
        assertThat(f.or(FALSE, TRUE)).isEqualTo(TRUE);
        assertThat(f.or(NA)).isEqualTo(NA);
        assertThat(f.or(X, Y, X, Y, X)).isEqualTo(OR1);
        assertThat(f.or(f.or(X, Y), X, f.or(X, Y))).isEqualTo(OR1);
        assertThat(f.or(FALSE, X, Y, FALSE)).isEqualTo(OR1);
        assertThat(f.or(NA, NA, NA)).isEqualTo(NA);
        assertThat(f.or(NA, NA, FALSE, FALSE)).isEqualTo(NA);
        assertThat(f.or(NA, NA, TRUE, FALSE)).isEqualTo(TRUE);
        final List<Literal> lits = Arrays.asList(X, Y);
        assertThat(f.or(lits)).isEqualTo(OR1);
        assertThat(f.or(A, B, X, TRUE)).isEqualTo(TRUE);
        assertThat(f.or(f.or(A, B), f.or(X, Y))).isEqualTo(f.or(A, B, X, Y));
        assertThat(f.or(f.and(A, B), f.or(f.and(f.and(NA, NB)), f.and(f.or(NA, FALSE), NB)))).isEqualTo(OR3);
        assertThat(f.naryOperator(FType.OR, Arrays.asList(X, Y, X, Y, X))).isEqualTo(OR1);
    }

    @Test
    public void testComplementaryCheck() {
        assertThat(f.or(A, NA)).isEqualTo(TRUE);
        assertThat(f.or(A, B, f.or(C, X, NB))).isEqualTo(TRUE);
        assertThat(f.or(A, B, f.or(NX, B, X))).isEqualTo(TRUE);
        assertThat(f.or(X, Y, f.and(NX, B, X))).isEqualTo(OR1);
    }

    @Test
    public void testVariables() {
        assertThat(OR2.variables().size()).isEqualTo(2);
        SortedSet<Variable> lits = new TreeSet<>(Arrays.asList(X, Y));
        assertThat(OR2.variables()).isEqualTo(lits);

        final Formula or = f.or(A, A, B, IMP3);
        assertThat(or.variables().size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(A, B, X, Y));
        assertThat(or.variables()).isEqualTo(lits);
    }

    @Test
    public void testLiterals() {
        assertThat(OR2.literals().size()).isEqualTo(2);
        SortedSet<Literal> lits = new TreeSet<>(Arrays.asList(NX, NY));
        assertThat(OR2.literals()).isEqualTo(lits);

        final Formula or = f.or(A, A, B, f.implication(NB, NA));
        assertThat(or.literals().size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(A, NA, B, NB));
        assertThat(or.literals()).isEqualTo(lits);
    }

    @Test
    public void testToString() {
        final FormulaFactory f =
                FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(OR1.toString()).isEqualTo("x | y");
        assertThat(OR2.toString()).isEqualTo("~x | ~y");
        assertThat(OR3.toString()).isEqualTo("a & b | ~a & ~b");
        assertThat(f.or(A, B, NX, NY).toString()).isEqualTo("a | b | ~x | ~y");
        assertThat(f.or(IMP1, IMP2).toString()).isEqualTo("(a => b) | (~a => ~b)");
        assertThat(f.or(EQ1, EQ2).toString()).isEqualTo("(a <=> b) | (~a <=> ~b)");
    }

    @Test
    public void testEquals() {
        assertThat(f.or(X, Y)).isEqualTo(OR1);
        assertThat(f.or(AND1, AND2)).isEqualTo(OR3);
        assertThat(OR2).isEqualTo(OR2);
        assertThat(f.or(NX, A, NB, AND1)).isEqualTo(f.or(A, NB, AND1, NX));
        assertThat(OR2).isNotEqualTo(OR1);
        assertThat(f.or(A, B, C)).isNotEqualTo(OR1);
    }

    @Test
    public void testEqualsDifferentFormulaFactory() {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        final FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(g.or(g.variable("x"), g.variable("y"))).isEqualTo(OR1);
        assertThat(g.or(AND1, AND2)).isEqualTo(OR3);
        assertThat(g.or(g.and(g.literal("y", false), g.variable("x")), f.and(g.variable("b"), g.variable("a")))).isEqualTo(f.or(f.and(f.variable("a"), f.variable("b")), f.and(f.variable("x"), f.literal("y", false))));
        assertThat(g.or(g.literal("x", false), g.variable("a"), g.literal("b", false), g.and(g.variable("a"), g.variable("b")))).isEqualTo(f.or(A, NB, AND1, NX));
        assertThat(g.or(g.literal("a", false), g.variable("b"))).isNotEqualTo(OR1);
        assertThat(g.or(g.variable("a"), g.literal("b", false))).isNotEqualTo(OR1);
        assertThat(f.or(A, B, g.variable("c"))).isNotEqualTo(OR1);
    }

    @Test
    public void testHash() {
        final Formula or = f.or(AND1, AND2);
        assertThat(or.hashCode()).isEqualTo(OR3.hashCode());
        assertThat(or.hashCode()).isEqualTo(OR3.hashCode());
        assertThat(f.or(NX, NY).hashCode()).isEqualTo(OR2.hashCode());
    }

    @Test
    public void testNumberOfAtoms() {
        assertThat(OR1.numberOfAtoms()).isEqualTo(2);
        assertThat(OR2.numberOfAtoms()).isEqualTo(2);
        assertThat(OR3.numberOfAtoms()).isEqualTo(4);
        assertThat(OR3.numberOfAtoms()).isEqualTo(4);
    }

    @Test
    public void testNumberOfNodes() {
        assertThat(OR1.numberOfNodes()).isEqualTo(3);
        assertThat(OR2.numberOfNodes()).isEqualTo(3);
        assertThat(OR3.numberOfNodes()).isEqualTo(7);
        assertThat(OR3.numberOfNodes()).isEqualTo(7);
    }

    @Test
    public void testNumberOfInternalNodes() throws ParserException {
        final Formula or = new PropositionalParser(f).parse("a & (b | c) => (d <=> (b | c))");
        assertThat(OR3.numberOfInternalNodes()).isEqualTo(7);
        assertThat(or.numberOfInternalNodes()).isEqualTo(8);
    }

    @Test
    public void testNumberOfOperands() {
        assertThat(OR1.numberOfOperands()).isEqualTo(2);
        assertThat(OR3.numberOfOperands()).isEqualTo(2);
        assertThat(f.or(A, NX, EQ1).numberOfOperands()).isEqualTo(3);
    }

    @Test
    public void testIsConstantFormula() {
        assertThat(OR1.isConstantFormula()).isFalse();
        assertThat(OR2.isConstantFormula()).isFalse();
    }

    @Test
    public void testAtomicFormula() {
        assertThat(OR1.isAtomicFormula()).isFalse();
    }

    @Test
    public void testContains() throws ParserException {
        assertThat(OR1.containsVariable(f.variable("x"))).isTrue();
        assertThat(OR1.containsVariable(f.variable("a"))).isFalse();
        final PropositionalParser parser = new PropositionalParser(f);
        final Formula contAnd = parser.parse("a | b | (c & (d | e))");
        assertThat(contAnd.containsNode(parser.parse("d | e"))).isTrue();
    }

    @Test
    public void testIsNNF() {
        assertThat(OR1.isNNF()).isTrue();
        assertThat(OR2.isNNF()).isTrue();
        assertThat(OR3.isNNF()).isTrue();
    }

    @Test
    public void testIsDNF() {
        assertThat(OR1.isDNF()).isTrue();
        assertThat(OR2.isDNF()).isTrue();
        assertThat(OR3.isDNF()).isTrue();
    }

    @Test
    public void testIsCNF() {
        assertThat(OR1.isCNF()).isTrue();
        assertThat(OR2.isCNF()).isTrue();
        assertThat(OR3.isCNF()).isFalse();
    }
}
