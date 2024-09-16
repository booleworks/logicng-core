// SPDX-License-Identifier: Apache-2.0 and MIT
// Copyright 2015-2023 Christoph Zengler
// Copyright 2023-20xx BooleWorks GmbH

package com.booleworks.logicng.knowledgecompilation.dnnf;

import static com.booleworks.logicng.handlers.events.ComputationFinishedEvent.SAT_CALL_FINISHED;
import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.BACKBONE_COMPUTATION_STARTED;
import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.DNNF_COMPUTATION_STARTED;
import static com.booleworks.logicng.handlers.events.ComputationStartedEvent.SAT_CALL_STARTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.DNNF_DTREE_MIN_FILL_GRAPH_INITIALIZED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.DNNF_DTREE_MIN_FILL_NEW_ITERATION;
import static com.booleworks.logicng.handlers.events.SimpleEvent.DNNF_DTREE_PROCESSING_NEXT_ORDER_VARIABLE;
import static com.booleworks.logicng.handlers.events.SimpleEvent.DNNF_SHANNON_EXPANSION;
import static com.booleworks.logicng.handlers.events.SimpleEvent.SAT_CONFLICT_DETECTED;
import static com.booleworks.logicng.handlers.events.SimpleEvent.SUBSUMPTION_ADDED_NEW_SET;
import static com.booleworks.logicng.handlers.events.SimpleEvent.SUBSUMPTION_STARTING_UB_TREE_GENERATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.booleworks.logicng.LongRunningTag;
import com.booleworks.logicng.encodings.EncoderConfig;
import com.booleworks.logicng.formulas.FType;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.PbConstraint;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.graphs.algorithms.ConnectedComponentsComputation;
import com.booleworks.logicng.graphs.datastructures.Graph;
import com.booleworks.logicng.graphs.datastructures.Node;
import com.booleworks.logicng.graphs.generators.ConstraintGraphGenerator;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.handlers.events.LngEvent;
import com.booleworks.logicng.io.parsers.FormulaParser;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.parsers.PropositionalParser;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.io.readers.FormulaReader;
import com.booleworks.logicng.knowledgecompilation.bdds.Bdd;
import com.booleworks.logicng.knowledgecompilation.bdds.BddFactory;
import com.booleworks.logicng.knowledgecompilation.bdds.jbuddy.BddKernel;
import com.booleworks.logicng.knowledgecompilation.bdds.orderings.ForceOrdering;
import com.booleworks.logicng.knowledgecompilation.dnnf.datastructures.Dnnf;
import com.booleworks.logicng.knowledgecompilation.dnnf.functions.DnnfModelCountFunction;
import com.booleworks.logicng.predicates.satisfiability.TautologyPredicate;
import com.booleworks.logicng.transformations.cnf.CnfFactorization;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DnnfCompilerTest {

    private final FormulaFactory f = FormulaFactory.caching();
    private final FormulaParser parser = new PropositionalParser(f);

    @Test
    public void testTrivialFormulas() throws ParserException {
        testFormula(f, parser.parse("$true"));
        testFormula(f, parser.parse("$false"));
        testFormula(f, parser.parse("a"));
        testFormula(f, parser.parse("~a"));
        testFormula(f, parser.parse("a & b"));
        testFormula(f, parser.parse("a | b"));
        testFormula(f, parser.parse("a => b"));
        testFormula(f, parser.parse("a <=> b"));
        testFormula(f, parser.parse("a | b | c"));
        testFormula(f, parser.parse("a & b & c"));
        testFormula(f, parser.parse("f & ((~b | c) <=> ~a & ~c)"));
        testFormula(f, parser.parse("a | ((b & ~c) | (c & (~d | ~a & b)) & e)"));
        testFormula(f, parser.parse("a + b + c + d <= 1"));
        testFormula(f, parser.parse("a + b + c + d <= 3"));
        testFormula(f, parser.parse("2*a + 3*b + -2*c + d < 5"));
        testFormula(f, parser.parse("2*a + 3*b + -2*c + d >= 5"));
        testFormula(f, parser.parse("~a & (~a | b | c | d)"));
    }

    @Test
    @LongRunningTag
    public void testLargeFormulas() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        List<Formula> dimacs = DimacsReader.readCNF(f, "../test_files/dnnf/both_bdd_dnnf_1.cnf");
        testFormula(f, f.cnf(dimacs));
        dimacs = DimacsReader.readCNF(f, "../test_files/dnnf/both_bdd_dnnf_2.cnf");
        testFormula(f, f.cnf(dimacs));
        dimacs = DimacsReader.readCNF(f, "../test_files/dnnf/both_bdd_dnnf_3.cnf");
        testFormula(f, f.cnf(dimacs));
        dimacs = DimacsReader.readCNF(f, "../test_files/dnnf/both_bdd_dnnf_4.cnf");
        testFormula(f, f.cnf(dimacs));
        dimacs = DimacsReader.readCNF(f, "../test_files/dnnf/both_bdd_dnnf_5.cnf");
        testFormula(f, f.cnf(dimacs));
    }

    @Test
    public void testDnnfProperties() throws ParserException {
        final Dnnf dnnf = DnnfCompiler.compile(f, parser.parse("a | ((b & ~c) | (c & (~d | ~a & b)) & e)"));
        assertThat(dnnf.getOriginalVariables()).extracting(Variable::getName)
                .containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    }

    @Test
    public void testDnnfEvents() throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        f.putConfiguration(EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.PURE).build());
        final Formula parsed = FormulaReader.readFormula(f, "../test_files/formulas/formula1.txt");
        final DnnfComputationHandler handler = new DnnfComputationHandler();
        final LngResult<Dnnf> dnnf = DnnfCompiler.compile(f, parsed, handler);
        assertThat(dnnf.isSuccess()).isTrue();
        assertThat(handler.eventCounter).containsExactly(
                entry(BACKBONE_COMPUTATION_STARTED, 1),
                entry(SAT_CALL_STARTED, 125),
                entry(SAT_CONFLICT_DETECTED, 51),
                entry(SAT_CALL_FINISHED, 125),
                entry(SUBSUMPTION_STARTING_UB_TREE_GENERATION, 1),
                entry(SUBSUMPTION_ADDED_NEW_SET, 4104),
                entry(DNNF_DTREE_MIN_FILL_GRAPH_INITIALIZED, 1),
                entry(DNNF_DTREE_MIN_FILL_NEW_ITERATION, 411),
                entry(DNNF_DTREE_PROCESSING_NEXT_ORDER_VARIABLE, 411),
                entry(DNNF_COMPUTATION_STARTED, 1),
                entry(DNNF_SHANNON_EXPANSION, 6866)
        );
    }

    @Test
    @LongRunningTag
    public void testLargeFormula() throws IOException, ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        f.putConfiguration(EncoderConfig.builder().amoEncoding(EncoderConfig.AmoEncoder.PURE).build());
        final Formula parsed = FormulaReader.readFormula(f, "../test_files/formulas/formula1.txt");
        Dnnf dnnf = DnnfCompiler.compile(f, parsed);
        final BigInteger dnnfCount = dnnf.execute(new DnnfModelCountFunction(f));
        final List<Formula> formulas = new ArrayList<>();
        final List<Formula> originalFormulas = new ArrayList<>();
        for (final Formula formula : parsed) {
            originalFormulas.add(formula);
            if (formula instanceof PbConstraint) {
                formulas.add(formula);
            } else {
                formulas.add(formula.transform(new CnfFactorization(f)));
            }
        }
        final Graph<Variable> constraintGraph = ConstraintGraphGenerator.generateFromFormulas(f, formulas);
        final Set<Set<Node<Variable>>> ccs = ConnectedComponentsComputation.compute(constraintGraph);
        final List<List<Formula>> split =
                ConnectedComponentsComputation.splitFormulasByComponent(f, originalFormulas, ccs);
        BigInteger multipliedCount = BigInteger.ONE;
        for (final List<Formula> component : split) {
            dnnf = DnnfCompiler.compile(f, f.and(component));
            multipliedCount = multipliedCount.multiply(dnnf.execute(new DnnfModelCountFunction(f)));
        }
        assertThat(dnnfCount).isEqualTo(multipliedCount);
    }

    private void testFormula(final FormulaFactory f, final Formula formula) {
        final Dnnf dnnf = DnnfCompiler.compile(f, formula);
        final BigInteger dnnfCount = dnnf.execute(new DnnfModelCountFunction(f));
        final Formula equivalence = f.equivalence(formula, dnnf.getFormula());
        assertThat(equivalence.holds(new TautologyPredicate(formula.getFactory()))).isTrue();
        final BigInteger bddCount = countWithBdd(formula);
        assertThat(dnnfCount).isEqualTo(bddCount);
    }

    private BigInteger countWithBdd(final Formula formula) {
        if (formula.getType() == FType.TRUE) {
            return BigInteger.ONE;
        } else if (formula.getType() == FType.FALSE) {
            return BigInteger.ZERO;
        }
        final BddKernel kernel = new BddKernel(formula.getFactory(),
                new ForceOrdering().getOrder(formula.getFactory(), formula), 100000, 1000000);
        final Bdd bdd = BddFactory.build(formula.getFactory(), formula, kernel);
        return bdd.modelCount();
    }

    private static class DnnfComputationHandler implements ComputationHandler {
        private final Map<LngEvent, Integer> eventCounter = new LinkedHashMap<>();

        @Override
        public boolean shouldResume(final LngEvent event) {
            eventCounter.put(event, eventCounter.getOrDefault(event, 0) + 1);
            return true;
        }
    }
}
