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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class AndTest extends TestWithExampleFormulas {

    @Test
    public void testType() {
        assertThat(AND1.type()).isEqualTo(FType.AND);
    }

    @Test
    public void testCreator() {
        assertThat(f.and()).isEqualTo(TRUE);
        assertThat(f.and(TRUE)).isEqualTo(TRUE);
        assertThat(f.and(FALSE)).isEqualTo(FALSE);
        assertThat(f.and(TRUE, FALSE)).isEqualTo(FALSE);
        assertThat(f.and(FALSE, TRUE)).isEqualTo(FALSE);
        assertThat(f.and(NA)).isEqualTo(NA);
        assertThat(f.and(A, B, A, B, A)).isEqualTo(AND1);
        assertThat(f.and(f.and(A, B), A, f.and(B, A))).isEqualTo(AND1);
        assertThat(f.and(TRUE, A, B, TRUE)).isEqualTo(AND1);
        assertThat(f.and(NA, NA, NA)).isEqualTo(NA);
        assertThat(f.and(NA, NA, TRUE, TRUE)).isEqualTo(NA);
        assertThat(f.and(NA, NA, FALSE, TRUE)).isEqualTo(FALSE);
        final List<Literal> lits = new ArrayList<>();
        lits.add(A);
        lits.add(B);
        assertThat(f.and(lits)).isEqualTo(AND1);
        assertThat(f.and(A, B, X, FALSE)).isEqualTo(FALSE);
        assertThat(f.and(f.and(A, B), f.and(X, Y))).isEqualTo(f.and(A, B, X, Y));
        assertThat(f.cnf(f.clause(X, Y), f.and(f.or(f.and(NX, NX), NY), f.or(f.and(NX, TRUE), NY)))).isEqualTo(AND3);
        assertThat(f.naryOperator(FType.AND, A, B, A, B, A)).isEqualTo(AND1);
        assertThat(f.naryOperator(FType.AND, Arrays.asList(A, B, A, B, A))).isEqualTo(AND1);
    }

    @Test
    public void testComplementaryCheck() {
        assertThat(f.and(A, NA)).isEqualTo(FALSE);
        assertThat(f.and(A, B, f.and(C, X, NB))).isEqualTo(FALSE);
        assertThat(f.and(A, B, f.and(NX, B, X))).isEqualTo(FALSE);
    }

    @Test
    public void testIllegalCreation() {
        assertThatThrownBy(() -> f.naryOperator(FType.EQUIV, A, B, C)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testVariables() {
        assertThat(AND2.variables().size()).isEqualTo(2);
        SortedSet<Variable> lits = new TreeSet<>(Arrays.asList(A, B));
        assertThat(AND2.variables()).isEqualTo(lits);

        final Formula and = f.and(A, A, B, IMP3);
        assertThat(and.variables().size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(A, B, X, Y));
        assertThat(and.variables()).isEqualTo(lits);
    }

    @Test
    public void testLiterals() {
        assertThat(AND2.literals().size()).isEqualTo(2);
        SortedSet<Literal> lits = new TreeSet<>(Arrays.asList(NA, NB));
        assertThat(AND2.literals()).isEqualTo(lits);

        final Formula and = f.and(A, A, B, f.implication(NA, NB));
        assertThat(and.literals().size()).isEqualTo(4);
        lits = new TreeSet<>(Arrays.asList(A, NA, B, NB));
        assertThat(and.literals()).isEqualTo(lits);
    }

    @Test
    public void testToString() {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(AND1.toString()).isEqualTo("a & b");
        assertThat(AND2.toString()).isEqualTo("~a & ~b");
        assertThat(AND3.toString()).isEqualTo("(x | y) & (~x | ~y)");
        assertThat(f.and(A, B, NX, NY).toString()).isEqualTo("a & b & ~x & ~y");
        assertThat(f.and(IMP1, IMP2).toString()).isEqualTo("(a => b) & (~a => ~b)");
        assertThat(f.and(EQ1, EQ2).toString()).isEqualTo("(a <=> b) & (~a <=> ~b)");
    }

    @Test
    public void testEquals() {
        assertThat(f.and(A, B)).isEqualTo(AND1);
        assertThat(f.and(OR1, OR2)).isEqualTo(AND3);
        assertThat(AND2).isEqualTo(AND2);
        assertThat(f.and(f.or(f.literal("y", false), f.variable("x")), f.or(f.variable("b"), f.variable("a"))))
                .isEqualTo(f.and(f.or(f.variable("a"), f.variable("b")), f.or(f.variable("x"), f.literal("y", false))));
        assertThat(f.and(NX, A, NB, OR1)).isEqualTo(f.and(A, NB, OR1, NX));
        assertThat(AND2).isNotEqualTo(AND1);
        assertThat(f.and(A, B, C)).isNotEqualTo(AND1);
    }

    @Test
    public void testEqualsDifferentFormulaFactory() {
        final FormulaFactory f = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        final FormulaFactory g = FormulaFactory.caching(FormulaFactoryConfig.builder().formulaMergeStrategy(FormulaFactoryConfig.FormulaMergeStrategy.IMPORT).build());
        assertThat(g.and(g.variable("a"), g.variable("b"))).isEqualTo(AND1);
        assertThat(g.and(OR1, OR2)).isEqualTo(AND3);
        assertThat(g.and(g.or(g.literal("y", false), g.variable("x")), f.or(g.variable("b"), g.variable("a"))))
                .isEqualTo(f.and(f.or(f.variable("a"), f.variable("b")), f.or(f.variable("x"), f.literal("y", false))));
        assertThat(g.and(g.literal("x", false), g.variable("a"), g.literal("b", false), g.or(g.variable("x"), g.variable("y"))))
                .isEqualTo(f.and(A, NB, OR1, NX));
        assertThat(g.and(g.literal("a", false), g.variable("b"))).isNotEqualTo(AND1);
        assertThat(g.and(g.variable("a"), g.literal("b", false))).isNotEqualTo(AND1);
        assertThat(f.and(A, B, g.variable("c"))).isNotEqualTo(AND1);
    }

    @Test
    public void testHash() {
        final Formula and = f.and(OR1, OR2);
        assertThat(and.hashCode()).isEqualTo(AND3.hashCode());
        assertThat(and.hashCode()).isEqualTo(AND3.hashCode());
        assertThat(f.and(NA, NB).hashCode()).isEqualTo(AND2.hashCode());
    }

    @Test
    public void testNumberOfAtoms() {
        assertThat(AND1.numberOfAtoms()).isEqualTo(2);
        assertThat(AND2.numberOfAtoms()).isEqualTo(2);
        assertThat(AND3.numberOfAtoms()).isEqualTo(4);
        assertThat(AND3.numberOfAtoms()).isEqualTo(4);
    }

    @Test
    public void testNumberOfNodes() {
        assertThat(AND1.numberOfNodes()).isEqualTo(3);
        assertThat(AND2.numberOfNodes()).isEqualTo(3);
        assertThat(AND3.numberOfNodes()).isEqualTo(7);
        assertThat(AND3.numberOfNodes()).isEqualTo(7);
    }

    @Test
    public void testNumberOfInternalNodes() throws ParserException {
        final Formula and = new PropositionalParser(f).parse("a & (b | c) => (d <=> (b | c))");
        assertThat(AND3.numberOfInternalNodes()).isEqualTo(7);
        assertThat(and.numberOfInternalNodes()).isEqualTo(8);
    }

    @Test
    public void testNumberOfOperands() {
        assertThat(AND1.numberOfOperands()).isEqualTo(2);
        assertThat(AND3.numberOfOperands()).isEqualTo(2);
        assertThat(f.and(A, NX, EQ1).numberOfOperands()).isEqualTo(3);
    }

    @Test
    public void testIsConstantFormula() {
        assertThat(AND1.isConstantFormula()).isFalse();
        assertThat(AND2.isConstantFormula()).isFalse();
    }

    @Test
    public void testAtomicFormula() {
        assertThat(AND1.isAtomicFormula()).isFalse();
    }

    @Test
    public void testContains() throws ParserException {
        assertThat(AND3.containsVariable(f.variable("x"))).isTrue();
        assertThat(AND3.containsVariable(f.variable("a"))).isFalse();
        final PropositionalParser parser = new PropositionalParser(f);
        final Formula contAnd = parser.parse("a & b & (c | (d & e))");
        assertThat(contAnd.containsNode(parser.parse("d & e"))).isTrue();
    }

    @Test
    public void testIsNNF() {
        assertThat(AND1.isNNF()).isTrue();
        assertThat(AND2.isNNF()).isTrue();
        assertThat(AND3.isNNF()).isTrue();
    }

    @Test
    public void testIsDNF() {
        assertThat(AND1.isDNF()).isTrue();
        assertThat(AND2.isDNF()).isTrue();
        assertThat(AND3.isDNF()).isFalse();
    }

    @Test
    public void testIsCNF() {
        assertThat(AND1.isCNF()).isTrue();
        assertThat(AND2.isCNF()).isTrue();
        assertThat(AND3.isCNF()).isTrue();
    }
}
