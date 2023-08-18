// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.datastructures.Assignment;
import org.logicng.datastructures.Tristate;
import org.logicng.io.parsers.ParserException;
import org.logicng.predicates.NNFPredicate;
import org.logicng.predicates.satisfiability.ContingencyPredicate;
import org.logicng.predicates.satisfiability.ContradictionPredicate;
import org.logicng.predicates.satisfiability.SATPredicate;
import org.logicng.predicates.satisfiability.TautologyPredicate;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;
import org.logicng.transformations.dnf.DNFFactorization;

import java.util.List;

public class FormulaFactoryWithoutContradictionCheckTest {

    private final FormulaFactoryConfig config = FormulaFactoryConfig.builder().simplifyComplementaryOperands(false).build();
    private final FormulaFactory f = FormulaFactory.caching(config);
    private final Variable a = f.variable("A");
    private final Literal notA = f.literal("A", false);
    private final Formula tautology = f.or(a, f.literal("A", false));
    private final Formula contradiction = f.and(a, f.literal("A", false));

    @Test
    public void testSimpleFormulas() throws ParserException {
        assertThat(f.parse("$true").toString()).isEqualTo("$true");
        assertThat(f.parse("$false").toString()).isEqualTo("$false");
        assertThat(f.parse("A").toString()).isEqualTo("A");
        assertThat(f.parse("~A").toString()).isEqualTo("~A");
        assertThat(f.parse("A & A & B").toString()).isEqualTo("A & B");
        assertThat(f.parse("A | A | B").toString()).isEqualTo("A | B");
        assertThat(f.parse("A => A & B").toString()).isEqualTo("A => A & B");
        assertThat(f.parse("A <=> A & B").toString()).isEqualTo("A <=> A & B");
    }

    @Test
    public void testContradictions() throws ParserException {
        assertThat(f.parse("A & ~A").toString()).isEqualTo("A & ~A");
        assertThat(f.parse("~A & A").toString()).isEqualTo("A & ~A");
        assertThat(f.parse("~A & A & A & ~A & A & A & ~A").toString()).isEqualTo("A & ~A");
        assertThat(f.parse("(A | B) & ~(A | B)").toString()).isEqualTo("(A | B) & ~(A | B)");
        assertThat(f.parse("(A | B) & ~(B | A)").toString()).isEqualTo("(A | B) & ~(A | B)");
    }

    @Test
    public void testTautologies() throws ParserException {
        assertThat(f.parse("A | ~A").toString()).isEqualTo("A | ~A");
        assertThat(f.parse("~A | A").toString()).isEqualTo("A | ~A");
        assertThat(f.parse("~A | A | A | ~A | A | A | ~A").toString()).isEqualTo("A | ~A");
        assertThat(f.parse("(A & B) | ~(A & B)").toString()).isEqualTo("A & B | ~(A & B)");
        assertThat(f.parse("(A & B) | ~(B & A)").toString()).isEqualTo("A & B | ~(A & B)");
    }

    @Test
    public void testFormulaProperties() {
        assertThat(tautology.isConstantFormula()).isFalse();
        assertThat(contradiction.isConstantFormula()).isFalse();
        assertThat(tautology.isAtomicFormula()).isFalse();
        assertThat(contradiction.isAtomicFormula()).isFalse();
        assertThat(tautology.numberOfAtoms()).isEqualTo(2);
        assertThat(contradiction.numberOfAtoms()).isEqualTo(2);
        assertThat(tautology.numberOfNodes()).isEqualTo(3);
        assertThat(contradiction.numberOfNodes()).isEqualTo(3);
        assertThat(tautology.type()).isEqualTo(FType.OR);
        assertThat(contradiction.type()).isEqualTo(FType.AND);
        assertThat(tautology.variables()).containsExactly(a);
        assertThat(contradiction.variables()).containsExactly(a);
        assertThat(tautology.literals()).containsExactlyInAnyOrder(a, notA);
        assertThat(contradiction.literals()).containsExactlyInAnyOrder(a, notA);
        assertThat(tautology.containsNode(a)).isTrue();
        assertThat(contradiction.containsNode(a)).isTrue();
        assertThat(tautology.containsNode(notA)).isTrue();
        assertThat(contradiction.containsNode(notA)).isTrue();
        assertThat(tautology.containsNode(tautology)).isTrue();
        assertThat(contradiction.containsNode(tautology)).isFalse();
        assertThat(tautology.containsNode(contradiction)).isFalse();
        assertThat(contradiction.containsNode(contradiction)).isTrue();
    }

