// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.logicng.TestWithExampleFormulas;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

public class ImplicationTest extends TestWithExampleFormulas {

    @Test
    public void testType() {
        assertThat(IMP1.type()).isEqualTo(FType.IMPL);
    }

    @Test
    public void testCreator() {
        assertThat(f.implication(FALSE, A)).isEqualTo(TRUE);
        assertThat(f.implication(A, TRUE)).isEqualTo(TRUE);
        assertThat(f.implication(TRUE, A)).isEqualTo(A);
        assertThat(f.implication(A, FALSE)).isEqualTo(NA);
        assertThat(f.implication(A, A)).isEqualTo(TRUE);
        assertThat(f.binaryOperator(FType.IMPL, AND1, OR1)).isEqualTo(IMP3);
    }

    @Test
    public void testIllegalCreation() {
        assertThatThrownBy(() -> f.binaryOperator(FType.NOT, AND1, OR1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testGetters() {
        assertThat(((Implication) IMP2).left()).isEqualTo(NA);
        assertThat(((Implication) IMP2).right()).isEqualTo(NB);
    }

    @Test
    public void testVariables() {
        assertThat(IMP3.variables().size()).isEqualTo(4);
        SortedSet<Variable> lits = new TreeSet<>(Arrays.asList(A, B, X, Y));
        assertThat(IMP3.variables()).isEqualTo(lits);

        final Formula imp = f.implication(AND1, AND2);
        assertThat(imp.variables().size()).isEqualTo(2);
        lits = new TreeSet<>(Arrays.asList(A, B));
        assertThat(imp.variables()).isEqualTo(lits);
    }

    @Test
    public void testLiterals() {
        assertThat(IMP3.literals().size()).isEqualTo(4);
        SortedSet<Literal> lits = new TreeSet<>(Arrays.asList(A, B, X, Y));
        assertThat(IMP3.literals()).isEqualTo(lits);

        Formula imp = f.implication(AND1, AND2);
        assertThat(imp.literals().size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(A, B, NA, NB));
        assertThat(imp.literals()).isEqualTo(lits);

        imp = f.implication(AND1, A);
        assertThat(imp.literals().size()).isEqualTo(2);
        lits = new TreeSet<>(Arrays.asList(A, B));
        assertThat(imp.literals()).isEqualTo(lits);
    }

    @Test
    public void testNegation() {
        assertThat(IMP1.negate()).isEqualTo(f.not(IMP1));
        assertThat(IMP2.negate()).isEqualTo(f.not(IMP2));
        assertThat(IMP3.negate()).isEqualTo(f.not(IMP3));
        assertThat(IMP4.negate()).isEqualTo(f.not(IMP4));
    }

    @Test
    public void testToString() {
        assertThat(IMP1.toString()).isEqualTo("a => b");
        assertThat(IMP2.toString()).isEqualTo("~a => ~b");
        assertThat(IMP3.toString()).isEqualTo("a & b => x | y");
        assertThat(IMP4.toString()).isEqualTo("(a <=> b) => (~x <=> ~y)");
    }

    @Test
    public void testEquals() {
        assertThat(f.implication(A, B)).isEqualTo(IMP1);
        assertThat(f.implication(AND1, OR1)).isEqualTo(IMP3);
        assertThat(IMP2).isEqualTo(IMP2);
        assertThat(IMP2).isNotEqualTo(IMP1);
        assertThat(IMP2).isNotEqualTo("String");
    }

    @Test
    public void testEqualsDifferentFormulaFactory() {
        final FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(g.implication(g.variable("a"), g.variable("b"))).isEqualTo(IMP1);
        assertThat(g.implication(AND1, OR1)).isEqualTo(IMP3);
        assertThat(g.implication(g.variable("b"), g.variable("a"))).isNotEqualTo(IMP1);
        assertThat(g.implication(g.literal("a", false), g.variable("b"))).isNotEqualTo(IMP1);
        assertThat(g.implication(g.variable("a"), g.literal("b", false))).isNotEqualTo(IMP1);
    }

    @Test
    public void testHash() {
        final Formula imp = f.implication(NA, NB);
        assertThat(imp.hashCode()).isEqualTo(IMP2.hashCode());
        assertThat(imp.hashCode()).isEqualTo(IMP2.hashCode());
        assertThat(f.implication(AND1, OR1).hashCode()).isEqualTo(IMP3.hashCode());
    }

    @Test
    public void testNumberOfAtoms() {
        assertThat(IMP1.numberOfAtoms()).isEqualTo(2);
        assertThat(IMP3.numberOfAtoms()).isEqualTo(4);
        assertThat(IMP3.numberOfAtoms()).isEqualTo(4);
    }

    @Test
    public void testNumberOfNodes() {
        assertThat(IMP1.numberOfNodes()).isEqualTo(3);
        assertThat(IMP4.numberOfNodes()).isEqualTo(7);
        assertThat(IMP4.numberOfNodes()).isEqualTo(7);
    }

    @Test
    public void testNumberOfInternalNodes() throws ParserException {
        final Formula imp = new PropositionalParser(f).parse("a & (b | c) => (d <=> (b | c))");
        assertThat(IMP4.numberOfInternalNodes()).isEqualTo(7);
        assertThat(imp.numberOfInternalNodes()).isEqualTo(8);
    }

    @Test
    public void testNumberOfOperands() {
        assertThat(IMP1.numberOfOperands()).isEqualTo(2);
        assertThat(IMP3.numberOfOperands()).isEqualTo(2);
        assertThat(IMP4.numberOfOperands()).isEqualTo(2);
    }

    @Test
    public void testIsConstantFormula() {
        assertThat(IMP1.isConstantFormula()).isFalse();
        assertThat(IMP2.isConstantFormula()).isFalse();
        assertThat(IMP3.isConstantFormula()).isFalse();
        assertThat(IMP4.isConstantFormula()).isFalse();
    }

    @Test
    public void testAtomicFormula() {
        assertThat(IMP1.isAtomicFormula()).isFalse();
        assertThat(IMP4.isAtomicFormula()).isFalse();
    }

    @Test
    public void testContains() {
        assertThat(IMP4.containsVariable(f.variable("a"))).isTrue();
        assertThat(IMP4.containsVariable(f.variable("c"))).isFalse();
    }

    @Test
    public void testIsNNF() {
        assertThat(IMP1.isNNF()).isFalse();
        assertThat(IMP2.isNNF()).isFalse();
        assertThat(IMP3.isNNF()).isFalse();
        assertThat(IMP4.isNNF()).isFalse();
    }

    @Test
    public void testIsDNF() {
        assertThat(IMP1.isDNF()).isFalse();
        assertThat(IMP2.isDNF()).isFalse();
        assertThat(IMP3.isDNF()).isFalse();
        assertThat(IMP4.isDNF()).isFalse();
    }

    @Test
    public void testIsCNF() {
        assertThat(IMP1.isCNF()).isFalse();
        assertThat(IMP2.isCNF()).isFalse();
        assertThat(IMP3.isCNF()).isFalse();
        assertThat(IMP4.isCNF()).isFalse();
    }
}
