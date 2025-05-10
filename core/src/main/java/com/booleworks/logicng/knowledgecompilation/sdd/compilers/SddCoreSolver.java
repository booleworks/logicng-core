package com.booleworks.logicng.knowledgecompilation.sdd.compilers;

import com.booleworks.logicng.collections.LngIntVector;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.knowledgecompilation.dnnf.DnnfCoreSolver;

public class SddCoreSolver extends DnnfCoreSolver {
    public SddCoreSolver(final FormulaFactory f, final int numberOfVariables) {
        super(f, numberOfVariables);
    }

    public int isImplied(final int variable) {
        for (int i = trail.size() - 1; i >= 0; --i) {
            final int lit = trail.get(i);
            if (var(lit) == variable) {
                return lit;
            }
        }
        return -1;
    }

    public LngIntVector getImplied() {
        return trail;
    }
}
