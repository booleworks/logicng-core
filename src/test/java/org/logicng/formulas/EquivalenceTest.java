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
import java.util.SortedSet;
import java.util.TreeSet;

public class EquivalenceTest extends TestWithExampleFormulas {

    @Test
    public void testType() {
        assertThat(EQ1.type()).isEqualTo(FType.EQUIV);
    }

    @Test
    public void testCreator() {
        assertThat(f.equivalence(TRUE, AND1)).isEqualTo(AND1);
        assertThat(f.equivalence(AND1, TRUE)).isEqualTo(AND1);
        assertThat(f.equivalence(FALSE, AND1)).isEqualTo(NOT1);
        assertThat(f.equivalence(AND1, FALSE)).isEqualTo(NOT1);
        assertThat(f.equivalence(OR1, OR1)).isEqualTo(TRUE);
        assertThat(f.equivalence(NOT1, AND1)).isEqualTo(FALSE);
        assertThat(f.equivalence(AND1, NOT1)).isEqualTo(FALSE);
        assertThat(f.equivalence(OR1, NOT2)).isEqualTo(FALSE);
        assertThat(f.equivalence(NOT2, OR1)).isEqualTo(FALSE);
        assertThat(f.binaryOperator(FType.EQUIV, AND1, OR1)).isEqualTo(EQ3);
    }

    @Test
    public void testGetters() {
        assertThat(((Equivalence) EQ2).left()).isEqualTo(NA);
        assertThat(((Equivalence) EQ2).right()).isEqualTo(NB);
    }

    @Test
    public void testVariables() {
        assertThat(IMP3.variables().size()).isEqualTo(4);
        SortedSet<Variable> lits = new TreeSet<>(Arrays.asList(A, B, X, Y));
        assertThat(IMP3.variables()).isEqualTo(lits);

        final Formula equiv = f.equivalence(AND1, AND2);
        assertThat(equiv.variables().size()).isEqualTo(2);
        lits = new TreeSet<>(Arrays.asList(A, B));
        assertThat(equiv.variables()).isEqualTo(lits);
    }

    @Test
    public void testLiterals() {
        assertThat(IMP3.literals().size()).isEqualTo(4);
        SortedSet<Literal> lits = new TreeSet<>(Arrays.asList(A, B, X, Y));
        assertThat(IMP3.literals()).isEqualTo(lits);

        Formula equiv = f.equivalence(AND1, AND2);
        assertThat(equiv.literals().size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(A, B, NA, NB));
        assertThat(equiv.literals()).isEqualTo(lits);

        equiv = f.equivalence(AND1, A);
        assertThat(equiv.literals().size()).isEqualTo(2);
        lits = new TreeSet<>(Arrays.asList(A, B));
        assertThat(equiv.literals()).isEqualTo(lits);
    }

    @Test
    public void testNegation() {
        assertThat(EQ1.negate()).isEqualTo(f.not(EQ1));
        assertThat(EQ2.negate()).isEqualTo(f.not(EQ2));
        assertThat(EQ3.negate()).isEqualTo(f.not(EQ3));
        assertThat(EQ4.negate()).isEqualTo(f.not(EQ4));
    }

    @Test
    public void testToString() {
        assertThat(EQ1.toString()).isEqualTo("a <=> b");
        assertThat(EQ2.toString()).isEqualTo("~a <=> ~b");
        assertThat(EQ3.toString()).isEqualTo("a & b <=> x | y");
        assertThat(EQ4.toString()).isEqualTo("a => b <=> ~a => ~b");
    }

    @Test
    public void testEquals() {
        assertThat(f.equivalence(A, B)).isEqualTo(EQ1);
        assertThat(f.equivalence(B, A)).isEqualTo(EQ1);
        assertThat(f.equivalence(AND1, OR1)).isEqualTo(EQ3);
        assertThat(EQ4).isEqualTo(EQ4);
        assertThat(EQ2).isNotEqualTo(EQ1);
    }

