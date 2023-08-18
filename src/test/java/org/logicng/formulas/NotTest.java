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

public class NotTest extends TestWithExampleFormulas {

    @Test
    public void testType() {
        assertThat(NOT1.type()).isEqualTo(FType.NOT);
    }

    @Test
    public void testCreator() {
        assertThat(f.not(FALSE)).isEqualTo(TRUE);
        assertThat(f.not(TRUE)).isEqualTo(FALSE);
        assertThat(f.not(NA)).isEqualTo(A);
        assertThat(f.not(A)).isEqualTo(NA);
        assertThat(f.not(f.not(IMP3))).isEqualTo(IMP3);
        assertThat(f.not(AND1)).isEqualTo(NOT1);
    }

    @Test
    public void testGetters() {
        assertThat(((Not) NOT1).operand()).isEqualTo(AND1);
        assertThat(((Not) NOT2).operand()).isEqualTo(OR1);
    }

    @Test
    public void testVariables() {
        assertThat(NOT1.variables().size()).isEqualTo(2);
        SortedSet<Variable> lits = new TreeSet<>(Arrays.asList(A, B));
        assertThat(NOT1.variables()).isEqualTo(lits);

        assertThat(NOT2.variables().size()).isEqualTo(2);
        lits = new TreeSet<>(Arrays.asList(X, Y));
        assertThat(NOT2.variables()).isEqualTo(lits);
    }

    @Test
    public void testLiterals() {
        assertThat(NOT1.literals().size()).isEqualTo(2);
        SortedSet<? extends Literal> lits = new TreeSet<>(Arrays.asList(A, B));
        assertThat(NOT1.literals()).isEqualTo(lits);

        final Formula not = f.not(f.and(A, NB, f.implication(B, NA)));
        assertThat(not.literals().size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(A, NA, B, NB));
        assertThat(not.literals()).isEqualTo(lits);
    }

    @Test
    public void testToString() {
        assertThat(NOT1.toString()).isEqualTo("~(a & b)");
        assertThat(NOT2.toString()).isEqualTo("~(x | y)");
    }

    @Test
    public void testEquals() {
        assertThat(f.not(AND1)).isEqualTo(NOT1);
        assertThat(f.not(OR1)).isEqualTo(NOT2);
        assertThat(NOT1).isEqualTo(NOT1);
        assertThat(NOT2).isNotEqualTo(NOT1);
        assertThat(NOT2).isNotEqualTo("String");
    }

    @Test
    public void testEqualsDifferentFormulaFactory() {
        final FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(g.not(AND1)).isEqualTo(NOT1);
        assertThat(g.not(g.or(g.variable("x"), g.variable("y")))).isEqualTo(NOT2);
        assertThat(g.not(g.or(g.variable("a"), g.variable("b")))).isNotEqualTo(NOT2);
    }

    @Test
    public void testHash() {
        final Formula not = f.not(AND1);
        assertThat(not.hashCode()).isEqualTo(NOT1.hashCode());
        assertThat(not.hashCode()).isEqualTo(NOT1.hashCode());
        assertThat(f.not(OR1).hashCode()).isEqualTo(NOT2.hashCode());
    }

    @Test
    public void testNumberOfAtoms() {
        assertThat(NOT1.numberOfAtoms()).isEqualTo(2);
        assertThat(NOT1.numberOfAtoms()).isEqualTo(2);
        assertThat(NOT2.numberOfAtoms()).isEqualTo(2);
        assertThat(OR1.numberOfAtoms()).isEqualTo(2);
        assertThat(OR1.numberOfAtoms()).isEqualTo(2);
    }

    @Test
    public void testNumberOfNodes() {
        assertThat(NOT1.numberOfNodes()).isEqualTo(4);
        assertThat(NOT2.numberOfNodes()).isEqualTo(4);
        assertThat(NOT2.numberOfNodes()).isEqualTo(4);
    }

    @Test
    public void testNumberOfInternalNodes() throws ParserException {
        final Formula eq = new PropositionalParser(f).parse("a & (b | c) <=> ~(d => (b | c))");
        assertThat(NOT1.numberOfInternalNodes()).isEqualTo(4);
        assertThat(eq.numberOfInternalNodes()).isEqualTo(9);
    }

    @Test
    public void testNumberOfOperands() {
        assertThat(NOT1.numberOfOperands()).isEqualTo(1);
        assertThat(f.not(EQ1).numberOfOperands()).isEqualTo(1);
    }

    @Test
    public void testIsConstantFormula() {
        assertThat(NOT1.isConstantFormula()).isFalse();
        assertThat(NOT2.isConstantFormula()).isFalse();
    }

    @Test
    public void testAtomicFormula() {
        assertThat(NOT1.isAtomicFormula()).isFalse();
        assertThat(NOT2.isAtomicFormula()).isFalse();
    }

    @Test
    public void testContains() {
        assertThat(NOT1.containsVariable(f.variable("a"))).isTrue();
        assertThat(NOT1.containsVariable(f.variable("x"))).isFalse();
    }

    @Test
    public void testIsNNF() {
        assertThat(NOT1.isNNF()).isFalse();
        assertThat(NOT2.isNNF()).isFalse();
    }

    @Test
    public void testIsDNF() {
        assertThat(NOT1.isDNF()).isFalse();
        assertThat(NOT2.isDNF()).isFalse();
    }

    @Test
    public void testIsCNF() {
        assertThat(NOT1.isCNF()).isFalse();
        assertThat(NOT2.isCNF()).isFalse();
    }
}
