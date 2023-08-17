// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.transformations.simplification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
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

public class QuineMcCluskeyTest {

    @Test
    public void testSimple1() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final Formula formula = p.parse("(~a & ~b & ~c) | (~a & ~b & c) | (~a & b & ~c) | (a & ~b & c) | (a & b & ~c) | (a & b & c)");
        final Formula dnf = formula.transform(new QuineMcCluskeySimplifier(f));
        assertThat(dnf.isDNF()).isTrue();
        assertThat(f.equivalence(formula, dnf).holds(new TautologyPredicate(f))).isTrue();
    }

    @Test
    public void testSimple2() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final Formula formula = p.parse("(~a & ~b & ~c) | (~a & b & ~c) | (a & ~b & c) | (a & b & c)");
        final Formula dnf = formula.transform(new QuineMcCluskeySimplifier(f));
        assertThat(dnf.isDNF()).isTrue();
        assertThat(f.equivalence(formula, dnf).holds(new TautologyPredicate(f))).isTrue();
    }

    /**
     * Example from <a href="https://github.com/logic-ng/LogicNG/issues/15">issue 15</a>.
     * Ensure only original formula variables are returned, i.e., no auxiliary variables are returned.
     * @throws ParserException if any malformed formula is encountered
     */
    @Test
    public void testSimple3() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final Formula formula = p.parse("~5 & ~4 & 3 & 2 & 1 | ~3 & ~7 & ~2 & 1 | ~6 & 1 & ~3 & 2 | ~9 & 6 & 8 & ~1 | 3 & 4 & 2 & 1 | ~2 & 7 & 1 | ~10 & ~8 & ~1");
        final Formula dnf = formula.transform(new QuineMcCluskeySimplifier(f));
        assertThat(dnf.isDNF()).isTrue();
        assertThat(f.equivalence(formula, dnf).holds(new TautologyPredicate(f))).isTrue();
        assertThat(formula.variables()).containsAll(dnf.variables());
    }

    @Test
    public void testLarge1() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final Formula formula = p.parse("A => B & ~((D | E | I | J) & ~K) & L");
        final Formula dnf = formula.transform(new QuineMcCluskeySimplifier(f));
        assertThat(dnf.isDNF()).isTrue();
        assertThat(f.equivalence(formula, dnf).holds(new TautologyPredicate(f))).isTrue();
    }

    @Test
    public void testLarge2() throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/large_formula.txt", f);
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(formula);
        final List<Assignment> models = solver.enumerateAllModels(Arrays.asList(
                f.variable("v111"),
                f.variable("v410"),
                f.variable("v434"),
                f.variable("v35"),
                f.variable("v36"),
                f.variable("v78"),
                f.variable("v125"),
                f.variable("v125"),
                f.variable("v58"),
                f.variable("v61")));
        final List<Formula> operands = new ArrayList<>(models.size());
        for (final Assignment model : models) {
            operands.add(model.formula(f));
        }
        final Formula canonicalDNF = f.or(operands);
        final Formula dnf = canonicalDNF.transform(new QuineMcCluskeySimplifier(f));
        assertThat(dnf.isDNF()).isTrue();
        assertThat(f.equivalence(canonicalDNF, dnf).holds(new TautologyPredicate(f))).isTrue();
    }

    @Test
    public void testLarge3() throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/large_formula.txt", f);
        final SATSolver solver = MiniSat.miniSat(f);
        solver.add(formula);
        final List<Assignment> models = solver.enumerateAllModels(Arrays.asList(
                f.variable("v111"),
                f.variable("v410"),
                f.variable("v434"),
                f.variable("v35"),
                f.variable("v36"),
                f.variable("v78"),
                f.variable("v125"),
                f.variable("v125"),
                f.variable("v58"),
                f.variable("v27"),
                f.variable("v462"),
                f.variable("v463"),
                f.variable("v280"),
                f.variable("v61")));
        final List<Formula> operands = new ArrayList<>(models.size());
        for (final Assignment model : models) {
            operands.add(model.formula(f));
        }
        final Formula canonicalDNF = f.or(operands);
        final Formula dnf = canonicalDNF.transform(new QuineMcCluskeySimplifier(f));
        assertThat(dnf.isDNF()).isTrue();
        assertThat(f.equivalence(canonicalDNF, dnf).holds(new TautologyPredicate(f))).isTrue();
    }

    @Test
    public void testSmallFormulas() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final PropositionalParser p = new PropositionalParser(f);
        final BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/formulas/small_formulas.txt"));
        while (reader.ready()) {
            final Formula formula = p.parse(reader.readLine());
            final List<Variable> variables = new ArrayList<>(formula.variables());
            final List<Variable> projectedVars = variables.subList(0, Math.min(6, variables.size()));

            final SATSolver solver = MiniSat.miniSat(f);
            solver.add(formula);
            final List<Assignment> models = solver.enumerateAllModels(projectedVars);
            final List<Formula> operands = new ArrayList<>(models.size());
            for (final Assignment model : models) {
                operands.add(model.formula(f));
            }
            final Formula canonicalDNF = f.or(operands);

            final Formula dnf = canonicalDNF.transform(new QuineMcCluskeySimplifier(f));
            assertThat(dnf.isDNF()).isTrue();
            assertThat(f.equivalence(canonicalDNF, dnf).holds(new TautologyPredicate(f))).isTrue();
        }
    }

    @Test
    public void testTrivialCases() {
        final FormulaFactory f = FormulaFactory.caching();
        final Formula verumDNF = f.verum().transform(new QuineMcCluskeySimplifier(f));
        final Formula falsumDNF = f.falsum().transform(new QuineMcCluskeySimplifier(f));
        final Formula aDNF = f.variable("a").transform(new QuineMcCluskeySimplifier(f));
        final Formula notADNF = f.literal("a", false).transform(new QuineMcCluskeySimplifier(f));
        assertThat(verumDNF).isEqualTo(f.verum());
        assertThat(falsumDNF).isEqualTo(f.falsum());
        assertThat(aDNF).isEqualTo(f.variable("a"));
        assertThat(notADNF).isEqualTo(f.literal("a", false));
    }
}
