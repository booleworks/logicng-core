package com.booleworks.logicng.knowledgecompilation.sdd.algorithms;

import com.booleworks.logicng.datastructures.Assignment;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.Sdd;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;

import java.util.HashMap;
import java.util.Map;

public class SddSatisfiability {
    public static boolean evaluate(final Assignment assignment, final SddNode node, final Sdd sdd) {
        return evaluateRecursive(assignment, node, sdd, new HashMap<>());
    }

    private static boolean evaluateRecursive(final Assignment assignment, final SddNode node, final Sdd sdd,
                                             final Map<SddNode, Boolean> cache) {
        final Boolean cached = cache.get(node);
        if (cached != null) {
            return cached;
        }
        boolean result;
        if (node.isFalse()) {
            result = false;
        } else if (node.isTrue()) {
            result = true;
        } else if (node.isLiteral()) {
            final Literal lit = (Literal) node.asTerminal().toFormula(sdd);
            result = assignment.evaluateLit(lit);
        } else {
            result = false;
            for (final SddElement element : node.asDecomposition()) {
                if (evaluateRecursive(assignment, element.getPrime(), sdd, cache)
                        && evaluateRecursive(assignment, element.getSub(), sdd, cache)) {
                    result = true;
                    break;
                }
            }
        }
        cache.put(node, result);
        return result;
    }
}
