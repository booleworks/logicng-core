// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package org.logicng.knowledgecompilation.dnnf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.logicng.LongRunningTag;
import org.logicng.cardinalityconstraints.CCConfig;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.PBConstraint;
import org.logicng.formulas.Variable;
import org.logicng.graphs.algorithms.ConnectedComponentsComputation;
import org.logicng.graphs.datastructures.Graph;
import org.logicng.graphs.datastructures.Node;
import org.logicng.graphs.generators.ConstraintGraphGenerator;
import org.logicng.io.parsers.FormulaParser;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PseudoBooleanParser;
import org.logicng.io.readers.DimacsReader;
import org.logicng.io.readers.FormulaReader;
import org.logicng.knowledgecompilation.bdds.BDD;
import org.logicng.knowledgecompilation.bdds.BDDFactory;
import org.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import org.logicng.knowledgecompilation.bdds.orderings.ForceOrdering;
import org.logicng.knowledgecompilation.dnnf.datastructures.Dnnf;
import org.logicng.knowledgecompilation.dnnf.functions.DnnfModelCountFunction;
import org.logicng.predicates.satisfiability.TautologyPredicate;
import org.logicng.transformations.cnf.CNFFactorization;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DnnfCompilerTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final FormulaParser parser = new PseudoBooleanParser(f);

    @Test
    public void testTrivialFormulas() throws ParserException {
        testFormula(parser.parse("$true"), f, true);
        testFormula(parser.parse("$false"), f, true);
        testFormula(parser.parse("a"), f, true);
        testFormula(parser.parse("~a"), f, true);
        testFormula(parser.parse("a & b"), f, true);
        testFormula(parser.parse("a | b"), f, true);
        testFormula(parser.parse("a => b"), f, true);
        testFormula(parser.parse("a <=> b"), f, true);
        testFormula(parser.parse("a | b | c"), f, true);
        testFormula(parser.parse("a & b & c"), f, true);
        testFormula(parser.parse("f & ((~b | c) <=> ~a & ~c)"), f, true);
        testFormula(parser.parse("a | ((b & ~c) | (c & (~d | ~a & b)) & e)"), f, true);
        testFormula(parser.parse("a + b + c + d <= 1"), f, true);
        testFormula(parser.parse("a + b + c + d <= 3"), f, true);
        testFormula(parser.parse("2*a + 3*b + -2*c + d < 5"), f, true);
        testFormula(parser.parse("2*a + 3*b + -2*c + d >= 5"), f, true);
        testFormula(parser.parse("~a & (~a | b | c | d)"), f, true);
    }

    @Test
    public void testLargeFormulas() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        List<Formula> dimacs = DimacsReader.readCNF("src/test/resources/dnnf/both_bdd_dnnf_1.cnf", f);
        testFormula(f.cnf(dimacs), f, true);
        dimacs = DimacsReader.readCNF("src/test/resources/dnnf/both_bdd_dnnf_2.cnf", f);
        testFormula(f.cnf(dimacs), f, true);
        dimacs = DimacsReader.readCNF("src/test/resources/dnnf/both_bdd_dnnf_3.cnf", f);
        testFormula(f.cnf(dimacs), f, true);
        dimacs = DimacsReader.readCNF("src/test/resources/dnnf/both_bdd_dnnf_4.cnf", f);
        testFormula(f.cnf(dimacs), f, true);
        dimacs = DimacsReader.readCNF("src/test/resources/dnnf/both_bdd_dnnf_5.cnf", f);
        testFormula(f.cnf(dimacs), f, true);
    }

    @Test
    public void testDnnfProperties() throws ParserException {
        final Dnnf dnnf = new DnnfFactory().compile(parser.parse("a | ((b & ~c) | (c & (~d | ~a & b)) & e)"), f);
        assertThat(dnnf.getOriginalVariables()).extracting(Variable::name).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    }

    @Test
    @LongRunningTag
    public void testAllSmallFormulas() throws IOException, ParserException {
        final Formula formulas = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/small_formulas.txt", f);
        formulas.stream().forEach(op -> testFormula(op, f, false));
    }

    @Test
    @LongRunningTag
    public void testLargeFormula() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        f.putConfiguration(CCConfig.builder().amoEncoding(CCConfig.AMO_ENCODER.PURE).build());
        final Formula parsed = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/formula1.txt", f);
        final DnnfFactory dnnfFactory = new DnnfFactory();
        Dnnf dnnf = dnnfFactory.compile(parsed, f);
        final BigInteger dnnfCount = dnnf.execute(new DnnfModelCountFunction(f));
        final List<Formula> formulas = new ArrayList<>();
        final List<Formula> originalFormulas = new ArrayList<>();
        for (final Formula formula : parsed) {
            originalFormulas.add(formula);
            if (formula instanceof PBConstraint) {
                formulas.add(formula);
            } else {
                formulas.add(formula.transform(new CNFFactorization(f)));
            }
        }
        final Graph<Variable> constraintGraph = ConstraintGraphGenerator.generateFromFormulas(formulas);
        final Set<Set<Node<Variable>>> ccs = ConnectedComponentsComputation.compute(constraintGraph);
        final List<List<Formula>> split = ConnectedComponentsComputation.splitFormulasByComponent(originalFormulas, ccs);
        BigInteger multipliedCount = BigInteger.ONE;
        for (final List<Formula> component : split) {
            dnnf = dnnfFactory.compile(f.and(component), f);
            multipliedCount = multipliedCount.multiply(dnnf.execute(new DnnfModelCountFunction(f)));
        }
        assertThat(dnnfCount).isEqualTo(multipliedCount);
    }

    private void testFormula(final Formula formula, final FormulaFactory f, final boolean withEquivalence) {
        final DnnfFactory dnnfFactory = new DnnfFactory();
        final Dnnf dnnf = dnnfFactory.compile(formula, f);
        final BigInteger dnnfCount = dnnf.execute(new DnnfModelCountFunction(f));
        if (withEquivalence) {
            final Formula equivalence = f.equivalence(formula, dnnf.formula());
            assertThat(equivalence.holds(new TautologyPredicate(formula.factory()))).isTrue();
        }
        final BigInteger bddCount = countWithBdd(formula);
        assertThat(dnnfCount).isEqualTo(bddCount);
    }

    private BigInteger countWithBdd(final Formula formula) {
        if (formula.type() == FType.TRUE) {
            return BigInteger.ONE;
        } else if (formula.type() == FType.FALSE) {
            return BigInteger.ZERO;
        }
        final BDDKernel kernel = new BDDKernel(formula.factory(), new ForceOrdering().getOrder(formula.factory(), formula), 100000, 1000000);
        final BDD bdd = BDDFactory.build(formula.factory(), formula, kernel);
        return bdd.modelCount();
    }
}
