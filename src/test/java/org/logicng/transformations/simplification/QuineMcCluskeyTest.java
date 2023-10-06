// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.simplification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaContext;
import org.logicng.formulas.TestWithFormulaContext;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.readers.FormulaReader;
import org.logicng.predicates.satisfiability.TautologyPredicate;
import org.logicng.solvers.MiniSat;
import org.logicng.solvers.SATSolver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuineMcCluskeyTest extends TestWithFormulaContext {

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimple1(final FormulaContext _c) throws ParserException {
        final Formula formula = _c.p.parse("(~a & ~b & ~c) | (~a & ~b & c) | (~a & b & ~c) | (a & ~b & c) | (a & b & ~c) | (a & b & c)");
        final Formula dnf = formula.transform(new QuineMcCluskeySimplifier(_c.f));
        assertThat(dnf.isDNF(_c.f)).isTrue();
        assertThat(_c.f.equivalence(formula, dnf).holds(new TautologyPredicate(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimple2(final FormulaContext _c) throws ParserException {
        final Formula formula = _c.p.parse("(~a & ~b & ~c) | (~a & b & ~c) | (a & ~b & c) | (a & b & c)");
        final Formula dnf = formula.transform(new QuineMcCluskeySimplifier(_c.f));
        assertThat(dnf.isDNF(_c.f)).isTrue();
        assertThat(_c.f.equivalence(formula, dnf).holds(new TautologyPredicate(_c.f))).isTrue();
    }

    /**
     * Example from <a href="https://github.com/logic-ng/LogicNG/issues/15">issue 15</a>.
     * Ensure only original formula variables are returned, i.e., no auxiliary variables are returned.
     * @throws ParserException if any malformed formula is encountered
     */
    @ParameterizedTest
    @MethodSource("contexts")
    public void testSimple3(final FormulaContext _c) throws ParserException {
        final Formula formula = _c.p.parse("~5 & ~4 & 3 & 2 & 1 | ~3 & ~7 & ~2 & 1 | ~6 & 1 & ~3 & 2 | ~9 & 6 & 8 & ~1 | 3 & 4 & 2 & 1 | ~2 & 7 & 1 | ~10 & ~8 & ~1");
        final Formula dnf = formula.transform(new QuineMcCluskeySimplifier(_c.f));
        assertThat(dnf.isDNF(_c.f)).isTrue();
        assertThat(_c.f.equivalence(formula, dnf).holds(new TautologyPredicate(_c.f))).isTrue();
        assertThat(formula.variables(_c.f)).containsAll(dnf.variables(_c.f));
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLarge1(final FormulaContext _c) throws ParserException {
        final Formula formula = _c.p.parse("A => B & ~((D | E | I | J) & ~K) & L");
        final Formula dnf = formula.transform(new QuineMcCluskeySimplifier(_c.f));
        assertThat(dnf.isDNF(_c.f)).isTrue();
        assertThat(_c.f.equivalence(formula, dnf).holds(new TautologyPredicate(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLarge2(final FormulaContext _c) throws ParserException, IOException {
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/large_formula.txt", _c.f);
        final SATSolver solver = MiniSat.miniSat(_c.f);
        solver.add(formula);
        final List<Assignment> models = solver.enumerateAllModels(Arrays.asList(
                _c.f.variable("v111"),
                _c.f.variable("v410"),
                _c.f.variable("v434"),
                _c.f.variable("v35"),
                _c.f.variable("v36"),
                _c.f.variable("v78"),
                _c.f.variable("v125"),
                _c.f.variable("v125"),
                _c.f.variable("v58"),
                _c.f.variable("v61")));
        final List<Formula> operands = new ArrayList<>(models.size());
        for (final Assignment model : models) {
            operands.add(model.formula(_c.f));
        }
        final Formula canonicalDNF = _c.f.or(operands);
        final Formula dnf = canonicalDNF.transform(new QuineMcCluskeySimplifier(_c.f));
        assertThat(dnf.isDNF(_c.f)).isTrue();
        assertThat(_c.f.equivalence(canonicalDNF, dnf).holds(new TautologyPredicate(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testLarge3(final FormulaContext _c) throws ParserException, IOException {
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/large_formula.txt", _c.f);
        final SATSolver solver = MiniSat.miniSat(_c.f);
        solver.add(formula);
        final List<Assignment> models = solver.enumerateAllModels(Arrays.asList(
                _c.f.variable("v111"),
                _c.f.variable("v410"),
                _c.f.variable("v434"),
                _c.f.variable("v35"),
                _c.f.variable("v36"),
                _c.f.variable("v78"),
                _c.f.variable("v125"),
                _c.f.variable("v125"),
                _c.f.variable("v58"),
                _c.f.variable("v27"),
                _c.f.variable("v462"),
                _c.f.variable("v463"),
                _c.f.variable("v280"),
                _c.f.variable("v61")));
        final List<Formula> operands = new ArrayList<>(models.size());
        for (final Assignment model : models) {
            operands.add(model.formula(_c.f));
        }
        final Formula canonicalDNF = _c.f.or(operands);
        final Formula dnf = canonicalDNF.transform(new QuineMcCluskeySimplifier(_c.f));
        assertThat(dnf.isDNF(_c.f)).isTrue();
        assertThat(_c.f.equivalence(canonicalDNF, dnf).holds(new TautologyPredicate(_c.f))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testSmallFormulas(final FormulaContext _c) throws IOException, ParserException {
        final BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/formulas/small_formulas.txt"));
        while (reader.ready()) {
            final Formula formula = _c.p.parse(reader.readLine());
            final List<Variable> variables = new ArrayList<>(formula.variables(_c.f));
            final List<Variable> projectedVars = variables.subList(0, Math.min(6, variables.size()));

            final SATSolver solver = MiniSat.miniSat(_c.f);
            solver.add(formula);
            final List<Assignment> models = solver.enumerateAllModels(projectedVars);
            final List<Formula> operands = new ArrayList<>(models.size());
            for (final Assignment model : models) {
                operands.add(model.formula(_c.f));
            }
            final Formula canonicalDNF = _c.f.or(operands);

            final Formula dnf = canonicalDNF.transform(new QuineMcCluskeySimplifier(_c.f));
            assertThat(dnf.isDNF(_c.f)).isTrue();
            assertThat(_c.f.equivalence(canonicalDNF, dnf).holds(new TautologyPredicate(_c.f))).isTrue();
        }
    }

    @ParameterizedTest
    @MethodSource("contexts")
    public void testTrivialCases(final FormulaContext _c) {
        final Formula verumDNF = _c.f.verum().transform(new QuineMcCluskeySimplifier(_c.f));
        final Formula falsumDNF = _c.f.falsum().transform(new QuineMcCluskeySimplifier(_c.f));
        final Formula aDNF = _c.f.variable("a").transform(new QuineMcCluskeySimplifier(_c.f));
        final Formula notADNF = _c.f.literal("a", false).transform(new QuineMcCluskeySimplifier(_c.f));
        assertThat(verumDNF).isEqualTo(_c.f.verum());
        assertThat(falsumDNF).isEqualTo(_c.f.falsum());
        assertThat(aDNF).isEqualTo(_c.f.variable("a"));
        assertThat(notADNF).isEqualTo(_c.f.literal("a", false));
    }
}
