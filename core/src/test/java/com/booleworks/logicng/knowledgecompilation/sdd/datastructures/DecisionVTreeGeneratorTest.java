package com.booleworks.logicng.knowledgecompilation.sdd.datastructures;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.formulas.Variable;
import com.booleworks.logicng.io.parsers.ParserException;
import com.booleworks.logicng.io.readers.DimacsReader;
import com.booleworks.logicng.knowledgecompilation.dnnf.DnnfCoreSolver;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.DecisionVTreeGenerator;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTree;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.vtree.VTreeInternal;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class DecisionVTreeGeneratorTest {
    @Test
    public void test() throws ParserException, IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sf = Sdd.independent(f);
        final Formula formula = f.and(DimacsReader.readCNF(f, "../test_files/sdd/compile_example1.cnf"));
        final DnnfCoreSolver solver = new DnnfCoreSolver(f, formula.variables(f).size());
        solver.add(formula);

        final VTree tree = new DecisionVTreeGenerator(formula, solver).generate(sf);
        assert !tree.isLeaf();
        assert isDecisionVTreeFor(tree.asInternal(), formula, sf);
    }

    @Test
    public void testWithoutSolver() throws IOException {
        final FormulaFactory f = FormulaFactory.caching();
        final Sdd sf = Sdd.independent(f);
        final Formula formula = f.and(DimacsReader.readCNF(f, "../test_files/sdd/compile_example1.cnf"));
        final VTree tree = new DecisionVTreeGenerator(formula).generate(sf);
        assert !tree.isLeaf();
        assert isDecisionVTreeFor(tree.asInternal(), formula, sf);
    }

    private boolean isDecisionVTreeFor(final VTreeInternal vTree, final Formula cnf, final Sdd sdd) {
        if (!vTree.isShannon()) {
            switch (cnf.getType()) {
                case OR: {
                    boolean left = false;
                    boolean right = false;
                    for (final Formula op : cnf) {
                        if (!left) {
                            left = vTreeContainsVar(vTree.getLeft(), ((Literal) op).variable(), sdd);
                        }
                        if (!right) {
                            right = vTreeContainsVar(vTree.getRight(), ((Literal) op).variable(), sdd);
                        }
                        if (left && right) {
                            return false;
                        }
                    }
                    break;
                }
                case AND:
                    for (final Formula op : cnf) {
                        if (!isDecisionVTreeFor(vTree, op, sdd)) {
                            return false;
                        }
                    }
                    break;
                case LITERAL:
                case TRUE:
                case FALSE:
                    break;
                default:
                    throw new IllegalArgumentException("Expected cnf");
            }
        }
        if (!vTree.getLeft().isLeaf()) {
            final boolean res = isDecisionVTreeFor(vTree.getLeft().asInternal(), cnf, sdd);
            if (!res) {
                return false;
            }
        }
        if (!vTree.getRight().isLeaf()) {
            return isDecisionVTreeFor(vTree.getRight().asInternal(), cnf, sdd);
        }
        return true;
    }

    private boolean vTreeContainsVar(final VTree vTree, final Variable var, final Sdd sdd) {
        if (vTree.isLeaf()) {
            return sdd.knows(var) && vTree.asLeaf().getVariable() == sdd.variableToIndex(var);
        } else {
            return vTreeContainsVar(vTree.asInternal().getLeft(), var, sdd)
                    && vTreeContainsVar(vTree.asInternal().getRight(), var, sdd);
        }
    }
}