    @Test
    public void testEval() {
        assertThat(tautology.evaluate(new Assignment())).isTrue();
        assertThat(tautology.evaluate(new Assignment(a))).isTrue();
        assertThat(tautology.evaluate(new Assignment(notA))).isTrue();
        assertThat(contradiction.evaluate(new Assignment())).isFalse();
        assertThat(contradiction.evaluate(new Assignment(a))).isFalse();
        assertThat(contradiction.evaluate(new Assignment(notA))).isFalse();
    }

    @Test
    public void testRestrict() {
        assertThat(tautology.restrict(new Assignment())).isEqualTo(tautology);
        assertThat(tautology.restrict(new Assignment(a))).isEqualTo(f.verum());
        assertThat(tautology.restrict(new Assignment(notA))).isEqualTo(f.verum());
        assertThat(contradiction.restrict(new Assignment())).isEqualTo(contradiction);
        assertThat(contradiction.restrict(new Assignment(a))).isEqualTo(f.falsum());
        assertThat(contradiction.restrict(new Assignment(notA))).isEqualTo(f.falsum());
    }

    @Test
    public void testNormalforms() {
        assertThat(tautology.nnf()).isEqualTo(tautology);
        assertThat(contradiction.nnf()).isEqualTo(contradiction);
        assertThat(tautology.cnf()).isEqualTo(tautology);
        assertThat(contradiction.cnf()).isEqualTo(contradiction);
        assertThat(tautology.transform(new DNFFactorization(f))).isEqualTo(tautology);
        assertThat(contradiction.transform(new DNFFactorization(f))).isEqualTo(contradiction);
    }

    @Test
    public void testPredicates() {
        assertThat(tautology.isCNF()).isTrue();
        assertThat(contradiction.isCNF()).isTrue();
        assertThat(tautology.holds(new NNFPredicate())).isTrue();
        assertThat(contradiction.holds(new NNFPredicate())).isTrue();
        assertThat(tautology.isDNF()).isTrue();
        assertThat(contradiction.isDNF()).isTrue();
        assertThat(tautology.holds(new SATPredicate(f))).isTrue();
        assertThat(contradiction.holds(new SATPredicate(f))).isFalse();
        assertThat(tautology.holds(new TautologyPredicate(f))).isTrue();
        assertThat(contradiction.holds(new TautologyPredicate(f))).isFalse();
        assertThat(tautology.holds(new ContradictionPredicate(f))).isFalse();
        assertThat(contradiction.holds(new ContradictionPredicate(f))).isTrue();
        assertThat(tautology.holds(new ContingencyPredicate(f))).isFalse();
        assertThat(contradiction.holds(new ContingencyPredicate(f))).isFalse();
    }

    @Test
    public void testSatSolverWithTautologies() throws ParserException {
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("A"));
        solver.add(f.parse("A => B"));
        solver.add(f.parse("C | ~C"));
        List<Assignment> models = solver.enumerateAllModels();
        assertThat(models).hasSize(2);
        models.forEach(m -> assertThat(m.literals()).containsAnyOf(f.literal("C", true), f.literal("C", false)));
        solver.add(f.parse("D | ~D"));
        models = solver.enumerateAllModels();
        assertThat(models).hasSize(4);
        models.forEach(m -> assertThat(m.literals()).containsAnyOf(f.literal("C", true), f.literal("C", false),
                f.literal("D", true), f.literal("D", false)));
    }

    @Test
    public void testSatSolverWithContradictions() throws ParserException {
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(f.parse("A"));
        solver.add(f.parse("A => B"));
        solver.add(f.parse("C | ~C"));
        final List<Assignment> models = solver.enumerateAllModels();
        assertThat(models).hasSize(2);
        models.forEach(m -> assertThat(m.literals()).containsAnyOf(f.literal("C", true), f.literal("C", false)));
        solver.add(f.parse("D & ~D"));
        assertThat(solver.sat()).isEqualTo(Tristate.FALSE);
    }

    @Test
    public void testSubsumption() throws ParserException {
        assertThat(tautology.substitute(a, notA)).isEqualTo(tautology);
        assertThat(contradiction.substitute(a, notA)).isEqualTo(contradiction);
        assertThat(tautology.substitute(a, f.variable("B"))).isEqualTo(f.parse("B | ~B"));
        assertThat(contradiction.substitute(a, f.variable("B"))).isEqualTo(f.parse("B & ~B"));
    }

    @Test
    public void testBdds() {
        assertThat(tautology.bdd().isTautology()).isTrue();
        assertThat(contradiction.bdd().isTautology()).isFalse();
        assertThat(tautology.bdd().isContradiction()).isFalse();
        assertThat(contradiction.bdd().isContradiction()).isTrue();
    }
}
