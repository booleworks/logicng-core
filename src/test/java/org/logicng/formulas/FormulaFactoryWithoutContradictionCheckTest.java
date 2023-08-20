// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.formulas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FormulaFactoryWithoutContradictionCheckTest {

    private static final FormulaFactoryConfig CONFIG = FormulaFactoryConfig.builder().simplifyComplementaryOperands(false).build();

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
        assertThat(_c.tautology.numberOfAtoms()).isEqualTo(2);
        assertThat(_c.contradiction.numberOfAtoms()).isEqualTo(2);
        assertThat(_c.tautology.numberOfNodes()).isEqualTo(3);
        assertThat(_c.contradiction.numberOfNodes()).isEqualTo(3);
        assertThat(_c.tautology.type()).isEqualTo(FType.OR);
        assertThat(_c.contradiction.type()).isEqualTo(FType.AND);
        assertThat(_c.tautology.variables()).containsExactly(_c.a);
        assertThat(_c.contradiction.variables()).containsExactly(_c.a);
        assertThat(_c.tautology.literals()).containsExactlyInAnyOrder(_c.a, _c.na);
        assertThat(_c.contradiction.literals()).containsExactlyInAnyOrder(_c.a, _c.na);
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
        assertThat(_c.tautology.restrict(new Assignment())).isEqualTo(_c.tautology);
        assertThat(_c.tautology.restrict(new Assignment(_c.a))).isEqualTo(_c.f.verum());
        assertThat(_c.tautology.restrict(new Assignment(_c.na))).isEqualTo(_c.f.verum());
        assertThat(_c.contradiction.restrict(new Assignment())).isEqualTo(_c.contradiction);
        assertThat(_c.contradiction.restrict(new Assignment(_c.a))).isEqualTo(_c.f.falsum());
        assertThat(_c.contradiction.restrict(new Assignment(_c.na))).isEqualTo(_c.f.falsum());
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testNormalforms(final FormulaContext _c) {
        assertThat(_c.tautology.nnf()).isEqualTo(_c.tautology);
        assertThat(_c.contradiction.nnf()).isEqualTo(_c.contradiction);
        assertThat(_c.tautology.cnf()).isEqualTo(_c.tautology);
        assertThat(_c.contradiction.cnf()).isEqualTo(_c.contradiction);
        assertThat(_c.tautology.transform(new DNFFactorization(_c.f))).isEqualTo(_c.tautology);
        assertThat(_c.contradiction.transform(new DNFFactorization(_c.f))).isEqualTo(_c.contradiction);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testPredicates(final FormulaContext _c) {
        assertThat(_c.tautology.isCNF()).isTrue();
        assertThat(_c.contradiction.isCNF()).isTrue();
        assertThat(_c.tautology.holds(new NNFPredicate(_c.f))).isTrue();
        assertThat(_c.contradiction.holds(new NNFPredicate(_c.f))).isTrue();
        assertThat(_c.tautology.isDNF()).isTrue();
        assertThat(_c.contradiction.isDNF()).isTrue();
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
        final SATSolver solver = MiniSat.miniSat(_c.f);
        solver.add(_c.f.parse("A"));
        solver.add(_c.f.parse("A => B"));
        solver.add(_c.f.parse("C | ~C"));
        List<Assignment> models = solver.enumerateAllModels();
        assertThat(models).hasSize(2);
        models.forEach(m -> assertThat(m.literals()).containsAnyOf(_c.f.literal("C", true), _c.f.literal("C", false)));
        solver.add(_c.f.parse("D | ~D"));
        models = solver.enumerateAllModels();
        assertThat(models).hasSize(4);
        models.forEach(m -> assertThat(m.literals()).containsAnyOf(_c.f.literal("C", true), _c.f.literal("C", false),
                _c.f.literal("D", true), _c.f.literal("D", false)));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSatSolverWithContradictions(final FormulaContext _c) throws ParserException {
        final SATSolver solver = MiniSat.miniSat(_c.f);
        solver.add(_c.f.parse("A"));
        solver.add(_c.f.parse("A => B"));
        solver.add(_c.f.parse("C | ~C"));
        final List<Assignment> models = solver.enumerateAllModels();
        assertThat(models).hasSize(2);
        models.forEach(m -> assertThat(m.literals()).containsAnyOf(_c.f.literal("C", true), _c.f.literal("C", false)));
        solver.add(_c.f.parse("D & ~D"));
        assertThat(solver.sat()).isEqualTo(Tristate.FALSE);
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSubsumption(final FormulaContext _c) throws ParserException {
        assertThat(_c.tautology.substitute(_c.a, _c.na)).isEqualTo(_c.tautology);
        assertThat(_c.contradiction.substitute(_c.a, _c.na)).isEqualTo(_c.contradiction);
        assertThat(_c.tautology.substitute(_c.a, _c.f.variable("B"))).isEqualTo(_c.f.parse("B | ~B"));
        assertThat(_c.contradiction.substitute(_c.a, _c.f.variable("B"))).isEqualTo(_c.f.parse("B & ~B"));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testBdds(final FormulaContext _c) {
        assertThat(_c.tautology.bdd().isTautology()).isTrue();
        assertThat(_c.contradiction.bdd().isTautology()).isFalse();
        assertThat(_c.tautology.bdd().isContradiction()).isFalse();
        assertThat(_c.contradiction.bdd().isContradiction()).isTrue();
    }
}
