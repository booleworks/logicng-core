package com.booleworks.logicng.knowledgecompilation.sdd.functions;

import com.booleworks.logicng.formulas.Formula;
import com.booleworks.logicng.formulas.FormulaFactory;
import com.booleworks.logicng.handlers.ComputationHandler;
import com.booleworks.logicng.handlers.LngResult;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddElement;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddFactory;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNode;
import com.booleworks.logicng.knowledgecompilation.sdd.datastructures.SddNodeDecomposition;

import java.util.ArrayList;

public class SddExportFormula implements SddFunction<Formula> {
    public final SddNode sdd;

    public SddExportFormula(final SddNode sdd) {
        this.sdd = sdd;
    }

    @Override
    public LngResult<Formula> apply(final SddFactory sf, final ComputationHandler handler) {
        return LngResult.of(applyRec(sdd, sf));
    }

    public Formula applyRec(final SddNode node, final SddFactory sf) {
        final FormulaFactory f = sf.getFactory();
        if (node.isDecomposition()) {
            final SddNodeDecomposition decomp = node.asDecomposition();
            final ArrayList<Formula> elementFormulas = new ArrayList<>(decomp.getElements().size());
            for (final SddElement element : node.asDecomposition().getElements()) {
                final Formula sub = applyRec(element.getSub(), sf);
                final Formula prime = applyRec(element.getPrime(), sf);
                elementFormulas.add(f.and(sub, prime));
            }
            return f.or(elementFormulas);
        } else {
            return node.asTerminal().getTerminal();
        }
    }
}
