// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.predicates.NNFPredicate;
import com.booleworks.logicng.predicates.satisfiability.ContingencyPredicate;
import com.booleworks.logicng.predicates.satisfiability.ContradictionPredicate;
import com.booleworks.logicng.predicates.satisfiability.SATPredicate;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import com.booleworks.logicng.solvers.SATSolver;
import com.booleworks.logicng.transformations.dnf.DNFFactorization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FormulaFactoryWithoutContradictionCheckTest {

    private static final FormulaFactoryConfig CONFIG =
            FormulaFactoryConfig.builder().simplifyComplementaryOperands(false).build();

    public static Collection<Object[]> contexts() {
        final List<Object[]> contexts = new ArrayList<>();
        contexts.add(new Object[]{new FormulaContext(FormulaFactory.caching(CONFIG))});
        contexts.add(new Object[]{new FormulaContext(FormulaFactory.nonCaching(CONFIG))});
        return contexts;
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimpleFormulas(final FormulaContext _c) throws ParserException {
        assertThat(_c.f.parse("$true").toString()).isEqualTo("$true");
        assertThat(_c.f.parse("$false").toString()).isEqualTo("$false");
        assertThat(_c.f.parse("a").toString()).isEqualTo("a");
        assertThat(_c.f.parse("~a").toString()).isEqualTo("~a");
        assertThat(_c.f.parse("a & a & b").toString()).isEqualTo("a & b");
        assertThat(_c.f.parse("a | a | b").toString()).isEqualTo("a | b");
        assertThat(_c.f.parse("a => a & b").toString()).isEqualTo("a => a & b");
        assertThat(_c.f.parse("a <=> a & b").toString()).isEqualTo("a <=> a & b");
    }

    @Test
    public void testContradictionsCaching() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching(CONFIG);
        assertThat(f.parse("a & ~a").toString()).isEqualTo("a & ~a");
        assertThat(f.parse("~a & a").toString()).isEqualTo("a & ~a");
        assertThat(f.parse("~a & a & a & ~a & a & a & ~a").toString()).isEqualTo("a & ~a");
        assertThat(f.parse("(a | b) & ~(a | b)").toString()).isEqualTo("(a | b) & ~(a | b)");
        assertThat(f.parse("(a | b) & ~(b | a)").toString()).isEqualTo("(a | b) & ~(a | b)");
    }

    @Test
    public void testContradictionsNonCaching() throws ParserException {
        final FormulaFactory f = FormulaFactory.nonCaching(CONFIG);
        assertThat(f.parse("a & ~a").toString()).isEqualTo("a & ~a");
        assertThat(f.parse("~a & a").toString()).isEqualTo("~a & a");
        assertThat(f.parse("~a & a & a & ~a & a & a & ~a").toString()).isEqualTo("~a & a");
        assertThat(f.parse("(a | b) & ~(a | b)").toString()).isEqualTo("(a | b) & ~(a | b)");
        assertThat(f.parse("(a | b) & ~(b | a)").toString()).isEqualTo("(a | b) & ~(b | a)");
    }

    @Test
    public void testTautologiesCaching() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching(CONFIG);
        assertThat(f.parse("a | ~a").toString()).isEqualTo("a | ~a");
        assertThat(f.parse("~a | a").toString()).isEqualTo("a | ~a");
        assertThat(f.parse("~a | a | a | ~a | a | a | ~a").toString()).isEqualTo("a | ~a");
        assertThat(f.parse("(a & b) | ~(a & b)").toString()).isEqualTo("a & b | ~(a & b)");
        assertThat(f.parse("(a & b) | ~(b & a)").toString()).isEqualTo("a & b | ~(a & b)");
    }

    @Test
    public void testTautologiesNonCaching() throws ParserException {
        final FormulaFactory f = FormulaFactory.nonCaching(CONFIG);
        assertThat(f.parse("a | ~a").toString()).isEqualTo("a | ~a");
        assertThat(f.parse("~a | a").toString()).isEqualTo("~a | a");
        assertThat(f.parse("~a | a | a | ~a | a | a | ~a").toString()).isEqualTo("~a | a");
        assertThat(f.parse("(a & b) | ~(a & b)").toString()).isEqualTo("a & b | ~(a & b)");
        assertThat(f.parse("(a & b) | ~(b & a)").toString()).isEqualTo("a & b | ~(b & a)");
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testFormulaProperties(final FormulaContext _c) {
        assertThat(_c.tautology.isConstantFormula()).isFalse();
        assertThat(_c.contradiction.isConstantFormula()).isFalse();
        assertThat(_c.tautology.isAtomicFormula()).isFalse();
        assertThat(_c.contradiction.isAtomicFormula()).isFalse();
        assertThat(_c.tautology.numberOfAtoms(_c.f)).isEqualTo(2);
        assertThat(_c.contradiction.numberOfAtoms(_c.f)).isEqualTo(2);
        assertThat(_c.tautology.numberOfNodes(_c.f)).isEqualTo(3);
        assertThat(_c.contradiction.numberOfNodes(_c.f)).isEqualTo(3);
        assertThat(_c.tautology.getType()).isEqualTo(FType.OR);
        assertThat(_c.contradiction.getType()).isEqualTo(FType.AND);
        assertThat(_c.tautology.variables(_c.f)).containsExactly(_c.a);
        assertThat(_c.contradiction.variables(_c.f)).containsExactly(_c.a);
        assertThat(_c.tautology.literals(_c.f)).containsExactlyInAnyOrder(_c.a, _c.na);
        assertThat(_c.contradiction.literals(_c.f)).containsExactlyInAnyOrder(_c.a, _c.na);
        assertThat(_c.tautology.containsNode(_c.a)).isTrue();
        assertThat(_c.contradiction.containsNode(_c.a)).isTrue();
        assertThat(_c.tautology.containsNode(_c.na)).isTrue();
        assertThat(_c.contradiction.containsNode(_c.na)).isTrue();
        assertThat(_c.tautology.containsNode(_c.tautology)).isTrue();
        assertThat(_c.contradiction.containsNode(_c.tautology)).isFalse();
        assertThat(_c.tautology.containsNode(_c.contradiction)).isFalse();
        assertThat(_c.contradiction.containsNode(_c.contradiction)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testEval(final FormulaContext _c) {
        assertThat(_c.tautology.evaluate(new Assignment())).isTrue();
        assertThat(_c.tautology.evaluate(new Assignment(_c.a))).isTrue();
        assertThat(_c.tautology.evaluate(new Assignment(_c.na))).isTrue();
        assertThat(_c.contradiction.evaluate(new Assignment())).isFalse();
        assertThat(_c.contradiction.evaluate(new Assignment(_c.a))).isFalse();
        assertThat(_c.contradiction.evaluate(new Assignment(_c.na))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testRestrict(final FormulaContext _c) {
        assertThat(_c.tautology.restrict(_c.f, new Assignment())).isEqualTo(_c.tautology);
        assertThat(_c.tautology.restrict(_c.f, new Assignment(_c.a))).isEqualTo(_c.f.verum());
        assertThat(_c.tautology.restrict(_c.f, new Assignment(_c.na))).isEqualTo(_c.f.verum());
        assertThat(_c.contradiction.restrict(_c.f, new Assignment())).isEqualTo(_c.contradiction);
        assertThat(_c.contradiction.restrict(_c.f, new Assignment(_c.a))).isEqualTo(_c.f.falsum());
        assertThat(_c.contradiction.restrict(_c.f, new Assignment(_c.na))).isEqualTo(_c.f.falsum());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNormalforms(final FormulaContext _c) {
        assertThat(_c.tautology.nnf(_c.f)).isEqualTo(_c.tautology);
        assertThat(_c.contradiction.nnf(_c.f)).isEqualTo(_c.contradiction);
        assertThat(_c.tautology.cnf(_c.f)).isEqualTo(_c.tautology);
        assertThat(_c.contradiction.cnf(_c.f)).isEqualTo(_c.contradiction);
        assertThat(_c.tautology.transform(new DNFFactorization(_c.f))).isEqualTo(_c.tautology);
        assertThat(_c.contradiction.transform(new DNFFactorization(_c.f))).isEqualTo(_c.contradiction);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPredicates(final FormulaContext _c) {
        assertThat(_c.tautology.isCNF(_c.f)).isTrue();
        assertThat(_c.contradiction.isCNF(_c.f)).isTrue();
        assertThat(_c.tautology.holds(new NNFPredicate(_c.f))).isTrue();
        assertThat(_c.contradiction.holds(new NNFPredicate(_c.f))).isTrue();
        assertThat(_c.tautology.isDNF(_c.f)).isTrue();
        assertThat(_c.contradiction.isDNF(_c.f)).isTrue();
        assertThat(_c.tautology.holds(new SATPredicate(_c.f))).isTrue();
        assertThat(_c.contradiction.holds(new SATPredicate(_c.f))).isFalse();
        assertThat(_c.tautology.holds(new TautologyPredicate(_c.f))).isTrue();
        assertThat(_c.contradiction.holds(new TautologyPredicate(_c.f))).isFalse();
        assertThat(_c.tautology.holds(new ContradictionPredicate(_c.f))).isFalse();
        assertThat(_c.contradiction.holds(new ContradictionPredicate(_c.f))).isTrue();
        assertThat(_c.tautology.holds(new ContingencyPredicate(_c.f))).isFalse();
        assertThat(_c.contradiction.holds(new ContingencyPredicate(_c.f))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSatSolverWithTautologies(final FormulaContext _c) throws ParserException {
        final SATSolver solver = SATSolver.newSolver(_c.f);
        solver.add(_c.f.parse("A"));
        solver.add(_c.f.parse("A => B"));
        solver.add(_c.f.parse("C | ~C"));
        List<Model> models = solver.enumerateAllModels(_c.f.variables("A", "B", "C"));
        assertThat(models).hasSize(2);
        models.forEach(m -> assertThat(m.getLiterals()).containsAnyOf(_c.f.literal("C", true),
                _c.f.literal("C", false)));
        solver.add(_c.f.parse("D | ~D"));
        models = solver.enumerateAllModels(_c.f.variables("A", "B", "C", "D"));
        assertThat(models).hasSize(4);
        models.forEach(m -> assertThat(m.getLiterals()).containsAnyOf(_c.f.literal("C", true),
                _c.f.literal("C", false),
                _c.f.literal("D", true), _c.f.literal("D", false)));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSatSolverWithContradictions(final FormulaContext _c) throws ParserException {
        final SATSolver solver = SATSolver.newSolver(_c.f);
        solver.add(_c.f.parse("A"));
        solver.add(_c.f.parse("A => B"));
        solver.add(_c.f.parse("C | ~C"));
        final List<Model> models = solver.enumerateAllModels(_c.f.variables("A", "B", "C"));
        assertThat(models).hasSize(2);
        models.forEach(m -> assertThat(m.getLiterals()).containsAnyOf(_c.f.literal("C", true),
                _c.f.literal("C", false)));
        solver.add(_c.f.parse("D & ~D"));
        assertThat(solver.sat()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSubstitution(final FormulaContext _c) throws ParserException {
        assertThat(_c.tautology.substitute(_c.f, _c.a, _c.na)).isEqualTo(_c.tautology);
        assertThat(_c.contradiction.substitute(_c.f, _c.a, _c.na)).isEqualTo(_c.contradiction);
        assertThat(_c.tautology.substitute(_c.f, _c.a, _c.f.variable("B"))).isEqualTo(_c.f.parse("B | ~B"));
        assertThat(_c.contradiction.substitute(_c.f, _c.a, _c.f.variable("B"))).isEqualTo(_c.f.parse("B & ~B"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBdds(final FormulaContext _c) {
        assertThat(_c.tautology.bdd(_c.f).isTautology()).isTrue();
        assertThat(_c.contradiction.bdd(_c.f).isTautology()).isFalse();
        assertThat(_c.tautology.bdd(_c.f).isContradiction()).isFalse();
        assertThat(_c.contradiction.bdd(_c.f).isContradiction()).isTrue();
    }
}
