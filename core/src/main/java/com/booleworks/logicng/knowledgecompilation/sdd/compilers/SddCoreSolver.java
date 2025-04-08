package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.formulas.Literal;
import com.booleworks.logicng.knowledgecompilation.dnnf.DnnfCoreSolver;

public class SddCoreSolver extends DnnfCoreSolver {
    public SddCoreSolver(FormulaFactory f, int numberOfVariables) {
        super(f, numberOfVariables);
    }

    public Literal isImplied(final int variable) {
        for (int i = trail.size() - 1; i >= 0; --i) {
            final int lit = trail.get(i);
            if (var(lit) == variable) {
                return intToLiteral(lit);
            }
        }
        return null;
    }

    public LngIntVector getImplied() {
        return trail;
    }
}
