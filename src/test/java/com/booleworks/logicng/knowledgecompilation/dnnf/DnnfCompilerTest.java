// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.encodings.EncoderConfig;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.PBConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.graphs.algorithms.ConnectedComponentsComputation;
import com.booleworks.logicng.graphs.datastructures.Graph;
import com.booleworks.logicng.graphs.datastructures.Node;
import com.booleworks.logicng.graphs.generators.ConstraintGraphGenerator;
import com.booleworks.logicng.io.parsers.FormulaParser;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.knowledgecompilation.bdds.BDD;
import com.booleworks.logicng.knowledgecompilation.bdds.BDDFactory;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BDDKernel;
import com.booleworks.logicng.knowledgecompilation.bdds.orderings.ForceOrdering;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.Dnnf;
import com.booleworks.logicng.knowledgecompilation.dnnf.functions.DnnfModelCountFunction;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import com.booleworks.logicng.transformations.cnf.CNFFactorization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DnnfCompilerTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final FormulaParser parser = new PropositionalParser(f);

    @Test
    public void testTrivialFormulas() throws ParserException {
        testFormula(f, parser.parse("$true"), true);
        testFormula(f, parser.parse("$false"), true);
        testFormula(f, parser.parse("a"), true);
        testFormula(f, parser.parse("~a"), true);
        testFormula(f, parser.parse("a & b"), true);
        testFormula(f, parser.parse("a | b"), true);
        testFormula(f, parser.parse("a => b"), true);
        testFormula(f, parser.parse("a <=> b"), true);
        testFormula(f, parser.parse("a | b | c"), true);
        testFormula(f, parser.parse("a & b & c"), true);
        testFormula(f, parser.parse("f & ((~b | c) <=> ~a & ~c)"), true);
        testFormula(f, parser.parse("a | ((b & ~c) | (c & (~d | ~a & b)) & e)"), true);
        testFormula(f, parser.parse("a + b + c + d <= 1"), true);
        testFormula(f, parser.parse("a + b + c + d <= 3"), true);
        testFormula(f, parser.parse("2*a + 3*b + -2*c + d < 5"), true);
        testFormula(f, parser.parse("2*a + 3*b + -2*c + d >= 5"), true);
        testFormula(f, parser.parse("~a & (~a | b | c | d)"), true);
    }

    @Test
    @LongRunningTag
    public void testLargeFormulas() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        List<Formula> dimacs = DimacsReader.readCNF(f, "src/test/resources/dnnf/both_bdd_dnnf_1.cnf");
        testFormula(f, f.cnf(dimacs), true);
        dimacs = DimacsReader.readCNF(f, "src/test/resources/dnnf/both_bdd_dnnf_2.cnf");
        testFormula(f, f.cnf(dimacs), true);
        dimacs = DimacsReader.readCNF(f, "src/test/resources/dnnf/both_bdd_dnnf_3.cnf");
        testFormula(f, f.cnf(dimacs), true);
        dimacs = DimacsReader.readCNF(f, "src/test/resources/dnnf/both_bdd_dnnf_4.cnf");
        testFormula(f, f.cnf(dimacs), true);
        dimacs = DimacsReader.readCNF(f, "src/test/resources/dnnf/both_bdd_dnnf_5.cnf");
        testFormula(f, f.cnf(dimacs), true);
    }

    @Test
    public void testDnnfProperties() throws ParserException {
        final Dnnf dnnf = new DnnfFactory().compile(f, parser.parse("a | ((b & ~c) | (c & (~d | ~a & b)) & e)"));
        assertThat(dnnf.getOriginalVariables()).extracting(Variable::name).containsExactlyInAnyOrder("a", "b", "c", "d",
                "e");
    }

    @Test
    @LongRunningTag
    public void testAllSmallFormulas() throws IOException, ParserException {
        final Formula formulas =
                FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/small_formulas.txt");
        formulas.stream().forEach(op -> testFormula(f, op, false));
    }

    @Test
    @LongRunningTag
    public void testLargeFormula() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        f.putConfiguration(EncoderConfig.builder().amoEncoding(EncoderConfig.AMO_ENCODER.PURE).build());
        final Formula parsed = FormulaReader.readPropositionalFormula(f, "src/test/resources/formulas/formula1.txt");
        final DnnfFactory dnnfFactory = new DnnfFactory();
        Dnnf dnnf = dnnfFactory.compile(f, parsed);
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
        final Graph<Variable> constraintGraph = ConstraintGraphGenerator.generateFromFormulas(f, formulas);
        final Set<Set<Node<Variable>>> ccs = ConnectedComponentsComputation.compute(constraintGraph);
        final List<List<Formula>> split =
                ConnectedComponentsComputation.splitFormulasByComponent(f, originalFormulas, ccs);
        BigInteger multipliedCount = BigInteger.ONE;
        for (final List<Formula> component : split) {
            dnnf = dnnfFactory.compile(f, f.and(component));
            multipliedCount = multipliedCount.multiply(dnnf.execute(new DnnfModelCountFunction(f)));
        }
        assertThat(dnnfCount).isEqualTo(multipliedCount);
    }

    private void testFormula(final FormulaFactory f, final Formula formula, final boolean withEquivalence) {
        final DnnfFactory dnnfFactory = new DnnfFactory();
        final Dnnf dnnf = dnnfFactory.compile(f, formula);
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
        final BDDKernel kernel = new BDDKernel(formula.factory(),
                new ForceOrdering().getOrder(formula.factory(), formula), 100000, 1000000);
        final BDD bdd = BDDFactory.build(formula.factory(), formula, kernel);
        return bdd.modelCount();
    }
}
