// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.bdds;

import static com.booleworks.logicng.TestWithExampleFormulas.parse;
import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.knowledgecompilation.bdds.orderings.BfsOrdering;
import com.booleworks.logicng.knowledgecompilation.bdds.orderings.DfsOrdering;
import com.booleworks.logicng.knowledgecompilation.bdds.orderings.MaxToMinOrdering;
import com.booleworks.logicng.knowledgecompilation.bdds.orderings.MinToMaxOrdering;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import org.junit.jupiter.api.Test;

public class FormulaBddTest {

    @Test
    public void testSimpleCases() {
        final FormulaFactory f = FormulaFactory.caching();
        Bdd bdd = f.verum().bdd(f);
        assertThat(bdd.isTautology()).isTrue();
        bdd = f.falsum().bdd(f);
        assertThat(bdd.isContradiction()).isTrue();
        bdd = f.variable("A").bdd(f);
        assertThat(bdd.enumerateAllModels()).containsExactly(new Model(f.variable("A")));
    }

    @Test
    public void testBDDGeneration() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final Formula formula =
                p.parse("(A => ~B) & ((A & C) | (D & ~C)) & (A | Y | X) & (Y <=> (X | (W + A + F < 1)))");
        final Bdd bddNoOrder = formula.bdd(f);
        final Bdd bddBfs = formula.bdd(f, new BfsOrdering());
        final Bdd bddDfs = formula.bdd(f, new DfsOrdering());
        final Bdd bddMin2Max = formula.bdd(f, new MinToMaxOrdering());
        final Bdd bddMax2Min = formula.bdd(f, new MaxToMinOrdering());

        assertThat(bddNoOrder.nodeCount()).isEqualTo(13);
        assertThat(bddBfs.nodeCount()).isEqualTo(14);
        assertThat(bddDfs.nodeCount()).isEqualTo(13);
        assertThat(bddMin2Max.nodeCount()).isEqualTo(14);
        assertThat(bddMax2Min.nodeCount()).isEqualTo(22);

        final TautologyPredicate tautology = new TautologyPredicate(f);
        assertThat(f.equivalence(bddNoOrder.cnf(), formula).holds(tautology)).isTrue();
        assertThat(f.equivalence(bddBfs.cnf(), formula).holds(tautology)).isTrue();
        assertThat(f.equivalence(bddDfs.cnf(), formula).holds(tautology)).isTrue();
        assertThat(f.equivalence(bddMin2Max.cnf(), formula).holds(tautology)).isTrue();
        assertThat(f.equivalence(bddMax2Min.cnf(), formula).holds(tautology)).isTrue();

        assertThat(f.equivalence(bddNoOrder.dnf(), formula).holds(tautology)).isTrue();
        assertThat(f.equivalence(bddBfs.dnf(), formula).holds(tautology)).isTrue();
        assertThat(f.equivalence(bddDfs.dnf(), formula).holds(tautology)).isTrue();
        assertThat(f.equivalence(bddMin2Max.dnf(), formula).holds(tautology)).isTrue();
        assertThat(f.equivalence(bddMax2Min.dnf(), formula).holds(tautology)).isTrue();
    }

    @Test
    public void testNonNnfs() {
        final FormulaFactory f = FormulaFactory.caching();
        assertThat(parse(f, "A + 2*B - C = 1").bdd(f)).isNotNull();
        assertThat(parse(f, "(A & B & C | D & E & F) & (A - 2*B -D <= 0) | (C + 3*D - F > 0)").bdd(f))
                .isNotNull();
    }
}