    @Test
    public void testEqualsDifferentFormulaFactory() {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        final FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(g.equivalence(g.variable("a"), g.variable("b"))).isEqualTo(EQ1);
        assertThat(g.equivalence(B, A)).isEqualTo(EQ1);
        assertThat(g.equivalence(AND1, OR1)).isEqualTo(EQ3);
        assertThat(g.equivalence(g.literal("a", false), g.variable("b"))).isNotEqualTo(EQ1);
        assertThat(g.equivalence(g.variable("a"), g.literal("b", false))).isNotEqualTo(EQ1);
    }

    @Test
    public void testHash() {
        final Formula eq = f.equivalence(IMP1, IMP2);
        assertThat(eq.hashCode()).isEqualTo(EQ4.hashCode());
        assertThat(eq.hashCode()).isEqualTo(EQ4.hashCode());
        assertThat(f.equivalence(AND1, OR1).hashCode()).isEqualTo(EQ3.hashCode());
    }

    @Test
    public void testNumberOfAtoms() {
        assertThat(EQ1.numberOfAtoms()).isEqualTo(2);
        assertThat(EQ4.numberOfAtoms()).isEqualTo(4);
        assertThat(EQ4.numberOfAtoms()).isEqualTo(4);
    }

    @Test
    public void testNumberOfNodes() {
        assertThat(EQ1.numberOfNodes()).isEqualTo(3);
        assertThat(EQ4.numberOfNodes()).isEqualTo(7);
        assertThat(EQ4.numberOfNodes()).isEqualTo(7);
    }

    @Test
    public void testNumberOfInternalNodes() throws ParserException {
        final Formula eq = new PropositionalParser(f).parse("a & (b | c) <=> (d => (b | c))");
        assertThat(EQ4.numberOfInternalNodes()).isEqualTo(7);
        assertThat(eq.numberOfInternalNodes()).isEqualTo(8);
    }

    @Test
    public void testNumberOfOperands() {
        assertThat(EQ1.numberOfOperands()).isEqualTo(2);
        assertThat(EQ3.numberOfOperands()).isEqualTo(2);
        assertThat(EQ4.numberOfOperands()).isEqualTo(2);
    }

    @Test
    public void testIsConstantFormula() {
        assertThat(EQ1.isConstantFormula()).isFalse();
        assertThat(EQ2.isConstantFormula()).isFalse();
        assertThat(EQ3.isConstantFormula()).isFalse();
        assertThat(EQ4.isConstantFormula()).isFalse();
    }

    @Test
    public void testAtomicFormula() {
        assertThat(EQ1.isAtomicFormula()).isFalse();
        assertThat(EQ4.isAtomicFormula()).isFalse();
    }

    @Test
    public void testContains() {
        assertThat(EQ4.containsVariable(f.variable("a"))).isTrue();
        assertThat(EQ4.containsVariable(f.variable("x"))).isFalse();
        assertThat(EQ4.containsNode(IMP1)).isTrue();
        assertThat(EQ4.containsNode(IMP4)).isFalse();
    }

    @Test
    public void testIsNNF() {
        assertThat(EQ1.isNNF()).isFalse();
        assertThat(EQ2.isNNF()).isFalse();
        assertThat(EQ3.isNNF()).isFalse();
        assertThat(EQ4.isNNF()).isFalse();
    }

    @Test
    public void testIsDNF() {
        assertThat(EQ1.isDNF()).isFalse();
        assertThat(EQ2.isDNF()).isFalse();
        assertThat(EQ3.isDNF()).isFalse();
        assertThat(EQ4.isDNF()).isFalse();
    }

    @Test
    public void testIsCNF() {
        assertThat(EQ1.isCNF()).isFalse();
        assertThat(EQ2.isCNF()).isFalse();
        assertThat(EQ3.isCNF()).isFalse();
        assertThat(EQ4.isCNF()).isFalse();
    }
}
