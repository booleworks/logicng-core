package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import static org.assertj.core.api.Assertions.assertThat;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.datastructures.Model;
import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.handlers.NopHandler;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.sdd.SddTestUtil;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompilerBottomUp;
import com.booleworks.logicng.knowledgecompilation.sdd.compilers.SddCompilerTopDown;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddCompilationResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.BalancedVTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeRoot;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddModelEnumeration;
import com.booleworks.logicng.knowledgecompilation.sdd.functions.SddVariablesFunction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SddRestrictTest {
    private final static List<String> FILES = List.of(
            "../test_files/sdd/compile_example1.cnf",
            "../test_files/sdd/compile_example2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_1.cnf",
            "../test_files/dnnf/both_bdd_dnnf_2.cnf",
            "../test_files/dnnf/both_bdd_dnnf_3.cnf",
            "../test_files/dnnf/both_bdd_dnnf_4.cnf",
            "../test_files/dnnf/both_bdd_dnnf_5.cnf"
    );
    private final static List<List<Integer>> RESTRICT_VARS = List.of(
            List.of(13, 3, 16, 12, 13),
            List.of(11, 4, 19, 20, 25),
            List.of(181, 146, 122, 14, 68, 79, 140, 28),
            List.of(30, 237, 36, 0, 4),
            List.of(9, 181, 85, 35),
            List.of(132, 163, 55, 209, 18, 132, 40),
            List.of(49, 129, 55, 133, 63, 183)
    );

    @Test
    public void testRestrictSimple() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sf = Sdd.independent(f);
        final Formula formula = f.parse("(A | ~C) & (B | C | D) & (B | D) & (X | C)");
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sf);
        final VTreeRoot root = sf.constructRoot(vtree);
        final SddNode node = SddCompilerBottomUp.cnfToSdd(formula, root, sf, NopHandler.get()).getResult();
        final int cIdx = sf.variableToIndex(f.variable("C"));
        SddNode restricted =
                SddRestrict.restrict(cIdx, true, node, root, sf, NopHandler.get()).getResult();
        checkRestrictedModel(f.literal("C", true), restricted, node, root, sf);
        restricted =
                SddRestrict.restrict(cIdx, false, node, root, sf, NopHandler.get()).getResult();
        checkRestrictedModel(f.literal("C", false), restricted, node, root, sf);
    }

    @Test
    public void testRestrictMultiple() throws ParserException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sf = Sdd.independent(f);
        final Formula formula = f.parse("(A | ~C) & (B | C | D) & (B | D) & (X | C)");
        final VTree vtree = new BalancedVTreeGenerator(formula.variables(f)).generate(sf);
        final VTreeRoot root = sf.constructRoot(vtree);
        final SddNode node = SddCompilerBottomUp.cnfToSdd(formula, root, sf, NopHandler.get()).getResult();
        final int aIdx = sf.variableToIndex(f.variable("A"));
        final int bIdx = sf.variableToIndex(f.variable("B"));
        final int dIdx = sf.variableToIndex(f.variable("D"));
        final SddNode restricted =
                SddRestrict.restrict(aIdx, true, node, root, sf, NopHandler.get()).getResult();
        checkRestrictedModel(f.literal("A", true), restricted, node, root, sf);
        final SddNode restricted2 =
                SddRestrict.restrict(dIdx, false, restricted, root, sf, NopHandler.get()).getResult();
        checkRestrictedModel(f.literal("D", false), restricted2, restricted, root, sf);
        final SddNode restricted3 =
                SddRestrict.restrict(bIdx, false, restricted2, root, sf, NopHandler.get()).getResult();
        assertThat(restricted3.isFalse()).isTrue();
    }

    @Test
    public void testFilesRestrictMultiple() throws IOException {
        int fileIndex = 0;
        for (final String file : FILES) {
            final FormulaFactory f = FormulaFactory.caching();
            final Sdd sf = Sdd.independent(f);
            final Formula formula = f.and(DimacsReader.readCNF(f, file));
            final SddCompilationResult result =
                    SddCompilerTopDown.compile(formula, sf, NopHandler.get()).getResult();
            SddNode node = result.getSdd();
            final VTreeRoot root = result.getVTree();
            final List<Variable> vars = new ArrayList<>(formula.variables(f));
            final List<Literal> restrictVars =
                    RESTRICT_VARS.get(fileIndex).stream().map(i -> i % 2 == 0 ? vars.get(i) : vars.get(i).negate(f))
                            .collect(Collectors.toList());
            final Assignment rs = new Assignment();
            for (final Literal r : restrictVars) {
                final int litIdx = sf.variableToIndex(r.variable());
                node = SddRestrict.restrict(litIdx, r.getPhase(), node, root, sf, NopHandler.get()).getResult();
                rs.addLiteral(r);
            }
            SddTestUtil.validateExport(node, formula.restrict(f, rs), sf);
            fileIndex++;
        }
    }

    private static void checkRestrictedModel(final Literal lit, final SddNode restricted, final SddNode original,
                                             final VTreeRoot root, final Sdd sf) {
        final Set<Variable> originalVariables = sf.apply(new SddVariablesFunction(original));
        final Set<Variable> restrictedVariables = new TreeSet<>(originalVariables);
        restrictedVariables.remove(lit.variable());
        final List<Model> originalModels =
                sf.apply(new SddModelEnumeration(originalVariables, original, root));
        final List<Model> restrictedModels =
                sf.apply(new SddModelEnumeration(restrictedVariables, restricted, root));
        final Set<Assignment> restrictedModelsWithA = restrictedModels
                .stream()
                .map(Model::toAssignment)
                .peek(m -> m.addLiteral(lit))
                .collect(Collectors.toSet());
        final Set<Assignment> originalAssignmentsWithA = originalModels
                .stream()
                .map(Model::toAssignment)
                .filter(m -> lit.getPhase()
                             ? m.positiveVariables().contains(lit.variable())
                             : m.negativeLiterals().contains(lit)
                )
                .collect(Collectors.toSet());
        assertThat(restrictedModelsWithA).containsExactlyInAnyOrderElementsOf(originalAssignmentsWithA);
    }
}
